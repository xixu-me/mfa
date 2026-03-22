package com.github.kr328.clash.ui.main

import androidx.compose.material3.SnackbarDuration

data class MainUiState(
    val clashRunning: Boolean = false,
    val forwarded: String = "0 Bytes",
    val mode: String = "",
    val profileName: String? = null,
    val hasProviders: Boolean = false,
    val aboutVersionName: String? = null,
)

data class MainSnackbarEvent(
    val message: String,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val actionLabel: String? = null,
    val action: MainSnackbarAction = MainSnackbarAction.None,
)

enum class MainSnackbarAction {
    None,
    OpenProfiles,
}
