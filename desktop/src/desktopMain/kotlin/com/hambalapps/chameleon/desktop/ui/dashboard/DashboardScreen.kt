package com.hambalapps.chameleon.desktop.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.hambalapps.chameleon.desktop.ui.components.*
import com.hambalapps.chameleon.desktop.vpn.SingboxManager
import kotlinx.coroutines.delay
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
    val totalTraffic by SingboxManager.totalTraffic.collectAsState()
    val connectedStartTime by SingboxManager.connectedStartTime.collectAsState()

    val scope = rememberCoroutineScope()
    
    // Traffic History State
    val uploadHistory = remember { mutableStateListOf(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L) }
    val downloadHistory = remember { mutableStateListOf(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L) }

    LaunchedEffect(trafficStats) {
        if (uploadHistory.size >= 18) uploadHistory.removeAt(0)
        if (downloadHistory.size >= 18) downloadHistory.removeAt(0)
        uploadHistory.add(trafficStats.first)
        downloadHistory.add(trafficStats.second)
    }

    // Live Connected Session Timer
    var uptimeSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(vpnState, connectedStartTime) {
        if (vpnState == "CONNECTED" && connectedStartTime > 0L) {
            while (true) {
                uptimeSeconds = (System.currentTimeMillis() - connectedStartTime) / 1000L
                delay(1000)
            }
        } else {
            uptimeSeconds = 0L
        }
    }

    val formatUptime = remember {
        { seconds: Long ->
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
            else String.format("%02d:%02d", m, s)
        }
    }

    val formatBytes = remember {
        { bytes: Long ->
            when {
                bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes / (1024f * 1024f * 1024f))
                bytes >= 1024L * 1024L -> String.format("%.2f MB", bytes / (1024f * 1024f))
                bytes >= 1024L -> String.format("%.1f KB", bytes / 1024f)
                else -> "$bytes B"
            }
        }
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
        // Top Header Section
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
                    text = "Sing-Box Extended Core v1.12.0",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (vpnState == "CONNECTED") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = ExpressiveChipShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatUptime(uptimeSeconds),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                ExpressiveStatusBadge(status = vpnState)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Row (Hero Connection Dial + Speed Graph Card)
        Row(
            modifier = Modifier.fillMaxWidth().height(230.dp),
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (vpnState) {
                                "CONNECTED" -> "Click to Disconnect"
                                "CONNECTING" -> "Establishing Tunnel..."
                                "DISCONNECTING" -> "Closing Tunnel..."
                                else -> "Click to Connect"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        Column {
                            Text(
                                text = "Live Bandwidth Monitor",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total: ↑ ${formatBytes(totalTraffic.first)}  •  ↓ ${formatBytes(totalTraffic.second)}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Surface(
                                color = Color(0xFF3B82F6).copy(alpha = 0.12f),
                                shape = ExpressiveChipShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(formatSpeed(trafficStats.second), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                                }
                            }
                            Surface(
                                color = Color(0xFF10B981).copy(alpha = 0.12f),
                                shape = ExpressiveChipShape
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(formatSpeed(trafficStats.first), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    BandwidthCanvasGraph(
                        uploadHistory = uploadHistory,
                        downloadHistory = downloadHistory,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                    modifier = Modifier.height(115.dp),
                    cardStyle = settings.cardStyle,
                    onClick = { settingsManager.setEnableTun(!settings.enableTun) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (settings.enableTun) Icons.Default.Shield else Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (settings.enableTun) "TUN Mode" else "System Proxy",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (settings.enableTun) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = if (settings.enableTun) "VIRTUAL ADAPTER" else "REGISTRY 2080",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.enableTun) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.enableTun) "WinTUN driver active (Root required)" else "Direct Windows HTTP/SOCKS proxy",
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
                    modifier = Modifier.height(115.dp),
                    cardStyle = settings.cardStyle,
                    onClick = onNavigateToNodes
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Dns,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        val activeName = if (settings.activeProfile.contains("#")) {
                            try { java.net.URLDecoder.decode(settings.activeProfile.substringAfter("#"), "UTF-8") } catch (e: Exception) { settings.activeProfile.substringAfter("#") }
                        } else if (settings.activeProfile.isNotEmpty()) "Selected Node" else "No Node Selected"
                        val activeFlag = com.hambalapps.chameleon.desktop.vpn.getFlagEmoji(activeName)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Active Node",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$activeFlag $activeName",
                                fontSize = 14.sp,
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
                    modifier = Modifier.height(115.dp),
                    cardStyle = settings.cardStyle,
                    onClick = { settingsManager.setBypassIran(!settings.bypassIran) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (settings.bypassIran) Icons.Default.AltRoute else Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Iran Geo-Bypass",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (settings.bypassIran) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = if (settings.bypassIran) "ACTIVE" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.bypassIran) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.bypassIran) "Direct for domestic .ir & local services" else "Routing all internet traffic via proxy",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bento Card 4: DNS Shield & Protocol
            item {
                ExpressiveGlassCard(
                    modifier = Modifier.height(115.dp),
                    cardStyle = settings.cardStyle
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "DNS Shield",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "DoH: ${settings.secureDns.substringAfter("://").substringBefore("/")}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "Strategy: ${settings.ipVersion.replace("_", " ").uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bento Card 5: Split Tunneling Status
            item {
                ExpressiveGlassCard(
                    modifier = Modifier.height(115.dp),
                    cardStyle = settings.cardStyle,
                    onClick = { settingsManager.setSplitTunnelingEnabled(!settings.splitTunnelingEnabled) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CallSplit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Split Tunneling",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (settings.splitTunnelingEnabled) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = if (settings.splitTunnelingEnabled) "ON" else "OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.splitTunnelingEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.splitTunnelingEnabled) "${settings.splitTunnelingApps.size} apps configured (${settings.splitTunnelingMode})" else "All desktop applications tunneled",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bento Card 6: Anti-Censorship Features (Fragment & Mux)
            item {
                ExpressiveGlassCard(
                    modifier = Modifier.height(115.dp),
                    cardStyle = settings.cardStyle,
                    onClick = { settingsManager.setEnableFragment(!settings.enableFragment) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "TLS Fragment",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (settings.enableFragment) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                                    shape = ExpressiveChipShape
                                ) {
                                    Text(
                                        text = if (settings.enableFragment) "BYPASS ACTIVE" else "DISABLED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (settings.enableFragment) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (settings.enableFragment) "Len: ${settings.fragmentLength}  •  Int: ${settings.fragmentInterval}" else "Standard TLS handshake",
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
