#include "options/CommandLineOptions.h"

#include <cctype>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <climits>

#include "model/PolygonClippingFixtures.h"

CommandLineOptions::CommandLineOptions()
    : offlineMode(false)
    , hasFixtureIndex(false)
    , fixtureIndex(0)
    , hasWires(false)
    , wires(true)
    , hasSurfaces(false)
    , surfaces(true)
    , hasPoints(false)
    , points(true)
    , hasTessellationMode(false)
    , tessellationMode(PolygonSurfaceTessellationMode::GLU)
{}

static bool strEqI(const char* a, const char* b)
{
    while (*a && *b) {
        if (std::tolower((unsigned char)*a) != std::tolower((unsigned char)*b)) return false;
        a++; b++;
    }
    return *a == '\0' && *b == '\0';
}

static bool parseBoolean(const char* raw, bool& out)
{
    if (strEqI(raw, "true") || strEqI(raw, "on") || strEqI(raw, "1") || strEqI(raw, "yes")) {
        out = true; return true;
    }
    if (strEqI(raw, "false") || strEqI(raw, "off") || strEqI(raw, "0") || strEqI(raw, "no")) {
        out = false; return true;
    }
    std::fprintf(stderr, "[PolygonClippingExample] Invalid boolean value: %s\n", raw);
    return false;
}

static bool looksNumeric(const char* raw)
{
    if (!raw || !*raw) return false;
    const char* p = raw;
    if (*p == '-') p++;
    if (!*p) return false;
    while (*p) {
        if (!std::isdigit((unsigned char)*p)) return false;
        p++;
    }
    return true;
}

static bool parseInt(const char* raw, int& out)
{
    if (!looksNumeric(raw)) {
        std::fprintf(stderr, "[PolygonClippingExample] Invalid integer: %s\n", raw);
        return false;
    }
    errno = 0;
    char* end = nullptr;
    long v = std::strtol(raw, &end, 10);
    if (errno != 0 || end == raw || *end != '\0' || v < INT_MIN || v > INT_MAX) {
        std::fprintf(stderr, "[PolygonClippingExample] Integer out of range: %s\n", raw);
        return false;
    }
    out = (int)v;
    return true;
}

static bool parseTessellationMode(const char* raw, PolygonSurfaceTessellationMode& out)
{
    if (strEqI(raw, "GLU")) {
        out = PolygonSurfaceTessellationMode::GLU;
        return true;
    }

    const char* normalized = raw;
    if (strEqI(raw, "MONOTONE_DECOMPOSITION")
        || strEqI(raw, "MONOTONE-DECOMPOSITION")
        || strEqI(raw, "Monotone decomposition")
        || strEqI(raw, "monotone_decomposition")) {
        out = PolygonSurfaceTessellationMode::MONOTONE_DECOMPOSITION;
        return true;
    }

    std::fprintf(stderr,
        "[PolygonClippingExample] Invalid tessellation mode: %s"
        " (expected GLU or MONOTONE_DECOMPOSITION)\n", raw);
    return false;
}

static int findFixtureIndexByName(const char* name)
{
    for (int i = 0; i < PolygonClippingFixtures::COUNT; i++) {
        if (strEqI(PolygonClippingFixtures::CASES[i].name, name)) return i;
    }
    return -1;
}

static bool applyFixtureByValue(const char* raw, bool oneBased, int& indexOut)
{
    if (looksNumeric(raw)) {
        int v = 0;
        if (!parseInt(raw, v)) return false;
        if (oneBased) v--;
        if (v < 0 || v >= PolygonClippingFixtures::COUNT) {
            std::fprintf(stderr,
                "[PolygonClippingExample] Fixture index out of range: %s\n", raw);
            return false;
        }
        indexOut = v;
        return true;
    }
    int idx = findFixtureIndexByName(raw);
    if (idx < 0) {
        std::fprintf(stderr, "[PolygonClippingExample] Unknown fixture name: %s\n", raw);
        return false;
    }
    indexOut = idx;
    return true;
}

static const char* startsWith(const char* str, const char* prefix)
{
    while (*prefix) {
        if (*str != *prefix) return nullptr;
        str++; prefix++;
    }
    return str;
}

bool CommandLineOptions::parse(int argc, char** argv, CommandLineOptions& out)
{
    out = CommandLineOptions();

    for (int i = 1; i < argc; i++) {
        const char* arg = argv[i];
        const char* val = nullptr;

        if (std::strcmp(arg, "--offline") == 0 || std::strcmp(arg, "-offline") == 0) {
            out.offlineMode = true;
            continue;
        }

        if (std::strcmp(arg, "--help") == 0 || std::strcmp(arg, "-h") == 0) {
            printUsage();
            return false;
        }

        if (std::strcmp(arg, "--fixture") == 0 || std::strcmp(arg, "--fixture-index") == 0) {
            if (i + 1 >= argc) {
                std::fprintf(stderr, "[PolygonClippingExample] Missing value for %s\n", arg);
                return false;
            }
            val = argv[++i];
            bool zeroBased = (std::strcmp(arg, "--fixture-index") == 0);
            out.hasFixtureIndex = applyFixtureByValue(val, !zeroBased, out.fixtureIndex);
            if (!out.hasFixtureIndex) return false;
            continue;
        }

        if ((val = startsWith(arg, "--fixture=")) != nullptr) {
            out.hasFixtureIndex = applyFixtureByValue(val, true, out.fixtureIndex);
            if (!out.hasFixtureIndex) return false;
            continue;
        }

        if ((val = startsWith(arg, "--fixture-index=")) != nullptr) {
            out.hasFixtureIndex = applyFixtureByValue(val, false, out.fixtureIndex);
            if (!out.hasFixtureIndex) return false;
            continue;
        }

        if (std::strcmp(arg, "--wires") == 0) {
            if (i + 1 >= argc) {
                std::fprintf(stderr, "[PolygonClippingExample] Missing value for --wires\n");
                return false;
            }
            out.hasWires = parseBoolean(argv[++i], out.wires);
            if (!out.hasWires) return false;
            continue;
        }

        if ((val = startsWith(arg, "--wires=")) != nullptr) {
            out.hasWires = parseBoolean(val, out.wires);
            if (!out.hasWires) return false;
            continue;
        }

        if (std::strcmp(arg, "--surfaces") == 0) {
            if (i + 1 >= argc) {
                std::fprintf(stderr, "[PolygonClippingExample] Missing value for --surfaces\n");
                return false;
            }
            out.hasSurfaces = parseBoolean(argv[++i], out.surfaces);
            if (!out.hasSurfaces) return false;
            continue;
        }

        if ((val = startsWith(arg, "--surfaces=")) != nullptr) {
            out.hasSurfaces = parseBoolean(val, out.surfaces);
            if (!out.hasSurfaces) return false;
            continue;
        }

        if (std::strcmp(arg, "--points") == 0) {
            if (i + 1 >= argc) {
                std::fprintf(stderr, "[PolygonClippingExample] Missing value for --points\n");
                return false;
            }
            out.hasPoints = parseBoolean(argv[++i], out.points);
            if (!out.hasPoints) return false;
            continue;
        }

        if ((val = startsWith(arg, "--points=")) != nullptr) {
            out.hasPoints = parseBoolean(val, out.points);
            if (!out.hasPoints) return false;
            continue;
        }

        if (std::strcmp(arg, "--tessellation-mode") == 0
            || std::strcmp(arg, "--polygon-surface-tessellation-mode") == 0) {
            if (i + 1 >= argc) {
                std::fprintf(stderr, "[PolygonClippingExample] Missing value for %s\n", arg);
                return false;
            }
            out.hasTessellationMode = parseTessellationMode(argv[++i], out.tessellationMode);
            if (!out.hasTessellationMode) return false;
            continue;
        }

        if ((val = startsWith(arg, "--tessellation-mode=")) != nullptr) {
            out.hasTessellationMode = parseTessellationMode(val, out.tessellationMode);
            if (!out.hasTessellationMode) return false;
            continue;
        }

        if ((val = startsWith(arg, "--polygon-surface-tessellation-mode=")) != nullptr) {
            out.hasTessellationMode = parseTessellationMode(val, out.tessellationMode);
            if (!out.hasTessellationMode) return false;
            continue;
        }

        std::fprintf(stderr, "[PolygonClippingExample] Unknown argument: %s\n", arg);
        return false;
    }

    return true;
}

void CommandLineOptions::printUsage()
{
    std::fprintf(stderr,
        "Usage: [--fixture <index|name>] [--fixture-index <0-based-index>]\n"
        "       [--wires <true|false|on|off>] [--surfaces <true|false|on|off>]\n"
        "       [--points <true|false|on|off>]\n"
        "       [--tessellation-mode <GLU|MONOTONE_DECOMPOSITION>]\n"
        "       [--offline]\n"
        "Alias: --polygon-surface-tessellation-mode <...>\n"
        "Examples:\n"
        "  --fixture 1 --wires on --surfaces off\n"
        "  --fixture TRIANGLE_VS_QUAD --surfaces true\n"
        "  --fixture-index 0 --wires false --surfaces true\n"
        "  --tessellation-mode MONOTONE_DECOMPOSITION\n"
        "  --tessellation-mode MONOTONE_DECOMPOSITION --wires on --surfaces on --offline\n"
        "  --tessellation-mode MONOTONE_DECOMPOSITION --points on --offline\n"
        "  --offline\n"
        "Available fixtures:\n");

    for (int i = 0; i < PolygonClippingFixtures::COUNT; i++) {
        std::fprintf(stderr, "  %d: %s\n", i, PolygonClippingFixtures::CASES[i].name);
    }
}
