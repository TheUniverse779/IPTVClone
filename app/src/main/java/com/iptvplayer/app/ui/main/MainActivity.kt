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
    enum class Tab { HOME, CHANNELS, XTREAM, SPORT }

    override val applyInsets = false
    private var current = Tab.HOME
    private var lastBack = 0L

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun setup(savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.fragmentContainer.updatePadding(top = bars.top)
            // Bar grows by the gesture-nav inset; the raised "+" keeps its 24dp overhang above it.
            val barH = resources.getDimensionPixelSize(com.iptvplayer.app.R.dimen.h_bottombar) + bars.bottom
            binding.bottomBar.bar.updatePadding(bottom = bars.bottom)
            binding.bottomBar.bar.layoutParams.height = barH
            binding.bottomBar.root.layoutParams.height = barH + (24 * resources.displayMetrics.density).toInt()
            binding.bottomBar.root.requestLayout()
            insets
        }

        current = savedInstanceState?.getString(STATE_TAB)?.let { Tab.valueOf(it) }
            ?: intent.getStringExtra(EXTRA_TAB)?.let { Tab.valueOf(it) } ?: Tab.HOME
        with(binding.bottomBar) {
            tabHome.setOnClickListener { selectTab(Tab.HOME) }
            tabChannels.setOnClickListener { selectTab(Tab.CHANNELS) }
            tabXtream.setOnClickListener { selectTab(Tab.XTREAM) }
            tabSport.setOnClickListener { selectTab(Tab.SPORT) }
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

        if (Build.VERSION.SDK_INT >= 33 && savedInstanceState == null) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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
        Tab.SPORT -> SportFragment()
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
