package Sakuin.Project.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import Sakuin.Project.ui.screens.addtransaction.AddTransactionScreen
import Sakuin.Project.ui.screens.home.HomeScreen
import Sakuin.Project.ui.screens.stats.StatsScreen
import Sakuin.Project.ui.screens.transactions.TransactionsScreen
import Sakuin.Project.ui.screens.wallets.WalletsScreen

@Composable
fun SakuinNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> { HomeScreen() }
        composable<TransactionsRoute> { TransactionsScreen() }
        composable<StatsRoute> { StatsScreen() }
        composable<WalletsRoute> { WalletsScreen() }
        composable<AddTransactionRoute> {
            AddTransactionScreen(onBack = navController::popBackStack)
        }
    }
}
