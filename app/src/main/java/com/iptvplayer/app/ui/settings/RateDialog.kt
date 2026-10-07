package com.iptvplayer.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.databinding.DialogConfirmBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.util.dp

/** 4–5 stars → Play Store listing (swap for the In-App Review API once published). 1–3 → feedback form. */
class RateDialog : BaseDialog<DialogConfirmBinding>(DialogConfirmBinding::inflate) {
    private var stars = 0
    private val starButtons = mutableListOf<ImageButton>()

    override fun setup(savedInstanceState: Bundle?) {
        stars = savedInstanceState?.getInt("stars") ?: 0
        binding.title.setText(R.string.rate_title)
        binding.message.setText(R.string.rate_body)
        binding.btnNegative.setText(R.string.later)
        binding.btnPositive.setText(R.string.send)
        val row = LinearLayout(requireContext()).apply { gravity = Gravity.CENTER; setPadding(0, 16.dp, 0, 0) }
        starButtons.clear()
        (1..5).forEach { i ->
            val b = ImageButton(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_icon_btn); setPadding(6.dp, 6.dp, 6.dp, 6.dp)
                contentDescription = "$i"
                setOnClickListener { stars = i; renderStars() }
            }
            starButtons += b
            row.addView(b, LinearLayout.LayoutParams(44.dp, 44.dp))
        }
        binding.extra.addView(row)
        binding.btnNegative.setOnClickListener { dismiss() }
        binding.btnPositive.setOnClickListener {
            val ctx = requireContext()
            dismiss()
            if (stars >= 4) runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}"))) }
            else if (com.iptvplayer.app.Features.FEEDBACK) Nav.feedback(ctx)
        }
        renderStars()
    }

    private fun renderStars() {
        starButtons.forEachIndexed { i, b ->
            b.setImageResource(if (i < stars) R.drawable.ic_star_fill else R.drawable.ic_star)
            b.setColorFilter(requireContext().getColor(R.color.gold))
        }
        binding.btnPositive.isEnabled = stars > 0
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putInt("stars", stars) }
}
