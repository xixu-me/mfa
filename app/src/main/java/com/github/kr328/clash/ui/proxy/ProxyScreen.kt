package com.github.kr328.clash.ui.proxy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceGroup
import com.github.kr328.clash.design.compose.PreferenceOption
import com.github.kr328.clash.design.compose.PreferenceSelectableItem
import com.github.kr328.clash.design.compose.PreferenceSwitchItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProxyScreen(
    title: String,
    state: ProxyUiState,
    onBack: () -> Unit,
    onReloadPage: (Int) -> Unit,
    onUrlTest: (Int) -> Unit,
    onSelectProxy: (Int, String) -> Unit,
    onPageSelected: (Int) -> Unit,
    onExcludeNotSelectableChanged: (Boolean) -> Unit,
    onProxyLineChanged: (Int) -> Unit,
    onProxySortChanged: (ProxySort) -> Unit,
    onOverrideModeChanged: (TunnelState.Mode?) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    var showMenu by remember { mutableStateOf(false) }

    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        actions = {
            val currentPage = state.pages.getOrNull(state.initialPageIndex.coerceAtLeast(0))
            if (currentPage != null) {
                if (currentPage.urlTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.item_tailing_margin)),
                        strokeWidth = dimensionResource(R.dimen.divider_size) * 2,
                    )
                } else {
                    IconButton(onClick = { onUrlTest(state.initialPageIndex.coerceAtLeast(0)) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_flash_on),
                            contentDescription = stringResource(R.string.delay_test),
                        )
                    }
                }
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_more_vert),
                    contentDescription = stringResource(R.string.more),
                )
            }
        },
    ) { padding ->
        if (state.pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.proxy_empty_tips),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            ProxyPagerContent(
                state = state,
                contentPadding = padding,
                onReloadPage = onReloadPage,
                onUrlTest = onUrlTest,
                onSelectProxy = onSelectProxy,
                onPageSelected = onPageSelected,
            )
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
        ) {
            Column(
                modifier = Modifier.padding(bottom = dimensionResource(R.dimen.bottom_sheet_menu_items_padding)),
            ) {
                PreferenceGroup(title = stringResource(R.string.filter)) {
                    PreferenceSwitchItem(
                        title = stringResource(R.string.not_selectable),
                        checked = state.excludeNotSelectable,
                        onCheckedChange = onExcludeNotSelectableChanged,
                    )
                }

                PreferenceGroup(title = stringResource(R.string.mode)) {
                    PreferenceSelectableItem(
                        title = stringResource(R.string.mode),
                        value = state.overrideMode,
                        options = listOf(
                            PreferenceOption<TunnelState.Mode?>(null, stringResource(R.string.dont_modify)),
                            PreferenceOption(TunnelState.Mode.Direct, stringResource(R.string.direct_mode)),
                            PreferenceOption(TunnelState.Mode.Global, stringResource(R.string.global_mode)),
                            PreferenceOption(TunnelState.Mode.Rule, stringResource(R.string.rule_mode)),
                        ),
                        onSelected = onOverrideModeChanged,
                    )
                }

                PreferenceGroup(title = stringResource(R.string.layout)) {
                    PreferenceSelectableItem(
                        title = stringResource(R.string.layout),
                        value = state.proxyLine,
                        options = listOf(
                            PreferenceOption(1, stringResource(R.string.single)),
                            PreferenceOption(2, stringResource(R.string.doubles)),
                            PreferenceOption(3, stringResource(R.string.multiple)),
                        ),
                        onSelected = onProxyLineChanged,
                    )
                }

                PreferenceGroup(title = stringResource(R.string.sort)) {
                    PreferenceSelectableItem(
                        title = stringResource(R.string.sort),
                        value = state.proxySort,
                        options = listOf(
                            PreferenceOption(ProxySort.Default, stringResource(R.string.default_)),
                            PreferenceOption(ProxySort.Title, stringResource(R.string.name)),
                            PreferenceOption(ProxySort.Delay, stringResource(R.string.delay)),
                        ),
                        onSelected = onProxySortChanged,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProxyPagerContent(
    state: ProxyUiState,
    contentPadding: PaddingValues,
    onReloadPage: (Int) -> Unit,
    onUrlTest: (Int) -> Unit,
    onSelectProxy: (Int, String) -> Unit,
    onPageSelected: (Int) -> Unit,
) {
    val initialPage = state.initialPageIndex.coerceIn(0, state.pages.lastIndex)
    val pagerState = rememberPagerState(initialPage = initialPage) { state.pages.size }
    val bottomReached = remember { mutableStateListOf<Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.pages.size) {
        while (bottomReached.size < state.pages.size) {
            bottomReached.add(false)
        }
        while (bottomReached.size > state.pages.size) {
            bottomReached.removeAt(bottomReached.lastIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onPageSelected(page)
        }
    }

    val currentPageIndex = pagerState.currentPage.coerceIn(0, state.pages.lastIndex)
    val currentPage = state.pages[currentPageIndex]
    val showFab = !pagerState.isScrollInProgress &&
        !currentPage.urlTesting &&
        !(bottomReached.getOrNull(currentPageIndex) ?: false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                state.pages.forEachIndexed { index, page ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = page.title) },
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                ProxyPageGrid(
                    page = state.pages[pageIndex],
                    columns = state.proxyLine,
                    onReload = { onReloadPage(pageIndex) },
                    onSelectProxy = { onSelectProxy(pageIndex, it) },
                    onBottomChanged = { bottom ->
                        if (pageIndex in bottomReached.indices) {
                            bottomReached[pageIndex] = bottom
                        }
                    },
                )
            }
        }

        if (showFab) {
            FloatingActionButton(
                onClick = { onUrlTest(currentPageIndex) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dimensionResource(R.dimen.item_tailing_margin)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_flash_on),
                    contentDescription = stringResource(R.string.delay_test),
                )
            }
        }
    }
}

@Composable
private fun ProxyPageGrid(
    page: ProxyPageUiState,
    columns: Int,
    onReload: () -> Unit,
    onSelectProxy: (String) -> Unit,
    onBottomChanged: (Boolean) -> Unit,
) {
    if (page.loading && page.proxies.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val gridState = rememberLazyGridState()

    LaunchedEffect(gridState) {
        snapshotFlow { !gridState.canScrollForward }.collect(onBottomChanged)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns.coerceIn(1, 3)),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.item_header_margin),
            vertical = dimensionResource(R.dimen.item_padding_vertical),
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
    ) {
        items(page.proxies.size, key = { index -> page.proxies[index].id }) { index ->
            ProxyCard(
                proxy = page.proxies[index],
                selectable = page.selectable,
                columns = columns,
                onClick = { onSelectProxy(page.proxies[index].id) },
            )
        }
    }
}

@Composable
private fun ProxyCard(
    proxy: ProxyItemUiState,
    selectable: Boolean,
    columns: Int,
    onClick: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = selectable,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (proxy.selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (proxy.selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.item_header_margin),
                    vertical = if (columns == 1) dimensionResource(R.dimen.item_padding_vertical) else 12.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.item_text_margin)),
        ) {
            Text(
                text = proxy.title,
                style = if (columns == 3) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            )
            Text(
                text = proxy.subtitle,
                style = if (columns == 3) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            )
            proxy.delayText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProxyScreenPreview() {
    ClashTheme {
        ProxyScreen(
            title = "Proxy",
            state = ProxyUiState(
                pages = listOf(
                    ProxyPageUiState(
                        id = "Auto",
                        title = "Auto",
                        selectable = true,
                        loading = false,
                        proxies = listOf(
                            ProxyItemUiState(
                                id = "node-1",
                                title = "Tokyo",
                                subtitle = "SS",
                                delayText = "45",
                                selected = true,
                            ),
                            ProxyItemUiState(
                                id = "node-2",
                                title = "Singapore",
                                subtitle = "VMess",
                                delayText = "89",
                                selected = false,
                            ),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onReloadPage = {},
            onUrlTest = {},
            onSelectProxy = { _, _ -> },
            onPageSelected = {},
            onExcludeNotSelectableChanged = {},
            onProxyLineChanged = {},
            onProxySortChanged = {},
            onOverrideModeChanged = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
