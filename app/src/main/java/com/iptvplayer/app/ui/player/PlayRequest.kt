package com.iptvplayer.app.ui.player

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** What the player should open. Passed to PlayerActivity as a Parcelable extra. */
sealed interface PlayRequest : Parcelable {
    /** A channel from an M3U playlist; the rest of the playlist is the zapping list. */
    @Parcelize data class Channel(val playlistId: Long, val channelId: Long) : PlayRequest
    /** A single link (single stream, device video). */
    @Parcelize data class Url(val url: String, val title: String) : PlayRequest
    /** Xtream live channel; zapping list = the profile's live channels. */
    @Parcelize data class XtreamLive(val profileId: String, val streamId: Int) : PlayRequest
    @Parcelize data class XtreamMovie(val profileId: String, val streamId: Int) : PlayRequest
    @Parcelize data class XtreamEpisode(val profileId: String, val seriesId: Int, val episodeId: String) : PlayRequest
}
