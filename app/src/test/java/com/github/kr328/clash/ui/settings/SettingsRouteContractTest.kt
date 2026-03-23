package com.github.kr328.clash.ui.settings

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SettingsRouteContractTest {
    @Test
    fun `compact layout keeps supporting pane back handler`() {
        val handler = supportingPaneBackHandler(compactLayout = true) {}

        assertNotNull(handler)
    }

    @Test
    fun `expanded layout hides supporting pane back handler`() {
        val handler = supportingPaneBackHandler(compactLayout = false) {}

        assertNull(handler)
    }
}
