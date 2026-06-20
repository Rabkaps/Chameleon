### What's New in v1.5.1

This release introduces VMess, Hysteria2 (including `hy2` alias), and TUIC protocol integrations, along with connection safety checks, VMess name decoding in notifications, and compiler clean-up.

This release includes standalone architecture-specific APKs and a comprehensive universal bundle for standard edition.

---

### 🚀 Changelog compared to v1.0.69:

#### Added
- **VMess, Hysteria2, and TUIC Protocol Support (New in v1.5.1)**: Full JSON configuration compilers, name decoding, and latency tests for the new protocol formats.
- **Material 3 Expressive Animation & Transitions (v1.5.0)**: Added custom wavy circular progress indicators (`CircularWavyProgressIndicator`), pulsing connection status dot inside dashboard pills, and horizontal tab transition scaling and fade effects.
- **Swipeable Bottom Navigation (v1.5.0)**: Fully integrated `HorizontalPager` to allow users to swipe between Home, Configs, Logs, and Settings tabs smoothly.
- **DNS Poisoning & Hijacking Mitigation (v1.5.0)**: Implemented a Kotlin-layer parallel DNS pre-resolver using public/secure DNS endpoints to bypass carrier DNS poisoning, injecting resolved IP directly into configurations.
- **Advanced Transport Protocol Creator/Editor (v1.5.0)**: Added transport configuration parameters inside the app creator/editor for VLESS, Trojan, and Shadowsocks protocols, supporting WebSocket, gRPC, mKCP, and HTTPUpgrade.
- **Translucent Popup Activity (`NodesPopupActivity`) (v1.5.0)**: Created a translucent overlay dialog activity launched directly from the connection status notification **"List"** action to display configurations, search, pings, and sub selector on top of other apps.
- **Subscription Auto-Connect & Selector (v1.5.0)**:
  - **Auto-Connect**: Evaluates latency of all servers in the active subscription in parallel on connection startup and connects to the one with the lowest delay.
  - **Subscription Selector**: Added a horizontal scrollable row of chips to easily switch between subscriptions on both the main screen nodes overlay and the popup dialog.
- **Improved Connection Notification visibility (v1.5.0)**: Re-configured standard notifications to use a default importance channel (`vpn_service_channel_v2`) and builder priority, keeping action buttons ("List" and "Disconnect") visible without being collapsed or hidden in silent sections.
- **Multi-Architecture ABI Splits (Option B) (v1.5.0)**: Compiled standalone APKs targeting individual architectures (`armeabi-v7a` (32-bit ARM), `arm64-v8a` (64-bit ARM), `x86`, `x86_64`) plus a consolidated **universal bundle** for maximum compatibility.

#### Fixed
- **Active Node Safety Check (v1.5.1)**: Added UI-level and service-level checks to abort connections with a Toast warning if the user attempts to connect without selecting any node.
- **VMess Notification Name Decoding (v1.5.1)**: The status notification now correctly decodes and displays the VMess remark name (`"ps"`) instead of showing the raw base64 string.
- **Boilerplate Clean Up (v1.5.1)**: Removed unused skeleton `MainScreenViewModel.kt` and its tests to resolve compilation issues.
- **Stuck Tab Animations (v1.5.0)**: Resolved a visual glitch where rapid tab jumps or jumps across non-adjacent tabs would cause the animated screen to get stuck in a scaled-down, faded-out intermediate state.
- **Live Stats Default Value (v1.5.0)**: Reconfigured default settings so that the live stats speed indicators are disabled by default.

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
