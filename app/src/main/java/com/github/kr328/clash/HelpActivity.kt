package com.github.kr328.clash

import android.content.Intent
import androidx.core.net.toUri
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.help.HelpLinkItemUiState
import com.github.kr328.clash.ui.help.HelpScreen
import com.github.kr328.clash.ui.help.HelpSectionUiState
import com.github.kr328.clash.ui.help.HelpUiState
import kotlinx.coroutines.isActive

class HelpActivity : BaseActivity() {
    override suspend fun main() {
        val state = HelpUiState(
            tips = getString(com.github.kr328.clash.design.R.string.tips_help),
            sections = listOf(
                HelpSectionUiState(
                    title = getString(com.github.kr328.clash.design.R.string.document),
                    items = listOf(
                        HelpLinkItemUiState(
                            title = getString(com.github.kr328.clash.design.R.string.clash_wiki),
                            summary = getString(com.github.kr328.clash.design.R.string.clash_wiki_url),
                            url = getString(com.github.kr328.clash.design.R.string.clash_wiki_url),
                        ),
                        HelpLinkItemUiState(
                            title = getString(com.github.kr328.clash.design.R.string.clash_meta_wiki),
                            summary = getString(com.github.kr328.clash.design.R.string.clash_meta_wiki_url),
                            url = getString(com.github.kr328.clash.design.R.string.clash_meta_wiki_url),
                        ),
                    ),
                ),
                HelpSectionUiState(
                    title = getString(com.github.kr328.clash.design.R.string.sources),
                    items = listOf(
                        HelpLinkItemUiState(
                            title = getString(com.github.kr328.clash.design.R.string.clash_meta_core),
                            summary = getString(com.github.kr328.clash.design.R.string.clash_meta_core_url),
                            url = getString(com.github.kr328.clash.design.R.string.clash_meta_core_url),
                        ),
                        HelpLinkItemUiState(
                            title = getString(com.github.kr328.clash.design.R.string.clash_meta_for_android),
                            summary = getString(com.github.kr328.clash.design.R.string.meta_github_url),
                            url = getString(com.github.kr328.clash.design.R.string.meta_github_url),
                        ),
                    ),
                ),
            ),
        )

        setComposeContent {
            ClashTheme {
                HelpScreen(
                    title = title?.toString().orEmpty(),
                    state = state,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onOpenLink = {
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
