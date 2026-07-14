package com.hambalapps.chameleon.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.data.UserSettings
import com.hambalapps.chameleon.desktop.vpn.CdnIpScanner
import com.hambalapps.chameleon.desktop.vpn.ScannedIp
import kotlinx.coroutines.launch

private val ExpressiveCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
private val ExpressiveChipShape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CdnScannerScreen(
    settings: UserSettings,
    settingsManager: SettingsManager
) {
    val isFarsi = settings.isFarsi
    val scope = rememberCoroutineScope()

    var activePreset by remember { mutableStateOf("cloudflare") }
    var customIpsInput by remember { mutableStateOf("") }
    
    var isScanning by remember { mutableStateOf(false) }
    var scannedIpsList by remember { mutableStateOf<List<ScannedIp>>(emptyList()) }
    var scanMessage by remember { mutableStateOf<String?>(null) }

    val presets = listOf(
        "cloudflare" to "Cloudflare CDN",
        "cloudfront" to "Cloudfront CDN",
        "custom" to (if (isFarsi) "لیست سفارشی" else "Custom IP List")
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = if (isFarsi) "اسکنر آی‌پی تمیز CDN" else "CDN Clean IP Scanner",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Preset selection Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), ExpressiveCardShape)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isFarsi) "انتخاب سرویس‌دهنده" else "Select CDN Provider",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (presetKey, displayName) ->
                        val selected = activePreset == presetKey
                        FilterChip(
                            selected = selected,
                            onClick = { activePreset = presetKey },
                            label = { Text(text = displayName, fontSize = 12.sp) },
                            shape = ExpressiveButtonShape,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Custom IPs input (if custom selected)
        AnimatedVisibility(
            visible = activePreset == "custom",
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), ExpressiveCardShape)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isFarsi) "لیست آی‌پی‌ها (با اینتر یا کاما جدا کنید)" else "IP Addresses List (Separated by newlines or commas)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = customIpsInput,
                        onValueChange = { customIpsInput = it },
                        placeholder = { Text("1.1.1.1, 1.0.0.1, ...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = ExpressiveCardShape
                    )
                }
            }
        }

        // Run Scan Button
        Button(
            onClick = {
                if (!isScanning) {
                    isScanning = true
                    scanMessage = null
                    scope.launch {
                        val customList = if (activePreset == "custom") {
                            customIpsInput.split(Regex("[,\\n]")).map { it.trim() }.filter { it.isNotEmpty() }
                        } else {
                            emptyList()
                        }
                        
                        val result = CdnIpScanner.performScan(
                            preset = activePreset,
                            customIps = customList,
                            port = 443,
                            timeoutMs = 650
                        )
                        scannedIpsList = result.workingIps
                        isScanning = false
                        if (result.workingIpsCount == 0) {
                            scanMessage = if (isFarsi) "هیچ آی‌پی تمیزی یافت نشد." else "No clean CDN IPs resolved."
                        }
                    }
                }
            },
            enabled = !isScanning,
            shape = ExpressiveButtonShape,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFarsi) "در حال تست آی‌پی‌ها..." else "Testing IP Latency...")
            } else {
                Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isFarsi) "شروع اسکن تاخیر" else "Run Clean IP Scan")
            }
        }

        // Results Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(ExpressiveCardShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), ExpressiveCardShape)
                .padding(16.dp)
        ) {
            if (scannedIpsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = scanMessage ?: (if (isFarsi) "روی دکمه شروع اسکن کلیک کنید تا تاخیر آی‌پی‌ها تست شود." else "Press the button above to run latency tests."),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isFarsi) "نتایج اسکن آی‌پی" else "IP Latency Scan Results",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${scannedIpsList.size} IP(s)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(scannedIpsList) { index, item ->
                            val isBest = index == 0
                            val latency = item.latencyMs
                            val badgeColor = when {
                                latency < 180 -> Color(0xFF2E7D32)
                                latency < 350 -> Color(0xFFEF6C00)
                                else -> MaterialTheme.colorScheme.error
                            }

                            Card(
                                shape = ExpressiveChipShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        // Copy to clipboard
                                        try {
                                            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                                            val selection = java.awt.datatransfer.StringSelection(item.ip)
                                            clipboard.setContents(selection, null)
                                            // Show simple message or log
                                            println("[Chameleon] Copied IP to clipboard: ${item.ip}")
                                        } catch (e: Exception) {}
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isBest) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Fastest",
                                                tint = Color(0xFFFBC02D),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = item.ip,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(badgeColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$latency ms",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = badgeColor
                                            )
                                        }
                                        
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy IP",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
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
