@file:Suppress("BlockingMethodInNonBlockingContext")

package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.grantPermissions
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.common.util.PatternFileName
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toBytesString
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.files.FileItemUiState
import com.github.kr328.clash.ui.files.FilesScreen
import com.github.kr328.clash.ui.files.FilesUiState
import com.github.kr328.clash.ui.files.PendingFileNameDialog
import com.github.kr328.clash.ui.files.PendingFileNameMode
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.Stack
import java.util.concurrent.TimeUnit

class FilesActivity : BaseActivity() {
    private val uiState = MutableStateFlow(FilesUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    private val stack = Stack<String>()
    private lateinit var client: FilesClient
    private lateinit var root: String
    private var currentFiles: List<File> = emptyList()
    private var configurationEditable = false
    private var pendingImportUri: Uri? = null
    private var pendingRenameFileId: String? = null

    override suspend fun main() {
        val uuid = intent.uuid ?: return finish()
        val profile = withProfile { queryByUUID(uuid) } ?: return finish()
        root = uuid.toString()
        configurationEditable = profile.type != Profile.Type.Url
        client = FilesClient(this)

        refreshFiles()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                FilesScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = ::handleBack,
                    onOpenFile = ::openFile,
                    onOpenMenu = { fileId ->
                        uiState.value = uiState.value.copy(activeMenuFileId = fileId)
                    },
                    onDismissMenu = {
                        uiState.value = uiState.value.copy(activeMenuFileId = null)
                    },
                    onNewFile = {
                        launch { startImportNewFile() }
                    },
                    onRenameFile = { fileId ->
                        val file = currentFiles.firstOrNull { it.id == fileId } ?: return@FilesScreen
                        pendingRenameFileId = fileId
                        uiState.value = uiState.value.copy(
                            pendingFileNameDialog = PendingFileNameDialog(
                                title = getString(com.github.kr328.clash.design.R.string.file_name),
                                initialValue = file.name,
                                mode = PendingFileNameMode.Rename,
                            ),
                        )
                    },
                    onImportToFile = { fileId ->
                        launch { importToExistingFile(fileId) }
                    },
                    onExportFile = { fileId ->
                        launch { exportFile(fileId) }
                    },
                    onDeleteFile = { fileId ->
                        launch { deleteRemoteFile(fileId) }
                    },
                    onDismissFileNameDialog = {
                        pendingImportUri = null
                        pendingRenameFileId = null
                        uiState.value = uiState.value.copy(pendingFileNameDialog = null)
                    },
                    onConfirmFileNameDialog = { fileName ->
                        launch { confirmFileNameDialog(fileName) }
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ActivityStop -> refreshFiles()
                        else -> Unit
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        refreshFiles()
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        handleBack()
    }

    private fun handleBack() {
        if (stack.empty()) {
            finish()
        } else {
            stack.pop()
            launch { refreshFiles() }
        }
    }

    private suspend fun refreshFiles() {
        val documentId = stack.lastOrNull() ?: root
        val files = if (stack.empty()) {
            val list = client.list(documentId)
            val config = list.firstOrNull { it.id.endsWith("config.yaml") }
            if (config == null || config.size > 0) list else listOf(config)
        } else {
            client.list(documentId)
        }

        currentFiles = files
        val currentInBaseDir = stack.empty()
        uiState.value = uiState.value.copy(
            files = files.map { file ->
                FileItemUiState(
                    id = file.id,
                    name = file.name,
                    sizeSummary = if (file.isDirectory) null else file.size.toBytesString(),
                    elapsedSummary = if (file.isDirectory) null else (System.currentTimeMillis() - file.lastModified).elapsedIntervalString(this),
                    isDirectory = file.isDirectory,
                    canImport = !file.isDirectory && (!currentInBaseDir || configurationEditable),
                    canExport = !file.isDirectory && file.size > 0,
                    canRename = !currentInBaseDir,
                    canDelete = !currentInBaseDir,
                )
            },
            currentInBaseDir = currentInBaseDir,
            configurationEditable = configurationEditable,
            activeMenuFileId = null,
        )
    }

    private fun openFile(fileId: String) {
        val file = currentFiles.firstOrNull { it.id == fileId } ?: return

        launch {
            try {
                if (file.isDirectory) {
                    stack.push(file.id)
                    refreshFiles()
                } else {
                    startActivityForResult(
                        ActivityResultContracts.StartActivityForResult(),
                        Intent(Intent.ACTION_VIEW).setDataAndType(
                            client.buildDocumentUri(file.id),
                            "text/plain",
                        ).grantPermissions(),
                    )
                }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private suspend fun startImportNewFile() {
        try {
            val uri = startActivityForResult(ActivityResultContracts.GetContent(), "*/*")
            if (uri != null) {
                pendingImportUri = uri
                uiState.value = uiState.value.copy(
                    pendingFileNameDialog = PendingFileNameDialog(
                        title = getString(com.github.kr328.clash.design.R.string.file_name),
                        initialValue = uri.fileName ?: "File",
                        mode = PendingFileNameMode.Import,
                    ),
                )
            }
        } catch (e: Exception) {
            showError(e)
        }
    }

    private suspend fun importToExistingFile(fileId: String) {
        try {
            val uri = startActivityForResult(ActivityResultContracts.GetContent(), "*/*")
            if (uri != null) {
                client.copyDocument(fileId, uri)
                refreshFiles()
            }
        } catch (e: Exception) {
            showError(e)
        }
    }

    private suspend fun exportFile(fileId: String) {
        val file = currentFiles.firstOrNull { it.id == fileId } ?: return

        try {
            val uri = startActivityForResult(
                ActivityResultContracts.CreateDocument("text/plain"),
                file.name,
            )
            if (uri != null) {
                client.copyDocument(uri, file.id)
                refreshFiles()
            }
        } catch (e: Exception) {
            showError(e)
        }
    }

    private suspend fun deleteRemoteFile(fileId: String) {
        try {
            client.deleteDocument(fileId)
            refreshFiles()
        } catch (e: Exception) {
            showError(e)
        }
    }

    private suspend fun confirmFileNameDialog(fileName: String) {
        if (!PatternFileName.matches(fileName) || fileName.isBlank()) {
            snackbarMessages.emit(
                ClashSnackbarMessage(
                    message = getString(com.github.kr328.clash.design.R.string.invalid_file_name),
                    duration = SnackbarDuration.Long,
                ),
            )
            return
        }

        try {
            when (uiState.value.pendingFileNameDialog?.mode) {
                PendingFileNameMode.Rename -> {
                    val fileId = pendingRenameFileId ?: return
                    client.renameDocument(fileId, fileName)
                }
                PendingFileNameMode.Import -> {
                    val uri = pendingImportUri ?: return
                    val parentDocumentId = stack.lastOrNull() ?: root
                    client.importDocument(parentDocumentId, uri, fileName)
                }
                null -> return
            }

            pendingImportUri = null
            pendingRenameFileId = null
            uiState.value = uiState.value.copy(pendingFileNameDialog = null)
            refreshFiles()
        } catch (e: Exception) {
            showError(e)
        }
    }

    private suspend fun showError(e: Exception) {
        snackbarMessages.emit(
            ClashSnackbarMessage(
                message = e.message ?: "Unknown",
                duration = SnackbarDuration.Long,
            ),
        )
    }
}
