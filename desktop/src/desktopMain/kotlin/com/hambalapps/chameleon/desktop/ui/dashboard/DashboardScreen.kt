package com.hambalapps.chameleon.desktop.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.ui.components.*
import com.hambalapps.chameleon.desktop.vpn.SingboxManager
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive Bento Connection Dashboard Screen.
 */
@Composable
fun DashboardScreen(
    settingsManager: SettingsManager,
    onNavigateToNodes: () -> Unit
) {
    val settings by settingsManager.settings.collectAsState()
    val vpnState by SingboxManager.vpnState.collectAsState()
    val trafficStats by SingboxManager.trafficStats.collectAsState()

    val scope = rememberCoroutineScope()
    
    // Traffic History State
    val uploadHistory = remember { mutableStateListOf(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L) }
    val downloadHistory = remember { mutableStateListOf(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L) }

    LaunchedEffect(trafficStats) {
        if (uploadHistory.size >= 15) uploadHistory.removeAt(0)
        if (downloadHistory.size >= 15) downloadHistory.removeAt(0)
        uploadHistory.add(trafficStats.first)
        downloadHistory.add(trafficStats.second)
    }

    val formatSpeed = remember {
        { bytes: Long ->
            when {
                bytes >= 1024 * 1024 -> String.format("%.2f MB/s", bytes / (1024f * 1024f))
                bytes >= 1024 -> String.format("%.1f KB/s", bytes / 1024f)
                else -> "$bytes B/s"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Material 3 Expressive VPN Core",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ExpressiveStatusBadge(status = vpnState)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Row (Hero Connection Dial + Speed Graph Card)
        Row(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Connection Dial Card
            ExpressiveGlassCard(
                modifier = Modifier.width(280.dp),
                cardStyle = settings.cardStyle
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    HeroConnectionDial(
                        vpnState = vpnState,
                        onClick = {
                            if (vpnState == "CONNECTED" || vpnState == "CONNECTING") {
                                SingboxManager.stop()
                            } else {
                                scope.launch {
                                    val profileToStart = if (settings.activeProfile.isNotEmpty()) settings.activeProfile else {
                                        val combined = settings.allSubscriptionServers + "\n" + settings.manualServers
                                        val firstNode = combined.lines().firstOrNull { it.trim().isNotEmpty() } ?: ""
                                        if (firstNode.isNotEmpty()) {
                                            settingsManager.setActiveProfile(firstNode)
                                        }
                                        firstNode
                                    }
                                    SingboxManager.start(profileToStart, settingsManager)
                                }
                            }
                        }
                    )
                }
            }

            // Realtime Bandwidth Speed Graph Card
            ExpressiveGlassCard(
                modifier = Modifier.weight(1f),
                cardStyle = settings.cardStyle
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Bandwidth Graph",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                Text(formatSpeed(trafficStats.second), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Text(formatSpeed(trafficStats.first), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    BandwidthCanvasGraph(
                        uploadHistory = uploadHistory,
                        downloadHistory = downloadHistory,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bento Cards Section
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Bento Card 1: Active Mode (TUN vs Proxy)
            item {
                ExpressiveGlassCard(
                    modifier = Modifier.height(110.dp),
                    cardStyle = settings.cardStyle,
                    onClick = { settingsManager.setEnableTun(!settings.enableTun) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (settings.enableTun) Icons.Default.Shield else Icons.Default.Language,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = if (settings.enableTun) "TUN Mode" else "System Proxy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (settings.enableTun) "WinTUN Virtual Adapter" else "127.0.0.1:2080 Registry",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bento Card 2: Quick Node Switcher
            item {
                ExpressiveGlassCard(
                    modifier = Modifier.height(110.dp),
                    cardStyle = settings.cardStyle,
                    onClick = onNavigateToNodes
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        val activeName = if (settings.activeProfile.contains("#")) {
                            try { java.net.URLDecoder.decode(settings.activeProfile.substringAfter("#"), "UTF-8") } catch (e: Exception) { settings.activeProfile.substringAfter("#") }
                        } else if (settings.activeProfile.isNotEmpty()) "Selected Node" else "No Node Selected"
                        val activeFlag = com.hambalapps.chameleon.desktop.vpn.getFlagEmoji(activeName)

                        Column {
                            Text(
                                text = "Active Node",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$activeFlag $activeName",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bento Card 3: Iran GEOIP Bypass Status
            item {
                ExpressiveGlassCard(
                    modifier = Modifier.height(110.dp),
                    cardStyle = settings.cardStyle,
                    onClick = { settingsManager.setBypassIran(!settings.bypassIran) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (settings.bypassIran) Icons.Default.AltRoute else Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Iran GEOIP Bypass",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (settings.bypassIran) "Bypassing Iranian Domain/IPs" else "Routing All Traffic",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
