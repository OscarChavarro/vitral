package options;

import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.ShadingType;
import models.CsgSampleNames;
import models.SolidModelNames;

public class CommandLineOptions
{
    private boolean offline;
    private String outputPath;
    private SolidModelNames solidModelName;
    private CsgSampleNames csgSampleName;
    private Boolean drawPoints;
    private Boolean drawWires;
    private Boolean drawSurfaces;
    private ShadingType shadingType;

    public CommandLineOptions()
    {
        offline = false;
        outputPath = "output.png";
        solidModelName = null;
        csgSampleName = null;
        drawPoints = null;
        drawWires = null;
        drawSurfaces = null;
        shadingType = null;
    }

    public boolean isOffline()
    {
        return offline;
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
            else if ( "--output".equals(arg) ) {
                if ( i + 1 >= args.length ) {
                    throw new IllegalArgumentException(
                        "Missing value for --output");
                }
                options.outputPath = args[++i];
            }
            else if ( arg.startsWith("--output=") ) {
                options.outputPath = arg.substring("--output=".length());
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

        if ( offlineProperty != null ) {
            options.offline = Boolean.parseBoolean(offlineProperty);
        }
        if ( outputProperty != null && !outputProperty.isBlank() ) {
            options.outputPath = outputProperty;
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
