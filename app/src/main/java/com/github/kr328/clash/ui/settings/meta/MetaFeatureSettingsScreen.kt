package com.github.kr328.clash.ui.settings.meta

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
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceClickableItem
import com.github.kr328.clash.design.compose.PreferenceEditableTextListItem
import com.github.kr328.clash.design.compose.PreferenceGroup
import com.github.kr328.clash.design.compose.PreferenceOption
import com.github.kr328.clash.design.compose.PreferenceSelectableItem
import com.github.kr328.clash.design.compose.dependencyEnabled

@Composable
fun MetaFeatureSettingsScreen(
    title: String,
    state: MetaFeatureSettingsUiState,
    onBack: () -> Unit,
    onResetRequested: () -> Unit,
    onResetDismissed: () -> Unit,
    onResetConfirmed: () -> Unit,
    onConfigurationChanged: (ConfigurationOverride.() -> Unit) -> Unit,
    onImportGeoIp: () -> Unit,
    onImportGeoSite: () -> Unit,
    onImportCountry: () -> Unit,
    onImportASN: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val configuration = state.configuration
    val placeholder = stringResource(id = R.string.dont_modify)
    val empty = stringResource(id = R.string.empty)
    val context = LocalContext.current
    val elementsSummary: (Int) -> String = { count ->
        context.getString(R.string.format_elements, count)
    }
    val booleanOptions: List<PreferenceOption<Boolean?>> = listOf(
        PreferenceOption<Boolean?>(null, placeholder),
        PreferenceOption(true, stringResource(id = R.string.enabled)),
        PreferenceOption(false, stringResource(id = R.string.disabled)),
    )
    val snifferEnabled = dependencyEnabled(configuration.sniffer.enable)

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
            PreferenceGroup(title = stringResource(id = R.string.settings)) {
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.unified_delay),
                    value = configuration.unifiedDelay,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { unifiedDelay = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.geodata_mode),
                    value = configuration.geodataMode,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { geodataMode = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.tcp_concurrent),
                    value = configuration.tcpConcurrent,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { tcpConcurrent = it } },
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.find_process_mode),
                    value = configuration.findProcessMode,
                    options = listOf(
                        PreferenceOption<ConfigurationOverride.FindProcessMode?>(null, placeholder),
                        PreferenceOption(ConfigurationOverride.FindProcessMode.Off, stringResource(id = R.string.off)),
                        PreferenceOption(ConfigurationOverride.FindProcessMode.Strict, stringResource(id = R.string.strict)),
                        PreferenceOption(ConfigurationOverride.FindProcessMode.Always, stringResource(id = R.string.always)),
                    ),
                    onSelected = { onConfigurationChanged { findProcessMode = it } },
                )
            }

            PreferenceGroup(title = stringResource(id = R.string.sniffer_setting)) {
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.strategy),
                    value = configuration.sniffer.enable,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.enable = it } },
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.sniff_http_ports),
                    values = configuration.sniffer.sniff.http.ports,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.sniff.http.ports = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.sniff_http_override_destination),
                    value = configuration.sniffer.sniff.http.overrideDestination,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.sniff.http.overrideDestination = it } },
                    enabled = snifferEnabled,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.sniff_tls_ports),
                    values = configuration.sniffer.sniff.tls.ports,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.sniff.tls.ports = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.sniff_tls_override_destination),
                    value = configuration.sniffer.sniff.tls.overrideDestination,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.sniff.tls.overrideDestination = it } },
                    enabled = snifferEnabled,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.sniff_quic_ports),
                    values = configuration.sniffer.sniff.quic.ports,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.sniff.quic.ports = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.sniff_quic_override_destination),
                    value = configuration.sniffer.sniff.quic.overrideDestination,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.sniff.quic.overrideDestination = it } },
                    enabled = snifferEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.force_dns_mapping),
                    value = configuration.sniffer.forceDnsMapping,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.forceDnsMapping = it } },
                    enabled = snifferEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.parse_pure_ip),
                    value = configuration.sniffer.parsePureIp,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.parsePureIp = it } },
                    enabled = snifferEnabled,
                )
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.override_destination),
                    value = configuration.sniffer.overrideDestination,
                    options = booleanOptions,
                    onSelected = { onConfigurationChanged { sniffer.overrideDestination = it } },
                    enabled = snifferEnabled,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.force_domain),
                    values = configuration.sniffer.forceDomain,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.forceDomain = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.skip_domain),
                    values = configuration.sniffer.skipDomain,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.skipDomain = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.skip_src_address),
                    values = configuration.sniffer.skipSrcAddress,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.skipSrcAddress = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
                PreferenceEditableTextListItem(
                    title = stringResource(id = R.string.skip_dst_address),
                    values = configuration.sniffer.skipDstAddress,
                    placeholder = placeholder,
                    onValueChange = { onConfigurationChanged { sniffer.skipDstAddress = it } },
                    enabled = snifferEnabled,
                    empty = empty,
                    formatElements = elementsSummary,
                )
            }

            PreferenceGroup(title = stringResource(id = R.string.geox_files)) {
                PreferenceClickableItem(
                    title = stringResource(id = R.string.import_geoip_file),
                    summary = stringResource(id = R.string.press_to_import),
                    onClick = onImportGeoIp,
                )
                PreferenceClickableItem(
                    title = stringResource(id = R.string.import_geosite_file),
                    summary = stringResource(id = R.string.press_to_import),
                    onClick = onImportGeoSite,
                )
                PreferenceClickableItem(
                    title = stringResource(id = R.string.import_country_file),
                    summary = stringResource(id = R.string.press_to_import),
                    onClick = onImportCountry,
                )
                PreferenceClickableItem(
                    title = stringResource(id = R.string.import_asn_file),
                    summary = stringResource(id = R.string.press_to_import),
                    onClick = onImportASN,
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

@Preview
@Composable
private fun MetaFeatureSettingsScreenPreview() {
    ClashTheme {
        MetaFeatureSettingsScreen(
            title = "Meta Features",
            state = MetaFeatureSettingsUiState(configuration = ConfigurationOverride()),
            onBack = {},
            onResetRequested = {},
            onResetDismissed = {},
            onResetConfirmed = {},
            onConfigurationChanged = {},
            onImportGeoIp = {},
            onImportGeoSite = {},
            onImportCountry = {},
            onImportASN = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
