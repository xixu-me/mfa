package com.github.kr328.clash

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toBytesString
import com.github.kr328.clash.design.util.toDateStr
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.profiles.ProfileItemUiState
import com.github.kr328.clash.ui.profiles.ProfilesScreen
import com.github.kr328.clash.ui.profiles.ProfilesUiState
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.UUID
import java.util.concurrent.TimeUnit

class ProfilesActivity : BaseActivity() {
    private val uiState = MutableStateFlow(ProfilesUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    private var currentProfiles: List<Profile> = emptyList()

    override suspend fun main() {
        refreshProfiles()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                ProfilesScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onCreate = { startActivity(NewProfileActivity::class.intent) },
                    onUpdateAll = { launch { updateAllProfiles() } },
                    onActivate = { profileId -> launch { activateProfile(profileId) } },
                    onOpenMenu = { profileId ->
                        uiState.value = uiState.value.copy(activeMenuProfileId = profileId)
                    },
                    onDismissMenu = {
                        uiState.value = uiState.value.copy(activeMenuProfileId = null)
                    },
                    onUpdate = { profileId -> launch { updateProfile(profileId) } },
                    onEdit = { profileId -> editProfile(profileId) },
                    onDuplicate = { profileId -> launch { duplicateProfile(profileId) } },
                    onDelete = { profileId -> launch { deleteProfile(profileId) } },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        val ticker = ticker(TimeUnit.MINUTES.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart, Event.ProfileChanged -> refreshProfiles()
                        else -> Unit
                    }
                }
                if (activityStarted) {
                    ticker.onReceive {
                        refreshProfiles()
                    }
                }
            }
        }
    }

    private suspend fun refreshProfiles() {
        currentProfiles = withProfile { queryAll() }
        uiState.value = uiState.value.copy(
            profiles = currentProfiles.map { profile ->
                ProfileItemUiState(
                    id = profile.uuid.toString(),
                    name = profile.name,
                    typeSummary = profileTypeSummary(profile),
                    trafficSummary = profileTrafficSummary(profile),
                    expireSummary = if (profile.expire > 0) profile.expire.toDateStr() else null,
                    elapsedSummary = (System.currentTimeMillis() - profile.updatedAt).elapsedIntervalString(this),
                    active = profile.active,
                    usageProgress = profileUsageProgress(profile),
                    canUpdate = profile.imported && profile.type != Profile.Type.File,
                    canDuplicate = profile.imported,
                )
            },
            updateAllVisible = currentProfiles.any { it.imported && it.type != Profile.Type.File },
            activeMenuProfileId = null,
        )
    }

    private fun profileTypeSummary(profile: Profile): String {
        val type = when (profile.type) {
            Profile.Type.File -> getString(R.string.file)
            Profile.Type.Url -> getString(R.string.url)
            Profile.Type.External -> getString(R.string.external)
        }
        return if (profile.pending) getString(R.string.format_type_unsaved, type) else type
    }

    private fun profileTrafficSummary(profile: Profile): String? {
        if (profile.download < 2) return null
        val current = (profile.download + profile.upload).toBytesString()
        val total = if (profile.total > 1) profile.total.toBytesString() else null
        return if (total != null) "$current/$total" else current
    }

    private fun profileUsageProgress(profile: Profile): Float? {
        if (profile.total < 2) return null
        val used = profile.download + profile.upload
        return (used.toFloat() / profile.total.toFloat()).coerceIn(0f, 1f)
    }

    private fun profileById(profileId: String): Profile? {
        return currentProfiles.firstOrNull { it.uuid.toString() == profileId }
    }

    private suspend fun updateAllProfiles() {
        uiState.value = uiState.value.copy(updateAllRunning = true, activeMenuProfileId = null)
        try {
            withProfile {
                queryAll().forEach { p ->
                    if (p.imported && p.type != Profile.Type.File) {
                        update(p.uuid)
                    }
                }
            }
        } finally {
            uiState.value = uiState.value.copy(updateAllRunning = false)
            refreshProfiles()
        }
    }

    private suspend fun updateProfile(profileId: String) {
        val profile = profileById(profileId) ?: return
        withProfile { update(profile.uuid) }
        refreshProfiles()
    }

    private suspend fun deleteProfile(profileId: String) {
        val profile = profileById(profileId) ?: return
        withProfile { delete(profile.uuid) }
        refreshProfiles()
    }

    private fun editProfile(profileId: String) {
        val profile = profileById(profileId) ?: return
        startActivity(PropertiesActivity::class.intent.setUUID(profile.uuid))
    }

    private suspend fun activateProfile(profileId: String) {
        val profile = profileById(profileId) ?: return
        withProfile {
            if (profile.imported) {
                setActive(profile)
            } else {
                snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = getString(R.string.active_unsaved_tips),
                        duration = SnackbarDuration.Long,
                        actionLabel = getString(R.string.edit),
                        onAction = { editProfile(profileId) },
                    ),
                )
            }
        }
    }

    private suspend fun duplicateProfile(profileId: String) {
        val profile = profileById(profileId) ?: return
        val uuid = withProfile { clone(profile.uuid) }
        startActivity(PropertiesActivity::class.intent.setUUID(uuid))
    }

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if (uuid == null) return
        launch {
            var name: String? = null
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            snackbarMessages.emit(
                ClashSnackbarMessage(
                    message = getString(R.string.toast_profile_updated_complete, name),
                    duration = SnackbarDuration.Long,
                ),
            )
        }
    }

    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if (uuid == null) return
        launch {
            var name: String? = null
            withProfile {
                name = queryByUUID(uuid)?.name
            }
            snackbarMessages.emit(
                ClashSnackbarMessage(
                    message = getString(R.string.toast_profile_updated_failed, name, reason),
                    duration = SnackbarDuration.Long,
                    actionLabel = getString(R.string.edit),
                    onAction = { startActivity(PropertiesActivity::class.intent.setUUID(uuid)) },
                ),
            )
        }
    }
}
