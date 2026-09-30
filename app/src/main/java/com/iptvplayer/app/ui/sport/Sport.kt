package com.iptvplayer.app.ui.sport

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.chip.Chip
import com.google.android.material.materialswitch.MaterialSwitch
import com.iptvplayer.app.R
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.BaseBottomSheet
import com.iptvplayer.app.base.BaseFragment
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.data.repository.Match
import com.iptvplayer.app.data.repository.MatchState
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.data.repository.SportRepository
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.ActivityMatchDetailBinding
import com.iptvplayer.app.databinding.ActivityMyMatchBinding
import com.iptvplayer.app.databinding.ActivitySportMatchesBinding
import com.iptvplayer.app.databinding.FragmentSportBinding
import com.iptvplayer.app.databinding.ItemDateBinding
import com.iptvplayer.app.databinding.ItemMatchBinding
import com.iptvplayer.app.databinding.SheetListBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SheetRows
import com.iptvplayer.app.ui.common.SimpleAdapter
import com.iptvplayer.app.ui.common.addToolbarButton
import com.iptvplayer.app.ui.player.PlayRequest
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.toast
import com.iptvplayer.app.util.visible
import com.iptvplayer.app.work.MatchReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// ======================= shared =======================

@HiltViewModel
class SportViewModel @Inject constructor(val repo: SportRepository, private val settings: SettingsStore) : ViewModel() {
    val matches = MutableStateFlow<List<Match>>(emptyList())
    val loading = MutableStateFlow(true)
    val error = MutableStateFlow(false)
    val followed = repo.observeFavourites().map { list -> list.map { it.eventId }.toSet() }
    private var poll: Job? = null

    init { load() }

    /** Today → +14 days for the followed leagues (covers international breaks); re-polls every 30 s while any match is live. */
    fun load() {
        poll?.cancel()
        poll = viewModelScope.launch {
            while (isActive) {
                loading.value = matches.value.isEmpty()
                val slugs = settings.current().selectedLeagues
                val from = SportRepository.startOfDay(); val to = SportRepository.startOfDay(days = 14)
                val list = repo.matches(slugs, from, to, maxAgeMs = if (matches.value.any { it.state == MatchState.LIVE }) 20_000 else 60_000)
                error.value = list.isEmpty() && matches.value.isEmpty()
                if (list.isNotEmpty() || matches.value.isEmpty()) matches.value = list
                loading.value = false
                delay(if (list.any { it.state == MatchState.LIVE }) 30_000 else 5 * 60_000)
            }
        }
    }

    fun toggleFollow(ctx: android.content.Context, m: Match, followed: Boolean) = viewModelScope.launch {
        if (followed) { repo.unfollow(m.id); MatchReminderWorker.cancel(ctx, m.id); ctx.toast(R.string.reminder_off) }
        else { repo.follow(m); MatchReminderWorker.schedule(ctx, m.id, "${m.home} vs ${m.away}", m.league.name, m.startTime); ctx.toast(R.string.reminder_on) }
    }
}

private val TIME = SimpleDateFormat("HH:mm", Locale.getDefault())
private val DOW = SimpleDateFormat("EEE", Locale.getDefault())

private fun dayLabel(ctx: android.content.Context, t: Long): String {
    val c = Calendar.getInstance(); val today = c.get(Calendar.DAY_OF_YEAR); val year = c.get(Calendar.YEAR)
    c.timeInMillis = t
    return when {
        c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today -> ctx.getString(R.string.today)
        c.get(Calendar.YEAR) == year && c.get(Calendar.DAY_OF_YEAR) == today + 1 -> ctx.getString(R.string.tomorrow)
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(t))
    }
}

private fun crest(tv: TextView, name: String, short: String, color: Int?) {
    tv.text = short.take(3).uppercase(Locale.ROOT)
    // ESPN colours can be pure black/white (unreadable on the dark card / with white text) → use the generated colour.
    val usable = color?.takeIf { androidx.core.graphics.ColorUtils.calculateLuminance(it) in 0.02..0.6 }
    tv.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(usable ?: LogoUtil.color(name)) }
}

fun bindMatch(b: ItemMatchBinding, m: Match, followed: Boolean, compact: Boolean, onOpen: (Match) -> Unit, onBell: (Match) -> Unit, onWatch: (Match) -> Unit) {
    val ctx = b.root.context
    b.league.text = m.league.name
    b.home.text = m.home; b.away.text = m.away
    crest(b.homeCrest, m.home, m.homeShort, m.homeColor); crest(b.awayCrest, m.away, m.awayShort, m.awayColor)
    b.liveBadge.visible(m.state == MatchState.LIVE)
    b.btnBell.visible(m.state == MatchState.PRE)
    b.btnBell.isSelected = followed
    b.btnBell.setImageResource(if (followed) R.drawable.ic_bell_on else R.drawable.ic_bell)
    when (m.state) {
        MatchState.PRE -> {
            b.score.text = TIME.format(Date(m.startTime)); b.score.textSize = 22f
            b.clock.text = dayLabel(ctx, m.startTime); b.clock.setTextColor(ctx.getColor(R.color.text_3))
        }
        MatchState.LIVE -> {
            b.score.text = "${m.homeScore} – ${m.awayScore}"; b.score.textSize = 34f
            b.clock.text = m.clock; b.clock.setTextColor(ctx.getColor(R.color.live))
        }
        MatchState.POST -> {
            b.score.text = "${m.homeScore} – ${m.awayScore}"; b.score.textSize = 34f
            b.clock.setText(R.string.ft); b.clock.setTextColor(ctx.getColor(R.color.text_3))
        }
    }
    b.btnWatch.visible(!compact && m.state == MatchState.LIVE)
    b.btnWatch.setOnClickListener { onWatch(m) }
    b.btnBell.setOnClickListener { onBell(m) }
    b.root.setOnClickListener { onOpen(m) }
}

// ======================= Tab: SportFragment =======================

@AndroidEntryPoint
class SportFragment : BaseFragment<FragmentSportBinding>(FragmentSportBinding::inflate) {
    private val vm: SportViewModel by activityViewModels()
    private var followed: Set<String> = emptySet()
    private var sport = "soccer"

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.btnBack.visible(false)
        binding.toolbar.tvTitle.setText(R.string.sport_title)
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_trophy, boxed = true, desc = R.string.leagues_followed) { LeaguePickerSheet.show(childFragmentManager) }
        childFragmentManager.setFragmentResultListener(LeaguePickerSheet.RESULT, viewLifecycleOwner) { _, _ -> vm.load() }
        sport = savedInstanceState?.getString("sport") ?: sport
        listOf("soccer" to "Soccer", "basketball" to "Basketball").forEach { (key, s) ->
            binding.sports.addView(Chip(requireContext(), null, 0).apply {
                setTextAppearance(R.style.Text_Label); text = s; isCheckable = true; isCheckedIconVisible = false; isChecked = key == sport
                id = android.view.View.generateViewId(); setOnClickListener { sport = key; render() }
                chipBackgroundColor = requireContext().getColorStateList(R.color.chip_bg); chipStrokeColor = requireContext().getColorStateList(R.color.chip_stroke)
                chipStrokeWidth = 1.5f * resources.displayMetrics.density; setTextColor(requireContext().getColorStateList(R.color.chip_text))
            })
        }
        binding.refresh.setColorSchemeColors(requireContext().getColor(R.color.accent))
        binding.refresh.setProgressBackgroundColorSchemeColor(requireContext().getColor(R.color.surface_2))
        binding.refresh.setOnRefreshListener { vm.load() }
        header(binding.secLive, R.string.live_now, null)
        header(binding.secMine, R.string.my_matches) { Nav.myMatches(requireContext()) }
        header(binding.secUpcoming, R.string.upcoming) { Nav.sportMatches(requireContext()) }

        collect(vm.followed) { followed = it; render() }
        collect(vm.matches) { render() }
        collect(vm.loading) { binding.loading.visible(it); if (!it) binding.refresh.isRefreshing = false; render() }
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("sport", sport) }

    private fun header(h: com.iptvplayer.app.databinding.LayoutSectionHeaderBinding, title: Int, onAll: (() -> Unit)?) {
        h.secTitle.setText(title)
        h.secAction.visible(onAll != null); h.secAction.setText(if (title == R.string.upcoming) R.string.schedule else R.string.see_all)
        h.secAction.setOnClickListener { onAll?.invoke() }
    }

    private fun render() {
        if (view == null) return
        val all = vm.matches.value.filter { it.league.sport == sport }
        val live = all.filter { it.state == MatchState.LIVE }
        val mine = all.filter { it.id in followed && it.state != MatchState.POST }
        val up = all.filter { it.state == MatchState.PRE }.take(8)
        fill(binding.live, live, false); fill(binding.mine, mine, true); fill(binding.upcoming, up, true)
        binding.secLive.root.visible(live.isNotEmpty()); binding.live.visible(live.isNotEmpty())
        binding.secLive.secAction.visible(live.isNotEmpty()); binding.secLive.secAction.text = getString(R.string.n_matches, live.size)
        binding.secLive.secAction.setTextColor(requireContext().getColor(R.color.live))
        binding.mineHint.visible(mine.isEmpty())
        binding.secUpcoming.root.visible(up.isNotEmpty())
        val empty = all.isEmpty() && !vm.loading.value
        binding.empty.root.visible(empty)
        if (empty) {
            binding.empty.emptyIcon.setImageResource(R.drawable.ic_calendar)
            binding.empty.emptyTitle.setText(if (vm.error.value) R.string.sport_load_error else R.string.no_matches)
            binding.empty.emptyBody.setText(R.string.no_matches_body)
        }
    }

    private fun fill(box: LinearLayout, list: List<Match>, compact: Boolean) {
        box.removeAllViews()
        list.forEach { m ->
            val b = ItemMatchBinding.inflate(LayoutInflater.from(requireContext()), box, false)
            bindMatch(b, m, m.id in followed, compact,
                onOpen = { Nav.match(requireContext(), it.league.slug, it.id, it.startTime) },
                onBell = { vm.toggleFollow(requireContext().applicationContext, it, it.id in followed) },
                onWatch = { WatchMatchSheet.show(childFragmentManager, "${it.home} ${it.away}") })
            box.addView(b.root)
        }
    }
}

// ======================= Schedule (date strip + league filter) =======================

@AndroidEntryPoint
class SportMatchesActivity : BaseActivity<ActivitySportMatchesBinding>(ActivitySportMatchesBinding::inflate) {
    @Inject lateinit var repo: SportRepository
    @Inject lateinit var settings: SettingsStore
    private val vm: SportViewModel by viewModels()
    private var dayOffset = 0
    private var league: String? = null
    private var followed: Set<String> = emptySet()
    private var list: List<Match> = emptyList()

    private val adapter = SimpleAdapter<Match, ItemMatchBinding>(ItemMatchBinding::inflate, { a, b -> a.id == b.id }) { b, m, _ ->
        bindMatch(b, m, m.id in followed, true, { Nav.match(this, it.league.slug, it.id, it.startTime) }, { vm.toggleFollow(applicationContext, it, it.id in followed) }, {})
    }

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.schedule)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        addToolbarButton(binding.toolbar.actions, R.drawable.ic_trophy, desc = R.string.leagues_followed) { LeaguePickerSheet.show(supportFragmentManager) }
        supportFragmentManager.setFragmentResultListener(LeaguePickerSheet.RESULT, this) { _, _ -> buildLeagues(); load() }
        binding.rv.adapter = adapter
        val days = (-1..6).toList()
        val dates = SimpleAdapter<Int, ItemDateBinding>(ItemDateBinding::inflate, { a, b -> a == b }) { b, off, _ ->
            val t = System.currentTimeMillis() + off * 86_400_000L
            b.dow.text = when (off) { 0 -> getString(R.string.today); -1 -> getString(R.string.yesterday); else -> DOW.format(Date(t)) }
            b.day.text = SimpleDateFormat("d", Locale.getDefault()).format(Date(t))
            b.root.isSelected = off == dayOffset
            b.root.setOnClickListener { dayOffset = off; binding.dates.adapter?.notifyDataSetChanged(); load() }
        }
        binding.dates.adapter = dates
        dates.submitList(days)
        binding.dates.scrollToPosition(1)
        buildLeagues()
        collect(vm.followed) { followed = it; adapter.notifyDataSetChanged() }
        load()
    }

    private fun buildLeagues() = lifecycleScope.launch {
        val selected = settings.current().selectedLeagues
        binding.leagues.removeAllViews()
        (listOf<Pair<String?, String>>(null to getString(R.string.all)) + repo.leagues.filter { it.slug in selected }.map { it.slug to it.name }).forEach { (slug, name) ->
            binding.leagues.addView(Chip(this@SportMatchesActivity, null, 0).apply {
                setTextAppearance(R.style.Text_Label); text = name; isCheckable = true; isCheckedIconVisible = false; isChecked = slug == league
                chipBackgroundColor = getColorStateList(R.color.chip_bg); chipStrokeColor = getColorStateList(R.color.chip_stroke)
                chipStrokeWidth = 1.5f * resources.displayMetrics.density; setTextColor(getColorStateList(R.color.chip_text))
                setOnClickListener { league = slug; render() }
            })
        }
    }

    private fun load() = lifecycleScope.launch {
        binding.loading.visible(true); binding.empty.root.visible(false)
        val from = SportRepository.startOfDay(days = dayOffset)
        list = repo.matches(settings.current().selectedLeagues, from, from + SportRepository.DAY).filter { it.startTime >= from }
        binding.loading.visible(false)
        render()
    }

    private fun render() {
        val shown = list.filter { league == null || it.league.slug == league }
        adapter.submitList(shown)
        binding.empty.root.visible(shown.isEmpty() && binding.loading.visibility != android.view.View.VISIBLE)
        binding.empty.emptyIcon.setImageResource(R.drawable.ic_calendar)
        binding.empty.emptyTitle.setText(R.string.no_matches)
        binding.empty.emptyBody.setText(R.string.no_matches_body)
    }
}

// ======================= My matches =======================

@AndroidEntryPoint
class MyMatchActivity : BaseActivity<ActivityMyMatchBinding>(ActivityMyMatchBinding::inflate) {
    private val vm: SportViewModel by viewModels()
    private var followed: Set<String> = emptySet()

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.tvTitle.setText(R.string.my_matches)
        binding.toolbar.btnBack.setOnClickListener { finish() }
        val adapter = SimpleAdapter<Match, ItemMatchBinding>(ItemMatchBinding::inflate, { a, b -> a.id == b.id }) { b, m, _ ->
            bindMatch(b, m, true, true, { Nav.match(this, it.league.slug, it.id, it.startTime) }, { vm.toggleFollow(applicationContext, it, true) }, {})
        }
        binding.rv.adapter = adapter
        fun render() {
            val list = vm.matches.value.filter { it.id in followed }
            adapter.submitList(list)
            binding.empty.root.visible(list.isEmpty() && !vm.loading.value)
            binding.empty.emptyIcon.setImageResource(R.drawable.ic_bell)
            binding.empty.emptyTitle.setText(R.string.no_follow_title)
            binding.empty.emptyBody.setText(R.string.no_follow_body)
        }
        collect(vm.followed) { followed = it; render() }
        collect(vm.matches) { render() }
    }
}

// ======================= Match detail =======================

@HiltViewModel
class MatchDetailViewModel @Inject constructor(private val repo: SportRepository, handle: SavedStateHandle) : ViewModel() {
    private val slug: String = handle[Nav.EXTRA_LEAGUE]!!
    private val id: String = handle[Nav.EXTRA_ID]!!
    private val time: Long = handle[Nav.EXTRA_TIME] ?: System.currentTimeMillis()
    val match = MutableStateFlow<Match?>(null)
    val followed = repo.observeFavourites().map { l -> l.any { it.eventId == id } }

    init {
        viewModelScope.launch {
            while (isActive) {
                repo.match(slug, id, time)?.let { match.value = it }
                delay(if (match.value?.state == MatchState.LIVE) 30_000 else 300_000)
            }
        }
    }
    fun follow(on: Boolean) = viewModelScope.launch { match.value?.let { if (on) repo.follow(it) else repo.unfollow(it.id) } }
}

@AndroidEntryPoint
class MatchDetailActivity : BaseActivity<ActivityMatchDetailBinding>(ActivityMatchDetailBinding::inflate) {
    private val vm: MatchDetailViewModel by viewModels()
    private var followed = false

    override fun setup(savedInstanceState: Bundle?) {
        binding.toolbar.btnBack.setOnClickListener { finish() }
        val bell = addToolbarButton(binding.toolbar.actions, R.drawable.ic_bell, desc = R.string.reminder_on) {
            val m = vm.match.value ?: return@addToolbarButton
            vm.follow(!followed)
            if (followed) { MatchReminderWorker.cancel(this, m.id); toast(R.string.reminder_off) }
            else { MatchReminderWorker.schedule(this, m.id, "${m.home} vs ${m.away}", m.league.name, m.startTime); toast(R.string.reminder_on) }
        }
        collect(vm.followed) { followed = it; bell.setImageResource(if (it) R.drawable.ic_bell_on else R.drawable.ic_bell); bell.setColorFilter(getColor(if (it) R.color.accent else R.color.text)) }
        collect(vm.match) { m ->
            m ?: return@collect
            binding.toolbar.tvTitle.text = m.league.name
            bindMatch(binding.card, m, followed, true, {}, {}, {})
            binding.card.btnBell.visible(false)
            binding.btnWatchMain.setText(if (m.state == MatchState.POST) R.string.find_replay else R.string.watch_on_your_channel)
            binding.btnWatchMain.setOnClickListener { WatchMatchSheet.show(supportFragmentManager, "${m.home} ${m.away}") }
            binding.secEvents.visible(m.events.isNotEmpty()); binding.events.visible(m.events.isNotEmpty())
            binding.events.removeAllViews()
            m.events.forEach { e ->
                val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, 5.dp, 0, 5.dp) }
                val icon = when (e.kind) { "goal" -> "⚽"; "red" -> "🟥"; else -> "🟨" }
                val left = TextView(this).apply { text = if (e.home) "$icon ${e.text}" else ""; gravity = Gravity.END; setTextAppearance(R.style.Text); textSize = 13f }
                val mid = TextView(this).apply { text = e.minute; gravity = Gravity.CENTER; setTextAppearance(R.style.Text_Cond); textSize = 14f; setTextColor(getColor(R.color.text_2)) }
                val right = TextView(this).apply { text = if (!e.home) "$icon ${e.text}" else ""; setTextAppearance(R.style.Text); textSize = 13f }
                row.addView(left, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(mid, LinearLayout.LayoutParams(52.dp, -2)); row.addView(right, LinearLayout.LayoutParams(0, -2, 1f))
                binding.events.addView(row)
            }
            binding.venueCard.visible(!m.venue.isNullOrBlank()); binding.venue.text = m.venue
        }
    }
}

// ======================= Sheets =======================

/** Followed leagues (default: the original app's hot leagues). */
@AndroidEntryPoint
class LeaguePickerSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {
    @Inject lateinit var repo: SportRepository
    @Inject lateinit var settings: SettingsStore

    override fun setup(savedInstanceState: Bundle?) {
        SheetRows.header(binding, getString(R.string.leagues_followed)) { dismiss() }
        binding.header.sheetEnd.setImageResource(R.drawable.ic_check)
        lifecycleScope.launch {
            val selected = settings.current().selectedLeagues.toMutableSet()
            repo.leagues.forEach { l ->
                val sw = MaterialSwitch(requireContext()).apply { isChecked = l.slug in selected; isClickable = false }
                val row = SheetRows.option(binding.container, l.name, l.sport.replaceFirstChar { it.uppercase() }, trailing = sw) {
                    sw.toggle(); if (sw.isChecked) selected += l.slug else selected -= l.slug
                }
                row.title.setTextColor(requireContext().getColor(R.color.text))
            }
            binding.header.sheetEnd.setOnClickListener {
                lifecycleScope.launch { settings.setSelectedLeagues(selected); parentFragmentManager.setFragmentResult(RESULT, Bundle()); dismiss() }
            }
        }
    }

    companion object {
        const val RESULT = "leagues_changed"
        fun show(fm: FragmentManager) = LeaguePickerSheet().show(fm, "leagues")
    }
}

/**
 * "Watch on your channel": searches the user's own sources for sport channels / team names.
 * No stream links come from us (the original pushed links via Remote Config).
 */
@AndroidEntryPoint
class WatchMatchSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {
    @Inject lateinit var playlists: PlaylistRepository
    @Inject lateinit var xtream: XtreamRepository

    override fun setup(savedInstanceState: Bundle?) {
        SheetRows.header(binding, getString(R.string.pick_channel)) { dismiss() }
        val q = requireArguments().getString("q").orEmpty().split(' ').firstOrNull().orEmpty()
        lifecycleScope.launch {
            val hasPlaylists = playlists.observePlaylists().first().isNotEmpty()
            val profiles = xtream.observeProfiles().first()
            val c = binding.container
            if (!hasPlaylists && profiles.isEmpty()) {
                val empty = LayoutInflater.from(requireContext()).inflate(R.layout.layout_empty_state, c, false)
                empty.findViewById<android.widget.ImageView>(R.id.emptyIcon).setImageResource(R.drawable.ic_tv)
                empty.findViewById<TextView>(R.id.emptyTitle).setText(R.string.add_source_to_watch)
                empty.findViewById<TextView>(R.id.emptyBody).setText(R.string.add_source_to_watch_body)
                c.addView(empty)
                SheetRows.option(c, getString(R.string.add_playlist), icon = R.drawable.ic_link) { dismiss(); Nav.import(requireContext(), "url") }
                SheetRows.option(c, getString(R.string.add_xtream), icon = R.drawable.ic_xtream) { dismiss(); Nav.addProfile(requireContext()) }
                return@launch
            }
            c.addView(TextView(requireContext()).apply { setText(R.string.sports_in_sources); setTextAppearance(R.style.Text_Hint); setPadding(12.dp, 0, 12.dp, 8.dp) })
            val list: List<ChannelEntity> = playlists.sportsLike(q).first()
            list.take(30).forEach { ch ->
                val row = SheetRows.option(c, ch.name, ch.groupName, icon = R.drawable.ic_play) { dismiss(); Nav.play(requireContext(), PlayRequest.Channel(ch.playlistId, ch.id)) }
                row.icon.setColorFilter(requireContext().getColor(R.color.accent))
            }
            profiles.forEach { p ->
                SheetRows.option(c, p.name, getString(R.string.search_live), icon = R.drawable.ic_xtream) { dismiss(); Nav.xtreamHome(requireContext(), p.id) }
            }
            if (list.isEmpty() && profiles.isEmpty()) c.addView(TextView(requireContext()).apply { setText(R.string.no_results); setTextAppearance(R.style.Text_Body_Secondary); setPadding(12.dp, 8.dp, 12.dp, 8.dp) })
        }
    }

    companion object {
        fun show(fm: FragmentManager, q: String) = WatchMatchSheet().apply { arguments = bundleOf("q" to q) }.show(fm, "watch_match")
    }
}

