package com.github.kr328.clash

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.proxy.ProxyItemUiState
import com.github.kr328.clash.ui.proxy.ProxyPageUiState
import com.github.kr328.clash.ui.proxy.ProxyScreen
import com.github.kr328.clash.ui.proxy.ProxyUiState
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ProxyActivity : BaseActivity() {
    private lateinit var names: List<String>
    private val reloadLock = Semaphore(10)
    private val groups = linkedMapOf<String, ProxyGroup>()
    private val selectedByGroup = linkedMapOf<String, String>()
    private val uiState = MutableStateFlow(ProxyUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    private val pageMeta = linkedMapOf<String, PageMeta>()

    override suspend fun main() {
        names = queryGroupNames()
        syncPageMeta()
        publishState(
            overrideMode = withClash { queryOverride(Clash.OverrideSlot.Session).mode },
            initialPageIndex = names.indexOf(uiStore.proxyLastGroup).takeIf { it >= 0 } ?: 0,
        )

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                ProxyScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onReloadPage = ::reloadGroup,
                    onUrlTest = ::requestUrlTest,
                    onSelectProxy = ::selectProxy,
                    onPageSelected = ::handlePageSelected,
                    onExcludeNotSelectableChanged = ::changeExcludeNotSelectable,
                    onProxyLineChanged = ::changeProxyLine,
                    onProxySortChanged = ::changeProxySort,
                    onOverrideModeChanged = ::patchMode,
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        reloadAll()

        while (isActive) {
            when (events.receive()) {
                Event.ProfileLoaded -> {
                    val newNames = queryGroupNames()
                    if (newNames != names) {
                        relaunch()
                    }
                }
                else -> Unit
            }
        }
    }

    private suspend fun queryGroupNames(): List<String> {
        return withClash {
            queryProxyGroupNames(uiStore.proxyExcludeNotSelectable)
        }
    }

    private fun reloadAll() {
        names.indices.forEach(::reloadGroup)
    }

    private fun reloadGroup(index: Int) {
        val groupName = names.getOrNull(index) ?: return
        pageMeta.getOrPut(groupName, ::PageMeta).loading = true
        publishState()

        launch {
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

    private fun requestUrlTest(index: Int) {
        val groupName = names.getOrNull(index) ?: return
        pageMeta.getOrPut(groupName, ::PageMeta).urlTesting = true
        publishState()

        launch {
            try {
                withClash {
                    healthCheck(groupName)
                }
                reloadGroup(index)
            } catch (e: Exception) {
                pageMeta.getOrPut(groupName, ::PageMeta).urlTesting = false
                publishState()
                showError(e)
            }
        }
    }

    private fun selectProxy(index: Int, name: String) {
        val groupName = names.getOrNull(index) ?: return

        launch {
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

    private fun patchMode(mode: TunnelState.Mode?) {
        publishState(overrideMode = mode)
        snackbarMessages.tryEmit(
            ClashSnackbarMessage(
                message = getString(R.string.mode_switch_tips),
                duration = SnackbarDuration.Long,
            ),
        )

        launch {
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

    private fun changeExcludeNotSelectable(value: Boolean) {
        if (uiStore.proxyExcludeNotSelectable == value) {
            return
        }

        uiStore.proxyExcludeNotSelectable = value
        relaunch()
    }

    private fun changeProxyLine(value: Int) {
        uiStore.proxyLine = value.coerceIn(1, 3)
        publishState()
    }

    private fun changeProxySort(value: ProxySort) {
        if (uiStore.proxySort == value) {
            return
        }

        uiStore.proxySort = value
        publishState()
        reloadAll()
    }

    private fun handlePageSelected(index: Int) {
        names.getOrNull(index)?.let {
            uiStore.proxyLastGroup = it
            publishState(initialPageIndex = index)
        }
    }

    private fun publishState(
        overrideMode: TunnelState.Mode? = uiState.value.overrideMode,
        initialPageIndex: Int = uiState.value.initialPageIndex,
    ) {
        syncPageMeta()
        val pageIndex = if (names.isEmpty()) 0 else initialPageIndex.coerceIn(0, names.lastIndex)

        uiState.value = ProxyUiState(
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

    private fun relaunch() {
        startActivity(ProxyActivity::class.intent)
        finish()
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
        snackbarMessages.tryEmit(
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
