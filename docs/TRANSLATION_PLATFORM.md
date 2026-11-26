# Translation Platform Evaluation and Setup Guide (Hosted Weblate vs Crowdin Open Source)

This document records an engineering assessment of third-party localization platforms against the actual `lang/*.json` dictionary schema used by **HuashijieLibre** (`GPL-3.0`, targeting **画世界Pro** v3.4.6). It avoids speculative configuration files and provides tested local bridge commands so the project owner can enable web translation with minimal maintenance overhead.

## 1. Real Schema Constraints in HuashijieLibre

Inspecting `lang/_template_zh.json`, `lang/vi.json`, and `lang/en.json` reveals four concrete friction points for automated web translation platforms:

1. **Mixed `__metadata__` and string tables in one file:** Every JSON file places `__metadata__` (`locale`, `name`, `app`, `version`, `total_strings`) at the top level alongside the four string objects. Standard nested JSON parsers expose string fields inside `__metadata__` (`app`, `version`, `locale`) as translatable keys unless explicitly filtered or stripped, risking accidental corruption of `net.huanci.hsjpro` or `zh-CN`.
2. **2.65x cross-table string duplication:** Across the four tables (`strings_by_id`: 3,130, `strings_by_name`: 3,130, `strings_by_text`: 3,820, `strings_by_value`: 36), there are **10,116** leaf entries representing only **3,824** unique Chinese source strings (`2,777` values in `strings_by_id` overlap directly with `strings_by_text`). Translating the raw files directly on any platform forces translators to see common labels (`画笔`, `图层`) three or more times and produces cross-table drift (already present in ~30 entries each in `vi.json` and `en.json`).
3. **Opaque hex IDs vs runtime-skipped names:** `TranslationEngine.java` reads `strings_by_id` (`0x7f110000`), `strings_by_text`, and `strings_by_value`, while skipping `strings_by_name` (`Account_security`) at runtime. On raw JSON platforms, translators editing `strings_by_id` only see hex IDs without semantic resource names.
4. **Strict typography and formatting rules:** Downstream validation enforces UTF-8 without BOM, Unix `LF`, `0` em-dashes (`U+2014`), `0` en-dashes (`U+2013`), `0` bullets (`U+2022`), and strict placeholder/newline/HTML-tag parity. Platform auto-punctuation or re-serialization can break these checks unless gated by CI.

## 2. Hosted Weblate (Libre Plan) vs Crowdin Open Source

| Evaluation Dimension | Hosted Weblate (Libre Plan) | Crowdin Open Source |
| :--- | :--- | :--- |
| License alignment (`GPL-3.0`) | Built on `GPL-3.0`; Libre hosting explicitly targets public OSI/FSF repositories with zero vendor lock-in or self-host fallback. | Free Open Source license tier requires manual application approval; platform itself is proprietary SaaS. |
| Nested JSON & base template | Native `json-nested` format with explicit monolingual template (`lang/_template_zh.json`) and `lang/*.json` filemask. | Supports nested JSON, but `crowdin.yml` has **no native parameter to ignore specific nested keys** (`__metadata__`); requires Marketplace apps (`File Import Tweak`) or CI pre/post-processing scripts. |
| Duplicate string workload | Supports string propagation across components and translation memory, though raw 10,116-leaf nested JSON still exposes `__metadata__` and 4 tables unless filtered or bridged. | Supports Translation Memory auto-fill, but raw JSON still exposes `__metadata__` unless stripped before upload. |
| Built-in QA checks | Built-in printf placeholder checks (`%s`, `%1$d`), unchanged check, length checks, and automated GitHub PR workflow. | Built-in placeholder QA and GitHub integration, weaker custom character-ban enforcement out of the box. |
| Repo configuration reality | Component format (`json-nested`, template, PR branch) is configured in Weblate's web UI or API, **not** by committing a guessed `.weblate` file (`.weblate` is only a `wlc` CLI client pointer). | Uses `crowdin.yml`, but a minimal `crowdin.yml` alone cannot protect `__metadata__` or deduplicate the 4 tables safely. |

**Recommendation:** Choose **Hosted Weblate (Libre Plan)** as the primary community web platform, paired with `.github/workflows/validate-translations.yml` as the mandatory merge gate. Do not commit speculative `.weblate` or `crowdin.yml` config files that cannot safely protect `__metadata__` on their own.

## 3. Recommended Integration Modes

### Mode A: Deduplicated Flat Bridge (Recommended Lowest Friction)

Before connecting Weblate (or when importing large community packs), use the built-in flat bridge in `i18n/lang_helper.py`:

1. Export a deduplicated flat JSON (~3,824 keys using semantic `res.<name>`, `libre.<key>`, and `txt.<hash>` identifiers, excluding `__metadata__` and protected filesystem IDs `0x7f110803` / `0x7f110077`):

```powershell
python i18n/lang_helper.py export-flat lang/_template_zh.json -o scratch/source_flat.json
```

2. Translate the flat file on Weblate/Crowdin (zero `__metadata__` risk, zero duplicate translation across 4 tables, ~62% fewer strings to translate).
3. Import the translated flat JSON back into the full 4-table dictionary and verify with the validator:

```powershell
python i18n/lang_helper.py import-flat scratch/ja_flat.json -t lang/ja.json
python i18n/validate_dictionaries.py
```

### Mode B: Direct `lang/*.json` Weblate PRs + CI Gate

If you prefer Weblate reading `lang/*.json` directly with zero intermediate export files:

1. Configure Weblate to push to a dedicated PR branch (never direct push to `main`).
2. Lock or exclude `__metadata__.*`, `0x7f110803`, `0x7f110077`, `sdcard_root_folder`, and `album_folder` in Weblate using read-only / regex filtering (`^__metadata__\.`) so translators only see UI strings.
3. Let `.github/workflows/validate-translations.yml` inspect every Weblate PR automatically, and run `python i18n/lang_helper.py sync lang/<code >.json --sync-duplicates` when unifying cross-table terms.

## 4. One-Time Setup Checklist for Project Owner (When Enabling Weblate)

1. Apply for the **Hosted Weblate Libre Plan** for `https://github.com/Doni4c/HuashijieLibre` (confirming `GPL-3.0` public repository).
2. Create one translation component in Weblate UI:
   1. Repository URL: `https://github.com/Doni4c/HuashijieLibre.git`, branch `main`, push via GitHub Pull Request.
   2. File format: `Nested JSON` (`json-nested`), File mask: `lang/*.json`, Monolingual base language file: `lang/_template_zh.json`, Source language: `zh_Hans` (`zh-CN`).
   3. Add component filter / read-only enforcement excluding `__metadata__`, `0x7f110803`, `0x7f110077`, `sdcard_root_folder`, and `album_folder` (or point the Weblate component at the flat bridge branch generated via `export-flat`).
3. Enable Weblate quality checks (`printf` format strings, unchanged translations, XML/HTML tags) and add the `weblate` GitHub app to open PRs automatically.
4. Update line 18 of `TRANSLATING.md` with the live Weblate project URL so all existing and future APK "Contribute translation" links route users straight to the portal.
