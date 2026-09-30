#include "model/SolidTextureExampleColorNames.h"

static const int SOLID_TEXTURE_COLOR_COUNT = 16;

const char* solidTextureExampleColorName(SolidTextureExampleColorNames value)
{
    switch ( value ) {
      case SolidTextureExampleColorNames::NO_TEXTURE: return "NO_TEXTURE";
      case SolidTextureExampleColorNames::COLOUR_TEXTURE: return "COLOUR_TEXTURE";
      case SolidTextureExampleColorNames::BOZO_TEXTURE: return "BOZO_TEXTURE";
      case SolidTextureExampleColorNames::MARBLE_TEXTURE: return "MARBLE_TEXTURE";
      case SolidTextureExampleColorNames::WOOD_TEXTURE: return "WOOD_TEXTURE";
      case SolidTextureExampleColorNames::CHECKER_TEXTURE: return "CHECKER_TEXTURE";
      case SolidTextureExampleColorNames::CHECKER_TEXTURE_TEXTURE: return "CHECKER_TEXTURE_TEXTURE";
      case SolidTextureExampleColorNames::SPOTTED_TEXTURE: return "SPOTTED_TEXTURE";
      case SolidTextureExampleColorNames::AGATE_TEXTURE: return "AGATE_TEXTURE";
      case SolidTextureExampleColorNames::GRANITE_TEXTURE: return "GRANITE_TEXTURE";
      case SolidTextureExampleColorNames::GRADIENT_TEXTURE: return "GRADIENT_TEXTURE";
      case SolidTextureExampleColorNames::IMAGE_MAP_TEXTURE: return "IMAGE_MAP_TEXTURE";
      case SolidTextureExampleColorNames::ONION_TEXTURE: return "ONION_TEXTURE";
      case SolidTextureExampleColorNames::LEOPARD_TEXTURE: return "LEOPARD_TEXTURE";
      case SolidTextureExampleColorNames::BRICK_TEXTURE: return "BRICK_TEXTURE";
      case SolidTextureExampleColorNames::MATERIAL_MAP_TEXTURE: return "MATERIAL_MAP_TEXTURE";
    }
    return "UNKNOWN";
}

SolidTextureExampleColorNames nextSolidTextureExampleColorName(
    SolidTextureExampleColorNames value)
{
    int next = (static_cast<int>(value) + 1) % SOLID_TEXTURE_COLOR_COUNT;
    return static_cast<SolidTextureExampleColorNames>(next);
}

SolidTextureExampleColorNames previousSolidTextureExampleColorName(
    SolidTextureExampleColorNames value)
{
    int previous = (static_cast<int>(value) + SOLID_TEXTURE_COLOR_COUNT - 1) %
        SOLID_TEXTURE_COLOR_COUNT;
    return static_cast<SolidTextureExampleColorNames>(previous);
}
