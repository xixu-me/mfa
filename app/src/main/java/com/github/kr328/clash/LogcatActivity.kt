package com.github.kr328.clash

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.compat.startForegroundServiceCompat
import com.github.kr328.clash.common.log.Log
import com.github.kr328.clash.common.util.fileName
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.model.LogMessage
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.model.LogFile
import com.github.kr328.clash.design.util.format
import com.github.kr328.clash.log.LogcatFilter
import com.github.kr328.clash.log.LogcatReader
import com.github.kr328.clash.ui.logcat.LogMessageItemUiState
import com.github.kr328.clash.ui.logcat.LogcatScreen
import com.github.kr328.clash.ui.logcat.LogcatUiState
import com.github.kr328.clash.util.logsDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LogcatActivity : BaseActivity() {
    private var conn: ServiceConnection? = null
    private val uiState = MutableStateFlow(LogcatUiState(streaming = false))
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)

    override suspend fun main() {
        val fileName = intent?.fileName

        if (fileName != null) {
            val file = LogFile.parseFromFileName(fileName) ?: return showInvalid()

            return mainLocalFile(file)
        }

        return mainStreaming()
    }

    private suspend fun mainLocalFile(file: LogFile) {
        val messages = try {
            LogcatReader(this, file).readAll()
        } catch (e: Exception) {
            Log.e("Fail to read log file ${file.fileName}: ${e.message}")
            return showInvalid()
        }

        uiState.value = LogcatUiState(
            streaming = false,
            messages = messages.map(::toLogMessageItemUiState),
        )

        setComposeScreen()

        while (isActive) {
            when (events.receive()) {
                Event.ActivityStop,
                Event.ActivityStart,
                Event.ClashStart,
                Event.ClashStop,
                Event.ProfileChanged,
                Event.ProfileLoaded,
                Event.ProfileUpdateCompleted,
                Event.ProfileUpdateFailed,
                Event.ServiceRecreated -> Unit
            }
        }
    }

    private suspend fun mainStreaming() {
        uiState.value = LogcatUiState(streaming = true)
        setComposeScreen()

        startForegroundServiceCompat(LogcatService::class.intent)

        val logcat = bindLogcatService()
        val ticker = ticker(500)

        var initial = true

        while (isActive) {
            select<Unit> {
                events.onReceive {

                }
                if (activityStarted) {
                    ticker.onReceive {
                        val snapshot = logcat.snapshot(initial) ?: return@onReceive

                        uiState.value = LogcatUiState(
                            streaming = true,
                            messages = snapshot.messages.map(::toLogMessageItemUiState),
                        )

                        initial = false
                    }
                }
            }
        }
    }

    private suspend fun setComposeScreen() {
        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                LogcatScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onClose = {
                        stopService(LogcatService::class.intent)
                        startActivity(LogsActivity::class.intent)
                        finish()
                    },
                    onDelete = {
                        this@LogcatActivity.launch {
                            val fileName = intent?.fileName ?: return@launch
                            withContext(Dispatchers.IO) {
                                logsDir.resolve(fileName).delete()
                            }
                            finish()
                        }
                    },
                    onExport = {
                        this@LogcatActivity.launch {
                            exportCurrentLog()
                        }
                    },
                    onCopied = {
                        snackbarMessages.tryEmit(
                            ClashSnackbarMessage(
                                message = getString(R.string.copied),
                            ),
                        )
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }
    }

    override fun onDestroy() {
        conn?.apply(this::unbindService)

        super.onDestroy()
    }

    private suspend fun bindLogcatService(): LogcatService {
        return suspendCoroutine { ctx ->
            bindService(LogcatService::class.intent, object : ServiceConnection {
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
        LogcatFilter(OutputStreamWriter(contentResolver.openOutputStream(uri)), this).use {
            uiState.value = uiState.value.copy(
                exportLabel = file.fileName,
                exportProgress = if (messages.isEmpty()) null else 0f,
            )

            withContext(Dispatchers.IO) {
                it.writeHeader(file.date)

                messages.forEachIndexed { idx, msg ->
                    it.writeMessage(msg)
                    uiState.value = uiState.value.copy(
                        exportLabel = file.fileName,
                        exportProgress = if (messages.isEmpty()) {
                            null
                        } else {
                            (idx + 1).toFloat() / messages.size.toFloat()
                        },
                    )
                }
            }
        }
    }

    private suspend fun exportCurrentLog() {
        val fileName = intent?.fileName ?: return
        val file = LogFile.parseFromFileName(fileName) ?: return showInvalid()
        val messages = withContext(Dispatchers.IO) {
            LogcatReader(this@LogcatActivity, file).readAll()
        }
        val output = startActivityForResult(
            ActivityResultContracts.CreateDocument("text/plain"),
            file.fileName,
        )

        if (output != null) {
            try {
                withContext(Dispatchers.IO) {
                    writeLogTo(messages, file, output)
                }

                snackbarMessages.tryEmit(
                    ClashSnackbarMessage(
                        message = getString(R.string.file_exported),
                        duration = SnackbarDuration.Long,
                    ),
                )
            } catch (e: Exception) {
                snackbarMessages.tryEmit(
                    ClashSnackbarMessage(
                        message = e.message ?: "Unknown",
                        duration = SnackbarDuration.Long,
                    ),
                )
            } finally {
                uiState.value = uiState.value.copy(
                    exportLabel = null,
                    exportProgress = null,
                )
            }
        }
    }

    private fun showInvalid() {
        Toast.makeText(this, R.string.invalid_log_file, Toast.LENGTH_LONG).show()
    }

    private fun toLogMessageItemUiState(message: LogMessage): LogMessageItemUiState {
        return LogMessageItemUiState(
            level = message.level.name.uppercase(),
            time = message.time.format(this, includeDate = false, includeTime = true),
            message = message.message,
        )
    }
}
