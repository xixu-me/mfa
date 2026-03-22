package com.github.kr328.clash.ui.logcat

data class LogcatUiState(
    val streaming: Boolean,
    val messages: List<LogMessageItemUiState> = emptyList(),
    val exportLabel: String? = null,
    val exportProgress: Float? = null,
)

data class LogMessageItemUiState(
    val level: String,
    val time: String,
    val message: String,
)
