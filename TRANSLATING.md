<div align="center">

# Translating HuashijieLibre

**Translate HuashijiePro v3.4.6 using plain UTF-8 JSON dictionaries, with live on-device preview and automated validation checks.**

[![Keys](https://img.shields.io/badge/Keys-3%2C130%20ID%20%7C%203%2C817%20Text%20%7C%2033%20Pattern-0969DA?style=for-the-badge)](lang/_template_zh.json)
[![Localization](https://img.shields.io/badge/Localization-Multi--Language%20JSON%20%5B6%2C980%20entries%5D-1F883D?style=for-the-badge)](lang/)
[![GPL-3.0 License](https://img.shields.io/badge/License-GPL--3.0-DC2626?style=for-the-badge)](LICENSE)

[**Back to README**](README.md) | [**Chinese Source Template**](lang/_template_zh.json) | [**Bundled Packs**](lang/)

</div>

HuashijieLibre loads its entire interface from UTF-8 JSON files so contributors worldwide can add or refine translations without writing Java or Kotlin code or recompiling APKs. You can translate from the canonical Simplified Chinese source template or use an existing bundled pack under `lang/` as reference.

## Dictionary Structure

Inside `lang/`, every language pack maintains 100% key parity with `lang/_template_zh.json`, which contains the Simplified Chinese strings extracted from HuashijiePro v3.4.6 alongside internal `libre_*` preference keys.

Each JSON dictionary contains four top-level objects alongside `__metadata__`:

| JSON Object | Entry Count | Purpose in App | What to Edit |
| :--- | :---: | :--- | :--- |
| `strings_by_id` | 3,130 | Hex Android resource IDs such as `"0x7f110000"` | Keep left hex ID intact; translate right value |
| `strings_by_name` | 3,130 | Resource entry name mapping synchronized with `strings_by_id` | Keep left entry name intact; translate right value |
| `strings_by_text` | 3,817 | Direct Chinese UI strings and `libre_*` settings labels | Keep left source string intact; translate right value |
| `strings_by_value` | 33 | Dynamic counter and unit pattern templates | Keep left pattern intact; translate right format |

## Formatting Guidelines

1. **Preserve 100% key parity:** Keep every key across all four sections (`strings_by_id`, `strings_by_name`, `strings_by_text`, and `strings_by_value`) identical in spelling to `lang/_template_zh.json`, with no missing or extra keys.
2. **Keep format specifiers intact:** Placeholders such as `%s`, `%d`, `%1$s`, `%2$d`, and `\n` populate dynamic counters, canvas sizes, and brush parameters at runtime. Match placeholder types and counts even if word order changes.
3. **Keep toolbar labels compact:** Keep widely understood digital art terms (such as Layer, Canvas, Liquify, Curves, HSB, RGB, Alpha Lock, and Clipping Mask) concise so narrow tabs and icon labels fit without awkward wrapping.
4. **Use valid UTF-8 JSON and ASCII punctuation:** Avoid trailing commas, U+2014 em-dashes, U+2013 en-dashes, and U+2022 bullet characters, which fail repository validation.

## Testing Your Draft Live on Android

Copy `lang/_template_zh.json` (or an existing bundled pack in `lang/`) and rename it to match your target language code (such as `es.json`, `fr.json`, `ja.json`, `ko.json`, or `pt.json`). After translating the right-hand string values, copy the file onto your Android device inside:

```text
/storage/emulated/0/HuashijiePro/lang/
```

Open the patched HuashijiePro app, navigate to Settings > General Settings > Huashijie Libre, tap Active language pack, and select your JSON file. Menus, toolbars, brush editors, and export dialogs reload immediately so you can verify text fit live.

## Submitting Changes

You can open a Pull Request directly via GitHub's Web Editor by pressing `.` on [Doni4c/HuashijieLibre](https://github.com/Doni4c/HuashijieLibre) and adding or updating `lang/<language_code>.json`.

If you build locally with JDK 21 and Morphe GitHub Packages credentials configured, run the dictionary validator before pushing:

```powershell
.\gradlew.bat :patches:validateDictionaries --no-daemon
```

Pull Requests touching `lang/*.json` automatically run `.github/workflows/validate-translations.yml` to verify JSON syntax, four-section key parity, and punctuation rules.
