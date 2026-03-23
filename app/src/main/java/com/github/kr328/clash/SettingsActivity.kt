package com.github.kr328.clash

import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.settings.SettingsScreen
import kotlinx.coroutines.isActive

class SettingsActivity : BaseActivity() {
    override suspend fun main() {
        setComposeContent {
            ClashTheme {
                SettingsScreen(
                    title = title?.toString().orEmpty(),
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onOpenApp = { startActivity(AppSettingsActivity::class.intent) },
                    onOpenNetwork = { startActivity(NetworkSettingsActivity::class.intent) },
                    onOpenOverride = { startActivity(OverrideSettingsActivity::class.intent) },
                    onOpenMetaFeature = { startActivity(MetaFeatureSettingsActivity::class.intent) },
                )
            }
        }

        while (isActive) {
            events.receive()
        }
    }
}
