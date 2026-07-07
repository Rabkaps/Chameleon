package com.hambalapps.chameleon.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.vpn.ProxyNameResolver
import com.hambalapps.chameleon.data.deserializeProxyChains

@Composable
fun ChainBuilderDialog(
    editingChainLink: String?,
    proxyChainsStr: String,
    serverList: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, relay: String, exit: String) -> Unit
) {
    val context = LocalContext.current
    
    // Parse all available single servers (exclude chains to prevent circular dependencies)
    val poolNodes = remember(serverList) {
        serverList.filter { !it.startsWith("chain://") }
    }

    var chainName by remember { mutableStateOf("") }
    var selectedRelay by remember { mutableStateOf("") }
    var selectedExit by remember { mutableStateOf("") }

    // Dropdown open states
    var relayDropdownExpanded by remember { mutableStateOf(false) }
    var exitDropdownExpanded by remember { mutableStateOf(false) }

    // Parse existing settings if editing
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
            chainName = "Custom Chain"
            selectedRelay = poolNodes.firstOrNull() ?: ""
            selectedExit = poolNodes.getOrNull(1) ?: poolNodes.firstOrNull() ?: ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingChainLink == null || editingChainLink == "new_chain") 
                    stringResource(R.string.build_chain) else stringResource(R.string.edit_node_config),
                fontWeight = FontWeight.Bold
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
                    label = { Text("Chain Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveButtonShape
                )

                // Relay Node Selector (First Hop)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.relay_node),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedRelay.isNotEmpty()) ProxyNameResolver.getProxyName(selectedRelay, context) else "Select Server",
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
                                    text = { Text(ProxyNameResolver.getProxyName(node, context)) },
                                    onClick = {
                                        selectedRelay = node
                                        relayDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Exit Node Selector (Second Hop)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.exit_node),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedExit.isNotEmpty()) ProxyNameResolver.getProxyName(selectedExit, context) else "Select Server",
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
                                    text = { Text(ProxyNameResolver.getProxyName(node, context)) },
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
                modifier = Modifier.pressScaleEffect(),
                shape = ExpressiveButtonShape
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pressScaleEffect()
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
