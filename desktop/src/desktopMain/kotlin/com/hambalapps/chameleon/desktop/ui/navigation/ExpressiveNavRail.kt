package com.hambalapps.chameleon.desktop.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.ui.components.expressiveSpringPress

data class NavItem(
    val id: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val DesktopNavItems = listOf(
    NavItem("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    NavItem("nodes", "Nodes", Icons.Filled.Dns, Icons.Outlined.Dns),
    NavItem("subs", "Subscriptions", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
    NavItem("tools", "Tools", Icons.Filled.Build, Icons.Outlined.Build),
    NavItem("logs", "Logs", Icons.Filled.Terminal, Icons.Outlined.Terminal),
    NavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

/**
 * Material 3 Expressive Side Navigation Rail with animated selection pill.
 */
@Composable
fun ExpressiveNavRail(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .width(88.dp)
            .fillMaxHeight()
            .background(colorScheme.surfaceContainerLowest)
            .border(
                width = 1.dp,
                color = colorScheme.outline.copy(alpha = 0.15f),
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            )
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo Icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colorScheme.primaryContainer)
        ) {
            Icon(
                imageVector = Icons.Filled.VpnKey,
                contentDescription = "Chameleon Logo",
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigation Items List
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DesktopNavItems.forEach { item ->
                val isSelected = item.id == currentRoute

                val pillBgColor by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "NavPillBg"
                )

                val iconColor by animateColorAsState(
                    targetValue = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "NavIconColor"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .expressiveSpringPress { onNavigate(item.id) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .width(56.dp)
                            .height(36.dp)
                            .clip(CircleShape)
                            .background(pillBgColor)
                    ) {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            tint = iconColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        color = iconColor,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Version Badge
        Text(
            text = "v1.5.0",
            color = colorScheme.outline.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
