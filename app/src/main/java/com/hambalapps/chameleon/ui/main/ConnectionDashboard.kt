package com.hambalapps.chameleon.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.hambalapps.chameleon.vpn.ConfigInjector
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.chameleon.vpn.CdnIpScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.Config
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.hambalapps.chameleon.vpn.ProxyNameResolver
import com.hambalapps.chameleon.data.SettingsManager
import com.hambalapps.chameleon.data.Subscription
import com.hambalapps.chameleon.vpn.VpnServiceWrapper
import com.hambalapps.chameleon.vpn.registerWarpAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun startVpnService(context: Context) {
    val settingsManager = SettingsManager(context.applicationContext)
    CoroutineScope(Dispatchers.IO).launch {
        val currentSettings = settingsManager.settings.first()
        withContext(Dispatchers.Main) {
            val intent = Intent(context, VpnServiceWrapper::class.java).apply {
                action = VpnServiceWrapper.ACTION_START
                putExtra("active_profile", currentSettings.activeProfile)
                putExtra("show_live_notification", currentSettings.showLiveNotification)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

internal fun stopVpnService(context: Context) {
    val intent = Intent(context, VpnServiceWrapper::class.java).apply {
        action = VpnServiceWrapper.ACTION_STOP
    }
    context.startService(intent)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectionDashboard(
    state: String,
    cardStyle: String,
    isDark: Boolean,
    delayTestUrl: String,
    activeProfile: String,
    activeSubId: String,
    subscriptions: List<Subscription>,
    vpnMode: String,
    vpnModeTunnelGames: Boolean,
    settingsManager: SettingsManager,
    scope: CoroutineScope,
    onConnectToggle: () -> Unit,
    onNavigateToServers: () -> Unit,
    onNavigateToCdnFronting: (() -> Unit)? = null,
    isEditMode: Boolean = false,
    sessionDownBytesProvider: () -> Long = { 0L },
    sessionUpBytesProvider: () -> Long = { 0L },
    activeCountryCode: String? = null
) {
    val context = LocalContext.current
    val standardColorScheme = MaterialTheme.colorScheme
    val transition = updateTransition(targetState = state, label = "VPNStateTransition")

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

    val stateText = if (Config.IS_SPECIAL) {
        when (state) {
            "CONNECTED" -> "Meow 🐾"
            "CONNECTING" -> "CONNECTING TO YOUR HEART... 💓"
            "DISCONNECTING" -> "DISCONNECTING... 💔"
            else -> "OFFLINE, BUT THINKING OF YOU 💔"
        }
    } else {
        when (state) {
            "CONNECTED" -> "SECURED"
            "CONNECTING" -> "CONNECTING..."
            "DISCONNECTING" -> "DISCONNECTING..."
            else -> "UNPROTECTED"
        }
    }

    val isVpnActive = state == "CONNECTED" || state == "CONNECTING"
    
    val containerColor = if (isVpnActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    
    val contentColor = if (isVpnActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val buttonColor by transition.animateColor(label = "ButtonColor") { s ->
        if (s == "CONNECTED" || s == "CONNECTING") Color.White else MaterialTheme.colorScheme.primary
    }

    val buttonIconColor by transition.animateColor(label = "ButtonIconColor") { s ->
        if (s == "CONNECTED" || s == "CONNECTING") MaterialTheme.colorScheme.primary else Color.White
    }

    val pulseScaleState = remember { androidx.compose.animation.core.Animatable(1.0f) }
    val isPulseActive = (state == "CONNECTING" || state == "DISCONNECTING") && isActivityResumed
    LaunchedEffect(isPulseActive) {
        if (isPulseActive) {
            pulseScaleState.animateTo(
                targetValue = 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(1200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseScaleState.snapTo(1.0f)
        }
    }
    val pulseScale = pulseScaleState.value

    val scaleFactor by transition.animateFloat(
        label = "ButtonScale",
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) }
    ) { s ->
        if (s == "CONNECTED") 1.05f else 1.0f
    }

    val finalScale = if (state == "CONNECTING") pulseScale else scaleFactor

    var userIpAddress by remember { mutableStateOf("Detecting...") }
    var resolvedIpCountryCode by remember { mutableStateOf<String?>(null) }
    
    val ipFlagEmoji = remember(resolvedIpCountryCode) {
        if (!resolvedIpCountryCode.isNullOrEmpty() && resolvedIpCountryCode != "🌐") {
            getFlagEmojiFromCountryCode(resolvedIpCountryCode!!)
        } else {
            "🌐"
        }
    }

    val fontScale = LocalDensity.current.fontScale
    val ipTextSize = remember(userIpAddress, fontScale) {
        val base = when {
            userIpAddress.length > 25 -> 10.sp
            userIpAddress.length > 18 -> 11.sp
            userIpAddress.length > 14 -> 12.sp
            else -> 14.sp
        }
        if (fontScale > 1.0f) {
            (base.value / fontScale).sp
        } else {
            base
        }
    }

    LaunchedEffect(state) {
        if (state == "CONNECTING" || state == "DISCONNECTING") {
            userIpAddress = "Detecting..."
            resolvedIpCountryCode = null
        } else {
            userIpAddress = "Detecting..."
            resolvedIpCountryCode = null
            launch(Dispatchers.IO) {
                var ip = "Unknown"
                var attempts = 3
                while (attempts > 0) {
                    var connection: java.net.HttpURLConnection? = null
                    try {
                        val url = java.net.URL("https://api.ipify.org")
                        connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.requestMethod = "GET"
                        connection.useCaches = false
                        val responseCode = connection.responseCode
                        if (responseCode == 200) {
                            ip = connection.inputStream.bufferedReader().use { it.readText().trim() }
                            break
                        }
                    } catch (e: Exception) {
                        attempts--
                        delay(1000)
                    } finally {
                        connection?.disconnect()
                    }
                }
                var cc: String? = null
                if (ip != "Unknown" && ip != "Detecting...") {
                    cc = IpCountryResolver.resolveCountryCode(ip)
                }
                withContext(Dispatchers.Main) {
                    userIpAddress = ip
                    resolvedIpCountryCode = cc
                }
            }
        }
    }

    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }
    val activeSubName = activeSubscription?.name ?: "Manual"
    val serverName = if (activeProfile.isEmpty()) {
        stringResource(R.string.no_profile_active)
    } else if (activeProfile.startsWith("{")) {
        stringResource(R.string.custom_json)
    } else {
        ProxyNameResolver.getProxyName(activeProfile, context)
    }
    val protocolName = if (activeProfile.isEmpty()) {
        ""
    } else if (activeProfile.startsWith("{")) {
        "JSON"
    } else {
        activeProfile.substringBefore("://").uppercase()
    }
    val flagEmoji = remember(serverName, activeCountryCode) { getFlagEmoji(serverName, activeCountryCode) }

    // Read settings fields for WARP configuration
    val settingsState = settingsManager.settings.collectAsState(initial = null).value
    val warpDetourMode = settingsState?.warpDetourMode ?: "proxy"
    val warpPort = settingsState?.warpPort ?: "2408"
    val enableMtProxy = settingsState?.enableMtProxy ?: false
    val mtProxyPort = settingsState?.mtProxyPort ?: "19999"
    val mtProxySecret = settingsState?.mtProxySecret ?: "dd000102030405060708090a0b0c0d0e0f"
    var isRegisteringWarp by remember { mutableStateOf(false) }

    // Recompute bento card brushes to exactly match Settings bento cards styling
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer
    val tertiaryContainer = MaterialTheme.colorScheme.tertiaryContainer
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow

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
                        Color.White.copy(alpha = 0.60f),
                        Color.Black.copy(alpha = 0.10f)
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

    val primaryCardBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, surfaceContainerHigh, primaryContainer, secondaryContainer) {
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
                        Color.White.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.15f)
                    )
                }
                Brush.linearGradient(colors = colors)
            }
            else -> SolidColor(surfaceContainerHigh)
        }
    }

    val secondaryCardBrush = remember(isDark, cardStyle, secondaryColor, tertiaryColor, surfaceContainer, secondaryContainer, tertiaryContainer) {
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
                        Color.White.copy(alpha = 0.50f),
                        Color.White.copy(alpha = 0.15f)
                    )
                }
                Brush.linearGradient(colors = colors)
            }
            else -> SolidColor(surfaceContainer)
        }
    }

    // Determine layout based on orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    @Composable
    fun ConnectCard(paddingVertical: Int = 32) {
        ExpressiveCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onConnectToggle() },
            brush = if (isVpnActive) primaryCardBrush else secondaryCardBrush,
            shape = RoundedCornerShape(32.dp),
            borderBrush = cardBorderBrush,
            cardStyle = cardStyle
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = paddingVertical.dp, horizontal = 16.dp)
            ) {
                val buttonCornerRadius by animateDpAsState(
                    targetValue = if (state == "CONNECTED") 32.dp else 58.dp,
                    animationSpec = spring(
                        dampingRatio = 0.55f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "ButtonShapeMorph"
                )
                val buttonShape = RoundedCornerShape(buttonCornerRadius)

                val auraScale by animateFloatAsState(
                    targetValue = if (state == "CONNECTED") 1.25f else 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "AuraScale"
                )
                val auraAlpha by animateFloatAsState(
                    targetValue = if (state == "CONNECTED") 0.25f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "AuraAlpha"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    if (isActivityResumed && (state == "CONNECTED" || state == "CONNECTING")) {
                        WaveVisualizer(
                            state = state,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(180.dp)
                        )
                    }

                    if (isVpnActive) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .graphicsLayer {
                                    scaleX = finalScale * pulseScale
                                    scaleY = finalScale * pulseScale
                                    alpha = 0.15f
                                }
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        )
                    }

                    // Ambient glowing aura behind the button
                    Box(
                        modifier = Modifier
                            .size(116.dp)
                            .graphicsLayer {
                                scaleX = auraScale
                                scaleY = auraScale
                                alpha = auraAlpha
                            }
                            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                            .blur(24.dp)
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(116.dp)
                            .graphicsLayer {
                                scaleX = finalScale
                                scaleY = finalScale
                            }
                            .pressScaleEffect()
                            .clip(buttonShape)
                            .background(buttonColor)
                            .border(
                                width = 4.dp,
                                color = if (state == "CONNECTED" || state == "CONNECTING") {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                },
                                shape = buttonShape
                            )
                    ) {
                        if (state == "CONNECTING") {
                            androidx.compose.material3.LoadingIndicator(
                                modifier = Modifier.size(56.dp),
                                color = buttonIconColor
                            )
                        } else {
                            Icon(
                                imageVector = if (state == "CONNECTED") Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                                contentDescription = stringResource(R.string.connect_toggle),
                                tint = buttonIconColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = stateText,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    },
                    label = "StateTextTransition"
                ) { targetText ->
                    Text(
                        text = targetText,
                        color = contentColor,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 2.sp
                    )
                }

                if (state == "CONNECTED") {
                    Spacer(modifier = Modifier.height(4.dp))
                    val durationVal = remember { mutableStateOf("00:00:00") }
                    val serviceManager = VpnServiceWrapper.vpnState
                    LaunchedEffect(state) {
                        val startTime = System.currentTimeMillis()
                        while (serviceManager.value == "CONNECTED") {
                            val elapsed = System.currentTimeMillis() - startTime
                            val sec = (elapsed / 1000) % 60
                            val min = (elapsed / (1000 * 60)) % 60
                            val hr = elapsed / (1000 * 60 * 60)
                            durationVal.value = String.format(java.util.Locale.US, "%02d:%02d:%02d", hr, min, sec)
                            delay(1000)
                        }
                    }
                    Text(
                        text = durationVal.value,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = contentColor.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }

    @Composable
    fun ServerCard() {
        ExpressiveCard(
            modifier = Modifier
                .fillMaxWidth()
                .pressScaleEffect()
                .clickable { onNavigateToServers() },
            brush = secondaryCardBrush,
            shape = ExpressiveCardShape,
            borderBrush = cardBorderBrush,
            cardStyle = cardStyle
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = activeSubName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = serverName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Select Server",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
        }
    }

    @Composable
    fun PingProtocolRow() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                brush = secondaryCardBrush,
                shape = ExpressiveCardShape,
                borderBrush = cardBorderBrush,
                cardStyle = cardStyle
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawLine(
                                color = outlineVariantColor,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Top-Left: Download
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(top = 10.dp, start = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Down",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = formatBytes(sessionDownBytesProvider()),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Bottom-Right: Upload
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 10.dp, end = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = formatBytes(sessionUpBytesProvider()),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Up",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
            }

            ExpressiveCard(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                brush = secondaryCardBrush,
                shape = ExpressiveCardShape,
                borderBrush = cardBorderBrush,
                cardStyle = cardStyle
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "IP Address",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            if (ipFlagEmoji != "🌐") {
                                if (ipFlagEmoji == "🇮🇷") {
                                    Image(
                                        painter = painterResource(id = com.hambalapps.chameleon.R.drawable.ic_lion_sun_flag),
                                        contentDescription = "Iran Flag",
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Text(
                                        text = ipFlagEmoji,
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }
                            }
                        }
                        Column {
                            Text(
                                text = "IP ADDRESS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userIpAddress,
                                fontSize = ipTextSize,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit.Unspecified),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
            }
        }
    }

    @Composable
    fun BypassCard() {
        ExpressiveCard(
            modifier = Modifier
                .fillMaxWidth(),
            brush = secondaryCardBrush,
            shape = ExpressiveCardShape,
            borderBrush = cardBorderBrush,
            cardStyle = cardStyle
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "WARP",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "WARP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Allows bypassing AI and streaming services limitations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = vpnMode == "ai_bypass",
                            onCheckedChange = { checked ->
                                scope.launch {
                                    if (checked) {
                                        val privateKey = settingsManager.settings.first().warpPrivateKey
                                        val clientId = settingsManager.settings.first().warpClientId
                                        if (privateKey.isEmpty() || clientId.isEmpty()) {
                                            isRegisteringWarp = true
                                            val creds = registerWarpAccount()
                                            isRegisteringWarp = false
                                            if (creds != null) {
                                                settingsManager.setWarpCredentials(creds.privateKey, creds.publicKey, creds.ipAddress, creds.clientId)
                                                settingsManager.setVpnMode("ai_bypass")
                                                if (state == "CONNECTED") {
                                                    startVpnService(context)
                                                }
                                            }
                                        } else {
                                            settingsManager.setVpnMode("ai_bypass")
                                            if (state == "CONNECTED") {
                                                startVpnService(context)
                                            }
                                        }
                                    } else {
                                        settingsManager.setVpnMode("global")
                                        if (state == "CONNECTED") {
                                            startVpnService(context)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = vpnMode == "ai_bypass",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(R.string.warp_detour_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.warp_detour_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("proxy" to "Proxy", "direct" to "Direct").forEach { (optionKey, optionName) ->
                                    val isSelected = warpDetourMode == optionKey
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch {
                                                settingsManager.setWarpDetourMode(optionKey)
                                                if (state == "CONNECTED") {
                                                    startVpnService(context)
                                                }
                                            }
                                        },
                                        label = { Text(optionName) },
                                        modifier = Modifier.weight(1f),
                                        shape = ExpressiveButtonShape
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(R.string.warp_port_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.warp_port_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("2408", "500", "1701", "4500").forEach { portStr ->
                                    val isSelected = warpPort == portStr
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scope.launch {
                                                settingsManager.setWarpPort(portStr)
                                                if (state == "CONNECTED") {
                                                    startVpnService(context)
                                                }
                                            }
                                        },
                                        label = { Text(portStr) },
                                        modifier = Modifier.weight(1f),
                                        shape = ExpressiveButtonShape
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    @Composable
    fun GamingModeCard() {
        ExpressiveCard(
            modifier = Modifier
                .fillMaxWidth(),
            brush = secondaryCardBrush,
            shape = ExpressiveCardShape,
            borderBrush = cardBorderBrush,
            cardStyle = cardStyle
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = "Routing Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Routing Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select active VPN tunnel routing style",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ConnectedButtonGroup(
                        selectedIndex = if (vpnMode == "gaming") 1 else 0,
                        options = listOf("Standard Mode", "Gaming Mode"),
                        containerColor = standardColorScheme.primaryContainer.copy(alpha = 0.5f),
                        indicatorColor = standardColorScheme.primary,
                        selectedTextColor = standardColorScheme.onPrimary,
                        unselectedTextColor = standardColorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        onSelect = { index ->
                            scope.launch {
                                val targetMode = if (index == 1) "gaming" else "standard"
                                settingsManager.setVpnMode(targetMode)
                                if (state == "CONNECTED") {
                                    startVpnService(context)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = vpnMode == "gaming",
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.tunnel_games_title),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.tunnel_games_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Switch(
                                    checked = vpnModeTunnelGames,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            settingsManager.setVpnModeTunnelGames(checked)
                                            if (state == "CONNECTED") {
                                                startVpnService(context)
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

    @Composable
    fun TelegramProxyCard() {
        ExpressiveCard(
            modifier = Modifier
                .fillMaxWidth(),
            brush = secondaryCardBrush,
            shape = ExpressiveCardShape,
            borderBrush = cardBorderBrush,
            cardStyle = cardStyle
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Telegram Proxy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Telegram Proxy",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Run local Telegram MTProxy server",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = enableMtProxy,
                            onCheckedChange = { checked ->
                                scope.launch {
                                    settingsManager.setEnableMtProxy(checked)
                                    settingsManager.enableMtProxy.first { it == checked }
                                    
                                    if (state == "CONNECTED") {
                                        startVpnService(context)
                                    } else {
                                        if (checked) {
                                            val intent = Intent(context, VpnServiceWrapper::class.java).apply {
                                                action = VpnServiceWrapper.ACTION_START_PROXY
                                            }
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                        } else {
                                            val intent = Intent(context, VpnServiceWrapper::class.java).apply {
                                                action = VpnServiceWrapper.ACTION_STOP
                                                putExtra("force_stop", true)
                                            }
                                            context.startService(intent)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = enableMtProxy,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Proxy Port",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Port for MTProxy (1024-65535)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                var portText by remember(mtProxyPort) { mutableStateOf(mtProxyPort) }
                                OutlinedTextField(
                                    value = portText,
                                    onValueChange = {
                                        portText = it
                                        if (it.toIntOrNull() in 1024..65535) {
                                            scope.launch {
                                                settingsManager.setMtProxyPort(it)
                                                if (state == "CONNECTED") {
                                                    startVpnService(context)
                                                } else {
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
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(90.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Secret Key",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "MTProto client obfuscated secret",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                var secretText by remember(mtProxySecret) { mutableStateOf(mtProxySecret) }
                                OutlinedTextField(
                                    value = secretText,
                                    onValueChange = {
                                        secretText = it
                                        val isLengthOk = it.length == 34 || it.length == 32
                                        val startsWithValid = it.startsWith("dd", ignoreCase = true) || 
                                                              it.startsWith("ee", ignoreCase = true) || 
                                                              (it.length == 32 && !it.startsWith("dd", ignoreCase = true) && !it.startsWith("ee", ignoreCase = true))
                                        if (isLengthOk && startsWithValid) {
                                            scope.launch {
                                                settingsManager.setMtProxySecret(it)
                                                if (state == "CONNECTED") {
                                                    startVpnService(context)
                                                } else {
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
                                    },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(180.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = {
                                    val normalizedSecret = ConfigInjector.normalizeMtProxySecret(mtProxySecret)
                                    val link = "https://t.me/proxy?server=127.0.0.1&port=${mtProxyPort}&secret=${normalizedSecret}"
                                    val sendIntent: Intent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, link)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = ExpressiveButtonShape
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect / Share Telegram Proxy")
                            }
                        }
                    }
            }
        }
    }

    @Composable
    fun LoveNotesCard() {
        var showLoveNoteDialog by remember { mutableStateOf(false) }
        var currentLoveNote by remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        currentLoveNote = Config.LOVE_QUOTES.random()
                        showLoveNoteDialog = true
                    }
                    .pressScaleEffect(),
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.25f else 0.40f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.love_notes),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.love_notes_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Peaking Kitty peeking from the top-right of the card itself
            PeakingKitty(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-32).dp, y = (-20).dp)
            )
        }

        if (showLoveNoteDialog) {
            AlertDialog(
                onDismissRequest = { showLoveNoteDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.note_for_sana), fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text(
                        text = currentLoveNote,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showLoveNoteDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                shape = ExpressiveCardShape,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }

    val activeCardIds by settingsManager.dashboardCards.collectAsState(initial = listOf("connect_button", "selected_server", "traffic", "current_ip"))
    var showAddCardSheet by remember { mutableStateOf(false) }

    fun moveCard(index: Int, direction: Int) {
        val nextIndex = index + direction
        if (nextIndex in activeCardIds.indices) {
            val mutable = activeCardIds.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(nextIndex, item)
            scope.launch { settingsManager.setDashboardCards(mutable) }
        }
    }

    fun getCardTitle(cardId: String): String = when (cardId) {
        "connect_button" -> "Main Connection Button"
        "selected_server" -> "Active Server Node"
        "traffic" -> "Down / Up Live Traffic"
        "current_ip" -> "IP Address & Location"
        "cdn_fronting" -> "CDN Fronting & Clean IP"
        "live_logs" -> "Live Engine Logs Stream"
        "mode_selector" -> "VPN Routing Mode"
        "warp_status" -> "WARP Detour Bypass"
        "telegram_proxy" -> "Telegram MTProxy Server"
        else -> cardId
    }

    @Composable
    fun RenderCardById(cardId: String) {
        when (cardId) {
            "connect_button" -> ConnectCard(paddingVertical = 24)
            "selected_server" -> ServerCard()
            "traffic" -> PingProtocolRow()
            "cdn_fronting" -> CdnFrontingDashboardCard()
            "live_logs" -> LiveLogsDashboardCard()
            "mode_selector" -> GamingModeCard()
            "warp_status" -> BypassCard()
            "telegram_proxy" -> TelegramProxyCard()
            else -> {}
        }
    }

    @Composable
    fun DashboardCardWrapper(
        cardId: String,
        index: Int,
        content: @Composable () -> Unit
    ) {
        if (!isEditMode) {
            content()
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), ExpressiveCardShape),
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = getCardTitle(cardId),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { moveCard(index, -1) }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (index > 0) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            IconButton(onClick = { moveCard(index, 1) }, enabled = index < activeCardIds.size - 1, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (index < activeCardIds.size - 1) MaterialTheme.colorScheme.primary else Color.Gray)
                            }
                            IconButton(onClick = { scope.launch { settingsManager.setDashboardCards(activeCardIds - cardId) } }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove Card", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Box(modifier = Modifier.padding(8.dp)) {
                        content()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isEditMode) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ExpressiveCardShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Customize Dashboard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Reorder cards or add new widgets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showAddCardSheet = true },
                        shape = ExpressivePillShape
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Card")
                    }
                }
            }
        }

        activeCardIds.forEachIndexed { index, cardId ->
            DashboardCardWrapper(
                cardId = cardId,
                index = index,
                onMoveUp = { moveCard(index, -1) },
                onMoveDown = { moveCard(index, 1) },
                onRemove = { scope.launch { settingsManager.setDashboardCards(activeCardIds - cardId) } }
            ) {
                RenderCardById(cardId)
            }
        }

        if (Config.IS_SPECIAL && !activeCardIds.contains("love_notes")) {
            LoveNotesCard()
        }
    }

    if (showAddCardSheet) {
        val allAvailableCards = listOf(
            "connect_button" to ("Main Connection Button" to "Primary VPN connect button and wave visualizer"),
            "selected_server" to ("Active Server Node" to "Current server flag, name, and 1-tap picker"),
            "traffic" to ("Down / Up Live Traffic" to "Session download and upload bytes counter"),
            "cdn_fronting" to ("CDN Fronting & Clean IP" to "Quick toggle and clean IP status card"),
            "live_logs" to ("Live Engine Stream" to "Real-time sing-box terminal log console"),
            "mode_selector" to ("VPN Routing Mode" to "Standard vs Gaming routing mode chips"),
            "warp_status" to ("WARP Detour Bypass" to "Cloudflare WARP account and detour options"),
            "telegram_proxy" to ("Telegram MTProxy Server" to "Local MTProto proxy server toggle")
        )
        val inactiveCards = allAvailableCards.filter { !activeCardIds.contains(it.first) }

        ModalBottomSheet(
            onDismissRequest = { showAddCardSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add Card to Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (inactiveCards.isEmpty()) {
                    Text("All available cards are already active on your dashboard!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    inactiveCards.forEach { (cardId, info) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        settingsManager.setDashboardCards(activeCardIds + cardId)
                                        showAddCardSheet = false
                                    }
                                },
                            shape = ExpressiveCardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(info.first, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(info.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.Add, contentDescription = "Add Card", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun ConnectedButtonGroup(
    selectedIndex: Int,
    options: List<String>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    selectedTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
) {
    BoxWithConstraints(
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(1.dp, indicatorColor.copy(alpha = 0.25f), CircleShape)
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val count = options.size
        if (count > 0) {
            val itemWidth = totalWidth / count
            val targetOffset = itemWidth * selectedIndex
            
            val animatedOffset by animateDpAsState(
                targetValue = targetOffset,
                animationSpec = spring(
                    dampingRatio = 0.65f, // moderate bounce (expressive spatial)
                    stiffness = Spring.StiffnessLow
                ),
                label = "indicatorOffset"
            )
            
            // Sliding background capsule
            Box(
                modifier = Modifier
                    .offset(x = animatedOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
            
            // Buttons Row
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                options.forEachIndexed { index, text ->
                    val isSelected = index == selectedIndex
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy, // expressive effects (no overshoot)
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "textColor"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable { onSelect(index) }
                            .pressScaleEffect(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

