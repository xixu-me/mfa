package com.github.kr328.clash.ui.providers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.util.elapsedIntervalString

@Composable
fun ProvidersScreen(
    title: String,
    state: ProvidersUiState,
    onBack: () -> Unit,
    onUpdateAll: () -> Unit,
    onUpdate: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            IconButton(onClick = onUpdateAll) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_sync),
                    contentDescription = null,
                )
            }
        },
    ) { padding ->
        ProvidersContent(
            state = state,
            padding = padding,
            onUpdate = onUpdate,
        )
    }
}

@Composable
private fun ProvidersContent(
    state: ProvidersUiState,
    padding: PaddingValues,
    onUpdate: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = padding,
    ) {
        itemsIndexed(
            items = state.providers,
            key = { index, item -> "${item.name}-$index" },
        ) { index, item ->
            ProviderRow(
                state = item,
                currentTime = state.currentTime,
                onUpdate = { onUpdate(index) },
            )
        }
    }
}

@Composable
private fun ProviderRow(
    state: ProviderItemUiState,
    currentTime: Long,
    onUpdate: () -> Unit,
) {
    val context = LocalContext.current
    val elapsed = remember(currentTime, state.updatedAt) {
        (currentTime - state.updatedAt).coerceAtLeast(0L).elapsedIntervalString(context)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.item_header_margin),
                top = dimensionResource(R.dimen.item_padding_vertical),
                end = dimensionResource(R.dimen.item_tailing_margin),
                bottom = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        ) {
            Text(
                text = state.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = state.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.updateEnabled) {
            Text(
                text = elapsed,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(
                modifier = Modifier.size(dimensionResource(R.dimen.item_tailing_component_size)),
                contentAlignment = Alignment.Center,
            ) {
                if (state.updating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensionResource(R.dimen.item_tailing_component_size)),
                        strokeWidth = dimensionResource(R.dimen.divider_size) * 2,
                    )
                } else {
                    IconButton(onClick = onUpdate) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_swap_vert),
                            contentDescription = null,
                            modifier = Modifier.padding(2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProvidersScreenPreview() {
    ClashTheme {
        ProvidersScreen(
            title = "Providers",
            state = ProvidersUiState(
                providers = listOf(
                    ProviderItemUiState(
                        name = "Demo Provider",
                        summary = "Proxy(HTTP)",
                        updatedAt = System.currentTimeMillis() - 60_000,
                        updateEnabled = true,
                        updating = false,
                    ),
                    ProviderItemUiState(
                        name = "Inline Rules",
                        summary = "Rule(Inline)",
                        updatedAt = System.currentTimeMillis(),
                        updateEnabled = false,
                        updating = false,
                    ),
                ),
            ),
            onBack = {},
            onUpdateAll = {},
            onUpdate = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
