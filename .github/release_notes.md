# Chameleon v1.8.5 Release Notes

Chameleon v1.8.5 introduces enhanced anti-censorship routing, native post-quantum TLS Reality support, updated Iran routing rules, and extensive UI polish across Android and Desktop!

## What's New

### 🛡️ Core & Connectivity
* **Post-Quantum Reality (X25519MLKEM768)**: Updated native core (`libbox.aar`) with full support for hybrid post-quantum key exchange in TLS Reality handshakes, eliminating `reality verification failed` against modern Xray-core servers.
* **Carrier DPI Poisoning Defense**: Active rejection rules for Iranian carrier DPI block pages (`10.10.34.34/31`, `10.10.34.36/32`), isolated Reality SNI resolution, and clean bootstrap DNS to prevent deadlocks.
* **Dual-Stack & IP Strategy**: Added IPv6 dual-stack support and customizable IP version strategies (`prefer_ipv4`, `prefer_ipv6`, `ipv4_only`, `ipv6_only`).
* **Updated Geo Rule-Sets**: Refreshed Iran GeoIP and GeoSite binary rule-sets (`geoip-ir.srs`, `geosite-ir.srs`) from upstream for reliable domestic bypassing.
* **Strict Config Conformance**: Cleaned up sing-box JSON configuration generation to prevent startup deserialization failures.

### ✨ UI & Personalization
* **Desktop M3 Expressive UI**: Fluid Material 3 Expressive Bento dashboard on Desktop with spring physics and responsive card layouts.
* **Bento Dashboard Customization**: Interactive drag-and-drop card reordering with drop-on-release preview, live resize snapping, and 1-tap IP refresh.
* **2026 Material 3 Expressive Palette**: Authentic Android Monet dynamic theming with pure black AMOLED dark mode and fixed theme presets.

---
Thank you for using Chameleon!
