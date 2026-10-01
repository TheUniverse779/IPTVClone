package com.iptvplayer.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

enum class UserAgentMode(val value: String?) {
    APP(null),
    VLC("VLC/3.0.20 LibVLC/3.0.20"),
    CHROME("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"),
}

data class Settings(
    val firstRunDone: Boolean,
    val disclaimerAccepted: Boolean,
    val hasPasscode: Boolean,
    val autoPip: Boolean,
    val backgroundAudio: Boolean,
    val userAgentMode: UserAgentMode,
    val softwareDecoder: Boolean,
    val selectedLeagues: Set<String>,
)

/** App settings (replaces the original app's Hawk prefs). Passcode is stored as salted SHA-256. */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val FIRST_RUN = booleanPreferencesKey("first_run_done")
        val DISCLAIMER = booleanPreferencesKey("disclaimer_accepted")
        val PASS_HASH = stringPreferencesKey("passcode_hash")
        val PASS_SALT = stringPreferencesKey("passcode_salt")
        val AUTO_PIP = booleanPreferencesKey("auto_pip")
        val BG_AUDIO = booleanPreferencesKey("background_audio")
        val UA = stringPreferencesKey("user_agent_mode")
        val SW_DECODER = booleanPreferencesKey("software_decoder")
        val LEAGUES = stringPreferencesKey("selected_leagues")
        val LANG_DEFAULTED = booleanPreferencesKey("language_defaulted")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): Settings = settings.first()

    private fun Preferences.toSettings() = Settings(
        firstRunDone = this[K.FIRST_RUN] ?: false,
        disclaimerAccepted = this[K.DISCLAIMER] ?: false,
        hasPasscode = this[K.PASS_HASH] != null,
        autoPip = this[K.AUTO_PIP] ?: true,
        backgroundAudio = this[K.BG_AUDIO] ?: false,
        userAgentMode = runCatching { UserAgentMode.valueOf(this[K.UA] ?: "APP") }.getOrDefault(UserAgentMode.APP),
        softwareDecoder = this[K.SW_DECODER] ?: false,
        selectedLeagues = this[K.LEAGUES]?.split(',')?.filter { it.isNotBlank() }?.toSet() ?: DEFAULT_LEAGUES,
    )

    suspend fun setFirstRunDone() = context.dataStore.edit { it[K.FIRST_RUN] = true }

    /** True once the default app language has been applied (so a later user choice is never overridden). */
    suspend fun languageDefaulted() = context.dataStore.data.first()[K.LANG_DEFAULTED] ?: false
    suspend fun setLanguageDefaulted() = context.dataStore.edit { it[K.LANG_DEFAULTED] = true }
    suspend fun setDisclaimerAccepted() = context.dataStore.edit { it[K.DISCLAIMER] = true }
    suspend fun setAutoPip(v: Boolean) = context.dataStore.edit { it[K.AUTO_PIP] = v }
    suspend fun setBackgroundAudio(v: Boolean) = context.dataStore.edit { it[K.BG_AUDIO] = v }
    suspend fun setUserAgentMode(v: UserAgentMode) = context.dataStore.edit { it[K.UA] = v.name }
    suspend fun setSoftwareDecoder(v: Boolean) = context.dataStore.edit { it[K.SW_DECODER] = v }
    suspend fun setSelectedLeagues(v: Set<String>) = context.dataStore.edit { it[K.LEAGUES] = v.joinToString(",") }

    suspend fun setPasscode(code: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toHex()
        context.dataStore.edit { it[K.PASS_SALT] = salt; it[K.PASS_HASH] = hash(salt, code) }
    }

    suspend fun clearPasscode() = context.dataStore.edit { it.remove(K.PASS_HASH); it.remove(K.PASS_SALT) }

    suspend fun checkPasscode(code: String): Boolean {
        val p = context.dataStore.data.first()
        val salt = p[K.PASS_SALT] ?: return false
        return p[K.PASS_HASH] == hash(salt, code)
    }

    private fun hash(salt: String, code: String): String =
        MessageDigest.getInstance("SHA-256").digest("$salt:$code".toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }

    companion object {
        val DEFAULT_LEAGUES = setOf("eng.1", "esp.1", "ita.1", "uefa.champions")
    }
}
