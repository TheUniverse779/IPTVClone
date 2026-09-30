package com.iptvplayer.app.data.network

import com.iptvplayer.app.data.network.dto.CategoryDto
import com.iptvplayer.app.data.network.dto.LiveStreamDto
import com.iptvplayer.app.data.network.dto.ScoreboardResponse
import com.iptvplayer.app.data.network.dto.SeriesDto
import com.iptvplayer.app.data.network.dto.SeriesInfoResponse
import com.iptvplayer.app.data.network.dto.VodInfoResponse
import com.iptvplayer.app.data.network.dto.VodStreamDto
import com.iptvplayer.app.data.network.dto.XtreamAuthResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface XtreamApi {
    @GET("player_api.php")
    suspend fun auth(@Query("username") u: String, @Query("password") p: String): XtreamAuthResponse

    @GET("player_api.php")
    suspend fun categories(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String): List<CategoryDto>

    @GET("player_api.php")
    suspend fun live(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_live_streams"): List<LiveStreamDto>

    @GET("player_api.php")
    suspend fun vod(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_vod_streams"): List<VodStreamDto>

    @GET("player_api.php")
    suspend fun series(@Query("username") u: String, @Query("password") p: String, @Query("action") action: String = "get_series"): List<SeriesDto>

    @GET("player_api.php")
    suspend fun vodInfo(@Query("username") u: String, @Query("password") p: String, @Query("vod_id") vodId: Int, @Query("action") action: String = "get_vod_info"): VodInfoResponse

    @GET("player_api.php")
    suspend fun seriesInfo(@Query("username") u: String, @Query("password") p: String, @Query("series_id") seriesId: Int, @Query("action") action: String = "get_series_info"): SeriesInfoResponse
}

/** Unofficial ESPN site API used by the original app (base https://site.api.espn.com/apis/site/v2/). */
interface EspnApi {
    @GET("sports/{sport}/{league}/scoreboard")
    suspend fun scoreboard(@Path("sport") sport: String, @Path("league") league: String, @Query("dates") dates: String? = null): ScoreboardResponse
}
