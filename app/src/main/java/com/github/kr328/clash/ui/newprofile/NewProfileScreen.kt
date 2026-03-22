package com.github.kr328.clash.ui.newprofile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme

@Composable
fun NewProfileScreen(
    title: String,
    state: NewProfileUiState,
    onBack: () -> Unit,
    onCreate: (String) -> Unit,
    onDetail: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(state.providers, key = NewProfileProviderUiState::id) { provider ->
                ProviderRow(
                    provider = provider,
                    onClick = { onCreate(provider.id) },
                    onLongClick = if (provider.showDetail) {
                        { onDetail(provider.id) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProviderRow(
    provider: NewProfileProviderUiState,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        Icon(
            painter = painterResource(provider.iconRes),
            contentDescription = null,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        ) {
            Text(
                text = provider.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = provider.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun NewProfileScreenPreview() {
    ClashTheme {
        NewProfileScreen(
            title = "New Profile",
            state = NewProfileUiState(
                providers = listOf(
                    NewProfileProviderUiState("file", "File", "Import from File", R.drawable.ic_baseline_attach_file, false),
                    NewProfileProviderUiState("url", "URL", "Import from URL", R.drawable.ic_baseline_cloud_download, false),
                ),
            ),
            onBack = {},
            onCreate = {},
            onDetail = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
