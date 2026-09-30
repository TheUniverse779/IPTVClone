package com.iptvplayer.app.ui.xtream

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.materialswitch.MaterialSwitch
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseBottomSheet
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.DialogConfirmBinding
import com.iptvplayer.app.databinding.SheetListBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.ConfirmDialog
import com.iptvplayer.app.ui.common.SheetRows
import com.iptvplayer.app.ui.common.bindProfileSub
import com.iptvplayer.app.ui.common.isExpired
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.UrlUtils
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun bindHead(b: SheetListBinding, p: XtreamProfileEntity, sub: String) {
    b.head.visible(true)
    LogoUtil.avatar(b.headAvatar, p.name, p.avatarColor, 14)
    b.headTitle.text = p.name
    b.headSub.text = sub
    val expired = isExpired(p)
    b.headStatus.setText(if (expired) R.string.status_expired else R.string.status_active)
    b.headStatus.setBackgroundResource(if (expired) R.drawable.bg_status_bad else R.drawable.bg_status_ok)
    b.headStatus.setTextColor(b.root.context.getColor(if (expired) R.color.danger_text else R.color.ok))
}

/** Long-press / ⋮ on a profile. */
@AndroidEntryPoint
class ProfileActionsSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {
    @Inject lateinit var repo: XtreamRepository
    private val profileId by lazy { requireArguments().getString("id")!! }

    override fun setup(savedInstanceState: Bundle?) {
        binding.header.root.visible(false)
        childFragmentManager.setFragmentResultListener(KEY_LOCK, this) { _, r ->
            if (r.getBoolean(PasscodeDialog.KEY_OK)) lifecycleScope.launch {
                val locked = r.getBundle(PasscodeDialog.KEY_PAYLOAD)!!.getBoolean("lock")
                repo.setLocked(profileId, locked)
                context?.toast(if (locked) R.string.profile_locked else R.string.profile_unlocked)
            }
        }
        childFragmentManager.setFragmentResultListener(KEY_DELETE, this) { _, r ->
            if (r.getInt(ConfirmDialog.KEY_WHICH) == 0) lifecycleScope.launch {
                repo.deleteProfile(profileId); context?.toast(R.string.profile_deleted); dismissAllowingStateLoss()
            }
        }
        collect(repo.observeProfile(profileId).filterNotNull()) { render(it) }
    }

    private fun render(p: XtreamProfileEntity) {
        bindHead(binding, p, "${UrlUtils.host(p.serverUrl)} · ${p.username}")
        val c = binding.container
        c.removeAllViews()
        SheetRows.option(c, getString(R.string.watch), icon = R.drawable.ic_play) { dismiss(); parentFragmentManager.setFragmentResult(RESULT_OPEN, bundleOf("id" to p.id)) }
        SheetRows.option(c, getString(R.string.resync), getString(R.string.last_sync, TimeFmt.ago(requireContext(), p.lastSync)), R.drawable.ic_refresh) {
            dismiss(); XtreamSyncDialog.resync(parentFragmentManager, p.id)
        }
        SheetRows.option(c, getString(R.string.account_info), getString(R.string.account_info_sub), R.drawable.ic_info) { dismiss(); AccountSheet.show(parentFragmentManager, p.id) }
        SheetRows.option(c, getString(R.string.edit_profile), icon = R.drawable.ic_edit) { dismiss(); Nav.editProfile(requireContext(), p.id) }
        val sw = MaterialSwitch(requireContext()).apply { isChecked = p.passcodeLocked; isClickable = false }
        SheetRows.option(c, getString(R.string.lock_with_passcode_short), icon = R.drawable.ic_lock, trailing = sw) {
            val lock = !p.passcodeLocked
            // Turning on: ensure a passcode exists. Turning off: must enter it.
            PasscodeDialog.show(childFragmentManager, KEY_LOCK, if (lock) PasscodeDialog.MODE_ENSURE else PasscodeDialog.MODE_ENTER, p.name, bundleOf("lock" to lock))
        }
        SheetRows.option(c, getString(R.string.delete_profile), icon = R.drawable.ic_trash, danger = true) {
            ConfirmDialog.show(childFragmentManager, KEY_DELETE, getString(R.string.delete_profile_q, p.name), getString(R.string.delete_profile_msg), ok = getString(R.string.delete), danger = true)
        }
    }

    companion object {
        /** Parent listens for this to run the passcode → expiry → open flow. */
        const val RESULT_OPEN = "profile_actions_open"
        private const val KEY_LOCK = "pa_lock"; private const val KEY_DELETE = "pa_delete"
        fun show(fm: FragmentManager, id: String) = ProfileActionsSheet().apply { arguments = bundleOf("id" to id) }.show(fm, "profile_actions")
    }
}

/** user_info / server_info details — not in the original app. */
@AndroidEntryPoint
class AccountSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {
    @Inject lateinit var repo: XtreamRepository

    override fun setup(savedInstanceState: Bundle?) {
        binding.header.root.visible(false)
        val id = requireArguments().getString("id")!!
        collect(repo.observeProfile(id).filterNotNull()) { p ->
            bindHead(binding, p, getString(R.string.account_info))
            val c = binding.container
            c.removeAllViews()
            c.setPadding(16.dp, 0, 16.dp, 0)
            fun kv(k: Int, v: String, err: Boolean = false) {
                val row = LinearLayout(requireContext()).apply { setPadding(0, 6.dp, 0, 6.dp) }
                row.addView(TextView(requireContext()).apply { setText(k); setTextAppearance(R.style.Text_Hint); textSize = 14f; width = 110.dp })
                row.addView(TextView(requireContext()).apply { text = v; setTextAppearance(R.style.Text); if (err) setTextColor(context.getColor(R.color.danger_text)) })
                c.addView(row)
            }
            kv(R.string.expiry, TimeFmt.date(p.expDate), isExpired(p))
            kv(R.string.connections, "${p.activeCons} / ${p.maxConnections.takeIf { it > 0 } ?: "∞"}")
            kv(R.string.formats, p.allowedOutputFormats.replace(",", ", "))
            kv(R.string.server, p.serverUrl)
            kv(R.string.username, p.username)
            if (p.timezone.isNotBlank()) kv(R.string.timezone, p.timezone)
            val stats = LayoutInflater.from(requireContext()).inflate(R.layout.view_stat_row, c, false)
            stats.findViewById<TextView>(R.id.n1).text = "%,d".format(p.liveCount); stats.findViewById<TextView>(R.id.l1).setText(R.string.live_channels)
            stats.findViewById<TextView>(R.id.n2).text = "%,d".format(p.vodCount); stats.findViewById<TextView>(R.id.l2).setText(R.string.movies_lc)
            stats.findViewById<TextView>(R.id.n3).text = "%,d".format(p.seriesCount); stats.findViewById<TextView>(R.id.l3).setText(R.string.series_lc)
            c.addView(stats)
            c.addView(TextView(requireContext()).apply { text = getString(R.string.last_sync, TimeFmt.ago(context, p.lastSync)); setTextAppearance(R.style.Text_Hint); setPadding(0, 8.dp, 0, 0) })
            val actions = LayoutInflater.from(requireContext()).inflate(R.layout.view_two_buttons, c, false)
            actions.findViewById<TextView>(R.id.btn1).apply { setText(R.string.edit); setOnClickListener { dismiss(); Nav.editProfile(requireContext(), p.id) } }
            actions.findViewById<TextView>(R.id.btn2).apply { setText(R.string.resync); setOnClickListener { dismiss(); XtreamSyncDialog.resync(parentFragmentManager, p.id) } }
            c.addView(actions)
        }
    }

    companion object {
        fun show(fm: FragmentManager, id: String) = AccountSheet().apply { arguments = bundleOf("id" to id) }.show(fm, "account")
    }
}

/** Tapping an expired profile. */
class ExpiredDialog : BaseDialog<DialogConfirmBinding>(DialogConfirmBinding::inflate) {
    override fun setup(savedInstanceState: Bundle?) {
        val a = requireArguments()
        val id = a.getString("id")!!
        binding.heroWrap.visible(true)
        binding.heroWrap.setBackgroundResource(R.drawable.bg_hero_icon_danger)
        binding.heroIcon.setImageResource(R.drawable.ic_calendar)
        binding.heroIcon.setColorFilter(requireContext().getColor(R.color.danger_text))
        binding.title.setText(R.string.expired_dialog_title)
        binding.message.text = getString(R.string.expired_dialog_body, a.getString("name"), TimeFmt.date(a.getLong("exp")))
        binding.actions.visible(false)
        binding.stackActions.visible(true)
        val inf = LayoutInflater.from(requireContext())
        fun btn(layout: Int, text: Int, click: () -> Unit) {
            val v = inf.inflate(layout, binding.stackActions, false) as TextView
            v.setText(text); v.setOnClickListener { dismiss(); click() }
            binding.stackActions.addView(v)
        }
        btn(R.layout.view_btn_primary, R.string.check_again) { XtreamSyncDialog.resync(parentFragmentManager, id) }
        btn(R.layout.view_btn_tonal, R.string.edit_login) { Nav.editProfile(requireContext(), id) }
        btn(R.layout.view_btn_text, R.string.open_anyway) { Nav.xtreamHome(requireContext(), id) }
    }

    companion object {
        fun show(fm: FragmentManager, id: String, name: String, exp: Long) =
            ExpiredDialog().apply { arguments = bundleOf("id" to id, "name" to name, "exp" to exp) }.show(fm, "expired")
    }
}
