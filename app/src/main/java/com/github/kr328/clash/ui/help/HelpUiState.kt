package com.github.kr328.clash.ui.help

data class HelpUiState(
    val tips: String,
    val sections: List<HelpSectionUiState>,
)

data class HelpSectionUiState(
    val title: String,
    val items: List<HelpLinkItemUiState>,
)

data class HelpLinkItemUiState(
    val title: String,
    val summary: String,
    val url: String,
)
