package com.iptvplayer.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.iptvplayer.app.R
import java.util.Locale
import kotlin.math.abs

fun Context.toast(msg: CharSequence) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
fun Context.toast(res: Int, vararg args: Any) = toast(getString(res, *args))

val Int.dp: Int get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

fun View.visible(v: Boolean) { visibility = if (v) View.VISIBLE else View.GONE }

/**
 * Bottom padding for a tab's scrolling list so its last item can scroll clear of the bottom bar
 * (and of the floating chatbot button when [aboveFab] is true). [cover] is the bar height in px.
 */
fun View.padForBottomBar(cover: Int, aboveFab: Boolean = false) {
    val extra = if (aboveFab) 52.dp + 12.dp + 12.dp else 16.dp // FAB height + its gap above the bar + breathing room
    setPadding(paddingLeft, paddingTop, paddingRight, cover + extra)
}

/**
 * Label + switch rows: tapping anywhere on the row toggles its switch (not just the thumb).
 * Rows that already have a click listener (e.g. Settings) are left alone.
 */
fun View.wireSwitchRows() {
    if (this is com.google.android.material.materialswitch.MaterialSwitch) {
        val row = parent as? android.widget.LinearLayout ?: return
        if (row.orientation == android.widget.LinearLayout.HORIZONTAL && !row.hasOnClickListeners()) {
            row.setOnClickListener { if (isEnabled) toggle() }
            row.background ?: row.setBackgroundResource(android.R.color.transparent)
        }
        return
    }
    if (this is android.view.ViewGroup) for (i in 0 until childCount) getChildAt(i).wireSwitchRows()
}

fun Context.copyText(text: String) {
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("link", text))
}

fun Context.clipboardText(): String? =
    (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()

fun Context.openCustomTab(url: String) {
    runCatching {
        CustomTabsIntent.Builder()
            .setDefaultColorSchemeParams(CustomTabColorSchemeParams.Builder().setToolbarColor(ContextCompat.getColor(this, R.color.bg)).build())
            .build().launchUrl(this, Uri.parse(url))
    }.onFailure { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
}

fun Context.shareText(text: String) =
    startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))

/** Colored initials used when a channel/profile has no logo (matches the prototype's generated logos). */
object LogoUtil {
    private val COLORS = intArrayOf(0xFF2B5CB8.toInt(), 0xFFB83A2B.toInt(), 0xFF1F8A6B.toInt(), 0xFF8A4BD0.toInt(), 0xFFC7861B.toInt(),
        0xFF157A9A.toInt(), 0xFFA1336B.toInt(), 0xFF4A6B1F.toInt(), 0xFF5B5BD6.toInt(), 0xFF9A5A2B.toInt())

    fun color(name: String) = COLORS[abs(name.hashCode()) % COLORS.size]

    fun initials(name: String): String {
        val clean = name.replace(Regex("""\(.*?\)|\[.*?]"""), "").trim()
        val words = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size <= 1) return clean.take(4).uppercase(Locale.ROOT)
        val nums = words.filter { w -> w.all { it.isDigit() } }.joinToString("")
        val letters = words.filterNot { w -> w.all { it.isDigit() } }.joinToString("") { it.take(1) }.take(3)
        return (letters + nums).uppercase(Locale.ROOT).take(4)
    }

    /** Shows the image at [url] in [image]; while loading/failed shows colored initials in [fallback]. */
    fun bind(image: ImageView, fallback: TextView, name: String, url: String?, radiusDp: Int = 10) {
        fallback.text = initials(name)
        fallback.background = GradientDrawable().apply { cornerRadius = radiusDp.dp.toFloat(); setColor(color(name)) }
        fallback.visible(true)
        if (url.isNullOrBlank()) { Glide.with(image).clear(image); image.setImageDrawable(null); return }
        Glide.with(image).load(url).centerInside()
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean) = false
                override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                    fallback.visible(false); return false
                }
            }).into(image)
    }

    fun avatar(view: TextView, name: String, color: Int, radiusDp: Int) {
        view.text = name.trim().take(1).uppercase(Locale.ROOT).ifEmpty { "?" }
        view.background = GradientDrawable().apply { cornerRadius = radiusDp.dp.toFloat(); setColor(color) }
    }
}

object TimeFmt {
    fun clock(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
        return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec) else String.format(Locale.ROOT, "%d:%02d", m, sec)
    }

    fun ago(ctx: Context, at: Long): String =
        if (at <= 0) "—" else DateUtils.getRelativeTimeSpanString(at, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

    fun date(epochSec: Long): String =
        if (epochSec <= 0) "∞" else java.text.SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(java.util.Date(epochSec * 1000))
}

fun ImageView.tint(colorRes: Int) { imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes)) }
fun gradient(c1: Int, c2: Int) = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
fun darken(c: Int) = Color.rgb((Color.red(c) * 0.35).toInt(), (Color.green(c) * 0.35).toInt(), (Color.blue(c) * 0.4).toInt())
