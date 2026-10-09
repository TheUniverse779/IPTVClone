package com.iptvplayer.app.ui.firstrun

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.iptvplayer.app.R
import com.iptvplayer.app.ads.AppAds
import com.iptvplayer.app.databinding.FragmentOnboardingAdPageBinding
import com.iptvplayer.app.databinding.FragmentOnboardingPageBinding
import com.iptvplayer.app.util.dp

/**
 * One page of [OnboardingActivity]. Every page owns its whole layout, so the activity is only the
 * pager: artwork at the image's own ratio with the text right underneath, the dots + NEXT row, and
 * the page's native card pinned to the bottom.
 *
 * The full-ad page has no artwork: it fills itself with a native ad and floats NEXT in the corner.
 */
class OnboardingPageFragment : Fragment() {

    private var binding: FragmentOnboardingPageBinding? = null
    private var adBinding: FragmentOnboardingAdPageBinding? = null

    private val position get() = requireArguments().getInt(ARG_POSITION)
    private val page get() = ONBOARDING_PAGES[position]

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        if (page.kind == OnboardingPageKind.FullAd) {
            FragmentOnboardingAdPageBinding.inflate(inflater, container, false).also { adBinding = it }.root
        } else {
            FragmentOnboardingPageBinding.inflate(inflater, container, false).also { binding = it }.root
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val page = page
        if (page.kind == OnboardingPageKind.FullAd) {
            val adPage = adBinding!!.adPage
            adBinding?.btnNext?.setOnClickListener { next() }
            // The SDK wraps the native layout in its own wrap_content FrameLayout, so match_parent in
            // layout_native_full_ad.xml only fills that wrapper and the page ends ~200px short of the
            // bottom. Stretch whatever the SDK adds to the full height of the page.
            adPage.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View, child: View) {
                    child.layoutParams = child.layoutParams.apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
                }
                override fun onChildViewRemoved(parent: View, child: View) = Unit
            })
            page.nativeKey?.let { AppAds.showNative(requireActivity(), it, adPage, R.layout.layout_native_full_ad) }
            return
        }

        val b = binding ?: return
        b.art.setImageResource(page.art)
        b.tvTitle.setText(page.title)
        b.tvBody.setText(page.body)
        b.btnNext.setText(if (position == ONBOARDING_PAGES.lastIndex) R.string.onb_start else R.string.onb_next)
        b.btnNext.setOnClickListener { next() }
        buildDots(b, position)
        // The card of the page the user is about to reach, so it is ready on arrival.
        ONBOARDING_PAGES.getOrNull(position + 1)?.nativeKey?.let { AppAds.preloadNative(it) }
        page.nativeKey?.let { AppAds.showNative(requireActivity(), it, b.adNative) }
    }

    /** Advances the pager, or hands over to the activity on the last page. */
    private fun next() {
        val host = activity as? OnboardingActivity ?: return
        if (position < ONBOARDING_PAGES.lastIndex) host.goTo(position + 1) else host.finishFlow()
    }

    /** Four dots, the current one a stretched pill. */
    private fun buildDots(b: FragmentOnboardingPageBinding, selected: Int) {
        b.dots.removeAllViews()
        repeat(ONBOARDING_PAGES.size) { i ->
            val on = i == selected
            val dot = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = if (on) 5f * resources.displayMetrics.density else 99f
                    setColor(requireContext().getColor(if (on) R.color.accent else R.color.surface_3))
                }
            }
            b.dots.addView(dot, android.widget.LinearLayout.LayoutParams(if (on) 22.dp else 8.dp, 8.dp).apply {
                marginEnd = 8.dp
            })
        }
    }

    override fun onDestroyView() {
        binding = null
        adBinding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POSITION = "position"
        fun newInstance(position: Int) = OnboardingPageFragment().apply {
            arguments = Bundle().apply { putInt(ARG_POSITION, position) }
        }
    }
}
