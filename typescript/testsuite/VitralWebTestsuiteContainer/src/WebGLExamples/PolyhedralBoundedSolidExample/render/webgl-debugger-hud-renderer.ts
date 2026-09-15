import {
  PolyhedralBoundedSolidStatistics,
  RGBAImageUncompressed,
  type PolyhedralBoundedSolid,
} from '@vitral/base';
import { WebGLImageRenderer, WebGLPolyhedralBoundedSolidDebugHUDRenderer } from '@vitral/webgl';
import { appelDisplayModeLabel } from '../models/appel-display-mode';
import { csgOperationLabel } from '../models/csg-operation-names';
import {
  csgSampleDisplayIndex,
  csgSampleLabel,
  csgSampleTotalSamples,
} from '../models/csg-sample-names';
import type { DebuggerModel } from '../models/debugger-model';
import {
  solidModelDisplayIndex,
  solidModelTotalModels,
  solidModelUsesCsgDebugControls,
} from '../models/solid-model-names';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/render/Jogl4DebuggerHudRenderer.java`.
 *
 * A viewport-sized transparent overlay: a translucent black block across the
 * top holding the face selection, the selected model and its ordinal, the CSG
 * sample, motif and operation lines when the model uses them, the reference
 * frame and hidden-line switches, and the two subdivision counts on the
 * right; the build error in red along the bottom; the CSG statistics issues
 * above it when a set operation ran and reported any; and, drawn by
 * `WebGLPolyhedralBoundedSolidDebugHUDRenderer`, the selected face's id and
 * the vertex ids. The block height, the paddings, the line height, the colours,
 * the two faces and every message string are Java's.
 *
 * The boundary is `java.awt.Graphics2D`, crossed the way the container's other
 * HUDs cross it: an offscreen `<canvas>` replaces the `BufferedImage`, a
 * cleared transparent canvas replaces `clearRect` over a transparent
 * background, `fillRect`/`fillText` replace their Java namesakes on the same
 * baselines, and `measureText` replaces `getStringBounds`. Java asks for bold
 * 18-point and plain 12-point `SansSerif`, which at Java2D's 72 dpi are 18 and
 * 12 pixels, and the browser is asked for the same; the glyphs are each
 * host's. `copyBufferedOverlayToImage` reads one `getImageData` instead of a
 * `getRGB` per pixel and copies it a row at a time, which stores in the
 * `RGBAImageUncompressed` the bytes Java's `putPixel` loop stores.
 */
export class WebGLDebuggerHudRenderer {
  private static readonly LINE_HEIGHT = 34;
  private static readonly HUD_TOP_PADDING = 28;
  private static readonly HUD_LEFT_PADDING = 16;
  private static readonly HUD_BOTTOM_PADDING = 16;
  private static readonly HUD_FONT = 'bold 18px sans-serif';
  private static readonly LABEL_FONT = '12px sans-serif';

  private overlayImage: RGBAImageUncompressed | null;
  private bufferedOverlay: CanvasRenderingContext2D | null;
  private viewportWidth: number;
  private viewportHeight: number;

  constructor(private readonly model: DebuggerModel) {
    this.overlayImage = null;
    this.bufferedOverlay = null;
    this.viewportWidth = 0;
    this.viewportHeight = 0;
  }

  /**
   * Java's `init(drawable)`, which sizes the overlay. The program
   * `WebGLImageRenderer` draws with is also compiled here, without drawing;
   * see the drawing-buffer rule recorded for the WebGL example programs.
   */
  async init(gl: WebGL2RenderingContext, width: number, height: number): Promise<void> {
    await WebGLImageRenderer.prepare(gl);
    this.updateViewportSize(width, height);
  }

  updateViewportSize(width: number, height: number): void {
    this.viewportWidth = Math.max(1, width);
    this.viewportHeight = Math.max(1, height);
  }

  async draw(gl: WebGL2RenderingContext | null): Promise<void> {
    if (gl === null || this.model === null) {
      return;
    }

    const viewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const width: number = this.viewportWidth > 0 ? this.viewportWidth : Math.max(1, viewport[2]!);
    const height: number =
      this.viewportHeight > 0 ? this.viewportHeight : Math.max(1, viewport[3]!);
    this.updateViewportSize(width, height);
    this.ensureOverlayBuffers(width, height);

    const g: CanvasRenderingContext2D = this.bufferedOverlay!;
    // Java asks for text antialiasing and quality rendering through
    // RenderingHints; a 2D canvas context antialiases unconditionally.
    g.clearRect(0, 0, width, height);
    g.textBaseline = 'alphabetic';

    this.drawHudText(g, width, height);
    this.drawSelectedFaceLabel(g);
    this.drawDebugVertexLabels(g);

    this.copyBufferedOverlayToImage(width, height);
    WebGLImageRenderer.unload(gl, this.overlayImage);
    await WebGLImageRenderer.draw(gl, this.overlayImage);
  }

  dispose(gl: WebGL2RenderingContext | null): void {
    if (gl !== null && this.overlayImage !== null) {
      WebGLImageRenderer.unload(gl, this.overlayImage);
    }
    this.overlayImage = null;
    this.bufferedOverlay = null;
  }

  private drawHudText(g: CanvasRenderingContext2D, width: number, height: number): void {
    const model: DebuggerModel = this.model;
    let nextLeftLine = 2;
    let blockHeight: number =
      WebGLDebuggerHudRenderer.HUD_TOP_PADDING +
      5 * WebGLDebuggerHudRenderer.LINE_HEIGHT +
      WebGLDebuggerHudRenderer.HUD_BOTTOM_PADDING;
    if (solidModelUsesCsgDebugControls(model.getSolidModelName())) {
      blockHeight += model.usesKurlanderBowlSingleMotifControls()
        ? 2 * WebGLDebuggerHudRenderer.LINE_HEIGHT
        : WebGLDebuggerHudRenderer.LINE_HEIGHT;
    }

    g.fillStyle = 'rgba(0, 0, 0, ' + 180 / 255 + ')';
    g.fillRect(0, 0, width, Math.min(height, blockHeight));
    g.font = WebGLDebuggerHudRenderer.HUD_FONT;
    g.fillStyle = 'rgb(255, 242, 51)';

    const top: number = WebGLDebuggerHudRenderer.HUD_TOP_PADDING;
    const left: number = WebGLDebuggerHudRenderer.HUD_LEFT_PADDING;
    const lineHeight: number = WebGLDebuggerHudRenderer.LINE_HEIGHT;

    const showingFaceLoopMessage: string = 'Face [1, 2]: ' + this.formatFaceLoopLabel();
    const selectedModelMessage: string =
      'Selected model [3, 4]: ' +
      model.getSolidModelName() +
      ' (' +
      solidModelDisplayIndex(model.getSolidModelName()) +
      '/' +
      solidModelTotalModels() +
      ')';
    const csgSampleMessage: string =
      'CSG sample [6]: ' +
      csgSampleLabel(model.getCsgSample()) +
      ' (' +
      csgSampleDisplayIndex(model.getCsgSample()) +
      '/' +
      csgSampleTotalSamples() +
      ')';
    const kurlanderMotifMessage: string =
      'Motif [e, E]: ' + model.getKurlanderBowlSingleMotifLabel();
    const csgOperationMessage: string = 'CSG op [5]: ' + csgOperationLabel(model.getCsgOperation());
    const referenceFrameMessage: string =
      'Reference frame [Space]: ' + (model.isShowCoordinateSystem() ? 'ON' : 'OFF');
    const nrMessage: string = 'NR [q, Q]: ' + model.getSubdivisionCircumference();
    const nhMessage: string = 'NH [w, W]: ' + model.getSubdivisionHeight();

    g.fillText(showingFaceLoopMessage, left, height - (height - top));
    g.fillText(selectedModelMessage, left, top + lineHeight);

    if (solidModelUsesCsgDebugControls(model.getSolidModelName())) {
      g.fillText(csgSampleMessage, left, top + nextLeftLine * lineHeight);
      nextLeftLine++;
      if (model.usesKurlanderBowlSingleMotifControls()) {
        g.fillText(kurlanderMotifMessage, left, top + nextLeftLine * lineHeight);
        nextLeftLine++;
      }
      g.fillText(csgOperationMessage, left, top + nextLeftLine * lineHeight);
      nextLeftLine++;
    }
    g.fillText(referenceFrameMessage, left, top + nextLeftLine * lineHeight);
    nextLeftLine++;
    const hiddenLineMessage: string =
      'Hidden line [8]: ' + appelDisplayModeLabel(model.getAppelDisplayMode());
    g.fillText(hiddenLineMessage, left, top + nextLeftLine * lineHeight);
    WebGLDebuggerHudRenderer.drawTopRight(g, width, nrMessage, top);
    WebGLDebuggerHudRenderer.drawTopRight(g, width, nhMessage, top + lineHeight);

    if (model.isErrorState()) {
      g.fillStyle = 'rgb(255, 38, 38)';
      g.fillText(model.getErrorMessage(), left, height - 16);
      g.fillStyle = 'rgb(255, 242, 51)';
    }
    this.drawCsgStatisticsSummary(g, height);
  }

  private drawCsgStatisticsSummary(g: CanvasRenderingContext2D, height: number): void {
    const model: DebuggerModel = this.model;
    if (!solidModelUsesCsgDebugControls(model.getSolidModelName())) {
      return;
    }
    if (!PolyhedralBoundedSolidStatistics.isEnabled()) {
      return;
    }
    if (PolyhedralBoundedSolidStatistics.getSetOpCalls() <= 0n) {
      return;
    }

    const failures: bigint = PolyhedralBoundedSolidStatistics.getOperationFailureCases();
    const warnings: bigint = PolyhedralBoundedSolidStatistics.getConsistencyWarningCases();
    const he1eqhe2: bigint = PolyhedralBoundedSolidStatistics.getHe1EqualsHe2Cases();
    const invalidInputs: bigint = PolyhedralBoundedSolidStatistics.getInvalidHalfEdgeInputCases();
    const joinIncomplete: bigint = PolyhedralBoundedSolidStatistics.getJoinIncompleteCases();

    const issueTotal: bigint = failures + warnings + he1eqhe2 + invalidInputs + joinIncomplete;
    if (issueTotal <= 0n) {
      return;
    }

    const lineGap = 22;
    const startY: number = model.isErrorState() ? 16 + 3 * lineGap : 16;
    const left: number = WebGLDebuggerHudRenderer.HUD_LEFT_PADDING;

    g.fillStyle = 'rgb(255, 38, 38)';
    g.fillText('CSG stats issues:', left, height - startY - 2 * lineGap);
    g.fillText(
      'fail=' + failures + ' warn=' + warnings + ' he1==he2=' + he1eqhe2,
      left,
      height - startY - lineGap,
    );
    g.fillText(
      'joinIncomplete=' + joinIncomplete + ' invalidHE=' + invalidInputs,
      left,
      height - startY,
    );
  }

  private drawDebugVertexLabels(g: CanvasRenderingContext2D): void {
    const solid: PolyhedralBoundedSolid | null = this.model.getSolid();
    if (this.model.notDebugVertices() || solid === null || solid.getVerticesList() === null) {
      return;
    }
    g.font = WebGLDebuggerHudRenderer.LABEL_FONT;
    WebGLPolyhedralBoundedSolidDebugHUDRenderer.drawDebugVertexLabels(
      g,
      solid,
      this.model.getCamera(),
      this.viewportWidth,
      this.viewportHeight,
    );
  }

  private drawSelectedFaceLabel(g: CanvasRenderingContext2D): void {
    g.font = WebGLDebuggerHudRenderer.LABEL_FONT;
    WebGLPolyhedralBoundedSolidDebugHUDRenderer.drawSelectedFaceLabel(
      g,
      this.model.getSolid(),
      this.model.getFaceIndex(),
      this.model.getCamera(),
      this.viewportWidth,
      this.viewportHeight,
    );
  }

  private formatFaceLoopLabel(): string {
    if (this.model.getFaceIndex() === -2) {
      return 'NONE';
    }
    if (this.model.getFaceIndex() === -1) {
      return 'ALL';
    }

    const currentFace: number = this.model.getFaceIndex() + 1;
    let totalFaces = 0;
    const solid: PolyhedralBoundedSolid | null = this.model.getSolid();
    if (solid !== null && solid.getPolygonsList() !== null) {
      totalFaces = solid.getPolygonsList().size();
    }
    return '[' + currentFace + '/' + totalFaces + ']';
  }

  private ensureOverlayBuffers(width: number, height: number): void {
    if (
      this.overlayImage !== null &&
      this.bufferedOverlay !== null &&
      this.overlayImage.getXSize() === width &&
      this.overlayImage.getYSize() === height
    ) {
      return;
    }
    this.overlayImage = new RGBAImageUncompressed();
    this.overlayImage.init(width, height);
    const canvas: HTMLCanvasElement = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d', { willReadFrequently: true });
    if (context === null) {
      throw new Error('PolyhedralBoundedSolidExample requires a 2D canvas context for its HUD.');
    }
    this.bufferedOverlay = context;
  }

  /**
   * Java's `getRGB` / `putPixel` loop. `putPixel(x, y)` writes row `y` of the
   * overlay into row `height - 1 - y` of the image's buffer, so each canvas
   * row is copied, whole, to that row.
   */
  private copyBufferedOverlayToImage(width: number, height: number): void {
    const rendered: Uint8ClampedArray = this.bufferedOverlay!.getImageData(
      0,
      0,
      width,
      height,
    ).data;
    const raw: Uint8Array = this.overlayImage!.getRawImageDirectBuffer();
    const rowBytes: number = width * 4;
    for (let y = 0; y < height; y++) {
      const source: number = y * rowBytes;
      raw.set(rendered.subarray(source, source + rowBytes), (height - 1 - y) * rowBytes);
    }
  }

  private static drawTopRight(
    g: CanvasRenderingContext2D,
    width: number,
    text: string,
    baselineY: number,
  ): void {
    const textWidth: number = g.measureText(text).width;
    const x: number = width - WebGLDebuggerHudRenderer.HUD_LEFT_PADDING - Math.ceil(textWidth);
    g.fillText(text, Math.max(WebGLDebuggerHudRenderer.HUD_LEFT_PADDING, x), baselineY);
  }
}
