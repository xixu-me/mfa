package com.github.kr328.clash.ui.properties

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceClickableItem
import com.github.kr328.clash.design.compose.PreferenceTipsItem
import com.github.kr328.clash.service.model.Profile
import java.util.UUID
import java.util.concurrent.TimeUnit

@Composable
fun PropertiesScreen(
    title: String,
    state: PropertiesUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onOpenFiles: () -> Unit,
    onNameChanged: (String) -> Unit,
    onSourceChanged: (String) -> Unit,
    onIntervalChanged: (Long) -> Unit,
    onDiscardDismissed: () -> Unit,
    onDiscardConfirmed: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val profile = state.profile ?: return

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            if (state.isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.item_tailing_margin)),
                    strokeWidth = dimensionResource(R.dimen.divider_size) * 2,
                )
            } else {
                IconButton(onClick = onSave) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_save),
                        contentDescription = stringResource(R.string.save),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceTipsItem(text = stringResource(R.string.tips_properties))

            EditablePreferenceItem(
                title = stringResource(R.string.name),
                summary = profile.name,
                placeholder = stringResource(R.string.profile_name),
                iconRes = R.drawable.ic_outline_label,
                enabled = !state.isProcessing,
                validate = { it.isNotBlank() },
                errorText = stringResource(R.string.should_not_be_blank),
                onConfirmed = onNameChanged,
            )

            EditablePreferenceItem(
                title = stringResource(R.string.url),
                summary = profile.source,
                placeholder = stringResource(R.string.accept_http_content),
                iconRes = R.drawable.ic_outline_inbox,
                enabled = !state.isProcessing && profile.type != Profile.Type.File && profile.type != Profile.Type.External,
                validate = { value ->
                    value.startsWith("http://") || value.startsWith("https://")
                },
                errorText = stringResource(R.string.accept_http_content),
                onConfirmed = onSourceChanged,
            )

            EditablePreferenceItem(
                title = stringResource(R.string.auto_update),
                summary = intervalSummary(profile.interval),
                placeholder = stringResource(R.string.auto_update_minutes),
                iconRes = R.drawable.ic_outline_update,
                enabled = !state.isProcessing && profile.type != Profile.Type.File,
                keyboardType = KeyboardType.Number,
                validate = { value ->
                    value.isBlank() || value.toLongOrNull()?.let { it == 0L || it >= 15L } == true
                },
                errorText = stringResource(R.string.at_least_15_minutes),
                onConfirmed = { value ->
                    val minutes = value.toLongOrNull() ?: 0L
                    onIntervalChanged(TimeUnit.MINUTES.toMillis(minutes))
                },
            )

            PreferenceClickableItem(
                title = stringResource(R.string.browse_files),
                summary = stringResource(R.string.browse_configuration_providers),
                enabled = !state.isProcessing,
                iconRes = R.drawable.ic_outline_folder,
                onClick = onOpenFiles,
            )
        }
    }

    if (state.showDiscardChangesDialog) {
        AlertDialog(
            onDismissRequest = onDiscardDismissed,
            title = { Text(text = stringResource(R.string.exit_without_save)) },
            text = { Text(text = stringResource(R.string.exit_without_save_warning)) },
            confirmButton = {
                TextButton(onClick = onDiscardConfirmed) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDiscardDismissed) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }

    if (state.isProcessing) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(text = stringResource(R.string.save)) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
                ) {
                    Text(
                        text = state.progressMessage ?: stringResource(R.string.initializing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (state.progress == null) {
                        CircularProgressIndicator()
                    } else {
                        LinearProgressIndicator(progress = { state.progress.coerceIn(0f, 1f) })
                    }
                }
            },
        )
    }
}

@Composable
private fun EditablePreferenceItem(
    title: String,
    summary: String,
    placeholder: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    validate: (String) -> Boolean,
    errorText: String,
    onConfirmed: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    var value by remember(showDialog, summary) { mutableStateOf(summary) }
    val valid = validate(value)

    PreferenceClickableItem(
        title = title,
        summary = summary,
        enabled = enabled,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = title) },
                    placeholder = { Text(text = placeholder) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    isError = !valid,
                    supportingText = {
                        if (!valid) {
                            Text(text = errorText)
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmed(value.trim())
                        showDialog = false
                    },
                    enabled = valid,
                ) {
                    Text(text = stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun intervalSummary(interval: Long): String {
    return if (interval <= 0L) {
        stringResource(R.string.disabled)
    } else {
        stringResource(
            R.string.format_minutes,
            TimeUnit.MILLISECONDS.toMinutes(interval),
        )
    }
}

@Preview
@Composable
private fun PropertiesScreenPreview() {
    ClashTheme {
        PropertiesScreen(
            title = "Properties",
            state = PropertiesUiState(
                profile = Profile(
                    uuid = UUID.randomUUID(),
                    name = "Example",
                    type = Profile.Type.Url,
                    source = "https://example.com/config.yaml",
                    active = true,
                    interval = TimeUnit.MINUTES.toMillis(60),
                    upload = 0,
                    download = 0,
                    total = 0,
                    expire = 0,
                    updatedAt = 0,
                    imported = true,
                    pending = false,
                ),
                isProcessing = true,
                progressMessage = "Verifying",
                progress = 0.5f,
            ),
            onBack = {},
            onSave = {},
            onOpenFiles = {},
            onNameChanged = {},
            onSourceChanged = {},
            onIntervalChanged = {},
            onDiscardDismissed = {},
            onDiscardConfirmed = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
