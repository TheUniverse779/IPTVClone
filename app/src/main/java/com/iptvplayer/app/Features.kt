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
}
