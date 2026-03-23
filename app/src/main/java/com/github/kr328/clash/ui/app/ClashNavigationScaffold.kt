package com.github.kr328.clash.ui.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination

@Composable
fun ClashNavigationScaffold(
    appState: ClashAppState,
    currentDestination: NavDestination?,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        modifier = Modifier.fillMaxSize(),
        navigationSuiteItems = {
            topLevelDestinations.forEach { destination ->
                item(
                    selected = appState.isTopLevelDestination(currentDestination, destination.route),
                    onClick = { appState.navigateToTopLevel(destination.route) },
                    icon = {
                        Icon(
                            painter = painterResource(destination.iconRes),
                            contentDescription = stringResource(destination.route.titleRes),
                        )
                    },
                    label = {
                        Text(text = stringResource(destination.route.titleRes))
                    },
                )
            }
        },
    ) {
        content()
    }
}
