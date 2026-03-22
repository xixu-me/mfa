package com.github.kr328.clash.ui.newprofile

data class NewProfileUiState(
    val providers: List<NewProfileProviderUiState> = emptyList(),
)

data class NewProfileProviderUiState(
    val id: String,
    val title: String,
    val summary: String,
    val iconRes: Int,
    val showDetail: Boolean,
)
