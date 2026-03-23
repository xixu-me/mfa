package com.github.kr328.clash.ui.files

import com.github.kr328.clash.design.model.File
import kotlin.test.Test
import kotlin.test.assertEquals

class FilesRouteContractTest {
    @Test
    fun `base directory only shows empty config when it is the only editable entry`() {
        val files = listOf(
            File(
                id = "profiles/123/config.yaml",
                name = "config.yaml",
                size = 0,
                lastModified = 0,
                isDirectory = false,
            ),
            File(
                id = "profiles/123/providers",
                name = "providers",
                size = 0,
                lastModified = 0,
                isDirectory = true,
            ),
        )

        assertEquals(listOf(files.first()), filterFilesForDisplay(files, inBaseDir = true))
    }

    @Test
    fun `base directory keeps full listing when config already has content`() {
        val files = listOf(
            File(
                id = "profiles/123/config.yaml",
                name = "config.yaml",
                size = 12,
                lastModified = 0,
                isDirectory = false,
            ),
            File(
                id = "profiles/123/providers",
                name = "providers",
                size = 0,
                lastModified = 0,
                isDirectory = true,
            ),
        )

        assertEquals(files, filterFilesForDisplay(files, inBaseDir = true))
    }

    @Test
    fun `nested directory keeps full listing`() {
        val files = listOf(
            File(
                id = "profiles/123/providers",
                name = "providers",
                size = 0,
                lastModified = 0,
                isDirectory = true,
            ),
            File(
                id = "profiles/123/providers/a.yaml",
                name = "a.yaml",
                size = 128,
                lastModified = 0,
                isDirectory = false,
            ),
        )

        assertEquals(files, filterFilesForDisplay(files, inBaseDir = false))
    }
}
