#!/usr/bin/env python3
"""Validate the mapped DOS payload before making a transfer archive."""
import argparse
from pathlib import Path
import re


def validate(root):
    errors = []
    seen = {}
    count = 0
    longest = 0
    selections = ("BASE", "DOS", "TESTSUIT/OFFLEXAM/RAYOFFEX")
    for selection in selections:
        directory = root / selection
        if not directory.is_dir():
            errors.append("Missing mapped directory: " + selection)
            continue
        for path in [directory, *directory.rglob("*")]:
            relative = path.relative_to(root).as_posix()
            key = relative.upper()
            if key in seen and seen[key] != relative:
                errors.append("DOS name collision: " + seen[key] + " / " + relative)
            seen[key] = relative
            for part in Path(relative).parts:
                if not re.fullmatch(r"[A-Za-z0-9_$~-]{1,8}(\.[A-Za-z0-9_$~-]{1,3})?", part):
                    errors.append("Not an 8.3 name: " + relative)
                    break
            target = "C:\\VITRAL\\TURBOC\\" + relative.replace("/", "\\")
            if path.is_dir():
                longest = max(longest, len(target))
                if len(target) > 63:
                    errors.append("Directory exceeds DOS path limit: " + target)
            count += path.is_file()
    if errors:
        raise SystemExit("\n".join(errors))
    print(f"Validated {count} DOS files; longest directory: {longest} characters")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    validate(parser.parse_args().root)
