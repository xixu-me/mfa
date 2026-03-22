package com.github.kr328.clash.ui.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.github.kr328.clash.design.compose.PreferenceClickableItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    title: String,
    state: FilesUiState,
    onBack: () -> Unit,
    onOpenFile: (String) -> Unit,
    onOpenMenu: (String) -> Unit,
    onDismissMenu: () -> Unit,
    onNewFile: () -> Unit,
    onRenameFile: (String) -> Unit,
    onImportToFile: (String) -> Unit,
    onExportFile: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onDismissFileNameDialog: () -> Unit,
    onConfirmFileNameDialog: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val activeMenuItem = state.files.firstOrNull { it.id == state.activeMenuFileId }

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            if (!state.currentInBaseDir) {
                IconButton(onClick = onNewFile) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_add),
                        contentDescription = stringResource(id = R.string._new),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(state.files, key = FileItemUiState::id) { file ->
                FileRow(
                    file = file,
                    onOpen = { onOpenFile(file.id) },
                    onMore = { onOpenMenu(file.id) },
                )
            }
        }
    }

    if (activeMenuItem != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissMenu,
        ) {
            Column(
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.bottom_sheet_menu_items_padding)),
            ) {
                if (activeMenuItem.canImport) {
                    PreferenceClickableItem(
                        title = stringResource(id = R.string.import_),
                        onClick = {
                            onDismissMenu()
                            onImportToFile(activeMenuItem.id)
                        },
                    )
                }
                if (activeMenuItem.canExport) {
                    PreferenceClickableItem(
                        title = stringResource(id = R.string.export),
                        onClick = {
                            onDismissMenu()
                            onExportFile(activeMenuItem.id)
                        },
                    )
                }
                if (activeMenuItem.canRename) {
                    PreferenceClickableItem(
                        title = stringResource(id = R.string.rename),
                        onClick = {
                            onDismissMenu()
                            onRenameFile(activeMenuItem.id)
                        },
                    )
                }
                if (activeMenuItem.canDelete) {
                    PreferenceClickableItem(
                        title = stringResource(id = R.string.delete),
                        onClick = {
                            onDismissMenu()
                            onDeleteFile(activeMenuItem.id)
                        },
                    )
                }
            }
        }
    }

    state.pendingFileNameDialog?.let { dialog ->
        var value by remember(dialog) { mutableStateOf(dialog.initialValue) }
        AlertDialog(
            onDismissRequest = onDismissFileNameDialog,
            title = { Text(text = dialog.title) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = dialog.title) },
                    isError = value.isBlank(),
                    supportingText = {
                        if (value.isBlank()) {
                            Text(text = stringResource(id = R.string.invalid_file_name))
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmFileNameDialog(value) },
                    enabled = value.isNotBlank(),
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissFileNameDialog) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun FileRow(
    file: FileItemUiState,
    onOpen: () -> Unit,
    onMore: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        Icon(
            painter = painterResource(
                id = if (file.isDirectory) R.drawable.ic_outline_folder else R.drawable.ic_outline_article,
            ),
            contentDescription = null,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
            )
            file.sizeSummary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        file.elapsedSummary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onMore) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_more_vert),
                contentDescription = null,
            )
        }
    }
}

@Preview
@Composable
private fun FilesScreenPreview() {
    ClashTheme {
        FilesScreen(
            title = "Files",
            state = FilesUiState(
                files = listOf(
                    FileItemUiState(
                        id = "1",
                        name = "config.yaml",
                        sizeSummary = "2.50 KiB",
                        elapsedSummary = "3 min ago",
                        isDirectory = false,
                        canImport = true,
                        canExport = true,
                        canRename = true,
                        canDelete = true,
                    ),
                ),
                currentInBaseDir = false,
            ),
            onBack = {},
            onOpenFile = {},
            onOpenMenu = {},
            onDismissMenu = {},
            onNewFile = {},
            onRenameFile = {},
            onImportToFile = {},
            onExportFile = {},
            onDeleteFile = {},
            onDismissFileNameDialog = {},
            onConfirmFileNameDialog = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
