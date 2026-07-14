package com.hambalapps.chameleon.desktop.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.data.SettingsManager
import com.hambalapps.chameleon.desktop.data.UserSettings
import kotlinx.coroutines.launch

private val ExpressiveCardShape = RoundedCornerShape(topStart = 24.dp, bottomEnd = 24.dp, topEnd = 8.dp, bottomStart = 8.dp)
private val ExpressiveButtonShape = RoundedCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp)
private val ExpressiveChipShape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 2.dp, bottomStart = 2.dp)

data class PresetApp(
    val name: String,
    val exe: String,
    val category: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelingScreen(
    settings: UserSettings,
    settingsManager: SettingsManager
) {
    val isFarsi = settings.isFarsi
    val scope = rememberCoroutineScope()

    val isEnabled = settings.splitTunnelingEnabled
    val mode = settings.splitTunnelingMode
    val selectedApps = settings.splitTunnelingApps

    var customExeInput by remember { mutableStateOf("") }

    val presetApps = remember {
        listOf(
            PresetApp("Google Chrome", "chrome.exe", "Browsers"),
            PresetApp("Microsoft Edge", "msedge.exe", "Browsers"),
            PresetApp("Mozilla Firefox", "firefox.exe", "Browsers"),
            PresetApp("Brave Browser", "brave.exe", "Browsers"),
            PresetApp("Telegram Desktop", "telegram.exe", "Social"),
            PresetApp("Discord", "discord.exe", "Social"),
            PresetApp("WhatsApp", "WhatsApp.exe", "Social"),
            PresetApp("Spotify", "spotify.exe", "Media"),
            PresetApp("VLC Player", "vlc.exe", "Media"),
            PresetApp("Steam Client", "steam.exe", "Gaming"),
            PresetApp("Epic Games Launcher", "EpicGamesLauncher.exe", "Gaming"),
            PresetApp("Git Version Control", "git.exe", "DevTools"),
            PresetApp("Curl CLI", "curl.exe", "DevTools"),
            PresetApp("Command Prompt", "cmd.exe", "System"),
            PresetApp("PowerShell", "powershell.exe", "System")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = if (isFarsi) "تونل‌سازی تقسیم‌شده (Split Tunneling)" else "Split Tunneling",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Enable Toggle Card
        Card(
            shape = ExpressiveCardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), ExpressiveCardShape)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { settingsManager.setSplitTunnelingEnabled(!isEnabled) }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFarsi) "فعال‌سازی تونل‌سازی تقسیم‌شده" else "Enable Split Tunneling",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isFarsi)
                            "ترافیک برنامه‌های خاصی را دور بزنید (مستقیم) یا فقط برنامه‌های مشخصی را از فیلترشکن عبور دهید."
                        else
                            "Bypass specific apps from the VPN (Direct) or only route designated apps through the VPN.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { settingsManager.setSplitTunnelingEnabled(it) }
                )
            }
        }

        AnimatedVisibility(
            visible = isEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mode Selector
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), ExpressiveCardShape)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isFarsi) "حالت اجرای تونل‌سازی" else "Split Tunneling Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val bypassSelected = mode == "bypass"
                            FilterChip(
                                selected = bypassSelected,
                                onClick = { settingsManager.setSplitTunnelingMode("bypass") },
                                label = {
                                    Text(
                                        text = if (isFarsi) "دور زدن VPN (برنامه‌های منتخب مستقیم)" else "Bypass VPN (Selected Apps Direct)",
                                        fontSize = 12.sp
                                    )
                                },
                                shape = ExpressiveButtonShape,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !bypassSelected,
                                onClick = { settingsManager.setSplitTunnelingMode("proxy") },
                                label = {
                                    Text(
                                        text = if (isFarsi) "عبور از VPN (فقط برنامه‌های منتخب)" else "Route VPN (Only Selected Apps)",
                                        fontSize = 12.sp
                                    )
                                },
                                shape = ExpressiveButtonShape,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Add Custom Executable (.exe)
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), ExpressiveCardShape)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = customExeInput,
                            onValueChange = { customExeInput = it },
                            placeholder = { Text(if (isFarsi) "نام پروسه دلخواه (مثال: idman.exe)" else "Custom process name (e.g. idman.exe)") },
                            modifier = Modifier.weight(1f),
                            shape = ExpressiveButtonShape,
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val clean = customExeInput.trim()
                                if (clean.isNotEmpty()) {
                                    val formatted = if (clean.endsWith(".exe", ignoreCase = true)) clean else "$clean.exe"
                                    settingsManager.setSplitTunnelingApps(selectedApps + formatted)
                                    customExeInput = ""
                                }
                            },
                            shape = ExpressiveButtonShape
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFarsi) "افزودن" else "Add")
                        }
                    }
                }

                // Preset Apps Checkboxes
                Card(
                    shape = ExpressiveCardShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), ExpressiveCardShape)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isFarsi) "برنامه‌های پیشنهادی ویندوز" else "Popular Windows Applications",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Render them in a flowing wrap row or layout
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            presetApps.forEach { app ->
                                val isChecked = selectedApps.contains(app.exe)
                                val bg = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                val contentColor = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                
                                Row(
                                    modifier = Modifier
                                        .clip(ExpressiveChipShape)
                                        .background(bg)
                                        .clickable {
                                            val updated = if (isChecked) selectedApps - app.exe else selectedApps + app.exe
                                            settingsManager.setSplitTunnelingApps(updated)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            val updated = if (checked == true) selectedApps + app.exe else selectedApps - app.exe
                                            settingsManager.setSplitTunnelingApps(updated)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.colorScheme.primary,
                                            uncheckedColor = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.size(20.dp).padding(end = 4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = app.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Currently Split-Tunneled Apps List
                if (selectedApps.isNotEmpty()) {
                    Text(
                        text = if (isFarsi) "برنامه‌های سفارشی اضافه شده" else "Active Split-Tunnel App Rules",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedApps.forEach { appName ->
                            val isPreset = presetApps.any { it.exe == appName }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(50))
                                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPreset) Icons.Default.AppShortcut else Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = appName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                settingsManager.setSplitTunnelingApps(selectedApps - appName)
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
