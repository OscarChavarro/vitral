package model;

public enum SolidTextureExampleColorNames {
    NO_TEXTURE,
    COLOUR_TEXTURE,
    BOZO_TEXTURE,
    MARBLE_TEXTURE,
    WOOD_TEXTURE,
    CHECKER_TEXTURE,
    CHECKER_TEXTURE_TEXTURE,
    SPOTTED_TEXTURE,
    AGATE_TEXTURE,
    GRANITE_TEXTURE,
    GRADIENT_TEXTURE,
    IMAGE_MAP_TEXTURE,
    ONION_TEXTURE,
    LEOPARD_TEXTURE,
    BRICK_TEXTURE,
    MATERIAL_MAP_TEXTURE;

    public SolidTextureExampleColorNames next()
    {
        SolidTextureExampleColorNames[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public SolidTextureExampleColorNames previous()
    {
        SolidTextureExampleColorNames[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }
}
