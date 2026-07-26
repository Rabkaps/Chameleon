package com.hambalapps.chameleon.desktop.ui.subs

import androidx.compose.foundation.background
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
import com.hambalapps.chameleon.desktop.data.Subscription
import com.hambalapps.chameleon.desktop.data.serializeSubscriptions
import com.hambalapps.chameleon.desktop.ui.components.ExpressiveGlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Material 3 Expressive Subscription Manager Screen with complete parser & update capabilities.
 */
@Composable
fun SubscriptionScreen(
    settingsManager: SettingsManager
) {
    val settings by settingsManager.settings.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showImportLinksDialog by remember { mutableStateOf(false) }
    var rawLinksInput by remember { mutableStateOf("") }

    var subNameInput by remember { mutableStateOf("") }
    var subUrlInput by remember { mutableStateOf("") }
    var isUpdatingAll by remember { mutableStateOf(false) }
    val updatingMap = remember { mutableStateMapOf<String, Boolean>() }

    val subscriptions = remember(settings.subscriptionList) {
        settings.deserializedSubscriptions
    }

    fun syncSubscriptionServers(subs: List<Subscription>) {
        val allServers = subs.joinToString("\n") { it.servers }.trim()
        settingsManager.setSubscriptionServers(allServers)
        if (settings.activeProfile.isEmpty()) {
            val firstNode = allServers.lines().firstOrNull { it.trim().isNotEmpty() }
            if (firstNode != null) {
                settingsManager.setActiveProfile(firstNode)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Subscriptions",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Manage Remote V2Ray & Subscription Links",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Import Raw V2Ray Links Button
                OutlinedButton(
                    onClick = { showImportLinksDialog = true },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Links")
                }

                // Refresh All Button
                OutlinedButton(
                    onClick = {
                        isUpdatingAll = true
                        scope.launch(Dispatchers.IO) {
                            val updatedSubs = subscriptions.map { sub ->
                                if (sub.url.startsWith("http")) {
                                    val res = fetchSubscription(sub.url)
                                    sub.copy(
                                        servers = res.servers.joinToString("\n"),
                                        upload = res.upload ?: sub.upload,
                                        download = res.download ?: sub.download,
                                        total = res.total ?: sub.total,
                                        expire = res.expire ?: sub.expire
                                    )
                                } else sub
                            }
                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedSubs))
                            syncSubscriptionServers(updatedSubs)
                            isUpdatingAll = false
                        }
                    },
                    enabled = !isUpdatingAll && subscriptions.isNotEmpty(),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isUpdatingAll) "Updating..." else "Update All")
                }

                // Add Subscription Button
                Button(
                    onClick = { showAddDialog = true },
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Sub")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Subscription Cards List
        if (subscriptions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No subscriptions added yet.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(subscriptions) { sub ->
                    val isSingleUpdating = updatingMap[sub.id] == true

                    ExpressiveGlassCard(
                        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                        cardStyle = settings.cardStyle
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.RssFeed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = sub.name,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = sub.url,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Update Single Subscription Button
                                    IconButton(
                                        onClick = {
                                            updatingMap[sub.id] = true
                                            scope.launch(Dispatchers.IO) {
                                                if (sub.url.startsWith("http")) {
                                                    val res = fetchSubscription(sub.url)
                                                    val updatedSub = sub.copy(
                                                        servers = res.servers.joinToString("\n"),
                                                        upload = res.upload ?: sub.upload,
                                                        download = res.download ?: sub.download,
                                                        total = res.total ?: sub.total,
                                                        expire = res.expire ?: sub.expire
                                                    )
                                                    val updatedList = subscriptions.map { if (it.id == sub.id) updatedSub else it }
                                                    settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
                                                    syncSubscriptionServers(updatedList)
                                                }
                                                updatingMap[sub.id] = false
                                            }
                                        },
                                        enabled = !isSingleUpdating
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = "Sync Sub", tint = MaterialTheme.colorScheme.primary)
                                    }

                                    IconButton(
                                        onClick = {
                                            val updated = subscriptions.filter { it.id != sub.id }
                                            settingsManager.setSubscriptionList(serializeSubscriptions(updated))
                                            syncSubscriptionServers(updated)
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Quota Bandwidth Progress Bar & Expiry Details
                            if (sub.total != null && sub.total > 0) {
                                Spacer(modifier = Modifier.height(14.dp))
                                val usedBytes = (sub.download ?: 0L) + (sub.upload ?: 0L)
                                val progress = (usedBytes.toFloat() / sub.total.toFloat()).coerceIn(0f, 1f)

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Traffic: ${formatBytes(usedBytes)} / ${formatBytes(sub.total)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (sub.expire != null) {
                                            Text(
                                                text = "Expires: ${formatExpiry(sub.expire)}",
                                                fontSize = 12.sp,
                                                color = Color(0xFFF59E0B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Subscription Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Subscription URL") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = subNameInput,
                        onValueChange = { subNameInput = it },
                        label = { Text("Subscription Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subUrlInput,
                        onValueChange = { subUrlInput = it },
                        label = { Text("Subscription URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (subUrlInput.isNotEmpty()) {
                            val newSub = Subscription(
                                id = System.currentTimeMillis().toString(),
                                name = subNameInput.ifEmpty { "Subscription" },
                                url = subUrlInput,
                                servers = ""
                            )
                            val updated = subscriptions + newSub
                            settingsManager.setSubscriptionList(serializeSubscriptions(updated))

                            // Fetch sub content in background with parser
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val res = fetchSubscription(subUrlInput)
                                    val fetchedSub = newSub.copy(
                                        servers = res.servers.joinToString("\n"),
                                        upload = res.upload,
                                        download = res.download,
                                        total = res.total,
                                        expire = res.expire
                                    )
                                    val finalList = updated.map { if (it.id == newSub.id) fetchedSub else it }
                                    settingsManager.setSubscriptionList(serializeSubscriptions(finalList))
                                    syncSubscriptionServers(finalList)
                                } catch (e: Exception) {}
                            }

                            showAddDialog = false
                            subNameInput = ""
                            subUrlInput = ""
                        }
                    }
                ) {
                    Text("Save & Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste Raw V2Ray Links Dialog
    if (showImportLinksDialog) {
        AlertDialog(
            onDismissRequest = { showImportLinksDialog = false },
            title = { Text("Paste Raw Proxy Links") },
            text = {
                OutlinedTextField(
                    value = rawLinksInput,
                    onValueChange = { rawLinksInput = it },
                    placeholder = { Text("Paste raw links (vless://, vmess://, ss://, trojan://, hy2://)...") },
                    modifier = Modifier.fillMaxWidth().height(180.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawLinksInput.isNotEmpty()) {
                            val currentManual = settings.manualServers
                            val updatedManual = if (currentManual.isEmpty()) rawLinksInput else "$currentManual\n$rawLinksInput"
                            settingsManager.setManualServers(updatedManual)
                            if (settings.activeProfile.isEmpty()) {
                                val firstLink = rawLinksInput.lines().firstOrNull { it.trim().isNotEmpty() }
                                if (firstLink != null) {
                                    settingsManager.setActiveProfile(firstLink)
                                }
                            }
                            showImportLinksDialog = false
                            rawLinksInput = ""
                        }
                    }
                ) {
                    Text("Import Links")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportLinksDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
