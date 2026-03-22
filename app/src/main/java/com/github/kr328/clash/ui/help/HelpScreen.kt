package com.github.kr328.clash.ui.help

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
fun HelpScreen(
    title: String,
    state: HelpUiState,
    onBack: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
    ) { padding ->
        HelpContent(
            state = state,
            padding = padding,
            onOpenLink = onOpenLink,
        )
    }
}

@Composable
private fun HelpContent(
    state: HelpUiState,
    padding: PaddingValues,
    onOpenLink: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
    ) {
        PreferenceTipsItem(text = state.tips)

        state.sections.forEach { section ->
            PreferenceCategory(title = section.title)

            section.items.forEach { item ->
                PreferenceClickableItem(
                    title = item.title,
                    summary = item.summary,
                    onClick = { onOpenLink(item.url) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun HelpScreenPreview() {
    ClashTheme {
        HelpScreen(
            title = "Help",
            state = HelpUiState(
                tips = "Clash Meta for Android is a free app.",
                sections = listOf(
                    HelpSectionUiState(
                        title = "Document",
                        items = listOf(
                            HelpLinkItemUiState("Clash Wiki", "https://github.com/Dreamacro/clash/wiki", "https://github.com/Dreamacro/clash/wiki"),
                            HelpLinkItemUiState("Clash Meta Wiki", "https://wiki.metacubex.one", "https://wiki.metacubex.one"),
                        ),
                    ),
                    HelpSectionUiState(
                        title = "Sources",
                        items = listOf(
                            HelpLinkItemUiState("Clash Meta Core", "https://github.com/MetaCubeX/Clash.Meta", "https://github.com/MetaCubeX/Clash.Meta"),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onOpenLink = {},
        )
    }
}
