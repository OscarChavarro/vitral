#!/usr/bin/env python3
"""Find, on the host, C++ constructs that Borland Turbo C++ 3.0 rejects.

Modern compilers (gcc -std=gnu++98, clang -std=c++98) accept all of these, so
they only show up after a transfer to the DOS machine and a 20 minute build.
Each rule comes from an error of the real compiler:

  for-scope   a for-init declaration lives in the enclosing block (pre-ISO
              rule): two `for (int i ...)`, or one and a plain `int i`, in one
              block are a "Multiple declaration".
  keyword     `near`, `far`, `huge`, `pascal`, `cdecl`, `interrupt`, `_seg`,
              ... are Turbo C++ keywords: flagged when used as a name.
  condition   no declarations in `if (T* x = ...)` / `while (...)`.
  nested      a nested class defined outside its enclosing class
              (`class A::B {`) is a "Multiple declaration".
  template    `static` function templates are rejected, and `T` is not
              deduced from an argument such as `List<T*>&`.
  ternary     class temporaries in a conditional expression ("Destructor
              for String required in conditional expression"); heuristic,
              for `String(...)` and `Type()` temporaries.
  friend      Vitral does not use `friend` declarations (in any port).
  inline-ctor a loop (`for`, `while`, `do`) in an inline constructor defined
              out of its class (`inline A::A(...) {...}` in a header) is an
              "Expression syntax" error: define it in the .cpp.

The rules are heuristics (no parser): check each hit, and the real compiler
remains the reference (see scripts/makeProbeBat.py).

Usage: python3 scripts/checkTurboCpp.py [files...]
Without files, it checks the sources that the DOS batches compile.
"""
import re
import subprocess
import sys
from pathlib import Path

KEYWORDS = ("near", "far", "huge", "pascal", "cdecl", "interrupt", "_seg", "_cs", "_ds", "_es", "_ss",
            "_export", "_loadds", "_saveregs")
TYPE = (r"(?:const\s+)?(?:unsigned\s+|signed\s+)?(?:long\s+long|long\s+int|long|int|short|char|size_t|double|float"
        r"|BOOLEAN|[A-Z]\w*)\s*\**\s*")
FOR_DECL = re.compile(r"\bfor\s*\(\s*" + TYPE + r"(\w+)\s*=")
PLAIN_DECL = re.compile(r"^\s*" + TYPE + r"(\w+)\s*(?:=[^=]|;|,)")
NOT_DECL = re.compile(r"\s*(return|delete|else|case|goto|throw)\b")
# A keyword used as a name: followed by what follows a variable, not by `*`,
# `(` or a declarator (`char far *p`, `void interrupt handler()` are fine)
KEYWORD_RE = re.compile(r"\b(" + "|".join(KEYWORDS) + r")\s*(?:[=;,)?\[.]|->)")
CONDITION_RE = re.compile(r"\b(?:if|while)\s*\(\s*(?:const\s+)?[A-Za-z_][\w:<>]*\s*[*&]?\s*\w+\s*=[^=]")
NESTED_RE = re.compile(r"^\s*(?:class|struct)\s+\w+::\w+\s*[:{]")
STATIC_TEMPLATE_RE = re.compile(r"template\s*<[^>]*>\s*static\b")
FRIEND_RE = re.compile(r"\bfriend\b")
TERNARY_RE = re.compile(r"\?[^;]*(?:\bString\s*\(|(?<!new )\b[A-Z]\w*\s*\(\s*\))")
DEFAULT_ROOTS = ("base/src/main", "dos/src/main", "bgi/src", "testsuite/BGIExamples", "testsuite/OfflineExamples")
# Sources of base that the DOS batches do not compile (see map_to_83.py)
EXCLUDED = ("java/net/", "java/util/regex/", "TangibleInterfaceNetworkClient")


def strip(text):
    """Blanks comments and string literals, keeping the line numbers."""
    text = re.sub(r"/\*.*?\*/", lambda m: "\n" * m.group().count("\n"), text, flags=re.S)
    text = re.sub(r"//[^\n]*", "", text)
    return re.sub(r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', '""', text)


def check(path):
    findings = []
    text = strip(Path(path).read_text(errors="replace"))
    findings += check_inline_constructors(path, text)
    lines = text.split("\n")
    stack = [dict()]
    for number, line in enumerate(lines, 1):
        if line.lstrip().startswith("#"):
            continue
        where = "%s:%d" % (path, number)
        if KEYWORD_RE.search(line):
            findings.append((where, "keyword", "'%s' is a Turbo C++ keyword" % KEYWORD_RE.search(line).group(1)))
        if CONDITION_RE.search(line) and "==" not in line.split("=", 1)[1][:1]:
            findings.append((where, "condition", "declaration in a condition"))
        if NESTED_RE.match(line):
            findings.append((where, "nested", "nested class defined outside its enclosing class"))
        if STATIC_TEMPLATE_RE.search(line):
            findings.append((where, "template", "static function template"))
        if FRIEND_RE.search(line):
            findings.append((where, "friend", "friend declaration"))
        if TERNARY_RE.search(line):
            findings.append((where, "ternary", "class temporary in a conditional expression?"))
        # Blocks: split at braces so that `{ for (...) }` on one line is seen inside its block
        for piece in re.split(r"([{}])", line):
            if piece == "{":
                stack.append(dict())
                continue
            if piece == "}":
                if len(stack) > 1:
                    stack.pop()
                continue
            names = [(m.group(1), "for") for m in FOR_DECL.finditer(piece)]
            if not names:
                match = PLAIN_DECL.match(piece)
                if match and not NOT_DECL.match(piece):
                    names.append((match.group(1), "plain"))
            for name, kind in names:
                block = stack[-1]
                if name in block and (kind == "for" or block[name][1] == "for"):
                    findings.append((where, "for-scope",
                                     "'%s' already declared in this block at line %d" % (name, block[name][0])))
                block.setdefault(name, (number, kind))
    return findings


LOOP_RE = re.compile(r"\b(for|while|do)\b")


def check_inline_constructors(path, text):
    """Constructors with a loop in a header: inline, rejected by Turbo C++."""
    findings = []
    if not path.endswith(".h"):
        return findings
    for name in set(re.findall(r"\b(?:class|struct)\s+(\w+)", text)):
        # only definitions out of the class body (`inline A::A(...) {`): a
        # constructor with a loop defined in the class body compiles
        head = re.compile(r"\b" + name + r"::" + name + r"\s*\(")
        for match in head.finditer(text):
            # skip declarations: the body starts after the parameters and
            # an optional initializer list
            depth = 0
            i = match.end() - 1
            while i < len(text):
                if text[i] == "(":
                    depth += 1
                elif text[i] == ")":
                    depth -= 1
                    if depth == 0:
                        break
                i += 1
            rest = re.match(r"[^;{]*\{", text[i + 1:])
            if not rest or text[match.start():match.end()].lstrip(" \t\n:;{}").startswith("~"):
                continue
            start = i + 1 + rest.end()
            depth = 1
            j = start
            while j < len(text) and depth:
                depth += {"{": 1, "}": -1}.get(text[j], 0)
                j += 1
            if LOOP_RE.search(text[start:j]):
                line = text.count("\n", 0, match.start()) + 1
                findings.append(("%s:%d" % (path, line), "inline-ctor",
                                 "loop in the inline constructor of %s" % name))
    return findings


def default_files():
    port = Path(__file__).resolve().parent.parent
    listed = subprocess.run(["git", "ls-files", *DEFAULT_ROOTS], cwd=port, capture_output=True, text=True,
                            check=True).stdout.split()
    return [str(port / name) for name in listed
            if name.endswith((".cpp", ".h", ".txx")) and not any(part in name for part in EXCLUDED)]


def main():
    files = sys.argv[1:] or default_files()
    findings = []
    for name in files:
        findings += check(name)
    for where, rule, message in findings:
        print("%s: [%s] %s" % (where, rule, message))
    print("%d findings in %d files" % (len(findings), len(files)), file=sys.stderr)
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
