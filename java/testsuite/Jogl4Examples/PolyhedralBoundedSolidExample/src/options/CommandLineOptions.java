package options;

import vsdk.toolkit.environment.material.ShadingType;
import models.CsgSampleNames;
import models.SolidModelNames;

public class CommandLineOptions
{
    private boolean offline;
    private boolean motifSweep;
    private String outputPath;
    private SolidModelNames solidModelName;
    private CsgSampleNames csgSampleName;
    private Boolean drawPoints;
    private Boolean drawWires;
    private Boolean drawSurfaces;
    private ShadingType shadingType;
    private Integer kurlanderBowlMotifIndex;
    private Integer faceId;
    private Integer edgeIndex;
    private Double vertexNormalSmoothingThresholdDegrees;

    public CommandLineOptions()
    {
        offline = false;
        motifSweep = false;
        outputPath = "output.png";
        solidModelName = null;
        csgSampleName = null;
        drawPoints = null;
        drawWires = null;
        drawSurfaces = null;
        shadingType = null;
        kurlanderBowlMotifIndex = null;
        faceId = null;
        edgeIndex = null;
        vertexNormalSmoothingThresholdDegrees = null;
    }

    public boolean isOffline()
    {
        return offline;
    }

    public boolean isMotifSweep()
    {
        return motifSweep;
    }

    public String getOutputPath()
    {
        return outputPath;
    }

    public SolidModelNames getSolidModelName()
    {
        return solidModelName;
    }

    public CsgSampleNames getCsgSampleName()
    {
        return csgSampleName;
    }

    public Boolean getDrawPoints()
    {
        return drawPoints;
    }

    public Boolean getDrawWires()
    {
        return drawWires;
    }

    public Boolean getDrawSurfaces()
    {
        return drawSurfaces;
    }

    public ShadingType getShadingType()
    {
        return shadingType;
    }

    public Integer getKurlanderBowlMotifIndex()
    {
        return kurlanderBowlMotifIndex;
    }

    public Integer getFaceId()
    {
        return faceId;
    }

    public Double getVertexNormalSmoothingThresholdDegrees()
    {
        return vertexNormalSmoothingThresholdDegrees;
    }

    public Integer getEdgeIndex()
    {
        return edgeIndex;
    }

    public static CommandLineOptions parse(String[] args)
    {
        CommandLineOptions options = new CommandLineOptions();
        applySystemProperties(options);
        int i;

        for ( i = 0; args != null && i < args.length; i++ ) {
            String arg = args[i];
            if ( "--offline".equals(arg) ) {
                options.offline = true;
            }
            else if ( "--motifSweep".equals(arg) ) {
                options.motifSweep = true;
                options.offline = true;
            }
            else if ( "--screenshot".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --screenshot");
                }
                options.outputPath = args[++i];
            }
            else if ( arg.startsWith("--screenshot=") ) {
                options.outputPath = arg.substring("--screenshot=".length());
            }
            else if ( "--solidModel".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --solidModel");
                }
                options.solidModelName = parseSolidModel(args[++i]);
            }
            else if ( arg.startsWith("--solidModel=") ) {
                options.solidModelName = parseSolidModel(
                    arg.substring("--solidModel=".length()));
            }
            else if ( "--csgSample".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --csgSample");
                }
                options.csgSampleName = parseCsgSample(args[++i]);
            }
            else if ( arg.startsWith("--csgSample=") ) {
                options.csgSampleName = parseCsgSample(
                    arg.substring("--csgSample=".length()));
            }
            else if ( "--points".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException("Missing value for --points");
                }
                options.drawPoints = parseBooleanFlag("--points", args[++i]);
            }
            else if ( arg.startsWith("--points=") ) {
                options.drawPoints = parseBooleanFlag("--points",
                    arg.substring("--points=".length()));
            }
            else if ( "--wires".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException("Missing value for --wires");
                }
                options.drawWires = parseBooleanFlag("--wires", args[++i]);
            }
            else if ( arg.startsWith("--wires=") ) {
                options.drawWires = parseBooleanFlag("--wires",
                    arg.substring("--wires=".length()));
            }
            else if ( "--surfaces".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException("Missing value for --surfaces");
                }
                options.drawSurfaces = parseBooleanFlag("--surfaces", args[++i]);
            }
            else if ( arg.startsWith("--surfaces=") ) {
                options.drawSurfaces = parseBooleanFlag("--surfaces",
                    arg.substring("--surfaces=".length()));
            }
            else if ( "--shading".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException("Missing value for --shading");
                }
                options.shadingType = parseShadingType(args[++i]);
            }
            else if ( arg.startsWith("--shading=") ) {
                options.shadingType = parseShadingType(
                    arg.substring("--shading=".length()));
            }
            else if ( "--motifIndex".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --motifIndex");
                }
                options.kurlanderBowlMotifIndex = parseMotifIndex(args[++i]);
            }
            else if ( arg.startsWith("--motifIndex=") ) {
                options.kurlanderBowlMotifIndex = parseMotifIndex(
                    arg.substring("--motifIndex=".length()));
            }
            else if ( "--faceId".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --faceId");
                }
                options.faceId = parseFaceId(args[++i]);
            }
            else if ( arg.startsWith("--faceId=") ) {
                options.faceId = parseFaceId(
                    arg.substring("--faceId=".length()));
            }
            else if ( "--edgeIndex".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --edgeIndex");
                }
                options.edgeIndex = parseEdgeIndex(args[++i]);
            }
            else if ( arg.startsWith("--edgeIndex=") ) {
                options.edgeIndex = parseEdgeIndex(
                    arg.substring("--edgeIndex=".length()));
            }
            else if ( "--normalThresholdDeg".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --normalThresholdDeg");
                }
                options.vertexNormalSmoothingThresholdDegrees =
                    parseNormalThresholdDegrees(args[++i]);
            }
            else if ( arg.startsWith("--normalThresholdDeg=") ) {
                options.vertexNormalSmoothingThresholdDegrees =
                    parseNormalThresholdDegrees(
                        arg.substring("--normalThresholdDeg=".length()));
            }
            else {
                throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }

        if ( options.outputPath == null || options.outputPath.isBlank() ) {
            options.outputPath = "output.png";
        }

        return options;
    }

    private static void applySystemProperties(CommandLineOptions options)
    {
        String offlineProperty = System.getProperty("poly.offline");
        String outputProperty = System.getProperty("poly.output");
        String thresholdProperty = System.getProperty("poly.normalThresholdDeg");
        String edgeIndexProperty = System.getProperty("poly.edgeIndex");

        if ( offlineProperty != null ) {
            options.offline = Boolean.parseBoolean(offlineProperty);
        }
        if ( outputProperty != null && !outputProperty.isBlank() ) {
            options.outputPath = outputProperty;
        }
        if ( thresholdProperty != null && !thresholdProperty.isBlank() ) {
            options.vertexNormalSmoothingThresholdDegrees =
                parseNormalThresholdDegrees(thresholdProperty);
        }
        if ( edgeIndexProperty != null && !edgeIndexProperty.isBlank() ) {
            options.edgeIndex = parseEdgeIndex(edgeIndexProperty);
        }

        String solidModelProperty = System.getProperty("poly.solidModel");
        if ( solidModelProperty != null && !solidModelProperty.isBlank() ) {
            options.solidModelName = parseSolidModel(solidModelProperty);
        }

        String csgSampleProperty = System.getProperty("poly.csgSample");
        if ( csgSampleProperty != null && !csgSampleProperty.isBlank() ) {
            options.csgSampleName = parseCsgSample(csgSampleProperty);
        }

        String pointsProperty = System.getProperty("poly.points");
        if ( pointsProperty != null && !pointsProperty.isBlank() ) {
            options.drawPoints = parseBooleanFlag("poly.points", pointsProperty);
        }

        String wiresProperty = System.getProperty("poly.wires");
        if ( wiresProperty != null && !wiresProperty.isBlank() ) {
            options.drawWires = parseBooleanFlag("poly.wires", wiresProperty);
        }

        String surfacesProperty = System.getProperty("poly.surfaces");
        if ( surfacesProperty != null && !surfacesProperty.isBlank() ) {
            options.drawSurfaces = parseBooleanFlag("poly.surfaces",
                surfacesProperty);
        }

        String shadingProperty = System.getProperty("poly.shading");
        if ( shadingProperty != null && !shadingProperty.isBlank() ) {
            options.shadingType = parseShadingType(shadingProperty);
        }

        String motifIndexProperty = System.getProperty("poly.motifIndex");
        if ( motifIndexProperty != null && !motifIndexProperty.isBlank() ) {
            options.kurlanderBowlMotifIndex = parseMotifIndex(motifIndexProperty);
        }

        String faceIdProperty = System.getProperty("poly.faceId");
        if ( faceIdProperty != null && !faceIdProperty.isBlank() ) {
            options.faceId = parseFaceId(faceIdProperty);
        }
    }

    private static Integer parseMotifIndex(String rawValue)
    {
        try {
            return Integer.valueOf(rawValue);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid --motifIndex value '" + rawValue + "'");
        }
    }

    private static Integer parseFaceId(String rawValue)
    {
        try {
            return Integer.valueOf(rawValue);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid --faceId value '" + rawValue + "'");
        }
    }

    private static Integer parseEdgeIndex(String rawValue)
    {
        try {
            return Integer.valueOf(rawValue);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid --edgeIndex value '" + rawValue + "'");
        }
    }

    private static Double parseNormalThresholdDegrees(String rawValue)
    {
        try {
            return Double.valueOf(rawValue);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid --normalThresholdDeg value '" + rawValue + "'");
        }
    }

    private static SolidModelNames parseSolidModel(String rawValue)
    {
        try {
            return SolidModelNames.valueOf(rawValue);
        }
        catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException(
                "Invalid --solidModel value '" + rawValue + "'");
        }
    }

    private static CsgSampleNames parseCsgSample(String rawValue)
    {
        try {
            return CsgSampleNames.valueOf(rawValue);
        }
        catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException(
                "Invalid --csgSample value '" + rawValue + "'");
        }
    }

    private static Boolean parseBooleanFlag(String optionName, String rawValue)
    {
        if ( "true".equalsIgnoreCase(rawValue) ) {
            return Boolean.TRUE;
        }
        if ( "false".equalsIgnoreCase(rawValue) ) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(
            "Invalid boolean value for " + optionName + ": '" + rawValue + "'");
    }

    private static ShadingType parseShadingType(
        String rawValue)
    {
        String normalized = rawValue.trim();

        if ( normalized.startsWith("SHADING_TYPE_") ) {
            normalized = normalized.substring("SHADING_TYPE_".length());
        }

        try {
            return ShadingType.valueOf(normalized);
        }
        catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException(
                "Invalid --shading value '" + rawValue + "'");
        }
    }
}
