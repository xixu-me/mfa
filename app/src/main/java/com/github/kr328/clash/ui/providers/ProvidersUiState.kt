package com.github.kr328.clash.ui.providers

data class ProvidersUiState(
    val providers: List<ProviderItemUiState> = emptyList(),
    val currentTime: Long = System.currentTimeMillis(),
)

data class ProviderItemUiState(
    val name: String,
    val summary: String,
    val updatedAt: Long,
    val updateEnabled: Boolean,
    val updating: Boolean,
)
