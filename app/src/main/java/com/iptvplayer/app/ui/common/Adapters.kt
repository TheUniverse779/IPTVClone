package com.iptvplayer.app.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.iptvplayer.app.R
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.databinding.ItemChannelGridBinding
import com.iptvplayer.app.databinding.ItemChannelListBinding
import com.iptvplayer.app.databinding.ItemStripChannelBinding
import com.iptvplayer.app.databinding.ItemXtreamChipBinding
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.visible

class VH<B : ViewBinding>(val b: B) : RecyclerView.ViewHolder(b.root)

/** Minimal ListAdapter for a single view type: [bind] fills the binding for each item. */
open class SimpleAdapter<T : Any, B : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup, Boolean) -> B,
    same: (T, T) -> Boolean,
    private val bind: (B, T, Int) -> Unit,
) : ListAdapter<T, VH<B>>(object : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(a: T, b: T) = same(a, b)
    @android.annotation.SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(a: T, b: T) = a == b
}) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH<B>, position: Int) = bind(holder.b, getItem(position), position)
}

private val CHANNEL_DIFF = object : DiffUtil.ItemCallback<ChannelEntity>() {
    override fun areItemsTheSame(a: ChannelEntity, b: ChannelEntity) = a.id == b.id
    override fun areContentsTheSame(a: ChannelEntity, b: ChannelEntity) = a == b
}

interface ChannelActions {
    fun onOpen(c: ChannelEntity)
    fun onFav(c: ChannelEntity)
    fun onMore(c: ChannelEntity, anchor: android.view.View) {}
}

/** Paged channel list/grid for large playlists. Switch [grid] then call notifyDataSetChanged(). */
class ChannelPagingAdapter(private val actions: ChannelActions) : PagingDataAdapter<ChannelEntity, RecyclerView.ViewHolder>(CHANNEL_DIFF) {
    var grid = false
    var currentId: Long = -1

    override fun getItemViewType(position: Int) = if (grid) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == 1) VH(ItemChannelGridBinding.inflate(inf, parent, false)) else VH(ItemChannelListBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val c = getItem(position) ?: return
        @Suppress("UNCHECKED_CAST")
        when (val b = (holder as VH<*>).b) {
            is ItemChannelListBinding -> bindChannelRow(b, c, position, actions, c.id == currentId)
            is ItemChannelGridBinding -> bindChannelTile(b, c, actions)
        }
    }
}

fun bindChannelRow(b: ItemChannelListBinding, c: ChannelEntity, index: Int, actions: ChannelActions, current: Boolean = false, subtitle: String? = null) {
    b.num.text = (index + 1).toString()
    LogoUtil.bind(b.logo.image, b.logo.initials, c.name, c.logo)
    b.name.text = c.name
    b.name.setTextColor(b.root.context.getColor(if (current) R.color.accent else R.color.text))
    b.sub.text = subtitle ?: c.groupName
    b.root.setBackgroundResource(if (current) R.drawable.bg_row_current else R.drawable.bg_row)
    b.lockIcon.visible(c.isLocked)
    b.btnFav.isSelected = c.isFavorite
    b.btnFav.setImageResource(if (c.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
    b.root.setOnClickListener { actions.onOpen(c) }
    b.btnFav.setOnClickListener { actions.onFav(c) }
    b.btnMore.setOnClickListener { actions.onMore(c, it) }
}

fun bindChannelTile(b: ItemChannelGridBinding, c: ChannelEntity, actions: ChannelActions) {
    LogoUtil.bind(b.logo.image, b.logo.initials, c.name, c.logo)
    b.name.text = c.name
    b.btnFav.isSelected = c.isFavorite
    b.btnFav.setImageResource(if (c.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
    b.root.setOnClickListener { actions.onOpen(c) }
    b.btnFav.setOnClickListener { actions.onFav(c) }
}

class ChannelGridAdapter(actions: ChannelActions) : SimpleAdapter<ChannelEntity, ItemChannelGridBinding>(
    ItemChannelGridBinding::inflate, { a, b -> a.id == b.id }, { b, c, _ -> bindChannelTile(b, c, actions) })

class StripAdapter(onOpen: (ChannelEntity) -> Unit) : SimpleAdapter<ChannelEntity, ItemStripChannelBinding>(
    ItemStripChannelBinding::inflate, { a, b -> a.id == b.id }, { b, c, _ ->
        LogoUtil.bind(b.logo.image, b.logo.initials, c.name, c.logo, 9)
        b.name.text = c.name
        b.root.setOnClickListener { onOpen(c) }
    })

class XtreamChipAdapter(onOpen: (XtreamProfileEntity) -> Unit) : SimpleAdapter<XtreamProfileEntity, ItemXtreamChipBinding>(
    ItemXtreamChipBinding::inflate, { a, b -> a.id == b.id }, { b, p, _ ->
        LogoUtil.avatar(b.avatar, p.name, p.avatarColor, 10)
        b.name.text = p.name
        bindProfileSub(b.sub, p)
        b.lock.visible(p.passcodeLocked)
        b.root.setOnClickListener { onOpen(p) }
    })

fun isExpired(p: XtreamProfileEntity) =
    p.status.equals("Expired", true) || (p.expDate > 0 && p.expDate * 1000 < System.currentTimeMillis())

fun hasVod(p: XtreamProfileEntity) = p.vodCount + p.seriesCount > 0

fun bindProfileSub(tv: android.widget.TextView, p: XtreamProfileEntity) {
    val ctx = tv.context
    when {
        isExpired(p) -> { tv.text = ctx.getString(R.string.expired_on, TimeFmt.date(p.expDate)); tv.setTextColor(ctx.getColor(R.color.danger_text)) }
        !hasVod(p) -> { tv.setText(R.string.live_only); tv.setTextColor(ctx.getColor(R.color.text_3)) }
        else -> { tv.text = ctx.getString(R.string.valid_until, TimeFmt.date(p.expDate)); tv.setTextColor(ctx.getColor(R.color.text_3)) }
    }
}
