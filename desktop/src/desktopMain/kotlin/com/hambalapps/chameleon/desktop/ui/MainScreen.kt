package com.hambalapps.chameleon.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.ui.dashboard.DashboardScreen
import com.hambalapps.chameleon.desktop.ui.logs.LogsConsoleScreen
import com.hambalapps.chameleon.desktop.ui.navigation.ExpressiveNavRail
import com.hambalapps.chameleon.desktop.ui.nodes.NodeSelectorScreen
import com.hambalapps.chameleon.desktop.ui.settings.SettingsHubScreen
import com.hambalapps.chameleon.desktop.ui.subs.SubscriptionScreen
import com.hambalapps.chameleon.desktop.ui.tools.ToolsHubScreen

/**
 * Main Shell Window for Chameleon Desktop.
 * Host for ExpressiveNavRail and Screen Router.
 */
@Composable
fun MainScreen(
    settingsManager: SettingsManager = remember { SettingsManager() }
) {
    var currentRoute by remember { mutableStateOf("dashboard") }

    Row(modifier = Modifier.fillMaxSize()) {
        // M3 Expressive Side Navigation Rail
        ExpressiveNavRail(
            currentRoute = currentRoute,
            onNavigate = { route -> currentRoute = route }
        )

        // Screen View Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { route ->
                when (route) {
                    "dashboard" -> DashboardScreen(
                        settingsManager = settingsManager,
                        onNavigateToNodes = { currentRoute = "nodes" }
                    )
                    "nodes" -> NodeSelectorScreen(settingsManager = settingsManager)
                    "subs" -> SubscriptionScreen(settingsManager = settingsManager)
                    "tools" -> ToolsHubScreen(settingsManager = settingsManager)
                    "logs" -> LogsConsoleScreen()
                    "settings" -> SettingsHubScreen(settingsManager = settingsManager)
                    else -> DashboardScreen(
                        settingsManager = settingsManager,
                        onNavigateToNodes = { currentRoute = "nodes" }
                    )
                }
            }
        }
    }
}
