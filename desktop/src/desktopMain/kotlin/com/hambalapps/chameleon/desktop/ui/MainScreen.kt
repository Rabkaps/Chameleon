@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class
)

package com.hambalapps.chameleon.desktop.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.data.*
import com.hambalapps.chameleon.desktop.vpn.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

private val ExpressiveCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
private val ExpressiveChipShape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)

@Composable
fun VibrantCardContent(
    cardStyle: String,
    content: @Composable () -> Unit
) {
    if (cardStyle == "vibrant") {
        val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
        val cardBackground = MaterialTheme.colorScheme.primaryContainer
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme.copy(
                primary = onPrimaryContainer,
                onPrimary = cardBackground,
                primaryContainer = onPrimaryContainer.copy(alpha = 0.20f),
                onPrimaryContainer = onPrimaryContainer,
                
                secondary = onPrimaryContainer,
                onSecondary = cardBackground,
                secondaryContainer = onPrimaryContainer,
                onSecondaryContainer = cardBackground,
                
                tertiary = onPrimaryContainer,
                onTertiary = cardBackground,
                tertiaryContainer = onPrimaryContainer.copy(alpha = 0.20f),
                onTertiaryContainer = onPrimaryContainer,
                
                surface = cardBackground,
                onSurface = onPrimaryContainer,
                onSurfaceVariant = onPrimaryContainer.copy(alpha = 0.80f),
                surfaceVariant = onPrimaryContainer.copy(alpha = 0.15f),
                
                outline = onPrimaryContainer.copy(alpha = 0.45f),
                outlineVariant = onPrimaryContainer.copy(alpha = 0.25f),
                onError = Color.White,
                
                surfaceContainerLowest = onPrimaryContainer.copy(alpha = 0.05f),
                surfaceContainerLow = onPrimaryContainer.copy(alpha = 0.10f),
                surfaceContainer = onPrimaryContainer.copy(alpha = 0.15f),
                surfaceContainerHigh = onPrimaryContainer.copy(alpha = 0.20f),
                surfaceContainerHighest = onPrimaryContainer.copy(alpha = 0.25f)
            )
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                content = content
            )
        }
    } else {
        content()
    }
}

object DesktopStrings {
    private val fa = mapOf(
        "app_name" to "Chameleon",
        "sub_manager" to "مدیریت اشتراک",
        "connected" to "متصل شد",
        "connecting" to "در حال اتصال...",
        "disconnected" to "قطع شد",
        "disconnecting" to "در حال قطع اتصال...",
        "active_node" to "سرور فعال",
        "dashboard" to "داشبورد",
        "profiles" to "پروفایل‌ها",
        "add_config" to "افزودن پیکربندی",
        "logs" to "لاگ‌ها",
        "settings" to "تنظیمات",
        "bypass_iran" to "دور زدن سایت‌های ایرانی (Direct Iran)",
        "bypass_lan" to "دور زدن شبکه محلی (Bypass LAN)",
        "secure_dns" to "دی‌ان‌اس امن (Secure DNS)",
        "theme" to "تم رنگی برنامه",
        "dark_mode" to "حالت تاریک",
        "advanced_mode" to "تنظیمات پیشرفته (Fragmentation & Mux)",
        "import_profile" to "وارد کردن لینک سرور (پکیج)",
        "import_sub" to "وارد کردن لینک اشتراک",
        "add_node" to "ساخت سرور دستی",
        "protocol" to "پروتکل",
        "remark" to "نام مستعار (Remark)",
        "server" to "آدرس سرور (IP/Host)",
        "port" to "پورت",
        "uuid" to "شناسه کاربری (UUID/Password)",
        "sni" to "نام سرور امنیتی (SNI)",
        "tls" to "فعال‌سازی TLS / Reality",
        "save" to "ذخیره سرور",
        "delete" to "حذف",
        "ping_all" to "تست تاخیر همه سرورها",
        "auto_connect" to "اتصال خودکار به بهترین پینگ",
        "log_level" to "سطح نمایش لاگ",
        "clear_logs" to "پاک کردن لاگ‌ها",
        "active_sub" to "اشتراک فعال",
        "paste_links" to "پیست کردن لینک‌های پیکربندی (vless, vmess, trojan, ss)",
        "import_btn" to "وارد کردن",
        "sub_url" to "آدرس URL اشتراک",
        "sub_name" to "نام اشتراک",
        "sub_import_btn" to "افزودن اشتراک",
        "download" to "دانلود",
        "upload" to "آپلود",
        "theme_dynamic" to "سیستم (ویندوز)",
        "theme_cherry" to "شکوفه گیلاس",
        "theme_lavender" to "اسطوخودوس",
        "theme_rose" to "رز گلد",
        "theme_midnight" to "آبی نیمه‌شب",
        "theme_forest" to "سبز جنگلی",
        "theme_sunset" to "غروب آفتاب",
        "theme_teal" to "آبی اقیانوسی",
        "theme_amethyst" to "آمیتیس سلطنتی",
        "theme_slate" to "سنگ لوح",
        "lang_name" to "زبان / Language",
        "enable_mux" to "فعال‌سازی Multiplexing (smux)",
        "enable_frag" to "فعال‌سازی Fragmentation",
        "frag_len" to "بازه طول پکت (Packet Length)",
        "frag_int" to "بازه زمانی تاخیر (Interval ms)",
        "split_tunneling" to "تونل‌سازی اپلیکیشن",
        "cdn_scanner" to "اسکنر آی‌پی تمیز",
        "double_hop_chain" to "پروکسی زنجیره‌ای"
    )

    private val en = mapOf(
        "app_name" to "Chameleon",
        "sub_manager" to "Subscriptions",
        "connected" to "Connected",
        "connecting" to "Connecting...",
        "disconnected" to "Disconnected",
        "disconnecting" to "Disconnecting...",
        "active_node" to "Active Node",
        "dashboard" to "Dashboard",
        "profiles" to "Profiles",
        "add_config" to "Add Config",
        "logs" to "Logs",
        "settings" to "Settings",
        "bypass_iran" to "Bypass Iran Domains (Direct)",
        "bypass_lan" to "Bypass Local Area Network (LAN)",
        "secure_dns" to "Secure DNS Server",
        "theme" to "Application Theme Color",
        "dark_mode" to "Dark Mode",
        "advanced_mode" to "Advanced Mode (Fragmentation & Mux)",
        "import_profile" to "Import Profile Link",
        "import_sub" to "Import Subscription",
        "add_node" to "Add Manual Node",
        "protocol" to "Protocol",
        "remark" to "Remark / Name",
        "server" to "Server (IP/Host)",
        "port" to "Port",
        "uuid" to "Credentials (UUID/Password)",
        "sni" to "Server Name (SNI)",
        "tls" to "Enable TLS / Reality",
        "save" to "Save Config",
        "delete" to "Delete",
        "ping_all" to "Test All Server Pings",
        "auto_connect" to "Auto-Connect to Best Ping",
        "log_level" to "Log Level",
        "clear_logs" to "Clear Logs",
        "active_sub" to "Active Subscription",
        "paste_links" to "Paste raw connection links (vless, vmess, trojan, ss, hy2, tuic)",
        "import_btn" to "Import Configs",
        "sub_url" to "Subscription URL",
        "sub_name" to "Subscription Name",
        "sub_import_btn" to "Import Subscription",
        "download" to "Download",
        "upload" to "Upload",
        "theme_dynamic" to "System (Windows Accent)",
        "theme_cherry" to "Cherry Blossom",
        "theme_lavender" to "Lavender Dreams",
        "theme_rose" to "Rose Gold",
        "theme_midnight" to "Midnight Blue",
        "theme_forest" to "Forest Green",
        "theme_sunset" to "Sunset Orange",
        "theme_teal" to "Ocean Teal",
        "theme_amethyst" to "Royal Amethyst",
        "theme_slate" to "Nordic Slate",
        "lang_name" to "Language / زبان",
        "enable_mux" to "Enable Multiplexing (smux)",
        "enable_frag" to "Enable Fragmentation",
        "frag_len" to "Packet Length Range",
        "frag_int" to "Delay Interval Range (ms)",
        "split_tunneling" to "App Split Tunneling",
        "cdn_scanner" to "Clean IP Scanner",
        "double_hop_chain" to "Proxy Chaining"
    )

    fun get(key: String, isFarsi: Boolean): String {
        val map = if (isFarsi) fa else en
        return map[key] ?: key
    }
}

data class ServerItem(
    val link: String,
    val name: String,
    val type: String
)

@Composable
fun AnimatedAcrylicBackground(isDark: Boolean, primary: Color, secondary: Color, tertiary: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "acrylic_bg")
    
    val t1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(40000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "t1"
    )
    val t2 by infiniteTransition.animateFloat(
        initialValue = 180f, targetValue = 540f,
        animationSpec = infiniteRepeatable(animation = tween(35000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "t2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        // Draw deep base background color
        drawRect(color = if (isDark) Color(0xFF0C0E14) else Color(0xFFF7F8FC))

        // Glowing blurred neon circular points in corners
        val radius1 = w * 0.45f
        val radius2 = w * 0.4f

        val angle1Rad = Math.toRadians(t1.toDouble())
        val offset1 = Offset(
            x = (w * 0.25f + Math.cos(angle1Rad) * (w * 0.1f)).toFloat(),
            y = (h * 0.25f + Math.sin(angle1Rad) * (h * 0.1f)).toFloat()
        )

        val angle2Rad = Math.toRadians(t2.toDouble())
        val offset2 = Offset(
            x = (w * 0.75f + Math.cos(angle2Rad) * (w * 0.1f)).toFloat(),
            y = (h * 0.70f + Math.sin(angle2Rad) * (h * 0.1f)).toFloat()
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = if (isDark) 0.15f else 0.18f), Color.Transparent),
                center = offset1,
                radius = radius1
            ),
            center = offset1,
            radius = radius1
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = if (isDark) 0.12f else 0.15f), Color.Transparent),
                center = offset2,
                radius = radius2
            ),
            center = offset2,
            radius = radius2
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = if (isDark) 0.08f else 0.10f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.9f),
                radius = radius2
            ),
            center = Offset(w * 0.5f, h * 0.9f),
            radius = radius2
        )
    }
}

@Composable
fun MainScreen() {
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager() }
    val settings by settingsManager.settings.collectAsState()

    val isFarsi = settings.isFarsi
    val layoutDirection = if (isFarsi) LayoutDirection.Rtl else LayoutDirection.Ltr

    var currentScreen by remember { mutableStateOf("dashboard") }

    val isDark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val cardStyle = settings.cardStyle

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerLow = MaterialTheme.colorScheme.surfaceContainerLow
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val cardBorderBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, outlineVariant) {
        if (cardStyle == "solid" || cardStyle == "vibrant") {
            SolidColor(outlineVariant)
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.16f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    val primaryCardBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, surfaceContainerHigh, primaryContainer, surfaceColor) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainerHigh)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            SolidColor(surfaceColor.copy(alpha = 0.45f))
        }
    }

    val secondaryCardBrush = remember(isDark, cardStyle, secondaryColor, tertiaryColor, surfaceContainer, primaryContainer, surfaceColor) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainer)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            SolidColor(surfaceColor.copy(alpha = 0.40f))
        }
    }

    val tertiaryCardBrush = remember(isDark, cardStyle, tertiaryColor, primaryColor, surfaceContainerLow, primaryContainer, surfaceColor) {
        if (cardStyle == "solid") {
            SolidColor(surfaceContainerLow)
        } else if (cardStyle == "vibrant") {
            SolidColor(primaryContainer)
        } else {
            SolidColor(surfaceColor.copy(alpha = 0.35f))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ActiveCardTransition")
    val flowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flowOffset"
    )

    val activeCardBackgroundBrush = remember(isDark, cardStyle, primaryColor, secondaryColor, tertiaryColor, primaryContainer, flowOffset) {
        if (cardStyle == "solid") {
            SolidColor(primaryContainer)
        } else if (cardStyle == "vibrant") {
            Brush.linearGradient(
                colors = listOf(primaryColor, secondaryColor),
                start = Offset(flowOffset - 500f, 0f),
                end = Offset(flowOffset + 500f, 1000f)
            )
        } else {
            val colors = if (isDark) {
                listOf(
                    primaryColor.copy(alpha = 0.40f),
                    secondaryColor.copy(alpha = 0.25f)
                )
            } else {
                listOf(
                    primaryColor.copy(alpha = 0.18f),
                    secondaryColor.copy(alpha = 0.08f)
                )
            }
            Brush.linearGradient(
                colors = colors,
                start = Offset(flowOffset - 500f, 0f),
                end = Offset(flowOffset + 500f, 1000f)
            )
        }
    }

    // Keep 40-point history buffer of traffic upload/download speeds
    val trafficStats by SingboxManager.trafficStats.collectAsState()
    val speedHistory = remember { mutableStateListOf<Pair<Long, Long>>() }
    LaunchedEffect(trafficStats) {
        val state = SingboxManager.vpnState.value
        if (state == "CONNECTED" || state == "CONNECTING") {
            speedHistory.add(trafficStats)
            if (speedHistory.size > 40) {
                speedHistory.removeAt(0)
            }
        } else {
            speedHistory.clear()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Neon flowing blurred backdrops
            AnimatedAcrylicBackground(
                isDark = isDark,
                primary = primaryColor,
                secondary = secondaryColor,
                tertiary = tertiaryColor
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar Navigation
                Sidebar(
                    currentScreen = currentScreen,
                    onScreenChange = { currentScreen = it },
                    settings = settings,
                    settingsManager = settingsManager
                )

                // Translucent Main Content container
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(24.dp)
                ) {
                    when (currentScreen) {
                        "dashboard" -> DashboardScreen(
                            settings = settings,
                            settingsManager = settingsManager,
                            primaryCardBrush = primaryCardBrush,
                            secondaryCardBrush = secondaryCardBrush,
                            activeCardBackgroundBrush = activeCardBackgroundBrush,
                            cardBorderBrush = cardBorderBrush,
                            speedHistory = speedHistory
                        )
                        "subscriptions" -> SubscriptionManagerScreen(
                            settings = settings,
                            settingsManager = settingsManager,
                            primaryCardBrush = primaryCardBrush,
                            secondaryCardBrush = secondaryCardBrush,
                            cardBorderBrush = cardBorderBrush
                        )
                        "profiles" -> ProfilesScreen(settings, settingsManager)
                        "add_config" -> AddConfigScreen(settings, settingsManager)
                        "split_tunneling" -> SplitTunnelingScreen(settings, settingsManager)
                        "cdn_scanner" -> CdnScannerScreen(settings, settingsManager)
                        "logs" -> LogsScreen(settings)
                        "settings" -> SettingsScreen(
                            settings = settings,
                            settingsManager = settingsManager,
                            primaryCardBrush = primaryCardBrush,
                            secondaryCardBrush = secondaryCardBrush,
                            cardBorderBrush = cardBorderBrush
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Sidebar(
    currentScreen: String,
    onScreenChange: (String) -> Unit,
    settings: UserSettings,
    settingsManager: SettingsManager
) {
    val isFarsi = settings.isFarsi
    
    // Glassmorphic side navigation column
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(245.dp)
            .background(Color.Black.copy(alpha = if (settings.themeMode == "dark" || isSystemInDarkTheme()) 0.25f else 0.04f))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                shape = RectangleShape
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Premium Brand Logo Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
            ) {
                Image(
                    painter = painterResource("icon.png"),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = DesktopStrings.get("app_name", isFarsi),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Sleek navigation items
            val navItems = listOf(
                Triple("dashboard", Icons.Filled.Dashboard, "dashboard"),
                Triple("subscriptions", Icons.Filled.RssFeed, "sub_manager"),
                Triple("profiles", Icons.AutoMirrored.Filled.List, "profiles"),
                Triple("split_tunneling", Icons.Filled.FilterAlt, "split_tunneling"),
                Triple("cdn_scanner", Icons.Filled.Speed, "cdn_scanner"),
                Triple("add_config", Icons.Filled.Add, "add_config"),
                Triple("logs", Icons.Filled.Terminal, "logs"),
                Triple("settings", Icons.Filled.Settings, "settings")
            )

            navItems.forEach { (screen, icon, stringKey) ->
                val selected = currentScreen == screen
                
                // Hover visual springs
                var isHovered by remember { mutableStateOf(false) }
                val scaleFactor by animateFloatAsState(
                    targetValue = if (isHovered) 1.03f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                val pillWidthPercent by animateFloatAsState(
                    targetValue = if (selected) 1f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .scale(scaleFactor)
                        .onPointerEvent(PointerEventType.Enter) { isHovered = true }
                        .onPointerEvent(PointerEventType.Exit) { isHovered = false }
                        .clip(ExpressiveButtonShape)
                        .clickable { onScreenChange(screen) }
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    // Active selection backdrop pill
                    if (pillWidthPercent > 0.01f) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                                        )
                                    ),
                                    shape = ExpressiveButtonShape
                                )
                        )
                        // Tiny highlight line on active
                        Box(
                            modifier = Modifier
                                .height(20.dp)
                                .width(4.dp)
                                .align(Alignment.CenterStart)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = if (pillWidthPercent > 0.01f) 8.dp else 0.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = DesktopStrings.get(stringKey, isFarsi),
                            fontSize = 13.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Language Select Button at the bottom
        Button(
            onClick = { settingsManager.setIsFarsi(!isFarsi) },
            shape = ExpressiveButtonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.Language, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isFarsi) "English" else "فارسی", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardScreen(
    settings: UserSettings,
    settingsManager: SettingsManager,
    primaryCardBrush: Brush,
    secondaryCardBrush: Brush,
    activeCardBackgroundBrush: Brush,
    cardBorderBrush: Brush,
    speedHistory: List<Pair<Long, Long>>
) {
    val isFarsi = settings.isFarsi
    val vpnState by SingboxManager.vpnState.collectAsState()
    val trafficStats by SingboxManager.trafficStats.collectAsState()

    val scope = rememberCoroutineScope()
    
    val activeProfile = settings.activeProfile
    val displayNodeName = remember(activeProfile) {
        if (activeProfile.isEmpty()) {
            "No Node Selected"
        } else {
            getProxyName(activeProfile)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: connection stats & speed chart
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = DesktopStrings.get("dashboard", isFarsi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stats Cards (Row Layout)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                            .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                    ) {
                        VibrantCardContent(settings.cardStyle) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DesktopStrings.get("download", isFarsi),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatSpeed(trafficStats.second),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Card(
                        shape = ExpressiveCardShape,
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                            .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                    ) {
                        VibrantCardContent(settings.cardStyle) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Upload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = DesktopStrings.get("upload", isFarsi),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatSpeed(trafficStats.first),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // High fidelity Live Speed Chart Card
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                        .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                        Text(
                            text = if (isFarsi) "نمودار سرعت ترافیک زنده" else "Live Speed Chart",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SpeedChart(
                            history = speedHistory,
                            primaryColor = MaterialTheme.colorScheme.primary,
                            secondaryColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Node Banner
            val isVpnActive = vpnState == "CONNECTED" || vpnState == "CONNECTING"
            val activeBrush = if (isVpnActive) activeCardBackgroundBrush else primaryCardBrush
            
            Card(
                shape = ExpressiveCardShape,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = activeBrush, shape = ExpressiveCardShape)
                    .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
            ) {
                VibrantCardContent(settings.cardStyle) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = DesktopStrings.get("active_node", isFarsi),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayNodeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(32.dp))

        // Right Column: large animated connect circle
        Column(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(280.dp)
            ) {
                // Dual pulsing rings for connected state
                if (vpnState == "CONNECTED" || vpnState == "CONNECTING") {
                    val pulseTransition = rememberInfiniteTransition(label = "pulse")
                    val pulse1 by pulseTransition.animateFloat(
                        initialValue = 130f,
                        targetValue = 200f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = EaseOutQuad),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulse1"
                    )
                    val alpha1 by pulseTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = EaseOutQuad),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha1"
                    )
                    
                    val pulse2 by pulseTransition.animateFloat(
                        initialValue = 130f,
                        targetValue = 240f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, delayMillis = 1000, easing = EaseOutQuad),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulse2"
                    )
                    val alpha2 by pulseTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, delayMillis = 1000, easing = EaseOutQuad),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha2"
                    )

                    Box(
                        modifier = Modifier
                            .size(pulse1.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha1))
                    )
                    Box(
                        modifier = Modifier
                            .size(pulse2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = alpha2))
                    )
                }

                // Rotating gradient progress ring
                val infiniteTransition = rememberInfiniteTransition(label = "rotating_progress")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )

                val buttonScale by animateFloatAsState(
                    targetValue = if (vpnState == "CONNECTED") 1.05f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                )

                // Main Connection Button
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(buttonScale)
                        .clip(CircleShape)
                        .clickable {
                            if (vpnState == "CONNECTED") {
                                SingboxManager.stop()
                            } else if (vpnState == "DISCONNECTED") {
                                if (activeProfile.isNotEmpty()) {
                                    scope.launch {
                                        SingboxManager.start(activeProfile, settingsManager)
                                    }
                                }
                            }
                        }
                        .background(
                            Brush.sweepGradient(
                                if (vpnState == "CONNECTED") {
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                } else {
                                    listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), MaterialTheme.colorScheme.surfaceVariant)
                                }
                            )
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    if (vpnState == "CONNECTING" || vpnState == "DISCONNECTING") {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(rotation)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (vpnState == "CONNECTED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (vpnState == "CONNECTED") {
                                DesktopStrings.get("connected", isFarsi)
                            } else if (vpnState == "CONNECTING") {
                                DesktopStrings.get("connecting", isFarsi)
                            } else if (vpnState == "DISCONNECTING") {
                                DesktopStrings.get("disconnecting", isFarsi)
                            } else {
                                DesktopStrings.get("disconnected", isFarsi)
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vpnState == "CONNECTED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Connection Stopwatch Timer Badge
            if (vpnState == "CONNECTED") {
                Spacer(modifier = Modifier.height(16.dp))
                StopwatchTimerBadge()
            }
        }
    }
}

@Composable
fun StopwatchTimerBadge() {
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000
            delay(1000)
        }
    }
    val minutes = (elapsedSeconds / 60) % 60
    val hours = elapsedSeconds / 3600
    val seconds = elapsedSeconds % 60
    val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = Modifier
            .clip(ExpressiveChipShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .border(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), shape = ExpressiveChipShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2ecc71))
            )
            Text(
                text = timeStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun SpeedChart(
    history: List<Pair<Long, Long>>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f || history.isEmpty()) return@Canvas

        // Draw horizontal grid lines
        val gridLinesCount = 3
        for (i in 1..gridLinesCount) {
            val y = h * (i.toFloat() / (gridLinesCount + 1))
            drawLine(
                color = Color.White.copy(alpha = 0.08f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }

        // Calculate maximum value in buffer to scale y-axis
        val maxSpeed = (history.maxOfOrNull { maxOf(it.first, it.second) } ?: 0L).coerceAtLeast(1024L)
        val maxSpeedFloat = maxSpeed.toFloat()

        val pointsCount = history.size
        val xInterval = w / 39f // Always show a 40 point window range

        // 1. Download Path (Blue)
        val dlPath = Path()
        // 2. Upload Path (Green)
        val ulPath = Path()

        history.forEachIndexed { index, pair ->
            val x = index * xInterval
            // Download y
            val dlY = h - (pair.second.toFloat() / maxSpeedFloat) * (h * 0.85f)
            // Upload y
            val ulY = h - (pair.first.toFloat() / maxSpeedFloat) * (h * 0.85f)

            if (index == 0) {
                dlPath.moveTo(x, dlY)
                ulPath.moveTo(x, ulY)
            } else {
                val prevX = (index - 1) * xInterval
                val prevDlY = h - (history[index - 1].second.toFloat() / maxSpeedFloat) * (h * 0.85f)
                val prevUlY = h - (history[index - 1].first.toFloat() / maxSpeedFloat) * (h * 0.85f)

                // Smooth cubic beziers
                dlPath.cubicTo(
                    (prevX + x) / 2f, prevDlY,
                    (prevX + x) / 2f, dlY,
                    x, dlY
                )
                ulPath.cubicTo(
                    (prevX + x) / 2f, prevUlY,
                    (prevX + x) / 2f, ulY,
                    x, ulY
                )
            }
        }

        // Draw filled gradient under Download curve
        if (history.size > 1) {
            val dlFillPath = Path().apply {
                addPath(dlPath)
                lineTo((history.size - 1) * xInterval, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = dlFillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.15f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )
        }

        // Draw curves
        drawPath(
            path = dlPath,
            color = primaryColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        drawPath(
            path = ulPath,
            color = secondaryColor,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// Helper formats
private fun formatSpeed(bytes: Long): String {
    val kb = bytes / 1024f
    return if (kb > 1024) {
        val mb = kb / 1024f
        String.format("%.2f MB/s", mb)
    } else {
        String.format("%.1f KB/s", kb)
    }
}

@Composable
fun ProfilesScreen(settings: UserSettings, settingsManager: SettingsManager) {
    val isFarsi = settings.isFarsi
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("ALL", "VLESS", "TROJAN", "SHADOWSOCKS", "VMESS", "HYSTERIA", "TUIC", "CHAIN")

    val subscriptions = settings.deserializedSubscriptions
    val activeSubId = settings.activeSubId

    val activeSubscription = remember(subscriptions, activeSubId) {
        subscriptions.find { it.id == activeSubId } ?: subscriptions.firstOrNull()
    }

    val serverList = remember(activeSubscription) {
        activeSubscription?.servers?.split("\n")?.filter { it.trim().isNotEmpty() } ?: emptyList()
    }

    var pingsMap by remember { mutableStateOf(mapOf<String, Int>()) }
    var isTestingPings by remember { mutableStateOf(false) }

    // Chain builder popup states
    var showChainBuilder by remember { mutableStateOf(false) }
    val chains = remember(settings.proxyChains) { deserializeProxyChains(settings.proxyChains) }

    val filteredServerList = remember(serverList, selectedTab, chains) {
        if (selectedTab == 7) { // CHAIN Tab
            chains.map { chain ->
                val escapedName = java.net.URLEncoder.encode(chain.name, "UTF-8")
                ServerItem(
                    link = "chain://${chain.id}#$escapedName",
                    name = chain.name,
                    type = "CHAIN"
                )
            }
        } else {
            serverList.mapNotNull { serverLink ->
                val type = serverLink.substringBefore("://").uppercase()
                val matchesTab = when (selectedTab) {
                    0 -> true
                    1 -> type == "VLESS"
                    2 -> type == "TROJAN"
                    3 -> type == "SS" || type == "SHADOWSOCKS"
                    4 -> type == "VMESS"
                    5 -> type == "HYSTERIA" || type == "HYSTERIA2" || type == "HY2"
                    6 -> type == "TUIC"
                    else -> true
                }
                if (matchesTab) {
                    val name = getProxyName(serverLink)
                    ServerItem(link = serverLink, name = name, type = type)
                } else null
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DesktopStrings.get("profiles", isFarsi),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Double-hop Proxy Chain & Ping Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedTab == 7) {
                    // Create Chain Button
                    Button(
                        onClick = { showChainBuilder = true },
                        shape = ExpressiveButtonShape,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (isFarsi) "ایجاد زنجیره دو مرحله‌ای" else "Build Proxy Chain", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            if (!isTestingPings && serverList.isNotEmpty()) {
                                isTestingPings = true
                                scope.launch {
                                    val jobs = serverList.map { server ->
                                        async {
                                            val hostPort = getHostAndPortFromLink(server)
                                            val delay = if (hostPort != null) {
                                                measurePingDelay(hostPort.first, hostPort.second)
                                            } else {
                                                -1
                                            }
                                            server to delay
                                        }
                                    }
                                    val results = jobs.awaitAll()
                                    pingsMap = results.toMap()
                                    isTestingPings = false
                                }
                            }
                        },
                        shape = ExpressiveButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        if (isTestingPings) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Filled.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTestingPings) (if (isFarsi) "در حال تست..." else "Testing...") else DesktopStrings.get("ping_all", isFarsi),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Selector Row
        if (subscriptions.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subscriptions) { sub ->
                    val selected = sub.id == activeSubId
                    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    val tc = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    
                    Box(
                        modifier = Modifier
                            .clip(ExpressiveChipShape)
                            .background(bg)
                            .clickable { settingsManager.setActiveSubId(sub.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = sub.name, color = tc, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sleek horizontal scroll tabs
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(tabs) { idx, tabTitle ->
                val selected = selectedTab == idx
                val tc = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .clip(ExpressiveChipShape)
                        .background(bg)
                        .clickable { selectedTab = idx }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = tabTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tc
                    )
                }
            }
        }

        // Servers List Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(ExpressiveCardShape)
                .background(Color.White.copy(alpha = if (settings.themeMode == "dark" || isSystemInDarkTheme()) 0.08f else 0.4f))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = ExpressiveCardShape)
        ) {
            if (filteredServerList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isFarsi) "پیکربندی یافت نشد." else "No profiles found in this category.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredServerList) { server ->
                        val isActive = settings.activeProfile == server.link
                        val bg = if (isActive) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            Color.White.copy(alpha = 0.04f)
                        }
                        
                        val borderCol = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        } else {
                            Color.White.copy(alpha = 0.05f)
                        }

                        // Hover animations
                        var isItemHovered by remember { mutableStateOf(false) }
                        val scaleFactor by animateFloatAsState(
                            targetValue = if (isItemHovered) 1.015f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(scaleFactor)
                                .onPointerEvent(PointerEventType.Enter) { isItemHovered = true }
                                .onPointerEvent(PointerEventType.Exit) { isItemHovered = false }
                                .clip(ExpressiveCardShape)
                                .background(bg)
                                .border(width = 1.dp, color = borderCol, shape = ExpressiveCardShape)
                                .clickable { settingsManager.setActiveProfile(server.link) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Protocol Type Icon Pill
                                Box(
                                    modifier = Modifier
                                        .clip(ExpressiveChipShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = server.type,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = server.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Right Side: ping result or delete button for chains
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (server.type == "CHAIN") {
                                    IconButton(
                                        onClick = {
                                            val chainId = server.link.substringAfter("chain://").substringBefore("#")
                                            val filteredChains = chains.filter { it.id != chainId }
                                            settingsManager.setProxyChains(serializeProxyChains(filteredChains))
                                            if (settings.activeProfile == server.link) {
                                                settingsManager.setActiveProfile("")
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    val ping = pingsMap[server.link]
                                    if (ping != null) {
                                        val color = when {
                                            ping < 0 -> MaterialTheme.colorScheme.error
                                            ping < 150 -> Color(0xFF2ecc71)
                                            ping < 300 -> Color(0xFFf1c40f)
                                            else -> Color(0xFFe67e22)
                                        }
                                        val text = if (ping < 0) "Timeout" else "${ping}ms"
                                        Text(
                                            text = text,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
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

    if (showChainBuilder) {
        ChainBuilderDialog(
            editingChainLink = null,
            proxyChainsStr = settings.proxyChains,
            serverList = serverList,
            isFarsi = isFarsi,
            onDismiss = { showChainBuilder = false },
            onSave = { name, relay, exit ->
                val newChain = ProxyChain(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    relayLink = relay,
                    exitLink = exit
                )
                val updatedChains = chains + newChain
                settingsManager.setProxyChains(serializeProxyChains(updatedChains))
                showChainBuilder = false
            }
        )
    }
}

private fun getHostAndPortFromLink(link: String): Pair<String, Int>? {
    return try {
        val trimmed = link.trim()
        val rest = trimmed.substringAfter("://")
        val content = rest.substringBefore("#")
        val mainPart = content.substringBefore("?")
        val serverPart = mainPart.substringAfter("@")
        val parts = serverPart.split(":")
        val host = parts[0]
        val port = parts[1].toInt()
        host to port
    } catch (e: Exception) {
        null
    }
}

private suspend fun measurePingDelay(host: String, port: Int): Int = withContext(Dispatchers.IO) {
    val start = System.currentTimeMillis()
    try {
        val socket = java.net.Socket()
        socket.connect(java.net.InetSocketAddress(host, port), 2500)
        socket.close()
        (System.currentTimeMillis() - start).toInt()
    } catch (e: Exception) {
        -1
    }
}

@Composable
fun SubscriptionManagerScreen(
    settings: UserSettings,
    settingsManager: SettingsManager,
    primaryCardBrush: Brush,
    secondaryCardBrush: Brush,
    cardBorderBrush: Brush
) {
    val isFarsi = settings.isFarsi
    var nameInput by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val subscriptions = settings.deserializedSubscriptions

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = DesktopStrings.get("sub_manager", isFarsi),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Import Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = DesktopStrings.get("import_sub", isFarsi),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(text = DesktopStrings.get("sub_name", isFarsi)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveButtonShape
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(text = DesktopStrings.get("sub_url", isFarsi)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = ExpressiveButtonShape
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (urlInput.isNotEmpty() && nameInput.isNotEmpty()) {
                            isFetching = true
                            statusText = if (isFarsi) "در حال دریافت اطلاعات..." else "Fetching subscription..."
                            scope.launch {
                                val result = fetchSubscription(urlInput)
                                if (result.fetchError != null) {
                                    statusText = result.fetchError
                                } else {
                                    val newSub = Subscription(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = nameInput,
                                        url = urlInput,
                                        servers = result.servers.joinToString("\n"),
                                        upload = result.upload,
                                        download = result.download,
                                        total = result.total,
                                        expire = result.expire
                                    )
                                    val list = subscriptions.filter { it.id != "manual" } + newSub
                                    settingsManager.setSubscriptionList(serializeSubscriptions(list))
                                    nameInput = ""
                                    urlInput = ""
                                    statusText = if (isFarsi) "اشتراک با موفقیت اضافه شد." else "Subscription imported successfully."
                                }
                                isFetching = false
                            }
                        }
                    },
                    shape = ExpressiveButtonShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFetching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(text = DesktopStrings.get("sub_import_btn", isFarsi), fontWeight = FontWeight.Bold)
                    }
                }
                if (statusText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = statusText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscriptions List
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subscriptions.filter { it.id != "manual" }) { sub ->
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = secondaryCardBrush, shape = ExpressiveCardShape)
                        .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = sub.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val list = subscriptions.filter { it.id != sub.id && it.id != "manual" }
                                settingsManager.setSubscriptionList(serializeSubscriptions(list))
                            }) {
                                Icon(imageVector = Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        Text(text = sub.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        
                        // Display stats details if available
                        if (sub.total != null && sub.total > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val used = (sub.upload ?: 0L) + (sub.download ?: 0L)
                            Text(
                                text = "Traffic Usage: ${formatBytes(used)} / ${formatBytes(sub.total)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LinearProgressIndicator(
                                progress = (used.toFloat() / sub.total.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(CircleShape)
                            )
                        }
                        if (sub.expire != null && sub.expire > 0) {
                            Text(
                                text = "Expiry Date: ${formatExpiry(sub.expire)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddConfigScreen(settings: UserSettings, settingsManager: SettingsManager) {
    val isFarsi = settings.isFarsi
    var textInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = DesktopStrings.get("add_config", isFarsi),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Import textarea card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White.copy(alpha = if (settings.themeMode == "dark" || isSystemInDarkTheme()) 0.08f else 0.4f))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.08f), shape = ExpressiveCardShape)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Text(
                    text = DesktopStrings.get("paste_links", isFarsi),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text(text = "vless://...\nvmess://...\ntrojan://...\nss://...") },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = ExpressiveButtonShape
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val links = textInput.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                        if (links.isNotEmpty()) {
                            val existing = settings.manualServers.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                            val updated = (existing + links).joinToString("\n")
                            settingsManager.setManualServers(updated)
                            textInput = ""
                            statusText = if (isFarsi) "پیکربندی‌ها با موفقیت وارد شدند." else "Configurations imported successfully."
                        }
                    },
                    shape = ExpressiveButtonShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = DesktopStrings.get("import_btn", isFarsi), fontWeight = FontWeight.Bold)
                }
                if (statusText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = statusText, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LogsScreen(settings: UserSettings) {
    val isFarsi = settings.isFarsi
    val logs by SingboxManager.vpnLogs.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DesktopStrings.get("logs", isFarsi),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = { SingboxManager.clearLogs() },
                shape = ExpressiveButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(text = DesktopStrings.get("clear_logs", isFarsi), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Log Console Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(ExpressiveCardShape)
                .background(Color.Black.copy(alpha = 0.8f))
                .border(width = 1.dp, color = Color.White.copy(alpha = 0.1f), shape = ExpressiveCardShape)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(logs) {
                scrollState.scrollTo(scrollState.maxValue)
            }
            Text(
                text = if (logs.isEmpty()) "Logs console is empty..." else logs,
                color = Color(0xFF2ecc71),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun SettingsScreen(
    settings: UserSettings,
    settingsManager: SettingsManager,
    primaryCardBrush: Brush,
    secondaryCardBrush: Brush,
    cardBorderBrush: Brush
) {
    val isFarsi = settings.isFarsi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = DesktopStrings.get("settings", isFarsi),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        // General VPN Settings Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
        ) {
            VibrantCardContent(settings.cardStyle) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Direct Iranian domains
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsManager.setBypassIran(!settings.bypassIran) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DesktopStrings.get("bypass_iran", isFarsi),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFarsi) "سرعت سایت‌های داخلی ایران را افزایش داده و مصرف ترافیک داخلی را نیم‌بها نگه می‌دارد." else "Improves local connections speed and saves international data by bypassing VPN.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.bypassIran, onCheckedChange = { settingsManager.setBypassIran(it) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bypass LAN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsManager.setBypassLan(!settings.bypassLan) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DesktopStrings.get("bypass_lan", isFarsi),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFarsi) "اتصال‌های شبکه محلی (مانند پرینتر یا مودم) را مستقیماً هدایت می‌کند." else "Bypasses VPN for local area network (LAN) devices.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.bypassLan, onCheckedChange = { settingsManager.setBypassLan(it) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // TUN Mode Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsManager.setEnableTun(!settings.enableTun) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFarsi) "حالت تون (TUN Mode - نیازمند ادمین)" else "Virtual TUN Interface (Requires Admin)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isFarsi) "کل ترافیک سیستم را در سطح کارت شبکه هدایت می‌کند. نیاز به اجرای برنامه به صورت Run as Administrator دارد." else "Routes all system traffic at the network adapter level. Requires running Chameleon as Administrator.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.enableTun, onCheckedChange = { settingsManager.setEnableTun(it) })
                }
                if (settings.enableTun) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isFarsi) "توجه: برای اجرای موفقیت‌آمیز حالت تون، باید wintun.dll را در پوشه برنامه قرار دهید." else "Note: Place wintun.dll in the application directory for the TUN driver to initialize successfully.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            }
        }

        // Secure DNS Settings Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
        ) {
            VibrantCardContent(settings.cardStyle) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = DesktopStrings.get("secure_dns", isFarsi),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "https://1.1.1.1/dns-query" to "Cloudflare DoH (Fast & Secure)",
                    "https://8.8.8.8/dns-query" to "Google DoH",
                    "https://78.22.122.100/dns-query" to "Shecan DNS (Iran circumvention DoH)",
                    "udp://1.1.1.1" to "Cloudflare UDP Standard (Fastest)"
                ).forEach { (dnsUrl, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setSecureDns(dnsUrl) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = settings.secureDns == dnsUrl,
                            onClick = { settingsManager.setSecureDns(dnsUrl) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = dnsUrl, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }

        // Advanced Config Card (Fragment & Mux)
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
        ) {
            VibrantCardContent(settings.cardStyle) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = DesktopStrings.get("advanced_mode", isFarsi),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Advanced DPI bypassing parameters. Useful under extreme ISP restrictions.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = settings.isAdvancedMode, onCheckedChange = { settingsManager.setAdvancedMode(it) })
                }

                if (settings.isAdvancedMode) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Multiplexing
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setEnableMux(!settings.enableMux) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = settings.enableMux, onCheckedChange = { settingsManager.setEnableMux(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = DesktopStrings.get("enable_mux", isFarsi), fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Fragmentation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settingsManager.setEnableFragment(!settings.enableFragment) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = settings.enableFragment, onCheckedChange = { settingsManager.setEnableFragment(it) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = DesktopStrings.get("enable_frag", isFarsi), fontSize = 13.sp)
                    }

                    if (settings.enableFragment) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.fragmentLength,
                            onValueChange = { settingsManager.setFragmentLength(it) },
                            label = { Text(text = DesktopStrings.get("frag_len", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = settings.fragmentInterval,
                            onValueChange = { settingsManager.setFragmentInterval(it) },
                            label = { Text(text = DesktopStrings.get("frag_int", isFarsi)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = ExpressiveButtonShape
                        )
                    }
                }
            }
            }
        }

        // Theme Palette Selector Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = primaryCardBrush, shape = ExpressiveCardShape)
                .border(width = 1.dp, brush = cardBorderBrush, shape = ExpressiveCardShape)
        ) {
            VibrantCardContent(settings.cardStyle) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = DesktopStrings.get("theme", isFarsi),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                val themes = listOf(
                    "dynamic" to "theme_dynamic",
                    "cherry_blossom" to "theme_cherry",
                    "lavender_dreams" to "theme_lavender",
                    "rose_gold" to "theme_rose",
                    "midnight_blue" to "theme_midnight",
                    "forest_green" to "theme_forest",
                    "sunset_orange" to "theme_sunset",
                    "ocean_teal" to "theme_teal",
                    "royal_amethyst" to "theme_amethyst",
                    "nordic_slate" to "theme_slate"
                )

                val themeColors = mapOf(
                    "dynamic" to Color(0xFF0078D4),
                    "cherry_blossom" to Color(0xFFFF8A9F),
                    "lavender_dreams" to Color(0xFFC39BD3),
                    "rose_gold" to Color(0xFFF1948A),
                    "midnight_blue" to Color(0xFF5DADE2),
                    "forest_green" to Color(0xFF58D68D),
                    "sunset_orange" to Color(0xFFF5B041),
                    "ocean_teal" to Color(0xFF48C9B0),
                    "royal_amethyst" to Color(0xFFA569BD),
                    "nordic_slate" to Color(0xFF5D6D7E)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themes.forEach { (themeKey, nameKey) ->
                        val selected = settings.specialTheme == themeKey
                        val tc = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        val swatchColor = themeColors[themeKey] ?: Color.Gray

                        Box(
                            modifier = Modifier
                                .clip(ExpressiveChipShape)
                                .background(bg)
                                .clickable { settingsManager.setSpecialTheme(themeKey) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                )
                                Text(
                                    text = DesktopStrings.get(nameKey, isFarsi),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tc
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = if (isFarsi) "استایل کارت‌ها" else "Card Background Style",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                val cardStyles = listOf(
                    "glass" to (if (isFarsi) "شیشه‌ای (شفاف)" else "Glass (Translucent)"),
                    "solid" to (if (isFarsi) "توپر (مات)" else "Solid (Opaque)"),
                    "vibrant" to (if (isFarsi) "پویا (پررنگ)" else "Vibrant (High Contrast)")
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cardStyles.forEach { (styleKey, displayName) ->
                        val selected = settings.cardStyle == styleKey
                        val tc = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        
                        Box(
                            modifier = Modifier
                                .clip(ExpressiveChipShape)
                                .background(bg)
                                .clickable { settingsManager.setCardStyle(styleKey) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = tc
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dark mode toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val nextMode = when (settings.themeMode) {
                                "dark" -> "light"
                                "light" -> "system"
                                else -> "dark"
                            }
                            settingsManager.setThemeMode(nextMode)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = DesktopStrings.get("dark_mode", isFarsi) + " (${settings.themeMode.uppercase()})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = settings.themeMode == "dark",
                        onCheckedChange = {
                            settingsManager.setThemeMode(if (it) "dark" else "light")
                        }
                    )
                }
            }
            }
        }
    }
}

private data class FetchResult(
    val servers: List<String>,
    val upload: Long? = null,
    val download: Long? = null,
    val total: Long? = null,
    val expire: Long? = null,
    val fetchError: String? = null
)

private data class SubscriptionUserInfo(
    val upload: Long?,
    val download: Long?,
    val total: Long?,
    val expire: Long?
)

private fun parseSubscriptionUserInfo(header: String?): SubscriptionUserInfo? {
    if (header == null) return null
    var upload: Long? = null
    var download: Long? = null
    var total: Long? = null
    var expire: Long? = null
    header.split(Regex("[;,]")).forEach { part ->
        val pair = if (part.contains("=")) part.split("=") else part.split(":")
        if (pair.size == 2) {
            val key = pair[0].trim().lowercase()
            val value = pair[1].trim().toLongOrNull()
            when (key) {
                "upload" -> upload = value
                "download" -> download = value
                "total" -> total = value
                "expire" -> expire = value
            }
        }
    }
    return SubscriptionUserInfo(upload, download, total, expire)
}

private data class CurlResult(val body: String, val headers: Map<String, List<String>>)

private fun fetchWithSystemCurl(urlStr: String): CurlResult? {
    try {
        val process = ProcessBuilder(
            "curl",
            "-i",
            "-s",
            "-L",
            "-A", "sing-box/1.10.0",
            "--connect-timeout", "15",
            urlStr
        ).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)
        if (process.exitValue() == 0 && text.trim().isNotEmpty()) {
            val responses = text.split(Regex("(?m)^HTTP/\\d+\\.\\d+\\s+\\d+"))
            val lastResponse = responses.lastOrNull() ?: text
            val parts = lastResponse.split(Regex("(\\r?\\n){2}"), 2)
            if (parts.size == 2) {
                val headerLines = parts[0].split("\n").map { it.trim() }
                val body = parts[1]
                val headers = mutableMapOf<String, List<String>>()
                headerLines.forEach { line ->
                    val colonIdx = line.indexOf(":")
                    if (colonIdx > 0) {
                        val key = line.substring(0, colonIdx).trim().lowercase()
                        val value = line.substring(colonIdx + 1).trim()
                        headers[key] = listOf(value)
                    }
                }
                return CurlResult(body, headers)
            } else {
                return CurlResult(text, emptyMap())
            }
        }
    } catch (e: Exception) {
        try {
            val process = ProcessBuilder(
                "powershell",
                "-Command",
                "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (Invoke-WebRequest -Uri '$urlStr' -UserAgent 'sing-box/1.10.0' -UseBasicParsing).Content"
            ).start()
            val text = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS)
            if (process.exitValue() == 0 && text.trim().isNotEmpty()) {
                return CurlResult(text, emptyMap())
            }
        } catch (ex: Exception) {
            // Ignore
        }
    }
    return null
}

private suspend fun fetchSubscription(urlStr: String): FetchResult = withContext(Dispatchers.IO) {
    var rawBody: String? = null
    var headersMap: Map<String, List<String>>? = null
    var isHtmlResponse = false
    var htmlPreview = ""
    
    // 1. Try Java HttpURLConnection first
    var connection: java.net.HttpURLConnection? = null
    try {
        var currentUrl = urlStr.trim()
        var redirectCount = 0
        var responseCode = 0
        
        while (redirectCount < 5) {
            val url = java.net.URL(currentUrl)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "sing-box/1.10.0")
            connection.connect()
            
            responseCode = connection.responseCode
            if (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                val newUrl = connection.getHeaderField("Location")
                if (newUrl != null && newUrl.isNotEmpty()) {
                    currentUrl = newUrl
                    redirectCount++
                    continue
                }
            }
            break
        }
        
        if (responseCode == 200) {
            val rawData = connection!!.inputStream.bufferedReader().use { it.readText() }
            val trimmed = rawData.trim()
            if (trimmed.lowercase().startsWith("<!doctype html") || trimmed.lowercase().startsWith("<html") || trimmed.lowercase().contains("<head>")) {
                isHtmlResponse = true
                htmlPreview = if (trimmed.length > 150) trimmed.take(150) + "..." else trimmed
            } else {
                rawBody = rawData
                headersMap = connection.headerFields
            }
        }
    } catch (e: Exception) {
        // Fail over silently
    } finally {
        connection?.disconnect()
    }
    
    // 2. If Java fetch failed, was blocked (HTML), or threw an exception, fall back to curl
    if (rawBody == null || isHtmlResponse) {
        val curlResult = fetchWithSystemCurl(urlStr)
        if (curlResult != null) {
            val trimmed = curlResult.body.trim()
            if (!(trimmed.lowercase().startsWith("<!doctype html") || trimmed.lowercase().startsWith("<html") || trimmed.lowercase().contains("<head>"))) {
                rawBody = curlResult.body
                headersMap = curlResult.headers
                isHtmlResponse = false // Reset since curl got actual configs!
            } else {
                isHtmlResponse = true
                htmlPreview = if (trimmed.length > 150) trimmed.take(150) + "..." else trimmed
            }
        }
    }
    
    // 3. Process the raw body if we successfully obtained it
    if (rawBody != null && !isHtmlResponse) {
        val trimmed = rawBody.trim()
        val decodedText = try {
            val cleanB64 = trimmed.replace("\r", "").replace("\n", "").replace(" ", "")
            val decodedBytes = try {
                java.util.Base64.getDecoder().decode(cleanB64)
            } catch (e: Exception) {
                java.util.Base64.getUrlDecoder().decode(cleanB64)
            }
            String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8)
        } catch (e: Exception) {
            tryBase64Decode(rawBody) ?: rawBody
        }
        
        val servers = decodedText.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            
        var userInfoHeader: String? = null
        if (headersMap != null) {
            for ((key, values) in headersMap) {
                if (key != null && (key.equals("subscription-userinfo", ignoreCase = true) || key.equals("x-user-info", ignoreCase = true))) {
                    userInfoHeader = values.firstOrNull()
                    break
                }
            }
        }
        val parsedInfo = parseSubscriptionUserInfo(userInfoHeader)
        FetchResult(
            servers = servers,
            upload = parsedInfo?.upload,
            download = parsedInfo?.download,
            total = parsedInfo?.total,
            expire = parsedInfo?.expire
        )
    } else {
        if (isHtmlResponse) {
            FetchResult(
                servers = emptyList(),
                fetchError = "Server returned HTML instead of configs: ${htmlPreview.replace("\n", " ").trim()}"
            )
        } else {
            FetchResult(
                servers = emptyList(),
                fetchError = "Failed to retrieve subscription configs. Check connection or URL."
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatExpiry(expirySecs: Long): String {
    if (expirySecs <= 0) return ""
    val ms = expirySecs * 1000L
    val date = java.util.Date(ms)
    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
    return format.format(date)
}

fun getProxyName(link: String): String {
    val hashIdx = link.indexOf("#")
    return if (hashIdx >= 0) {
        try {
            URLDecoder.decode(link.substring(hashIdx + 1), "UTF-8")
        } catch (e: Exception) {
            link.substring(hashIdx + 1)
        }
    } else {
        try {
            val rest = link.substringAfter("://")
            val host = rest.substringAfter("@").substringBefore(":")
            val scheme = link.substringBefore("://").uppercase()
            "$scheme ($host)"
        } catch (e: Exception) {
            "Unnamed Profile"
        }
    }
}
