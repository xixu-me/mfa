package com.github.kr328.clash.ui.logs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme

@Composable
fun LogsScreen(
    title: String,
    state: LogsUiState,
    onBack: () -> Unit,
    onStartLogcat: () -> Unit,
    onDeleteAll: () -> Unit,
    onOpenFile: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(text = stringResource(id = R.string.delete_all_logs)) },
            text = { Text(text = stringResource(id = R.string.delete_all_logs_warn)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteAllDialog = false
                        onDeleteAll()
                    },
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = { showDeleteAllDialog = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_clear_all),
                    contentDescription = null,
                )
            }
        },
    ) { padding ->
        LogsContent(
            state = state,
            padding = padding,
            onStartLogcat = onStartLogcat,
            onOpenFile = onOpenFile,
        )
    }
}

@Composable
private fun LogsContent(
    state: LogsUiState,
    padding: PaddingValues,
    onStartLogcat: () -> Unit,
    onOpenFile: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    ) {
        item {
            ActionRow(
                title = stringResource(id = R.string.clash_logcat),
                summary = stringResource(id = R.string.tap_to_start),
                iconRes = R.drawable.ic_baseline_adb,
                onClick = onStartLogcat,
            )
        }

        item {
            Text(
                text = stringResource(id = R.string.history),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.item_header_margin),
                        top = dimensionResource(R.dimen.item_text_margin),
                        end = dimensionResource(R.dimen.item_header_margin),
                        bottom = dimensionResource(R.dimen.item_text_margin),
                    ),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        }

        items(state.files, key = { it.fileName }) { file ->
            ActionRow(
                title = file.fileName,
                summary = file.summary,
                iconRes = null,
                onClick = { onOpenFile(file.fileName) },
            )
        }
    }
}

@Composable
private fun ActionRow(
    title: String,
    summary: String,
    iconRes: Int?,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
        ) {
            if (iconRes != null) {
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
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun LogsScreenPreview() {
    ClashTheme {
        LogsScreen(
            title = "Logs",
            state = LogsUiState(
                files = listOf(
                    LogFileItemUiState(
                        fileName = "clash-123.log",
                        summary = "2026-03-23 11:11:11.111",
                    ),
                ),
            ),
            onBack = {},
            onStartLogcat = {},
            onDeleteAll = {},
            onOpenFile = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
