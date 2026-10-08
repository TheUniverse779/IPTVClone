package com.iptvplayer.app.ads

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.ezt.v2.ezt.admobdemo.ads.placement.AdFullScreenResult
import com.ezt.v2.ezt.admobdemo.ads.placement.AdPreloadState
import com.ezt.v2.ezt.admobdemo.ads.placement.AdsConfigSource
import com.ezt.v2.ezt.admobdemo.ads.placement.AdsConfiguration
import com.ezt.v2.ezt.admobdemo.ads.placement.AdsKit
import com.ezt.v2.ezt.admobdemo.ads.placement.AssetAdsConfigSource
import com.ezt.v2.ezt.admobdemo.ads.placement.BannerAdOptions
import com.ezt.v2.ezt.admobdemo.ads.placement.BannerSize
import com.ezt.v2.ezt.admobdemo.ads.core.AdsSdk
import com.ezt.v2.ezt.admobdemo.ads.placement.InlineAdBinding
import com.ezt.v2.ezt.admobdemo.ads.placement.PlacementFormat
import com.ezt.v2.ezt.admobdemo.ads.placement.StartupAdsConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.iptvplayer.app.BuildConfig
import com.iptvplayer.app.Features

/**
 * One [AdsKit] for the whole app. [initializeFromSplash] runs the startup work once per process:
 * pick the placements JSON (Firebase Remote Config → cache → the JSON shipped in assets), validate
 * and apply it, then register the app-open placement. Every other screen only uses [kit].
 *
 * Placement keys live in `assets/ads/placements.json`; the debug build reads
 * `debug/assets/ads/app-placements.json` instead, so test IDs never ship in release builds.
 */
object AppAds {
    const val REMOTE_KEY = "ad_placements"

    /** Bottom banner on the two home screens. */
    const val BANNER = "main_banner"

    /** Interstitial shown before opening a source (playlist, Xtream profile, single stream). */
    const val INTERSTITIAL = "next_screen"

    private const val CACHE_KEY = "validated_json"
    private const val FETCH_TIMEOUT_MS = 2_500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initialization: Deferred<Unit>? = null

    @Volatile private var activeJson: String? = null

    lateinit var kit: AdsKit
        private set

    lateinit var configuration: AdsConfiguration
        private set

    var isInitialized = false
        private set

    /**
     * True once the ads runtime can serve requests: consent answered and the Google Mobile Ads SDK
     * initialised. Its init is asynchronous and finishes a few seconds after consent, so ads are
     * requested from this flag rather than straight after [initializeFromSplash].
     */
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    /** How long a screen waits for [ready] before giving up (then it shows no ad). */
    private const val READY_TIMEOUT_MS = 10_000L

    /** Adaptive banner: full width, height chosen by AdMob for the screen. */
    private val BANNER_OPTIONS = BannerAdOptions(bannerSize = BannerSize.ADAPTIVE)

    @MainThread
    suspend fun initializeFromSplash(context: Context): AdsKit {
        if (initialization == null) {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(
                if (BuildConfig.DEBUG) "startup_ads_debug" else "startup_ads_release",
                Context.MODE_PRIVATE,
            )
            kit = AdsKit.Builder(app)
                .manageHelperAds()
                .configSource(AdsConfigSource { checkNotNull(activeJson) { "Initialize ads at Splash first" } })
                .build()

            initialization = scope.async {
                val startup = StartupAdsConfig(
                    fetchRemote = { remoteConfig()?.getString(REMOTE_KEY).orEmpty() },
                    readCached = { prefs.getString(CACHE_KEY, null) },
                    readFallback = {
                        val asset = if (BuildConfig.DEBUG) "ads/app-placements.json" else "ads/placements.json"
                        AssetAdsConfigSource(app, asset).read()
                    },
                    apply = { json ->
                        kit.applyConfig(json)
                        activeJson = json
                    },
                    saveRemote = { json ->
                        check(prefs.edit().putString(CACHE_KEY, json).commit()) { "Cannot save validated ads config" }
                    },
                    onError = { Log.w(TAG, "Ads config source unavailable", it) },
                    fetchTimeoutMs = FETCH_TIMEOUT_MS,
                )
                configuration = withContext(Dispatchers.IO) { startup.initialize() }
                isInitialized = true
                Log.d(TAG, "Configuration ready: revision=${configuration.revision}")
            }
        }
        initialization!!.await()
        return kit
    }

    /** Remote Config is optional: without it the SDK falls back to the cache, then to the shipped JSON. */
    private suspend fun remoteConfig(): FirebaseRemoteConfig? = runCatching {
        val remote = FirebaseRemoteConfig.getInstance()
        remote.ensureInitialized().awaitResult()
        remote.setConfigSettingsAsync(
            FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3_600)
                .setFetchTimeoutInSeconds(4)
                .build(),
        ).awaitResult()
        remote.fetchAndActivate().awaitResult()
        remote
    }.getOrElse {
        Log.w(TAG, "Remote Config unavailable, using cache/asset", it)
        null
    }

    /**
     * Consent (UMP form when the user's region requires one) and Google Ads initialisation.
     * Called from Splash: any consent form belongs to that Activity, so the caller should not
     * navigate away until this returns.
     */
    @MainThread
    suspend fun requestConsent(activity: FragmentActivity): Boolean = suspendCancellableCoroutine { cont ->
        AdsSdk.initializeWithConsent(activity) { enabled, error ->
            error?.let { Log.w(TAG, "Consent: ${it.errorCode} ${it.message}") }
            if (enabled) _ready.value = true
            Log.d(TAG, "consent done: enabled=$enabled")
            if (cont.isActive) cont.resume(enabled)
        }
        // The ads runtime can still become usable shortly after the callback (its init is async),
        // so keep watching for a moment instead of leaving the first screens without ads.
        scope.launch {
            repeat(24) {
                delay(250)
                if (_ready.value) return@launch
                if (AdsSdk.canLoadAds(PlacementFormat.BANNER)) { _ready.value = true; return@launch }
            }
        }
    }

    /** Banner inside [container]. Shown as soon as the ads runtime is ready; skipped if it never is. */
    @MainThread
    fun showBanner(
        activity: FragmentActivity,
        container: android.view.ViewGroup,
        onState: (AdPreloadState) -> Unit = {},
    ) {
        if (!Features.ADS) return
        if (!isInitialized) { Log.w(TAG, "showBanner skipped: ads not initialised"); return }
        activity.lifecycleScope.launch {
            if (!_ready.value) withTimeoutOrNull(READY_TIMEOUT_MS) { _ready.first { it } }
            if (!_ready.value) { Log.w(TAG, "showBanner skipped: ads never became ready"); return@launch }
            if (activity.isFinishing || activity.isDestroyed) return@launch
            kit.showBanner(activity, BANNER, container, BANNER_OPTIONS) { state -> Log.d(TAG, "banner $state"); onState(state) }
        }
    }

    /**
     * Interstitial, then [onDone]. Navigation must happen in [onDone]: the guide calls for waiting
     * until the ad finishes while the Activity is still resumed. When ads are off (disabled
     * placement, no fill, consent refused) this runs [onDone] straight away.
     */
    @MainThread
    fun showInterstitial(activity: FragmentActivity, onDone: () -> Unit) {
        if (!Features.ADS) return onDone()
        if (!isInitialized || !_ready.value) {
            Log.d(TAG, "interstitial skipped: initialised=$isInitialized ready=${_ready.value}")
            return onDone()
        }
        activity.lifecycleScope.launch {
            val result = runCatching { kit.showAdFullScreen(activity, INTERSTITIAL) }
                .onFailure { Log.w(TAG, "Interstitial failed", it) }
                .getOrNull()
            Log.d(TAG, "next_screen: $result (${(result as? AdFullScreenResult)?.outcome})")
            if (activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) onDone()
        }
    }

    private const val TAG = "AppAds"
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        when {
            task.isCanceled -> continuation.resumeWithException(IllegalStateException("Firebase task was cancelled"))
            task.isSuccessful -> continuation.resume(task.result)
            else -> continuation.resumeWithException(task.exception ?: IllegalStateException("Firebase task failed"))
        }
    }
}
