package com.iptvplayer.app

/**
 * Compile-time feature switches. Flip to true to bring a hidden feature back
 * (its screens and code are kept, only the entry points are hidden).
 */
object Features {
    /** Community link sharing (needs Firebase Realtime Database; hidden until decided). */
    const val COMMUNITY = false

    /**
     * Sport tab (ESPN schedule). When false the 4th bottom tab shows Settings instead,
     * like the original app's Remote Config `show_sport` = false.
     */
    const val SPORT = false

    /**
     * "Where to find playlists" help: suggested third-party sites (WebView sheet), the Google
     * search shortcut and the guide strips in the add-source forms. When false the guide only
     * explains how to enter a link/account the user already has.
     */
    const val GUIDE_SITES = false

    /**
     * "Keep playing when the screen is off" (PlaybackService, a mediaPlayback foreground service).
     * Off for the first Play release so no foreground-service declaration/video is needed. To turn it
     * back on, also restore the FOREGROUND_SERVICE permissions and the <service> in AndroidManifest.xml.
     */
    const val BACKGROUND_AUDIO = false

    /** Player "Cast" button (opens Android's cast / wireless display settings) and the FAQ entry about it. */
    const val CAST = false

    /** Settings › Data: back up playlists to a JSON file and restore from it. */
    const val BACKUP = false
}
