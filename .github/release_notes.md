### What's New in v1.0.65

This release fixes performance stutter/lags and reverts the visual redesign to restore snappy performance and clean interface.

#### Performance Improvements
- **Startup Lag Fixes**: Optimized DataStore flow collection, cached dynamic system color lookups (avoiding expensive reflection), and offloaded crash log file operations to background thread.
- **Visual Revert**: Reverted custom animated background canvas, morphing connect button, rolling telemetry graph, and glassmorphic cards back to standard, high-performance Material 3 dashboard layout.

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
