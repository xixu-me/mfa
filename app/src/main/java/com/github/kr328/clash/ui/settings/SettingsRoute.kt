package com.github.kr328.clash.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableSupportingPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberSupportingPaneScaffoldNavigator
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.github.kr328.clash.design.R
import com.github.kr328.clash.ui.accesscontrol.AccessControlRoute
import com.github.kr328.clash.ui.app.AppRoute
import com.github.kr328.clash.ui.settings.app.AppSettingsRoute
import com.github.kr328.clash.ui.settings.meta.MetaFeatureSettingsRoute
import com.github.kr328.clash.ui.settings.network.NetworkSettingsRoute
import com.github.kr328.clash.ui.settings.override.OverrideSettingsRoute
import kotlinx.coroutines.launch

enum class SettingsPane(val route: AppRoute) {
    App(AppRoute.AppSettings),
    Network(AppRoute.NetworkSettings),
    Override(AppRoute.OverrideSettings),
    Meta(AppRoute.MetaFeatureSettings),
    ;

    companion object {
        fun fromRoute(route: String): SettingsPane {
            return entries.firstOrNull { it.route.route == route } ?: App
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SettingsRoute(
    navController: NavController,
    widthSizeClass: WindowWidthSizeClass,
) {
    val context = LocalContext.current
    val scaffoldNavigator = rememberSupportingPaneScaffoldNavigator()
    val scope = rememberCoroutineScope()
    var selectedPaneRoute by rememberSaveable { mutableStateOf(SettingsPane.App.route.route) }
    val backBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange
    val compactLayout = widthSizeClass == WindowWidthSizeClass.Compact

    LaunchedEffect(widthSizeClass) {
        if (!compactLayout) {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        }
    }

    fun openPane(pane: SettingsPane) {
        selectedPaneRoute = pane.route.route
        scope.launch {
            scaffoldNavigator.navigateTo(SupportingPaneScaffoldRole.Supporting)
        }
    }

    val selectedPane = SettingsPane.fromRoute(selectedPaneRoute)

    NavigableSupportingPaneScaffold(
        navigator = scaffoldNavigator,
        mainPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                SettingsScreen(
                    title = context.getString(R.string.settings),
                    onBack = { navController.popBackStack() },
                    onOpenApp = { openPane(SettingsPane.App) },
                    onOpenNetwork = { openPane(SettingsPane.Network) },
                    onOpenOverride = { openPane(SettingsPane.Override) },
                    onOpenMetaFeature = { openPane(SettingsPane.Meta) },
                )
            }
        },
        supportingPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                when (selectedPane) {
                    SettingsPane.App -> AppSettingsRoute(
                        onBack = supportingPaneBackHandler(
                            compactLayout = compactLayout,
                            onBack = { scope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
                        ),
                    )

                    SettingsPane.Network -> NetworkSettingsRoute(
                        navController = navController,
                        onBack = supportingPaneBackHandler(
                            compactLayout = compactLayout,
                            onBack = { scope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
                        ),
                    )

                    SettingsPane.Override -> OverrideSettingsRoute(
                        onBack = supportingPaneBackHandler(
                            compactLayout = compactLayout,
                            onBack = { scope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
                        ),
                    )

                    SettingsPane.Meta -> MetaFeatureSettingsRoute(
                        onBack = supportingPaneBackHandler(
                            compactLayout = compactLayout,
                            onBack = { scope.launch { scaffoldNavigator.navigateBack(backBehavior) } },
                        ),
                    )
                }
            }
        },
    )
}

internal fun supportingPaneBackHandler(
    compactLayout: Boolean,
    onBack: () -> Unit,
): (() -> Unit)? {
    return if (compactLayout) onBack else null
}

@Composable
fun AccessControlSettingsRoute(
    navController: NavController,
    onBack: (() -> Unit)?,
) {
    AccessControlRoute(
        navController = navController,
        onBack = onBack,
    )
}
