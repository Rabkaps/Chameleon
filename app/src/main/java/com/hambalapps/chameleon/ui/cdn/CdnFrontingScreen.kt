package com.hambalapps.chameleon.ui.cdn

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.hambalapps.chameleon.ui.main.pressScaleEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.data.SettingsManager
import com.hambalapps.chameleon.vpn.CdnIpScanner
import com.hambalapps.chameleon.vpn.ScanResult
import com.hambalapps.chameleon.vpn.ScannedIp
import com.hambalapps.chameleon.vpn.VpnServiceWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val ExpressiveCardShape = RoundedCornerShape(28.dp)
val ExpressivePillShape = RoundedCornerShape(50)

private fun restartVpnService(context: Context) {
    val intent = Intent(context, VpnServiceWrapper::class.java).apply {
        action = VpnServiceWrapper.ACTION_START
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CdnFrontingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    val isEnabled by settingsManager.globalCamouflageEnabled.collectAsStateWithLifecycle(initialValue = false)
    val preset by settingsManager.globalCamouflagePreset.collectAsStateWithLifecycle(initialValue = "cloudflare")
    val customSni by settingsManager.globalCamouflageSni.collectAsStateWithLifecycle(initialValue = "")
    val customHost by settingsManager.globalCamouflageHost.collectAsStateWithLifecycle(initialValue = "")
    val customIps by settingsManager.globalCamouflageCustomIps.collectAsStateWithLifecycle(initialValue = "")
    val timeoutStr by settingsManager.globalCamouflageTimeout.collectAsStateWithLifecycle(initialValue = "600")
    val pinnedIp by settingsManager.globalCamouflagePinnedIp.collectAsStateWithLifecycle(initialValue = "")

    val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()

    var isScanning by remember { mutableStateOf(false) }
    var scanResults by remember { mutableStateOf<List<ScannedIp>>(emptyList()) }
    var scanSummary by remember { mutableStateOf<String?>(null) }
    var triggerScan by remember { mutableStateOf(0) }
    
    var sampleDensity by remember { mutableFloatStateOf(50f) }
    var isCustomDensity by remember { mutableStateOf(false) }
    var customDensityInput by remember { mutableStateOf("50") }

    val activeSampleCount = remember(sampleDensity, isCustomDensity, customDensityInput) {
        if (isCustomDensity) {
            customDensityInput.toIntOrNull()?.coerceAtLeast(1) ?: 50
        } else {
            sampleDensity.toInt()
        }
    }

    val motionScheme = MaterialTheme.motionScheme
    val spatialSpec = remember(motionScheme) { motionScheme.defaultSpatialSpec<Float>() }

    // Load initial scan results for current preset
    LaunchedEffect(preset, triggerScan) {
        val stored = CdnIpScanner.lastScanResults[preset]
        if (stored != null && stored.isNotEmpty()) {
            scanResults = stored
        }
    }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun startIpScan(count: Int = activeSampleCount) {
        if (isScanning) return
        scope.launch {
            isScanning = true
            scanSummary = "Scanning CDN Edges ($count IPs)..."
            val customIpsList = if (preset == "custom" || customIps.isNotEmpty()) {
                customIps.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            val timeoutMs = timeoutStr.toIntOrNull() ?: 600

            val result: ScanResult = withContext(Dispatchers.IO) {
                if (customIpsList.any { it.contains("/") }) {
                    CdnIpScanner.performCidrScan(
                        cidrs = customIpsList,
                        sampleCount = count,
                        port = 443,
                        timeoutMs = timeoutMs
                    )
                } else {
                    CdnIpScanner.performScan(
                        preset = preset,
                        customIps = customIpsList,
                        port = 443,
                        timeoutMs = timeoutMs
                    )
                }
            }

            scanResults = result.workingIps
            if (result.workingIpsCount > 0) {
                scanSummary = "Found ${result.workingIpsCount} clean CDN edge IPs. Fastest: ${result.fastestIp} (${result.fastestLatencyMs} ms)"
                val cleanPoolStr = result.workingIps.take(5).map { it.ip }.joinToString(",")
                settingsManager.setGlobalCamouflageCleanPool(cleanPoolStr)
            } else {
                scanSummary = "No clean CDN edge IPs responded within ${timeoutMs}ms"
            }
            isScanning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CDN Fronting & Clean IP", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isEnabled) "Active Domain Fronting Override" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Main Toggle Card with Expressive Spring Animation
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Enable CDN Domain Fronting",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Overlays proxy outbounds with clean CDN edge IPs to defeat SNI throttling",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        settingsManager.setGlobalCamouflageEnabled(checked)
                                        if (vpnState == "CONNECTED") {
                                            restartVpnService(context)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (isEnabled) {
                // Active Clean IP Banner
                item {
                    val activeIpText = if (pinnedIp.isNotEmpty()) pinnedIp else (scanResults.firstOrNull()?.ip ?: "Auto-Scanning CDN Edge...")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active Clean Edge IP",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeIpText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (pinnedIp.isNotEmpty()) {
                                    Text(
                                        text = "Pinned by User (Overrides Scanner)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (scanResults.isNotEmpty()) {
                                    Text(
                                        text = "Auto-Selected Lowest Latency (${scanResults.first().latencyMs} ms)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (pinnedIp.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            settingsManager.setGlobalCamouflagePinnedIp("")
                                            if (vpnState == "CONNECTED") restartVpnService(context)
                                        }
                                    }
                                ) {
                                    Text("Unpin & Auto")
                                }
                            }
                        }
                    }
                }

                // CDN Provider Selection Chips
                item {
                    Column {
                        Text(
                            text = "CDN Infrastructure Provider",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(
                                listOf(
                                    "cloudflare" to "Cloudflare",
                                    "cloudfront" to "CloudFront",
                                    "fastly" to "Fastly",
                                    "custom" to "Custom"
                                )
                            ) { (presetVal, label) ->
                                FilterChip(
                                    selected = preset == presetVal,
                                    onClick = {
                                        scope.launch {
                                            settingsManager.setGlobalCamouflagePreset(presetVal)
                                            if (presetVal == "cloudflare") {
                                                settingsManager.setGlobalCamouflageSni("speedtest.net")
                                                settingsManager.setGlobalCamouflageHost("speedtest.net")
                                            } else if (presetVal == "cloudfront") {
                                                settingsManager.setGlobalCamouflageSni("aws.amazon.com")
                                                settingsManager.setGlobalCamouflageHost("aws.amazon.com")
                                            }
                                            if (vpnState == "CONNECTED") restartVpnService(context)
                                        }
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) },
                                    shape = ExpressivePillShape,
                                    modifier = Modifier.pressScaleEffect()
                                )
                            }
                        }
                    }
                }

                // Subnet Range Chips & CIDR Scanner Controls (Always Visible)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Subnet Ranges & Power-User Scanner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            
                            Text("Preset Subnet Ranges (Tap to Select / Scan)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(CdnIpScanner.PRESET_SUBNETS.toList()) { (label, cidr) ->
                                    FilterChip(
                                        selected = customIps.contains(cidr),
                                        onClick = {
                                            val updated = if (customIps.contains(cidr)) {
                                                customIps.replace(cidr, "").replace(",,", ",").trim(',', ' ')
                                            } else {
                                                if (customIps.isBlank()) cidr else "$customIps, $cidr"
                                            }
                                            scope.launch {
                                                settingsManager.setGlobalCamouflageCustomIps(updated)
                                                if (updated.isNotEmpty()) settingsManager.setGlobalCamouflagePreset("custom")
                                            }
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                        shape = ExpressivePillShape,
                                        modifier = Modifier.pressScaleEffect()
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = customIps,
                                onValueChange = { input ->
                                    scope.launch {
                                        settingsManager.setGlobalCamouflageCustomIps(input)
                                        if (input.isNotEmpty()) settingsManager.setGlobalCamouflagePreset("custom")
                                    }
                                },
                                label = { Text("Target Subnet Ranges / CIDR IP Pool") },
                                placeholder = { Text("e.g. 104.16.0.0/13, 172.64.0.0/13, 162.159.0.0/16") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )

                            // M3 Expressive Spring Motion Slider & Custom Input Field
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Concurrent IP Scan Density", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Surface(
                                    shape = ExpressivePillShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = "$activeSampleCount IPs",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (!isCustomDensity) {
                                Slider(
                                    value = sampleDensity,
                                    onValueChange = { sampleDensity = it },
                                    valueRange = 10f..300f,
                                    steps = 28,
                                    modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                                )
                            } else {
                                OutlinedTextField(
                                    value = customDensityInput,
                                    onValueChange = { input -> customDensityInput = input.filter { char -> char.isDigit() } },
                                    label = { Text("Custom Concurrent IP Count (e.g. 500)") },
                                    placeholder = { Text("Enter any integer number of IPs") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                items(listOf(20, 50, 100, 250)) { presetCount ->
                                    FilterChip(
                                        selected = !isCustomDensity && sampleDensity.toInt() == presetCount,
                                        onClick = {
                                            isCustomDensity = false
                                            sampleDensity = presetCount.toFloat()
                                        },
                                        label = { Text("$presetCount IPs", fontWeight = FontWeight.Bold) },
                                        shape = ExpressivePillShape,
                                        modifier = Modifier.pressScaleEffect()
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = isCustomDensity,
                                        onClick = { isCustomDensity = !isCustomDensity },
                                        label = { Text(if (isCustomDensity) "Use Slider" else "Custom Count...", fontWeight = FontWeight.Bold) },
                                        leadingIcon = { Icon(if (isCustomDensity) Icons.Default.Tune else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        shape = ExpressivePillShape,
                                        modifier = Modifier.pressScaleEffect()
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom SNI & Host Override Fields
                if (preset == "custom") {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Custom SNI & Host Overrides", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                OutlinedTextField(
                                    value = customSni,
                                    onValueChange = { scope.launch { settingsManager.setGlobalCamouflageSni(it) } },
                                    label = { Text("Target Fronting SNI (server_name)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                OutlinedTextField(
                                    value = customHost,
                                    onValueChange = { scope.launch { settingsManager.setGlobalCamouflageHost(it) } },
                                    label = { Text("Transport Host Header") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp)
                                )
                            }
                        }
                    }
                }

                // Scan Action Card
                item {
                    Button(
                        onClick = { startIpScan(activeSampleCount) },
                        enabled = !isScanning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressivePillShape,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        if (isScanning) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Scanning CDN Edge IPs...")
                        } else {
                            Icon(imageVector = Icons.Default.Radar, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan Clean CDN Edge IPs", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (scanSummary != null) {
                    item {
                        Text(
                            text = scanSummary!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // Scanned Working IPs List Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Scanned Edge IPs (${scanResults.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (scanResults.isEmpty() && !isScanning) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveCardShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ) {
                            Box(
                                modifier = Modifier.padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tap 'Scan Clean CDN Edge IPs' above to test and discover the fastest clean CDN IPs for your network.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(scanResults, key = { it.ip }) { scannedIp ->
                        val isPinned = pinnedIp == scannedIp.ip
                        val latencyColor = when {
                            scannedIp.latencyMs < 50 -> Color(0xFF4CAF50)
                            scannedIp.latencyMs < 120 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
                                .border(
                                    width = if (isPinned) 2.dp else 1.dp,
                                    color = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = ExpressiveCardShape
                                ),
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = scannedIp.ip,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = ExpressivePillShape,
                                            color = latencyColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${scannedIp.latencyMs} ms",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = latencyColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (isPinned) {
                                        Text(
                                            text = "PINNED CLEAN IP",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { copyToClipboard(scannedIp.ip, "Clean IP") }) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy IP")
                                    }
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                if (isPinned) {
                                                    settingsManager.setGlobalCamouflagePinnedIp("")
                                                } else {
                                                    settingsManager.setGlobalCamouflagePinnedIp(scannedIp.ip)
                                                }
                                                if (vpnState == "CONNECTED") restartVpnService(context)
                                            }
                                        },
                                        colors = if (isPinned) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        else ButtonDefaults.filledTonalButtonColors(),
                                        shape = ExpressivePillShape
                                    ) {
                                        Text(if (isPinned) "Pinned" else "Pin IP")
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
