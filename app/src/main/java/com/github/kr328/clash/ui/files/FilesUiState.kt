package com.github.kr328.clash.ui.files

data class FilesUiState(
    val files: List<FileItemUiState> = emptyList(),
    val currentInBaseDir: Boolean = true,
    val configurationEditable: Boolean = false,
    val activeMenuFileId: String? = null,
    val pendingFileNameDialog: PendingFileNameDialog? = null,
)

data class FileItemUiState(
    val id: String,
    val name: String,
    val sizeSummary: String?,
    val elapsedSummary: String?,
    val isDirectory: Boolean,
    val canImport: Boolean,
    val canExport: Boolean,
    val canRename: Boolean,
    val canDelete: Boolean,
)

data class PendingFileNameDialog(
    val title: String,
    val initialValue: String,
    val mode: PendingFileNameMode,
)

enum class PendingFileNameMode {
    Rename,
    Import,
}
