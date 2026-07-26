package com.hambalapps.chameleon.desktop.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets

suspend fun measurePingDelay(host: String, port: Int): Int = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 2000)
        }
        (System.currentTimeMillis() - startTime).toInt()
    } catch (e: Exception) {
        -1
    }
}

/**
 * Safely decodes Base64 strings recursively (up to 3 levels for double-base64 subscriptions)
 * without mangling plain text multiline URL lists or JSON configs.
 */
fun tryBase64Decode(str: String): String? {
    var current = str.trim()
    if (current.isEmpty()) return null

    // If string already contains URI scheme, return intact
    if (current.contains("://")) {
        return current
    }

    for (pass in 0 until 3) {
        if (current.contains("://")) return current
        val clean = current.replace("\r", "").replace("\n", "").replace(" ", "").replace("\t", "")
        if (clean.length < 4) break

        var decodedSuccess = false
        val padded = when (clean.length % 4) {
            2 -> "$clean=="
            3 -> "$clean="
            else -> clean
        }

        for (decoder in listOf(java.util.Base64.getDecoder(), java.util.Base64.getUrlDecoder())) {
            try {
                val decodedBytes = decoder.decode(padded)
                val decodedStr = String(decodedBytes, StandardCharsets.UTF_8).trim()
                if (decodedStr.isNotEmpty() && (decodedStr.contains("://") || decodedStr.contains("\n") || decodedStr.contains("#"))) {
                    current = decodedStr
                    decodedSuccess = true
                    break
                }
            } catch (e: Exception) {}
        }
        if (!decodedSuccess) break
    }

    return if (current.contains("://") || current.contains("\n") || current.startsWith("{")) current else null
}

fun getHostAndPortFromLink(link: String): Pair<String, Int>? {
    try {
        val trimmed = link.trim()
        val rest = if (trimmed.contains("#")) trimmed.substring(0, trimmed.indexOf("#")) else trimmed
        val schemeIdx = rest.indexOf("://")
        val scheme = if (schemeIdx >= 0) rest.substring(0, schemeIdx).lowercase() else ""
        val content = if (schemeIdx >= 0) rest.substring(schemeIdx + 3) else rest
        val queryIdx = content.indexOf("?")
        val mainPart = if (queryIdx >= 0) content.substring(0, queryIdx) else content

        if (scheme == "vmess") {
            val decoded = tryBase64Decode(mainPart)
            if (decoded != null && decoded.startsWith("{")) {
                val vmessJson = JSONObject(decoded)
                val add = vmessJson.optString("add")
                val portVal = vmessJson.opt("port")
                val port = when (portVal) {
                    is Number -> portVal.toInt()
                    is String -> portVal.toIntOrNull() ?: 443
                    else -> 443
                }
                if (add.isNotEmpty()) {
                    return Pair(add, port)
                }
            }
        }
        
        val serverPart = if (mainPart.contains("@")) mainPart.substring(mainPart.indexOf("@") + 1) else mainPart
        val colonIdx = serverPart.lastIndexOf(":")
        val host = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
        val portStr = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
        val port = portStr.toIntOrNull() ?: 443
        return Pair(host, port)
    } catch (e: Exception) {
        return null
    }
}
