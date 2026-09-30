package com.iptvplayer.app.ui.playlist

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.databinding.ActivityPlaylistDetailBinding
import com.iptvplayer.app.databinding.ItemGroupBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.ChannelActions
import com.iptvplayer.app.ui.common.ChannelPagingAdapter
import com.iptvplayer.app.ui.common.ConfirmDialog
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.SourceActions
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.common.setup
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.copyText
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChannelSort(val key: String, val label: Int) {
    ORDER_ASC("order_asc", R.string.sort_order_asc), ORDER_DESC("order_desc", R.string.sort_order_desc),
    AZ("az", R.string.sort_az), ZA("za", R.string.sort_za)
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(private val repo: PlaylistRepository, handle: SavedStateHandle) : ViewModel() {
    val playlistId: Long = handle[Nav.EXTRA_ID] ?: 0L
    val showChannels = handle.getStateFlow("tab_channels", false).let { MutableStateFlow(it.value) }
    val grid = MutableStateFlow(false)
    val query = MutableStateFlow("")
    val group = MutableStateFlow<String?>(null)
    val sort = MutableStateFlow(ChannelSort.ORDER_ASC)
    val groupsByCount = MutableStateFlow(false)

    val playlist = repo.observePlaylist(playlistId)
    val total = repo.channelCount(playlistId)

    val groups = combine(query, groupsByCount) { q, byCount -> q to byCount }
        .flatMapLatest { (q, byCount) -> repo.groups(playlistId, if (showChannels.value) "" else q).let { f ->
            kotlinx.coroutines.flow.flow { f.collect { list -> emit(if (byCount) list.sortedByDescending { it.count } else list) } }
        } }

    val filteredCount = combine(query, group) { q, g -> q to g }.flatMapLatest { (q, g) -> repo.countChannels(playlistId, g, q) }

    val channels = combine(query, group, sort) { q, g, s -> Triple(q, g, s) }
        .flatMapLatest { (q, g, s) ->
            Pager(PagingConfig(pageSize = 60, prefetchDistance = 30, enablePlaceholders = false)) { repo.pagedChannels(playlistId, g, q, s.key) }.flow
        }.cachedIn(viewModelScope)

    fun toggleFav(id: Long) = viewModelScope.launch { repo.toggleChannelFavorite(id) }
    fun rename(id: Long, name: String) = viewModelScope.launch { repo.renameChannel(id, name) }
    fun toggleLock(id: Long) = viewModelScope.launch { repo.toggleChannelLock(id) }
    fun deleteChannel(id: Long) = viewModelScope.launch { repo.deleteChannel(id) }
    fun deletePlaylist() = viewModelScope.launch { repo.delete(playlistId) }
    fun togglePlaylistFav() = viewModelScope.launch { repo.toggleFavorite(playlistId) }
}

/** Category / Channels = two RecyclerViews toggled by the segmented control (no fragments). */
@AndroidEntryPoint
class PlaylistDetailActivity : BaseActivity<ActivityPlaylistDetailBinding>(ActivityPlaylistDetailBinding::inflate) {
    private val vm: PlaylistDetailViewModel by viewModels()
    private lateinit var sourceActions: SourceActions

    private val channelActions = object : ChannelActions {
        override fun onOpen(c: ChannelEntity) {
            if (c.isLocked) com.iptvplayer.app.ui.passcode.PasscodeDialog.show(supportFragmentManager, KEY_PLAY_LOCKED, com.iptvplayer.app.ui.passcode.PasscodeDialog.MODE_ENTER, c.name, bundleOf("id" to c.id))
            else play(c.id)
        }
        override fun onFav(c: ChannelEntity) { vm.toggleFav(c.id); toast(if (c.isFavorite) R.string.removed_from_fav else R.string.added_to_fav) }
        override fun onMore(c: ChannelEntity, anchor: View) = channelMenu(c, anchor)
    }
    private val channelAdapter = ChannelPagingAdapter(channelActions)
    private val groupAdapter = SimpleAdapter<Pair<String?, Int>, ItemGroupBinding>(ItemGroupBinding::inflate, { a, b -> a.first == b.first }) { b, g, pos ->
        b.name.text = g.first ?: getString(R.string.all_channels)
        b.name.setTypeface(null, if (g.first == null) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        b.count.text = "%,d".format(g.second)
        b.root.setBackgroundResource(R.drawable.bg_row)
        b.root.setOnClickListener { vm.group.value = g.first; selectTab(channels = true) }
    }

    override fun setup(savedInstanceState: Bundle?) {
        sourceActions = SourceActions(this) { vm.deletePlaylist(); finish() }.register()
        binding.toolbar.btnBack.setOnClickListener { finish() }
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_more) { anchor ->
            lifecycleScope.launch { collectOnce()?.let { sourceActions.playlistMenu(anchor, it) } }
        }
        binding.seg.s1.setText(R.string.category); binding.seg.s2.setText(R.string.tab_channels)
        binding.seg.s1.setOnClickListener { selectTab(false) }; binding.seg.s2.setOnClickListener { selectTab(true) }

        binding.rvGroups.adapter = groupAdapter
        binding.rvGroups.background = getDrawable(R.drawable.bg_list_card)
        binding.rvGroups.clipToOutline = true
        binding.rvChannels.adapter = channelAdapter
        applyLayout()

        binding.search.setup(R.string.search_groups) { q -> vm.query.value = q }
        binding.btnView.setOnClickListener { vm.grid.value = !vm.grid.value; applyLayout() }
        binding.btnSort.setOnClickListener { if (vm.showChannels.value) sortMenu(it) else {
            vm.groupsByCount.value = !vm.groupsByCount.value
            toast(if (vm.groupsByCount.value) R.string.sort_groups_count else R.string.sort_groups_name)
        } }
        binding.chipGroup.setOnCloseIconClickListener { vm.group.value = null }

        collect(vm.playlist) { p ->
            if (p == null) { finish(); return@collect }
            binding.toolbar.tvTitle.text = p.name
            binding.toolbar.tvSubtitle.visible(true)
            binding.toolbar.tvSubtitle.text = getString(R.string.playlist_meta, "%,d".format(p.channelCount), getString(R.string.updated_ago, TimeFmt.ago(this, p.lastSync)))
        }
        collect(vm.total) { total -> groupsTotal = total; refreshGroups() }
        collect(vm.groups) { list -> lastGroups = list.map { it.groupName to it.count }; refreshGroups() }
        collect(vm.channels) { channelAdapter.submitData(it) }
        collect(combine(vm.group, vm.sort) { g, s -> g to s }) { (g, s) ->
            binding.chipGroup.visible(g != null); binding.chipGroup.text = g
            updateMeta()
        }
        channelAdapter.addLoadStateListener { s -> refreshing = s.refresh is androidx.paging.LoadState.Loading; updateMeta() }
        collect(vm.filteredCount) { filtered = it; updateMeta() }

        supportFragmentManager.setFragmentResultListener(KEY_PLAY_LOCKED, this) { _, r ->
            if (r.getBoolean(com.iptvplayer.app.ui.passcode.PasscodeDialog.KEY_OK)) play(r.getBundle(com.iptvplayer.app.ui.passcode.PasscodeDialog.KEY_PAYLOAD)!!.getLong("id"))
        }
        supportFragmentManager.setFragmentResultListener(KEY_DELETE_CH, this) { _, r ->
            if (r.getInt(ConfirmDialog.KEY_WHICH) == 0) vm.deleteChannel(r.getBundle(ConfirmDialog.KEY_PAYLOAD)!!.getLong("id"))
        }
        selectTab(savedInstanceState?.getBoolean("channels") ?: false)
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putBoolean("channels", vm.showChannels.value) }

    private var groupsTotal = 0
    private var lastGroups: List<Pair<String?, Int>> = emptyList()
    private fun refreshGroups() {
        val q = vm.query.value
        groupAdapter.submitList(if (q.isEmpty()) listOf<Pair<String?, Int>>(null to groupsTotal) + lastGroups else lastGroups)
        if (!vm.showChannels.value) showEmpty(lastGroups.isEmpty() && q.isNotEmpty(), q)
    }

    private var refreshing = true
    private var filtered = -1

    private fun updateMeta() {
        val n = if (filtered >= 0) filtered else channelAdapter.itemCount
        binding.tvMeta.text = getString(R.string.list_meta, "%,d".format(n), getString(vm.sort.value.label))
        if (vm.showChannels.value) showEmpty(n == 0 && !refreshing && vm.query.value.isNotEmpty(), vm.query.value)
    }

    private fun showEmpty(show: Boolean, q: String) {
        binding.empty.root.visible(show)
        if (!show) return
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_search)
        binding.empty.emptyTitle.text = getString(R.string.no_channel_q, q)
        binding.empty.emptyBody.setText(R.string.no_channel_q_body)
        binding.empty.emptyAction.visible(true)
        binding.empty.emptyAction.setText(R.string.search_all_playlists)
        binding.empty.emptyAction.setOnClickListener { Nav.search(this, q) }
    }

    private fun selectTab(channels: Boolean) {
        vm.showChannels.value = channels
        binding.seg.s1.isSelected = !channels; binding.seg.s2.isSelected = channels
        binding.rvGroups.visible(!channels); binding.rvChannels.visible(channels)
        binding.filterBar.visible(channels)
        binding.btnView.visible(channels)
        binding.search.et.setHint(if (channels) R.string.search_channels else R.string.search_groups)
        binding.empty.root.visible(false)
        if (channels) updateMeta() else refreshGroups()
    }

    private fun applyLayout() {
        val grid = vm.grid.value
        channelAdapter.grid = grid
        binding.rvChannels.layoutManager = if (grid) GridLayoutManager(this, 3) else LinearLayoutManager(this)
        binding.rvChannels.setPadding(if (grid) 11.dp else 8.dp, 0, if (grid) 11.dp else 8.dp, 24.dp)
        binding.btnView.setImageResource(if (grid) R.drawable.ic_rows else R.drawable.ic_grid)
        @Suppress("NotifyDataSetChanged") channelAdapter.notifyDataSetChanged()
    }

    private fun sortMenu(anchor: View) = PopupMenu(this, anchor).apply {
        menu.add(0, 0, 0, R.string.sort_by).isEnabled = false
        ChannelSort.entries.forEachIndexed { i, s -> menu.add(1, i + 1, i + 1, s.label).apply { isCheckable = true; isChecked = vm.sort.value == s } }
        menu.setGroupCheckable(1, true, true)
        setOnMenuItemClickListener { vm.sort.value = ChannelSort.entries[it.itemId - 1]; true }
    }.show()

    private fun channelMenu(c: ChannelEntity, anchor: View) = PopupMenu(this, anchor).apply {
        menu.add(0, 1, 0, R.string.rename).setIcon(R.drawable.ic_edit)
        menu.add(0, 2, 1, if (c.isLocked) R.string.unlock_channel else R.string.lock_channel).setIcon(R.drawable.ic_lock)
        menu.add(0, 3, 2, R.string.copy_stream_link).setIcon(R.drawable.ic_copy)
        menu.add(0, 4, 3, R.string.remove_from_playlist).setIcon(R.drawable.ic_trash)
        setForceShowIcon(true)
        setOnMenuItemClickListener {
            when (it.itemId) {
                1 -> renameDialog(c)
                2 -> vm.toggleLock(c.id)
                3 -> { copyText(c.url); toast(R.string.copied) }
                4 -> ConfirmDialog.show(supportFragmentManager, KEY_DELETE_CH, getString(R.string.remove_from_playlist) + "?", c.name, ok = getString(R.string.delete), danger = true, payload = bundleOf("id" to c.id))
            }
            true
        }
    }.show()

    private fun renameDialog(c: ChannelEntity) {
        val et = EditText(this).apply { setText(c.name); setSelection(c.name.length); setPadding(20.dp, 16.dp, 20.dp, 16.dp) }
        MaterialAlertDialogBuilder(this).setTitle(R.string.rename).setView(et)
            .setPositiveButton(R.string.save) { _, _ -> et.text.toString().trim().takeIf { it.isNotEmpty() }?.let { vm.rename(c.id, it) } }
            .setNegativeButton(R.string.cancel, null).show()
    }

    private fun play(id: Long) = Nav.play(this, PlayRequest.Channel(vm.playlistId, id))

    private suspend fun collectOnce() = vm.playlist.firstOrNull()


    companion object { private const val KEY_PLAY_LOCKED = "pd_play_locked"; private const val KEY_DELETE_CH = "pd_delete_ch" }
}
