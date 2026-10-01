package com.iptvplayer.app.ui.main

import android.view.View
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.util.padForBottomBar

/**
 * For fragments shown as a MainActivity tab: keeps [scroll]'s bottom padding equal to the real bottom-bar
 * height (gesture vs 3-button nav differ), plus room for the floating chatbot when [aboveFab].
 * No-op when the fragment is hosted elsewhere (e.g. Settings opened as its own screen).
 */
fun BaseFragment<*>.padForMainTabs(scroll: View, aboveFab: Boolean = false) {
    val host = activity as? MainActivity ?: return
    collect(host.bottomCover) { cover -> if (cover > 0) scroll.padForBottomBar(cover, aboveFab) }
}
