@file:Suppress("BlockingMethodInNonBlockingContext")

package com.github.kr328.clash.ui.files

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.common.util.PatternFileName
import com.github.kr328.clash.common.util.grantPermissions
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.model.File
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toBytesString
import com.github.kr328.clash.remote.FilesClient
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.util.fileName
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

internal sealed interface FilesEffect {
    data class OpenTextFile(val uri: Uri) : FilesEffect
    data object PickNewFile : FilesEffect
    data class PickImportFile(val fileId: String) : FilesEffect
    data class CreateDocument(
        val fileId: String,
        val fileName: String,
    ) : FilesEffect
}

class FilesViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(FilesUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _effects = MutableSharedFlow<FilesEffect>(extraBufferCapacity = 4)
    internal val effects = _effects.asSharedFlow()

    private val stack = ArrayDeque<String>()
    private val profileId = savedStateHandle.get<String>(AppRoute.PROFILE_ID)
    private val client = FilesClient(application)
    private var root: String? = null
    private var currentFiles: List<File> = emptyList()
    private var configurationEditable = false
    private var pendingImportUri: Uri? = null
    private var pendingRenameFileId: String? = null

    init {
        viewModelScope.launch {
            loadProfile()
            while (isActive && root != null) {
                delay(TimeUnit.MINUTES.toMillis(1))
                refreshFiles()
            }
        }
    }

    override fun onProfileLoaded() {
        viewModelScope.launch {
            loadProfile()
        }
    }

    override fun onProfileChanged() {
        viewModelScope.launch {
            refreshFiles()
        }
    }

    fun onBack(onExit: () -> Unit) {
        if (stack.isEmpty()) {
            onExit()
        } else {
            stack.removeLastOrNull()
            viewModelScope.launch {
                refreshFiles()
            }
        }
    }

    fun onOpenFile(fileId: String) {
        val file = currentFiles.firstOrNull { it.id == fileId } ?: return

        viewModelScope.launch {
            try {
                if (file.isDirectory) {
                    stack.addLast(file.id)
                    refreshFiles()
                } else {
                    _effects.emit(FilesEffect.OpenTextFile(client.buildDocumentUri(file.id)))
                }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun onOpenMenu(fileId: String) {
        _uiState.value = _uiState.value.copy(activeMenuFileId = fileId)
    }

    fun onDismissMenu() {
        _uiState.value = _uiState.value.copy(activeMenuFileId = null)
    }

    fun onRequestNewFile() {
        viewModelScope.launch {
            _effects.emit(FilesEffect.PickNewFile)
        }
    }

    fun onNewFilePicked(uri: Uri?) {
        if (uri == null) return

        pendingImportUri = uri
        _uiState.value = _uiState.value.copy(
            pendingFileNameDialog = PendingFileNameDialog(
                title = app.getString(R.string.file_name),
                initialValue = uri.fileName ?: "File",
                mode = PendingFileNameMode.Import,
            ),
        )
    }

    fun onRenameFile(fileId: String) {
        val file = currentFiles.firstOrNull { it.id == fileId } ?: return
        pendingRenameFileId = fileId
        _uiState.value = _uiState.value.copy(
            pendingFileNameDialog = PendingFileNameDialog(
                title = app.getString(R.string.file_name),
                initialValue = file.name,
                mode = PendingFileNameMode.Rename,
            ),
        )
    }

    fun onRequestImportToFile(fileId: String) {
        viewModelScope.launch {
            _effects.emit(FilesEffect.PickImportFile(fileId))
        }
    }

    fun onImportToFilePicked(fileId: String, uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            try {
                client.copyDocument(fileId, uri)
                refreshFiles()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun onRequestExportFile(fileId: String) {
        val file = currentFiles.firstOrNull { it.id == fileId } ?: return

        viewModelScope.launch {
            _effects.emit(FilesEffect.CreateDocument(fileId, file.name))
        }
    }

    fun onExportFilePicked(fileId: String, uri: Uri?) {
        if (uri == null) return

        viewModelScope.launch {
            try {
                client.copyDocument(uri, fileId)
                refreshFiles()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun onDeleteFile(fileId: String) {
        viewModelScope.launch {
            try {
                client.deleteDocument(fileId)
                refreshFiles()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun onDismissFileNameDialog() {
        pendingImportUri = null
        pendingRenameFileId = null
        _uiState.value = _uiState.value.copy(pendingFileNameDialog = null)
    }

    fun onConfirmFileNameDialog(fileName: String) {
        if (!PatternFileName.matches(fileName) || fileName.isBlank()) {
            viewModelScope.launch {
                _snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = app.getString(R.string.invalid_file_name),
                        duration = SnackbarDuration.Long,
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                when (_uiState.value.pendingFileNameDialog?.mode) {
                    PendingFileNameMode.Rename -> {
                        val fileId = pendingRenameFileId ?: return@launch
                        client.renameDocument(fileId, fileName)
                    }

                    PendingFileNameMode.Import -> {
                        val uri = pendingImportUri ?: return@launch
                        val parentDocumentId = stack.lastOrNull() ?: root ?: return@launch
                        client.importDocument(parentDocumentId, uri, fileName)
                    }

                    null -> return@launch
                }

                onDismissFileNameDialog()
                refreshFiles()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    private suspend fun loadProfile() {
        val currentProfileId = profileId ?: return
        val uuid = UUID.fromString(currentProfileId)
        val profile = withProfile { queryByUUID(uuid) } ?: return

        root = uuid.toString()
        configurationEditable = profile.type != Profile.Type.Url
        stack.clear()
        refreshFiles()
    }

    private suspend fun refreshFiles() {
        val rootDocumentId = root ?: return
        val currentInBaseDir = stack.isEmpty()
        val documentId = stack.lastOrNull() ?: rootDocumentId
        val files = filterFilesForDisplay(client.list(documentId), currentInBaseDir)

        currentFiles = files
        _uiState.value = _uiState.value.copy(
            files = files.map { file ->
                FileItemUiState(
                    id = file.id,
                    name = file.name,
                    sizeSummary = if (file.isDirectory) null else file.size.toBytesString(),
                    elapsedSummary = if (file.isDirectory) null else (System.currentTimeMillis() - file.lastModified).elapsedIntervalString(app),
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

    private suspend fun showError(error: Exception) {
        _snackbarMessages.emit(
            ClashSnackbarMessage(
                message = error.message ?: "Unknown",
                duration = SnackbarDuration.Long,
            ),
        )
    }
}

internal fun filterFilesForDisplay(
    files: List<File>,
    inBaseDir: Boolean,
): List<File> {
    if (!inBaseDir) return files

    val config = files.firstOrNull { it.id.endsWith("config.yaml") }
    return if (config == null || config.size > 0) files else listOf(config)
}

@Composable
fun FilesRoute(
    navController: NavController,
    viewModel: FilesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingImportTargetFileId by remember { mutableStateOf<String?>(null) }
    var pendingExportTargetFileId by remember { mutableStateOf<String?>(null) }

    val openTextFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { }
    val newFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = viewModel::onNewFilePicked,
    )
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        pendingImportTargetFileId?.let { fileId ->
            viewModel.onImportToFilePicked(fileId, uri)
        }
        pendingImportTargetFileId = null
    }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        pendingExportTargetFileId?.let { fileId ->
            viewModel.onExportFilePicked(fileId, uri)
        }
        pendingExportTargetFileId = null
    }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FilesEffect.OpenTextFile -> {
                    openTextFileLauncher.launch(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(effect.uri, "text/plain")
                            .grantPermissions(),
                    )
                }

                FilesEffect.PickNewFile -> {
                    newFileLauncher.launch("*/*")
                }

                is FilesEffect.PickImportFile -> {
                    pendingImportTargetFileId = effect.fileId
                    importFileLauncher.launch("*/*")
                }

                is FilesEffect.CreateDocument -> {
                    pendingExportTargetFileId = effect.fileId
                    exportFileLauncher.launch(effect.fileName)
                }
            }
        }
    }

    FilesScreen(
        title = context.getString(R.string.files),
        state = state,
        onBack = { viewModel.onBack(navController::popBackStack) },
        onOpenFile = viewModel::onOpenFile,
        onOpenMenu = viewModel::onOpenMenu,
        onDismissMenu = viewModel::onDismissMenu,
        onNewFile = viewModel::onRequestNewFile,
        onRenameFile = viewModel::onRenameFile,
        onImportToFile = viewModel::onRequestImportToFile,
        onExportFile = viewModel::onRequestExportFile,
        onDeleteFile = viewModel::onDeleteFile,
        onDismissFileNameDialog = viewModel::onDismissFileNameDialog,
        onConfirmFileNameDialog = viewModel::onConfirmFileNameDialog,
        snackbarHostState = snackbarHostState,
    )
}
