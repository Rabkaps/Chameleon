### What's New in v1.7.0

This release introduces the Material Expressive Bento Grid UI redesign, along with restored Gaming Mode sub-toggles, tap-and-hold options, robust HTTPS geo-resolution fallbacks, and real delay optimizations.

---

### 🚀 Changelog:

#### Added
- **Material Bento UI Redesign**: Fully harmonized dashboard card style with Monet-themed dynamic gradient brushes, status pulsing ring, and transition animations.
- **Gaming Mode Sub-Toggles**: Restored the option to configure whether game traffic tunnels through the VPN proxy or routes directly via active TCP Radar DNS and direct UDP.
- **Tap-and-Hold Menu**: Added long-press actions to servers list items to quickly Share Config, Share QR Code, Edit Config (manual), or Delete Config (manual).
- **HTTPS Geolocation Fallbacks**: Transitioned country code resolution to a robust, pure-HTTPS fallback chain (`freeipapi.com` -> `api.country.is` -> `ipwho.is`), bypassing Android cleartext limits.
- **HTTPS Real Delay Tester**: Migrated the default delay test URL to secure HTTPS (`https://cp.cloudflare.com/generate_204`) to fix the real-time latency ping tracker.
- **WARP Detour Options**: Fully restored WARP detour routing settings and customized port configuration options.

---

### Chameleon Release Installation Note

When installing this APK, Google Play Protect may display a warning such as **"Blocked by Play Protect"** or **"Unrecognized Developer"**. 

This warning appears because the APK is built and signed using a developer debug signature rather than a signing key registered with the Google Play Store console.

#### How to proceed with installation:
1. In the Play Protect dialog, tap **"More details"**.
2. Tap **"Install anyway"** to complete the installation.
3. If the installation is still blocked, you can temporarily disable scanning:
   * Open the **Google Play Store** app.
   * Tap your profile icon -> **Play Protect** -> tap the **Settings** (gear) icon in the top right.
   * Toggle off **"Scan apps with Play Protect"**.
