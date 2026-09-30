package com.iptvplayer.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.iptvplayer.app.R
import com.iptvplayer.app.databinding.ItemOptionBinding
import com.iptvplayer.app.databinding.SheetListBinding
import com.iptvplayer.app.util.visible

/** Helpers to fill [SheetListBinding] with option rows. */
object SheetRows {
    fun header(b: SheetListBinding, title: CharSequence?, startText: String? = null, onStart: (() -> Unit)? = null, onClose: () -> Unit) {
        b.header.sheetTitle.text = title ?: ""
        b.header.sheetStart.visible(startText != null)
        b.header.sheetStart.text = startText
        b.header.sheetStart.setOnClickListener { onStart?.invoke() }
        b.header.sheetEnd.setOnClickListener { onClose() }
    }

    fun option(
        container: LinearLayout, title: CharSequence, sub: CharSequence? = null, icon: Int = 0,
        checked: Boolean = false, danger: Boolean = false, trailing: View? = null, onClick: () -> Unit,
    ): ItemOptionBinding {
        val o = ItemOptionBinding.inflate(LayoutInflater.from(container.context), container, false)
        val ctx = container.context
        o.title.text = title
        o.sub.visible(sub != null); o.sub.text = sub
        o.icon.visible(icon != 0); if (icon != 0) o.icon.setImageResource(icon)
        o.check.visible(checked)
        if (checked) { o.root.setBackgroundResource(R.drawable.bg_row_current); o.title.setTextColor(ctx.getColor(R.color.accent)) }
        if (danger) { o.title.setTextColor(ctx.getColor(R.color.danger_text)); o.icon.setColorFilter(ctx.getColor(R.color.danger_text)) }
        trailing?.let { (o.root as LinearLayout).addView(it) }
        o.root.setOnClickListener { onClick() }
        container.addView(o.root)
        return o
    }

    fun divider(container: LinearLayout) {
        container.addView(View(container.context).apply { setBackgroundColor(context.getColor(R.color.surface_2)) },
            LinearLayout.LayoutParams(-1, 1).apply { topMargin = 8; bottomMargin = 8 })
    }
}
