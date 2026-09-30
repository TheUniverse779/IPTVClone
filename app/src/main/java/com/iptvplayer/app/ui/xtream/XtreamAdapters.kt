package com.iptvplayer.app.ui.xtream

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.iptvplayer.app.R
import com.iptvplayer.app.data.database.ContinueItem
import com.iptvplayer.app.data.database.XtreamLiveEntity
import com.iptvplayer.app.data.database.XtreamSeriesEntity
import com.iptvplayer.app.data.database.XtreamVodEntity
import com.iptvplayer.app.databinding.ItemContinueBinding
import com.iptvplayer.app.databinding.ItemLiveChannelBinding
import com.iptvplayer.app.databinding.ItemPosterBinding
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.VH
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.darken
import com.iptvplayer.app.util.gradient
import com.iptvplayer.app.util.visible
import java.util.Locale

/** Movies and series share one poster card. */
data class Poster(val id: Int, val title: String, val image: String, val rating: Double, val isSeries: Boolean)

fun XtreamVodEntity.toPoster() = Poster(streamId, name, icon, rating, false)
fun XtreamSeriesEntity.toPoster() = Poster(seriesId, name, cover, rating, true)

fun bindPoster(b: ItemPosterBinding, p: Poster, fullWidth: Boolean, onClick: (Poster) -> Unit) {
    val lp = b.root.layoutParams
    if (fullWidth) { lp.width = ViewGroup.LayoutParams.MATCH_PARENT; (lp as? ViewGroup.MarginLayoutParams)?.setMargins(5, 5, 5, 5) }
    b.title.text = p.title
    b.fallback.text = p.title
    val c = LogoUtil.color(p.title)
    b.posterBox.background = gradient(c, darken(c))
    b.fallback.visible(true)
    Glide.with(b.image).load(p.image.takeIf { it.isNotBlank() }).centerCrop()
        .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
            override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, first: Boolean) = false
            override fun onResourceReady(r: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, ds: com.bumptech.glide.load.DataSource, first: Boolean): Boolean {
                b.fallback.visible(false); return false
            }
        }).into(b.image)
    b.rate.visible(p.rating > 0)
    b.rate.text = String.format(Locale.ROOT, "%.1f", p.rating)
    b.root.setOnClickListener { onClick(p) }
}

class PosterAdapter(private val fullWidth: Boolean = false, onClick: (Poster) -> Unit) : SimpleAdapter<Poster, ItemPosterBinding>(
    ItemPosterBinding::inflate, { a, b -> a.id == b.id && a.isSeries == b.isSeries }, { b, p, _ -> bindPoster(b, p, fullWidth, onClick) })

private val VOD_DIFF = object : DiffUtil.ItemCallback<XtreamVodEntity>() {
    override fun areItemsTheSame(a: XtreamVodEntity, b: XtreamVodEntity) = a.streamId == b.streamId
    override fun areContentsTheSame(a: XtreamVodEntity, b: XtreamVodEntity) = a == b
}
private val SERIES_DIFF = object : DiffUtil.ItemCallback<XtreamSeriesEntity>() {
    override fun areItemsTheSame(a: XtreamSeriesEntity, b: XtreamSeriesEntity) = a.seriesId == b.seriesId
    override fun areContentsTheSame(a: XtreamSeriesEntity, b: XtreamSeriesEntity) = a == b
}
private val LIVE_DIFF = object : DiffUtil.ItemCallback<XtreamLiveEntity>() {
    override fun areItemsTheSame(a: XtreamLiveEntity, b: XtreamLiveEntity) = a.streamId == b.streamId
    override fun areContentsTheSame(a: XtreamLiveEntity, b: XtreamLiveEntity) = a == b
}

class VodPagingAdapter(private val onClick: (Poster) -> Unit) : PagingDataAdapter<XtreamVodEntity, VH<ItemPosterBinding>>(VOD_DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH<ItemPosterBinding>, position: Int) { getItem(position)?.let { bindPoster(holder.b, it.toPoster(), true, onClick) } }
}

class SeriesPagingAdapter(private val onClick: (Poster) -> Unit) : PagingDataAdapter<XtreamSeriesEntity, VH<ItemPosterBinding>>(SERIES_DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemPosterBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH<ItemPosterBinding>, position: Int) { getItem(position)?.let { bindPoster(holder.b, it.toPoster(), true, onClick) } }
}

fun bindLive(b: ItemLiveChannelBinding, l: XtreamLiveEntity, onOpen: (XtreamLiveEntity) -> Unit, onFav: (XtreamLiveEntity) -> Unit) {
    LogoUtil.bind(b.logo.image, b.logo.initials, l.name, l.icon)
    b.name.text = l.name
    b.btnFav.isSelected = l.isFavorite
    b.btnFav.setImageResource(if (l.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
    b.root.setOnClickListener { onOpen(l) }
    b.btnFav.setOnClickListener { onFav(l) }
}

class LivePagingAdapter(private val onOpen: (XtreamLiveEntity) -> Unit, private val onFav: (XtreamLiveEntity) -> Unit) :
    PagingDataAdapter<XtreamLiveEntity, VH<ItemLiveChannelBinding>>(LIVE_DIFF) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(ItemLiveChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH<ItemLiveChannelBinding>, position: Int) { getItem(position)?.let { bindLive(holder.b, it, onOpen, onFav) } }
}

class LiveListAdapter(onOpen: (XtreamLiveEntity) -> Unit, onFav: (XtreamLiveEntity) -> Unit) : SimpleAdapter<XtreamLiveEntity, ItemLiveChannelBinding>(
    ItemLiveChannelBinding::inflate, { a, b -> a.streamId == b.streamId }, { b, l, _ -> bindLive(b, l, onOpen, onFav) })

fun continueLabel(ctx: android.content.Context, c: ContinueItem): String = if (c.kind == "EPISODE") "S${c.season} · E${c.episodeNum}"
    else ctx.getString(com.iptvplayer.app.R.string.time_left, com.iptvplayer.app.util.TimeFmt.clock((c.durationMs - c.positionMs).coerceAtLeast(0)))

class ContinueAdapter(onClick: (ContinueItem) -> Unit) : SimpleAdapter<ContinueItem, ItemContinueBinding>(
    ItemContinueBinding::inflate, { a, b -> a.kind == b.kind && a.refId == b.refId && a.episodeId == b.episodeId }, { b, c, _ ->
        b.title.text = c.title
        b.sub.text = continueLabel(b.root.context, c)
        b.progress.progress = if (c.durationMs > 0) (c.positionMs * 100 / c.durationMs).toInt() else 0
        val col = LogoUtil.color(c.title)
        (b.image.parent as ViewGroup).background = gradient(col, darken(col))
        Glide.with(b.image).load(c.image.takeIf { it.isNotBlank() }).centerCrop().into(b.image)
        b.root.setOnClickListener { onClick(c) }
    })
