#ifndef __POLYHEDRAL_EXAMPLE_COMMAND_LINE_OPTIONS__
#define __POLYHEDRAL_EXAMPLE_COMMAND_LINE_OPTIONS__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/material/ShadingType.h"
#include "models/CsgSampleNames.h"
#include "models/SolidModelNames.h"

/**
C++ port of Java's `options.CommandLineOptions`. The nullable Java fields
(`Boolean`, `Integer`, `Double` and the enums) are a value plus a `hasX`
flag; the getters return null when the option was not given.

@throws java::IllegalArgumentException from `parse` on invalid options
*/
class CommandLineOptions {
public:
    CommandLineOptions();

    bool isOffline() const;
    bool isMotifSweep() const;
    const java::String& getOutputPath() const;
    const SolidModelNames* getSolidModelName() const;
    const CsgSampleNames* getCsgSampleName() const;
    const bool* getDrawPoints() const;
    const bool* getDrawWires() const;
    const bool* getDrawSurfaces() const;
    const ShadingType* getShadingType() const;
    const int* getKurlanderBowlMotifIndex() const;
    const int* getFaceId() const;
    const double* getVertexNormalSmoothingThresholdDegrees() const;
    const int* getEdgeIndex() const;
    /// @return null when not given
    const java::String* getAppelDumpPath() const;

    static CommandLineOptions parse(const java::ArrayList<java::String>& args);

private:
    bool offline;
    bool motifSweep;
    java::String outputPath;
    bool hasSolidModelName;
    SolidModelNames solidModelName;
    bool hasCsgSampleName;
    CsgSampleNames csgSampleName;
    bool hasDrawPoints;
    bool drawPoints;
    bool hasDrawWires;
    bool drawWires;
    bool hasDrawSurfaces;
    bool drawSurfaces;
    bool hasShadingType;
    ShadingType shadingType;
    bool hasKurlanderBowlMotifIndex;
    int kurlanderBowlMotifIndex;
    bool hasFaceId;
    int faceId;
    bool hasEdgeIndex;
    int edgeIndex;
    bool hasAppelDumpPath;
    java::String appelDumpPath;
    bool hasVertexNormalSmoothingThresholdDegrees;
    double vertexNormalSmoothingThresholdDegrees;

    static void applySystemProperties(CommandLineOptions& options);
    static int parseMotifIndex(const java::String& rawValue);
    static int parseFaceId(const java::String& rawValue);
    static int parseEdgeIndex(const java::String& rawValue);
    static double parseNormalThresholdDegrees(const java::String& rawValue);
    static SolidModelNames parseSolidModel(const java::String& rawValue);
    static CsgSampleNames parseCsgSample(const java::String& rawValue);
    static bool parseBooleanFlag(const java::String& optionName,
                                 const java::String& rawValue);
    static ShadingType parseShadingType(const java::String& rawValue);
};

#endif
