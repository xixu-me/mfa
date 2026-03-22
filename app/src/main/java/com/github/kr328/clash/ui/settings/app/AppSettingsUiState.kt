package com.github.kr328.clash.ui.settings.app

import com.github.kr328.clash.design.model.DarkMode

data class AppSettingsUiState(
    val autoRestart: Boolean = false,
    val darkMode: DarkMode = DarkMode.Auto,
    val hideAppIcon: Boolean = false,
    val hideFromRecents: Boolean = false,
    val dynamicNotification: Boolean = true,
    val running: Boolean = false,
)
