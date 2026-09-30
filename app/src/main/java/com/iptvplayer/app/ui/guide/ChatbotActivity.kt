package com.iptvplayer.app.ui.guide

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.databinding.ActivityChatbotBinding
import com.iptvplayer.app.databinding.ItemChatBotBinding
import com.iptvplayer.app.databinding.ItemChatSuggestBinding
import com.iptvplayer.app.databinding.ItemChatUserBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.VH
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Normalizer

/** Offline keyword assistant (the original also answered locally). 3 suggestions; no Premium question. */
@AndroidEntryPoint
class ChatbotActivity : BaseActivity<ActivityChatbotBinding>(ActivityChatbotBinding::inflate) {

    private sealed interface Msg {
        data class Bot(val text: Int, val steps: Int = 0, val topic: String? = null) : Msg
        data class User(val text: String) : Msg
        data object Typing : Msg
        data class Suggest(val text: Int) : Msg
    }

    private val msgs = mutableListOf<Msg>()
    private val adapter = ChatAdapter()

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.assistant_title)
        binding.toolbar.tvSubtitle.visible(true)
        binding.toolbar.tvSubtitle.setText(R.string.assistant_sub)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        binding.rv.adapter = adapter
        msgs += Msg.Bot(R.string.bot_hello)
        msgs += listOf(R.string.bot_q1, R.string.bot_q2, R.string.bot_q3).map { Msg.Suggest(it) }
        adapter.notifyDataSetChanged()
        binding.btnSend.setOnClickListener { send(binding.et.text.toString()) }
        binding.et.setOnEditorActionListener { v, id, _ -> if (id == EditorInfo.IME_ACTION_SEND) { send(v.text.toString()); true } else false }
    }

    private fun send(text: String) {
        val q = text.trim()
        if (q.isEmpty()) return
        binding.et.setText("")
        msgs.removeAll { it is Msg.Suggest }
        msgs += Msg.User(q); msgs += Msg.Typing
        adapter.notifyDataSetChanged(); binding.rv.scrollToPosition(msgs.size - 1)
        lifecycleScope.launch {
            delay(1500)
            msgs.remove(Msg.Typing)
            msgs += when (match(q)) {
                "m3u" -> Msg.Bot(R.string.bot_a_m3u, R.string.bot_a_m3u_s, "url")
                "single" -> Msg.Bot(R.string.bot_a_single, R.string.bot_a_single_s, "single")
                "xtream" -> Msg.Bot(R.string.bot_a_xtream, R.string.bot_a_xtream_s, "xtream")
                "fix" -> Msg.Bot(R.string.bot_a_fix, R.string.bot_a_fix_s, "fix")
                else -> Msg.Bot(R.string.bot_unknown, topic = "unknown")
            }
            adapter.notifyDataSetChanged(); binding.rv.scrollToPosition(msgs.size - 1)
        }
    }

    /** Keyword match, accent-insensitive so Vietnamese questions work too. */
    private fun match(q: String): String? {
        val t = Normalizer.normalize(q.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").replace('đ', 'd')
        return when {
            Regex("khong (phat|xem|chay|load)|khong duoc|giat|lag|buffer|loi|error|den|not (play|work)|black screen|fail").containsMatchIn(t) -> "fix"
            Regex("xtream|username|password|server|host|tai khoan").containsMatchIn(t) -> "xtream"
            Regex("m3u|playlist|danh sach|file").containsMatchIn(t) -> "m3u"
            Regex("single|stream|link|phat").containsMatchIn(t) -> "single"
            else -> null
        }
    }

    private inner class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = msgs.size
        override fun getItemViewType(p: Int) = when (msgs[p]) { is Msg.User -> 1; is Msg.Suggest -> 2; else -> 0 }
        override fun onCreateViewHolder(parent: ViewGroup, type: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return when (type) { 1 -> VH(ItemChatUserBinding.inflate(inf, parent, false)); 2 -> VH(ItemChatSuggestBinding.inflate(inf, parent, false)); else -> VH(ItemChatBotBinding.inflate(inf, parent, false)) }
        }
        override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {
            when (val b = (h as VH<*>).b) {
                is ItemChatUserBinding -> b.text.text = (msgs[p] as Msg.User).text
                is ItemChatSuggestBinding -> { val s = msgs[p] as Msg.Suggest; b.text.setText(s.text); b.text.setOnClickListener { send(getString(s.text)) } }
                is ItemChatBotBinding -> bindBot(b, msgs[p])
            }
        }
    }

    private fun bindBot(b: ItemChatBotBinding, m: Msg) {
        val typing = m is Msg.Typing
        b.typing.visible(typing); b.text.visible(!typing)
        b.steps.visible(false); b.actions.visible(false); b.actions.removeAllViews()
        if (m !is Msg.Bot) return
        b.text.setText(m.text)
        if (m.steps != 0) { b.steps.visible(true); b.steps.setText(m.steps) }
        fun action(label: Int, primary: Boolean, onClick: () -> Unit) = b.actions.addView(Chip(this).apply {
            setText(label)
            chipBackgroundColor = getColorStateList(if (primary) R.color.accent else R.color.surface_3)
            setTextColor(getColor(if (primary) R.color.white else R.color.text))
            setOnClickListener { onClick() }
        })
        when (m.topic) {
            "url" -> { action(R.string.add_playlist, true) { Nav.import(this, "url") }; action(R.string.view_guide, false) { Nav.howTo(this, "url") } }
            "single" -> action(R.string.open_form, true) { Nav.import(this, "single") }
            "xtream" -> { action(R.string.add_xtream, true) { Nav.addProfile(this) }; action(R.string.view_guide, false) { Nav.howTo(this, "xtream") } }
            "fix" -> { action(R.string.set_user_agent, true) { Nav.settings(this) }; action(R.string.view_faq, false) { Nav.faq(this) } }
            "unknown" -> { action(R.string.view_faq, false) { Nav.faq(this) }; action(R.string.contact_support, false) { Nav.feedback(this) } }
        }
        b.actions.visible(b.actions.childCount > 0)
    }
}
