package com.github.kr328.clash.ui.logs

data class LogsUiState(
    val files: List<LogFileItemUiState> = emptyList(),
)

data class LogFileItemUiState(
    val fileName: String,
    val summary: String,
)
