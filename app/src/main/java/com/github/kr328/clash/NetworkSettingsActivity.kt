package com.github.kr328.clash

import android.os.Build
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.settings.network.NetworkSettingsScreen
import com.github.kr328.clash.ui.settings.network.NetworkSettingsUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.selects.select

class NetworkSettingsActivity : BaseActivity() {
    private val serviceStore by lazy { ServiceStore(this) }
    private val uiState = MutableStateFlow(NetworkSettingsUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 4)

    override suspend fun main() {
        refreshState()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                NetworkSettingsScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onEnableVpnChange = {
                        uiStore.enableVpn = it
                        refreshState()
                    },
                    onBypassPrivateNetworkChange = {
                        serviceStore.bypassPrivateNetwork = it
                        refreshState()
                    },
                    onDnsHijackingChange = {
                        serviceStore.dnsHijacking = it
                        refreshState()
                    },
                    onAllowBypassChange = {
                        serviceStore.allowBypass = it
                        refreshState()
                    },
                    onAllowIpv6Change = {
                        serviceStore.allowIpv6 = it
                        refreshState()
                    },
                    onSystemProxyChange = {
                        serviceStore.systemProxy = it
                        refreshState()
                    },
                    onTunStackModeChange = {
                        serviceStore.tunStackMode = it
                        refreshState()
                    },
                    onAccessControlModeChange = {
                        serviceStore.accessControlMode = it
                        refreshState()
                    },
                    onOpenAccessControl = {
                        startActivity(AccessControlActivity::class.intent)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        showRunningMessageIfNeeded()

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ClashStart, Event.ClashStop, Event.ServiceRecreated -> {
                            refreshState()
                            showRunningMessageIfNeeded()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun refreshState() {
        uiState.value = NetworkSettingsUiState(
            enableVpn = uiStore.enableVpn,
            bypassPrivateNetwork = serviceStore.bypassPrivateNetwork,
            dnsHijacking = serviceStore.dnsHijacking,
            allowBypass = serviceStore.allowBypass,
            allowIpv6 = serviceStore.allowIpv6,
            systemProxy = serviceStore.systemProxy,
            tunStackMode = serviceStore.tunStackMode,
            accessControlMode = serviceStore.accessControlMode,
            running = clashRunning,
            supportsSystemProxy = Build.VERSION.SDK_INT >= 29,
        )
    }

    private fun showRunningMessageIfNeeded() {
        if (clashRunning) {
            snackbarMessages.tryEmit(
                ClashSnackbarMessage(
                    message = getString(R.string.options_unavailable),
                    duration = SnackbarDuration.Long,
                ),
            )
        }
    }
}
