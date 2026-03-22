package com.github.kr328.clash

import com.github.kr328.clash.common.compat.versionCodeCompat
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.log.SystemLogcat
import com.github.kr328.clash.ui.appcrashed.AppCrashedScreen
import com.github.kr328.clash.ui.appcrashed.AppCrashedUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AppCrashedActivity : BaseActivity() {
    override suspend fun main() {
        val packageInfo = withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0)
        }

        Log.i("App version: versionName = ${packageInfo.versionName} versionCode = ${packageInfo.versionCodeCompat}")

        val logs = withContext(Dispatchers.IO) {
            SystemLogcat.dumpCrash()
        }

        val state = AppCrashedUiState(logs = logs)

        setComposeContent {
            ClashTheme {
                AppCrashedScreen(
                    title = title?.toString().orEmpty(),
                    state = state,
                    onBack = onBackPressedDispatcher::onBackPressed,
                )
            }
        }

        while (isActive) {
            events.receive()
        }
    }
}
