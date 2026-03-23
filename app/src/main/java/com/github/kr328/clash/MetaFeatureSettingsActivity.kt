package com.github.kr328.clash

import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.model.ConfigurationOverride
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.compose.ClashTheme
import com.github.kr328.clash.ui.settings.meta.MetaFeatureSettingsScreen
import com.github.kr328.clash.ui.settings.meta.MetaFeatureSettingsUiState
import com.github.kr328.clash.util.clashDir
import com.github.kr328.clash.util.withClash
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MetaFeatureSettingsActivity : BaseActivity() {
    private lateinit var configuration: ConfigurationOverride
    private val uiState = MutableStateFlow(MetaFeatureSettingsUiState(configuration = ConfigurationOverride()))

    override suspend fun main() {
        configuration = withClash { queryOverride(Clash.OverrideSlot.Persist) }

        defer {
            withClash {
                patchOverride(Clash.OverrideSlot.Persist, configuration)
            }
        }

        refreshState()

        setComposeContent {
            val state = uiState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            ClashTheme {
                MetaFeatureSettingsScreen(
                    title = title?.toString().orEmpty(),
                    state = state.value,
                    onBack = onBackPressedDispatcher::onBackPressed,
                    onResetRequested = {
                        uiState.value = uiState.value.copy(showResetConfirm = true)
                    },
                    onResetDismissed = {
                        uiState.value = uiState.value.copy(showResetConfirm = false)
                    },
                    onResetConfirmed = {
                        defer {
                            withClash {
                                clearOverride(Clash.OverrideSlot.Persist)
                            }
                        }
                        finish()
                    },
                    onConfigurationChanged = {
                        configuration.apply(it)
                        refreshState()
                    },
                    onImportGeoIp = {
                        this@MetaFeatureSettingsActivity.launch {
                            importGeoFile(pickFile(), ImportKind.GeoIp)
                        }
                    },
                    onImportGeoSite = {
                        this@MetaFeatureSettingsActivity.launch {
                            importGeoFile(pickFile(), ImportKind.GeoSite)
                        }
                    },
                    onImportCountry = {
                        this@MetaFeatureSettingsActivity.launch {
                            importGeoFile(pickFile(), ImportKind.Country)
                        }
                    },
                    onImportASN = {
                        this@MetaFeatureSettingsActivity.launch {
                            importGeoFile(pickFile(), ImportKind.ASN)
                        }
                    },
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        awaitCancellation()
    }

    private suspend fun pickFile(): Uri? {
        return startActivityForResult(ActivityResultContracts.GetContent(), "*/*")
    }

    private val validDatabaseExtensions = listOf(".metadb", ".db", ".dat", ".mmdb")

    private suspend fun importGeoFile(uri: Uri?, importKind: ImportKind) {
        val cursor: Cursor? = uri?.let {
            contentResolver.query(it, null, null, null, null, null)
        }
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val displayName = if (columnIndex != -1) it.getString(columnIndex) else ""
                val ext = "." + displayName.substringAfterLast(".")

                if (!validDatabaseExtensions.contains(ext)) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.geofile_unknown_db_format)
                        .setMessage(
                            getString(
                                R.string.geofile_unknown_db_format_message,
                                validDatabaseExtensions.joinToString("/"),
                            ),
                        )
                        .setPositiveButton(R.string.ok) { _, _ -> }
                        .show()
                    return
                }

                val outputFileName = when (importKind) {
                    ImportKind.GeoIp -> "geoip$ext"
                    ImportKind.GeoSite -> "geosite$ext"
                    ImportKind.Country -> "country$ext"
                    ImportKind.ASN -> "ASN$ext"
                }

                withContext(Dispatchers.IO) {
                    val outputFile = File(clashDir, outputFileName)
                    contentResolver.openInputStream(uri).use { ins ->
                        FileOutputStream(outputFile).use { outs ->
                            ins?.copyTo(outs)
                        }
                    }
                }
                Toast.makeText(
                    this,
                    getString(R.string.geofile_imported, displayName),
                    Toast.LENGTH_LONG,
                ).show()
                return
            }
        }
        Toast.makeText(this, R.string.geofile_import_failed, Toast.LENGTH_LONG).show()
    }

    private fun refreshState() {
        uiState.value = MetaFeatureSettingsUiState(
            configuration = configuration,
            showResetConfirm = uiState.value.showResetConfirm,
        )
    }

    private enum class ImportKind {
        GeoIp,
        GeoSite,
        Country,
        ASN,
    }
}
