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

    /**
     * Settings › Decoder (hardware / software). Only meaningful with a software decoder extension
     * (e.g. media3 FFmpeg), which the app doesn't bundle, so the switch currently changes nothing.
     */
    const val DECODER_SETTING = false

    /**
     * Ask for POST_NOTIFICATIONS on first launch (Android 13+). Off while nothing sends notifications:
     * match reminders need [SPORT], the media notification needs [BACKGROUND_AUDIO].
     */
    const val NOTIFICATION_PERMISSION = false

    /**
     * Settings › Language (placeholder picker, English / Tiếng Việt). Hidden until there are more languages
     * or the custom language screen exists; the app starts in English (App.DEFAULT_LANGUAGE). Android 13+
     * still offers the per-app language in system settings (res/xml/locales_config.xml).
     */
    const val LANGUAGE_SETTING = false

    /** Settings › Playback › "Picture-in-picture when leaving the app" switch. Hidden: auto-PiP stays on (its default). */
    const val AUTO_PIP_SETTING = false

    /** Settings › Support › Send feedback (needs a real support e-mail). */
    const val FEEDBACK = false

    /** Settings › Legal group: Privacy policy, Terms of use, License agreement (needs real privacy/terms URLs). */
    const val LEGAL = false

    /**
     * Google AdMob ads (EZTech Ads SDK): the banner on the home screens and the interstitial before a
     * source opens. Turn off to ship a build with no ads at all (the SDK is still configured in App,
     * but nothing is requested or shown). The debug build uses Google's test ad units.
     */
    const val ADS = true
}
