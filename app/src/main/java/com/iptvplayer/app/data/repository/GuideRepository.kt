package com.iptvplayer.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GuideSite(
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String,
    /** "webview" (in-app sheet) or "customtab". */
    @SerializedName("mode") val mode: String = "webview",
)

data class GuideSites(
    @SerializedName("iptv") val iptv: List<GuideSite> = emptyList(),
    @SerializedName("xtream") val xtream: List<GuideSite> = emptyList(),
    @SerializedName("single") val single: List<GuideSite> = emptyList(),
    @SerializedName("search_query") val searchQuery: String = "",
) {
    fun forType(type: String) = when (type) { "xtream" -> xtream; "single" -> single; else -> iptv }
}

/**
 * Suggested third-party sites shown in the guide. Defaults ship in `res/raw/guide_sites.json`.
 * Designed so a Remote Config value (key `guide_sites`, same JSON shape) can override it later
 * without an app update — plug it into [override] once Firebase is added.
 */
@Singleton
class GuideRepository @Inject constructor(@ApplicationContext private val context: Context, private val gson: Gson) {
    @Volatile var override: String? = null

    fun sites(): GuideSites {
        override?.let { json -> runCatching { return gson.fromJson(json, GuideSites::class.java) } }
        val raw = context.resources.openRawResource(com.iptvplayer.app.R.raw.guide_sites).bufferedReader().use { it.readText() }
        return gson.fromJson(raw, GuideSites::class.java)
    }
}
