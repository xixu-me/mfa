package com.github.kr328.clash.ui.appcrashed

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.kr328.clash.design.compose.ClashScaffold
import com.github.kr328.clash.design.compose.ClashTheme

@Composable
fun AppCrashedScreen(
    title: String,
    state: AppCrashedUiState,
    onBack: () -> Unit,
) {
    ClashScaffold(
        title = title,
        onBack = onBack,
    ) { padding ->
        AppCrashedContent(
            state = state,
            padding = padding,
        )
    }
}

@Composable
private fun AppCrashedContent(
    state: AppCrashedUiState,
    padding: PaddingValues,
) {
    SelectionContainer {
        Text(
            text = state.logs,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Preview
@Composable
private fun AppCrashedScreenPreview() {
    ClashTheme {
        AppCrashedScreen(
            title = "Application Crashed",
            state = AppCrashedUiState(
                logs = "java.lang.IllegalStateException: boom\n    at com.example.Main.main(Main.kt:12)",
            ),
            onBack = {},
        )
    }
}
