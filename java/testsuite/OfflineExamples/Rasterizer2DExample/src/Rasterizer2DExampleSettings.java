/** Shared image-size parsing for the Rasterizer2D offline examples. */
public final class Rasterizer2DExampleSettings {
    public static final int DEFAULT_WIDTH = 640;
    public static final int DEFAULT_HEIGHT = 480;

    private Rasterizer2DExampleSettings() { }

    public static int positiveArgument(String[] args, int index, int fallback) {
        if (args == null || args.length <= index || args[index] == null) return fallback;
        try {
            int value = Integer.parseInt(args[index]);
            return value > 0 ? value : fallback;
        }
        catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
