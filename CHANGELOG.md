# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.63] - 2026-06-16

### Added
- Created `proguard-rules.pro` configuration for release minification (R8). Safely keeps JNI callback bindings (`libbox`), go-runtime wrappers (`go`), and serialization schemas (`kotlinx.serialization`) intact.

### Fixed
- **Launch Performance Lag**: Resolved reflection-based accent color resource lookup jank by wrapping Monet theme palette derivation in a `remember` block, preventing `resources.getIdentifier` from being called 20+ times on every recomposition.
- **Scroll Frame Drops**: Replaced list auto-scroll coroutine animations (`animateScrollToItem()`) with instant programmatic jumps (`scrollToItem()`) inside the engine log viewer, avoiding continuous layout invalidation when receiving burst logs on connection.
- **Proxy List Scrolling Stutter**: Optimized item stagger animations in the main server listing. If the stagger entrance completes, individual items bypass reading animated progress states and render static draw parameters directly, eliminating recompositions during scrolling.
- **Immersive Edge-to-Edge Navigation**: Replaced the root container safe area/margin padding with a full-screen layout. The settings drawer now slides smoothly from the absolute screen boundaries, and action app bars stretch flush to status and navigation bar areas.
- **App Menu Version Display**: Bumped the displayed application version to `v1.0.63` in the Drawer layout.

### Optimized
- Enabled R8 minification (`isMinifyEnabled = true`) on release configurations for maximum runtime execution speed, smaller APK size, and smoother layout animations.
- Bound debug signature config to release build type to simplify testing of production-optimized builds.

---

## [1.0.62] - 2026-06-15

### Added
- Refactored `WaveVisualizer` in `MainScreen.kt` using `rememberInfiniteTransition()` to shift wave computations entirely to the GPU Draw phase, dropping CPU composition cycles during active animation.
- Moved package manager app scanner queries to `Dispatchers.IO` background thread in `SplitTunnelingScreen.kt` to avoid blocking the Main thread.
- Enabled spring-based slide-and-fade navigation transitions using Compose Navigation 3.
