package com.github.kr328.clash.ui.accesscontrol

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.kr328.clash.design.compose.ClashTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessControlScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun topBarActionsExposeAccessibleLabels() {
        composeRule.setContent {
            ClashTheme {
                AccessControlScreen(
                    title = "Access Control",
                    state = AccessControlUiState(),
                    onBack = {},
                    onToggleApp = {},
                    onSelectAll = {},
                    onSelectNone = {},
                    onSelectInvert = {},
                    onToggleShowSystemApps = {},
                    onSetSort = {},
                    onToggleReverse = {},
                    onImportClipboard = {},
                    onExportClipboard = {},
                    onOpenSearch = {},
                    onCloseSearch = {},
                    onSearchQueryChanged = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Search").assertHasClickAction()
        composeRule.onNodeWithContentDescription("More").assertHasClickAction()
    }
}
