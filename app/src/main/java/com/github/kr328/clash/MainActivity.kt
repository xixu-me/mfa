package com.github.kr328.clash

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.app.AppRouteIntent
import com.github.kr328.clash.ui.app.ClashApp

class MainActivity : AppCompatActivity() {
    private var launchRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchRoute = AppRouteIntent.resolveRoute(intent)

        setContent {
            ClashTheme {
                ClashApp(launchRoute = launchRoute)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchRoute = AppRouteIntent.resolveRoute(intent)
    }
}

val mainActivityAlias = "${MainActivity::class.java.name}Alias"
