package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.util.type
import com.github.kr328.clash.ui.providers.ProviderItemUiState
import com.github.kr328.clash.ui.providers.ProvidersScreen
import com.github.kr328.clash.ui.providers.ProvidersUiState
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.TimeUnit

class ProvidersActivity : BaseActivity() {
    private val uiState = MutableStateFlow(ProvidersUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    private val providers = mutableListOf<Provider>()

    override suspend fun main() {
        refreshProviders()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                ProvidersScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onUpdateAll = ::updateAllProviders,
                    onUpdate = ::requestUpdateProvider,
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ProfileLoaded -> {
                            refreshProviders()
                        }
                        else -> Unit
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        uiState.update {
                            it.copy(currentTime = System.currentTimeMillis())
                        }
                    }
                }
            }
        }
    }

    private suspend fun refreshProviders() {
        val newProviders = withClash { queryProviders().sorted() }

        providers.clear()
        providers += newProviders

        uiState.value = ProvidersUiState(
            providers = newProviders.map(::toProviderItemUiState),
            currentTime = System.currentTimeMillis(),
        )
    }

    private fun requestUpdateProvider(index: Int) {
        val provider = providers.getOrNull(index) ?: return

        uiState.update { state ->
            state.copy(
                providers = state.providers.mapIndexed { currentIndex, item ->
                    if (currentIndex == index) item.copy(updating = true) else item
                },
            )
        }

        launch {
            try {
                withClash {
                    updateProvider(provider.type, provider.name)
                }

                providers[index] = provider.copy(updatedAt = System.currentTimeMillis())
                uiState.update { state ->
                    state.copy(
                        currentTime = System.currentTimeMillis(),
                        providers = state.providers.mapIndexed { currentIndex, item ->
                            if (currentIndex == index) {
                                item.copy(
                                    updatedAt = System.currentTimeMillis(),
                                    updating = false,
                                )
                            } else {
                                item
                            }
                        },
                    )
                }
            } catch (e: Exception) {
                uiState.update { state ->
                    state.copy(
                        providers = state.providers.mapIndexed { currentIndex, item ->
                            if (currentIndex == index) item.copy(updating = false) else item
                        },
                    )
                }
                snackbarMessages.tryEmit(
                    ClashSnackbarMessage(
                        message = getString(
                            R.string.format_update_provider_failure,
                            provider.name,
                            e.message ?: "Unknown",
                        ),
                        duration = SnackbarDuration.Long,
                    ),
                )
            }
        }
    }

    private fun updateAllProviders() {
        providers.forEachIndexed { index, provider ->
            if (provider.vehicleType != Provider.VehicleType.Inline) {
                requestUpdateProvider(index)
            }
        }
    }

    private fun toProviderItemUiState(provider: Provider): ProviderItemUiState {
        return ProviderItemUiState(
            name = provider.name,
            summary = provider.type(this),
            updatedAt = provider.updatedAt,
            updateEnabled = provider.vehicleType != Provider.VehicleType.Inline,
            updating = false,
        )
    }
}
