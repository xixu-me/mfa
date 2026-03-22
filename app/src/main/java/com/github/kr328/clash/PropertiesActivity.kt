package com.github.kr328.clash

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.uuid
import com.github.kr328.clash.core.model.FetchStatus
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.properties.PropertiesScreen
import com.github.kr328.clash.ui.properties.PropertiesUiState
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PropertiesActivity : BaseActivity() {
    private var canceled = false
    private lateinit var original: Profile
    private lateinit var profile: Profile
    private val uiState = MutableStateFlow(PropertiesUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)

    override suspend fun main() {
        setResult(RESULT_CANCELED)

        val uuid = intent.uuid ?: return finish()
        original = withProfile { queryByUUID(uuid) } ?: return finish()
        profile = original
        refreshState()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                PropertiesScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = ::handleBackPressed,
                    onSave = {
                        launch {
                            verifyAndCommit()
                        }
                    },
                    onOpenFiles = {
                        startActivity(FilesActivity::class.intent.setUUID(uuid))
                    },
                    onNameChanged = {
                        profile = profile.copy(name = it)
                        refreshState()
                    },
                    onSourceChanged = {
                        profile = profile.copy(source = it)
                        refreshState()
                    },
                    onIntervalChanged = {
                        profile = profile.copy(interval = it)
                        refreshState()
                    },
                    onDiscardDismissed = {
                        uiState.value = uiState.value.copy(showDiscardChangesDialog = false)
                    },
                    onDiscardConfirmed = {
                        uiState.value = uiState.value.copy(showDiscardChangesDialog = false)
                        finish()
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        defer {
            canceled = true
            withProfile { release(uuid) }
        }

        while (isActive) {
            when (events.receive()) {
                Event.ActivityStop -> persistDraftIfNeeded()
                Event.ServiceRecreated -> finish()
                else -> Unit
            }
        }
    }

    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun handleBackPressed() {
        if (uiState.value.isProcessing) {
            return
        }

        if (profile == original) {
            finish()
        } else {
            uiState.value = uiState.value.copy(showDiscardChangesDialog = true)
        }
    }

    private suspend fun persistDraftIfNeeded() {
        if (!canceled && profile != original) {
            withProfile {
                patch(profile.uuid, profile.name, profile.source, profile.interval)
            }
        }
    }

    private suspend fun verifyAndCommit() {
        when {
            profile.name.isBlank() -> {
                showSnackbar(R.string.empty_name)
            }
            profile.type != Profile.Type.File && profile.source.isBlank() -> {
                showSnackbar(R.string.invalid_url)
            }
            else -> {
                uiState.value = uiState.value.copy(
                    isProcessing = true,
                    progressMessage = getString(R.string.initializing),
                    progress = null,
                )

                try {
                    withProfile {
                        patch(profile.uuid, profile.name, profile.source, profile.interval)

                        coroutineScope {
                            commit(profile.uuid) { status ->
                                updateFetchStatus(status)
                            }
                        }
                    }

                    setResult(RESULT_OK)
                    finish()
                } catch (e: Exception) {
                    snackbarMessages.emit(
                        ClashSnackbarMessage(
                            message = e.message ?: "Unknown",
                            duration = SnackbarDuration.Long,
                        ),
                    )
                } finally {
                    uiState.value = uiState.value.copy(
                        isProcessing = false,
                        progressMessage = null,
                        progress = null,
                    )
                }
            }
        }
    }

    private fun updateFetchStatus(status: FetchStatus) {
        val progress = if (status.max > 0 && status.action != FetchStatus.Action.FetchConfiguration) {
            status.progress.toFloat() / status.max.toFloat()
        } else {
            null
        }

        uiState.value = uiState.value.copy(
            isProcessing = true,
            progressMessage = when (status.action) {
                FetchStatus.Action.FetchConfiguration -> {
                    getString(R.string.format_fetching_configuration, status.args[0])
                }
                FetchStatus.Action.FetchProviders -> {
                    getString(R.string.format_fetching_provider, status.args[0])
                }
                FetchStatus.Action.Verifying -> {
                    getString(R.string.verifying)
                }
            },
            progress = progress,
        )
    }

    private fun refreshState() {
        uiState.value = uiState.value.copy(
            profile = profile,
        )
    }

    private suspend fun showSnackbar(message: Int) {
        snackbarMessages.emit(
            ClashSnackbarMessage(
                message = getString(message),
                duration = SnackbarDuration.Long,
            ),
        )
    }
}
