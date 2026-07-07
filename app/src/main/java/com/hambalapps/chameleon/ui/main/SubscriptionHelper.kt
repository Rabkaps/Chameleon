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
            val servers = decoded.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            var userInfoHeader: String? = null
            for ((key, values) in connection.headerFields) {
                if (key != null && (key.equals("subscription-userinfo", ignoreCase = true) || 
                    key.equals("x-user-info", ignoreCase = true))) {
                    userInfoHeader = values.firstOrNull()
                    break
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
