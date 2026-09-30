#ifndef __SOLID_TEXTURE_EXAMPLE_COLOR_NAMES__
#define __SOLID_TEXTURE_EXAMPLE_COLOR_NAMES__

enum class SolidTextureExampleColorNames {
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
    MATERIAL_MAP_TEXTURE
};

const char* solidTextureExampleColorName(SolidTextureExampleColorNames value);
SolidTextureExampleColorNames nextSolidTextureExampleColorName(
    SolidTextureExampleColorNames value);
SolidTextureExampleColorNames previousSolidTextureExampleColorName(
    SolidTextureExampleColorNames value);

#endif
