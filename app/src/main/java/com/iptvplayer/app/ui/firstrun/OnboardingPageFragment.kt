package com.iptvplayer.app.ui.firstrun

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.iptvplayer.app.R
import com.iptvplayer.app.ads.AppAds
import com.iptvplayer.app.databinding.FragmentOnboardingPageBinding
import com.iptvplayer.app.databinding.FragmentOnboardingAdPageBinding

/**
 * One page of [OnboardingActivity].
 *
 * Artwork pages show the image (full width, its own aspect ratio, with the text right underneath)
 * and load their native card into the activity's bottom container. The full-ad page has no artwork
 * and instead fills itself with a native ad, with NEXT overlaid in the top corner.
 */
class OnboardingPageFragment : Fragment() {

    private var binding: FragmentOnboardingPageBinding? = null
    private var adBinding: FragmentOnboardingAdPageBinding? = null
    private var adContainer: FrameLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val page = ONBOARDING_PAGES[requireArguments().getInt(ARG_POSITION)]
        return if (page.kind == OnboardingPageKind.FullAd) {
            FragmentOnboardingAdPageBinding.inflate(inflater, container, false)
                .also { adBinding = it; adContainer = it.adPage }.root
        } else {
            FragmentOnboardingPageBinding.inflate(inflater, container, false).also { binding = it }.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val page = ONBOARDING_PAGES[requireArguments().getInt(ARG_POSITION)]
        if (page.kind == OnboardingPageKind.FullAd) {
            adBinding?.btnNext?.setOnClickListener { (activity as? OnboardingActivity)?.nextPage() }
            val container = adContainer
            if (container != null && page.nativeKey != null) {
                AppAds.showNative(requireActivity(), page.nativeKey, container, R.layout.layout_native_full_ad)
            }
        } else {
            binding?.art?.setImageResource(page.art)
            binding?.tvTitle?.setText(page.title)
            binding?.tvBody?.setText(page.body)
        }
    }

    override fun onDestroyView() {
        binding = null
        adBinding = null
        adContainer = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_POSITION = "position"
        fun newInstance(position: Int) = OnboardingPageFragment().apply {
            arguments = Bundle().apply { putInt(ARG_POSITION, position) }
        }
    }
}
