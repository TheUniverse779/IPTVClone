package com.iptvplayer.app.ui.guide

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import com.google.android.material.chip.Chip
import com.iptvplayer.app.Features
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.BaseBottomSheet
import com.iptvplayer.app.data.repository.GuideRepository
import com.iptvplayer.app.databinding.ActivityFaqBinding
import com.iptvplayer.app.databinding.ActivityHowToAddBinding
import com.iptvplayer.app.databinding.ItemActionCardBinding
import com.iptvplayer.app.databinding.ItemFaqBinding
import com.iptvplayer.app.databinding.ItemStepBinding
import com.iptvplayer.app.databinding.SheetWebGuideBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.util.UrlUtils
import com.iptvplayer.app.util.clipboardText
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.openCustomTab
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Steps per source type. Title/body pairs; <b> is rendered. Without [Features.GUIDE_SITES] the steps start from a link the user already has. */
private val HOWTO = mapOf(
    "url" to Pair(R.string.howto_title_url,
        if (Features.GUIDE_SITES) listOf(R.string.howto_url_1 to R.string.howto_url_1b, R.string.howto_url_2 to R.string.howto_url_2b, R.string.howto_url_3 to R.string.howto_url_3b, R.string.howto_url_4 to R.string.howto_url_4b)
        else listOf(R.string.howto_url_get to R.string.howto_url_get_b, R.string.howto_url_add to R.string.howto_url_3b, R.string.howto_url_4 to R.string.howto_url_4b)),
    "xtream" to Pair(R.string.howto_title_xtream, listOf(R.string.howto_xt_1 to (if (Features.GUIDE_SITES) R.string.howto_xt_1b else 0), R.string.howto_xt_2 to R.string.howto_xt_2b, R.string.howto_xt_3 to R.string.howto_xt_3b, R.string.howto_xt_4 to 0)),
    "single" to Pair(R.string.howto_title_single, listOf(R.string.howto_single_1 to 0, R.string.howto_single_2 to 0, R.string.howto_single_3 to R.string.howto_single_3b)),
    "upload" to Pair(R.string.howto_title_upload, listOf(R.string.howto_upload_1 to 0, R.string.howto_upload_2 to 0, R.string.howto_upload_3 to R.string.howto_upload_3b)),
)

/** How to add a source: steps, suggested sites (WebView sheet / Custom Tab), Google search, open form. */
@AndroidEntryPoint
class HowToAddActivity : BaseActivity<ActivityHowToAddBinding>(ActivityHowToAddBinding::inflate) {
    @Inject lateinit var guide: GuideRepository
    private var type = "url"

    override fun setup(savedInstanceState: Bundle?) {
        type = savedInstanceState?.getString("type") ?: intent.getStringExtra(Nav.EXTRA_TYPE) ?: "url"
        binding.toolbar.tvTitle.setText(R.string.guide_title)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        listOf("url" to R.string.howto_url, "xtream" to R.string.howto_xtream, "single" to R.string.howto_single, "upload" to R.string.howto_file).forEach { (t, label) ->
            binding.types.addView(Chip(this, null, 0).apply {
                setTextAppearance(R.style.Text_Label); setText(label); isCheckable = true; isCheckedIconVisible = false
                chipBackgroundColor = getColorStateList(R.color.chip_bg); chipStrokeColor = getColorStateList(R.color.chip_stroke); chipStrokeWidth = 1.5f * resources.displayMetrics.density
                setTextColor(getColorStateList(R.color.chip_text))
                isChecked = t == type
                setOnClickListener { type = t; render() }
            })
        }
        supportFragmentManager.setFragmentResultListener(WebGuideSheet.RESULT_LINK, this) { _, r ->
            val link = r.getString(WebGuideSheet.KEY_URL) ?: return@setFragmentResultListener
            if (type == "xtream") UrlUtils.parseXtream(link)?.let { Nav.addProfile(this, it.server, it.username, it.password) } ?: Nav.import(this, "xtream")
            else Nav.import(this, if (type == "single") "single" else "url", link)
        }
        render()
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("type", type) }

    private fun render() {
        val (title, steps) = HOWTO.getValue(type)
        binding.title.setText(title)
        binding.steps.removeAllViews()
        steps.forEachIndexed { i, (t, b) ->
            val s = ItemStepBinding.inflate(LayoutInflater.from(this), binding.steps, false)
            s.n.text = (i + 1).toString()
            s.title.text = Html.fromHtml(getString(t), Html.FROM_HTML_MODE_COMPACT)
            s.body.visible(b != 0); if (b != 0) s.body.setText(b)
            binding.steps.addView(s.root)
        }
        binding.sites.removeAllViews()
        binding.sitesTitle.visible(Features.GUIDE_SITES); binding.sites.visible(Features.GUIDE_SITES)
        binding.note.setText(if (Features.GUIDE_SITES) R.string.third_party_note else R.string.no_content_note)
        if (Features.GUIDE_SITES) renderSites()
        binding.btnForm.setText(if (type == "upload") R.string.choose_m3u_file else R.string.open_import_form)
        binding.btnForm.setOnClickListener {
            when (type) { "upload" -> Nav.upload(this); "xtream" -> Nav.addProfile(this); else -> Nav.import(this, type) }
        }
    }

    private fun renderSites() {
        val sites = guide.sites()
        val siteType = if (type == "upload") "url" else type
        sites.forType(if (siteType == "url") "iptv" else siteType).forEach { site ->
            card(R.drawable.ic_globe, site.title, UrlUtils.host(site.url), R.drawable.ic_chevron) {
                if (site.mode == "customtab") openCustomTab(site.url) else WebGuideSheet.show(supportFragmentManager, site.url, site.title)
            }
        }
        card(R.drawable.ic_search, getString(R.string.search_google), "\"${sites.searchQuery}\"", R.drawable.ic_external) {
            openCustomTab("https://www.google.com/search?q=" + Uri.encode(sites.searchQuery))
        }
    }

    private fun card(icon: Int, title: String, sub: String, trail: Int, onClick: () -> Unit) {
        val b = ItemActionCardBinding.inflate(LayoutInflater.from(this), binding.sites, false)
        b.icon.setImageResource(icon); b.title.text = title; b.sub.text = sub; b.trail.setImageResource(trail)
        b.root.setOnClickListener { onClick() }
        binding.sites.addView(b.root, android.widget.LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10.dp })
    }
}

/**
 * In-app WebView sheet for suggested sites (like the original). Extra: when the user copies a
 * playlist link, or taps a link to a .m3u / get.php URL, a snackbar offers "Use this link",
 * which returns the URL via [RESULT_LINK].
 */
class WebGuideSheet : BaseBottomSheet<SheetWebGuideBinding>(SheetWebGuideBinding::inflate) {
    override val tall = true
    private var clipListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun setup(savedInstanceState: Bundle?) {
        val a = requireArguments()
        binding.header.sheetTitle.text = a.getString("title") ?: getString(R.string.suggested_page)
        binding.header.sheetStart.visible(true)
        binding.header.sheetStart.setText(R.string.close)
        binding.header.sheetStart.setOnClickListener { dismiss() }
        binding.header.sheetEnd.setImageResource(R.drawable.ic_external)
        binding.header.sheetEnd.setOnClickListener { requireContext().openCustomTab(binding.web.url ?: a.getString("url")!!) }
        (dialog as? com.google.android.material.bottomsheet.BottomSheetDialog)?.behavior?.isDraggable = false

        with(binding.web) {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, p: Int) { binding.progress.visible(p < 100); binding.progress.setProgressCompat(p, true) }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()
                    if (UrlUtils.isPlaylistLink(url)) { offer(url); return true }
                    if (!url.startsWith("http")) return true
                    return false
                }
                override fun onPageFinished(view: WebView, url: String) { binding.addr.text = url.removePrefix("https://").removePrefix("http://") }
            }
            if (savedInstanceState != null) restoreState(savedInstanceState) else loadUrl(a.getString("url")!!)
        }
        binding.btnWebBack.setOnClickListener { if (binding.web.canGoBack()) binding.web.goBack() }
        binding.btnReload.setOnClickListener { binding.web.reload() }

        val cm = requireContext().getSystemService(ClipboardManager::class.java)
        clipListener = ClipboardManager.OnPrimaryClipChangedListener {
            val text = requireContext().clipboardText() ?: return@OnPrimaryClipChangedListener
            val url = UrlUtils.firstUrl(text)
            if (url != null && (UrlUtils.isPlaylistLink(url) || UrlUtils.parseXtream(text) != null)) offer(url)
        }.also { cm.addPrimaryClipChangedListener(it) }
    }

    private fun offer(url: String) {
        binding.snack.visible(true)
        binding.snackText.text = getString(R.string.found_playlist_link)
        binding.snackAction.setOnClickListener {
            parentFragmentManager.setFragmentResult(RESULT_LINK, bundleOf(KEY_URL to url))
            dismiss()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); _bindingOrNull()?.web?.saveState(outState) }
    private fun _bindingOrNull() = runCatching { binding }.getOrNull()

    override fun onDestroyView() {
        clipListener?.let { requireContext().getSystemService(ClipboardManager::class.java).removePrimaryClipChangedListener(it) }
        _bindingOrNull()?.web?.destroy()
        super.onDestroyView()
    }

    companion object {
        const val RESULT_LINK = "web_guide_link"
        const val KEY_URL = "url"

        /** Opens the first suggested site for [type] (iptv / xtream / single). */
        fun show(ctx: android.content.Context, fm: FragmentManager, type: String) {
            val repo = dagger.hilt.android.EntryPointAccessors.fromApplication(ctx.applicationContext, GuideEntryPoint::class.java).guide()
            val site = repo.sites().forType(type).firstOrNull()
            show(fm, site?.url ?: "https://github.com/iptv-org/iptv", site?.title)
        }

        fun show(fm: FragmentManager, url: String, title: String?) =
            WebGuideSheet().apply { arguments = bundleOf("url" to url, "title" to title) }.show(fm, "web_guide")
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface GuideEntryPoint { fun guide(): GuideRepository }

/** FAQ with inline-expanding answers (no separate detail screen). */
@AndroidEntryPoint
class FaqActivity : BaseActivity<ActivityFaqBinding>(ActivityFaqBinding::inflate) {
    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.faq_title)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        binding.btnAsk.setOnClickListener { Nav.chatbot(this) }
        listOf(R.string.faq_q1 to R.string.faq_a1, R.string.faq_q2 to R.string.faq_a2, R.string.faq_q3 to R.string.faq_a3,
            R.string.faq_q4 to R.string.faq_a4, R.string.faq_q5 to R.string.faq_a5).forEachIndexed { i, (q, a) ->
            val b = ItemFaqBinding.inflate(LayoutInflater.from(this), binding.items, false)
            b.q.setText(q); b.a.setText(a)
            b.root.setOnClickListener {
                val open = b.a.visibility != android.view.View.VISIBLE
                b.a.visible(open); b.chev.setImageResource(if (open) R.drawable.ic_chevron_down else R.drawable.ic_chevron)
            }
            if (i > 0) binding.items.addView(android.view.View(this).apply { setBackgroundColor(getColor(R.color.surface_2)) }, android.widget.LinearLayout.LayoutParams(-1, 1))
            binding.items.addView(b.root)
        }
    }
}
