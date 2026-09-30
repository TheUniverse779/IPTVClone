package com.iptvplayer.app.ui.main

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.databinding.FragmentXtreamProfilesBinding
import com.iptvplayer.app.databinding.ItemProfileAddBinding
import com.iptvplayer.app.databinding.ItemProfileBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SourceActions
import com.iptvplayer.app.ui.common.VH
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.common.bindProfileSub
import com.iptvplayer.app.ui.common.isExpired
import com.iptvplayer.app.ui.xtream.ProfileActionsSheet
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint

/** Tab 3 "Xtream" → Who's watching? */
@AndroidEntryPoint
class XtreamProfilesFragment : BaseFragment<FragmentXtreamProfilesBinding>(FragmentXtreamProfilesBinding::inflate) {
    private val vm: MainViewModel by activityViewModels()
    private lateinit var actions: SourceActions
    private val adapter = ProfileAdapter()

    override fun setup(savedInstanceState: Bundle?) {
        actions = SourceActions(this) { vm.deletePlaylist(it) }.register()
        actions.profileLookup = { id -> vm.state.value.profiles.firstOrNull { it.id == id } }
        binding.toolbar.btnBack.visible(false)
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_help, boxed = true, desc = R.string.help) { Nav.howTo(requireContext(), "xtream") }
        binding.rvProfiles.adapter = adapter

        with(binding.empty) {
            emptyIcon.setImageResource(R.drawable.ic_xtream)
            emptyTitle.setText(R.string.no_xtream_title)
            emptyBody.setText(R.string.no_xtream_body)
            emptyAction.visible(true)
            emptyAction.setText(R.string.add_profile)
            emptyAction.setIconResource(R.drawable.ic_plus)
            emptyAction.setOnClickListener { Nav.addProfile(requireContext()) }
        }
        binding.guide.glText.text = Html.fromHtml(getString(R.string.no_xtream_guide), Html.FROM_HTML_MODE_COMPACT)
        binding.guide.root.setOnClickListener { Nav.howTo(requireContext(), "xtream") }

        collect(vm.state) { s ->
            if (!s.loaded) return@collect
            binding.content.visible(s.profiles.isNotEmpty())
            binding.emptyBlock.visible(s.profiles.isEmpty())
            adapter.items = s.profiles
        }
    }

    private inner class ProfileAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var items: List<XtreamProfileEntity> = emptyList()
            @android.annotation.SuppressLint("NotifyDataSetChanged") set(v) { field = v; notifyDataSetChanged() }

        override fun getItemCount() = items.size + 1
        override fun getItemViewType(position: Int) = if (position == items.size) 1 else 0
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == 1) VH(ItemProfileAddBinding.inflate(inf, parent, false)) else VH(ItemProfileBinding.inflate(inf, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val b = (holder as VH<*>).b) {
                is ItemProfileAddBinding -> b.addBox.setOnClickListener { Nav.addProfile(requireContext()) }
                is ItemProfileBinding -> bindProfile(b, items[position])
            }
        }
    }

    private fun bindProfile(b: ItemProfileBinding, p: XtreamProfileEntity) {
        val expired = isExpired(p)
        LogoUtil.avatar(b.avatar, p.name, p.avatarColor, 24)
        b.avatar.alpha = if (expired) 0.55f else 1f
        b.name.text = p.name
        bindProfileSub(b.sub, p)
        b.lockBadge.visible(p.passcodeLocked)
        b.newRing.visible(p.isNew)
        b.tag.visible(p.isNew || expired)
        when {
            p.isNew -> { b.tag.setText(R.string.tag_new); b.tag.setBackgroundResource(R.drawable.bg_tag_new); b.tag.setTextColor(requireContext().getColor(R.color.white)) }
            expired -> { b.tag.setText(R.string.tag_expired); b.tag.setBackgroundResource(R.drawable.bg_tag_exp); b.tag.setTextColor(requireContext().getColor(R.color.danger_text)) }
        }
        b.avatar.setOnClickListener {
            if (p.isNew) vm.clearNew(p.id)
            actions.openProfile(p)
        }
        b.avatar.setOnLongClickListener { ProfileActionsSheet.show(childFragmentManager, p.id); true }
        b.btnMore.setOnClickListener { ProfileActionsSheet.show(childFragmentManager, p.id) }
    }
}
