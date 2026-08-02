package com.hambalapps.chameleon.vpn

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import android.content.Context
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap


suspend fun measurePingDelay(host: String, port: Int): Int = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 2000) // 2-second timeout
        }
        (System.currentTimeMillis() - startTime).toInt()
    } catch (e: Exception) {
        -1
    }
}

suspend fun measureDetailedPingDelay(host: String, port: Int): DetailedPingResult = CensorshipDiagnostics.diagnoseConnection(host, port)

fun tryBase64Decode(str: String): String? {
    val trimmed = str.trim()
    if (trimmed.isEmpty() || trimmed.startsWith("{") || trimmed.startsWith("[")) {
        return null
    }
    val cleaned = trimmed.replace("\r", "").replace("\n", "").replace(" ", "")
    try {
        val flags = listOf(
            Base64.DEFAULT,
            Base64.URL_SAFE,
            Base64.NO_PADDING,
            Base64.NO_WRAP,
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
        for (flag in flags) {
            try {
                val decoded = Base64.decode(cleaned, flag)
                val decodedStr = String(decoded, StandardCharsets.UTF_8).trim()
                if (decodedStr.isNotEmpty() && (
                    decodedStr.contains("://") ||
                    decodedStr.startsWith("{") ||
                    decodedStr.startsWith("[") ||
                    decodedStr.contains("proxies:")
                )) {
                    return decodedStr
                }
            } catch (e: Throwable) {
                // continue
            }
        }
    } catch (e: Throwable) {
        // Android Base64 not mocked in JVM unit tests
    }

    // Fallback for JVM unit testing environments
    try {
        val cleaned = str.trim().replace("\r", "").replace("\n", "").replace(" ", "").replace("-", "+").replace("_", "/")
        var toDecode = cleaned
        while (toDecode.length % 4 != 0) {
            toDecode += "="
        }
        val decoded = java.util.Base64.getDecoder().decode(toDecode)
        val decodedStr = String(decoded, StandardCharsets.UTF_8).trim()
        if (decodedStr.isNotEmpty()) {
            return decodedStr
        }
    } catch (e: Throwable) {
        // continue
    }
    return null
}

fun getHostAndPortFromLink(link: String): Pair<String, Int>? {
    try {
        val trimmed = link.trim()
        if (trimmed.startsWith("{")) {
            try {
                val json = JSONObject(trimmed)
                var server = json.optString("server").ifEmpty { json.optString("add") }
                var portVal = json.opt("server_port") ?: json.opt("port")
                var port = when (portVal) {
                    is Number -> portVal.toInt()
                    is String -> portVal.toIntOrNull() ?: 443
                    else -> 443
                }
                if (server.isEmpty()) {
                    val peers = json.optJSONArray("peers")
                    if (peers != null && peers.length() > 0) {
                        val peer = peers.optJSONObject(0)
                        val pAddr = peer?.optString("address")?.ifEmpty { peer.optString("server")?.ifEmpty { peer.optString("endpoint")?.substringBefore(":") } } ?: ""
                        val pPort = peer?.optInt("port", peer.optInt("server_port", 2408)) ?: 2408
                        if (pAddr.isNotEmpty()) {
                            server = pAddr
                            port = pPort
                        }
                    }
                }
                if (server.isEmpty()) {
                    val outbounds = json.optJSONArray("outbounds")
                    if (outbounds != null) {
                        for (i in 0 until outbounds.length()) {
                            val out = outbounds.optJSONObject(i) ?: continue
                            val s = out.optString("server").ifEmpty { out.optString("add") }
                            val pVal = out.opt("server_port") ?: out.opt("port")
                            val p = when (pVal) {
                                is Number -> pVal.toInt()
                                is String -> pVal.toIntOrNull() ?: 443
                                else -> 443
                            }
                            if (s.isNotEmpty()) {
                                server = s
                                port = p
                                break
                            }
                        }
                    }
                }
                if (server.isEmpty()) {
                    val endpoints = json.optJSONArray("endpoints")
                    if (endpoints != null) {
                        for (i in 0 until endpoints.length()) {
                            val ep = endpoints.optJSONObject(i) ?: continue
                            val epPeers = ep.optJSONArray("peers")
                            if (epPeers != null && epPeers.length() > 0) {
                                val peer = epPeers.optJSONObject(0)
                                val pAddr = peer?.optString("address")?.ifEmpty { peer.optString("server")?.ifEmpty { peer.optString("endpoint")?.substringBefore(":") } } ?: ""
                                val pPort = peer?.optInt("port", peer.optInt("server_port", 2408)) ?: 2408
                                if (pAddr.isNotEmpty()) {
                                    server = pAddr
                                    port = pPort
                                    break
                                }
                            }
                        }
                    }
                }
                if (server.isNotEmpty()) {
                    return Pair(server, port)
                }
            } catch (e: Exception) {}
        }

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

        if (scheme == "ss") {
            val atIdx = mainPart.indexOf("@")
            if (atIdx < 0) {
                val decoded = tryBase64Decode(mainPart)
                if (decoded != null && decoded.contains("@")) {
                    val parts = decoded.split("@")
                    val serverPart = parts[1]
                    val colonIdx = serverPart.lastIndexOf(":")
                    val h = if (colonIdx >= 0) serverPart.substring(0, colonIdx) else serverPart
                    val pStr = if (colonIdx >= 0) serverPart.substring(colonIdx + 1) else "443"
                    return Pair(h, pStr.toIntOrNull() ?: 443)
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

object ProxyNameResolver {
    private val nameCache = ConcurrentHashMap<String, String>()

    fun getProxyName(link: String, context: Context): String {
        val cached = nameCache[link]
        if (cached != null) return cached

        val trimmed = link.trim()
        if (trimmed.startsWith("{")) {
            try {
                val json = JSONObject(trimmed)
                val tag = json.optString("tag").ifEmpty { json.optString("name").ifEmpty { json.optString("remark").ifEmpty { json.optString("ps") } } }
                if (tag.isNotEmpty()) {
                    nameCache[link] = tag
                    return tag
                }
                val server = json.optString("server").ifEmpty { json.optString("add") }
                val type = json.optString("type", json.optString("protocol")).uppercase()
                if (server.isNotEmpty() && type.isNotEmpty()) {
                    val cleanHost = if (server.length > 20) server.take(20) + "..." else server
                    val name = "$type ($cleanHost)"
                    nameCache[link] = name
                    return name
                }
                val outbounds = json.optJSONArray("outbounds")
                if (outbounds != null && outbounds.length() > 0) {
                    val firstOut = outbounds.optJSONObject(0)
                    if (firstOut != null) {
                        val firstTag = firstOut.optString("tag").ifEmpty { firstOut.optString("name") }
                        val firstServer = firstOut.optString("server").ifEmpty { firstOut.optString("add") }
                        val firstType = firstOut.optString("type", firstOut.optString("protocol")).uppercase()
                        val name = if (firstTag.isNotEmpty()) {
                            "Sing-Box ($firstTag)"
                        } else if (firstServer.isNotEmpty() && firstType.isNotEmpty()) {
                            "$firstType ($firstServer)"
                        } else {
                            "Sing-Box Config"
                        }
                        nameCache[link] = name
                        return name
                    }
                }
            } catch (e: Exception) {}
        }

        val hashIdx = trimmed.indexOf("#")
        if (hashIdx >= 0) {
            val name = try {
                URLDecoder.decode(trimmed.substring(hashIdx + 1), "UTF-8")
            } catch (e: Exception) {
                trimmed.substring(hashIdx + 1)
            }
            nameCache[link] = name
            return name
        }

        if (trimmed.startsWith("vmess://")) {
            try {
                val mainPart = trimmed.substring(8)
                val decoded = tryBase64Decode(mainPart)
                if (decoded != null && decoded.startsWith("{")) {
                    val json = JSONObject(decoded)
                    val ps = json.optString("ps")
                    if (ps.isNotEmpty()) {
                        nameCache[link] = ps
                        return ps
                    }
                    val add = json.optString("add")
                    if (add.isNotEmpty()) {
                        val cleanHost = if (add.length > 20) add.take(20) + "..." else add
                        val name = "VMESS ($cleanHost)"
                        nameCache[link] = name
                        return name
                    }
                }
            } catch (e: Exception) {}
        }

        if (trimmed.startsWith("ss://")) {
            try {
                val mainPart = trimmed.substring(5).substringBefore("#").substringBefore("?")
                if (!mainPart.contains("@")) {
                    val decoded = tryBase64Decode(mainPart)
                    if (decoded != null && decoded.contains("@")) {
                        val host = decoded.substringAfter("@").substringBefore(":")
                        val cleanHost = if (host.length > 20) host.take(20) + "..." else host
                        val name = "SS ($cleanHost)"
                        nameCache[link] = name
                        return name
                    }
                }
            } catch (e: Exception) {}
        }

        val name = try {
            val schemeIdx = trimmed.indexOf("://")
            val scheme = if (schemeIdx >= 0) trimmed.substring(0, schemeIdx).uppercase() else "VPN"
            val rest = if (schemeIdx >= 0) trimmed.substring(schemeIdx + 3) else trimmed
            val host = if (rest.contains("@")) {
                rest.substringAfter("@").substringBefore(":")
            } else {
                rest.substringBefore(":")
            }
            val cleanHost = if (host.length > 20) host.take(20) + "..." else host
            "$scheme ($cleanHost)"
        } catch (e: Exception) {
            context.getString(com.hambalapps.chameleon.R.string.notif_unnamed)
        }
        nameCache[link] = name
        return name
    }
}

