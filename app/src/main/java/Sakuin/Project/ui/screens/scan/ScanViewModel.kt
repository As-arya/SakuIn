package Sakuin.Project.ui.screens.scan

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import Sakuin.Project.data.local.ReceiptStore
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.local.entity.CategoryEntity
import Sakuin.Project.data.local.entity.CategoryKind
import Sakuin.Project.data.local.model.WalletWithBalance
import Sakuin.Project.data.remote.GeminiReceiptClient
import Sakuin.Project.data.remote.ReceiptDraft
import Sakuin.Project.data.remote.ReceiptItem
import Sakuin.Project.data.remote.ScanError
import Sakuin.Project.data.remote.ScanException
import Sakuin.Project.data.remote.toDraft
import Sakuin.Project.data.repository.CategoryRepository
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.ui.screens.addtransaction.FormError
import Sakuin.Project.util.MAX_AMOUNT_DIGITS
import Sakuin.Project.util.NetworkMonitor
import Sakuin.Project.util.parseRupiahInput
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the scan flow is (Figma 09–12): pick a photo → reading → review, or a problem. */
sealed interface ScanStep {
    data object Capture : ScanStep
    data object Reading : ScanStep
    data object Review : ScanStep
    data class Failed(val error: ScanError) : ScanStep
}

/** Problems worth retrying with the same photo; the others need a new photo or manual entry. */
private val RetrySamePhoto = setOf(ScanError.Network, ScanError.Timeout, ScanError.Busy, ScanError.Server)

class ScanViewModel(
    private val scanner: GeminiReceiptClient,
    private val store: ReceiptStore,
    private val network: NetworkMonitor,
    walletRepository: WalletRepository,
    categoryRepository: CategoryRepository,
    private val transactions: TransactionRepository,
    private val settings: SettingsRepository,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = network.isOnline

    val wallets: StateFlow<List<WalletWithBalance>> = walletRepository.observeWallets()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val categoryFlow = categoryRepository.observeCategories(CategoryKind.EXPENSE)
    val categories: StateFlow<List<CategoryEntity>> = categoryFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var step by mutableStateOf<ScanStep>(ScanStep.Capture)
        private set

    /** The shrunk copy that was (or is being) scanned; kept as the receipt photo on save. */
    var photo by mutableStateOf<File?>(null)
        private set
    private var source: Uri? = null
    private var job: Job? = null

    // Review fields, all editable before saving.
    var amountDigits by mutableStateOf("")
        private set
    var title by mutableStateOf("")
        private set
    var merchant by mutableStateOf<String?>(null)
        private set
    var categoryId by mutableStateOf<Long?>(null)
        private set
    /** The category Gemini picked, marked in the UI as an AI suggestion. */
    var suggestedCategoryId by mutableStateOf<Long?>(null)
        private set
    var walletId by mutableStateOf<Long?>(null)
        private set
    var occurredAt by mutableLongStateOf(0L)
    var items by mutableStateOf<List<ReceiptItem>>(emptyList())
        private set

    var error by mutableStateOf<FormError?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    /** The retry button repeats the request for connection/server problems, otherwise asks for a new photo. */
    val retriesSamePhoto: Boolean
        get() = (step as? ScanStep.Failed)?.error in RetrySamePhoto

    fun cameraFile(): File = store.cameraFile()

    fun refreshConnection() = network.refresh()

    fun onPhoto(uri: Uri) {
        discardPhoto()
        source = uri
        read()
    }

    private fun read() {
        val uri = source ?: return
        job?.cancel()
        step = ScanStep.Reading
        job = viewModelScope.launch {
            step = try {
                val file = photo ?: store.prepareForScan(uri).also { photo = it }
                val bytes = withContext(Dispatchers.IO) { file.readBytes() }
                val known = categoryFlow.first()
                val receipt = scanner.scan(bytes, known.map { it.name }, LocalDate.now(zone))
                fill(receipt.toDraft(known, LocalDateTime.now(zone), zone))
                ScanStep.Review
            } catch (e: ScanException) {
                Log.w(TAG, "Receipt scan failed", e)
                ScanStep.Failed(e.error)
            }
        }
    }

    private fun fill(draft: ReceiptDraft) {
        amountDigits = draft.amount?.toString()?.take(MAX_AMOUNT_DIGITS).orEmpty()
        title = draft.title
        merchant = draft.merchant
        categoryId = draft.categoryId
        suggestedCategoryId = draft.categoryId
        occurredAt = draft.occurredAt
        items = draft.items
        if (walletId == null) walletId = wallets.value.firstOrNull()?.wallet?.id
        error = null
    }

    fun tryAgain() {
        if (retriesSamePhoto) read() else retake()
    }

    /** Back to choosing a photo; also cancels a scan in progress. */
    fun retake() {
        job?.cancel()
        discardPhoto()
        source = null
        error = null
        step = ScanStep.Capture
    }

    fun onAmountChange(digits: String) {
        amountDigits = digits
        if (error == FormError.AmountRequired) error = null
    }

    fun onTitleChange(value: String) {
        title = value.take(60)
        if (error == FormError.TitleRequired) error = null
    }

    fun selectCategory(id: Long) {
        categoryId = id
        if (error == FormError.CategoryRequired) error = null
    }

    fun selectWallet(id: Long) {
        walletId = id
        if (error == FormError.WalletRequired) error = null
    }

    fun save(onSaved: () -> Unit) {
        val amount = parseRupiahInput(amountDigits)
        error = when {
            amount == null || amount <= 0 -> FormError.AmountRequired
            categoryId == null -> FormError.CategoryRequired
            title.isBlank() -> FormError.TitleRequired
            walletId == null -> FormError.WalletRequired
            else -> null
        }
        if (error != null || saving) return
        saving = true
        viewModelScope.launch {
            // With "Keep receipt photos" off the scan copy is simply dropped (see onCleared).
            val path = if (settings.keepReceiptPhotos.first()) photo?.let { store.keep(it) } else null
            if (path != null) photo = null
            // The store name is kept as the note when the item name says something else.
            val note = merchant?.takeUnless { it.equals(title.trim(), ignoreCase = true) }
            transactions.addExpense(amount!!, walletId!!, categoryId!!, title, note, occurredAt, receiptImagePath = path)
            onSaved()
        }
    }

    private fun discardPhoto() {
        store.discard(photo)
        photo = null
    }

    override fun onCleared() {
        discardPhoto()
    }

    private companion object {
        const val TAG = "ReceiptScan"
    }
}
