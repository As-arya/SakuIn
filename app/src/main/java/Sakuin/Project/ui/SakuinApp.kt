package Sakuin.Project.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import Sakuin.Project.ui.components.SakuinBottomBar
import Sakuin.Project.ui.navigation.AddTransactionRoute
import Sakuin.Project.ui.navigation.HomeRoute
import Sakuin.Project.ui.navigation.OnboardingRoute
import Sakuin.Project.ui.navigation.ScanRoute
import Sakuin.Project.ui.navigation.WalletFormRoute
import Sakuin.Project.ui.navigation.SakuinNavHost
import Sakuin.Project.ui.navigation.TopLevelDestination
import Sakuin.Project.ui.theme.SakuinTheme

@Composable
fun SakuinApp(startDestination: Any) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }
    // Forms and onboarding sit on the white surface; lists and tabs on the gray app background.
    val onSurface = currentDestination?.hierarchy?.any {
        it.hasRoute(OnboardingRoute::class) || it.hasRoute(AddTransactionRoute::class) || it.hasRoute(WalletFormRoute::class)
    } == true
    // The scan screen paints behind the system bars itself (its capture step is dark).
    val fullBleed = currentDestination?.hasRoute(ScanRoute::class) == true

    Scaffold(
        containerColor = if (onSurface || fullBleed) SakuinTheme.colors.bgSurface else SakuinTheme.colors.bgApp,
        contentWindowInsets = if (fullBleed) WindowInsets(0, 0, 0, 0) else ScaffoldDefaults.contentWindowInsets,
        bottomBar = {
            if (currentTab != null) {
                SakuinBottomBar(
                    selected = currentTab,
                    onSelect = navController::navigateToTab,
                    onAddClick = { navController.navigate(AddTransactionRoute()) },
                )
            }
        },
    ) { innerPadding ->
        SakuinNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        )
    }
}

private fun NavDestination?.isOn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.route::class) } == true

/** Home is always at the bottom of the back stack once onboarding is done. */
fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<HomeRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
