package vsdk.toolkit.common;

public enum ShadingType
{
    NOLIGHT(RendererConfiguration.SHADING_TYPE_NOLIGHT),
    FLAT(RendererConfiguration.SHADING_TYPE_FLAT),
    GOURAUD(RendererConfiguration.SHADING_TYPE_GOURAUD),
    PHONG(RendererConfiguration.SHADING_TYPE_PHONG),
    COOK_TERRANCE(RendererConfiguration.SHADING_TYPE_COOK_TERRANCE);

    private final int code;

    ShadingType(int code)
    {
        this.code = code;
    }

    public int getCode()
    {
        return code;
    }

    public static ShadingType fromCode(int code)
    {
        for ( ShadingType value : values() ) {
            if ( value.code == code ) {
                return value;
            }
        }
        return GOURAUD;
    }

    public ShadingType next()
    {
        ShadingType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
