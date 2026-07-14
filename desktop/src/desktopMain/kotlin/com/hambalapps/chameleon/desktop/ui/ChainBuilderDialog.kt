package com.hambalapps.chameleon.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.hambalapps.chameleon.desktop.data.deserializeProxyChains

private val ExpressiveCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChainBuilderDialog(
    editingChainLink: String?,
    proxyChainsStr: String,
    serverList: List<String>,
    isFarsi: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, relay: String, exit: String) -> Unit
) {
    // Exclude chain profiles from selectable pool to prevent infinite loops
    val poolNodes = remember(serverList) {
        serverList.filter { !it.startsWith("chain://") }
    }

    var chainName by remember { mutableStateOf("") }
    var selectedRelay by remember { mutableStateOf("") }
    var selectedExit by remember { mutableStateOf("") }

    var relayDropdownExpanded by remember { mutableStateOf(false) }
    var exitDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(editingChainLink) {
        if (editingChainLink != null && editingChainLink.startsWith("chain://")) {
            val chainId = editingChainLink.substringAfter("chain://").substringBefore("#")
            val chains = deserializeProxyChains(proxyChainsStr)
            val existing = chains.find { it.id == chainId }
            if (existing != null) {
                chainName = existing.name
                selectedRelay = existing.relayLink
                selectedExit = existing.exitLink
            }
        } else {
            chainName = if (isFarsi) "زنجیره سفارشی" else "Custom Chain"
            selectedRelay = poolNodes.firstOrNull() ?: ""
            selectedExit = poolNodes.getOrNull(1) ?: poolNodes.firstOrNull() ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isFarsi) "ساخت زنجیره پروکسی (Double-Hop)" else "Build Proxy Chain",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Chain Name Input
                OutlinedTextField(
                    value = chainName,
                    onValueChange = { chainName = it },
                    label = { Text(if (isFarsi) "نام زنجیره" else "Chain Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveButtonShape
                )

                // Relay Node Selector (Hop 1)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isFarsi) "سرور میانی (Hop 1)" else "Relay Server (Hop 1)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedRelay.isNotEmpty()) getProxyName(selectedRelay) else (if (isFarsi) "انتخاب سرور" else "Select Server"),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { relayDropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { relayDropdownExpanded = true },
                            shape = ExpressiveButtonShape
                        )
                        DropdownMenu(
                            expanded = relayDropdownExpanded,
                            onDismissRequest = { relayDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            poolNodes.forEach { node ->
                                DropdownMenuItem(
                                    text = { Text(getProxyName(node)) },
                                    onClick = {
                                        selectedRelay = node
                                        relayDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Exit Node Selector (Hop 2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isFarsi) "سرور خروجی (Hop 2)" else "Exit Server (Hop 2)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedExit.isNotEmpty()) getProxyName(selectedExit) else (if (isFarsi) "انتخاب سرور" else "Select Server"),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { exitDropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { exitDropdownExpanded = true },
                            shape = ExpressiveButtonShape
                        )
                        DropdownMenu(
                            expanded = exitDropdownExpanded,
                            onDismissRequest = { exitDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            poolNodes.forEach { node ->
                                DropdownMenuItem(
                                    text = { Text(getProxyName(node)) },
                                    onClick = {
                                        selectedExit = node
                                        exitDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chainName.trim().isNotEmpty() && selectedRelay.isNotEmpty() && selectedExit.isNotEmpty()) {
                        onSave(chainName.trim(), selectedRelay, selectedExit)
                    }
                },
                shape = ExpressiveButtonShape
            ) {
                Text(if (isFarsi) "ذخیره" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isFarsi) "لغو" else "Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
