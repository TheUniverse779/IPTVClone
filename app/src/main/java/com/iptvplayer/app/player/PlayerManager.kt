package com.iptvplayer.app.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.data.network.HttpClients
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/** One playable source with its per-stream HTTP headers / DRM (from M3U #EXTVLCOPT, #KODIPROP, `|` headers). */
data class StreamSource(
    val url: String,
    val title: String,
    val subtitle: String = "",
    val userAgent: String? = null,
    val referrer: String? = null,
    val headersJson: String? = null,
    val drmJson: String? = null,
    val isLive: Boolean = true,
)

data class TrackOption(val id: String, val label: String, val selected: Boolean, val group: Int, val index: Int)

enum class AspectMode { FIT, FILL, ZOOM, R16_9, R4_3 }

/**
 * App-wide ExoPlayer holder. Lives outside the Activity so playback, the sleep timer and PiP
 * survive rotation and PlayerActivity re-creation. Also backs the MediaSession service.
 *
 * Improvements over the original's stock player: OkHttp data source with per-channel
 * User-Agent/Referer/headers, ClearKey/Widevine from KODIPROP, live-tuned buffering,
 * multi-source (`;`) fallback and auto-retry with backoff.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val http: HttpClients,
    private val settings: SettingsStore,
    private val gson: Gson,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _player: ExoPlayer? = null
    val player: ExoPlayer get() = _player ?: build().also { _player = it }

    private val _error = MutableStateFlow<PlaybackException?>(null)
    val error: StateFlow<PlaybackException?> = _error
    private val _buffering = MutableStateFlow(false)
    val buffering: StateFlow<Boolean> = _buffering
    private val _tracks = MutableStateFlow(Tracks.EMPTY)
    val tracks: StateFlow<Tracks> = _tracks

    /** Remaining sleep-timer millis, or null when off. */
    private val _sleepLeft = MutableStateFlow<Long?>(null)
    val sleepLeft: StateFlow<Long?> = _sleepLeft
    private var sleepJob: Job? = null

    var current: StreamSource? = null; private set
    private var alternatives: List<String> = emptyList()
    private var altIndex = 0
    private var retries = 0
    private var retryJob: Job? = null

    private fun build(): ExoPlayer {
        val sw = runBlocking { settings.current().softwareDecoder }
        val renderers = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(if (sw) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val load = DefaultLoadControl.Builder()
            .setBufferDurationsMs(15_000, 50_000, 1_500, 3_000)
            .build()
        return ExoPlayer.Builder(context, renderers)
            .setLoadControl(load)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(3)))
            .setSeekBackIncrementMs(10_000).setSeekForwardIncrementMs(10_000)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        _buffering.value = state == Player.STATE_BUFFERING
                        if (state == Player.STATE_READY) { retries = 0; _error.value = null }
                    }
                    override fun onPlayerError(error: PlaybackException) = handleError(error)
                    override fun onTracksChanged(tracks: Tracks) { _tracks.value = tracks }
                })
            }
    }

    fun play(source: StreamSource, startPositionMs: Long = 0) {
        retryJob?.cancel()
        current = source
        alternatives = source.url.split(';').map { it.trim() }.filter { it.isNotEmpty() }
        altIndex = 0; retries = 0
        _error.value = null
        prepare(startPositionMs)
    }

    private fun prepare(startPositionMs: Long = 0) {
        val src = current ?: return
        val url = alternatives.getOrElse(altIndex) { src.url }
        val p = player
        val factory = DefaultMediaSourceFactory(dataSourceFactory(src))
        clearKeyLicense(src)?.let { json ->
            // Inline ClearKey (kid:key from #KODIPROP) is served locally instead of from a license URL.
            val drm = androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(androidx.media3.exoplayer.drm.LocalMediaDrmCallback(json.toByteArray()))
            factory.setDrmSessionManagerProvider { drm }
        }
        p.setMediaSource(factory.createMediaSource(mediaItem(src, url)))
        if (startPositionMs > 0) p.seekTo(startPositionMs)
        p.prepare()
        p.playWhenReady = true
    }

    private fun dataSourceFactory(src: StreamSource): DefaultDataSource.Factory {
        val headers = HashMap<String, String>()
        src.headersJson?.let { runCatching { headers.putAll(gson.fromJson<Map<String, String>>(it, object : TypeToken<Map<String, String>>() {}.type)) } }
        src.referrer?.let { headers["Referer"] = it }
        val http = OkHttpDataSource.Factory(this.http.ok)
            .setUserAgent(src.userAgent ?: this.http.userAgent)
            .setDefaultRequestProperties(headers)
        return DefaultDataSource.Factory(context, http)
    }

    private fun mediaItem(src: StreamSource, url: String): MediaItem {
        val b = MediaItem.Builder().setUri(Uri.parse(url))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(src.title).setArtist(src.subtitle).build())
        if (src.isLive) b.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(8_000).build())
        guessMime(url)?.let { b.setMimeType(it) }
        drm(src)?.let { (type, key) ->
            val uuid = when { "clearkey" in type -> C.CLEARKEY_UUID; "widevine" in type -> C.WIDEVINE_UUID; else -> null }
            if (uuid != null) {
                val drm = MediaItem.DrmConfiguration.Builder(uuid)
                if (key.startsWith("http")) drm.setLicenseUri(key)
                b.setDrmConfiguration(drm.build())
            }
        }
        return b.build()
    }

    private fun drm(src: StreamSource): Pair<String, String>? = src.drmJson?.let { json ->
        runCatching {
            val m = gson.fromJson<Map<String, String?>>(json, object : TypeToken<Map<String, String?>>() {}.type)
            m["type"]?.lowercase().orEmpty() to m["key"].orEmpty()
        }.getOrNull()
    }

    /** `kid:key` hex pair (or several, comma separated) → ClearKey JSON license. */
    private fun clearKeyLicense(src: StreamSource): String? {
        val (type, key) = drm(src) ?: return null
        if ("clearkey" !in type || key.startsWith("http") || !key.contains(':')) return null
        fun b64(hex: String) = android.util.Base64.encodeToString(hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
        return runCatching {
            val keys = key.split(',').map { it.trim().split(':', limit = 2) }.joinToString(",") { (kid, k) ->
                """{"kty":"oct","k":"${b64(k)}","kid":"${b64(kid)}"}"""
            }
            """{"keys":[$keys],"type":"temporary"}"""
        }.getOrNull()
    }

    private fun guessMime(url: String): String? {
        val l = url.lowercase().substringBefore('?')
        return when {
            l.endsWith(".m3u8") || l.contains("/hls/") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
            l.endsWith(".mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
            l.startsWith("rtsp://") -> androidx.media3.common.MimeTypes.APPLICATION_RTSP
            else -> null
        }
    }

    /** Try the next `;` alternative, then retry the same source up to 3 times with backoff, then surface the error. */
    private fun handleError(e: PlaybackException) {
        if (altIndex + 1 < alternatives.size) { altIndex++; prepare(); return }
        // 4xx (except 408/429) won't heal by retrying: show the error at once instead of ~15 s of backoff.
        val http = (e.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode
        val permanent = http != null && http in 400..499 && http != 408 && http != 429
        if (!permanent && retries < MAX_RETRIES && e.errorCode in RETRYABLE) {
            retries++
            retryJob = scope.launch { delay(1_000L * retries * retries); prepare(if (current?.isLive == true) 0 else player.currentPosition) }
            return
        }
        _error.value = e
    }

    fun retry() { retries = 0; altIndex = 0; _error.value = null; prepare() }

    // ---------------- tracks ----------------
    fun trackOptions(type: Int): List<TrackOption> {
        val out = mutableListOf<TrackOption>()
        _tracks.value.groups.forEachIndexed { gi, g ->
            if (g.type != type) return@forEachIndexed
            for (i in 0 until g.length) {
                val f = g.getTrackFormat(i)
                val label = when (type) {
                    C.TRACK_TYPE_VIDEO -> if (f.height > 0) "${f.height}p" else f.label ?: "Video ${i + 1}"
                    else -> listOfNotNull(f.label, f.language?.let { java.util.Locale(it).displayLanguage }).firstOrNull()
                        ?: "Track ${out.size + 1}"
                }
                val extra = if (type == C.TRACK_TYPE_AUDIO && f.channelCount > 0) " · ${if (f.channelCount >= 6) "5.1" else "stereo"}" else ""
                out += TrackOption("$gi:$i", label + extra, g.isTrackSelected(i), gi, i)
            }
        }
        return out.distinctBy { it.label }
    }

    fun selectTrack(type: Int, option: TrackOption?) {
        val p = player
        val b = p.trackSelectionParameters.buildUpon()
        if (option == null) {
            if (type == C.TRACK_TYPE_TEXT) b.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) else b.clearOverridesOfType(type)
        } else {
            val group = _tracks.value.groups[option.group].mediaTrackGroup
            b.setTrackTypeDisabled(type, false).setOverrideForType(TrackSelectionOverride(group, option.index))
        }
        p.trackSelectionParameters = b.build()
    }

    fun textEnabled() = !player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)

    // ---------------- sleep timer ----------------
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        if (minutes == null) { _sleepLeft.value = null; return }
        val end = System.currentTimeMillis() + minutes * 60_000L
        sleepJob = scope.launch {
            while (isActive) {
                val left = end - System.currentTimeMillis()
                if (left <= 0) { _player?.pause(); _sleepLeft.value = null; break }
                _sleepLeft.value = left
                delay(1_000)
            }
        }
    }

    fun stop() {
        retryJob?.cancel()
        _player?.stop()
        _player?.clearMediaItems()
        current = null
    }

    fun release() { stop(); _player?.release(); _player = null }

    companion object {
        private const val MAX_RETRIES = 3
        private val RETRYABLE = setOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW, PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )
    }
}
