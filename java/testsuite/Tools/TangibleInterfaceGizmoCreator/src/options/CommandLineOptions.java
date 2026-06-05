package options;

import models.GizmoNames;
import vsdk.toolkit.environment.material.ShadingType;

public class CommandLineOptions
{
    private boolean offline;
    private String outputPath;
    private GizmoNames solidModelName;
    private Boolean drawPoints;
    private Boolean drawWires;
    private Boolean drawSurfaces;
    private ShadingType shadingType;

    public CommandLineOptions()
    {
        offline = false;
        outputPath = "output.png";
        solidModelName = null;
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

    public GizmoNames getSolidModelName()
    {
        return solidModelName;
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
        int i;

        for ( i = 0; args != null && i < args.length; i++ ) {
            String arg = args[i];
            if ( "--offline".equals(arg) ) {
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

    private static GizmoNames parseSolidModel(String rawValue)
    {
        if ( rawValue == null || rawValue.isBlank() ) {
            throw new IllegalArgumentException("solid model cannot be blank");
        }
        if ( "cube".equalsIgnoreCase(rawValue) ) {
            return GizmoNames.CUBE_PART_1;
        }
        if ( "sphere".equalsIgnoreCase(rawValue) ) {
            return GizmoNames.CUBE_PART_2;
        }
        if ( "text".equalsIgnoreCase(rawValue) ) {
            return GizmoNames.EXPERIMENTAL;
        }
        try {
            return GizmoNames.fromId(Integer.parseInt(rawValue));
        }
        catch ( NumberFormatException e ) {
            return GizmoNames.valueOf(rawValue.trim().toUpperCase());
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
            "Invalid boolean value for " + optionName + ": " + rawValue);
    }

    private static ShadingType parseShadingType(String rawValue)
    {
        try {
            return ShadingType.valueOf(rawValue.trim().toUpperCase());
        }
        catch ( IllegalArgumentException e ) {
            throw new IllegalArgumentException(
                "Invalid shading type: " + rawValue);
        }
    }
}
