package com.github.kr328.clash.ui.settings.meta

import com.github.kr328.clash.core.model.ConfigurationOverride

data class MetaFeatureSettingsUiState(
    val configuration: ConfigurationOverride,
    val showResetConfirm: Boolean = false,
)
