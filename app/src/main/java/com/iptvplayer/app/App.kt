package com.iptvplayer.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.iptvplayer.app.work.MatchReminderWorker
import com.iptvplayer.app.work.PlaylistUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        PlaylistUpdateWorker.schedule(this)
        // Sport hidden: drop match reminders scheduled earlier so no sport notification fires.
        // (Worker class name is an implicit tag, so this also covers reminders scheduled before the explicit tag existed.)
        if (!Features.SPORT) androidx.work.WorkManager.getInstance(this).cancelAllWorkByTag(MatchReminderWorker::class.java.name)
    }
}
