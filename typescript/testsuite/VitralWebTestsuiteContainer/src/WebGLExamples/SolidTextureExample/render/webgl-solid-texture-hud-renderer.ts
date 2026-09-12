import { RGBImageUncompressed } from '@vitral/base';
import { WebGLImageRenderer } from '@vitral/webgl';
import type { SolidTextureModel } from '../model/solid-texture-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/render/Jogl4SolidTextureHudRenderer.java`.
 *
 * The heads-up display: a black strip across the top of the viewport carrying
 * the four lines of state the keyboard bindings change. Java rasterizes those
 * lines with `java.awt.Graphics2D` onto a `BufferedImage`, copies the result
 * pixel by pixel into a `RGBImageUncompressed`, and hands that to
 * `Jogl4ImageRenderer` after forcing a re-upload.
 *
 * The browser counterpart of `Graphics2D` is a 2D canvas context, which is the
 * one boundary this class crosses: an offscreen `<canvas>` replaces the
 * `BufferedImage`, `fillText` replaces `drawString` on the same baselines with
 * the same bold 18-pixel sans-serif face, and a single `getImageData` replaces
 * the `getRGB` loop — the copy into the Vitral image stays a loop, because that
 * is where the RGBA samples become RGB ones. Text metrics are the platform's in
 * both cases, so the glyphs are the host's, not Java's.
 *
 * Java reads the current viewport with `glGetIntegerv(GL_VIEWPORT)`; WebGL
 * answers the same query, and the strip is placed and the viewport restored
 * exactly as Java does.
 */
export class WebGLSolidTextureHudRenderer {
  private static readonly HUD_HEIGHT = 122;
  private static readonly HUD_LEFT = 16;
  private static readonly HUD_BASELINE_1 = 28;
  private static readonly HUD_BASELINE_2 = 54;
  private static readonly HUD_BASELINE_3 = 80;
  private static readonly HUD_BASELINE_4 = 106;
  private static readonly HUD_FONT = 'bold 18px sans-serif';

  private hudImage: RGBImageUncompressed | null = null;
  private hudCanvas: HTMLCanvasElement | null = null;
  private hudContext: CanvasRenderingContext2D | null = null;
  private hudWidth = 0;
  private hudHeight = 0;

  constructor(private readonly model: SolidTextureModel) {}

  async draw(gl: WebGL2RenderingContext): Promise<void> {
    const viewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const targetWidth: number = Math.max(1, viewport[2]!);
    const targetHeight: number = Math.min(
      WebGLSolidTextureHudRenderer.HUD_HEIGHT,
      Math.max(1, viewport[3]!),
    );
    this.ensureHudBuffers(targetWidth, targetHeight);

    const g: CanvasRenderingContext2D = this.hudContext!;
    // Java asks for text antialiasing through a RenderingHint; a 2D canvas
    // context antialiases text unconditionally and has no such switch.
    g.fillStyle = '#000000';
    g.fillRect(0, 0, this.hudWidth, this.hudHeight);
    g.fillStyle = '#ffffff';
    g.font = WebGLSolidTextureHudRenderer.HUD_FONT;
    g.textBaseline = 'alphabetic';
    g.fillText(
      'Operation mode [1]: ' + this.model.getOperationMode(),
      WebGLSolidTextureHudRenderer.HUD_LEFT,
      WebGLSolidTextureHudRenderer.HUD_BASELINE_1,
    );
    g.fillText(
      'Texture side [2, 3]: ' + this.model.getSolidTextureSize(),
      WebGLSolidTextureHudRenderer.HUD_LEFT,
      WebGLSolidTextureHudRenderer.HUD_BASELINE_2,
    );
    g.fillText(
      'Selected texture [4, 5]: ' + this.model.getSelectedSolidTexture(),
      WebGLSolidTextureHudRenderer.HUD_LEFT,
      WebGLSolidTextureHudRenderer.HUD_BASELINE_3,
    );
    g.fillText(
      'Animation [r]: ' + (this.model.isAnimationEnabled() ? 'PLAY' : 'STOP'),
      WebGLSolidTextureHudRenderer.HUD_LEFT,
      WebGLSolidTextureHudRenderer.HUD_BASELINE_4,
    );

    const rendered: Uint8ClampedArray = g.getImageData(0, 0, this.hudWidth, this.hudHeight).data;
    const hudImage: RGBImageUncompressed = this.hudImage!;
    for (let y = 0; y < this.hudHeight; y++) {
      for (let x = 0; x < this.hudWidth; x++) {
        const base: number = (y * this.hudWidth + x) * 4;
        hudImage.putPixel(x, y, rendered[base]!, rendered[base + 1]!, rendered[base + 2]!);
      }
    }

    WebGLImageRenderer.unload(gl, hudImage);

    gl.viewport(
      viewport[0]!,
      viewport[1]! + viewport[3]! - this.hudHeight,
      this.hudWidth,
      this.hudHeight,
    );
    await WebGLImageRenderer.draw(gl, hudImage);
    gl.viewport(viewport[0]!, viewport[1]!, viewport[2]!, viewport[3]!);
  }

  /**
   * Compiles the program `WebGLImageRenderer` will need, without drawing; see
   * the drawing-buffer rule recorded for the WebGL example programs.
   */
  async prepare(gl: WebGL2RenderingContext): Promise<void> {
    await WebGLImageRenderer.prepare(gl);
  }

  dispose(gl: WebGL2RenderingContext): void {
    if (this.hudImage !== null) {
      WebGLImageRenderer.unload(gl, this.hudImage);
    }
    this.hudImage = null;
    this.hudCanvas = null;
    this.hudContext = null;
  }

  private ensureHudBuffers(width: number, height: number): void {
    if (
      this.hudImage !== null &&
      this.hudContext !== null &&
      this.hudWidth === width &&
      this.hudHeight === height
    ) {
      return;
    }
    this.hudWidth = width;
    this.hudHeight = height;
    this.hudImage = new RGBImageUncompressed();
    this.hudImage.init(this.hudWidth, this.hudHeight);
    this.hudCanvas = document.createElement('canvas');
    this.hudCanvas.width = this.hudWidth;
    this.hudCanvas.height = this.hudHeight;
    const context = this.hudCanvas.getContext('2d', { willReadFrequently: true });
    if (context === null) {
      throw new Error('SolidTextureExample requires a 2D canvas context for its HUD.');
    }
    this.hudContext = context;
  }
}
