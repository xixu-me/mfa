package com.github.kr328.clash.ui.accesscontrol

import android.widget.ImageView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.model.AppInfoSort

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlScreen(
    title: String,
    state: AccessControlUiState,
    onBack: () -> Unit,
    onToggleApp: (String) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    onSelectInvert: () -> Unit,
    onToggleShowSystemApps: () -> Unit,
    onSetSort: (AppInfoSort) -> Unit,
    onToggleReverse: () -> Unit,
    onImportClipboard: () -> Unit,
    onExportClipboard: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (state.searchOpen) {
        SearchDialog(
            state = state,
            onDismiss = onCloseSearch,
            onQueryChanged = onSearchQueryChanged,
            onToggleApp = onToggleApp,
        )
    }

    ClashScaffold(
        title = title,
        onBack = onBack,
        actions = {
            IconButton(onClick = onOpenSearch) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_search),
                    contentDescription = stringResource(R.string.search),
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_more_vert),
                        contentDescription = stringResource(R.string.more),
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.select_all)) },
                        onClick = {
                            menuExpanded = false
                            onSelectAll()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.select_none)) },
                        onClick = {
                            menuExpanded = false
                            onSelectNone()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.select_invert)) },
                        onClick = {
                            menuExpanded = false
                            onSelectInvert()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.system_apps)) },
                        trailingIcon = {
                            if (state.showSystemApps) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_outline_check_circle),
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = {
                            onToggleShowSystemApps()
                        },
                    )
                    HorizontalDivider()
                    SortMenuItem(
                        label = stringResource(R.string.name),
                        selected = state.sort == AppInfoSort.Label,
                        onClick = { onSetSort(AppInfoSort.Label) },
                    )
                    SortMenuItem(
                        label = stringResource(R.string.package_name),
                        selected = state.sort == AppInfoSort.PackageName,
                        onClick = { onSetSort(AppInfoSort.PackageName) },
                    )
                    SortMenuItem(
                        label = stringResource(R.string.install_time),
                        selected = state.sort == AppInfoSort.InstallTime,
                        onClick = { onSetSort(AppInfoSort.InstallTime) },
                    )
                    SortMenuItem(
                        label = stringResource(R.string.update_time),
                        selected = state.sort == AppInfoSort.UpdateTime,
                        onClick = { onSetSort(AppInfoSort.UpdateTime) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.reverse)) },
                        trailingIcon = {
                            if (state.reverse) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_outline_check_circle),
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = { onToggleReverse() },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.import_from_clipboard)) },
                        onClick = {
                            menuExpanded = false
                            onImportClipboard()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export_to_clipboard)) },
                        onClick = {
                            menuExpanded = false
                            onExportClipboard()
                        },
                    )
                }
            }
        },
    ) { padding ->
        AppList(
            apps = state.apps,
            padding = padding,
            onToggleApp = onToggleApp,
        )
    }
}

@Composable
private fun SortMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_check_circle),
                    contentDescription = null,
                )
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun AppList(
    apps: List<AccessControlAppItemUiState>,
    padding: PaddingValues,
    onToggleApp: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppRow(
                app = app,
                onClick = { onToggleApp(app.packageName) },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AccessControlAppItemUiState,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    val icon = remember(app.packageName) {
        runCatching {
            packageManager.getApplicationIcon(app.packageName)
        }.getOrNull()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                start = dimensionResource(R.dimen.item_header_margin),
                top = dimensionResource(R.dimen.item_padding_vertical),
                end = dimensionResource(R.dimen.item_tailing_margin),
                bottom = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        AndroidView(
            factory = { ImageView(it) },
            modifier = Modifier.padding(end = dimensionResource(R.dimen.item_text_margin)),
            update = { view ->
                view.setImageDrawable(icon)
            },
        )

        androidx.compose.foundation.layout.Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        ) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Checkbox(
            checked = app.selected,
            onCheckedChange = { onClick() },
        )
    }
}

@Composable
private fun SearchDialog(
    state: AccessControlUiState,
    onDismiss: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onToggleApp: (String) -> Unit,
) {
    val filteredApps = remember(state.apps, state.searchQuery) {
        val keyword = state.searchQuery.trim()
        if (keyword.isEmpty()) {
            emptyList()
        } else {
            state.apps.filter {
                it.label.contains(keyword, ignoreCase = true) ||
                    it.packageName.contains(keyword, ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.item_header_margin)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_close),
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                    TextField(
                        value = state.searchQuery,
                        onValueChange = onQueryChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.keyword)) },
                        singleLine = true,
                    )
                }

                AppList(
                    apps = filteredApps,
                    padding = PaddingValues(bottom = dimensionResource(R.dimen.dialog_padding)),
                    onToggleApp = onToggleApp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AccessControlScreenPreview() {
    ClashTheme {
        AccessControlScreen(
            title = "Access Control",
            state = AccessControlUiState(
                apps = listOf(
                    AccessControlAppItemUiState(
                        packageName = "com.example.app",
                        label = "Example",
                        selected = true,
                    ),
                ),
                searchOpen = false,
            ),
            onBack = {},
            onToggleApp = {},
            onSelectAll = {},
            onSelectNone = {},
            onSelectInvert = {},
            onToggleShowSystemApps = {},
            onSetSort = {},
            onToggleReverse = {},
            onImportClipboard = {},
            onExportClipboard = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchQueryChanged = {},
        )
    }
}
