package Sakuin.Project.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import Sakuin.Project.R
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data object TransactionsRoute

@Serializable
data object StatsRoute

@Serializable
data object WalletsRoute

@Serializable
data object AddTransactionRoute

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
