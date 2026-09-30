package com.iptvplayer.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptvplayer.app.data.database.ChannelEntity
import com.iptvplayer.app.data.database.PlaylistEntity
import com.iptvplayer.app.data.database.SingleStreamEntity
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.data.repository.XtreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val loaded: Boolean = false,
    val playlists: List<PlaylistEntity> = emptyList(),
    val profiles: List<XtreamProfileEntity> = emptyList(),
    val singles: List<SingleStreamEntity> = emptyList(),
    val recent: List<ChannelEntity> = emptyList(),
    val favorites: List<ChannelEntity> = emptyList(),
) {
    val hasSources get() = playlists.isNotEmpty() || profiles.isNotEmpty() || singles.isNotEmpty()
}

/** Shared by the MainActivity tab fragments (activityViewModels). */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val playlists: PlaylistRepository,
    private val xtream: XtreamRepository,
) : ViewModel() {

    val homeChip = MutableStateFlow(HomeChip.RECENT)

    val state: StateFlow<HomeState> = combine(
        playlists.observePlaylists(), xtream.observeProfiles(), playlists.singles(), playlists.recent(12), playlists.favorites(),
    ) { p, x, s, r, f -> HomeState(true, p, x, s, r, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun togglePlaylistFav(id: Long) = viewModelScope.launch { playlists.toggleFavorite(id) }
    fun toggleChannelFav(id: Long) = viewModelScope.launch { playlists.toggleChannelFavorite(id) }
    fun deletePlaylist(id: Long) = viewModelScope.launch { playlists.delete(id) }
    fun deleteSingle(id: Long) = viewModelScope.launch { playlists.deleteSingle(id) }
    fun setProfileLocked(id: String, locked: Boolean) = viewModelScope.launch { xtream.setLocked(id, locked) }
    fun deleteProfile(id: String) = viewModelScope.launch { xtream.deleteProfile(id) }
    fun clearNew(id: String) = viewModelScope.launch { xtream.clearNew(id) }
}

enum class HomeChip { RECENT, FAVOURITE }
