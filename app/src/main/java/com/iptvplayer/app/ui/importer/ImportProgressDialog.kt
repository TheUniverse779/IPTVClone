package com.iptvplayer.app.ui.importer

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.repository.ImportError
import com.iptvplayer.app.data.repository.ImportProgress
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.databinding.DialogImportProgressBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.main.MainActivity
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportJobViewModel @Inject constructor(private val repo: PlaylistRepository) : ViewModel() {
    val progress = MutableStateFlow<ImportProgress?>(null)
    private var job: Job? = null
    /** Files queued after the current one (UploadM3uActivity imports several). */
    private val queue = ArrayDeque<Pair<String, Uri>>()
    var lastPlaylistId = -1L; private set
    var totalChannels = 0; private set
    var totalGroups = 0; private set

    fun start(args: Bundle) {
        if (job != null) return
        when (args.getString(ARG_MODE)) {
            MODE_URL -> run(repo.addFromUrl(args.getString("name")!!, args.getString("url")!!, args.getBoolean("lock"), args.getBoolean("auto")))
            MODE_REFRESH -> run(repo.refresh(args.getLong("id")))
            MODE_FILES -> {
                val names = args.getStringArrayList("names")!!; val uris = args.getStringArrayList("uris")!!
                names.indices.forEach { queue += names[it] to Uri.parse(uris[it]) }
                next(args.getBoolean("lock"))
            }
        }
    }

    private fun next(lock: Boolean) {
        val (name, uri) = queue.removeFirstOrNull() ?: return
        run(repo.addFromFile(name, uri, lock)) { if (queue.isNotEmpty()) { job = null; next(lock) } }
    }

    private fun run(flow: Flow<ImportProgress>, onDone: () -> Unit = {}) {
        job = viewModelScope.launch {
            flow.onCompletion { }.collect { p ->
                if (p is ImportProgress.Done) {
                    lastPlaylistId = p.playlistId; totalChannels += p.channels; totalGroups += p.groups
                    if (queue.isNotEmpty()) { onDone(); return@collect }
                    progress.value = ImportProgress.Done(p.playlistId, totalChannels, totalGroups)
                } else progress.value = p
            }
        }
    }

    fun cancel() { job?.cancel(); queue.clear() }

    companion object {
        const val ARG_MODE = "mode"; const val MODE_URL = "url"; const val MODE_REFRESH = "refresh"; const val MODE_FILES = "files"
    }
}

@AndroidEntryPoint
class ImportProgressDialog : BaseDialog<DialogImportProgressBinding>(DialogImportProgressBinding::inflate) {
    private val vm: ImportJobViewModel by viewModels()
    private var pulse: AnimatorSet? = null
    private var shownCount = 0

    override fun setup(savedInstanceState: Bundle?) {
        isCancelable = false
        val a = requireArguments()
        vm.start(a)
        binding.loading.text = getString(R.string.loading_x, a.getString("name"))
        binding.btnCancel.setOnClickListener {
            vm.cancel(); context?.toast(R.string.import_cancelled); dismiss()
        }
        startPulse()
        collect(vm.progress) { p ->
            when (p) {
                null -> render(0, 0, null)
                is ImportProgress.Reading -> render(p.channels, p.groups, p.fraction)
                is ImportProgress.Done -> done(p)
                is ImportProgress.Failed -> failed(p)
            }
        }
    }

    private fun startPulse() {
        fun ring(v: View, delay: Long) = ObjectAnimator.ofPropertyValuesHolder(v,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.45f, 1f), PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.45f, 1f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0f)).apply { duration = 2000; startDelay = delay; repeatCount = ValueAnimator.INFINITE }
        pulse = AnimatorSet().apply { playTogether(ring(binding.ring1, 0), ring(binding.ring2, 1000)); start() }
    }

    private fun render(channels: Int, groups: Int, fraction: Float?) {
        ValueAnimator.ofInt(shownCount, channels).apply {
            duration = 150
            addUpdateListener { binding.count.text = "%,d".format(it.animatedValue as Int) }
        }.start()
        shownCount = channels
        binding.countSub.text = getString(R.string.channels_read, groups)
        binding.bar.isIndeterminate = fraction == null
        if (fraction != null) binding.bar.setProgressCompat((fraction * 100).toInt(), true)
    }

    private fun done(p: ImportProgress.Done) {
        pulse?.cancel()
        binding.running.visible(false); binding.result.visible(true)
        val refresh = requireArguments().getString(ImportJobViewModel.ARG_MODE) == ImportJobViewModel.MODE_REFRESH
        binding.rTitle.text = getString(if (refresh) R.string.import_updated else R.string.import_added, requireArguments().getString("name"))
        binding.stats.n1.text = "%,d".format(p.channels); binding.stats.l1.setText(R.string.channels)
        binding.stats.n2.text = "%,d".format(p.groups); binding.stats.l2.setText(R.string.groups)
        binding.stats.c3.visible(false)
        binding.buttons.btn1.setText(R.string.go_home)
        binding.buttons.btn2.setText(R.string.view_channels)
        binding.buttons.btn1.setOnClickListener { dismiss(); if (!refresh) { MainActivity.openTab(requireContext(), MainActivity.Tab.HOME); activity?.takeIf { it !is MainActivity }?.finish() } }
        binding.buttons.btn2.setOnClickListener {
            val ctx = requireContext(); dismiss()
            Nav.playlist(ctx, p.playlistId)
            if (!refresh) activity?.takeIf { it !is MainActivity }?.finish()
        }
    }

    private fun failed(f: ImportProgress.Failed) {
        pulse?.cancel()
        binding.running.visible(false); binding.result.visible(true)
        binding.stats.root.visible(false)
        binding.rIconWrap.setBackgroundResource(R.drawable.bg_hero_icon_danger)
        binding.rIcon.setImageResource(R.drawable.ic_alert)
        binding.rIcon.setColorFilter(requireContext().getColor(R.color.danger_text))
        binding.rTitle.setText(R.string.import_failed_title)
        binding.rBody.visible(true)
        binding.rBody.text = when (f.reason) {
            ImportError.HTTP -> getString(R.string.import_failed_http, f.detail ?: "HTTP")
            ImportError.NETWORK -> getString(R.string.import_failed_network)
            ImportError.EMPTY, ImportError.INVALID -> getString(R.string.import_failed_empty)
            ImportError.STREAM -> getString(R.string.import_is_stream)
        }
        if (f.reason == ImportError.STREAM) {
            val url = requireArguments().getString("url").orEmpty()
            binding.rTitle.setText(R.string.import_is_stream_title)
            binding.buttons.btn1.setText(R.string.cancel)
            binding.buttons.btn2.setText(R.string.play_as_stream)
            binding.buttons.btn1.setOnClickListener { dismiss() }
            binding.buttons.btn2.setOnClickListener {
                val ctx = requireContext(); dismiss()
                Nav.play(ctx, com.iptvplayer.app.ui.player.PlayRequest.Url(url, url.substringAfterLast('/').substringBefore('?')))
            }
            return
        }
        binding.buttons.btn1.setText(R.string.fix_link)
        binding.buttons.btn2.setText(R.string.view_guide)
        binding.buttons.btn1.setOnClickListener { dismiss() }
        binding.buttons.btn2.setOnClickListener { val ctx = requireContext(); dismiss(); Nav.howTo(ctx, "url") }
    }

    override fun onDestroyView() { pulse?.cancel(); super.onDestroyView() }

    companion object {
        private fun show(fm: FragmentManager, args: Bundle) = ImportProgressDialog().apply { arguments = args }.show(fm, "import_progress")

        fun addUrl(fm: FragmentManager, name: String, url: String, lock: Boolean, auto: Boolean) =
            show(fm, bundleOf(ImportJobViewModel.ARG_MODE to ImportJobViewModel.MODE_URL, "name" to name, "url" to url, "lock" to lock, "auto" to auto))

        fun refresh(fm: FragmentManager, id: Long, name: String) =
            show(fm, bundleOf(ImportJobViewModel.ARG_MODE to ImportJobViewModel.MODE_REFRESH, "id" to id, "name" to name))

        fun addFiles(fm: FragmentManager, names: List<String>, uris: List<Uri>, lock: Boolean) =
            show(fm, bundleOf(ImportJobViewModel.ARG_MODE to ImportJobViewModel.MODE_FILES, "name" to names.first(),
                "names" to ArrayList(names), "uris" to ArrayList(uris.map { it.toString() }), "lock" to lock))
    }
}
