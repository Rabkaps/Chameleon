package com.hambalapps.chameleon.ui.main

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.R
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
    activeCountryCode: String? = null
) {
    val context = LocalContext.current
    val transition = updateTransition(targetState = state, label = "VPNStateTransition")

    val stateText = when (state) {
        "CONNECTED" -> "SECURED"
        "CONNECTING" -> "CONNECTING..."
        "DISCONNECTING" -> "DISCONNECTING..."
        else -> "UNPROTECTED"
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
    val isPulseActive = state == "CONNECTING" || state == "DISCONNECTING"
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

    var pingTime by remember { mutableStateOf("--") }
    var userIpAddress by remember { mutableStateOf("Detecting...") }
    
    LaunchedEffect(state, delayTestUrl) {
        if (state == "CONNECTED") {
            launch(Dispatchers.IO) {
                while (true) {
                    val startTime = System.currentTimeMillis()
                    var connection: java.net.HttpURLConnection? = null
                    val ping = try {
                        val url = java.net.URL(delayTestUrl)
                        connection = url.openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 3000
                        connection.readTimeout = 3000
                        connection.requestMethod = "GET"
                        connection.useCaches = false
                        connection.instanceFollowRedirects = false
                        val responseCode = connection.responseCode
                        val elapsed = System.currentTimeMillis() - startTime
                        "${elapsed}ms"
                    } catch (e: Exception) {
                        "Timeout"
                    } finally {
                        connection?.disconnect()
                    }
                    
                    withContext(Dispatchers.Main) {
                        pingTime = ping
                    }
                    delay(10000)
                }
            }
        } else {
            pingTime = "--"
        }
    }

    LaunchedEffect(state) {
        if (state == "CONNECTING" || state == "DISCONNECTING") {
            userIpAddress = "Detecting..."
        } else {
            userIpAddress = "Detecting..."
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
                withContext(Dispatchers.Main) {
                    userIpAddress = ip
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
        if (cardStyle == "solid") {
            SolidColor(outlineVariant)
        } else if (cardStyle == "vibrant") {
            val colors = listOf(
                primaryColor.copy(alpha = if (isDark) 0.8f else 0.4f),
                secondaryColor.copy(alpha = if (isDark) 0.6f else 0.2f)
            )
            Brush.linearGradient(colors = colors)
        } else {
            val colors = listOf(
                primaryColor.copy(alpha = if (isDark) 0.60f else 0.18f),
                secondaryColor.copy(alpha = if (isDark) 0.40f else 0.06f)
            )
            Brush.linearGradient(colors = colors)
        }
    }

    val primaryCardBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, surfaceContainerHigh, primaryContainer, secondaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(primaryContainer)
        } else if (cardStyle == "vibrant") {
            val colors = listOf(
                primaryContainer,
                secondaryContainer.copy(alpha = 0.7f)
            )
            Brush.linearGradient(colors = colors)
        } else {
            val colors = if (isDark) {
                listOf(
                    primaryColor.copy(alpha = 0.55f),
                    secondaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    primaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    val secondaryCardBrush = remember(isDark, cardStyle, secondaryColor, tertiaryColor, surfaceContainer, secondaryContainer, tertiaryContainer) {
        if (cardStyle == "solid") {
            SolidColor(secondaryContainer)
        } else if (cardStyle == "vibrant") {
            val colors = listOf(
                secondaryContainer,
                tertiaryContainer.copy(alpha = 0.7f)
            )
            Brush.linearGradient(colors = colors)
        } else {
            val colors = if (isDark) {
                listOf(
                    secondaryColor.copy(alpha = 0.55f),
                    tertiaryColor.copy(alpha = 0.28f)
                )
            } else {
                listOf(
                    secondaryColor.copy(alpha = 0.18f),
                    surfaceContainerHigh
                )
            }
            Brush.linearGradient(colors = colors)
        }
    }

    // Determine layout based on orientation
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    @Composable
    fun ConnectCard(paddingVertical: Int = 32) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = if (isVpnActive) primaryCardBrush else secondaryCardBrush, shape = RoundedCornerShape(32.dp))
                .border(
                    width = 1.dp,
                    brush = cardBorderBrush,
                    shape = RoundedCornerShape(32.dp)
                )
                .clickable { onConnectToggle() },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = paddingVertical.dp, horizontal = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    if (state == "CONNECTED" || state == "CONNECTING") {
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

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(116.dp)
                            .graphicsLayer {
                                scaleX = finalScale
                                scaleY = finalScale
                            }
                            .pressScaleEffect()
                            .clip(CircleShape)
                            .background(buttonColor)
                            .border(
                                width = 4.dp,
                                color = if (state == "CONNECTED" || state == "CONNECTING") {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                },
                                shape = CircleShape
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
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                .clickable { onNavigateToServers() }
                .pressScaleEffect(),
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            VibrantCardContent(cardStyle) {
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
    }

    @Composable
    fun PingProtocolRow() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                VibrantCardContent(cardStyle) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .height(86.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Ping",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "PING",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = pingTime,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                VibrantCardContent(cardStyle) {
                    Column(
                        modifier = Modifier
                            .padding(18.dp)
                            .height(86.dp),
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
                                modifier = Modifier.size(20.dp)
                            )
                            if (state == "CONNECTED" && flagEmoji != "🌐") {
                                Text(
                                    text = flagEmoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "IP ADDRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = userIpAddress,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BypassCard() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            VibrantCardContent(cardStyle) {
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
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = "AI-Bypass",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "AI-Bypass",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Smart routing active",
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
    }

    @Composable
    fun GamingModeCard() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            VibrantCardContent(cardStyle) {
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
                                contentDescription = "Gaming Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Gaming Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Lowest latency routing",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = vpnMode == "gaming",
                            onCheckedChange = { checked ->
                                scope.launch {
                                    if (checked) {
                                        settingsManager.setVpnMode("gaming")
                                    } else {
                                        settingsManager.setVpnMode("standard")
                                    }
                                    if (state == "CONNECTED") {
                                        startVpnService(context)
                                    }
                                }
                            }
                        )
                    }

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
    }

    @Composable
    fun TelegramProxyCard() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape),
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            VibrantCardContent(cardStyle) {
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
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(0.9f)) {
                ConnectCard(paddingVertical = 16)
            }
            Column(
                modifier = Modifier.weight(1.1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ServerCard()
                PingProtocolRow()
                BypassCard()
                GamingModeCard()
                TelegramProxyCard()
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ConnectCard(paddingVertical = 32)
            ServerCard()
            PingProtocolRow()
            BypassCard()
            GamingModeCard()
            TelegramProxyCard()
        }
    }
}
