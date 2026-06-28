package vsdk.toolkit.media.solidTexture.from2d;

public enum ImageToSolidTextureInterpolationTypes
{
    NO_INTERPOLATION(0),
    NEAREST_NEIGHBOR(1),
    BI_LINEAR(2),
    CUBIC_SPLINE(3),
    NORMALIZED_DIST(4);

    private final int value;

    ImageToSolidTextureInterpolationTypes(int value)
    {
        this.value = value;
    }

    public int value()
    {
        return value;
    }

    public static ImageToSolidTextureInterpolationTypes fromInt(int value)
    {
        return switch ( value ) {
            case 1 -> NEAREST_NEIGHBOR;
            case 2 -> BI_LINEAR;
            case 3 -> CUBIC_SPLINE;
            case 4 -> NORMALIZED_DIST;
            default -> NO_INTERPOLATION;
        };
    }
}
