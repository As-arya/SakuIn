package Sakuin.Project.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import Sakuin.Project.ui.components.SakuinBottomBar
import Sakuin.Project.ui.navigation.AddTransactionRoute
import Sakuin.Project.ui.navigation.SakuinNavHost
import Sakuin.Project.ui.navigation.TopLevelDestination
import Sakuin.Project.ui.theme.SakuinTheme

@Composable
fun SakuinApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { currentDestination.isOn(it) }

    Scaffold(
        containerColor = SakuinTheme.colors.bgApp,
        bottomBar = {
            if (currentTab != null) {
                SakuinBottomBar(
                    selected = currentTab,
                    onSelect = navController::navigateToTab,
                    onAddClick = { navController.navigate(AddTransactionRoute) },
                )
            }
        },
    ) { innerPadding ->
        SakuinNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

private fun NavDestination?.isOn(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(tab.route::class) } == true

private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
