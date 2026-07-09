### What's New in v1.7.8

This release delivers critical fixes for MTProxy (MTProto) stopping reliability and greatly improves OpenVPN configuration parsing and connection support.

#### Added & Improved
- **Robust OpenVPN Integration**: Restructured the OpenVPN parsing engine to expose `server` and `server_port` at the outbound JSON top level and embedded the raw profile configuration under the required `config` fields. This resolves connection errors when importing custom `.ovpn` profiles.
- **Reliable Local MTProto Proxy Stopping**: Extended the command server teardown delay to 500ms and added thread blocking in `onDestroy()` to guarantee the native Go/sing-box service completely terminates and releases the listening socket.

---

### 🚀 Changelog:

#### Added & Improved
- **Clean Standard Branding**: Removed "Standard" branding suffixes to display plain "Chameleon" globally.
- **Landscape Layout Dashboard**: Redesigned UI to show connection dashboard (left column) and servers list (right column) side-by-side on device rotation, eliminating vertical scrolling to connect in landscape mode.
- **Quick Settings Tile Long Press**: Re-implemented the tile launcher pending intent to directly open the dashboard on long press.
- **Batch Actions**: Added Select All, Deselect All, and Batch Delete capabilities to the expanded manual servers list via long press.

#### Fixed
- **MTProxy Socket Release**: Wrapped local proxy engine teardown inside a `NonCancellable` context to prevent socket leaks on shutdown, freeing port `19999` and `3000` immediately when disabled.
- **Server Selection & Gestures**: Re-implemented the button press animations using unconsumed initial pass pointer events, resolving the selection and long-press menu lockup in the servers list.
- **Connect Button Morphing Waves**: Migrated wave animations to `rememberInfiniteTransition` to fix LaunchedEffect coroutine lifecycle bugs and restore the visual morphing shapes.
- **Root Mode Placement**: Removed the misplaced Root Mode switch from the Telegram Proxy card. Root Mode remains safely inside the settings sheet drawer.

---

### Chameleon Release Installation Note

When installing this APK, Google Play Protect may display a warning such as **"Blocked by Play Protect"** or **"Unrecognized Developer"**. 

This warning appears because the APK is built and signed using a developer key rather than a signing key registered with the Google Play Store console.

#### How to proceed with installation:
1. In the Play Protect dialog, tap **"More details"**.
2. Tap **"Install anyway"** to complete the installation.
3. If the installation is still blocked, you can temporarily disable scanning:
   * Open the **Google Play Store** app.
   * Tap your profile icon -> **Play Protect** -> tap the **Settings** (gear) icon in the top right.
   * Toggle off **"Scan apps with Play Protect"**.
