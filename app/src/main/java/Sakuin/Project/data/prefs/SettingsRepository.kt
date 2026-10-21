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

/** Small key-value settings: the user's name from onboarding and app preferences. */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val userName: Flow<String?> = dataStore.data.map { it[USER_NAME] }

    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_DONE] ?: false }

    suspend fun setUserName(name: String) {
        require(name.isNotBlank()) { "Name is required" }
        dataStore.edit { it[USER_NAME] = name.trim() }
    }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[ONBOARDING_DONE] = true }
    }

    private companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }
}
