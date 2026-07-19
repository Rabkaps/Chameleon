package com.hambalapps.chameleon.ui.main

import com.hambalapps.chameleon.vpn.tryBase64Decode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class FetchResult(
    val servers: List<String>,
    val upload: Long? = null,
    val download: Long? = null,
    val total: Long? = null,
    val expire: Long? = null
)

internal data class SubscriptionUserInfo(
    val upload: Long?,
    val download: Long?,
    val total: Long?,
    val expire: Long?
)

internal fun parseSubscriptionUserInfo(header: String?): SubscriptionUserInfo? {
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

internal fun parseDataSize(text: String): Long? {
    val clean = text.trim()
    val regex = Regex("""(\d+(?:\.\d+)?)\s*(TB|GB|MB|KB|B)""", RegexOption.IGNORE_CASE)
    val match = regex.find(clean)
    if (match != null) {
        val number = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].uppercase()
        return when {
            unit.startsWith("TB") -> (number * 1024L * 1024L * 1024L * 1024L).toLong()
            unit.startsWith("GB") -> (number * 1024L * 1024L * 1024L).toLong()
            unit.startsWith("MB") -> (number * 1024L * 1024L).toLong()
            unit.startsWith("KB") -> (number * 1024L).toLong()
            unit.startsWith("B") -> number.toLong()
            else -> (number * 1024L * 1024L * 1024L).toLong()
        }
    }
    // Fallback: original implementation
    val numberPart = clean.uppercase().takeWhile { it.isDigit() || it == '.' }
    val number = numberPart.toDoubleOrNull() ?: return null
    val unit = clean.uppercase().drop(numberPart.length).trim()
    return when {
        unit.startsWith("TB") -> (number * 1024L * 1024L * 1024L * 1024L).toLong()
        unit.startsWith("GB") -> (number * 1024L * 1024L * 1024L).toLong()
        unit.startsWith("MB") -> (number * 1024L * 1024L).toLong()
        unit.startsWith("KB") -> (number * 1024L).toLong()
        unit.startsWith("B") -> number.toLong()
        else -> (number * 1024L * 1024L * 1024L).toLong() // Default to GB
    }
}

internal fun parseDateString(text: String): Long? {
    val clean = text.trim()
    val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy")
    for (fmt in formats) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            val date = sdf.parse(clean)
            if (date != null) {
                return date.time / 1000L
            }
        } catch (e: Exception) {}
    }
    return clean.toLongOrNull()
}

internal suspend fun fetchSubscription(urlStr: String): FetchResult = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "sing-box/1.9.0")
        connection.connect()
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val rawData = connection.inputStream.bufferedReader().use { it.readText() }
            val decoded = tryBase64Decode(rawData) ?: rawData
            val lines = decoded.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            
            var userInfoHeader: String? = null
            for ((key, values) in connection.headerFields) {
                if (key != null) {
                    val lowerKey = key.lowercase()
                    if (lowerKey.contains("userinfo") || lowerKey.contains("user-info")) {
                        userInfoHeader = values.firstOrNull()
                        break
                    }
                }
            }
            
            val parsedInfo = parseSubscriptionUserInfo(userInfoHeader)
            var uploadVal: Long? = parsedInfo?.upload
            var downloadVal: Long? = parsedInfo?.download
            var totalVal: Long? = parsedInfo?.total
            var expireVal: Long? = parsedInfo?.expire
            
            val servers = mutableListOf<String>()
            for (line in lines) {
                if (line.isEmpty()) continue
                
                // 1. Parse comments/metadata lines (starts with # or //)
                if (line.startsWith("#") || line.startsWith("//")) {
                    val content = line.substring(if (line.startsWith("#")) 1 else 2).trim()
                    content.split(Regex("[;,]")).forEach { part ->
                        val pair = if (part.contains("=")) part.split("=") else part.split(":")
                        if (pair.size == 2) {
                            val key = pair[0].trim().lowercase()
                            val valueStr = pair[1].trim()
                            when (key) {
                                "upload" -> uploadVal = valueStr.toLongOrNull() ?: parseDataSize(valueStr) ?: uploadVal
                                "download" -> downloadVal = valueStr.toLongOrNull() ?: parseDataSize(valueStr) ?: downloadVal
                                "total" -> totalVal = valueStr.toLongOrNull() ?: parseDataSize(valueStr) ?: totalVal
                                "expire" -> expireVal = parseDateString(valueStr) ?: expireVal
                            }
                        }
                    }
                    continue
                }
                
                // 2. Parse dummy node comments/remarks (e.g. 'vmess://...#Traffic: 24.3 GB / 100 GB')
                if (line.contains("#")) {
                    val parts = line.split("#")
                    val remark = try { java.net.URLDecoder.decode(parts.last(), "UTF-8") } catch (e: Exception) { parts.last() }
                    val remarkLower = remark.lowercase()
                    
                    val isQuotaRemark = remarkLower.contains("traffic") || 
                                        remarkLower.contains("remaining") || 
                                        remarkLower.contains("expiry") || 
                                        remarkLower.contains("limit") || 
                                        remarkLower.contains("expire") || 
                                        remarkLower.contains("quota") ||
                                        remarkLower.contains("gb") || 
                                        remarkLower.contains("mb") ||
                                        remarkLower.contains("tb") ||
                                        remarkLower.contains("حجم") ||
                                        remarkLower.contains("باقیمانده") ||
                                        remarkLower.contains("باقی") ||
                                        remarkLower.contains("مصرف") ||
                                        remarkLower.contains("انقضا") ||
                                        remarkLower.contains("ترافیک")
                                  
                    if (isQuotaRemark) {
                        if (remarkLower.contains("/") || remarkLower.contains("traffic") || remarkLower.contains("حجم") || remarkLower.contains("مصرف") || remarkLower.contains("ترافیک")) {
                            val slashParts = remark.split("/")
                            if (slashParts.size == 2) {
                                downloadVal = parseDataSize(slashParts[0]) ?: downloadVal
                                totalVal = parseDataSize(slashParts[1]) ?: totalVal
                            }
                        }
                        if (remarkLower.contains("remaining") || remarkLower.contains("باقیمانده") || remarkLower.contains("باقی")) {
                            val remainingBytes = parseDataSize(remark)
                            if (remainingBytes != null) {
                                if (totalVal != null) {
                                    downloadVal = totalVal - remainingBytes
                                } else {
                                    totalVal = remainingBytes
                                    downloadVal = 0L
                                }
                            }
                        }
                        if (remarkLower.contains("expiry") || remarkLower.contains("expire") || remarkLower.contains("انقضا") || remarkLower.contains("تاریخ")) {
                            val dateRegex = Regex("""(\d{4}[-/]\d{2}[-/]\d{2})|(\d{2}[-/]\d{2}[-/]\d{4})""")
                            val dateMatch = dateRegex.find(remark)
                            if (dateMatch != null) {
                                expireVal = parseDateString(dateMatch.value) ?: expireVal
                            } else {
                                val daysRegex = Regex("""(\d+)\s*(day|days|روز)""", RegexOption.IGNORE_CASE)
                                val daysMatch = daysRegex.find(remarkLower)
                                if (daysMatch != null) {
                                    val days = daysMatch.groupValues[1].toLongOrNull()
                                    if (days != null) {
                                        expireVal = (System.currentTimeMillis() / 1000L) + (days * 24 * 3600)
                                    }
                                }
                            }
                        }
                        if (isDummyHost(line)) {
                            continue // Exclude dummy config from connection profile list!
                        }
                    }
                }
                
                val lowerLine = line.lowercase()
                val hasValidScheme = lowerLine.startsWith("vless://") ||
                                     lowerLine.startsWith("vmess://") ||
                                     lowerLine.startsWith("ss://") ||
                                     lowerLine.startsWith("ssr://") ||
                                     lowerLine.startsWith("trojan://") ||
                                     lowerLine.startsWith("tuic://") ||
                                     lowerLine.startsWith("hysteria://") ||
                                     lowerLine.startsWith("hysteria2://") ||
                                     lowerLine.startsWith("hy2://") ||
                                     lowerLine.startsWith("shadowsocks://") ||
                                     lowerLine.startsWith("wg://") ||
                                     lowerLine.startsWith("wireguard://") ||
                                     lowerLine.startsWith("awg://") ||
                                     lowerLine.startsWith("amneziawg://") ||
                                     lowerLine.startsWith("socks://") ||
                                     lowerLine.startsWith("socks5://") ||
                                     lowerLine.startsWith("http://") ||
                                     lowerLine.startsWith("https://") ||
                                     lowerLine.startsWith("shadowtls://") ||
                                     lowerLine.startsWith("snell://") ||
                                     lowerLine.startsWith("mieru://") ||
                                     lowerLine.startsWith("masque://")
                
                if (hasValidScheme) {
                    servers.add(line)
                }
            }
            
            FetchResult(
                servers = servers,
                upload = uploadVal,
                download = downloadVal,
                total = totalVal,
                expire = expireVal
            )
        } else {
            FetchResult(emptyList())
        }
    } catch (e: Exception) {
        FetchResult(emptyList())
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

internal fun formatExpiry(expirySecs: Long): String {
    if (expirySecs <= 0) return ""
    val ms = expirySecs * 1000L
    val date = Date(ms)
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return format.format(date)
}

private fun isDummyHost(line: String): Boolean {
    try {
        val trimmed = line.trim()
        val schemeIdx = trimmed.indexOf("://")
        if (schemeIdx < 0) return false
        val scheme = trimmed.substring(0, schemeIdx).lowercase()
        val rest = trimmed.substring(schemeIdx + 3)
        val mainPart = rest.substringBefore("?").substringBefore("#")
        
        if (scheme == "vmess") {
            val decoded = tryBase64Decode(mainPart) ?: return false
            if (decoded.startsWith("{")) {
                val json = org.json.JSONObject(decoded)
                val add = json.optString("add") ?: ""
                return add == "127.0.0.1" || add == "0.0.0.0" || add == "localhost" || add.isEmpty()
            }
        }
        
        val atIdx = mainPart.indexOf("@")
        val serverPart = if (atIdx >= 0) mainPart.substring(atIdx + 1) else mainPart
        val host = serverPart.substringBefore(":")
        return host == "127.0.0.1" || host == "0.0.0.0" || host == "localhost" || host.isEmpty()
    } catch (e: Exception) {
        return false
    }
}
