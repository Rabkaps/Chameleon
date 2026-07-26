@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hambalapps.chameleon.desktop.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.hambalapps.chameleon.desktop.ui.components.ExpressiveGlassCard
import com.hambalapps.chameleon.desktop.ui.components.expressiveSpringPress
import com.hambalapps.chameleon.desktop.vpn.CdnIpScanner
import com.hambalapps.chameleon.desktop.vpn.ProcessPickerUtils
import com.hambalapps.chameleon.desktop.vpn.ScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive Tools Hub Screen (CDN Scanner, Split Tunneling, Chain Builder).
 */
@Composable
fun ToolsHubScreen(
    settingsManager: SettingsManager
) {
    val settings by settingsManager.settings.collectAsState()
    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header & Expressive Tab Row
        Column {
            Text(
                text = "Tools Hub",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "CDN Scanner, Split Tunneling & Proxy Chaining",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryTabRow(
                selectedTabIndex = activeTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Clean IP Scanner") },
                    icon = { Icon(Icons.Default.Radar, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Split Tunneling") },
                    icon = { Icon(Icons.Default.AltRoute, contentDescription = null) }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = { Text("Proxy Chains") },
                    icon = { Icon(Icons.Default.Link, contentDescription = null) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Tab Body
        when (activeTab) {
            0 -> CdnScannerTab()
            1 -> SplitTunnelingTab(settingsManager = settingsManager)
            else -> ProxyChainTab(settingsManager = settingsManager)
        }
    }
}

@Composable
fun CdnScannerTab() {
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<com.hambalapps.chameleon.desktop.vpn.ScannedIp>() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cloudflare & CDN Clean IP Scanner",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = {
                    isScanning = true
                    results.clear()
                    scope.launch(Dispatchers.IO) {
                        val scanRes = CdnIpScanner.performScan("cloudflare")
                        results.addAll(scanRes.workingIps)
                        isScanning = false
                    }
                },
                enabled = !isScanning,
                shape = CircleShape
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isScanning) "Scanning IPs..." else "Start Scan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results) { res ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = res.ip, fontWeight = FontWeight.Bold)
                        Text(text = "${res.latencyMs}ms", color = if (res.latencyMs < 200) Color(0xFF10B981) else Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SplitTunnelingTab(settingsManager: SettingsManager) {
    val settings by settingsManager.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var runningApps by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            runningApps = ProcessPickerUtils.getRunningProcesses().map { it.name }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "App Per-App Proxying",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Choose Windows applications (.exe) to route or bypass",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    val file = ProcessPickerUtils.selectExecutableFile()
                    if (file != null) {
                        val current = settings.splitTunnelingApps
                        settingsManager.setSplitTunnelingApps(current + file)
                    }
                },
                shape = CircleShape
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse .exe")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selected Split Tunneling Apps:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(settings.splitTunnelingApps.toList()) { app ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = app, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                settingsManager.setSplitTunnelingApps(settings.splitTunnelingApps - app)
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProxyChainTab(settingsManager: SettingsManager) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Proxy Chain Builder - Config detours active.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
