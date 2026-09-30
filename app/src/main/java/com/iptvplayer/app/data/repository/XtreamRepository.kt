package com.iptvplayer.app.data.repository

import com.iptvplayer.app.data.database.AppDatabase
import com.iptvplayer.app.data.database.MediaType
import com.iptvplayer.app.data.database.XtreamCategoryEntity
import com.iptvplayer.app.data.database.XtreamEpisodeEntity
import com.iptvplayer.app.data.database.XtreamLiveEntity
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.data.database.XtreamSeriesEntity
import com.iptvplayer.app.data.database.XtreamVodEntity
import com.iptvplayer.app.data.network.HttpClients
import com.iptvplayer.app.data.network.dto.VodInfoResponse
import com.iptvplayer.app.util.UrlUtils
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.UnknownHostException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStep { AUTH, LIVE, VOD, SERIES, SAVE }
enum class StepState { WAITING, RUNNING, DONE }
enum class SyncError { HOST, AUTH, EXPIRED, NETWORK, EMPTY }

sealed interface SyncProgress {
    /** [saved]/[total]: rows written to the local DB during [SyncStep.SAVE] (big panels: 100k+ rows, several seconds). */
    data class Step(val states: Map<SyncStep, StepState>, val counts: Map<SyncStep, Int>, val saved: Int = 0, val total: Int = 0) : SyncProgress
    data class Done(val profile: XtreamProfileEntity) : SyncProgress
    data class Failed(val error: SyncError, val host: String) : SyncProgress
}

data class NewProfileInput(val name: String, val server: String, val username: String, val password: String, val locked: Boolean, val color: Int? = null)

@Singleton
class XtreamRepository @Inject constructor(private val db: AppDatabase, private val http: HttpClients) {
    private val dao = db.xtreamDao()

    fun observeProfiles() = dao.observeProfiles()
    fun observeProfile(id: String) = dao.observeProfile(id)
    suspend fun profile(id: String) = dao.profile(id)
    suspend fun clearNew(id: String) = dao.clearNew(id)
    suspend fun setLocked(id: String, locked: Boolean) = dao.setLocked(id, locked)
    suspend fun deleteProfile(id: String) = dao.deleteProfile(id)
    suspend fun nextDefaultName() = "My Xtream ${dao.profileCount() + 1}"
    suspend fun updateProfileInfo(p: XtreamProfileEntity) = dao.upsertProfile(p)

    /**
     * Logs in and downloads categories + live + VOD + series.
     * For a new profile the row is only written on success, so a failed add leaves nothing behind.
     * Success needs at least one of live/VOD/series (the original required all three).
     */
    fun sync(existingId: String?, input: NewProfileInput?): Flow<SyncProgress> = channelFlow {
        val existing = existingId?.let { dao.profile(it) }
        val server = UrlUtils.normalizeServer(existing?.serverUrl ?: input!!.server)
        val user = existing?.username ?: input!!.username
        val pass = existing?.password ?: input!!.password
        val host = UrlUtils.host(server)
        val api = http.xtream(server)
        val states = linkedMapOf(SyncStep.AUTH to StepState.RUNNING, SyncStep.LIVE to StepState.WAITING, SyncStep.VOD to StepState.WAITING,
            SyncStep.SERIES to StepState.WAITING, SyncStep.SAVE to StepState.WAITING)
        val counts = HashMap<SyncStep, Int>()
        suspend fun push(saved: Int = 0, total: Int = 0) = send(SyncProgress.Step(HashMap(states), HashMap(counts), saved, total))
        push()

        val auth = try { api.auth(user, pass) } catch (e: Exception) { send(SyncProgress.Failed(e.toSyncError(), host)); return@channelFlow }
        val info = auth.userInfo
        if (info == null || info.auth == 0) { send(SyncProgress.Failed(SyncError.AUTH, host)); return@channelFlow }
        val expSec = info.expDate?.toLongOrNull() ?: 0L
        val expired = info.status.equals("Expired", true) || (expSec > 0 && expSec * 1000 < System.currentTimeMillis())
        if (expired && existing != null) {
            dao.upsertProfile(existing.copy(status = "Expired", expDate = expSec))
            send(SyncProgress.Failed(SyncError.EXPIRED, host)); return@channelFlow
        }
        states[SyncStep.AUTH] = StepState.DONE
        states[SyncStep.LIVE] = StepState.RUNNING; states[SyncStep.VOD] = StepState.RUNNING; states[SyncStep.SERIES] = StepState.RUNNING
        push()

        val id = existing?.id ?: UUID.randomUUID().toString()
        val result = coroutineScope {
            val liveCats = async { runCatching { api.categories(user, pass, "get_live_categories") }.getOrDefault(emptyList()) }
            val vodCats = async { runCatching { api.categories(user, pass, "get_vod_categories") }.getOrDefault(emptyList()) }
            val seriesCats = async { runCatching { api.categories(user, pass, "get_series_categories") }.getOrDefault(emptyList()) }
            val live = async { runCatching { api.live(user, pass) }.getOrDefault(emptyList()).also { counts[SyncStep.LIVE] = it.size; states[SyncStep.LIVE] = StepState.DONE; push() } }
            val vod = async { runCatching { api.vod(user, pass) }.getOrDefault(emptyList()).also { counts[SyncStep.VOD] = it.size; states[SyncStep.VOD] = StepState.DONE; push() } }
            val series = async { runCatching { api.series(user, pass) }.getOrDefault(emptyList()).also { counts[SyncStep.SERIES] = it.size; states[SyncStep.SERIES] = StepState.DONE; push() } }
            Synced(liveCats.await(), vodCats.await(), seriesCats.await(), live.await(), vod.await(), series.await())
        }
        if (result.live.isEmpty() && result.vod.isEmpty() && result.series.isEmpty()) { send(SyncProgress.Failed(SyncError.EMPTY, host)); return@channelFlow }

        val profile = (existing ?: XtreamProfileEntity(
            id = id, name = input!!.name.ifBlank { nextDefaultName() }, serverUrl = server, username = user, password = pass,
            passcodeLocked = input.locked, avatarColor = input.color ?: AVATAR_COLORS[dao.profileCount() % AVATAR_COLORS.size], isNew = true,
        )).copy(
            status = info.status ?: "Active", expDate = expSec,
            activeCons = info.activeCons?.toIntOrNull() ?: 0, maxConnections = info.maxConnections?.toIntOrNull() ?: 0,
            allowedOutputFormats = info.allowedOutputFormats?.joinToString(",") ?: "m3u8,ts",
            timezone = auth.serverInfo?.timezone.orEmpty(),
            liveCount = result.live.size, vodCount = result.vod.size, seriesCount = result.series.size,
            lastSync = System.currentTimeMillis(),
        )
        states[SyncStep.SAVE] = StepState.RUNNING
        val total = result.live.size + result.vod.size + result.series.size
        push(0, total)
        // Saving must finish even if the dialog is closed ("Run in background"): NonCancellable keeps the
        // transaction from being rolled back half-way, and the profile appears in the list when it commits.
        withContext(Dispatchers.IO + NonCancellable) {
            var lastPush = 0L
            store(profile, result) { saved ->
                val now = System.currentTimeMillis()
                if (now - lastPush > 120 || saved == total) { lastPush = now; trySend(SyncProgress.Step(HashMap(states), HashMap(counts), saved, total)) }
            }
        }
        // A screen that (re)subscribed while this long transaction held the DB can miss its invalidation;
        // a small write afterwards goes through the normal path and refreshes every observer.
        dao.upsertProfile(profile)
        states[SyncStep.SAVE] = StepState.DONE
        send(SyncProgress.Done(profile))
    }.flowOn(Dispatchers.IO)

    private class Synced(
        val liveCats: List<com.iptvplayer.app.data.network.dto.CategoryDto>,
        val vodCats: List<com.iptvplayer.app.data.network.dto.CategoryDto>,
        val seriesCats: List<com.iptvplayer.app.data.network.dto.CategoryDto>,
        val live: List<com.iptvplayer.app.data.network.dto.LiveStreamDto>,
        val vod: List<com.iptvplayer.app.data.network.dto.VodStreamDto>,
        val series: List<com.iptvplayer.app.data.network.dto.SeriesDto>,
    )

    private suspend fun store(p: XtreamProfileEntity, r: Synced, onSaved: (Int) -> Unit) {
        val id = p.id
        // Keep favourites / progress across a re-sync: snapshot them before replacing the rows.
        val favLive = dao.allLive(id).filter { it.isFavorite }.map { it.streamId }.toSet()
        val oldVod = dao.allVod(id).associateBy { it.streamId }
        val oldSeries = dao.allSeries(id).associateBy { it.seriesId }
        var saved = 0
        fun progress(n: Int) { saved += n; onSaved(saved) }
        db.withTransaction {
            dao.clearCategories(id); dao.clearLive(id); dao.clearVod(id); dao.clearSeries(id)
            fun cats(list: List<com.iptvplayer.app.data.network.dto.CategoryDto>, t: MediaType) =
                list.mapIndexedNotNull { i, c -> c.categoryId?.let { XtreamCategoryEntity(id, t, it, c.categoryName ?: it, i) } }
            dao.insertCategories(cats(r.liveCats, MediaType.LIVE) + cats(r.vodCats, MediaType.MOVIE) + cats(r.seriesCats, MediaType.SERIES))
            r.live.mapIndexedNotNull { i, s ->
                s.streamId?.let { XtreamLiveEntity(id, it, s.name.orEmpty(), s.streamIcon.orEmpty(), s.categoryId.orEmpty(), s.epgChannelId, i, it in favLive) }
            }.chunked(2000).forEach { dao.insertLive(it); progress(it.size) }
            r.vod.mapNotNull { v ->
                val sid = v.streamId ?: return@mapNotNull null
                val old = oldVod[sid]
                XtreamVodEntity(id, sid, v.name.orEmpty(), v.streamIcon.orEmpty(), v.categoryId.orEmpty(), v.rating5 ?: 0.0,
                    v.extension ?: "mp4", v.added?.toLongOrNull() ?: 0, old?.isFavorite ?: false, old?.positionMs ?: 0, old?.durationMs ?: 0, old?.lastPlayed ?: 0)
            }.chunked(2000).forEach { dao.insertVod(it); progress(it.size) }
            r.series.mapNotNull { s ->
                val sid = s.seriesId ?: return@mapNotNull null
                val old = oldSeries[sid]
                XtreamSeriesEntity(id, sid, s.name.orEmpty(), s.cover.orEmpty(), s.categoryId.orEmpty(), s.rating5 ?: 0.0,
                    s.plot.orEmpty(), s.cast.orEmpty(), s.director.orEmpty(), s.genre.orEmpty(), s.releaseDate.orEmpty(), s.youtubeTrailer.orEmpty(),
                    old?.isFavorite ?: false, old?.lastPlayed ?: 0)
            }.chunked(2000).forEach { dao.insertSeries(it); progress(it.size) }
            dao.upsertProfile(p)
        }
    }

    // ---- browsing ----
    fun categories(id: String, type: MediaType) = dao.categories(id, type)
    fun livePaged(id: String, cat: String?, q: String) = dao.livePaged(id, cat, q)
    fun vodPaged(id: String, cat: String?, q: String) = dao.vodPaged(id, cat, q)
    fun seriesPaged(id: String, cat: String?, q: String) = dao.seriesPaged(id, cat, q)
    fun latestVod(id: String, limit: Int = 20) = dao.latestVod(id, limit)
    fun latestSeries(id: String, limit: Int = 20) = dao.latestSeries(id, limit)
    suspend fun randomVod(id: String) = dao.randomVod(id)
    fun observeVod(id: String, streamId: Int) = dao.observeVod(id, streamId)
    fun observeSeries(id: String, seriesId: Int) = dao.observeSeries(id, seriesId)
    fun similarVod(id: String, cat: String, exclude: Int) = dao.similarVod(id, cat, exclude)
    fun searchVod(id: String, q: String) = dao.searchVod(id, q)
    fun searchLive(id: String, q: String) = dao.searchLive(id, q)
    fun searchSeries(id: String, q: String) = dao.searchSeries(id, q)
    fun vodFavorites(id: String) = dao.vodFavorites(id)
    fun seriesFavorites(id: String) = dao.seriesFavorites(id)
    fun liveFavorites(id: String) = dao.liveFavorites(id)
    fun episodes(id: String, seriesId: Int) = dao.episodes(id, seriesId)
    fun continueWatching(id: String, limit: Int = 30) = dao.continueWatching(id, limit)
    suspend fun live(id: String, streamId: Int) = dao.live(id, streamId)
    suspend fun allLive(id: String) = dao.allLive(id)
    suspend fun episode(id: String, episodeId: String) = dao.episode(id, episodeId)
    suspend fun vod(id: String, streamId: Int) = dao.vod(id, streamId)
    suspend fun toggleLiveFavorite(id: String, s: Int) = dao.toggleLiveFavorite(id, s)
    suspend fun toggleVodFavorite(id: String, s: Int) = dao.toggleVodFavorite(id, s)
    suspend fun toggleSeriesFavorite(id: String, s: Int) = dao.toggleSeriesFavorite(id, s)
    suspend fun markLivePlayed(id: String, s: Int) = dao.markLivePlayed(id, s)
    suspend fun saveVodProgress(id: String, s: Int, pos: Long, dur: Long) = dao.saveVodProgress(id, s, pos, dur)
    suspend fun saveEpisodeProgress(id: String, e: String, seriesId: Int, pos: Long, dur: Long) {
        dao.saveEpisodeProgress(id, e, pos, dur); dao.markSeriesPlayed(id, seriesId)
    }
    suspend fun clearHistory(id: String) { dao.clearVodHistory(id); dao.clearEpisodeHistory(id) }

    suspend fun vodInfo(p: XtreamProfileEntity, streamId: Int): VodInfoResponse? =
        runCatching { http.xtream(p.serverUrl).vodInfo(p.username, p.password, streamId) }.getOrNull()

    /** Fetches seasons/episodes on demand and caches them (INSERT OR IGNORE keeps watch progress). */
    suspend fun loadEpisodes(p: XtreamProfileEntity, seriesId: Int): Boolean = runCatching {
        val info = http.xtream(p.serverUrl).seriesInfo(p.username, p.password, seriesId)
        val list = info.episodes.orEmpty().flatMap { (seasonKey, eps) ->
            eps.mapNotNull { e ->
                val eid = e.id ?: return@mapNotNull null
                XtreamEpisodeEntity(p.id, eid, seriesId, e.season ?: seasonKey.toIntOrNull() ?: 1, e.episodeNum ?: 0,
                    e.title.orEmpty(), e.extension ?: "mp4", e.info?.image.orEmpty(), e.info?.durationSecs ?: 0)
            }
        }
        dao.insertEpisodes(list)
        true
    }.getOrDefault(false)

    // ---- stream URLs ----
    fun liveUrl(p: XtreamProfileEntity, streamId: Int): String {
        val ext = if (p.allowedOutputFormats.split(',').contains("m3u8")) "m3u8" else "ts"
        return "${p.serverUrl.trimEnd('/')}/live/${p.username}/${p.password}/$streamId.$ext"
    }
    fun movieUrl(p: XtreamProfileEntity, streamId: Int, ext: String) = "${p.serverUrl.trimEnd('/')}/movie/${p.username}/${p.password}/$streamId.$ext"
    fun episodeUrl(p: XtreamProfileEntity, episodeId: String, ext: String) = "${p.serverUrl.trimEnd('/')}/series/${p.username}/${p.password}/$episodeId.$ext"

    private fun Exception.toSyncError() = when (this) {
        is UnknownHostException -> SyncError.HOST
        is HttpException -> if (code() == 401 || code() == 403) SyncError.AUTH else SyncError.NETWORK
        is IOException -> if (cause is UnknownHostException) SyncError.HOST else SyncError.NETWORK
        else -> SyncError.NETWORK
    }

    companion object {
        val AVATAR_COLORS = intArrayOf(0xFF5B5BD6.toInt(), 0xFF1F8A6B.toInt(), 0xFFC7861B.toInt(), 0xFF157A9A.toInt(), 0xFFA1336B.toInt(), 0xFFB83A2B.toInt(), 0xFF4A6B1F.toInt())
    }
}
