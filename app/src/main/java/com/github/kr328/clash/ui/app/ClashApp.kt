package com.github.kr328.clash.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.kr328.clash.ui.files.FilesRoute
import com.github.kr328.clash.ui.help.HelpRoute
import com.github.kr328.clash.ui.logs.LogcatRoute
import com.github.kr328.clash.ui.logs.LogsRoute
import com.github.kr328.clash.ui.main.MainRoute
import com.github.kr328.clash.ui.newprofile.NewProfileRoute
import com.github.kr328.clash.ui.profiles.ProfilesRoute
import com.github.kr328.clash.ui.profiles.PropertiesRoute
import com.github.kr328.clash.ui.proxy.ProxyRoute
import com.github.kr328.clash.ui.providers.ProvidersRoute
import com.github.kr328.clash.ui.accesscontrol.AccessControlRoute
import com.github.kr328.clash.ui.settings.app.AppSettingsRoute
import com.github.kr328.clash.ui.settings.meta.MetaFeatureSettingsRoute
import com.github.kr328.clash.ui.settings.network.NetworkSettingsRoute
import com.github.kr328.clash.ui.settings.override.OverrideSettingsRoute
import com.github.kr328.clash.ui.settings.SettingsRoute

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ClashApp(
    launchRoute: String? = null,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val appState = ClashAppState(
        navController = navController,
        windowWidthSizeClass = calculateWindowSizeClass(context as androidx.activity.ComponentActivity).widthSizeClass,
    )
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    LaunchedEffect(launchRoute) {
        val route = launchRoute ?: return@LaunchedEffect
        if (currentDestination?.route == route) return@LaunchedEffect

        navController.navigate(route) {
            launchSingleTop = true
            popUpTo(navController.graph.startDestinationId) {
                inclusive = route != AppRoute.Home.route
            }
        }
    }

    ClashNavigationScaffold(
        appState = appState,
        currentDestination = currentDestination,
    ) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Home.route,
        ) {
            composable(AppRoute.Home.route) {
                MainRoute(navController = navController)
            }
            composable(AppRoute.Proxy.route) {
                ProxyRoute(navController = navController)
            }
            composable(AppRoute.Profiles.route) {
                ProfilesRoute(navController = navController)
            }
            composable(AppRoute.Providers.route) {
                ProvidersRoute(navController = navController)
            }
            composable(AppRoute.Logs.route) {
                LogsRoute(navController = navController)
            }
            composable(AppRoute.LiveLogcat.route) {
                LogcatRoute(navController = navController)
            }
            composable(
                route = AppRoute.LogFile.route,
                arguments = listOf(navArgument(AppRoute.FILE_NAME) { type = NavType.StringType }),
            ) {
                LogcatRoute(navController = navController)
            }
            composable(AppRoute.Settings.route) {
                SettingsRoute(
                    navController = navController,
                    widthSizeClass = appState.windowWidthSizeClass,
                )
            }
            composable(AppRoute.AppSettings.route) {
                AppSettingsRoute(onBack = { navController.popBackStack() })
            }
            composable(AppRoute.NetworkSettings.route) {
                NetworkSettingsRoute(
                    navController = navController,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoute.AccessControl.route) {
                AccessControlRoute(
                    navController = navController,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppRoute.OverrideSettings.route) {
                OverrideSettingsRoute(onBack = { navController.popBackStack() })
            }
            composable(AppRoute.MetaFeatureSettings.route) {
                MetaFeatureSettingsRoute(onBack = { navController.popBackStack() })
            }
            composable(AppRoute.Help.route) {
                HelpRoute(navController = navController)
            }
            composable(AppRoute.NewProfile.route) {
                NewProfileRoute(navController = navController)
            }
            composable(
                route = AppRoute.Properties.route,
                arguments = listOf(navArgument(AppRoute.PROFILE_ID) { type = NavType.StringType }),
            ) { backStackEntry ->
                val profileId = backStackEntry.arguments?.getString(AppRoute.PROFILE_ID) ?: return@composable
                PropertiesRoute(
                    navController = navController,
                    profileId = profileId,
                )
            }
            composable(
                route = AppRoute.Files.route,
                arguments = listOf(navArgument(AppRoute.PROFILE_ID) { type = NavType.StringType }),
            ) {
                FilesRoute(navController = navController)
            }
        }
    }
}
