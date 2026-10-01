import type { Camera, RendererConfiguration, SimpleBodyGroup, ZBuffer } from '@vitral/base';

/**
 * Port of `render.ProjectedViewRenderer`.
 *
 * Renders the projected views used by `ProjectedViewsDebugger`, with the
 * rendering technology in use (asynchronously, as WebGL drawing is).
 */
export interface ProjectedViewRenderer {
  /**
   * Renders a group of bodies alone, over an empty background.
   * @param bodies bodies to render
   * @param camera camera taking the view
   * @param quality rendering configuration
   * @param xSize width of the view, in pixels
   * @param ySize height of the view, in pixels
   * @return the depth buffer of the rendered view
   */
  renderDepth(
    bodies: SimpleBodyGroup,
    camera: Camera,
    quality: RendererConfiguration,
    xSize: number,
    ySize: number,
  ): Promise<ZBuffer>;
}
