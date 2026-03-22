package com.github.kr328.clash.design.compose

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.AttrRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.github.kr328.clash.design.util.resolveThemedColor

@Composable
fun ClashTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = remember(context, context.resources.configuration) {
        context.clashColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private fun Context.clashColorScheme(): ColorScheme {
    val isDark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    return if (isDark) {
        darkColorScheme(
            primary = themedColor(com.google.android.material.R.attr.colorPrimary),
            onPrimary = themedColor(com.google.android.material.R.attr.colorOnPrimary),
            background = themedColor(android.R.attr.windowBackground),
            surface = themedColor(com.google.android.material.R.attr.colorSurface),
            onSurface = themedColor(com.google.android.material.R.attr.colorControlNormal),
            onBackground = themedColor(com.google.android.material.R.attr.colorControlNormal),
            secondary = themedColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = themedColor(com.google.android.material.R.attr.colorOnSecondary),
        )
    } else {
        lightColorScheme(
            primary = themedColor(com.google.android.material.R.attr.colorPrimary),
            onPrimary = themedColor(com.google.android.material.R.attr.colorOnPrimary),
            background = themedColor(android.R.attr.windowBackground),
            surface = themedColor(android.R.attr.windowBackground),
            onSurface = themedColor(com.google.android.material.R.attr.colorControlNormal),
            onBackground = themedColor(com.google.android.material.R.attr.colorControlNormal),
            secondary = themedColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = themedColor(com.google.android.material.R.attr.colorOnSecondary),
        )
    }
}

private fun Context.themedColor(@AttrRes attr: Int): Color {
    return Color(resolveThemedColor(attr))
}
