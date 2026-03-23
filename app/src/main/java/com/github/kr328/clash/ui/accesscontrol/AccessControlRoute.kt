package com.github.kr328.clash.ui.accesscontrol

import android.Manifest.permission.INTERNET
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.remote.Remote
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessControlViewModel(application: Application) : ClashViewModel(application) {
    private val serviceStore = ServiceStore(application)
    private val _uiState = MutableStateFlow(AccessControlUiState())
    val uiState = _uiState.asStateFlow()
    private val selected = linkedSetOf<String>()

    init {
        viewModelScope.launch {
            selected.clear()
            selected.addAll(withContext(Dispatchers.IO) { serviceStore.accessControlPackages })
            reloadApps()
        }
    }

    fun onToggleApp(packageName: String) {
        if (!selected.add(packageName)) {
            selected.remove(packageName)
        }
        refreshAppSelection()
    }

    fun onSelectAll() {
        selected.clear()
        selected.addAll(_uiState.value.apps.map(AccessControlAppItemUiState::packageName))
        refreshAppSelection()
    }

    fun onSelectNone() {
        selected.clear()
        refreshAppSelection()
    }

    fun onSelectInvert() {
        val current = _uiState.value.apps.map(AccessControlAppItemUiState::packageName).toSet()
        val next = current - selected
        selected.clear()
        selected.addAll(next)
        refreshAppSelection()
    }

    fun onToggleShowSystemApps() {
        uiStore.accessControlSystemApp = !uiStore.accessControlSystemApp
        reload()
    }

    fun onSetSort(sort: AppInfoSort) {
        uiStore.accessControlSort = sort
        reload()
    }

    fun onToggleReverse() {
        uiStore.accessControlReverse = !uiStore.accessControlReverse
        reload()
    }

    fun onImportClipboard(text: String?) {
        if (text.isNullOrBlank()) return
        val packages = text.split("\n").toSet()
        val all = _uiState.value.apps.map(AccessControlAppItemUiState::packageName).intersect(packages)
        selected.clear()
        selected.addAll(all)
        refreshAppSelection()
    }

    fun exportClipboard(): String = selected.joinToString("\n")

    fun onOpenSearch() {
        _uiState.update { it.copy(searchOpen = true) }
    }

    fun onCloseSearch() {
        _uiState.update { it.copy(searchOpen = false, searchQuery = "") }
    }

    fun onSearchQueryChanged(keyword: String) {
        _uiState.update { it.copy(searchQuery = keyword) }
    }

    fun persistChanges() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val changed = selected != serviceStore.accessControlPackages
                serviceStore.accessControlPackages = selected.toSet()
                if (clashRunning && changed) {
                    app.stopClashService()
                    while (Remote.broadcasts.clashRunning) {
                        delay(200)
                    }
                    app.startClashService()
                }
            }
        }
    }

    private fun reload() {
        viewModelScope.launch {
            reloadApps()
        }
    }

    private suspend fun reloadApps() {
        _uiState.value = AccessControlUiState(
            apps = loadApps().map {
                AccessControlAppItemUiState(
                    packageName = it.packageName,
                    label = it.label,
                    selected = it.packageName in selected,
                )
            },
            sort = uiStore.accessControlSort,
            reverse = uiStore.accessControlReverse,
            showSystemApps = uiStore.accessControlSystemApp,
            searchQuery = _uiState.value.searchQuery,
            searchOpen = _uiState.value.searchOpen,
        )
    }

    private suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val reverse = uiStore.accessControlReverse
        val sort = uiStore.accessControlSort
        val showSystemApps = uiStore.accessControlSystemApp
        val pm = app.packageManager

        pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            .asSequence()
            .filter { it.packageName != app.packageName }
            .filter { it.applicationInfo != null }
            .filter {
                it.requestedPermissions?.contains(INTERNET) == true ||
                    it.applicationInfo!!.uid < android.os.Process.FIRST_APPLICATION_UID
            }
            .filter { showSystemApps || !it.isSystemApp }
            .map { it.toAppInfo(pm) }
            .sortedWith { first, second ->
                val selectedCompare = compareValues(
                    second.packageName in selected,
                    first.packageName in selected,
                )
                if (selectedCompare != 0) {
                    selectedCompare
                } else if (reverse) {
                    sort.compare(second, first)
                } else {
                    sort.compare(first, second)
                }
            }
            .toList()
    }

    private fun refreshAppSelection() {
        _uiState.update { state ->
            state.copy(
                apps = state.apps.map { appState ->
                    appState.copy(selected = appState.packageName in selected)
                },
            )
        }
    }

    private val PackageInfo.isSystemApp: Boolean
        get() = applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
}

@Composable
fun AccessControlRoute(
    navController: NavController,
    onBack: (() -> Unit)?,
    viewModel: AccessControlViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.persistChanges()
        }
    }

    AccessControlScreen(
        title = context.getString(R.string.access_control_packages),
        state = state,
        onBack = {
            onBack?.invoke() ?: navController.popBackStack()
        },
        onToggleApp = viewModel::onToggleApp,
        onSelectAll = viewModel::onSelectAll,
        onSelectNone = viewModel::onSelectNone,
        onSelectInvert = viewModel::onSelectInvert,
        onToggleShowSystemApps = viewModel::onToggleShowSystemApps,
        onSetSort = viewModel::onSetSort,
        onToggleReverse = viewModel::onToggleReverse,
        onImportClipboard = {
            val clipboard = context.getSystemService<ClipboardManager>()
            viewModel.onImportClipboard(clipboard?.primaryClip?.getItemAt(0)?.text?.toString())
        },
        onExportClipboard = {
            val clipboard = context.getSystemService<ClipboardManager>()
            clipboard?.setPrimaryClip(ClipData.newPlainText("packages", viewModel.exportClipboard()))
        },
        onOpenSearch = viewModel::onOpenSearch,
        onCloseSearch = viewModel::onCloseSearch,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
    )
}
