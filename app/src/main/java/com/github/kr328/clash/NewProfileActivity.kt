package com.github.kr328.clash

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.github.kr328.clash.common.constants.Intents
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.setUUID
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashSnackbarEffect
import com.github.kr328.clash.design.compose.ClashSnackbarMessage
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.model.ProfileProvider
import com.github.kr328.clash.service.model.Profile
import com.github.kr328.clash.ui.newprofile.NewProfileProviderUiState
import com.github.kr328.clash.ui.newprofile.NewProfileScreen
import com.github.kr328.clash.ui.newprofile.NewProfileUiState
import com.github.kr328.clash.util.withProfile
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.QRResult.QRError
import io.github.g00fy2.quickie.QRResult.QRMissingPermission
import io.github.g00fy2.quickie.QRResult.QRSuccess
import io.github.g00fy2.quickie.QRResult.QRUserCanceled
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class NewProfileActivity : BaseActivity() {
    private val self: NewProfileActivity
        get() = this

    private val uiState = MutableStateFlow(NewProfileUiState())
    private val snackbarMessages = MutableSharedFlow<ClashSnackbarMessage>(extraBufferCapacity = 4)
    private var providers: List<ProfileProvider> = emptyList()
    private val scanLauncher = registerForActivityResult(ScanQRCode(), ::scanResultHandler)

    override suspend fun main() {
        providers = queryProfileProviders()
        uiState.value = NewProfileUiState(
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

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashSnackbarEffect(
                messages = snackbarMessages,
                snackbarHostState = snackbarHostState,
            )

            ClashTheme {
                NewProfileScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onCreate = { providerId ->
                        this@NewProfileActivity.launch {
                            handleCreate(providerById(providerId) ?: return@launch)
                        }
                    },
                    onDetail = { providerId ->
                        val provider = providerById(providerId) as? ProfileProvider.External ?: return@NewProfileScreen
                        launchAppDetailed(provider)
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        awaitCancellation()
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

    private suspend fun handleCreate(provider: ProfileProvider) {
        withProfile {
            val name = getString(R.string.new_profile)

            val uuid: UUID? = when (provider) {
                is ProfileProvider.File -> create(Profile.Type.File, name)
                is ProfileProvider.Url -> create(Profile.Type.Url, name)
                is ProfileProvider.QR -> {
                    scanLauncher.launch(null)
                    null
                }
                is ProfileProvider.External -> {
                    val data = provider.get()
                    if (data != null) {
                        val (uri, initialName) = data
                        create(Profile.Type.External, initialName ?: name, uri.toString())
                    } else {
                        null
                    }
                }
            }

            if (uuid != null) {
                launchProperties(uuid)
            }
        }
    }

    private fun launchAppDetailed(provider: ProfileProvider.External) {
        val data = Uri.fromParts(
            "package",
            provider.intent.component?.packageName ?: return,
            null,
        )

        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(data))
    }

    private suspend fun launchProperties(uuid: UUID) {
        val result = startActivityForResult(
            ActivityResultContracts.StartActivityForResult(),
            PropertiesActivity::class.intent.setUUID(uuid),
        )

        if (result.resultCode == Activity.RESULT_OK) {
            finish()
        }
    }

    private suspend fun ProfileProvider.External.get(): Pair<Uri, String?>? {
        val result = startActivityForResult(
            ActivityResultContracts.StartActivityForResult(),
            intent,
        )

        if (result.resultCode != RESULT_OK) {
            return null
        }

        val uri = result.data?.data
        val name = result.data?.getStringExtra(Intents.EXTRA_NAME)
        return if (uri != null) uri to name else null
    }

    private suspend fun queryProfileProviders(): List<ProfileProvider> {
        return withContext(Dispatchers.IO) {
            val externalProviders = packageManager.queryIntentActivities(
                Intent(Intents.ACTION_PROVIDE_URL),
                0,
            ).map {
                val activity = it.activityInfo
                val name = activity.applicationInfo.loadLabel(packageManager)
                val summary = activity.loadLabel(packageManager)
                val icon = activity.loadIcon(packageManager)
                val intent = Intent(Intents.ACTION_PROVIDE_URL)
                    .setComponent(ComponentName(activity.packageName, activity.name))

                ProfileProvider.External(name.toString(), summary.toString(), icon, intent)
            }

            listOf(
                ProfileProvider.File(self),
                ProfileProvider.Url(self),
                ProfileProvider.QR(self),
            ) + externalProviders
        }
    }

    private fun scanResultHandler(result: QRResult) {
        lifecycleScope.launch {
            when (result) {
                is QRSuccess -> {
                    val url = result.content.rawValue
                        ?: result.content.rawBytes?.let { String(it) }.orEmpty()
                    createProfileByQrCode(url)
                }
                QRUserCanceled -> Unit
                QRMissingPermission -> snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = getString(R.string.import_from_qr_no_permission),
                        duration = SnackbarDuration.Long,
                    ),
                )
                is QRError -> snackbarMessages.emit(
                    ClashSnackbarMessage(
                        message = getString(R.string.import_from_qr_exception),
                        duration = SnackbarDuration.Long,
                    ),
                )
            }
        }
    }

    private suspend fun createProfileByQrCode(url: String) {
        withProfile {
            launchProperties(
                create(
                    type = Profile.Type.Url,
                    name = getString(R.string.new_profile),
                    source = url,
                ),
            )
        }
    }
}
