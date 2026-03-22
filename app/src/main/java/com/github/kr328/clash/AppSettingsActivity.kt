package com.github.kr328.clash

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.settings.app.AppSettingsScreen
import com.github.kr328.clash.ui.settings.app.AppSettingsUiState
import com.github.kr328.clash.util.ApplicationObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class AppSettingsActivity : BaseActivity() {
    private val serviceStore by lazy { ServiceStore(this) }
    private val uiState = MutableStateFlow(AppSettingsUiState())

    override suspend fun main() {
        refreshState()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashTheme {
                AppSettingsScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onAutoRestartChange = {
                        autoRestart = it
                        refreshState()
                    },
                    onDarkModeChange = {
                        uiStore.darkMode = it
                        refreshState()
                        recreateAllActivities()
                    },
                    onHideAppIconChange = {
                        uiStore.hideAppIcon = it
                        onHideIconChange(it)
                        refreshState()
                    },
                    onHideFromRecentsChange = {
                        uiStore.hideFromRecents = it
                        refreshState()
                        recreateAllActivities()
                    },
                    onDynamicNotificationChange = {
                        serviceStore.dynamicNotification = it
                        refreshState()
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart, Event.ClashStop, Event.ServiceRecreated -> refreshState()
                        else -> Unit
                    }
                }
            }
        }
    }

    private var autoRestart: Boolean
        get() {
            val status = packageManager.getComponentEnabledSetting(
                RestartReceiver::class.componentName,
            )

            return status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        set(value) {
            val status = if (value) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            packageManager.setComponentEnabledSetting(
                RestartReceiver::class.componentName,
                status,
                PackageManager.DONT_KILL_APP,
            )
        }

    private fun onHideIconChange(hide: Boolean) {
        val newState = if (hide) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        packageManager.setComponentEnabledSetting(
            ComponentName(this, mainActivityAlias),
            newState,
            PackageManager.DONT_KILL_APP,
        )
    }

    private fun refreshState() {
        uiState.value = AppSettingsUiState(
            autoRestart = autoRestart,
            darkMode = uiStore.darkMode,
            hideAppIcon = uiStore.hideAppIcon,
            hideFromRecents = uiStore.hideFromRecents,
            dynamicNotification = serviceStore.dynamicNotification,
            running = clashRunning,
        )
    }

    private fun recreateAllActivities() {
        ApplicationObserver.createdActivities.forEach {
            it.recreate()
        }
    }
}
