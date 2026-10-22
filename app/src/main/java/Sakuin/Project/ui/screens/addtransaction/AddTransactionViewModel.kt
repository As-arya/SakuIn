package Sakuin.Project.ui.screens.addtransaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import Sakuin.Project.data.local.ReceiptStore
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import Sakuin.Project.data.local.entity.TransactionEntity
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.repository.CategoryRepository
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.ui.navigation.AddTransactionRoute
import Sakuin.Project.util.parseRupiahInput
import java.io.File
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FormError { AmountRequired, CategoryRequired, TitleRequired, WalletRequired, SameWallet }

/**
 * State for the Add/Edit Transaction screen (Figma 03–05). The amount, date and note are shared by
 * the three tabs; each tab keeps its own wallet and category choice.
 */
@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AddTransactionViewModel(
    savedStateHandle: SavedStateHandle,
    walletRepository: WalletRepository,
    categoryRepository: CategoryRepository,
    private val transactions: TransactionRepository,
    private val receipts: ReceiptStore,
    settings: SettingsRepository,
    clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AddTransactionRoute>()
    val isEdit = route.transactionId != null
    private var editing: TransactionEntity? = null

    var tab by mutableStateOf(route.tab)
        private set

    val wallets: StateFlow<List<WalletWithBalance>> = walletRepository.observeWallets()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val expenseCategories: StateFlow<List<CategoryEntity>> = categoryRepository.observeCategories(CategoryKind.EXPENSE)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** Whether the app bar offers the receipt scan (it can be switched off in Settings). */
    val scanEnabled: StateFlow<Boolean> = settings.receiptScanEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val incomeCategories: StateFlow<List<CategoryEntity>> = categoryRepository.observeCategories(CategoryKind.INCOME)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var amountDigits by mutableStateOf("")
        private set
    var occurredAt by mutableLongStateOf(clock())
    var note by mutableStateOf("")

    /** Wallet for an expense (paid from) or income (received into). */
    var walletId by mutableStateOf<Long?>(null)
        private set
    var expenseCategoryId by mutableStateOf<Long?>(null)
        private set
    var incomeCategoryId by mutableStateOf<Long?>(null)
        private set
    var title by mutableStateOf("")
        private set

    var fromWalletId by mutableStateOf<Long?>(null)
        private set
    var toWalletId by mutableStateOf<Long?>(null)
        private set

    var error by mutableStateOf<FormError?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    /** Photo of the receipt an edited expense was scanned from. */
    var receiptFile by mutableStateOf<File?>(null)
        private set

    /** Earlier item names in the chosen expense category that start with what was typed. */
    val titleSuggestions: StateFlow<List<String>> =
        snapshotFlow { expenseCategoryId to title }
            .debounce(200)
            .mapLatest { (categoryId, typed) ->
                if (categoryId == null) emptyList()
                else transactions.suggestTitles(categoryId, typed).filterNot { it.equals(typed.trim(), ignoreCase = true) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            route.transactionId?.let { id -> transactions.getTransaction(id)?.let(::fillFrom) }
            applyDefaults()
        }
    }

    private fun fillFrom(t: TransactionEntity) {
        editing = t
        tab = t.type
        amountDigits = t.amount.toString()
        occurredAt = t.occurredAt
        note = t.note.orEmpty()
        receiptFile = t.receiptImagePath?.let(receipts::resolve)
        when (t.type) {
            TransactionType.EXPENSE -> {
                walletId = t.walletId
                expenseCategoryId = t.categoryId
                title = t.title
            }
            TransactionType.INCOME -> {
                walletId = t.walletId
                incomeCategoryId = t.categoryId
            }
            TransactionType.TRANSFER -> {
                fromWalletId = t.walletId
                toWalletId = t.toWalletId
            }
        }
    }

    /** Pre-selects the first wallet and category so a quick entry needs only the amount and name. */
    private suspend fun applyDefaults() {
        val list = wallets.filter { it.isNotEmpty() }.first()
        val preferred = route.fromWalletId?.takeIf { id -> list.any { it.wallet.id == id } } ?: list.first().wallet.id
        if (walletId == null) walletId = preferred
        if (fromWalletId == null) fromWalletId = preferred
        if (toWalletId == null) toWalletId = list.firstOrNull { it.wallet.id != fromWalletId }?.wallet?.id
        if (expenseCategoryId == null) expenseCategoryId = expenseCategories.filter { it.isNotEmpty() }.first().first().id
        if (incomeCategoryId == null) incomeCategoryId = incomeCategories.filter { it.isNotEmpty() }.first().first().id
    }

    fun selectTab(type: TransactionType) {
        if (isEdit) return
        tab = type
        error = null
    }

    fun onAmountChange(digits: String) {
        amountDigits = digits
        error = null
    }

    fun onTitleChange(value: String) {
        title = value.take(60)
        if (error == FormError.TitleRequired) error = null
    }

    fun selectWallet(id: Long) {
        walletId = id
        error = null
    }

    fun selectCategory(id: Long) {
        if (tab == TransactionType.INCOME) incomeCategoryId = id else expenseCategoryId = id
        if (error == FormError.CategoryRequired) error = null
    }

    fun selectFrom(id: Long) {
        if (id == toWalletId) toWalletId = fromWalletId
        fromWalletId = id
        error = null
    }

    fun selectTo(id: Long) {
        if (id == fromWalletId) fromWalletId = toWalletId
        toWalletId = id
        error = null
    }

    fun swapWallets() {
        val from = fromWalletId
        fromWalletId = toWalletId
        toWalletId = from
        error = null
    }

    /** Balance the selected wallet ends up with, counting an edited transaction's old amount back in. */
    fun balanceAfter(wallet: WalletWithBalance, amount: Long): Long {
        val original = editing?.takeIf { it.walletId == wallet.wallet.id }
        val restored = when (original?.type) {
            TransactionType.EXPENSE, TransactionType.TRANSFER -> original.amount
            TransactionType.INCOME -> -original.amount
            null -> 0
        }
        val change = if (tab == TransactionType.INCOME) amount else -amount
        return wallet.balance + restored + change
    }

    fun save(onSaved: () -> Unit) {
        val amount = parseRupiahInput(amountDigits)
        error = when {
            amount == null || amount <= 0 -> FormError.AmountRequired
            tab == TransactionType.TRANSFER && (fromWalletId == null || toWalletId == null || fromWalletId == toWalletId) -> FormError.SameWallet
            tab != TransactionType.TRANSFER && walletId == null -> FormError.WalletRequired
            tab == TransactionType.EXPENSE && expenseCategoryId == null -> FormError.CategoryRequired
            tab == TransactionType.INCOME && incomeCategoryId == null -> FormError.CategoryRequired
            tab == TransactionType.EXPENSE && title.isBlank() -> FormError.TitleRequired
            else -> null
        }
        if (error != null || saving) return
        saving = true
        viewModelScope.launch {
            val existing = editing
            if (existing != null) transactions.updateTransaction(edited(existing, amount!!)) else insert(amount!!)
            onSaved()
        }
    }

    private suspend fun insert(amount: Long) {
        when (tab) {
            TransactionType.EXPENSE -> transactions.addExpense(amount, walletId!!, expenseCategoryId!!, title, note, occurredAt)
            TransactionType.INCOME -> transactions.addIncome(amount, walletId!!, incomeCategoryId!!, note, occurredAt)
            TransactionType.TRANSFER -> transactions.addTransfer(amount, fromWalletId!!, toWalletId!!, note, occurredAt)
        }
    }

    private fun edited(existing: TransactionEntity, amount: Long): TransactionEntity {
        val cleanNote = note.trim().takeIf { it.isNotEmpty() }
        val base = existing.copy(amount = amount, occurredAt = occurredAt, note = cleanNote)
        return when (tab) {
            TransactionType.EXPENSE -> base.copy(walletId = walletId!!, categoryId = expenseCategoryId, title = title.trim())
            TransactionType.INCOME -> base.copy(walletId = walletId!!, categoryId = incomeCategoryId)
            TransactionType.TRANSFER -> base.copy(walletId = fromWalletId!!, toWalletId = toWalletId)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = route.transactionId ?: return
        viewModelScope.launch {
            transactions.deleteTransaction(id)
            onDeleted()
        }
    }

    companion object {
        val Tabs = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
    }
}
