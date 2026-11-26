#!/usr/bin/env python3
"""
Contributor helper CLI for HuashijieLibre translation dictionaries.

Supports:
1. init: scaffold a new lang/<lang_code>.json from lang/_template_zh.json
2. sync: align existing dictionary keys and ordering with _template_zh.json,
   and optionally propagate translations across duplicate source strings
3. export-flat / import-flat: deduplicated flat JSON bridge for Hosted Weblate
   or Crowdin Open Source workflows without touching __metadata__.

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
PROTECTED_ID_KEYS = {"0x7f110803", "0x7f110077"}
PROTECTED_NAME_KEYS = {"sdcard_root_folder", "album_folder"}
BCP47_RE = re.compile(r"^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})*$")

COMMON_LOCALE_NAMES = {
    "ar": "العربية",
    "de": "Deutsch",
    "en": "English",
    "en-US": "English",
    "es": "Español",
    "fr": "Français",
    "id": "Bahasa Indonesia",
    "it": "Italiano",
    "ja": "日本語",
    "ko": "한국어",
    "ms": "Bahasa Melayu",
    "pl": "Polski",
    "pt": "Português",
    "pt-BR": "Português (Brasil)",
    "ru": "Русский",
    "th": "ไทย",
    "tr": "Türkçe",
    "uk": "Українська",
    "vi": "Tiếng Việt",
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


def write_json_lf(path: Path, data: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    path.write_text(serialized, encoding="utf-8", newline="\n")


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


def cmd_init(args: argparse.Namespace) -> int:
    lang_code = args.lang_code.strip()
    if not BCP47_RE.match(lang_code):
        print(
            f"ERROR: Invalid language code '{lang_code}'. Use BCP-47 such as 'es', 'ja', 'ko', 'pt-BR'.",
            file=sys.stderr,
        )
        return 1
    if lang_code.lower() in {"zh-cn", "_template_zh"}:
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

    tmpl_meta = template.get("__metadata__", {})
    display_name = args.name or COMMON_LOCALE_NAMES.get(lang_code) or lang_code

    new_doc: dict[str, Any] = {
        "__metadata__": {
            "locale": lang_code,
            "name": display_name,
            "app": tmpl_meta.get("app", "net.huanci.hsjpro"),
            "version": tmpl_meta.get("version", "3.4.6"),
            "total_strings": compute_total_strings(template),
        }
    }

    for sec in SECTIONS:
        ref_sec = template.get(sec, {})
        en_sec = en_data.get(sec, {}) if en_data else {}
        out_sec: dict[str, str] = {}
        for k, zh_val in ref_sec.items():
            is_protected = (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
            )
            if en_data and not is_protected and isinstance(en_sec.get(k), str):
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
    tmpl_meta = template.get("__metadata__", {})
    tgt_meta = target.get("__metadata__", {})
    if not isinstance(tgt_meta, dict):
        tgt_meta = {}

    merged_meta = {
        "locale": tgt_meta.get("locale", target_path.stem),
        "name": tgt_meta.get("name", target_path.stem),
        "app": tmpl_meta.get("app", "net.huanci.hsjpro"),
        "version": tmpl_meta.get("version", "3.4.6"),
        "total_strings": compute_total_strings(template),
    }

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
                is_protected = (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                    sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
                )
                if en_data and not is_protected and isinstance(en_sec.get(k), str):
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
                if (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                    sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
                ):
                    continue
                val = sec_map[k]
                if val != zh_val and zh_val not in zh_to_trans:
                    zh_to_trans[zh_val] = val

        for sec in SECTIONS:
            ref_sec = template[sec]
            sec_map = synced[sec]
            for k, zh_val in ref_sec.items():
                if (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                    sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
                ):
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


def cmd_export_flat(args: argparse.Namespace) -> int:
    template = load_json_utf8(args.template)
    lang_data = load_json_utf8(args.input_file)

    id_keys = list(template["strings_by_id"].keys())
    name_keys = list(template["strings_by_name"].keys())
    id_to_name = {
        ik: nk
        for ik, nk in zip(id_keys, name_keys)
        if template["strings_by_id"][ik] == template["strings_by_name"][nk]
    }

    flat: dict[str, str] = {}
    seen_zh_key: dict[str, str] = {}
    used_keys: set[str] = set()

    for sec in SECTIONS:
        ref_sec = template[sec]
        tgt_sec = lang_data.get(sec, {})
        for k, zh_val in ref_sec.items():
            if (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
            ):
                continue
            # Keep libre_* and other symbolic keys distinct even if Chinese text matches
            dedup_token = f"SYM:{k}" if (sec == "strings_by_text" and k != zh_val) else f"TXT:{zh_val}"
            if dedup_token in seen_zh_key:
                continue
            flat_key = canonical_flat_key(sec, k, zh_val, id_to_name, used_keys)
            seen_zh_key[dedup_token] = flat_key
            val = tgt_sec.get(k, zh_val)
            flat[flat_key] = val if isinstance(val, str) and val != "" else zh_val

    write_json_lf(args.output, flat)
    print(f"Exported {len(flat)} deduplicated translation keys to {args.output}.")
    return 0


def cmd_import_flat(args: argparse.Namespace) -> int:
    template = load_json_utf8(args.template)
    flat = load_json_utf8(args.flat_file)
    target = load_json_utf8(args.target)

    id_keys = list(template["strings_by_id"].keys())
    name_keys = list(template["strings_by_name"].keys())
    id_to_name = {
        ik: nk
        for ik, nk in zip(id_keys, name_keys)
        if template["strings_by_id"][ik] == template["strings_by_name"][nk]
    }

    # Build exact same dedup mapping from template to flat keys
    seen_zh_key: dict[str, str] = {}
    used_keys: set[str] = set()
    for sec in SECTIONS:
        for k, zh_val in template[sec].items():
            if (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
            ):
                continue
            dedup_token = f"SYM:{k}" if (sec == "strings_by_text" and k != zh_val) else f"TXT:{zh_val}"
            if dedup_token not in seen_zh_key:
                seen_zh_key[dedup_token] = canonical_flat_key(sec, k, zh_val, id_to_name, used_keys)

    updated = 0
    for sec in SECTIONS:
        ref_sec = template[sec]
        tgt_sec = target.setdefault(sec, {})
        for k, zh_val in ref_sec.items():
            if (sec == "strings_by_id" and k in PROTECTED_ID_KEYS) or (
                sec == "strings_by_name" and k in PROTECTED_NAME_KEYS
            ):
                tgt_sec[k] = zh_val
                continue
            dedup_token = f"SYM:{k}" if (sec == "strings_by_text" and k != zh_val) else f"TXT:{zh_val}"
            flat_key = seen_zh_key.get(dedup_token)
            if flat_key and isinstance(flat.get(flat_key), str) and flat[flat_key] != "":
                if tgt_sec.get(k) != flat[flat_key]:
                    tgt_sec[k] = flat[flat_key]
                    updated += 1

    write_json_lf(args.target, target)
    print(f"Imported flat translations into {args.target} (updated {updated} entries).")
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
    p_exp.add_argument("input_file", type=Path, help="Source or translated dictionary JSON.")
    p_exp.add_argument("--output", "-o", type=Path, required=True, help="Output flat JSON path.")
    p_exp.set_defaults(func=cmd_export_flat)

    p_imp = sub.add_parser("import-flat", help="Import translated flat JSON back into full dictionary JSON.")
    p_imp.add_argument("flat_file", type=Path, help="Translated flat JSON path.")
    p_imp.add_argument("--target", "-t", type=Path, required=True, help="Target dictionary JSON path to update.")
    p_imp.set_defaults(func=cmd_import_flat)

    return parser


def main(argv: list[str] | None = None) -> int:
    configure_stdio()
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
