package com.iptvplayer.app.ui.xtream

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commitNow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.iptvplayer.app.R
import com.iptvplayer.app.ads.AppAds
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.base.BaseBottomSheet
import com.iptvplayer.app.base.collect
import com.iptvplayer.app.data.database.MediaType
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.data.repository.XtreamRepository
import com.iptvplayer.app.databinding.ActivityXtreamHomeBinding
import com.iptvplayer.app.databinding.SheetListBinding
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.common.SheetRows
import com.iptvplayer.app.ui.common.SourceActions
import com.iptvplayer.app.ui.common.hasVod
import com.iptvplayer.app.ui.common.isExpired
import com.iptvplayer.app.ui.main.MainActivity
import com.iptvplayer.app.util.LogoUtil
import com.iptvplayer.app.util.dp
import com.iptvplayer.app.util.TimeFmt
import com.iptvplayer.app.util.visible
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared by XtreamHomeActivity and its 4 tab fragments (activityViewModels). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class XtreamHomeViewModel @Inject constructor(val repo: XtreamRepository, handle: SavedStateHandle) : ViewModel() {
    val profileId: String = handle[Nav.EXTRA_PROFILE]!!
    val profile = repo.observeProfile(profileId)

    // Movies tab
    val latestVod = repo.latestVod(profileId)
    val latestSeries = repo.latestSeries(profileId)
    val continueWatching = repo.continueWatching(profileId, 12)
    val vodCats = repo.categories(profileId, MediaType.MOVIE)
    val seriesCats = repo.categories(profileId, MediaType.SERIES)
    val hero = MutableStateFlow<com.iptvplayer.app.data.database.XtreamVodEntity?>(null)

    /** Newest titles of one genre, for its poster row. */
    suspend fun genreRow(c: com.iptvplayer.app.data.database.XtreamCategoryEntity, limit: Int = 15): List<Poster> =
        if (c.type == MediaType.SERIES) repo.seriesInCategory(profileId, c.categoryId, limit).map { it.toPoster() }
        else repo.vodInCategory(profileId, c.categoryId, limit).map { it.toPoster() }

    // Live tab
    val liveCats = repo.categories(profileId, MediaType.LIVE)
    val liveCat = MutableStateFlow<String?>(null)
    val liveQuery = MutableStateFlow("")
    val live = combine(liveCat, liveQuery) { c, q -> c to q }.flatMapLatest { (c, q) ->
        Pager(PagingConfig(60, enablePlaceholders = false)) { repo.livePaged(profileId, c, q) }.flow
    }.cachedIn(viewModelScope)

    // Search tab
    val query = MutableStateFlow("")
    val searchVod = query.flatMapLatest { if (it.isBlank()) flowOf(emptyList()) else repo.searchVod(profileId, it) }
    val searchSeries = query.flatMapLatest { if (it.isBlank()) flowOf(emptyList()) else repo.searchSeries(profileId, it) }
    val searchLive = query.flatMapLatest { if (it.isBlank()) flowOf(emptyList()) else repo.searchLive(profileId, it) }

    // Favorite tab
    val favVod = repo.vodFavorites(profileId)
    val favSeries = repo.seriesFavorites(profileId)
    val favLive = repo.liveFavorites(profileId)

    init { viewModelScope.launch { hero.value = repo.randomVod(profileId) } }

    fun toggleLiveFav(streamId: Int) = viewModelScope.launch { repo.toggleLiveFavorite(profileId, streamId) }
}

/** Xtream home: BottomNavigationView with 4 tab fragments (like the original), profile switcher in the title. */
@AndroidEntryPoint
class XtreamHomeActivity : BaseActivity<ActivityXtreamHomeBinding>(ActivityXtreamHomeBinding::inflate) {
    private val vm: XtreamHomeViewModel by viewModels()
    private var current = R.id.nav_movie
    private lateinit var actions: SourceActions

    /** Banner lives in the layout, directly above the tab bar. */
    override val showAdBanner = false

    override fun setup(savedInstanceState: Bundle?) {
        AppAds.showBanner(this, binding.adBanner)
        actions = SourceActions(this) {}.register()
        actions.profileLookup = { vm.repo.profile(it) }
        actions.onOpened = { finish() }
        binding.btnBack.setOnClickListener { finish() }
        binding.titleBtn.setOnClickListener { ProfileSwitcherSheet.show(supportFragmentManager, vm.profileId) }
        binding.btnMore.setOnClickListener { v ->
            PopupMenu(this, v).apply {
                menu.add(0, 1, 0, R.string.account_info).setIcon(R.drawable.ic_info)
                menu.add(0, 2, 1, R.string.resync).setIcon(R.drawable.ic_refresh)
                menu.add(0, 3, 2, R.string.continue_watching).setIcon(R.drawable.ic_history)
                menu.add(0, 4, 3, R.string.edit_profile).setIcon(R.drawable.ic_edit)
                setForceShowIcon(true)
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        1 -> AccountSheet.show(supportFragmentManager, vm.profileId)
                        2 -> XtreamSyncDialog.resync(supportFragmentManager, vm.profileId)
                        3 -> Nav.xtreamRecent(this@XtreamHomeActivity, vm.profileId)
                        4 -> Nav.editProfile(this@XtreamHomeActivity, vm.profileId)
                    }
                    true
                }
            }.show()
        }
        binding.btnWarnEdit.setOnClickListener { Nav.editProfile(this, vm.profileId) }
        binding.bnav.setOnItemSelectedListener { select(it.itemId); true }

        supportFragmentManager.setFragmentResultListener(ProfileSwitcherSheet.RESULT_PICK, this) { _, b ->
            val id = b.getString("id")!!
            // Opens the other profile (passcode / expiry checks included); this screen stays until it opens.
            if (id != vm.profileId) lifecycleScope.launch { vm.repo.profile(id)?.let { actions.openProfile(it) } }
        }

        var first = savedInstanceState == null
        collect(vm.profile) { p ->
            if (p == null) { finish(); return@collect }
            render(p)
            if (first) {
                first = false
                vm.repo.clearNew(p.id)
                val start = if (hasVod(p)) R.id.nav_movie else R.id.nav_live
                binding.bnav.selectedItemId = start // fires the listener → select(start)
                if (current != start) select(start)
            }
        }
        if (savedInstanceState != null) current = savedInstanceState.getInt("tab", R.id.nav_movie)
    }

    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putInt("tab", current) }

    private fun render(p: XtreamProfileEntity) {
        LogoUtil.avatar(binding.avatar, p.name, p.avatarColor, 10)
        binding.name.text = p.name
        val expired = isExpired(p)
        binding.sub.text = if (expired) getString(R.string.expired_on, TimeFmt.date(p.expDate)) else getString(R.string.valid_until, TimeFmt.date(p.expDate))
        binding.warnBar.visible(expired)
    }

    private fun select(id: Int) {
        current = id
        val fm = supportFragmentManager
        // commitNow: findFragmentByTag must see the fragment added by a previous call immediately,
        // otherwise two quick selects add the same tab twice (stacked, overlapping UIs).
        fm.commitNow {
            setReorderingAllowed(true)
            listOf(R.id.nav_movie, R.id.nav_live, R.id.nav_search, R.id.nav_fav).forEach { t ->
                val tag = "xt_$t"
                val f = fm.findFragmentByTag(tag)
                if (t == id) { if (f == null) add(binding.fragmentContainer.id, create(t), tag) else show(f) } else f?.let { hide(it) }
            }
        }
    }

    private fun create(id: Int): Fragment = when (id) {
        R.id.nav_live -> XtreamLiveFragment()
        R.id.nav_search -> XtreamSearchFragment()
        R.id.nav_fav -> XtreamFavoriteFragment()
        else -> XtreamMovieFragment()
    }
}

/** Title tap on Xtream home: switch profile without going back to Who's watching. */
@AndroidEntryPoint
class ProfileSwitcherSheet : BaseBottomSheet<SheetListBinding>(SheetListBinding::inflate) {
    @Inject lateinit var repo: XtreamRepository

    override fun setup(savedInstanceState: Bundle?) {
        SheetRows.header(binding, getString(R.string.switch_profile)) { dismiss() }
        val cur = requireArguments().getString("id")
        collect(repo.observeProfiles()) { list ->
            val c = binding.container
            c.removeAllViews()
            list.forEach { p ->
                val sub = when {
                    isExpired(p) -> getString(R.string.expired_on, TimeFmt.date(p.expDate))
                    !hasVod(p) -> getString(R.string.live_only)
                    else -> getString(R.string.valid_until, TimeFmt.date(p.expDate))
                }
                val row = SheetRows.option(c, p.name, sub, checked = p.id == cur) {
                    dismiss()
                    parentFragmentManager.setFragmentResult(RESULT_PICK, bundleOf("id" to p.id))
                }
                row.icon.visible(false)
                val av = android.widget.TextView(requireContext()).apply {
                    gravity = android.view.Gravity.CENTER; setTextAppearance(R.style.Text_Cond); textSize = 16f; setTextColor(requireContext().getColor(R.color.white))
                    LogoUtil.avatar(this, p.name, p.avatarColor, 8)
                }
                (row.root as android.widget.LinearLayout).addView(av, 0, android.widget.LinearLayout.LayoutParams(32.dp, 32.dp).apply { marginEnd = 12.dp })
                if (p.passcodeLocked) row.check.apply { visible(true); setImageResource(R.drawable.ic_lock); if (p.id != cur) setColorFilter(requireContext().getColor(R.color.text_2)) }
            }
            SheetRows.divider(c)
            SheetRows.option(c, getString(R.string.manage_profiles), icon = R.drawable.ic_user) {
                dismiss(); MainActivity.openTab(requireContext(), MainActivity.Tab.XTREAM)
            }
            SheetRows.option(c, getString(R.string.add_profile), icon = R.drawable.ic_plus) { dismiss(); Nav.addProfile(requireContext()) }
        }
    }

    companion object {
        const val RESULT_PICK = "switch_profile"
        fun show(fm: FragmentManager, currentId: String) = ProfileSwitcherSheet().apply { arguments = bundleOf("id" to currentId) }.show(fm, "switcher")
    }
}
