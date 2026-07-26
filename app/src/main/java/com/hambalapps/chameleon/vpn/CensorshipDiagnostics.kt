package com.hambalapps.chameleon.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocketFactory

enum class CensorshipDiagnosticResult {
    OK,                 // Connection successful
    LOCAL_OFFLINE,      // Local network offline
    DNS_POISONED,       // Host resolved to DNS sinkhole or loopback
    IP_BLACKBOXED,      // TCP SYN dropped by firewall / network block
    DPI_SNI_BLOCKED,    // TCP connected, but sending TLS ClientHello triggered ECONNRESET
    SERVER_DOWN,        // Connection refused before TLS
    UNKNOWN_ERROR       // Generic timeout or IO failure
}

data class DetailedPingResult(
    val delayMs: Int,
    val status: CensorshipDiagnosticResult,
    val detailMessage: String = ""
)

object CensorshipDiagnostics {

    // Known Iranian DNS sinkhole / local loopback IPs
    private val SINKHOLE_IPS = setOf(
        "10.10.34.34", "10.10.34.35", "127.0.0.1", "0.0.0.0", "::1"
    )

    suspend fun diagnoseConnection(host: String, port: Int, sni: String = host): DetailedPingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Check DNS Resolution
        val resolvedIp = try {
            val addr = InetAddress.getByName(host)
            addr.hostAddress ?: ""
        } catch (e: Exception) {
            ""
        }

        if (resolvedIp.isEmpty()) {
            return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DNS_POISONED, "DNS resolution failed")
        }

        if (SINKHOLE_IPS.contains(resolvedIp) || resolvedIp.startsWith("10.10.")) {
            return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DNS_POISONED, "DNS Sinkhole ($resolvedIp)")
        }

        // 2. Test Plain TCP Socket Connect
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(resolvedIp, port), 2500)
            }
        } catch (e: java.net.ConnectException) {
            return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.SERVER_DOWN, e.localizedMessage ?: "Connection Refused")
        } catch (e: java.net.SocketTimeoutException) {
            return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.IP_BLACKBOXED, "TCP SYN Timed Out (IP Dropped)")
        } catch (e: Exception) {
            return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.IP_BLACKBOXED, e.localizedMessage ?: "TCP Connect Failed")
        }

        val tcpDelay = (System.currentTimeMillis() - startTime).toInt()

        // 3. Test TLS SNI Handshake (for HTTPS / SSL ports)
        if (port == 443 || port == 8443 || port == 2053 || port == 2083) {
            try {
                val sslFactory = SSLSocketFactory.getDefault()
                sslFactory.createSocket().use { sslSocket ->
                    sslSocket.connect(InetSocketAddress(resolvedIp, port), 2500)
                    (sslSocket as? javax.net.ssl.SSLSocket)?.apply {
                        soTimeout = 2500
                        startHandshake()
                    }
                }
            } catch (e: java.io.IOException) {
                val msg = e.message ?: ""
                if (msg.contains("reset", ignoreCase = true) || msg.contains("broken pipe", ignoreCase = true) || msg.contains("RST", ignoreCase = true) || msg.contains("Connection reset", ignoreCase = true)) {
                    return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DPI_SNI_BLOCKED, "DPI Injected TCP RST during TLS Handshake")
                } else {
                    return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DPI_SNI_BLOCKED, "TLS Handshake Failed ($msg)")
                }
            } catch (e: Exception) {
                return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DPI_SNI_BLOCKED, e.localizedMessage ?: "TLS Handshake Error")
            }
        }

        DetailedPingResult(tcpDelay, CensorshipDiagnosticResult.OK, "OK")
    }
}
