package com.github.kr328.clash

import android.content.Intent
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.ui.app.AppRouteIntent

class PropertiesActivity : LegacyRouteActivity() {
    override fun resolveRoute(intent: Intent?): String? {
        return AppRouteIntent.propertiesOrNull(intent?.uuid?.toString())
    }
}
