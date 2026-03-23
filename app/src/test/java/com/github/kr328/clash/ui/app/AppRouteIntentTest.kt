package com.github.kr328.clash.ui.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppRouteIntentTest {
    @Test
    fun `resolveRoute uses explicit route extra first`() {
        assertEquals(
            AppRoute.Help.route,
            AppRouteIntent.resolveRoute(
                routeExtra = AppRoute.Help.route,
                action = android.content.Intent.ACTION_APPLICATION_PREFERENCES,
            ),
        )
    }

    @Test
    fun `resolveRoute maps application preferences to settings`() {
        assertEquals(
            AppRoute.Settings.route,
            AppRouteIntent.resolveRoute(
                routeExtra = null,
                action = android.content.Intent.ACTION_APPLICATION_PREFERENCES,
            ),
        )
    }

    @Test
    fun `resolveRoute maps qs tile preferences to settings`() {
        assertEquals(
            AppRoute.Settings.route,
            AppRouteIntent.resolveRoute(
                routeExtra = null,
                action = "android.service.quicksettings.action.QS_TILE_PREFERENCES",
            ),
        )
    }

    @Test
    fun `resolveRoute returns null when no app route can be derived`() {
        assertNull(
            AppRouteIntent.resolveRoute(
                routeExtra = null,
                action = android.content.Intent.ACTION_MAIN,
            ),
        )
    }

    @Test
    fun `route factories build typed routes`() {
        assertEquals("properties/123", AppRouteIntent.properties("123"))
        assertEquals("files/456", AppRouteIntent.files("456"))
        assertEquals("logs/file/app.log", AppRouteIntent.logFile("app.log"))
    }

    @Test
    fun `legacy file-backed routes are derived from optional args`() {
        assertEquals("properties/profile-1", AppRouteIntent.propertiesOrNull("profile-1"))
        assertEquals("files/profile-2", AppRouteIntent.filesOrNull("profile-2"))
        assertEquals("logs/file/clash.log", AppRouteIntent.logcatRoute("clash.log"))
    }

    @Test
    fun `legacy route helpers return null when required args are absent`() {
        assertNull(AppRouteIntent.propertiesOrNull(null))
        assertNull(AppRouteIntent.filesOrNull(null))
    }

    @Test
    fun `logcat route falls back to live stream without filename`() {
        assertEquals(AppRoute.LiveLogcat.route, AppRouteIntent.logcatRoute(null))
    }
}
