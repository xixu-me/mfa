package com.github.kr328.clash.ui.app

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController

class ClashAppState(
    val navController: NavHostController,
    val windowWidthSizeClass: WindowWidthSizeClass,
) {
    fun isTopLevelDestination(destination: NavDestination?, route: AppRoute): Boolean {
        if (destination == null) return false

        return when (route) {
            AppRoute.Logs -> destination.hierarchy.any {
                it.route == AppRoute.Logs.route ||
                    it.route == AppRoute.LiveLogcat.route ||
                    it.route == AppRoute.LogFile.route
            }
            AppRoute.Settings -> destination.hierarchy.any {
                it.route == AppRoute.Settings.route ||
                    it.route == AppRoute.AppSettings.route ||
                    it.route == AppRoute.NetworkSettings.route ||
                    it.route == AppRoute.OverrideSettings.route ||
                    it.route == AppRoute.MetaFeatureSettings.route ||
                    it.route == AppRoute.AccessControl.route
            }
            AppRoute.Profiles -> destination.hierarchy.any {
                it.route == AppRoute.Profiles.route ||
                    it.route == AppRoute.NewProfile.route ||
                    it.route == AppRoute.Properties.route ||
                    it.route == AppRoute.Files.route
            }
            else -> destination.hierarchy.any { it.route == route.route }
        }
    }

    fun navigateToTopLevel(route: AppRoute) {
        navController.navigate(route.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}
