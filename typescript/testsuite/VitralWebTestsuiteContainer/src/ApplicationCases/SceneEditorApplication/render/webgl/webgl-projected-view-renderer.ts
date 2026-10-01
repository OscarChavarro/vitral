import type { Camera, RendererConfiguration, SimpleBodyGroup, ZBuffer } from '@vitral/base';
import { WebGLFrameBufferReader, WebGLOffscreenFrameBuffer } from '@vitral/webgl';
import type { ProjectedViewRenderer } from '../projected-view-renderer';
import { WebGLSceneRenderer } from './webgl-scene-renderer';

/**
 * Port of `render.jogl.Jogl4ProjectedViewRenderer`.
 *
 * Renders with WebGL the projected views used by `ProjectedViewsDebugger`.
 * Java draws them into the window's frame buffer (the next frame repaints
 * it); here they are drawn into an offscreen frame buffer of the size of the
 * view, whose depth texture can be read (see `WebGLFrameBufferReader`). The
 * frame buffer that was bound is bound again afterwards.
 */
export class WebGLProjectedViewRenderer implements ProjectedViewRenderer {
  private readonly gl: WebGL2RenderingContext;
  private readonly target: WebGLOffscreenFrameBuffer;

  /**
   * @param gl context where views are rendered
   */
  constructor(gl: WebGL2RenderingContext) {
    this.gl = gl;
    this.target = new WebGLOffscreenFrameBuffer(gl);
  }

  async renderDepth(
    bodies: SimpleBodyGroup | null,
    camera: Camera,
    quality: RendererConfiguration,
    xSize: number,
    ySize: number,
  ): Promise<ZBuffer> {
    const gl: WebGL2RenderingContext = this.gl;
    const previousFrameBuffer: WebGLFramebuffer | null = gl.getParameter(gl.FRAMEBUFFER_BINDING) as WebGLFramebuffer | null;
    const previousViewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;

    try {
      this.target.bind(xSize, ySize);
      gl.viewport(0, 0, xSize, ySize);

      //-----------------------------------------------------------------
      gl.clearColor(0.5, 0.5, 0.9, 1);
      gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
      gl.enable(gl.DEPTH_TEST);
      gl.depthMask(true);

      if (bodies !== null) {
        await WebGLSceneRenderer.drawBodyGroup(gl, bodies, camera, null, quality);
      }

      gl.flush();

      //- Obtain ZBuffer ------------------------------------------------
      return await WebGLFrameBufferReader.readDepth(gl, this.target.getDepthTexture()!);
    } finally {
      gl.bindFramebuffer(gl.FRAMEBUFFER, previousFrameBuffer);
      gl.viewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);
    }
  }

  /**
   * Releases the offscreen frame buffer.
   */
  dispose(): void {
    this.target.release();
  }
}
