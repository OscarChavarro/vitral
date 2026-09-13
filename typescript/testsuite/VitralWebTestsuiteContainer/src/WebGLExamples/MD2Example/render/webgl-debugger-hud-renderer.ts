import { RGBImageUncompressed } from '@vitral/base';
import { WebGLImageRenderer } from '@vitral/webgl';
import type { DebuggerModel } from '../model/debugger-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MD2Example/src/render/Jogl4DebuggerHudRenderer.java`.
 *
 * A black strip across the top of the viewport carrying two lines on one
 * baseline: the selected animation on the left and the object the XYZ keys
 * move on the right, the second right-aligned by its measured width. Java
 * rasterizes both with `java.awt.Graphics2D` onto a `BufferedImage`, copies the
 * result pixel by pixel into a `RGBImageUncompressed`, and hands that to
 * `Jogl4ImageRenderer` after forcing a re-upload.
 *
 * The browser counterpart of `Graphics2D` is a 2D canvas context, which is the
 * one boundary this class crosses: an offscreen `<canvas>` replaces the
 * `BufferedImage`, `fillText` replaces `drawString` on the same baseline with
 * the same bold 20-pixel sans-serif face, `measureText` replaces
 * `getFontMetrics().stringWidth`, and a single `getImageData` replaces the
 * `getRGB` loop — the copy into the Vitral image stays a loop, because that is
 * where the RGBA samples become RGB ones. Text metrics are the platform's in
 * both cases, so the glyphs are the host's, not Java's.
 *
 * Java's `init` and `updateViewportSize` are empty, since the buffers are
 * validated on each draw; both are kept, with their comments.
 */
export class WebGLDebuggerHudRenderer {
  private static readonly HUD_HEIGHT = 64;
  private static readonly HUD_LEFT = 16;
  private static readonly HUD_BASELINE = 42;
  private static readonly HUD_FONT = 'bold 20px sans-serif';

  private hudImage: RGBImageUncompressed | null = null;
  private hudCanvas: HTMLCanvasElement | null = null;
  private hudContext: CanvasRenderingContext2D | null = null;
  private hudWidth = 0;
  private hudHeight = 0;

  constructor(private readonly model: DebuggerModel) {}

  init(_gl: WebGL2RenderingContext): void {
    // HUD buffers are lazily created with current viewport dimensions.
  }

  updateViewportSize(_width: number, _height: number): void {
    // HUD buffers are validated on each draw.
  }

  /**
   * Compiles the program `WebGLImageRenderer` will need, without drawing; see
   * the drawing-buffer rule recorded for the WebGL example programs. No Java
   * counterpart, whose shader compilation happens inside the draw call.
   */
  async prepare(gl: WebGL2RenderingContext): Promise<void> {
    await WebGLImageRenderer.prepare(gl);
  }

  async draw(gl: WebGL2RenderingContext): Promise<void> {
    const viewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const targetWidth: number = Math.max(1, viewport[2]!);
    const targetHeight: number = Math.min(
      WebGLDebuggerHudRenderer.HUD_HEIGHT,
      Math.max(1, viewport[3]!),
    );
    this.ensureHudBuffers(targetWidth, targetHeight);

    const md2Mesh = this.model.getMd2Mesh();
    const totalAnimations: number = this.getTotalAnimations();
    const selectedAnimation: number = Math.min(
      md2Mesh.getCurrentAnimationInd() + 1,
      totalAnimations,
    );
    const animationName: string = md2Mesh.getAnimationName(md2Mesh.getCurrentAnimationInd());
    const msg: string =
      'Selected animation [1, 2]: ' +
      selectedAnimation +
      '/' +
      totalAnimations +
      ' ' +
      animationName.toUpperCase();
    const selectedObjectMsg: string =
      'Selected object to move with XYZ [3, 4]: ' + this.getSelectedObjectName();

    const g: CanvasRenderingContext2D = this.hudContext!;
    // Java asks for text antialiasing through a RenderingHint; a 2D canvas
    // context antialiases text unconditionally and has no such switch.
    g.fillStyle = '#000000';
    g.fillRect(0, 0, this.hudWidth, this.hudHeight);
    g.fillStyle = '#ffffff';
    g.font = WebGLDebuggerHudRenderer.HUD_FONT;
    g.textBaseline = 'alphabetic';
    g.fillText(msg, WebGLDebuggerHudRenderer.HUD_LEFT, WebGLDebuggerHudRenderer.HUD_BASELINE);
    const rightX: number = Math.max(
      WebGLDebuggerHudRenderer.HUD_LEFT,
      this.hudWidth - WebGLDebuggerHudRenderer.HUD_LEFT - g.measureText(selectedObjectMsg).width,
    );
    g.fillText(selectedObjectMsg, rightX, WebGLDebuggerHudRenderer.HUD_BASELINE);

    const rendered: Uint8ClampedArray = g.getImageData(0, 0, this.hudWidth, this.hudHeight).data;
    const hudImage: RGBImageUncompressed = this.hudImage!;
    for (let y = 0; y < this.hudHeight; y++) {
      for (let x = 0; x < this.hudWidth; x++) {
        const base: number = (y * this.hudWidth + x) * 4;
        hudImage.putPixel(x, y, rendered[base]!, rendered[base + 1]!, rendered[base + 2]!);
      }
    }

    WebGLImageRenderer.unload(gl, hudImage);

    const hudX: number = viewport[0]!;
    const hudY: number = viewport[1]! + viewport[3]! - this.hudHeight;
    gl.viewport(hudX, hudY, this.hudWidth, this.hudHeight);
    await WebGLImageRenderer.draw(gl, hudImage);
    gl.viewport(viewport[0]!, viewport[1]!, viewport[2]!, viewport[3]!);
  }

  /**
   * Java's `dispose` is a no-op, since the texture is recreated on each draw
   * via unload plus draw. The image this port allocated is released here all
   * the same, because a module torn down inside a live page leaves its context
   * behind, which a JVM shutting down does not.
   */
  dispose(gl: WebGL2RenderingContext): void {
    if (this.hudImage !== null) {
      WebGLImageRenderer.unload(gl, this.hudImage);
    }
    this.hudImage = null;
    this.hudCanvas = null;
    this.hudContext = null;
  }

  private getTotalAnimations(): number {
    const md2Mesh = this.model.getMd2Mesh();
    const animStartEnd: Int16Array = new Int16Array(2);
    md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
    return Math.max(1, md2Mesh.getMaxAnimationInd() + 1);
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
      throw new Error('MD2Example requires a 2D canvas context for its HUD.');
    }
    this.hudContext = context;
  }

  private getSelectedObjectName(): string {
    if (this.model.getSelectedObject() < 0) {
      return 'Camera';
    }
    return 'Light ' + (this.model.getSelectedObject() + 1);
  }
}
