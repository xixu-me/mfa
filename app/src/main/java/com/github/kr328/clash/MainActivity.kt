package com.github.kr328.clash

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.ticker
import com.github.kr328.clash.core.bridge.*
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.core.util.trafficTotal
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.main.MainScreen
import com.github.kr328.clash.ui.main.MainSnackbarAction
import com.github.kr328.clash.ui.main.MainSnackbarEvent
import com.github.kr328.clash.ui.main.MainUiState
import com.github.kr328.clash.util.startClashService
import com.github.kr328.clash.util.stopClashService
import com.github.kr328.clash.util.withClash
import com.github.kr328.clash.util.withProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : BaseActivity() {
    private val uiState = MutableStateFlow(MainUiState())
    private val snackbarEvents = MutableSharedFlow<MainSnackbarEvent>(extraBufferCapacity = 8)

    override suspend fun main() {
        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()

            ClashTheme {
                MainScreen(
                    state = state.value,
                    snackbarEvents = snackbarEvents,
                    onToggleStatus = { this@MainActivity.launchToggle() },
                    onOpenProxy = { startActivity(ProxyActivity::class.intent) },
                    onOpenProfiles = { startActivity(ProfilesActivity::class.intent) },
                    onOpenProviders = { startActivity(ProvidersActivity::class.intent) },
                    onOpenLogs = {
                        if (LogcatService.running) {
                            startActivity(LogcatActivity::class.intent)
                        } else {
                            startActivity(LogsActivity::class.intent)
                        }
                    },
                    onOpenSettings = { startActivity(SettingsActivity::class.intent) },
                    onOpenHelp = { startActivity(HelpActivity::class.intent) },
                    onOpenAbout = { this@MainActivity.launchAbout() },
                    onDismissAbout = {
                        uiState.value = uiState.value.copy(aboutVersionName = null)
                    },
                )
            }
        }

        refreshMainState()

        val ticker = ticker(TimeUnit.SECONDS.toMillis(1))

        while (isActive) {
            select<Unit> {
                events.onReceive {
                    when (it) {
                        Event.ActivityStart,
                        Event.ServiceRecreated,
                        Event.ClashStop, Event.ClashStart,
                        Event.ProfileLoaded, Event.ProfileChanged -> refreshMainState()
                        else -> Unit
                    }
                }
                if (clashRunning) {
                    ticker.onReceive {
                        refreshTraffic()
                    }
                }
            }
        }
    }

    private suspend fun refreshMainState() {
        val tunnelState = withClash {
            queryTunnelState()
        }
        val providers = withClash {
            queryProviders()
        }
        val profileName = withProfile {
            queryActive()?.name
        }

        uiState.value = uiState.value.copy(
            clashRunning = clashRunning,
            mode = tunnelState.mode.displayName(this),
            hasProviders = providers.isNotEmpty(),
            profileName = profileName,
        )

        if (clashRunning) {
            refreshTraffic()
        } else {
            uiState.value = uiState.value.copy(forwarded = "0 Bytes")
        }
    }

    private suspend fun refreshTraffic() {
        uiState.value = uiState.value.copy(
            forwarded = withClash {
                queryTrafficTotal().trafficTotal()
            },
        )
    }

    private fun launchToggle() {
        this@MainActivity.launch {
            if (clashRunning) {
                stopClashService()
            } else {
                startClash()
            }
        }
    }

    private fun launchAbout() {
        this@MainActivity.launch {
            uiState.value = uiState.value.copy(
                aboutVersionName = queryAppVersionName(),
            )
        }
    }

    private suspend fun startClash() {
        val active = withProfile { queryActive() }

        if (active == null || !active.imported) {
            snackbarEvents.tryEmit(
                MainSnackbarEvent(
                    message = getString(R.string.no_profile_selected),
                    actionLabel = getString(R.string.profiles),
                    action = MainSnackbarAction.OpenProfiles,
                ),
            )
            return
        }

        val vpnRequest = startClashService()

        try {
            if (vpnRequest != null) {
                val result = startActivityForResult(
                    ActivityResultContracts.StartActivityForResult(),
                    vpnRequest
                )

                if (result.resultCode == RESULT_OK)
                    startClashService()
            }
        } catch (e: Exception) {
            snackbarEvents.tryEmit(
                MainSnackbarEvent(
                    message = getString(R.string.unable_to_start_vpn),
                ),
            )
        }
    }

    private suspend fun queryAppVersionName(): String {
        return withContext(Dispatchers.IO) {
            packageManager.getPackageInfo(packageName, 0).versionName + "\n" + Bridge.nativeCoreVersion().replace("_", "-")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher =
                registerForActivityResult(RequestPermission()
                ) { isGranted: Boolean ->
                }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private fun TunnelState.Mode.displayName(activity: MainActivity): String {
    return when (this) {
        TunnelState.Mode.Direct -> activity.getString(R.string.direct_mode)
        TunnelState.Mode.Global -> activity.getString(R.string.global_mode)
        TunnelState.Mode.Rule,
        TunnelState.Mode.Script -> activity.getString(R.string.rule_mode)
    }
}

val mainActivityAlias = "${MainActivity::class.java.name}Alias"
