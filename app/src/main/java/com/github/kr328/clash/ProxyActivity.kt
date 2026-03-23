package com.github.kr328.clash

import android.content.Intent
import com.github.kr328.clash.ui.app.AppRoute

class ProxyActivity : LegacyRouteActivity() {
    override fun resolveRoute(intent: Intent?): String = AppRoute.Proxy.route
}
