package com.hambalapps.chameleon.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hambalapps.chameleon.R
import com.hambalapps.chameleon.vpn.VpnServiceWrapper

@Composable
fun LogsConsole(
    isActive: Boolean,
    context: Context,
    cardStyle: String,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val rawVpnLogs by VpnServiceWrapper.vpnLogs.collectAsStateWithLifecycle()
    val vpnLogs = if (isActive) rawVpnLogs else ""
    val logLines = remember(vpnLogs) {
        if (vpnLogs.isEmpty()) {
            emptyList()
        } else {
            vpnLogs.split("\n")
        }
    }

    var selectedFilter by remember { mutableStateOf("ALL") }
    val filteredLogLines = remember(logLines, selectedFilter) {
        if (selectedFilter == "ALL") {
            logLines
        } else {
            logLines.filter { line ->
                when (selectedFilter) {
                    "INFO" -> line.contains("INFO", ignoreCase = true)
                    "WARN" -> line.contains("WARN", ignoreCase = true)
                    "ERROR" -> line.contains("ERROR", ignoreCase = true) || line.contains("FATAL", ignoreCase = true)
                    "DEBUG" -> line.contains("DEBUG", ignoreCase = true)
                    else -> true
                }
            }
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF33FF33).copy(alpha = 0.3f),
                shape = ExpressiveCardShape
            ),
        shape = ExpressiveCardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF00FF66),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.engine_logs),
                        color = Color(0xFF00FF66),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val logsToCopy = filteredLogLines.joinToString("\n")
                            val clip = ClipData.newPlainText("VPN Logs", logsToCopy)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.pressScaleEffect(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00FF66))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.copy), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { VpnServiceWrapper.clearLogs() },
                        modifier = Modifier.pressScaleEffect(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF3333))
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.clear), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF33FF33).copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            // Log level filter chips
            val filterLevels = listOf("ALL", "INFO", "WARN", "ERROR", "DEBUG")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterLevels.forEach { level ->
                    val isSelected = selectedFilter == level
                    val chipBg = if (isSelected) Color(0xFF00FF66).copy(alpha = 0.15f) else Color.Transparent
                    val chipText = if (isSelected) Color(0xFF00FF66) else Color.Gray
                    val chipBorder = if (isSelected) BorderStroke(1.dp, Color(0xFF00FF66)) else BorderStroke(1.dp, Color.DarkGray)
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = chipBg,
                        border = chipBorder,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedFilter = level }
                    ) {
                        Text(
                            text = level,
                            color = chipText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val listState = rememberLazyListState()
            LaunchedEffect(filteredLogLines.size) {
                if (filteredLogLines.isNotEmpty()) {
                    listState.scrollToItem(filteredLogLines.size - 1)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFF0A0A0A), shape = RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF33FF33).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredLogLines.isEmpty()) {
                        item {
                            Text(
                                text = if (logLines.isEmpty()) stringResource(R.string.logs_placeholder) else "No logs match this filter",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        itemsIndexed(filteredLogLines) { index, line ->
                            val textColor = when {
                                line.contains("WARN", ignoreCase = true) -> Color(0xFFFFCC00)
                                line.contains("ERROR", ignoreCase = true) || line.contains("FATAL", ignoreCase = true) -> Color(0xFFFF3333)
                                line.contains("INFO", ignoreCase = true) -> Color(0xFF00FF66)
                                else -> Color(0xFFE0E0E0)
                            }
                            Text(
                                text = line,
                                color = textColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
