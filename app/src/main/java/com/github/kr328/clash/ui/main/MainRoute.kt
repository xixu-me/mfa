package com.github.kr328.clash.ui.main

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.LogcatService
import com.github.kr328.clash.design.R
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

sealed interface MainEffect {
    data class RequestVpnPermission(val intent: Intent) : MainEffect
}

class MainViewModel(application: Application) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<MainSnackbarEvent>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _effects = MutableSharedFlow<MainEffect>(extraBufferCapacity = 4)
    val effects = _effects.asSharedFlow()

    init {
        refreshMainState()
        viewModelScope.launch {
            while (isActive) {
                if (clashRunning) {
                    refreshTraffic()
                }
                delay(TimeUnit.SECONDS.toMillis(1))
            }
        }
    }

    fun onToggleStatus() {
        if (clashRunning) {
            app.stopClashService()
        } else {
            startClash()
        }
    }

    fun onDismissAbout() {
        _uiState.value = _uiState.value.copy(aboutVersionName = null)
    }

    fun onOpenAbout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(aboutVersionName = queryAppVersionName())
        }
    }

    fun onVpnPermissionResult(approved: Boolean) {
        if (approved) {
            app.startClashService()
        } else {
            _snackbarEvents.tryEmit(
                MainSnackbarEvent(
                    message = app.getString(R.string.unable_to_start_vpn),
                ),
            )
        }
    }

    override fun onServiceRecreated() = refreshMainState()
    override fun onStarted() = refreshMainState()
    override fun onStopped(cause: String?) = refreshMainState()
    override fun onProfileChanged() = refreshMainState()
    override fun onProfileLoaded() = refreshMainState()

    private fun refreshMainState() {
        viewModelScope.launch {
            val tunnelState = withClash { queryTunnelState() }
            val providers = withClash { queryProviders() }
            val profileName = withProfile { queryActive()?.name }

            _uiState.value = _uiState.value.copy(
                clashRunning = clashRunning,
                mode = tunnelState.mode.displayName(app),
                hasProviders = providers.isNotEmpty(),
                profileName = profileName,
                forwarded = if (clashRunning) _uiState.value.forwarded else "0 Bytes",
            )

            if (clashRunning) {
                refreshTraffic()
            }
        }
    }

    private fun refreshTraffic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                forwarded = withClash {
                    queryTrafficTotal().trafficTotal()
                },
            )
        }
    }

    private fun startClash() {
        viewModelScope.launch {
            val active = withProfile { queryActive() }

            if (active == null || !active.imported) {
                _snackbarEvents.emit(
                    MainSnackbarEvent(
                        message = app.getString(R.string.no_profile_selected),
                        actionLabel = app.getString(R.string.profiles),
                        action = MainSnackbarAction.OpenProfiles,
                    ),
                )
                return@launch
            }

            try {
                val vpnRequest = app.startClashService()
                if (vpnRequest != null) {
                    _effects.emit(MainEffect.RequestVpnPermission(vpnRequest))
                }
            } catch (_: Exception) {
                _snackbarEvents.emit(
                    MainSnackbarEvent(
                        message = app.getString(R.string.unable_to_start_vpn),
                    ),
                )
            }
        }
    }

    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            val versionName = app.packageManager.getPackageInfo(app.packageName, 0).versionName
            "$versionName\n${com.github.kr328.clash.core.bridge.Bridge.nativeCoreVersion().replace("_", "-")}"
        }
    }
}

@Composable
fun MainRoute(
    navController: NavController,
    viewModel: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onVpnPermissionResult(result.resultCode == android.app.Activity.RESULT_OK)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MainEffect.RequestVpnPermission -> vpnLauncher.launch(effect.intent)
            }
        }
    }

    MainScreen(
        state = state,
        snackbarEvents = viewModel.snackbarEvents,
        onToggleStatus = viewModel::onToggleStatus,
        onOpenProxy = { navController.navigate(AppRoute.Proxy.route) },
        onOpenProfiles = { navController.navigate(AppRoute.Profiles.route) },
        onOpenProviders = { navController.navigate(AppRoute.Providers.route) },
        onOpenLogs = {
            navController.navigate(
                if (LogcatService.running) AppRoute.LiveLogcat.route else AppRoute.Logs.route,
            )
        },
        onOpenSettings = { navController.navigate(AppRoute.Settings.route) },
        onOpenHelp = { navController.navigate(AppRoute.Help.route) },
        onOpenAbout = viewModel::onOpenAbout,
        onDismissAbout = viewModel::onDismissAbout,
    )
}

private fun com.github.kr328.clash.core.model.TunnelState.Mode.displayName(application: Application): String {
    return when (this) {
        com.github.kr328.clash.core.model.TunnelState.Mode.Direct -> application.getString(R.string.direct_mode)
        com.github.kr328.clash.core.model.TunnelState.Mode.Global -> application.getString(R.string.global_mode)
        com.github.kr328.clash.core.model.TunnelState.Mode.Rule,
        com.github.kr328.clash.core.model.TunnelState.Mode.Script -> application.getString(R.string.rule_mode)
    }
}
