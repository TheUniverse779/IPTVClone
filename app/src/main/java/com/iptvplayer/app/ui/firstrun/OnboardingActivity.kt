package com.iptvplayer.app.ui.firstrun

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.iptvplayer.app.R
import com.iptvplayer.app.ads.AppAds
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.databinding.ActivityOnboardingBinding
import com.iptvplayer.app.ui.disclaimer.DisclaimerActivity
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint

/** What each onboarding page shows. Shared with [OnboardingPageFragment]. */
enum class OnboardingPageKind { Artwork, FullAd }

data class OnboardingPage(
    val kind: OnboardingPageKind,
    val art: Int = 0,
    val title: Int = 0,
    val body: Int = 0,
    /** Native placement shown under this page; null when the page has no ad. */
    val nativeKey: String? = null,
)

/** page 1 and 4 show artwork with a native card, page 2 is clean, page 3 is a full-page native ad. */
val ONBOARDING_PAGES = listOf(
    OnboardingPage(OnboardingPageKind.Artwork, R.drawable.onb_1, R.string.onb1_title, R.string.onb1_body, AppAds.NATIVE_OBD1),
    OnboardingPage(OnboardingPageKind.Artwork, R.drawable.onb_2, R.string.onb2_title, R.string.onb2_body, null),
    OnboardingPage(OnboardingPageKind.FullAd, nativeKey = AppAds.NATIVE_FULL),
    OnboardingPage(OnboardingPageKind.Artwork, R.drawable.onb_3, R.string.onb3_title, R.string.onb3_body, AppAds.NATIVE_OBD3),
)

/**
 * First-run onboarding: four pages over a ViewPager2.
 *
 * The native card is pinned to the bottom of the screen and the dots/NEXT row rides on its top edge,
 * so both disappear on page 3, where the whole page is the ad. Finishing goes through the full-page
 * native break (obd_done) before the license screen.
 */
@AndroidEntryPoint
class OnboardingActivity : BaseActivity<ActivityOnboardingBinding>(ActivityOnboardingBinding::inflate) {

    override val showAdBanner = false
    override val applyInsets = false

    private var position = 0

    override fun setup(savedInstanceState: Bundle?) {
        hideSystemBars()
        binding.pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = ONBOARDING_PAGES.size
            override fun createFragment(position: Int) = OnboardingPageFragment.newInstance(position)
        }
        binding.pager.offscreenPageLimit = 1
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(p: Int) { position = p; render() }
        })

        binding.btnNext.setOnClickListener {
            if (position < ONBOARDING_PAGES.lastIndex) binding.pager.currentItem = position + 1 else finishFlow()
        }
        render()
    }

    /** Dots (current one is a stretched pill), the NEXT/start label, and the card for this page. */
    private fun render() {
        val page = ONBOARDING_PAGES[position]
        val showsCard = page.kind == OnboardingPageKind.Artwork
        binding.adNative.visible(showsCard)
        binding.foot.visible(showsCard)
        binding.btnNext.setText(if (position == ONBOARDING_PAGES.lastIndex) R.string.onb_start else R.string.onb_next)
        buildDots(position)
        // The card for this page, pinned to the bottom of the screen.
        page.nativeKey?.let {
            if (showsCard) AppAds.showNative(this, it, binding.adNative)
            else AppAds.preloadNative(it)
        }
        // Load the card of the page the user is about to reach so it is ready on arrival.
        ONBOARDING_PAGES.getOrNull(position + 1)?.nativeKey?.let { AppAds.preloadNative(it) }
        android.util.Log.d("Onboarding", "page=$position dots=${binding.dots.childCount} card=${binding.adNative.childCount}")
    }

    private fun buildDots(selected: Int) {
        binding.dots.removeAllViews()
        repeat(ONBOARDING_PAGES.size) { i ->
            val on = i == selected
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = if (on) 5f * resources.displayMetrics.density else 99f
                    setColor(getColor(if (on) R.color.accent else R.color.surface_3))
                }
            }
            binding.dots.addView(dot, android.widget.LinearLayout.LayoutParams(if (on) 22.dp else 8.dp, 8.dp).apply { marginEnd = 8.dp })
        }
    }

    /** Used by the full-ad page's own NEXT button. */
    fun nextPage() {
        if (position < ONBOARDING_PAGES.lastIndex) binding.pager.currentItem = position + 1 else finishFlow()
    }

    private fun finishFlow() {
        AppAds.showFullScreen(this, AppAds.NATIVE_DONE) {
            startActivity(Intent(this, DisclaimerActivity::class.java))
            finish()
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
}
