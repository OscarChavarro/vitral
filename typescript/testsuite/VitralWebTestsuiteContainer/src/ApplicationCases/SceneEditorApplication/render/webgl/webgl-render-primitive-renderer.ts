import type { Camera, Light, RendererConfiguration } from '@vitral/base';
import { WebGLGeometryRenderer } from '@vitral/webgl';
import type { RenderPrimitive } from '../render-primitive';

/**
 * Port of `render.jogl.Jogl4RenderPrimitiveRenderer`.
 *
 * Draws technology independent `RenderPrimitive`s with WebGL: the
 * implementation side, for this technology, of the feedback geometry described
 * by editors and debugging tools (see `BodyEditFeedbackProvider`). Java's two
 * overloads (one primitive, a list of them) are `draw` and `drawAll`.
 */
export class WebGLRenderPrimitiveRenderer {
  private constructor() {}

  /**
   * @param gl WebGL context
   * @param primitive primitive to draw
   * @param camera camera that views the primitive
   * @param lights lights of the scene, or null or empty for a light at the camera
   * @param quality bits of rendering configuration
   */
  static async draw(
    gl: WebGL2RenderingContext,
    primitive: RenderPrimitive,
    camera: Camera,
    lights: Iterable<Light | null> | null,
    quality: RendererConfiguration,
  ): Promise<void> {
    await WebGLGeometryRenderer.draw(gl, primitive.getGeometry(), camera, lights,
      primitive.getMaterial(), quality, null, null, primitive.getTransform());
  }

  /**
   * @param gl WebGL context
   * @param primitives primitives to draw
   * @param camera camera that views the primitives
   * @param lights lights of the scene, or null or empty for a light at the camera
   * @param quality bits of rendering configuration
   */
  static async drawAll(
    gl: WebGL2RenderingContext,
    primitives: readonly RenderPrimitive[],
    camera: Camera,
    lights: Iterable<Light | null> | null,
    quality: RendererConfiguration,
  ): Promise<void> {
    for (const primitive of primitives) {
      await WebGLRenderPrimitiveRenderer.draw(gl, primitive, camera, lights, quality);
    }
  }
}
