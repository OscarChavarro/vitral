#include <ctype.h>
#include <errno.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "model/PolygonClippingFixtures.h"
#include "options/CommandLineOptions.h"
CommandLineOptions::CommandLineOptions()
    : offlineMode(FALSE)
    , hasFixtureIndex(FALSE)
    , fixtureIndex(0)
    , hasWires(FALSE)
    , wires(TRUE)
    , hasSurfaces(FALSE)
    , surfaces(TRUE)
    , hasPoints(FALSE)
    , points(TRUE)
    , hasTessellationMode(FALSE)
    , tessellationMode(PolygonSurfaceTessellationMode::GLU)
{}

static BOOLEAN strEqI(const char* a, const char* b)
{
    while (*a && *b) {
        if (tolower((unsigned char)*a) != tolower((unsigned char)*b)) return FALSE;
        a++; b++;
    }
    return *a == '\0' && *b == '\0';
}

static BOOLEAN parseBoolean(const char* raw, BOOLEAN& out)
{
    if (strEqI(raw, "true") || strEqI(raw, "on") || strEqI(raw, "1") || strEqI(raw, "yes")) {
        out = TRUE; return TRUE;
    }
    if (strEqI(raw, "false") || strEqI(raw, "off") || strEqI(raw, "0") || strEqI(raw, "no")) {
        out = FALSE; return TRUE;
    }
    fprintf(stderr, "[PolygonClippingExample] Invalid boolean value: %s\n", raw);
    return FALSE;
}

static BOOLEAN looksNumeric(const char* raw)
{
    if (!raw || !*raw) return FALSE;
    const char* p = raw;
    if (*p == '-') p++;
    if (!*p) return FALSE;
    while (*p) {
        if (!isdigit((unsigned char)*p)) return FALSE;
        p++;
    }
    return TRUE;
}

static BOOLEAN parseInt(const char* raw, int& out)
{
    if (!looksNumeric(raw)) {
        fprintf(stderr, "[PolygonClippingExample] Invalid integer: %s\n", raw);
        return FALSE;
    }
    errno = 0;
    char* end = NULL;
    long v = strtol(raw, &end, 10);
    if (errno != 0 || end == raw || *end != '\0' || v < INT_MIN || v > INT_MAX) {
        fprintf(stderr, "[PolygonClippingExample] Integer out of range: %s\n", raw);
        return FALSE;
    }
    out = (int)v;
    return TRUE;
}

static BOOLEAN parseTessellationMode(const char* raw, PolygonSurfaceTessellationMode& out)
{
    if (strEqI(raw, "GLU")) {
        out = PolygonSurfaceTessellationMode::GLU;
        return TRUE;
    }

    if (strEqI(raw, "MONOTONE_DECOMPOSITION")
        || strEqI(raw, "MONOTONE-DECOMPOSITION")
        || strEqI(raw, "Monotone decomposition")
        || strEqI(raw, "monotone_decomposition")) {
        out = PolygonSurfaceTessellationMode::MONOTONE_DECOMPOSITION;
        return TRUE;
    }

    fprintf(stderr,
        "[PolygonClippingExample] Invalid tessellation mode: %s"
        " (expected GLU or MONOTONE_DECOMPOSITION)\n", raw);
    return FALSE;
}

static int findFixtureIndexByName(const char* name)
{
    for (int i = 0; i < PolygonClippingFixtures::COUNT; i++) {
        if (strEqI(PolygonClippingFixtures::CASES[i].name, name)) return i;
    }
    return -1;
}

static BOOLEAN applyFixtureByValue(const char* raw, BOOLEAN oneBased, int& indexOut)
{
    if (looksNumeric(raw)) {
        int v = 0;
        if (!parseInt(raw, v)) return FALSE;
        if (oneBased) v--;
        if (v < 0 || v >= PolygonClippingFixtures::COUNT) {
            fprintf(stderr,
                "[PolygonClippingExample] Fixture index out of range: %s\n", raw);
            return FALSE;
        }
        indexOut = v;
        return TRUE;
    }
    int idx = findFixtureIndexByName(raw);
    if (idx < 0) {
        fprintf(stderr, "[PolygonClippingExample] Unknown fixture name: %s\n", raw);
        return FALSE;
    }
    indexOut = idx;
    return TRUE;
}

static const char* startsWith(const char* str, const char* prefix)
{
    while (*prefix) {
        if (*str != *prefix) return NULL;
        str++; prefix++;
    }
    return str;
}

BOOLEAN CommandLineOptions::parse(int argc, char** argv, CommandLineOptions& out)
{
    out = CommandLineOptions();

    for (int i = 1; i < argc; i++) {
        const char* arg = argv[i];
        const char* val = NULL;

        if (strcmp(arg, "--offline") == 0 || strcmp(arg, "-offline") == 0) {
            out.offlineMode = TRUE;
            continue;
        }

        if (strcmp(arg, "--help") == 0 || strcmp(arg, "-h") == 0) {
            printUsage();
            return FALSE;
        }

        if (strcmp(arg, "--fixture") == 0 || strcmp(arg, "--fixture-index") == 0) {
            if (i + 1 >= argc) {
                fprintf(stderr, "[PolygonClippingExample] Missing value for %s\n", arg);
                return FALSE;
            }
            val = argv[++i];
            BOOLEAN zeroBased = (strcmp(arg, "--fixture-index") == 0);
            out.hasFixtureIndex = applyFixtureByValue(val, !zeroBased, out.fixtureIndex);
            if (!out.hasFixtureIndex) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--fixture=")) != NULL) {
            out.hasFixtureIndex = applyFixtureByValue(val, TRUE, out.fixtureIndex);
            if (!out.hasFixtureIndex) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--fixture-index=")) != NULL) {
            out.hasFixtureIndex = applyFixtureByValue(val, FALSE, out.fixtureIndex);
            if (!out.hasFixtureIndex) return FALSE;
            continue;
        }

        if (strcmp(arg, "--wires") == 0) {
            if (i + 1 >= argc) {
                fprintf(stderr, "[PolygonClippingExample] Missing value for --wires\n");
                return FALSE;
            }
            out.hasWires = parseBoolean(argv[++i], out.wires);
            if (!out.hasWires) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--wires=")) != NULL) {
            out.hasWires = parseBoolean(val, out.wires);
            if (!out.hasWires) return FALSE;
            continue;
        }

        if (strcmp(arg, "--surfaces") == 0) {
            if (i + 1 >= argc) {
                fprintf(stderr, "[PolygonClippingExample] Missing value for --surfaces\n");
                return FALSE;
            }
            out.hasSurfaces = parseBoolean(argv[++i], out.surfaces);
            if (!out.hasSurfaces) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--surfaces=")) != NULL) {
            out.hasSurfaces = parseBoolean(val, out.surfaces);
            if (!out.hasSurfaces) return FALSE;
            continue;
        }

        if (strcmp(arg, "--points") == 0) {
            if (i + 1 >= argc) {
                fprintf(stderr, "[PolygonClippingExample] Missing value for --points\n");
                return FALSE;
            }
            out.hasPoints = parseBoolean(argv[++i], out.points);
            if (!out.hasPoints) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--points=")) != NULL) {
            out.hasPoints = parseBoolean(val, out.points);
            if (!out.hasPoints) return FALSE;
            continue;
        }

        if (strcmp(arg, "--tessellation-mode") == 0
            || strcmp(arg, "--polygon-surface-tessellation-mode") == 0) {
            if (i + 1 >= argc) {
                fprintf(stderr, "[PolygonClippingExample] Missing value for %s\n", arg);
                return FALSE;
            }
            out.hasTessellationMode = parseTessellationMode(argv[++i], out.tessellationMode);
            if (!out.hasTessellationMode) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--tessellation-mode=")) != NULL) {
            out.hasTessellationMode = parseTessellationMode(val, out.tessellationMode);
            if (!out.hasTessellationMode) return FALSE;
            continue;
        }

        if ((val = startsWith(arg, "--polygon-surface-tessellation-mode=")) != NULL) {
            out.hasTessellationMode = parseTessellationMode(val, out.tessellationMode);
            if (!out.hasTessellationMode) return FALSE;
            continue;
        }

        fprintf(stderr, "[PolygonClippingExample] Unknown argument: %s\n", arg);
        return FALSE;
    }

    return TRUE;
}

void CommandLineOptions::printUsage()
{
    fprintf(stderr,
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
        fprintf(stderr, "  %d: %s\n", i, PolygonClippingFixtures::CASES[i].name);
    }
}
