import type { Geometry, Matrix4x4d, SimpleMaterial } from '@vitral/base';

/**
 * Port of `render.RenderPrimitive`.
 *
 * One geometry to draw, with its transformation and material. Lists of
 * primitives are the technology independent way for the application to
 * describe feedback geometry (debug entities, editor overlays): each rendering
 * technology draws them with its own renderer (i.e.
 * `WebGLRenderPrimitiveRenderer`), so they are portable to new technologies by
 * writing only that renderer.
 */
export class RenderPrimitive {
  private readonly geometry: Geometry;
  private readonly transform: Matrix4x4d;
  private readonly material: SimpleMaterial;

  /**
   * @param geometry geometry to draw
   * @param transform transformation from geometry to world coordinates
   * @param material material of the geometry
   */
  constructor(geometry: Geometry, transform: Matrix4x4d, material: SimpleMaterial) {
    this.geometry = geometry;
    this.transform = transform;
    this.material = material;
  }

  getGeometry(): Geometry {
    return this.geometry;
  }

  getTransform(): Matrix4x4d {
    return this.transform;
  }

  getMaterial(): SimpleMaterial {
    return this.material;
  }
}
