package com.hambalapps.chameleon.ui.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.data.SettingsManager
import com.hambalapps.chameleon.data.deserializeSubscriptions
import com.hambalapps.chameleon.theme.ChameleonTheme
import com.hambalapps.chameleon.vpn.VpnServiceWrapper
import com.hambalapps.chameleon.vpn.measurePingDelay
import com.hambalapps.chameleon.vpn.getHostAndPortFromLink
import com.hambalapps.chameleon.vpn.ProxyNameResolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items


class NodesPopupActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChameleonTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val settingsManager = remember { SettingsManager(context.applicationContext) }
                val settings by settingsManager.settings.collectAsStateWithLifecycle(initialValue = SettingsManager.defaultSettings)
                val activeProfile = settings.activeProfile
                val activeSubId = settings.activeSubId
                val subscriptions = settings.deserializedSubscriptions
                val manualServersStr = settings.manualServers
                var isMultiSelectMode by remember { mutableStateOf(false) }
                var selectedNodes by remember { mutableStateOf(setOf<String>()) }
                var filterSubId by remember(activeSubId) { mutableStateOf(activeSubId) }
                val serverList = remember(subscriptions, filterSubId) {
                    if (filterSubId == "all") {
                        subscriptions.flatMap { it.servers.split("\n") }.filter { it.trim().isNotEmpty() }.distinct()
                    } else {
                        val sub = subscriptions.find { it.id == filterSubId } ?: subscriptions.firstOrNull()
                        sub?.servers?.split("\n")?.filter { it.trim().isNotEmpty() } ?: emptyList()
                    }
                }

                var searchQuery by remember { mutableStateOf("") }
                var isSearchVisible by remember { mutableStateOf(false) }
                var selectedTab by remember { mutableStateOf(0) }
                var pingsMap by remember { mutableStateOf(mapOf<String, Int>()) }
                var isTestingPings by remember { mutableStateOf(false) }

                val filteredServerList = remember(serverList, searchQuery, selectedTab) {
                    serverList.mapIndexedNotNull { index, serverLink ->
                        val type = serverLink.substringBefore("://").uppercase()
                        val matchesTab = when (selectedTab) {
                            0 -> true
                            1 -> type == "VLESS"
                            2 -> type == "TROJAN"
                            3 -> type == "SS" || type == "SHADOWSOCKS"
                            else -> true
                        }
                        if (matchesTab) {
                            val name = ProxyNameResolver.getProxyName(serverLink, context)
                            if (name.contains(searchQuery, ignoreCase = true)) {
                                ServerItem(
                                    id = "${serverLink}_$index",
                                    link = serverLink,
                                    name = name,
                                    type = type,
                                    transport = getTransportType(serverLink)
                                )
                            } else null
                        } else null
                    }
                }

                val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()

                // Translucent dim backdrop click closes popup
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(500.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = ExpressiveCardShape
                            )
                            .clickable(enabled = false) {}, // Prevent clicks inside card from closing
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (isMultiSelectMode) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            isMultiSelectMode = false
                                            selectedNodes = emptySet()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Exit Selection Mode",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${selectedNodes.size} selected",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val allFilteredManuals = remember(filteredServerList, manualServersStr) {
                                            filteredServerList.filter { item ->
                                                manualServersStr.split("\n").map { it.trim() }.contains(item.link.trim())
                                            }.map { it.link }
                                        }
                                        val isAllSelected = remember(selectedNodes, allFilteredManuals) {
                                            allFilteredManuals.isNotEmpty() && selectedNodes.containsAll(allFilteredManuals)
                                        }

                                        TextButton(
                                            onClick = {
                                                if (isAllSelected) {
                                                    selectedNodes = selectedNodes - allFilteredManuals.toSet()
                                                    if (selectedNodes.isEmpty()) {
                                                        isMultiSelectMode = false
                                                    }
                                                } else {
                                                    selectedNodes = selectedNodes + allFilteredManuals.toSet()
                                                }
                                            }
                                        ) {
                                            Text(if (isAllSelected) "Deselect All" else "Select All")
                                        }

                                        IconButton(
                                            onClick = {
                                                val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                                val newManualList = currentManualList.filter { !selectedNodes.contains(it) }
                                                scope.launch {
                                                    settingsManager.setManualServers(newManualList.joinToString("\n"))
                                                }
                                                isMultiSelectMode = false
                                                selectedNodes = emptySet()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Selected",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Dns,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.available_nodes),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                isSearchVisible = !isSearchVisible
                                                if (!isSearchVisible) searchQuery = ""
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isSearchVisible) Icons.Default.SearchOff else Icons.Default.Search,
                                                contentDescription = stringResource(R.string.search),
                                                tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (!isTestingPings) {
                                                    scope.launch {
                                                        isTestingPings = true
                                                        val jobs = serverList.map { link ->
                                                            scope.async(Dispatchers.IO) {
                                                                val hostPort = getHostAndPortFromLink(link)
                                                                val ping = if (hostPort != null) {
                                                                    measurePingDelay(hostPort.first, hostPort.second)
                                                                } else {
                                                                    -1
                                                                }
                                                                link to ping
                                                            }
                                                        }
                                                        val results = jobs.awaitAll()
                                                        pingsMap = pingsMap + results.toMap()
                                                        isTestingPings = false
                                                    }
                                                }
                                            },
                                            enabled = !isTestingPings
                                        ) {
                                            if (isTestingPings) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Speed,
                                                    contentDescription = stringResource(R.string.test_pings),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        IconButton(onClick = { finish() }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isSearchVisible,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            if (subscriptions.size > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        FilterChip(
                                            selected = filterSubId == "all",
                                            onClick = { filterSubId = "all" },
                                            label = { Text(stringResource(R.string.tab_all)) },
                                            shape = ExpressiveChipShape
                                        )
                                    }
                                    items(subscriptions) { sub ->
                                        val isSelected = filterSubId == sub.id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { filterSubId = sub.id },
                                            label = { Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            shape = ExpressiveChipShape
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            ScrollableTabRow(
                                selectedTabIndex = selectedTab,
                                edgePadding = 0.dp,
                                containerColor = Color.Transparent,
                                contentColor = MaterialTheme.colorScheme.primary,
                                indicator = { tabPositions ->
                                    if (selectedTab < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                divider = {}
                            ) {
                                listOf(stringResource(R.string.tab_all), "VLESS", "Trojan", "Shadowsocks").forEachIndexed { index, title ->
                                    Tab(
                                        selected = selectedTab == index,
                                        onClick = { selectedTab = index },
                                        text = {
                                            Text(
                                                text = title,
                                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (filteredServerList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_matching_nodes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(filteredServerList, key = { _, item -> item.link }) { index, serverItem ->
                                        val serverLink = serverItem.link
                                        val isSelected = activeProfile == serverLink
                                        val name = serverItem.name
                                        val type = serverItem.type
                                        val transport = serverItem.transport

                                        val tagContainerColor = when (type) {
                                            "VLESS" -> MaterialTheme.colorScheme.primaryContainer
                                            "TROJAN" -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.tertiaryContainer
                                        }
                                        val tagTextColor = when (type) {
                                            "VLESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            "TROJAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                            else -> MaterialTheme.colorScheme.onTertiaryContainer
                                        }

                                        val isManualNode = remember(manualServersStr, serverLink) {
                                            manualServersStr.split("\n").map { it.trim() }.contains(serverLink.trim())
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(ExpressiveButtonShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                    else Color.Transparent
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                                                    shape = ExpressiveButtonShape
                                                )
                                                .combinedClickable(
                                                    onClick = {
                                                        if (isMultiSelectMode) {
                                                            if (isManualNode) {
                                                                selectedNodes = if (selectedNodes.contains(serverLink)) {
                                                                    selectedNodes - serverLink
                                                                } else {
                                                                    selectedNodes + serverLink
                                                                }
                                                                if (selectedNodes.isEmpty()) {
                                                                    isMultiSelectMode = false
                                                                }
                                                            } else {
                                                                android.widget.Toast.makeText(context, "Only custom/manual nodes can be selected for batch deletion", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            scope.launch {
                                                                settingsManager.setActiveProfile(serverLink)
                                                                if (vpnState == "CONNECTED") {
                                                                    val startIntent = Intent(context, VpnServiceWrapper::class.java).apply {
                                                                        action = VpnServiceWrapper.ACTION_START
                                                                    }
                                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                        context.startForegroundService(startIntent)
                                                                    } else {
                                                                        context.startService(startIntent)
                                                                    }
                                                                }
                                                                finish() // Close popup on selection
                                                            }
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (isManualNode) {
                                                            if (!isMultiSelectMode) {
                                                                isMultiSelectMode = true
                                                                selectedNodes = setOf(serverLink)
                                                            }
                                                        }
                                                    }
                                                )
                                                .pressScaleEffect()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isMultiSelectMode && isManualNode) {
                                                Checkbox(
                                                    checked = selectedNodes.contains(serverLink),
                                                    onCheckedChange = { checked ->
                                                        selectedNodes = if (checked) {
                                                            selectedNodes + serverLink
                                                        } else {
                                                            selectedNodes - serverLink
                                                        }
                                                        if (selectedNodes.isEmpty()) {
                                                            isMultiSelectMode = false
                                                        }
                                                    },
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isSelected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Hub,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                     Box(
                                                         modifier = Modifier
                                                             .clip(ExpressiveChipShape)
                                                             .background(tagContainerColor)
                                                             .padding(horizontal = 6.dp, vertical = 2.dp)
                                                     ) {
                                                         Text(
                                                             text = type,
                                                             style = MaterialTheme.typography.labelSmall,
                                                             color = tagTextColor,
                                                             fontWeight = FontWeight.Bold,
                                                             maxLines = 1,
                                                             softWrap = false
                                                         )
                                                     }

                                                     Spacer(modifier = Modifier.width(4.dp))

                                                     Box(
                                                         modifier = Modifier
                                                             .clip(ExpressiveChipShape)
                                                             .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                                             .padding(horizontal = 6.dp, vertical = 2.dp)
                                                     ) {
                                                         Text(
                                                             text = transport,
                                                             style = MaterialTheme.typography.labelSmall,
                                                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                             fontWeight = FontWeight.Bold,
                                                             maxLines = 1,
                                                             softWrap = false
                                                         )
                                                     }

                                                     Spacer(modifier = Modifier.width(8.dp))

                                                    val ping = pingsMap[serverLink]
                                                    if (ping != null) {
                                                        val isTimeout = ping < 0
                                                        val pingColor = when {
                                                            isTimeout -> Color(0xFFF44336)
                                                            ping < 60 -> Color(0xFF4CAF50)
                                                            ping < 120 -> Color(0xFFFFB300)
                                                            else -> Color(0xFFF44336)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(pingColor)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = if (isTimeout) "Timeout" else "${ping} ms",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = pingColor,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = stringResource(R.string.untested),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            softWrap = false
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
                }
            }
        }
    }
}

