#ifndef __PLYGN_CLPPNG_CMMND_LN_OPTNS__
#define __PLYGN_CLPPNG_CMMND_LN_OPTNS__

#include "vsdk/toolkit/common/VSDK.h"
#include "model/PolygonSurfaceTessellationMode.h"
struct CommandLineOptions {
    BOOLEAN offlineMode;

    BOOLEAN hasFixtureIndex;
    int  fixtureIndex;

    BOOLEAN hasWires;
    BOOLEAN wires;

    BOOLEAN hasSurfaces;
    BOOLEAN surfaces;

    BOOLEAN hasPoints;
    BOOLEAN points;

    BOOLEAN hasTessellationMode;
    PolygonSurfaceTessellationMode tessellationMode;

    CommandLineOptions();

    static BOOLEAN parse(int argc, char** argv, CommandLineOptions& out);
    static void printUsage();
};

#endif
