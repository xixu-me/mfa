package com.github.kr328.clash.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme

@Composable
fun SettingsScreen(
    title: String,
    onBack: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenOverride: () -> Unit,
    onOpenMetaFeature: () -> Unit,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsRow(
                title = stringResource(id = R.string.app),
                iconRes = R.drawable.ic_baseline_settings,
                onClick = onOpenApp,
            )
            SettingsRow(
                title = stringResource(id = R.string.network),
                iconRes = R.drawable.ic_baseline_dns,
                onClick = onOpenNetwork,
            )
            SettingsRow(
                title = stringResource(id = R.string.override),
                iconRes = R.drawable.ic_baseline_extension,
                onClick = onOpenOverride,
            )
            SettingsRow(
                title = stringResource(id = R.string.meta_features),
                iconRes = R.drawable.ic_baseline_meta,
                onClick = onOpenMetaFeature,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    iconRes: Int,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimensionResource(R.dimen.item_header_margin),
                vertical = dimensionResource(R.dimen.item_padding_vertical),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_midden_margin)),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    ClashTheme {
        SettingsScreen(
            title = "Settings",
            onBack = {},
            onOpenApp = {},
            onOpenNetwork = {},
            onOpenOverride = {},
            onOpenMetaFeature = {},
        )
    }
}
