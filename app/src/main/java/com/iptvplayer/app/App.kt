package com.iptvplayer.app

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ezt.v2.ezt.admobdemo.ads.core.AdUnitDefaults
import com.ezt.v2.ezt.admobdemo.ads.core.AdsHostConfig
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.google.firebase.analytics.FirebaseAnalytics
import com.iptvplayer.app.work.MatchReminderWorker
import com.iptvplayer.app.work.PlaylistUpdateWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {
    companion object { const val DEFAULT_LANGUAGE = "en" }

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        configureAds()
        PlaylistUpdateWorker.schedule(this)
        // Sport hidden: drop match reminders scheduled earlier so no sport notification fires.
        // (Worker class name is an implicit tag, so this also covers reminders scheduled before the explicit tag existed.)
        if (!Features.SPORT) androidx.work.WorkManager.getInstance(this).cancelAllWorkByTag(MatchReminderWorker::class.java.name)
    }

    /**
     * Ads SDK host config, once per process. The app is free (no purchase removes ads), so [AdsHostConfig.isAdFree]
     * is false; wire it to a real "remove ads" purchase when that exists. Consent + SDK initialisation happen at
     * Splash ([com.iptvplayer.app.ads.AppAds.initializeFromSplash]).
     */
    private fun configureAds() {
        if (!Features.ADS) return
        val analytics = FirebaseAnalytics.getInstance(this)
        val accepted = AdsSdk.configure(
            AdsHostConfig(
                adUnits = AdUnitDefaults(appId = getString(R.string.admob_app_id)),
                isAdFree = { false },
                onEvent = { analytics.logEvent(it, null) },
                onPaid = { micros, currency -> recordAdRevenue(analytics, micros, currency) },
            ),
        )
        if (!accepted) Log.e("AppAds", "Package rejected: ${AdsSdk.accessError}")
    }

    /** Maps the app's own events; revenue goes to Google Play Console, not to Firebase. */
    private fun recordAdRevenue(analytics: FirebaseAnalytics, micros: Long, currency: String) {
        if (micros <= 0 || currency.isBlank()) return
        analytics.logEvent("ad_impression_value", Bundle().apply {
            putDouble(FirebaseAnalytics.Param.VALUE, micros / 1_000_000.0)
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
        })
    }
}
