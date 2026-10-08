package com.iptvplayer.app.ui.common

import com.iptvplayer.app.Features
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.iptvplayer.app.R
import com.iptvplayer.app.data.database.PlaylistEntity
import com.iptvplayer.app.data.database.SourceType
import com.iptvplayer.app.data.database.XtreamProfileEntity
import com.iptvplayer.app.ads.AppAds
import com.iptvplayer.app.ui.Nav
import com.iptvplayer.app.ui.community.ShareDialog
import com.iptvplayer.app.ui.importer.ImportProgressDialog
import com.iptvplayer.app.ui.passcode.PasscodeDialog
import com.iptvplayer.app.ui.playlist.EditPlaylistDialog
import com.iptvplayer.app.ui.xtream.ExpiredDialog
import com.iptvplayer.app.ui.xtream.ProfileActionsSheet
import com.iptvplayer.app.util.copyText
import com.iptvplayer.app.util.toast
import kotlinx.coroutines.launch

/**
 * Shared behaviour for playlist cards and Xtream profiles, usable from a Fragment or an Activity.
 * Every dialog result is routed through fragment result keys so it survives rotation.
 */
class SourceActions private constructor(
    private val fm: FragmentManager,
    private val owner: LifecycleOwner,
    private val context: () -> Context,
    /** Needed for ads (banner/consent); null when the caller is not a FragmentActivity. */
    private val activity: () -> FragmentActivity?,
    private val onDeletePlaylist: (Long) -> Unit,
) {
    constructor(f: Fragment, onDeletePlaylist: (Long) -> Unit) : this(f.childFragmentManager, f, { f.requireContext() }, { f.activity as? FragmentActivity }, onDeletePlaylist)
    constructor(a: FragmentActivity, onDeletePlaylist: (Long) -> Unit) : this(a.supportFragmentManager, a, { a }, { a }, onDeletePlaylist)

    /** Resolves a profile by id for ProfileActionsSheet's "Watch". */
    var profileLookup: suspend (String) -> XtreamProfileEntity? = { null }

    fun register(): SourceActions {
        fm.setFragmentResultListener(KEY_OPEN_PL, owner) { _, b ->
            if (b.getBoolean(PasscodeDialog.KEY_OK)) launchSource { Nav.playlist(context(), b.getBundle(PasscodeDialog.KEY_PAYLOAD)!!.getLong("id")) }
        }
        fm.setFragmentResultListener(KEY_DELETE_PL, owner) { _, b ->
            if (b.getInt(ConfirmDialog.KEY_WHICH) == 0) {
                onDeletePlaylist(b.getBundle(ConfirmDialog.KEY_PAYLOAD)!!.getLong("id"))
                context().toast(R.string.playlist_deleted)
            }
        }
        fm.setFragmentResultListener(KEY_OPEN_PROFILE, owner) { _, b ->
            if (b.getBoolean(PasscodeDialog.KEY_OK)) afterUnlock(b.getBundle(PasscodeDialog.KEY_PAYLOAD)!!)
        }
        fm.setFragmentResultListener(ProfileActionsSheet.RESULT_OPEN, owner) { _, b ->
            val id = b.getString("id")!!
            owner.lifecycleScope.launch { profileLookup(id)?.let { openProfile(it) } }
        }
        return this
    }

    fun openPlaylist(p: PlaylistEntity) {
        if (p.isLocked) PasscodeDialog.show(fm, KEY_OPEN_PL, PasscodeDialog.MODE_ENTER, p.name, bundleOf("id" to p.id))
        else launchSource { Nav.playlist(context(), p.id) }
    }

    fun playlistMenu(anchor: View, p: PlaylistEntity) {
        val ctx = context()
        PopupMenu(ctx, anchor).apply {
            menu.add(0, 1, 0, R.string.edit).setIcon(R.drawable.ic_edit)
            if (p.sourceType == SourceType.URL) menu.add(0, 2, 1, R.string.menu_update_now).setIcon(R.drawable.ic_refresh)
            menu.add(0, 3, 2, R.string.menu_copy_url).setIcon(R.drawable.ic_copy)
            if (Features.COMMUNITY) menu.add(0, 4, 3, R.string.menu_share_community).setIcon(R.drawable.ic_share)
            menu.add(0, 5, 4, R.string.delete).setIcon(R.drawable.ic_trash)
            setForceShowIcon(true)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> EditPlaylistDialog.show(fm, p.id)
                    2 -> ImportProgressDialog.refresh(fm, p.id, p.name)
                    3 -> { ctx.copyText(p.url); ctx.toast(R.string.copied) }
                    4 -> ShareDialog.show(fm, "iptv", p.name, p.url)
                    5 -> ConfirmDialog.show(fm, KEY_DELETE_PL, ctx.getString(R.string.delete_playlist_q), ctx.getString(R.string.delete_playlist_msg),
                        ok = ctx.getString(R.string.delete), danger = true, payload = bundleOf("id" to p.id))
                }
                true
            }
        }.show()
    }

    /** Tap on a profile: passcode (if locked) → expiry warning (if expired) → Xtream home. */
    fun openProfile(p: XtreamProfileEntity) {
        val payload = bundleOf("id" to p.id, "name" to p.name, "expired" to isExpired(p), "exp" to p.expDate)
        if (p.passcodeLocked) PasscodeDialog.show(fm, KEY_OPEN_PROFILE, PasscodeDialog.MODE_ENTER, p.name, payload) else afterUnlock(payload)
    }

    private fun afterUnlock(b: Bundle) {
        val id = b.getString("id")!!
        if (b.getBoolean("expired")) ExpiredDialog.show(fm, id, b.getString("name")!!, b.getLong("exp"))
        else launchSource { Nav.xtreamHome(context(), id); onOpened() }
    }

    /**
     * Opening a source is a natural break in the app: show the interstitial first (the SDK applies its
     * own cooldown), then navigate. Falls through immediately when ads are off or have no fill.
     */
    private fun launchSource(navigate: () -> Unit) {
        val act = activity()
        if (act == null || act.isFinishing || act.isDestroyed) navigate() else act.runOnUiThread { AppAds.showInterstitial(act, navigate) }
    }

    /** Called after Xtream home is launched (XtreamHomeActivity finishes itself when switching). */
    var onOpened: () -> Unit = {}

    companion object {
        private const val KEY_OPEN_PL = "src_open_playlist"
        private const val KEY_DELETE_PL = "src_delete_playlist"
        private const val KEY_OPEN_PROFILE = "src_open_profile"
    }
}
