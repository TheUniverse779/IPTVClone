package com.iptvplayer.app.ui.importer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.format.Formatter
import androidx.activity.result.contract.ActivityResultContracts
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.databinding.ActivityUploadM3uBinding
import com.iptvplayer.app.databinding.ItemPlaylistCardBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint

/** Import up to 5 local .m3u/.m3u8 files; each becomes its own playlist. */
@AndroidEntryPoint
class UploadM3uActivity : BaseActivity<ActivityUploadM3uBinding>(ActivityUploadM3uBinding::inflate) {
    private val files = mutableListOf<Pair<String, Uri>>()

    private val pick = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            if (files.none { it.second == uri }) files += displayName(uri) to uri
        }
        if (files.size > MAX) { files.subList(MAX, files.size).clear(); toast(R.string.max_5_files) }
        render()
    }

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.title_upload)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_help, desc = R.string.help) { Nav.howTo(this, "upload") }
        binding.lockRow.swTitle.setText(R.string.lock_with_passcode)
        binding.btnPick.setOnClickListener { pick.launch(arrayOf("audio/x-mpegurl", "application/x-mpegurl", "application/vnd.apple.mpegurl", "text/plain", "application/octet-stream", "*/*")) }
        binding.btnImport.setOnClickListener {
            if (binding.lockRow.sw.isChecked) PasscodeDialog.show(supportFragmentManager, KEY_LOCK, PasscodeDialog.MODE_ENSURE) else startImport()
        }
        supportFragmentManager.setFragmentResultListener(KEY_LOCK, this) { _, r -> if (r.getBoolean(PasscodeDialog.KEY_OK)) startImport() }
        render()
    }

    private fun startImport() {
        val custom = binding.etName.text.toString().trim()
        val names = files.mapIndexed { i, f -> if (custom.isNotEmpty()) (if (files.size == 1) custom else "$custom ${i + 1}") else f.first.substringBeforeLast('.') }
        ImportProgressDialog.addFiles(supportFragmentManager, names, files.map { it.second }, binding.lockRow.sw.isChecked)
    }

    private fun render() {
        binding.files.removeAllViews()
        files.forEachIndexed { i, (name, uri) ->
            val b = ItemPlaylistCardBinding.inflate(layoutInflater, binding.files, false)
            b.icon.setImageResource(R.drawable.ic_doc)
            b.name.text = name
            b.meta.text = sizeOf(uri)
            b.btnFav.visible(false)
            b.btnMore.setImageResource(R.drawable.ic_close)
            b.btnMore.setOnClickListener { files.removeAt(i); render() }
            binding.files.addView(b.root)
        }
        binding.options.visible(files.isNotEmpty())
        if (files.size == 1 && binding.etName.text.isNullOrBlank()) binding.etName.hint = files[0].first.substringBeforeLast('.')
        binding.btnImport.isEnabled = files.isNotEmpty()
        binding.btnImport.text = getString(R.string.import_n_files, files.size)
    }

    private fun displayName(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment ?: "playlist.m3u"

    private fun sizeOf(uri: Uri): String =
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) Formatter.formatShortFileSize(this, c.getLong(0)) else ""
        }.orEmpty()

    companion object { private const val MAX = 5; private const val KEY_LOCK = "upload_lock" }
}
