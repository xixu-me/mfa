package com.github.kr328.clash.design.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.text.HtmlCompat
import com.github.kr328.clash.design.R

data class PreferenceOption<T>(
    val value: T,
    val title: String,
)

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.item_header_margin),
                top = dimensionResource(R.dimen.item_text_margin),
                end = dimensionResource(R.dimen.item_header_margin),
                bottom = dimensionResource(R.dimen.item_text_margin),
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun PreferenceGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    PreferenceCategory(title = title)
    Column(content = content)
}

@Composable
fun PreferenceTipsItem(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_text_margin),
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.dialog_padding)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_outline_info),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(R.dimen.tips_icon_size)),
            )
            Text(
                text = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun PreferenceClickableItem(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
    onClick: () -> Unit,
) {
    PreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        iconRes = iconRes,
        onClick = onClick,
    )
}

@Composable
fun PreferenceSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
) {
    PreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        iconRes = iconRes,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else null,
            )
        },
    )
}

@Composable
fun <T> PreferenceSelectableItem(
    title: String,
    value: T,
    options: List<PreferenceOption<T>>,
    onSelected: (T) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
) {
    var showDialog by remember { mutableStateOf(false) }

    PreferenceRow(
        title = title,
        summary = summary ?: options.firstOrNull { it.value == value }?.title,
        enabled = enabled,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelected(option.value)
                                    showDialog = false
                                }
                                .padding(vertical = dimensionResource(R.dimen.item_text_margin)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.item_midden_margin),
                            ),
                        ) {
                            RadioButton(
                                selected = option.value == value,
                                onClick = {
                                    onSelected(option.value)
                                    showDialog = false
                                },
                            )
                            Text(text = option.title)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun PreferenceTextFieldItem(
    title: String,
    value: String?,
    placeholder: String,
    onValueChange: (String?) -> Unit,
    empty: String? = null,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingValue by remember(value, showDialog) { mutableStateOf(value ?: "") }

    PreferenceRow(
        title = title,
        summary = nullableTextSummary(value, placeholder, empty),
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
                    value = editingValue,
                    onValueChange = { editingValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(text = title) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(editingValue)
                        showDialog = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onValueChange(null)
                            showDialog = false
                        },
                    ) {
                        Text(text = stringResource(id = R.string.reset))
                    }
                    TextButton(onClick = { showDialog = false }) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                }
            },
        )
    }
}

@Composable
fun PreferenceEditableTextListItem(
    title: String,
    values: List<String>?,
    placeholder: String,
    onValueChange: (List<String>?) -> Unit,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
    empty: String = "Empty",
    formatElements: (Int) -> String,
) {
    var showDialog by remember { mutableStateOf(false) }

    PreferenceRow(
        title = title,
        summary = collectionSummary(values?.size, placeholder, empty, formatElements),
        enabled = enabled,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        EditableStringListDialog(
            title = title,
            initialValues = values,
            onDismiss = { showDialog = false },
            onApply = {
                onValueChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
fun PreferenceEditableTextMapItem(
    title: String,
    values: Map<String, String>?,
    placeholder: String,
    onValueChange: (Map<String, String>?) -> Unit,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
    empty: String = "Empty",
    formatElements: (Int) -> String,
) {
    var showDialog by remember { mutableStateOf(false) }

    PreferenceRow(
        title = title,
        summary = collectionSummary(values?.size, placeholder, empty, formatElements),
        enabled = enabled,
        iconRes = iconRes,
        onClick = { showDialog = true },
    )

    if (showDialog) {
        EditableStringMapDialog(
            title = title,
            initialValues = values,
            onDismiss = { showDialog = false },
            onApply = {
                onValueChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    summary: String?,
    enabled: Boolean,
    @DrawableRes iconRes: Int?,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
        }

        trailing?.invoke()
    }
}

@Composable
private fun EditableStringListDialog(
    title: String,
    initialValues: List<String>?,
    onDismiss: () -> Unit,
    onApply: (List<String>?) -> Unit,
) {
    val values = remember { mutableStateListOf<String>() }
    var newValue by remember { mutableStateOf("") }

    LaunchedEffect(initialValues) {
        values.clear()
        values.addAll(initialValues.orEmpty())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.dialog_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(text = stringResource(id = R.string._new)) },
                    )
                    TextButton(
                        onClick = {
                            if (newValue.isNotBlank()) {
                                values.add(newValue.trim())
                                newValue = ""
                            }
                        },
                    ) {
                        Text(text = stringResource(id = R.string._new))
                    }
                }
                if (values.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .heightIn(min = dimensionResource(R.dimen.dialog_menu_min_width)),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
                    ) {
                        itemsIndexed(values, key = { index, item -> "$index-$item" }) { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    dimensionResource(R.dimen.item_text_margin),
                                ),
                            ) {
                                Text(
                                    text = item,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { values.removeAt(index) }) {
                                    Text(text = stringResource(id = R.string.delete))
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { onApply(null) }) {
                        Text(text = stringResource(id = R.string.reset))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    TextButton(onClick = { onApply(values.toList()) }) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
private fun EditableStringMapDialog(
    title: String,
    initialValues: Map<String, String>?,
    onDismiss: () -> Unit,
    onApply: (Map<String, String>?) -> Unit,
) {
    val entries = remember { mutableStateListOf<Pair<String, String>>() }
    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }

    LaunchedEffect(initialValues) {
        entries.clear()
        entries.addAll(initialValues.orEmpty().entries.map { it.key to it.value })
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dimensionResource(R.dimen.dialog_padding)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge)
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
                ) {
                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(id = R.string.name)) },
                    )
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(text = stringResource(id = R.string.value)) },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                val trimmedKey = newKey.trim()
                                val trimmedValue = newValue.trim()
                                if (trimmedKey.isNotEmpty() && trimmedValue.isNotEmpty()) {
                                    entries.removeAll { it.first == trimmedKey }
                                    entries.add(trimmedKey to trimmedValue)
                                    newKey = ""
                                    newValue = ""
                                }
                            },
                        ) {
                            Text(text = stringResource(id = R.string._new))
                        }
                    }
                }
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .heightIn(min = dimensionResource(R.dimen.dialog_menu_min_width)),
                            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
                    ) {
                        itemsIndexed(entries, key = { index, item -> "$index-${item.first}" }) { index, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(20.dp),
                                    )
                                    .padding(dimensionResource(R.dimen.item_padding_vertical)),
                            ) {
                                Text(
                                    text = item.first,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = item.second,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { entries.removeAt(index) }) {
                                        Text(text = stringResource(id = R.string.delete))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = { onApply(null) }) {
                        Text(text = stringResource(id = R.string.reset))
                    }
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.cancel))
                    }
                    TextButton(onClick = { onApply(entries.toMap()) }) {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}
