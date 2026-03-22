package com.github.kr328.clash.design.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreferenceFormattersTest {
    @Test
    fun `nullableTextSummary prefers placeholder for null`() {
        assertEquals("placeholder", nullableTextSummary(null, "placeholder", "empty"))
    }

    @Test
    fun `nullableTextSummary prefers empty label for blank string`() {
        assertEquals("empty", nullableTextSummary("", "placeholder", "empty"))
    }

    @Test
    fun `nullableTextSummary returns text for non empty value`() {
        assertEquals("7890", nullableTextSummary("7890", "placeholder", "empty"))
    }

    @Test
    fun `collectionSummary reports placeholder empty and count`() {
        assertEquals(
            "placeholder",
            collectionSummary(null, "placeholder", "empty") { "$it elements" },
        )
        assertEquals(
            "empty",
            collectionSummary(0, "placeholder", "empty") { "$it elements" },
        )
        assertEquals(
            "3 elements",
            collectionSummary(3, "placeholder", "empty") { "$it elements" },
        )
    }

    @Test
    fun `dependencyEnabled only disables for false`() {
        assertTrue(dependencyEnabled(null))
        assertTrue(dependencyEnabled(true))
        assertFalse(dependencyEnabled(false))
    }

    @Test
    fun `port converters follow legacy preference behavior`() {
        assertEquals(null, portToText(null))
        assertEquals("", portToText(0))
        assertEquals("9090", portToText(9090))

        assertEquals(null, textToPort(null))
        assertEquals(0, textToPort("abc"))
        assertEquals(8080, textToPort("8080"))
    }
}
