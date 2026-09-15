import { Polygon2D, RGBImageUncompressed } from '@vitral/base';
import { WebGLImageRenderer } from '@vitral/webgl';
import { PolygonClippingModelingTools } from '../model/polygon-clipping-modeling-tools';
import { polygonClippingOperationDisplayName } from '../model/polygon-clipping-operation';
import { polygonSurfaceTessellationModeDisplayName } from '../model/polygon-surface-tessellation-mode';
import type { PolygonClippingDebuggerModel } from '../model/polygon-clipping-debugger-model';
import type { PolygonClippingTestCase } from '../model/polygon-clipping-test-case';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/render/JoglPolygonClippingHudRenderer.java`.
 *
 * A black strip across the top of the viewport carrying six left-aligned lines
 * — the fixture and its ordinal, the boolean operation, the surface
 * tessellation mode, the source-polygon switches, the output switches and the
 * reference-frame switch — with three right-aligned ones beside the first
 * three: the loop counts of the four polygons, the number of intersection
 * pairs, and the utility keys. A build error is printed in red along the
 * bottom of the strip. The strip height, the three paddings, the line height,
 * the sixteen-pixel margins, the two colours and every message string are
 * Java's.
 *
 * The one boundary is `java.awt.Graphics2D`: an offscreen `<canvas>` replaces
 * the `BufferedImage`, `fillText` replaces `drawString`, `measureText`
 * replaces `FontMetrics.stringWidth`, and one `getImageData` replaces the
 * `getRGB` loop — the copy into the Vitral image stays a loop, because that is
 * where the RGBA samples become RGB ones. Java asks for a bold 23-point
 * `SansSerif` face and the browser is asked for the same; the glyphs are each
 * host's.
 *
 * Java reads the current viewport with `glGetIntegerv(GL_VIEWPORT)` and
 * restores it after drawing the strip, which WebGL answers the same way.
 */
export class WebGLPolygonClippingHudRenderer {
  private static readonly LINE_HEIGHT = 38;
  private static readonly HUD_TOP_PADDING = 34;
  private static readonly HUD_BOTTOM_PADDING = 20;
  private static readonly HUD_FONT = 'bold 23px sans-serif';

  private hudImage: RGBImageUncompressed | null = null;
  private hudCanvas: HTMLCanvasElement | null = null;
  private hudContext: CanvasRenderingContext2D | null = null;
  private viewportWidth = 0;
  private viewportHeight = 0;

  /**
   * Compiles the program `WebGLImageRenderer` will need and sizes the buffers,
   * without drawing; see the drawing-buffer rule recorded for the WebGL
   * example programs. Java's `init(width, height)` does the second half only.
   */
  async init(gl: WebGL2RenderingContext, width: number, height: number): Promise<void> {
    await WebGLImageRenderer.prepare(gl);
    this.updateViewportSize(width, height);
    this.ensureHudBuffers(Math.max(1, this.viewportWidth), Math.max(1, this.viewportHeight));
  }

  updateViewportSize(width: number, height: number): void {
    this.viewportWidth = Math.max(1, width);
    this.viewportHeight = Math.max(1, height);
  }

  async draw(
    gl: WebGL2RenderingContext | null,
    model: PolygonClippingDebuggerModel,
  ): Promise<void> {
    if (gl === null) {
      return;
    }

    const viewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const width: number = this.viewportWidth > 0 ? this.viewportWidth : viewport[2]!;
    const height: number = this.viewportHeight > 0 ? this.viewportHeight : viewport[3]!;
    const hudHeight: number = Math.min(
      Math.max(1, height),
      WebGLPolygonClippingHudRenderer.HUD_TOP_PADDING +
        6 * WebGLPolygonClippingHudRenderer.LINE_HEIGHT +
        WebGLPolygonClippingHudRenderer.HUD_BOTTOM_PADDING,
    );
    this.ensureHudBuffers(Math.max(1, width), hudHeight);

    const testCase: PolygonClippingTestCase = model.getCurrentTestCase();

    const testMessage: string =
      'Test [1, 2]: ' +
      testCase.name +
      ' (' +
      (model.getTestIndex() + 1) +
      '/' +
      model.getTotalTestCases() +
      ')';
    const operationMessage: string =
      'Operation [3]: ' + polygonClippingOperationDisplayName(model.getOperation());
    const tessellationMessage: string =
      'Surface tessellation [T]: ' +
      polygonSurfaceTessellationModeDisplayName(model.getPolygonSurfaceTessellationMode());
    const sourcesMessage: string =
      'Clip [C]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowClipPolygon()) +
      '  Subject [S]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowSubjectPolygon()) +
      '  Points [P]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowIntersections());
    const outputsMessage: string = WebGLPolygonClippingHudRenderer.buildOutputsMessage(model);
    const referenceFrameMessage: string =
      'Reference frame [Space]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowReferenceFrame());
    const countsMessage: string =
      'Loops C/S/I/O: ' +
      WebGLPolygonClippingHudRenderer.countLoops(model.getClipPolygon()) +
      '/' +
      WebGLPolygonClippingHudRenderer.countLoops(model.getSubjectPolygon()) +
      '/' +
      WebGLPolygonClippingHudRenderer.countLoops(model.getInnerPolygon()) +
      '/' +
      WebGLPolygonClippingHudRenderer.countLoops(model.getOuterPolygon());
    const intersectionsMessage: string =
      'Intersections: ' +
      PolygonClippingModelingTools.countPairedVertices(model.getSubjectPolygonWA());
    const utilityMessage = 'Fullscreen [F]  Snapshot [H]  Quality [F1/F2/F3]';

    const g: CanvasRenderingContext2D = this.hudContext!;
    // Java asks for text antialiasing through a RenderingHint; a 2D canvas
    // context antialiases text unconditionally and has no such switch.
    g.fillStyle = '#000000';
    g.fillRect(0, 0, width, hudHeight);
    g.fillStyle = 'rgb(255, 242, 51)';
    g.font = WebGLPolygonClippingHudRenderer.HUD_FONT;
    g.textBaseline = 'alphabetic';

    const top = WebGLPolygonClippingHudRenderer.HUD_TOP_PADDING;
    const lineHeight = WebGLPolygonClippingHudRenderer.LINE_HEIGHT;
    g.fillText(testMessage, 16, top);
    g.fillText(operationMessage, 16, top + lineHeight);
    g.fillText(tessellationMessage, 16, top + 2 * lineHeight);
    g.fillText(sourcesMessage, 16, top + 3 * lineHeight);
    g.fillText(outputsMessage, 16, top + 4 * lineHeight);
    g.fillText(referenceFrameMessage, 16, top + 5 * lineHeight);

    WebGLPolygonClippingHudRenderer.drawTopRight(g, width, countsMessage, top);
    WebGLPolygonClippingHudRenderer.drawTopRight(g, width, intersectionsMessage, top + lineHeight);
    WebGLPolygonClippingHudRenderer.drawTopRight(g, width, utilityMessage, top + 2 * lineHeight);

    if (model.isErrorState()) {
      g.fillStyle = 'rgb(255, 38, 38)';
      g.fillText(model.getErrorMessage(), 16, hudHeight - 10);
    }

    const rendered: Uint8ClampedArray = g.getImageData(0, 0, width, hudHeight).data;
    const hudImage: RGBImageUncompressed = this.hudImage!;
    for (let y = 0; y < hudHeight; y++) {
      for (let x = 0; x < width; x++) {
        const base: number = (y * width + x) * 4;
        hudImage.putPixel(x, y, rendered[base]!, rendered[base + 1]!, rendered[base + 2]!);
      }
    }

    WebGLImageRenderer.unload(gl, hudImage);
    gl.viewport(viewport[0]!, viewport[1]! + viewport[3]! - hudHeight, width, hudHeight);
    await WebGLImageRenderer.draw(gl, hudImage);
    gl.viewport(viewport[0]!, viewport[1]!, viewport[2]!, viewport[3]!);
  }

  dispose(gl: WebGL2RenderingContext | null): void {
    if (gl !== null && this.hudImage !== null) {
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
      this.hudImage.getXSize() === width &&
      this.hudImage.getYSize() === height
    ) {
      return;
    }
    this.hudImage = new RGBImageUncompressed();
    this.hudImage.init(width, height);
    this.hudCanvas = document.createElement('canvas');
    this.hudCanvas.width = width;
    this.hudCanvas.height = height;
    const context = this.hudCanvas.getContext('2d', { willReadFrequently: true });
    if (context === null) {
      throw new Error('PolygonClippingExample requires a 2D canvas context for its HUD.');
    }
    this.hudContext = context;
  }

  private static drawTopRight(
    g: CanvasRenderingContext2D,
    width: number,
    text: string,
    baselineY: number,
  ): void {
    const textWidth: number = g.measureText(text).width;
    const x: number = width - 16 - textWidth;
    g.fillText(text, Math.max(16, x), baselineY);
  }

  private static countLoops(polygon: Polygon2D | null): number {
    if (polygon === null) {
      return 0;
    }
    return polygon.loops.length;
  }

  private static onOff(value: boolean): string {
    return value ? 'ON' : 'OFF';
  }

  private static buildOutputsMessage(model: PolygonClippingDebuggerModel): string {
    if (model.getOperation() === 'INTERSECTION') {
      return (
        'Inner [I]: ' +
        WebGLPolygonClippingHudRenderer.onOff(model.isShowInnerPolygon()) +
        '  Outer [O]: ' +
        WebGLPolygonClippingHudRenderer.onOff(model.isShowOuterPolygon()) +
        '  Fill [G]: ' +
        WebGLPolygonClippingHudRenderer.onOff(model.isShowFilledPolygons())
      );
    }
    return (
      'Result [I]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowInnerPolygon()) +
      '  Secondary [O]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowOuterPolygon()) +
      '  Fill [G]: ' +
      WebGLPolygonClippingHudRenderer.onOff(model.isShowFilledPolygons())
    );
  }
}
