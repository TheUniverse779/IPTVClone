package com.iptvplayer.app.data.network

import com.google.gson.Gson
import com.iptvplayer.app.BuildConfig
import com.iptvplayer.app.data.datastore.SettingsStore
import com.iptvplayer.app.data.datastore.UserAgentMode
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One shared OkHttpClient (the original app built a new trust-all client per call).
 * Normal TLS validation is kept; the User-Agent follows the user's setting.
 */
@Singleton
class HttpClients @Inject constructor(private val settings: SettingsStore, private val gson: Gson) {

    @Volatile private var uaMode: UserAgentMode = UserAgentMode.APP

    fun refreshUserAgent() { uaMode = runBlocking { settings.current().userAgentMode } }

    val userAgent: String get() = uaMode.value ?: APP_UA

    val ok: OkHttpClient by lazy {
        refreshUserAgent()
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request()
                val b = req.newBuilder()
                if (req.header("User-Agent") == null) b.header("User-Agent", userAgent)
                chain.proceed(b.build())
            }
            .build()
    }

    private val xtream = ConcurrentHashMap<String, XtreamApi>()

    /** Retrofit instance per server base URL, cached. */
    fun xtream(serverUrl: String): XtreamApi = xtream.getOrPut(serverUrl) {
        Retrofit.Builder().baseUrl(serverUrl.trimEnd('/') + "/").client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson)).build().create(XtreamApi::class.java)
    }

    val espn: EspnApi by lazy {
        Retrofit.Builder().baseUrl("https://site.api.espn.com/apis/site/v2/").client(ok)
            .addConverterFactory(GsonConverterFactory.create(gson)).build().create(EspnApi::class.java)
    }

    companion object {
        val APP_UA = "IPTVPlayer/${BuildConfig.VERSION_NAME} (Android)"
    }
}
