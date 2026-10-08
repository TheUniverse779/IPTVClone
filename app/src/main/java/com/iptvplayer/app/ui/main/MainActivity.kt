package com.iptvplayer.app.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.iptvplayer.app.Features
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.databinding.ActivityMainBinding
import com.iptvplayer.app.ui.sport.SportFragment
import com.iptvplayer.app.util.toast
import dagger.hilt.android.AndroidEntryPoint

/**
 * Bottom bar with 4 tab fragments (show/hide, created once so each keeps its state) and the
 * raised "+" that opens [AddSourceSheet].
 */
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    /** The 4th tab is Sport or Settings depending on [Features.SPORT]. */
    enum class Tab { HOME, CHANNELS, XTREAM, SPORT }

    private var current = Tab.HOME

    /** Pixels hidden behind the bottom bar (incl. gesture-nav inset). Tab fragments pad their lists by this. */
    val bottomCover = kotlinx.coroutines.flow.MutableStateFlow(0)
    private var lastBack = 0L

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun setup(savedInstanceState: Bundle?) {
        // BaseActivity pads the column for the system bars, so the bar itself no longer carries the
        // nav-bar inset: it sits above the ad banner, and the banner above the nav bar.
        val barH = resources.getDimensionPixelSize(com.iptvplayer.app.R.dimen.h_bottombar)
        binding.bottomBar.bar.layoutParams.height = barH
        // The raised "+" keeps its 24dp overhang above the bar.
        binding.bottomBar.root.layoutParams.height = barH + (24 * resources.displayMetrics.density).toInt()
        binding.bottomBar.root.requestLayout()
        bottomCover.value = barH

        current = savedInstanceState?.getString(STATE_TAB)?.let { Tab.valueOf(it) }
            ?: intent.getStringExtra(EXTRA_TAB)?.let { Tab.valueOf(it) } ?: Tab.HOME
        with(binding.bottomBar) {
            tabHome.setOnClickListener { selectTab(Tab.HOME) }
            tabChannels.setOnClickListener { selectTab(Tab.CHANNELS) }
            tabXtream.setOnClickListener { selectTab(Tab.XTREAM) }
            tabSport.setOnClickListener { selectTab(Tab.SPORT) }
            if (!Features.SPORT) { icSport.setImageResource(R.drawable.ic_settings); tvSport.setText(R.string.tab_settings) }
            fab.setOnClickListener { openAddSource() }
        }
        selectTab(current)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (current != Tab.HOME) { selectTab(Tab.HOME); return }
                val now = System.currentTimeMillis()
                if (now - lastBack < 2000) finish() else { lastBack = now; toast(com.iptvplayer.app.R.string.press_back_exit) }
            }
        })

        if (Features.NOTIFICATION_PERMISSION && Build.VERSION.SDK_INT >= 33 && savedInstanceState == null) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_TAB)?.let { selectTab(Tab.valueOf(it)) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_TAB, current.name)
    }

    fun selectTab(tab: Tab) {
        current = tab
        val fm = supportFragmentManager
        fm.commitNow { // synchronous so findFragmentByTag never misses a just-added tab
            setReorderingAllowed(true)
            Tab.entries.forEach { t ->
                val tag = t.name
                val existing = fm.findFragmentByTag(tag)
                if (t == tab) {
                    if (existing == null) add(binding.fragmentContainer.id, create(t), tag) else show(existing)
                } else existing?.let { hide(it) }
            }
        }
        with(binding.bottomBar) {
            tabHome.isSelected = tab == Tab.HOME
            tabChannels.isSelected = tab == Tab.CHANNELS
            tabXtream.isSelected = tab == Tab.XTREAM
            tabSport.isSelected = tab == Tab.SPORT
        }
    }

    private fun create(t: Tab): Fragment = when (t) {
        Tab.HOME -> HomeFragment()
        Tab.CHANNELS -> PlaylistsFragment()
        Tab.XTREAM -> XtreamProfilesFragment()
        Tab.SPORT -> if (Features.SPORT) SportFragment() else com.iptvplayer.app.ui.settings.SettingsFragment()
    }

    fun openAddSource() = AddSourceSheet().show(supportFragmentManager, "add_source")

    companion object {
        private const val EXTRA_TAB = "tab"
        private const val STATE_TAB = "state_tab"

        /** Brings MainActivity to front on [tab] (it is singleTask). */
        fun openTab(ctx: Context, tab: Tab) =
            ctx.startActivity(Intent(ctx, MainActivity::class.java).putExtra(EXTRA_TAB, tab.name).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
    }
}
