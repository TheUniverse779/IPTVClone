package com.iptvplayer.app.ui.xtream

import android.os.Bundle
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.AppDatabase
import com.iptvplayer.app.data.database.ContinueItem
import com.iptvplayer.app.data.database.SearchHistoryEntity
import com.iptvplayer.app.databinding.FragmentXtreamFavoriteBinding
import com.iptvplayer.app.databinding.FragmentXtreamLiveBinding
import com.iptvplayer.app.databinding.FragmentXtreamMovieBinding
import com.iptvplayer.app.databinding.FragmentXtreamSearchBinding
import com.iptvplayer.app.databinding.ItemLiveCatBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.hasVod
import com.iptvplayer.app.ui.common.setup
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.darken
import com.iptvplayer.app.util.gradient
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import java.util.Locale
import javax.inject.Inject

private fun BaseFragment<*>.openPoster(vm: XtreamHomeViewModel, p: Poster) =
    if (p.isSeries) Nav.series(requireContext(), vm.profileId, p.id) else Nav.movie(requireContext(), vm.profileId, p.id)

private fun BaseFragment<*>.resume(vm: XtreamHomeViewModel, c: ContinueItem) =
    if (c.kind == "EPISODE") Nav.play(requireContext(), PlayRequest.XtreamEpisode(vm.profileId, c.refId, c.episodeId))
    else Nav.play(requireContext(), PlayRequest.XtreamMovie(vm.profileId, c.refId))

/** Movies (+series) tab: hero, continue watching, latest rows, genres. */
@AndroidEntryPoint
class XtreamMovieFragment : BaseFragment<FragmentXtreamMovieBinding>(FragmentXtreamMovieBinding::inflate) {
    private val vm: XtreamHomeViewModel by activityViewModels()

    override fun setup(savedInstanceState: Bundle?) {
        val cont = ContinueAdapter { resume(vm, it) }
        val movies = PosterAdapter { openPoster(vm, it) }
        val series = PosterAdapter { openPoster(vm, it) }
        binding.rvContinue.adapter = cont; binding.rvMovies.adapter = movies; binding.rvSeries.adapter = series
        header(binding.secContinue, R.string.continue_watching) { Nav.xtreamRecent(requireContext(), vm.profileId) }
        header(binding.secMovies, R.string.recently_added) { Nav.xtreamCategory(requireContext(), vm.profileId, "movie", null) }
        header(binding.secSeries, R.string.series) { Nav.xtreamCategory(requireContext(), vm.profileId, "series", null) }
        binding.secGenres.secTitle.setText(R.string.by_genre)
        binding.filter.setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        collect(vm.continueWatching) { cont.submitList(it); binding.secContinue.root.visible(it.isNotEmpty()); binding.rvContinue.visible(it.isNotEmpty()) }
        collect(vm.latestVod) { movies.submitList(it.map { v -> v.toPoster() }); applyFilter() }
        collect(vm.latestSeries) { series.submitList(it.map { s -> s.toPoster() }); applyFilter() }
        collect(vm.vodCats) { cats ->
            binding.genres.removeAllViews()
            cats.take(24).forEach { c ->
                binding.genres.addView(Chip(requireContext(), null, 0).apply {
                    setTextAppearance(R.style.Text_Label); text = c.name
                    chipBackgroundColor = requireContext().getColorStateList(R.color.surface_2); setTextColor(requireContext().getColor(R.color.text_2))
                    setOnClickListener { Nav.xtreamCategory(requireContext(), vm.profileId, "movie", c.categoryId) }
                })
            }
            binding.secGenres.root.visible(cats.isNotEmpty())
        }
        collect(vm.hero) { h ->
            binding.hero.visible(h != null)
            h ?: return@collect
            binding.heroTitle.text = h.name
            binding.heroMeta.text = listOfNotNull(h.rating.takeIf { it > 0 }?.let { String.format(Locale.ROOT, "%.1f", it) }, h.extension.uppercase()).joinToString(" · ")
            val c = LogoUtil.color(h.name)
            binding.hero.background = gradient(c, darken(c))
            Glide.with(binding.heroImage).load(h.icon.takeIf { it.isNotBlank() }).centerCrop().into(binding.heroImage)
            binding.heroPlay.setOnClickListener { Nav.play(requireContext(), PlayRequest.XtreamMovie(vm.profileId, h.streamId)) }
            binding.heroInfo.setOnClickListener { Nav.movie(requireContext(), vm.profileId, h.streamId) }
        }
        collect(vm.profile) { p ->
            p ?: return@collect
            val none = !hasVod(p)
            binding.scroll.visible(!none)
            binding.empty.root.visible(none)
            if (none) with(binding.empty) {
                emptyIcon.setImageResource(R.drawable.ic_movie)
                emptyTitle.setText(R.string.no_movies_title)
                emptyBody.text = getString(R.string.no_movies_body, p.name)
                emptyAction.visible(true); emptyAction.setText(R.string.watch_live)
                emptyAction.setOnClickListener { (activity as? XtreamHomeActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bnav)?.selectedItemId = R.id.nav_live }
            }
        }
    }

    private fun applyFilter() {
        val id = binding.filter.checkedChipId
        val showMovies = id != R.id.fSeries; val showSeries = id != R.id.fMovies
        val hasMovies = (binding.rvMovies.adapter?.itemCount ?: 0) > 0
        val hasSeries = (binding.rvSeries.adapter?.itemCount ?: 0) > 0
        binding.secMovies.root.visible(showMovies && hasMovies); binding.rvMovies.visible(showMovies && hasMovies)
        binding.secSeries.root.visible(showSeries && hasSeries); binding.rvSeries.visible(showSeries && hasSeries)
        binding.hero.visible(showMovies && vm.hero.value != null)
    }

    private fun header(h: com.iptvplayer.app.databinding.LayoutSectionHeaderBinding, title: Int, onAll: () -> Unit) {
        h.secTitle.setText(title); h.secAction.visible(true); h.secAction.setText(R.string.see_all); h.secAction.setOnClickListener { onAll() }
    }
}

/** Live tab: category rail on the left, paged channel list on the right. */
@AndroidEntryPoint
class XtreamLiveFragment : BaseFragment<FragmentXtreamLiveBinding>(FragmentXtreamLiveBinding::inflate) {
    private val vm: XtreamHomeViewModel by activityViewModels()

    override fun setup(savedInstanceState: Bundle?) {
        val live = LivePagingAdapter({ Nav.play(requireContext(), PlayRequest.XtreamLive(vm.profileId, it.streamId)) }, { vm.toggleLiveFav(it.streamId) })
        binding.rvLive.adapter = live
        val cats = SimpleAdapter<Pair<String?, String>, ItemLiveCatBinding>(ItemLiveCatBinding::inflate, { a, b -> a.first == b.first }) { b, c, _ ->
            b.name.text = c.second
            b.name.isSelected = c.first == vm.liveCat.value
            b.root.setOnClickListener { vm.liveCat.value = c.first }
        }
        binding.rvCats.adapter = cats
        binding.search.setup(R.string.search_live) { vm.liveQuery.value = it }
        collect(combine(vm.liveCats, vm.liveCat) { list, _ -> list }) { list ->
            cats.submitList(listOf<Pair<String?, String>>(null to getString(R.string.all)) + list.map { it.categoryId to it.name })
            cats.notifyDataSetChanged()
        }
        collect(vm.live) { live.submitData(it) }
    }
}

/** Search movies + series of this profile. */
@AndroidEntryPoint
class XtreamSearchFragment : BaseFragment<FragmentXtreamSearchBinding>(FragmentXtreamSearchBinding::inflate) {
    private val vm: XtreamHomeViewModel by activityViewModels()
    @Inject lateinit var db: AppDatabase

    override fun setup(savedInstanceState: Bundle?) {
        val rememberQ = { vm.query.value.takeIf { q -> q.isNotBlank() }?.let { q -> remember(q) } }
        val adapter = PosterAdapter(fullWidth = true) { rememberQ(); openPoster(vm, it) }
        // Live matches first (full-width rows), then movies/series as a 3-column grid.
        val live = LiveListAdapter({ rememberQ(); Nav.play(requireContext(), PlayRequest.XtreamLive(vm.profileId, it.streamId)) }, { vm.toggleLiveFav(it.streamId) })
        val concat = androidx.recyclerview.widget.ConcatAdapter(live, adapter)
        binding.rv.layoutManager = GridLayoutManager(requireContext(), 3).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position < live.itemCount) 3 else 1
            }
        }
        binding.rv.adapter = concat
        binding.search.setup(R.string.search_vod_hint, onSubmit = { remember(it) }) { vm.query.value = it }
        binding.secRecent.secTitle.setText(R.string.recent_searches)
        collect(db.searchHistoryDao().recent(vm.profileId)) { list ->
            binding.recent.removeAllViews()
            list.forEach { h ->
                binding.recent.addView(Chip(requireContext()).apply {
                    text = h.query; setChipIconResource(R.drawable.ic_history); isChipIconVisible = true
                    chipIconTint = requireContext().getColorStateList(R.color.text_2)
                    chipBackgroundColor = requireContext().getColorStateList(R.color.surface_2); setTextColor(requireContext().getColor(R.color.text_2))
                    setOnClickListener { binding.search.et.setText(h.query) }
                })
            }
            updateRecent()
        }
        collect(combine(vm.query, vm.searchVod, vm.searchSeries, vm.searchLive) { q, v, s, l -> SearchResult(q, v.map { it.toPoster() } + s.map { it.toPoster() }, l) }) { r ->
            val newQuery = r.query != lastQuery; lastQuery = r.query
            live.submitList(r.live)
            adapter.submitList(r.posters) { if (newQuery) binding.rv.post { binding.rv.scrollToPosition(0) } }
            binding.empty.root.visible(r.query.isNotBlank() && r.posters.isEmpty() && r.live.isEmpty())
            binding.empty.emptyIcon.setImageResource(R.drawable.ic_search)
            binding.empty.emptyTitle.setText(R.string.no_results)
            binding.empty.emptyBody.setText(R.string.no_results_body)
            updateRecent()
        }
    }

    private var lastQuery = ""
    private data class SearchResult(val query: String, val posters: List<Poster>, val live: List<com.iptvplayer.app.data.database.XtreamLiveEntity>)

    private fun updateRecent() {
        val show = vm.query.value.isBlank() && binding.recent.childCount > 0
        binding.secRecent.root.visible(show); binding.recent.visible(show)
    }

    private fun remember(q: String) = viewLifecycleOwner.lifecycleScope.launch { db.searchHistoryDao().add(SearchHistoryEntity(vm.profileId, q)) }
}

/** Favorites: Movies & Series grid / Live channels list. */
@AndroidEntryPoint
class XtreamFavoriteFragment : BaseFragment<FragmentXtreamFavoriteBinding>(FragmentXtreamFavoriteBinding::inflate) {
    private val vm: XtreamHomeViewModel by activityViewModels()
    private var liveTab = false

    override fun setup(savedInstanceState: Bundle?) {
        val vod = PosterAdapter(fullWidth = true) { openPoster(vm, it) }
        val live = LiveListAdapter({ Nav.play(requireContext(), PlayRequest.XtreamLive(vm.profileId, it.streamId)) }, { vm.toggleLiveFav(it.streamId) })
        binding.rvVod.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvVod.adapter = vod
        binding.rvLive.adapter = live
        binding.seg.s1.setText(R.string.fav_movies_series); binding.seg.s2.setText(R.string.fav_live)
        binding.seg.s1.setOnClickListener { liveTab = false; render(vod.itemCount, live.itemCount) }
        binding.seg.s2.setOnClickListener { liveTab = true; render(vod.itemCount, live.itemCount) }
        collect(combine(vm.favVod, vm.favSeries) { v, s -> v.map { it.toPoster() } + s.map { it.toPoster() } }) { vod.submitList(it) { render(vod.itemCount, live.itemCount) } }
        collect(vm.favLive) { live.submitList(it) { render(vod.itemCount, live.itemCount) } }
        render(0, 0)
    }

    private fun render(vodCount: Int, liveCount: Int) {
        binding.seg.s1.isSelected = !liveTab; binding.seg.s2.isSelected = liveTab
        binding.rvVod.visible(!liveTab); binding.rvLive.visible(liveTab)
        val empty = if (liveTab) liveCount == 0 else vodCount == 0
        binding.empty.root.visible(empty)
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_heart)
        binding.empty.emptyTitle.setText(R.string.no_favs_title)
        binding.empty.emptyBody.setText(R.string.no_favs_body)
    }

}
