package Sakuin.Project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.prefs.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(settings: SettingsRepository) : ViewModel() {
    /** Null while settings are loading; the splash screen stays up until it is known. */
    val onboardingDone: StateFlow<Boolean?> = settings.onboardingDone
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Null while loading, so the first frame is already drawn in the chosen theme. */
    val themeMode: StateFlow<ThemeMode?> = settings.themeMode
        .map<ThemeMode, ThemeMode?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
