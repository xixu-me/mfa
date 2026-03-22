package com.github.kr328.clash.ui.settings.override

import com.github.kr328.clash.core.model.ConfigurationOverride

data class OverrideSettingsUiState(
    val configuration: ConfigurationOverride,
    val showResetConfirm: Boolean = false,
)
