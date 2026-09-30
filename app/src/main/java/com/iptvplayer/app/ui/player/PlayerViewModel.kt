package com.iptvplayer.app.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.player.PlayerManager
import com.iptvplayer.app.player.StreamSource
import com.iptvplayer.app.util.LogoUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A row in the zapping list (M3U channels or Xtream live). */
data class ZapItem(val key: String, val name: String, val sub: String, val logo: String, val isFavorite: Boolean)

data class NowPlaying(
    val title: String,
    val subtitle: String,
    val logo: String?,
    val isLive: Boolean,
    val favoriteKey: String?,
    val isFavorite: Boolean,
    val resumeMs: Long = 0,
    val listTitle: String = "",
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    val manager: PlayerManager,
    private val playlists: PlaylistRepository,
    private val xtream: XtreamRepository,
) : ViewModel() {

    private val _now = MutableStateFlow<NowPlaying?>(null)
    val now: StateFlow<NowPlaying?> = _now
    private val _list = MutableStateFlow<List<ZapItem>>(emptyList())
    val list: StateFlow<List<ZapItem>> = _list
    private val _currentKey = MutableStateFlow<String?>(null)
    val currentKey: StateFlow<String?> = _currentKey

    var request: PlayRequest? = null; private set
    private var progressJob: Job? = null

    fun open(req: PlayRequest, force: Boolean = false) {
        if (!force && req == request && manager.current != null) return
        saveProgress()
        request = req
        viewModelScope.launch {
            when (req) {
                is PlayRequest.Channel -> openChannel(req.playlistId, req.channelId)
                is PlayRequest.Url -> {
                    _list.value = emptyList(); _currentKey.value = null
                    _now.value = NowPlaying(req.title, com.iptvplayer.app.util.UrlUtils.host(req.url), null, true, null, false)
                    manager.play(StreamSource(req.url, req.title, isLive = !req.url.lowercase().substringBefore('?').let { it.endsWith(".mp4") || it.endsWith(".mkv") }))
                }
                is PlayRequest.XtreamLive -> openXtreamLive(req.profileId, req.streamId)
                is PlayRequest.XtreamMovie -> openMovie(req.profileId, req.streamId)
                is PlayRequest.XtreamEpisode -> openEpisode(req.profileId, req.seriesId, req.episodeId)
            }
            startProgressSaver()
        }
    }

    private suspend fun openChannel(playlistId: Long, channelId: Long) {
        val c = playlists.channel(channelId) ?: return
        val pl = playlists.get(playlistId)
        if (_list.value.isEmpty() || _list.value.firstOrNull()?.key?.startsWith("c:") != true) {
            _list.value = playlists.channelsIn(playlistId).map { ZapItem("c:${it.id}", it.name, it.groupName, it.logo, it.isFavorite) }
        }
        _currentKey.value = "c:${c.id}"
        _now.value = NowPlaying(c.name, listOfNotNull(pl?.name, c.groupName).joinToString(" · "), c.logo, true, "c:${c.id}", c.isFavorite, listTitle = pl?.name.orEmpty())
        playlists.markPlayed(c.id)
        manager.play(StreamSource(c.url, c.name, pl?.name.orEmpty(), c.userAgent, c.referrer, c.headersJson, c.drmJson, isLive = true))
    }

    private suspend fun openXtreamLive(profileId: String, streamId: Int) {
        val p = xtream.profile(profileId) ?: return
        val live = xtream.live(profileId, streamId) ?: return
        if (_list.value.firstOrNull()?.key?.startsWith("x:") != true) {
            _list.value = xtream.allLive(profileId).map { ZapItem("x:${it.streamId}", it.name, "", it.icon, it.isFavorite) }
        }
        _currentKey.value = "x:$streamId"
        _now.value = NowPlaying(live.name, p.name, live.icon, true, "x:$streamId", live.isFavorite, listTitle = p.name)
        xtream.markLivePlayed(profileId, streamId)
        manager.play(StreamSource(xtream.liveUrl(p, streamId), live.name, p.name, isLive = true))
    }

    private suspend fun openMovie(profileId: String, streamId: Int) {
        val p = xtream.profile(profileId) ?: return
        val v = xtream.vod(profileId, streamId) ?: return
        _list.value = emptyList(); _currentKey.value = null
        val resume = v.positionMs.takeIf { v.durationMs == 0L || it < v.durationMs - FINISHED_MS } ?: 0
        _now.value = NowPlaying(v.name, p.name, v.icon, false, "m:$streamId", v.isFavorite, resume)
        manager.play(StreamSource(xtream.movieUrl(p, streamId, v.extension), v.name, p.name, isLive = false), resume)
    }

    private suspend fun openEpisode(profileId: String, seriesId: Int, episodeId: String) {
        val p = xtream.profile(profileId) ?: return
        val e = xtream.episode(profileId, episodeId) ?: return
        _list.value = emptyList(); _currentKey.value = null
        val resume = e.positionMs.takeIf { e.durationMs == 0L || it < e.durationMs - FINISHED_MS } ?: 0
        _now.value = NowPlaying("S${e.season} · E${e.episodeNum} ${e.title}".trim(), p.name, e.icon, false, null, false, resume)
        manager.play(StreamSource(xtream.episodeUrl(p, episodeId, e.extension), e.title, p.name, isLive = false), resume)
    }

    /** Zap to the item at [delta] from the current one (live lists only). */
    fun zap(delta: Int) {
        val list = _list.value; val cur = _currentKey.value ?: return
        val i = list.indexOfFirst { it.key == cur }
        if (i < 0 || list.isEmpty()) return
        openKey(list[(i + delta + list.size) % list.size].key)
    }

    fun openKey(key: String) {
        val req = request ?: return
        when {
            key.startsWith("c:") && req is PlayRequest.Channel -> open(PlayRequest.Channel(req.playlistId, key.drop(2).toLong()))
            key.startsWith("x:") && req is PlayRequest.XtreamLive -> open(PlayRequest.XtreamLive(req.profileId, key.drop(2).toInt()))
        }
    }

    fun toggleFavorite() {
        val n = _now.value ?: return; val key = n.favoriteKey ?: return; val req = request
        viewModelScope.launch {
            when {
                key.startsWith("c:") -> playlists.toggleChannelFavorite(key.drop(2).toLong())
                key.startsWith("x:") && req is PlayRequest.XtreamLive -> xtream.toggleLiveFavorite(req.profileId, key.drop(2).toInt())
                key.startsWith("m:") && req is PlayRequest.XtreamMovie -> xtream.toggleVodFavorite(req.profileId, key.drop(2).toInt())
            }
            _now.value = n.copy(isFavorite = !n.isFavorite)
            _list.value = _list.value.map { if (it.key == key) it.copy(isFavorite = !n.isFavorite) else it }
        }
    }

    /** Saves VOD position every 10 s and on exit (under 4 s left = finished, like the original). */
    private fun startProgressSaver() {
        progressJob?.cancel()
        if (_now.value?.isLive != false) return
        progressJob = viewModelScope.launch { while (isActive) { delay(10_000); saveProgress() } }
    }

    fun saveProgress() {
        val req = request ?: return
        val p = manager.player
        val dur = p.duration.takeIf { it > 0 } ?: return
        val pos = p.currentPosition.let { if (dur - it < FINISHED_MS) 0 else it }
        // Not viewModelScope: this also runs from onStop()/onCleared() while the screen is closing,
        // when viewModelScope is (being) cancelled and the write would be dropped.
        saveScope.launch {
            when (req) {
                is PlayRequest.XtreamMovie -> xtream.saveVodProgress(req.profileId, req.streamId, pos, dur)
                is PlayRequest.XtreamEpisode -> xtream.saveEpisodeProgress(req.profileId, req.episodeId, req.seriesId, pos, dur)
                else -> Unit
            }
        }
    }

    fun logoColor(name: String) = LogoUtil.color(name)

    override fun onCleared() { saveProgress(); super.onCleared() }

    companion object {
        private const val FINISHED_MS = 4_000L
        private val saveScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    }
}
