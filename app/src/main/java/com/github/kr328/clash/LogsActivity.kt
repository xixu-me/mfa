package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setFileName
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.format
import com.github.kr328.clash.ui.logs.LogFileItemUiState
import com.github.kr328.clash.ui.logs.LogsScreen
import com.github.kr328.clash.ui.logs.LogsUiState
import com.github.kr328.clash.util.logsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext

class LogsActivity : BaseActivity() {
    private val uiState = MutableStateFlow(LogsUiState())

    override suspend fun main() {
        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashTheme {
                LogsScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onStartLogcat = {
                        startActivity(LogcatActivity::class.intent)
                        finish()
                    },
                    onDeleteAll = {
                        this@LogsActivity.launch {
                            withContext(Dispatchers.IO) {
                                deleteAllLogs()
                            }

                            uiState.value = LogsUiState()
                            refreshFiles()
                        }
                    },
                    onOpenFile = { fileName ->
                        startActivity(LogcatActivity::class.intent.setFileName(fileName))
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart -> {
                            refreshFiles()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private suspend fun refreshFiles() {
        uiState.value = LogsUiState(
            files = withContext(Dispatchers.IO) {
                loadFiles().map {
                    LogFileItemUiState(
                        fileName = it.fileName,
                        summary = it.date.format(this@LogsActivity),
                    )
                }
            },
        )
    }

    private fun loadFiles(): List<LogFile> {
        val list = cacheDir.resolve("logs").listFiles()?.toList() ?: emptyList()

        return list.mapNotNull { LogFile.parseFromFileName(it.name) }
    }

    private fun deleteAllLogs() {
        logsDir.deleteRecursively()
    }
}
