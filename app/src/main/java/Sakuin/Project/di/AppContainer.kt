package Sakuin.Project.di

import android.content.Context
import Sakuin.Project.BuildConfig
import Sakuin.Project.data.local.ReceiptStore
import Sakuin.Project.data.local.SakuinDatabase
import Sakuin.Project.data.prefs.SettingsRepository
import Sakuin.Project.data.prefs.settingsDataStore
import Sakuin.Project.data.remote.GeminiReceiptClient
import Sakuin.Project.data.repository.CategoryRepository
import Sakuin.Project.data.repository.TransactionRepository
import Sakuin.Project.data.repository.WalletRepository
import Sakuin.Project.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Manual dependency container, created once in [Sakuin.Project.SakuinApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** Lives as long as the process; for work that must not be tied to a screen. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: SakuinDatabase by lazy { SakuinDatabase.build(appContext) }

    val walletRepository by lazy { WalletRepository(database.walletDao()) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val transactionRepository by lazy { TransactionRepository(database.transactionDao()) }
    val settingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    // Receipt scan: the only part of the app that goes online.
    val networkMonitor by lazy { NetworkMonitor(appContext) }
    val receiptStore by lazy { ReceiptStore(appContext) }
    val receiptScanner by lazy { GeminiReceiptClient(apiKey = BuildConfig.GEMINI_API_KEY) }

    init {
        applicationScope.launch { categoryRepository.ensureDefaults() }
        val startedAt = System.currentTimeMillis()
        applicationScope.launch {
            receiptStore.cleanUp(referenced = transactionRepository.receiptImagePaths(), startedAt = startedAt)
        }
    }
}
