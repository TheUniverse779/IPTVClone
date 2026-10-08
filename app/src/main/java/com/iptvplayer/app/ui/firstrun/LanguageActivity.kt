package com.iptvplayer.app.ui.firstrun

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.iptvplayer.app.R
import com.iptvplayer.app.ads.AppAds
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.databinding.ActivityLanguageAppBinding
import com.iptvplayer.app.databinding.ItemLanguageBinding
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.util.toast
import dagger.hilt.android.AndroidEntryPoint

/** One option on the language screen. */
private data class LangOption(val tag: String, val label: String, val flag: Int)

/**
 * First-run language screen. The list is the whole screen; the choice is confirmed by the check in
 * the title bar, which stays invisible until something is picked (same behaviour as the reference
 * app, including the hint when it is tapped with nothing selected). That tap is what moves on to
 * onboarding, through the full-page native break.
 */
@AndroidEntryPoint
class LanguageActivity : BaseActivity<ActivityLanguageAppBinding>(ActivityLanguageAppBinding::inflate) {

    /** Full-screen both ways: no status bar, no navigation bar. */
    override val showAdBanner = false
    override val applyInsets = false

    private var picked: String? = null

    /**
     * Applying a per-app locale recreates this Activity, so the ad + navigation cannot run in the
     * click handler's coroutine (it is cancelled on recreation). The flag carries the intent across
     * that recreation and [setup] continues the flow.
     */
    private var pendingContinue = false
    private val options = listOf(
        LangOption("en", "English", R.drawable.ic_flag_gb),
        LangOption("vi", "Tiếng Việt", R.drawable.ic_flag_vn),
    )

    private lateinit var adapter: SimpleAdapter<LangOption, ItemLanguageBinding>

    override fun setup(savedInstanceState: Bundle?) {
        hideSystemBars()
        picked = savedInstanceState?.getString(STATE_PICK)
        pendingContinue = savedInstanceState?.getBoolean(STATE_PENDING) ?: false

        adapter = SimpleAdapter(
            ItemLanguageBinding::inflate,
            same = { a, b -> a.tag == b.tag },
        ) { b, item, _ ->
            b.name.text = item.label
            b.flag.setImageResource(item.flag)
            b.root.isSelected = item.tag == picked
            b.radio.isSelected = item.tag == picked
            b.root.setOnClickListener { pick(item.tag) }
        }
        binding.rv.layoutManager = LinearLayoutManager(this)
        binding.rv.adapter = adapter
        adapter.submitList(options)

        binding.btnDone.setOnClickListener {
            val tag = picked
            if (tag == null) toast(R.string.pick_language_first)
            else {
                // Saving the language recreates this screen; setup() picks the flow up from here.
                pendingContinue = true
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        }
        render()
        // The card is pinned to the bottom of the screen; it loads once the ads runtime is ready.
        AppAds.showNative(this, AppAds.NATIVE_LANGUAGE, binding.adNative)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PICK, picked)
        outState.putBoolean(STATE_PENDING, pendingContinue)
    }

    private fun pick(tag: String) {
        picked = tag
        render()
    }

    private fun render() {
        adapter.notifyDataSetChanged()
        binding.btnDone.animate().alpha(if (picked == null) 0f else 1f).setDuration(300).start()
        binding.btnDone.isEnabled = picked != null
    }

    private fun openOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
        finish()
    }

    /** The full-page break needs a resumed host (the SDK reports HostNotResumed otherwise). */
    override fun onResume() {
        super.onResume()
        if (pendingContinue) {
            pendingContinue = false
            // Posted, not called straight away: the ads SDK reports HostNotResumed until the
            // activity has actually finished resuming.
            binding.root.post { AppAds.showFullScreen(this, AppAds.NATIVE_DONE) { openOnboarding() } }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        private const val STATE_PICK = "pick"
        private const val STATE_PENDING = "pending"
    }
}
