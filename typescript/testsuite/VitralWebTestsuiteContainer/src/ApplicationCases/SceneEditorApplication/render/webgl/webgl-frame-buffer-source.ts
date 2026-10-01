import type { RGBImageUncompressed, ZBuffer } from '@vitral/base';
import { WebGLFrameBufferReader, type WebGLOffscreenFrameBuffer } from '@vitral/webgl';
import type { FrameBufferSource } from '../frame-buffer-source';

/**
 * Port of `render.jogl.Jogl4FrameBufferSource`.
 *
 * Frame buffers of a WebGL context. The frame is drawn into a
 * `WebGLOffscreenFrameBuffer` (WebGL can only read a depth that was drawn into
 * a depth texture), whose depth texture is read here.
 */
export class WebGLFrameBufferSource implements FrameBufferSource {
  private readonly gl: WebGL2RenderingContext;
  private readonly frameBuffer: WebGLOffscreenFrameBuffer;

  /**
   * @param gl context whose current frame buffers are read
   * @param frameBuffer offscreen frame buffer the frame is drawn into
   */
  constructor(gl: WebGL2RenderingContext, frameBuffer: WebGLOffscreenFrameBuffer) {
    this.gl = gl;
    this.frameBuffer = frameBuffer;
  }

  readColor(): RGBImageUncompressed {
    return WebGLFrameBufferReader.readColor(this.gl);
  }

  async readDepth(): Promise<ZBuffer> {
    const depthTexture: WebGLTexture | null = this.frameBuffer.getDepthTexture();
    if (depthTexture === null) {
      throw new Error('The offscreen frame buffer has no depth texture yet');
    }
    return WebGLFrameBufferReader.readDepth(this.gl, depthTexture);
  }
}
