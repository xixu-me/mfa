package com.github.kr328.clash.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun ProfilesScreen(
    title: String,
    state: ProfilesUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onUpdateAll: () -> Unit,
    onActivate: (String) -> Unit,
    onOpenMenu: (String) -> Unit,
    onDismissMenu: () -> Unit,
    onUpdate: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDelete: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val activeMenuItem = state.profiles.firstOrNull { it.id == state.activeMenuProfileId }

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            if (state.updateAllVisible) {
                if (state.updateAllRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.item_tailing_margin)),
                        strokeWidth = dimensionResource(R.dimen.divider_size) * 2,
                    )
                } else {
                    IconButton(onClick = onUpdateAll) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_sync),
                            contentDescription = null,
                        )
                    }
                }
            }
            IconButton(onClick = onCreate) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_add),
                    contentDescription = null,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(state.profiles, key = ProfileItemUiState::id) { profile ->
                ProfileRow(
                    profile = profile,
                    onClick = { onActivate(profile.id) },
                    onMore = { onOpenMenu(profile.id) },
                )
            }
        }
    }

    if (activeMenuItem != null) {
        ModalBottomSheet(onDismissRequest = onDismissMenu) {
            Column(
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.bottom_sheet_menu_items_padding)),
            ) {
                if (activeMenuItem.canUpdate) {
                    PreferenceClickableItem(
                        title = stringResource(R.string.update),
                        onClick = {
                            onDismissMenu()
                            onUpdate(activeMenuItem.id)
                        },
                    )
                }
                PreferenceClickableItem(
                    title = stringResource(R.string.edit),
                    onClick = {
                        onDismissMenu()
                        onEdit(activeMenuItem.id)
                    },
                )
                if (activeMenuItem.canDuplicate) {
                    PreferenceClickableItem(
                        title = stringResource(R.string.duplicate),
                        onClick = {
                            onDismissMenu()
                            onDuplicate(activeMenuItem.id)
                        },
                    )
                }
                PreferenceClickableItem(
                    title = stringResource(R.string.delete),
                    onClick = {
                        onDismissMenu()
                        onDelete(activeMenuItem.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileItemUiState,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_text_margin),
            ),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(
                    horizontal = dimensionResource(R.dimen.item_header_margin),
                    vertical = dimensionResource(R.dimen.item_padding_vertical),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
        ) {
            RadioButton(
                selected = profile.active,
                onClick = null,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
            ) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = profile.typeSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                profile.trafficSummary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                profile.expireSummary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                profile.usageProgress?.let {
                    LinearProgressIndicator(progress = { it.coerceIn(0f, 1f) })
                }
            }
            Text(
                text = profile.elapsedSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.item_text_margin)),
            )
            IconButton(onClick = onMore) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_more_vert),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfilesScreenPreview() {
    ClashTheme {
        ProfilesScreen(
            title = "Profiles",
            state = ProfilesUiState(
                profiles = listOf(
                    ProfileItemUiState(
                        id = "1",
                        name = "Example",
                        typeSummary = "URL",
                        trafficSummary = "1.00 GiB/10.00 GiB",
                        expireSummary = "2026-03-30 12:00:00",
                        elapsedSummary = "3 min ago",
                        active = true,
                        usageProgress = 0.1f,
                        canUpdate = true,
                        canDuplicate = true,
                    ),
                ),
                updateAllVisible = true,
            ),
            onBack = {},
            onCreate = {},
            onUpdateAll = {},
            onActivate = {},
            onOpenMenu = {},
            onDismissMenu = {},
            onUpdate = {},
            onEdit = {},
            onDuplicate = {},
            onDelete = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
