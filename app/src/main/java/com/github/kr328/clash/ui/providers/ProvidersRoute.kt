package com.github.kr328.clash.ui.providers

import android.app.Application
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.core.model.Provider
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.util.type
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ProvidersViewModel(application: Application) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState = _uiState.asStateFlow()
    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()
    private val providers = mutableListOf<Provider>()

    init {
        refreshProviders()
        viewModelScope.launch {
            while (isActive) {
                delay(TimeUnit.MINUTES.toMillis(1))
                _uiState.value = _uiState.value.copy(currentTime = System.currentTimeMillis())
            }
        }
    }

    fun onUpdate(index: Int) {
        val provider = providers.getOrNull(index) ?: return

        _uiState.value = _uiState.value.copy(
            providers = _uiState.value.providers.mapIndexed { currentIndex, item ->
                if (currentIndex == index) item.copy(updating = true) else item
            },
        )

        viewModelScope.launch {
            try {
                withClash {
                    updateProvider(provider.type, provider.name)
                }
                providers[index] = provider.copy(updatedAt = System.currentTimeMillis())
                _uiState.value = _uiState.value.copy(
                    currentTime = System.currentTimeMillis(),
                    providers = _uiState.value.providers.mapIndexed { currentIndex, item ->
                        if (currentIndex == index) {
                            item.copy(updatedAt = System.currentTimeMillis(), updating = false)
                        } else {
                            item
                        }
                    },
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    providers = _uiState.value.providers.mapIndexed { currentIndex, item ->
                        if (currentIndex == index) item.copy(updating = false) else item
                    },
                )
                _snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = app.getString(
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

    fun onUpdateAll() {
        providers.forEachIndexed { index, provider ->
            if (provider.vehicleType != Provider.VehicleType.Inline) {
                onUpdate(index)
            }
        }
    }

    override fun onProfileLoaded() = refreshProviders()

    private fun refreshProviders() {
        viewModelScope.launch {
            val newProviders = withClash { queryProviders().sorted() }
            providers.clear()
            providers += newProviders
            _uiState.value = ProvidersUiState(
                providers = newProviders.map(::toProviderItemUiState),
                currentTime = System.currentTimeMillis(),
            )
        }
    }

    private fun toProviderItemUiState(provider: Provider): ProviderItemUiState {
        return ProviderItemUiState(
            name = provider.name,
            summary = provider.type(app),
            updatedAt = provider.updatedAt,
            updateEnabled = provider.vehicleType != Provider.VehicleType.Inline,
            updating = false,
        )
    }
}

@Composable
fun ProvidersRoute(
    navController: NavController,
    viewModel: ProvidersViewModel = viewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    ProvidersScreen(
        title = androidx.compose.ui.platform.LocalContext.current.getString(R.string.providers),
        state = state.value,
        onBack = { navController.popBackStack() },
        onUpdateAll = viewModel::onUpdateAll,
        onUpdate = viewModel::onUpdate,
        snackbarHostState = snackbarHostState,
    )
}
