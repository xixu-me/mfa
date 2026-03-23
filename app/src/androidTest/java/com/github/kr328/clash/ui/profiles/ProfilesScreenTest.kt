package com.github.kr328.clash.ui.profiles

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.kr328.clash.design.compose.ClashTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfilesScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun clickingProfileRowInvokesSelectionCallback() {
        var selectedProfileId = ""

        composeRule.setContent {
            ClashTheme {
                ProfilesScreen(
                    title = "Profiles",
                    state = ProfilesUiState(
                        profiles = listOf(
                            ProfileItemUiState(
                                id = "profile-1",
                                name = "Example",
                                typeSummary = "URL",
                                trafficSummary = null,
                                expireSummary = null,
                                elapsedSummary = "now",
                                active = false,
                                usageProgress = null,
                                canUpdate = true,
                                canDuplicate = true,
                            ),
                        ),
                    ),
                    onBack = {},
                    onCreate = {},
                    onUpdateAll = {},
                    onSelect = { selectedProfileId = it },
                    onActivate = {},
                    onOpenMenu = {},
                    onDismissMenu = {},
                    onUpdate = {},
                    onEdit = {},
                    onDuplicate = {},
                    onDelete = {},
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                )
            }
        }

        composeRule.onNodeWithText("Example").performClick()
        assertEquals("profile-1", selectedProfileId)
    }

    @Test
    fun overflowMenuButtonKeepsAccessibleLabel() {
        composeRule.setContent {
            ClashTheme {
                ProfilesScreen(
                    title = "Profiles",
                    state = ProfilesUiState(
                        profiles = listOf(
                            ProfileItemUiState(
                                id = "profile-1",
                                name = "Example",
                                typeSummary = "URL",
                                trafficSummary = null,
                                expireSummary = null,
                                elapsedSummary = "now",
                                active = false,
                                usageProgress = null,
                                canUpdate = true,
                                canDuplicate = true,
                            ),
                        ),
                        activeMenuProfileId = "profile-1",
                    ),
                    onBack = {},
                    onCreate = {},
                    onUpdateAll = {},
                    onSelect = {},
                    onActivate = {},
                    onOpenMenu = {},
                    onDismissMenu = {},
                    onUpdate = {},
                    onEdit = {},
                    onDuplicate = {},
                    onDelete = {},
                    snackbarHostState = androidx.compose.material3.SnackbarHostState(),
                )
            }
        }

        composeRule.onNodeWithContentDescription("More").assertHasClickAction()
    }
}
