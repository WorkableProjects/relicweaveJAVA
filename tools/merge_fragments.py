#!/usr/bin/env python3
"""Merge resource fragments written by parallel tasks into the shared resource files.

Layout: tools/fragments/<task-id>/<path under src/main/resources>, for example
  tools/fragments/T01/assets/relicweave/lang/en_us.json
  tools/fragments/T02/data/minecraft/tags/blocks/mineable/pickaxe.json

Rules
- lang/*.json and sounds.json: key-by-key merge. A key already present with a different value is an error.
- tags (any path containing /tags/): "values" are unioned, existing order kept, "replace" stays false.
- any other file: copied if the target does not exist; identical targets are fine; differing targets are an error.

Usage: python3 tools/merge_fragments.py [--keep]   (fragments are deleted after a clean merge unless --keep)
"""
import json
import shutil
import sys
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FRAGMENTS = ROOT / "tools" / "fragments"
RESOURCES = ROOT / "src" / "main" / "resources"


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f, object_pairs_hook=OrderedDict)


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def main():
    keep = "--keep" in sys.argv
    errors = []
    merged = 0
    if not FRAGMENTS.exists():
        print("no fragments")
        return 0
    for task_dir in sorted(p for p in FRAGMENTS.iterdir() if p.is_dir()):
        for frag in sorted(p for p in task_dir.rglob("*") if p.is_file()):
            rel = frag.relative_to(task_dir)
            target = RESOURCES / rel
            label = f"{task_dir.name}:{rel}"
            is_dict_merge = rel.suffix == ".json" and (rel.parent.name == "lang" or rel.name == "sounds.json")
            is_tag = "/tags/" in "/" + rel.as_posix() and rel.suffix == ".json"
            if is_dict_merge:
                data = load(target) if target.exists() else OrderedDict()
                for key, value in load(frag).items():
                    if key in data and data[key] != value:
                        errors.append(f"{label}: key {key!r} already has a different value")
                    else:
                        data[key] = value
                dump(target, data)
            elif is_tag:
                data = load(target) if target.exists() else OrderedDict([("replace", False), ("values", [])])
                for value in load(frag).get("values", []):
                    if value not in data["values"]:
                        data["values"].append(value)
                dump(target, data)
            else:
                if target.exists():
                    if target.read_bytes() != frag.read_bytes():
                        errors.append(f"{label}: target exists with different content")
                        continue
                else:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(frag, target)
            merged += 1
    for e in errors:
        print("ERROR", e)
    print(f"merged {merged} fragment files, {len(errors)} errors")
    if not errors and not keep:
        for task_dir in [p for p in FRAGMENTS.iterdir() if p.is_dir()]:
            shutil.rmtree(task_dir)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
