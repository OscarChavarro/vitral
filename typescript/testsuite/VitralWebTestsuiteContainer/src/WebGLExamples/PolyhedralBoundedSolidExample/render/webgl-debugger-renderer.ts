import {
  ByteArrayOutputStream,
  Calligraphic2DBuffer,
  Camera,
  ColorRgb,
  HiddenLineRenderer,
  ImagePersistence,
  JavaMath,
  LightGizmoStyle,
  Matrix4x4d,
  RGBImageUncompressed,
  SimpleBody,
  SimpleMaterial,
  Vector3Dd,
  type PolyhedralBoundedSolid,
} from '@vitral/base';
import {
  WebGLImageRenderer,
  WebGLLightRenderer,
  WebGLLineRenderer,
  WebGLPolyhedralBoundedSolidRenderer,
  WebGLRendererConfigurationShaderSelector,
  WebGLSimpleMaterialRenderer,
} from '@vitral/webgl';
import { saveBytesAs } from '../io/browser-file-output';
import type { AppelDisplayMode } from '../models/appel-display-mode';
import type { DebuggerModel } from '../models/debugger-model';
import { WebGLDebuggerHudRenderer } from './webgl-debugger-hud-renderer';

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolyhedralBoundedSolidExample/src/render/Jogl4DebuggerRenderer.java`.
 *
 * The `GLEventListener` maps onto this class one method at a time — `init`,
 * `dispose`, `display`, `reshape` — and the component calls them where the JOGL
 * canvas would. `display` is Java's: a mid-grey clear, the depth test, the
 * objects, a depth clear, the two CSG operand insets in the lower corners, the
 * HUD, and the pending screenshot. `drawObjectsGL` is Java's too: the material
 * and the two lights activated, each light drawn as an omni billboard, the
 * solid, the reference frame, the boundary arrows of the selected face or of
 * every face, the selected face filled, and then either the edge-visibility
 * debug view or the Appel hidden-line result drawn as projected lines — hidden
 * ones thin and grey, visible ones black and four pixels wide, contours eight.
 *
 * The runtime boundaries are the container's recurring ones: drawing is
 * asynchronous because a GLSL source arrives over `fetch`; `glViewport` is
 * the component's; and `exportPendingScreenshot` reads the pixels where Java
 * reads them, inside the frame, because a WebGL drawing buffer is cleared at
 * the end of the task that drew it, then encodes them with the ported
 * `ImagePersistence.exportPNG` and hands `screenshot.png` to the browser as a
 * download. `refreshCanvasAfterWindowModeChange`, which revalidates the Swing
 * frame after a macOS fullscreen switch, has no counterpart: the browser lays
 * the canvas out again by itself and the resize observer repaints it.
 */
export class WebGLDebuggerRenderer {
  private static readonly HUD_INSET_DEPTH = 2.8;

  private readonly hudRenderer: WebGLDebuggerHudRenderer;
  private readonly csgOperandMaterialA: SimpleMaterial;
  private readonly csgOperandMaterialB: SimpleMaterial;
  private pendingScreenshotFile: string | null;

  constructor(private readonly model: DebuggerModel) {
    this.hudRenderer = new WebGLDebuggerHudRenderer(model);
    this.csgOperandMaterialA = WebGLDebuggerRenderer.createInsetMaterial(1.0, 0.502, 0.502);
    this.csgOperandMaterialB = WebGLDebuggerRenderer.createInsetMaterial(0.502, 1.0, 0.502);
    this.pendingScreenshotFile = null;
  }

  requestScreenshot(outputFile: string): void {
    this.pendingScreenshotFile = outputFile;
  }

  private static createInsetMaterial(r: number, g: number, b: number): SimpleMaterial {
    let m = new SimpleMaterial();
    m = m.withAmbient(new ColorRgb(0.2 * r, 0.2 * g, 0.2 * b));
    m = m.withDiffuse(new ColorRgb(r, g, b));
    m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
    m = m.withDoubleSided(false);
    m = m.withPhongExponent(100);
    return m;
  }

  private static solidCenter(solid: PolyhedralBoundedSolid | null): Vector3Dd {
    if (solid === null) {
      return new Vector3Dd(0, 0, 0);
    }
    const minMax: Float64Array | null = solid.getMinMax();
    if (minMax === null || minMax.length < 6) {
      return new Vector3Dd(0, 0, 0);
    }
    return new Vector3Dd(
      (minMax[0]! + minMax[3]!) / 2.0,
      (minMax[1]! + minMax[4]!) / 2.0,
      (minMax[2]! + minMax[5]!) / 2.0,
    );
  }

  private static solidMaxExtent(solid: PolyhedralBoundedSolid | null): number {
    if (solid === null) {
      return 1.0;
    }
    const minMax: Float64Array | null = solid.getMinMax();
    if (minMax === null || minMax.length < 6) {
      return 1.0;
    }
    const ex: number = Math.abs(minMax[0]! - minMax[3]!);
    const ey: number = Math.abs(minMax[1]! - minMax[4]!);
    const ez: number = Math.abs(minMax[2]! - minMax[5]!);
    return Math.max(ex, Math.max(ey, ez));
  }

  private static cameraRelativeAnchor(
    camera: Camera,
    ndcX: number,
    ndcY: number,
    depth: number,
  ): Vector3Dd {
    const eye: Vector3Dd = camera.getPosition();
    const front: Vector3Dd = camera.getFront().normalized();
    const up: Vector3Dd = camera.getUp().normalized();
    const right: Vector3Dd = camera.getLeft().multiply(-1).normalized();
    const viewportY: number = Math.max(camera.getViewportYSize(), 1e-9);
    const aspect: number = camera.getViewportXSize() / viewportY;
    let offsetX: number;
    let offsetY: number;
    const safeDepth: number = Math.max(depth, 1e-9);

    if (camera.getProjectionMode() === Camera.PROJECTION_MODE_ORTHOGONAL) {
      const zoom: number = Math.max(camera.getOrthogonalZoom(), 1e-9);
      offsetX = ndcX * (aspect / zoom);
      offsetY = ndcY * (1.0 / zoom);
    } else {
      const halfHeight: number = safeDepth * Math.tan(JavaMath.toRadians(camera.getFov() / 2.0));
      const halfWidth: number = halfHeight * aspect;
      offsetX = ndcX * halfWidth;
      offsetY = ndcY * halfHeight;
    }

    return eye
      .add(front.multiply(safeDepth))
      .add(right.multiply(offsetX))
      .add(up.multiply(offsetY));
  }

  private static buildInsetModelMatrix(
    anchorPoint: Vector3Dd,
    scale: number,
    center: Vector3Dd,
  ): Matrix4x4d {
    return new Matrix4x4d()
      .translation(anchorPoint)
      .multiply(
        new Matrix4x4d()
          .scale(scale, scale, scale)
          .multiply(new Matrix4x4d().translation(-center.x(), -center.y(), -center.z())),
      );
  }

  private async drawInsetSolid(
    gl: WebGL2RenderingContext,
    solid: PolyhedralBoundedSolid | null,
    material: SimpleMaterial,
    anchorPoint: Vector3Dd,
    mainSolidExtent: number,
  ): Promise<void> {
    if (solid === null) {
      return;
    }
    const center: Vector3Dd = WebGLDebuggerRenderer.solidCenter(solid);
    let extent: number = WebGLDebuggerRenderer.solidMaxExtent(solid);
    if (extent < 1e-12) {
      extent = 1.0;
    }
    if (mainSolidExtent < 1e-12) {
      mainSolidExtent = 1.0;
    }
    const scale: number = 0.75 * (mainSolidExtent / extent);

    const modelMatrix: Matrix4x4d = WebGLDebuggerRenderer.buildInsetModelMatrix(
      anchorPoint,
      scale,
      center,
    );
    WebGLSimpleMaterialRenderer.activate(gl, material);
    await WebGLPolyhedralBoundedSolidRenderer.draw(
      gl,
      solid,
      this.model.getCamera(),
      this.model.getQuality(),
      modelMatrix,
    );
  }

  private async drawCsgOperandInsets(
    gl: WebGL2RenderingContext,
    viewportWidth: number,
    viewportHeight: number,
  ): Promise<void> {
    const operandA: PolyhedralBoundedSolid | null = this.model.getCsgPreviewOperandA();
    const operandB: PolyhedralBoundedSolid | null = this.model.getCsgPreviewOperandB();
    const mainSolid: PolyhedralBoundedSolid | null = this.model.getSolid();
    const camera: Camera = this.model.getCamera();

    if (operandA === null || operandB === null || mainSolid === null) {
      return;
    }
    if (viewportWidth <= 0 || viewportHeight <= 0) {
      return;
    }
    const mainExtent: number = WebGLDebuggerRenderer.solidMaxExtent(mainSolid);

    const leftAnchor: Vector3Dd = WebGLDebuggerRenderer.cameraRelativeAnchor(
      camera,
      -0.76,
      -0.76,
      WebGLDebuggerRenderer.HUD_INSET_DEPTH,
    );
    const rightAnchor: Vector3Dd = WebGLDebuggerRenderer.cameraRelativeAnchor(
      camera,
      0.76,
      -0.76,
      WebGLDebuggerRenderer.HUD_INSET_DEPTH,
    );

    await this.drawInsetSolid(gl, operandA, this.csgOperandMaterialA, leftAnchor, mainExtent);
    await this.drawInsetSolid(gl, operandB, this.csgOperandMaterialB, rightAnchor, mainExtent);
    WebGLSimpleMaterialRenderer.activate(gl, this.model.getMaterial());
  }

  private static appendSegmentedLines(
    source: Calligraphic2DBuffer,
    r: number,
    g: number,
    b: number,
    positions: number[],
    colors: number[],
  ): void {
    for (let i = 0; i < source.getNumLines(); i++) {
      const segment: [Vector3Dd, Vector3Dd] = source.get2DLine(i);
      const p0: Vector3Dd = segment[0];
      const p1: Vector3Dd = segment[1];
      positions.push(p0.x());
      positions.push(p0.y());
      positions.push(p0.z());
      positions.push(p1.x());
      positions.push(p1.y());
      positions.push(p1.z());
      for (let v = 0; v < 2; v++) {
        colors.push(r);
        colors.push(g);
        colors.push(b);
      }
    }
  }

  private static async drawBufferedLines(
    gl: WebGL2RenderingContext,
    lines: Calligraphic2DBuffer,
    r: number,
    g: number,
    b: number,
    lineWidth: number,
  ): Promise<void> {
    const positions: number[] = [];
    const colors: number[] = [];

    WebGLDebuggerRenderer.appendSegmentedLines(lines, r, g, b, positions, colors);
    if (positions.length === 0) {
      return;
    }
    await WebGLLineRenderer.drawLines(
      gl,
      Matrix4x4d.identityMatrix(),
      new Float32Array(positions),
      new Float32Array(colors),
      lineWidth,
      Math.fround(-4.0e-4),
    );
  }

  private async renderLinesResult(
    gl: WebGL2RenderingContext,
    contourLines: Calligraphic2DBuffer,
    visibleLines: Calligraphic2DBuffer,
    hiddenLines: Calligraphic2DBuffer,
    showHiddenLines: boolean,
    showVisibleLines: boolean,
  ): Promise<void> {
    if (showHiddenLines) {
      await WebGLDebuggerRenderer.drawBufferedLines(gl, hiddenLines, 0.7, 0.7, 0.7, 1.0);
    }
    if (showVisibleLines) {
      await WebGLDebuggerRenderer.drawBufferedLines(gl, visibleLines, 0.0, 0.0, 0.0, 4.0);
    }
    await WebGLDebuggerRenderer.drawBufferedLines(gl, contourLines, 0.0, 0.0, 0.0, 8.0);
  }

  private async drawReferenceFrame(gl: WebGL2RenderingContext, mvp: Matrix4x4d): Promise<void> {
    if (this.model.getEdgeIndex() <= -3 || !this.model.isShowCoordinateSystem()) {
      return;
    }
    const positions = new Float32Array([0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1]);
    const colors = new Float32Array([1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1]);
    await WebGLLineRenderer.drawLines(gl, mvp, positions, colors, 3.0, Math.fround(-3.0e-4));
  }

  private async drawObjectsGL(
    gl: WebGL2RenderingContext,
    _viewportWidth: number,
    _viewportHeight: number,
  ): Promise<void> {
    const solid: PolyhedralBoundedSolid | null = this.model.getSolid();
    if (solid === null) {
      return;
    }
    const modelMatrix: Matrix4x4d = this.model.getSolidModelMatrix();
    const mvp: Matrix4x4d = this.model
      .getCamera()
      .calculateProjectionMatrix()
      .multiply(modelMatrix);

    WebGLSimpleMaterialRenderer.activate(gl, this.model.getMaterial());
    WebGLLightRenderer.activate(gl, this.model.getLight1());
    await WebGLLightRenderer.draw(
      gl,
      this.model.getLight1(),
      this.model.getCamera(),
      LightGizmoStyle.OMNI_BILLBOARD,
    );
    WebGLLightRenderer.activate(gl, this.model.getLight2());
    await WebGLLightRenderer.draw(
      gl,
      this.model.getLight2(),
      this.model.getCamera(),
      LightGizmoStyle.OMNI_BILLBOARD,
    );
    await WebGLPolyhedralBoundedSolidRenderer.draw(
      gl,
      solid,
      this.model.getCamera(),
      this.model.getQuality(),
      modelMatrix,
    );

    await this.drawReferenceFrame(gl, mvp);
    await WebGLPolyhedralBoundedSolidRenderer.drawDebugFaceBoundary(
      gl,
      solid,
      this.model.getFaceIndex(),
      mvp,
    );
    await WebGLPolyhedralBoundedSolidRenderer.drawDebugFace(
      gl,
      solid,
      this.model.getFaceIndex(),
      modelMatrix,
      mvp,
      this.model.getCamera(),
    );

    let contourLines: Calligraphic2DBuffer;
    let visibleLines: Calligraphic2DBuffer;
    let hiddenLines: Calligraphic2DBuffer;
    let bodyArray: SimpleBody[];
    let body: SimpleBody;

    if (this.model.isDebugEdges() && this.model.getEdgeIndex() > -3) {
      await WebGLPolyhedralBoundedSolidRenderer.drawDebugEdges(
        gl,
        solid,
        this.model.getCamera(),
        this.model.getEdgeIndex(),
        mvp,
      );
    } else if (this.model.getEdgeIndex() === -3 || this.model.getAppelDisplayMode() !== 'OFF') {
      contourLines = new Calligraphic2DBuffer();
      visibleLines = new Calligraphic2DBuffer();
      hiddenLines = new Calligraphic2DBuffer();
      bodyArray = [];

      body = new SimpleBody();
      body.setGeometry(solid);
      body.setPosition(new Vector3Dd());
      body.setRotation(modelMatrix);
      body.setRotationInverse(modelMatrix.inverse());
      bodyArray.push(body);
      HiddenLineRenderer.executeAppelAlgorithm(
        bodyArray,
        this.model.getCamera(),
        contourLines,
        visibleLines,
        hiddenLines,
      );
      const displayMode: AppelDisplayMode = this.model.getAppelDisplayMode();
      const showHiddenLines: boolean = displayMode === 'EDGES_VISIBLE_HIDDEN';
      const showVisibleLines: boolean = displayMode !== 'EDGES_ONLY';
      await this.renderLinesResult(
        gl,
        contourLines,
        visibleLines,
        hiddenLines,
        showHiddenLines,
        showVisibleLines,
      );
    }

    /*
        contourLines = null;
        visibleLines = null;
        hiddenLines = null;
        bodyArray = null;
        body = null;
        */
  }

  /** Java's `display(GLAutoDrawable)`. */
  async display(
    gl: WebGL2RenderingContext,
    surfaceWidth: number,
    surfaceHeight: number,
  ): Promise<void> {
    gl.clearColor(0.5, 0.5, 0.5, 1);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);
    gl.enable(gl.DEPTH_TEST);

    await this.drawObjectsGL(gl, surfaceWidth, surfaceHeight);
    gl.clear(gl.DEPTH_BUFFER_BIT);
    await this.drawCsgOperandInsets(gl, surfaceWidth, surfaceHeight);
    if (this.model.isHudEnabled()) {
      await this.hudRenderer.draw(gl);
    }
    this.exportPendingScreenshot(gl, surfaceWidth, surfaceHeight);
  }

  /**
   * Java's `init(GLAutoDrawable)`, which sizes the HUD. Every program a frame
   * can reach is also compiled here — the line program, the solid renderer's
   * colour and surface programs, and the HUD's image program — so that no
   * frame awaits a shader source; see the drawing-buffer rule recorded for the
   * WebGL example programs.
   */
  async init(gl: WebGL2RenderingContext, width: number, height: number): Promise<void> {
    await WebGLLineRenderer.prepare(gl);
    await WebGLPolyhedralBoundedSolidRenderer.prepare(gl);
    await this.hudRenderer.init(gl, width, height);
  }

  /** Java's `dispose(GLAutoDrawable)`. */
  dispose(gl: WebGL2RenderingContext | null): void {
    if (gl !== null) {
      WebGLLineRenderer.release(gl);
      WebGLPolyhedralBoundedSolidRenderer.release(gl);
      WebGLImageRenderer.dispose(gl);
      WebGLRendererConfigurationShaderSelector.dispose(gl);
    }
    this.hudRenderer.dispose(gl);
  }

  /** Java's `reshape(GLAutoDrawable, x, y, width, height)`. */
  reshape(gl: WebGL2RenderingContext, width: number, height: number): void {
    gl.viewport(0, 0, width, height);

    this.model.getCamera().updateViewportResize(width, height);
    this.hudRenderer.updateViewportSize(width, height);
  }

  private exportPendingScreenshot(gl: WebGL2RenderingContext, width: number, height: number): void {
    const outputFile: string | null = this.pendingScreenshotFile;
    if (outputFile === null || width <= 0 || height <= 0) {
      return;
    }

    this.pendingScreenshotFile = null;
    gl.finish();
    const image: RGBImageUncompressed = WebGLDebuggerRenderer.captureRgbImage(gl, width, height);
    const output = new ByteArrayOutputStream();
    ImagePersistence.exportPNG(output, image);
    saveBytesAs(outputFile, output.toByteArray(), 'image/png');
    console.log('[PolyhedralBoundedSolidExample] Exported ' + outputFile);
  }

  /**
   * Java reads `GL_RGB` bytes. WebGL2 only guarantees `RGBA` with
   * `UNSIGNED_BYTE` for the default framebuffer, so four bytes a pixel are
   * read and the alpha byte is stepped over.
   */
  private static captureRgbImage(
    gl: WebGL2RenderingContext,
    width: number,
    height: number,
  ): RGBImageUncompressed {
    const bb = new Uint8Array(4 * width * height);
    gl.pixelStorei(gl.PACK_ALIGNMENT, 1);
    gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, bb);

    const image = new RGBImageUncompressed();
    image.init(width, height);

    let pos = 0;
    for (let y = image.getYSize() - 1; y >= 0; y--) {
      for (let x = 0; x < image.getXSize(); x++) {
        image.putPixel(x, y, bb[pos]!, bb[pos + 1]!, bb[pos + 2]!);
        pos += 4;
      }
    }
    return image;
  }
}
