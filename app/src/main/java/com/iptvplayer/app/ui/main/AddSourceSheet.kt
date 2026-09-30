package com.iptvplayer.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseBottomSheet
import com.iptvplayer.app.databinding.ItemActionCardBinding
import com.iptvplayer.app.databinding.SheetListBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SheetRows
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.dp

/** "+" in the bottom bar: the 5 ways to add a source (same as the original app). */
class AddSourceSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {

    private val pickVideo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { dismiss(); Nav.play(requireContext(), PlayRequest.Url(uri.toString(), uri.lastPathSegment ?: "Video")) }
    }

    override fun setup(savedInstanceState: Bundle?) {
        SheetRows.header(binding, getString(R.string.add_source), getString(R.string.close), onStart = { dismiss() }, onClose = { dismiss(); Nav.howTo(requireContext()) })
        binding.header.sheetEnd.setImageResource(R.drawable.ic_help)
        binding.container.setPadding(16.dp, 4.dp, 16.dp, 8.dp)
        card(R.drawable.ic_link, R.string.src_url, R.string.src_url_sub) { Nav.import(requireContext(), "url") }
        card(R.drawable.ic_xtream, R.string.src_xtream, R.string.src_xtream_sub) { Nav.import(requireContext(), "xtream") }
        card(R.drawable.ic_play_circle, R.string.src_single, R.string.src_single_sub) { Nav.import(requireContext(), "single") }
        card(R.drawable.ic_upload, R.string.src_file, R.string.src_file_sub) { Nav.upload(requireContext()) }
        card(R.drawable.ic_device, R.string.src_device, R.string.src_device_sub, dismissFirst = false) {
            pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        }
    }

    private fun card(icon: Int, title: Int, sub: Int, dismissFirst: Boolean = true, onClick: () -> Unit) {
        val b = ItemActionCardBinding.inflate(LayoutInflater.from(requireContext()), binding.container, false)
        b.root.setBackgroundResource(R.drawable.bg_card_2_press)
        b.icWrap.setBackgroundResource(R.drawable.bg_ic_wrap_3)
        b.icon.setImageResource(icon); b.title.setText(title); b.sub.setText(sub)
        b.root.setOnClickListener { if (dismissFirst) dismiss(); onClick() }
        binding.container.addView(b.root, android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8.dp })
    }
}
