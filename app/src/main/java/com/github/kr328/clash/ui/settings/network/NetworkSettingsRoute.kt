package com.github.kr328.clash.ui.settings.network

import android.app.Application
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.service.model.AccessControlMode
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.app.ClashViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkSettingsViewModel(application: Application) : ClashViewModel(application) {
    private val serviceStore = ServiceStore(application)
    private val _uiState = MutableStateFlow(NetworkSettingsUiState())
    val uiState = _uiState.asStateFlow()
    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 4)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        refreshState()
    }

    fun onEnableVpnChange(value: Boolean) { uiStore.enableVpn = value; refreshState() }
    fun onBypassPrivateNetworkChange(value: Boolean) { serviceStore.bypassPrivateNetwork = value; refreshState() }
    fun onDnsHijackingChange(value: Boolean) { serviceStore.dnsHijacking = value; refreshState() }
    fun onAllowBypassChange(value: Boolean) { serviceStore.allowBypass = value; refreshState() }
    fun onAllowIpv6Change(value: Boolean) { serviceStore.allowIpv6 = value; refreshState() }
    fun onSystemProxyChange(value: Boolean) { serviceStore.systemProxy = value; refreshState() }
    fun onTunStackModeChange(value: String) { serviceStore.tunStackMode = value; refreshState() }
    fun onAccessControlModeChange(value: AccessControlMode) { serviceStore.accessControlMode = value; refreshState() }

    override fun onStarted() = onRunningChanged()
    override fun onStopped(cause: String?) = onRunningChanged()
    override fun onServiceRecreated() = onRunningChanged()

    private fun onRunningChanged() {
        refreshState()
        if (clashRunning) {
            _snackbarMessages.tryEmit(
                ClashSnackbarMessage(
                    message = app.getString(R.string.options_unavailable),
                    duration = SnackbarDuration.Long,
                ),
            )
        }
    }

    private fun refreshState() {
        _uiState.value = NetworkSettingsUiState(
            enableVpn = uiStore.enableVpn,
            bypassPrivateNetwork = serviceStore.bypassPrivateNetwork,
            dnsHijacking = serviceStore.dnsHijacking,
            allowBypass = serviceStore.allowBypass,
            allowIpv6 = serviceStore.allowIpv6,
            systemProxy = serviceStore.systemProxy,
            tunStackMode = serviceStore.tunStackMode,
            accessControlMode = serviceStore.accessControlMode,
            running = clashRunning,
            supportsSystemProxy = android.os.Build.VERSION.SDK_INT >= 29,
        )
    }
}

@Composable
fun NetworkSettingsRoute(
    navController: NavController,
    onBack: (() -> Unit)?,
    viewModel: NetworkSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    NetworkSettingsScreen(
        title = LocalContext.current.getString(R.string.network),
        state = state,
        onBack = onBack,
        onEnableVpnChange = viewModel::onEnableVpnChange,
        onBypassPrivateNetworkChange = viewModel::onBypassPrivateNetworkChange,
        onDnsHijackingChange = viewModel::onDnsHijackingChange,
        onAllowBypassChange = viewModel::onAllowBypassChange,
        onAllowIpv6Change = viewModel::onAllowIpv6Change,
        onSystemProxyChange = viewModel::onSystemProxyChange,
        onTunStackModeChange = viewModel::onTunStackModeChange,
        onAccessControlModeChange = viewModel::onAccessControlModeChange,
        onOpenAccessControl = { navController.navigate(AppRoute.AccessControl.route) },
        snackbarHostState = snackbarHostState,
    )
}
