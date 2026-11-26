# Translation Platform Evaluation and Setup Guide (Hosted Weblate vs Crowdin Open Source)

This document records the platform evaluation against the `lang/*.json` dictionary schema used by **HuashijieLibre** (`GPL-3.0`, targeting **画世界Pro** v3.4.6) and defines the automated two-way GitHub Actions bridge connecting `lang/*.json` and `i18n/weblate/*.json` for **Hosted Weblate**.

## 1. Real Schema Constraints in HuashijieLibre

Inspecting `lang/_template_zh.json`, `lang/vi.json`, and `lang/en.json` reveals four constraints for automated web translation platforms:

1. **Mixed `__metadata__` and string tables in one file:** Every dictionary places `__metadata__` (`locale`, `name`, `app`, `version`, `total_strings`) at the top level alongside four string objects. Standard nested JSON parsers expose fields inside `__metadata__` (`app`, `version`, `locale`) as translatable keys unless explicitly stripped, risking corruption of `net.huanci.hsjpro` or `zh-CN`.
2. **2.64x cross-table string duplication:** Across the four tables (`10,118` leaf entries total), there are only **3,836** deduplicated flat keys (**3,470** unique Chinese source strings). Translating raw `lang/*.json` forces translators to repeat identical labels (`画笔`, `图层`) multiple times and causes cross-table drift.
3. **Opaque hex IDs vs runtime-skipped names:** `TranslationEngine.java` reads `strings_by_id` (`0x7f110000`), `strings_by_text`, and `strings_by_value`, while skipping `strings_by_name` at runtime. On raw JSON platforms, `strings_by_id` shows hex IDs without semantic resource names.
4. **Strict typography and formatting rules:** Downstream validation enforces UTF-8 without BOM, Unix `LF`, `0` em-dashes (`U+2014`), `0` en-dashes (`U+2013`), `0` bullets (`U+2022`), protected Android paths (`HuashijiePro`, `画世界Pro`), and exact placeholder/newline/HTML-tag parity.

## 2. Hosted Weblate (Libre Plan) vs Crowdin Open Source

| Evaluation Dimension | Hosted Weblate (Libre Plan) | Crowdin Open Source |
| :--- | :--- | :--- |
| License alignment (`GPL-3.0`) | Built on `GPL-3.0`; Libre hosting targets public OSI/FSF repositories with zero vendor lock-in. | Free Open Source tier requires manual approval on proprietary SaaS. |
| Flat vs nested JSON bridge | Works cleanly with flat monolingual JSON (`i18n/weblate/*.json`) using `i18n/weblate/_template_zh.json` as the base file. | Supports nested JSON, but `crowdin.yml` lacks native exclusion for `__metadata__` without Marketplace apps or scripts. |
| Duplicate string workload | Reading `i18n/weblate/*.json` eliminates `__metadata__`, protected path keys, and cross-table duplicates (`3,836` keys instead of `10,118`). | Requires custom pre/post-processing scripts anyway to deduplicate 4 tables. |
| Built-in QA checks | Built-in `printf` format checks, HTML/XML tag checks, unchanged translation warnings, and GitHub App PR automation. | Built-in placeholder QA and GitHub integration, weaker custom character-ban checks. |
| Repo configuration | Configured directly in Weblate UI (do not commit speculative `.weblate` config files; `.weblate` is only a `wlc` CLI client pointer). | Requires `crowdin.yml` plus custom sync scripts. |

**Decision:** Use **Hosted Weblate (Libre Plan)** pointed at the flat bridge directory **`i18n/weblate/`**, synchronized automatically with `lang/*.json` by GitHub Actions.

## 3. Automated Two-Way Sync Architecture (Zero AI, Loop-Safe)

1. **Flat Bridge Directory (`i18n/weblate/`):** Contains `_template_zh.json` (source base file), `en.json`, `vi.json`, and any future `<locale>.json` files exported via `i18n/lang_helper.py` (`res.<name>`, `libre.<key>`, `txt.<hash>`). Protected Android folder keys (`0x7f110803`, `0x7f110077`, `0x7f110804`, `sdcard_root_folder`, `album_folder`, `sdcard_root_old_folder`) and `__metadata__` are excluded from Weblate and restored automatically during compilation.
2. **Direction 1 (`lang/*.json` -> `i18n/weblate/*.json`):** `.github/workflows/sync-weblate-export.yml` runs on `push` to `main` touching `lang/*.json` or `i18n/lang_helper.py`, executes `python3 i18n/lang_helper.py export-flat --all --flat-dir i18n/weblate`, and pushes to `main` with `[skip weblate-sync] [skip ci]` only if `i18n/weblate/` changed.
3. **Direction 2 (`i18n/weblate/*.json` -> `lang/*.json`):** `.github/workflows/sync-weblate-import.yml` handles two safe stages to respect `GITHUB_TOKEN` permissions:
   1. **On Pull Request (`pull_request` touching `i18n/weblate/*.json`):** Runs with `contents: read` (works on both Hosted Weblate PRs and external fork PRs without 403 errors or branch push conflicts against Weblate), compiles `i18n/weblate/*.json` in the runner workspace (`import-flat --all`), runs `i18n/validate_dictionaries.py --summary-file "$GITHUB_STEP_SUMMARY"`, and runs `gradle :patches:validateDictionaries --no-daemon`.
   2. **After Merge to `main` (`push` to `main` touching `i18n/weblate/*.json`):** Runs `import-flat --all` (automatically initializing new BCP-47 `lang/<code >.json` files with valid `__metadata__` and 4 tables when Weblate adds a new language), validates with Python and Gradle, and commits `lang/` to `main` with `[skip weblate-sync] [skip ci]`.
4. **Validation Separation:** `.github/workflows/validate-translations.yml` excludes `!i18n/weblate/**` so Weblate PRs never run duplicate or uncompiled checks.
5. **Local Bridge Commands (Optional Manual Testing):**

```powershell
python i18n/lang_helper.py export-flat --all --flat-dir i18n/weblate
python i18n/lang_helper.py import-flat --all --flat-dir i18n/weblate
python i18n/validate_dictionaries.py
```

## 4. Copy-Paste Setup Table for Hosted Weblate (`weblate.org`)

### 4.1 Libre Plan Hosting Application (`GPL-3.0`)

| Weblate Form Field | Value to Copy-Paste | Notes |
| :--- | :--- | :--- |
| Project name | `HuashijieLibre` | Write without spaces. |
| Source code license | `GPL-3.0` (`GPL-3.0-only` / `GPL-3.0-or-later`) | Matches repository `LICENSE`. |
| Website / Project URL | `https://github.com/Doni4c/HuashijieLibre` | Public GitHub repository. |
| Translation instructions URL | `https://github.com/Doni4c/HuashijieLibre/blob/main/TRANSLATING.md` | Links contributors to glossary and rules. |

### 4.2 Component Creation Settings (`weblate.org` UI)

| Weblate Component Field | Value to Copy-Paste | Why This Exact Setting |
| :--- | :--- | :--- |
| Component name | `HuashijieLibre UI` | Displayed inside the Weblate project dashboard. |
| Version control system | `Git` (`GitHub` integration) | Connect via the official **Hosted Weblate GitHub App** instead of manual PAT/SSH keys. |
| Source code repository | `https://github.com/Doni4c/HuashijieLibre.git` | Official repository URL. |
| Repository branch | `main` | Primary release and CI branch. |
| Version control push / PR mode | `GitHub Pull Request` (via Hosted Weblate App) | Opens PRs instead of direct pushes so CI validates every change first. |
| File format | `JSON` (`json`) | Use flat `JSON`, **not** `JSON nested structure`, so `res.*`, `libre.*`, and `txt.*` keys stay single-level. |
| File mask | `i18n/weblate/*.json` | Matches all exported language packs in the flat bridge folder. |
| Monolingual base language file | `i18n/weblate/_template_zh.json` | Canonical Simplified Chinese source strings (`3,836` keys). |
| Source language | `Chinese (Simplified)` (`zh_Hans`) | Matches base file language. |
| Edit base file | `Off` (unchecked) | Source strings are generated only from the host APK `画世界Pro`. |
| Template for new translations | *(Leave empty)* | New languages start empty instead of copying Chinese source strings as fake translations. |
| Language filter | `^(?!_template_zh$).+$` | Excludes `_template_zh.json` from appearing as a target translation language. |
| Language code style | `BCP style` (hyphen separator) | Writes `pt-BR.json` and `zh-Hant.json`; `lang_helper.py` also normalizes underscores as fallback. |
| Quality checks / Format flags | Enable `printf` (`c_format`), `XML markup`, `Unchanged translation` | Enforce `printf` and XML/HTML checks; keep `Unchanged translation` as non-blocking warning (`RGB`, `画世界Pro`). |
| Adding new translation | `Open` / `Create new language` enabled | Allows contributors to start new languages directly; `import-flat` builds the 4-table file after merge. |

### 4.3 GitHub Repository Settings (Owner Checklist)

1. **Workflow write access (`Settings > Actions > General > Workflow permissions`):** Select **Read and write permissions** (required if repository policy overrides workflow-level `permissions: contents: write` so `GITHUB_TOKEN` can push compiled files to `main`).
2. **Optional instant Weblate refresh secret (`Settings > Secrets and variables > Actions`):** `GITHUB_TOKEN` pushes intentionally suppress GitHub push webhooks, so Hosted Weblate syncs Direction 1 updates on its scheduled repository pull (or manual Update). If you want Weblate to pull immediately after Direction 1 runs, add one fine-grained PAT secret named `WEBLATE_SYNC_PAT` (`Contents: Read and write` on `Doni4c/HuashijieLibre`). Workflows automatically fall back to `github.token` when `WEBLATE_SYNC_PAT` is absent.
3. **Final onboarding step:** After Hosted Weblate approves the project, update line 18 of `TRANSLATING.md` with the live `https://hosted.weblate.org/projects/huashijielibre/` URL.
