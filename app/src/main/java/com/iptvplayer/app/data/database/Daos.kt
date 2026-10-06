package com.iptvplayer.app.data.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class GroupCount(val groupName: String, val count: Int)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlist ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PlaylistEntity>>

    /** Used when the passcode is turned off: nothing can stay locked without it. */
    @Query("UPDATE playlist SET isLocked = 0 WHERE isLocked = 1")
    suspend fun unlockAll()

    @Query("SELECT * FROM playlist WHERE id = :id")
    fun observe(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun get(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlist WHERE autoUpdate = 1 AND sourceType = 'URL'")
    suspend fun autoUpdatable(): List<PlaylistEntity>

    @Query("SELECT COUNT(*) FROM playlist")
    suspend fun count(): Int

    @Insert
    suspend fun insert(p: PlaylistEntity): Long

    @Update
    suspend fun update(p: PlaylistEntity)

    @Query("UPDATE playlist SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("UPDATE playlist SET channelCount = :channels, groupCount = :groups, lastSync = :at WHERE id = :id")
    suspend fun setCounts(id: Long, channels: Int, groups: Int, at: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlist WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ChannelDao {
    @Insert
    suspend fun insertAll(list: List<ChannelEntity>)

    @Query("DELETE FROM channel WHERE playlistId = :playlistId")
    suspend fun deleteForPlaylist(playlistId: Long)

    @Query("SELECT * FROM channel WHERE id = :id")
    suspend fun get(id: Long): ChannelEntity?

    @Query("SELECT * FROM channel WHERE id = :id")
    fun observe(id: Long): Flow<ChannelEntity?>

    @Query(
        """SELECT groupName, COUNT(*) AS count FROM channel WHERE playlistId = :playlistId
           AND groupName LIKE '%' || :q || '%' GROUP BY groupName ORDER BY groupName COLLATE NOCASE"""
    )
    fun groups(playlistId: Long, q: String): Flow<List<GroupCount>>

    @Query("SELECT COUNT(*) FROM channel WHERE playlistId = :playlistId")
    fun count(playlistId: Long): Flow<Int>

    /** Same filter as [paged], for the "N channels" label. */
    @Query("SELECT COUNT(*) FROM channel WHERE playlistId = :playlistId AND (:group IS NULL OR groupName = :group) AND name LIKE '%' || :q || '%'")
    fun countFiltered(playlistId: Long, group: String?, q: String): Flow<Int>

    @Query(
        """SELECT * FROM channel WHERE playlistId = :playlistId AND (:group IS NULL OR groupName = :group)
           AND name LIKE '%' || :q || '%' ORDER BY
           CASE WHEN :sort = 'order_asc' THEN sortIndex END ASC,
           CASE WHEN :sort = 'order_desc' THEN sortIndex END DESC,
           CASE WHEN :sort = 'az' THEN name END COLLATE NOCASE ASC,
           CASE WHEN :sort = 'za' THEN name END COLLATE NOCASE DESC"""
    )
    fun paged(playlistId: Long, group: String?, q: String, sort: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channel WHERE playlistId = :playlistId ORDER BY sortIndex")
    suspend fun allInPlaylist(playlistId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channel WHERE name LIKE '%' || :q || '%' AND playlistId NOT IN (SELECT id FROM playlist WHERE isLocked = 1) ORDER BY lastPlayed DESC, name COLLATE NOCASE LIMIT 200")
    fun search(q: String): Flow<List<ChannelEntity>> // cross-playlist lists skip locked playlists (no lock bypass)

    @Query("SELECT * FROM channel WHERE isFavorite = 1 AND playlistId NOT IN (SELECT id FROM playlist WHERE isLocked = 1) ORDER BY name COLLATE NOCASE")
    fun favorites(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channel WHERE lastPlayed > 0 AND playlistId NOT IN (SELECT id FROM playlist WHERE isLocked = 1) ORDER BY lastPlayed DESC LIMIT :limit")
    fun recent(limit: Int = 20): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channel WHERE (groupName LIKE '%sport%' OR name LIKE '%sport%' OR name LIKE '%' || :q || '%') AND playlistId NOT IN (SELECT id FROM playlist WHERE isLocked = 1) ORDER BY name LIMIT 60")
    fun sportsLike(q: String): Flow<List<ChannelEntity>>

    @Query("UPDATE channel SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("UPDATE channel SET lastPlayed = :at WHERE id = :id")
    suspend fun markPlayed(id: Long, at: Long = System.currentTimeMillis())

    @Query("UPDATE channel SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE channel SET isLocked = NOT isLocked WHERE id = :id")
    suspend fun toggleLock(id: Long)

    @Query("UPDATE channel SET isLocked = 0 WHERE isLocked = 1")
    suspend fun unlockAll()

    @Query("DELETE FROM channel WHERE id = :id")
    suspend fun delete(id: Long)

    /** Favourites survive a playlist refresh by matching on (name, url). */
    @Query("SELECT name || '|' || url FROM channel WHERE playlistId = :playlistId AND isFavorite = 1")
    suspend fun favoriteKeys(playlistId: Long): List<String>

    @Query("UPDATE channel SET isFavorite = 1 WHERE playlistId = :playlistId AND (name || '|' || url) IN (:keys)")
    suspend fun restoreFavorites(playlistId: Long, keys: List<String>)
}

@Dao
interface SingleStreamDao {
    @Query("SELECT * FROM single_stream ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SingleStreamEntity>>

    @Query("SELECT * FROM single_stream WHERE id = :id")
    suspend fun get(id: Long): SingleStreamEntity?

    @Insert
    suspend fun insert(s: SingleStreamEntity): Long

    @Query("DELETE FROM single_stream WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface XtreamDao {
    // ---- profiles ----
    @Query("SELECT * FROM xtream_profile ORDER BY createdAt ASC")
    fun observeProfiles(): Flow<List<XtreamProfileEntity>>

    @Query("SELECT * FROM xtream_profile WHERE id = :id")
    fun observeProfile(id: String): Flow<XtreamProfileEntity?>

    @Query("SELECT * FROM xtream_profile WHERE id = :id")
    suspend fun profile(id: String): XtreamProfileEntity?

    @Query("SELECT COUNT(*) FROM xtream_profile")
    suspend fun profileCount(): Int

    @Upsert
    suspend fun upsertProfile(p: XtreamProfileEntity)

    @Query("UPDATE xtream_profile SET isNew = 0 WHERE id = :id")
    suspend fun clearNew(id: String)

    @Query("UPDATE xtream_profile SET passcodeLocked = :locked WHERE id = :id")
    suspend fun setLocked(id: String, locked: Boolean)

    @Query("UPDATE xtream_profile SET passcodeLocked = 0 WHERE passcodeLocked = 1")
    suspend fun unlockAll()

    @Transaction
    suspend fun deleteProfile(id: String) {
        deleteProfileRow(id); clearCategories(id); clearLive(id); clearVod(id); clearSeries(id); clearEpisodes(id)
    }

    @Query("DELETE FROM xtream_profile WHERE id = :id") suspend fun deleteProfileRow(id: String)

    // ---- categories ----
    @Query("DELETE FROM xtream_category WHERE profileId = :id") suspend fun clearCategories(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(list: List<XtreamCategoryEntity>)
    @Query("SELECT * FROM xtream_category WHERE profileId = :id AND type = :type ORDER BY sortIndex")
    fun categories(id: String, type: MediaType): Flow<List<XtreamCategoryEntity>>

    // ---- live ----
    @Query("DELETE FROM xtream_live WHERE profileId = :id") suspend fun clearLive(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLive(list: List<XtreamLiveEntity>)
    @Query(
        """SELECT * FROM xtream_live WHERE profileId = :id AND (:cat IS NULL OR categoryId = :cat)
           AND name LIKE '%' || :q || '%' ORDER BY sortIndex"""
    )
    fun livePaged(id: String, cat: String?, q: String): PagingSource<Int, XtreamLiveEntity>
    @Query("SELECT * FROM xtream_live WHERE profileId = :id AND isFavorite = 1 ORDER BY name")
    fun liveFavorites(id: String): Flow<List<XtreamLiveEntity>>
    @Query("UPDATE xtream_live SET isFavorite = NOT isFavorite WHERE profileId = :id AND streamId = :streamId")
    suspend fun toggleLiveFavorite(id: String, streamId: Int)
    @Query("UPDATE xtream_live SET lastPlayed = :at WHERE profileId = :id AND streamId = :streamId")
    suspend fun markLivePlayed(id: String, streamId: Int, at: Long = System.currentTimeMillis())
    @Query("SELECT * FROM xtream_live WHERE profileId = :id AND streamId = :streamId")
    suspend fun live(id: String, streamId: Int): XtreamLiveEntity?
    @Query("SELECT * FROM xtream_live WHERE profileId = :id ORDER BY sortIndex")
    suspend fun allLive(id: String): List<XtreamLiveEntity>
    @Query("SELECT * FROM xtream_live WHERE profileId = :id AND name LIKE '%' || :q || '%' ORDER BY sortIndex LIMIT 30")
    fun searchLive(id: String, q: String): Flow<List<XtreamLiveEntity>>

    // ---- vod ----
    @Query("DELETE FROM xtream_vod WHERE profileId = :id") suspend fun clearVod(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertVod(list: List<XtreamVodEntity>)
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id ORDER BY added DESC LIMIT :limit")
    fun latestVod(id: String, limit: Int): Flow<List<XtreamVodEntity>>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND icon != '' ORDER BY RANDOM() LIMIT 1")
    suspend fun randomVod(id: String): XtreamVodEntity?
    @Query(
        """SELECT * FROM xtream_vod WHERE profileId = :id AND (:cat IS NULL OR categoryId = :cat)
           AND name LIKE '%' || :q || '%' ORDER BY added DESC"""
    )
    fun vodPaged(id: String, cat: String?, q: String): PagingSource<Int, XtreamVodEntity>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND streamId = :streamId")
    fun observeVod(id: String, streamId: Int): Flow<XtreamVodEntity?>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND streamId = :streamId")
    suspend fun vod(id: String, streamId: Int): XtreamVodEntity?
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND (isFavorite = 1 OR lastPlayed > 0)")
    suspend fun allVod(id: String): List<XtreamVodEntity>
    @Query("SELECT * FROM xtream_series WHERE profileId = :id AND (isFavorite = 1 OR lastPlayed > 0)")
    suspend fun allSeries(id: String): List<XtreamSeriesEntity>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND categoryId = :cat AND streamId != :exclude LIMIT 12")
    fun similarVod(id: String, cat: String, exclude: Int): Flow<List<XtreamVodEntity>>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND categoryId = :cat ORDER BY added DESC LIMIT :limit")
    suspend fun vodInCategory(id: String, cat: String, limit: Int): List<XtreamVodEntity>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND name LIKE '%' || :q || '%' LIMIT 60")
    fun searchVod(id: String, q: String): Flow<List<XtreamVodEntity>>
    @Query("SELECT * FROM xtream_vod WHERE profileId = :id AND isFavorite = 1")
    fun vodFavorites(id: String): Flow<List<XtreamVodEntity>>
    @Query("UPDATE xtream_vod SET isFavorite = NOT isFavorite WHERE profileId = :id AND streamId = :streamId")
    suspend fun toggleVodFavorite(id: String, streamId: Int)
    @Query("UPDATE xtream_vod SET positionMs = :pos, durationMs = :dur, lastPlayed = :at WHERE profileId = :id AND streamId = :streamId")
    suspend fun saveVodProgress(id: String, streamId: Int, pos: Long, dur: Long, at: Long = System.currentTimeMillis())

    // ---- series ----
    @Query("DELETE FROM xtream_series WHERE profileId = :id") suspend fun clearSeries(id: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSeries(list: List<XtreamSeriesEntity>)
    @Query("SELECT * FROM xtream_series WHERE profileId = :id ORDER BY rowid DESC LIMIT :limit")
    fun latestSeries(id: String, limit: Int): Flow<List<XtreamSeriesEntity>>
    @Query("SELECT * FROM xtream_series WHERE profileId = :id AND categoryId = :cat ORDER BY rowid DESC LIMIT :limit")
    suspend fun seriesInCategory(id: String, cat: String, limit: Int): List<XtreamSeriesEntity>
    @Query(
        """SELECT * FROM xtream_series WHERE profileId = :id AND (:cat IS NULL OR categoryId = :cat)
           AND name LIKE '%' || :q || '%' ORDER BY name"""
    )
    fun seriesPaged(id: String, cat: String?, q: String): PagingSource<Int, XtreamSeriesEntity>
    @Query("SELECT * FROM xtream_series WHERE profileId = :id AND seriesId = :seriesId")
    fun observeSeries(id: String, seriesId: Int): Flow<XtreamSeriesEntity?>
    @Query("SELECT * FROM xtream_series WHERE profileId = :id AND name LIKE '%' || :q || '%' LIMIT 60")
    fun searchSeries(id: String, q: String): Flow<List<XtreamSeriesEntity>>
    @Query("SELECT * FROM xtream_series WHERE profileId = :id AND isFavorite = 1")
    fun seriesFavorites(id: String): Flow<List<XtreamSeriesEntity>>
    @Query("UPDATE xtream_series SET isFavorite = NOT isFavorite WHERE profileId = :id AND seriesId = :seriesId")
    suspend fun toggleSeriesFavorite(id: String, seriesId: Int)
    @Query("UPDATE xtream_series SET lastPlayed = :at WHERE profileId = :id AND seriesId = :seriesId")
    suspend fun markSeriesPlayed(id: String, seriesId: Int, at: Long = System.currentTimeMillis())

    // ---- episodes ----
    @Query("DELETE FROM xtream_episode WHERE profileId = :id") suspend fun clearEpisodes(id: String)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertEpisodes(list: List<XtreamEpisodeEntity>)
    @Query("SELECT * FROM xtream_episode WHERE profileId = :id AND seriesId = :seriesId ORDER BY season, episodeNum")
    fun episodes(id: String, seriesId: Int): Flow<List<XtreamEpisodeEntity>>
    @Query("SELECT * FROM xtream_episode WHERE profileId = :id AND episodeId = :episodeId")
    suspend fun episode(id: String, episodeId: String): XtreamEpisodeEntity?
    @Query("UPDATE xtream_episode SET positionMs = :pos, durationMs = :dur, lastPlayed = :at WHERE profileId = :id AND episodeId = :episodeId")
    suspend fun saveEpisodeProgress(id: String, episodeId: String, pos: Long, dur: Long, at: Long = System.currentTimeMillis())

    // ---- continue watching (movies + episodes) ----
    @Query(
        """SELECT 'MOVIE' AS kind, streamId AS refId, '' AS episodeId, name AS title, icon AS image, positionMs, durationMs, lastPlayed, 0 AS season, 0 AS episodeNum
             FROM xtream_vod WHERE profileId = :id AND lastPlayed > 0 AND positionMs > 0
           UNION ALL
           SELECT 'EPISODE' AS kind, e.seriesId AS refId, e.episodeId, s.name AS title, s.cover AS image, e.positionMs, e.durationMs, e.lastPlayed, e.season, e.episodeNum
             FROM xtream_episode e JOIN xtream_series s ON s.profileId = e.profileId AND s.seriesId = e.seriesId
             WHERE e.profileId = :id AND e.lastPlayed > 0 AND e.positionMs > 0
           ORDER BY lastPlayed DESC LIMIT :limit"""
    )
    fun continueWatching(id: String, limit: Int = 30): Flow<List<ContinueItem>>

    @Query("UPDATE xtream_vod SET positionMs = 0, lastPlayed = 0 WHERE profileId = :id")
    suspend fun clearVodHistory(id: String)
    @Query("UPDATE xtream_episode SET positionMs = 0, lastPlayed = 0 WHERE profileId = :id")
    suspend fun clearEpisodeHistory(id: String)
}

data class ContinueItem(
    val kind: String,
    val refId: Int,
    val episodeId: String,
    val title: String,
    val image: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayed: Long,
    val season: Int,
    val episodeNum: Int,
)

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history WHERE scope = :scope ORDER BY at DESC LIMIT 10")
    fun recent(scope: String): Flow<List<SearchHistoryEntity>>

    @Upsert
    suspend fun add(e: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE scope = :scope AND query = :query")
    suspend fun remove(scope: String, query: String)

    @Query("DELETE FROM search_history WHERE scope = :scope")
    suspend fun clear(scope: String)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM favourite_match ORDER BY startTime")
    fun observeAll(): Flow<List<FavouriteMatchEntity>>

    @Upsert
    suspend fun add(m: FavouriteMatchEntity)

    @Query("DELETE FROM favourite_match WHERE eventId = :id")
    suspend fun remove(id: String)
}
