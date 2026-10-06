package com.iptvplayer.app.ui.passcode

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.TranslateAnimation
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.databinding.DialogPasscodeBinding
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One app-wide passcode (stored hashed). Modes:
 *  - CREATE: create → confirm, then result OK
 *  - ENTER: check (or biometric), then result OK
 *  - ENSURE: ENTER when a passcode exists, otherwise CREATE (used when turning a lock on)
 *  - CHANGE: ENTER the current passcode (or biometric) → CREATE → confirm, then result OK
 * Listen with `setFragmentResultListener(requestKey)`; result bundle has [KEY_OK] = true.
 */
@AndroidEntryPoint
class PasscodeDialog : BaseDialog<DialogPasscodeBinding>(DialogPasscodeBinding::inflate) {
    @Inject lateinit var settings: SettingsStore
    override val fullScreen = true

    private enum class Step { CREATE, CONFIRM, ENTER }
    private var step = Step.ENTER
    private var entered = ""
    private var first = ""
    private val changing get() = requireArguments().getString(ARG_MODE) == MODE_CHANGE

    override fun setup(savedInstanceState: Bundle?) {
        isCancelable = false
        binding.btnClose.setOnClickListener { dismiss() }
        buildDots(); buildKeypad()
        lifecycleScope.launch {
            val has = settings.current().hasPasscode
            step = when (requireArguments().getString(ARG_MODE)) {
                MODE_CREATE -> Step.CREATE
                MODE_ENSURE, MODE_CHANGE -> if (has) Step.ENTER else Step.CREATE
                else -> if (has) Step.ENTER else { finishOk(); return@launch }
            }
            render(null)
            if (step == Step.ENTER) maybeBiometric()
        }
    }

    private fun render(error: Int?) {
        val name = requireArguments().getString(ARG_NAME).orEmpty()
        binding.title.setText(when (step) {
            Step.CREATE -> if (changing) R.string.pass_new else R.string.pass_create
            Step.CONFIRM -> R.string.pass_confirm
            Step.ENTER -> if (changing) R.string.pass_enter_current else R.string.pass_enter
        })
        binding.sub.text = error?.let { getString(it) } ?: when (step) {
            Step.CREATE -> getString(R.string.pass_create_sub)
            Step.CONFIRM -> getString(R.string.pass_confirm_sub)
            Step.ENTER -> if (name.isNotEmpty()) getString(R.string.pass_enter_sub, name) else ""
        }
        binding.sub.setTextColor(requireContext().getColor(if (error != null) R.color.danger_text else R.color.text_2))
        // Biometrics only unlock; when creating a code the key would do nothing. maybeBiometric() shows it again for ENTER.
        if (step != Step.ENTER) binding.keypad.findViewWithTag<View>("bio")?.visibility = View.INVISIBLE
        updateDots()
    }

    private fun buildDots() {
        repeat(LENGTH) {
            binding.dots.addView(View(requireContext()), android.widget.LinearLayout.LayoutParams(16.dp, 16.dp).apply { marginStart = 8.dp; marginEnd = 8.dp })
        }
    }

    private fun updateDots() {
        for (i in 0 until binding.dots.childCount) {
            binding.dots.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                val on = i < entered.length
                setColor(if (on) requireContext().getColor(R.color.accent) else 0)
                setStroke(2.dp, requireContext().getColor(if (on) R.color.accent else R.color.text_3))
            }
        }
    }

    private fun buildKeypad() {
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "bio", "0", "del")
        keys.forEach { k ->
            val lp = GridLayout.LayoutParams().apply { width = 72.dp; height = 64.dp; setMargins(14.dp, 6.dp, 14.dp, 6.dp) }
            val v: View = when (k) {
                "del" -> ImageButton(requireContext()).apply {
                    setImageResource(R.drawable.ic_back); background = ContextCompat.getDrawable(context, R.drawable.bg_icon_btn)
                    contentDescription = getString(R.string.delete)
                    setOnClickListener { if (entered.isNotEmpty()) { entered = entered.dropLast(1); updateDots() } }
                }
                "bio" -> ImageButton(requireContext()).apply {
                    setImageResource(R.drawable.ic_user); background = ContextCompat.getDrawable(context, R.drawable.bg_icon_btn)
                    tag = "bio"
                    setOnClickListener { maybeBiometric() }
                }
                else -> TextView(requireContext()).apply {
                    text = k; gravity = Gravity.CENTER; textSize = 30f
                    setTextAppearance(R.style.Text_Cond); textSize = 30f
                    background = ContextCompat.getDrawable(context, R.drawable.bg_icon_btn)
                    setOnClickListener { press(k) }
                }
            }
            binding.keypad.addView(v, lp)
        }
    }

    private fun press(k: String) {
        if (entered.length >= LENGTH) return
        entered += k
        updateDots()
        if (entered.length == LENGTH) binding.root.postDelayed({ submit() }, 120)
    }

    private fun submit() {
        val code = entered
        entered = ""
        when (step) {
            Step.CREATE -> { first = code; step = Step.CONFIRM; render(null) }
            Step.CONFIRM -> if (code != first) { step = Step.CREATE; render(R.string.pass_mismatch); shake() } else lifecycleScope.launch {
                settings.setPasscode(code); context?.toast(if (changing) R.string.pass_changed else R.string.pass_created); finishOk()
            }
            Step.ENTER -> lifecycleScope.launch {
                if (settings.checkPasscode(code)) unlocked() else { render(R.string.pass_wrong); shake() }
            }
        }
    }

    /** Current passcode (or biometric) accepted: CHANGE goes on to the new code, other modes are done. */
    private fun unlocked() {
        if (!changing) return finishOk()
        step = Step.CREATE
        render(null)
    }

    private fun shake() = binding.dots.startAnimation(TranslateAnimation(-16f, 16f, 0f, 0f).apply { duration = 60; repeatCount = 4; repeatMode = TranslateAnimation.REVERSE })

    private fun maybeBiometric() {
        if (step != Step.ENTER) return
        val ok = BiometricManager.from(requireContext()).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        binding.keypad.findViewWithTag<View>("bio")?.visibility = if (ok) View.VISIBLE else View.INVISIBLE
        if (!ok) return
        BiometricPrompt(this, ContextCompat.getMainExecutor(requireContext()), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = unlocked()
        }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(getString(R.string.biometric_title))
            .setSubtitle(requireArguments().getString(ARG_NAME)).setNegativeButtonText(getString(R.string.biometric_use_passcode)).build())
    }

    private fun finishOk() {
        if (!isAdded) return
        parentFragmentManager.setFragmentResult(requireArguments().getString(ARG_KEY)!!, bundleOf(KEY_OK to true, KEY_PAYLOAD to requireArguments().getBundle(ARG_PAYLOAD)))
        dismissAllowingStateLoss()
    }

    companion object {
        const val KEY_OK = "ok"
        const val KEY_PAYLOAD = "payload"
        const val MODE_CREATE = "create"
        const val MODE_ENTER = "enter"
        const val MODE_ENSURE = "ensure"
        const val MODE_CHANGE = "change"
        private const val ARG_MODE = "mode"; private const val ARG_NAME = "name"; private const val ARG_KEY = "key"; private const val ARG_PAYLOAD = "payload"
        private const val LENGTH = 4

        fun show(fm: FragmentManager, requestKey: String, mode: String, name: String? = null, payload: Bundle? = null) =
            PasscodeDialog().apply { arguments = bundleOf(ARG_MODE to mode, ARG_NAME to name, ARG_KEY to requestKey, ARG_PAYLOAD to payload) }.show(fm, requestKey)
    }
}
