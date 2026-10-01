package com.iptvplayer.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.AppDatabase
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.database.SearchHistoryEntity
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.databinding.ActivitySearchBinding
import com.iptvplayer.app.databinding.ItemChannelListBinding
import com.iptvplayer.app.databinding.ItemOptionBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.ChannelActions
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.bindChannelRow
import com.iptvplayer.app.ui.common.setup
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(private val repo: PlaylistRepository, db: AppDatabase, handle: SavedStateHandle) : ViewModel() {
    private val history = db.searchHistoryDao()
    val query = MutableStateFlow(handle.get<String>(Nav.EXTRA_Q).orEmpty())
    val results = query.flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else repo.search(q) }
    val recent = history.recent(SCOPE)
    val playlists = repo.observePlaylists()

    fun remember(q: String) { if (q.isNotBlank()) viewModelScope.launch { history.add(SearchHistoryEntity(SCOPE, q)) } }
    fun forget(q: String) = viewModelScope.launch { history.remove(SCOPE, q) }
    fun clear() = viewModelScope.launch { history.clear(SCOPE) }
    fun toggleFav(id: Long) = viewModelScope.launch { repo.toggleChannelFavorite(id) }

    companion object { const val SCOPE = "channels" }
}

/** Search channels across every playlist, with recent-search history. */
@AndroidEntryPoint
class SearchActivity : BaseActivity<ActivitySearchBinding>(ActivitySearchBinding::inflate) {
    private val vm: SearchViewModel by viewModels()
    private var playlistNames: Map<Long, String> = emptyMap()

    private val actions = object : ChannelActions {
        override fun onOpen(c: ChannelEntity) { vm.remember(vm.query.value); Nav.play(this@SearchActivity, PlayRequest.Channel(c.playlistId, c.id)) }
        override fun onFav(c: ChannelEntity) { vm.toggleFav(c.id); toast(if (c.isFavorite) R.string.removed_from_fav else R.string.added_to_fav) }
    }
    private val adapter = SimpleAdapter<ChannelEntity, ItemChannelListBinding>(ItemChannelListBinding::inflate, { a, b -> a.id == b.id }) { b, c, i ->
        bindChannelRow(b, c, i, actions, subtitle = listOfNotNull(playlistNames[c.playlistId], c.groupName).joinToString(" · "))
        b.btnMore.visible(false)
    }

    override fun setup(savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener { finish() }
        binding.rv.adapter = adapter
        binding.search.setup(R.string.search_hint, onSubmit = { vm.remember(it) }) { vm.query.value = it }
        if (vm.query.value.isNotEmpty()) binding.search.et.setText(vm.query.value)
        binding.search.et.requestFocus()
        binding.secHistory.secTitle.setText(R.string.recent_searches)
        binding.secHistory.secAction.visible(true)
        binding.secHistory.secAction.setText(R.string.clear)
        binding.secHistory.secAction.setOnClickListener { vm.clear() }

        collect(vm.playlists) { list -> playlistNames = list.associate { it.id to it.name } }
        collect(vm.recent) { list ->
            binding.history.removeAllViews()
            list.forEach { h ->
                val o = ItemOptionBinding.inflate(LayoutInflater.from(this), binding.history, false)
                o.icon.visible(true); o.icon.setImageResource(R.drawable.ic_history); o.icon.setColorFilter(getColor(R.color.text_2))
                o.title.text = h.query
                o.check.visible(true); o.check.setImageResource(R.drawable.ic_close); o.check.setColorFilter(getColor(R.color.text_3))
                o.check.setOnClickListener { vm.forget(h.query) }
                o.root.setOnClickListener { binding.search.et.setText(h.query); binding.search.et.setSelection(h.query.length) }
                binding.history.addView(o.root)
            }
            renderState(vm.query.value, adapter.currentList)
        }
        collect(combine(vm.query, vm.results) { q, r -> q to r }) { (q, r) -> adapter.submitList(r); renderState(q, r) }
    }

    private fun renderState(q: String, r: List<ChannelEntity>) {
        val blank = q.isBlank()
        binding.historyBlock.visible(blank && binding.history.childCount > 0)
        binding.tvCount.visible(!blank && r.isNotEmpty())
        binding.tvCount.text = getString(R.string.n_results_in, r.size)
        binding.rv.visible(!blank)
        // Blank query and no history: a hint instead of an empty page.
        val intro = blank && binding.history.childCount == 0
        binding.empty.root.visible((!blank && r.isEmpty()) || intro)
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_search)
        if (intro) {
            binding.empty.emptyTitle.setText(R.string.search_intro_title)
            binding.empty.emptyBody.setText(R.string.search_intro_body)
        } else if (!blank && r.isEmpty()) {
            binding.empty.emptyTitle.text = getString(R.string.not_found_q, q)
            binding.empty.emptyBody.setText(R.string.not_found_body)
        }
    }
}
