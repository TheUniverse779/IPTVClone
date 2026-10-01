package com.iptvplayer.app.ui.main

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.activityViewModels
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.databinding.FragmentPlaylistsBinding
import com.iptvplayer.app.databinding.ItemPlaylistCardBinding
import com.iptvplayer.app.databinding.ItemSingleBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SourceActions
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.copyText
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint

/** Tab 2 "Channels": every source the user added (playlists + single streams). */
@AndroidEntryPoint
class PlaylistsFragment : BaseFragment<FragmentPlaylistsBinding>(FragmentPlaylistsBinding::inflate) {
    private val vm: MainViewModel by activityViewModels()
    private lateinit var actions: SourceActions

    override fun setup(savedInstanceState: Bundle?) {
        actions = SourceActions(this) { vm.deletePlaylist(it) }.register()
        binding.toolbar.btnBack.visible(false)
        binding.toolbar.tvTitle.setText(R.string.your_sources)
        padForMainTabs(binding.root)
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_search, boxed = true) { Nav.search(requireContext()) }
        binding.secSingles.secTitle.setText(R.string.single_streams)
        binding.addSource.glIcon.setImageResource(R.drawable.ic_plus)
        binding.addSource.glText.text = Html.fromHtml(getString(R.string.add_new_source), Html.FROM_HTML_MODE_COMPACT)
        binding.addSource.glChevron.visible(false)
        binding.addSource.root.setOnClickListener { (activity as? MainActivity)?.openAddSource() }
        binding.community.glIcon.setImageResource(R.drawable.ic_share)
        binding.community.glText.text = Html.fromHtml(getString(R.string.community_entry), Html.FROM_HTML_MODE_COMPACT)
        binding.community.root.setOnClickListener { Nav.community(requireContext()) }

        collect(vm.state) { s ->
            if (!s.loaded) return@collect
            binding.playlistContainer.removeAllViews()
            s.playlists.forEach { p ->
                val b = ItemPlaylistCardBinding.inflate(layoutInflater, binding.playlistContainer, false)
                bindPlaylistCard(b, p, actions) { vm.togglePlaylistFav(p.id) }
                binding.playlistContainer.addView(b.root)
            }
            binding.secSingles.root.visible(s.singles.isNotEmpty())
            binding.singleContainer.visible(s.singles.isNotEmpty())
            binding.singleContainer.removeAllViews()
            s.singles.forEach { st ->
                val b = ItemSingleBinding.inflate(layoutInflater, binding.singleContainer, false)
                b.name.text = st.name; b.url.text = st.url
                b.root.setOnClickListener { Nav.play(requireContext(), PlayRequest.Url(st.url, st.name)) }
                b.btnMore.setOnClickListener { v ->
                    PopupMenu(requireContext(), v).apply {
                        menu.add(0, 1, 0, R.string.copy); menu.add(0, 2, 1, R.string.delete)
                        setOnMenuItemClickListener {
                            if (it.itemId == 1) { requireContext().copyText(st.url); requireContext().toast(R.string.copied) } else vm.deleteSingle(st.id)
                            true
                        }
                    }.show()
                }
                binding.singleContainer.addView(b.root)
            }
            binding.empty.root.visible(s.playlists.isEmpty() && s.singles.isEmpty())
            binding.empty.emptyIcon.setImageResource(R.drawable.ic_list)
            binding.empty.emptyTitle.setText(R.string.home_empty_title)
            binding.empty.emptyBody.setText(R.string.home_empty_body)
        }
    }
}
