### What's New in v1.5.0

ExpressiveBox v1.5.0 is a major upgrade introducing the premium **Material 3 Expressive** design system, full swipeable transitions, parallel DNS pre-resolution to bypass ISP censorship, a translucent quick-action pop-up dialog, and subscription auto-connect based on live latency checks.

This release includes standalone architecture-specific APKs and a comprehensive universal bundle.

---

### 🚀 Changelog compared to v1.0.69:

#### Added
- **Material 3 Expressive Animation & Transitions**: Added custom wavy circular progress indicators (`CircularWavyProgressIndicator`), pulsing connection status dot inside dashboard pills, and horizontal tab transition scaling and fade effects.
- **Swipeable Bottom Navigation**: Fully integrated `HorizontalPager` to allow users to swipe between Home, Configs, Logs, and Settings tabs smoothly.
- **DNS Poisoning & Hijacking Mitigation**: Implemented a Kotlin-layer parallel DNS pre-resolver using public/secure DNS endpoints to bypass carrier DNS poisoning, injecting resolved IP directly into configurations.
- **Advanced Transport Protocol Creator/Editor**: Added transport configuration parameters inside the app creator/editor for VLESS, Trojan, and Shadowsocks protocols, supporting WebSocket, gRPC, mKCP, and HTTPUpgrade.
- **Translucent Popup Activity (`NodesPopupActivity`)**: Created a translucent overlay dialog activity launched directly from the connection status notification **"List"** action to display configurations, search, pings, and sub selector on top of other apps.
- **Subscription Auto-Connect & Selector**:
  - **Auto-Connect**: Evaluates latency of all servers in the active subscription in parallel on connection startup and connects to the one with the lowest delay.
  - **Subscription Selector**: Added a horizontal scrollable row of chips to easily switch between subscriptions on both the main screen nodes overlay and the popup dialog.
- **Improved Connection Notification visibility**: Re-configured standard notifications to use a default importance channel (`vpn_service_channel_v2`) and builder priority, keeping action buttons ("List" and "Disconnect") visible without being collapsed or hidden in silent sections.
- **Multi-Architecture ABI Splits (Option B)**: Compiled standalone APKs targeting individual architectures (`armeabi-v7a` (32-bit ARM), `arm64-v8a` (64-bit ARM), `x86`, `x86_64`) plus a consolidated **universal bundle** for maximum compatibility.

#### Fixed
- **Stuck Tab Animations**: Resolved a visual glitch where rapid tab jumps or jumps across non-adjacent tabs (e.g. Home to Logs) would cause the animated screen to get stuck in a scaled-down, faded-out intermediate state. Moved visual transforms inside the `graphicsLayer` lambda draw phase.
- **Live Stats Default Value**: Reconfigured default settings so that the live stats speed indicators are disabled by default.

---

### ExpressiveBox Release Installation Note

When installing this APK, Google Play Protect may display a warning such as **"Blocked by Play Protect"** or **"Unrecognized Developer"**. 

This warning appears because the APK is built and signed using a developer debug signature rather than a signing key registered with the Google Play Store console.

#### How to proceed with installation:
1. In the Play Protect dialog, tap **"More details"**.
2. Tap **"Install anyway"** to complete the installation.
3. If the installation is still blocked, you can temporarily disable scanning:
   * Open the **Google Play Store** app.
   * Tap your profile icon in the top right -> **Play Protect** -> tap the **Settings** (gear) icon in the top right.
   * Toggle off **"Scan apps with Play Protect"**.
