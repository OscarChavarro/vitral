#ifndef __COMMAND_LINE_OPTIONS__
#define __COMMAND_LINE_OPTIONS__

#include "model/PolygonSurfaceTessellationMode.h"
struct CommandLineOptions {
    bool offlineMode;

    bool hasFixtureIndex;
    int  fixtureIndex;

    bool hasWires;
    bool wires;

    bool hasSurfaces;
    bool surfaces;

    bool hasPoints;
    bool points;

    bool hasTessellationMode;
    PolygonSurfaceTessellationMode tessellationMode;

    CommandLineOptions();

    static bool parse(int argc, char** argv, CommandLineOptions& out);
    static void printUsage();
};

#endif
