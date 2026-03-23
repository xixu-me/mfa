package com.github.kr328.clash

import android.content.Intent
import com.github.kr328.clash.ui.app.AppRoute

class SettingsActivity : LegacyRouteActivity() {
    override fun resolveRoute(intent: Intent?): String = AppRoute.Settings.route
}
