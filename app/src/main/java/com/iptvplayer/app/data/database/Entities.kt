package com.iptvplayer.app.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SourceType { URL, FILE }
enum class MediaType { LIVE, MOVIE, SERIES }

@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val sourceType: SourceType = SourceType.URL,
    val channelCount: Int = 0,
    val groupCount: Int = 0,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    val autoUpdate: Boolean = true,
    val userAgent: String? = null,
    val epgUrl: String? = null,
    val lastSync: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "channel",
    indices = [Index("playlistId", "groupName"), Index("playlistId", "name"), Index("isFavorite"), Index("lastPlayed")],
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val sortIndex: Int,
    val name: String,
    val logo: String = "",
    val url: String,
    val groupName: String = "Unknown",
    val tvgId: String? = null,
    val tvgChno: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    val headersJson: String? = null,
    val drmJson: String? = null,
    val catchup: String? = null,
    val isFavorite: Boolean = false,
    val isLocked: Boolean = false,
    /** 0 = never played; otherwise epoch millis of last play (drives Recent). */
    val lastPlayed: Long = 0,
)

@Entity(tableName = "single_stream")
data class SingleStreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "xtream_profile")
data class XtreamProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val serverUrl: String,
    val username: String,
    val password: String,
    val status: String = "Active",
    /** Epoch seconds, 0 = unlimited/unknown. */
    val expDate: Long = 0,
    val activeCons: Int = 0,
    val maxConnections: Int = 0,
    val allowedOutputFormats: String = "m3u8,ts",
    val timezone: String = "",
    val passcodeLocked: Boolean = false,
    val avatarColor: Int,
    val liveCount: Int = 0,
    val vodCount: Int = 0,
    val seriesCount: Int = 0,
    val lastSync: Long = 0,
    /** Shows the "Mới" tag until the profile is opened for the first time. */
    val isNew: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "xtream_category", primaryKeys = ["profileId", "type", "categoryId"])
data class XtreamCategoryEntity(
    val profileId: String,
    val type: MediaType,
    val categoryId: String,
    val name: String,
    val sortIndex: Int,
)

@Entity(tableName = "xtream_live", primaryKeys = ["profileId", "streamId"], indices = [Index("profileId", "categoryId")])
data class XtreamLiveEntity(
    val profileId: String,
    val streamId: Int,
    val name: String,
    val icon: String,
    val categoryId: String,
    val epgChannelId: String? = null,
    val sortIndex: Int,
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0,
)

@Entity(tableName = "xtream_vod", primaryKeys = ["profileId", "streamId"], indices = [Index("profileId", "categoryId")])
data class XtreamVodEntity(
    val profileId: String,
    val streamId: Int,
    val name: String,
    val icon: String,
    val categoryId: String,
    val rating: Double = 0.0,
    val extension: String = "mp4",
    val added: Long = 0,
    val isFavorite: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val lastPlayed: Long = 0,
)

@Entity(tableName = "xtream_series", primaryKeys = ["profileId", "seriesId"], indices = [Index("profileId", "categoryId")])
data class XtreamSeriesEntity(
    val profileId: String,
    val seriesId: Int,
    val name: String,
    val cover: String,
    val categoryId: String,
    val rating: Double = 0.0,
    val plot: String = "",
    val cast: String = "",
    val director: String = "",
    val genre: String = "",
    val releaseDate: String = "",
    val youtubeTrailer: String = "",
    val isFavorite: Boolean = false,
    val lastPlayed: Long = 0,
)

@Entity(tableName = "xtream_episode", primaryKeys = ["profileId", "episodeId"], indices = [Index("profileId", "seriesId")])
data class XtreamEpisodeEntity(
    val profileId: String,
    val episodeId: String,
    val seriesId: Int,
    val season: Int,
    val episodeNum: Int,
    val title: String,
    val extension: String,
    val icon: String = "",
    val durationSecs: Int = 0,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val lastPlayed: Long = 0,
)

@Entity(tableName = "search_history", primaryKeys = ["scope", "query"])
data class SearchHistoryEntity(
    /** "channels" for global channel search, or an Xtream profile id. */
    val scope: String,
    val query: String,
    val at: Long = System.currentTimeMillis(),
)

@Entity(tableName = "favourite_match")
data class FavouriteMatchEntity(
    @PrimaryKey val eventId: String,
    val leagueSlug: String,
    val sportSlug: String,
    val home: String,
    val away: String,
    val startTime: Long,
)
