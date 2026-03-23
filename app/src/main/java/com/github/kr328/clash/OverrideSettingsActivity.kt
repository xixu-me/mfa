package com.github.kr328.clash

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.settings.override.OverrideSettingsScreen
import com.github.kr328.clash.ui.settings.override.OverrideSettingsUiState
import com.github.kr328.clash.util.withClash
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow

class OverrideSettingsActivity : BaseActivity() {
    private lateinit var configuration: ConfigurationOverride
    private val uiState = MutableStateFlow(OverrideSettingsUiState(configuration = ConfigurationOverride()))

    override suspend fun main() {
        configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }

        defer {
            withClash {
                patchOverride(Clash.OverrideSlot.Persist, configuration)
            }
        }

        refreshState()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashTheme {
                OverrideSettingsScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onResetRequested = {
                        uiState.value = uiState.value.copy(showResetConfirm = true)
                    },
                    onResetDismissed = {
                        uiState.value = uiState.value.copy(showResetConfirm = false)
                    },
                    onResetConfirmed = {
                        defer {
                            withClash {
                                clearOverride(Clash.OverrideSlot.Persist)
                            }
                        }
                        finish()
                    },
                    onConfigurationChanged = {
                        configuration.apply(it)
                        refreshState()
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        awaitCancellation()
    }

    private fun refreshState() {
        uiState.value = OverrideSettingsUiState(
            configuration = configuration,
            showResetConfirm = uiState.value.showResetConfirm,
        )
    }
}
