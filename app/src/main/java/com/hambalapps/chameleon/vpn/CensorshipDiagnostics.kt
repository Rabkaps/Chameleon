package com.hambalapps.chameleon.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

enum class CensorshipDiagnosticResult {
    OK,                 // Connection successful
    LOCAL_OFFLINE,      // Local network offline
    DNS_POISONED,       // Host resolved to DNS sinkhole or loopback
    IP_BLACKBOXED,      // TCP SYN dropped by firewall / network block
    DPI_SNI_BLOCKED,    // TCP connected, but sending TLS ClientHello triggered ECONNRESET or block
    SERVER_DOWN,        // Connection refused or backend handshake timed out
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

    private val trustAllSslSocketFactory: SSLSocketFactory by lazy {
        val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAll, java.security.SecureRandom())
        sslContext.socketFactory
    }

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

        val isTlsPort = port == 443 || port == 8443 || port == 2053 || port == 2083 || port == 2087 || port == 2096
        val isTlsHandshakeTarget = isTlsPort || (sni.isNotEmpty() && sni != host)

        if (isTlsHandshakeTarget) {
            // 2. Test TCP connect + TLS Handshake (verifies entire tunnel from domestic relay to foreign backend)
            try {
                Socket().use { plainSocket ->
                    plainSocket.connect(InetSocketAddress(resolvedIp, port), 2500)
                    val targetSni = if (sni.isNotEmpty()) sni else host
                    (trustAllSslSocketFactory.createSocket(plainSocket, targetSni, port, true) as SSLSocket).use { sslSocket ->
                        sslSocket.soTimeout = 2500
                        sslSocket.startHandshake()
                    }
                }
            } catch (e: java.net.ConnectException) {
                return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.SERVER_DOWN, e.localizedMessage ?: "Connection Refused")
            } catch (e: java.net.SocketTimeoutException) {
                return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.SERVER_DOWN, "Backend/Tunnel Handshake Timed Out")
            } catch (e: java.io.IOException) {
                val msg = e.message ?: ""
                if (msg.contains("reset", ignoreCase = true) || msg.contains("broken pipe", ignoreCase = true) || msg.contains("RST", ignoreCase = true)) {
                    return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DPI_SNI_BLOCKED, "DPI Injected TCP RST during TLS Handshake")
                } else {
                    return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DPI_SNI_BLOCKED, "TLS Handshake Failed ($msg)")
                }
            } catch (e: Exception) {
                return@withContext DetailedPingResult(-1, CensorshipDiagnosticResult.DPI_SNI_BLOCKED, e.localizedMessage ?: "TLS Handshake Error")
            }
        } else {
            // 2. Test Plain TCP Socket Connect for non-TLS ports
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
        }

        val totalDelay = (System.currentTimeMillis() - startTime).toInt()
        DetailedPingResult(totalDelay, CensorshipDiagnosticResult.OK, "OK")
    }
}
