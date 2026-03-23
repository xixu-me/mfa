package com.github.kr328.clash.ui.settings.override

import android.app.Application
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.R
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OverrideSettingsViewModel(application: Application) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(OverrideSettingsUiState(configuration = ConfigurationOverride()))
    val uiState = _uiState.asStateFlow()
    private var configuration = ConfigurationOverride()

    init {
        viewModelScope.launch {
            configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }
            refresh()
        }
    }

    fun onResetRequested() {
        _uiState.update { it.copy(showResetConfirm = true) }
    }

    fun onResetDismissed() {
        _uiState.update { it.copy(showResetConfirm = false) }
    }

    fun onResetConfirmed() {
        viewModelScope.launch {
            withClash { clearOverride(Clash.OverrideSlot.Persist) }
            configuration = ConfigurationOverride()
            refresh()
        }
    }

    fun onConfigurationChanged(block: ConfigurationOverride.() -> Unit) {
        configuration.apply(block)
        refresh()
        persist()
    }

    private fun refresh() {
        _uiState.value = OverrideSettingsUiState(
            configuration = configuration,
            showResetConfirm = _uiState.value.showResetConfirm,
        )
    }

    private fun persist() {
        viewModelScope.launch {
            withClash {
                patchOverride(Clash.OverrideSlot.Persist, configuration)
            }
        }
    }
}

@Composable
fun OverrideSettingsRoute(
    onBack: (() -> Unit)?,
    viewModel: OverrideSettingsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OverrideSettingsScreen(
        title = LocalContext.current.getString(R.string.override),
        state = state,
        onBack = onBack,
        onResetRequested = viewModel::onResetRequested,
        onResetDismissed = viewModel::onResetDismissed,
        onResetConfirmed = viewModel::onResetConfirmed,
        onConfigurationChanged = viewModel::onConfigurationChanged,
        snackbarHostState = remember { SnackbarHostState() },
    )
}
