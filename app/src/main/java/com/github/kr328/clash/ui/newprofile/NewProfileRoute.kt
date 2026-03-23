package com.github.kr328.clash.ui.newprofile

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.model.ProfileProvider
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.app.ClashViewModel
import com.github.kr328.clash.util.withProfile
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRError
import io.github.g00fy2.quickie.QRResult.QRMissingPermission
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.QRResult.QRUserCanceled
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

internal sealed interface NewProfileEffect {
    data object LaunchQrScanner : NewProfileEffect
    data class LaunchExternalProvider(
        val providerId: String,
        val intent: Intent,
    ) : NewProfileEffect
    data class NavigateToProperties(val profileId: String) : NewProfileEffect
}

class NewProfileViewModel(application: Application) : ClashViewModel(application) {
    private val _uiState = MutableStateFlow(NewProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 4)
    val snackbarMessages = _snackbarMessages.asSharedFlow()

    private val _effects = MutableSharedFlow<NewProfileEffect>(extraBufferCapacity = 4)
    internal val effects = _effects.asSharedFlow()

    private var providers: List<ProfileProvider> = emptyList()

    init {
        viewModelScope.launch {
            providers = queryProfileProviders()
            _uiState.value = NewProfileUiState(
                providers = providers.mapIndexed { index, provider ->
                    NewProfileProviderUiState(
                        id = index.toString(),
                        title = provider.name,
                        summary = provider.summary,
                        iconRes = providerIcon(provider),
                        showDetail = provider is ProfileProvider.External,
                    )
                },
            )
        }
    }

    fun onCreate(providerId: String) {
        val provider = providerById(providerId) ?: return

        viewModelScope.launch {
            when (provider) {
                is ProfileProvider.File -> launchProperties(createProfile(Profile.Type.File, app.getString(R.string.new_profile)))
                is ProfileProvider.Url -> launchProperties(createProfile(Profile.Type.Url, app.getString(R.string.new_profile)))
                is ProfileProvider.QR -> _effects.emit(NewProfileEffect.LaunchQrScanner)
                is ProfileProvider.External -> _effects.emit(
                    NewProfileEffect.LaunchExternalProvider(
                        providerId = providerId,
                        intent = provider.intent,
                    ),
                )
            }
        }
    }

    fun onExternalProviderResult(
        providerId: String,
        resultCode: Int,
        data: Intent?,
    ) {
        if (resultCode != Activity.RESULT_OK) return

        val provider = providerById(providerId) as? ProfileProvider.External ?: return
        val uri = data?.data ?: return
        val initialName = data.getStringExtra(Intents.EXTRA_NAME)

        viewModelScope.launch {
            val uuid = withProfile {
                create(
                    type = Profile.Type.External,
                    name = initialName ?: app.getString(R.string.new_profile),
                    source = uri.toString(),
                )
            }
            launchProperties(uuid)
        }
    }

    fun onScanResult(result: QRResult) {
        viewModelScope.launch {
            when (result) {
                is QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.let { String(it) }.orEmpty()
                    val uuid = withProfile {
                        create(
                            type = Profile.Type.Url,
                            name = app.getString(R.string.new_profile),
                            source = url,
                        )
                    }
                    launchProperties(uuid)
                }

                QRUserCanceled -> Unit
                QRMissingPermission -> _snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = app.getString(R.string.import_from_qr_no_permission),
                        duration = SnackbarDuration.Long,
                    ),
                )

                is QRError -> _snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = app.getString(R.string.import_from_qr_exception),
                        duration = SnackbarDuration.Long,
                    ),
                )
            }
        }
    }

    fun detailPackageName(providerId: String): String? {
        return (providerById(providerId) as? ProfileProvider.External)?.intent?.component?.packageName
    }

    private suspend fun createProfile(type: Profile.Type, name: String): UUID {
        return withProfile { create(type, name) }
    }

    private suspend fun launchProperties(uuid: UUID) {
        _effects.emit(NewProfileEffect.NavigateToProperties(uuid.toString()))
    }

    private fun providerById(id: String): ProfileProvider? {
        return providers.getOrNull(id.toIntOrNull() ?: -1)
    }

    private fun providerIcon(provider: ProfileProvider): Int {
        return when (provider) {
            is ProfileProvider.File -> R.drawable.ic_baseline_attach_file
            is ProfileProvider.Url -> R.drawable.ic_baseline_cloud_download
            is ProfileProvider.QR -> R.drawable.baseline_qr_code_scanner
            is ProfileProvider.External -> R.drawable.ic_baseline_extension
        }
    }

    private suspend fun queryProfileProviders(): List<ProfileProvider> {
        return withContext(Dispatchers.IO) {
            val externalProviders = app.packageManager.queryIntentActivities(
                Intent(Intents.ACTION_PROVIDE_URL),
                0,
            ).map {
                val activity = it.activityInfo
                val name = activity.applicationInfo.loadLabel(app.packageManager)
                val summary = activity.loadLabel(app.packageManager)
                val icon = activity.loadIcon(app.packageManager)
                val intent = Intent(Intents.ACTION_PROVIDE_URL)
                    .setComponent(ComponentName(activity.packageName, activity.name))

                ProfileProvider.External(name.toString(), summary.toString(), icon, intent)
            }

            listOf(
                ProfileProvider.File(app),
                ProfileProvider.Url(app),
                ProfileProvider.QR(app),
            ) + externalProviders
        }
    }
}

@Composable
fun NewProfileRoute(
    navController: NavController,
    viewModel: NewProfileViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExternalProviderId by remember { mutableStateOf<String?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanQRCode(), viewModel::onScanResult)
    val externalProviderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        pendingExternalProviderId?.let { providerId ->
            viewModel.onExternalProviderResult(providerId, result.resultCode, result.data)
        }
        pendingExternalProviderId = null
    }

    ClashSnackbarEffect(
        messages = viewModel.snackbarMessages,
        snackbarHostState = snackbarHostState,
    )

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                NewProfileEffect.LaunchQrScanner -> scanLauncher.launch(null)
                is NewProfileEffect.LaunchExternalProvider -> {
                    pendingExternalProviderId = effect.providerId
                    externalProviderLauncher.launch(effect.intent)
                }

                is NewProfileEffect.NavigateToProperties -> {
                    navController.navigate(AppRoute.Properties.createRoute(effect.profileId))
                }
            }
        }
    }

    NewProfileScreen(
        title = context.getString(R.string.create_profile),
        state = state,
        onBack = { navController.popBackStack() },
        onCreate = viewModel::onCreate,
        onDetail = { providerId ->
            val packageName = viewModel.detailPackageName(providerId) ?: return@NewProfileScreen
            val data = Uri.fromParts("package", packageName, null)
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(data))
        },
        snackbarHostState = snackbarHostState,
    )
}
