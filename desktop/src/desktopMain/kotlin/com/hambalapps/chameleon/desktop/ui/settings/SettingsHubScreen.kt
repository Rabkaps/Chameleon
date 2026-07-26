package com.hambalapps.chameleon.desktop.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.ui.components.expressiveSpringPress
import com.hambalapps.chameleon.desktop.updateStartupRegistry

data class ThemePaletteItem(
    val key: String,
    val name: String,
    val primaryColor: Color
)

val DesktopThemePalettes = listOf(
    ThemePaletteItem("lavender_dreams", "Lavender", Color(0xFF624FBE)),
    ThemePaletteItem("cherry_blossom", "Cherry", Color(0xFFD03A60)),
    ThemePaletteItem("rose_gold", "Rose Gold", Color(0xFF944B56)),
    ThemePaletteItem("midnight_blue", "Midnight", Color(0xFF1B365D)),
    ThemePaletteItem("forest_green", "Forest", Color(0xFF2E6F40)),
    ThemePaletteItem("sunset_orange", "Sunset", Color(0xFFD35400)),
    ThemePaletteItem("ocean_teal", "Teal", Color(0xFF007A78)),
    ThemePaletteItem("royal_amethyst", "Amethyst", Color(0xFF6F35A5)),
    ThemePaletteItem("nordic_slate", "Slate", Color(0xFF4F6272))
)

/**
 * Material 3 Expressive Settings & Customization Hub.
 */
@Composable
fun SettingsHubScreen(
    settingsManager: SettingsManager
) {
    val settings by settingsManager.settings.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Settings & Customization",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Material 3 Expressive Theme Tokens & Routing Options",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 1: Material 3 Expressive Color Palettes
        item {
            Text("Material 3 Expressive HCT Color Scheme", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DesktopThemePalettes.forEach { palette ->
                    val isSelected = settings.specialTheme == palette.key
                    Surface(
                        color = if (isSelected) palette.primaryColor else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = CircleShape,
                        modifier = Modifier.expressiveSpringPress {
                            settingsManager.setSpecialTheme(palette.key)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = palette.name,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Bento Card Style
        item {
            Text("Bento Card Container Style", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("glass" to "Glassmorphism", "vibrant" to "Vibrant Gradient", "solid" to "Solid Material", "tonal" to "Tonal Surface").forEach { (key, name) ->
                    val isSelected = settings.cardStyle == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { settingsManager.setCardStyle(key) },
                        label = { Text(name) },
                        shape = CircleShape
                    )
                }
            }
        }

        // Section 3: Core VPN Settings
        item {
            Text("VPN Core & Routing Settings", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // WinTUN Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WinTUN Virtual Network Adapter", fontWeight = FontWeight.Bold)
                            Text("Routes all system traffic natively via WinTUN driver (Requires Admin)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.enableTun,
                            onCheckedChange = { settingsManager.setEnableTun(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Iran GEOIP Bypass
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bypass Iranian Websites & IPs", fontWeight = FontWeight.Bold)
                            Text("Directly routes .ir domains and Iranian GEOIP addresses", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.bypassIran,
                            onCheckedChange = { settingsManager.setBypassIran(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Bypass LAN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bypass Local Area Network (LAN)", fontWeight = FontWeight.Bold)
                            Text("Exclude local IP ranges (192.168.x.x, 10.x.x.x) from VPN tunnel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.bypassLan,
                            onCheckedChange = { settingsManager.setBypassLan(it) }
                        )
                    }
                }
            }
        }

        // Section 4: System Integration & Windows Tray
        item {
            Text("Windows System Integration", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Minimize to Tray
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Minimize to System Tray on Close", fontWeight = FontWeight.Bold)
                            Text("Keep VPN active in taskbar notification tray when window is closed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.minimizeToTray,
                            onCheckedChange = { settingsManager.setMinimizeToTray(it) }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                    // Launch at Startup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Launch at Windows Startup", fontWeight = FontWeight.Bold)
                            Text("Automatically start Chameleon when Windows boots", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.launchAtStartup,
                            onCheckedChange = {
                                settingsManager.setLaunchAtStartup(it)
                                updateStartupRegistry(it)
                            }
                        )
                    }
                }
            }
        }
    }
}
