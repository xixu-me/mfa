package com.github.kr328.clash.ui.app

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.github.kr328.clash.design.R

sealed class AppRoute(
    val route: String,
    @StringRes val titleRes: Int,
) {
    data object Home : AppRoute("home", R.string.launch_name)
    data object Proxy : AppRoute("proxy", R.string.proxy)
    data object Profiles : AppRoute("profiles", R.string.profiles)
    data object Providers : AppRoute("providers", R.string.providers)
    data object Logs : AppRoute("logs", R.string.logs)
    data object Help : AppRoute("help", R.string.help)
    data object Settings : AppRoute("settings", R.string.settings)
    data object AccessControl : AppRoute("access-control", R.string.access_control_packages)
    data object AppSettings : AppRoute("settings/app", R.string.app)
    data object NetworkSettings : AppRoute("settings/network", R.string.network)
    data object OverrideSettings : AppRoute("settings/override", R.string.override)
    data object MetaFeatureSettings : AppRoute("settings/meta", R.string.meta_features)
    data object NewProfile : AppRoute("profiles/new", R.string.create_profile)
    data object LiveLogcat : AppRoute("logs/live", R.string.logcat)
    data object ApkBroken : AppRoute("apk-broken", R.string.application_broken)
    data object AppCrashed : AppRoute("app-crashed", R.string.application_crashed)

    data object Properties : AppRoute("properties/{profileId}", R.string.profile) {
        fun createRoute(profileId: String): String = "properties/$profileId"
    }

    data object Files : AppRoute("files/{profileId}", R.string.files) {
        fun createRoute(profileId: String): String = "files/$profileId"
    }

    data object LogFile : AppRoute("logs/file/{fileName}", R.string.logcat) {
        fun createRoute(fileName: String): String = "logs/file/$fileName"
    }

    companion object {
        const val PROFILE_ID = "profileId"
        const val FILE_NAME = "fileName"
    }
}

data class TopLevelDestination(
    val route: AppRoute,
    @DrawableRes val iconRes: Int,
)

val topLevelDestinations = listOf(
    TopLevelDestination(AppRoute.Home, R.drawable.ic_clash),
    TopLevelDestination(AppRoute.Proxy, R.drawable.ic_baseline_apps),
    TopLevelDestination(AppRoute.Profiles, R.drawable.ic_baseline_view_list),
    TopLevelDestination(AppRoute.Providers, R.drawable.ic_baseline_swap_vertical_circle),
    TopLevelDestination(AppRoute.Logs, R.drawable.ic_baseline_assignment),
    TopLevelDestination(AppRoute.Settings, R.drawable.ic_baseline_settings),
    TopLevelDestination(AppRoute.Help, R.drawable.ic_baseline_help_center),
)
