package vsdk.toolkit.media.solidTexture.from2d;

public enum ImageToSolidTextureProjectionMethods
{
    PLANAR_MAP(0),
    SPHERICAL_MAP(1),
    CYLINDRICAL_MAP(2),
    TORUS_MAP(5);

    private final int value;

    ImageToSolidTextureProjectionMethods(int value)
    {
        this.value = value;
    }

    public int value()
    {
        return value;
    }

    public static ImageToSolidTextureProjectionMethods fromInt(int value)
    {
        return switch ( value ) {
            case 1 -> SPHERICAL_MAP;
            case 2 -> CYLINDRICAL_MAP;
            case 5 -> TORUS_MAP;
            default -> PLANAR_MAP;
        };
    }
}
