package com.hambalapps.chameleon.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val settings by settingsManager.settings.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    // Layered Ambient Surface with radial accent glow
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        // Subtle ambient radial glow in top right
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primaryContainer.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f),
                    radius = size.width * 0.5f
                )
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // M3 Expressive Side Navigation Rail
            ExpressiveNavRail(
                currentRoute = currentRoute,
                onNavigate = { route -> currentRoute = route }
            )

            // Screen View Content Area with spring spatial transition
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        (fadeIn(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) +
                            slideInHorizontally(
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = 0.8f,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                ),
                                initialOffsetX = { 40 }
                            ))
                            .togetherWith(
                                fadeOut(animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
                            )
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
}
