package Sakuin.Project.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import Sakuin.Project.data.local.entity.TransactionType
import Sakuin.Project.ui.navigateToTab
import Sakuin.Project.ui.screens.addtransaction.AddTransactionScreen
import Sakuin.Project.ui.screens.scan.ScanScreen
import Sakuin.Project.ui.screens.settings.CategoriesScreen
import Sakuin.Project.ui.screens.settings.SettingsScreen
import Sakuin.Project.ui.screens.home.HomeScreen
import Sakuin.Project.ui.screens.onboarding.OnboardingDoneScreen
import Sakuin.Project.ui.screens.onboarding.OnboardingIntroScreen
import Sakuin.Project.ui.screens.onboarding.OnboardingNameScreen
import Sakuin.Project.ui.screens.onboarding.OnboardingWalletScreen
import Sakuin.Project.ui.screens.stats.StatsScreen
import Sakuin.Project.ui.screens.transactions.TransactionsScreen
import Sakuin.Project.ui.screens.wallets.WalletDetailScreen
import Sakuin.Project.ui.screens.wallets.WalletFormScreen
import Sakuin.Project.ui.screens.wallets.WalletsScreen

@Composable
fun SakuinNavHost(
    navController: NavHostController,
    startDestination: Any,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        navigation<OnboardingRoute>(startDestination = OnboardingIntroRoute) {
            composable<OnboardingIntroRoute> {
                OnboardingIntroScreen(onFinish = { navController.navigate(OnboardingNameRoute) })
            }
            composable<OnboardingNameRoute> {
                OnboardingNameScreen(
                    onBack = navController::popBackStack,
                    onContinue = { navController.navigate(OnboardingWalletRoute) },
                )
            }
            composable<OnboardingWalletRoute> {
                OnboardingWalletScreen(
                    onBack = navController::popBackStack,
                    onCreated = {
                        // The wallet exists now, so the earlier steps must not be reachable again.
                        navController.navigate(OnboardingDoneRoute) {
                            popUpTo<OnboardingIntroRoute> { inclusive = true }
                        }
                    },
                )
            }
            composable<OnboardingDoneRoute> {
                OnboardingDoneScreen(
                    onGoHome = {
                        navController.navigate(HomeRoute) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    },
                )
            }
        }

        composable<HomeRoute> {
            HomeScreen(
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onAdd = { navController.navigate(AddTransactionRoute(tab = it)) },
                onScan = { navController.navigate(ScanRoute) },
                onSeeAll = { navController.navigateToTab(TopLevelDestination.Transactions) },
                onOpenTransaction = { navController.navigate(AddTransactionRoute(transactionId = it)) },
            )
        }
        composable<ScanRoute> {
            // The scan can be opened from Home or from the Add Transaction form (checked while the scan is on top).
            val openedFromForm = remember {
                navController.previousBackStackEntry?.destination?.hasRoute<AddTransactionRoute>() == true
            }
            ScanScreen(
                onBack = navController::popBackStack,
                onAddManually = {
                    if (openedFromForm) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(AddTransactionRoute()) { popUpTo<ScanRoute> { inclusive = true } }
                    }
                },
                onSaved = {
                    if (openedFromForm) {
                        navController.popBackStack<AddTransactionRoute>(inclusive = true)
                    } else {
                        navController.popBackStack()
                    }
                },
                onAddWallet = { navController.navigate(WalletFormRoute()) },
            )
        }
        composable<TransactionsRoute> {
            TransactionsScreen(onOpenTransaction = { navController.navigate(AddTransactionRoute(transactionId = it)) })
        }
        composable<StatsRoute> { StatsScreen() }
        composable<WalletsRoute> {
            WalletsScreen(
                onAddWallet = { navController.navigate(WalletFormRoute()) },
                onOpenWallet = { navController.navigate(WalletDetailRoute(it)) },
            )
        }
        composable<WalletDetailRoute> {
            WalletDetailScreen(
                onBack = navController::popBackStack,
                onEdit = { navController.navigate(WalletFormRoute(it)) },
                onTransfer = { navController.navigate(AddTransactionRoute(TransactionType.TRANSFER, fromWalletId = it)) },
                onOpenTransaction = { navController.navigate(AddTransactionRoute(transactionId = it)) },
            )
        }
        composable<WalletFormRoute> {
            WalletFormScreen(
                onBack = navController::popBackStack,
                onSaved = navController::popBackStack,
                onArchived = { navController.popBackStack<WalletsRoute>(inclusive = false) },
            )
        }
        composable<AddTransactionRoute> {
            AddTransactionScreen(
                onBack = navController::popBackStack,
                onAddWallet = { navController.navigate(WalletFormRoute()) },
                onScan = { navController.navigate(ScanRoute) },
                // Back to wherever the form was opened from (e.g. the wallet's detail with its updated history).
                onDone = navController::popBackStack,
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = navController::popBackStack,
                onOpenCategories = { navController.navigate(CategoriesRoute) },
                onOpenWallets = { navController.navigateToTab(TopLevelDestination.Wallets) },
            )
        }
        composable<CategoriesRoute> { CategoriesScreen(onBack = navController::popBackStack) }
    }
}
