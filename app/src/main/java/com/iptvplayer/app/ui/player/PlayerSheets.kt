package com.iptvplayer.app.ui.player

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.media3.common.C
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseBottomSheet
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.databinding.DialogConfirmBinding
import com.iptvplayer.app.databinding.SheetListBinding
import com.iptvplayer.app.player.AspectMode
import com.iptvplayer.app.player.PlayerManager
import com.iptvplayer.app.ui.common.SheetRows
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Timer / subtitles / audio+quality / aspect option sheets, and the cast dialog. */
@AndroidEntryPoint
class PlayerOptionSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {
    @Inject lateinit var manager: PlayerManager

    override fun setup(savedInstanceState: Bundle?) {
        val kind = requireArguments().getString("kind")!!
        val c = binding.container
        fun close() = dismiss()
        when (kind) {
            "timer" -> {
                SheetRows.header(binding, getString(R.string.sleep_timer)) { close() }
                val on = manager.sleepLeft.value != null
                SheetRows.option(c, getString(R.string.off), checked = !on) { manager.setSleepTimer(null); close() }
                listOf(15 to R.string.min_15, 30 to R.string.min_30, 45 to R.string.min_45, 60 to R.string.hour_1).forEach { (m, label) ->
                    SheetRows.option(c, getString(label)) {
                        manager.setSleepTimer(m); requireContext().toast(getString(R.string.timer_set, m)); close()
                    }
                }
            }
            "subs" -> {
                SheetRows.header(binding, getString(R.string.subtitles)) { close() }
                val opts = manager.trackOptions(C.TRACK_TYPE_TEXT)
                val enabled = manager.textEnabled()
                SheetRows.option(c, getString(R.string.off), checked = !enabled || opts.none { it.selected }) { manager.selectTrack(C.TRACK_TYPE_TEXT, null); close() }
                opts.forEach { o -> SheetRows.option(c, o.label, checked = enabled && o.selected) { manager.selectTrack(C.TRACK_TYPE_TEXT, o); close() } }
            }
            "audio" -> {
                SheetRows.header(binding, getString(R.string.audio_and_quality)) { close() }
                val audio = manager.trackOptions(C.TRACK_TYPE_AUDIO)
                if (audio.isEmpty()) SheetRows.option(c, getString(R.string.auto), checked = true) { close() }
                audio.forEach { o -> SheetRows.option(c, o.label, checked = o.selected) { manager.selectTrack(C.TRACK_TYPE_AUDIO, o); close() } }
                val video = manager.trackOptions(C.TRACK_TYPE_VIDEO)
                if (video.size > 1) {
                    SheetRows.divider(c)
                    val header = android.widget.TextView(requireContext()).apply { setText(R.string.video_quality); setTextAppearance(R.style.Text_Hint); setPadding(24, 12, 24, 4) }
                    c.addView(header)
                    val overridden = manager.player.trackSelectionParameters.overrides.keys.any { it.type == C.TRACK_TYPE_VIDEO }
                    SheetRows.option(c, getString(R.string.auto), checked = !overridden) { manager.selectTrack(C.TRACK_TYPE_VIDEO, null); close() }
                    video.sortedByDescending { it.label.removeSuffix("p").toIntOrNull() ?: 0 }.forEach { o ->
                        SheetRows.option(c, o.label, checked = overridden && o.selected) { manager.selectTrack(C.TRACK_TYPE_VIDEO, o); close() }
                    }
                }
            }
            "aspect" -> {
                SheetRows.header(binding, getString(R.string.aspect)) { close() }
                val cur = AspectMode.valueOf(requireArguments().getString("cur")!!)
                listOf(
                    Triple(AspectMode.FIT, R.string.aspect_fit, R.string.aspect_fit_sub), Triple(AspectMode.FILL, R.string.aspect_fill, R.string.aspect_fill_sub),
                    Triple(AspectMode.ZOOM, R.string.aspect_zoom, 0), Triple(AspectMode.R16_9, R.string.aspect_16_9, 0), Triple(AspectMode.R4_3, R.string.aspect_4_3, 0),
                ).forEach { (m, t, s) ->
                    SheetRows.option(c, getString(t), if (s != 0) getString(s) else null, checked = m == cur) {
                        parentFragmentManager.setFragmentResult(PlayerSheets.RESULT_ASPECT, bundleOf("v" to m.name)); close()
                    }
                }
            }
        }
    }
}

/** Opens the system cast / wireless display settings (no ads, unlike the original). */
class CastDialog : BaseDialog<DialogConfirmBinding>(DialogConfirmBinding::inflate) {
    override fun setup(savedInstanceState: Bundle?) {
        binding.heroWrap.visible(true)
        binding.heroIcon.setImageResource(R.drawable.ic_cast)
        binding.title.setText(R.string.cast_title)
        binding.message.setText(R.string.cast_body)
        binding.btnPositive.setText(R.string.open_settings)
        binding.btnNegative.setOnClickListener { dismiss() }
        binding.btnPositive.setOnClickListener {
            dismiss()
            val ok = listOf(Settings.ACTION_CAST_SETTINGS, "android.settings.WIFI_DISPLAY_SETTINGS").any { action ->
                runCatching { startActivity(Intent(action)) }.isSuccess
            }
            if (!ok) requireContext().toast(R.string.device_not_supported)
        }
    }
}

object PlayerSheets {
    const val RESULT_ASPECT = "player_aspect"
    private fun show(fm: FragmentManager, kind: String, extra: Bundle = Bundle()) =
        PlayerOptionSheet().apply { arguments = bundleOf("kind" to kind).apply { putAll(extra) } }.show(fm, "player_$kind")

    fun timer(fm: FragmentManager) = show(fm, "timer")
    fun subtitles(fm: FragmentManager) = show(fm, "subs")
    fun audio(fm: FragmentManager) = show(fm, "audio")
    fun aspect(fm: FragmentManager, cur: AspectMode) = show(fm, "aspect", bundleOf("cur" to cur.name))
    fun cast(fm: FragmentManager) = CastDialog().show(fm, "cast")
}
