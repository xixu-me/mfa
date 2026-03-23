package com.github.kr328.clash.ui.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashTheme
import kotlinx.coroutines.flow.Flow

@Composable
fun MainScreen(
    state: MainUiState,
    snackbarEvents: Flow<MainSnackbarEvent>,
    onToggleStatus: () -> Unit,
    onOpenProxy: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    onDismissAbout: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarEvents, snackbarHostState) {
        snackbarEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = event.duration,
            )

            if (result == SnackbarResult.ActionPerformed && event.action == MainSnackbarAction.OpenProfiles) {
                onOpenProfiles()
            }
        }
    }

    if (state.aboutVersionName != null) {
        AlertDialog(
            onDismissRequest = onDismissAbout,
            confirmButton = {
                TextButton(onClick = onDismissAbout) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.application_name))
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_clash),
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(R.dimen.about_icon_size)),
                    )
                    Text(text = state.aboutVersionName)
                }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = dimensionResource(R.dimen.main_padding_horizontal),
                    vertical = dimensionResource(R.dimen.item_padding_vertical),
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.main_label_margin_vertical)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(R.dimen.main_card_margin_vertical)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_clash),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(R.dimen.main_logo_size)),
                )
                Text(
                    text = stringResource(id = R.string.application_name),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            MainCard(
                title = if (state.clashRunning) {
                    stringResource(id = R.string.running)
                } else {
                    stringResource(id = R.string.stopped)
                },
                summary = if (state.clashRunning) {
                    stringResource(id = R.string.format_traffic_forwarded, state.forwarded)
                } else {
                    stringResource(id = R.string.tap_to_start)
                },
                iconRes = if (state.clashRunning) R.drawable.ic_outline_check_circle else R.drawable.ic_outline_not_interested,
                highlighted = state.clashRunning,
                onClick = onToggleStatus,
            )

            if (state.clashRunning) {
                MainCard(
                    title = stringResource(id = R.string.proxy),
                    summary = state.mode,
                    iconRes = R.drawable.ic_baseline_apps,
                    highlighted = false,
                    onClick = onOpenProxy,
                )
            }

            MainCard(
                title = stringResource(id = R.string.profile),
                summary = state.profileName?.let {
                    stringResource(id = R.string.format_profile_activated, it)
                } ?: stringResource(id = R.string.not_selected),
                iconRes = R.drawable.ic_baseline_view_list,
                highlighted = false,
                onClick = onOpenProfiles,
            )

            if (state.clashRunning && state.hasProviders) {
                MainLabel(
                    title = stringResource(id = R.string.providers),
                    iconRes = R.drawable.ic_baseline_swap_vertical_circle,
                    onClick = onOpenProviders,
                )
            }

            MainLabel(
                title = stringResource(id = R.string.logs),
                iconRes = R.drawable.ic_baseline_assignment,
                onClick = onOpenLogs,
            )
            MainLabel(
                title = stringResource(id = R.string.settings),
                iconRes = R.drawable.ic_baseline_settings,
                onClick = onOpenSettings,
            )
            MainLabel(
                title = stringResource(id = R.string.help),
                iconRes = R.drawable.ic_baseline_help_center,
                onClick = onOpenHelp,
            )
            MainLabel(
                title = stringResource(id = R.string.about),
                iconRes = R.drawable.ic_baseline_info,
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
private fun MainCard(
    title: String,
    summary: String,
    iconRes: Int,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (highlighted) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.dialog_padding)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun MainLabel(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.large_item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        Box(
            modifier = Modifier.size(dimensionResource(R.dimen.large_item_header_component_size)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    ClashTheme {
        MainScreen(
            state = MainUiState(
                clashRunning = true,
                forwarded = "12.30 MiB",
                mode = "Rule Mode",
                profileName = "Demo",
                hasProviders = true,
            ),
            snackbarEvents = kotlinx.coroutines.flow.emptyFlow(),
            onToggleStatus = {},
            onOpenProxy = {},
            onOpenProfiles = {},
            onOpenProviders = {},
            onOpenLogs = {},
            onOpenSettings = {},
            onOpenHelp = {},
            onOpenAbout = {},
            onDismissAbout = {},
        )
    }
}
