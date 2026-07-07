package com.hambalapps.chameleon.ui.main

import com.hambalapps.chameleon.vpn.tryBase64Decode
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

data class ServerItem(
    val id: String,
    val link: String,
    val name: String,
    val type: String,
    val transport: String
)

fun getTransportType(link: String): String {
    val scheme = link.substringBefore("://").lowercase()
    if (scheme == "vmess") {
        val base64Part = link.substringAfter("vmess://")
        val decoded = tryBase64Decode(base64Part)
        if (decoded != null && decoded.startsWith("{")) {
            try {
                val json = JSONObject(decoded)
                val net = json.optString("net").lowercase()
                if (net.isNotEmpty()) {
                    return when (net) {
                        "tcp" -> "TCP"
                        "ws" -> "WebSocket"
                        "h2" -> "HTTP/2"
                        "http" -> "HTTP"
                        "grpc" -> "gRPC"
                        "httpupgrade" -> "HTTPUpgrade"
                        "kcp" -> "mKCP"
                        "mkcp" -> "mKCP"
                        "quic" -> "QUIC"
                        else -> net.uppercase()
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    } else {
        try {
            val uri = URI(link)
            val query = uri.rawQuery
            if (query != null) {
                val params = query.split("&").associate {
                    val parts = it.split("=")
                    val key = parts[0].lowercase()
                    val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                    key to value
                }
                
                val type = params["type"]?.lowercase()
                if (type != null && type.isNotEmpty()) {
                    return when (type) {
                        "tcp" -> {
                            val headerType = params["headerType"] ?: params["header_type"]
                            if (headerType == "http") "HTTP" else "TCP"
                        }
                        "ws" -> "WebSocket"
                        "grpc" -> "gRPC"
                        "httpupgrade" -> "HTTPUpgrade"
                        "xhttp" -> "xHTTP"
                        "kcp" -> "mKCP"
                        "mkcp" -> "mKCP"
                        "quic" -> "QUIC"
                        else -> type.uppercase()
                    }
                }
                val plugin = params["plugin"]
                if (plugin != null && plugin.isNotEmpty()) {
                    return plugin.substringBefore(";").uppercase()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
    }
    return when (scheme) {
        "hysteria", "hysteria2", "hy2" -> "Hysteria"
        "tuic" -> "TUIC"
        "ssh" -> "SSH"
        else -> "TCP"
    }
}
