package com.hambalapps.chameleon.ui.main

import android.app.Activity
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.app.NotificationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.res.stringResource
import com.hambalapps.chameleon.R
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.chameleon.data.SettingsManager
import com.hambalapps.chameleon.data.UserSettings
import com.hambalapps.chameleon.data.Subscription
import com.hambalapps.chameleon.data.serializeSubscriptions
import com.hambalapps.chameleon.data.deserializeSubscriptions
import com.hambalapps.chameleon.data.ProxyChain
import com.hambalapps.chameleon.data.CamouflageConfig
import com.hambalapps.chameleon.data.deserializeProxyChains
import com.hambalapps.chameleon.data.serializeProxyChains
import com.hambalapps.chameleon.data.deserializeCamouflageSettings
import com.hambalapps.chameleon.data.serializeCamouflageSettings
import com.hambalapps.chameleon.vpn.VpnServiceWrapper
import com.hambalapps.chameleon.vpn.ConfigInjector
import com.hambalapps.chameleon.vpn.measurePingDelay
import com.hambalapps.chameleon.vpn.getHostAndPortFromLink
import com.hambalapps.chameleon.vpn.tryBase64Decode
import com.hambalapps.chameleon.vpn.ProxyNameResolver
import com.hambalapps.chameleon.vpn.registerWarpAccount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.hambalapps.chameleon.SplitTunneling
import com.hambalapps.chameleon.ui.qr.QrScannerScreen
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.hambalapps.chameleon.Config
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer

// Expressive shapes defining Material 3 Expressive aesthetics
val ExpressiveCardShape = RoundedCornerShape(28.dp)
val ExpressiveButtonShape = RoundedCornerShape(16.dp)
val ExpressiveChipShape = RoundedCornerShape(12.dp)

private fun compositeColor(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha
    return Color(
        red = foreground.red * alpha + background.red * (1f - alpha),
        green = foreground.green * alpha + background.green * (1f - alpha),
        blue = foreground.blue * alpha + background.blue * (1f - alpha),
        alpha = 1f
    )
}

@Composable
fun VibrantCardContent(
    cardStyle: String,
    isSecondary: Boolean = false,
    content: @Composable () -> Unit
) {
    val originalColorScheme = MaterialTheme.colorScheme
    val textCol = if (isSecondary) originalColorScheme.onSecondaryContainer else originalColorScheme.onPrimaryContainer
    val bgCol = if (isSecondary) originalColorScheme.secondaryContainer else originalColorScheme.primaryContainer
    
    val targetColorScheme = if (cardStyle == "vibrant" || cardStyle == "solid") {
        originalColorScheme.copy(
            primary = textCol,
            onPrimary = bgCol,
            primaryContainer = textCol.copy(alpha = 0.20f),
            onPrimaryContainer = textCol,
            
            secondary = textCol,
            onSecondary = bgCol,
            secondaryContainer = textCol.copy(alpha = 0.20f),
            onSecondaryContainer = textCol,
            
            tertiary = textCol,
            onTertiary = bgCol,
            tertiaryContainer = textCol.copy(alpha = 0.20f),
            onTertiaryContainer = textCol,
            
            surface = bgCol,
            onSurface = textCol,
            onSurfaceVariant = textCol.copy(alpha = 0.80f),
            surfaceVariant = textCol.copy(alpha = 0.15f),
            
            outline = textCol.copy(alpha = 0.45f),
            outlineVariant = textCol.copy(alpha = 0.25f),
            onError = Color.White,
            
            surfaceContainerLowest = textCol.copy(alpha = 0.05f),
            surfaceContainerLow = textCol.copy(alpha = 0.10f),
            surfaceContainer = textCol.copy(alpha = 0.15f),
            surfaceContainerHigh = textCol.copy(alpha = 0.20f),
            surfaceContainerHighest = textCol.copy(alpha = 0.25f)
        )
    } else {
        originalColorScheme
    }

    androidx.compose.material3.MaterialExpressiveTheme(
        colorScheme = targetColorScheme
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            content = content
        )
    }
}

@Composable
fun ExpressiveCard(
    modifier: Modifier = Modifier,
    brush: Brush,
    shape: androidx.compose.ui.graphics.Shape = ExpressiveCardShape,
    borderBrush: Brush,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    cardStyle: String,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(brush = brush, shape = shape)
                .then(if (cardStyle == "glass" && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Modifier.blur(20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                } else {
                    Modifier
                })
                .border(width = borderWidth, brush = borderBrush, shape = shape)
        )
        VibrantCardContent(cardStyle) {
            content()
        }
    }
}

private fun handleScannedQrResult(
    result: String,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    settingsManager: com.hambalapps.chameleon.data.SettingsManager,
    subscriptions: List<com.hambalapps.chameleon.data.Subscription>,
    manualServersStr: String,
    vpnState: String
) {
    val trimmedImport = result.trim()
    if (trimmedImport.isNotEmpty()) {
        scope.launch {
            if (trimmedImport.startsWith("http://") || trimmedImport.startsWith("https://")) {
                try {
                    val fetchResult = fetchSubscription(trimmedImport)
                    if (fetchResult.servers.isNotEmpty()) {
                        val domain = try {
                            java.net.URI(trimmedImport).host ?: "Subscription"
                        } catch (e: Exception) {
                            "Subscription"
                        }
                        val newSub = com.hambalapps.chameleon.data.Subscription(
                            id = java.util.UUID.randomUUID().toString(),
                            name = domain,
                            url = trimmedImport,
                            servers = fetchResult.servers.joinToString("\n"),
                            upload = fetchResult.upload,
                            download = fetchResult.download,
                            total = fetchResult.total,
                            expire = fetchResult.expire
                        )
                        val updatedList = subscriptions + newSub
                        settingsManager.setSubscriptionList(com.hambalapps.chameleon.data.serializeSubscriptions(updatedList))
                        settingsManager.setActiveSubId(newSub.id)
                        settingsManager.setActiveProfile(fetchResult.servers[0])
                        android.widget.Toast.makeText(context, "Subscription imported successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        if (vpnState == "CONNECTED") {
                            startVpnService(context)
                        }
                    } else {
                        android.widget.Toast.makeText(context, "No configs found in subscription link", android.widget.Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to fetch subscription: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } else {
                val finalLink = if (trimmedImport.contains("dev tun") || trimmedImport.lowercase().startsWith("client") || trimmedImport.lowercase().contains("client\n") || trimmedImport.lowercase().contains("client\r")) {
                    val b64 = android.util.Base64.encodeToString(trimmedImport.toByteArray(), android.util.Base64.NO_WRAP)
                    "openvpn://vpn?config=$b64#OpenVPN_Imported"
                } else if (trimmedImport.contains("[Interface]") && trimmedImport.contains("[Peer]")) {
                    val b64 = android.util.Base64.encodeToString(trimmedImport.toByteArray(), android.util.Base64.NO_WRAP)
                    "awg://vpn?config=$b64#AmneziaWG_Imported"
                } else {
                    trimmedImport
                }
                val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                val newLinkWithoutRemark = finalLink.substringBefore("#")
                val updatedManualList = (currentManualList.filter { it.substringBefore("#") != newLinkWithoutRemark } + finalLink).distinct()
                settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                settingsManager.setActiveSubId("manual")
                settingsManager.setActiveProfile(finalLink)
                android.widget.Toast.makeText(context, "Config imported successfully!", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
fun FlagTextRow(
    text: String,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    if (text.contains("🇮🇷")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = androidx.compose.ui.res.painterResource(id = com.hambalapps.chameleon.R.drawable.ic_lion_sun_flag),
                contentDescription = "Iran Flag",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text.replace("🇮🇷", "").trim(),
                style = style,
                color = color,
                maxLines = maxLines,
                overflow = overflow
            )
        }
    } else {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onItemClick: (androidx.navigation3.runtime.NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val standardColorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }

    // Observe settings from DataStore
    val settings by settingsManager.settings.collectAsStateWithLifecycle(initialValue = SettingsManager.defaultSettings)
    val cardStyle = settings.cardStyle
    val isAdvancedMode = settings.isAdvancedMode
    val bypassIran = settings.bypassIran
    val bypassLan = settings.bypassLan
    val secureDns = settings.secureDns
    val tunStack = settings.tunStack
    val enableFragment = settings.enableFragment
    val fragmentLength = settings.fragmentLength
    val fragmentInterval = settings.fragmentInterval
    val enableMux = settings.enableMux
    val enableDebugLogging = settings.enableDebugLogging
    val activeProfile = settings.activeProfile
    val subscriptionUrl = settings.subscriptionUrl
    val subscriptionListStr = settings.subscriptionList
    val activeSubId = settings.activeSubId
    val showLiveNotification = settings.showLiveNotification
    val splitTunnelingEnabled = settings.splitTunnelingEnabled
    val splitTunnelingApps = settings.splitTunnelingApps
    val splitTunnelingMode = settings.splitTunnelingMode
    val manualServersStr = settings.manualServers
    val autoUpdateSubs = settings.autoUpdateSubs
    val autoUpdateInterval = settings.autoUpdateInterval
    val lastSubsUpdateTime = settings.lastSubsUpdateTime
    val autoConnectSubs = settings.autoConnectSubs
    val rootMode = settings.rootMode
    val showLogsTab = settings.showLogsTab
    val vpnMode = settings.vpnMode
    val warpPrivateKey = settings.warpPrivateKey
    val warpPublicKey = settings.warpPublicKey
    val warpIpAddress = settings.warpIpAddress
    val warpClientId = settings.warpClientId
    val vpnModeTunnelGames = settings.vpnModeTunnelGames
    val delayTestUrl = settings.delayTestUrl
    val warpDetourMode = settings.warpDetourMode
    val warpPort = settings.warpPort

    val subscriptions = settings.deserializedSubscriptions
    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }

    val serverList = remember(subscriptions, manualServersStr) {
        val list = mutableListOf<String>()
        subscriptions.forEach { sub ->
            sub.servers.split("\n").forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) list.add(trimmed)
            }
        }
        manualServersStr.split("\n").forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) list.add(trimmed)
        }
        list.distinct()
    }

    var showLivePromoGuide by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

    // Check battery optimization exemption on launch
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                if (!isIgnoring) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        showBatteryOptimizationDialog = true
                    }
                }
            }
        }
    }

    // Auto subscription update check on launch
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val currentSettings = settingsManager.settings.first()
            val autoUpdate = currentSettings.autoUpdateSubs
            if (autoUpdate) {
                val interval = currentSettings.autoUpdateInterval
                val lastTime = currentSettings.lastSubsUpdateTime
                val currentTime = System.currentTimeMillis()
                
                val shouldUpdate = when (interval) {
                    "startup" -> true
                    "hourly" -> (currentTime - lastTime) >= currentSettings.autoUpdateIntervalHours * 60 * 60 * 1000L
                    "daily" -> (currentTime - lastTime) >= 24 * 60 * 60 * 1000L
                    "weekly" -> (currentTime - lastTime) >= 7 * 24 * 60 * 60 * 1000L
                    else -> false
                }
                
                if (shouldUpdate) {
                    // Delay 5 seconds on startup to allow the network interfaces to fully initialize
                    kotlinx.coroutines.delay(5000)
                    try {
                        val currentListStr = currentSettings.subscriptionList
                        val currentSubs = deserializeSubscriptions(currentListStr)
                        var anyUpdated = false
                        var updateFailed = false
                        val updatedSubs = currentSubs.map { sub ->
                            if (!sub.url.startsWith("local://")) {
                                try {
                                    val result = fetchSubscription(sub.url)
                                    if (result.servers.isNotEmpty()) {
                                        anyUpdated = true
                                        sub.copy(
                                            servers = result.servers.joinToString("\n"),
                                            upload = result.upload,
                                            download = result.download,
                                            total = result.total,
                                            expire = result.expire
                                        )
                                    } else {
                                        sub
                                    }
                                } catch (e: Exception) {
                                    updateFailed = true
                                    sub
                                }
                            } else {
                                sub
                            }
                        }

                        if (anyUpdated) {
                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedSubs.filter { !it.url.startsWith("local://") }))
                            
                            val activeSubIdVal = currentSettings.activeSubId
                            val activeProfileVal = currentSettings.activeProfile
                            val updatedActiveSub = updatedSubs.find { it.id == activeSubIdVal }
                            if (updatedActiveSub != null) {
                                val sList = updatedActiveSub.servers.split("\n").filter { it.isNotEmpty() }
                                if (sList.isNotEmpty() && !sList.contains(activeProfileVal)) {
                                    settingsManager.setActiveProfile(sList[0])
                                    if (VpnServiceWrapper.vpnState.value == "CONNECTED") {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            startVpnService(context)
                                        }
                                    }
                                }
                            }
                        }
                        // Only save last update timestamp if all updates succeeded to avoid locking out retries
                        if (!updateFailed) {
                            settingsManager.setLastSubsUpdateTime(currentTime)
                        }
                    } catch (e: Exception) {
                        // Silently handle error
                    }
                }
            }
        }
    }

    // Dedicated server auto-select removed

    // Observe VPN state and logs
    val vpnState by VpnServiceWrapper.vpnState.collectAsStateWithLifecycle()
    val sessionDownBytes by VpnServiceWrapper.sessionDownBytes.collectAsStateWithLifecycle()
    val sessionUpBytes by VpnServiceWrapper.sessionUpBytes.collectAsStateWithLifecycle()
    var appVersion by remember { mutableStateOf("v1.6.12") }
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateDialogTitle by remember { mutableStateOf("") }
    var updateDialogMessage by remember { mutableStateOf("") }
    var updateLatestUrl by remember { mutableStateOf("") }

    fun checkForUpdates() {
        if (isCheckingUpdates) return
        isCheckingUpdates = true
        scope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://api.github.com/repos/Rabkaps/Chameleon/releases/latest")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("User-Agent", "Chameleon-App")
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    
                    if (conn.responseCode == 200) {
                        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = org.json.JSONObject(responseText)
                        val latestTag = json.getString("tag_name").trim()
                        val latestClean = latestTag.removePrefix("v").trim()
                        val currentClean = appVersion.removePrefix("v").trim()
                        
                        if (latestClean != currentClean) {
                            val body = json.optString("body", "")
                            Result.success(Pair(latestTag, body))
                        } else {
                            Result.success(Pair(latestTag, null))
                        }
                    } else {
                        Result.failure(Exception("HTTP error: ${conn.responseCode}"))
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            
            isCheckingUpdates = false
            result.fold(
                onSuccess = { (latestTag, releaseNotes) ->
                    if (releaseNotes != null) {
                        updateDialogTitle = "Update Available"
                        updateDialogMessage = "A new version of Chameleon ($latestTag) is available.\n\n" +
                                "Change Log:\n${releaseNotes.take(300)}${if (releaseNotes.length > 300) "..." else ""}"
                        updateLatestUrl = "https://github.com/Rabkaps/Chameleon/releases/tag/$latestTag"
                    } else {
                        updateDialogTitle = "Up to Date"
                        updateDialogMessage = "You are running the latest version of Chameleon ($appVersion)."
                        updateLatestUrl = ""
                    }
                    showUpdateDialog = true
                },
                onFailure = { error ->
                    updateDialogTitle = "Check Failed"
                    updateDialogMessage = "Unable to check for updates. Please verify your internet connection.\n\nError: ${error.localizedMessage}"
                    updateLatestUrl = ""
                    showUpdateDialog = true
                }
            )
        }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val version = "v${pInfo.versionName ?: "1.6.12"}"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    appVersion = version
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    // Auto-start MTProxy local service on launch if enabled and VPN is off
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val currentSettings = settingsManager.settings.first()
            if (currentSettings.enableMtProxy) {
                val state = VpnServiceWrapper.vpnState.value
                if (state != "CONNECTED" && state != "CONNECTING") {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val intent = Intent(context, VpnServiceWrapper::class.java).apply {
                            action = VpnServiceWrapper.ACTION_START_PROXY
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                    }
                }
            }
        }
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showLogs by remember { mutableStateOf(false) }
    
    // Tabs list ordered as: 1. Servers, 2. Home (Main), 3. Settings, 4. Logs (if enabled)
    val tabs = remember(showLogsTab, context) {
        listOfNotNull(
            Triple(1, context.getString(R.string.tab_servers), Icons.Default.Dns),
            Triple(0, context.getString(R.string.tab_home), Icons.Default.Home),
            Triple(3, context.getString(R.string.tab_settings), Icons.Default.Settings),
            if (showLogsTab) Triple(2, context.getString(R.string.tab_logs), Icons.Default.Terminal) else null
        )
    }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })
    var isRegisteringWarp by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    var subUrlInput by remember { mutableStateOf("") }
    var subNameInput by remember { mutableStateOf("") }
    var isAddFormExpanded by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf(
        stringResource(R.string.tab_all),
        "Favorites",
        "VLESS",
        "Trojan",
        "Shadowsocks",
        "VMess",
        "Hysteria",
        "TUIC",
        "OpenVPN",
        "AmneziaWG",
        "MASQUE"
    )
    var editingSubscription by remember { mutableStateOf<com.hambalapps.chameleon.data.Subscription?>(null) }
    var editSubNameInput by remember { mutableStateOf("") }
    var editSubUrlInput by remember { mutableStateOf("") }
    var showEditSubDialog by remember { mutableStateOf(false) }
    var selectedCountryFilter by remember { mutableStateOf("All Countries") }
    var selectedSubGroupFilter by remember(activeSubscription) { mutableStateOf(activeSubscription?.name ?: "All Groups") }
    var pingsMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedNodes by remember { mutableStateOf(setOf<String>()) }
    var resolvedCountries by remember { mutableStateOf(mapOf<String, String>()) }
    var isTestingPings by remember { mutableStateOf(false) }
    var isUpdatingSubs by remember { mutableStateOf(false) }

    val onUpdateSubscriptions = {
        if (!isUpdatingSubs) {
            scope.launch {
                isUpdatingSubs = true
                var anyUpdated = false
                var updateFailed = false
                val currentSubs = subscriptions
                val updatedSubs = currentSubs.map { sub ->
                    if (!sub.url.startsWith("local://")) {
                        try {
                            val result = fetchSubscription(sub.url)
                            if (result.servers.isNotEmpty()) {
                                anyUpdated = true
                                sub.copy(
                                    servers = result.servers.joinToString("\n"),
                                    upload = result.upload,
                                    download = result.download,
                                    total = result.total,
                                    expire = result.expire
                                )
                            } else {
                                sub
                            }
                        } catch (e: Exception) {
                            updateFailed = true
                            sub
                        }
                    } else {
                        sub
                    }
                }
                if (anyUpdated) {
                    settingsManager.setSubscriptionList(serializeSubscriptions(updatedSubs.filter { !it.url.startsWith("local://") }))
                    val activeSubIdVal = settings.activeSubId
                    val activeProfileVal = settings.activeProfile
                    val updatedActiveSub = updatedSubs.find { it.id == activeSubIdVal }
                    if (updatedActiveSub != null) {
                        val sList = updatedActiveSub.servers.split("\n").filter { it.isNotEmpty() }
                        if (sList.isNotEmpty() && !sList.contains(activeProfileVal)) {
                            settingsManager.setActiveProfile(sList[0])
                            if (vpnState == "CONNECTED") {
                                startVpnService(context)
                            }
                        }
                    }
                    android.widget.Toast.makeText(context, "Subscriptions updated!", android.widget.Toast.LENGTH_SHORT).show()
                } else if (updateFailed) {
                    android.widget.Toast.makeText(context, "Failed to update some subscriptions", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "No updates found", android.widget.Toast.LENGTH_SHORT).show()
                }
                isUpdatingSubs = false
            }
        }
    }

    LaunchedEffect(Unit) {
        IpCountryResolver.init(context)
    }

    LaunchedEffect(serverList) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val newResolved = resolvedCountries.toMutableMap()
            var changed = false
            serverList.forEach { link ->
                val host = getHostAndPortFromLink(link)?.first
                if (host != null) {
                    val cached = IpCountryResolver.getCachedCountryCode(host)
                    if (cached != null) {
                        if (newResolved[link] != cached) {
                            newResolved[link] = cached
                            changed = true
                        }
                    } else {
                        val cc = IpCountryResolver.resolveCountryCode(host)
                        newResolved[link] = cc
                        changed = true
                        kotlinx.coroutines.delay(120)
                    }
                }
            }
            if (changed) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    resolvedCountries = newResolved.toMap()
                }
            }
        }
    }

    val filteredServerList = remember(serverList, searchQuery, selectedTab, selectedCountryFilter, selectedSubGroupFilter, subscriptions, resolvedCountries, pingsMap, settings.favoriteServers) {
        val mapped = serverList.mapIndexedNotNull { originalIndex, serverLink ->
            val rawType = serverLink.substringBefore("://").uppercase()
            val type = when (rawType) {
                "OVPN" -> "OPENVPN"
                "AWG", "WIREGUARD" -> "AMNEZIAWG"
                "HY2" -> "HYSTERIA2"
                else -> rawType
            }
            val selectedTitle = tabTitles.getOrNull(selectedTab) ?: "All"
            val matchesTab = when (selectedTitle) {
                "All" -> true
                "Favorites" -> settings.favoriteServers.contains(serverLink)
                "VLESS" -> type == "VLESS"
                "Trojan" -> type == "TROJAN"
                "Shadowsocks" -> type == "SS" || type == "SHADOWSOCKS"
                "VMess" -> type == "VMESS"
                "Hysteria" -> type == "HYSTERIA" || type == "HYSTERIA2" || type == "HY2"
                "TUIC" -> type == "TUIC"
                "OpenVPN" -> type == "OPENVPN"
                "AmneziaWG" -> type == "AMNEZIAWG"
                "MASQUE" -> type == "MASQUE"
                else -> true
            }
            if (matchesTab) {
                val name = ProxyNameResolver.getProxyName(serverLink, context)
                val matchesSearch = name.contains(searchQuery, ignoreCase = true)
                
                val matchesGroup = when (selectedSubGroupFilter) {
                    "All Groups" -> true
                    "Favorites" -> settings.favoriteServers.contains(serverLink)
                    else -> {
                        val matchingSub = subscriptions.find { it.name == selectedSubGroupFilter }
                        matchingSub?.servers?.split("\n")?.map { it.trim() }?.contains(serverLink.trim()) ?: false
                    }
                }
                
                val matchesCountry = if (selectedCountryFilter == "All Countries") {
                    true
                } else {
                    val emoji = selectedCountryFilter.substringBefore(" ")
                    val countryCode = resolvedCountries[serverLink]
                    getFlagEmoji(name, countryCode) == emoji
                }
                
                if (matchesSearch && matchesGroup && matchesCountry) {
                    ServerItem(
                        id = "${serverLink}_$originalIndex",
                        link = serverLink,
                        name = name,
                        type = type,
                        transport = getTransportType(serverLink)
                    )
                } else null
            } else null
        }
        
        mapped.sortedWith { item1, item2 ->
            val p1 = pingsMap[item1.link] ?: -1
            val p2 = pingsMap[item2.link] ?: -1
            val latency1 = if (p1 < 0) Int.MAX_VALUE else p1
            val latency2 = if (p2 < 0) Int.MAX_VALUE else p2
            latency1.compareTo(latency2)
        }
    }

    var showLoveNoteDialog by remember { mutableStateOf(false) }
    var qrCodeToShare by remember { mutableStateOf<Pair<String, String>?>(null) }
    var currentLoveNote by remember { mutableStateOf("") }
    var scanResultCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var editingNodeLink by remember { mutableStateOf<String?>(null) }
    var editLinkInput by remember { mutableStateOf("") }
    var editorMode by remember { mutableStateOf("form") } // "form" or "link"
    var editType by remember { mutableStateOf("vless") }
    var editRemark by remember { mutableStateOf("") }
    var editUsername by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }
    var editServer by remember { mutableStateOf("") }
    var editPort by remember { mutableStateOf("443") }
    var editCreds by remember { mutableStateOf("") }
    var editTls by remember { mutableStateOf(false) }
    var editSni by remember { mutableStateOf("") }
    var editFlow by remember { mutableStateOf("") }
    var editRealityEnabled by remember { mutableStateOf(false) }
    var editRealityPbk by remember { mutableStateOf("") }
    var editRealitySid by remember { mutableStateOf("") }
    var editRealitySpx by remember { mutableStateOf("") }
    var editUtlsFingerprint by remember { mutableStateOf("chrome") }
    var editShowAdvanced by remember { mutableStateOf(false) }
    var editMasqueProfileId by remember { mutableStateOf("") }
    var editMasqueToken by remember { mutableStateOf("") }
    var editMasqueUseHttp2 by remember { mutableStateOf(false) }
    var editMasqueUseIpv6 by remember { mutableStateOf(false) }
    var editTransportType by remember { mutableStateOf("tcp") }
    var editTransportPath by remember { mutableStateOf("") }
    var editTransportHost by remember { mutableStateOf("") }
    var editTransportServiceName by remember { mutableStateOf("") }
    var editTransportSeed by remember { mutableStateOf("") }
    var editTransportHeaderType by remember { mutableStateOf("none") }
    var editCamouflageEnabled by remember { mutableStateOf(false) }
    var editCamouflagePreset by remember { mutableStateOf("cloudflare") }
    var editCamouflageSni by remember { mutableStateOf("") }
    var editCamouflageHost by remember { mutableStateOf("") }
    var isNodesExpanded by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    androidx.activity.compose.BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }
    val refreshingSubs = remember { mutableStateMapOf<String, Boolean>() }

    // Launcher for VPN system permission dialog
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(context)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrEmpty()) {
                    importText = content
                }
            } catch (e: Exception) {
                android.util.Log.e("Chameleon", "Failed to read chosen file: ${e.message}")
            }
        }
    }

    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val cardBackground = if (isDark) Color.Black else Color(0xFFF7F9FB)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val cardBorderBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, outlineVariant) {
        when (cardStyle) {
            "solid" -> SolidColor(outlineVariant)
            "tonal" -> SolidColor(outlineVariant.copy(alpha = 0.25f))
            "glass" -> {
                val colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                } else {
                    listOf(
                        primaryColor.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.50f)
                    )
                }
                Brush.linearGradient(colors = colors)
            }
            else -> { // vibrant
                val colors = listOf(
                    primaryColor.copy(alpha = if (isDark) 0.8f else 0.4f),
                    secondaryColor.copy(alpha = if (isDark) 0.6f else 0.2f)
                )
                Brush.linearGradient(colors = colors)
            }
        }
    }

    val primaryCardBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, primaryContainer, secondaryContainer, surfaceContainerHigh) {
        when (cardStyle) {
            "solid" -> SolidColor(primaryContainer)
            "vibrant" -> {
                val colors = listOf(
                    primaryContainer,
                    secondaryContainer.copy(alpha = 0.7f)
                )
                Brush.linearGradient(colors = colors)
            }
            "tonal" -> SolidColor(surfaceContainerHigh)
            "glass" -> {
                val colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.03f)
                    )
                } else {
                    listOf(
                        primaryColor.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.65f)
                    )
                }
                Brush.linearGradient(colors = colors)
            }
            else -> SolidColor(surfaceContainerHigh)
        }
    }

    val secondaryCardBrush = remember(isDark, cardStyle, secondaryColor, tertiaryColor, secondaryContainer, tertiaryContainer, surfaceContainer) {
        when (cardStyle) {
            "solid" -> SolidColor(secondaryContainer)
            "vibrant" -> {
                val colors = listOf(
                    secondaryContainer,
                    tertiaryContainer.copy(alpha = 0.7f)
                )
                Brush.linearGradient(colors = colors)
            }
            "tonal" -> SolidColor(surfaceContainer)
            "glass" -> {
                val colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.03f)
                    )
                } else {
                    listOf(
                        secondaryColor.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.65f)
                    )
                }
                Brush.linearGradient(colors = colors)
            }
            else -> SolidColor(surfaceContainer)
        }
    }

    val tertiaryCardBrush = remember(isDark, cardStyle, tertiaryColor, primaryColor, tertiaryContainer, primaryContainer, surfaceContainerLow) {
        when (cardStyle) {
            "solid" -> SolidColor(tertiaryContainer)
            "vibrant" -> {
                val colors = listOf(
                    tertiaryContainer,
                    primaryContainer.copy(alpha = 0.7f)
                )
                Brush.linearGradient(colors = colors)
            }
            "tonal" -> SolidColor(surfaceContainerLow)
            "glass" -> {
                val colors = if (isDark) {
                    listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.03f)
                    )
                } else {
                    listOf(
                        tertiaryColor.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.65f)
                    )
                }
                Brush.linearGradient(colors = colors)
            }
            else -> SolidColor(surfaceContainerLow)
        }
    }

    // Active Card background animation (only runs when connected/connecting and in foreground to save CPU)
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var isActivityResumed by remember { mutableStateOf(lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            isActivityResumed = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    val flowOffsetState = remember { androidx.compose.animation.core.Animatable(0f) }
    val isVpnActive = (vpnState == "CONNECTED" || vpnState == "CONNECTING") && isActivityResumed
    LaunchedEffect(isVpnActive) {
        if (isVpnActive) {
            flowOffsetState.animateTo(
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            flowOffsetState.snapTo(0f)
        }
    }
    val flowOffset = flowOffsetState.value

    val activeCardBackgroundBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, tertiaryColor, primaryContainer, flowOffset, surfaceContainerHigh) {
        when (cardStyle) {
            "solid" -> SolidColor(primaryContainer)
            "tonal" -> SolidColor(surfaceContainerHigh)
            "vibrant" -> {
                Brush.linearGradient(
                    colors = listOf(primaryColor, secondaryColor),
                    start = Offset(flowOffset - 500f, 0f),
                    end = Offset(flowOffset + 500f, 1000f)
                )
            }
            "glass" -> {
                val colors = if (isDark) {
                    listOf(
                        primaryColor.copy(alpha = 0.25f),
                        secondaryColor.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                } else {
                    listOf(
                        primaryColor.copy(alpha = 0.12f),
                        secondaryColor.copy(alpha = 0.08f),
                        Color.Black.copy(alpha = 0.02f)
                    )
                }
                Brush.linearGradient(
                    colors = colors,
                    start = Offset(flowOffset - 500f, 0f),
                    end = Offset(flowOffset + 500f, 1000f)
                )
            }
            else -> SolidColor(surfaceContainerHigh)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight(),
                drawerContainerColor = if (MaterialTheme.colorScheme.background.red < 0.5f) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.hambalapps.chameleon.R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = context.getString(com.hambalapps.chameleon.R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = if (Config.IS_SPECIAL) stringResource(R.string.app_name_special) else stringResource(R.string.app_name_standard),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = if (Config.IS_SPECIAL) "Developed with love by Gumball for Sana. Featuring a custom reactive visualizer, Monet adaptive themes, and stable key signing." 
                               else stringResource(R.string.app_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { checkForUpdates() },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Check Updates",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/Rabkaps/Chameleon"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.hambalapps.chameleon.R.drawable.ic_github),
                                contentDescription = "GitHub Repo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (Config.IS_SPECIAL) {
                        PawPrint(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.secure_network_engine),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )

                    if (showUpdateDialog) {
                        AlertDialog(
                            onDismissRequest = { showUpdateDialog = false },
                            title = { Text(text = updateDialogTitle, fontWeight = FontWeight.Bold) },
                            text = { Text(text = updateDialogMessage) },
                            confirmButton = {
                                if (updateLatestUrl.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            showUpdateDialog = false
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateLatestUrl))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                        }
                                    ) {
                                        Text("Download")
                                    }
                                } else {
                                    Button(onClick = { showUpdateDialog = false }) {
                                        Text("OK")
                                    }
                                }
                            },
                            dismissButton = {
                                if (updateLatestUrl.isNotEmpty()) {
                                    TextButton(onClick = { showUpdateDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            },
                            shape = ExpressiveCardShape,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (Config.IS_SPECIAL) {
                PeakingKitty(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 12.dp, y = (-80).dp)
                        .graphicsLayer {
                            rotationZ = -90f
                        }
                )
            }
            Scaffold(
            topBar = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    context.getString(com.hambalapps.chameleon.R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                if (Config.IS_SPECIAL) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    PawPrint(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { _, dragAmount ->
                                            if (dragAmount > 10f) {
                                                scope.launch { drawerState.open() }
                                            }
                                        }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = stringResource(R.string.open_settings_drawer),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        actions = {
                            if (showLogsTab) {
                                val targetLogPageIdx = remember(tabs) { tabs.indexOfFirst { it.first == 2 } }
                                if (targetLogPageIdx >= 0) {
                                    IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(targetLogPageIdx) } }) {
                                        Icon(
                                            imageVector = Icons.Default.Terminal,
                                            contentDescription = stringResource(R.string.show_logs),
                                            tint = if (pagerState.targetPage == targetLogPageIdx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    )
                    if (Config.IS_SPECIAL) {
                        PeakingKitty(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-18).dp)
                        )
                    }
                }
            },
            bottomBar = {
                if (isMultiSelectMode) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        color = androidx.compose.ui.graphics.lerp(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.error,
                            0.05f
                        ).copy(alpha = 0.95f),
                        tonalElevation = 6.dp,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isMultiSelectMode = false
                                        selectedNodes = emptySet()
                                    },
                                    modifier = Modifier.pressScaleEffect()
                                ) {
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val allFilteredManuals = remember(filteredServerList, manualServersStr) {
                                    filteredServerList.filter { item ->
                                        manualServersStr.split("\n").map { it.trim() }.contains(item.link.trim())
                                    }.map { it.link }.toSet()
                                }
                                val isAllSelected = selectedNodes.containsAll(allFilteredManuals) && allFilteredManuals.isNotEmpty()
                                TextButton(
                                    onClick = {
                                        if (isAllSelected) {
                                            val nextSelection = selectedNodes - allFilteredManuals
                                            selectedNodes = nextSelection
                                            if (nextSelection.isEmpty()) {
                                                isMultiSelectMode = false
                                            }
                                        } else {
                                            selectedNodes = selectedNodes + allFilteredManuals
                                        }
                                    },
                                    modifier = Modifier.pressScaleEffect()
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
                                    },
                                    modifier = Modifier.pressScaleEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val tabId = if (pageIndex < tabs.size) tabs[pageIndex].first else 0
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                val scale = 0.92f + (1f - 0.92f) * (1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f))
                                val alpha = 0.5f + (1f - 0.5f) * (1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f))
                                this.scaleX = scale
                                this.scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        when (tabId) {
                    0 -> { // Home Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            ConnectionDashboard(
                                state = vpnState,
                                cardStyle = cardStyle,
                                isDark = isDark,
                                delayTestUrl = delayTestUrl,
                                activeProfile = activeProfile,
                                activeSubId = activeSubId,
                                subscriptions = subscriptions,
                                vpnMode = vpnMode,
                                vpnModeTunnelGames = vpnModeTunnelGames,
                                sessionDownBytesProvider = { sessionDownBytes },
                                sessionUpBytesProvider = { sessionUpBytes },
                                settingsManager = settingsManager,
                                scope = scope,
                                onConnectToggle = {
                                    if (vpnState == "CONNECTED") {
                                        stopVpnService(context)
                                    } else {
                                        if (activeProfile.trim().isEmpty()) {
                                            android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.notif_no_node),
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            val intent = VpnService.prepare(context)
                                            if (intent != null) {
                                                vpnPermissionLauncher.launch(intent)
                                            } else {
                                                startVpnService(context)
                                            }
                                        }
                                    }
                                },
                                onNavigateToServers = {
                                    val idx = tabs.indexOfFirst { it.first == 1 }
                                    if (idx >= 0) {
                                        scope.launch { pagerState.animateScrollToPage(idx) }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                    1 -> { // Servers Tab
                        val configuration = LocalConfiguration.current
                        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                        val screenWidthDp = configuration.screenWidthDp
                        val useDropdownMenu = !isLandscape || screenWidthDp < 600

                        var showSubManagerDialog by remember { mutableStateOf(false) }
                        var isGroupDropdownExpanded by remember { mutableStateOf(false) }
                        var isCountryDropdownExpanded by remember { mutableStateOf(false) }
                        
                        val uniqueCountries = remember(serverList, resolvedCountries) {
                            val list = mutableSetOf("All Countries")
                            serverList.forEach { link ->
                                val name = ProxyNameResolver.getProxyName(link, context)
                                val countryCode = resolvedCountries[link]
                                val emoji = getFlagEmoji(name, countryCode)
                                if (emoji != "🌐") {
                                    val cc = countryCode ?: when (emoji) {
                                        "🇩🇪" -> "DE"
                                        "🇪🇸" -> "ES"
                                        "🇯🇵" -> "JP"
                                        "🇺🇸" -> "US"
                                        "🇬🇧" -> "GB"
                                        "🇫🇷" -> "FR"
                                        "🇳🇱" -> "NL"
                                        "🇸🇬" -> "SG"
                                        "🇹🇷" -> "TR"
                                        "🇨🇦" -> "CA"
                                        "🇮🇷" -> "IR"
                                        "🇫🇮" -> "FI"
                                        "🇸🇪" -> "SE"
                                        "🇮🇹" -> "IT"
                                        "🇨🇭" -> "CH"
                                        "🇦🇪" -> "AE"
                                        "🇭🇰" -> "HK"
                                        "🇰🇷" -> "KR"
                                        else -> null
                                    }
                                    val countryName = if (cc != null) {
                                        java.util.Locale("", cc).getDisplayCountry(java.util.Locale.US)
                                    } else ""
                                    if (countryName.isNotEmpty()) {
                                        list.add("$emoji $countryName")
                                    }
                                }
                            }
                            list.toList()
                        }

                        val subGroups = remember(subscriptions) {
                            listOf("All Groups", "Favorites") + subscriptions.map { it.name }
                        }

                        val subscriptionManagerCard: @Composable (Modifier, Modifier) -> Unit = { modifier, listModifier ->
                            Card(
                                modifier = modifier
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(
                                        width = 1.dp,
                                        brush = cardBorderBrush,
                                        shape = ExpressiveCardShape
                                    )
                                    .animateContentSize(),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.Transparent
                                )
                            ) {
                                VibrantCardContent(settings.cardStyle) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = stringResource(R.string.sub_manager),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(R.string.sub_count, subscriptions.size),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        FilledTonalIconToggleButton(
                                            checked = isAddFormExpanded,
                                            onCheckedChange = { isAddFormExpanded = it },
                                            modifier = Modifier.pressScaleEffect()
                                        ) {
                                            Icon(
                                                imageVector = if (isAddFormExpanded) Icons.Default.Close else Icons.Default.Add,
                                                contentDescription = stringResource(R.string.add_sub)
                                            )
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = isAddFormExpanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            OutlinedTextField(
                                                value = subNameInput,
                                                onValueChange = { subNameInput = it },
                                                label = { Text(stringResource(R.string.sub_name_label)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = ExpressiveButtonShape,
                                                singleLine = true,
                                                placeholder = { Text(stringResource(R.string.sub_name_placeholder)) }
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedTextField(
                                                value = subUrlInput,
                                                onValueChange = { subUrlInput = it },
                                                label = { Text(stringResource(R.string.sub_link_label)) },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = ExpressiveButtonShape,
                                                singleLine = true,
                                                placeholder = { Text("https://example.com/sub") },
                                                trailingIcon = {
                                                    IconButton(
                                                        onClick = {
                                                            scanResultCallback = { result ->
                                                                subUrlInput = result
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.QrCodeScanner,
                                                            contentDescription = stringResource(R.string.scan_qr_code)
                                                        )
                                                    }
                                                }
                                            )
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (subUrlInput.isNotEmpty()) {
                                                            scope.launch {
                                                                isFetching = true
                                                                fetchError = null
                                                                try {
                                                                    val result = fetchSubscription(subUrlInput)
                                                                    if (result.servers.isNotEmpty()) {
                                                                        val domain = try {
                                                                            java.net.URI(subUrlInput).host ?: context.getString(R.string.custom_provider)
                                                                        } catch (e: Exception) {
                                                                            context.getString(R.string.custom_provider)
                                                                        }
                                                                        val name = if (subNameInput.trim().isNotEmpty()) subNameInput.trim() else domain
                                                                        val newSub = Subscription(
                                                                            id = java.util.UUID.randomUUID().toString(),
                                                                            name = name,
                                                                            url = subUrlInput.trim(),
                                                                            servers = result.servers.joinToString("\n"),
                                                                            upload = result.upload,
                                                                            download = result.download,
                                                                            total = result.total,
                                                                            expire = result.expire
                                                                        )
                                                                        val updatedList = subscriptions + newSub
                                                                        settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
                                                                        settingsManager.setActiveSubId(newSub.id)
                                                                        settingsManager.setActiveProfile(result.servers[0])
                                                                        
                                                                        subUrlInput = ""
                                                                        subNameInput = ""
                                                                        isAddFormExpanded = false
                                                                        
                                                                        if (vpnState == "CONNECTED") {
                                                                            startVpnService(context)
                                                                        }
                                                                    } else {
                                                                        fetchError = context.getString(R.string.no_valid_configs)
                                                                    }
                                                                } catch (e: Exception) {
                                                                    fetchError = context.getString(R.string.fetch_failed, e.message ?: "")
                                                                } finally {
                                                                    isFetching = false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1.5f).pressScaleEffect(),
                                                    shape = ExpressiveButtonShape,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                    enabled = !isFetching && subUrlInput.isNotEmpty()
                                                ) {
                                                    if (isFetching) {
                                                        LoadingIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                                    } else {
                                                        Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(stringResource(R.string.fetch_and_add), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                                
                                                OutlinedButton(
                                                    onClick = {
                                                        subUrlInput = ""
                                                        subNameInput = ""
                                                        fetchError = null
                                                    },
                                                    modifier = Modifier.weight(1f).pressScaleEffect(),
                                                    shape = ExpressiveButtonShape,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    enabled = !isFetching
                                                ) {
                                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(stringResource(R.string.clear), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            
                                            fetchError?.let { err ->
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                    }

                                    if (subscriptions.isEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = stringResource(R.string.no_subs_added),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = listModifier
                                        ) {
                                            subscriptions.forEach { sub ->
                                                val isActive = sub.id == activeSubId
                                                var menuExpanded by remember { mutableStateOf(false) }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(ExpressiveButtonShape)
                                                        .background(
                                                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                            else Color.Transparent
                                                        )
                                                        .clickable {
                                                            scope.launch {
                                                                settingsManager.setActiveSubId(sub.id)
                                                                val servers = sub.servers.split("\n").filter { it.isNotEmpty() }
                                                                if (servers.isNotEmpty()) {
                                                                    settingsManager.setActiveProfile(servers[0])
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isActive) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.surfaceVariant
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isActive) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Active",
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.RssFeed,
                                                                contentDescription = "Sub",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = sub.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        val domain = try {
                                                            java.net.URI(sub.url).host ?: sub.url
                                                        } catch (e: Exception) {
                                                            sub.url
                                                        }
                                                        Text(
                                                            text = domain,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        if (sub.total != null && sub.total > 0) {
                                                            Spacer(modifier = Modifier.height(6.dp))
                                                            val up = sub.upload ?: 0L
                                                            val down = sub.download ?: 0L
                                                            val used = up + down
                                                            val total = sub.total
                                                            val pct = (used.toDouble() / total.toDouble()).coerceIn(0.0, 1.0)
                                                            
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                androidx.compose.material3.LinearProgressIndicator(
                                                                    progress = pct.toFloat(),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(4.dp)
                                                                        .clip(CircleShape),
                                                                    color = if (pct > 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                                )
                                                                Text(
                                                                    text = "${(pct * 100).toInt()}%",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontSize = 9.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "${formatBytes(used)} / ${formatBytes(total)}" + 
                                                                    if (sub.expire != null && sub.expire > 0) " • Exp: ${formatExpiry(sub.expire)}" else "",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        } else if (sub.expire != null && sub.expire > 0) {
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "Expires: ${formatExpiry(sub.expire)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontSize = 9.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }

                                                    val isAutoConnectEnabled = autoConnectSubs.contains(sub.id)
                                                    val isLocalSub = sub.url.startsWith("local://")
                                                    val isRefreshing = refreshingSubs[sub.id] ?: false

                                                    if (useDropdownMenu) {
                                                        Box {
                                                            IconButton(
                                                                onClick = { menuExpanded = true },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.MoreVert,
                                                                    contentDescription = "Subscription options",
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            
                                                            DropdownMenu(
                                                                expanded = menuExpanded,
                                                                onDismissRequest = { menuExpanded = false },
                                                                modifier = Modifier.background(standardColorScheme.surfaceContainerHigh)
                                                            ) {
                                                                androidx.compose.material3.MaterialExpressiveTheme(colorScheme = standardColorScheme) {
                                                                DropdownMenuItem(
                                                                    text = {
                                                                        Text(
                                                                            text = if (isAutoConnectEnabled) "Auto Connect (Enabled)" else "Auto Connect",
                                                                            style = MaterialTheme.typography.bodyMedium
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        menuExpanded = false
                                                                        scope.launch {
                                                                            settingsManager.toggleAutoConnectSub(sub.id)
                                                                        }
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Bolt,
                                                                            contentDescription = null,
                                                                            tint = if (isAutoConnectEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                     text = { Text("Edit", style = MaterialTheme.typography.bodyMedium) },
                                                                     onClick = {
                                                                         menuExpanded = false
                                                                         editingSubscription = sub
                                                                         editSubNameInput = sub.name
                                                                         editSubUrlInput = sub.url
                                                                         showEditSubDialog = true
                                                                     },
                                                                     leadingIcon = {
                                                                         Icon(
                                                                             imageVector = Icons.Default.Edit,
                                                                             contentDescription = null,
                                                                             tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                             modifier = Modifier.size(18.dp)
                                                                         )
                                                                     }
                                                                 )
                                                                if (!isLocalSub) {
                                                                    DropdownMenuItem(
                                                                        text = { Text("Refresh", style = MaterialTheme.typography.bodyMedium) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            scope.launch {
                                                                                refreshingSubs[sub.id] = true
                                                                                try {
                                                                                    val result = fetchSubscription(sub.url)
                                                                                    if (result.servers.isNotEmpty()) {
                                                                                        val updatedList = subscriptions.map {
                                                                                            if (it.id == sub.id) {
                                                                                                it.copy(
                                                                                                    servers = result.servers.joinToString("\n"),
                                                                                                    upload = result.upload,
                                                                                                    download = result.download,
                                                                                                    total = result.total,
                                                                                                    expire = result.expire
                                                                                                )
                                                                                            } else {
                                                                                                it
                                                                                            }
                                                                                        }
                                                                                        settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                                        if (isActive) {
                                                                                            val currentActive = activeProfile
                                                                                            val sList = result.servers.map { it.trim() }.filter { it.isNotEmpty() }
                                                                                            if (sList.isNotEmpty() && !sList.contains(currentActive)) {
                                                                                                settingsManager.setActiveProfile(sList[0])
                                                                                                if (vpnState == "CONNECTED") {
                                                                                                    startVpnService(context)
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_updated, sub.name), android.widget.Toast.LENGTH_SHORT).show()
                                                                                    } else {
                                                                                        android.widget.Toast.makeText(context, context.getString(R.string.toast_no_servers), android.widget.Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                } catch(e: Exception) {
                                                                                    android.widget.Toast.makeText(context, context.getString(R.string.toast_update_failed, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                                                                                } finally {
                                                                                    refreshingSubs[sub.id] = false
                                                                                }
                                                                            }
                                                                        },
                                                                        leadingIcon = {
                                                                            if (isRefreshing) {
                                                                                LoadingIndicator(
                                                                                    modifier = Modifier.size(18.dp),
                                                                                    color = MaterialTheme.colorScheme.primary
                                                                                )
                                                                            } else {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.Refresh,
                                                                                    contentDescription = null,
                                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                                    modifier = Modifier.size(20.dp)
                                                                                )
                                                                            }
                                                                        },
                                                                        enabled = !isRefreshing
                                                                    )

                                                                    DropdownMenuItem(
                                                                        text = { Text("Share Link", style = MaterialTheme.typography.bodyMedium) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            val sendIntent = Intent().apply {
                                                                                action = Intent.ACTION_SEND
                                                                                putExtra(Intent.EXTRA_TEXT, sub.url)
                                                                                this.type = "text/plain"
                                                                            }
                                                                            val shareIntent = Intent.createChooser(sendIntent, null)
                                                                            context.startActivity(shareIntent)
                                                                        },
                                                                        leadingIcon = {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Share,
                                                                                contentDescription = null,
                                                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    )

                                                                    DropdownMenuItem(
                                                                        text = { Text("Share QR Code", style = MaterialTheme.typography.bodyMedium) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            qrCodeToShare = Pair(sub.name, sub.url)
                                                                        },
                                                                        leadingIcon = {
                                                                            Icon(
                                                                                imageVector = Icons.Default.QrCode,
                                                                                contentDescription = null,
                                                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                                modifier = Modifier.size(18.dp)
                                                                            )
                                                                        }
                                                                    )
                                                                }
                                                                DropdownMenuItem(
                                                                    text = { Text("Delete", style = MaterialTheme.typography.bodyMedium) },
                                                                    onClick = {
                                                                        menuExpanded = false
                                                                        scope.launch {
                                                                            val updatedList = subscriptions.filter { it != sub }
                                                                            settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                            if (isActive) {
                                                                                val nextActive = updatedList.firstOrNull()
                                                                                if (nextActive != null) {
                                                                                    settingsManager.setActiveSubId(nextActive.id)
                                                                                    val nextServers = nextActive.servers.split("\n").filter { it.isNotEmpty() }
                                                                                    if (nextServers.isNotEmpty()) {
                                                                                        settingsManager.setActiveProfile(nextServers[0])
                                                                                    }
                                                                                } else {
                                                                                    settingsManager.setActiveSubId("")
                                                                                }
                                                                                if (vpnState == "CONNECTED") {
                                                                                    startVpnService(context)
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Delete,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                )
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    settingsManager.toggleAutoConnectSub(sub.id)
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Bolt,
                                                                contentDescription = "Auto Connect Toggle",
                                                                tint = if (isAutoConnectEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        if (!isLocalSub) {
                                                            IconButton(
                                                                onClick = {
                                                                    scope.launch {
                                                                        refreshingSubs[sub.id] = true
                                                                        try {
                                                                            val result = fetchSubscription(sub.url)
                                                                            if (result.servers.isNotEmpty()) {
                                                                                val updatedList = subscriptions.map {
                                                                                    if (it.id == sub.id) {
                                                                                        it.copy(
                                                                                            servers = result.servers.joinToString("\n"),
                                                                                            upload = result.upload,
                                                                                            download = result.download,
                                                                                            total = result.total,
                                                                                            expire = result.expire
                                                                                        )
                                                                                    } else {
                                                                                        it
                                                                                    }
                                                                                }
                                                                                settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                                if (isActive) {
                                                                                    val currentActive = activeProfile
                                                                                    val sList = result.servers.map { it.trim() }.filter { it.isNotEmpty() }
                                                                                    if (sList.isNotEmpty() && !sList.contains(currentActive)) {
                                                                                        settingsManager.setActiveProfile(sList[0])
                                                                                        if (vpnState == "CONNECTED") {
                                                                                            startVpnService(context)
                                                                                        }
                                                                                    }
                                                                                }
                                                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_updated, sub.name), android.widget.Toast.LENGTH_SHORT).show()
                                                                            } else {
                                                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_no_servers), android.widget.Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        } catch(e: Exception) {
                                                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_update_failed, e.message ?: ""), android.widget.Toast.LENGTH_SHORT).show()
                                                                        } finally {
                                                                            refreshingSubs[sub.id] = false
                                                                        }
                                                                    }
                                                                },
                                                                modifier = Modifier.size(36.dp),
                                                                enabled = !isRefreshing
                                                            ) {
                                                                if (isRefreshing) {
                                                                    LoadingIndicator(
                                                                        modifier = Modifier.size(18.dp),
                                                                        color = MaterialTheme.colorScheme.primary
                                                                    )
                                                                } else {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Refresh,
                                                                        contentDescription = stringResource(R.string.refresh_label),
                                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    val sendIntent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, sub.url)
                                                                        this.type = "text/plain"
                                                                    }
                                                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                                                    context.startActivity(shareIntent)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Share,
                                                                    contentDescription = "Share",
                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    qrCodeToShare = Pair(sub.name, sub.url)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.QrCode,
                                                                    contentDescription = "QR Share",
                                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    val updatedList = subscriptions.filter { it != sub }
                                                                    settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                                                    if (isActive) {
                                                                        val nextActive = updatedList.firstOrNull()
                                                                        if (nextActive != null) {
                                                                            settingsManager.setActiveSubId(nextActive.id)
                                                                            val nextServers = nextActive.servers.split("\n").filter { it.isNotEmpty() }
                                                                            if (nextServers.isNotEmpty()) {
                                                                                settingsManager.setActiveProfile(nextServers[0])
                                                                            }
                                                                        } else {
                                                                            settingsManager.setActiveSubId("")
                                                                        }
                                                                        if (vpnState == "CONNECTED") {
                                                                            startVpnService(context)
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete",
                                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { showImportDialog = true },
                                            modifier = Modifier.weight(1f).pressScaleEffect(),
                                            shape = ExpressiveButtonShape
                                        ) {
                                            Icon(imageVector = Icons.Default.AddLink, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.import_str))
                                        }
                                        Button(
                                            onClick = {
                                                editingNodeLink = "new_node"
                                                editType = "vless"
                                                editRemark = ""
                                                editServer = ""
                                                editPort = "443"
                                                editCreds = ""
                                                editTls = false
                                                editSni = ""
                                                editLinkInput = ""
                                                editorMode = "form"
                                            },
                                            modifier = Modifier.weight(1f).pressScaleEffect(),
                                            shape = ExpressiveButtonShape
                                        ) {
                                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.create_str))
                                        }
                                    }
                                }
                                }
                            }
                        }

                        val availableNodesCard: @Composable (Modifier, Modifier) -> Unit = { modifier, listModifier ->
                            Box(modifier = modifier) {
                                if (Config.IS_SPECIAL) {
                                    PeakingKitty(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-32).dp, y = (-22).dp)
                                    )
                                }
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                        .border(
                                            width = 1.dp,
                                            brush = cardBorderBrush,
                                            shape = ExpressiveCardShape
                                        )
                                        .animateContentSize(),
                                    shape = ExpressiveCardShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    VibrantCardContent(settings.cardStyle) {
                                        Column(modifier = Modifier.padding(20.dp)) {
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
                                                    style = MaterialTheme.typography.titleSmall,
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
                                                    onClick = onUpdateSubscriptions,
                                                    enabled = !isUpdatingSubs && !isTestingPings,
                                                    modifier = Modifier.pressScaleEffect()
                                                ) {
                                                    if (isUpdatingSubs) {
                                                        LoadingIndicator(
                                                            modifier = Modifier.size(20.dp),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Sync,
                                                            contentDescription = "Update Subscriptions",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        if (!isTestingPings) {
                                                            scope.launch {
                                                                isTestingPings = true
                                                                val jobs = serverList.map { link ->
                                                                    scope.async(kotlinx.coroutines.Dispatchers.IO) {
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
                                                        LoadingIndicator(
                                                            modifier = Modifier.size(20.dp),
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

                                                IconButton(
                                                    onClick = { isNodesExpanded = true },
                                                    modifier = Modifier.pressScaleEffect()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Fullscreen,
                                                        contentDescription = "Expand Card",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
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
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Group filter chip
                                                    Box {
                                                        androidx.compose.material3.FilterChip(
                                                            selected = selectedSubGroupFilter != "All Groups",
                                                            onClick = { isGroupDropdownExpanded = true },
                                                            label = { 
                                                                Text(
                                                                    text = if (selectedSubGroupFilter == "All Groups") "All Groups" else selectedSubGroupFilter,
                                                                    maxLines = 1,
                                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                )
                                                            },
                                                            modifier = Modifier.widthIn(max = 130.dp)
                                                        )
                                                        DropdownMenu(
                                                            expanded = isGroupDropdownExpanded,
                                                            onDismissRequest = { isGroupDropdownExpanded = false }
                                                        ) {
                                                            androidx.compose.material3.MaterialExpressiveTheme(colorScheme = standardColorScheme) {
                                                                subGroups.forEach { group ->
                                                                    DropdownMenuItem(
                                                                        text = { Text(group) },
                                                                        onClick = {
                                                                            selectedSubGroupFilter = group
                                                                            isGroupDropdownExpanded = false
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    // Country filter chip
                                                    Box {
                                                        androidx.compose.material3.FilterChip(
                                                            selected = selectedCountryFilter != "All Countries",
                                                            onClick = { isCountryDropdownExpanded = true },
                                                            label = { 
                                                                Text(
                                                                    text = if (selectedCountryFilter == "All Countries") "All Countries" else selectedCountryFilter,
                                                                    maxLines = 1,
                                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                                )
                                                            },
                                                            modifier = Modifier.widthIn(max = 130.dp)
                                                        )
                                                        DropdownMenu(
                                                            expanded = isCountryDropdownExpanded,
                                                            onDismissRequest = { isCountryDropdownExpanded = false }
                                                        ) {
                                                            androidx.compose.material3.MaterialExpressiveTheme(colorScheme = standardColorScheme) {
                                                                uniqueCountries.forEach { country ->
                                                                    DropdownMenuItem(
                                                                        text = { Text(country) },
                                                                        onClick = {
                                                                            selectedCountryFilter = country
                                                                            isCountryDropdownExpanded = false
                                                                        }
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.weight(1f))

                                                    // Update/Refresh Subscriptions button
                                                    FilledIconButton(
                                                        onClick = onUpdateSubscriptions,
                                                        modifier = Modifier.size(36.dp).pressScaleEffect(),
                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        ),
                                                        shape = CircleShape,
                                                        enabled = !isUpdatingSubs && !isTestingPings
                                                    ) {
                                                        if (isUpdatingSubs) {
                                                            LoadingIndicator(
                                                                modifier = Modifier.size(16.dp),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.Sync,
                                                                contentDescription = "Update Subscriptions",
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // Ping/Speed test button
                                                    FilledIconButton(
                                                        onClick = {
                                                            if (!isTestingPings) {
                                                                scope.launch {
                                                                    isTestingPings = true
                                                                    val jobs = serverList.map { link ->
                                                                        scope.async(kotlinx.coroutines.Dispatchers.IO) {
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
                                                        modifier = Modifier.size(36.dp).pressScaleEffect(),
                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        ),
                                                        shape = CircleShape,
                                                        enabled = !isTestingPings
                                                    ) {
                                                        if (isTestingPings) {
                                                            LoadingIndicator(
                                                                modifier = Modifier.size(16.dp),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Default.Speed,
                                                                contentDescription = stringResource(R.string.test_pings),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

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
                                            tabTitles.forEachIndexed { index, title ->
                                                Tab(
                                                    selected = selectedTab == index,
                                                    onClick = { selectedTab = index },
                                                    text = { 
                                                        Text(
                                                            text = title, 
                                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                            fontSize = 13.sp
                                                        ) 
                                                    }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))

                                        if (filteredServerList.isEmpty()) {
                                            Box(
                                                modifier = listModifier,
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
                                                modifier = listModifier,
                                                contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                itemsIndexed(filteredServerList, key = { _, item -> item.id }) { index, serverItem ->
                                                    val serverLink = serverItem.link
                                                    val isSelected = activeProfile == serverLink
                                                    val name = serverItem.name
                                                    val type = serverItem.type
                                                    val transport = serverItem.transport
                                                    val isManualNode = remember(manualServersStr, serverLink) {
                                                        manualServersStr.split("\n").map { it.trim() }.contains(serverLink.trim())
                                                    }
                                                    
                                                    val tagContainerColor = when (type) {
                                                        "VLESS" -> MaterialTheme.colorScheme.primaryContainer
                                                        "TROJAN" -> MaterialTheme.colorScheme.secondaryContainer
                                                        "VMESS" -> MaterialTheme.colorScheme.tertiaryContainer
                                                        "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.errorContainer
                                                        "TUIC" -> MaterialTheme.colorScheme.primaryContainer
                                                        "CHAIN" -> MaterialTheme.colorScheme.tertiaryContainer
                                                        "OPENVPN", "OVPN" -> MaterialTheme.colorScheme.secondaryContainer
                                                        "AMNEZIAWG", "AWG", "WIREGUARD" -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                    val tagTextColor = when (type) {
                                                        "VLESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "TROJAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        "VMESS" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.onErrorContainer
                                                        "TUIC" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "CHAIN" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        "OPENVPN", "OVPN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        "AMNEZIAWG", "AWG", "WIREGUARD" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .animateItem()
                                                            .fillMaxWidth()
                                                            .clip(ExpressiveButtonShape)
                                                            .background(
                                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                                else Color.Transparent
                                                            )
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Transparent,
                                                                shape = ExpressiveButtonShape
                                                            )
                                                            .pressScaleEffect()
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
                                                                            val parentSub = subscriptions.find { it.servers.split("\n").map { s -> s.trim() }.contains(serverLink.trim()) }
                                                                            val newSubId = parentSub?.id ?: "manual"
                                                                            settingsManager.setActiveSubId(newSubId)
                                                                            if (vpnState == "CONNECTED") {
                                                                                startVpnService(context)
                                                                            }
                                                                        }
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    if (!isMultiSelectMode && isManualNode) {
                                                                        isMultiSelectMode = true
                                                                        selectedNodes = setOf(serverLink)
                                                                    }
                                                                }
                                                            )
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
                                                                
                                                                if (transport.isNotEmpty()) {
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
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        fontWeight = FontWeight.Bold,
                                                                        maxLines = 1,
                                                                        softWrap = false
                                                                    )
                                                                } else {
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
                                                        
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            IconButton(
                                                                onClick = {
                                                                    val sendIntent = Intent().apply {
                                                                        action = Intent.ACTION_SEND
                                                                        putExtra(Intent.EXTRA_TEXT, serverLink)
                                                                        this.type = "text/plain"
                                                                    }
                                                                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_config))
                                                                    context.startActivity(shareIntent)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Share,
                                                                    contentDescription = stringResource(R.string.share_config),
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }
                                                            
                                                            IconButton(
                                                                onClick = {
                                                                    qrCodeToShare = Pair(name, serverLink)
                                                                },
                                                                modifier = Modifier.size(36.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.QrCode,
                                                                    contentDescription = "QR Share",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(18.dp)
                                                                )
                                                            }

                                                            if (activeSubId == "manual") {
                                                                IconButton(
                                                                    onClick = {
                                                                        editingNodeLink = serverLink
                                                                        editLinkInput = serverLink
                                                                    },
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Edit,
                                                                        contentDescription = stringResource(R.string.edit_config),
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }

                                                                IconButton(
                                                                    onClick = {
                                                                        scope.launch {
                                                                            val currentManual = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                                                            val updatedManualList = currentManual.filter { it != serverLink }
                                                                            val updatedManualStr = updatedManualList.joinToString("\n")
                                                                            settingsManager.setManualServers(updatedManualStr)

                                                                            if (serverLink.startsWith("chain://")) {
                                                                                val chainId = serverLink.substringAfter("chain://").substringBefore("#")
                                                                                val currentChains = deserializeProxyChains(settings.proxyChains)
                                                                                val updatedChains = currentChains.filter { it.id != chainId }
                                                                                settingsManager.setProxyChains(serializeProxyChains(updatedChains))
                                                                            }
                                                                            
                                                                            val currentCam = deserializeCamouflageSettings(settings.camouflageSettings)
                                                                            val updatedCam = currentCam.filter { it.nodeLink.substringBefore("#") != serverLink.substringBefore("#") }
                                                                            settingsManager.setCamouflageSettings(serializeCamouflageSettings(updatedCam))

                                                                            if (isSelected) {
                                                                                val nextActive = updatedManualList.firstOrNull() ?: ""
                                                                                settingsManager.setActiveProfile(nextActive)
                                                                                if (vpnState == "CONNECTED" && nextActive.isNotEmpty()) {
                                                                                    startVpnService(context)
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(36.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = stringResource(R.string.delete_config),
                                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                                        modifier = Modifier.size(18.dp)
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

                        Box(modifier = Modifier.fillMaxSize()) {
                            if (useDropdownMenu) {
                                val fontScale = LocalDensity.current.fontScale
                                val bentoTitleSize = if (fontScale > 1.0f) (15f / fontScale).sp else 15.sp
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Top Bento Row
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.spacedBy(12.dp)
                                 ) {
                                     // QR Scanner Bento Card
                                     Card(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(68.dp)
                                             .clip(RoundedCornerShape(28.dp))
                                             .clickable {
                                                 scanResultCallback = { result ->
                                                     handleScannedQrResult(
                                                         result = result,
                                                         scope = scope,
                                                         context = context,
                                                         settingsManager = settingsManager,
                                                         subscriptions = subscriptions,
                                                         manualServersStr = manualServersStr,
                                                         vpnState = vpnState
                                                     )
                                                 }
                                             }
                                             .background(brush = primaryCardBrush, shape = RoundedCornerShape(28.dp))
                                             .border(1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(28.dp)),
                                         colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                         shape = RoundedCornerShape(28.dp)
                                     ) {
                                         VibrantCardContent(settings.cardStyle) {
                                             Column(
                                                 modifier = Modifier
                                                     .fillMaxSize()
                                                     .padding(12.dp),
                                                 verticalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     horizontalArrangement = Arrangement.SpaceBetween,
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.QrCodeScanner,
                                                         contentDescription = "Scan QR Code",
                                                         tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                         modifier = Modifier.size(20.dp)
                                                     )
                                                 }
                                                 Column {
                                                     Text(
                                                         text = "Scan QR Code",
                                                         fontSize = bentoTitleSize,
                                                         style = MaterialTheme.typography.titleMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.onPrimaryContainer
                                                     )
                                                 }
                                             }
                                         }
                                     }

                                     // Subscriptions Manager Card
                                     Card(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(68.dp)
                                             .clip(RoundedCornerShape(28.dp))
                                             .clickable { showSubManagerDialog = true }
                                             .background(brush = secondaryCardBrush, shape = RoundedCornerShape(28.dp))
                                             .border(1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(28.dp)),
                                         colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                         shape = RoundedCornerShape(28.dp)
                                     ) {
                                         VibrantCardContent(settings.cardStyle, isSecondary = true) {
                                             Column(
                                                 modifier = Modifier
                                                     .fillMaxSize()
                                                     .padding(12.dp),
                                                 verticalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Folder,
                                                     contentDescription = "Subscriptions",
                                                     tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                     modifier = Modifier.size(20.dp)
                                                 )
                                                 Column {
                                                     Text(
                                                         text = "Subscriptions",
                                                         fontSize = bentoTitleSize,
                                                         style = MaterialTheme.typography.titleMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.onSecondaryContainer
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }

                                 // Import & Create Node Bento Row
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.spacedBy(12.dp)
                                 ) {
                                     // Import Card
                                     Card(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(68.dp)
                                             .clip(RoundedCornerShape(28.dp))
                                             .clickable { showImportDialog = true }
                                             .background(brush = primaryCardBrush, shape = RoundedCornerShape(28.dp))
                                             .border(1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(28.dp)),
                                         colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                         shape = RoundedCornerShape(28.dp)
                                     ) {
                                         VibrantCardContent(settings.cardStyle) {
                                             Column(
                                                 modifier = Modifier.fillMaxSize().padding(12.dp),
                                                 verticalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.AddLink,
                                                     contentDescription = "Import",
                                                     tint = MaterialTheme.colorScheme.primary,
                                                     modifier = Modifier.size(20.dp)
                                                 )
                                                 Column {
                                                     Text(
                                                         text = "Import Link",
                                                         fontSize = bentoTitleSize,
                                                         style = MaterialTheme.typography.titleMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.onPrimaryContainer
                                                     )
                                                 }
                                             }
                                         }
                                     }

                                     // Search Card
                                     Card(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(68.dp)
                                             .clip(RoundedCornerShape(28.dp))
                                             .clickable {
                                                 isSearchVisible = true
                                             }
                                             .background(brush = secondaryCardBrush, shape = RoundedCornerShape(28.dp))
                                             .border(1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(28.dp)),
                                         colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                         shape = RoundedCornerShape(28.dp)
                                     ) {
                                         VibrantCardContent(settings.cardStyle, isSecondary = true) {
                                             Column(
                                                 modifier = Modifier.fillMaxSize().padding(12.dp),
                                                 verticalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Search,
                                                     contentDescription = "Search",
                                                     tint = MaterialTheme.colorScheme.secondary,
                                                     modifier = Modifier.size(20.dp)
                                                 )
                                                 Column {
                                                     Text(
                                                         text = "Search Nodes",
                                                         fontSize = bentoTitleSize,
                                                         style = MaterialTheme.typography.titleMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.onSecondaryContainer
                                                     )
                                                 }
                                             }
                                         }
                                     }
                                 }

                                 // Side-by-side Filtering Cards
                                 Row(
                                     modifier = Modifier.fillMaxWidth(),
                                     horizontalArrangement = Arrangement.spacedBy(10.dp)
                                 ) {
                                     // Country Filtering Card
                                     var isCountryDropdownExpanded by remember { mutableStateOf(false) }
                                     Card(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(50.dp)
                                             .clip(RoundedCornerShape(16.dp))
                                             .clickable { isCountryDropdownExpanded = true }
                                             .background(brush = cardBorderBrush, shape = RoundedCornerShape(16.dp))
                                             .border(1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(16.dp)),
                                         colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                         shape = RoundedCornerShape(16.dp)
                                     ) {
                                         VibrantCardContent(settings.cardStyle) {
                                             Row(
                                                 modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Column {
                                                     Text(
                                                         text = "COUNTRY",
                                                         style = MaterialTheme.typography.labelSmall,
                                                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                         fontWeight = FontWeight.Bold,
                                                         letterSpacing = 0.5.sp
                                                     )
                                                     FlagTextRow(
                                                         text = selectedCountryFilter,
                                                         style = MaterialTheme.typography.bodyMedium,
                                                         color = MaterialTheme.colorScheme.onSurface,
                                                         maxLines = 1,
                                                         overflow = TextOverflow.Ellipsis
                                                     )
                                                 }
                                                 Icon(
                                                     imageVector = Icons.Default.FilterAlt,
                                                     contentDescription = null,
                                                     tint = MaterialTheme.colorScheme.primary,
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                             }
                                         }
                                         DropdownMenu(
                                             expanded = isCountryDropdownExpanded,
                                             onDismissRequest = { isCountryDropdownExpanded = false }
                                         ) {
                                             uniqueCountries.forEach { country ->
                                                 DropdownMenuItem(
                                                     text = { FlagTextRow(country) },
                                                     onClick = {
                                                         selectedCountryFilter = country
                                                         isCountryDropdownExpanded = false
                                                     }
                                                 )
                                             }
                                         }
                                     }

                                     // Subscription Selector Card
                                     var isSubDropdownExpanded by remember { mutableStateOf(false) }
                                     Card(
                                         modifier = Modifier
                                             .weight(1f)
                                             .height(50.dp)
                                             .clip(RoundedCornerShape(16.dp))
                                             .clickable { isSubDropdownExpanded = true }
                                             .background(brush = cardBorderBrush, shape = RoundedCornerShape(16.dp))
                                             .border(1.dp, brush = cardBorderBrush, shape = RoundedCornerShape(16.dp)),
                                         colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                         shape = RoundedCornerShape(16.dp)
                                     ) {
                                         VibrantCardContent(settings.cardStyle) {
                                             Row(
                                                 modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Column {
                                                     Text(
                                                         text = "SUBSCRIPTION",
                                                         style = MaterialTheme.typography.labelSmall,
                                                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                         fontWeight = FontWeight.Bold,
                                                         letterSpacing = 0.5.sp
                                                     )
                                                     FlagTextRow(
                                                         text = activeSubscription?.name ?: "Manual Config",
                                                         style = MaterialTheme.typography.bodyMedium,
                                                         color = MaterialTheme.colorScheme.onSurface,
                                                         maxLines = 1,
                                                         overflow = TextOverflow.Ellipsis
                                                     )
                                                 }
                                                 Icon(
                                                     imageVector = Icons.Default.Layers,
                                                     contentDescription = null,
                                                     tint = MaterialTheme.colorScheme.secondary,
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                             }
                                         }
                                         DropdownMenu(
                                             expanded = isSubDropdownExpanded,
                                             onDismissRequest = { isSubDropdownExpanded = false }
                                         ) {
                                             DropdownMenuItem(
                                                 text = { Text("Manual Config") },
                                                 onClick = {
                                                     scope.launch {
                                                         settingsManager.setActiveSubId("manual")
                                                     }
                                                     isSubDropdownExpanded = false
                                                 }
                                             )
                                             subscriptions.forEach { sub ->
                                                 DropdownMenuItem(
                                                     text = { Text(sub.name) },
                                                     onClick = {
                                                         scope.launch {
                                                             settingsManager.setActiveSubId(sub.id)
                                                         }
                                                         isSubDropdownExpanded = false
                                                     }
                                                 )
                                             }
                                         }
                                     }
                                 }

                                // Row with Speed Test & Fullscreen Buttons
// Row with Speed Test, Chain & Fullscreen Buttons
                                 AnimatedContent(
                                     targetState = isSearchVisible,
                                     transitionSpec = {
                                         fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) togetherWith
                                         fadeOut(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
                                     },
                                     label = "SearchOrActionsTransition"
                                 ) { searchActive ->
                                     if (searchActive) {
                                         Card(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .height(42.dp),
                                             shape = RoundedCornerShape(21.dp),
                                             colors = CardDefaults.cardColors(
                                                 containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                             ),
                                             border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                         ) {
                                             Row(
                                                 modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 IconButton(
                                                     onClick = {
                                                         isSearchVisible = false
                                                         searchQuery = ""
                                                     },
                                                     modifier = Modifier.size(32.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.ArrowBack,
                                                         contentDescription = "Close Search",
                                                         tint = MaterialTheme.colorScheme.primary,
                                                         modifier = Modifier.size(18.dp)
                                                     )
                                                 }
                                                 
                                                 androidx.compose.foundation.text.BasicTextField(
                                                     value = searchQuery,
                                                     onValueChange = { searchQuery = it },
                                                     modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                                     singleLine = true,
                                                     textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                         color = MaterialTheme.colorScheme.onSurface
                                                     ),
                                                     decorationBox = { innerTextField ->
                                                         Box(modifier = Modifier.fillMaxWidth()) {
                                                             if (searchQuery.isEmpty()) {
                                                                 Text(
                                                                     text = "Search servers...",
                                                                     style = MaterialTheme.typography.bodyMedium,
                                                                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                                 )
                                                             }
                                                             innerTextField()
                                                         }
                                                     }
                                                 )
                                                 
                                                 if (searchQuery.isNotEmpty()) {
                                                     IconButton(
                                                         onClick = { searchQuery = "" },
                                                         modifier = Modifier.size(32.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.Clear,
                                                             contentDescription = "Clear",
                                                             tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                             modifier = Modifier.size(18.dp)
                                                         )
                                                     }
                                                 }
                                             }
                                         }
                                     } else {
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.End,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             // Update/Refresh Subscriptions button
                                             FilledIconButton(
                                                 onClick = onUpdateSubscriptions,
                                                 modifier = Modifier.size(36.dp).pressScaleEffect(),
                                                 colors = IconButtonDefaults.filledIconButtonColors(
                                                     containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                     contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                 ),
                                                 shape = CircleShape,
                                                 enabled = !isUpdatingSubs && !isTestingPings
                                             ) {
                                                 if (isUpdatingSubs) {
                                                     LoadingIndicator(
                                                         modifier = Modifier.size(16.dp),
                                                         color = MaterialTheme.colorScheme.onPrimaryContainer
                                                     )
                                                 } else {
                                                     Icon(
                                                         imageVector = Icons.Default.Sync,
                                                         contentDescription = "Update Subscriptions",
                                                         modifier = Modifier.size(16.dp)
                                                     )
                                                 }
                                             }

                                             Spacer(modifier = Modifier.width(8.dp))

                                             // Ping/Speed test button
                                             FilledIconButton(
                                                 onClick = {
                                                     if (!isTestingPings) {
                                                         scope.launch {
                                                             isTestingPings = true
                                                             val jobs = serverList.map { link ->
                                                                 scope.async(kotlinx.coroutines.Dispatchers.IO) {
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
                                                 modifier = Modifier.size(36.dp).pressScaleEffect(),
                                                 colors = IconButtonDefaults.filledIconButtonColors(
                                                     containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                     contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                 ),
                                                 shape = CircleShape,
                                                 enabled = !isTestingPings
                                             ) {
                                                 if (isTestingPings) {
                                                     LoadingIndicator(
                                                         modifier = Modifier.size(16.dp),
                                                         color = MaterialTheme.colorScheme.onPrimaryContainer
                                                     )
                                                 } else {
                                                     Icon(
                                                         imageVector = Icons.Default.Speed,
                                                         contentDescription = stringResource(R.string.test_pings),
                                                         modifier = Modifier.size(16.dp)
                                                     )
                                                 }
                                             }
                                             
                                             Spacer(modifier = Modifier.width(8.dp))
                                             
                                             // Chain/Proxy Chain Button next to Ping button
                                             FilledIconButton(
                                                 onClick = { editingNodeLink = "new_chain" },
                                                 modifier = Modifier.size(36.dp).pressScaleEffect(),
                                                 colors = IconButtonDefaults.filledIconButtonColors(
                                                     containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                     contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                 ),
                                                 shape = CircleShape
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Link,
                                                     contentDescription = "Create Proxy Chain",
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                             }
                                             
                                             Spacer(modifier = Modifier.width(8.dp))
                                             
                                             FilledIconButton(
                                                 onClick = { isNodesExpanded = true },
                                                 modifier = Modifier.size(36.dp).pressScaleEffect(),
                                                 colors = IconButtonDefaults.filledIconButtonColors(
                                                     containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                     contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                 ),
                                                 shape = CircleShape
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Fullscreen,
                                                     contentDescription = "Expand Card",
                                                     modifier = Modifier.size(18.dp)
                                                 )
                                             }
                                         }
                                     }
                                 }

                                // Servers List
                                if (filteredServerList.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
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
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(filteredServerList, key = { _, item -> item.id }) { index, serverItem ->
                                            val serverLink = serverItem.link
                                            val isSelected = activeProfile == serverLink
                                            val name = serverItem.name
                                            val type = serverItem.type
                                            val countryCode = resolvedCountries[serverLink]
                                            val flagEmoji = remember(name, countryCode) { getFlagEmoji(name, countryCode) }
                                            val ping = pingsMap[serverLink]
                                            val isTimeout = ping != null && ping < 0
                                            
                                            var menuExpanded by remember { mutableStateOf(false) }
                                            val isManualNode = remember(manualServersStr, serverLink) {
                                                manualServersStr.split("\n").map { it.trim() }.contains(serverLink.trim())
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .pressScaleEffect()
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
                                                                    val parentSub = subscriptions.find { it.servers.split("\n").map { s -> s.trim() }.contains(serverLink.trim()) }
                                                                    val newSubId = parentSub?.id ?: "manual"
                                                                    settingsManager.setActiveSubId(newSubId)
                                                                    if (vpnState == "CONNECTED") {
                                                                        startVpnService(context)
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onLongClick = {
                                                            if (!isMultiSelectMode && isManualNode) {
                                                                isMultiSelectMode = true
                                                                selectedNodes = setOf(serverLink)
                                                            } else {
                                                                menuExpanded = true
                                                            }
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                                     else MaterialTheme.colorScheme.surface
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
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
                                                                                                                                 .size(40.dp)
                                                                                                                                 .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape),
                                                                                                                             contentAlignment = Alignment.Center
                                                                                                                         ) {
                                                                                                                             if (flagEmoji == "🇮🇷") {
                                                                                                                                 Image(
                                                                                                                                     painter = androidx.compose.ui.res.painterResource(id = com.hambalapps.chameleon.R.drawable.ic_lion_sun_flag),
                                                                                                                                     contentDescription = "Iran Flag",
                                                                                                                                     modifier = Modifier.size(24.dp)
                                                                                                                                 )
                                                                                                                             } else {
                                                                                                                                 Text(
                                                                                                                                     text = flagEmoji,
                                                                                                                                     style = MaterialTheme.typography.titleMedium
                                                                                                                                 )
                                                                                                                             }
                                                                                                                         }
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(
                                                                text = name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = type,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        if (ping != null) {
                                                            val pingColor = if (isTimeout) Color.Red else if (ping < 150) Color.Green else Color.Yellow
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(pingColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (isTimeout) "Timeout" else "${ping}ms",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = pingColor,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        
                                                        val isFavorite = remember(settings.favoriteServers, serverLink) {
                                                            settings.favoriteServers.contains(serverLink)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                scope.launch {
                                                                    settingsManager.toggleFavorite(serverLink)
                                                                }
                                                            },
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                                contentDescription = "Favorite",
                                                                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }

                                                        var menuExpanded by remember { mutableStateOf(false) }
                                                        Box {
                                                            IconButton(onClick = { menuExpanded = true }) {
                                                                Icon(
                                                                    imageVector = Icons.Default.MoreVert,
                                                                    contentDescription = "Options",
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            DropdownMenu(
                                                                expanded = menuExpanded,
                                                                onDismissRequest = { menuExpanded = false }
                                                            ) {
                                                                DropdownMenuItem(
                                                                    text = { Text(stringResource(R.string.share_config)) },
                                                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                                                    onClick = {
                                                                        menuExpanded = false
                                                                        val sendIntent = Intent().apply {
                                                                            action = Intent.ACTION_SEND
                                                                            putExtra(Intent.EXTRA_TEXT, serverLink)
                                                                            this.type = "text/plain"
                                                                        }
                                                                        val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_config))
                                                                        context.startActivity(shareIntent)
                                                                    }
                                                                )
                                                                DropdownMenuItem(
                                                                    text = { Text("Share QR Code") },
                                                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                                                                    onClick = {
                                                                        menuExpanded = false
                                                                        qrCodeToShare = Pair(name, serverLink)
                                                                    }
                                                                )
                                                                if (isManualNode) {
                                                                    DropdownMenuItem(
                                                                        text = { Text(stringResource(R.string.edit_config)) },
                                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            editingNodeLink = serverLink
                                                                            editLinkInput = serverLink
                                                                        }
                                                                    )
                                                                    DropdownMenuItem(
                                                                        text = { Text(stringResource(R.string.delete_config)) },
                                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                                                        onClick = {
                                                                            menuExpanded = false
                                                                            scope.launch {
                                                                                val currentManual = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                                                                val updatedManualList = currentManual.filter { it != serverLink }
                                                                                settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                                                                                
                                                                                if (serverLink.startsWith("chain://")) {
                                                                                    val chainId = serverLink.substringAfter("chain://").substringBefore("#")
                                                                                    val currentChains = deserializeProxyChains(settings.proxyChains)
                                                                                    val updatedChains = currentChains.filter { it.id != chainId }
                                                                                    settingsManager.setProxyChains(serializeProxyChains(updatedChains))
                                                                                }
                                                                                
                                                                                val currentCam = deserializeCamouflageSettings(settings.camouflageSettings)
                                                                                val updatedCam = currentCam.filter { it.nodeLink.substringBefore("#") != serverLink.substringBefore("#") }
                                                                                settingsManager.setCamouflageSettings(serializeCamouflageSettings(updatedCam))
                                                                                
                                                                                if (isSelected) {
                                                                                    val nextActive = updatedManualList.firstOrNull() ?: ""
                                                                                    settingsManager.setActiveProfile(nextActive)
                                                                                    if (vpnState == "CONNECTED" && nextActive.isNotEmpty()) {
                                                                                        startVpnService(context)
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
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



                               if (showSubManagerDialog) {
                                    androidx.compose.ui.window.Dialog(onDismissRequest = { showSubManagerDialog = false }) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(0.85f),
                                            shape = RoundedCornerShape(28.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                        ) {
                                            subscriptionManagerCard(
                                                Modifier.fillMaxSize(),
                                                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                                            )
                                        }
                                    }
                                }
                               } else {
                                   Row(
                                       modifier = Modifier.fillMaxSize().padding(16.dp),
                                       horizontalArrangement = Arrangement.spacedBy(16.dp)
                                   ) {
                                       subscriptionManagerCard(Modifier.weight(1.2f).fillMaxHeight(), Modifier.weight(1f).fillMaxHeight())
                                       availableNodesCard(Modifier.weight(1.8f).fillMaxHeight(), Modifier.weight(1f).fillMaxHeight())
                                   }
                               }
                            }
                    }
                    2 -> { // Logs Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val isLogsTabActive = tabs[pagerState.currentPage].first == 2
                            LogsConsole(
                                isActive = isLogsTabActive,
                                context = context,
                                cardStyle = cardStyle,
                                isDark = isDark,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                    3 -> { // Settings Tab
                        var showAdvancedSettings by remember { mutableStateOf(false) }
                        var localMtu by remember(settings.vpnMtu) { mutableStateOf(settings.vpnMtu.toFloat()) }
                        val animatedMtu by animateFloatAsState(
                            targetValue = localMtu,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                            label = "mtuSpring"
                        )
                        val initialFragmentInterval = settings.fragmentInterval.toFloatOrNull() ?: 10f
                        var localFragmentInterval by remember(settings.fragmentInterval) { mutableStateOf(initialFragmentInterval) }
                        val animatedFragmentDelay by animateFloatAsState(
                            targetValue = localFragmentInterval,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                            label = "delaySpring"
                        )
                        var localUpdateHours by remember(settings.autoUpdateIntervalHours) { mutableStateOf(settings.autoUpdateIntervalHours.toFloat()) }
                        val animatedUpdateHours by animateFloatAsState(
                            targetValue = localUpdateHours,
                            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
                            label = "hoursSpring"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Category 1 Header: Connection & Profiles
                            Text(
                                text = "Connection & Profiles",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, bottom = 8.dp)
                            )
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                VibrantCardContent(settings.cardStyle) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        // Bypass Iran
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(stringResource(R.string.bypass_iran), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.bypass_iran_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Switch(checked = bypassIran, onCheckedChange = { scope.launch { settingsManager.setBypassIran(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Bypass LAN
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(stringResource(R.string.bypass_lan), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.bypass_lan_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Switch(checked = bypassLan, onCheckedChange = { scope.launch { settingsManager.setBypassLan(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Share VPN Connection (LAN)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("Share VPN connection (LAN)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Allows other local network devices to connect via proxy", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Switch(checked = settings.shareVpnLan, onCheckedChange = { scope.launch { settingsManager.setShareVpnLan(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                        }

                                        AnimatedVisibility(visible = settings.shareVpnLan) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                        Spacer(modifier = Modifier.width(28.dp))
                                                        Column {
                                                            Text("Proxy Port", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text("Port for HTTP/Socks5 (1024-65535)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    var portText by remember(settings.shareVpnPort) { mutableStateOf(settings.shareVpnPort) }
                                                    OutlinedTextField(
                                                        value = portText,
                                                        onValueChange = {
                                                            portText = it
                                                            if (it.toIntOrNull() in 1024..65535) {
                                                                scope.launch { settingsManager.setShareVpnPort(it) }
                                                            }
                                                        },
                                                        singleLine = true,
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        modifier = Modifier.width(90.dp),
                                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                                    )
                                                }
                                            }
                                        }

                                        AnimatedVisibility(visible = showAdvancedSettings) {
                                            Column {
                                                Spacer(modifier = Modifier.height(16.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Root Mode
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("Root Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text("Use root privileges for transparent proxy routing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Switch(checked = rootMode, onCheckedChange = { checked ->
                                                        if (checked) {
                                                            val hasRoot = try {
                                                                val process = Runtime.getRuntime().exec("su")
                                                                val os = process.outputStream.bufferedWriter()
                                                                os.write("exit\n")
                                                                os.flush()
                                                                process.waitFor() == 0
                                                            } catch (e: Exception) {
                                                                false
                                                            }
                                                            if (hasRoot) {
                                                                scope.launch {
                                                                    settingsManager.setRootMode(true)
                                                                    if (vpnState == "CONNECTED") startVpnService(context)
                                                                }
                                                            } else {
                                                                android.widget.Toast.makeText(context, "Root access not available or denied", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        } else {
                                                            scope.launch {
                                                                settingsManager.setRootMode(false)
                                                                if (vpnState == "CONNECTED") startVpnService(context)
                                                            }
                                                        }
                                                    })
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                Spacer(modifier = Modifier.height(16.dp))

                                                // Secure DNS (DOH)
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text(stringResource(R.string.secure_dns_doh), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                            Text("DNS-over-HTTPS queries to prevent hijacking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    OutlinedTextField(
                                                        value = secureDns,
                                                        onValueChange = { scope.launch { settingsManager.setSecureDns(it) } },
                                                        label = { Text("DNS-over-HTTPS URL") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = ExpressiveButtonShape
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        val presets = listOf(
                                                            Pair("Cloudflare", "https://1.1.1.1/dns-query"),
                                                            Pair("Google", "https://dns.google/dns-query"),
                                                            Pair("AdGuard (AdBlock)", "https://dns.adguard-dns.com/dns-query"),
                                                            Pair("NextDNS", "https://dns.nextdns.io")
                                                        )
                                                        presets.forEach { (label, urlVal) ->
                                                            FilterChip(
                                                                selected = secureDns == urlVal,
                                                                onClick = { scope.launch { settingsManager.setSecureDns(urlVal) } },
                                                                label = { Text(label) },
                                                                shape = ExpressiveButtonShape,
                                                                modifier = Modifier.pressScaleEffect()
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // WARP Management Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                VibrantCardContent(settings.cardStyle) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("Cloudflare WARP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Manage registration, exit IP, detour and port", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Account Status Section
                                        val hasWarp = settings.warpPrivateKey.isNotEmpty()
                                        if (hasWarp) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Status: Registered", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Account registration active", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row {
                                                    Button(
                                                        onClick = {
                                                            scope.launch {
                                                                isRegisteringWarp = true
                                                                val creds = registerWarpAccount()
                                                                isRegisteringWarp = false
                                                                if (creds != null) {
                                                                    settingsManager.setWarpCredentials(creds.privateKey, creds.publicKey, creds.ipAddress, creds.clientId)
                                                                    if (vpnState == "CONNECTED") startVpnService(context)
                                                                }
                                                            }
                                                        },
                                                        shape = ExpressiveButtonShape,
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    ) {
                                                        if (isRegisteringWarp) {
                                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondary)
                                                        } else {
                                                            Text("Change Exit IP", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                                        }
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            scope.launch {
                                                                settingsManager.clearWarpCredentials()
                                                                if (vpnState == "CONNECTED") startVpnService(context)
                                                            }
                                                        },
                                                        shape = ExpressiveButtonShape
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Status: Not Registered", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Requires registration to use WARP features", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            isRegisteringWarp = true
                                                            val creds = registerWarpAccount()
                                                            isRegisteringWarp = false
                                                            if (creds != null) {
                                                                settingsManager.setWarpCredentials(creds.privateKey, creds.publicKey, creds.ipAddress, creds.clientId)
                                                                if (vpnState == "CONNECTED") startVpnService(context)
                                                            }
                                                        }
                                                    },
                                                    shape = ExpressiveButtonShape
                                                ) {
                                                    if (isRegisteringWarp) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                                    } else {
                                                        Text("Register Account", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Detour Mode Selector
                                        Text(
                                            text = "Detour Connection Via",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("proxy" to "Main Proxy", "direct" to "Direct Connection").forEach { (modeVal, label) ->
                                                FilterChip(
                                                    selected = settings.warpDetourMode == modeVal,
                                                    onClick = {
                                                        scope.launch {
                                                            settingsManager.setWarpDetourMode(modeVal)
                                                            if (vpnState == "CONNECTED") startVpnService(context)
                                                        }
                                                    },
                                                    label = { Text(label) },
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Port and IP Customization
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "WARP Port",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                var portText by remember(settings.warpPort) { mutableStateOf(settings.warpPort) }
                                                OutlinedTextField(
                                                    value = portText,
                                                    onValueChange = {
                                                        portText = it
                                                        scope.launch { settingsManager.setWarpPort(it) }
                                                    },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = ExpressiveButtonShape,
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Client Local IP",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                var ipText by remember(settings.warpIpAddress) { mutableStateOf(settings.warpIpAddress) }
                                                OutlinedTextField(
                                                    value = ipText,
                                                    onValueChange = {
                                                        ipText = it
                                                        scope.launch { settingsManager.setWarpIpAddress(it) }
                                                    },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Peer IP and Client ID Customization
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "WARP Endpoint IP (Peer)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                var peerIpText by remember(settings.warpPeerIp) { mutableStateOf(settings.warpPeerIp) }
                                                OutlinedTextField(
                                                    value = peerIpText,
                                                    onValueChange = {
                                                        peerIpText = it
                                                        scope.launch { settingsManager.setWarpPeerIp(it) }
                                                    },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = ExpressiveButtonShape
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Client ID (Reserved Bytes)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                var clientIdText by remember(settings.warpClientId) { mutableStateOf(settings.warpClientId) }
                                                OutlinedTextField(
                                                    value = clientIdText,
                                                    onValueChange = {
                                                        clientIdText = it
                                                        scope.launch { settingsManager.setWarpClientId(it) }
                                                    },
                                                    singleLine = true,
                                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = ExpressiveButtonShape
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                VibrantCardContent(settings.cardStyle) {
                                    var isScanningCamo by remember { mutableStateOf(false) }
                                    var isCamoMenuExpanded by remember { mutableStateOf(false) }
                                    var lastScanTrigger by remember { mutableStateOf(0) }
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(stringResource(R.string.camouflage_settings), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("Dynamic domain-fronting with clean CDN IP scanning", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Switch(
                                                checked = settings.globalCamouflageEnabled,
                                                onCheckedChange = { scope.launch { settingsManager.setGlobalCamouflageEnabled(it); if (vpnState == "CONNECTED") startVpnService(context) } }
                                            )
                                        }

                                        AnimatedVisibility(visible = settings.globalCamouflageEnabled) {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                Spacer(modifier = Modifier.height(12.dp))

                                                Text(
                                                    text = stringResource(R.string.camouflage_preset),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    listOf("cloudflare" to "Cloudflare", "cloudfront" to "Cloudfront", "custom" to "Custom").forEach { (presetVal, label) ->
                                                        FilterChip(
                                                            selected = settings.globalCamouflagePreset == presetVal,
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
                                                                    if (vpnState == "CONNECTED") startVpnService(context)
                                                                }
                                                            },
                                                            label = { Text(label) },
                                                            shape = ExpressiveButtonShape
                                                        )
                                                    }
                                                }

                                                if (settings.globalCamouflagePreset == "custom") {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    OutlinedTextField(
                                                        value = settings.globalCamouflageSni,
                                                        onValueChange = { scope.launch { settingsManager.setGlobalCamouflageSni(it) } },
                                                        label = { Text(stringResource(R.string.custom_sni)) },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = ExpressiveButtonShape
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    OutlinedTextField(
                                                        value = settings.globalCamouflageHost,
                                                        onValueChange = { scope.launch { settingsManager.setGlobalCamouflageHost(it) } },
                                                        label = { Text(stringResource(R.string.custom_host)) },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = ExpressiveButtonShape
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    OutlinedTextField(
                                                        value = settings.globalCamouflageCustomIps,
                                                        onValueChange = { scope.launch { settingsManager.setGlobalCamouflageCustomIps(it) } },
                                                        label = { Text("Custom IP List (comma/line separated)") },
                                                        placeholder = { Text("e.g. 104.16.85.20, 104.16.86.20") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = ExpressiveButtonShape
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))
                                                OutlinedTextField(
                                                    value = settings.globalCamouflageTimeout,
                                                    onValueChange = { scope.launch { settingsManager.setGlobalCamouflageTimeout(it) } },
                                                    label = { Text("Scanner Timeout (ms)") },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = ExpressiveButtonShape,
                                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                                )

                                                if (settings.globalCamouflageEnabled) {
                                                    val scannedIps = remember(settings.globalCamouflagePreset, lastScanTrigger) {
                                                        com.hambalapps.chameleon.vpn.CdnIpScanner.lastScanResults[settings.globalCamouflagePreset] ?: emptyList()
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .animateContentSize()
                                                            .clip(RoundedCornerShape(24.dp))
                                                            .clickable { isCamoMenuExpanded = !isCamoMenuExpanded }
                                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                                        ),
                                                        shape = RoundedCornerShape(24.dp)
                                                    ) {
                                                        Column(modifier = Modifier.padding(16.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = "Selected Clean IP",
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                    Text(
                                                                        text = if (settings.globalCamouflagePinnedIp.isEmpty()) "Auto (Fastest Scanned IP)" else settings.globalCamouflagePinnedIp,
                                                                        style = MaterialTheme.typography.bodyLarge,
                                                                        color = MaterialTheme.colorScheme.onSurface,
                                                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                                                    )
                                                                }
                                                                Icon(
                                                                    imageVector = if (isCamoMenuExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                                    contentDescription = null,
                                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                            
                                                            if (isCamoMenuExpanded) {
                                                                Spacer(modifier = Modifier.height(12.dp))
                                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                                
                                                                // Option: Auto
                                                                Row(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clip(RoundedCornerShape(12.dp))
                                                                        .clickable {
                                                                            scope.launch {
                                                                                settingsManager.setGlobalCamouflagePinnedIp("")
                                                                                if (vpnState == "CONNECTED") startVpnService(context)
                                                                            }
                                                                        }
                                                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Settings,
                                                                            contentDescription = null,
                                                                            tint = if (settings.globalCamouflagePinnedIp.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                        Spacer(modifier = Modifier.width(10.dp))
                                                                        Text(
                                                                            text = "Auto (Fastest IP)",
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            color = if (settings.globalCamouflagePinnedIp.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                            fontWeight = if (settings.globalCamouflagePinnedIp.isEmpty()) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                                        )
                                                                    }
                                                                    if (settings.globalCamouflagePinnedIp.isEmpty()) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Check,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(18.dp)
                                                                        )
                                                                    }
                                                                }
                                                                
                                                                if (scannedIps.isEmpty()) {
                                                                    Spacer(modifier = Modifier.height(4.dp))
                                                                    Text(
                                                                        text = "No scanned IPs. Run a scan to see working targets.",
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                                    )
                                                                } else {
                                                                    // Scanned IPs list
                                                                    scannedIps.forEach { scannedIp ->
                                                                        val isSelected = settings.globalCamouflagePinnedIp == scannedIp.ip
                                                                        Row(
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .clip(RoundedCornerShape(12.dp))
                                                                                .clickable {
                                                                                    scope.launch {
                                                                                        settingsManager.setGlobalCamouflagePinnedIp(scannedIp.ip)
                                                                                        if (vpnState == "CONNECTED") startVpnService(context)
                                                                                    }
                                                                                }
                                                                                .padding(vertical = 10.dp, horizontal = 8.dp),
                                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                Icon(
                                                                                    imageVector = Icons.Default.List,
                                                                                    contentDescription = null,
                                                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                    modifier = Modifier.size(20.dp)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(10.dp))
                                                                                Text(
                                                                                    text = scannedIp.ip,
                                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                                                                )
                                                                            }
                                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                val pingColor = when {
                                                                                    scannedIp.latencyMs < 150 -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
                                                                                    scannedIp.latencyMs < 300 -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Orange
                                                                                    else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
                                                                                }
                                                                                Text(
                                                                                    text = "${scannedIp.latencyMs} ms",
                                                                                    style = MaterialTheme.typography.labelSmall,
                                                                                    color = pingColor,
                                                                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                                                    modifier = Modifier
                                                                                        .clip(RoundedCornerShape(6.dp))
                                                                                        .background(pingColor.copy(alpha = 0.1f))
                                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                                )
                                                                                if (isSelected) {
                                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                                    Icon(
                                                                                        imageVector = Icons.Default.Check,
                                                                                        contentDescription = null,
                                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                                        modifier = Modifier.size(18.dp)
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

                                                Spacer(modifier = Modifier.height(16.dp))
                                                Button(
                                                    onClick = {
                                                        isScanningCamo = true
                                                        scope.launch {
                                                            val customIpsList = if (settings.globalCamouflagePreset == "custom") {
                                                                settings.globalCamouflageCustomIps.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
                                                            } else {
                                                                emptyList()
                                                            }
                                                            val timeoutVal = settings.globalCamouflageTimeout.toIntOrNull() ?: 600
                                                            
                                                            val res = com.hambalapps.chameleon.vpn.CdnIpScanner.performScan(
                                                                preset = settings.globalCamouflagePreset,
                                                                customIps = customIpsList,
                                                                port = 443,
                                                                timeoutMs = timeoutVal
                                                            )
                                                            
                                                            isScanningCamo = false
                                                            lastScanTrigger++
                                                            
                                                            val msg = if (res.workingIpsCount > 0) {
                                                                "Scan completed! Found ${res.workingIpsCount} clean CDN edge IPs. Best: ${res.fastestIp} (${res.fastestLatencyMs}ms)"
                                                            } else {
                                                                "Scan completed! No clean IPs found. Check connection or increase timeout."
                                                            }
                                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                            if (vpnState == "CONNECTED") {
                                                                startVpnService(context)
                                                            }
                                                        }
                                                    },
                                                    enabled = !isScanningCamo,
                                                    modifier = Modifier.fillMaxWidth().pressScaleEffect(),
                                                    shape = ExpressiveButtonShape
                                                ) {
                                                    if (isScanningCamo) {
                                                        LoadingIndicator(
                                                            modifier = Modifier.size(20.dp),
                                                            color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Scanning CDN Edges...")
                                                    } else {
                                                        Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Scan Edges Now")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                                    .clickable { onItemClick(SplitTunneling) }
                                    .pressScaleEffect(),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                VibrantCardContent(settings.cardStyle) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(stringResource(R.string.split_tunneling), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = if (splitTunnelingEnabled) {
                                                    val modeText = if (splitTunnelingMode == "bypass") stringResource(R.string.bypass_apps) else stringResource(R.string.route_apps)
                                                    "$modeText: ${splitTunnelingApps.size}"
                                                } else stringResource(R.string.split_tunneling_desc),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ExpressiveChipShape)
                                                .background(if (splitTunnelingEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (splitTunnelingEnabled) stringResource(R.string.state_on) else stringResource(R.string.state_off),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (splitTunnelingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Category 4 Header: System Updates
                            Text(
                                text = "System Updates",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, bottom = 8.dp)
                            )
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(brush = tertiaryCardBrush, shape = ExpressiveCardShape)
                                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                shape = ExpressiveCardShape,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                VibrantCardContent(settings.cardStyle) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(stringResource(R.string.auto_update), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.auto_update_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Switch(checked = autoUpdateSubs, onCheckedChange = { scope.launch { settingsManager.setAutoUpdateSubs(it) } })
                                        }
                                        AnimatedVisibility(visible = autoUpdateSubs) {
                                            Column {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Text("Update Frequency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
                                                ConnectedButtonGroup(
                                                    selectedIndex = when (autoUpdateInterval) {
                                                        "startup" -> 0
                                                        "hourly" -> 1
                                                        "daily" -> 2
                                                        "weekly" -> 3
                                                        else -> 2
                                                    },
                                                    options = listOf("Startup", "Hourly", "Daily", "Weekly"),
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                                    selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    onSelect = { index ->
                                                        val intervalKey = when (index) {
                                                            0 -> "startup"
                                                            1 -> "hourly"
                                                            2 -> "daily"
                                                            3 -> "weekly"
                                                            else -> "daily"
                                                        }
                                                        scope.launch { settingsManager.setAutoUpdateInterval(intervalKey) }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                AnimatedVisibility(visible = autoUpdateInterval == "hourly") {
                                                    Column {
                                                        Spacer(modifier = Modifier.height(16.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("Hourly Interval", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                            Text("Every ${settings.autoUpdateIntervalHours} hour${if (settings.autoUpdateIntervalHours > 1) "s" else ""}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Slider(
                                                            value = animatedUpdateHours,
                                                            onValueChange = {
                                                                localUpdateHours = it
                                                                scope.launch { settingsManager.setAutoUpdateIntervalHours(it.toInt()) }
                                                            },
                                                            valueRange = 1f..24f,
                                                            steps = 23,
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = standardColorScheme.primary,
                                                                activeTrackColor = standardColorScheme.primary,
                                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                                activeTickColor = Color.Transparent,
                                                                inactiveTickColor = Color.Transparent
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Category 2 Header: Advanced Tweaks (with Switch!)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Advanced Tweaks",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Switch(
                                    checked = showAdvancedSettings,
                                    onCheckedChange = { showAdvancedSettings = it }
                                )
                            }
                            
                            AnimatedVisibility(visible = showAdvancedSettings) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                        .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                    shape = ExpressiveCardShape,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    VibrantCardContent(settings.cardStyle) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            // TUN Stack selector
                                            Text(stringResource(R.string.tun_network_stack), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            ConnectedButtonGroup(
                                                selectedIndex = when (tunStack) {
                                                    "mixed" -> 0
                                                    "gvisor" -> 1
                                                    "system" -> 2
                                                    else -> 0
                                                },
                                                options = listOf("Mixed", "gVisor", "System"),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                onSelect = { index ->
                                                    val stackVal = when (index) {
                                                        0 -> "mixed"
                                                        1 -> "gvisor"
                                                        2 -> "system"
                                                        else -> "mixed"
                                                    }
                                                    scope.launch { settingsManager.setTunStack(stackVal); if (vpnState == "CONNECTED") startVpnService(context) }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // VPN MTU Slider
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("VPN MTU", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text("${settings.vpnMtu} bytes", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Slider(
                                                    value = animatedMtu,
                                                    onValueChange = {
                                                        localMtu = it
                                                        scope.launch { settingsManager.setVpnMtu(it.toInt()) }
                                                    },
                                                    valueRange = 1200f..1500f,
                                                    steps = 300,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = standardColorScheme.primary,
                                                        activeTrackColor = standardColorScheme.primary,
                                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                        activeTickColor = Color.Transparent,
                                                        inactiveTickColor = Color.Transparent
                                                    )
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(12.dp))

                                            // TLS Fragmentation Toggle
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.tls_fragmentation), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.tls_fragmentation_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(checked = enableFragment, onCheckedChange = { scope.launch { settingsManager.setEnableFragment(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                            }

                                            AnimatedVisibility(visible = enableFragment) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    // Fragment Length Input
                                                    OutlinedTextField(
                                                        value = fragmentLength,
                                                        onValueChange = { scope.launch { settingsManager.setFragmentLength(it) } },
                                                        label = { Text("Fragment Packet Length (e.g. 10-20)") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = ExpressiveButtonShape
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    // Fragment Interval Slider
                                                    val intervalVal = fragmentInterval.toIntOrNull() ?: 10
                                                    Column {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("Fragment Delay Interval", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                            Text("${intervalVal} ms", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Slider(
                                                            value = animatedFragmentDelay,
                                                            onValueChange = {
                                                                localFragmentInterval = it
                                                                scope.launch { settingsManager.setFragmentInterval(it.toInt().toString()) }
                                                            },
                                                            valueRange = 1f..100f,
                                                            steps = 100,
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = standardColorScheme.primary,
                                                                activeTrackColor = standardColorScheme.primary,
                                                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                                activeTickColor = Color.Transparent,
                                                                inactiveTickColor = Color.Transparent
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // TCP Multiplexing
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.tcp_multiplexing), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.tcp_multiplexing_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(checked = enableMux, onCheckedChange = { scope.launch { settingsManager.setEnableMux(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Enable Debug Logging
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.enable_debug_logging), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.enable_debug_logging_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(checked = enableDebugLogging, onCheckedChange = { scope.launch { settingsManager.setEnableDebugLogging(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Reset to Defaults Button
                                            OutlinedButton(
                                                onClick = {
                                                    scope.launch {
                                                        settingsManager.setTunStack("mixed")
                                                        settingsManager.setVpnMtu(1280)
                                                        localMtu = 1280f
                                                        settingsManager.setEnableFragment(false)
                                                        settingsManager.setFragmentLength("10-20")
                                                        settingsManager.setFragmentInterval("10")
                                                        localFragmentInterval = 10f
                                                        settingsManager.setEnableMux(false)
                                                        settingsManager.setEnableDebugLogging(false)
                                                        settingsManager.setRootMode(false)
                                                        settingsManager.setSecureDns("https://8.8.8.8/dns-query")
                                                        
                                                        if (vpnState == "CONNECTED") startVpnService(context)
                                                        
                                                        android.widget.Toast.makeText(context, "Advanced settings reset to defaults", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().pressScaleEffect(),
                                                shape = ExpressiveButtonShape,
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Reset Advanced Tweaks", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Category 3 Header: Preferences & Style
                            Text(
                                text = "Preferences & Style",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, bottom = 8.dp)
                            )
                            
                            if (true) {
                                val defaultThemeKey = if (Config.IS_SPECIAL) "cherry_blossom" else "dynamic"
                                val currentTheme by settingsManager.specialTheme.collectAsStateWithLifecycle(initialValue = defaultThemeKey)
                                val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle(initialValue = "system")
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                                        .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                                    shape = ExpressiveCardShape,
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    VibrantCardContent(settings.cardStyle) {
                                        Column(modifier = Modifier.padding(16.dp)) {

                                            // Live Notification Toggle
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.live_stats), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.live_stats_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(checked = showLiveNotification, onCheckedChange = { scope.launch { settingsManager.setShowLiveNotification(it); if (vpnState == "CONNECTED") startVpnService(context) } })
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Logs Tab Toggle
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(stringResource(R.string.show_logs_tab), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Text(stringResource(R.string.show_logs_tab_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Switch(checked = showLogsTab, onCheckedChange = { scope.launch { settingsManager.setShowLogsTab(it) } })
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Ping Delay Test URL custom field
                                            OutlinedTextField(
                                                value = delayTestUrl,
                                                onValueChange = { scope.launch { settingsManager.setDelayTestUrl(it) } },
                                                label = { Text("Ping Delay Test URL") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = ExpressiveButtonShape
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(
                                                    "https://cp.cloudflare.com/generate_204" to "Cloudflare",
                                                    "https://www.google.com/generate_204" to "Google",
                                                    "https://www.gstatic.com/generate_204" to "GStatic",
                                                    "https://play.googleapis.com/generate_204" to "Google Play"
                                                ).forEach { (urlVal, label) ->
                                                    FilterChip(
                                                        selected = delayTestUrl == urlVal,
                                                        onClick = { scope.launch { settingsManager.setDelayTestUrl(urlVal) } },
                                                        label = { Text(label) },
                                                        shape = ExpressiveButtonShape,
                                                        modifier = Modifier.pressScaleEffect()
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Theme Mode Connected Button Group
                                            Text(stringResource(R.string.dark_mode_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            ConnectedButtonGroup(
                                                selectedIndex = when (themeMode) {
                                                    "system" -> 0
                                                    "light" -> 1
                                                    "dark" -> 2
                                                    else -> 0
                                                },
                                                options = listOf("System", "Light", "Dark"),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                onSelect = { index ->
                                                    val modeVal = when (index) {
                                                        0 -> "system"
                                                        1 -> "light"
                                                        2 -> "dark"
                                                        else -> "system"
                                                    }
                                                    scope.launch { settingsManager.setThemeMode(modeVal) }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Card Style Connected Button Group
                                            Text(stringResource(R.string.style_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            ConnectedButtonGroup(
                                                selectedIndex = when (cardStyle) {
                                                    "glass" -> 0
                                                    "solid" -> 1
                                                    "vibrant" -> 2
                                                    "tonal" -> 3
                                                    else -> 2
                                                },
                                                options = listOf("Glass", "Solid", "Vibrant", "Tonal"),
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                onSelect = { index ->
                                                    val styleVal = when (index) {
                                                        0 -> "glass"
                                                        1 -> "solid"
                                                        2 -> "vibrant"
                                                        3 -> "tonal"
                                                        else -> "vibrant"
                                                    }
                                                    scope.launch { settingsManager.setCardStyle(styleVal) }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Theme Palette Selection
                                            Text(stringResource(R.string.app_theme), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                listOf(
                                                    "dynamic" to R.string.theme_dynamic,
                                                    "cherry_blossom" to R.string.theme_cherry,
                                                    "lavender_dreams" to R.string.theme_lavender,
                                                    "rose_gold" to R.string.theme_rosegold,
                                                    "midnight_blue" to R.string.theme_midnight,
                                                    "forest_green" to R.string.theme_forest,
                                                    "sunset_orange" to R.string.theme_sunset,
                                                    "ocean_teal" to R.string.theme_teal,
                                                    "royal_amethyst" to R.string.theme_amethyst,
                                                    "nordic_slate" to R.string.theme_slate
                                                ).filter { (themeKey, _) ->
                                                    Config.IS_SPECIAL || (themeKey != "cherry_blossom" && themeKey != "lavender_dreams" && themeKey != "rose_gold")
                                                }.forEach { (themeKey, stringId) ->
                                                    FilterChip(
                                                        selected = currentTheme == themeKey,
                                                        onClick = { scope.launch { settingsManager.setSpecialTheme(themeKey) } },
                                                        label = { Text(stringResource(stringId)) },
                                                        shape = ExpressiveButtonShape
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            ExpressiveCard(
                                modifier = Modifier.fillMaxWidth(),
                                brush = secondaryCardBrush,
                                borderBrush = cardBorderBrush,
                                cardStyle = settings.cardStyle
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Expressive Box $appVersion",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (Config.IS_SPECIAL) "Made with ❤️ for Sana" else stringResource(R.string.app_short_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(110.dp))
                        }
                    }
                }
                }
                }

                if (!isMultiSelectMode) {
                    val glassBorderBrush = remember(isDark) {
                        val topColor = if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.45f)
                        val bottomColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.12f)
                        Brush.verticalGradient(listOf(topColor, bottomColor))
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .width(LocalConfiguration.current.screenWidthDp.dp * 0.68f)
                            .height(58.dp)
                            .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background layer containing blurred dynamic gradient copy in dark mode, or solid background in light mode
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (cardStyle == "glass") {
                                        Modifier
                                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                                            .blur(radius = 16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                    } else if (isDark) {
                                        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    } else {
                                        Modifier.background(MaterialTheme.colorScheme.background)
                                    }
                                )
                        )
                        
                        // Main content container
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, glassBorderBrush, CircleShape)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                                val totalWidth = maxWidth
                                val count = tabs.size
                                if (count > 0) {
                                    val itemWidth = totalWidth / count
                                    val targetOffset = itemWidth * (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                                    
                                    val animatedOffset by animateDpAsState(
                                        targetValue = targetOffset,
                                        animationSpec = spring(
                                            dampingRatio = 0.65f, // moderate bounce (expressive spatial)
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "navIndicatorOffset"
                                    )
                                    
                                    // Sliding tab background pill (solid primary!)
                                    Box(
                                        modifier = Modifier
                                            .offset(x = animatedOffset)
                                            .width(itemWidth)
                                            .fillMaxHeight()
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    
                                    // Tabs Row
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        tabs.forEachIndexed { index, (tabId, label, icon) ->
                                            val isSelected = pagerState.currentPage == index
                                            val activeColor = MaterialTheme.colorScheme.onPrimary
                                            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            val color by animateColorAsState(
                                                targetValue = if (isSelected) activeColor else inactiveColor,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "navColor"
                                            )
                                            val scale by animateFloatAsState(
                                                targetValue = if (isSelected) 1.18f else 1.0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.55f, // bouncy spatial spring
                                                    stiffness = Spring.StiffnessMediumLow
                                                ),
                                                label = "navScale"
                                            )
                                            
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        scope.launch { pagerState.animateScrollToPage(index) }
                                                    }
                                                    .pressScaleEffect(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = color,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .graphicsLayer {
                                                            scaleX = scale
                                                            scaleY = scale
                                                        }
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = color
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

    var isImportFetching by remember { mutableStateOf(false) }

    // Edit Subscription Dialog
    if (showEditSubDialog && editingSubscription != null) {
        var isSubEditFetching by remember { mutableStateOf(false) }
        var subEditError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showEditSubDialog = false
                editingSubscription = null
            },
            title = { Text("Edit Subscription") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editSubNameInput,
                        onValueChange = { editSubNameInput = it },
                        label = { Text("Subscription Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveButtonShape,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editSubUrlInput,
                        onValueChange = { editSubUrlInput = it },
                        label = { Text("Subscription Link") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveButtonShape,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    scanResultCallback = { result ->
                                        editSubUrlInput = result
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan QR Code"
                                )
                            }
                        }
                    )
                    subEditError?.let { err ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isSubEditFetching = true
                            subEditError = null
                            try {
                                val urlTrimmed = editSubUrlInput.trim()
                                val nameTrimmed = editSubNameInput.trim()
                                val updatedSub = if (urlTrimmed != editingSubscription!!.url) {
                                    val result = fetchSubscription(urlTrimmed)
                                    editingSubscription!!.copy(
                                        name = nameTrimmed,
                                        url = urlTrimmed,
                                        servers = result.servers.joinToString("\n"),
                                        upload = result.upload,
                                        download = result.download,
                                        total = result.total,
                                        expire = result.expire
                                    )
                                } else {
                                    editingSubscription!!.copy(
                                        name = nameTrimmed
                                    )
                                }
                                val updatedList = subscriptions.map {
                                    if (it.id == editingSubscription!!.id) updatedSub else it
                                }
                                settingsManager.setSubscriptionList(serializeSubscriptions(updatedList.filter { !it.url.startsWith("local://") }))
                                
                                if (editingSubscription!!.id == activeSubId) {
                                    val servers = updatedSub.servers.split("\n").filter { it.isNotEmpty() }
                                    if (servers.isNotEmpty()) {
                                        settingsManager.setActiveProfile(servers[0])
                                        if (vpnState == "CONNECTED") {
                                            startVpnService(context)
                                        }
                                    }
                                }
                                showEditSubDialog = false
                                editingSubscription = null
                            } catch (e: Exception) {
                                subEditError = "Failed to update: ${e.message}"
                            } finally {
                                isSubEditFetching = false
                            }
                        }
                    },
                    enabled = !isSubEditFetching && editSubNameInput.isNotEmpty() && editSubUrlInput.isNotEmpty()
                ) {
                    if (isSubEditFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditSubDialog = false
                        editingSubscription = null
                    },
                    enabled = !isSubEditFetching
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Import Profile Dialog
    if (showImportDialog) {
        val isImportingSubscription = remember(importText) {
            val trimmed = importText.trim()
            (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
            !trimmed.startsWith("vless://") &&
            !trimmed.startsWith("trojan://") &&
            !trimmed.startsWith("ss://") &&
            !trimmed.startsWith("socks5://") &&
            !trimmed.startsWith("socks://") &&
            !trimmed.startsWith("vmess://") &&
            !trimmed.startsWith("hysteria2://") &&
            !trimmed.startsWith("hy2://") &&
            !trimmed.startsWith("tuic://") &&
            !trimmed.startsWith("openvpn://") &&
            !trimmed.startsWith("ovpn://") &&
            !trimmed.startsWith("awg://") &&
            !trimmed.startsWith("amneziawg://") &&
            !trimmed.startsWith("wireguard://") &&
            !trimmed.startsWith("masque://")
        }

        AlertDialog(
            onDismissRequest = { if (!isImportFetching) showImportDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.import_config_link),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.paste_config_link),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = ExpressiveButtonShape,
                        placeholder = { Text("vless://, awg://, or raw .conf contents") },
                        enabled = !isImportFetching
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveButtonShape,
                        enabled = !isImportFetching
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Choose .conf File")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showImportDialog = false
                            editingNodeLink = "new_node"
                            editType = "vless"
                            editRemark = ""
                            editServer = ""
                            editPort = "443"
                            editCreds = ""
                            editTls = false
                            editSni = ""
                            editLinkInput = ""
                            editorMode = "form"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveButtonShape,
                        enabled = !isImportFetching,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Manually (Manual Config)")
                    }
                    if (isImportingSubscription) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
                        val detectionText = if (isRtl) {
                            "لینک اشتراک شناسایی شد! با زدن درون‌ریزی، کانفیگ‌های آن دریافت و به مدیریت اشتراک‌ها اضافه می‌شوند."
                        } else {
                            "Subscription link detected! Clicking import will fetch its configs and add it to your Subscription Manager."
                        }
                        Text(
                            text = detectionText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedImport = importText.trim()
                        if (trimmedImport.contains("dev tun") || trimmedImport.lowercase().startsWith("client") || trimmedImport.lowercase().contains("client\n") || trimmedImport.lowercase().contains("client\r") || trimmedImport.startsWith("openvpn://")) {
                            android.widget.Toast.makeText(context, "OpenVPN is not supported", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            scope.launch {
                                if (trimmedImport.isNotEmpty()) {
                                    if (isImportingSubscription) {
                                        isImportFetching = true
                                        try {
                                            val result = fetchSubscription(trimmedImport)
                                            if (result.servers.isNotEmpty()) {
                                                val domain = try {
                                                    java.net.URI(trimmedImport).host ?: context.getString(R.string.custom_provider)
                                                } catch (e: Exception) {
                                                    context.getString(R.string.custom_provider)
                                                }
                                                val newSub = Subscription(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    name = domain,
                                                    url = trimmedImport,
                                                    servers = result.servers.joinToString("\n"),
                                                    upload = result.upload,
                                                    download = result.download,
                                                    total = result.total,
                                                    expire = result.expire
                                                )
                                                val updatedList = subscriptions + newSub
                                                settingsManager.setSubscriptionList(serializeSubscriptions(updatedList))
                                                settingsManager.setActiveSubId(newSub.id)
                                                settingsManager.setActiveProfile(result.servers[0])
                                                
                                                if (vpnState == "CONNECTED") {
                                                    startVpnService(context)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("Chameleon", "Failed to fetch subscription on import: ${e.message}")
                                        } finally {
                                            isImportFetching = false
                                        }
                                    } else {
                                        val finalLink = if (trimmedImport.contains("[Interface]") && trimmedImport.contains("[Peer]")) {
                                            val b64 = android.util.Base64.encodeToString(trimmedImport.toByteArray(), android.util.Base64.NO_WRAP)
                                            "awg://vpn?config=$b64#AmneziaWG_Imported"
                                        } else {
                                            trimmedImport
                                        }
                                        val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                        val newLinkWithoutRemark = finalLink.substringBefore("#")
                                        val updatedManualList = (currentManualList.filter { it.substringBefore("#") != newLinkWithoutRemark } + finalLink).distinct()
                                        settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                                        settingsManager.setActiveSubId("manual")
                                        settingsManager.setActiveProfile(finalLink)
                                        if (vpnState == "CONNECTED") {
                                            startVpnService(context)
                                        }
                                    }
                                }
                                showImportDialog = false
                                importText = ""
                            }
                        }
                    },
                    modifier = Modifier.pressScaleEffect(),
                    shape = ExpressiveButtonShape,
                    enabled = !isImportFetching && importText.trim().isNotEmpty()
                ) {
                    if (isImportFetching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.import_str))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImportDialog = false },
                    modifier = Modifier.pressScaleEffect(),
                    enabled = !isImportFetching
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = ExpressiveCardShape
        )
    }

    // Observe/Parse link when editor opens
    LaunchedEffect(editingNodeLink) {
        val link = editingNodeLink
        if (link != null) {
            if (link == "new_node") {
                editType = "vless"
                editRemark = "New Node"
                editUsername = ""
                editPassword = ""
                editServer = ""
                editPort = "443"
                editCreds = ""
                editTls = false
                editSni = ""
                editLinkInput = ""
                editorMode = "form"
                editShowAdvanced = false
                editTransportType = "tcp"
                editTransportPath = ""
                editTransportHost = ""
                editTransportServiceName = ""
                editTransportSeed = ""
                editTransportHeaderType = "none"
                editCamouflageEnabled = false
                editCamouflagePreset = "cloudflare"
                editCamouflageSni = ""
                editCamouflageHost = ""
                editFlow = ""
                editRealityEnabled = false
                editRealityPbk = ""
                editRealitySid = ""
                editRealitySpx = ""
                editUtlsFingerprint = "chrome"
                editMasqueProfileId = ""
                editMasqueToken = ""
                editMasqueUseHttp2 = false
                editMasqueUseIpv6 = false
            } else if (link.startsWith("{")) {
                editorMode = "link"
                editLinkInput = link
                editUsername = ""
                editPassword = ""
            } else if (link.startsWith("awg://") || link.startsWith("amneziawg://")) {
                editorMode = "link"
                val trimmed = link.trim()
                val fragmentIdx = trimmed.indexOf("#")
                editRemark = if (fragmentIdx >= 0) {
                    try { java.net.URLDecoder.decode(trimmed.substring(fragmentIdx + 1), "UTF-8") } catch(e: Exception) { trimmed.substring(fragmentIdx + 1) }
                } else { "" }
                
                val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
                
                val queryParams = mutableMapOf<String, String>()
                val queryIdx = rest.indexOf("?")
                if (queryIdx >= 0) {
                    val queryPart = rest.substring(queryIdx + 1)
                    val pairs = queryPart.split("&")
                    for (pair in pairs) {
                        val idx = pair.indexOf("=")
                        if (idx > 0) {
                            try {
                                val k = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                                val v = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                                queryParams[k] = v
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                }
                
                val base64Config = queryParams["config"] ?: ""
                editUsername = queryParams["username"] ?: ""
                editPassword = queryParams["password"] ?: ""
                
                val decodedConfig = try {
                    String(android.util.Base64.decode(base64Config, android.util.Base64.DEFAULT))
                } catch (e: Exception) {
                    base64Config
                }
                editLinkInput = decodedConfig
                editType = "amneziawg"
            } else {
                editUsername = ""
                editPassword = ""
                try {
                    val trimmed = link.trim()
                    val fragmentIdx = trimmed.indexOf("#")
                    editRemark = if (fragmentIdx >= 0) {
                        try { java.net.URLDecoder.decode(trimmed.substring(fragmentIdx + 1), "UTF-8") } catch(e: Exception) { trimmed.substring(fragmentIdx + 1) }
                    } else { "" }
                    
                    val rest = if (fragmentIdx >= 0) trimmed.substring(0, fragmentIdx) else trimmed
                    val schemeIdx = rest.indexOf("://")
                    val scheme = if (schemeIdx >= 0) rest.substring(0, schemeIdx).lowercase() else "vless"
                    editType = scheme
                    
                    val content = if (schemeIdx >= 0) rest.substring(schemeIdx + 3) else rest
                    val queryIdx = content.indexOf("?")
                    val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content
                    val queryPart = if (queryIdx >= 0) content.substring(queryIdx + 1) else ""
                    
                    val atIdx = mainPart.indexOf("@")
                    editCreds = if (atIdx >= 0) mainPart.substring(0, atIdx) else ""
                    val serverPart = if (atIdx >= 0) mainPart.substring(atIdx + 1) else mainPart
                    
                    val colonIdx = serverPart.lastIndexOf(":")
                    editServer = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
                    editPort = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
                    
                    val queryParams = mutableMapOf<String, String>()
                    if (queryPart.isNotEmpty()) {
                        val pairs = queryPart.split("&")
                        for (pair in pairs) {
                            val idx = pair.indexOf("=")
                            if (idx > 0) {
                                val k = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                                val v = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                                queryParams[k] = v
                            }
                        }
                    }
                    val security = queryParams["security"]?.lowercase()
                    editTls = security == "tls" || security == "reality" || queryParams["tls"] == "true" || queryParams["tls"] == "1" || scheme == "https"
                    editSni = queryParams["sni"] ?: ""
                    
                    val type = queryParams["type"] ?: "tcp"
                    editTransportType = type
                    editTransportPath = queryParams["path"] ?: ""
                    editTransportHost = queryParams["host"] ?: ""
                    editTransportServiceName = queryParams["serviceName"] ?: queryParams["service_name"] ?: ""
                    editTransportSeed = queryParams["seed"] ?: ""
                    editTransportHeaderType = queryParams["headerType"] ?: queryParams["header_type"] ?: queryParams["header"] ?: "none"
                    editShowAdvanced = type != "tcp" && type.isNotEmpty()

                    editFlow = queryParams["flow"] ?: ""
                    editRealityEnabled = security == "reality"
                    editRealityPbk = queryParams["pbk"] ?: ""
                    editRealitySid = queryParams["sid"] ?: ""
                    editRealitySpx = queryParams["spx"] ?: ""
                    editUtlsFingerprint = queryParams["fp"] ?: "chrome"
                    editMasqueProfileId = queryParams["id"] ?: queryParams["profile_id"] ?: ""
                    editMasqueToken = queryParams["token"] ?: queryParams["auth_token"] ?: ""
                    editMasqueUseHttp2 = queryParams["use_http2"] == "true" || queryParams["use_http2"] == "1"
                    editMasqueUseIpv6 = queryParams["use_ipv6"] == "true" || queryParams["use_ipv6"] == "1"
                    
                    editLinkInput = link
                    editorMode = "form"

                    // Parse existing camouflage config for this node
                    val configs = deserializeCamouflageSettings(settings.camouflageSettings)
                    val configLinkWithoutRemark = link.substringBefore("#")
                    val camConfig = configs.find { it.nodeLink.substringBefore("#") == configLinkWithoutRemark }
                    if (camConfig != null) {
                        editCamouflageEnabled = camConfig.enabled
                        editCamouflagePreset = camConfig.preset
                        editCamouflageSni = camConfig.customSni
                        editCamouflageHost = camConfig.customHost
                    } else {
                        editCamouflageEnabled = false
                        editCamouflagePreset = "cloudflare"
                        editCamouflageSni = ""
                        editCamouflageHost = ""
                    }
                } catch(e: Exception) {
                    editorMode = "link"
                    editLinkInput = link
                }
            }
        }
    }

    // Edit/Create Node Dialog
    if (editingNodeLink != null) {
        val link = editingNodeLink!!
        val isChain = link.startsWith("chain://") || link == "new_chain"
        if (isChain) {
            ChainBuilderDialog(
                editingChainLink = link,
                proxyChainsStr = settings.proxyChains,
                serverList = serverList,
                onDismiss = { editingNodeLink = null },
                onSave = { name, relay, exit ->
                    scope.launch {
                        val currentChains = deserializeProxyChains(settings.proxyChains).toMutableList()
                        val isNewChain = link == "new_chain"
                        
                        val chainId = if (isNewChain) {
                            java.util.UUID.randomUUID().toString()
                        } else {
                            link.substringAfter("chain://").substringBefore("#")
                        }
                        
                        currentChains.removeAll { it.id == chainId }
                        currentChains.add(ProxyChain(id = chainId, name = name, relayLink = relay, exitLink = exit))
                        settingsManager.setProxyChains(serializeProxyChains(currentChains))
                        
                        val finalLink = "chain://$chainId#${java.net.URLEncoder.encode(name, "UTF-8")}"
                        
                        if (isNewChain) {
                            val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            val newLinkWithoutRemark = finalLink.substringBefore("#")
                            val updatedManualList = (currentManualList.filter { it.substringBefore("#") != newLinkWithoutRemark } + finalLink).distinct()
                            settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                            settingsManager.setActiveSubId("manual")
                            settingsManager.setActiveProfile(finalLink)
                        } else {
                            val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            val updatedManualList = currentManualList.map {
                                if (it == link) finalLink else it
                            }.distinct()
                            settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                            if (activeProfile == link) {
                                settingsManager.setActiveProfile(finalLink)
                            }
                        }
                        
                        if (vpnState == "CONNECTED") {
                            startVpnService(context)
                        }
                        editingNodeLink = null
                    }
                }
            )
        } else {
            val isNewNode = editingNodeLink == "new_node"
            AlertDialog(
                onDismissRequest = { editingNodeLink = null },
            title = {
                Text(
                    text = if (isNewNode) stringResource(R.string.create_custom_node) else stringResource(R.string.edit_node_config),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editRemark,
                        onValueChange = { editRemark = it },
                        label = { Text(stringResource(R.string.remark_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = ExpressiveButtonShape,
                        singleLine = true
                    )

                    if (editType != "amneziawg") {
                        TabRow(
                            selectedTabIndex = if (editorMode == "form") 0 else 1,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = editorMode == "form",
                                onClick = { editorMode = "form" },
                                text = { Text(stringResource(R.string.form_editor)) }
                            )
                            Tab(
                                selected = editorMode == "link",
                                onClick = { editorMode = "link" },
                                text = { Text(stringResource(R.string.raw_config)) }
                            )
                        }
                    } else {
                        LaunchedEffect(Unit) {
                            editorMode = "link"
                        }
                    }

                    if (editorMode == "form") {
                        // Protocol selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.protocol), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("vless", "trojan", "ss").forEach { proto ->
                                    val label = if (proto == "ss") "Shadowsocks" else proto.uppercase()
                                    FilterChip(
                                        selected = editType == proto,
                                        onClick = { 
                                            editType = proto
                                            if (proto == "ss") editTls = false
                                        },
                                        label = { Text(label) },
                                        shape = ExpressiveChipShape
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("socks5", "http", "https", "masque").forEach { proto ->
                                    FilterChip(
                                        selected = editType == proto,
                                        onClick = { 
                                            editType = proto 
                                            if (proto == "https") editTls = true
                                        },
                                        label = { Text(proto.uppercase()) },
                                        shape = ExpressiveChipShape
                                    )
                                }
                            }
                        }


                        // Server & Port
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editServer,
                                onValueChange = { editServer = it },
                                label = { Text(stringResource(R.string.server_address)) },
                                modifier = Modifier.weight(2f),
                                shape = ExpressiveButtonShape
                            )
                            OutlinedTextField(
                                value = editPort,
                                onValueChange = { editPort = it },
                                label = { Text(stringResource(R.string.port)) },
                                modifier = Modifier.weight(1f),
                                shape = ExpressiveButtonShape
                            )
                        }

                        // Credentials
                        OutlinedTextField(
                            value = editCreds,
                            onValueChange = { editCreds = it },
                            label = { Text(if (editType == "vless") stringResource(R.string.uuid) else if (editType == "masque") "Private Key (Optional)" else stringResource(R.string.password_credentials)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )

                        if (editType == "masque") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editMasqueProfileId,
                                onValueChange = { editMasqueProfileId = it },
                                label = { Text("Profile ID (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ExpressiveButtonShape,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editMasqueToken,
                                onValueChange = { editMasqueToken = it },
                                label = { Text("Auth Token (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ExpressiveButtonShape,
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Use HTTP2 (default: HTTP/3 / QUIC)", fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = editMasqueUseHttp2,
                                    onCheckedChange = { editMasqueUseHttp2 = it }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Use IPv6 (default: IPv4)", fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = editMasqueUseIpv6,
                                    onCheckedChange = { editMasqueUseIpv6 = it }
                                )
                            }
                        }

                        // Flow control (VLESS only)
                        if (editType == "vless") {
                            OutlinedTextField(
                                value = editFlow,
                                onValueChange = { editFlow = it },
                                label = { Text("Flow (e.g. xtls-rprx-vision)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ExpressiveButtonShape,
                                singleLine = true
                            )
                        }

                        // TLS & SNI options
                        val showTlsOption = editType == "vless" || editType == "trojan" || editType == "https"
                        if (showTlsOption) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(stringResource(R.string.enable_tls), fontWeight = FontWeight.Bold)
                                Switch(
                                    checked = editTls || editType == "https",
                                    onCheckedChange = { if (editType != "https") editTls = it },
                                    enabled = editType != "https"
                                )
                            }
                            
                            if (editTls || editType == "https") {
                                OutlinedTextField(
                                    value = editSni,
                                    onValueChange = { editSni = it },
                                    label = { Text(stringResource(R.string.sni_server_name)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = ExpressiveButtonShape,
                                    singleLine = true
                                )

                                if (editType == "vless") {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Enable Reality", fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = editRealityEnabled,
                                            onCheckedChange = { editRealityEnabled = it }
                                        )
                                    }

                                    if (editRealityEnabled) {
                                        OutlinedTextField(
                                            value = editRealityPbk,
                                            onValueChange = { editRealityPbk = it },
                                            label = { Text("Public Key (pbk)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ExpressiveButtonShape,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = editRealitySid,
                                            onValueChange = { editRealitySid = it },
                                            label = { Text("Short ID (sid)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ExpressiveButtonShape,
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = editRealitySpx,
                                            onValueChange = { editRealitySpx = it },
                                            label = { Text("SpiderX (spx)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ExpressiveButtonShape,
                                            singleLine = true
                                        )
                                    }
                                }

                                if (editType == "vless" || editType == "trojan") {
                                    OutlinedTextField(
                                        value = editUtlsFingerprint,
                                        onValueChange = { editUtlsFingerprint = it },
                                        label = { Text("uTLS Fingerprint (fp)") },
                                        placeholder = { Text("chrome") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        // Advanced Settings Toggle
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced Transport Settings",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Switch(
                                checked = editShowAdvanced,
                                onCheckedChange = { editShowAdvanced = it }
                            )
                        }

                        if (editShowAdvanced) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Transport Type selector
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Transport Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("tcp", "ws", "grpc").forEach { trans ->
                                            FilterChip(
                                                selected = editTransportType == trans,
                                                onClick = { editTransportType = trans },
                                                label = { Text(trans.uppercase()) },
                                                shape = ExpressiveChipShape
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("httpupgrade", "mkcp", "xhttp").forEach { trans ->
                                            val label = if (trans == "mkcp") "mKCP" else trans
                                            FilterChip(
                                                selected = editTransportType == trans,
                                                onClick = { editTransportType = trans },
                                                label = { Text(label) },
                                                shape = ExpressiveChipShape
                                            )
                                        }
                                    }
                                }

                                if (editTransportType == "ws" || editTransportType == "httpupgrade" || editTransportType == "xhttp") {
                                    OutlinedTextField(
                                        value = editTransportPath,
                                        onValueChange = { editTransportPath = it },
                                        label = { Text("Path") },
                                        placeholder = { Text("/") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = editTransportHost,
                                        onValueChange = { editTransportHost = it },
                                        label = { Text("Host") },
                                        placeholder = { Text("example.com") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                } else if (editTransportType == "grpc") {
                                    OutlinedTextField(
                                        value = editTransportServiceName,
                                        onValueChange = { editTransportServiceName = it },
                                        label = { Text("Service Name") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                } else if (editTransportType == "kcp" || editTransportType == "mkcp") {
                                    OutlinedTextField(
                                        value = editTransportSeed,
                                        onValueChange = { editTransportSeed = it },
                                        label = { Text("KCP Seed") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = ExpressiveButtonShape,
                                        singleLine = true
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Header Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("none", "srtp", "utp").forEach { hType ->
                                                FilterChip(
                                                    selected = editTransportHeaderType == hType,
                                                    onClick = { editTransportHeaderType = hType },
                                                    label = { Text(hType) },
                                                    shape = ExpressiveChipShape
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("wechat-video", "dtls", "wireguard").forEach { hType ->
                                                FilterChip(
                                                    selected = editTransportHeaderType == hType,
                                                    onClick = { editTransportHeaderType = hType },
                                                    label = { Text(hType) },
                                                    shape = ExpressiveChipShape
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Stealth Camouflage Expandable Section
                        if (editType == "vless" || editType == "trojan" || editType == "vmess" || editType == "ss") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = stringResource(R.string.camouflage_settings),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stringResource(R.string.enable_camouflage), style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = editCamouflageEnabled,
                                    onCheckedChange = { editCamouflageEnabled = it }
                                )
                            }
                            
                            if (editCamouflageEnabled) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(stringResource(R.string.camouflage_preset), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("cloudflare" to "Cloudflare", "cloudfront" to "Cloudfront", "custom" to "Custom").forEach { (presetVal, label) ->
                                            FilterChip(
                                                selected = editCamouflagePreset == presetVal,
                                                onClick = { editCamouflagePreset = presetVal },
                                                label = { Text(label) },
                                                shape = ExpressiveChipShape
                                            )
                                        }
                                    }

                                    if (editCamouflagePreset == "custom") {
                                        OutlinedTextField(
                                            value = editCamouflageSni,
                                            onValueChange = { editCamouflageSni = it },
                                            label = { Text(stringResource(R.string.custom_sni)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ExpressiveButtonShape,
                                            singleLine = true,
                                            placeholder = { Text("e.g. microsoft.com") }
                                        )

                                        OutlinedTextField(
                                            value = editCamouflageHost,
                                            onValueChange = { editCamouflageHost = it },
                                            label = { Text(stringResource(R.string.custom_host)) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = ExpressiveButtonShape,
                                            singleLine = true,
                                            placeholder = { Text("e.g. my-worker.workers.dev") }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Raw text area
                        OutlinedTextField(
                            value = editLinkInput,
                            onValueChange = { editLinkInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = ExpressiveButtonShape,
                            placeholder = { Text(if (editType == "amneziawg") "Paste raw AmneziaWG/WireGuard .conf text" else "Paste link (vless://...) or raw .conf text") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onClickLabel@{
                        val text = editLinkInput.trim()
                        if (editorMode == "link" && (text.contains("dev tun") || text.lowercase().startsWith("client") || text.lowercase().contains("client\n") || text.lowercase().contains("client\r"))) {
                            android.widget.Toast.makeText(context, "OpenVPN is not supported", android.widget.Toast.LENGTH_LONG).show()
                            return@onClickLabel
                        }
                        val originalLink = editingNodeLink
                        if (originalLink != null) {
                            val finalLink = if (editorMode == "link") {
                                val finalRemark = editRemark.trim()
                                val fragmentStr = if (finalRemark.isNotEmpty()) "#" + java.net.URLEncoder.encode(finalRemark, "UTF-8") else ""
                                
                                if (text.contains("[Interface]") && text.contains("[Peer]")) {
                                    val b64 = android.util.Base64.encodeToString(text.toByteArray(), android.util.Base64.NO_WRAP)
                                    "awg://vpn?config=$b64$fragmentStr"
                                } else {
                                    text
                                }
                            } else {
                                try {
                                    val finalUserInfo = editCreds.trim()
                                    val finalServer = editServer.trim()
                                    val finalPort = editPort.trim().toIntOrNull() ?: 443
                                    val finalRemark = editRemark.trim()
                                    val queryList = mutableListOf<String>()
                                     if (editType == "vless" || editType == "trojan") {
                                         if (editTls) {
                                             if (editRealityEnabled && editType == "vless") {
                                                 queryList.add("security=reality")
                                                 if (editRealityPbk.isNotEmpty()) queryList.add("pbk=${java.net.URLEncoder.encode(editRealityPbk.trim(), "UTF-8")}")
                                                 if (editRealitySid.isNotEmpty()) queryList.add("sid=${java.net.URLEncoder.encode(editRealitySid.trim(), "UTF-8")}")
                                                 if (editRealitySpx.isNotEmpty()) queryList.add("spx=${java.net.URLEncoder.encode(editRealitySpx.trim(), "UTF-8")}")
                                             } else {
                                                 queryList.add("security=tls")
                                             }
                                             if (editSni.isNotEmpty()) queryList.add("sni=${java.net.URLEncoder.encode(editSni.trim(), "UTF-8")}")
                                             if (editUtlsFingerprint.isNotEmpty()) queryList.add("fp=${java.net.URLEncoder.encode(editUtlsFingerprint.trim(), "UTF-8")}")
                                         } else {
                                             queryList.add("security=none")
                                         }
                                         if (editFlow.isNotEmpty()) {
                                             queryList.add("flow=${java.net.URLEncoder.encode(editFlow.trim(), "UTF-8")}")
                                         }
                                     } else if (editType == "https") {
                                         if (editSni.isNotEmpty()) queryList.add("sni=${java.net.URLEncoder.encode(editSni.trim(), "UTF-8")}")
                                     } else if (editType == "masque") {
                                         if (editMasqueProfileId.isNotEmpty()) queryList.add("id=${java.net.URLEncoder.encode(editMasqueProfileId.trim(), "UTF-8")}")
                                         if (editMasqueToken.isNotEmpty()) queryList.add("token=${java.net.URLEncoder.encode(editMasqueToken.trim(), "UTF-8")}")
                                         if (editMasqueUseHttp2) queryList.add("use_http2=1")
                                         if (editMasqueUseIpv6) queryList.add("use_ipv6=1")
                                         if (editSni.isNotEmpty()) queryList.add("sni=${java.net.URLEncoder.encode(editSni.trim(), "UTF-8")}")
                                     }

                                    if (editShowAdvanced && editTransportType != "tcp") {
                                        queryList.add("type=$editTransportType")
                                        if (editTransportType == "ws" || editTransportType == "httpupgrade" || editTransportType == "xhttp") {
                                            if (editTransportPath.isNotEmpty()) {
                                                queryList.add("path=${java.net.URLEncoder.encode(editTransportPath.trim(), "UTF-8")}")
                                            }
                                            if (editTransportHost.isNotEmpty()) {
                                                queryList.add("host=${java.net.URLEncoder.encode(editTransportHost.trim(), "UTF-8")}")
                                            }
                                        } else if (editTransportType == "grpc") {
                                            if (editTransportServiceName.isNotEmpty()) {
                                                queryList.add("serviceName=${java.net.URLEncoder.encode(editTransportServiceName.trim(), "UTF-8")}")
                                            }
                                        } else if (editTransportType == "kcp" || editTransportType == "mkcp") {
                                            if (editTransportSeed.isNotEmpty()) {
                                                queryList.add("seed=${java.net.URLEncoder.encode(editTransportSeed.trim(), "UTF-8")}")
                                            }
                                            if (editTransportHeaderType.isNotEmpty()) {
                                                queryList.add("headerType=${java.net.URLEncoder.encode(editTransportHeaderType.trim(), "UTF-8")}")
                                            }
                                        }
                                    }

                                    val queryStr = if (queryList.isNotEmpty()) "?" + queryList.joinToString("&") else ""
                                    val remarkStr = if (finalRemark.isNotEmpty()) "#" + java.net.URLEncoder.encode(finalRemark, "UTF-8") else ""
                                    val protocolScheme = editType
                                    "$protocolScheme://$finalUserInfo@$finalServer:$finalPort$queryStr$remarkStr"
                                } catch (e: Exception) {
                                    ""
                                }
                            }

                            if (finalLink.isNotEmpty()) {
                                scope.launch {
                                    if (isNewNode) {
                                        val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                        val newLinkWithoutRemark = finalLink.substringBefore("#")
                                        val updatedManualList = (currentManualList.filter { it.substringBefore("#") != newLinkWithoutRemark } + finalLink).distinct()
                                        settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                                        settingsManager.setActiveSubId("manual")
                                        settingsManager.setActiveProfile(finalLink)
                                    } else {
                                        val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                        val updatedManualList = currentManualList.map {
                                            if (it == originalLink) finalLink else it
                                        }.distinct()
                                        settingsManager.setManualServers(updatedManualList.joinToString("\n"))
                                        if (activeProfile == originalLink) {
                                            settingsManager.setActiveProfile(finalLink)
                                        }
                                    }

                                    // Save Camouflage configuration
                                    val currentCamList = deserializeCamouflageSettings(settings.camouflageSettings).toMutableList()
                                    val configLinkWithoutRemark = finalLink.substringBefore("#")
                                    currentCamList.removeAll { 
                                        it.nodeLink.substringBefore("#") == configLinkWithoutRemark || 
                                        (!isNewNode && it.nodeLink.substringBefore("#") == originalLink.substringBefore("#")) 
                                    }
                                     
                                    if (editCamouflageEnabled) {
                                        currentCamList.add(
                                            CamouflageConfig(
                                                nodeLink = finalLink,
                                                enabled = editCamouflageEnabled,
                                                preset = editCamouflagePreset,
                                                customSni = editCamouflageSni.trim(),
                                                customHost = editCamouflageHost.trim()
                                            )
                                        )
                                    }
                                    settingsManager.setCamouflageSettings(serializeCamouflageSettings(currentCamList))

                                    if (vpnState == "CONNECTED") {
                                        startVpnService(context)
                                    }
                                    editingNodeLink = null
                                }
                            }
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
                    onClick = { editingNodeLink = null },
                    modifier = Modifier.pressScaleEffect()
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = ExpressiveCardShape
        )
    }
}

    val currentCallback = scanResultCallback
    if (currentCallback != null) {
        QrScannerScreen(
            onScanSuccess = { result ->
                currentCallback(result)
                scanResultCallback = null
            },
            onClose = {
                scanResultCallback = null
            }
        )
    }

    val currentQrShare = qrCodeToShare
    if (currentQrShare != null) {
        QrCodeShareDialog(
            title = currentQrShare.first,
            content = currentQrShare.second,
            onDismiss = { qrCodeToShare = null }
        )
    }

    AnimatedVisibility(
        visible = isNodesExpanded,
        enter = slideInVertically(
            initialOffsetY = { it / 6 }, 
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
        ) + scaleIn(
            initialScale = 0.92f, 
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
        ) + fadeIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it / 6 }, 
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        ) + scaleOut(
            targetScale = 0.92f, 
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        ) + fadeOut(
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        )
    ) {
        androidx.compose.material3.Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                isNodesExpanded = false 
                                isMultiSelectMode = false
                                selectedNodes = emptySet()
                            },
                            modifier = Modifier.pressScaleEffect()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.available_nodes),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) searchQuery = ""
                            },
                            modifier = Modifier.pressScaleEffect()
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
                            enabled = !isTestingPings,
                            modifier = Modifier.pressScaleEffect()
                        ) {
                            if (isTestingPings) {
                                LoadingIndicator(
                                    modifier = Modifier.size(20.dp),
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
                    }

                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (subscriptions.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(subscriptions) { sub ->
                                val isSelected = activeSubId == sub.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        scope.launch {
                                            settingsManager.setActiveSubId(sub.id)
                                        }
                                    },
                                    label = { Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    shape = ExpressiveChipShape
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 16.dp,
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
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            },
            bottomBar = {
                if (isMultiSelectMode) {
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                        color = androidx.compose.ui.graphics.lerp(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.colorScheme.error,
                            0.05f
                        ).copy(alpha = 0.95f),
                        tonalElevation = 6.dp,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isMultiSelectMode = false
                                        selectedNodes = emptySet()
                                    },
                                    modifier = Modifier.pressScaleEffect()
                                ) {
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val allFilteredManuals = remember(filteredServerList, manualServersStr) {
                                    filteredServerList.filter { item ->
                                        manualServersStr.split("\n").map { it.trim() }.contains(item.link.trim())
                                    }.map { it.link }.toSet()
                                }
                                val isAllSelected = selectedNodes.containsAll(allFilteredManuals) && allFilteredManuals.isNotEmpty()
                                TextButton(
                                    onClick = {
                                        if (isAllSelected) {
                                            val nextSelection = selectedNodes - allFilteredManuals
                                            selectedNodes = nextSelection
                                            if (nextSelection.isEmpty()) {
                                                isMultiSelectMode = false
                                            }
                                        } else {
                                            selectedNodes = selectedNodes + allFilteredManuals
                                        }
                                    },
                                    modifier = Modifier.pressScaleEffect()
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
                                    },
                                    modifier = Modifier.pressScaleEffect()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Selected",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (filteredServerList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_matching_nodes),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(filteredServerList, key = { _, item -> item.id }) { index, serverItem ->
                            val serverLink = serverItem.link
                            val isSelected = activeProfile == serverLink
                            val name = serverItem.name
                            val type = serverItem.type
                            val transport = serverItem.transport

                            val tagContainerColor = when (type) {
                                "VLESS" -> MaterialTheme.colorScheme.primaryContainer
                                "TROJAN" -> MaterialTheme.colorScheme.secondaryContainer
                                "VMESS" -> MaterialTheme.colorScheme.tertiaryContainer
                                "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.errorContainer
                                "TUIC" -> MaterialTheme.colorScheme.primaryContainer
                                "CHAIN" -> MaterialTheme.colorScheme.tertiaryContainer
                                "OPENVPN", "OVPN" -> MaterialTheme.colorScheme.secondaryContainer
                                "AMNEZIAWG", "AWG", "WIREGUARD" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val tagTextColor = when (type) {
                                "VLESS" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "TROJAN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "VMESS" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "HYSTERIA", "HYSTERIA2", "HY2" -> MaterialTheme.colorScheme.onErrorContainer
                                "TUIC" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "CHAIN" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "OPENVPN", "OVPN" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "AMNEZIAWG", "AWG", "WIREGUARD" -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val isManualNode = remember(manualServersStr, serverLink) {
                                manualServersStr.split("\n").map { it.trim() }.contains(serverLink.trim())
                            }

                            Row(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(ExpressiveButtonShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else Color.Transparent,
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
                                                        startVpnService(context)
                                                    }
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
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
                                            .size(36.dp)
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
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(ExpressiveChipShape)
                                                .background(tagContainerColor)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = type,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = tagTextColor,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (!transport.isNullOrEmpty()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(ExpressiveChipShape)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = transport.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val ping = pingsMap[serverLink]
                                    val isTimeout = ping != null && ping < 0
                                    if (ping != null) {
                                        val pingColor = if (isTimeout) Color.Red else if (ping < 150) Color.Green else Color.Yellow
                                        Box(
                                            modifier = Modifier
                                                .background(pingColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = if (isTimeout) "Timeout" else "${ping}ms",
                                                color = pingColor,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (isManualNode && !isMultiSelectMode) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    val currentManualList = manualServersStr.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                                                    val updatedManualList = currentManualList.filter { it != serverLink }.distinct()
                                                    settingsManager.setManualServers(updatedManualList.joinToString("\n"))

                                                    if (serverLink.startsWith("chain://")) {
                                                        val chainId = serverLink.substringAfter("chain://").substringBefore("#")
                                                        val currentChains = deserializeProxyChains(settings.proxyChains)
                                                        val updatedChains = currentChains.filter { it.id != chainId }
                                                        settingsManager.setProxyChains(serializeProxyChains(updatedChains))
                                                    }
                                                    
                                                    val currentCam = deserializeCamouflageSettings(settings.camouflageSettings)
                                                    val updatedCam = currentCam.filter { it.nodeLink.substringBefore("#") != serverLink.substringBefore("#") }
                                                    settingsManager.setCamouflageSettings(serializeCamouflageSettings(updatedCam))

                                                    if (isSelected) {
                                                        val nextActive = updatedManualList.firstOrNull() ?: ""
                                                        settingsManager.setActiveProfile(nextActive)
                                                        if (vpnState == "CONNECTED" && nextActive.isNotEmpty()) {
                                                            startVpnService(context)
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.delete_config),
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
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

    if (isRegisteringWarp) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text(stringResource(R.string.registering_warp)) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator()
                }
            },
            shape = ExpressiveCardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}
}
