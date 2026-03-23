package com.github.kr328.clash.ui.logs

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.LogcatService
import com.github.kr328.clash.common.compat.startForegroundServiceCompat
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.util.fileName
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.format
import com.github.kr328.clash.log.LogcatFilter
import com.github.kr328.clash.log.LogcatReader
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.ui.logcat.LogMessageItemUiState
import com.github.kr328.clash.ui.logcat.LogcatScreen
import com.github.kr328.clash.ui.logcat.LogcatUiState
import com.github.kr328.clash.util.logsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LogsViewModel(application: Application) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshFiles()
    }

    fun onDeleteAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                app.logsDir.deleteRecursively()
            }
            refreshFiles()
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            _uiState.value = LogsUiState(
                files = withContext(Dispatchers.IO) {
                    loadFiles().map {
                        LogFileItemUiState(
                            fileName = it.fileName,
                            summary = it.date.format(app),
                        )
                    }
                },
            )
        }
    }

    private fun loadFiles(): List<LogFile> {
        val list = app.cacheDir.resolve("logs").listFiles()?.toList() ?: emptyList()
        return list.mapNotNull { LogFile.parseFromFileName(it.name) }
    }
}

@Composable
fun LogsRoute(
    navController: NavController,
    viewModel: LogsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LogsScreen(
        title = androidx.compose.ui.platform.LocalContext.current.getString(R.string.logs),
        state = state,
        onBack = { navController.popBackStack() },
        onStartLogcat = { navController.navigate(AppRoute.LiveLogcat.route) },
        onDeleteAll = viewModel::onDeleteAll,
        onOpenFile = { navController.navigate(AppRoute.LogFile.createRoute(it)) },
        snackbarHostState = remember { SnackbarHostState() },
    )
}

class LogcatViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(LogcatUiState(streaming = savedStateHandle.get<String>(AppRoute.FILE_NAME) == null))
    val uiState = _uiState.asStateFlow()
    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()
    private val fileName = savedStateHandle.get<String>(AppRoute.FILE_NAME)
    private var conn: ServiceConnection? = null

    init {
        if (fileName == null) {
            startStreaming()
        } else {
            loadLocalFile(fileName)
        }
    }

    fun onClose() {
        app.stopService(com.github.kr328.clash.LogcatService::class.java.let { android.content.Intent(app, it) })
    }

    fun onDelete() {
        val currentFileName = fileName ?: return
        viewModelScope.launch(Dispatchers.IO) {
            app.logsDir.resolve(currentFileName).delete()
        }
    }

    fun exportFileName(): String {
        return fileName ?: "clash.log"
    }

    fun onCopied() {
        _snackbarMessages.tryEmit(
            ClashSnackbarMessage(
                message = app.getString(R.string.copied),
            ),
        )
    }

    fun onExport(output: Uri) {
        val currentFileName = fileName ?: return
        val file = LogFile.parseFromFileName(currentFileName) ?: return

        viewModelScope.launch {
            try {
                val messages = withContext(Dispatchers.IO) {
                    LogcatReader(app, file).readAll()
                }
                withContext(Dispatchers.IO) {
                    writeLogTo(messages, file, output)
                }
                _snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = app.getString(R.string.file_exported),
                        duration = SnackbarDuration.Long,
                    ),
                )
            } catch (e: Exception) {
                _snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = e.message ?: "Unknown",
                        duration = SnackbarDuration.Long,
                    ),
                )
            } finally {
                _uiState.value = _uiState.value.copy(exportLabel = null, exportProgress = null)
            }
        }
    }

    override fun onCleared() {
        conn?.let { app.unbindService(it) }
        super.onCleared()
    }

    private fun loadLocalFile(fileName: String) {
        viewModelScope.launch {
            val file = LogFile.parseFromFileName(fileName)
            if (file == null) {
                showInvalid()
                return@launch
            }

            val messages = try {
                LogcatReader(app, file).readAll()
            } catch (e: Exception) {
                Log.e("Fail to read log file ${file.fileName}: ${e.message}")
                showInvalid()
                return@launch
            }

            _uiState.value = LogcatUiState(
                streaming = false,
                messages = messages.map(::toLogMessageItemUiState),
            )
        }
    }

    private fun startStreaming() {
        viewModelScope.launch {
            _uiState.value = LogcatUiState(streaming = true)
            app.startForegroundServiceCompat(com.github.kr328.clash.LogcatService::class.java.let { android.content.Intent(app, it) })
            val logcat = bindLogcatService()
            var initial = true
            while (isActive) {
                delay(500)
                val snapshot = logcat.snapshot(initial) ?: continue
                _uiState.value = LogcatUiState(
                    streaming = true,
                    messages = snapshot.messages.map(::toLogMessageItemUiState),
                )
                initial = false
            }
        }
    }

    private suspend fun bindLogcatService(): LogcatService {
        return suspendCoroutine { ctx ->
            val serviceIntent = android.content.Intent(app, LogcatService::class.java)
            app.bindService(serviceIntent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val srv = service!!.queryLocalInterface("") as LogcatService
                    ctx.resume(srv)
                    conn = this
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    conn = null
                }
            }, Context.BIND_AUTO_CREATE)
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun writeLogTo(messages: List<LogMessage>, file: LogFile, uri: Uri) {
        LogcatFilter(OutputStreamWriter(app.contentResolver.openOutputStream(uri)), app).use {
            _uiState.value = _uiState.value.copy(
                exportLabel = file.fileName,
                exportProgress = if (messages.isEmpty()) null else 0f,
            )

            withContext(Dispatchers.IO) {
                it.writeHeader(file.date)
                messages.forEachIndexed { idx, msg ->
                    it.writeMessage(msg)
                    _uiState.value = _uiState.value.copy(
                        exportLabel = file.fileName,
                        exportProgress = if (messages.isEmpty()) null else (idx + 1).toFloat() / messages.size.toFloat(),
                    )
                }
            }
        }
    }

    private fun showInvalid() {
        _snackbarMessages.tryEmit(
            ClashSnackbarMessage(
                message = app.getString(R.string.invalid_log_file),
                duration = SnackbarDuration.Long,
            ),
        )
    }

    private fun toLogMessageItemUiState(message: LogMessage): LogMessageItemUiState {
        return LogMessageItemUiState(
            level = message.level.name.uppercase(),
            time = message.time.format(app, includeDate = false, includeTime = true),
            message = message.message,
        )
    }
}

@Composable
fun LogcatRoute(
    navController: NavController,
    viewModel: LogcatViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { output ->
        if (output != null) {
            viewModel.onExport(output)
        }
    }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    LogcatScreen(
        title = androidx.compose.ui.platform.LocalContext.current.getString(R.string.logcat),
        state = state,
        onBack = { navController.popBackStack() },
        onClose = {
            viewModel.onClose()
            navController.navigate(AppRoute.Logs.route) {
                popUpTo(AppRoute.Logs.route) {
                    inclusive = true
                }
            }
        },
        onDelete = {
            viewModel.onDelete()
            navController.popBackStack()
        },
        onExport = {
            exportLauncher.launch(viewModel.exportFileName())
        },
        onCopied = viewModel::onCopied,
        snackbarHostState = snackbarHostState,
    )
}
