# ExpressiveBox

ExpressiveBox is a modern, high-performance VPN client for Android built entirely with **Jetpack Compose** and **Material Design 3**. It offers secure routing, modular connection profiles, and deep integration with the Android system's native VPN frameworks.

> [!NOTE]
> **Project Status**: ExpressiveBox is currently in its **early testing stages**. It supports standard subscription/profile parsing, custom DNS rules, split tunneling, and core connectivity using modern secure protocols (VLESS, VMess, Trojan, Shadowsocks).
> 
> * **Persian (Farsi) language support is coming soon!**

## Key Features

* **VpnService Integration**: Core routing using a high-performance native tunnel engine (`libbox`), exposing robust state monitoring and connection management.
* **Automated Iran Routing**: Built-in rules for automatic identification of domestic Iranian domains and IP ranges. It automatically routes local Iranian traffic directly (bypassing the VPN) to guarantee optimal speeds and uninterrupted access to domestic banking and government services.
* **Modern Material 3 UI**: Implements cohesive Material 3 design components, dynamic coloring, linear gradient headers, and card-based settings menus.
* **Granular Split Tunneling**: Route specific application traffic through the VPN or bypass it completely. Includes an option to toggle visibility of system apps, with a memory-cached icon loading architecture that guarantees smooth, stutter-free scrolling.
* **QR Code Parsing**: Fast configuration import by scanning QR codes using the device camera (powered by Android CameraX and Google ML Kit Barcode Scanning).
* **Tactile Interactions**: Custom press-scale effects and physics-based micro-animations that respond naturally to user gestures without swallowing click events.
* **Persistent Settings**: Multi-field configuration and user preferences persisted safely using Android Jetpack DataStore.

## Architecture

The project adheres to modern Android development best practices, featuring **Unidirectional Data Flow (UDF)** and a clean package structure:

* **`ui/`**: Screen composables and stateless content views partitioned by feature (e.g., `main`, `split`, `qr`). Consumes states via standard state holders and propagates event lambdas up.
* **`vpn/`**: Deals with Android `VpnService` lifecycle, network configuration injection, status broadcast listeners, and Quick Settings tile integration (`VpnQuickSettingsTileService`).
* **`data/`**: Configuration parsing, VPN profile management, and DataStore-backed preference stores.
* **`theme/`**: Color schemes, typography definitions, and shapes adhering to Material Design 3 guidelines.

## Technical Specifications

* **Minimum SDK**: Android 7.0 (API Level 24)
* **Target SDK**: Android 16 (API Level 36)
* **Compile SDK**: Android 16 (API Level 37)
* **Supported ABI / Architecture**: `arm64-v8a` (specifically configured for 64-bit ARM devices due to native VPN binaries)
* **UI Toolkit**: Jetpack Compose (Material 3)
* **Language Toolchain**: Kotlin 2.x, Java 17

---

## Getting Started

### Prerequisites

* Android Studio (Ladybug or newer)
* Android SDK (API 34+)
* JDK 17

### Building the Project

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/ExpressiveBox.git
   cd ExpressiveBox
   ```

2. Sync and build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

3. Run the application on a connected device:
   ```bash
   ./gradlew installDebug
   ```

## Development and Contributions

We follow the standard Git branching model. Please submit all feature additions or bug fixes via Pull Requests. Make sure to run static analysis and unit tests before committing changes.
