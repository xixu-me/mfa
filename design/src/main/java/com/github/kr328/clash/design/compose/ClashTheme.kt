package com.github.kr328.clash.design.compose

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.AttrRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.github.kr328.clash.design.model.DarkMode
import com.github.kr328.clash.design.store.UiStore
import com.github.kr328.clash.design.util.resolveThemedColor

@Composable
fun ClashTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = remember(context, context.resources.configuration) {
        context.isDarkTheme()
    }
    val colorScheme = remember(context, context.resources.configuration, darkTheme) {
        context.clashColorScheme(darkTheme = darkTheme)
    }

    SideEffect {
        val activity = context as? Activity ?: return@SideEffect
        activity.window.statusBarColor = colorScheme.surface.toArgb()
        activity.window.navigationBarColor = colorScheme.surface.toArgb()
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }

    CompositionLocalProvider(LocalClashSpacing provides ClashSpacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = ClashShapes,
            typography = ClashTypography,
            content = content,
        )
    }
}

private fun Context.isDarkTheme(): Boolean {
    return when (UiStore(this).darkMode) {
        DarkMode.Auto -> {
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        DarkMode.ForceLight -> false
        DarkMode.ForceDark -> true
    }
}

private fun Context.clashColorScheme(darkTheme: Boolean): ColorScheme {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return if (darkTheme) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
    }

    return if (darkTheme) {
        darkColorScheme(
            primary = themedColor(com.google.android.material.R.attr.colorPrimary),
            onPrimary = themedColor(com.google.android.material.R.attr.colorOnPrimary),
            background = themedColor(android.R.attr.windowBackground),
            surface = themedColor(com.google.android.material.R.attr.colorSurface),
            surfaceVariant = themedColor(com.google.android.material.R.attr.colorSurfaceVariant),
            onSurface = themedColor(com.google.android.material.R.attr.colorOnSurface),
            onSurfaceVariant = themedColor(com.google.android.material.R.attr.colorControlNormal),
            onBackground = themedColor(com.google.android.material.R.attr.colorControlNormal),
            secondary = themedColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = themedColor(com.google.android.material.R.attr.colorOnSecondary),
            tertiary = themedColor(com.google.android.material.R.attr.colorTertiary),
            onTertiary = themedColor(com.google.android.material.R.attr.colorOnTertiary),
        )
    } else {
        lightColorScheme(
            primary = themedColor(com.google.android.material.R.attr.colorPrimary),
            onPrimary = themedColor(com.google.android.material.R.attr.colorOnPrimary),
            background = themedColor(android.R.attr.windowBackground),
            surface = themedColor(android.R.attr.windowBackground),
            surfaceVariant = themedColor(com.google.android.material.R.attr.colorSurfaceVariant),
            onSurface = themedColor(com.google.android.material.R.attr.colorOnSurface),
            onSurfaceVariant = themedColor(com.google.android.material.R.attr.colorControlNormal),
            onBackground = themedColor(com.google.android.material.R.attr.colorControlNormal),
            secondary = themedColor(com.google.android.material.R.attr.colorSecondary),
            onSecondary = themedColor(com.google.android.material.R.attr.colorOnSecondary),
            tertiary = themedColor(com.google.android.material.R.attr.colorTertiary),
            onTertiary = themedColor(com.google.android.material.R.attr.colorOnTertiary),
        )
    }
}

private fun Context.themedColor(@AttrRes attr: Int): Color = Color(resolveThemedColor(attr))

private val ClashTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
)

private val ClashShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

data class ClashSpacing(
    val xSmall: androidx.compose.ui.unit.Dp = 4.dp,
    val small: androidx.compose.ui.unit.Dp = 8.dp,
    val medium: androidx.compose.ui.unit.Dp = 16.dp,
    val large: androidx.compose.ui.unit.Dp = 24.dp,
    val xLarge: androidx.compose.ui.unit.Dp = 32.dp,
)
