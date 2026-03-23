package com.github.kr328.clash.ui.profiles

import com.github.kr328.clash.ui.properties.PropertiesUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertiesRouteContractTest {
    @Test
    fun `back navigates immediately when no unsaved changes exist`() {
        assertTrue(
            shouldNavigateBack(
                PropertiesUiState(
                    hasUnsavedChanges = false,
                    isProcessing = false,
                ),
            ),
        )
    }

    @Test
    fun `back waits for confirmation when unsaved changes exist`() {
        assertFalse(
            shouldNavigateBack(
                PropertiesUiState(
                    hasUnsavedChanges = true,
                    isProcessing = false,
                ),
            ),
        )
    }

    @Test
    fun `back is blocked while save is in progress`() {
        assertFalse(
            shouldNavigateBack(
                PropertiesUiState(
                    hasUnsavedChanges = false,
                    isProcessing = true,
                ),
            ),
        )
    }
}
