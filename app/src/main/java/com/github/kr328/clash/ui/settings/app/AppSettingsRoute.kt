package com.github.kr328.clash.ui.settings.app

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.kr328.clash.MainActivity
import com.github.kr328.clash.RestartReceiver
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.app.ClashViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettingsViewModel(application: Application) : ClashViewModel(application) {
    private val serviceStore = ServiceStore(application)
    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun onAutoRestartChange(value: Boolean) {
        autoRestart = value
        refreshState()
    }

    fun onDarkModeChange(value: DarkMode) {
        uiStore.darkMode = value
        refreshState()
    }

    fun onHideAppIconChange(value: Boolean) {
        uiStore.hideAppIcon = value
        val newState = if (value) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        app.packageManager.setComponentEnabledSetting(
            ComponentName(app, "${MainActivity::class.java.name}Alias"),
            newState,
            PackageManager.DONT_KILL_APP,
        )
        refreshState()
    }

    fun onHideFromRecentsChange(value: Boolean) {
        uiStore.hideFromRecents = value
        refreshState()
    }

    fun onDynamicNotificationChange(value: Boolean) {
        serviceStore.dynamicNotification = value
        refreshState()
    }

    override fun onStarted() = refreshState()
    override fun onStopped(cause: String?) = refreshState()
    override fun onServiceRecreated() = refreshState()

    private fun refreshState() {
        _uiState.value = AppSettingsUiState(
            autoRestart = autoRestart,
            darkMode = uiStore.darkMode,
            hideAppIcon = uiStore.hideAppIcon,
            hideFromRecents = uiStore.hideFromRecents,
            dynamicNotification = serviceStore.dynamicNotification,
            running = clashRunning,
        )
    }

    private var autoRestart: Boolean
        get() {
            val status = app.packageManager.getComponentEnabledSetting(
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
            app.packageManager.setComponentEnabledSetting(
                RestartReceiver::class.componentName,
                status,
                PackageManager.DONT_KILL_APP,
            )
        }
}

@Composable
fun AppSettingsRoute(
    onBack: (() -> Unit)?,
    viewModel: AppSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AppSettingsScreen(
        title = LocalContext.current.getString(R.string.app),
        state = state,
        onBack = onBack,
        onAutoRestartChange = viewModel::onAutoRestartChange,
        onDarkModeChange = viewModel::onDarkModeChange,
        onHideAppIconChange = viewModel::onHideAppIconChange,
        onHideFromRecentsChange = viewModel::onHideFromRecentsChange,
        onDynamicNotificationChange = viewModel::onDynamicNotificationChange,
        snackbarHostState = remember { SnackbarHostState() },
    )
}
