#!/usr/bin/env python3
"""
Checks that class attributes follow Vitral's naming rules in the Java, C++
and TypeScript ports (see doc/attributesGettersAndSettersNames.md):

  - camelCase, without '_' as prefix or suffix and without snake_case,
  - semantic names, not 1-2 letter abbreviations.

Problems reported per attribute:
  P prefix '_', S suffix '_', N snake_case, C 1-2 letters,
  M trivial getter getX() that returns an attribute not named x.

Usage:
  scripts/checkAttributeNames.py [--csv] [--all] [repositoryRoot]

By default only the violations that are not exempted are printed and the
exit code is 1 if there is any. --csv prints every finding with its
exemption reason; --all also scans the testsuites.

The scanner is heuristic (brace/parenthesis tracking over source text, no
real parser). It finds member declarations of classes, including several
names declared in one statement and TypeScript constructor parameter
properties.
"""
import os
import re
import sys

SKIP_DIRS = {"build", "dist", "node_modules", "build-cmake", ".gradle",
             "target", "_deps", "artifacts", ".angular", "cmake-build-debug"}

#- Exemptions (phase 0 decisions) ------------------------------------------

# E1: immutable value types whose components use the usual math notation
# and whose accessors are named after the component (x(), r(), re()...).
VALUE_TYPE_FILES = re.compile(
    r"/(Vector[234]D[df]?|Vector[234]D|ColorRgba?|RGBA?Pixel(HDR)?|Complex|"
    r"Quaternion[df]?|Vertex2D|Matrix[234]x[234][df]?|Matrix2x2|Matrix4x4|"
    r"MatrixNxM)\.(java|h|ts)$")

# E2: coordinates and words that are already semantic in any class.
# gl: the OpenGL context, named as in every OpenGL binding; to: pairs with
# from in edges.
SEMANTIC_SHORT_NAMES = {"x", "y", "z", "w", "u", "v", "id", "up", "gl", "to"}

# E3: emulation of third party APIs keeps their names.
THIRD_PARTY_PATHS = re.compile(
    r"/src/main/java/|/src/main/jackson/|/src/main/org/|/src/main/com/")

# E4: attributes that keep an established notation, by "File:attribute".
# Mantyla's records keep the names of [MANT1988] (see CLAUDE.md); plane
# coefficients and ray parameters keep the math notation of their API.
NOTATION_EXEMPTIONS = {
    # [MANT1988] ch. 15 records: sonvv{va, vb}, sonvf{v, f}, sector{he, wa,
    # wb, cl}, null edges {e}, intersection coefficients.
    "_PolyhedralBoundedSolidSetOperatorVertexVertex:va",
    "_PolyhedralBoundedSolidSetOperatorVertexVertex:vb",
    "_PolyhedralBoundedSolidSetOperatorVertexFace:v",
    "_PolyhedralBoundedSolidSetOperatorVertexFace:f",
    "_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex:he",
    "_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector:wa",
    "_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector:wb",
    "_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace:cl",
    "_PolyhedralBoundedSolidSplitter:cl",
    "_PolyhedralBoundedSolidSplitter:e",
    "_PolyhedralBoundedSolidSetOperatorNullEdge:e",
    "_PolyhedralBoundedSolidSetNonIntersectingClassifier:a",
    "_PolyhedralBoundedSolidSetNonIntersectingClassifier:b",
    "_PolyhedralBoundedSolidSetGeometricPredicateProcessor:a1",
    "_PolyhedralBoundedSolidSetGeometricPredicateProcessor:a2",
    "_PolyhedralBoundedSolidSetGeometricPredicateProcessor:b1",
    "_PolyhedralBoundedSolidSetGeometricPredicateProcessor:b2",
    # Plane equation a*x + b*y + c*z + d = 0.
    "InfinitePlane:a", "InfinitePlane:b", "InfinitePlane:c", "InfinitePlane:d",
    # Ray parameter: point = origin + t * direction (getT / withT API).
    "Ray:t", "Intersection:t",
    # Diffuse/specular weights Kd, Ks: also the columns of the material
    # files and the shader uniforms (cookKd, cookKs).
    "MicroFacetedMaterial:kd", "MicroFacetedMaterial:ks",
    "CookTorranceShader:kd", "CookTorranceShader:ks",
    # Jacobian (TS only): the owner keeps A, B, C for now (2026-09-23).
    "Jacobian:A", "Jacobian:B", "Jacobian:C",
    # SHA-1 state words H0..H4, as named by FIPS 180-4.
    "WebSocketProtocol:h",
    # MD2 file format records (texture coordinates s, t and their indexes).
    "Md2Mesh:s", "Md2Mesh:t", "Md2Mesh:st",
}


# E5: immutable records whose accessors take the component names (Java
# record style: epsilon(), status()), so C++ (epsilon_) and TS
# (epsilonValue) store the component under another name.
RECORD_EXEMPTIONS = {
    "PolyhedralBoundedSolidNumericPolicy:" + name for name in (
        "modelScale_", "epsilon_", "bigEpsilon_", "unitVectorTolerance_",
        "angleTolerance_", "coplanarDotTolerance_", "unitIntervalTolerance_")
} | {
    "_PolyhedralBoundedSolidFace:" + name for name in (
        "status_", "intersectedHalfedge_", "intersectedVertex_")
}


# Pending decisions: reported as warnings, not as violations, until the
# owner decides.
PENDING_DECISIONS = set()


def is_constant_name(name):
    """Constants may use UPPER_CASE and KEY_x style."""
    return re.fullmatch(r"[A-Z][A-Z0-9]*(_[A-Za-z0-9]+)*", name) is not None


def problems_of(name, constant):
    found = []
    if constant and is_constant_name(name):
        return found
    if name.startswith("_"):
        found.append("P")
    if name.endswith("_") and len(name) > 1:
        found.append("S")
    if "_" in name.strip("_"):
        found.append("N")
    if len(name.strip("_")) <= 2:
        found.append("C")
    return found


def exemption_of(path, class_file, name, problems):
    if THIRD_PARTY_PATHS.search(path):
        return "E3 third-party API emulation"
    if VALUE_TYPE_FILES.search(path):
        # The component accessors (x(), r()...) take the plain names, so C++
        # (x_) and TS (xv) need another name for the stored component.
        return "E1 value type"
    if problems == ["C"]:
        if name in SEMANTIC_SHORT_NAMES:
            return "E2 semantic short name"
    if class_file + ":" + name in NOTATION_EXEMPTIONS:
        return "E4 established notation"
    if class_file + ":" + name in RECORD_EXEMPTIONS:
        return "E5 immutable record"
    if class_file + ":" + name in PENDING_DECISIONS:
        return "PENDING decision"
    return ""


#- Accessor names (problem M) ---------------------------------------------

# Trivial getters `getX() { return y; }` must return the attribute x. These
# getters are views on purpose (raw buffers, references instead of copies,
# enum wrappers, aliases, the current state of a gizmo against its
# configured value) or belong to value types (E1).
ACCESSOR_EXEMPTIONS = {
    "ColorRgb", "ColorRgba", "MatrixNxM",
    "ArrayListOfBytes:getRawArray", "IndexedColorImageUncompressed:getRawImage",
    "RGBAImageCompressed:getRawImage", "RGBAImageCompressed:getRawImageDirectBuffer",
    "SimpleMaterial:getAmbientReference", "SimpleMaterial:getDiffuseReference",
    "SimpleMaterial:getSpecularReference", "SimpleMaterial:getEmissionReference",
    "SimpleMaterial:getTransmittanceReference",
    "ControlledRGBAImageHDRUncompressed:getMapTypeEnum",
    "ControlledRGBAImageHDRUncompressed:getInterpolationTypeEnum",
    "RayGizmo:getPosition", "RayGizmo:getDirection", "RayGizmo:getRotationAngleInRadians",
    "InfinitePlaneGizmo:getPlane", "InfinitePlaneGizmo:getPoint", "InfinitePlaneGizmo:getNormal",
    "WidgetDialog:getChildren", "ZBuffer:getZBuffer", "ImagePersistenceTarga:getTexture",
    "StopWatch:getElapsedRealTime",
    # Testsuites: interface accessors, derived values and legacy servlet code.
    "BodyTransformState:getEntity", "CameraState:getEntity", "LightTransformState:getEntity",
    "UndoQueue:getUndoCount", "CsgSampleNames:getDisplayIndex", "PolygonModel:getImageHeight",
    "triangulatePolygon2D:getImageHeight",
    "ServletSessionInformation:getMethod", "ServletSessionInformation:getId",
    "SketchCanvas:getImage",
}
GETTER_PATTERNS = {
    "java": re.compile(r"\b(?:public|protected)\s+[\w<>\[\],.? ]+\s+(get\w+)\s*\(\s*\)\s*\{\s*return\s+(?:this\.)?(\w+)\s*;\s*\}", re.S),
    "cpp": re.compile(r"\b\w[\w:<>*& ]*\s+(?:\w+::)?(get\w+)\s*\(\s*\)\s*(?:const\s*)?\{\s*return\s+(?:this->)?(\w+)\s*;\s*\}", re.S),
    "ts": re.compile(r"\bpublic\s+(get\w+)\s*\(\s*\)\s*(?::\s*[^{]+)?\{\s*return\s+this\.(\w+)\s*;\s*\}", re.S),
}


def accessor_mismatches(path, lang, class_file):
    text = open(path, encoding="utf-8", errors="replace").read()
    for m in GETTER_PATTERNS[lang].finditer(text):
        getter, attribute = m.group(1), m.group(2)
        expected = getter[3:4].lower() + getter[4:]
        if attribute == expected or attribute in ("null", "nullptr", "true", "false") or \
                not attribute[:1].islower() or class_file in ACCESSOR_EXEMPTIONS or \
                class_file + ":" + getter in ACCESSOR_EXEMPTIONS:
            continue
        yield text[:m.start()].count("\n") + 1, getter, attribute


#- Scanner -----------------------------------------------------------------

def strip_code(line, state):
    """Removes comments and string/char literals; state tracks /* */."""
    out = []
    i = 0
    n = len(line)
    while i < n:
        if state["block"]:
            j = line.find("*/", i)
            if j < 0:
                return "".join(out)
            state["block"] = False
            i = j + 2
            continue
        c = line[i]
        if line.startswith("/*", i):
            state["block"] = True
            i += 2
            continue
        if line.startswith("//", i):
            break
        if c in "\"'`":
            q = c
            i += 1
            while i < n and line[i] != q:
                i += 2 if line[i] == "\\" else 1
            i += 1
            out.append(q + q)
            continue
        out.append(c)
        i += 1
    return "".join(out)


def split_top_level(text, separator=","):
    parts = []
    depth = 0
    current = ""
    for ch in text:
        if ch in "<([{":
            depth += 1
        elif ch in ">)]}":
            depth -= 1
        if ch == separator and depth == 0:
            parts.append(current)
            current = ""
        else:
            current += ch
    parts.append(current)
    return parts


JAVA_MODIFIERS = r"(?:(?:public|private|protected|static|final|transient|volatile)\s+)*"
CPP_MODIFIERS = r"(?:(?:static|const|mutable|constexpr|volatile|unsigned|signed|long|short|inline)\s+)*"
TS_MODIFIERS = r"(?:(?:public|private|protected|static|readonly|declare|override|abstract)\s+)*"
KEYWORDS = r"\b(class|interface|enum|record|struct|return|typedef|using|friend|operator|template|namespace|throw|new|case|default|goto|delete)\b"


def is_constant_statement(statement, lang):
    """True for final (Java), const/constexpr (C++) and readonly (TS)
    declarations, the ones that may hold constants."""
    head = statement.split("=")[0]
    if lang == "java":
        return re.search(r"\b(final|static)\b", head) is not None
    if lang == "cpp":
        return re.search(r"\b(constexpr|const)\b", head) is not None
    return re.search(r"\b(readonly|static)\b", head) is not None


def member_names(statement, lang):
    """Names declared by one member statement (text up to ';')."""
    statement = statement.strip()
    if not statement or re.search(KEYWORDS, statement.split("=")[0]):
        return []
    if lang == "ts":
        m = re.match(TS_MODIFIERS + r"#?([A-Za-z_$][\w$]*)\s*[?!]?\s*(:|=|$)", statement)
        return [m.group(1)] if m else []
    head = split_top_level(statement)
    first = head[0].split("=")[0]
    if "(" in first:
        return []
    modifiers = JAVA_MODIFIERS if lang == "java" else CPP_MODIFIERS
    m = re.match(modifiers + r"[A-Za-z_][\w:.<>\[\], ?*&]*?[\s*&]+(?:const\s+)?([A-Za-z_]\w*)\s*(\[[^\]]*\]\s*)*$", first.strip())
    if not m:
        return []
    names = [m.group(1)]
    for part in head[1:]:
        extra = re.match(r"\s*[*&]*\s*([A-Za-z_]\w*)\s*(\[[^\]]*\]\s*)*$", part.split("=")[0])
        if extra:
            names.append(extra.group(1))
    return names


def scan(path, lang):
    state = {"block": False}
    stack = []
    pending = ""
    statement = ""
    statement_line = 0
    parens = 0
    in_constructor = False
    for number, raw in enumerate(open(path, encoding="utf-8", errors="replace"), 1):
        line = strip_code(raw.rstrip("\n"), state)
        for ch in line:
            member_level = parens == 0 and len(stack) > 0 and \
                all(kind in ("class", "ns", "interface") for kind in stack) and \
                stack[-1] in ("class", "interface")
            if ch == "(":
                parens += 1
            elif ch == ")":
                parens = max(0, parens - 1)
            if ch == "{" and parens == 0:
                context = pending
                tail = context.split("class")[-1] if "class" in context else context
                if lang == "java" and re.search(r"\binterface\b", context):
                    # Java interface fields are implicit constants
                    stack.append("interface")
                elif re.search(r"\b(class|interface|record|struct)\b", context) and \
                        not re.search(r"\bnew\b|=>|\(", tail):
                    stack.append("class")
                elif re.search(r"\benum\b", context):
                    stack.append("enum")
                elif lang == "cpp" and re.search(r"\bnamespace\b|extern\s+\"C", context):
                    stack.append("ns")
                else:
                    stack.append("code")
                pending = ""
                statement = ""
            elif ch == "}" and parens == 0:
                if stack:
                    stack.pop()
                pending = ""
                statement = ""
            elif ch == ";" and parens == 0:
                if member_level:
                    constant = stack[-1] == "interface" or \
                        is_constant_statement(statement, lang)
                    for name in member_names(statement, lang):
                        yield statement_line, name, constant
                pending = ""
                statement = ""
            else:
                if not statement.strip():
                    statement_line = number
                pending += ch
                statement += ch
        # TS members may end without ';' at the end of the line
        if lang == "ts" and parens == 0 and stack and stack[-1] == "class" and \
                statement.strip() and re.match(TS_MODIFIERS + r"#?[A-Za-z_$][\w$]*\s*[?!]?\s*:[^=(]*$", statement.strip()):
            constant = is_constant_statement(statement, lang)
            for name in member_names(statement, lang):
                yield statement_line, name, constant
            statement = ""
        pending += " "
        statement += " "
        # TS parameter properties, also in constructors written over lines
        if lang == "ts" and ("constructor" in line or in_constructor):
            for m in re.finditer(r"\b(?:private|protected|public)\s+(?:readonly\s+)?([A-Za-z_$][\w$]*)\s*[?]?[:=]", line):
                yield number, m.group(1), "readonly" in line
            in_constructor = parens > 0


PORTS = [
    ("java", "java", {".java"}),
    ("cpp", "cpp", {".h", ".hpp", ".cpp"}),
    ("ts", "typescript", {".ts"}),
]


def main(argv):
    csv_mode = "--csv" in argv
    include_testsuites = "--all" in argv
    args = [a for a in argv[1:] if not a.startswith("--")]
    root = args[0] if args else os.path.join(os.path.dirname(__file__), "..")
    violations = 0
    if csv_mode:
        print("port,file,line,attribute,problems,exemption")
    for port, folder, extensions in PORTS:
        base = os.path.join(root, folder)
        for dirpath, dirnames, filenames in os.walk(base):
            dirnames[:] = sorted(d for d in dirnames if d not in SKIP_DIRS)
            for filename in sorted(filenames):
                path = os.path.join(dirpath, filename)
                relative = os.path.relpath(path, root)
                if os.path.splitext(filename)[1] not in extensions or filename.endswith(".d.ts"):
                    continue
                if not include_testsuites and "/testsuite/" in "/" + relative:
                    continue
                class_file = os.path.splitext(filename)[0]
                if not THIRD_PARTY_PATHS.search("/" + relative):
                    for line, getter, attribute in accessor_mismatches(path, port, class_file):
                        violations += 1
                        if csv_mode:
                            print(f"{port},{relative},{line},{getter},M,")
                        else:
                            print(f"{relative}:{line}: {getter}() returns {attribute} (M)")
                seen = set()
                for line, name, constant in scan(path, port):
                    if (line, name) in seen:
                        continue
                    seen.add((line, name))
                    problems = problems_of(name, constant)
                    if not problems:
                        continue
                    exemption = exemption_of("/" + relative, class_file, name, problems)
                    if csv_mode:
                        print(f"{port},{relative},{line},{name},{''.join(problems)},{exemption}")
                    elif not exemption:
                        print(f"{relative}:{line}: {name} ({''.join(problems)})")
                    elif exemption.startswith("PENDING"):
                        print(f"warning: {relative}:{line}: {name} ({exemption})", file=sys.stderr)
                    if not exemption:
                        violations += 1
    if not csv_mode:
        print(f"{violations} attribute name violations", file=sys.stderr)
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
