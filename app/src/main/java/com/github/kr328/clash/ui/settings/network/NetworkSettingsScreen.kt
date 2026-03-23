package com.github.kr328.clash.ui.settings.network

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceClickableItem
import com.github.kr328.clash.design.compose.PreferenceGroup
import com.github.kr328.clash.design.compose.PreferenceOption
import com.github.kr328.clash.design.compose.PreferenceSelectableItem
import com.github.kr328.clash.design.compose.PreferenceSwitchItem
import com.github.kr328.clash.service.model.AccessControlMode

@Composable
fun NetworkSettingsScreen(
    title: String,
    state: NetworkSettingsUiState,
    onBack: () -> Unit,
    onEnableVpnChange: (Boolean) -> Unit,
    onBypassPrivateNetworkChange: (Boolean) -> Unit,
    onDnsHijackingChange: (Boolean) -> Unit,
    onAllowBypassChange: (Boolean) -> Unit,
    onAllowIpv6Change: (Boolean) -> Unit,
    onSystemProxyChange: (Boolean) -> Unit,
    onTunStackModeChange: (String) -> Unit,
    onAccessControlModeChange: (AccessControlMode) -> Unit,
    onOpenAccessControl: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val vpnOptionsEnabled = state.enableVpn && !state.running

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceGroup(title = stringResource(id = R.string.network)) {
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.route_system_traffic),
                    summary = stringResource(id = R.string.routing_via_vpn_service),
                    checked = state.enableVpn,
                    onCheckedChange = onEnableVpnChange,
                    iconRes = R.drawable.ic_baseline_vpn_lock,
                    enabled = !state.running,
                )
            }

            PreferenceGroup(title = stringResource(id = R.string.vpn_service_options)) {
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.bypass_private_network),
                    summary = stringResource(id = R.string.bypass_private_network_summary),
                    checked = state.bypassPrivateNetwork,
                    onCheckedChange = onBypassPrivateNetworkChange,
                    enabled = vpnOptionsEnabled,
                )
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.dns_hijacking),
                    summary = stringResource(id = R.string.dns_hijacking_summary),
                    checked = state.dnsHijacking,
                    onCheckedChange = onDnsHijackingChange,
                    enabled = vpnOptionsEnabled,
                )
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.allow_bypass),
                    summary = stringResource(id = R.string.allow_bypass_summary),
                    checked = state.allowBypass,
                    onCheckedChange = onAllowBypassChange,
                    enabled = vpnOptionsEnabled,
                )
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.allow_ipv6),
                    summary = stringResource(id = R.string.allow_ipv6_summary),
                    checked = state.allowIpv6,
                    onCheckedChange = onAllowIpv6Change,
                    enabled = vpnOptionsEnabled,
                )
                if (state.supportsSystemProxy) {
                    PreferenceSwitchItem(
                        title = stringResource(id = R.string.system_proxy),
                        summary = stringResource(id = R.string.system_proxy_summary),
                        checked = state.systemProxy,
                        onCheckedChange = onSystemProxyChange,
                        enabled = vpnOptionsEnabled,
                    )
                }
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.tun_stack_mode),
                    value = state.tunStackMode,
                    options = listOf(
                        PreferenceOption("system", stringResource(id = R.string.tun_stack_system)),
                        PreferenceOption("gvisor", stringResource(id = R.string.tun_stack_gvisor)),
                        PreferenceOption("mixed", stringResource(id = R.string.tun_stack_mixed)),
                    ),
                    onSelected = onTunStackModeChange,
                    enabled = vpnOptionsEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.access_control_mode),
                    value = state.accessControlMode,
                    options = listOf(
                        PreferenceOption(AccessControlMode.AcceptAll, stringResource(id = R.string.allow_all_apps)),
                        PreferenceOption(AccessControlMode.AcceptSelected, stringResource(id = R.string.allow_selected_apps)),
                        PreferenceOption(AccessControlMode.DenySelected, stringResource(id = R.string.deny_selected_apps)),
                    ),
                    onSelected = onAccessControlModeChange,
                    enabled = vpnOptionsEnabled,
                )
                PreferenceClickableItem(
                    title = stringResource(id = R.string.access_control_packages),
                    summary = stringResource(id = R.string.access_control_packages_summary),
                    onClick = onOpenAccessControl,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NetworkSettingsScreenPreview() {
    ClashTheme {
        NetworkSettingsScreen(
            title = "Network Settings",
            state = NetworkSettingsUiState(),
            onBack = {},
            onEnableVpnChange = {},
            onBypassPrivateNetworkChange = {},
            onDnsHijackingChange = {},
            onAllowBypassChange = {},
            onAllowIpv6Change = {},
            onSystemProxyChange = {},
            onTunStackModeChange = {},
            onAccessControlModeChange = {},
            onOpenAccessControl = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
