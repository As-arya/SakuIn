package Sakuin.Project.ui.screens.wallets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import Sakuin.Project.data.local.entity.WalletType
import Sakuin.Project.data.local.model.TransactionWithDetails
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.BalanceSummary
import Sakuin.Project.data.repository.TransactionFilter
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.ui.navigation.WalletDetailRoute
import Sakuin.Project.ui.navigation.WalletFormRoute
import Sakuin.Project.util.TimeRange
import Sakuin.Project.util.parseRupiahInput
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WalletsState(
    val loading: Boolean = true,
    val wallets: List<WalletWithBalance> = emptyList(),
    val summary: BalanceSummary = BalanceSummary(0, 0, 0, 0),
)

class WalletsViewModel(wallets: WalletRepository) : ViewModel() {
    val state: StateFlow<WalletsState> = wallets.observeWallets()
        .map { WalletsState(loading = false, wallets = it, summary = BalanceSummary.from(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletsState())
}

data class WalletDetailState(
    val wallet: WalletWithBalance? = null,
    val history: List<TransactionWithDetails> = emptyList(),
)

class WalletDetailViewModel(
    savedStateHandle: SavedStateHandle,
    wallets: WalletRepository,
    transactions: TransactionRepository,
) : ViewModel() {
    val walletId = savedStateHandle.toRoute<WalletDetailRoute>().walletId

    val state: StateFlow<WalletDetailState> = combine(
        wallets.observeWallets(),
        transactions.observeTransactions(AllTime, TransactionFilter(walletId = walletId)),
    ) { list, history ->
        WalletDetailState(wallet = list.firstOrNull { it.wallet.id == walletId }, history = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletDetailState())

    private companion object {
        val AllTime = TimeRange(0, Long.MAX_VALUE)
    }
}

class WalletFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val wallets: WalletRepository,
) : ViewModel() {
    private val walletId = savedStateHandle.toRoute<WalletFormRoute>().walletId
    val isEdit = walletId != null

    var name by mutableStateOf("")
        private set
    var type by mutableStateOf(WalletType.CASH)
    var balanceDigits by mutableStateOf("")
    var showNameError by mutableStateOf(false)
        private set
    var saving by mutableStateOf(false)
        private set

    init {
        if (walletId != null) {
            viewModelScope.launch {
                wallets.getWallet(walletId)?.let {
                    name = it.name
                    type = it.type
                    balanceDigits = if (it.initialBalance > 0) it.initialBalance.toString() else ""
                }
            }
        }
    }

    fun onNameChange(value: String) {
        name = value.take(40)
        showNameError = false
    }

    fun save(onSaved: () -> Unit) {
        if (name.isBlank()) {
            showNameError = true
            return
        }
        if (saving) return
        saving = true
        val initialBalance = parseRupiahInput(balanceDigits) ?: 0
        viewModelScope.launch {
            if (walletId == null) {
                wallets.addWallet(name, type, initialBalance)
            } else {
                wallets.updateWallet(walletId, name, type, initialBalance)
            }
            onSaved()
        }
    }

    fun archive(onArchived: () -> Unit) {
        val id = walletId ?: return
        viewModelScope.launch {
            wallets.archiveWallet(id)
            onArchived()
        }
    }
}
