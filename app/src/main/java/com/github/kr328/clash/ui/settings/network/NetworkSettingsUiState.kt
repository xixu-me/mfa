package com.github.kr328.clash.ui.settings.network

import com.github.kr328.clash.service.model.AccessControlMode

data class NetworkSettingsUiState(
    val enableVpn: Boolean = true,
    val bypassPrivateNetwork: Boolean = true,
    val dnsHijacking: Boolean = true,
    val allowBypass: Boolean = true,
    val allowIpv6: Boolean = false,
    val systemProxy: Boolean = true,
    val tunStackMode: String = "system",
    val accessControlMode: AccessControlMode = AccessControlMode.AcceptAll,
    val running: Boolean = false,
    val supportsSystemProxy: Boolean = false,
)
