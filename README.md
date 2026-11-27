<div align="center">

# HuashijieLibre

**Open-source Morphe patches for 画世界Pro v3.4.6: JSON UI translations, custom fonts, offline access to login-gated tools, and canvas controls.**

[![Latest Release](https://img.shields.io/badge/Release-v1.0.0--beta.3-0969DA?style=for-the-badge&logo=github)](https://github.com/Doni4c/HuashijieLibre/releases)
[![Target App](https://img.shields.io/badge/Target-net.huanci.hsjpro%20v3.4.6%20%5B313%5D-1F883D?style=for-the-badge&logo=android)](https://github.com/Doni4c/HuashijieLibre/releases)
[![Patches](https://img.shields.io/badge/Patches-38%20Modules-8250DF?style=for-the-badge)](#patch-catalog)
[![Morphe Patcher](https://img.shields.io/badge/Morphe%20Patcher-v2.3.0-BF3989?style=for-the-badge)](https://morphe.software/)
[![Localization](https://img.shields.io/badge/Localization-Multi--Language%20JSON-D97706?style=for-the-badge)](TRANSLATING.md)
[![GPL-3.0 License](https://img.shields.io/badge/License-GPL--3.0-DC2626?style=for-the-badge)](LICENSE)

[**Add to Morphe**](https://morphe.software/add-source?github=Doni4c%2FHuashijieLibre) | [**Releases**](https://github.com/Doni4c/HuashijieLibre/releases) | [**Translating Guide**](TRANSLATING.md) | [**Patch Catalog**](#patch-catalog)

</div>

## Overview

画世界Pro v3.4.6 includes multi-layer painting, frame-by-frame animation, and subscription-free PSD export, but ships only in Simplified Chinese. It requires mainland phone sign-in for local tools such as PDF export and brush parameter editing, loads startup splash ads, and initializes domestic analytics SDKs.

HuashijieLibre provides 38 independent Morphe patches that translate the UI from external JSON files, apply custom `.ttf`/`.otf` UI fonts, unlock local login-gated tools offline, hide domestic feeds, cloud prompts, and ad slots, route exports to the Android Share Sheet, and add canvas controls (tap-outside Lasso exit, two-finger reference rotation, optional draggable brush sliders, watermark-free timelapse export, visible `.hsj` folder paths, and text auto-fit).

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

Each feature compiles as an independent Morphe patch paired with a runtime toggle under Settings > General Settings > HuashijieLibre. Patches unchecked at build time are omitted and leave no dead switches in the in-app menu. Patches are organized into seven groups:

| Module Group | Patch Count | Scope |
| :--- | :---: | :--- |
| Core Foundation | 2 | Startup signature check bypass and shared in-app settings screen |
| Localization and Typography | 3 | Runtime string replacement, external JSON dictionary loading, and custom font replacement |
| Privacy, Telemetry, and Account Visibility | 12 | Disabling analytics SDKs, blocking splash ads, and hiding domestic cloud or account entries |
| Workspace UI Visibility | 6 | Hiding online template promos, brush shop links, and domestic social share rows |
| Startup and System Sharing | 4 | Startup screen bypass and Android Share Sheet integration |
| Offline Profile and Brush Access | 3 | Offline user profile and local brush editing/scanning without account checks |
| Canvas Controls and Layout Fixes | 8 | Canvas gestures, watermark removal, `.hsj` path display, and layout/text fitting |

## Installation

Version 3.4.6 (build 313) is the sole verified target build. Obtain an unmodified `net.huanci.hsjpro` v3.4.6 APK (from an existing device backup or public APK archive) and verify that its SHA-256 checksum matches `1d5bfdcc32d9a593c3f34bc83c3af300ac19e691082b620bd89cfb46fc7271aa` before patching.

1. Install Morphe Manager on Android and open [**Add to Morphe**](https://morphe.software/add-source?github=Doni4c%2FHuashijieLibre), or paste `https://github.com/Doni4c/HuashijieLibre` inside the Sources tab (manifest fallback: `https://raw.githubusercontent.com/Doni4c/HuashijieLibre/main/patches-bundle.json`).
2. In the Apps tab, select 画世界Pro v3.4.6, tap No, I already have it, and choose your verified `base.apk` file from local storage.
3. Patch with the default selection (enable Expert mode in Morphe Manager settings beforehand to include or exclude individual modules), then tap Install and grant storage permission on first launch.

> [!WARNING]
> **Back Up Your Signing Key and `.hsj` Drafts After Your First Build.**
> Android ties future in-place updates to the signing certificate generated on your first Morphe build. Open Morphe Manager > Settings > Backup > Signing key > Export and store the backup file (`Morphe.p12` or `Morphe.keystore`) safely off-device. Back up `/storage/emulated/0/HuashijiePro/Draft/` before uninstalling or switching keys.

## Patch Catalog

Modules unchecked at build time are omitted from the APK, and installed modules expose matching runtime switches in General Settings. Floating Brush Sliders compiles as ON by default at patch time while starting OFF at runtime so drag handles stay hidden until enabled.

### 1. Core Foundation (2 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Native Signature Bypass | ON | Preserves original native byte signatures when modifying `libPaintLib.so` so patched builds pass startup integrity checks. |
| Libre In App Settings Panel | ON | Adds the HuashijieLibre settings screen in General Settings with update checks, credits, license info, and diagnostics export. |

### 2. Localization and Typography (3 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Language Pack Resources | ON | Bundles built-in Vietnamese, English, and Simplified Chinese JSON dictionaries inside the APK. |
| Dynamic External Translation Engine | ON | Translates UI strings at runtime from bundled or external JSON packs and keeps filesystem folder names untranslated. |
| Custom UI Font Injection | ON | Applies `.ttf` or `.otf` font files from `/sdcard/HuashijiePro/fonts/` across the interface while keeping bold and italic styles. |

### 3. Privacy, Telemetry, and Account Visibility (12 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Block Host Telemetry SDKs | ON | Stops the built-in `xCrash` and `MarsXlog` logging libraries from initializing at startup. |
| Block Umeng Analytics SDK | ON | Disables Umeng analytics initialization and background event collection. |
| Block Bugly Crash SDK | ON | Disables Tencent Bugly remote crash reporting initialization. |
| Suppress Splash Ad Loading | ON | Blocks startup splash ad requests independently of `Instant Launch to Studio`. |
| Hide Personalized Ads Setting | ON | Hides the personalized advertising switch in General Settings. |
| Hide Community Tabs | ON | Hides the community and discovery tabs on the home screen, leaving only local drafts. |
| Hide Top Bar Cloud Button | ON | Hides the cloud sync icon on the gallery top bar and blocks accidental taps. |
| Hide Canvas Popup Cloud Sync | ON | Hides cloud backup controls and the cloud tab inside the canvas settings popup. |
| Hide Draft Cloud Badge | ON | Hides cloud sync status icons and progress bars on draft thumbnails. |
| Hide Exit Cloud Sync Option | ON | Hides cloud upload options in the canvas exit dialog while keeping local save and discard actions. |
| Hide Baidu Cloud Backup Entry | ON | Hides the Baidu Netdisk backup entry in the navigation drawer. |
| Hide Drawer VIP Banner | ON | Hides the VIP membership banner in the navigation drawer. |

### 4. Workspace UI Visibility (6 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Hide Online Canvas Templates | ON | Hides the online template button on the New Canvas screen and centers the three local creation options. |
| Hide Online Brush Shop | ON | Hides the online store button in the quick brush selector while keeping all local brushes. |
| Hide Brush Library Shop | ON | Hides the online store button inside the full-screen Brush Library modal. |
| Block Brush Upgrade Tip | ON | Hides brush upgrade banners inside the brush selector. |
| Hide Domestic Export Social Icons | ON | Hides domestic social channel rows in the image export dialog while keeping local save actions. |
| Hide Timelapse Export Social Icons | ON | Hides domestic social channel buttons in the timelapse export dialog while keeping save and quality options. |

### 5. Startup and System Sharing (4 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Instant Launch to Studio | ON | Skips the startup countdown screen into the draft gallery while preserving external file open intents. |
| Native Android Share Sheet | ON | Routes artwork, PSD, PDF, brush, frame sequence, and `.hsj` exports through the standard Android Share Sheet. |
| Replay Android Share Sheet | ON | Routes timelapse video exports on the replay screen through the standard Android Share Sheet. |
| Export Sheet Share Button | ON | Adds a System Share button beside Album and File inside the image and timelapse export dialogs. |

### 6. Offline Profile and Brush Access (3 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Local Artist Profile | ON | Provides an offline profile in memory when signed out so login-gated local tools such as PDF export work without signing in. |
| Custom Brush Sovereignty | ON | Allows editing, renaming, and duplicating preset and imported brushes without account ownership checks. |
| External Local Brush Scanning | ON | Scans `/sdcard/HuashijiePro/ASBrush` on startup and keeps imported brush sets available offline. |

### 7. Canvas Controls and Layout Fixes (8 Patches)

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Lasso Tap-Outside Exit | ON | Exits active Lasso selection with a single tap outside the selection while preserving Magic Wand, Polygonal Lasso, Move/Transform, and active guide bars. |
| Floating Brush Sliders | ON (Runtime OFF) | Adds a 2x2 drag handle between the brush size and opacity sliders to reposition them and compact landscape sidebars. |
| Two-Finger Reference Rotation | ON | Enables two-finger rotation inside the floating reference window with an 8-degree anti-tilt deadzone during pinch zoom. |
| Watermark Free Timelapse Export | ON | Removes the corner brand watermark during timelapse playback and video export. |
| HSJ File Path Display | ON | Shows the active local `.hsj` draft and export folder paths inside Draft Storage Settings. |
| Canvas Popup HSJ Path Row | ON | Adds the active `.hsj` draft folder path inside the canvas settings tab with tap-to-copy clipboard support. |
| Canvas Popup Geometric Polish | ON | Adjusts spacing in canvas popups and reference windows and adapts the left tool grid between portrait and landscape columns. |
| Narrow Button Text Auto Fit | ON | Scales and wraps localized labels inside narrow buttons, tabs, and dialogs to prevent clipping. |

## Custom Languages and Fonts

You do not need to rebuild the APK to test a custom translation draft or change UI typography:

1. Copy a UTF-8 JSON language pack into `/storage/emulated/0/HuashijiePro/lang/`.
2. Copy `.ttf` or `.otf` font files into `/storage/emulated/0/HuashijiePro/fonts/`.
3. Open Settings > General Settings > HuashijieLibre and pick your file under Active language pack or Custom UI font for immediate reload.

## Contributing and Local Builds

Translators can contribute directly in the browser on [Hosted Weblate](https://hosted.weblate.org/projects/huashijielibre/) ([HuashijieLibre UI](https://hosted.weblate.org/projects/huashijielibre/ui/)) without editing JSON files or compiling Android projects. Manual JSON workflows, priority tiers, digital art terminology standards, and live device preview steps are documented in [TRANSLATING.md](TRANSLATING.md).

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