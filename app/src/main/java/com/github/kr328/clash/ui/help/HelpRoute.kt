package com.github.kr328.clash.ui.help

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.github.kr328.clash.design.R

@Composable
fun HelpRoute(navController: NavController) {
    val context = LocalContext.current
    val state = HelpUiState(
        tips = context.getString(R.string.tips_help),
        sections = listOf(
            HelpSectionUiState(
                title = context.getString(R.string.document),
                items = listOf(
                    HelpLinkItemUiState(
                        title = context.getString(R.string.clash_wiki),
                        summary = context.getString(R.string.clash_wiki_url),
                        url = context.getString(R.string.clash_wiki_url),
                    ),
                    HelpLinkItemUiState(
                        title = context.getString(R.string.clash_meta_wiki),
                        summary = context.getString(R.string.clash_meta_wiki_url),
                        url = context.getString(R.string.clash_meta_wiki_url),
                    ),
                ),
            ),
            HelpSectionUiState(
                title = context.getString(R.string.sources),
                items = listOf(
                    HelpLinkItemUiState(
                        title = context.getString(R.string.clash_meta_core),
                        summary = context.getString(R.string.clash_meta_core_url),
                        url = context.getString(R.string.clash_meta_core_url),
                    ),
                    HelpLinkItemUiState(
                        title = context.getString(R.string.clash_meta_for_android),
                        summary = context.getString(R.string.meta_github_url),
                        url = context.getString(R.string.meta_github_url),
                    ),
                ),
            ),
        ),
    )

    HelpScreen(
        title = context.getString(R.string.help),
        state = state,
        onBack = { navController.popBackStack() },
        onOpenLink = {
            context.startActivity(Intent(Intent.ACTION_VIEW).setData(it.toUri()))
        },
    )
}
