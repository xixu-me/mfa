package com.github.kr328.clash

import android.Manifest.permission.INTERNET
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.getSystemService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.model.AppInfo
import com.github.kr328.clash.design.model.AppInfoSort
import com.github.kr328.clash.design.util.toAppInfo
import com.github.kr328.clash.service.store.ServiceStore
import com.github.kr328.clash.ui.accesscontrol.AccessControlAppItemUiState
import com.github.kr328.clash.ui.accesscontrol.AccessControlScreen
import com.github.kr328.clash.ui.accesscontrol.AccessControlUiState
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccessControlActivity : BaseActivity() {
    private val uiState = MutableStateFlow(AccessControlUiState())

    override suspend fun main() {
        val service = ServiceStore(this)

        val selected = withContext(Dispatchers.IO) {
            service.accessControlPackages.toMutableSet()
        }

        defer {
            withContext(Dispatchers.IO) {
                val changed = selected != service.accessControlPackages
                service.accessControlPackages = selected
                if (clashRunning && changed) {
                    stopClashService()
                    while (clashRunning) {
                        delay(200)
                    }
                    startClashService()
                }
            }
        }

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()

            ClashTheme {
                AccessControlScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onToggleApp = { packageName ->
                        if (selected.contains(packageName)) {
                            selected.remove(packageName)
                        } else {
                            selected.add(packageName)
                        }
                        refreshAppSelection(selected)
                    },
                    onSelectAll = {
                        val all = uiState.value.apps.map(AccessControlAppItemUiState::packageName)
                        selected.clear()
                        selected.addAll(all)
                        refreshAppSelection(selected)
                    },
                    onSelectNone = {
                        selected.clear()
                        refreshAppSelection(selected)
                    },
                    onSelectInvert = {
                        val all = uiState.value.apps.map(AccessControlAppItemUiState::packageName).toSet() - selected
                        selected.clear()
                        selected.addAll(all)
                        refreshAppSelection(selected)
                    },
                    onToggleShowSystemApps = {
                        uiStore.accessControlSystemApp = !uiStore.accessControlSystemApp
                        launchReload(selected)
                    },
                    onSetSort = { sort ->
                        uiStore.accessControlSort = sort
                        launchReload(selected)
                    },
                    onToggleReverse = {
                        uiStore.accessControlReverse = !uiStore.accessControlReverse
                        launchReload(selected)
                    },
                    onImportClipboard = {
                        importClipboard(selected)
                    },
                    onExportClipboard = {
                        exportClipboard(selected)
                    },
                    onOpenSearch = {
                        uiState.update { it.copy(searchOpen = true) }
                    },
                    onCloseSearch = {
                        uiState.update { it.copy(searchOpen = false, searchQuery = "") }
                    },
                    onSearchQueryChanged = { keyword ->
                        uiState.update { it.copy(searchQuery = keyword) }
                    },
                )
            }
        }

        reloadApps(selected)

        while (isActive) {
            events.receive()
        }
    }

    private suspend fun loadApps(selected: Set<String>): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val reverse = uiStore.accessControlReverse
            val sort = uiStore.accessControlSort
            val systemApp = uiStore.accessControlSystemApp

            val base = compareByDescending<AppInfo> { it.packageName in selected }
            val comparator = if (reverse) base.thenDescending(sort) else base.then(sort)

            val pm = packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

            packages.asSequence()
                .filter {
                    it.packageName != packageName
                }
                .filter {
                    it.applicationInfo != null
                }
                .filter {
                    it.requestedPermissions?.contains(INTERNET) == true || it.applicationInfo!!.uid < android.os.Process.FIRST_APPLICATION_UID
                }
                .filter {
                    systemApp || !it.isSystemApp
                }
                .map {
                    it.toAppInfo(pm)
                }
                .sortedWith(comparator)
                .toList()
        }

    private fun launchReload(selected: Set<String>) {
        this@AccessControlActivity.launch {
            reloadApps(selected)
        }
    }

    private fun importClipboard(selected: MutableSet<String>) {
        val clipboard = getSystemService<ClipboardManager>()
        val data = clipboard?.primaryClip

        if (data != null && data.itemCount > 0) {
            val packages = data.getItemAt(0).text.split("\n").toSet()
            val all = uiState.value.apps.map(AccessControlAppItemUiState::packageName).intersect(packages)

            selected.clear()
            selected.addAll(all)
            refreshAppSelection(selected)
        }
    }

    private fun exportClipboard(selected: Set<String>) {
        val clipboard = getSystemService<ClipboardManager>()
        val data = ClipData.newPlainText(
            "packages",
            selected.joinToString("\n"),
        )

        clipboard?.setPrimaryClip(data)
    }

    private fun refreshAppSelection(selected: Set<String>) {
        uiState.update { state ->
            state.copy(
                apps = state.apps.map { app ->
                    app.copy(selected = app.packageName in selected)
                },
            )
        }
    }

    private suspend fun reloadApps(selected: Set<String>) {
        uiState.value = AccessControlUiState(
            apps = loadApps(selected).map {
                AccessControlAppItemUiState(
                    packageName = it.packageName,
                    label = it.label,
                    selected = it.packageName in selected,
                )
            },
            sort = uiStore.accessControlSort,
            reverse = uiStore.accessControlReverse,
            showSystemApps = uiStore.accessControlSystemApp,
            searchQuery = uiState.value.searchQuery,
            searchOpen = uiState.value.searchOpen,
        )
    }

    private val PackageInfo.isSystemApp: Boolean
        get() {
            return applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) != 0
        }
}
