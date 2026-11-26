#!/usr/bin/env python3
"""
Contributor helper CLI for HuashijieLibre translation dictionaries.

Supports:
1. init: scaffold a new lang/<lang_code>.json from lang/_template_zh.json
2. sync: align existing dictionary keys and ordering with _template_zh.json,
   and optionally propagate translations across duplicate source strings
3. export-flat / import-flat: deduplicated flat JSON bridge for Hosted Weblate
   (i18n/weblate/*.json and i18n/weblate-template.json) without touching __metadata__.

Uses only Python standard library modules; writes UTF-8 without BOM and LF.
"""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
from typing import Any


SECTIONS = ("strings_by_id", "strings_by_name", "strings_by_text", "strings_by_value")
PROTECTED_ID_KEYS = {"0x7f110803", "0x7f110077", "0x7f110804"}
PROTECTED_NAME_KEYS = {"sdcard_root_folder", "album_folder", "sdcard_root_old_folder"}
BCP47_RE = re.compile(r"^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$")
DEFAULT_FLAT_DIR = Path("i18n/weblate")
DEFAULT_FLAT_TEMPLATE = Path("i18n/weblate-template.json")

COMMON_LOCALE_NAMES = {
    "ar": "العربية",
    "bn": "বাংলা",
    "cs": "Čeština",
    "de": "Deutsch",
    "el": "Ελληνικά",
    "en": "English",
    "en-GB": "English (UK)",
    "en-US": "English",
    "es": "Español",
    "es-419": "Español (Latinoamérica)",
    "es-ES": "Español",
    "fa": "فارسی",
    "fil": "Filipino",
    "fr": "Français",
    "he": "עברית",
    "hi": "हिन्दी",
    "hu": "Magyar",
    "id": "Bahasa Indonesia",
    "it": "Italiano",
    "ja": "日本語",
    "km": "ខ្មែរ",
    "ko": "한국어",
    "lo": "ລາວ",
    "ms": "Bahasa Melayu",
    "my": "မြန်မာ",
    "nl": "Nederlands",
    "pl": "Polski",
    "pt": "Português",
    "pt-BR": "Português (Brasil)",
    "ro": "Română",
    "ru": "Русский",
    "sv": "Svenska",
    "th": "ไทย",
    "tl": "Tagalog",
    "tr": "Türkçe",
    "uk": "Українська",
    "ur": "اردو",
    "vi": "Tiếng Việt",
    "vi-VN": "Tiếng Việt",
    "zh-HK": "中文 (香港)",
    "zh-Hant": "中文 (繁體)",
    "zh-TW": "中文 (繁體)",
}


def configure_stdio() -> None:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            try:
                stream.reconfigure(encoding="utf-8", errors="replace")
            except OSError:
                pass


def load_json_utf8(path: Path) -> dict[str, Any]:
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        raw = raw[3:]
    data = json.loads(raw.decode("utf-8"))
    if not isinstance(data, dict):
        raise ValueError(f"Expected JSON object at root of {path}")
    return data


def write_json_lf(path: Path, data: dict[str, Any]) -> bool:
    """Write JSON as UTF-8 without BOM and LF line endings when bytes changed."""
    path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    encoded = serialized.encode("utf-8")
    if path.is_file() and path.read_bytes() == encoded:
        return False
    path.write_bytes(encoded)
    return True


def is_protected_key(sec: str, key: str) -> bool:
    """Return True if a dictionary entry stores a protected Android folder path."""
    return (sec == "strings_by_id" and key in PROTECTED_ID_KEYS) or (
        sec == "strings_by_name" and key in PROTECTED_NAME_KEYS
    )


def normalize_locale_code(raw_code: str) -> str:
    """Normalize Weblate or POSIX locale tags (e.g. pt_BR, sr@latin) toward BCP-47."""
    cleaned = raw_code.strip().replace("@", "-").replace("_", "-")
    if cleaned.lower().endswith("-latin"):
        cleaned = cleaned[:-6] + "-Latn"
    return cleaned


def compute_total_strings(template: dict[str, Any]) -> int:
    # Retain template metadata convention if keys count matches, otherwise compute directly
    meta = template.get("__metadata__", {})
    meta_total = meta.get("total_strings")
    computed = (
        len(template.get("strings_by_id", {}))
        + len(template.get("strings_by_text", {}))
        + len(template.get("strings_by_value", {}))
    )
    if isinstance(meta_total, int) and meta_total > 0:
        return meta_total
    return computed


def build_metadata(
    lang_code: str,
    display_name: str,
    template: dict[str, Any],
) -> dict[str, Any]:
    """Build normalized __metadata__ aligned with _template_zh.json."""
    tmpl_meta = template.get("__metadata__", {})
    return {
        "locale": lang_code,
        "name": display_name,
        "app": tmpl_meta.get("app", "net.huanci.hsjpro"),
        "version": tmpl_meta.get("version", "3.4.6"),
        "total_strings": compute_total_strings(template),
    }


def cmd_init(args: argparse.Namespace) -> int:
    lang_code = normalize_locale_code(args.lang_code)
    if not BCP47_RE.match(lang_code):
        print(
            f"ERROR: Invalid language code '{lang_code}'. Use BCP-47 such as 'es', 'ja', 'ko', 'pt-BR'.",
            file=sys.stderr,
        )
        return 1
    if lang_code.lower() in {"zh-cn", "_template-zh"}:
        print("ERROR: Use a target locale code other than 'zh-CN'.", file=sys.stderr)
        return 1

    template = load_json_utf8(args.template)
    en_data = load_json_utf8(args.en) if args.from_en and args.en.is_file() else None

    out_path: Path = args.output or (args.lang_dir / f"{lang_code}.json")
    if out_path.exists() and not args.force:
        print(
            f"ERROR: Target file already exists: {out_path}. Pass --force to overwrite.",
            file=sys.stderr,
        )
        return 1

    display_name = args.name or COMMON_LOCALE_NAMES.get(lang_code) or lang_code

    new_doc: dict[str, Any] = {
        "__metadata__": build_metadata(lang_code, display_name, template)
    }

    for sec in SECTIONS:
        ref_sec = template.get(sec, {})
        en_sec = en_data.get(sec, {}) if en_data else {}
        out_sec: dict[str, str] = {}
        for k, zh_val in ref_sec.items():
            if en_data and not is_protected_key(sec, k) and isinstance(en_sec.get(k), str):
                out_sec[k] = en_sec[k]
            else:
                out_sec[k] = zh_val
        new_doc[sec] = out_sec

    write_json_lf(out_path, new_doc)
    total_keys = sum(len(new_doc[s]) for s in SECTIONS)
    print(f"Created {out_path} (locale={lang_code}, name={display_name}, entries={total_keys}).")
    return 0


def sync_one_file(
    target_path: Path,
    template: dict[str, Any],
    en_data: dict[str, Any] | None,
    sync_duplicates: bool,
    unify_conflicts: bool,
    prefer_order: tuple[str, ...],
    dry_run: bool,
) -> tuple[int, int, int]:
    target = load_json_utf8(target_path)
    tgt_meta = target.get("__metadata__", {})
    if not isinstance(tgt_meta, dict):
        tgt_meta = {}

    merged_meta = build_metadata(
        str(tgt_meta.get("locale", target_path.stem)),
        str(tgt_meta.get("name", target_path.stem)),
        template,
    )

    added = 0
    removed = 0
    propagated = 0

    synced: dict[str, Any] = {"__metadata__": merged_meta}
    for sec in SECTIONS:
        ref_sec: dict[str, str] = template.get(sec, {})
        tgt_sec: dict[str, str] = target.get(sec, {}) if isinstance(target.get(sec), dict) else {}
        en_sec: dict[str, str] = en_data.get(sec, {}) if en_data and isinstance(en_data.get(sec), dict) else {}

        for k in tgt_sec:
            if k not in ref_sec:
                removed += 1

        new_sec: dict[str, str] = {}
        for k, zh_val in ref_sec.items():
            if k in tgt_sec and isinstance(tgt_sec[k], str) and tgt_sec[k] != "":
                new_sec[k] = tgt_sec[k]
            else:
                added += 1
                if en_data and not is_protected_key(sec, k) and isinstance(en_sec.get(k), str):
                    new_sec[k] = en_sec[k]
                else:
                    new_sec[k] = zh_val
        synced[sec] = new_sec

    if sync_duplicates:
        zh_to_trans: dict[str, str] = {}
        for sec in prefer_order:
            ref_sec = template[sec]
            sec_map = synced[sec]
            for k, zh_val in ref_sec.items():
                if is_protected_key(sec, k):
                    continue
                val = sec_map[k]
                if val != zh_val and zh_val not in zh_to_trans:
                    zh_to_trans[zh_val] = val

        for sec in SECTIONS:
            ref_sec = template[sec]
            sec_map = synced[sec]
            for k, zh_val in ref_sec.items():
                if is_protected_key(sec, k):
                    continue
                if zh_val not in zh_to_trans:
                    continue
                best = zh_to_trans[zh_val]
                if sec_map[k] == zh_val or (unify_conflicts and sec_map[k] != best):
                    if sec_map[k] != best:
                        sec_map[k] = best
                        propagated += 1

    if not dry_run:
        write_json_lf(target_path, synced)

    return added, removed, propagated


def cmd_sync(args: argparse.Namespace) -> int:
    template = load_json_utf8(args.template)
    en_data = load_json_utf8(args.en) if args.from_en and args.en.is_file() else None

    if args.all:
        targets = sorted(
            p for p in args.lang_dir.glob("*.json")
            if p.is_file() and p.resolve() != args.template.resolve()
        )
    elif args.files:
        targets = list(args.files)
    else:
        print("ERROR: Specify one or more file paths or pass --all.", file=sys.stderr)
        return 1

    all_secs = list(SECTIONS)
    prefer_order = (args.prefer,) + tuple(s for s in all_secs if s != args.prefer)

    for path in targets:
        added, removed, propagated = sync_one_file(
            target_path=path,
            template=template,
            en_data=en_data,
            sync_duplicates=args.sync_duplicates,
            unify_conflicts=args.unify,
            prefer_order=prefer_order,
            dry_run=args.dry_run,
        )
        prefix = "[DRY-RUN] " if args.dry_run else ""
        print(
            f"{prefix}Synced {path}: added={added}, removed={removed}, propagated={propagated}"
        )
    return 0


def canonical_flat_key(sec: str, key: str, zh_val: str, id_to_name: dict[str, str], used: set[str]) -> str:
    if sec == "strings_by_id" and key in id_to_name:
        candidate = f"res.{id_to_name[key]}"
    elif sec == "strings_by_name":
        candidate = f"res.{key}"
    elif sec == "strings_by_text" and key.startswith("libre_"):
        candidate = f"libre.{key}"
    else:
        digest = hashlib.sha1(zh_val.encode("utf-8")).hexdigest()[:10]
        candidate = f"txt.{digest}"
    if candidate in used:
        digest = hashlib.sha1(f"{sec}:{key}".encode("utf-8")).hexdigest()[:10]
        candidate = f"{candidate}.{digest}"
    used.add(candidate)
    return candidate


def dedup_token_for(sec: str, key: str, zh_val: str) -> str:
    # Keep libre_* and other symbolic keys distinct even if Chinese text matches
    return f"SYM:{key}" if (sec == "strings_by_text" and key != zh_val) else f"TXT:{zh_val}"


def build_flat_mapping(template: dict[str, Any]) -> dict[str, str]:
    """Build deterministic deduplication token -> flat key mapping from _template_zh.json."""
    id_keys = list(template["strings_by_id"].keys())
    name_keys = list(template["strings_by_name"].keys())
    id_to_name = {
        ik: nk
        for ik, nk in zip(id_keys, name_keys)
        if template["strings_by_id"][ik] == template["strings_by_name"][nk]
    }

    seen_zh_key: dict[str, str] = {}
    used_keys: set[str] = set()
    for sec in SECTIONS:
        for k, zh_val in template[sec].items():
            if is_protected_key(sec, k):
                continue
            token = dedup_token_for(sec, k, zh_val)
            if token not in seen_zh_key:
                seen_zh_key[token] = canonical_flat_key(sec, k, zh_val, id_to_name, used_keys)
    return seen_zh_key


def flatten_flat_json(data: dict[str, Any], prefix: str = "") -> dict[str, str]:
    """Flatten flat or nested Weblate JSON objects into dot-joined string entries."""
    out: dict[str, str] = {}
    for k, v in data.items():
        full_key = f"{prefix}.{k}" if prefix else str(k)
        if isinstance(v, dict):
            out.update(flatten_flat_json(v, full_key))
        elif isinstance(v, str):
            out[full_key] = v
    return out


def export_one_flat(
    template: dict[str, Any],
    input_path: Path,
    output_path: Path,
    mapping: dict[str, str],
) -> tuple[int, bool]:
    """Export one 4-table dictionary into deduplicated flat JSON."""
    lang_data = load_json_utf8(input_path)
    flat: dict[str, str] = {}
    written_tokens: set[str] = set()

    for sec in SECTIONS:
        ref_sec = template[sec]
        tgt_sec = lang_data.get(sec, {})
        if not isinstance(tgt_sec, dict):
            tgt_sec = {}
        for k, zh_val in ref_sec.items():
            if is_protected_key(sec, k):
                continue
            token = dedup_token_for(sec, k, zh_val)
            if token in written_tokens:
                continue
            written_tokens.add(token)
            flat_key = mapping[token]
            val = tgt_sec.get(k, zh_val)
            flat[flat_key] = val if isinstance(val, str) and val != "" else zh_val

    changed = write_json_lf(output_path, flat)
    return len(flat), changed


def import_one_flat(
    template: dict[str, Any],
    flat_path: Path,
    target_path: Path,
    mapping: dict[str, str],
    locale: str | None = None,
    name: str | None = None,
) -> tuple[int, bool, bool]:
    """Compile a flat JSON file into a new or existing 4-table dictionary."""
    raw_flat = load_json_utf8(flat_path)
    flat = flatten_flat_json(raw_flat)

    created = not target_path.is_file()
    if created:
        lang_code = normalize_locale_code(locale or target_path.stem)
        if not BCP47_RE.match(lang_code) or lang_code.lower() in {
            "zh-cn",
            "_template-zh",
            "weblate-template",
        }:
            raise ValueError(
                f"Invalid target locale '{lang_code}' derived from {flat_path.name}. Use BCP-47 tags such as 'es', 'ja', or 'pt-BR'."
            )
        display_name = name or COMMON_LOCALE_NAMES.get(lang_code) or lang_code
        existing: dict[str, Any] = {}
        merged_meta = build_metadata(lang_code, display_name, template)
    else:
        existing = load_json_utf8(target_path)
        tgt_meta = existing.get("__metadata__", {})
        if not isinstance(tgt_meta, dict):
            tgt_meta = {}
        lang_code = str(locale or tgt_meta.get("locale") or normalize_locale_code(target_path.stem))
        display_name = str(name or tgt_meta.get("name") or COMMON_LOCALE_NAMES.get(lang_code) or lang_code)
        merged_meta = build_metadata(lang_code, display_name, template)

    compiled: dict[str, Any] = {"__metadata__": merged_meta}
    updated = 0

    for sec in SECTIONS:
        ref_sec: dict[str, str] = template[sec]
        prev_sec = existing.get(sec, {})
        if not isinstance(prev_sec, dict):
            prev_sec = {}
        new_sec: dict[str, str] = {}

        for k, zh_val in ref_sec.items():
            prev_val = prev_sec.get(k)
            if is_protected_key(sec, k):
                next_val = zh_val
            else:
                flat_key = mapping.get(dedup_token_for(sec, k, zh_val))
                flat_val = flat.get(flat_key) if flat_key else None
                if isinstance(flat_val, str) and flat_val != "":
                    next_val = flat_val
                elif isinstance(prev_val, str) and prev_val != "":
                    next_val = prev_val
                else:
                    next_val = zh_val

            if prev_val != next_val:
                updated += 1
            new_sec[k] = next_val

        compiled[sec] = new_sec

    changed = write_json_lf(target_path, compiled)
    return updated, created, changed


def resolve_flat_output_path(lang_file: Path, flat_dir: Path) -> Path:
    """Resolve destination flat file path, reusing existing underscore variants if present."""
    direct = flat_dir / lang_file.name
    if direct.exists():
        return direct
    if "-" in lang_file.stem:
        alt = flat_dir / f"{lang_file.stem.replace('-', '_')}.json"
        if alt.exists():
            return alt
    return direct


def cmd_export_flat(args: argparse.Namespace) -> int:
    template = load_json_utf8(args.template)
    mapping = build_flat_mapping(template)

    if args.all:
        if args.input_file is not None or args.output is not None:
            print("ERROR: Do not pass positional input_file or --output when using --all.", file=sys.stderr)
            return 1
        sources = sorted(p for p in args.lang_dir.glob("*.json") if p.is_file())
        if not sources:
            print(f"ERROR: No JSON files found in {args.lang_dir}.", file=sys.stderr)
            return 1
        exported_template = False
        for src_path in sources:
            if src_path.resolve() == args.template.resolve() or src_path.stem == "_template_zh":
                if src_path.resolve() != args.template.resolve():
                    continue
                out_path = args.flat_template
                exported_template = True
            else:
                out_path = resolve_flat_output_path(src_path, args.flat_dir)
            count, changed = export_one_flat(template, src_path, out_path, mapping)
            state = "updated" if changed else "unchanged"
            print(f"Exported {count} keys from {src_path} to {out_path} ({state}).")
        if not exported_template and args.template.is_file():
            count, changed = export_one_flat(template, args.template, args.flat_template, mapping)
            state = "updated" if changed else "unchanged"
            print(f"Exported {count} keys from {args.template} to {args.flat_template} ({state}).")
        return 0

    if args.input_file is None or args.output is None:
        print("ERROR: Specify <input_file> and --output/-o, or pass --all.", file=sys.stderr)
        return 1

    count, changed = export_one_flat(template, args.input_file, args.output, mapping)
    state = "updated" if changed else "unchanged"
    print(f"Exported {count} deduplicated translation keys to {args.output} ({state}).")
    return 0


def cmd_import_flat(args: argparse.Namespace) -> int:
    template = load_json_utf8(args.template)
    mapping = build_flat_mapping(template)

    if args.all:
        if args.flat_file is not None or args.target is not None:
            print("ERROR: Do not pass positional flat_file or --target when using --all.", file=sys.stderr)
            return 1
        if not args.flat_dir.is_dir():
            print(f"ERROR: Flat directory does not exist: {args.flat_dir}", file=sys.stderr)
            return 1
        flat_files = sorted(p for p in args.flat_dir.glob("*.json") if p.is_file())
        for fp in flat_files:
            if (
                fp.resolve() == args.flat_template.resolve()
                or fp.name == args.template.name
                or fp.name == args.flat_template.name
                or fp.stem.lower() in {"_template_zh", "weblate-template", "weblate_template", "zh-cn", "zh_cn"}
            ):
                print(f"Skipped source template {fp}.")
                continue
            norm_code = normalize_locale_code(fp.stem)
            target_path = args.lang_dir / f"{norm_code}.json"
            try:
                updated, created, changed = import_one_flat(
                    template, fp, target_path, mapping
                )
            except ValueError as exc:
                print(f"ERROR: {exc}", file=sys.stderr)
                return 1
            action = "Created" if created else ("Updated" if changed else "Unchanged")
            print(f"{action} {target_path} from {fp} (entries changed={updated}).")
        return 0

    if args.flat_file is None or args.target is None:
        print("ERROR: Specify <flat_file> and --target/-t, or pass --all.", file=sys.stderr)
        return 1

    try:
        updated, created, changed = import_one_flat(
            template,
            args.flat_file,
            args.target,
            mapping,
            locale=args.locale,
            name=args.name,
        )
    except ValueError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    action = "Created" if created else ("Updated" if changed else "Unchanged")
    print(f"{action} {args.target} from flat translations (entries changed={updated}).")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="HuashijieLibre dictionary initialization and sync helper."
    )
    parser.add_argument("--lang-dir", type=Path, default=Path("lang"))
    parser.add_argument("--template", type=Path, default=Path("lang/_template_zh.json"))
    parser.add_argument("--en", type=Path, default=Path("lang/en.json"))

    sub = parser.add_subparsers(dest="command", required=True)

    p_init = sub.add_parser("init", help="Initialize a new language pack from _template_zh.json.")
    p_init.add_argument("lang_code", help="BCP-47 language code (e.g. es, ja, ko, pt-BR).")
    p_init.add_argument("--name", default=None, help="Native language display name.")
    p_init.add_argument("--output", type=Path, default=None, help="Custom output JSON path.")
    p_init.add_argument("--from-en", action="store_true", help="Seed strings from lang/en.json instead of Chinese.")
    p_init.add_argument("--force", action="store_true", help="Overwrite target file if it already exists.")
    p_init.set_defaults(func=cmd_init)

    p_sync = sub.add_parser("sync", help="Synchronize dictionary keys and ordering against _template_zh.json.")
    p_sync.add_argument("files", nargs="*", type=Path, help="Target dictionary files.")
    p_sync.add_argument("--all", action="store_true", help="Sync all non-template JSON files in lang/.")
    p_sync.add_argument("--from-en", action="store_true", help="Use lang/en.json fallback for newly added keys.")
    p_sync.add_argument(
        "--sync-duplicates",
        action="store_true",
        help="Fill untranslated entries matching an already-translated identical Chinese string.",
    )
    p_sync.add_argument(
        "--unify",
        action="store_true",
        help="With --sync-duplicates, also harmonize existing conflicts toward --prefer section.",
    )
    p_sync.add_argument(
        "--prefer",
        choices=SECTIONS,
        default="strings_by_id",
        help="Preferred section when resolving duplicate translations (default: strings_by_id).",
    )
    p_sync.add_argument("--dry-run", action="store_true", help="Preview counts without modifying files.")
    p_sync.set_defaults(func=cmd_sync)

    p_exp = sub.add_parser("export-flat", help="Export deduplicated flat JSON for Weblate/Crowdin.")
    p_exp.add_argument("input_file", nargs="?", type=Path, default=None, help="Source or translated dictionary JSON.")
    p_exp.add_argument("--output", "-o", type=Path, default=None, help="Output flat JSON path.")
    p_exp.add_argument(
        "--all",
        action="store_true",
        help="Export target lang/*.json files into --flat-dir and the base template to --flat-template.",
    )
    p_exp.add_argument("--flat-dir", type=Path, default=DEFAULT_FLAT_DIR, help="Flat dictionary directory.")
    p_exp.add_argument(
        "--flat-template",
        type=Path,
        default=DEFAULT_FLAT_TEMPLATE,
        help="Output flat monolingual base template path for --all (default: i18n/weblate-template.json).",
    )
    p_exp.set_defaults(func=cmd_export_flat)

    p_imp = sub.add_parser("import-flat", help="Import translated flat JSON back into full dictionary JSON.")
    p_imp.add_argument("flat_file", nargs="?", type=Path, default=None, help="Translated flat JSON path.")
    p_imp.add_argument("--target", "-t", type=Path, default=None, help="Target dictionary JSON path to update or create.")
    p_imp.add_argument("--locale", default=None, help="Explicit BCP-47 locale override when creating a new dictionary.")
    p_imp.add_argument("--name", default=None, help="Explicit native display name when creating a new dictionary.")
    p_imp.add_argument("--all", action="store_true", help="Import all non-template flat JSON files from --flat-dir.")
    p_imp.add_argument("--flat-dir", type=Path, default=DEFAULT_FLAT_DIR, help="Flat dictionary directory.")
    p_imp.add_argument(
        "--flat-template",
        type=Path,
        default=DEFAULT_FLAT_TEMPLATE,
        help="Flat monolingual base template path excluded from --all imports (default: i18n/weblate-template.json).",
    )
    p_imp.set_defaults(func=cmd_import_flat)

    return parser


def main(argv: list[str] | None = None) -> int:
    configure_stdio()
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
