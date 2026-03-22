package com.github.kr328.clash.ui.logcat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme

@Composable
fun LogcatScreen(
    title: String,
    state: LogcatUiState,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onCopied: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val listState = rememberLazyListState()
    val shouldStickToBottom by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            if (total == 0) {
                true
            } else {
                val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisible >= total - 3
            }
        }
    }

    LaunchedEffect(state.streaming, state.messages.size) {
        if (state.streaming && state.messages.isNotEmpty() && shouldStickToBottom) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    if (state.exportLabel != null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(text = stringResource(R.string.export)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
                ) {
                    Text(text = state.exportLabel)
                    if (state.exportProgress == null) {
                        androidx.compose.material3.CircularProgressIndicator()
                    } else {
                        LinearProgressIndicator(progress = { state.exportProgress.coerceIn(0f, 1f) })
                    }
                }
            },
        )
    }

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            if (state.streaming) {
                IconButton(onClick = onClose) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_stop),
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    enabled = state.exportLabel == null,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_delete),
                        contentDescription = null,
                    )
                }
                IconButton(
                    onClick = onExport,
                    enabled = state.exportLabel == null,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_publish),
                        contentDescription = null,
                    )
                }
            }
        },
    ) { padding ->
        LogcatContent(
            state = state,
            padding = padding,
            listState = listState,
            onCopied = onCopied,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LogcatContent(
    state: LogcatUiState,
    padding: PaddingValues,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onCopied: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = padding,
    ) {
        itemsIndexed(
            items = state.messages,
            key = { index, item -> "${item.time}-${item.message.hashCode()}-$index" },
        ) { _, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            clipboardManager.setText(AnnotatedString(item.message))
                            onCopied()
                        },
                    )
                    .padding(
                        horizontal = dimensionResource(R.dimen.logcat_padding_horizontal),
                        vertical = dimensionResource(R.dimen.logcat_padding_vertical),
                    ),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = item.level,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LogcatScreenPreview() {
    ClashTheme {
        LogcatScreen(
            title = "Logcat",
            state = LogcatUiState(
                streaming = false,
                messages = listOf(
                    LogMessageItemUiState(
                        level = "INFO",
                        time = "11:11:11.111",
                        message = "Preview message",
                    ),
                ),
            ),
            onBack = {},
            onClose = {},
            onDelete = {},
            onExport = {},
            onCopied = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
