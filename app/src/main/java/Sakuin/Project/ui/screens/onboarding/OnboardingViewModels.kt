package Sakuin.Project.ui.screens.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.util.parseRupiahInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MAX_NAME_LENGTH = 40

class OnboardingNameViewModel(private val settings: SettingsRepository) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var showError by mutableStateOf(false)
        private set

    init {
        // Keep the name if the user comes back to this step.
        viewModelScope.launch { settings.userName.first()?.let { name = it } }
    }

    fun onNameChange(value: String) {
        name = value.take(MAX_NAME_LENGTH)
        showError = false
    }

    fun save(onSaved: () -> Unit) {
        if (name.isBlank()) {
            showError = true
            return
        }
        viewModelScope.launch {
            settings.setUserName(name)
            onSaved()
        }
    }
}

class OnboardingWalletViewModel(
    defaultName: String,
    private val wallets: WalletRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    var type by mutableStateOf(WalletType.CASH)
    var name by mutableStateOf(defaultName)
        private set
    var balanceDigits by mutableStateOf("")
    var showNameError by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set

    fun onNameChange(value: String) {
        name = value.take(MAX_NAME_LENGTH)
        showNameError = false
    }

    fun create(onCreated: () -> Unit) {
        if (name.isBlank()) {
            showNameError = true
            return
        }
        if (saving) return
        saving = true
        viewModelScope.launch {
            wallets.addWallet(name, type, initialBalance = parseRupiahInput(balanceDigits) ?: 0)
            settings.setOnboardingDone()
            onCreated()
        }
    }
}

data class OnboardingDoneState(val userName: String = "", val wallet: WalletWithBalance? = null)

class OnboardingDoneViewModel(settings: SettingsRepository, wallets: WalletRepository) : ViewModel() {
    val state: StateFlow<OnboardingDoneState> =
        combine(settings.userName, wallets.observeWallets()) { name, list ->
            OnboardingDoneState(userName = name.orEmpty(), wallet = list.firstOrNull())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingDoneState())
}
