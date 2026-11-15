#!/usr/bin/env python3
"""Check the include guards of the turboc port.

Turbo C++ 3.0 distinguishes identifiers, macros included, by their first 32
characters only. Every include guard of the port (the `#ifndef NAME` /
`#define NAME` pair that opens a `.h` or `.txx` file) must therefore be at most
31 characters long, and no two guards may be equal (which, with that length,
also means that no two are equal in their first 32 characters).

Usage: checkIncludeGuards.py [root]   (default: the turboc folder)
Exits with a non-zero status and lists the offending files on failure.
"""
import os
import re
import sys

MAXIMUM_LENGTH = 31
SKIPPED_FOLDERS = ("build", "buildmk", "buildtc", "__pycache__", ".git")
GUARD = re.compile(r"^\s*#\s*ifndef\s+(\w+)\s*\n\s*#\s*define\s+(\w+)", re.M)


def guards(root):
    found = []
    for folder, folders, files in os.walk(root):
        folders[:] = sorted(f for f in folders if f not in SKIPPED_FOLDERS)
        for name in sorted(files):
            if not (name.endswith(".h") or name.endswith(".txx")):
                continue
            path = os.path.join(folder, name)
            with open(path, encoding="latin-1") as stream:
                text = stream.read()
            match = GUARD.search(text)
            if match is not None and match.group(1) == match.group(2):
                found.append((match.group(1), os.path.relpath(path, root)))
    return found


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else \
        os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
    errors = []
    seen = {}
    found = guards(root)
    for guard, path in found:
        if len(guard) > MAXIMUM_LENGTH:
            errors.append("Guard longer than %d characters (%d): %s in %s"
                          % (MAXIMUM_LENGTH, len(guard), guard, path))
        key = guard[:32]
        if key in seen:
            errors.append("Guard %s of %s coincides with %s of %s"
                          % (guard, path, seen[key][0], seen[key][1]))
        else:
            seen[key] = (guard, path)
    if errors:
        sys.stderr.write("\n".join(errors) + "\n")
        sys.exit(1)
    print("Checked %d include guards: all distinct, at most %d characters"
          % (len(found), MAXIMUM_LENGTH))


if __name__ == "__main__":
    main()
