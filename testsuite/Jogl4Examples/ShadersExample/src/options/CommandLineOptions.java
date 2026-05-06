package options;

import model.ShaderOperationMode;
import vsdk.toolkit.common.RendererConfiguration;

public class CommandLineOptions
{
    public enum TextureFilterOption {
        LINEAR,
        NEAREST
    }
    private static final int DEFAULT_OFFLINE_WIDTH = 1100;
    private static final int DEFAULT_OFFLINE_HEIGHT = 900;

    private boolean offline;
    private String offlineOutputPath;
    private ShaderOperationMode method;
    private Double rotationDegrees;
    private Double lightRotationDegrees;
    private Boolean withTexture;
    private Boolean withBumpMap;
    private Integer shadingType;
    private TextureFilterOption textureFilter;
    private Integer meridians;
    private Integer parallels;
    private Double cpuTextureOffsetUTexels;
    private Double cpuTextureOffsetVTexels;
    private boolean showHud;
    private int width;
    private int height;

    public static CommandLineOptions parse(String[] args)
    {
        CommandLineOptions options = new CommandLineOptions();
        options.offline = false;
        options.offlineOutputPath = null;
        options.method = ShaderOperationMode.OPENGL_4_1;
        options.rotationDegrees = null;
        options.lightRotationDegrees = null;
        options.withTexture = null;
        options.withBumpMap = null;
        options.shadingType = null;
        options.textureFilter = null;
        options.meridians = null;
        options.parallels = null;
        options.cpuTextureOffsetUTexels = null;
        options.cpuTextureOffsetVTexels = null;
        options.showHud = false;
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
            if ( "--light-rotation".equals(arg) ) {
                ensureHasValue(args, i, "--light-rotation");
                options.lightRotationDegrees =
                    parseDouble(args[++i], "--light-rotation");
                continue;
            }
            if ( arg.startsWith("--light-rotation=") ) {
                options.lightRotationDegrees = parseDouble(
                    arg.substring("--light-rotation=".length()),
                    "--light-rotation");
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
            if ( "--shading".equals(arg) ) {
                ensureHasValue(args, i, "--shading");
                options.shadingType = parseShading(args[++i]);
                continue;
            }
            if ( arg.startsWith("--shading=") ) {
                options.shadingType = parseShading(
                    arg.substring("--shading=".length()));
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
            if ( "--texture-filter".equals(arg) ) {
                ensureHasValue(args, i, "--texture-filter");
                options.textureFilter = parseTextureFilter(args[++i]);
                continue;
            }
            if ( arg.startsWith("--texture-filter=") ) {
                options.textureFilter = parseTextureFilter(
                    arg.substring("--texture-filter=".length()));
                continue;
            }
            if ( "--meridians".equals(arg) ) {
                ensureHasValue(args, i, "--meridians");
                options.meridians = Integer.valueOf(Math.max(3, parseInt(args[++i], "--meridians")));
                continue;
            }
            if ( arg.startsWith("--meridians=") ) {
                options.meridians = Integer.valueOf(Math.max(3, parseInt(
                    arg.substring("--meridians=".length()), "--meridians")));
                continue;
            }
            if ( "--parallels".equals(arg) ) {
                ensureHasValue(args, i, "--parallels");
                options.parallels = Integer.valueOf(Math.max(2, parseInt(args[++i], "--parallels")));
                continue;
            }
            if ( arg.startsWith("--parallels=") ) {
                options.parallels = Integer.valueOf(Math.max(2, parseInt(
                    arg.substring("--parallels=".length()), "--parallels")));
                continue;
            }
            if ( "--cpu-texture-offset-u".equals(arg) ) {
                ensureHasValue(args, i, "--cpu-texture-offset-u");
                options.cpuTextureOffsetUTexels =
                    Double.valueOf(parseDouble(args[++i], "--cpu-texture-offset-u"));
                continue;
            }
            if ( arg.startsWith("--cpu-texture-offset-u=") ) {
                options.cpuTextureOffsetUTexels = Double.valueOf(parseDouble(
                    arg.substring("--cpu-texture-offset-u=".length()),
                    "--cpu-texture-offset-u"));
                continue;
            }
            if ( "--cpu-texture-offset-v".equals(arg) ) {
                ensureHasValue(args, i, "--cpu-texture-offset-v");
                options.cpuTextureOffsetVTexels =
                    Double.valueOf(parseDouble(args[++i], "--cpu-texture-offset-v"));
                continue;
            }
            if ( arg.startsWith("--cpu-texture-offset-v=") ) {
                options.cpuTextureOffsetVTexels = Double.valueOf(parseDouble(
                    arg.substring("--cpu-texture-offset-v=".length()),
                    "--cpu-texture-offset-v"));
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
            if ( "--hud".equals(arg) ) {
                ensureHasValue(args, i, "--hud");
                options.showHud = parseBooleanSwitch(args[++i], "--hud");
                continue;
            }
            if ( arg.startsWith("--hud=") ) {
                options.showHud = parseBooleanSwitch(
                    arg.substring("--hud=".length()),
                    "--hud");
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

    private static boolean parseBooleanSwitch(String raw, String optionName)
    {
        String normalized = raw.trim().toLowerCase();
        if ( normalized.equals("on") || normalized.equals("true") ||
             normalized.equals("1") || normalized.equals("yes") ) {
            return true;
        }
        if ( normalized.equals("off") || normalized.equals("false") ||
             normalized.equals("0") || normalized.equals("no") ) {
            return false;
        }
        throw new IllegalArgumentException(
            "Unknown value for " + optionName + ": " + raw +
            ". Use on/off.");
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

    private static int parseShading(String raw)
    {
        String normalized = raw.trim().toLowerCase();
        if ( normalized.equals("constant") || normalized.equals("nolight") ) {
            return RendererConfiguration.SHADING_TYPE_NOLIGHT;
        }
        if ( normalized.equals("flat") ) {
            return RendererConfiguration.SHADING_TYPE_FLAT;
        }
        if ( normalized.equals("gouraud") ) {
            return RendererConfiguration.SHADING_TYPE_GOURAUD;
        }
        if ( normalized.equals("phong") ) {
            return RendererConfiguration.SHADING_TYPE_PHONG;
        }
        if ( normalized.equals("cook") ||
             normalized.equals("cook_torrance") ||
             normalized.equals("cook-torrance") ||
             normalized.equals("cooktorrance") ) {
            return RendererConfiguration.SHADING_TYPE_COOK_TERRANCE;
        }
        throw new IllegalArgumentException(
            "Unknown --shading value: " + raw +
            ". Use constant, flat, gouraud, phong, or cook_torrance.");
    }

    private static TextureFilterOption parseTextureFilter(String raw)
    {
        String normalized = raw.trim().toLowerCase();
        if ( normalized.equals("linear") ) {
            return TextureFilterOption.LINEAR;
        }
        if ( normalized.equals("nearest") ) {
            return TextureFilterOption.NEAREST;
        }
        throw new IllegalArgumentException(
            "Unknown --texture-filter value: " + raw +
            ". Use linear or nearest.");
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

    public Double getLightRotationDegrees()
    {
        return lightRotationDegrees;
    }

    public Boolean getWithTexture()
    {
        return withTexture;
    }

    public Boolean getWithBumpMap()
    {
        return withBumpMap;
    }

    public Integer getShadingType()
    {
        return shadingType;
    }

    public TextureFilterOption getTextureFilter()
    {
        return textureFilter;
    }

    public Integer getMeridians()
    {
        return meridians;
    }

    public Integer getParallels()
    {
        return parallels;
    }

    public Double getCpuTextureOffsetUTexels()
    {
        return cpuTextureOffsetUTexels;
    }

    public Double getCpuTextureOffsetVTexels()
    {
        return cpuTextureOffsetVTexels;
    }

    public boolean getShowHud()
    {
        return showHud;
    }
}
