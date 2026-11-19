<div align="center">

# HuashijieLibre

**Open-source Morphe patch suite that brings multi-language JSON localization, offline workflow access, and canvas ergonomics to 画世界Pro v3.4.6.**

[![Latest Release](https://img.shields.io/github/v/release/Doni4c/HuashijieLibre?include_prereleases&style=for-the-badge&logo=github&label=Release&color=0969DA)](https://github.com/Doni4c/HuashijieLibre/releases)
[![Target App](https://img.shields.io/badge/Target-net.huanci.hsjpro%20v3.4.6%20%5B313%5D-1F883D?style=for-the-badge&logo=android)](https://github.com/Doni4c/HuashijieLibre/releases)
[![Patches](https://img.shields.io/badge/Patches-38%20Atomic%20Modules-8250DF?style=for-the-badge)](#patch-catalog)
[![Morphe Patcher](https://img.shields.io/badge/Morphe%20Patcher-v2.3.0-BF3989?style=for-the-badge)](https://morphe.software/)
[![Localization](https://img.shields.io/badge/Localization-Multi--Language%20JSON-D97706?style=for-the-badge)](TRANSLATING.md)
[![GPL-3.0 License](https://img.shields.io/badge/License-GPL--3.0-DC2626?style=for-the-badge)](LICENSE)

[**Add to Morphe**](https://morphe.software/add-source?github=Doni4c%2FHuashijieLibre) | [**Releases**](https://github.com/Doni4c/HuashijieLibre/releases) | [**Translating Guide**](TRANSLATING.md) | [**Patch Catalog**](#patch-catalog)

</div>

## Overview

画世界Pro delivers responsive brush physics, generous layer limits on mid-range hardware, frame-by-frame animation, and subscription-free PSD export. Because its latest releases primarily ship through mainland Chinese distribution channels, international artists on v3.4.6 face a Simplified Chinese-only interface, +86 phone login gates around local features such as PDF export and brush parameter editing, startup splash advertisements, and domestic analytics SDKs.

HuashijieLibre provides 38 independent Morphe patches that localize the entire studio through externalizable JSON language packs, restore offline access to local drawing tools, clear domestic social and cloud clutter, and refine everyday canvas ergonomics (smart tap-outside selection exit, damped two-finger reference rotation, optional Procreate-style floating brush sliders with compact landscape sidebars, responsive landscape tool grids, watermark-free timelapse export via Android Share Sheet, transparent `.hsj` storage paths, custom `.ttf`/`.otf` UI typography, and adaptive text fitting).

## Compatibility and Artifacts

| Property | Specification |
| :--- | :--- |
| Target Application | 画世界Pro (`net.huanci.hsjpro`) |
| Target Version and Build | v3.4.6 (versionCode 313) |
| Clean Base APK SHA-256 | `1d5bfdcc32d9a593c3f34bc83c3af300ac19e691082b620bd89cfb46fc7271aa` |
| Supported ABIs | Universal (arm64-v8a, armeabi-v7a) matching stock APK |
| Patch Engine | Morphe Patcher 2.3.0 (`app.morphe.patches` 2.1.0) |
| Release Artifact Policy | Patch bundle only (`*.mpp`); no modified APKs or signing keystores hosted |

## Architecture Overview

Each capability compiles as an independent Morphe patch paired with runtime toggle registration under Settings > General Settings > HuashijieLibre. Modules excluded at patch time leave no dead switches in the in-app menu. Modules are organized into seven functional groups by architectural scope:

| Module Group | Patch Count | Classification Boundary |
| :--- | :---: | :--- |
| Core Foundation | 2 | App integrity verification bypasses and shared in-app configuration infrastructure |
| Localization and Typography | 3 | Runtime string interception, external JSON dictionary loading, and global typeface replacement |
| Privacy, Telemetry, and Account Decluttering | 12 | Background analytics SDK deactivation, ad suppression, and domestic cloud or login entry removal |
| Workspace UI Decluttering | 6 | Hiding promotional storefronts and domestic social rows across studio dialogs |
| Startup and System Sharing | 4 | Boot acceleration, Android Share Sheet routing, and dedicated export share entry controls |
| Offline Profile and Brush Access | 3 | Unlocking local creation tools and brush storage gated behind remote account checks |
| Canvas Workflow and Layout Geometry | 8 | Interactive canvas gestures, clean media export, storage visibility, and responsive layout polish |

## Installation

Version 3.4.6 (build 313) is the sole verified target build. Obtain an unmodified `net.huanci.hsjpro` v3.4.6 APK (from an existing device backup or public APK archive) and verify that its SHA-256 checksum matches `1d5bfdcc32d9a593c3f34bc83c3af300ac19e691082b620bd89cfb46fc7271aa` before patching.

1. Install Morphe Manager on Android and open [**Add to Morphe**](https://morphe.software/add-source?github=Doni4c%2FHuashijieLibre), or paste `https://github.com/Doni4c/HuashijieLibre` inside the Sources tab (manifest fallback: `https://raw.githubusercontent.com/Doni4c/HuashijieLibre/main/patches-bundle.json`).
2. In the Apps tab, select 画世界Pro v3.4.6, tap No, I already have it, and choose your verified `base.apk` file from local storage.
3. Patch with the default selection (enable Expert mode in Morphe Manager settings beforehand to include or exclude individual modules), then tap Install and grant storage permission on first launch.

> [!WARNING]
> **Back Up Your Signing Key and `.hsj` Drafts After Your First Build.**
> Android ties future in-place updates to the signing certificate generated on your first Morphe build. Open Morphe Manager > Settings > Backup > Signing key > Export and store the backup file (`Morphe.p12` or `Morphe.keystore`) safely off-device. Back up `/storage/emulated/0/HuashijiePro/Draft/` before uninstalling or switching keys.

## Patch Catalog

Modules disabled at build time are omitted cleanly from the APK, and installed modules expose matching runtime switches in General Settings. Floating Brush Sliders compiles as ON by default at patch time while starting OFF at runtime so drag handles stay hidden until you explicitly enable them.

### 1. Core Foundation (2 Patches)

**Classification criteria:** Restricted to low-level launch stability and shared preference infrastructure that all feature modules depend on (`Native Signature Bypass` and `Libre In App Settings Panel`).

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Native Signature Bypass | ON | Verifies original native byte signatures before patching `libPaintLib.so` so patched APK builds pass startup checks safely. |
| Libre In App Settings Panel | ON | Initializes preferences early in `DrawApplication`, flushes settings synchronously before restarts, and injects the inset-aware HuashijieLibre settings screen with GitHub update checks, Credits, license details, and diagnostics export. |

### 2. Localization and Typography (3 Patches)

**Classification criteria:** Owns resource bundling, runtime text interception, and font rendering across Android Views, dialogs, and custom Canvas draws.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Language Pack Resources | ON | Bundles built-in JSON dictionaries in `lang/` alongside the Simplified Chinese source template `_template_zh.json`. |
| Dynamic External Translation Engine | ON | Intercepts UI strings across views, dialogs, popups, adapters, and `StringFog` calls with zero-flicker Frame-0 rendering, hot-reload support, and protection for filesystem folder IDs. |
| Custom UI Font Injection | ON | Applies `.ttf` or `.otf` typefaces from `/sdcard/HuashijiePro/fonts/` across the app while preserving bold and italic weights and clean resets to the system default. |

### 3. Privacy, Telemetry, and Account Decluttering (12 Patches)

**Classification criteria:** Targets background tracking libraries, ad loaders, and domestic cloud or account surfaces that require network sign-in or transmit usage telemetry.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Block Host Telemetry SDKs | ON | Prevents `xCrash` and `MarsXlog` native logging libraries from initializing in `FlashActivity`. |
| Block Umeng Analytics SDK | ON | Disables Umeng analytics initialization and background device profiling. |
| Block Bugly Crash SDK | ON | Disables Tencent Bugly remote crash reporting and background network initialization. |
| Suppress Splash Ad Loading | ON | Blocks startup splash ad requests independently of `Instant Launch to Studio`. |
| Hide Personalized Ads Setting | ON | Hides the personalized advertising preference row in General Settings. |
| Hide Community Tabs | ON | Hides community and discovery feeds on `MainActivity`, leaving only local draft views. |
| Hide Top Bar Cloud Button | ON | Hides gallery top-bar cloud sync buttons and blocks accidental click triggers. |
| Hide Canvas Popup Cloud Sync | ON | Hides cloud backup controls and the cloud tab inside the `PaintActivity` settings popup. |
| Hide Draft Cloud Badge | ON | Hides cloud sync status icons and progress overlays on draft thumbnails. |
| Hide Exit Cloud Sync Option | ON | Hides cloud upload toggles inside the canvas exit prompt while keeping local save actions and balanced dialog spacing. |
| Hide Baidu Cloud Backup Entry | ON | Hides the Baidu Netdisk backup entry inside the main navigation drawer. |
| Hide Drawer VIP Banner | ON | Hides the promotional VIP membership banner inside the navigation drawer. |

### 4. Workspace UI Decluttering (6 Patches)

**Classification criteria:** Removes promotional storefronts, online template promos, and domestic social share rows from local creation popups without altering underlying share intents.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Hide Online Canvas Templates | ON | Hides the online template button on `CreateCanvasActivity` and centers the remaining three local creation options. |
| Hide Online Brush Shop | ON | Hides the online store entry inside the quick canvas brush selector while preserving all local brushes. |
| Hide Brush Library Shop | ON | Hides the online store button inside the full-screen Brush Library modal independently of the quick picker. |
| Block Brush Upgrade Tip | ON | Suppresses promotional brush upgrade banners inside `SelectPenView`. |
| Hide Domestic Export Social Icons | ON | Hides domestic social channel rows in the image export panel while keeping local export actions intact. |
| Hide Timelapse Export Social Icons | ON | Hides domestic social channel buttons in the timelapse export panel while keeping save and quality options intact. |

### 5. Startup and System Sharing (4 Patches)

**Classification criteria:** Modifies top-level boot navigation and Android OS sharing integration, including external file launch handling and `FileProvider`-backed `Intent.createChooser` flows.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Instant Launch to Studio | ON | Bypasses the `FlashActivity` splash countdown while preserving external file open intents (`ACTION_VIEW` and `ACTION_SEND`) and safe Process Death recovery. |
| Native Android Share Sheet | ON | Routes artwork, PSD, PDF, brush, animation frame folder, and `.hsj` exports in `PaintActivity` through Android's native Share Sheet using `content://` `FileProvider` URIs. |
| Replay Android Share Sheet | ON | Routes speedpaint video exports in `ReplayActivity` through the standard Android Share Sheet. |
| Export Sheet Share Button | ON | Adds an independent System Share button beside Album and File inside the `ExportPictureView` sheet for both artwork and timelapse exports. |

### 6. Offline Profile and Brush Access (3 Patches)

**Classification criteria:** Supplies local entitlement and storage sovereignty without requiring mainland phone verification, while preserving any real signed-in account session.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Local Artist Profile | ON | Supplies an in-memory offline profile when signed out so login-gated local tools such as PDF export work offline without overwriting active real accounts. |
| Custom Brush Sovereignty | ON | Restores editing, renaming, and duplicating permissions for preset and locally imported brushes offline. |
| External Local Brush Scanning | ON | Scans `/sdcard/HuashijiePro/ASBrush` on startup and preserves imported brush packs during offline sessions. |

### 7. Canvas Workflow and Layout Geometry (8 Patches)

**Classification criteria:** Encapsulates studio ergonomics, interactive touch gestures inside `PaintActivity`, output media cleanup, explicit storage path transparency, and responsive layout fixes for non-Chinese scripts.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Lasso Tap-Outside Exit | ON | Dismisses active selection mode with a single tap outside the selection bounds while protecting Magic Wand, Polygonal Lasso, Move/Transform, and active Symmetry/Perspective/Animation bars. |
| Floating Brush Sliders | ON (Runtime OFF) | Places a draggable 2x2 dot handle between the brush size and opacity sliders with normalized screen-ratio persistence, safe cutout-aware compact landscape sidebars, and crash-guarded layout hooks. |
| Two-Finger Reference Rotation | ON | Enables free two-finger rotation inside the floating reference image window with an 8-degree anti-tilt deadzone during pinch zoom and wrap-around angle smoothing. |
| Watermark Free Timelapse Export | ON | Removes the corner brand watermark during `ReplayActivity` playback and video export. |
| HSJ File Path Display | ON | Displays the active local `.hsj` draft and export folder paths dynamically inside Draft Storage Settings. |
| Canvas Popup HSJ Path Row | ON | Appends the active local `.hsj` folder info row inside the `PaintActivity` canvas settings tab with tap-to-copy clipboard support. |
| Canvas Popup Geometric Polish | ON | Balances canvas popup rows, adapts the left tool grid between 2 portrait columns and 3 to 4 scrollable landscape columns, and refines floating toolbars. |
| Narrow Button Text Auto Fit | ON | Scales and wraps localized labels inside tight buttons, tabs, and dialogs to prevent text truncation. |

## Custom Languages and Fonts

You do not need to rebuild the APK to test a custom translation draft or change UI typography:

1. Copy a UTF-8 JSON language pack into `/storage/emulated/0/HuashijiePro/lang/`.
2. Copy `.ttf` or `.otf` font files into `/storage/emulated/0/HuashijiePro/fonts/`.
3. Open Settings > General Settings > HuashijieLibre and pick your file under Active language pack or Custom UI font for immediate reload.

## Contributing and Local Builds

Translators can contribute new packs without writing code or compiling Android projects. Detailed workflow guidelines, priority tiers, digital art terminology standards, and live device preview steps are documented in [TRANSLATING.md](TRANSLATING.md).

Patch implementations live under `patches/src/main/kotlin/app/morphe/patches/huashijie/` across seven packages matching the catalog groups above, built against `app.morphe.patches` 2.1.0 and Morphe Patcher 2.3.0. Runtime controllers under `extensions/huashijie-libre/` compile into extension DEX (`classes6.dex`), routing `classes4.dex` hooks through `TranslationEngine.dispatch` to preserve the strict zero-new-MethodReference budget beneath the 65,535-method ceiling.

Local builds require JDK 21 (targeting Java 17 bytecode), an Android SDK, and GitHub Packages read credentials (`GITHUB_ACTOR` and `GITHUB_TOKEN` in environment variables or `~/.gradle/gradle.properties`):

```powershell
# Build patches, extension DEX, patches-list.json, and validate dictionaries
.\gradlew.bat build --no-daemon

# Validate JSON syntax, key parity, and punctuation checks across lang/*.json
.\gradlew.bat :patches:validateDictionaries --no-daemon

# Verify all 32 structural DEX target hooks against unmodified base.apk
.\gradlew.bat :patches:verifyTargetParity --no-daemon
```

## Disclaimer

1. **Independent project:** HuashijieLibre is an independent open-source Morphe patch collection not affiliated with Shijiazhuang Huanci Network Technology or Hangzhou Mojing Technology. No copyrighted APKs, pre-patched binaries, or signing keystores are hosted in this repository.
2. **Modular control:** Every patch is independently toggleable at patch time and exposed as a live preference in General Settings.