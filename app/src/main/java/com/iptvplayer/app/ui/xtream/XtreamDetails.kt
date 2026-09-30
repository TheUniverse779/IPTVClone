package com.iptvplayer.app.ui.xtream

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.MediaType
import com.iptvplayer.app.data.database.XtreamEpisodeEntity
import com.iptvplayer.app.data.network.dto.VodInfoResponse
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.ActivityMovieDetailBinding
import com.iptvplayer.app.databinding.ActivitySeriesDetailBinding
import com.iptvplayer.app.databinding.ActivityXtreamCategoryBinding
import com.iptvplayer.app.databinding.ActivityXtreamRecentBinding
import com.iptvplayer.app.databinding.ItemEpisodeBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.darken
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.gradient
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

// ======================= Category / See all =======================

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class XtreamCategoryViewModel @Inject constructor(val repo: XtreamRepository, handle: SavedStateHandle) : ViewModel() {
    val profileId: String = handle[Nav.EXTRA_PROFILE]!!
    val isSeries = handle.get<String>(Nav.EXTRA_TYPE) == "series"
    val cat = MutableStateFlow(handle.get<String>(Nav.EXTRA_CAT))
    val cats = repo.categories(profileId, if (isSeries) MediaType.SERIES else MediaType.MOVIE)
    val vod = cat.flatMapLatest { c -> Pager(PagingConfig(60, enablePlaceholders = false)) { repo.vodPaged(profileId, c, "") }.flow }.cachedIn(viewModelScope)
    val series = cat.flatMapLatest { c -> Pager(PagingConfig(60, enablePlaceholders = false)) { repo.seriesPaged(profileId, c, "") }.flow }.cachedIn(viewModelScope)
}

@AndroidEntryPoint
class XtreamCategoryActivity : BaseActivity<ActivityXtreamCategoryBinding>(ActivityXtreamCategoryBinding::inflate) {
    private val vm: XtreamCategoryViewModel by viewModels()

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.btnBack.setOnClickListener { finish() }
        binding.rv.layoutManager = GridLayoutManager(this, 3)
        val open: (Poster) -> Unit = { if (it.isSeries) Nav.series(this, vm.profileId, it.id) else Nav.movie(this, vm.profileId, it.id) }
        val adapter = if (vm.isSeries) SeriesPagingAdapter(open) else VodPagingAdapter(open)
        binding.rv.adapter = adapter
        adapter.addLoadStateListener {
            binding.toolbar.tvSubtitle.visible(true)
            binding.toolbar.tvSubtitle.text = getString(R.string.n_items, adapter.itemCount)
        }
        if (vm.isSeries) collect(vm.series) { (adapter as SeriesPagingAdapter).submitData(it) } else collect(vm.vod) { (adapter as VodPagingAdapter).submitData(it) }
        collect(vm.cats) { cats ->
            binding.cats.removeAllViews()
            val all = listOf<Pair<String?, String>>(null to getString(R.string.all)) + cats.map { it.categoryId to it.name }
            all.forEach { (id, name) ->
                binding.cats.addView(Chip(this, null, 0).apply {
                    setTextAppearance(R.style.Text_Label); text = name; isCheckable = true; isCheckedIconVisible = false
                    chipBackgroundColor = getColorStateList(R.color.chip_bg); chipStrokeColor = getColorStateList(R.color.chip_stroke); chipStrokeWidth = 1.5f * resources.displayMetrics.density
                    setTextColor(getColorStateList(R.color.chip_text))
                    isChecked = id == vm.cat.value
                    setOnClickListener { vm.cat.value = id; updateTitle(name) }
                })
            }
            updateTitle(all.firstOrNull { it.first == vm.cat.value }?.second ?: getString(R.string.all))
        }
    }

    private fun updateTitle(catName: String) {
        binding.toolbar.tvTitle.text = if (vm.cat.value == null) getString(if (vm.isSeries) R.string.all_series else R.string.all_movies) else catName
    }
}

// ======================= Movie detail =======================

@HiltViewModel
class MovieDetailViewModel @Inject constructor(val repo: XtreamRepository, handle: SavedStateHandle) : ViewModel() {
    val profileId: String = handle[Nav.EXTRA_PROFILE]!!
    val streamId: Int = handle[Nav.EXTRA_ID]!!
    val vod = repo.observeVod(profileId, streamId)
    val info = MutableStateFlow<VodInfoResponse?>(null)
    val loading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repo.profile(profileId)?.let { info.value = repo.vodInfo(it, streamId) }
            loading.value = false
        }
    }
    fun toggleFav() = viewModelScope.launch { repo.toggleVodFavorite(profileId, streamId) }
}

/** App original used a bottom sheet; here it's a full Activity (backdrop, trailer, more like this). */
@OptIn(ExperimentalCoroutinesApi::class)
@AndroidEntryPoint
class MovieDetailActivity : BaseActivity<ActivityMovieDetailBinding>(ActivityMovieDetailBinding::inflate) {
    private val vm: MovieDetailViewModel by viewModels()
    override val applyInsets = false

    override fun setup(savedInstanceState: Bundle?) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, i ->
            v.setPadding(v.paddingLeft, i.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top, v.paddingRight, 0); i
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFav.setOnClickListener { vm.toggleFav() }
        binding.btnPlay.setOnClickListener { Nav.play(this, PlayRequest.XtreamMovie(vm.profileId, vm.streamId)) }
        val similar = PosterAdapter { Nav.movie(this, vm.profileId, it.id) }
        binding.rvSimilar.adapter = similar
        binding.secSimilar.secTitle.setText(R.string.more_like_this)

        collect(vm.vod.filterNotNull()) { v ->
            binding.title.text = v.name
            val c = LogoUtil.color(v.name)
            binding.backdrop.background = gradient(c, darken(c))
            Glide.with(binding.poster).load(v.icon.takeIf { it.isNotBlank() }).centerCrop().into(binding.poster)
            if (vm.info.value?.info?.backdropPath.isNullOrEmpty()) Glide.with(binding.backdrop).load(v.icon.takeIf { it.isNotBlank() }).centerCrop().into(binding.backdrop)
            binding.btnFav.setImageResource(if (v.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
            binding.btnFav.setColorFilter(getColor(if (v.isFavorite) R.color.accent else R.color.white))
            val resumable = v.positionMs > 0 && (v.durationMs == 0L || v.positionMs < v.durationMs - 4000)
            binding.btnPlay.text = if (resumable) getString(R.string.resume_from, TimeFmt.clock(v.positionMs)) else getString(R.string.play)
        }
        collect(vm.loading) { binding.loading.visible(it) }
        collect(vm.info) { r ->
            val v = vm.repo.vod(vm.profileId, vm.streamId)
            val i = r?.info
            val meta = listOfNotNull(
                (i?.rating?.toDoubleOrNull() ?: v?.rating)?.takeIf { it > 0 }?.let { "★ " + String.format(Locale.ROOT, "%.1f", it) },
                i?.releaseDate?.take(4)?.takeIf { it.isNotBlank() }, i?.genre?.takeIf { it.isNotBlank() },
                i?.duration?.takeIf { it.isNotBlank() },
            )
            binding.meta.text = meta.joinToString("  ·  ")
            binding.plot.text = i?.plot.orEmpty(); binding.plot.visible(!i?.plot.isNullOrBlank())
            binding.facts.removeAllViews()
            fun fact(label: Int, value: String?) {
                if (value.isNullOrBlank()) return
                val row = LinearLayout(this).apply { setPadding(0, 3.dp, 0, 3.dp) }
                row.addView(TextView(this).apply { setText(label); setTextAppearance(R.style.Text_Hint); textSize = 13f; width = 84.dp })
                row.addView(TextView(this).apply { text = value; setTextAppearance(R.style.Text); textSize = 13f })
                binding.facts.addView(row)
            }
            fact(R.string.director, i?.director); fact(R.string.cast_label, i?.cast); fact(R.string.genre, i?.genre)
            i?.backdropPath?.firstOrNull()?.let { Glide.with(binding.backdrop).load(it).centerCrop().into(binding.backdrop) }
            val trailer = i?.youtubeTrailer?.takeIf { it.isNotBlank() }
            binding.btnTrailer.visible(trailer != null)
            binding.btnTrailer.setOnClickListener {
                val url = if (trailer!!.startsWith("http")) trailer else "https://www.youtube.com/watch?v=$trailer"
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }
            v?.let { vod -> lifecycleScope.launch { vm.repo.similarVod(vm.profileId, vod.categoryId, vod.streamId).collect { list ->
                similar.submitList(list.map { it.toPoster() })
                binding.secSimilar.root.visible(list.isNotEmpty()); binding.rvSimilar.visible(list.isNotEmpty())
            } } }
        }
    }
}

// ======================= Series detail =======================

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(val repo: XtreamRepository, handle: SavedStateHandle) : ViewModel() {
    val profileId: String = handle[Nav.EXTRA_PROFILE]!!
    val seriesId: Int = handle[Nav.EXTRA_ID]!!
    val series = repo.observeSeries(profileId, seriesId)
    val episodes = repo.episodes(profileId, seriesId)
    val season = MutableStateFlow<Int?>(null)
    val loading = MutableStateFlow(true)
    val failed = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            repo.profile(profileId)?.let { failed.value = !repo.loadEpisodes(it, seriesId) }
            loading.value = false
        }
    }
    fun toggleFav() = viewModelScope.launch { repo.toggleSeriesFavorite(profileId, seriesId) }
}

@AndroidEntryPoint
class SeriesDetailActivity : BaseActivity<ActivitySeriesDetailBinding>(ActivitySeriesDetailBinding::inflate) {
    private val vm: SeriesDetailViewModel by viewModels()
    override val applyInsets = false
    private var eps: List<XtreamEpisodeEntity> = emptyList()

    override fun setup(savedInstanceState: Bundle?) {
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { v, i ->
            v.setPadding(v.paddingLeft, i.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).top, v.paddingRight, 0); i
        }
        binding.btnBack.setOnClickListener { finish() }
        binding.btnFav.setOnClickListener { vm.toggleFav() }
        collect(vm.series.filterNotNull()) { s ->
            binding.title.text = s.name
            binding.meta.text = listOfNotNull(s.rating.takeIf { it > 0 }?.let { "★ " + String.format(Locale.ROOT, "%.1f", it) },
                s.releaseDate.take(4).takeIf { it.isNotBlank() }, s.genre.takeIf { it.isNotBlank() }).joinToString("  ·  ")
            binding.plot.text = s.plot; binding.plot.visible(s.plot.isNotBlank())
            val c = LogoUtil.color(s.name)
            binding.backdrop.background = gradient(c, darken(c))
            Glide.with(binding.poster).load(s.cover.takeIf { it.isNotBlank() }).centerCrop().into(binding.poster)
            Glide.with(binding.backdrop).load(s.cover.takeIf { it.isNotBlank() }).centerCrop().into(binding.backdrop)
            binding.btnFav.setImageResource(if (s.isFavorite) R.drawable.ic_heart_fill else R.drawable.ic_heart)
            binding.btnFav.setColorFilter(getColor(if (s.isFavorite) R.color.accent else R.color.white))
        }
        collect(vm.loading) { binding.loading.visible(it) }
        collect(vm.episodes) { list -> eps = list; renderSeasons(); renderEpisodes(); renderPlayButton() }
        collect(vm.season) { renderEpisodes() }
    }

    private fun renderSeasons() {
        val seasons = eps.map { it.season }.distinct().sorted()
        if (vm.season.value == null || vm.season.value !in seasons) vm.season.value = lastWatched()?.season ?: seasons.firstOrNull()
        binding.seasons.removeAllViews()
        seasons.forEach { s ->
            binding.seasons.addView(Chip(this, null, 0).apply {
                setTextAppearance(R.style.Text_Label); text = getString(R.string.season_n, s); isCheckable = true; isCheckedIconVisible = false
                chipBackgroundColor = getColorStateList(R.color.chip_bg); chipStrokeColor = getColorStateList(R.color.chip_stroke); chipStrokeWidth = 1.5f * resources.displayMetrics.density
                setTextColor(getColorStateList(R.color.chip_text))
                isChecked = s == vm.season.value
                setOnClickListener { vm.season.value = s }
            })
        }
    }

    private fun renderEpisodes() {
        binding.episodes.removeAllViews()
        eps.filter { it.season == vm.season.value }.forEach { e ->
            val b = ItemEpisodeBinding.inflate(LayoutInflater.from(this), binding.episodes, false)
            b.num.text = e.episodeNum.toString()
            b.title.text = e.title.ifBlank { "E${e.episodeNum}" }
            val pct = if (e.durationMs > 0) (e.positionMs * 100 / e.durationMs).toInt() else 0
            val watched = e.lastPlayed > 0 && e.positionMs == 0L
            b.progress.visible(pct > 0 || watched); b.progress.progress = if (watched) 100 else pct
            val mins = (e.durationSecs / 60).takeIf { it > 0 }?.let { getString(R.string.episode_meta, it) }
            b.sub.text = listOfNotNull(mins, if (watched) getString(R.string.watched) else if (pct > 0) getString(R.string.watching) else null).joinToString(" · ")
            val c = LogoUtil.color(e.title + e.episodeNum)
            (b.image.parent as android.view.View).background = gradient(c, darken(c))
            Glide.with(b.image).load(e.icon.takeIf { it.isNotBlank() }).centerCrop().into(b.image)
            b.root.setOnClickListener { Nav.play(this, PlayRequest.XtreamEpisode(vm.profileId, vm.seriesId, e.episodeId)) }
            binding.episodes.addView(b.root)
        }
    }

    private fun lastWatched() = eps.filter { it.lastPlayed > 0 }.maxByOrNull { it.lastPlayed }

    private fun renderPlayButton() {
        val target = lastWatched() ?: eps.firstOrNull()
        binding.btnPlay.visible(target != null)
        target ?: return
        binding.btnPlay.text = if (lastWatched() != null) getString(R.string.continue_ep, target.season, target.episodeNum) else getString(R.string.play)
        binding.btnPlay.setOnClickListener { Nav.play(this, PlayRequest.XtreamEpisode(vm.profileId, vm.seriesId, target.episodeId)) }
    }
}

// ======================= Continue watching =======================

@HiltViewModel
class XtreamRecentViewModel @Inject constructor(val repo: XtreamRepository, handle: SavedStateHandle) : ViewModel() {
    val profileId: String = handle[Nav.EXTRA_PROFILE]!!
    val items = repo.continueWatching(profileId, 100)
    fun clear() = viewModelScope.launch { repo.clearHistory(profileId) }
    fun remove(c: com.iptvplayer.app.data.database.ContinueItem) = viewModelScope.launch {
        if (c.kind == "EPISODE") repo.saveEpisodeProgress(profileId, c.episodeId, c.refId, 0, c.durationMs) else repo.saveVodProgress(profileId, c.refId, 0, c.durationMs)
    }
}

@AndroidEntryPoint
class XtreamRecentActivity : BaseActivity<ActivityXtreamRecentBinding>(ActivityXtreamRecentBinding::inflate) {
    private val vm: XtreamRecentViewModel by viewModels()

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.continue_watching)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        val clear = addToolbarButton(binding.toolbar.actions, R.drawable.ic_trash, desc = R.string.clear_all) { vm.clear(); toast(R.string.history_cleared) }
        val adapter = SimpleAdapter<com.iptvplayer.app.data.database.ContinueItem, ItemEpisodeBinding>(ItemEpisodeBinding::inflate,
            { a, b -> a.kind == b.kind && a.refId == b.refId && a.episodeId == b.episodeId }) { b, c, _ ->
            b.num.visible(false)
            b.title.text = c.title
            b.sub.text = continueLabel(this, c)
            b.progress.visible(true); b.progress.progress = if (c.durationMs > 0) (c.positionMs * 100 / c.durationMs).toInt() else 0
            Glide.with(b.image).load(c.image.takeIf { it.isNotBlank() }).centerCrop().into(b.image)
            b.btnRemove.visible(true)
            b.btnRemove.setOnClickListener { vm.remove(c); toast(R.string.removed_from_list) }
            b.root.setOnClickListener {
                if (c.kind == "EPISODE") Nav.play(this, PlayRequest.XtreamEpisode(vm.profileId, c.refId, c.episodeId)) else Nav.play(this, PlayRequest.XtreamMovie(vm.profileId, c.refId))
            }
        }
        binding.rv.adapter = adapter
        collect(vm.items) { list ->
            adapter.submitList(list)
            clear.visible(list.isNotEmpty())
            binding.empty.root.visible(list.isEmpty())
            binding.empty.emptyIcon.setImageResource(R.drawable.ic_history)
            binding.empty.emptyTitle.setText(R.string.continue_watching)
            binding.empty.emptyBody.setText(R.string.no_recent)
        }
    }

}
