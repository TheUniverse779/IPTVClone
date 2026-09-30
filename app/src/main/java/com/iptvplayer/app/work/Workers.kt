package com.iptvplayer.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.iptvplayer.app.R
import com.iptvplayer.app.data.repository.PlaylistRepository
import com.iptvplayer.app.ui.splash.SplashActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

/** Re-downloads playlists with "Auto update" on, every 24 h on unmetered-or-any network. */
@HiltWorker
class PlaylistUpdateWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: PlaylistRepository,
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        repo.autoUpdatable().forEach { repo.refresh(it.id).collect() }
        return Result.success()
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<PlaylistUpdateWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("playlist_auto_update", ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}

/** Fires 15 minutes before a followed match. */
class MatchReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.success()
        val league = inputData.getString(KEY_LEAGUE).orEmpty()
        val ctx = applicationContext
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.notif_channel_match), NotificationManager.IMPORTANCE_HIGH))
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, SplashActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_sport)
            .setContentTitle(ctx.getString(R.string.notif_match_title, title))
            .setContentText(league)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(ctx).notify(inputData.getString(KEY_ID).hashCode(), n)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "match_reminder"
        const val KEY_ID = "id"; const val KEY_TITLE = "title"; const val KEY_LEAGUE = "league"

        fun schedule(ctx: Context, id: String, title: String, league: String, startTime: Long) {
            val delay = startTime - 15 * 60_000 - System.currentTimeMillis()
            if (delay <= 0) return
            val req = OneTimeWorkRequestBuilder<MatchReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_ID to id, KEY_TITLE to title, KEY_LEAGUE to league))
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("match_$id", ExistingWorkPolicy.REPLACE, req)
        }

        fun cancel(ctx: Context, id: String) = WorkManager.getInstance(ctx).cancelUniqueWork("match_$id")
    }
}
