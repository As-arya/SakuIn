package Sakuin.Project.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** [System] follows the phone's dark theme setting until the user picks one in Settings. */
enum class ThemeMode { System, Light, Dark }

/** Small key-value settings: the user's name from onboarding and app preferences. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME] }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.System
    }

    /** Off hides every way into the (online) receipt scan. */
    val receiptScanEnabled: Flow<Boolean> = dataStore.data.map { it[RECEIPT_SCAN] ?: true }

    /** Off saves scanned expenses without their photo. */
    val keepReceiptPhotos: Flow<Boolean> = dataStore.data.map { it[KEEP_RECEIPT_PHOTOS] ?: true }

    suspend fun setUserName(name: String) {
        require(name.isNotBlank()) { "Name is required" }
        dataStore.edit { it[USER_NAME] = name.trim() }
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setReceiptScanEnabled(enabled: Boolean) {
        dataStore.edit { it[RECEIPT_SCAN] = enabled }
    }

    suspend fun setKeepReceiptPhotos(keep: Boolean) {
        dataStore.edit { it[KEEP_RECEIPT_PHOTOS] = keep }
    }

    private companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val RECEIPT_SCAN = booleanPreferencesKey("receipt_scan")
        val KEEP_RECEIPT_PHOTOS = booleanPreferencesKey("keep_receipt_photos")
    }
}
