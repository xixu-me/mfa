package com.github.kr328.clash.design.compose

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.Flow

data class ClashSnackbarMessage(
    val message: String,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
fun ClashSnackbarEffect(
    messages: Flow<ClashSnackbarMessage>,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(messages, snackbarHostState) {
        messages.collect { message ->
            val result = snackbarHostState.showSnackbar(
                message = message.message,
                actionLabel = message.actionLabel,
                duration = message.duration,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                message.onAction?.invoke()
            }
        }
    }
}
