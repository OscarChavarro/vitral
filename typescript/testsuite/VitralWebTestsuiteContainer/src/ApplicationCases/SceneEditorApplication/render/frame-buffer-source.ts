import type { RGBImageUncompressed, ZBuffer } from '@vitral/base';

/**
 * Port of `render.FrameBufferSource`.
 *
 * Access to the frame buffers of the rendering technology in use, so the
 * services that capture frames (see `FrameCaptureService`) do not depend on it.
 * Reads take the area currently set for drawing in the frame buffer.
 *
 * Reading the depth is asynchronous: WebGL reads it through a shader program
 * whose source arrives over `fetch` (see `WebGLFrameBufferReader`).
 */
export interface FrameBufferSource {
  /**
   * @return the content of the color buffer
   */
  readColor(): RGBImageUncompressed;

  /**
   * @return the content of the depth buffer
   */
  readDepth(): Promise<ZBuffer>;
}
