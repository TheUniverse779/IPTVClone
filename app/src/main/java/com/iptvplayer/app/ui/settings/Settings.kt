package com.iptvplayer.app.ui.settings

import com.iptvplayer.app.ui.main.padForMainTabs
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.iptvplayer.app.BuildConfig
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.AppDatabase
import com.iptvplayer.app.data.database.PlaylistEntity
import com.iptvplayer.app.data.database.SourceType
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.data.datastore.UserAgentMode
import com.iptvplayer.app.data.network.HttpClients
import com.iptvplayer.app.databinding.ActivityFeedbackBinding
import com.iptvplayer.app.databinding.ActivitySettingsBinding
import com.iptvplayer.app.databinding.FragmentSettingsBinding
import com.iptvplayer.app.databinding.ItemSettingsRowBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.disclaimer.DisclaimerActivity
import com.iptvplayer.app.ui.importer.ImportProgressDialog
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.openCustomTab
import com.iptvplayer.app.util.shareText
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Shown as MainActivity's 4th tab when Sport is off, and inside SettingsActivity. */
@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(FragmentSettingsBinding::inflate) {
    @Inject lateinit var store: SettingsStore
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var http: HttpClients
    @Inject lateinit var gson: Gson

    private val backup = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let { writeBackup(it) } }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { readBackup(it) } }

    override fun setup(savedInstanceState: Bundle?) {
        val standalone = arguments?.getBoolean(ARG_STANDALONE) ?: false
        binding.toolbar.tvTitle.setText(R.string.settings)
        binding.toolbar.btnBack.visible(standalone)
        binding.toolbar.btnBack.setOnClickListener { requireActivity().finish() }
        binding.root.setPadding(0, 0, 0, if (standalone) 24.dp else 0)
        if (!standalone) padForMainTabs(binding.root.parent as android.view.View)
        binding.version.text = getString(R.string.version_x, BuildConfig.VERSION_NAME)
        childFragmentManager.setFragmentResultListener(KEY_PASS, viewLifecycleOwner) { _, _ -> }
        collect(store.settings) { render(it) }
    }

    private fun render(s: com.iptvplayer.app.data.datastore.Settings) {
        val g = binding.groups
        g.removeAllViews()
        group(R.string.set_general) {
            row(it, R.drawable.ic_lang, R.string.set_language, value = currentLanguageName()) { pickLanguage() }
            row(it, R.drawable.ic_lock, R.string.set_passcode, value = getString(if (s.hasPasscode) R.string.passcode_on else R.string.passcode_off)) {
                PasscodeDialog.show(childFragmentManager, KEY_PASS, PasscodeDialog.MODE_CREATE)
            }
        }
        group(R.string.set_playback) {
            switch(it, R.drawable.ic_pip, R.string.set_auto_pip, s.autoPip) { v -> lifecycleScope.launch { store.setAutoPip(v) } }
            switch(it, R.drawable.ic_audio, R.string.set_bg_audio, s.backgroundAudio) { v -> lifecycleScope.launch { store.setBackgroundAudio(v) } }
            row(it, R.drawable.ic_globe, R.string.set_user_agent, value = s.userAgentMode.label()) { pickUserAgent(s.userAgentMode) }
            row(it, R.drawable.ic_bolt, R.string.set_decoder, value = getString(if (s.softwareDecoder) R.string.decoder_sw else R.string.decoder_hw)) {
                lifecycleScope.launch { store.setSoftwareDecoder(!s.softwareDecoder) }
            }
        }
        group(R.string.set_data) {
            row(it, R.drawable.ic_upload, R.string.set_backup) { backup.launch("iptv-playlists.json") }
            row(it, R.drawable.ic_folder, R.string.set_restore) { restore.launch(arrayOf("application/json", "text/plain", "*/*")) }
            row(it, R.drawable.ic_trash, R.string.set_clear_cache) {
                lifecycleScope.launch { withContext(Dispatchers.IO) { Glide.get(requireContext()).clearDiskCache() }; Glide.get(requireContext()).clearMemory(); requireContext().toast(R.string.cache_cleared) }
            }
        }
        group(R.string.set_support) {
            row(it, R.drawable.ic_help, R.string.set_howto) { Nav.howTo(requireContext()) }
            row(it, R.drawable.ic_chat, R.string.set_faq) { Nav.faq(requireContext()) }
            row(it, R.drawable.ic_mail, R.string.set_feedback) { Nav.feedback(requireContext()) }
            row(it, R.drawable.ic_star, R.string.set_rate) { RateDialog().show(childFragmentManager, "rate") }
            row(it, R.drawable.ic_share, R.string.set_share) { requireContext().shareText(getString(R.string.share_app_text, requireContext().packageName)) }
        }
        group(R.string.set_legal) {
            row(it, R.drawable.ic_shield, R.string.set_privacy, trail = R.drawable.ic_external) { requireContext().openCustomTab(PRIVACY_URL) }
            row(it, R.drawable.ic_doc, R.string.set_terms, trail = R.drawable.ic_external) { requireContext().openCustomTab(TERMS_URL) }
            row(it, R.drawable.ic_info, R.string.set_license) { DisclaimerActivity.start(requireContext(), true) }
        }
    }

    private fun group(title: Int, build: (LinearLayout) -> Unit) {
        binding.groups.addView(TextView(requireContext()).apply {
            setText(title); setTextAppearance(R.style.Text_Hint); textSize = 13f; setPadding(20.dp, 8.dp, 20.dp, 8.dp)
        })
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundResource(R.drawable.bg_list_card); clipToOutline = true
        }
        binding.groups.addView(box, LinearLayout.LayoutParams(-1, -2).apply { marginStart = 16.dp; marginEnd = 16.dp; bottomMargin = 16.dp })
        build(box)
    }

    private fun row(box: LinearLayout, icon: Int, title: Int, value: String? = null, trail: Int = R.drawable.ic_chevron, onClick: () -> Unit): ItemSettingsRowBinding {
        if (box.childCount > 0) box.addView(android.view.View(requireContext()).apply { setBackgroundColor(requireContext().getColor(R.color.surface_2)) }, LinearLayout.LayoutParams(-1, 1))
        val r = ItemSettingsRowBinding.inflate(LayoutInflater.from(requireContext()), box, false)
        r.icon.setImageResource(icon); r.title.setText(title)
        r.value.visible(value != null); r.value.text = value
        r.trail.setImageResource(trail)
        r.root.setOnClickListener { onClick() }
        box.addView(r.root)
        return r
    }

    private fun switch(box: LinearLayout, icon: Int, title: Int, on: Boolean, onChange: (Boolean) -> Unit) {
        val r = row(box, icon, title) {}
        r.trail.visible(false); r.sw.visible(true); r.sw.isChecked = on
        r.sw.setOnCheckedChangeListener { _, v -> onChange(v) }
        r.root.setOnClickListener { r.sw.toggle() }
    }

    private fun UserAgentMode.label() = when (this) { UserAgentMode.APP -> "App"; UserAgentMode.VLC -> "VLC"; UserAgentMode.CHROME -> "Chrome" }

    private fun pickUserAgent(cur: UserAgentMode) {
        val modes = UserAgentMode.entries
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.set_user_agent)
            .setSingleChoiceItems(modes.map { it.label() }.toTypedArray(), modes.indexOf(cur)) { d, i ->
                lifecycleScope.launch { store.setUserAgentMode(modes[i]); http.refreshUserAgent() }; d.dismiss()
            }.show()
    }

    /**
     * Placeholder language picker (per-app locale). Your own Language screen replaces this;
     * wire it in [pickLanguage].
     */
    private fun pickLanguage() {
        // No "Auto" entry: the app defaults to English (App.DEFAULT_LANGUAGE), not the phone language.
        val tags = listOf("en" to "English", "vi" to "Tiếng Việt")
        val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.language_title)
            .setSingleChoiceItems(tags.map { it.second }.toTypedArray(), tags.indexOfFirst { it.first == cur }.coerceAtLeast(0)) { d, i ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[i].first)); d.dismiss()
            }.show()
    }

    private fun currentLanguageName(): String {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return if (tag.startsWith("vi")) "Tiếng Việt" else "English"
    }

    // ---- backup / restore (URL playlists + Xtream profiles + single streams) ----
    private data class Backup(val playlists: List<Map<String, Any?>>, val singles: List<Map<String, String>>, val xtream: List<Map<String, String>>)

    private fun writeBackup(uri: Uri) = lifecycleScope.launch {
        val b = withContext(Dispatchers.IO) {
            Backup(
                db.query("SELECT name, url, sourceType, autoUpdate FROM playlist", null).use { c -> buildList {
                    while (c.moveToNext()) add(mapOf("name" to c.getString(0), "url" to c.getString(1), "type" to c.getString(2), "autoUpdate" to (c.getInt(3) == 1)))
                } },
                db.query("SELECT name, url FROM single_stream", null).use { c -> buildList { while (c.moveToNext()) add(mapOf("name" to c.getString(0), "url" to c.getString(1))) } },
                db.query("SELECT name, serverUrl, username, password FROM xtream_profile", null).use { c ->
                    buildList { while (c.moveToNext()) add(mapOf("name" to c.getString(0), "server" to c.getString(1), "user" to c.getString(2), "pass" to c.getString(3))) }
                },
            )
        }
        runCatching { requireContext().contentResolver.openOutputStream(uri)!!.use { it.write(gson.toJson(b).toByteArray()) } }
            .onSuccess { requireContext().toast(R.string.backup_done) }
    }

    /** Re-imports URL playlists (downloading them again), single streams; Xtream profiles open the add form. */
    private fun readBackup(uri: Uri) = lifecycleScope.launch {
        val b = runCatching { requireContext().contentResolver.openInputStream(uri)!!.bufferedReader().use { gson.fromJson(it, Backup::class.java) } }.getOrNull() ?: return@launch
        var n = 0
        b.playlists.filter { it["type"] == SourceType.URL.name }.forEach { p ->
            n++
            ImportProgressDialog.addUrl(childFragmentManager, p["name"].toString(), p["url"].toString(), false, p["autoUpdate"] as? Boolean ?: true)
        }
        withContext(Dispatchers.IO) { b.singles.forEach { db.singleStreamDao().insert(com.iptvplayer.app.data.database.SingleStreamEntity(name = it["name"].orEmpty(), url = it["url"].orEmpty())) } }
        b.xtream.firstOrNull()?.let { Nav.addProfile(requireContext(), it["server"], it["user"], it["pass"], it["name"]) }
        requireContext().toast(getString(R.string.restore_done, n))
    }

    companion object {
        const val ARG_STANDALONE = "standalone"
        private const val KEY_PASS = "settings_pass"
        // TODO: replace with your own policy pages before publishing.
        const val PRIVACY_URL = "https://example.com/privacy"
        const val TERMS_URL = "https://example.com/terms"
    }
}

@AndroidEntryPoint
class SettingsActivity : BaseActivity<ActivitySettingsBinding>(ActivitySettingsBinding::inflate) {
    override fun setup(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) supportFragmentManager.commit {
            replace(binding.container.id, SettingsFragment().apply { arguments = Bundle().apply { putBoolean(SettingsFragment.ARG_STANDALONE, true) } })
        }
    }
}

/** Feedback opens the user's mail app (no backend in this build). */
@AndroidEntryPoint
class FeedbackActivity : BaseActivity<ActivityFeedbackBinding>(ActivityFeedbackBinding::inflate) {
    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.feedback_title)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        listOf(R.string.fb_channel, R.string.fb_import, R.string.fb_idea, R.string.fb_other).forEachIndexed { i, t ->
            binding.types.addView(Chip(this, null, 0).apply {
                setTextAppearance(R.style.Text_Label); setText(t); isCheckable = true; isCheckedIconVisible = false; id = android.view.View.generateViewId()
                chipBackgroundColor = getColorStateList(R.color.chip_bg); chipStrokeColor = getColorStateList(R.color.chip_stroke); chipStrokeWidth = 1.5f * resources.displayMetrics.density
                setTextColor(getColorStateList(R.color.chip_text)); isChecked = i == 0
                // Chip(ctx, null, 0) skips the default style, which is what makes a chip clickable;
                // without a click listener taps were ignored, so the type could never change.
                isClickable = true; isFocusable = true
            })
        }
        binding.btnSend.setOnClickListener {
            val msg = binding.etMsg.text.toString().trim()
            if (msg.isEmpty()) { binding.etMsg.setBackgroundResource(R.drawable.bg_input_error); return@setOnClickListener }
            val type = binding.types.findViewById<Chip>(binding.types.checkedChipId)?.text ?: ""
            val body = "$msg\n\n— ${binding.etEmail.text}\nApp ${BuildConfig.VERSION_NAME}, Android ${android.os.Build.VERSION.RELEASE}, ${android.os.Build.MODEL}"
            val subject = "[IPTV Player] $type"
            // Subject/body go in the mailto: URI too: Gmail ignores the extras on ACTION_SENDTO.
            val mailto = "mailto:$SUPPORT_EMAIL?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
            runCatching {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(mailto))
                    .putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL)).putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, body))
                finish()
            }.onFailure { shareText(body) }
        }
    }

    companion object { const val SUPPORT_EMAIL = "support@example.com" }
}
