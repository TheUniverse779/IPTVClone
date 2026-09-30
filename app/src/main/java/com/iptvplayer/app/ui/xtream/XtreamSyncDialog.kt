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
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import com.iptvplayer.app.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import com.iptvplayer.app.util.toast

/**
 * Runs syncs outside any screen so "Run in background" can close the dialog while the (long) local save
 * finishes; one sync per key (profile id, or server+user for a new profile).
 */
@Singleton
class SyncRunner @Inject constructor(private val repo: XtreamRepository, @ApplicationContext private val app: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val runs = HashMap<String, Pair<Job, MutableStateFlow<SyncProgress?>>>()

    fun start(key: String, existingId: String?, input: NewProfileInput?): StateFlow<SyncProgress?> {
        runs[key]?.let { (job, flow) -> if (job.isActive || flow.value != null) { detached.remove(key); return flow } }
        val flow = MutableStateFlow<SyncProgress?>(null)
        val host = UrlUtils.host(input?.server.orEmpty())
        val job = scope.launch {
            try {
                repo.sync(existingId, input).collect { flow.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) { // e.g. disk full while saving: show an error instead of crashing
                flow.value = SyncProgress.Failed(SyncError.NETWORK, host)
            }
        }
        job.invokeOnCompletion { if (!detached.remove(key)) return@invokeOnCompletion; notifyDetached(flow.value); runs.remove(key) }
        runs[key] = job to flow
        return flow
    }

    /** User pressed Cancel before the save started: stop downloading. */
    fun cancel(key: String) { runs.remove(key)?.first?.cancel() }

    /** Dialog closed for good (result seen). */
    fun forget(key: String) { runs[key]?.let { (job, _) -> if (!job.isActive) runs.remove(key) } }

    private val detached = HashSet<String>()

    /** "Run in background": keep saving, tell the user with a toast when it's done. */
    fun detach(key: String) { if (runs[key]?.first?.isActive == true) detached += key else forget(key) }

    private fun notifyDetached(last: SyncProgress?) {
        when (last) {
            is SyncProgress.Done -> app.toast(app.getString(R.string.sync_bg_done, last.profile.name))
            is SyncProgress.Failed -> app.toast(R.string.err_network_title)
            else -> Unit
        }
    }
}

/** Survives rotation; the actual work lives in [SyncRunner]. */
@HiltViewModel
class SyncViewModel @Inject constructor(val runner: SyncRunner) : ViewModel() {
    var key: String = ""; private set
    var progress: StateFlow<SyncProgress?> = MutableStateFlow(null); private set

    fun start(key: String, existingId: String?, input: NewProfileInput?) {
        if (this.key == key) return
        this.key = key
        progress = runner.start(key, existingId, input)
    }
}

@AndroidEntryPoint
class XtreamSyncDialog : BaseDialog<DialogSyncBinding>(DialogSyncBinding::inflate) {
    private val vm: SyncViewModel by viewModels()
    private val isNew by lazy { requireArguments().getString(ARG_ID) == null }

    override fun setup(savedInstanceState: Bundle?) {
        isCancelable = false
        val a = requireArguments()
        val key = a.getString(ARG_ID) ?: "new:${a.getString("srv")}|${a.getString("user")}"
        vm.start(key, a.getString(ARG_ID), if (isNew) NewProfileInput(a.getString("name").orEmpty(), a.getString("srv")!!, a.getString("user")!!, a.getString("pass")!!, a.getBoolean("lock"), a.getInt("color").takeIf { a.containsKey("color") }) else null)
        binding.pTitle.setText(if (isNew) R.string.adding_profile else R.string.syncing)
        binding.btnCancel.setOnClickListener {
            if (saving) vm.runner.detach(vm.key) else vm.runner.cancel(vm.key)
            closeResult = false
            dismiss()
            if (saving && isNew) activity?.takeIf { it !is MainActivity }?.let { MainActivity.openTab(it, MainActivity.Tab.XTREAM); it.finish() }
        }
        collect(vm.progress) { p ->
            when (p) {
                null, is SyncProgress.Step -> renderSteps(p as? SyncProgress.Step)
                is SyncProgress.Done -> renderDone(p)
                is SyncProgress.Failed -> renderFail(p)
            }
        }
    }

    /** True while rows are being written locally: Cancel becomes "Run in background" (the save can't be undone half-way). */
    private var saving = false
    private var closeResult = true

    override fun onDestroy() {
        // Leaving with the result on screen (or after a real cancel): drop the finished run.
        if (closeResult && !requireActivity().isChangingConfigurations) vm.runner.forget(vm.key)
        super.onDestroy()
    }

    private fun renderSteps(p: SyncProgress.Step?) {
        binding.pHost.text = requireArguments().getString("host")
        binding.steps.removeAllViews()
        saving = p?.states?.get(SyncStep.SAVE) == StepState.RUNNING
        binding.btnCancel.setText(if (saving) R.string.run_in_background else R.string.cancel)
        binding.saveBar.visible(saving)
        binding.saveHint.visible(saving)
        if (saving && p != null && p.total > 0 && p.saved > 0) {
            // Indeterminate while old rows are cleared (re-sync), then real progress per inserted batch.
            binding.saveBar.isIndeterminate = false
            binding.saveBar.max = p.total
            binding.saveBar.setProgressCompat(p.saved, true)
        }
        listOf(SyncStep.AUTH to R.string.step_login, SyncStep.LIVE to R.string.step_live, SyncStep.VOD to R.string.step_vod,
            SyncStep.SERIES to R.string.step_series, SyncStep.SAVE to R.string.step_save).forEach { (step, label) ->
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
                    step == SyncStep.SAVE && state == StepState.RUNNING && p != null && p.total > 0 && p.saved > 0 -> {
                        text = "${p.saved * 100 / p.total}%"; setTextAppearance(R.style.Text_Cond); textSize = 16f; setTextColor(context.getColor(R.color.accent))
                    }
                    state != StepState.DONE -> {
                        setText(when { state == StepState.WAITING -> R.string.waiting; step == SyncStep.SAVE -> R.string.saving; else -> R.string.loading })
                        setTextAppearance(R.style.Text_Hint)
                    }
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
