package com.github.kr328.clash.ui.apkbroken

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceCategory
import com.github.kr328.clash.design.compose.PreferenceClickableItem
import com.github.kr328.clash.design.compose.PreferenceTipsItem

@Composable
fun ApkBrokenScreen(
    title: String,
    state: ApkBrokenUiState,
    onBack: () -> Unit,
    onOpenReleasePage: (String) -> Unit,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
    ) { padding ->
        ApkBrokenContent(
            state = state,
            padding = padding,
            onOpenReleasePage = onOpenReleasePage,
        )
    }
}

@Composable
private fun ApkBrokenContent(
    state: ApkBrokenUiState,
    padding: PaddingValues,
    onOpenReleasePage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
    ) {
        PreferenceTipsItem(text = state.tips)
        PreferenceCategory(title = state.sectionTitle)
        PreferenceClickableItem(
            title = state.releaseTitle,
            summary = state.releaseUrl,
            onClick = { onOpenReleasePage(state.releaseUrl) },
        )
    }
}

@Preview
@Composable
private fun ApkBrokenScreenPreview() {
    ClashTheme {
        ApkBrokenScreen(
            title = "Application Broken",
            state = ApkBrokenUiState(
                tips = "App lacks the necessary runtime components.",
                sectionTitle = "Reinstall",
                releaseTitle = "Github Releases",
                releaseUrl = "https://github.com/MetaCubeX/ClashMetaForAndroid",
            ),
            onBack = {},
            onOpenReleasePage = {},
        )
    }
}
