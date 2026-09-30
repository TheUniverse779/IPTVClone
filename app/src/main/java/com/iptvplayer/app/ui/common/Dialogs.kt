package com.iptvplayer.app.ui.common

import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.databinding.DialogConfirmBinding
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.visible

/**
 * Generic message dialog. Results go through the FragmentManager result API so they survive
 * rotation: listen with `supportFragmentManager.setFragmentResultListener(requestKey, …)`.
 * The result bundle has [KEY_WHICH] = index of the pressed button (0 = positive, -1 = negative).
 */
class ConfirmDialog : BaseDialog<DialogConfirmBinding>(DialogConfirmBinding::inflate) {

    override fun setup(savedInstanceState: Bundle?) {
        val a = requireArguments()
        val key = a.getString(ARG_KEY)!!
        binding.title.text = a.getString(ARG_TITLE)
        val msg = a.getString(ARG_MSG)
        binding.message.visible(!msg.isNullOrBlank())
        binding.message.text = msg?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT) }
        val icon = a.getInt(ARG_ICON)
        if (icon != 0) {
            binding.heroWrap.visible(true)
            binding.heroIcon.setImageResource(icon)
            when (a.getString(ARG_TONE)) {
                "danger" -> { binding.heroWrap.setBackgroundResource(R.drawable.bg_hero_icon_danger); binding.heroIcon.setColorFilter(requireContext().getColor(R.color.danger_text)) }
                "ok" -> { binding.heroWrap.setBackgroundResource(R.drawable.bg_hero_icon_ok); binding.heroIcon.setColorFilter(requireContext().getColor(R.color.ok)) }
            }
        }
        isCancelable = a.getBoolean(ARG_CANCELABLE, true)

        fun send(which: Int) {
            parentFragmentManager.setFragmentResult(key, bundleOf(KEY_WHICH to which, KEY_PAYLOAD to a.getBundle(ARG_PAYLOAD)))
            dismiss()
        }

        val stacked = a.getStringArrayList(ARG_STACK)
        if (stacked != null) {
            binding.actions.visible(false)
            binding.stackActions.visible(true)
            stacked.forEachIndexed { i, label ->
                val style = when (i) { 0 -> R.style.Btn_Primary; stacked.lastIndex -> R.style.Btn_Text_Muted; else -> R.style.Btn_Tonal }
                val btn = MaterialButton(android.view.ContextThemeWrapper(requireContext(), style), null, 0).apply {
                    text = label
                    setOnClickListener { send(i) }
                }
                binding.stackActions.addView(btn, android.widget.LinearLayout.LayoutParams(-1, -2).apply { if (i > 0) topMargin = 8.dp })
            }
            return
        }

        binding.btnPositive.text = a.getString(ARG_OK) ?: getString(R.string.ok)
        if (a.getBoolean(ARG_DANGER)) {
            binding.btnPositive.setBackgroundColor(requireContext().getColor(R.color.danger_bg))
            binding.btnPositive.setTextColor(requireContext().getColor(R.color.danger_text))
        }
        val neg = a.getString(ARG_CANCEL)
        binding.btnNegative.visible(neg != "")
        binding.btnNegative.text = neg ?: getString(R.string.cancel)
        binding.btnPositive.setOnClickListener { send(0) }
        binding.btnNegative.setOnClickListener { send(-1) }
    }

    companion object {
        const val KEY_WHICH = "which"
        const val KEY_PAYLOAD = "payload"
        private const val ARG_KEY = "key"; private const val ARG_TITLE = "title"; private const val ARG_MSG = "msg"
        private const val ARG_OK = "ok"; private const val ARG_CANCEL = "cancel"; private const val ARG_DANGER = "danger"
        private const val ARG_ICON = "icon"; private const val ARG_TONE = "tone"; private const val ARG_STACK = "stack"
        private const val ARG_PAYLOAD = "payload"; private const val ARG_CANCELABLE = "cancelable"

        fun show(
            fm: FragmentManager, requestKey: String, title: String, message: String? = null,
            ok: String? = null, cancel: String? = null, danger: Boolean = false,
            icon: Int = 0, tone: String? = null, stacked: List<String>? = null, payload: Bundle? = null, cancelable: Boolean = true,
        ) = ConfirmDialog().apply {
            arguments = bundleOf(ARG_KEY to requestKey, ARG_TITLE to title, ARG_MSG to message, ARG_OK to ok, ARG_CANCEL to cancel,
                ARG_DANGER to danger, ARG_ICON to icon, ARG_TONE to tone, ARG_STACK to stacked?.let { ArrayList(it) },
                ARG_PAYLOAD to payload, ARG_CANCELABLE to cancelable)
        }.show(fm, requestKey)
    }
}

fun View.onClick(block: () -> Unit) = setOnClickListener { block() }

/** Adds an icon button to a toolbar's `actions` container and returns it. */
fun addToolbarButton(container: android.view.ViewGroup, icon: Int, boxed: Boolean = false, desc: Int = R.string.more, onClick: (View) -> Unit): android.widget.ImageButton {
    val layout = if (boxed) R.layout.view_icon_btn_boxed else R.layout.view_icon_btn
    val btn = android.view.LayoutInflater.from(container.context).inflate(layout, container, false) as android.widget.ImageButton
    btn.setImageResource(icon)
    btn.contentDescription = container.context.getString(desc)
    btn.setOnClickListener(onClick)
    container.addView(btn)
    return btn
}
