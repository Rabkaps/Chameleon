package com.hambalapps.chameleon.desktop.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hambalapps.chameleon.desktop.vpn.SingboxManager

/**
 * Material 3 Expressive Real-time Logs Console Screen.
 */
@Composable
fun LogsConsoleScreen() {
    val logs by SingboxManager.vpnLogs.collectAsState()
    var searchFilter by remember { mutableStateOf("") }

    val logLines = remember(logs, searchFilter) {
        val lines = logs.lines()
        if (searchFilter.isEmpty()) lines
        else lines.filter { it.contains(searchFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Logs Console",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Live sing-box-extended process stdout/stderr output",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { SingboxManager.clearLogs() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Keyword Field
        OutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            placeholder = { Text("Filter logs by keyword...") },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Terminal Log Container
        Surface(
            color = Color(0xFF0D1117),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxSize()
        ) {
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(logLines) { line ->
                        val lineColor = when {
                            line.contains("ERROR", ignoreCase = true) || line.contains("CRITICAL", ignoreCase = true) -> Color(0xFFEF4444)
                            line.contains("WARN", ignoreCase = true) -> Color(0xFFF59E0B)
                            line.contains("Connected", ignoreCase = true) -> Color(0xFF10B981)
                            else -> Color(0xFFC9D1D9)
                        }

                        Text(
                            text = line,
                            color = lineColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
