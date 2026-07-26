package com.hambalapps.chameleon.desktop.ui.nodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import com.hambalapps.chameleon.desktop.vpn.SingboxManager
import com.hambalapps.chameleon.desktop.vpn.getFlagEmoji
import com.hambalapps.chameleon.desktop.vpn.getHostAndPortFromLink
import com.hambalapps.chameleon.desktop.vpn.measurePingDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class DesktopNode(
    val link: String,
    val name: String,
    val protocol: String
)

/**
 * Material 3 Expressive Node Selector Screen with Geolocation Flags & Favorites.
 */
@Composable
fun NodeSelectorScreen(
    settingsManager: SettingsManager
) {
    val settings by settingsManager.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val pings = remember { mutableStateMapOf<String, Long>() }
    var isTestingPings by remember { mutableStateOf(false) }

    val rawServers = remember(settings.subscriptionServers, settings.subscriptionList, settings.manualServers) {
        val servers = mutableListOf<DesktopNode>()
        val combined = settings.allSubscriptionServers + "\n" + settings.manualServers
        val uniqueLines = combined.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        uniqueLines.forEach { trimmed ->
            if (trimmed.isNotEmpty()) {
                val proto = when {
                    trimmed.startsWith("vless://") -> "VLESS"
                    trimmed.startsWith("vmess://") -> "VMESS"
                    trimmed.startsWith("trojan://") -> "TROJAN"
                    trimmed.startsWith("ss://") -> "SS"
                    trimmed.startsWith("hy2://") || trimmed.startsWith("hysteria2://") -> "HY2"
                    trimmed.startsWith("tuic://") -> "TUIC"
                    else -> "PROXY"
                }
                val rawName = if (trimmed.contains("#")) trimmed.substringAfter("#") else "Proxy Node"
                val name = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (e: Exception) { rawName }
                servers.add(DesktopNode(link = trimmed, name = name, protocol = proto))
            }
        }
        servers
    }

    // Sort favorites first, then preserve order
    val sortedServers = remember(rawServers, settings.favoriteServers) {
        rawServers.sortedWith(Comparator { a, b ->
            val isFavA = settings.favoriteServers.contains(a.link)
            val isFavB = settings.favoriteServers.contains(b.link)
            when {
                isFavA && !isFavB -> -1
                !isFavA && isFavB -> 1
                else -> 0
            }
        })
    }

    val filteredServers = remember(sortedServers, searchQuery) {
        if (searchQuery.isEmpty()) sortedServers
        else sortedServers.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.protocol.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Proxy Nodes (${rawServers.size})",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Select a node to route your connection profile",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    if (!isTestingPings) {
                        isTestingPings = true
                        scope.launch(Dispatchers.IO) {
                            rawServers.forEach { node ->
                                val hp = getHostAndPortFromLink(node.link)
                                if (hp != null) {
                                    val delay = measurePingDelay(hp.first, hp.second)
                                    pings[node.link] = delay.toLong()
                                }
                            }
                            isTestingPings = false
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isTestingPings) "Testing Latency..." else "Ping All Nodes")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search nodes by name or protocol...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Node List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredServers) { node ->
                val isSelected = settings.activeProfile == node.link
                val isFavorite = settings.favoriteServers.contains(node.link)
                val pingMs = pings[node.link]
                val flag = getFlagEmoji(node.name)

                val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow

                Surface(
                    color = cardBg,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .expressiveSpringPress {
                            settingsManager.setActiveProfile(node.link)
                            if (SingboxManager.vpnState.value == "CONNECTED") {
                                scope.launch {
                                    SingboxManager.start(node.link, settingsManager)
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Flag / Country Indicator
                            Text(
                                text = flag,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )

                            // Protocol Badge
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            ) {
                                Text(
                                    text = node.protocol.take(3),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = node.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                                Text(
                                    text = node.protocol,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Favorite Star Button
                            IconButton(
                                onClick = { settingsManager.toggleFavorite(node.link) }
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.outline
                                )
                            }

                            // Ping Badge
                            if (pingMs != null) {
                                val pingColor = when {
                                    pingMs < 0 -> MaterialTheme.colorScheme.error
                                    pingMs < 200 -> Color(0xFF10B981)
                                    pingMs < 500 -> Color(0xFFF59E0B)
                                    else -> MaterialTheme.colorScheme.error
                                }
                                Surface(
                                    color = pingColor.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (pingMs < 0) "Timeout" else "${pingMs}ms",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = pingColor,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
