package com.github.kr328.clash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.kr328.clash.ui.app.AppRouteIntent

abstract class LegacyRouteActivity : AppCompatActivity() {
    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val route = resolveRoute(intent)
        if (route == null) {
            finish()
            return
        }

        startActivity(AppRouteIntent.mainActivity(route))
        finish()
    }

    protected abstract fun resolveRoute(intent: Intent?): String?
}
