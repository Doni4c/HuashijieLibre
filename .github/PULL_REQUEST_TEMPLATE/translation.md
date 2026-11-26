## Translation Contribution Summary

1. Target file and BCP-47 code (example: `lang/ja.json`, `ja`):
2. Scope covered in this PR (Priority 1 core studio, Priority 2 secondary tools, terminology polish, or full pack):
3. Optional device tested (Android version / screen size) or Web editor submission:

## Contributor Self-Check

1. [ ] Every key matches `lang/_template_zh.json` across `strings_by_id`, `strings_by_name`, `strings_by_text`, and `strings_by_value`, with `__metadata__` updated (`locale`, `name`).
2. [ ] Format placeholders (`%s`, `%d`, `%1$s`, `%1$d`, `%%`), `\n` line breaks, and HTML tags match the Chinese source.
3. [ ] Protected folder keys (`0x7f110803` `HuashijiePro` and `0x7f110077` `画世界Pro`) were not modified.
4. [ ] Product naming keeps `HuashijieLibre` (no space), host app `画世界Pro`, and Android directory `HuashijiePro`.
5. [ ] File uses UTF-8 without BOM, Unix `LF` line endings, and zero em-dashes, en-dashes, or bullet characters.
6. [ ] Checked terminology against `GLOSSARY.md` and kept short toolbar/button labels compact.
7. [ ] Ran `python3 i18n/validate_dictionaries.py` locally (or verified the GitHub Actions summary after opening this PR).
