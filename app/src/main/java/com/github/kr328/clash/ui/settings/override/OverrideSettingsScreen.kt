package com.github.kr328.clash.ui.settings.override

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceEditableTextListItem
import com.github.kr328.clash.design.compose.PreferenceEditableTextMapItem
import com.github.kr328.clash.design.compose.PreferenceGroup
import com.github.kr328.clash.design.compose.PreferenceOption
import com.github.kr328.clash.design.compose.PreferenceSelectableItem
import com.github.kr328.clash.design.compose.PreferenceTextFieldItem
import com.github.kr328.clash.design.compose.dependencyEnabled

@Composable
fun OverrideSettingsScreen(
    title: String,
    state: OverrideSettingsUiState,
    onBack: (() -> Unit)?,
    onResetRequested: () -> Unit,
    onResetDismissed: () -> Unit,
    onResetConfirmed: () -> Unit,
    onConfigurationChanged: (ConfigurationOverride.() -> Unit) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val configuration = state.configuration
    val placeholder = stringResource(id = R.string.dont_modify)
    val empty = stringResource(id = R.string.empty)
    val disabled = stringResource(id = R.string.disabled)
    val defaultLabel = stringResource(id = R.string.default_)
    val context = LocalContext.current
    val elementsSummary: (Int) -> String = { count ->
        context.getString(R.string.format_elements, count)
    }
    val booleanOptions: List<PreferenceOption<Boolean?>> = listOf(
        PreferenceOption<Boolean?>(null, placeholder),
        PreferenceOption(true, stringResource(id = R.string.enabled)),
        PreferenceOption(false, stringResource(id = R.string.disabled)),
    )
    val dnsEnabled = dependencyEnabled(configuration.dns.enable)

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = onResetRequested) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_replay),
                    contentDescription = stringResource(id = R.string.reset),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceGroup(title = stringResource(id = R.string.general)) {
                PortItem(stringResource(id = R.string.http_port), configuration.httpPort) {
                    onConfigurationChanged { httpPort = it }
                }
                PortItem(stringResource(id = R.string.socks_port), configuration.socksPort) {
                    onConfigurationChanged { socksPort = it }
                }
                PortItem(stringResource(id = R.string.redirect_port), configuration.redirectPort) {
                    onConfigurationChanged { redirectPort = it }
                }
                PortItem(stringResource(id = R.string.tproxy_port), configuration.tproxyPort) {
                    onConfigurationChanged { tproxyPort = it }
                }
                PortItem(stringResource(id = R.string.mixed_port), configuration.mixedPort) {
                    onConfigurationChanged { mixedPort = it }
                }
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.authentication),
                    values = configuration.authentication,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { authentication = it } },
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.allow_lan),
                    value = configuration.allowLan,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { allowLan = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.ipv6),
                    value = configuration.ipv6,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { ipv6 = it } },
                )
                PreferenceTextFieldItem(
                    title = stringResource(id = R.string.bind_address),
                    value = configuration.bindAddress,
                    placeholder = placeholder,
                    empty = defaultLabel,
                    onValueChange = { onConfigurationChanged { bindAddress = it } },
                )
                PreferenceTextFieldItem(
                    title = stringResource(id = R.string.external_controller),
                    value = configuration.externalController,
                    placeholder = placeholder,
                    empty = defaultLabel,
                    onValueChange = { onConfigurationChanged { externalController = it } },
                )
                PreferenceTextFieldItem(
                    title = stringResource(id = R.string.external_controller_tls),
                    value = configuration.externalControllerTLS,
                    placeholder = placeholder,
                    empty = defaultLabel,
                    onValueChange = { onConfigurationChanged { externalControllerTLS = it } },
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.allow_origins),
                    values = configuration.externalControllerCors.allowOrigins,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { externalControllerCors.allowOrigins = it } },
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.allow_private_network),
                    value = configuration.externalControllerCors.allowPrivateNetwork,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { externalControllerCors.allowPrivateNetwork = it } },
                )
                PreferenceTextFieldItem(
                    title = stringResource(id = R.string.secret),
                    value = configuration.secret,
                    placeholder = placeholder,
                    empty = defaultLabel,
                    onValueChange = { onConfigurationChanged { secret = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.mode),
                    value = configuration.mode,
                    options = listOf(
                        PreferenceOption<TunnelState.Mode?>(null, placeholder),
                        PreferenceOption(TunnelState.Mode.Direct, stringResource(id = R.string.direct_mode)),
                        PreferenceOption(TunnelState.Mode.Global, stringResource(id = R.string.global_mode)),
                        PreferenceOption(TunnelState.Mode.Rule, stringResource(id = R.string.rule_mode)),
                    ),
                    onSelected = { onConfigurationChanged { mode = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.log_level),
                    value = configuration.logLevel,
                    options = listOf(
                        PreferenceOption<LogMessage.Level?>(null, placeholder),
                        PreferenceOption(LogMessage.Level.Info, stringResource(id = R.string.info)),
                        PreferenceOption(LogMessage.Level.Warning, stringResource(id = R.string.warning)),
                        PreferenceOption(LogMessage.Level.Error, stringResource(id = R.string.error)),
                        PreferenceOption(LogMessage.Level.Debug, stringResource(id = R.string.debug)),
                        PreferenceOption(LogMessage.Level.Silent, stringResource(id = R.string.silent)),
                    ),
                    onSelected = { onConfigurationChanged { logLevel = it } },
                )
                PreferenceEditableTextMapItem(
                    title = stringResource(id = R.string.hosts),
                    values = configuration.hosts,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { hosts = it } },
                    empty = empty,
                    formatElements = elementsSummary,
                )
            }

            PreferenceGroup(title = stringResource(id = R.string.dns)) {
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.strategy),
                    value = configuration.dns.enable,
                    options = listOf(
                        PreferenceOption<Boolean?>(null, placeholder),
                        PreferenceOption(true, stringResource(id = R.string.force_enable)),
                        PreferenceOption(false, stringResource(id = R.string.use_built_in)),
                    ),
                    onSelected = { onConfigurationChanged { dns.enable = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.prefer_h3),
                    value = configuration.dns.preferH3,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { dns.preferH3 = it } },
                    enabled = dnsEnabled,
                )
                PreferenceTextFieldItem(
                    title = stringResource(id = R.string.listen),
                    value = configuration.dns.listen,
                    placeholder = placeholder,
                    empty = disabled,
                    onValueChange = { onConfigurationChanged { dns.listen = it } },
                    enabled = dnsEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.append_system_dns),
                    value = configuration.app.appendSystemDns,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { app.appendSystemDns = it } },
                    enabled = dnsEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.ipv6),
                    value = configuration.dns.ipv6,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { dns.ipv6 = it } },
                    enabled = dnsEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.use_hosts),
                    value = configuration.dns.useHosts,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { dns.useHosts = it } },
                    enabled = dnsEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.enhanced_mode),
                    value = configuration.dns.enhancedMode,
                    options = listOf(
                        PreferenceOption<ConfigurationOverride.DnsEnhancedMode?>(null, placeholder),
                        PreferenceOption(ConfigurationOverride.DnsEnhancedMode.None, stringResource(id = R.string.disabled)),
                        PreferenceOption(ConfigurationOverride.DnsEnhancedMode.FakeIp, stringResource(id = R.string.fakeip)),
                        PreferenceOption(ConfigurationOverride.DnsEnhancedMode.Mapping, stringResource(id = R.string.mapping)),
                    ),
                    onSelected = { onConfigurationChanged { dns.enhancedMode = it } },
                    enabled = dnsEnabled,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.name_server),
                    values = configuration.dns.nameServer,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.nameServer = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.fallback),
                    values = configuration.dns.fallback,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.fallback = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.default_name_server),
                    values = configuration.dns.defaultServer,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.defaultServer = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.fakeip_filter),
                    values = configuration.dns.fakeIpFilter,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.fakeIpFilter = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.fakeip_filter_mode),
                    value = configuration.dns.fakeIPFilterMode,
                    options = listOf(
                        PreferenceOption<ConfigurationOverride.FilterMode?>(null, placeholder),
                        PreferenceOption(ConfigurationOverride.FilterMode.BlackList, stringResource(id = R.string.blacklist)),
                        PreferenceOption(ConfigurationOverride.FilterMode.WhiteList, stringResource(id = R.string.whitelist)),
                    ),
                    onSelected = { onConfigurationChanged { dns.fakeIPFilterMode = it } },
                    enabled = dnsEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.geoip_fallback),
                    value = configuration.dns.fallbackFilter.geoIp,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { dns.fallbackFilter.geoIp = it } },
                    enabled = dnsEnabled,
                )
                PreferenceTextFieldItem(
                    title = stringResource(id = R.string.geoip_fallback_code),
                    value = configuration.dns.fallbackFilter.geoIpCode,
                    placeholder = placeholder,
                    empty = stringResource(id = R.string.raw_cn),
                    onValueChange = { onConfigurationChanged { dns.fallbackFilter.geoIpCode = it } },
                    enabled = dnsEnabled,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.domain_fallback),
                    values = configuration.dns.fallbackFilter.domain,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.fallbackFilter.domain = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.ipcidr_fallback),
                    values = configuration.dns.fallbackFilter.ipcidr,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.fallbackFilter.ipcidr = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextMapItem(
                    title = stringResource(id = R.string.name_server_policy),
                    values = configuration.dns.nameserverPolicy,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { dns.nameserverPolicy = it } },
                    enabled = dnsEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
            }
        }
    }

    if (state.showResetConfirm) {
        AlertDialog(
            onDismissRequest = onResetDismissed,
            title = { Text(text = stringResource(id = R.string.reset_override_settings)) },
            text = { Text(text = stringResource(id = R.string.reset_override_settings_message)) },
            confirmButton = {
                TextButton(onClick = onResetConfirmed) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onResetDismissed) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun PortItem(
    title: String,
    value: Int?,
    onValueChange: (Int?) -> Unit,
) {
    PreferenceTextFieldItem(
        title = title,
        value = if (value == null) null else if (value > 0) value.toString() else "",
        placeholder = stringResource(id = R.string.dont_modify),
        empty = stringResource(id = R.string.disabled),
        onValueChange = {
            onValueChange(
                when {
                    it == null -> null
                    it.toIntOrNull() != null -> it.toInt()
                    else -> 0
                },
            )
        },
        keyboardType = KeyboardType.Number,
    )
}

@Preview
@Composable
private fun OverrideSettingsScreenPreview() {
    ClashTheme {
        OverrideSettingsScreen(
            title = "Override",
            state = OverrideSettingsUiState(configuration = ConfigurationOverride()),
            onBack = {},
            onResetRequested = {},
            onResetDismissed = {},
            onResetConfirmed = {},
            onConfigurationChanged = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
