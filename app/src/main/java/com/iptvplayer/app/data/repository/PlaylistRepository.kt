package com.iptvplayer.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.iptvplayer.app.data.database.AppDatabase
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.database.PlaylistEntity
import com.iptvplayer.app.data.database.SingleStreamEntity
import com.iptvplayer.app.data.database.SourceType
import com.iptvplayer.app.data.network.HttpClients
import com.iptvplayer.app.data.parser.JsonPlaylistParser
import com.iptvplayer.app.data.parser.M3uParser
import com.iptvplayer.app.data.parser.ParsedChannel
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ImportProgress {
    data class Reading(val channels: Int, val groups: Int, val fraction: Float?) : ImportProgress
    data class Done(val playlistId: Long, val channels: Int, val groups: Int) : ImportProgress
    data class Failed(val reason: ImportError, val detail: String? = null) : ImportProgress
}

/** STREAM: the link is one HLS stream (has #EXT-X- tags), not a channel list — play it as a single stream. */
enum class ImportError { NETWORK, HTTP, EMPTY, INVALID, STREAM }

class ImportException(val reason: ImportError, message: String? = null) : IOException(message)

@Singleton
class PlaylistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val http: HttpClients,
    private val gson: Gson,
) {
    private val playlists = db.playlistDao()
    private val channels = db.channelDao()

    fun observePlaylists() = playlists.observeAll()
    fun observePlaylist(id: Long) = playlists.observe(id)
    suspend fun get(id: Long) = playlists.get(id)
    suspend fun toggleFavorite(id: Long) = playlists.toggleFavorite(id)
    suspend fun update(p: PlaylistEntity) = playlists.update(p)
    suspend fun delete(id: Long) = db.withTransaction { channels.deleteForPlaylist(id); playlists.delete(id) }
    suspend fun nextDefaultName(): String = "Playlist ${playlists.count() + 1}"

    /** Adds a playlist from a URL and imports it. Emits progress; the playlist row is removed again on failure. */
    fun addFromUrl(name: String, url: String, locked: Boolean, autoUpdate: Boolean): Flow<ImportProgress> = flow {
        val id = playlists.insert(PlaylistEntity(name = name, url = url.trim(), sourceType = SourceType.URL, isLocked = locked, autoUpdate = autoUpdate))
        emitAll(id) { openUrl(url.trim()) }
    }.flowOn(Dispatchers.IO)

    fun addFromFile(name: String, uri: Uri, locked: Boolean): Flow<ImportProgress> = flow {
        val id = playlists.insert(PlaylistEntity(name = name, url = uri.toString(), sourceType = SourceType.FILE, isLocked = locked, autoUpdate = false))
        emitAll(id) { (context.contentResolver.openInputStream(uri) ?: throw ImportException(ImportError.INVALID)) to null }
    }.flowOn(Dispatchers.IO)

    /** Re-downloads an URL playlist, keeping favourites by (name, url). */
    fun refresh(id: Long): Flow<ImportProgress> = flow {
        val p = playlists.get(id) ?: return@flow
        val favKeys = channels.favoriteKeys(id)
        emitAll(id, replace = true, favKeys = favKeys) {
            if (p.sourceType == SourceType.FILE) context.contentResolver.openInputStream(Uri.parse(p.url))!! to null else openUrl(p.url)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<ImportProgress>.emitAll(
        playlistId: Long,
        replace: Boolean = false,
        favKeys: List<String> = emptyList(),
        open: suspend () -> Pair<InputStream, Long?>,
    ) {
        val groups = HashSet<String>()
        var count = 0
        try {
            val (raw, length) = open()
            val counting = CountingStream(BufferedInputStream(maybeGunzip(raw)))
            if (replace) channels.deleteForPlaylist(playlistId)
            val batch = ArrayList<ChannelEntity>(BATCH)
            var lastEmit = 0L
            suspend fun flush() { if (batch.isNotEmpty()) { channels.insertAll(batch.toList()); batch.clear() } }
            val onChannel: (ParsedChannel) -> Unit = { c ->
                groups += c.group
                batch += c.toEntity(playlistId, count++)
            }
            counting.use { stream ->
                stream.mark(4096)
                val head = ByteArray(4096).let { b -> String(b, 0, stream.read(b).coerceAtLeast(0), Charsets.UTF_8) }
                stream.reset()
                val first = head.firstOrNull { !it.isWhitespace() }
                // An HLS media/master playlist describes ONE stream (variants/segments), not channels.
                if (Regex("""^#EXT-X-(STREAM-INF|TARGETDURATION|MEDIA-SEQUENCE|VERSION)""", RegexOption.MULTILINE).containsMatchIn(head) &&
                    !head.contains("#EXTINF:-1") && !head.contains("tvg-")) throw ImportException(ImportError.STREAM)
                val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                if (JsonPlaylistParser.looksLikeJson(first)) {
                    JsonPlaylistParser.parse(reader, onChannel)
                    flush()
                } else {
                    // Pull entries one by one; flush in chunks so memory stays flat for 100k-line playlists.
                    val parser = M3uParser.Stream(reader)
                    while (true) {
                        val c = parser.next() ?: break
                        currentCoroutineContext().ensureActive()
                        onChannel(c)
                        if (batch.size >= BATCH) {
                            flush()
                            val now = System.currentTimeMillis()
                            if (now - lastEmit > 120) {
                                lastEmit = now
                                emit(ImportProgress.Reading(count, groups.size, length?.let { (counting.count.toFloat() / it).coerceIn(0f, 1f) }))
                            }
                        }
                    }
                    flush()
                }
            }
            if (count == 0) throw ImportException(ImportError.EMPTY)
            if (favKeys.isNotEmpty()) favKeys.chunked(500).forEach { channels.restoreFavorites(playlistId, it) }
            playlists.setCounts(playlistId, count, groups.size)
            emit(ImportProgress.Reading(count, groups.size, 1f))
            emit(ImportProgress.Done(playlistId, count, groups.size))
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (!replace) delete(playlistId)
            throw e
        } catch (e: ImportException) {
            if (!replace) delete(playlistId)
            emit(ImportProgress.Failed(e.reason, e.message))
        } catch (e: IOException) {
            if (!replace) delete(playlistId)
            emit(ImportProgress.Failed(ImportError.NETWORK, e.message))
        } catch (e: Exception) {
            if (!replace) delete(playlistId)
            emit(ImportProgress.Failed(ImportError.INVALID, e.message))
        }
    }

    private fun openUrl(url: String): Pair<InputStream, Long?> {
        val fixed = if (url.contains("://")) url else "http://$url"
        val resp = http.ok.newCall(Request.Builder().url(fixed).build()).execute()
        if (!resp.isSuccessful) { resp.close(); throw ImportException(ImportError.HTTP, "HTTP ${resp.code}") }
        val body = resp.body ?: throw ImportException(ImportError.EMPTY)
        return body.byteStream() to body.contentLength().takeIf { it > 0 }
    }

    private fun maybeGunzip(input: InputStream): InputStream {
        val b = BufferedInputStream(input)
        b.mark(2)
        val m1 = b.read(); val m2 = b.read()
        b.reset()
        return if (m1 == 0x1f && m2 == 0x8b) GZIPInputStream(b) else b
    }

    private fun ParsedChannel.toEntity(playlistId: Long, index: Int) = ChannelEntity(
        playlistId = playlistId, sortIndex = index, name = name, logo = logo, url = url, groupName = group,
        tvgId = tvgId, tvgChno = tvgChno, userAgent = userAgent, referrer = referrer,
        headersJson = headers.takeIf { it.isNotEmpty() }?.let { gson.toJson(it) },
        drmJson = drmType?.let { gson.toJson(mapOf("type" to it, "key" to drmKey)) },
        catchup = catchup,
    )

    // ---- channels ----
    fun groups(playlistId: Long, q: String) = channels.groups(playlistId, q)
    fun channelCount(playlistId: Long) = channels.count(playlistId)
    fun countChannels(playlistId: Long, group: String?, q: String) = channels.countFiltered(playlistId, group, q)
    fun pagedChannels(playlistId: Long, group: String?, q: String, sort: String) = channels.paged(playlistId, group, q, sort)
    suspend fun channelsIn(playlistId: Long) = channels.allInPlaylist(playlistId)
    suspend fun channel(id: Long) = channels.get(id)
    fun observeChannel(id: Long) = channels.observe(id)
    fun search(q: String) = channels.search(q)
    fun favorites() = channels.favorites()
    fun recent(limit: Int = 20) = channels.recent(limit)
    fun sportsLike(q: String) = channels.sportsLike(q)
    suspend fun toggleChannelFavorite(id: Long) = channels.toggleFavorite(id)
    suspend fun markPlayed(id: Long) = channels.markPlayed(id)
    suspend fun renameChannel(id: Long, name: String) = channels.rename(id, name)
    suspend fun toggleChannelLock(id: Long) = channels.toggleLock(id)
    suspend fun deleteChannel(id: Long) = channels.delete(id)
    suspend fun autoUpdatable() = playlists.autoUpdatable()

    // ---- single streams ----
    fun singles() = db.singleStreamDao().observeAll()
    suspend fun single(id: Long) = db.singleStreamDao().get(id)
    suspend fun addSingle(name: String, url: String) = db.singleStreamDao().insert(SingleStreamEntity(name = name, url = url))
    suspend fun deleteSingle(id: Long) = db.singleStreamDao().delete(id)

    private class CountingStream(private val inner: InputStream) : InputStream() {
        var count = 0L; private set
        private var markCount = 0L
        override fun read(): Int = inner.read().also { if (it >= 0) count++ }
        override fun read(b: ByteArray, off: Int, len: Int): Int = inner.read(b, off, len).also { if (it > 0) count += it }
        override fun markSupported() = inner.markSupported()
        override fun mark(readlimit: Int) { markCount = count; inner.mark(readlimit) }
        override fun reset() { inner.reset(); count = markCount }
        override fun close() = inner.close()
    }

    companion object { private const val BATCH = 1000 }
}
