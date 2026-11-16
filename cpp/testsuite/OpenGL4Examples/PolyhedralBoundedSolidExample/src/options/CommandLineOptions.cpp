#include "java/util/ArrayList.txx"
#include "options/CommandLineOptions.h"

#include "java/lang/Double.h"
#include "java/lang/IllegalArgumentException.h"
#include "java/lang/Integer.h"
#include "java/lang/NumberFormatException.h"
#include "java/lang/System.h"

/**
Java: `value == null || value.isBlank()`. C++ port note:
`java::System::getProperty` returns an empty text for a missing property.
*/
static bool isBlank(const java::String& value)
{
    return value.trim().isEmpty();
}

/// Java: `arg.substring(prefix.length())`
static java::String afterPrefix(const java::String& arg, const char* prefix)
{
    return arg.substring(java::String(prefix).length());
}

CommandLineOptions::CommandLineOptions()
    : offline(false),
      motifSweep(false),
      outputPath("output.png"),
      hasSolidModelName(false),
      solidModelName(SolidModelNames::HOLED_BOX),
      hasCsgSampleName(false),
      csgSampleName(CsgSampleNames::MANT1986_2),
      hasDrawPoints(false),
      drawPoints(false),
      hasDrawWires(false),
      drawWires(false),
      hasDrawSurfaces(false),
      drawSurfaces(false),
      hasShadingType(false),
      shadingType(SHADING_GOURAUD),
      hasKurlanderBowlMotifIndex(false),
      kurlanderBowlMotifIndex(0),
      hasFaceId(false),
      faceId(0),
      hasEdgeIndex(false),
      edgeIndex(0),
      hasAppelDumpPath(false),
      hasVertexNormalSmoothingThresholdDegrees(false),
      vertexNormalSmoothingThresholdDegrees(0.0)
{
}

bool CommandLineOptions::isOffline() const
{
    return offline;
}

bool CommandLineOptions::isMotifSweep() const
{
    return motifSweep;
}

const java::String& CommandLineOptions::getOutputPath() const
{
    return outputPath;
}

const SolidModelNames* CommandLineOptions::getSolidModelName() const
{
    return hasSolidModelName ? &solidModelName : nullptr;
}

const CsgSampleNames* CommandLineOptions::getCsgSampleName() const
{
    return hasCsgSampleName ? &csgSampleName : nullptr;
}

const bool* CommandLineOptions::getDrawPoints() const
{
    return hasDrawPoints ? &drawPoints : nullptr;
}

const bool* CommandLineOptions::getDrawWires() const
{
    return hasDrawWires ? &drawWires : nullptr;
}

const bool* CommandLineOptions::getDrawSurfaces() const
{
    return hasDrawSurfaces ? &drawSurfaces : nullptr;
}

const ShadingType* CommandLineOptions::getShadingType() const
{
    return hasShadingType ? &shadingType : nullptr;
}

const int* CommandLineOptions::getKurlanderBowlMotifIndex() const
{
    return hasKurlanderBowlMotifIndex ? &kurlanderBowlMotifIndex : nullptr;
}

const int* CommandLineOptions::getFaceId() const
{
    return hasFaceId ? &faceId : nullptr;
}

const double* CommandLineOptions::getVertexNormalSmoothingThresholdDegrees() const
{
    return hasVertexNormalSmoothingThresholdDegrees ?
        &vertexNormalSmoothingThresholdDegrees : nullptr;
}

const int* CommandLineOptions::getEdgeIndex() const
{
    return hasEdgeIndex ? &edgeIndex : nullptr;
}

const java::String* CommandLineOptions::getAppelDumpPath() const
{
    return hasAppelDumpPath ? &appelDumpPath : nullptr;
}

CommandLineOptions CommandLineOptions::parse(
    const java::ArrayList<java::String>& args)
{
    CommandLineOptions options;
    applySystemProperties(options);
    int i;
    int n = (int)args.size();

    for ( i = 0; i < n; i++ ) {
        java::String arg = args.get(i);
        if ( arg.equals("--offline") ) {
            options.offline = true;
        }
        else if ( arg.equals("--motifSweep") ) {
            options.motifSweep = true;
            options.offline = true;
        }
        else if ( arg.equals("--screenshot") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --screenshot");
            }
            options.outputPath = args.get(++i);
        }
        else if ( arg.startsWith("--screenshot=") ) {
            options.outputPath = afterPrefix(arg, "--screenshot=");
        }
        else if ( arg.equals("--solidModel") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --solidModel");
            }
            options.solidModelName = parseSolidModel(args.get(++i));
            options.hasSolidModelName = true;
        }
        else if ( arg.startsWith("--solidModel=") ) {
            options.solidModelName = parseSolidModel(
                afterPrefix(arg, "--solidModel="));
            options.hasSolidModelName = true;
        }
        else if ( arg.equals("--csgSample") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --csgSample");
            }
            options.csgSampleName = parseCsgSample(args.get(++i));
            options.hasCsgSampleName = true;
        }
        else if ( arg.startsWith("--csgSample=") ) {
            options.csgSampleName = parseCsgSample(
                afterPrefix(arg, "--csgSample="));
            options.hasCsgSampleName = true;
        }
        else if ( arg.equals("--points") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException("Missing value for --points");
            }
            options.drawPoints = parseBooleanFlag("--points", args.get(++i));
            options.hasDrawPoints = true;
        }
        else if ( arg.startsWith("--points=") ) {
            options.drawPoints = parseBooleanFlag("--points",
                afterPrefix(arg, "--points="));
            options.hasDrawPoints = true;
        }
        else if ( arg.equals("--wires") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException("Missing value for --wires");
            }
            options.drawWires = parseBooleanFlag("--wires", args.get(++i));
            options.hasDrawWires = true;
        }
        else if ( arg.startsWith("--wires=") ) {
            options.drawWires = parseBooleanFlag("--wires",
                afterPrefix(arg, "--wires="));
            options.hasDrawWires = true;
        }
        else if ( arg.equals("--surfaces") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException("Missing value for --surfaces");
            }
            options.drawSurfaces = parseBooleanFlag("--surfaces", args.get(++i));
            options.hasDrawSurfaces = true;
        }
        else if ( arg.startsWith("--surfaces=") ) {
            options.drawSurfaces = parseBooleanFlag("--surfaces",
                afterPrefix(arg, "--surfaces="));
            options.hasDrawSurfaces = true;
        }
        else if ( arg.equals("--shading") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException("Missing value for --shading");
            }
            options.shadingType = parseShadingType(args.get(++i));
            options.hasShadingType = true;
        }
        else if ( arg.startsWith("--shading=") ) {
            options.shadingType = parseShadingType(
                afterPrefix(arg, "--shading="));
            options.hasShadingType = true;
        }
        else if ( arg.equals("--motifIndex") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --motifIndex");
            }
            options.kurlanderBowlMotifIndex = parseMotifIndex(args.get(++i));
            options.hasKurlanderBowlMotifIndex = true;
        }
        else if ( arg.startsWith("--motifIndex=") ) {
            options.kurlanderBowlMotifIndex = parseMotifIndex(
                afterPrefix(arg, "--motifIndex="));
            options.hasKurlanderBowlMotifIndex = true;
        }
        else if ( arg.equals("--faceId") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --faceId");
            }
            options.faceId = parseFaceId(args.get(++i));
            options.hasFaceId = true;
        }
        else if ( arg.startsWith("--faceId=") ) {
            options.faceId = parseFaceId(afterPrefix(arg, "--faceId="));
            options.hasFaceId = true;
        }
        else if ( arg.equals("--edgeIndex") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --edgeIndex");
            }
            options.edgeIndex = parseEdgeIndex(args.get(++i));
            options.hasEdgeIndex = true;
        }
        else if ( arg.startsWith("--edgeIndex=") ) {
            options.edgeIndex = parseEdgeIndex(afterPrefix(arg, "--edgeIndex="));
            options.hasEdgeIndex = true;
        }
        else if ( arg.equals("--appelDump") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --appelDump");
            }
            options.appelDumpPath = args.get(++i);
            options.hasAppelDumpPath = true;
        }
        else if ( arg.startsWith("--appelDump=") ) {
            options.appelDumpPath = afterPrefix(arg, "--appelDump=");
            options.hasAppelDumpPath = true;
        }
        else if ( arg.equals("--normalThresholdDeg") ) {
            if ( i + 1 >= n ) {
                throw java::IllegalArgumentException(
                    "Missing value for --normalThresholdDeg");
            }
            options.vertexNormalSmoothingThresholdDegrees =
                parseNormalThresholdDegrees(args.get(++i));
            options.hasVertexNormalSmoothingThresholdDegrees = true;
        }
        else if ( arg.startsWith("--normalThresholdDeg=") ) {
            options.vertexNormalSmoothingThresholdDegrees =
                parseNormalThresholdDegrees(
                    afterPrefix(arg, "--normalThresholdDeg="));
            options.hasVertexNormalSmoothingThresholdDegrees = true;
        }
        else {
            throw java::IllegalArgumentException(
                java::String("Unknown option: ") + arg);
        }
    }

    if ( isBlank(options.outputPath) ) {
        options.outputPath = "output.png";
    }

    return options;
}

void CommandLineOptions::applySystemProperties(CommandLineOptions& options)
{
    java::String offlineProperty = java::System::getProperty("poly.offline");
    java::String outputProperty = java::System::getProperty("poly.output");
    java::String thresholdProperty =
        java::System::getProperty("poly.normalThresholdDeg");
    java::String edgeIndexProperty = java::System::getProperty("poly.edgeIndex");
    java::String appelDumpProperty = java::System::getProperty("poly.appelDump");

    if ( !offlineProperty.isEmpty() ) {
        // Java: `Boolean.parseBoolean`
        options.offline = offlineProperty.equalsIgnoreCase("true");
    }
    if ( !isBlank(outputProperty) ) {
        options.outputPath = outputProperty;
    }
    if ( !isBlank(thresholdProperty) ) {
        options.vertexNormalSmoothingThresholdDegrees =
            parseNormalThresholdDegrees(thresholdProperty);
        options.hasVertexNormalSmoothingThresholdDegrees = true;
    }
    if ( !isBlank(edgeIndexProperty) ) {
        options.edgeIndex = parseEdgeIndex(edgeIndexProperty);
        options.hasEdgeIndex = true;
    }
    if ( !isBlank(appelDumpProperty) ) {
        options.appelDumpPath = appelDumpProperty;
        options.hasAppelDumpPath = true;
    }

    java::String solidModelProperty = java::System::getProperty("poly.solidModel");
    if ( !isBlank(solidModelProperty) ) {
        options.solidModelName = parseSolidModel(solidModelProperty);
        options.hasSolidModelName = true;
    }

    java::String csgSampleProperty = java::System::getProperty("poly.csgSample");
    if ( !isBlank(csgSampleProperty) ) {
        options.csgSampleName = parseCsgSample(csgSampleProperty);
        options.hasCsgSampleName = true;
    }

    java::String pointsProperty = java::System::getProperty("poly.points");
    if ( !isBlank(pointsProperty) ) {
        options.drawPoints = parseBooleanFlag("poly.points", pointsProperty);
        options.hasDrawPoints = true;
    }

    java::String wiresProperty = java::System::getProperty("poly.wires");
    if ( !isBlank(wiresProperty) ) {
        options.drawWires = parseBooleanFlag("poly.wires", wiresProperty);
        options.hasDrawWires = true;
    }

    java::String surfacesProperty = java::System::getProperty("poly.surfaces");
    if ( !isBlank(surfacesProperty) ) {
        options.drawSurfaces = parseBooleanFlag("poly.surfaces",
            surfacesProperty);
        options.hasDrawSurfaces = true;
    }

    java::String shadingProperty = java::System::getProperty("poly.shading");
    if ( !isBlank(shadingProperty) ) {
        options.shadingType = parseShadingType(shadingProperty);
        options.hasShadingType = true;
    }

    java::String motifIndexProperty = java::System::getProperty("poly.motifIndex");
    if ( !isBlank(motifIndexProperty) ) {
        options.kurlanderBowlMotifIndex = parseMotifIndex(motifIndexProperty);
        options.hasKurlanderBowlMotifIndex = true;
    }

    java::String faceIdProperty = java::System::getProperty("poly.faceId");
    if ( !isBlank(faceIdProperty) ) {
        options.faceId = parseFaceId(faceIdProperty);
        options.hasFaceId = true;
    }
}

int CommandLineOptions::parseMotifIndex(const java::String& rawValue)
{
    try {
        return java::Integer::parseInt(rawValue);
    }
    catch ( const java::NumberFormatException& ) {
        throw java::IllegalArgumentException(
            java::String("Invalid --motifIndex value '") + rawValue + "'");
    }
}

int CommandLineOptions::parseFaceId(const java::String& rawValue)
{
    try {
        return java::Integer::parseInt(rawValue);
    }
    catch ( const java::NumberFormatException& ) {
        throw java::IllegalArgumentException(
            java::String("Invalid --faceId value '") + rawValue + "'");
    }
}

int CommandLineOptions::parseEdgeIndex(const java::String& rawValue)
{
    try {
        return java::Integer::parseInt(rawValue);
    }
    catch ( const java::NumberFormatException& ) {
        throw java::IllegalArgumentException(
            java::String("Invalid --edgeIndex value '") + rawValue + "'");
    }
}

double CommandLineOptions::parseNormalThresholdDegrees(
    const java::String& rawValue)
{
    try {
        return java::Double::parseDouble(rawValue);
    }
    catch ( const java::NumberFormatException& ) {
        throw java::IllegalArgumentException(
            java::String("Invalid --normalThresholdDeg value '") + rawValue + "'");
    }
}

SolidModelNames CommandLineOptions::parseSolidModel(const java::String& rawValue)
{
    SolidModelNames value;
    if ( !SolidModelNamesUtil::valueOf(rawValue.c_str(), &value) ) {
        throw java::IllegalArgumentException(
            java::String("Invalid --solidModel value '") + rawValue + "'");
    }
    return value;
}

CsgSampleNames CommandLineOptions::parseCsgSample(const java::String& rawValue)
{
    CsgSampleNames value;
    if ( !CsgSampleNamesUtil::valueOf(rawValue.c_str(), &value) ) {
        throw java::IllegalArgumentException(
            java::String("Invalid --csgSample value '") + rawValue + "'");
    }
    return value;
}

bool CommandLineOptions::parseBooleanFlag(const java::String& optionName,
                                          const java::String& rawValue)
{
    if ( java::String("true").equalsIgnoreCase(rawValue) ) {
        return true;
    }
    if ( java::String("false").equalsIgnoreCase(rawValue) ) {
        return false;
    }
    throw java::IllegalArgumentException(
        java::String("Invalid boolean value for ") + optionName + ": '" +
        rawValue + "'");
}

ShadingType CommandLineOptions::parseShadingType(const java::String& rawValue)
{
    java::String normalized = rawValue.trim();

    if ( normalized.startsWith("SHADING_TYPE_") ) {
        normalized = normalized.substring(java::String("SHADING_TYPE_").length());
    }

    // Java: `ShadingType.valueOf(normalized)`
    int code;
    for ( code = SHADING_NOLIGHT; code <= SHADING_COOK_TERRANCE; code++ ) {
        ShadingType value = ShadingTypeUtil::fromCode(code);
        if ( normalized.equals(ShadingTypeUtil::name(value)) ) {
            return value;
        }
    }
    throw java::IllegalArgumentException(
        java::String("Invalid --shading value '") + rawValue + "'");
}
