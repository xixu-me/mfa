package com.github.kr328.clash.ui.proxy

import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState

data class ProxyUiState(
    val pages: List<ProxyPageUiState> = emptyList(),
    val initialPageIndex: Int = 0,
    val proxyLine: Int = 2,
    val proxySort: ProxySort = ProxySort.Default,
    val overrideMode: TunnelState.Mode? = null,
    val excludeNotSelectable: Boolean = false,
)

data class ProxyPageUiState(
    val id: String,
    val title: String,
    val proxies: List<ProxyItemUiState> = emptyList(),
    val selectable: Boolean = false,
    val loading: Boolean = true,
    val urlTesting: Boolean = false,
)

data class ProxyItemUiState(
    val id: String,
    val title: String,
    val subtitle: String,
    val delayText: String?,
    val selected: Boolean,
)
