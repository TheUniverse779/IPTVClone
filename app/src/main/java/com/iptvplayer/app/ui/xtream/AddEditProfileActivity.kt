package com.iptvplayer.app.ui.xtream

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.ActivityAddEditProfileBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.ConfirmDialog
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.common.isExpired
import com.iptvplayer.app.ui.importer.ImportActivity
import com.iptvplayer.app.ui.main.MainActivity
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileEditViewModel @Inject constructor(val repo: XtreamRepository, handle: SavedStateHandle) : ViewModel() {
    val profileId: String? = handle[Nav.EXTRA_PROFILE]
    var colorIndex = 0
}

/** Add a new Xtream profile, or edit one (Save & re-sync / Delete). */
@AndroidEntryPoint
class AddEditProfileActivity : BaseActivity<ActivityAddEditProfileBinding>(ActivityAddEditProfileBinding::inflate) {
    private val vm: ProfileEditViewModel by viewModels()
    private var profile: XtreamProfileEntity? = null

    override fun setup(savedInstanceState: Bundle?) {
        val editing = vm.profileId != null
        binding.toolbar.tvTitle.setText(if (editing) R.string.edit_profile else R.string.add_profile)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_help, desc = R.string.help) { Nav.howTo(this, "xtream") }
        ImportActivity.setupXtream(binding.form, this, supportFragmentManager, vm.profileId)
        binding.btnColor.setOnClickListener { vm.colorIndex++; renderAvatar() }
        binding.form.etProfileName.doAfterTextChanged { renderAvatar() }

        // Prefill from Community ("Use this link") when adding.
        intent.getStringExtra(Nav.EXTRA_SRV)?.let { binding.form.etServer.setText(it) }
        intent.getStringExtra(Nav.EXTRA_USER)?.let { binding.form.etUser.setText(it) }
        intent.getStringExtra(Nav.EXTRA_PASS)?.let { binding.form.etPass.setText(it) }
        intent.getStringExtra(Nav.EXTRA_NAME)?.let { binding.form.etProfileName.setText(it) }

        lifecycleScope.launch {
            if (editing) {
                val p = vm.repo.profile(vm.profileId!!) ?: return@launch finish()
                profile = p
                vm.colorIndex = XtreamRepository.AVATAR_COLORS.indexOf(p.avatarColor).coerceAtLeast(0)
                if (savedInstanceState == null) with(binding.form) {
                    etProfileName.setText(p.name); etServer.setText(p.serverUrl); etUser.setText(p.username); etPass.setText(p.password)
                    swLockProfile.isChecked = p.passcodeLocked
                }
                bindAccountCard(p)
            } else binding.form.etProfileName.hint = vm.repo.nextDefaultName()
            renderAvatar()
        }

        binding.form.btnLogin.setOnClickListener { submit() }
        binding.form.btnDelete.visible(editing)
        binding.form.btnDelete.setOnClickListener {
            ConfirmDialog.show(supportFragmentManager, KEY_DELETE, getString(R.string.delete_profile_q, profile?.name.orEmpty()), getString(R.string.delete_profile_msg), ok = getString(R.string.delete), danger = true)
        }
        supportFragmentManager.setFragmentResultListener(KEY_DELETE, this) { _, r ->
            if (r.getInt(ConfirmDialog.KEY_WHICH) == 0) lifecycleScope.launch {
                vm.repo.deleteProfile(vm.profileId!!); toast(R.string.profile_deleted)
                MainActivity.openTab(this@AddEditProfileActivity, MainActivity.Tab.XTREAM); finish()
            }
        }
        supportFragmentManager.setFragmentResultListener(KEY_LOCK, this) { _, r -> if (r.getBoolean(PasscodeDialog.KEY_OK)) save() }
    }

    private fun renderAvatar() {
        val colors = XtreamRepository.AVATAR_COLORS
        val name = binding.form.etProfileName.text.toString().ifBlank { profile?.name.orEmpty() }
        LogoUtil.avatar(binding.avatar, name.ifBlank { " " }, colors[vm.colorIndex % colors.size], 24)
        binding.avatarIcon.visible(name.isBlank())
        if (name.isBlank()) binding.avatar.text = ""
    }

    private fun bindAccountCard(p: XtreamProfileEntity) = with(binding.form) {
        accountCard.visible(true)
        val expired = isExpired(p)
        accountSub.text = listOf(if (expired) getString(R.string.expired_on, TimeFmt.date(p.expDate)) else getString(R.string.valid_until, TimeFmt.date(p.expDate)),
            getString(R.string.last_sync, TimeFmt.ago(this@AddEditProfileActivity, p.lastSync))).joinToString(" · ")
        accountStatus.setText(if (expired) R.string.status_expired else R.string.status_active)
        accountStatus.setBackgroundResource(if (expired) R.drawable.bg_status_bad else R.drawable.bg_status_ok)
        accountStatus.setTextColor(getColor(if (expired) R.color.danger_text else R.color.ok))
        accountCard.setOnClickListener { AccountSheet.show(supportFragmentManager, p.id) }
    }

    private fun submit() {
        ImportActivity.readXtream(binding.form) ?: return
        val lock = binding.form.swLockProfile.isChecked
        val lockChanged = lock != (profile?.passcodeLocked ?: false)
        if (lock && lockChanged) PasscodeDialog.show(supportFragmentManager, KEY_LOCK, PasscodeDialog.MODE_ENSURE)
        else if (!lock && lockChanged) PasscodeDialog.show(supportFragmentManager, KEY_LOCK, PasscodeDialog.MODE_ENTER, profile?.name)
        else save()
    }

    private fun save() {
        val (name, srv, user, pass) = ImportActivity.readXtream(binding.form) ?: return
        val lock = binding.form.swLockProfile.isChecked
        val color = XtreamRepository.AVATAR_COLORS[vm.colorIndex % XtreamRepository.AVATAR_COLORS.size]
        val p = profile
        if (p == null) {
            XtreamSyncDialog.add(supportFragmentManager, name.ifEmpty { binding.form.etProfileName.hint.toString() }, srv, user, pass, lock, color)
            return
        }
        lifecycleScope.launch {
            val credsChanged = p.serverUrl != srv || p.username != user || p.password != pass
            vm.repo.updateProfileInfo(p.copy(name = name.ifEmpty { p.name }, serverUrl = srv, username = user, password = pass, passcodeLocked = lock, avatarColor = color))
            if (credsChanged) XtreamSyncDialog.resync(supportFragmentManager, p.id) else { toast(R.string.changes_saved); finish() }
        }
    }

    companion object { private const val KEY_DELETE = "profile_delete"; private const val KEY_LOCK = "profile_lock" }
}
