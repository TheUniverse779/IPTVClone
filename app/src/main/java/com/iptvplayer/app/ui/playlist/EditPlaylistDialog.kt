package com.iptvplayer.app.ui.playlist

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseDialog
import com.iptvplayer.app.data.database.SourceType
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.databinding.DialogEditPlaylistBinding
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.util.copyText
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditPlaylistDialog : BaseDialog<DialogEditPlaylistBinding>(DialogEditPlaylistBinding::inflate) {
    @Inject lateinit var repo: PlaylistRepository

    override fun setup(savedInstanceState: Bundle?) {
        val id = requireArguments().getLong("id")
        binding.lockRow.swTitle.setText(R.string.lock_with_passcode)
        binding.autoRow.swTitle.setText(R.string.auto_update)
        binding.buttons.btn1.setText(R.string.cancel)
        binding.buttons.btn2.setText(R.string.save)
        binding.buttons.btn1.setOnClickListener { dismiss() }

        lifecycleScope.launch {
            val p = repo.get(id) ?: return@launch dismiss()
            if (savedInstanceState == null) {
                binding.etName.setText(p.name)
                binding.lockRow.sw.isChecked = p.isLocked
                binding.autoRow.sw.isChecked = p.autoUpdate
            }
            binding.tvUrl.text = p.url
            binding.autoRow.root.visible(p.sourceType == SourceType.URL)
            binding.btnCopy.setOnClickListener { requireContext().copyText(p.url); requireContext().toast(R.string.copied) }
            binding.buttons.btn2.setOnClickListener {
                val lock = binding.lockRow.sw.isChecked
                // Turning a lock on needs a passcode to exist; turning it off needs the passcode.
                if (lock != p.isLocked) PasscodeDialog.show(childFragmentManager, KEY_PASS, if (lock) PasscodeDialog.MODE_ENSURE else PasscodeDialog.MODE_ENTER, p.name)
                else save()
            }
        }
        childFragmentManager.setFragmentResultListener(KEY_PASS, this) { _, r -> if (r.getBoolean(PasscodeDialog.KEY_OK)) save() }
    }

    private fun save() = lifecycleScope.launch {
        val p = repo.get(requireArguments().getLong("id")) ?: return@launch
        repo.update(p.copy(name = binding.etName.text.toString().trim().ifEmpty { p.name }, isLocked = binding.lockRow.sw.isChecked, autoUpdate = binding.autoRow.sw.isChecked))
        context?.toast(R.string.changes_saved)
        dismiss()
    }

    companion object {
        private const val KEY_PASS = "edit_pl_pass"
        fun show(fm: FragmentManager, id: Long) = EditPlaylistDialog().apply { arguments = bundleOf("id" to id) }.show(fm, "edit_playlist")
    }
}
