import { RGBImageUncompressed, ZBuffer } from '@vitral/base';
import { HtmlImageRenderer } from '@vitral/webgl';
import type { FrameBufferSource } from '../frame-buffer-source';

/**
 * Port of `render.awt.AwtFrameBufferSource`.
 *
 * Frame buffer of the canvas 2D presentation: an area of the off-screen canvas
 * where the viewport set is drawn. A 2D context draws only in 2D, so there is
 * no depth buffer: reading it gives a buffer at the far distance everywhere.
 */
export class HtmlFrameBufferSource implements FrameBufferSource {
  private readonly frame: CanvasRenderingContext2D;
  private readonly x: number;
  private readonly y: number;
  private readonly width: number;
  private readonly height: number;

  /**
   * @param frame the 2D context of the off-screen canvas of the viewport set
   * @param x left pixel of the area to read
   * @param y top pixel of the area to read
   * @param width width of the area to read
   * @param height height of the area to read
   */
  constructor(
    frame: CanvasRenderingContext2D,
    x: number,
    y: number,
    width: number,
    height: number,
  ) {
    this.frame = frame;
    this.x = Math.max(0, x);
    this.y = Math.max(0, y);
    this.width = Math.max(1, Math.min(width, frame.canvas.width - this.x));
    this.height = Math.max(1, Math.min(height, frame.canvas.height - this.y));
  }

  readColor(): RGBImageUncompressed {
    const image: RGBImageUncompressed = new RGBImageUncompressed();

    HtmlImageRenderer.importFromImageData(
      this.frame.getImageData(this.x, this.y, this.width, this.height),
      image,
    );
    return image;
  }

  readDepth(): Promise<ZBuffer> {
    const depth: ZBuffer = new ZBuffer(this.width, this.height);
    let i: number;
    let j: number;

    for (j = 0; j < this.height; j++) {
      for (i = 0; i < this.width; i++) {
        depth.setZ(i, j, 1.0);
      }
    }
    return Promise.resolve(depth);
  }
}
