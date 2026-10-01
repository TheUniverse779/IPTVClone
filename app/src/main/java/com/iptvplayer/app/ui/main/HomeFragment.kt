package com.iptvplayer.app.ui.main

import com.iptvplayer.app.Features
import com.iptvplayer.app.util.dp
import androidx.core.view.updateLayoutParams
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.database.PlaylistEntity
import com.iptvplayer.app.databinding.FragmentHomeBinding
import com.iptvplayer.app.databinding.ItemPlaylistCardBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.ChannelActions
import com.iptvplayer.app.ui.common.ChannelGridAdapter
import com.iptvplayer.app.ui.common.SourceActions
import com.iptvplayer.app.ui.common.StripAdapter
import com.iptvplayer.app.ui.common.XtreamChipAdapter
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.darken
import com.iptvplayer.app.util.gradient
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {
    private val vm: MainViewModel by activityViewModels()
    private lateinit var actions: SourceActions

    private val channelActions = object : ChannelActions {
        override fun onOpen(c: ChannelEntity) = play(c)
        override fun onFav(c: ChannelEntity) {
            vm.toggleChannelFav(c.id)
            requireContext().toast(if (c.isFavorite) R.string.removed_from_fav else R.string.added_to_fav)
        }
    }
    private val strip = StripAdapter { play(it) }
    private val xtChips = XtreamChipAdapter { actions.openProfile(it) }
    private val grid = ChannelGridAdapter(channelActions)

    override fun setup(savedInstanceState: Bundle?) {
        actions = SourceActions(this) { vm.deletePlaylist(it) }.register()
        val brand = SpannableString("${getString(R.string.brand_1)} ${getString(R.string.brand_2)}")
        brand.setSpan(ForegroundColorSpan(requireContext().getColor(R.color.accent)), getString(R.string.brand_1).length + 1, brand.length, 0)
        binding.brand.text = brand

        binding.btnSearch.setOnClickListener { Nav.search(requireContext()) }
        binding.btnHelp.setOnClickListener { Nav.howTo(requireContext()) }
        // With Sport off, Settings is the 4th tab: the gear switches to it instead of opening a second Settings screen.
        binding.btnSettings.setOnClickListener {
            if (Features.SPORT) Nav.settings(requireContext()) else (activity as? MainActivity)?.selectTab(MainActivity.Tab.SPORT)
        }
        binding.botFab.setOnClickListener { Nav.chatbot(requireContext()) }
        // Content scrolls fully past both the bottom bar and the chatbot button.
        padForMainTabs(binding.scroll, aboveFab = true)
        (activity as? MainActivity)?.let { host ->
            collect(host.bottomCover) { cover ->
                if (cover > 0) binding.botFab.updateLayoutParams<android.view.ViewGroup.MarginLayoutParams> { bottomMargin = cover + 12.dp }
            }
        }

        // empty state
        binding.btnImportHero.setOnClickListener { Nav.import(requireContext(), "url") }
        with(binding.cardXtream) {
            icon.setImageResource(R.drawable.ic_xtream); title.setText(R.string.import_xtream_title); sub.setText(R.string.import_xtream_sub)
            root.setOnClickListener { Nav.import(requireContext(), "xtream") }
        }
        with(binding.cardSingle) {
            icon.setImageResource(R.drawable.ic_play_circle); title.setText(R.string.play_single_title); sub.setText(R.string.play_single_sub)
            root.setOnClickListener { Nav.import(requireContext(), "single") }
        }
        binding.guide.glText.text = Html.fromHtml(getString(R.string.home_guide_link), Html.FROM_HTML_MODE_COMPACT)
        binding.guide.root.setOnClickListener { Nav.howTo(requireContext(), "url") }

        // content
        binding.rvStrip.adapter = strip
        binding.rvXtream.adapter = xtChips
        binding.rvChannels.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvChannels.adapter = grid
        binding.secPlaylists.secTitle.setText(R.string.your_playlists)
        binding.secPlaylists.secAction.setText(R.string.see_all)
        binding.secPlaylists.secAction.visible(true)
        binding.secPlaylists.secAction.setOnClickListener { (activity as? MainActivity)?.selectTab(MainActivity.Tab.CHANNELS) }
        binding.secXtream.secTitle.setText(R.string.xtream_header)
        binding.secXtream.secAction.setText(R.string.manage)
        binding.secXtream.secAction.visible(true)
        binding.secXtream.secAction.setOnClickListener { (activity as? MainActivity)?.selectTab(MainActivity.Tab.XTREAM) }
        binding.secChannels.secTitle.setText(R.string.channels_header)
        binding.chips.setOnCheckedStateChangeListener { _, ids ->
            vm.homeChip.value = if (ids.firstOrNull() == R.id.chipFav) HomeChip.FAVOURITE else HomeChip.RECENT
        }

        collect(vm.state) { render(it) }
        collect(vm.homeChip) { render(vm.state.value) }
    }

    private fun render(s: HomeState) {
        if (!s.loaded) return
        binding.emptyBlock.visible(!s.hasSources)
        binding.contentBlock.visible(s.hasSources)
        binding.botFab.visible(true)
        if (!s.hasSources) return

        val last = s.recent.firstOrNull()
        binding.onAir.visible(last != null)
        if (last != null) {
            val c = LogoUtil.color(last.name)
            binding.onAirScreen.background = gradient(c, darken(c))
            binding.onAirName.text = last.name
            binding.onAirSub.text = getString(R.string.continue_live, s.playlists.firstOrNull { it.id == last.playlistId }?.name ?: last.groupName)
            binding.onAirPlay.setOnClickListener { play(last) }
            strip.submitList(s.recent.drop(1))
        }

        binding.secPlaylists.root.visible(s.playlists.isNotEmpty())
        binding.playlistContainer.removeAllViews()
        s.playlists.forEach { binding.playlistContainer.addView(playlistCard(it)) }

        binding.secXtream.root.visible(s.profiles.isNotEmpty())
        binding.rvXtream.visible(s.profiles.isNotEmpty())
        xtChips.submitList(s.profiles)

        val list = if (vm.homeChip.value == HomeChip.FAVOURITE) s.favorites else s.recent
        grid.submitList(list)
        binding.tvChannelsEmpty.visible(list.isEmpty())
        binding.tvChannelsEmpty.setText(if (vm.homeChip.value == HomeChip.FAVOURITE) R.string.no_favourites else R.string.no_recent)
    }

    private fun playlistCard(p: PlaylistEntity) = ItemPlaylistCardBinding.inflate(LayoutInflater.from(requireContext()), binding.playlistContainer, false).also { b ->
        bindPlaylistCard(b, p, actions, onFav = { vm.togglePlaylistFav(p.id) })
    }.root

    private fun play(c: ChannelEntity) = Nav.play(requireContext(), PlayRequest.Channel(c.playlistId, c.id))
}

fun bindPlaylistCard(b: ItemPlaylistCardBinding, p: PlaylistEntity, actions: SourceActions, onFav: () -> Unit) {
    val ctx = b.root.context
    b.name.text = p.name
    b.meta.text = ctx.getString(R.string.playlist_meta, "%,d".format(p.channelCount), ctx.getString(R.string.updated_ago, TimeFmt.ago(ctx, p.lastSync)))
    b.lockBadge.visible(p.isLocked)
    b.btnFav.isSelected = p.isFavorite
    b.btnFav.setImageResource(if (p.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
    b.btnFav.setOnClickListener { onFav() }
    b.btnMore.setOnClickListener { actions.playlistMenu(it, p) }
    b.root.setOnClickListener { actions.openPlaylist(p) }
}
