package com.iptvplayer.app.ui.xtream

import android.os.Bundle
import android.text.Html
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.fragment.app.viewModels
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.repository.NewProfileInput
import com.iptvplayer.app.data.repository.StepState
import com.iptvplayer.app.data.repository.SyncError
import com.iptvplayer.app.data.repository.SyncProgress
import com.iptvplayer.app.data.repository.SyncStep
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.DialogSyncBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.main.MainActivity
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Holds the sync job so it survives rotation while the dialog is shown. */
@HiltViewModel
class SyncViewModel @Inject constructor(private val repo: XtreamRepository) : ViewModel() {
    val progress = MutableStateFlow<SyncProgress?>(null)
    private var job: Job? = null

    fun start(existingId: String?, input: NewProfileInput?) {
        if (job != null) return
        job = viewModelScope.launch { repo.sync(existingId, input).collect { progress.value = it } }
    }

    fun cancel() { job?.cancel() }
}

@AndroidEntryPoint
class XtreamSyncDialog : BaseDialog<DialogSyncBinding>(DialogSyncBinding::inflate) {
    private val vm: SyncViewModel by viewModels()
    private val isNew by lazy { requireArguments().getString(ARG_ID) == null }

    override fun setup(savedInstanceState: Bundle?) {
        isCancelable = false
        val a = requireArguments()
        vm.start(a.getString(ARG_ID), if (isNew) NewProfileInput(a.getString("name").orEmpty(), a.getString("srv")!!, a.getString("user")!!, a.getString("pass")!!, a.getBoolean("lock"), a.getInt("color").takeIf { a.containsKey("color") }) else null)
        binding.pTitle.setText(if (isNew) R.string.adding_profile else R.string.syncing)
        binding.btnCancel.setOnClickListener { vm.cancel(); dismiss() }
        collect(vm.progress) { p ->
            when (p) {
                null, is SyncProgress.Step -> renderSteps(p as? SyncProgress.Step)
                is SyncProgress.Done -> renderDone(p)
                is SyncProgress.Failed -> renderFail(p)
            }
        }
    }

    private fun renderSteps(p: SyncProgress.Step?) {
        binding.pHost.text = requireArguments().getString("host")
        binding.steps.removeAllViews()
        listOf(SyncStep.AUTH to R.string.step_login, SyncStep.LIVE to R.string.step_live, SyncStep.VOD to R.string.step_vod, SyncStep.SERIES to R.string.step_series).forEach { (step, label) ->
            val state = p?.states?.get(step) ?: if (step == SyncStep.AUTH) StepState.RUNNING else StepState.WAITING
            val row = LinearLayout(requireContext()).apply { gravity = Gravity.CENTER_VERTICAL; minimumHeight = 32.dp }
            val lead: View = when (state) {
                StepState.DONE -> ImageView(requireContext()).apply { setImageResource(R.drawable.ic_check); setColorFilter(context.getColor(R.color.ok)) }
                StepState.RUNNING -> ProgressBar(requireContext()).apply { indeterminateTintList = android.content.res.ColorStateList.valueOf(context.getColor(R.color.accent)) }
                StepState.WAITING -> TextView(requireContext()).apply { text = "•"; gravity = Gravity.CENTER; setTextColor(context.getColor(R.color.text_3)) }
            }
            row.addView(lead, LinearLayout.LayoutParams(20.dp, 20.dp).apply { marginEnd = 12.dp })
            row.addView(TextView(requireContext()).apply { setText(label); setTextAppearance(R.style.Text) }, LinearLayout.LayoutParams(0, -2, 1f))
            val count = p?.counts?.get(step)
            row.addView(TextView(requireContext()).apply {
                when {
                    state != StepState.DONE -> { setText(if (state == StepState.RUNNING) R.string.loading else R.string.waiting); setTextAppearance(R.style.Text_Hint) }
                    step == SyncStep.AUTH -> { text = "OK"; setTextAppearance(R.style.Text_Hint) }
                    count == 0 -> { setText(R.string.none); setTextAppearance(R.style.Text_Hint) }
                    else -> { text = "%,d".format(count); setTextAppearance(R.style.Text_Cond); textSize = 16f }
                }
            })
            binding.steps.addView(row)
        }
    }

    private fun showResult() { binding.progressBlock.visible(false); binding.resultBlock.visible(true) }

    private fun renderDone(d: SyncProgress.Done) {
        showResult()
        val p = d.profile
        binding.stats.n1.text = "%,d".format(p.liveCount); binding.stats.l1.setText(R.string.live_channels)
        binding.stats.n2.text = "%,d".format(p.vodCount); binding.stats.l2.setText(R.string.movies_lc)
        binding.stats.n3.text = "%,d".format(p.seriesCount); binding.stats.l3.setText(R.string.series_lc)
        if (isNew) {
            binding.rIconWrap.visible(false)
            binding.addedAvatarWrap.visible(true)
            LogoUtil.avatar(binding.addedAvatar, p.name, p.avatarColor, 22)
            binding.rTitle.text = getString(R.string.added_profile, p.name)
            binding.rBody.setText(R.string.added_profile_sub)
            binding.rNote.visible(p.vodCount + p.seriesCount == 0)
            binding.rNote.setText(R.string.account_live_only)
            binding.buttons.btn1.setText(R.string.back_to_list)
            binding.buttons.btn2.setText(R.string.watch_now)
            binding.buttons.btn1.setOnClickListener { finishAndOpenXtreamTab(null) }
            binding.buttons.btn2.setOnClickListener { finishAndOpenXtreamTab(p.id) }
        } else {
            binding.rTitle.setText(R.string.sync_done)
            binding.rBody.visible(false)
            binding.buttons.btn1.visible(false)
            binding.buttons.btn2.setText(R.string.close)
            binding.buttons.btn2.setOnClickListener { dismiss() }
        }
    }

    /** New profile: return to Main › Xtream (profile shows the "New" tag) and optionally open it. */
    private fun finishAndOpenXtreamTab(openId: String?) {
        val ctx = requireContext()
        dismiss()
        MainActivity.openTab(ctx, MainActivity.Tab.XTREAM)
        if (openId != null) Nav.xtreamHome(ctx, openId)
        activity?.takeIf { it !is MainActivity }?.finish()
    }

    private fun renderFail(f: SyncProgress.Failed) {
        showResult()
        binding.stats.root.visible(false)
        binding.rIconWrap.setBackgroundResource(R.drawable.bg_hero_icon_danger)
        binding.rIcon.setColorFilter(requireContext().getColor(R.color.danger_text))
        val (icon, title, body) = when (f.error) {
            SyncError.HOST -> Triple(R.drawable.ic_wifi_off, R.string.err_host_title, getString(R.string.err_host_body, f.host))
            SyncError.AUTH -> Triple(R.drawable.ic_lock, R.string.err_auth_title, getString(R.string.err_auth_body))
            SyncError.EXPIRED -> Triple(R.drawable.ic_calendar, R.string.err_expired_title, getString(R.string.err_expired_body))
            SyncError.EMPTY -> Triple(R.drawable.ic_alert, R.string.err_empty_title, getString(R.string.err_empty_body))
            SyncError.NETWORK -> Triple(R.drawable.ic_wifi_off, R.string.err_network_title, getString(R.string.err_network_body))
        }
        binding.rIcon.setImageResource(icon)
        binding.rTitle.setText(title)
        binding.rBody.text = Html.fromHtml(body, Html.FROM_HTML_MODE_COMPACT)
        if (f.error == SyncError.EXPIRED) {
            binding.buttons.btn1.visible(false)
            binding.buttons.btn2.setText(R.string.close)
            binding.buttons.btn2.setOnClickListener { dismiss() }
        } else {
            binding.buttons.btn1.setText(R.string.guide)
            binding.buttons.btn1.setOnClickListener { dismiss(); Nav.howTo(requireContext(), "xtream") }
            binding.buttons.btn2.setText(R.string.fix_again)
            binding.buttons.btn2.setOnClickListener { dismiss() }
        }
    }

    companion object {
        private const val ARG_ID = "id"

        fun resync(fm: FragmentManager, id: String, host: String? = null) =
            XtreamSyncDialog().apply { arguments = bundleOf(ARG_ID to id, "host" to host) }.show(fm, "xtream_sync")

        fun add(fm: FragmentManager, name: String, srv: String, user: String, pass: String, lock: Boolean, color: Int? = null) =
            XtreamSyncDialog().apply {
                arguments = bundleOf("name" to name, "srv" to srv, "user" to user, "pass" to pass, "lock" to lock, "host" to com.iptvplayer.app.util.UrlUtils.host(srv))
                    .apply { color?.let { putInt("color", it) } }
            }.show(fm, "xtream_sync")
    }
}
