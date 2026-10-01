package com.iptvplayer.app

/**
 * Compile-time feature switches. Flip to true to bring a hidden feature back
 * (its screens and code are kept, only the entry points are hidden).
 */
object Features {
    /** Community link sharing (needs Firebase Realtime Database; hidden until decided). */
    const val COMMUNITY = false
}
