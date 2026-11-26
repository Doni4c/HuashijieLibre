<div align="center">

# Translating HuashijieLibre

**Community localization guide for 画世界Pro v3.4.6 using plain UTF-8 JSON dictionaries, automated CI validation, standardized art terminology, and live on-device preview.**

[![Format](https://img.shields.io/badge/Format-UTF--8%20JSON%20%7C%20100%25%20Key%20Parity-0969DA?style=for-the-badge)](lang/_template_zh.json)
[![Glossary](https://img.shields.io/badge/Glossary-Standard%20Art%20Terms-6E40C9?style=for-the-badge)](GLOSSARY.md)
[![Workflow](https://img.shields.io/badge/Preview-Hot%20Reload%20on%20Android-1F883D?style=for-the-badge)](#testing-your-translation-live-on-android)
[![GPL-3.0 License](https://img.shields.io/badge/License-GPL--3.0-DC2626?style=for-the-badge)](LICENSE)

[**Back to README**](README.md) | [**Terminology Glossary**](GLOSSARY.md) | [**Chinese Source Template**](lang/_template_zh.json) | [**English Reference**](lang/en.json) | [**Bundled Packs**](lang/)

</div>

> [!NOTE]
> **10-Second Start:**
> 1. **Web translation portal:** Direct GitHub Pull Requests are currently the active submission channel (when Hosted Weblate is enabled, its link will appear right here).
> 2. **Pick your path:** Edit on the GitHub website with zero coding tools ([Track A](#track-a-web-only-no-git-or-cli-required)), or initialize and preview live on Android using our Python helper ([Track B](#track-b-local-tooling-and-live-android-preview)).
> 3. **Incremental PRs welcome:** Leave unfinished entries in Chinese or English; GitHub CI checks key parity, placeholders, and formatting automatically on every Pull Request.

HuashijieLibre loads its interface from external UTF-8 JSON dictionaries so artists and translators worldwide can contribute without Java, Kotlin, or Android Studio. Following the community model used by open-source art studios such as Krita, each language is reviewed by volunteer Language Maintainers against a shared art glossary and validated automatically before merge.

## Track A: Web-Only (No Git or CLI Required)

1. Open [lang/](lang/) and [open Pull Requests](https://github.com/Doni4c/HuashijieLibre/pulls) to check whether your language already has an active pack or reviewer.
2. Open [lang/_template_zh.json](lang/_template_zh.json) (canonical Simplified Chinese source) or [lang/en.json](lang/en.json) (English reference), copy its raw contents, and create `lang/<lang_code>.json` inside your fork using the GitHub web editor (press `.` on the repository page or use **Add file > Create new file**). Use a standard BCP-47 filename such as `es.json`, `fr.json`, `ja.json`, `ko.json`, or `pt-BR.json`.
3. Update `__metadata__.locale` and `__metadata__.name` at the top of the file, translate starting with Priority 1 entries, and leave all JSON keys untouched.
4. Open a Pull Request using the translation checklist template. `.github/workflows/validate-translations.yml` runs automatically and posts a readable validation table in the workflow summary showing any hard errors, soft warnings, and translation progress.

## Track B: Local Tooling and Live Android Preview

1. Clone your fork locally (requires Python 3.9+ with standard library only; no Android SDK needed unless running Gradle):

```powershell
# Create a new pack from _template_zh.json (add --from-en to start from English fallback)
python i18n/lang_helper.py init ja --name "日本語"
```

2. Translate Priority 1 strings first while consulting [GLOSSARY.md](GLOSSARY.md). When `_template_zh.json` updates or when you want identical Chinese terms synced across tables, run:

```powershell
python i18n/lang_helper.py sync lang/ja.json --sync-duplicates
```

3. Run the local validator (same checks as GitHub CI):

```powershell
python i18n/validate_dictionaries.py
```

4. Preview live on Android ([steps below](#testing-your-translation-live-on-android)) before opening your Pull Request.

## Translation Priority Tiers

You do not need to translate every entry in one Pull Request. Untranslated strings may stay in Simplified Chinese (or English if initialized with `--from-en`) while maintaining 100% key parity:

| Priority | Scope | Why It Matters |
| :--- | :--- | :--- |
| Priority 1: Core Studio and Libre Settings | `libre_*` settings keys, main gallery, canvas toolbar, brush editor, layer panel, color picker, selection tools, export dialogs | Covers roughly 90% of daily artist workflows inside 画世界Pro. |
| Priority 2: Secondary Tools and Dialogs | Filters, animation timeline, perspective/symmetry guides, keyboard shortcuts, undo/redo history labels, storage notices | Completes everyday studio polish across advanced workflows. |
| Priority 3: Safe to Skip (Leave Untouched) | Ad network logs (`AdFetcher`, `MRAID`, `adscope_*`, `bsyad_*`, `ms_*`), Chinese SMS/carrier login SDKs (`smssdk_*`, `fly_verify_*`, `ssdk_*`, `ct_account_*`), domestic social/VIP promos, AndroidX/Material internal strings | Disabled or unreachable in offline studio mode; translating them wastes contributor time. |

## Protected Keys and Naming Standards

1. **Filesystem folder keys (do not alter values):** `0x7f110803` (`sdcard_root_folder`: `"HuashijiePro"`) and `0x7f110077` (`album_folder`: `"画世界Pro"`). Modifying these causes fragmented DCIM or draft directories; the runtime engine also enforces protection on both IDs.
2. **Vector paths and class names:** Leave SVG strings (`0x7f110670` through `0x7f110673` `path_password_*`) and Java class paths (`com.google.android.material...`) untouched.
3. **Format specifiers, escapes, and tags:** Preserve `%s`, `%d`, `%.1f`, `%1$s`, `%2$d`, `%%`, `\n` line breaks, and HTML tags (`<font...>`, `<a>`) from the Chinese source. The CI validator blocks PRs with missing, reordered non-positional, or malformed placeholders (such as `%1$d` truncated to `%1`).
4. **Canonical brand and folder names:** Write the patch project as `HuashijieLibre` (never insert a space between `Huashijie` and `Libre`), the original host app as `画世界Pro` (no space before `Pro`), and Android external storage paths as `HuashijiePro` (for example `/storage/emulated/0/HuashijiePro/lang/`).

## Dictionary Structure

Inside `lang/`, every pack maintains 100% key parity with `lang/_template_zh.json` across `__metadata__` and four dictionary tables:

| JSON Object | Purpose in App | How to Edit |
| :--- | :--- | :--- |
| `__metadata__` | Stores `locale`, `name`, `app`, `version`, `total_strings` | Update `locale` and `name` only; keep `app`, `version`, and numeric `total_strings` intact. |
| `strings_by_id` | Hex Android resource IDs (`"0x7f110000"`) | Keep left hex ID intact; translate right value. |
| `strings_by_name` | Resource entry names paired 1-to-1 with `strings_by_id` | Keep left entry name intact; translate right value to match `strings_by_id`. |
| `strings_by_text` | Direct Chinese UI strings, `StringFog` strings, and `libre_*` settings labels | Keep left source key intact; translate right value. |
| `strings_by_value` | Dynamic counter and unit pattern templates | Keep left pattern intact; translate right format. |

> [!TIP]
> Because the same Chinese string often appears across `strings_by_id`, `strings_by_name`, and `strings_by_text`, run `python i18n/lang_helper.py sync lang/<code >.json --sync-duplicates` after editing to fill matching entries across tables automatically.

## Automated Checks and Hygiene Rules

GitHub Actions and `:patches:validateDictionaries` enforce two severity levels on every change:

1. **Hard errors (block merge until fixed):** Invalid JSON syntax, UTF-8 BOM, CRLF line endings, prohibited punctuation (U+2014 em-dash, U+2013 en-dash, U+2022 bullet), missing/broken `__metadata__`, missing or extra keys in any of the 4 sections, empty values, or mismatched placeholders / `\n` counts / HTML tags.
2. **Soft warnings (non-blocking review checklist):** Short UI labels whose translation exceeds 18 to 24+ characters (risking toolbar truncation), inconsistent translations for the same Chinese source across tables, modified protected folder keys, or spaced brand names (such as separating `Huashijie` and `Libre`, or inserting a space before `Pro`).

## Testing Your Translation Live on Android

1. Copy your JSON file onto your Android device inside:

```text
/storage/emulated/0/HuashijiePro/lang/
```

2. Open patched 画世界Pro, go to Settings > General Settings > HuashijieLibre, tap Active language pack, and select your file for instant hot reload without rebuilding the APK.
3. Verify truncation and wrapping on the main gallery, canvas toolbars (portrait and landscape), brush settings, layer menu, and export sheet.
4. **Clean up after release:** Files inside `/storage/emulated/0/HuashijiePro/lang/` override bundled dictionaries inside the APK. Delete your local test file once your PR is merged and shipped in a new release.

## Krita-Style Language Maintainer Governance

1. **Role:** Each active language can have one or more volunteer **Language Maintainers** listed in [.github/CODEOWNERS](.github/CODEOWNERS). Maintainers are automatically tagged on Pull Requests modifying their `lang/<code >.json`.
2. **Responsibilities:** Review PRs for glossary consistency against [GLOSSARY.md](GLOSSARY.md), inspect CI soft warnings (short button overflow and cross-table drift), verify Priority 1 screens on device when possible, and approve ready PRs so the repository owner can merge quickly without manual translation audits.
3. **Volunteering:** Contributors who have translated or thoroughly reviewed Priority 1 for a language can open an issue or PR note asking to be added under `.github/CODEOWNERS` for that file.
4. **Hosted Weblate readiness:** Maintainers considering web-based translation integration can inspect [docs/TRANSLATION_PLATFORM.md](docs/TRANSLATION_PLATFORM.md) for compatibility constraints and the deduplicated flat-JSON bridge (`export-flat` / `import-flat`).

## Local Gradle Verification

If you have JDK 21 and Morphe GitHub Packages credentials configured (`GITHUB_ACTOR` and `GITHUB_TOKEN`), run the Gradle gate locally before pushing:

```powershell
.\gradlew.bat :patches:validateDictionaries --no-daemon
```
