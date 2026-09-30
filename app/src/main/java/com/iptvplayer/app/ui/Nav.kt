package com.iptvplayer.app.ui

import android.content.Context
import android.content.Intent
import com.iptvplayer.app.ui.community.CommunityActivity
import com.iptvplayer.app.ui.guide.ChatbotActivity
import com.iptvplayer.app.ui.guide.FaqActivity
import com.iptvplayer.app.ui.guide.HowToAddActivity
import com.iptvplayer.app.ui.importer.ImportActivity
import com.iptvplayer.app.ui.importer.UploadM3uActivity
import com.iptvplayer.app.ui.player.PlayerActivity
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.ui.playlist.PlaylistDetailActivity
import com.iptvplayer.app.ui.search.SearchActivity
import com.iptvplayer.app.ui.settings.FeedbackActivity
import com.iptvplayer.app.ui.settings.SettingsActivity
import com.iptvplayer.app.ui.sport.MatchDetailActivity
import com.iptvplayer.app.ui.sport.MyMatchActivity
import com.iptvplayer.app.ui.sport.SportMatchesActivity
import com.iptvplayer.app.ui.xtream.AddEditProfileActivity
import com.iptvplayer.app.ui.xtream.MovieDetailActivity
import com.iptvplayer.app.ui.xtream.SeriesDetailActivity
import com.iptvplayer.app.ui.xtream.XtreamCategoryActivity
import com.iptvplayer.app.ui.xtream.XtreamHomeActivity
import com.iptvplayer.app.ui.xtream.XtreamRecentActivity

/** Every screen is an Activity; these are the typed entry points (extras live in one place). */
object Nav {
    const val EXTRA_TAB = "tab"
    const val EXTRA_ID = "id"
    const val EXTRA_URL = "url"
    const val EXTRA_Q = "q"
    const val EXTRA_TYPE = "type"
    const val EXTRA_PROFILE = "profile"
    const val EXTRA_CAT = "cat"
    const val EXTRA_LEAGUE = "league"
    const val EXTRA_TIME = "time"
    const val EXTRA_SRV = "srv"
    const val EXTRA_USER = "user"
    const val EXTRA_PASS = "pass"
    const val EXTRA_NAME = "name"

    fun import(ctx: Context, tab: String = "url", url: String? = null) =
        ctx.startActivity(Intent(ctx, ImportActivity::class.java).putExtra(EXTRA_TAB, tab).putExtra(EXTRA_URL, url))

    fun upload(ctx: Context) = ctx.startActivity(Intent(ctx, UploadM3uActivity::class.java))
    fun playlist(ctx: Context, id: Long) = ctx.startActivity(Intent(ctx, PlaylistDetailActivity::class.java).putExtra(EXTRA_ID, id))
    fun search(ctx: Context, q: String? = null) = ctx.startActivity(Intent(ctx, SearchActivity::class.java).putExtra(EXTRA_Q, q))
    fun play(ctx: Context, req: PlayRequest) = PlayerActivity.start(ctx, req)

    fun addProfile(ctx: Context, srv: String? = null, user: String? = null, pass: String? = null, name: String? = null) =
        ctx.startActivity(Intent(ctx, AddEditProfileActivity::class.java)
            .putExtra(EXTRA_SRV, srv).putExtra(EXTRA_USER, user).putExtra(EXTRA_PASS, pass).putExtra(EXTRA_NAME, name))

    fun editProfile(ctx: Context, id: String) = ctx.startActivity(Intent(ctx, AddEditProfileActivity::class.java).putExtra(EXTRA_PROFILE, id))
    fun xtreamHome(ctx: Context, id: String) = ctx.startActivity(Intent(ctx, XtreamHomeActivity::class.java).putExtra(EXTRA_PROFILE, id))
    fun xtreamCategory(ctx: Context, id: String, type: String, cat: String?) =
        ctx.startActivity(Intent(ctx, XtreamCategoryActivity::class.java).putExtra(EXTRA_PROFILE, id).putExtra(EXTRA_TYPE, type).putExtra(EXTRA_CAT, cat))
    fun movie(ctx: Context, id: String, streamId: Int) = ctx.startActivity(Intent(ctx, MovieDetailActivity::class.java).putExtra(EXTRA_PROFILE, id).putExtra(EXTRA_ID, streamId))
    fun series(ctx: Context, id: String, seriesId: Int) = ctx.startActivity(Intent(ctx, SeriesDetailActivity::class.java).putExtra(EXTRA_PROFILE, id).putExtra(EXTRA_ID, seriesId))
    fun xtreamRecent(ctx: Context, id: String) = ctx.startActivity(Intent(ctx, XtreamRecentActivity::class.java).putExtra(EXTRA_PROFILE, id))

    fun howTo(ctx: Context, type: String = "url") = ctx.startActivity(Intent(ctx, HowToAddActivity::class.java).putExtra(EXTRA_TYPE, type))
    fun faq(ctx: Context) = ctx.startActivity(Intent(ctx, FaqActivity::class.java))
    fun chatbot(ctx: Context) = ctx.startActivity(Intent(ctx, ChatbotActivity::class.java))
    fun settings(ctx: Context) = ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
    fun feedback(ctx: Context) = ctx.startActivity(Intent(ctx, FeedbackActivity::class.java))
    fun community(ctx: Context, tab: String = "iptv") = ctx.startActivity(Intent(ctx, CommunityActivity::class.java).putExtra(EXTRA_TAB, tab))

    fun sportMatches(ctx: Context) = ctx.startActivity(Intent(ctx, SportMatchesActivity::class.java))
    fun myMatches(ctx: Context) = ctx.startActivity(Intent(ctx, MyMatchActivity::class.java))
    fun match(ctx: Context, league: String, eventId: String, startTime: Long) =
        ctx.startActivity(Intent(ctx, MatchDetailActivity::class.java).putExtra(EXTRA_LEAGUE, league).putExtra(EXTRA_ID, eventId).putExtra(EXTRA_TIME, startTime))
}
