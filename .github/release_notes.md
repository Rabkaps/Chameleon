### What's New in v1.0.66

This release resolves the cold startup stutter/lag issues on variable refresh rate (VRR) screens and optimizes node list rendering.

#### Performance Improvements
- **120Hz Refresh Rate (Startup Stutter Fix)**: Programmatically requests high refresh rate (120Hz) on launch in `MainActivity` to prevent the system display compositor from locking the window to a low refresh rate (30Hz/60Hz) on cold launch.
- **Snappy Node List Rendering**: Excluded heavy animations, LaunchedEffects, and float animations from `LazyColumn` items to prevent lag on launch/search/tab navigation regardless of the user's node count.

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
