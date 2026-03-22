package com.github.kr328.clash

import android.content.Intent
import androidx.core.net.toUri
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.apkbroken.ApkBrokenScreen
import com.github.kr328.clash.ui.apkbroken.ApkBrokenUiState
import kotlinx.coroutines.isActive

class ApkBrokenActivity : BaseActivity() {
    override suspend fun main() {
        val state = ApkBrokenUiState(
            tips = getString(com.github.kr328.clash.design.R.string.application_broken_tips),
            sectionTitle = getString(com.github.kr328.clash.design.R.string.reinstall),
            releaseTitle = getString(com.github.kr328.clash.design.R.string.github_releases),
            releaseUrl = getString(com.github.kr328.clash.design.R.string.meta_github_url),
        )

        setComposeContent {
            ClashTheme {
                ApkBrokenScreen(
                    title = title?.toString().orEmpty(),
                    state = state,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onOpenReleasePage = {
                        startActivity(Intent(Intent.ACTION_VIEW).setData(it.toUri()))
                    },
                )
            }
        }

        while (isActive) {
            events.receive()
        }
    }
}
