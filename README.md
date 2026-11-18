<div align="center">

# HuashijieLibre

**Open-source Morphe patch suite that brings multi-language JSON localization, offline workflow access, and canvas ergonomics to HuashijiePro v3.4.6.**

[![Pre-release](https://img.shields.io/badge/Pre--release-v1.0.0--beta.1-0969DA?style=for-the-badge&logo=github)](https://github.com/Doni4c/HuashijieLibre/releases)
[![Target App](https://img.shields.io/badge/Target-net.huanci.hsjpro%20v3.4.6%20%5B313%5D-1F883D?style=for-the-badge&logo=android)](https://github.com/Doni4c/HuashijieLibre/releases)
[![Patches](https://img.shields.io/badge/Patches-37%20Atomic%20Modules-8250DF?style=for-the-badge)](#patch-catalog)
[![Morphe Patcher](https://img.shields.io/badge/Morphe%20Patcher-v2.3.0-BF3989?style=for-the-badge)](https://morphe.software/)
[![Localization](https://img.shields.io/badge/Localization-Multi--Language%20JSON-D97706?style=for-the-badge)](TRANSLATING.md)
[![GPL-3.0 License](https://img.shields.io/badge/License-GPL--3.0-DC2626?style=for-the-badge)](LICENSE)

[**Add to Morphe**](https://morphe.software/add-source?github=Doni4c%2FHuashijieLibre) | [**Releases**](https://github.com/Doni4c/HuashijieLibre/releases) | [**Translating Guide**](TRANSLATING.md) | [**Patch Catalog**](#patch-catalog)

</div>

## Overview

HuashijiePro (developed by Shijiazhuang Huanci Network Technology Co., Ltd.) delivers fast brush physics, high layer limits on mid-range hardware, frame-by-frame animation, and subscription-free PSD export. Because its latest releases primarily ship through mainland Chinese distribution channels, international users on v3.4.6 encounter a Simplified Chinese-only interface, +86 phone login gates around local tools such as PDF export and custom brush parameter editing, splash advertisements, and domestic analytics SDKs.

HuashijieLibre provides 37 modular Morphe patches that translate the interface through externalizable JSON language packs, restore offline access to local studio tools, hide domestic social and cloud clutter, and refine canvas usability (tap-outside Lasso exit, two-finger reference window rotation, optional floating brush sliders, watermark-free timelapse exports via Android Share Sheet, transparent `.hsj` storage paths, custom `.ttf`/`.otf` UI typography, and adaptive text fitting).

## Compatibility and Artifacts

| Property | Specification |
| :--- | :--- |
| Package ID | `net.huanci.hsjpro` |
| Target Version and Build | v3.4.6 (versionCode 313) |
| Clean Base APK SHA-256 | `1d5bfdcc32d9a593c3f34bc83c3af300ac19e691082b620bd89cfb46fc7271aa` |
| Supported ABIs | Universal (arm64-v8a, armeabi-v7a) matching stock APK |
| Patch Engine | Morphe Patcher 2.3.0 (`app.morphe.patches` 2.1.0) |
| Release Artifact Policy | Patch bundle only (`patches-1.0.0-beta.1.mpp`); no modified APKs or keystores hosted |

## Architecture Overview

Each capability is compiled as an independent Morphe patch and paired with runtime toggle registration under Settings > General Settings > Huashijie Libre. Modules excluded during patching do not leave dead switches in the in-app menu. Modules are organized into seven functional groups according to architectural scope:

| Module Group | Patch Count | Classification Boundary |
| :--- | :---: | :--- |
| Core Foundation | 2 | App integrity bypasses and in-app preference infrastructure shared by all feature modules |
| Localization and Typography | 3 | String interception, external JSON dictionary loading, and global typeface replacement |
| Privacy, Telemetry, and Account Decluttering | 12 | Background analytics SDK deactivation, ad suppression, and domestic cloud/login entry removal |
| Workspace UI Decluttering | 6 | Hiding promotional store entries and non-local social grids across studio popups |
| Startup and System Sharing | 3 | Boot flow acceleration and replacement of proprietary share sheets with Android system dialogs |
| Offline Profile and Brush Access | 3 | Unlocking local creation tools and brush storage gated behind remote account checks |
| Canvas Workflow and Layout Geometry | 8 | Interactive drawing gestures, media export cleanup, storage visibility, and UI geometry fixes |

## Installation

Version 3.4.6 (build 313) is the sole verified target build. Obtain an unmodified `net.huanci.hsjpro` v3.4.6 APK (from an existing device backup or public APK archive) and verify that its SHA-256 checksum matches `1d5bfdcc32d9a593c3f34bc83c3af300ac19e691082b620bd89cfb46fc7271aa` before patching.

1. Install Morphe Manager on Android and open [**Add to Morphe**](https://morphe.software/add-source?github=Doni4c%2FHuashijieLibre), or paste `https://github.com/Doni4c/HuashijieLibre` inside the Sources tab (manifest fallback: `https://raw.githubusercontent.com/Doni4c/HuashijieLibre/main/patches-bundle.json`).
2. In the Apps tab, select HuashijiePro v3.4.6, tap No, I already have it, and pick your verified `base.apk` file from local storage.
3. Patch with the default selection (enable Expert mode in Morphe Manager settings beforehand if you wish to include or exclude individual patches), then tap Install when finished and grant storage permission on first launch.

> [!WARNING]
> **Back Up Your Signing Key and `.hsj` Drafts After Your First Build.**
> Android ties future in-place updates to the signing certificate generated on your first Morphe build. Open Morphe Manager > Settings > Backup > Signing key > Export and store the backup file (`Morphe.p12` or `Morphe.keystore`) safely off-device. Back up `/storage/emulated/0/HuashijiePro/Draft/` before uninstalling or switching keys.

## Patch Catalog

Modules disabled at build time are omitted cleanly from the APK, and installed modules expose matching runtime switches in General Settings. Floating Brush Sliders compiles as ON by default at patch time while starting OFF at runtime so drag handles stay hidden until you explicitly turn them on.

### 1. Core Foundation (2 Patches)

**Classification criteria:** This group is restricted to low-level application stability and shared configuration plumbing that other patches depend on (`Native Signature Bypass` and `Libre In App Settings Panel`). Any module that does not provide foundational launch compatibility or host-wide settings dispatch belongs in a feature group below rather than here.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Native Signature Bypass | ON | Prevents native signature checks from rejecting patched APK builds during startup. |
| Libre In App Settings Panel | ON | Injects the Huashijie Libre settings screen grouped by workflow, including an asynchronous GitHub update checker, Credits, license details, and diagnostics export. |

### 2. Localization and Typography (3 Patches)

**Classification criteria:** This group owns resource bundling, runtime text interception, and font rendering across Android Views, dialogs, and custom Canvas draws. Patches that modify specific layout bounds or spacing without performing text translation or font injection belong to Canvas Workflow and Layout Geometry instead.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Language Pack Resources | ON | Bundles built-in JSON dictionaries in `lang/` alongside the Simplified Chinese reference `_template_zh.json`. |
| Dynamic External Translation Engine | ON | Intercepts UI strings across views, dialogs, popups, adapters, and `StringFog` calls with zero-flicker Frame-0 rendering and hot-reload support. |
| Custom UI Font Injection | ON | Applies `.ttf` or `.otf` typefaces from `/sdcard/HuashijiePro/fonts/` across the app while preserving bold and italic weights. |

### 3. Privacy, Telemetry, and Account Decluttering (12 Patches)

**Classification criteria:** This group targets background tracking libraries, ad network loaders, and cloud/account surfaces that require network sign-in or transmit usage telemetry. Offline feature unlocking goes to Offline Profile and Brush Access, while shop/social icon cleanup inside local creative panels goes to Workspace UI Decluttering.

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
| Hide Exit Cloud Sync Option | ON | Hides cloud upload toggles inside the canvas exit prompt while keeping local save and exit actions. |
| Hide Baidu Cloud Backup Entry | ON | Hides the Baidu Netdisk backup entry in General Settings. |
| Hide Drawer VIP Banner | ON | Hides the promotional VIP membership banner inside the navigation drawer. |

### 4. Workspace UI Decluttering (6 Patches)

**Classification criteria:** This group removes promotional storefronts, online template promos, and domestic third-party social share rows from local creation popups without altering underlying export or sharing intents. Behavior that reroutes system intents belongs to Startup and System Sharing.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Hide Online Canvas Templates | ON | Hides the online template button on `CreateCanvasActivity` and centers the remaining three local creation options. |
| Hide Online Brush Shop | ON | Hides the online store entry inside the quick canvas brush selector while preserving all local brushes. |
| Hide Brush Library Shop | ON | Hides the online store button inside the full-screen Brush Library modal independently of the quick picker. |
| Block Brush Upgrade Tip | ON | Suppresses promotional brush upgrade banners inside `SelectPenView`. |
| Hide Domestic Export Social Icons | ON | Hides domestic social channel rows in the image export panel while keeping local export actions intact. |
| Hide Timelapse Export Social Icons | ON | Hides domestic social channel buttons in the timelapse export panel while keeping save and quality options intact. |

### 5. Startup and System Sharing (3 Patches)

**Classification criteria:** This group modifies top-level navigation and Android OS integration, specifically accelerating cold boot navigation and replacing proprietary sharing SDK flows with `Intent.createChooser`.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Instant Launch to Studio | ON | Bypasses the `FlashActivity` splash countdown and preloads brushes directly into the draft gallery. |
| Native Android Share Sheet | ON | Routes artwork and `.hsj` exports in `PaintActivity` through Android's native Share Sheet instead of domestic ShareSDK. |
| Replay Android Share Sheet | ON | Routes speedpaint exports in `ReplayActivity` through the standard Android Share Sheet. |

### 6. Offline Profile and Brush Access (3 Patches)

**Classification criteria:** This group handles local entitlement and storage sovereignty, supplying in-memory offline account stubs and preserving user brush files when operating completely offline without mainland phone verification.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Local Artist Profile | ON | Supplies an offline profile in memory so login-gated local tools such as PDF export function without an account. |
| Custom Brush Sovereignty | ON | Restores editing, renaming, and duplicating permissions for preset and locally imported brushes offline. |
| External Local Brush Scanning | ON | Scans `/sdcard/HuashijiePro/ASBrush` on startup and preserves imported brush packs during offline sessions. |

### 7. Canvas Workflow and Layout Geometry (8 Patches)

**Classification criteria:** This group encapsulates studio ergonomics, interactive touch gestures inside `PaintActivity`, output media cleanup, explicit storage path transparency, and programmatic geometric alignment/auto-fit layout fixes for non-Chinese scripts.

| Patch Name | Default | Behavior |
| :--- | :---: | :--- |
| Lasso Tap-Outside Exit | ON | Dismisses active selection toolbars with a single tap outside the panel without interrupting brush strokes. |
| Floating Brush Sliders | ON (Runtime OFF) | Adds a draggable handle above the brush size and opacity sliders once enabled in General Settings. |
| Two-Finger Reference Rotation | ON | Enables free two-finger rotation gestures inside the floating reference image window. |
| Watermark Free Timelapse Export | ON | Removes the corner brand watermark during `ReplayActivity` playback and video export. |
| HSJ File Path Display | ON | Displays explicit local `.hsj` draft and export folder cards inside Draft Storage Settings. |
| Canvas Popup HSJ Path Row | ON | Appends a local `.hsj` folder info row inside the `PaintActivity` canvas settings tab. |
| Canvas Popup Geometric Polish | ON | Balances canvas popup rows, compacts scroll cushions, centers icon clusters, and refines floating toolbars. |
| Narrow Button Text Auto Fit | ON | Scales and wraps localized labels inside tight buttons, tabs, and dialogs to prevent text truncation. |

## Custom Languages and Fonts

You do not need to rebuild the APK to test a custom translation draft or change UI typography:

1. Copy a UTF-8 JSON language pack into `/storage/emulated/0/HuashijiePro/lang/`.
2. Copy `.ttf` or `.otf` font files into `/storage/emulated/0/HuashijiePro/fonts/`.
3. Open Settings > General Settings > Huashijie Libre and pick your file under Active language pack or Custom UI font for immediate reload.

## Contributing and Local Builds

Translators can contribute new packs without writing code or compiling Android projects. Every pack in `lang/` maintains strict 100% key parity with `lang/_template_zh.json`. Detailed key format rules and device preview steps are documented in [TRANSLATING.md](TRANSLATING.md).

Patch implementations live under `patches/src/main/kotlin/app/morphe/patches/huashijie/` across seven packages matching the catalog groups above, built against `app.morphe.patches` 2.1.0 and Morphe Patcher 2.3.0. Runtime controllers under `extensions/huashijie-libre/` compile into extension DEX (`classes6.dex`), routing `classes4.dex` hooks through `TranslationEngine.dispatch` to stay beneath the 65,535-method reference ceiling.

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

1. **Independent project:** HuashijieLibre is an independent open-source Morphe patch collection not affiliated with Shijiazhuang Huanci Network Technology Co., Ltd. No copyrighted APKs, pre-patched binaries, or signing keystores are hosted in this repository.
2. **Modular control:** Every patch is independently toggleable at patch time and exposed as a live preference in General Settings.
