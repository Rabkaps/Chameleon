### What's New in v1.7.9

This release delivers major upgrades to the Cloudflare WARP integration, resolving connection errors, startup crashes, and censorship blocks by transitioning to an offline WireGuard implementation with a customizable settings dashboard.

#### Added & Improved
- **Offline WireGuard WARP Transition**: Replaced the API-dependent `"warp"` outbound protocol with a standard, completely offline `"wireguard"` endpoint layout. This bypasses startup HTTP API registration calls to `api.cloudflareclient.com` entirely, resolving connection errors and timeout loops on restricted networks.
- **Premium WARP Settings Dashboard**: Added a new settings card in the Settings tab enabling users to:
  - Generate fresh Cloudflare registration credentials on-demand (Change Exit IP).
  - Delete local WARP accounts and clear credentials.
  - Customize the WARP port and local interface address.
  - Toggle detours between the proxy server and a direct connection.
  - Manually override the WARP endpoint address with custom clean Cloudflare IPs.
- **Dynamic Anycast IP Default**: Configured the default WARP peer address to `engage.cloudflareclient.com`, allowing the detour proxy to dynamically resolve and route to the closest and most optimal Cloudflare endpoint IP.

#### Fixed
- **CIDR Suffix Parsing Crash**: Added automatic suffix formatting to client IP addresses to append missing mask prefixes (like `/32` for IPv4 or `/128` for IPv6), preventing the sing-box core from crashing with `no '/'` errors.
- **Config Decode Crashes**: Completely stripped the unsupported `reserved` field from the WireGuard peer options to prevent configuration decoder crashes.
- **Base64 Public Key Encoding**: Corrected the base64 public key encoding typo to eliminate startup key decoding crashes.

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
