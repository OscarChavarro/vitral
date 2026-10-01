import type { SimpleBody } from '@vitral/base';
import type { RenderPrimitive } from './render-primitive';

/**
 * Port of `render.BodyEditFeedbackProvider`.
 *
 * A body editor (i.e. the modify panel of the GUI) as seen by the renderers:
 * it tells which body is under edition and which feedback geometry must be
 * presented over it (handles, bounds, control points...), described as
 * technology independent `RenderPrimitive`s.
 *
 * This is the abstraction side of a bridge: editors describe what to present
 * and each rendering technology (WebGL, OpenGL 4, Vulkan...) implements how to
 * draw it, so editors do not depend on any rendering technology and new
 * technologies do not need code for each editor.
 */
export interface BodyEditFeedbackProvider {
  /**
   * @return the body under edition, or null if there is none
   */
  getTarget(): SimpleBody | null;

  /**
   * @return the feedback geometry to present over the body under edition, in
   * world coordinates (empty if the editor has none)
   */
  buildEditFeedback(): RenderPrimitive[];
}
