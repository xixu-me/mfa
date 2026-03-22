package com.github.kr328.clash.ui.profiles

data class ProfilesUiState(
    val profiles: List<ProfileItemUiState> = emptyList(),
    val updateAllVisible: Boolean = false,
    val updateAllRunning: Boolean = false,
    val activeMenuProfileId: String? = null,
)

data class ProfileItemUiState(
    val id: String,
    val name: String,
    val typeSummary: String,
    val trafficSummary: String?,
    val expireSummary: String?,
    val elapsedSummary: String,
    val active: Boolean,
    val usageProgress: Float?,
    val canUpdate: Boolean,
    val canDuplicate: Boolean,
)
