package com.github.kr328.clash.ui.profiles

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.util.elapsedIntervalString
import com.github.kr328.clash.design.util.toBytesString
import com.github.kr328.clash.design.util.toDateStr
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.ui.properties.PropertiesScreen
import com.github.kr328.clash.ui.properties.PropertiesUiState
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class ProfilesViewModel(application: Application) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState = _uiState.asStateFlow()
    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()
    private var currentProfiles: List<Profile> = emptyList()

    init {
        refreshProfiles()
    }

    fun onOpenMenu(profileId: String) {
        _uiState.value = _uiState.value.copy(activeMenuProfileId = profileId)
    }

    fun onDismissMenu() {
        _uiState.value = _uiState.value.copy(activeMenuProfileId = null)
    }

    fun onUpdateAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(updateAllRunning = true, activeMenuProfileId = null)
            try {
                withProfile {
                    queryAll().forEach { profile ->
                        if (profile.imported && profile.type != Profile.Type.File) {
                            update(profile.uuid)
                        }
                    }
                }
            } finally {
                _uiState.value = _uiState.value.copy(updateAllRunning = false)
                refreshProfiles()
            }
        }
    }

    fun onUpdate(profileId: String) {
        val profile = profileById(profileId) ?: return
        viewModelScope.launch {
            withProfile { update(profile.uuid) }
            refreshProfiles()
        }
    }

    fun onDelete(profileId: String) {
        val profile = profileById(profileId) ?: return
        viewModelScope.launch {
            withProfile { delete(profile.uuid) }
            refreshProfiles()
        }
    }

    fun onActivate(profileId: String, onEditUnsaved: () -> Unit) {
        val profile = profileById(profileId) ?: return
        viewModelScope.launch {
            withProfile {
                if (profile.imported) {
                    setActive(profile)
                } else {
                    _snackbarMessages.emit(
                        ClashSnackbarMessage(
                            message = app.getString(R.string.active_unsaved_tips),
                            duration = SnackbarDuration.Long,
                            actionLabel = app.getString(R.string.edit),
                            onAction = onEditUnsaved,
                        ),
                    )
                }
            }
        }
    }

    fun onDuplicate(profileId: String, onDuplicated: (String) -> Unit) {
        val profile = profileById(profileId) ?: return
        viewModelScope.launch {
            val uuid = withProfile { clone(profile.uuid) }
            onDuplicated(uuid.toString())
        }
    }

    override fun onProfileChanged() = refreshProfiles()
    override fun onProfileLoaded() = refreshProfiles()

    override fun onProfileUpdateCompleted(uuid: UUID?) {
        if (uuid == null) return
        viewModelScope.launch {
            val name = withProfile { queryByUUID(uuid)?.name }
            _snackbarMessages.emit(
                ClashSnackbarMessage(
                    message = app.getString(R.string.toast_profile_updated_complete, name),
                    duration = SnackbarDuration.Long,
                ),
            )
        }
    }

    override fun onProfileUpdateFailed(uuid: UUID?, reason: String?) {
        if (uuid == null) return
        viewModelScope.launch {
            val name = withProfile { queryByUUID(uuid)?.name }
            _snackbarMessages.emit(
                ClashSnackbarMessage(
                    message = app.getString(R.string.toast_profile_updated_failed, name, reason),
                    duration = SnackbarDuration.Long,
                ),
            )
        }
    }

    private fun refreshProfiles() {
        viewModelScope.launch {
            currentProfiles = withProfile { queryAll() }
            _uiState.value = _uiState.value.copy(
                profiles = currentProfiles.map { profile ->
                    ProfileItemUiState(
                        id = profile.uuid.toString(),
                        name = profile.name,
                        typeSummary = profileTypeSummary(profile),
                        trafficSummary = profileTrafficSummary(profile),
                        expireSummary = if (profile.expire > 0) profile.expire.toDateStr() else null,
                        elapsedSummary = (System.currentTimeMillis() - profile.updatedAt).elapsedIntervalString(app),
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
    }

    private fun profileById(profileId: String): Profile? {
        return currentProfiles.firstOrNull { it.uuid.toString() == profileId }
    }

    private fun profileTypeSummary(profile: Profile): String {
        val type = when (profile.type) {
            Profile.Type.File -> app.getString(R.string.file)
            Profile.Type.Url -> app.getString(R.string.url)
            Profile.Type.External -> app.getString(R.string.external)
        }
        return if (profile.pending) app.getString(R.string.format_type_unsaved, type) else type
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
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ProfilesRoute(
    navController: NavController,
    viewModel: ProfilesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scaffoldNavigator = rememberListDetailPaneScaffoldNavigator<String>()
    val scope = rememberCoroutineScope()
    val widthSizeClass = calculateWindowSizeClass(context as ComponentActivity).widthSizeClass
    val compactLayout = widthSizeClass == WindowWidthSizeClass.Compact
    val backBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
    val selectedProfileId = scaffoldNavigator.currentDestination?.contentKey

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    fun openDetail(profileId: String) {
        scope.launch {
            scaffoldNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, profileId)
        }
    }

    fun closeDetail() {
        scope.launch {
            scaffoldNavigator.navigateBack(backBehavior)
        }
    }

    val detailBack: (() -> Unit)? = if (compactLayout) {
        { closeDetail() }
    } else {
        null
    }
    val commitSuccess: () -> Unit = if (compactLayout) {
        { closeDetail() }
    } else {
        {}
    }

    NavigableListDetailPaneScaffold(
        navigator = scaffoldNavigator,
        listPane = {
            AnimatedPane {
                ProfilesScreen(
                    title = context.getString(R.string.profiles),
                    state = state,
                    selectedProfileId = selectedProfileId,
                    onBack = { navController.popBackStack() },
                    onCreate = { navController.navigate(AppRoute.NewProfile.route) },
                    onUpdateAll = viewModel::onUpdateAll,
                    onSelect = ::openDetail,
                    onActivate = { profileId ->
                        viewModel.onActivate(profileId) {
                            openDetail(profileId)
                        }
                    },
                    onOpenMenu = viewModel::onOpenMenu,
                    onDismissMenu = viewModel::onDismissMenu,
                    onUpdate = viewModel::onUpdate,
                    onEdit = ::openDetail,
                    onDuplicate = { profileId ->
                        viewModel.onDuplicate(profileId) { newId ->
                            openDetail(newId)
                        }
                    },
                    onDelete = { profileId ->
                        viewModel.onDelete(profileId)
                        if (selectedProfileId == profileId) {
                            closeDetail()
                        }
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        },
        detailPane = {
            AnimatedPane {
                if (selectedProfileId == null) {
                    EmptyProfileDetailPane(onBack = if (compactLayout) ::closeDetail else null)
                } else {
                    PropertiesRoute(
                        navController = navController,
                        profileId = selectedProfileId,
                        onBack = detailBack,
                        onCommitSuccess = commitSuccess,
                    )
                }
            }
        },
    )
}

class PropertiesViewModel(
    application: Application,
    private val profileId: String,
) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(PropertiesUiState())
    val uiState = _uiState.asStateFlow()
    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 8)
    val snackbarMessages = _snackbarMessages.asSharedFlow()
    private var original: Profile? = null
    private var profile: Profile? = null

    init {
        loadProfile()
    }

    fun onNameChanged(value: String) {
        profile = profile?.copy(name = value)
        refreshState()
    }

    fun onSourceChanged(value: String) {
        profile = profile?.copy(source = value)
        refreshState()
    }

    fun onIntervalChanged(value: Long) {
        profile = profile?.copy(interval = value)
        refreshState()
    }

    fun onShowDiscardDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDiscardChangesDialog = show)
    }

    fun verifyAndCommit(onCommitted: () -> Unit) {
        val editing = profile ?: return
        when {
            editing.name.isBlank() -> showSnackbar(R.string.empty_name)
            editing.type != Profile.Type.File && editing.source.isBlank() -> showSnackbar(R.string.invalid_url)
            else -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = true,
                        progressMessage = app.getString(R.string.initializing),
                        progress = null,
                    )

                    try {
                        withProfile {
                            patch(editing.uuid, editing.name, editing.source, editing.interval)
                            coroutineScope {
                                commit(editing.uuid) { status ->
                                    val progress = if (status.max > 0 && status.action != com.github.kr328.clash.core.model.FetchStatus.Action.FetchConfiguration) {
                                        status.progress.toFloat() / status.max.toFloat()
                                    } else {
                                        null
                                    }

                                    _uiState.value = _uiState.value.copy(
                                        isProcessing = true,
                                        progressMessage = when (status.action) {
                                            com.github.kr328.clash.core.model.FetchStatus.Action.FetchConfiguration ->
                                                app.getString(R.string.format_fetching_configuration, status.args[0])
                                            com.github.kr328.clash.core.model.FetchStatus.Action.FetchProviders ->
                                                app.getString(R.string.format_fetching_provider, status.args[0])
                                            com.github.kr328.clash.core.model.FetchStatus.Action.Verifying ->
                                                app.getString(R.string.verifying)
                                        },
                                        progress = progress,
                                    )
                                }
                            }
                        }
                        original = profile
                        onCommitted()
                    } catch (e: Exception) {
                        _snackbarMessages.emit(
                            ClashSnackbarMessage(
                                message = e.message ?: "Unknown",
                                duration = SnackbarDuration.Long,
                            ),
                        )
                    } finally {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            progressMessage = null,
                            progress = null,
                        )
                    }
                }
            }
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val uuid = UUID.fromString(profileId)
            original = withProfile { queryByUUID(uuid) }
            profile = original
            refreshState()
        }
    }

    private fun refreshState() {
        _uiState.value = _uiState.value.copy(profile = profile)
            .copy(hasUnsavedChanges = profile != original)
    }

    private fun showSnackbar(message: Int) {
        viewModelScope.launch {
            _snackbarMessages.emit(
                ClashSnackbarMessage(
                    message = app.getString(message),
                    duration = SnackbarDuration.Long,
                ),
            )
        }
    }
}

@Composable
fun PropertiesRoute(
    navController: NavController,
    profileId: String,
    onBack: (() -> Unit)? = { navController.popBackStack() },
    onCommitSuccess: () -> Unit = { navController.popBackStack() },
) {
    PropertiesRoute(
        profileId = profileId,
        onBack = onBack,
        onCommitSuccess = onCommitSuccess,
        onOpenFiles = { selectedProfileId ->
            navController.navigate(AppRoute.Files.createRoute(selectedProfileId))
        },
    )
}

@Composable
fun PropertiesRoute(
    profileId: String,
    onBack: (() -> Unit)?,
    onCommitSuccess: () -> Unit,
    onOpenFiles: (String) -> Unit,
    viewModel: PropertiesViewModel = rememberPropertiesViewModel(profileId),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    PropertiesScreen(
        title = LocalContext.current.getString(R.string.profile),
        state = state,
        onBack = {
            if (state.isProcessing) return@PropertiesScreen
            if (shouldNavigateBack(state)) {
                onBack?.invoke()
            } else {
                viewModel.onShowDiscardDialog(true)
            }
        },
        onSave = {
            viewModel.verifyAndCommit {
                onCommitSuccess()
            }
        },
        onOpenFiles = {
            val selectedProfileId = state.profile?.uuid?.toString() ?: return@PropertiesScreen
            onOpenFiles(selectedProfileId)
        },
        onNameChanged = viewModel::onNameChanged,
        onSourceChanged = viewModel::onSourceChanged,
        onIntervalChanged = viewModel::onIntervalChanged,
        onDiscardDismissed = { viewModel.onShowDiscardDialog(false) },
        onDiscardConfirmed = {
            viewModel.onShowDiscardDialog(false)
            onBack?.invoke()
        },
        snackbarHostState = snackbarHostState,
    )
}

internal fun shouldNavigateBack(state: PropertiesUiState): Boolean {
    return !state.isProcessing && !state.hasUnsavedChanges
}

@Composable
private fun rememberPropertiesViewModel(profileId: String): PropertiesViewModel {
    val context = LocalContext.current.applicationContext as Application

    return viewModel(
        key = "properties:$profileId",
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras,
            ): T {
                @Suppress("UNCHECKED_CAST")
                return PropertiesViewModel(context, profileId) as T
            }
        },
    )
}

@Composable
private fun EmptyProfileDetailPane(
    onBack: (() -> Unit)?,
) {
    ClashScaffold(
        title = LocalContext.current.getString(R.string.profile),
        onBack = onBack,
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = LocalContext.current.getString(R.string.no_profile_selected))
        }
    }
}
