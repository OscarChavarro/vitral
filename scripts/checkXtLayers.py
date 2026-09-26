#!/usr/bin/env python3
"""
Checks the layers of the Xt GUIs of the C++ port: the Xt layer uses the Xt
Intrinsics only, and the widget sets over it (Athena and Motif) never use
each other, so each executable can be built with exactly one of them.

  - cpp/xt and the Xt code of the applications (all but their gui/xaw and
    gui/xm folders) use neither Athena (Xaw) nor Motif (Xm),
  - cpp/xaw and gui/xaw folders do not use Motif,
  - cpp/xm and gui/xm folders do not use Athena.

A source uses a widget set when it includes its headers or names its
functions, types, widget classes or resources (i.e. `XawListChange`,
`XmString`, `commandWidgetClass`, `XtNmenuName`). Comments and string
literals are not checked, so the documentation can name the implementations.

Usage:
  scripts/checkXtLayers.py [repositoryRoot]

Prints each violation as file:line and exits with 1 if there is any.
"""
import os
import re
import sys

SKIP_DIRS = {"build", "dist", "node_modules", ".gradle", "target", "_deps"}
SOURCE_SUFFIXES = (".h", ".hpp", ".cpp", ".cc", ".txx")

# Applications whose GUI is built over the Xt layer
XT_APPLICATIONS = ["cpp/testsuite/ApplicationCases/SceneEditorApplication/src"]

ATHENA_WIDGET_CLASSES = (
    "commandWidgetClass", "labelWidgetClass", "simpleMenuWidgetClass",
    "smeBSBObjectClass", "smeLineObjectClass", "smeObjectClass",
    "menuButtonWidgetClass", "asciiTextWidgetClass", "listWidgetClass",
    "viewportWidgetClass", "formWidgetClass", "boxWidgetClass",
    "panedWidgetClass", "toggleWidgetClass", "dialogWidgetClass",
    "scrollbarWidgetClass")
# Resources of Athena widgets (the Intrinsics ones, i.e. XtNwidth, are fine)
ATHENA_RESOURCES = (
    "international", "fontSet", "label", "justify", "bitmap", "leftBitmap",
    "leftMargin", "menuName", "popupOnEntry", "string", "editType", "type",
    "resize", "useStringInPlace", "shapeStyle", "allowVert", "forceBars",
    "defaultColumns", "forceColumns", "verticalList", "callback")

ATHENA = ("Athena", [
    re.compile(r"\bXaw\w*"),
    re.compile(r"\b(" + "|".join(ATHENA_WIDGET_CLASSES) + r")\b"),
    re.compile(r"\bXtN(" + "|".join(ATHENA_RESOURCES) + r")\b"),
], re.compile(r"X11/Xaw\d*/"))

MOTIF = ("Motif", [
    re.compile(r"\b_?Xm[A-Z_]\w*"),
    re.compile(r"\bxm\w*(WidgetClass|GadgetClass)\b"),
], re.compile(r"[<\"]Xm/"))

INCLUDE = re.compile(r"^\s*#\s*include\s*([<\"][^>\"]*[>\"])")


def stripCommentsAndStrings(text):
    """
    Replaces comments and string / character literals with spaces, keeping
    the line breaks, so line numbers stay.
    """
    result = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if text.startswith("//", i):
            end = text.find("\n", i)
            end = n if end < 0 else end
            result.append(" " * (end - i))
            i = end
        elif text.startswith("/*", i):
            end = text.find("*/", i + 2)
            end = n if end < 0 else end + 2
            result.append(re.sub(r"[^\n]", " ", text[i:end]))
            i = end
        elif c == '"' or c == "'":
            j = i + 1
            while j < n and text[j] != c and text[j] != "\n":
                j += 2 if text[j] == "\\" else 1
            end = min(j + 1, n)
            result.append(c + " " * (end - i - 2) + (c if end - i >= 2 else ""))
            i = end
        else:
            result.append(c)
            i += 1
    return "".join(result)


def sources(root, folder, excludedFolders=()):
    base = os.path.join(root, folder)
    for path, dirs, files in os.walk(base):
        dirs[:] = sorted(d for d in dirs
                         if d not in SKIP_DIRS and not d.startswith("build-")
                         and os.path.relpath(os.path.join(path, d), root)
                         not in excludedFolders)
        for name in sorted(files):
            if name.endswith(SOURCE_SUFFIXES):
                yield os.path.join(path, name)


def check(root, path, forbidden):
    violations = []
    with open(path, encoding="utf-8", errors="replace") as source:
        text = source.read()
    lines = text.split("\n")
    code = stripCommentsAndStrings(text).split("\n")
    relative = os.path.relpath(path, root)
    for number, line in enumerate(lines, 1):
        include = INCLUDE.match(line)
        for name, patterns, header in forbidden:
            if include:
                if header.search(include.group(1)):
                    violations.append("%s:%d: includes %s header %s" %
                                      (relative, number, name,
                                       include.group(1)))
                continue
            for pattern in patterns:
                match = pattern.search(code[number - 1])
                if match:
                    violations.append("%s:%d: uses %s (%s)" %
                                      (relative, number, name,
                                       match.group(0)))
                    break
    return violations


def main():
    root = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else
                           os.path.join(os.path.dirname(__file__), ".."))
    layers = []
    xtFolders = ["cpp/xt/src"] + XT_APPLICATIONS
    widgetSetFolders = tuple(os.path.join(application, "gui", widgetSet)
                             for application in XT_APPLICATIONS
                             for widgetSet in ("xaw", "xm"))
    for folder in xtFolders:
        layers.append((folder, widgetSetFolders, [ATHENA, MOTIF]))
    layers.append(("cpp/xaw/src", (), [MOTIF]))
    layers.append(("cpp/xm/src", (), [ATHENA]))
    for application in XT_APPLICATIONS:
        layers.append((os.path.join(application, "gui", "xaw"), (), [MOTIF]))
        layers.append((os.path.join(application, "gui", "xm"), (), [ATHENA]))

    violations = []
    for folder, excluded, forbidden in layers:
        for path in sources(root, folder, excluded):
            violations.extend(check(root, path, forbidden))
    for violation in violations:
        print(violation)
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
