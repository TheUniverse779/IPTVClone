package com.iptvplayer.app.data.network.dto

import com.google.gson.annotations.SerializedName

// ---------- Xtream player_api.php ----------

data class XtreamAuthResponse(
    @SerializedName("user_info") val userInfo: UserInfo?,
    @SerializedName("server_info") val serverInfo: ServerInfo?,
)

data class UserInfo(
    @SerializedName("auth") val auth: Int?,
    @SerializedName("status") val status: String?,
    @SerializedName("exp_date") val expDate: String?,
    @SerializedName("active_cons") val activeCons: String?,
    @SerializedName("max_connections") val maxConnections: String?,
    @SerializedName("allowed_output_formats") val allowedOutputFormats: List<String>?,
)

data class ServerInfo(
    @SerializedName("timezone") val timezone: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("port") val port: String?,
)

data class CategoryDto(
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("category_name") val categoryName: String?,
)

data class LiveStreamDto(
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("epg_channel_id") val epgChannelId: String?,
)

data class VodStreamDto(
    @SerializedName("stream_id") val streamId: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_icon") val streamIcon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("rating_5based") val rating5: Double?,
    @SerializedName("container_extension") val extension: String?,
    @SerializedName("added") val added: String?,
)

data class SeriesDto(
    @SerializedName("series_id") val seriesId: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("rating_5based") val rating5: Double?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("youtube_trailer") val youtubeTrailer: String?,
)

data class VodInfoResponse(
    @SerializedName("info") val info: VodInfo?,
    @SerializedName("movie_data") val movieData: MovieData?,
)

data class VodInfo(
    @SerializedName("plot") val plot: String?,
    @SerializedName("cast") val cast: String?,
    @SerializedName("director") val director: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releasedate") val releaseDate: String?,
    @SerializedName("duration") val duration: String?,
    @SerializedName("duration_secs") val durationSecs: Int?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("backdrop_path") val backdropPath: List<String>?,
    @SerializedName("youtube_trailer") val youtubeTrailer: String?,
)

data class MovieData(
    @SerializedName("container_extension") val extension: String?,
    @SerializedName("name") val name: String?,
)

data class SeriesInfoResponse(
    @SerializedName("seasons") val seasons: List<SeasonDto>?,
    @SerializedName("episodes") val episodes: Map<String, List<EpisodeDto>>?,
)

data class SeasonDto(
    @SerializedName("season_number") val seasonNumber: Int?,
    @SerializedName("name") val name: String?,
    @SerializedName("cover") val cover: String?,
)

data class EpisodeDto(
    @SerializedName("id") val id: String?,
    @SerializedName("episode_num") val episodeNum: Int?,
    @SerializedName("title") val title: String?,
    @SerializedName("container_extension") val extension: String?,
    @SerializedName("season") val season: Int?,
    @SerializedName("info") val info: EpisodeInfo?,
)

data class EpisodeInfo(
    @SerializedName("movie_image") val image: String?,
    @SerializedName("duration_secs") val durationSecs: Int?,
)

// ---------- ESPN site API ----------

data class ScoreboardResponse(@SerializedName("events") val events: List<EspnEvent>?)

data class EspnEvent(
    @SerializedName("id") val id: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("competitions") val competitions: List<EspnCompetition>?,
    @SerializedName("status") val status: EspnStatus?,
)

data class EspnCompetition(
    @SerializedName("competitors") val competitors: List<EspnCompetitor>?,
    @SerializedName("venue") val venue: EspnVenue?,
    @SerializedName("details") val details: List<EspnDetail>?,
    @SerializedName("status") val status: EspnStatus?,
)

data class EspnCompetitor(
    @SerializedName("homeAway") val homeAway: String?,
    @SerializedName("score") val score: String?,
    @SerializedName("team") val team: EspnTeam?,
)

data class EspnTeam(
    @SerializedName("id") val id: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("shortDisplayName") val shortName: String?,
    @SerializedName("abbreviation") val abbreviation: String?,
    @SerializedName("logo") val logo: String?,
    @SerializedName("color") val color: String?,
)

data class EspnVenue(@SerializedName("fullName") val fullName: String?)

data class EspnStatus(
    @SerializedName("displayClock") val displayClock: String?,
    @SerializedName("type") val type: EspnStatusType?,
)

data class EspnStatusType(
    /** pre | in | post */
    @SerializedName("state") val state: String?,
    @SerializedName("shortDetail") val shortDetail: String?,
    @SerializedName("completed") val completed: Boolean?,
)

data class EspnDetail(
    @SerializedName("type") val type: EspnDetailType?,
    @SerializedName("clock") val clock: EspnClock?,
    @SerializedName("team") val team: EspnTeam?,
    @SerializedName("athletesInvolved") val athletes: List<EspnAthlete>?,
    @SerializedName("scoringPlay") val scoringPlay: Boolean?,
    @SerializedName("yellowCard") val yellowCard: Boolean?,
    @SerializedName("redCard") val redCard: Boolean?,
)

data class EspnDetailType(@SerializedName("text") val text: String?)
data class EspnClock(@SerializedName("displayValue") val displayValue: String?)
data class EspnAthlete(@SerializedName("displayName") val displayName: String?)
