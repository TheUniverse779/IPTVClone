package com.iptvplayer.app.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.iptvplayer.app.ui.player.PlayerActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MediaSession for lock-screen / notification controls. Started only when the user enables
 * "Keep playing when the screen is off"; otherwise PlayerActivity pauses on stop.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var manager: PlayerManager
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(this, 0, Intent(this, PlayerActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        session = MediaSession.Builder(this, manager.player).setSessionActivity(open).build()
            // Register now (not only when a controller connects) so the media notification / foreground
            // state kicks in as soon as playback continues with the screen off.
            .also { addSession(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (session?.player?.playWhenReady != true) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
