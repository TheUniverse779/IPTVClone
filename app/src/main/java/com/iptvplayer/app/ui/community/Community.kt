package com.iptvplayer.app.ui.community

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.databinding.ActivityCommunityBinding
import com.iptvplayer.app.databinding.DialogShareBinding
import com.iptvplayer.app.databinding.ItemCommunityBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.ConfirmDialog
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.UrlUtils
import com.iptvplayer.app.util.copyText
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint

/** A shared link. `kind` = iptv / xtream / single. */
data class CommunityLink(val id: String, val kind: String, val title: String, val url: String, val user: String = "", val pass: String = "")

/**
 * Community (links shared by other users). The original used Firebase Realtime DB; this build
 * has no Firebase project yet, so the list is empty and sharing is disabled with a note.
 * Plug a CommunityRepository (Firebase RTDB nodes iptv / xtream / singleStream) into [items].
 */
@AndroidEntryPoint
class CommunityActivity : BaseActivity<ActivityCommunityBinding>(ActivityCommunityBinding::inflate) {
    private var tab = "iptv"
    private val items: List<CommunityLink> = emptyList()

    private val adapter = SimpleAdapter<CommunityLink, ItemCommunityBinding>(ItemCommunityBinding::inflate, { a, b -> a.id == b.id }) { b, it, _ ->
        b.icon.setImageResource(when (it.kind) { "xtream" -> R.drawable.ic_xtream; "single" -> R.drawable.ic_play_circle; else -> R.drawable.ic_list })
        b.title.text = if (it.kind == "xtream") UrlUtils.host(it.url) else it.title
        b.sub.text = if (it.kind == "xtream") "User: ${it.user}" else it.url
        b.btnUse.setText(if (it.kind == "single") R.string.play else R.string.use_link)
        b.btnUse.setOnClickListener { _ -> use(it) }
        b.btnCopy.setOnClickListener { _ -> copyText(if (it.kind == "xtream") "Server URL: \"${it.url}\" ,User Name: \"${it.user}\" ,Password: \"${it.pass}\"" else it.url); toast(R.string.copied) }
        b.btnReport.setOnClickListener { _ -> ConfirmDialog.show(supportFragmentManager, KEY_REPORT, getString(R.string.report_q), getString(R.string.report_msg), ok = getString(R.string.report)) }
    }

    override fun setup(savedInstanceState: Bundle?) {
        tab = savedInstanceState?.getString("tab") ?: intent.getStringExtra(Nav.EXTRA_TAB) ?: "iptv"
        binding.toolbar.tvTitle.setText(R.string.community)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_share, desc = R.string.share_link) { ShareDialog.show(supportFragmentManager, tab, "", "") }
        binding.seg.s1.setText(R.string.comm_playlist); binding.seg.s2.setText(R.string.comm_xtream); binding.seg.s3.setText(R.string.comm_stream)
        binding.seg.s1.setOnClickListener { select("iptv") }; binding.seg.s2.setOnClickListener { select("xtream") }; binding.seg.s3.setOnClickListener { select("single") }
        binding.rv.adapter = adapter
        supportFragmentManager.setFragmentResultListener(KEY_REPORT, this) { _, r -> if (r.getInt(ConfirmDialog.KEY_WHICH) == 0) toast(R.string.report_sent) }
        select(tab)
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("tab", tab) }

    private fun select(t: String) {
        tab = t
        binding.seg.s1.isSelected = t == "iptv"; binding.seg.s2.isSelected = t == "xtream"; binding.seg.s3.isSelected = t == "single"
        val list = items.filter { it.kind == t }
        adapter.submitList(list)
        binding.empty.root.visible(list.isEmpty())
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_share)
        binding.empty.emptyTitle.setText(R.string.no_shares_title)
        binding.empty.emptyBody.setText(R.string.community_coming)
    }

    private fun use(l: CommunityLink) = when (l.kind) {
        "xtream" -> Nav.addProfile(this, l.url, l.user, l.pass, l.title)
        "single" -> Nav.play(this, PlayRequest.Url(l.url, l.title))
        else -> Nav.import(this, "url", l.url)
    }

    companion object { private const val KEY_REPORT = "comm_report" }
}

/** Kept for the manifest entry; "my shares" lives in CommunityActivity until the backend exists. */
@AndroidEntryPoint
class MyShareActivity : BaseActivity<ActivityCommunityBinding>(ActivityCommunityBinding::inflate) {
    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.my_shares)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        binding.seg.root.visible(false)
        binding.empty.root.visible(true)
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_share)
        binding.empty.emptyTitle.setText(R.string.no_shares_title)
        binding.empty.emptyBody.setText(R.string.community_coming)
    }
}

/** Share a playlist / Xtream / stream (UI only until a Community backend is connected). */
class ShareDialog : BaseDialog<DialogShareBinding>(DialogShareBinding::inflate) {
    private var type = "iptv"

    override fun setup(savedInstanceState: Bundle?) {
        type = savedInstanceState?.getString("type") ?: requireArguments().getString("type") ?: "iptv"
        binding.seg.s1.setText(R.string.comm_playlist); binding.seg.s2.setText(R.string.comm_xtream); binding.seg.s3.setText(R.string.comm_stream)
        binding.seg.s1.setOnClickListener { select("iptv") }; binding.seg.s2.setOnClickListener { select("xtream") }; binding.seg.s3.setOnClickListener { select("single") }
        if (savedInstanceState == null) {
            binding.e1.setText(requireArguments().getString("name")); binding.e2.setText(requireArguments().getString("url"))
        }
        binding.buttons.btn1.setText(R.string.cancel); binding.buttons.btn2.setText(R.string.share)
        binding.buttons.btn1.setOnClickListener { dismiss() }
        binding.buttons.btn2.setOnClickListener { requireContext().toast(R.string.community_coming); dismiss() }
        select(type)
    }

    private fun select(t: String) {
        type = t
        binding.seg.s1.isSelected = t == "iptv"; binding.seg.s2.isSelected = t == "xtream"; binding.seg.s3.isSelected = t == "single"
        val x = t == "xtream"
        binding.l1.setText(if (x) R.string.server_url else R.string.name)
        binding.l2.setText(if (x) R.string.username else R.string.url)
        binding.l3.visible(x); binding.e3.visible(x)
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("type", type) }

    companion object {
        fun show(fm: FragmentManager, type: String, name: String, url: String) =
            ShareDialog().apply { arguments = bundleOf("type" to type, "name" to name, "url" to url) }.show(fm, "share")
    }
}
