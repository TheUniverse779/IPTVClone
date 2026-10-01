package com.iptvplayer.app.ui.splash

import com.iptvplayer.app.App
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.iptvplayer.app.base.BaseActivity
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.databinding.ActivitySplashBinding
import com.iptvplayer.app.ui.disclaimer.DisclaimerActivity
import com.iptvplayer.app.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {
    @Inject lateinit var settings: SettingsStore

    override fun setup(savedInstanceState: Bundle?) {
        // First launch: English by default (not the phone's language). Done once; after that the user's
        // choice in Settings › Language or Android's per-app language screen always wins.
        lifecycleScope.launch {
            if (!settings.languageDefaulted()) {
                settings.setLanguageDefaulted()
                if (AppCompatDelegate.getApplicationLocales().isEmpty) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(App.DEFAULT_LANGUAGE))
                }
            }
        }
        ValueAnimator.ofInt(0, 100).apply {
            duration = MIN_SPLASH_MS
            addUpdateListener { binding.progress.progress = it.animatedValue as Int }
            start()
        }
        lifecycleScope.launch {
            val s = settings.current()
            delay(MIN_SPLASH_MS)
            when {
                !s.firstRunDone -> openFirstRunFlow()
                !s.disclaimerAccepted -> start(DisclaimerActivity::class.java)
                else -> start(MainActivity::class.java)
            }
        }
    }

    /**
     * Hook for your own first-run flow (language picker + onboarding). When it finishes, call
     * `settings.setFirstRunDone()` and open [DisclaimerActivity]. Until that flow exists we go
     * straight to the disclaimer.
     */
    private suspend fun openFirstRunFlow() {
        settings.setFirstRunDone()
        start(DisclaimerActivity::class.java)
    }

    private fun start(cls: Class<*>) {
        startActivity(Intent(this, cls))
        finish()
    }

    companion object { private const val MIN_SPLASH_MS = 900L }
}
