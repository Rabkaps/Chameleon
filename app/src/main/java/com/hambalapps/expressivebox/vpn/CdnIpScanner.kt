package com.hambalapps.expressivebox.vpn

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

object CdnIpScanner {
    private val cleanIpCache = ConcurrentHashMap<String, Pair<String, Long>>() // preset -> Pair(IP, timestamp)
    private val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 minutes cache

    val CLOUDFLARE_IPS = listOf(
        "104.16.85.20", "104.16.86.20", "104.17.2.20", "104.18.26.240", "104.19.241.100",
        "172.67.2.20", "172.67.73.1", "172.67.180.12", "162.159.192.1", "162.159.193.1",
        "104.21.3.1", "104.21.3.2", "104.22.3.1", "104.22.3.2", "172.67.74.152", "104.20.10.10"
    )

    val CLOUDFRONT_IPS = listOf(
        "13.32.0.1", "13.33.0.1", "13.35.0.1", "13.224.0.1", "13.225.0.1",
        "18.64.0.1", "18.65.0.1", "99.84.0.1", "99.86.0.1", "54.230.0.1"
    )

    fun getCleanIp(preset: String): String? {
        val cached = cleanIpCache[preset]
        if (cached != null && (System.currentTimeMillis() - cached.second) < CACHE_DURATION_MS) {
            android.util.Log.i("ExpressiveBox", "Using cached clean IP for $preset: ${cached.first}")
            return cached.first
        }

        val ips = when (preset) {
            "cloudflare" -> CLOUDFLARE_IPS
            "cloudfront" -> CLOUDFRONT_IPS
            else -> return null
        }

        android.util.Log.i("ExpressiveBox", "No cached IP for $preset. Running clean IP scan...")
        val cleanIp = runBlocking {
            scanCleanIps(ips)
        }
        if (cleanIp != null) {
            android.util.Log.i("ExpressiveBox", "Clean IP scan succeeded. Found best IP for $preset: $cleanIp")
            cleanIpCache[preset] = Pair(cleanIp, System.currentTimeMillis())
        } else {
            android.util.Log.w("ExpressiveBox", "Clean IP scan failed for $preset. Falling back to default.")
        }
        return cleanIp
    }

    private suspend fun scanCleanIps(ips: List<String>): String? = withContext(Dispatchers.IO) {
        val jobs = ips.map { ip ->
            async {
                val startTime = System.currentTimeMillis()
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, 443), 600) // 600ms timeout
                    }
                    val latency = System.currentTimeMillis() - startTime
                    Pair(ip, latency)
                } catch (e: java.io.IOException) {
                    null
                }
            }
        }
        val results = jobs.awaitAll().filterNotNull().sortedBy { it.second }
        results.firstOrNull()?.first
    }
}
