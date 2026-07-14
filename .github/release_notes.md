### What's New in v1.7.10

This release delivers major upgrades to the home dashboard and server tab layouts, adding a premium minimalist diagonal traffic card, a custom neon app logo, and accessibility contrast enhancements for solid/vibrant chameleon themes.

#### Added & Improved
- **Premium Minimalist Diagonal Traffic Card**: Redesigned the dashboard's traffic card to use a sleek diagonal line layout drawn with Compose Canvas. It displays download speeds in the top-left and upload speeds in the bottom-right, removing visual clutter (such as the "TRAFFIC" label and connection dot) for a clean, modern look.
- **Symmetric Dashboard Cards**: Forced both the Traffic and IP Address cards to share identical height constraints (`100.dp`) at the parent Card level to prevent layout mismatch caused by internal padding.
- **Compact Bento Action Cards**: Reduced bento cards height to `80.dp` and country/subscription filtering card heights to `50.dp` to maximize the vertical scrollable space of the server node list.
- **Custom Side Drawer Logo**: Replaced the standard application logo with a custom-provided neon chameleon icon.
- **Emoji-Styled Lion & Sun Flag**: Replaced the custom Iran flag with a modern, rounded-rectangle flat emoji featuring transparent corners to match system flag emojis seamlessly.

#### Fixed
- **Solid & Vibrant Theme Accessibility**: Fixed a text contrast issue in the Subscriptions and Search Nodes cards when using the Solid or Vibrant Chameleon themes. A new `isSecondary` theme flag maps text and icons to `onSecondaryContainer` (the high-contrast text color) to ensure readability.
- **Spacious Contextual Selection Menu**: Replaced the constrained bottom `NavigationBar` container with a custom floating `Surface` when multi-select mode is active. Substituted text buttons with compact icons (`Close` and `Delete`) to prevent horizontal and vertical cramping on smaller screens.
- **IP-Based Dashboard Flag**: Corrected the Connection Dashboard flag to dynamically geolocate and display the flag of the user's external IP address instead of the connected node.

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
