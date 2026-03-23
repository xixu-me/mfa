package com.github.kr328.clash.ui.app

import android.content.Intent
import com.github.kr328.clash.MainActivity
import com.github.kr328.clash.common.util.intent

object AppRouteIntent {
    const val EXTRA_ROUTE = "com.github.kr328.clash.extra.ROUTE"
    private const val ACTION_QS_TILE_PREFERENCES = "android.service.quicksettings.action.QS_TILE_PREFERENCES"

    fun mainActivity(route: String): Intent {
        return MainActivity::class.intent
            .putExtra(EXTRA_ROUTE, route)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    fun resolveRoute(intent: Intent?): String? {
        if (intent == null) return null

        return resolveRoute(
            routeExtra = intent.getStringExtra(EXTRA_ROUTE),
            action = intent.action,
        )
    }

    internal fun resolveRoute(
        routeExtra: String?,
        action: String?,
    ): String? {
        routeExtra?.let { return it }

        return when (action) {
            Intent.ACTION_APPLICATION_PREFERENCES,
            ACTION_QS_TILE_PREFERENCES,
            -> AppRoute.Settings.route
            else -> null
        }
    }

    fun properties(profileId: String): String = AppRoute.Properties.createRoute(profileId)

    fun files(profileId: String): String = AppRoute.Files.createRoute(profileId)

    fun logFile(fileName: String): String = AppRoute.LogFile.createRoute(fileName)

    fun propertiesOrNull(profileId: String?): String? = profileId?.let(::properties)

    fun filesOrNull(profileId: String?): String? = profileId?.let(::files)

    fun logcatRoute(fileName: String?): String {
        return fileName?.let(::logFile) ?: AppRoute.LiveLogcat.route
    }
}
