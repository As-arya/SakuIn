package Sakuin.Project.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import Sakuin.Project.R
import Sakuin.Project.data.local.entity.TransactionType
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object TransactionsRoute

@Serializable
data object StatsRoute

@Serializable
data object WalletsRoute

/** Adds a transaction, or edits one when [transactionId] is set (the tab then follows its type). */
@Serializable
data class AddTransactionRoute(
    val tab: TransactionType = TransactionType.EXPENSE,
    val fromWalletId: Long? = null,
    val transactionId: Long? = null,
)

@Serializable
data object SettingsRoute

/** Receipt scan (online, Gemini); saves an expense after review. */
@Serializable
data object ScanRoute

@Serializable
data object CategoriesRoute

@Serializable
data class WalletDetailRoute(val walletId: Long)

/** Null [walletId] adds a new wallet; otherwise edits it. */
@Serializable
data class WalletFormRoute(val walletId: Long? = null)

/** Nested graph shown until the first wallet is created. */
@Serializable
data object OnboardingRoute

@Serializable
data object OnboardingIntroRoute

@Serializable
data object OnboardingNameRoute

@Serializable
data object OnboardingWalletRoute

@Serializable
data object OnboardingDoneRoute

/** Tabs shown in the bottom navigation, in display order (the add button sits in the middle). */
enum class TopLevelDestination(
    val route: Any,
    @param:DrawableRes val icon: Int,
    @param:StringRes val label: Int,
) {
    Home(HomeRoute, R.drawable.ic_home, R.string.nav_home),
    Transactions(TransactionsRoute, R.drawable.ic_receipt, R.string.nav_transactions),
    Stats(StatsRoute, R.drawable.ic_chart, R.string.nav_stats),
    Wallets(WalletsRoute, R.drawable.ic_wallet, R.string.nav_wallets),
}
