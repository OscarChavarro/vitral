/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/model/PolygonSurfaceTessellationMode.java`.
 *
 * The two ways this program can fill a clipping result: the tessellator the
 * polygon renderer carries, and the monotone decomposition of
 * `render.JoglTriangularRenderer`. The `GLU` name is kept, because it names the
 * Java mode and appears in the HUD as the Java program prints it, even though
 * the browser reaches that region through
 * `_Polygon2DOddWindingTessellator` — see `WebGLPolygon2DRenderer` for why the
 * covered region is what the two have to agree on.
 */
export type PolygonSurfaceTessellationMode = 'GLU' | 'MONOTONE_DECOMPOSITION';

const VALUES: readonly PolygonSurfaceTessellationMode[] = ['GLU', 'MONOTONE_DECOMPOSITION'];

const DISPLAY_NAMES: Readonly<Record<PolygonSurfaceTessellationMode, string>> = {
  GLU: 'GLU',
  MONOTONE_DECOMPOSITION: 'Monotone decomposition',
};

export function polygonSurfaceTessellationModeDisplayName(
  mode: PolygonSurfaceTessellationMode,
): string {
  return DISPLAY_NAMES[mode];
}

export function nextPolygonSurfaceTessellationMode(
  mode: PolygonSurfaceTessellationMode,
): PolygonSurfaceTessellationMode {
  const nextIndex = (VALUES.indexOf(mode) + 1) % VALUES.length;
  return VALUES[nextIndex]!;
}
