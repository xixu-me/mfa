package com.github.kr328.clash.ui.properties

import com.github.kr328.clash.service.model.Profile

data class PropertiesUiState(
    val profile: Profile? = null,
    val hasUnsavedChanges: Boolean = false,
    val showDiscardChangesDialog: Boolean = false,
    val isProcessing: Boolean = false,
    val progressMessage: String? = null,
    val progress: Float? = null,
)
