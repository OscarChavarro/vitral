package options;

import model.PolygonClippingDebuggerModel;
import model.PolygonClippingFixtures;
import model.PolygonSurfaceTessellationMode;

public class CommandLineOptions
{
    private final PolygonClippingDebuggerModel model;
    private boolean offlineMode;

    public CommandLineOptions(PolygonClippingDebuggerModel model)
    {
        if ( model == null ) {
            throw new IllegalArgumentException("model can not be null");
        }
        this.model = model;
    }

    public void parse(String[] args)
    {
        if ( args == null ) {
            return;
        }

        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[i];

            if ( "--fixture".equals(arg) || "--fixture-index".equals(arg) ) {
                ensureValue(args, i, arg);
                applyFixtureIndex(arg, args[++i]);
            }
            else if ( arg.startsWith("--fixture=") ) {
                applyFixtureIndex(arg, arg.substring("--fixture=".length()));
            }
            else if ( arg.startsWith("--fixture-index=") ) {
                applyFixtureIndex(arg, arg.substring("--fixture-index=".length()));
            }
            else if ( "--wires".equals(arg) ) {
                ensureValue(args, i, arg);
                model.getQuality().setWires(parseBoolean(args[++i], arg));
            }
            else if ( arg.startsWith("--wires=") ) {
                model.getQuality().setWires(parseBoolean(arg.substring("--wires=".length()), arg));
            }
            else if ( "--surfaces".equals(arg) ) {
                ensureValue(args, i, arg);
                model.getQuality().setSurfaces(parseBoolean(args[++i], arg));
            }
            else if ( arg.startsWith("--surfaces=") ) {
                model.getQuality().setSurfaces(parseBoolean(arg.substring("--surfaces=".length()), arg));
            }
            else if ( "--points".equals(arg) ) {
                ensureValue(args, i, arg);
                model.getQuality().setPoints(parseBoolean(args[++i], arg));
            }
            else if ( arg.startsWith("--points=") ) {
                model.getQuality().setPoints(parseBoolean(arg.substring("--points=".length()), arg));
            }
            else if ( "--tessellation-mode".equals(arg)
                      || "--polygon-surface-tessellation-mode".equals(arg) ) {
                ensureValue(args, i, arg);
                model.setPolygonSurfaceTessellationMode(
                    parseTessellationMode(args[++i], arg));
            }
            else if ( arg.startsWith("--tessellation-mode=") ) {
                model.setPolygonSurfaceTessellationMode(
                    parseTessellationMode(
                        arg.substring("--tessellation-mode=".length()), arg));
            }
            else if ( arg.startsWith("--polygon-surface-tessellation-mode=") ) {
                model.setPolygonSurfaceTessellationMode(
                    parseTessellationMode(
                        arg.substring("--polygon-surface-tessellation-mode=".length()), arg));
            }
            else if ( "--help".equals(arg) || "-h".equals(arg) ) {
                throw new IllegalArgumentException(usage());
            }
            else if ( "-offline".equals(arg) || "--offline".equals(arg) ) {
                offlineMode = true;
            }
            else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
    }

    public boolean isOfflineMode()
    {
        return offlineMode;
    }

    public static String usage()
    {
        StringBuilder out = new StringBuilder();
        out.append("Usage: [--fixture <index|name>] [--fixture-index <0-based-index>] ")
            .append("[--wires <true|false|on|off>] [--surfaces <true|false|on|off>] ")
            .append("[--points <true|false|on|off>] ")
            .append("[--tessellation-mode <GLU|MONOTONE_DECOMPOSITION>] ")
            .append("[-offline]\n")
            .append("Alias: --polygon-surface-tessellation-mode <...>\n")
            .append("Examples:\n")
            .append("  --fixture 1 --wires on --surfaces off\n")
            .append("  --fixture TRIANGLE_VS_QUAD --surfaces true\n")
            .append("  --fixture-index 0 --wires false --surfaces true\n")
            .append("  --tessellation-mode MONOTONE_DECOMPOSITION\n")
            .append("  --tessellation-mode MONOTONE_DECOMPOSITION --wires on --surfaces on -offline\n")
            .append("  --tessellation-mode MONOTONE_DECOMPOSITION --points on -offline\n")
            .append("  -offline\n")
            .append("Available fixtures:\n");

        for ( int i = 0; i < PolygonClippingFixtures.CASES.length; i++ ) {
            out.append("  ")
                .append(i)
                .append(": ")
                .append(PolygonClippingFixtures.CASES[i].name())
                .append('\n');
        }

        return out.toString().stripTrailing();
    }

    private static PolygonSurfaceTessellationMode parseTessellationMode(
        String rawValue, String optionName)
    {
        if ( rawValue == null ) {
            throw new IllegalArgumentException(
                "Invalid tessellation mode for " + optionName + ": null");
        }

        String normalized = rawValue.trim().replace('-', '_').replace(' ', '_');
        for ( PolygonSurfaceTessellationMode mode
              : PolygonSurfaceTessellationMode.values() ) {
            if ( mode.name().equalsIgnoreCase(normalized)
                 || mode.name().equalsIgnoreCase(rawValue.trim())
                 || mode.getDisplayName().equalsIgnoreCase(rawValue.trim()) ) {
                return mode;
            }
        }

        throw new IllegalArgumentException(
            "Invalid tessellation mode for " + optionName + ": " + rawValue
                + " (expected GLU or MONOTONE_DECOMPOSITION)");
    }

    private void applyFixtureIndex(String optionName, String rawValue)
    {
        if ( "--fixture-index".equals(optionName) ) {
            applyFixtureIndexByNumber(rawValue, false);
            return;
        }

        if ( tryApplyFixtureIndexByNumber(rawValue, true) ) {
            return;
        }

        int fixtureIndex = findFixtureIndexByName(rawValue);
        if ( fixtureIndex < 0 ) {
            throw new IllegalArgumentException(
                "Unknown fixture name: " + rawValue);
        }

        applyFixtureIndex(fixtureIndex);
    }

    private void applyFixtureIndexByNumber(String rawValue, boolean oneBased)
    {
        int fixtureIndex = parseInt(rawValue, "--fixture-index");
        if ( oneBased ) {
            fixtureIndex--;
        }

        int totalFixtures = PolygonClippingFixtures.CASES.length;
        if ( fixtureIndex < 0 || fixtureIndex >= totalFixtures ) {
            throw new IllegalArgumentException(
                "Fixture index out of range: " + rawValue
                    + " (valid range: " + ( oneBased ? "1" : "0" )
                    + ".." + ( oneBased ? totalFixtures : (totalFixtures - 1) )
                    + ")");
        }

        applyFixtureIndex(fixtureIndex);
    }

    private boolean tryApplyFixtureIndexByNumber(String rawValue, boolean oneBased)
    {
        try {
            applyFixtureIndexByNumber(rawValue, oneBased);
            return true;
        }
        catch ( IllegalArgumentException e ) {
            if ( oneBased && !looksNumeric(rawValue) ) {
                return false;
            }
            throw e;
        }
    }

    private void applyFixtureIndex(int fixtureIndex)
    {
        model.setTestIndex(fixtureIndex);
        System.out.println("[PolygonClippingExample] Selected fixture "
            + fixtureIndex + " [" + PolygonClippingFixtures.CASES[fixtureIndex].name() + "]");
    }

    private static int findFixtureIndexByName(String rawValue)
    {
        if ( rawValue == null ) {
            return -1;
        }

        for ( int i = 0; i < PolygonClippingFixtures.CASES.length; i++ ) {
            if ( PolygonClippingFixtures.CASES[i].name().equalsIgnoreCase(rawValue) ) {
                return i;
            }
        }
        return -1;
    }

    private static void ensureValue(String[] args, int index, String optionName)
    {
        if ( index + 1 >= args.length ) {
            throw new IllegalArgumentException("Missing value for " + optionName);
        }
    }

    private static int parseInt(String rawValue, String optionName)
    {
        try {
            return Integer.parseInt(rawValue);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid integer for " + optionName + ": " + rawValue);
        }
    }

    private static boolean looksNumeric(String rawValue)
    {
        if ( rawValue == null || rawValue.isBlank() ) {
            return false;
        }

        int start = rawValue.charAt(0) == '-' ? 1 : 0;
        if ( start >= rawValue.length() ) {
            return false;
        }

        for ( int i = start; i < rawValue.length(); i++ ) {
            if ( !Character.isDigit(rawValue.charAt(i)) ) {
                return false;
            }
        }
        return true;
    }

    private static boolean parseBoolean(String rawValue, String optionName)
    {
        String value = rawValue.trim().toLowerCase();
        if ( "true".equals(value) || "on".equals(value) || "1".equals(value)
             || "yes".equals(value) ) {
            return true;
        }
        if ( "false".equals(value) || "off".equals(value) || "0".equals(value)
             || "no".equals(value) ) {
            return false;
        }

        throw new IllegalArgumentException(
            "Invalid boolean for " + optionName + ": " + rawValue);
    }
}
