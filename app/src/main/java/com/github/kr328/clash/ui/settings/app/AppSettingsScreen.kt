package com.github.kr328.clash.ui.settings.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.design.compose.PreferenceGroup
import com.github.kr328.clash.design.compose.PreferenceOption
import com.github.kr328.clash.design.compose.PreferenceSelectableItem
import com.github.kr328.clash.design.compose.PreferenceSwitchItem
import com.github.kr328.clash.design.model.DarkMode

@Composable
fun AppSettingsScreen(
    title: String,
    state: AppSettingsUiState,
    onBack: () -> Unit,
    onAutoRestartChange: (Boolean) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
    onHideAppIconChange: (Boolean) -> Unit,
    onHideFromRecentsChange: (Boolean) -> Unit,
    onDynamicNotificationChange: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceGroup(title = stringResource(id = R.string.behavior)) {
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.auto_restart),
                    summary = stringResource(id = R.string.allow_clash_auto_restart),
                    checked = state.autoRestart,
                    onCheckedChange = onAutoRestartChange,
                    iconRes = R.drawable.ic_baseline_restore,
                )
            }

            PreferenceGroup(title = stringResource(id = R.string.interface_)) {
                PreferenceSelectableItem(
                    title = stringResource(id = R.string.dark_mode),
                    value = state.darkMode,
                    options = listOf(
                        PreferenceOption(DarkMode.Auto, stringResource(id = R.string.follow_system_android_10)),
                        PreferenceOption(DarkMode.ForceLight, stringResource(id = R.string.always_light)),
                        PreferenceOption(DarkMode.ForceDark, stringResource(id = R.string.always_dark)),
                    ),
                    onSelected = onDarkModeChange,
                    iconRes = R.drawable.ic_baseline_brightness_4,
                )
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.hide_app_icon_title),
                    summary = stringResource(id = R.string.hide_app_icon_desc),
                    checked = state.hideAppIcon,
                    onCheckedChange = onHideAppIconChange,
                    iconRes = R.drawable.ic_baseline_hide,
                )
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.hide_from_recents_title),
                    summary = stringResource(id = R.string.hide_from_recents_desc),
                    checked = state.hideFromRecents,
                    onCheckedChange = onHideFromRecentsChange,
                    iconRes = R.drawable.ic_baseline_stack,
                )
            }

            PreferenceGroup(title = stringResource(id = R.string.service)) {
                PreferenceSwitchItem(
                    title = stringResource(id = R.string.show_traffic),
                    summary = stringResource(id = R.string.show_traffic_summary),
                    checked = state.dynamicNotification,
                    onCheckedChange = onDynamicNotificationChange,
                    enabled = !state.running,
                    iconRes = R.drawable.ic_baseline_domain,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AppSettingsScreenPreview() {
    ClashTheme {
        AppSettingsScreen(
            title = "App Settings",
            state = AppSettingsUiState(),
            onBack = {},
            onAutoRestartChange = {},
            onDarkModeChange = {},
            onHideAppIconChange = {},
            onHideFromRecentsChange = {},
            onDynamicNotificationChange = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
