package com.iptvplayer.app.ui.importer

import com.iptvplayer.app.Features
import android.os.Bundle
import android.text.Html
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.ActivityImportBinding
import com.iptvplayer.app.databinding.FormImportXtreamBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.guide.WebGuideSheet
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.ui.xtream.XtreamSyncDialog
import com.iptvplayer.app.util.UrlUtils
import com.iptvplayer.app.util.clipboardText
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(private val playlists: PlaylistRepository, private val xtream: XtreamRepository) : ViewModel() {
    suspend fun defaultPlaylistName() = playlists.nextDefaultName()
    suspend fun defaultProfileName() = xtream.nextDefaultName()
    fun saveSingle(name: String, url: String) = viewModelScope.launch { playlists.addSingle(name, url) }
}

/**
 * Import URL / Xtream / Single stream. The three tabs are <include>d forms toggled in place
 * (no fragments), matching the approved design.
 */
@AndroidEntryPoint
class ImportActivity : BaseActivity<ActivityImportBinding>(ActivityImportBinding::inflate) {
    private val vm: ImportViewModel by viewModels()
    private var tab = "url"

    override fun setup(savedInstanceState: Bundle?) {
        tab = savedInstanceState?.getString(Nav.EXTRA_TAB) ?: intent.getStringExtra(Nav.EXTRA_TAB) ?: "url"
        binding.toolbar.btnBack.setOnClickListener { finish() }
        com.iptvplayer.app.ui.common.addToolbarButton(binding.toolbar.actions, R.drawable.ic_help, desc = R.string.help) {
            Nav.howTo(this, if (tab == "single") "single" else tab)
        }
        with(binding.seg) {
            s1.setText(R.string.tab_url); s2.setText(R.string.tab_xt); s3.setText(R.string.tab_single)
            s1.setOnClickListener { selectTab("url") }; s2.setOnClickListener { selectTab("xtream") }; s3.setOnClickListener { selectTab("single") }
        }
        setupUrl(); setupXtream(binding.formXtream, this, supportFragmentManager); wireXtreamSubmit(); setupSingle()
        intent.getStringExtra(Nav.EXTRA_URL)?.let { binding.formUrl.etUrl.setText(it) }
        selectTab(tab)

        supportFragmentManager.setFragmentResultListener(KEY_LOCK_THEN_IMPORT, this) { _, r -> if (r.getBoolean(PasscodeDialog.KEY_OK)) startUrlImport() }
        supportFragmentManager.setFragmentResultListener(WebGuideSheet.RESULT_LINK, this) { _, r ->
            val link = r.getString(WebGuideSheet.KEY_URL) ?: return@setFragmentResultListener
            if (tab == "single") binding.formSingle.etStream.setText(link) else { selectTab("url"); binding.formUrl.etUrl.setText(link) }
            toast(R.string.link_filled)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Nav.EXTRA_URL)?.let { selectTab("url"); binding.formUrl.etUrl.setText(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString(Nav.EXTRA_TAB, tab) }

    private fun selectTab(t: String) {
        tab = t
        binding.seg.s1.isSelected = t == "url"; binding.seg.s2.isSelected = t == "xtream"; binding.seg.s3.isSelected = t == "single"
        binding.formUrl.root.visible(t == "url"); binding.formXtream.root.visible(t == "xtream"); binding.formSingle.root.visible(t == "single")
        binding.toolbar.tvTitle.setText(when (t) { "xtream" -> R.string.title_import_xtream; "single" -> R.string.title_import_single; else -> R.string.title_import_url })
    }

    // ---------------- URL ----------------
    private fun setupUrl() = with(binding.formUrl) {
        lifecycleScope.launch { etName.hint = vm.defaultPlaylistName() }
        swAuto.isChecked = true
        etUrlPaste.setOnClickListener { paste(etUrl) }
        guideUrl.glIcon.setImageResource(R.drawable.ic_globe)
        guideUrl.glText.text = Html.fromHtml(getString(R.string.guide_strip_iptv), Html.FROM_HTML_MODE_COMPACT)
        guideUrl.root.setOnClickListener { WebGuideSheet.show(this@ImportActivity, supportFragmentManager, "iptv") }
        tvLicense.text = Html.fromHtml(getString(R.string.license_note), Html.FROM_HTML_MODE_COMPACT)
        tvLicense.setOnClickListener { com.iptvplayer.app.ui.disclaimer.DisclaimerActivity.start(this@ImportActivity, true) }
        btnAdd.setOnClickListener { submitUrl() }
    }

    private fun submitUrl() = with(binding.formUrl) {
        val url = etUrl.text.toString().trim()
        val err = when {
            url.isEmpty() -> R.string.err_url_empty
            !url.contains("://") && !url.lowercase().let { it.endsWith(".m3u") || it.endsWith(".m3u8") || it.endsWith(".json") } -> R.string.err_url_format
            else -> null
        }
        setError(etUrlBox, etUrlHelper, err, R.string.playlist_url_helper)
        if (err != null) return@with
        if (swLock.isChecked) PasscodeDialog.show(supportFragmentManager, KEY_LOCK_THEN_IMPORT, PasscodeDialog.MODE_ENSURE)
        else startUrlImport()
    }

    private fun startUrlImport() = with(binding.formUrl) {
        val name = etName.text.toString().trim().ifEmpty { etName.hint.toString() }
        ImportProgressDialog.addUrl(supportFragmentManager, name, etUrl.text.toString().trim(), swLock.isChecked, swAuto.isChecked)
    }

    // ---------------- Single ----------------
    private fun setupSingle() = with(binding.formSingle) {
        swSave.isChecked = true
        etStreamPaste.setOnClickListener { paste(etStream) }
        guideSingle.glIcon.setImageResource(R.drawable.ic_globe)
        guideSingle.glText.text = Html.fromHtml(getString(R.string.guide_strip_single), Html.FROM_HTML_MODE_COMPACT)
        guideSingle.root.setOnClickListener { WebGuideSheet.show(this@ImportActivity, supportFragmentManager, "single") }
        btnPlay.setOnClickListener {
            val url = etStream.text.toString().trim()
            if (url.isEmpty()) { setError(etStreamBox, etStreamHelper, R.string.err_url_empty, R.string.stream_link_helper); return@setOnClickListener }
            val name = etStreamName.text.toString().trim().ifEmpty { UrlUtils.host(url) }
            if (swSave.isChecked) vm.saveSingle(name, url)
            Nav.play(this@ImportActivity, PlayRequest.Url(url, name))
        }
    }

    private fun paste(target: EditText) {
        val clip = clipboardText()?.trim()
        if (clip.isNullOrEmpty()) { toast(R.string.clipboard_empty); return }
        target.setText(UrlUtils.firstUrl(clip) ?: clip)
        target.setSelection(target.text.length)
        toast(R.string.pasted)
    }

    companion object {
        private const val KEY_LOCK_THEN_IMPORT = "import_lock"

        fun setError(box: android.view.View, helper: TextView, err: Int?, normal: Int) {
            box.setBackgroundResource(if (err != null) R.drawable.bg_input_error else R.drawable.bg_input)
            helper.setText(err ?: normal)
            helper.setTextColor(helper.context.getColor(if (err != null) R.color.danger_text else R.color.text_3))
        }

        /**
         * Shared by ImportActivity (Xtream tab) and AddEditProfileActivity: smart paste, show/hide
         * password, validation, then [XtreamSyncDialog].
         */
        fun setupXtream(f: FormImportXtreamBinding, act: BaseActivity<*>, fm: androidx.fragment.app.FragmentManager, editingId: String? = null) = with(f) {
            etServerPaste.setOnClickListener {
                val creds = act.clipboardText()?.let { UrlUtils.parseXtream(it) }
                if (creds == null) { act.toast(R.string.split_fail); return@setOnClickListener }
                etServer.setText(creds.server); etUser.setText(creds.username); etPass.setText(creds.password)
                act.toast(R.string.split_ok)
            }
            // A full get.php / stream link typed or pasted straight into Server URL is split too.
            fun splitServerField() {
                val t = etServer.text.toString().trim()
                if (!t.contains("username=") && !t.contains("/live/") && !t.contains("User Name:")) return
                val creds = UrlUtils.parseXtream(t) ?: return
                etServer.setText(creds.server); etServer.setSelection(etServer.text.length)
                if (etUser.text.isNullOrBlank()) etUser.setText(creds.username)
                if (etPass.text.isNullOrEmpty()) etPass.setText(creds.password)
                act.toast(R.string.split_ok)
            }
            etServer.doAfterTextChanged { e -> if (e != null && e.length > 20 && (e.contains("password=") || e.contains("Password:"))) etServer.post { splitServerField() } }
            etServer.setOnFocusChangeListener { _, has -> if (!has) splitServerField() }
            var shown = false
            etPassEye.setOnClickListener {
                shown = !shown
                etPass.inputType = InputType.TYPE_CLASS_TEXT or if (shown) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD else InputType.TYPE_TEXT_VARIATION_PASSWORD
                etPass.setSelection(etPass.text.length)
                etPassEye.setImageResource(if (shown) R.drawable.ic_eye_off else R.drawable.ic_eye)
            }
            communityPick.glIcon.setImageResource(R.drawable.ic_share)
            communityPick.glText.text = Html.fromHtml(act.getString(R.string.community_xtream_pick), Html.FROM_HTML_MODE_COMPACT)
            communityPick.root.setOnClickListener { Nav.community(act, "xtream") }
            communityPick.root.visible(Features.COMMUNITY && editingId == null)
            guideXtream.glIcon.setImageResource(R.drawable.ic_help)
            guideXtream.glText.text = Html.fromHtml(act.getString(R.string.guide_strip_xtream), Html.FROM_HTML_MODE_COMPACT)
            guideXtream.root.setOnClickListener { WebGuideSheet.show(act, fm, "xtream") }
            btnLogin.setText(if (editingId == null) R.string.login_sync else R.string.save_resync)
        }

        /** Validates the Xtream form; returns null (and shows the error) if invalid. */
        fun readXtream(f: FormImportXtreamBinding): Array<String>? = with(f) {
            val srv = etServer.text.toString().trim(); val user = etUser.text.toString().trim(); val pass = etPass.text.toString()
            val err = if (srv.isEmpty() || user.isEmpty() || pass.isEmpty()) R.string.err_xtream_fields else null
            setError(etServerBox, etServerHelper, err, R.string.server_helper)
            if (err != null) return null
            arrayOf(etProfileName.text.toString().trim(), UrlUtils.normalizeServer(srv), user, pass)
        }
    }

    // ---------------- Xtream (add) ----------------
    private fun wireXtreamSubmit() {
        lifecycleScope.launch { binding.formXtream.etProfileName.hint = vm.defaultProfileName() }
        binding.formXtream.btnLogin.setOnClickListener { submitXtream() }
        supportFragmentManager.setFragmentResultListener(KEY_LOCK_THEN_XTREAM, this) { _, r -> if (r.getBoolean(PasscodeDialog.KEY_OK)) startXtream() }
    }

    private fun submitXtream() {
        readXtream(binding.formXtream) ?: return
        if (binding.formXtream.swLockProfile.isChecked) PasscodeDialog.show(supportFragmentManager, KEY_LOCK_THEN_XTREAM, PasscodeDialog.MODE_ENSURE)
        else startXtream()
    }

    private fun startXtream() {
        val (name, srv, user, pass) = readXtream(binding.formXtream) ?: return
        XtreamSyncDialog.add(supportFragmentManager, name.ifEmpty { binding.formXtream.etProfileName.hint.toString() }, srv, user, pass, binding.formXtream.swLockProfile.isChecked)
    }
}

private const val KEY_LOCK_THEN_XTREAM = "import_xtream_lock"
