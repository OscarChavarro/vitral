/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/model/SolidTextureExampleColorNames.java`.
 *
 * The sixteen solid textures the example cycles through, in the Java
 * declaration order, because `next` and `previous` walk that order modulo its
 * length. A TypeScript enum carries no methods, so those two become the
 * functions below, over the ordered value list a Java enum gets for free.
 */
export enum SolidTextureExampleColorNames {
  NO_TEXTURE = 'NO_TEXTURE',
  COLOUR_TEXTURE = 'COLOUR_TEXTURE',
  BOZO_TEXTURE = 'BOZO_TEXTURE',
  MARBLE_TEXTURE = 'MARBLE_TEXTURE',
  WOOD_TEXTURE = 'WOOD_TEXTURE',
  CHECKER_TEXTURE = 'CHECKER_TEXTURE',
  CHECKER_TEXTURE_TEXTURE = 'CHECKER_TEXTURE_TEXTURE',
  SPOTTED_TEXTURE = 'SPOTTED_TEXTURE',
  AGATE_TEXTURE = 'AGATE_TEXTURE',
  GRANITE_TEXTURE = 'GRANITE_TEXTURE',
  GRADIENT_TEXTURE = 'GRADIENT_TEXTURE',
  IMAGE_MAP_TEXTURE = 'IMAGE_MAP_TEXTURE',
  ONION_TEXTURE = 'ONION_TEXTURE',
  LEOPARD_TEXTURE = 'LEOPARD_TEXTURE',
  BRICK_TEXTURE = 'BRICK_TEXTURE',
  MATERIAL_MAP_TEXTURE = 'MATERIAL_MAP_TEXTURE',
}

/** Java's `SolidTextureExampleColorNames.values()`, in declaration order. */
export const SOLID_TEXTURE_COLOR_NAME_VALUES: readonly SolidTextureExampleColorNames[] = [
  SolidTextureExampleColorNames.NO_TEXTURE,
  SolidTextureExampleColorNames.COLOUR_TEXTURE,
  SolidTextureExampleColorNames.BOZO_TEXTURE,
  SolidTextureExampleColorNames.MARBLE_TEXTURE,
  SolidTextureExampleColorNames.WOOD_TEXTURE,
  SolidTextureExampleColorNames.CHECKER_TEXTURE,
  SolidTextureExampleColorNames.CHECKER_TEXTURE_TEXTURE,
  SolidTextureExampleColorNames.SPOTTED_TEXTURE,
  SolidTextureExampleColorNames.AGATE_TEXTURE,
  SolidTextureExampleColorNames.GRANITE_TEXTURE,
  SolidTextureExampleColorNames.GRADIENT_TEXTURE,
  SolidTextureExampleColorNames.IMAGE_MAP_TEXTURE,
  SolidTextureExampleColorNames.ONION_TEXTURE,
  SolidTextureExampleColorNames.LEOPARD_TEXTURE,
  SolidTextureExampleColorNames.BRICK_TEXTURE,
  SolidTextureExampleColorNames.MATERIAL_MAP_TEXTURE,
];

export function solidTextureColorNameNext(
  value: SolidTextureExampleColorNames,
): SolidTextureExampleColorNames {
  const values = SOLID_TEXTURE_COLOR_NAME_VALUES;
  return values[(values.indexOf(value) + 1) % values.length]!;
}

export function solidTextureColorNamePrevious(
  value: SolidTextureExampleColorNames,
): SolidTextureExampleColorNames {
  const values = SOLID_TEXTURE_COLOR_NAME_VALUES;
  return values[(values.indexOf(value) + values.length - 1) % values.length]!;
}
