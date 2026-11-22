# Chameleon v1.8.6 Release Notes

Chameleon v1.8.6 focuses on Android VPN stability, reliable UDP/Discord connectivity, accurate ping & real-delay diagnostics, and faster startup.

## What's New

### 🛡️ Core & Connectivity
* **Default `system` TUN Stack**: Switched the default TUN network stack from `mixed` to `system`, fixing UDP/QUIC stalls in apps like Discord while improving throughput and battery life.
* **Direct-Network Ping & Censorship Diagnostics**: Node pings and censorship diagnostics now bypass the active VPN tunnel over the physical network interface, giving accurate results even while connected.
* **Real Tunnel Delay Test**: Wired the configurable **Ping Delay Test URL** into the dashboard connection card so you can measure live HTTP round-trip latency through the active VPN tunnel.
* **TLS Fragmentation & DNS Routing Fixes**: Fixed `fragment_fallback_delay` range parsing (`"10-20"` → `"10ms"` instead of `"1020ms"`) and prevented `8.8.8.8` from leaking into `direct` routing rules.
* **Idle Clean-IP Failover Fix**: Clean-IP pool failover now triggers only on actual uplink-active stalls rather than when the device is simply idle.
* **Proxy Chain (`chain://`) Ping Support**: Pinging and auto-connecting a Proxy Chain now resolves and tests its entry relay server cleanly.

### ✨ Performance & QoL
* **Bounded Concurrent Pings & Faster Country Flags**: Capped parallel node pings at 16 to prevent router NAT saturation, and skipped redundant external GeoIP/DNS lookups when a node's name already contains a country or flag emoji.
* **Connect Fastest Shortcut & Cold-Start Locale**: Fixed the home-screen **Connect Fastest** shortcut to read all active subscriptions and removed blocking DataStore I/O from cold startup.

---
Thank you for using Chameleon!
