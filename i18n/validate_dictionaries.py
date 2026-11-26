#!/usr/bin/env python3
"""
Shared translation dictionary validator for HuashijieLibre.

Used by both Gradle (:patches:validateDictionaries) and GitHub Actions
(.github/workflows/validate-translations.yml) as the single source of truth.
Uses only Python standard library modules.
"""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass, field
import json
from pathlib import Path
import re
import sys
from typing import Any


REQUIRED_SECTIONS = (
    "strings_by_id",
    "strings_by_name",
    "strings_by_text",
    "strings_by_value",
)
REQUIRED_METADATA_KEYS = ("locale", "name", "app", "version", "total_strings")
EXPECTED_APP_ID = "net.huanci.hsjpro"
PROTECTED_ID_KEYS = {"0x7f110803", "0x7f110077", "0x7f110804"}
PROTECTED_NAME_KEYS = {"sdcard_root_folder", "album_folder", "sdcard_root_old_folder"}

FORBIDDEN_CHARS = {
    chr(0x2014): "em-dash (U+2014)",
    chr(0x2013): "en-dash (U+2013)",
    chr(0x2022): "bullet (U+2022)",
}

BCP47_RE = re.compile(r"^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$")
CJK_RE = re.compile(r"[\u4e00-\u9fff]")
FMT_RE = re.compile(
    r"%%|%(?:[1-9]\d*\$)?[-+#0,(]*(?:\d+)?(?:\.\d+)?[bcdeEfFgGhHosuxX]"
)
HTML_TAG_RE = re.compile(r"</?[a-zA-Z][^>]*>")
SPACE_LIBRE_RE = re.compile(r"huashijie\s+libre", re.IGNORECASE)
SPACE_ZH_PRO_RE = re.compile(r"\u753b\u4e16\u754c\s+pro", re.IGNORECASE)
PRO_BRAND_ZH = "\u753b\u4e16\u754cPro"
WRONG_PRO_BRAND_RE = re.compile(r"huashijie\s*pro", re.IGNORECASE)


@dataclass
class HardError:
    file_name: str
    section: str
    key: str
    message: str
    how_to_fix: str


@dataclass
class SoftWarning:
    file_name: str
    category: str
    section: str
    key: str
    detail: str


@dataclass
class FileReport:
    file_name: str
    locale: str = "?"
    display_name: str = "?"
    encoding_ok: bool = True
    syntax_ok: bool = True
    parity_ok: bool = True
    placeholders_ok: bool = True
    total_entries: int = 0
    changed_entries: int = 0
    unique_cjk_total: int = 0
    unique_cjk_translated: int = 0
    errors: list[HardError] = field(default_factory=list)
    warnings: list[SoftWarning] = field(default_factory=list)

    @property
    def passed(self) -> bool:
        return len(self.errors) == 0


def configure_stdio() -> None:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            try:
                stream.reconfigure(encoding="utf-8", errors="replace")
            except OSError:
                pass


def make_dup_checker(file_name: str, errors: list[HardError]):
    def hook(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for k, v in pairs:
            if k in result:
                errors.append(
                    HardError(
                        file_name=file_name,
                        section="(JSON object)",
                        key=k,
                        message=f"Duplicate JSON key '{k}' found inside the same object.",
                        how_to_fix="Remove or merge the duplicate key so each key appears only once.",
                    )
                )
            result[k] = v
        return result

    return hook


def scan_forbidden_in_parsed(
    value: Any,
    file_name: str,
    section: str,
    key_path: str,
    errors: list[HardError],
) -> None:
    if isinstance(value, str):
        for ch, label in FORBIDDEN_CHARS.items():
            if ch in value:
                errors.append(
                    HardError(
                        file_name=file_name,
                        section=section,
                        key=key_path,
                        message=f"Prohibited character {label} found in decoded JSON value.",
                        how_to_fix="Replace em-dash/en-dash with '-' or ',' and remove bullet symbols.",
                    )
                )
    elif isinstance(value, dict):
        for k, v in value.items():
            k_str = str(k)
            for ch, label in FORBIDDEN_CHARS.items():
                if ch in k_str:
                    errors.append(
                        HardError(
                            file_name=file_name,
                            section=section,
                            key=k_str,
                            message=f"Prohibited character {label} found in JSON key.",
                            how_to_fix="Remove prohibited typography characters from the key.",
                        )
                    )
            next_section = k_str if section == "$" else section
            next_key = k_str if section != "$" else "(root)"
            scan_forbidden_in_parsed(v, file_name, next_section, next_key, errors)
    elif isinstance(value, list):
        for idx, item in enumerate(value):
            scan_forbidden_in_parsed(item, file_name, section, f"{key_path}[{idx}]", errors)


LITERAL_PERCENT_RE = re.compile(r"(?<=\d)%(?![A-Za-z0-9$])")


def extract_placeholder_signature(text: str) -> tuple[Counter[str], list[str], int, int, Counter[str]]:
    tokens = FMT_RE.findall(text)
    positional_or_escaped = Counter(tokens)
    non_positional_seq = [
        tok for tok in tokens if tok != "%%" and "$" not in tok
    ]
    remainder = FMT_RE.sub("", text)
    # In plain non-format UI prose (e.g. "100%"), digits followed by '%' without
    # specifier letters are literal percentages, unlike broken "%1" or "%".
    if not tokens:
        remainder = LITERAL_PERCENT_RE.sub("", remainder)
    stray_percent = remainder.count("%")
    newline_count = text.count("\n")
    html_tags = Counter(HTML_TAG_RE.findall(text))
    return positional_or_escaped, non_positional_seq, stray_percent, newline_count, html_tags


def load_and_check_raw(path: Path, report: FileReport) -> dict[str, Any] | None:
    file_name = path.name
    try:
        raw = path.read_bytes()
    except OSError as exc:
        report.encoding_ok = False
        report.syntax_ok = False
        report.errors.append(
            HardError(
                file_name=file_name,
                section="(file)",
                key="(file)",
                message=f"Cannot read file: {exc}",
                how_to_fix="Ensure the file exists inside lang/ and is readable.",
            )
        )
        return None

    if raw.startswith(b"\xef\xbb\xbf"):
        report.encoding_ok = False
        report.errors.append(
            HardError(
                file_name=file_name,
                section="(encoding)",
                key="(file)",
                message="File starts with a UTF-8 BOM (byte order mark).",
                how_to_fix="Save the file as UTF-8 without BOM (or run python i18n/lang_helper.py sync).",
            )
        )

    if b"\r" in raw:
        report.encoding_ok = False
        report.errors.append(
            HardError(
                file_name=file_name,
                section="(line endings)",
                key="(file)",
                message="File contains CRLF or CR line endings instead of Unix LF.",
                how_to_fix="Convert line endings to LF (or run python i18n/lang_helper.py sync).",
            )
        )

    try:
        raw_text = raw.decode("utf-8")
    except UnicodeDecodeError as exc:
        report.encoding_ok = False
        report.syntax_ok = False
        report.errors.append(
            HardError(
                file_name=file_name,
                section="(encoding)",
                key="(file)",
                message=f"File is not valid UTF-8: {exc}",
                how_to_fix="Re-save the file using UTF-8 encoding without BOM.",
            )
        )
        return None

    for line_no, line in enumerate(raw_text.splitlines(), 1):
        for ch, label in FORBIDDEN_CHARS.items():
            if ch in line:
                report.errors.append(
                    HardError(
                        file_name=file_name,
                        section="(raw text)",
                        key=f"line {line_no}",
                        message=f"Prohibited character {label} found at line {line_no}.",
                        how_to_fix="Replace em-dash/en-dash with '-' or ',' and remove bullet symbols.",
                    )
                )

    dup_errors: list[HardError] = []
    try:
        parsed = json.loads(raw_text, object_pairs_hook=make_dup_checker(file_name, dup_errors))
    except json.JSONDecodeError as exc:
        report.syntax_ok = False
        report.errors.append(
            HardError(
                file_name=file_name,
                section="(JSON syntax)",
                key=f"line {exc.lineno}, col {exc.colno}",
                message=f"Invalid JSON syntax: {exc.msg}",
                how_to_fix="Fix missing quotes, brackets, or trailing commas near the reported line.",
            )
        )
        return None

    report.errors.extend(dup_errors)

    if not isinstance(parsed, dict):
        report.syntax_ok = False
        report.errors.append(
            HardError(
                file_name=file_name,
                section="(root)",
                key="(root)",
                message="Dictionary root must be a JSON object {...}.",
                how_to_fix="Keep the top-level {...} object structure from lang/_template_zh.json.",
            )
        )
        return None

    # Deduplicate parsed prohibited errors if already caught on raw lines only when key differs
    parsed_forb_errors: list[HardError] = []
    scan_forbidden_in_parsed(parsed, file_name, "$", "$", parsed_forb_errors)
    existing_msgs = {(e.file_name, e.message) for e in report.errors}
    for err in parsed_forb_errors:
        # Always include parsed errors when they come from \u2014 escapes (not in raw text lines)
        if "\\u2014" in raw_text or "\\u2013" in raw_text or "\\u2022" in raw_text:
            report.errors.append(err)
        elif not any(e.section == "(raw text)" and e.file_name == file_name for e in report.errors):
            if (err.file_name, err.message) not in existing_msgs:
                report.errors.append(err)

    return parsed


def validate_metadata(
    data: dict[str, Any],
    template_meta: dict[str, Any] | None,
    report: FileReport,
    is_template: bool,
) -> None:
    meta = data.get("__metadata__")
    if not isinstance(meta, dict):
        report.parity_ok = False
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="__metadata__",
                message="Missing or invalid '__metadata__' JSON object.",
                how_to_fix="Restore the '__metadata__' header block from lang/_template_zh.json.",
            )
        )
        return

    for req_key in REQUIRED_METADATA_KEYS:
        if req_key not in meta:
            report.parity_ok = False
            report.errors.append(
                HardError(
                    file_name=report.file_name,
                    section="__metadata__",
                    key=req_key,
                    message=f"Missing required metadata key '__metadata__.{req_key}'.",
                    how_to_fix=f"Add '{req_key}' inside '__metadata__' matching _template_zh.json.",
                )
            )

    locale = meta.get("locale")
    if isinstance(locale, str) and locale.strip():
        report.locale = locale.strip()
        if not BCP47_RE.match(report.locale):
            report.errors.append(
                HardError(
                    file_name=report.file_name,
                    section="__metadata__",
                    key="locale",
                    message=f"Invalid BCP-47 locale '{report.locale}'.",
                    how_to_fix="Use a standard tag such as 'en-US', 'vi-VN', 'ja', or 'pt-BR'.",
                )
            )
    else:
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="locale",
                message="'__metadata__.locale' must be a non-empty string.",
                how_to_fix="Set '__metadata__.locale' to your language code (for example 'es-ES').",
            )
        )

    disp_name = meta.get("name")
    if isinstance(disp_name, str) and disp_name.strip():
        report.display_name = disp_name.strip()
    else:
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="name",
                message="'__metadata__.name' must be a non-empty string.",
                how_to_fix="Set '__metadata__.name' to the native name of the language.",
            )
        )

    app_id = meta.get("app")
    if app_id != EXPECTED_APP_ID:
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="app",
                message=f"'__metadata__.app' is {app_id!r}, expected {EXPECTED_APP_ID!r}.",
                how_to_fix=f"Keep '__metadata__.app' set to '{EXPECTED_APP_ID}'.",
            )
        )

    version = meta.get("version")
    expected_version = template_meta.get("version") if template_meta else None
    if not isinstance(version, str) or not version.strip():
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="version",
                message="'__metadata__.version' must be a non-empty string.",
                how_to_fix="Keep '__metadata__.version' aligned with _template_zh.json.",
            )
        )
    elif expected_version and version != expected_version:
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="version",
                message=f"'__metadata__.version' is {version!r}, expected {expected_version!r}.",
                how_to_fix=f"Run python i18n/lang_helper.py sync or update version to '{expected_version}'.",
            )
        )

    total_strings = meta.get("total_strings")
    if not isinstance(total_strings, int) or isinstance(total_strings, bool) or total_strings <= 0:
        report.errors.append(
            HardError(
                file_name=report.file_name,
                section="__metadata__",
                key="total_strings",
                message=f"'__metadata__.total_strings' must be a positive integer (got {total_strings!r}).",
                how_to_fix="Restore numeric 'total_strings' from _template_zh.json.",
            )
        )

    if not is_template and not report.file_name.startswith("zh"):
        if isinstance(locale, str) and locale.strip().lower() == "zh-cn":
            report.errors.append(
                HardError(
                    file_name=report.file_name,
                    section="__metadata__",
                    key="locale",
                    message="'__metadata__.locale' was left as 'zh-CN'.",
                    how_to_fix="Update '__metadata__.locale' to your target BCP-47 locale code.",
                )
            )
        if isinstance(disp_name, str) and disp_name.strip() == "\u4e2d\u6587 (\u7b80\u4f53)":
            report.errors.append(
                HardError(
                    file_name=report.file_name,
                    section="__metadata__",
                    key="name",
                    message="'__metadata__.name' was left as Simplified Chinese.",
                    how_to_fix="Update '__metadata__.name' to your target language name.",
                )
            )


def validate_against_template(
    target_data: dict[str, Any],
    template_data: dict[str, Any],
    report: FileReport,
    is_template: bool,
) -> None:
    # 1. Required sections
    for section in REQUIRED_SECTIONS:
        if not isinstance(target_data.get(section), dict):
            report.parity_ok = False
            report.errors.append(
                HardError(
                    file_name=report.file_name,
                    section=section,
                    key=section,
                    message=f"Missing or non-object section '{section}'.",
                    how_to_fix="Do not delete top-level dictionary sections; restore from _template_zh.json.",
                )
            )
    if not report.parity_ok:
        return

    # 2. Key parity + value types + placeholders
    for section in REQUIRED_SECTIONS:
        ref_sec: dict[str, Any] = template_data[section]
        tgt_sec: dict[str, Any] = target_data[section]

        ref_keys = set(ref_sec.keys())
        tgt_keys = set(tgt_sec.keys())
        missing = [k for k in ref_sec.keys() if k not in tgt_keys]
        extra = [k for k in tgt_sec.keys() if k not in ref_keys]

        if missing or extra:
            report.parity_ok = False
            if missing:
                sample = ", ".join(repr(k) for k in missing[:10])
                more = f" (+{len(missing) - 10} more)" if len(missing) > 10 else ""
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=missing[0],
                        message=f"Missing {len(missing)} key(s) compared to _template_zh.json: {sample}{more}",
                        how_to_fix=f"Run: python i18n/lang_helper.py sync lang/{report.file_name}",
                    )
                )
            if extra:
                sample = ", ".join(repr(k) for k in extra[:10])
                more = f" (+{len(extra) - 10} more)" if len(extra) > 10 else ""
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=extra[0],
                        message=f"Found {len(extra)} unexpected extra key(s) not in _template_zh.json: {sample}{more}",
                        how_to_fix=f"Remove unknown keys or run: python i18n/lang_helper.py sync lang/{report.file_name}",
                    )
                )

        for key, ref_val in ref_sec.items():
            if key not in tgt_sec:
                continue
            tgt_val = tgt_sec[key]
            if not isinstance(tgt_val, str):
                report.parity_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message=f"Value must be a string, got {type(tgt_val).__name__}.",
                        how_to_fix="Wrap the translation value in double quotes as a JSON string.",
                    )
                )
                continue
            if isinstance(ref_val, str) and ref_val != "" and tgt_val == "":
                report.parity_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message="Translation value is empty ('').",
                        how_to_fix="Provide a translation or keep the original Chinese string if leaving for later.",
                    )
                )
                continue

            if not isinstance(ref_val, str) or is_template:
                continue

            ref_sig = extract_placeholder_signature(ref_val)
            tgt_sig = extract_placeholder_signature(tgt_val)
            # (counter, non_pos_seq, stray_pct, nl_count, html_tags)
            if ref_sig[0] != tgt_sig[0]:
                report.placeholders_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message=(
                            f"Format placeholder mismatch: source has {dict(ref_sig[0])}, "
                            f"translation has {dict(tgt_sig[0])}."
                        ),
                        how_to_fix="Keep exact placeholders (%s, %d, %1$s, %1$d, %%) from the Chinese string.",
                    )
                )
            elif ref_sig[1] != tgt_sig[1]:
                report.placeholders_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message=(
                            f"Non-positional placeholder order changed: source={ref_sig[1]}, "
                            f"translation={tgt_sig[1]}."
                        ),
                        how_to_fix="Keep non-positional placeholders (%s, %d) in their original order.",
                    )
                )
            elif ref_sig[2] != tgt_sig[2]:
                report.placeholders_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message=(
                            f"Malformed or dangling '%' token detected (source stray={ref_sig[2]}, "
                            f"translation stray={tgt_sig[2]})."
                        ),
                        how_to_fix="Fix incomplete placeholders (for example '%1' -> '%1$d') or escape literal '%' as '%%'.",
                    )
                )

            if ref_sig[3] != tgt_sig[3]:
                report.placeholders_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message=(
                            f"Linebreak count mismatch: source has {ref_sig[3]} '\\n', "
                            f"translation has {tgt_sig[3]}."
                        ),
                        how_to_fix="Preserve the same number of '\\n' line breaks as the Chinese source.",
                    )
                )

            if ref_sig[4] != tgt_sig[4]:
                report.placeholders_ok = False
                report.errors.append(
                    HardError(
                        file_name=report.file_name,
                        section=section,
                        key=key,
                        message=(
                            f"HTML formatting tag mismatch: source has {dict(ref_sig[4])}, "
                            f"translation has {dict(tgt_sig[4])}."
                        ),
                        how_to_fix="Keep all HTML tags (such as <font...>, </font>, <a>) intact.",
                    )
                )

    if is_template:
        report.total_entries = sum(len(template_data[s]) for s in REQUIRED_SECTIONS)
        report.changed_entries = report.total_entries
        return

    # 3. Progress stats & Soft Warnings
    cjk_sources_all: set[str] = set()
    cjk_sources_translated: set[str] = set()
    by_source: dict[str, list[tuple[str, str, str]]] = defaultdict(list)

    for section in REQUIRED_SECTIONS:
        ref_sec = template_data[section]
        tgt_sec = target_data[section]
        for key, ref_val in ref_sec.items():
            if not isinstance(ref_val, str):
                continue
            tgt_val = tgt_sec.get(key)
            if not isinstance(tgt_val, str):
                continue
            report.total_entries += 1
            if tgt_val != ref_val:
                report.changed_entries += 1
            if CJK_RE.search(ref_val):
                cjk_sources_all.add(ref_val)
                if tgt_val != ref_val:
                    cjk_sources_translated.add(ref_val)

            is_protected_key = (
                (section == "strings_by_id" and key in PROTECTED_ID_KEYS)
                or (section == "strings_by_name" and key in PROTECTED_NAME_KEYS)
            )
            if not is_protected_key:
                by_source[ref_val].append((section, key, tgt_val))

            # Soft warning A: Short UI label / button expansion risk
            if (
                not key.startswith("libre_")
                and 1 <= len(ref_val) <= 4
                and CJK_RE.search(ref_val)
                and "\n" not in ref_val
                and len(tgt_val) > max(18, len(ref_val) * 6)
            ):
                report.warnings.append(
                    SoftWarning(
                        file_name=report.file_name,
                        category="length_overflow",
                        section=section,
                        key=key,
                        detail=f"Source ({len(ref_val)} chars) {ref_val!r} -> ({len(tgt_val)} chars) {tgt_val!r}",
                    )
                )

            # Soft warning B: Brand and protected naming checks
            if SPACE_LIBRE_RE.search(tgt_val):
                report.warnings.append(
                    SoftWarning(
                        file_name=report.file_name,
                        category="brand_naming",
                        section=section,
                        key=key,
                        detail=f"Found spaced Huashijie/Libre name in {tgt_val!r}; write 'HuashijieLibre' without spaces.",
                    )
                )
            if SPACE_ZH_PRO_RE.search(tgt_val):
                report.warnings.append(
                    SoftWarning(
                        file_name=report.file_name,
                        category="brand_naming",
                        section=section,
                        key=key,
                        detail=f"Found space before 'Pro' in {tgt_val!r}; write '{PRO_BRAND_ZH}' without spaces.",
                    )
                )
            if is_protected_key and tgt_val != ref_val:
                report.warnings.append(
                    SoftWarning(
                        file_name=report.file_name,
                        category="protected_key",
                        section=section,
                        key=key,
                        detail=f"Protected folder key changed from {ref_val!r} to {tgt_val!r}.",
                    )
                )
            elif (
                not is_protected_key
                and PRO_BRAND_ZH in ref_val
                and PRO_BRAND_ZH not in tgt_val
                and "/HuashijiePro/" not in tgt_val
                and WRONG_PRO_BRAND_RE.search(tgt_val)
            ):
                report.warnings.append(
                    SoftWarning(
                        file_name=report.file_name,
                        category="brand_naming",
                        section=section,
                        key=key,
                        detail=f"Host app name '{PRO_BRAND_ZH}' was rewritten in {tgt_val!r}; keep '{PRO_BRAND_ZH}'.",
                    )
                )

    report.unique_cjk_total = len(cjk_sources_all)
    report.unique_cjk_translated = len(cjk_sources_translated)

    # Soft warning C1: Paired strings_by_id vs strings_by_name mismatch
    ref_id_items = list(template_data["strings_by_id"].items())
    ref_name_items = list(template_data["strings_by_name"].items())
    for (id_k, id_zh), (name_k, name_zh) in zip(ref_id_items, ref_name_items):
        if id_zh == name_zh:
            v_id = target_data["strings_by_id"].get(id_k)
            v_name = target_data["strings_by_name"].get(name_k)
            if isinstance(v_id, str) and isinstance(v_name, str) and v_id != v_name:
                report.warnings.append(
                    SoftWarning(
                        file_name=report.file_name,
                        category="id_name_mismatch",
                        section="strings_by_id / strings_by_name",
                        key=f"{id_k} / {name_k}",
                        detail=f"strings_by_id={v_id!r} vs strings_by_name={v_name!r} for {id_zh!r}",
                    )
                )

    # Soft warning C2: Cross-table discrepancy for identical Chinese source string
    for src_text, entries in by_source.items():
        distinct_vals = sorted({val for _, _, val in entries})
        if len(distinct_vals) > 1:
            sample_locs = ", ".join(f"{sec}:{k}={val!r}" for sec, k, val in entries[:4])
            report.warnings.append(
                SoftWarning(
                    file_name=report.file_name,
                    category="cross_table_drift",
                    section="multi-table",
                    key=src_text,
                    detail=f"{len(distinct_vals)} variants ({sample_locs})",
                )
            )


def format_pct(part: int, total: int) -> str:
    if total <= 0:
        return "0.0%"
    return f"{(part * 100.0) / total:.1f}%"


def build_markdown_summary(reports: list[FileReport]) -> str:
    all_passed = all(r.passed for r in reports)
    total_errors = sum(len(r.errors) for r in reports)
    total_warnings = sum(len(r.warnings) for r in reports)

    lines: list[str] = []
    lines.append("# HuashijieLibre Translation Validation Summary")
    lines.append("")
    if all_passed:
        lines.append(
            f"**Result: PASSED** (0 hard errors across {len(reports)} file(s); {total_warnings} soft warning(s))."
        )
    else:
        lines.append(
            f"**Result: FAILED** ({total_errors} hard error(s) found across {len(reports)} file(s); {total_warnings} soft warning(s))."
        )
    lines.append("")
    lines.append("## 1. File Overview")
    lines.append("")
    lines.append(
        "| File | Language | Encoding & JSON | Key Parity | Placeholders | Entry Progress | CJK String Progress | Hard Errors | Soft Warnings |"
    )
    lines.append(
        "| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |"
    )

    for r in reports:
        enc = "PASS" if (r.encoding_ok and r.syntax_ok) else "FAIL"
        par = "PASS" if r.parity_ok else "FAIL"
        ph = "PASS" if r.placeholders_ok else "FAIL"
        if r.file_name == "_template_zh.json":
            prog_entry = f"Source ({r.total_entries})"
            prog_cjk = "Canonical"
        else:
            prog_entry = f"{r.changed_entries}/{r.total_entries} ({format_pct(r.changed_entries, r.total_entries)})"
            prog_cjk = f"{r.unique_cjk_translated}/{r.unique_cjk_total} ({format_pct(r.unique_cjk_translated, r.unique_cjk_total)})"
        lines.append(
            f"| `{r.file_name}` | {r.display_name} (`{r.locale}`) | {enc} | {par} | {ph} | {prog_entry} | {prog_cjk} | {len(r.errors)} | {len(r.warnings)} |"
        )

    lines.append("")
    if total_errors > 0:
        lines.append("## 2. Hard Errors (Must Fix Before Merge)")
        lines.append("")
        lines.append("| File | Section | Key / Location | Issue | How to Fix |")
        lines.append("| :--- | :--- | :--- | :--- | :--- |")
        shown = 0
        for r in reports:
            for err in r.errors:
                if shown >= 50:
                    break
                msg = err.message.replace("|", "\\|").replace("\n", " ")
                fix = err.how_to_fix.replace("|", "\\|").replace("\n", " ")
                key_disp = err.key.replace("|", "\\|").replace("\n", "\\n")
                lines.append(
                    f"| `{err.file_name}` | `{err.section}` | `{key_disp}` | {msg} | {fix} |"
                )
                shown += 1
        if total_errors > shown:
            lines.append("")
            lines.append(f"Showing first {shown} of {total_errors} hard error(s).")
        lines.append("")
    else:
        lines.append("## 2. Hard Errors")
        lines.append("")
        lines.append("0 hard errors detected. Safe for maintainer review and merge.")
        lines.append("")

    lines.append("## 3. Soft Warnings Digest (Non-Blocking Review Hints)")
    lines.append("")
    if total_warnings == 0:
        lines.append("0 soft warnings.")
    else:
        cat_labels = {
            "brand_naming": "Brand or project spacing (`HuashijieLibre`, `画世界Pro`)",
            "protected_key": "Protected filesystem folder keys modified",
            "id_name_mismatch": "Mismatch between `strings_by_id` and `strings_by_name`",
            "cross_table_drift": "Inconsistent translations for same Chinese source across tables",
            "length_overflow": "Short UI label translation length risk (>5x or >20 chars)",
        }
        lines.append("| File | Warning Category | Count | Sample Details |")
        lines.append("| :--- | :--- | :---: | :--- |")
        for r in reports:
            grouped: dict[str, list[SoftWarning]] = defaultdict(list)
            for w in r.warnings:
                grouped[w.category].append(w)
            for cat in ("brand_naming", "protected_key", "id_name_mismatch", "cross_table_drift", "length_overflow"):
                items = grouped.get(cat, [])
                if not items:
                    continue
                samples = "; ".join(
                    f"`{it.key}`: {it.detail}" for it in items[:2]
                ).replace("|", "\\|").replace("\n", "\\n")
                lines.append(
                    f"| `{r.file_name}` | {cat_labels.get(cat, cat)} | {len(items)} | {samples} |"
                )
    lines.append("")
    return "\n".join(lines) + "\n"


def print_console_report(reports: list[FileReport]) -> None:
    for r in reports:
        status = "PASS" if r.passed else "FAIL"
        if r.file_name == "_template_zh.json":
            print(f"[{status}] {r.file_name} ({r.locale}): source template ({r.total_entries} entries)")
        else:
            print(
                f"[{status}] {r.file_name} ({r.locale}): "
                f"entries={r.changed_entries}/{r.total_entries} ({format_pct(r.changed_entries, r.total_entries)}), "
                f"cjk={r.unique_cjk_translated}/{r.unique_cjk_total} ({format_pct(r.unique_cjk_translated, r.unique_cjk_total)}), "
                f"errors={len(r.errors)}, warnings={len(r.warnings)}"
            )
        for err in r.errors[:20]:
            print(f"  ERROR [{err.section}] {err.key}: {err.message} -> {err.how_to_fix}")
        if len(r.errors) > 20:
            print(f"  ... ({len(r.errors) - 20} more hard error(s))")

        by_cat: dict[str, list[SoftWarning]] = defaultdict(list)
        for w in r.warnings:
            by_cat[w.category].append(w)
        for cat in ("brand_naming", "protected_key", "id_name_mismatch", "cross_table_drift", "length_overflow"):
            items = by_cat.get(cat, [])
            if items:
                print(f"  WARN [{cat}] ({len(items)}):")
                for w in items[:3]:
                    print(f"    - [{w.section}] {w.key}: {w.detail}")
                if len(items) > 3:
                    print(f"    ... ({len(items) - 3} more)")


def validate_files(template_path: Path, target_paths: list[Path]) -> list[FileReport]:
    reports: list[FileReport] = []
    template_report = FileReport(file_name=template_path.name)
    template_data = load_and_check_raw(template_path, template_report)
    if template_data is None:
        reports.append(template_report)
        return reports

    validate_metadata(template_data, None, template_report, is_template=True)
    for sec in REQUIRED_SECTIONS:
        if not isinstance(template_data.get(sec), dict):
            template_report.parity_ok = False
            template_report.errors.append(
                HardError(
                    file_name=template_path.name,
                    section=sec,
                    key=sec,
                    message=f"Template is missing JSON object section '{sec}'.",
                    how_to_fix="Restore the missing section in lang/_template_zh.json.",
                )
            )
    if template_report.parity_ok:
        template_report.total_entries = sum(len(template_data[s]) for s in REQUIRED_SECTIONS)
        template_report.changed_entries = template_report.total_entries
    reports.append(template_report)

    template_meta = template_data.get("__metadata__")
    template_meta_dict = template_meta if isinstance(template_meta, dict) else None

    for path in target_paths:
        if path.resolve() == template_path.resolve():
            continue
        rep = FileReport(file_name=path.name)
        data = load_and_check_raw(path, rep)
        if data is not None:
            validate_metadata(data, template_meta_dict, rep, is_template=False)
            if template_report.parity_ok:
                validate_against_template(data, template_data, rep, is_template=False)
        reports.append(rep)

    return reports


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Validate HuashijieLibre JSON translation dictionaries."
    )
    parser.add_argument(
        "--lang-dir",
        type=Path,
        default=Path("lang"),
        help="Directory containing translation JSON packs (default: lang).",
    )
    parser.add_argument(
        "--template",
        type=Path,
        default=None,
        help="Canonical template file (default: <lang-dir>/_template_zh.json).",
    )
    parser.add_argument(
        "--files",
        type=Path,
        nargs="+",
        default=None,
        help="Explicit list of target JSON files to validate against template.",
    )
    parser.add_argument(
        "--stamp",
        type=Path,
        default=None,
        help="Write validation summary stamp file when all hard checks pass.",
    )
    parser.add_argument(
        "--summary-file",
        type=Path,
        default=None,
        help="Append or write Markdown summary report (e.g. $GITHUB_STEP_SUMMARY).",
    )
    parser.add_argument(
        "--strict-warnings",
        action="store_true",
        help="Treat soft warnings as failures (non-zero exit code).",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    configure_stdio()
    args = parse_args(argv if argv is not None else sys.argv[1:])

    template_path = args.template or (args.lang_dir / "_template_zh.json")
    if args.files:
        target_paths = list(args.files)
    else:
        if not args.lang_dir.is_dir():
            print(f"ERROR: Language directory does not exist: {args.lang_dir}", file=sys.stderr)
            return 1
        target_paths = sorted(p for p in args.lang_dir.glob("*.json") if p.is_file())

    reports = validate_files(template_path, target_paths)
    print_console_report(reports)

    if args.summary_file is not None:
        args.summary_file.parent.mkdir(parents=True, exist_ok=True)
        summary_md = build_markdown_summary(reports)
        args.summary_file.write_text(summary_md, encoding="utf-8", newline="\n")

    has_errors = any(not r.passed for r in reports)
    has_warnings = any(r.warnings for r in reports)

    if has_errors or (args.strict_warnings and has_warnings):
        return 1

    if args.stamp is not None:
        args.stamp.parent.mkdir(parents=True, exist_ok=True)
        stamp_lines = [f"Validated {len(reports)} dictionaries successfully."]
        for r in reports:
            stamp_lines.append(
                f"{r.file_name}: locale={r.locale}, entries={r.changed_entries}/{r.total_entries}, warnings={len(r.warnings)}"
            )
        args.stamp.write_text("\n".join(stamp_lines) + "\n", encoding="utf-8", newline="\n")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
