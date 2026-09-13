import { RendererConfiguration, RGBImageUncompressed } from '@vitral/base';
import { WebGLImageRenderer } from '@vitral/webgl';
import type { ShadersModel } from '../model/shaders-model';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/ShadersExample/src/render/JogHudRenderer.java`.
 *
 * A black strip across the top of the viewport carrying two lines: on the
 * first, the mesh resolution and its triangle count when the GPU path is
 * drawing, or the single word `Raytracing` when the CPU one is, with the HUD
 * key right-aligned beside it; on the second, which path is drawing, with the
 * Cook-Torrance material name right-aligned beside it whenever that shading
 * model is selected. The two baselines, the left margin, the strip height and
 * the right-alignment arithmetic are Java's.
 *
 * The one boundary is `java.awt.Graphics2D`: an offscreen `<canvas>` replaces
 * the `BufferedImage`, `fillText` replaces `drawString`, `measureText`
 * replaces `FontMetrics.stringWidth`, and one `getImageData` replaces the
 * `getRGB` loop — the copy into the Vitral image stays a loop, because that is
 * where the RGBA samples become RGB ones. Java asks for a monospaced 16-point
 * face and the browser is asked for the same; the glyphs are each host's.
 *
 * Java reads the current viewport with `glGetIntegerv(GL_VIEWPORT)` and
 * restores it after drawing the strip, which WebGL answers the same way.
 */
export class WebGLHudRenderer {
  private static readonly HUD_HEIGHT = 64;
  private static readonly HUD_LEFT = 10;
  private static readonly HUD_BASELINE_1 = 24;
  private static readonly HUD_BASELINE_2 = 46;
  private static readonly HUD_FONT = '16px monospace';

  private hudImage: RGBImageUncompressed | null = null;
  private hudCanvas: HTMLCanvasElement | null = null;
  private hudContext: CanvasRenderingContext2D | null = null;
  private hudWidth = 0;
  private hudHeight = 0;

  async draw(gl: WebGL2RenderingContext | null, model: ShadersModel | null): Promise<void> {
    if (gl === null || model === null) {
      return;
    }
    if (!model.isShowHud()) {
      return;
    }
    const viewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const targetHudWidth: number = Math.max(1, viewport[2]!);
    const targetHudHeight: number = Math.min(
      WebGLHudRenderer.HUD_HEIGHT,
      Math.max(1, viewport[3]!),
    );
    this.ensureHudBuffers(targetHudWidth, targetHudHeight);

    let line1: string;
    if (model.getRenderingMode() === 'OPENGL_4_1') {
      const meridians: number = model.getSphereMeridians();
      const parallels: number = model.getSphereParallels();
      const triangles: number = Math.max(0, (parallels - 1) * meridians * 2);
      line1 =
        'Number of meridians: ' +
        meridians +
        ' Number of parallels: ' +
        parallels +
        ' Number of triangles: ' +
        triangles;
    } else {
      line1 = 'Raytracing';
    }

    const line2: string =
      model.getRenderingMode() === 'OPENGL_4_1' ? 'Mode [.]: GPU' : 'Mode [.]: CPU';
    const line2Right = 'Show HUD [h]';
    let lineCookMaterial: string | null = null;
    if (model.getQuality().getShadingType() === RendererConfiguration.SHADING_TYPE_COOK_TERRANCE) {
      lineCookMaterial = 'SimpleMaterial [m]: ' + model.getCookTorranceMaterialLabel();
    }

    const g: CanvasRenderingContext2D = this.hudContext!;
    // Java asks for text antialiasing through a RenderingHint; a 2D canvas
    // context antialiases text unconditionally and has no such switch.
    g.fillStyle = '#000000';
    g.fillRect(0, 0, this.hudWidth, this.hudHeight);
    g.fillStyle = '#ffffff';
    g.font = WebGLHudRenderer.HUD_FONT;
    g.textBaseline = 'alphabetic';
    g.fillText(line1, WebGLHudRenderer.HUD_LEFT, WebGLHudRenderer.HUD_BASELINE_1);
    g.fillText(line2, WebGLHudRenderer.HUD_LEFT, WebGLHudRenderer.HUD_BASELINE_2);
    const rightWidth: number = g.measureText(line2Right).width;
    const rightX: number = Math.max(
      WebGLHudRenderer.HUD_LEFT,
      this.hudWidth - rightWidth - WebGLHudRenderer.HUD_LEFT,
    );
    g.fillText(line2Right, rightX, WebGLHudRenderer.HUD_BASELINE_1);
    if (lineCookMaterial !== null) {
      const materialWidth: number = g.measureText(lineCookMaterial).width;
      const materialX: number = Math.max(
        WebGLHudRenderer.HUD_LEFT,
        this.hudWidth - materialWidth - WebGLHudRenderer.HUD_LEFT,
      );
      g.fillText(lineCookMaterial, materialX, WebGLHudRenderer.HUD_BASELINE_2);
    }

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

    // Save/restore viewport so HUD placement does not affect scene rendering.
    gl.viewport(hudX, hudY, this.hudWidth, this.hudHeight);
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

  /**
   * Java's `dispose()` is a no-op, because its texture is recreated on every
   * draw by `unload` + `draw`. The texture object still has to be released
   * here, since a page outlives the module that made it.
   */
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
      throw new Error('ShadersExample requires a 2D canvas context for its HUD.');
    }
    this.hudContext = context;
  }
}
