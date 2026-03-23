package com.github.kr328.clash

import android.content.Intent
import com.github.kr328.clash.common.util.fileName
import com.github.kr328.clash.ui.app.AppRouteIntent

class LogcatActivity : LegacyRouteActivity() {
    override fun resolveRoute(intent: Intent?): String {
        return AppRouteIntent.logcatRoute(intent?.fileName)
    }
}
