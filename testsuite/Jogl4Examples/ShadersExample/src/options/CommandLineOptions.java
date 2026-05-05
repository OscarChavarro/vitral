package options;

import model.ShaderOperationMode;

public class CommandLineOptions
{
    private static final int DEFAULT_OFFLINE_WIDTH = 1100;
    private static final int DEFAULT_OFFLINE_HEIGHT = 900;

    private boolean offline;
    private String offlineOutputPath;
    private ShaderOperationMode method;
    private Double rotationDegrees;
    private Boolean withTexture;
    private Boolean withBumpMap;
    private int width;
    private int height;

    public static CommandLineOptions parse(String[] args)
    {
        CommandLineOptions options = new CommandLineOptions();
        options.offline = false;
        options.offlineOutputPath = null;
        options.method = ShaderOperationMode.OPENGL_4_1;
        options.rotationDegrees = null;
        options.withTexture = null;
        options.withBumpMap = null;
        options.width = DEFAULT_OFFLINE_WIDTH;
        options.height = DEFAULT_OFFLINE_HEIGHT;

        for ( int i = 0; i < args.length; i++ ) {
            String arg = args[i];
            if ( "--offline".equals(arg) ) {
                ensureHasValue(args, i, "--offline");
                options.offline = true;
                options.offlineOutputPath = args[++i];
                continue;
            }
            if ( arg.startsWith("--offline=") ) {
                options.offline = true;
                options.offlineOutputPath = arg.substring("--offline=".length());
                continue;
            }
            if ( "--method".equals(arg) ) {
                ensureHasValue(args, i, "--method");
                options.method = parseMethod(args[++i]);
                continue;
            }
            if ( arg.startsWith("--method=") ) {
                options.method = parseMethod(arg.substring("--method=".length()));
                continue;
            }
            if ( "--rotation".equals(arg) ) {
                ensureHasValue(args, i, "--rotation");
                options.rotationDegrees = parseDouble(args[++i], "--rotation");
                continue;
            }
            if ( arg.startsWith("--rotation=") ) {
                options.rotationDegrees = parseDouble(
                    arg.substring("--rotation=".length()), "--rotation");
                continue;
            }
            if ( "--with".equals(arg) ) {
                ensureHasValue(args, i, "--with");
                applyFeatureSwitches(options, args[++i], true);
                continue;
            }
            if ( arg.startsWith("--with=") ) {
                applyFeatureSwitches(
                    options,
                    arg.substring("--with=".length()),
                    true);
                continue;
            }
            if ( "--without".equals(arg) ) {
                ensureHasValue(args, i, "--without");
                applyFeatureSwitches(options, args[++i], false);
                continue;
            }
            if ( arg.startsWith("--without=") ) {
                applyFeatureSwitches(
                    options,
                    arg.substring("--without=".length()),
                    false);
                continue;
            }
            if ( "--width".equals(arg) ) {
                ensureHasValue(args, i, "--width");
                options.width = Math.max(1, parseInt(args[++i], "--width"));
                continue;
            }
            if ( arg.startsWith("--width=") ) {
                options.width = Math.max(1, parseInt(
                    arg.substring("--width=".length()), "--width"));
                continue;
            }
            if ( "--height".equals(arg) ) {
                ensureHasValue(args, i, "--height");
                options.height = Math.max(1, parseInt(args[++i], "--height"));
                continue;
            }
            if ( arg.startsWith("--height=") ) {
                options.height = Math.max(1, parseInt(
                    arg.substring("--height=".length()), "--height"));
                continue;
            }

            throw new IllegalArgumentException("Unknown option: " + arg);
        }

        if ( options.offline && (options.offlineOutputPath == null ||
             options.offlineOutputPath.isBlank()) ) {
            throw new IllegalArgumentException(
                "--offline requires an output file path");
        }

        return options;
    }

    private static void ensureHasValue(String[] args, int i, String name)
    {
        if ( i + 1 >= args.length ) {
            throw new IllegalArgumentException(name + " requires a value");
        }
    }

    private static int parseInt(String raw, String optionName)
    {
        try {
            return Integer.parseInt(raw);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid integer for " + optionName + ": " + raw, e);
        }
    }

    private static double parseDouble(String raw, String optionName)
    {
        try {
            return Double.parseDouble(raw);
        }
        catch ( NumberFormatException e ) {
            throw new IllegalArgumentException(
                "Invalid number for " + optionName + ": " + raw, e);
        }
    }

    private static ShaderOperationMode parseMethod(String raw)
    {
        String normalized = raw.trim().toLowerCase();
        if ( normalized.equals("opengl") || normalized.equals("opengl_4_1") ) {
            return ShaderOperationMode.OPENGL_4_1;
        }
        if ( normalized.equals("software") ) {
            return ShaderOperationMode.SOFTWARE;
        }
        throw new IllegalArgumentException(
            "Unknown --method value: " + raw + ". Use opengl or software.");
    }

    private static void applyFeatureSwitches(
        CommandLineOptions options,
        String rawList,
        boolean enabled)
    {
        String[] tokens = rawList.split(",");
        for ( String rawToken : tokens ) {
            String token = rawToken.trim().toLowerCase();
            if ( token.isEmpty() ) {
                continue;
            }
            if ( token.equals("texture") || token.equals("textures") ) {
                options.withTexture = Boolean.valueOf(enabled);
                continue;
            }
            if ( token.equals("bumpmap") || token.equals("bump") ||
                 token.equals("normalmap") ) {
                options.withBumpMap = Boolean.valueOf(enabled);
                continue;
            }
            throw new IllegalArgumentException(
                "Unknown feature in --with/--without: " + rawToken +
                ". Use texture,bumpmap");
        }
    }

    public boolean isOffline()
    {
        return offline;
    }

    public String getOfflineOutputPath()
    {
        return offlineOutputPath;
    }

    public ShaderOperationMode getMethod()
    {
        return method;
    }

    public Double getRotationDegrees()
    {
        return rotationDegrees;
    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    public Boolean getWithTexture()
    {
        return withTexture;
    }

    public Boolean getWithBumpMap()
    {
        return withBumpMap;
    }
}
