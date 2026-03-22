package com.github.kr328.clash.ui.accesscontrol

import com.github.kr328.clash.design.model.AppInfoSort

data class AccessControlUiState(
    val apps: List<AccessControlAppItemUiState> = emptyList(),
    val sort: AppInfoSort = AppInfoSort.Label,
    val reverse: Boolean = false,
    val showSystemApps: Boolean = false,
    val searchQuery: String = "",
    val searchOpen: Boolean = false,
)

data class AccessControlAppItemUiState(
    val packageName: String,
    val label: String,
    val selected: Boolean,
)
