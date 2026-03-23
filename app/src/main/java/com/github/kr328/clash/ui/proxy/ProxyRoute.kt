package com.github.kr328.clash.ui.proxy

import android.app.Application
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ProxyViewModel(application: Application) : ClashViewModel(application) {
    private val reloadLock = Semaphore(10)
    private val groups = linkedMapOf<String, ProxyGroup>()
    private val selectedByGroup = linkedMapOf<String, String>()
    private val pageMeta = linkedMapOf<String, PageMeta>()
    private var names: List<String> = emptyList()

    private val _uiState = MutableStateFlow(ProxyUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            reloadAllState()
        }
    }

    fun onReloadPage(index: Int) {
        val groupName = names.getOrNull(index) ?: return
        pageMeta.getOrPut(groupName, ::PageMeta).loading = true
        publishState()

        viewModelScope.launch {
            try {
                val group = reloadLock.withPermit {
                    withClash {
                        queryProxyGroup(groupName, uiStore.proxySort)
                    }
                }

                groups[groupName] = group
                selectedByGroup[groupName] = group.now
                pageMeta.getOrPut(groupName, ::PageMeta).apply {
                    loading = false
                    urlTesting = false
                }
                publishState()
            } catch (e: Exception) {
                pageMeta.getOrPut(groupName, ::PageMeta).apply {
                    loading = false
                    urlTesting = false
                }
                publishState()
                showError(e)
            }
        }
    }

    fun onUrlTest(index: Int) {
        val groupName = names.getOrNull(index) ?: return
        pageMeta.getOrPut(groupName, ::PageMeta).urlTesting = true
        publishState()

        viewModelScope.launch {
            try {
                withClash {
                    healthCheck(groupName)
                }
                onReloadPage(index)
            } catch (e: Exception) {
                pageMeta.getOrPut(groupName, ::PageMeta).urlTesting = false
                publishState()
                showError(e)
            }
        }
    }

    fun onSelectProxy(index: Int, name: String) {
        val groupName = names.getOrNull(index) ?: return

        viewModelScope.launch {
            try {
                withClash {
                    patchSelector(groupName, name)
                }

                selectedByGroup[groupName] = name
                groups[groupName]?.let { group ->
                    groups[groupName] = group.copy(now = name)
                }
                publishState()
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun onOverrideModeChanged(mode: TunnelState.Mode?) {
        publishState(overrideMode = mode)
        _snackbarMessages.tryEmit(
            ClashSnackbarMessage(
                message = app.getString(R.string.mode_switch_tips),
                duration = SnackbarDuration.Long,
            ),
        )

        viewModelScope.launch {
            try {
                withClash {
                    val override = queryOverride(Clash.OverrideSlot.Session)
                    override.mode = mode
                    patchOverride(Clash.OverrideSlot.Session, override)
                }
            } catch (e: Exception) {
                showError(e)
            }
        }
    }

    fun onExcludeNotSelectableChanged(value: Boolean) {
        if (uiStore.proxyExcludeNotSelectable == value) return

        uiStore.proxyExcludeNotSelectable = value
        viewModelScope.launch {
            reloadAllState()
        }
    }

    fun onProxyLineChanged(value: Int) {
        uiStore.proxyLine = value.coerceIn(1, 3)
        publishState()
    }

    fun onProxySortChanged(value: ProxySort) {
        if (uiStore.proxySort == value) return

        uiStore.proxySort = value
        publishState()
        reloadAll()
    }

    fun onPageSelected(index: Int) {
        names.getOrNull(index)?.let {
            uiStore.proxyLastGroup = it
            publishState(initialPageIndex = index)
        }
    }

    override fun onProfileLoaded() {
        viewModelScope.launch {
            val newNames = queryGroupNames()
            if (newNames != names) {
                names = newNames
                groups.keys.retainAll(newNames.toSet())
                selectedByGroup.keys.retainAll(newNames.toSet())
                syncPageMeta()
                publishState(
                    overrideMode = withClash { queryOverride(Clash.OverrideSlot.Session).mode },
                    initialPageIndex = names.indexOf(uiStore.proxyLastGroup).takeIf { it >= 0 } ?: 0,
                )
                reloadAll()
            }
        }
    }

    private suspend fun reloadAllState() {
        names = queryGroupNames()
        syncPageMeta()
        publishState(
            overrideMode = withClash { queryOverride(Clash.OverrideSlot.Session).mode },
            initialPageIndex = names.indexOf(uiStore.proxyLastGroup).takeIf { it >= 0 } ?: 0,
        )
        reloadAll()
    }

    private suspend fun queryGroupNames(): List<String> {
        return withClash {
            queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
        }
    }

    private fun reloadAll() {
        names.indices.forEach(::onReloadPage)
    }

    private fun publishState(
        overrideMode: TunnelState.Mode? = _uiState.value.overrideMode,
        initialPageIndex: Int = _uiState.value.initialPageIndex,
    ) {
        syncPageMeta()
        val pageIndex = if (names.isEmpty()) 0 else initialPageIndex.coerceIn(0, names.lastIndex)

        _uiState.value = ProxyUiState(
            pages = names.map { groupName ->
                val group = groups[groupName]
                val meta = pageMeta.getOrPut(groupName, ::PageMeta)

                ProxyPageUiState(
                    id = groupName,
                    title = groupName,
                    proxies = group?.proxies?.map { proxy ->
                        proxy.toUiState(parentNow = group.now, links = selectedByGroup)
                    }.orEmpty(),
                    selectable = group?.type == Proxy.Type.Selector,
                    loading = meta.loading && group == null,
                    urlTesting = meta.urlTesting,
                )
            },
            initialPageIndex = pageIndex,
            proxyLine = uiStore.proxyLine.coerceIn(1, 3),
            proxySort = uiStore.proxySort,
            overrideMode = overrideMode,
            excludeNotSelectable = uiStore.proxyExcludeNotSelectable,
        )
    }

    private fun syncPageMeta() {
        val obsolete = pageMeta.keys - names.toSet()
        obsolete.forEach(pageMeta::remove)
        names.forEach { pageMeta.putIfAbsent(it, PageMeta()) }
    }

    private fun Proxy.toUiState(
        parentNow: String,
        links: Map<String, String>,
    ): ProxyItemUiState {
        val title: String
        val subtitle: String

        if (type.group) {
            title = name
            val linkedNow = links[name]
            subtitle = if (linkedNow == null) {
                type.name
            } else {
                "${type.name}(${linkedNow.ifEmpty { "*" }})"
            }
        } else {
            title = this.title
            subtitle = this.subtitle
        }

        return ProxyItemUiState(
            id = name,
            title = title,
            subtitle = subtitle,
            delayText = delay.takeIf { it in 0..Short.MAX_VALUE }?.toString(),
            selected = name == parentNow,
        )
    }

    private fun showError(error: Exception) {
        _snackbarMessages.tryEmit(
            ClashSnackbarMessage(
                message = error.message ?: "Unknown",
                duration = SnackbarDuration.Long,
            ),
        )
    }

    private data class PageMeta(
        var loading: Boolean = true,
        var urlTesting: Boolean = false,
    )
}

@Composable
fun ProxyRoute(
    navController: NavController,
    viewModel: ProxyViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    ProxyScreen(
        title = androidx.compose.ui.platform.LocalContext.current.getString(R.string.proxy),
        state = state,
        onBack = { navController.popBackStack() },
        onReloadPage = viewModel::onReloadPage,
        onUrlTest = viewModel::onUrlTest,
        onSelectProxy = viewModel::onSelectProxy,
        onPageSelected = viewModel::onPageSelected,
        onExcludeNotSelectableChanged = viewModel::onExcludeNotSelectableChanged,
        onProxyLineChanged = viewModel::onProxyLineChanged,
        onProxySortChanged = viewModel::onProxySortChanged,
        onOverrideModeChanged = viewModel::onOverrideModeChanged,
        snackbarHostState = snackbarHostState,
    )
}
