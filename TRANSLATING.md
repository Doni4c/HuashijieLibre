<div align="center">

# Translating HuashijieLibre

**Community localization guide for 画世界Pro v3.4.6 using plain UTF-8 JSON dictionaries, priority tiers, standardized art terminology, and live on-device preview.**

[![Format](https://img.shields.io/badge/Format-UTF--8%20JSON%20%7C%20100%25%20Key%20Parity-0969DA?style=for-the-badge)](lang/_template_zh.json)
[![Workflow](https://img.shields.io/badge/Preview-Hot%20Reload%20on%20Android-1F883D?style=for-the-badge)](#testing-your-translation-live-on-android)
[![GPL-3.0 License](https://img.shields.io/badge/License-GPL--3.0-DC2626?style=for-the-badge)](LICENSE)

[**Back to README**](README.md) | [**Chinese Source Template**](lang/_template_zh.json) | [**English Reference**](lang/en.json) | [**Bundled Packs**](lang/)

</div>

HuashijieLibre loads its entire user interface from external UTF-8 JSON dictionaries so artists and translators worldwide can contribute without Java, Kotlin, or Android Studio. Inspired by open-source art studio projects such as Krita, this guide organizes translation work by artist-facing priority, provides a shared digital art glossary, and explains which legacy third-party SDK strings you can safely skip.

## Quick Start

1. Check [lang/](lang/) and open Pull Requests first to see whether your language already has an active translation or reviewer.
2. Copy `lang/_template_zh.json` (canonical Simplified Chinese source) or `lang/en.json` (helpful if you prefer translating from English while cross-checking Chinese) and rename it with your BCP-47 language code (for example `es.json`, `fr.json`, `ja.json`, `ko.json`, or `pt-BR.json`).
3. Update the `__metadata__` block (`locale` and `name`), translate starting with Priority 1 strings below, and keep all JSON keys untouched.
4. Preview your file directly on Android, then open a Pull Request.

## Translation Priority Tiers (Incremental Contributions Welcome)

You do not have to translate every entry in a single Pull Request. Untranslated strings may remain in Simplified Chinese (or English if branching from `en.json`) while keeping 100% key parity so the file passes validation immediately. Focus your effort in this order:

| Priority | Scope | Why It Matters |
| :--- | :--- | :--- |
| Priority 1: Core Studio and Libre Settings | `libre_*` settings keys, main gallery, canvas toolbar, brush editor, layer panel, color picker, selection tools, export dialogs | Covers roughly 90% of daily artist workflows inside 画世界Pro. |
| Priority 2: Secondary Tools and Dialogs | Filters, animation timeline, perspective/symmetry guides, keyboard shortcuts, undo/redo history labels, storage notices | Completes everyday studio polish across advanced workflows. |
| Priority 3: Safe to Skip (Leave Untouched) | Ad network logs (`AdFetcher`, `MRAID`, `adscope_*`, `bsyad_*`, `ms_*`), Chinese SMS/carrier login SDKs (`smssdk_*`, `fly_verify_*`, `ssdk_*`, `ct_account_*`), domestic social/VIP promos, AndroidX/Material internal strings | Disabled or unreachable in offline studio mode; translating them wastes contributor time. |

## Protected Keys (Do Not Translate)

Never alter the values of technical or filesystem entries even when they look like normal words:

1. **Filesystem folder names:** `0x7f110803` (`sdcard_root_folder`: `"HuashijiePro"`) and `0x7f110077` (`album_folder`: `"画世界Pro"`). Translating these causes fragmented DCIM or draft folders; the runtime engine also enforces protection on both IDs.
2. **Vector paths and class names:** SVG strings such as `0x7f110670` through `0x7f110673` (`path_password_*`) and Java class paths such as `com.google.android.material...`.
3. **Format specifiers and escapes:** Placeholders such as `%s`, `%d`, `%1$s`, `%2$d`, `%%`, and `\n` inject live counters, canvas dimensions, and brush metrics at runtime. Preserve placeholder types and counts even when target grammar reorders them.

## Digital Art Terminology Glossary

Consistent studio terminology keeps menus intuitive for artists switching from Krita, Procreate, or Clip Studio Paint. Match established digital painting terms in your target language rather than translating Chinese characters literally:

| Chinese Source | Standard English Reference | UI Context and Guidance |
| :--- | :--- | :--- |
| `画布` | Canvas | Main drawing surface. |
| `草稿箱` | Drafts (or Gallery) | Local project library on the home screen. |
| `图层` / `图层组` | Layer / Layer Group | Keep concise for the narrow layer sidebar. |
| `剪贴蒙版` / `图层蒙版` | Clipping Mask / Layer Mask | Distinguish clearly between clipping and alpha masks. |
| `锁定透明度` | Alpha Lock | Locks transparent pixels on the active layer. |
| `不透明度` | Opacity | Keep short to fit slider headers. |
| `混合模式` (`正片叠底`, `滤色`, `叠加`) | Blending Mode (Multiply, Screen, Overlay) | Use standard graphics software naming in your locale. |
| `套索` / `魔棒` | Lasso / Magic Wand | Selection tools. |
| `液化` / `涂抹` | Liquify / Smudge | Canvas deformation and blending tools. |
| `吸管` | Eyedropper | Color sampling tool. |
| `抖动修正` | Stabilizer | Stroke smoothing control. |
| `洋葱皮` | Onion Skin | Animation frame ghosting overlay. |
| `画布盖印` | Merge Visible to New Layer (Stamp) | Flattens visible layers into a new top layer. |
| `柳叶笔` | Lasso Fill Brush (or Willow Pen) | Closed-shape fill brush unique to 画世界Pro. |

## Dictionary Structure

Inside `lang/`, every language pack maintains strict 100% key parity with `lang/_template_zh.json` across four synchronized objects alongside `__metadata__`:

| JSON Object | Purpose in App | How to Edit |
| :--- | :--- | :--- |
| `strings_by_id` | Hex Android resource IDs such as `"0x7f110000"` | Keep left hex ID intact; translate right value. |
| `strings_by_name` | Resource entry names matching `strings_by_id` | Keep left entry name intact; translate right value. |
| `strings_by_text` | Direct Chinese UI strings, `StringFog` strings, and `libre_*` settings labels | Keep left source key intact; translate right value. |
| `strings_by_value` | Dynamic counter and unit pattern templates | Keep left pattern intact; translate right format. |

> [!TIP]
> Because `strings_by_id`, `strings_by_name`, and `strings_by_text` mirror overlapping UI labels at runtime, use your editor's Find and Replace across sections when translating common single-word terms (such as `"画笔"` or `"图层"`) so every surface stays consistent.

## Formatting and Hygiene Rules

1. **Strict 100% key parity:** Keep every key across all four sections identical in spelling to `lang/_template_zh.json`, with no missing or extra keys.
2. **Keep toolbar labels compact:** Narrow icon buttons, tabs, and brush parameter headers have tight horizontal space; favor concise idioms over literal multi-word explanations.
3. **Valid UTF-8 JSON and clean punctuation:** Avoid trailing commas, U+2014 em-dashes, U+2013 en-dashes, and U+2022 bullet characters, all of which fail automated repository validation.
4. **Consistent product names:** Keep the patch suite name as `HuashijieLibre` and refer to the host app as `画世界Pro` (do not alter `/HuashijiePro/` filesystem paths).

## Testing Your Translation Live on Android

1. Copy your JSON file onto your Android device inside:

```text
/storage/emulated/0/HuashijiePro/lang/
```

2. Open patched 画世界Pro, go to Settings > General Settings > HuashijieLibre, tap Active language pack, and select your file for instant hot reload.
3. Verify truncation and wrapping on the main gallery, canvas toolbars (both portrait and landscape), brush settings, layer menu, and export sheet.
4. **Important when upgrading releases:** Files stored in `/storage/emulated/0/HuashijiePro/lang/` override the bundled dictionaries inside the APK. Delete your test file from that folder once your translation is merged and shipped in a new release so you receive future bundled updates automatically.

## Submitting Changes

1. Fork [Doni4c/HuashijieLibre](https://github.com/Doni4c/HuashijieLibre) or press `.` on GitHub to open the Web Editor, add or update `lang/<language_code>.json`, and open a Pull Request.
2. Every Pull Request touching `lang/*.json` automatically runs `.github/workflows/validate-translations.yml` to verify JSON syntax, four-section key parity, and punctuation rules.
3. If you build locally with JDK 21 and Morphe GitHub Packages credentials configured, run the validator before pushing:

```powershell
.\gradlew.bat :patches:validateDictionaries --no-daemon
```