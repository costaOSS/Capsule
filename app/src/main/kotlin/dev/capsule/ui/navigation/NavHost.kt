package dev.capsule.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import dev.capsule.ui.screens.home.HomeScreen
import dev.capsule.ui.screens.terminal.TerminalScreen
import dev.capsule.ui.screens.install.InstallScreen
import dev.capsule.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Terminal : Screen("terminal/{distroId}") {
        fun createRoute(distroId: Long) = "terminal/$distroId"
    }
    data object Install : Screen("install")
    data object Settings : Screen("settings")
}

@Composable
fun CapsuleNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTerminal = { distroId ->
                    navController.navigate(Screen.Terminal.createRoute(distroId))
                },
                onNavigateToInstall = {
                    navController.navigate(Screen.Install.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Terminal.route,
            arguments = listOf(
                navArgument("distroId") { type = androidx.navigation.NavType.LongType }
            )
        ) { backStackEntry ->
            val distroId = backStackEntry.arguments?.getLong("distroId") ?: 0L
            TerminalScreen(
                distroId = distroId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Install.route) {
            InstallScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}