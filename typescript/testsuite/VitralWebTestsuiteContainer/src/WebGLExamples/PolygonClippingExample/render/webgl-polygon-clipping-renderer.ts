import {
  Matrix4x4d,
  RendererConfiguration,
  RGBImageUncompressed,
  VSDK,
  type Polygon2D,
  type _DoubleLinkedListNode,
  type _Polygon2DWA,
  type _VertexNode2D,
} from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLPolygon2DRenderer,
  WebGLShaderProgramUtil,
  type WebGLPolygon2DRendererResources,
} from '@vitral/webgl';
import type { PolygonClippingDebuggerModel } from '../model/polygon-clipping-debugger-model';
import { WebGLPolygonClippingHudRenderer } from './webgl-polygon-clipping-hud-renderer';
import { WebGLTriangularRenderer } from './webgl-triangular-renderer';

/**
 * Bounding rectangle of `JoglPolygonClippingRenderer.Bounds2D`, a private
 * static nested class of the Java file. It is the second of the two the
 * program carries; `PolygonClippingModelingTools` has its own, with the same
 * name and a slightly different interface, and both are kept as they are.
 */
class Bounds2D {
  private minX = 0;
  private minY = 0;
  private maxX = 0;
  private maxY = 0;
  private started = false;

  include(x: number, y: number): void {
    if (!this.started) {
      this.minX = x;
      this.maxX = x;
      this.minY = y;
      this.maxY = y;
      this.started = true;
      return;
    }

    if (x < this.minX) {
      this.minX = x;
    }
    if (x > this.maxX) {
      this.maxX = x;
    }
    if (y < this.minY) {
      this.minY = y;
    }
    if (y > this.maxY) {
      this.maxY = y;
    }
  }

  initialized(): boolean {
    return this.started;
  }

  width(): number {
    return this.started ? this.maxX - this.minX : 0.0;
  }

  height(): number {
    return this.started ? this.maxY - this.minY : 0.0;
  }
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/render/JoglPolygonClippingRenderer.java`.
 *
 * The `GLEventListener` it implements maps onto this class one method at a
 * time — `init`, `dispose`, `display`, `reshape` — and the component calls
 * them where the JOGL canvas would. `display` is unchanged: depth test on,
 * blending off, a black clear, the camera's projection, the objects, then the
 * HUD between a saved and a restored viewport and capability triple, then the
 * snapshot if one was asked for.
 *
 * `drawObjects` is unchanged too: the reference frame first, then the two
 * annotated Weiler--Atherton polygons drawn in place, then the inner result on
 * a panel translated by minus one and a quarter depths in Z and the outer
 * result on a panel translated by one and a quarter widths in X. Every colour
 * triple, the two line widths, the eight-pixel point size, the
 * intersection-vertex recolouring and the `Math.max(6.0, ...)` panel floors are
 * Java's.
 *
 * Four runtime boundaries are crossed:
 *
 *   - `init` compiles its two programs from shader files, which a page
 *     fetches, so it is asynchronous, and so is every path that reaches the
 *     image renderer of the HUD.
 *   - `glPointSize` does not exist in WebGL; the value travels to the vertex
 *     shader as the `pointSizeLocal` uniform `WebGLShaderPreprocessor`
 *     injects, set where Java calls `glPointSize`.
 *   - `glIsEnabled` answers a `boolean` in both, but WebGL types it as a
 *     generic parameter answer, so the three capability reads are cast where
 *     Java needs no cast.
 *   - `ImagePersistence.exportPNG(new File("frameNNNN.png"), snapshot)` writes
 *     a file beside the program. A page has no such directory, so the same
 *     image, under the same name, is handed to the browser as a download. The
 *     `readPixels` that produces it is issued where Java issues it, inside the
 *     frame, because a drawing buffer is cleared at the end of the task that
 *     drew it.
 */
export class WebGLPolygonClippingRenderer {
  private readonly hudRenderer: WebGLPolygonClippingHudRenderer;
  private readonly triangularRenderer: WebGLTriangularRenderer;

  private resources: WebGLPolygon2DRendererResources | null = null;

  constructor(private readonly model: PolygonClippingDebuggerModel) {
    this.hudRenderer = new WebGLPolygonClippingHudRenderer();
    this.triangularRenderer = new WebGLTriangularRenderer();
  }

  async init(gl: WebGL2RenderingContext, width: number, height: number): Promise<void> {
    const lineProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
      gl,
      'lineVertexShader.glsl',
      'linePixelShader.glsl',
    );
    const constantProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
      gl,
      'constantVertexShader.glsl',
      'constantPixelShader.glsl',
    );

    const vertexArray = gl.createVertexArray();
    const positionBuffer = gl.createBuffer();
    const colorBuffer = gl.createBuffer();
    if (vertexArray === null || positionBuffer === null || colorBuffer === null) {
      throw new Error('PolygonClippingExample failed to create its vertex buffers.');
    }
    this.resources = { lineProgram, constantProgram, vertexArray, positionBuffer, colorBuffer };

    await this.hudRenderer.init(gl, width, height);
    await this.triangularRenderer.init(gl);
  }

  dispose(gl: WebGL2RenderingContext): void {
    if (this.resources !== null) {
      gl.deleteBuffer(this.resources.positionBuffer);
      gl.deleteBuffer(this.resources.colorBuffer);
      gl.deleteVertexArray(this.resources.vertexArray);
      gl.deleteProgram(this.resources.lineProgram);
      gl.deleteProgram(this.resources.constantProgram);
      this.resources = null;
    }

    WebGLCameraRenderer.dispose(gl);
    this.hudRenderer.dispose(gl);
    this.triangularRenderer.dispose(gl);
  }

  async display(gl: WebGL2RenderingContext, width: number, height: number): Promise<void> {
    gl.enable(gl.DEPTH_TEST);
    gl.disable(gl.BLEND);
    gl.clearColor(0.0, 0.0, 0.0, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    const projection: Matrix4x4d = WebGLCameraRenderer.activate(gl, this.model.getCamera());
    this.drawObjects(gl, projection);

    const viewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
    const depthEnabled: boolean = gl.isEnabled(gl.DEPTH_TEST);
    const blendEnabled: boolean = gl.isEnabled(gl.BLEND);
    const cullEnabled: boolean = gl.isEnabled(gl.CULL_FACE);
    await this.hudRenderer.draw(gl, this.model);
    gl.viewport(viewport[0]!, viewport[1]!, viewport[2]!, viewport[3]!);
    WebGLPolygonClippingRenderer.setCapability(gl, gl.DEPTH_TEST, depthEnabled);
    WebGLPolygonClippingRenderer.setCapability(gl, gl.BLEND, blendEnabled);
    WebGLPolygonClippingRenderer.setCapability(gl, gl.CULL_FACE, cullEnabled);

    if (this.model.isTakeSnapshot()) {
      this.model.setTakeSnapshot(false);
      const snapshot: RGBImageUncompressed = WebGLPolygonClippingRenderer.captureRgbImage(
        gl,
        width,
        height,
      );
      const output =
        'frame' + VSDK.formatNumberWithinZeroes(this.model.getSnapshotNumber(), 4) + '.png';
      WebGLPolygonClippingRenderer.exportPng(output, snapshot);
      this.model.setSnapshotNumber(this.model.getSnapshotNumber() + 1);
    }
  }

  reshape(gl: WebGL2RenderingContext, width: number, height: number): void {
    gl.viewport(0, 0, width, height);
    this.model.getCamera().updateViewportResize(width, height);
    this.hudRenderer.updateViewportSize(width, height);
  }

  private drawObjects(gl: WebGL2RenderingContext, projection: Matrix4x4d): void {
    if (this.model.isShowReferenceFrame()) {
      this.drawReferenceFrame(gl, projection);
    }

    const bounds: Bounds2D = this.calculateBounds();
    const panelWidth: number = Math.max(6.0, bounds.width());
    const panelDepth: number = Math.max(6.0, bounds.height());
    const polygonQuality = new RendererConfiguration();
    polygonQuality.clone(this.model.getQuality());
    polygonQuality.setSurfaces(polygonQuality.isSurfacesSet() && this.model.isShowFilledPolygons());

    if (this.model.isShowClipPolygon()) {
      this.drawPolygonWA(
        gl,
        projection,
        this.model.getClipPolygonWA(),
        0.2,
        0.75,
        0.25,
        0.7,
        0.2,
        0.0,
        0.0,
      );
    }
    if (this.model.isShowSubjectPolygon()) {
      this.drawPolygonWA(
        gl,
        projection,
        this.model.getSubjectPolygonWA(),
        0.8,
        0.74,
        0.2,
        0.82,
        0.56,
        0.0,
        0.0,
      );
    }

    const innerTransform: Matrix4x4d = new Matrix4x4d().translation(0.0, 0.0, -panelDepth * 1.25);
    if (this.model.isShowInnerPolygon()) {
      this.drawResultPolygon(
        gl,
        projection.multiply(innerTransform),
        this.model.getInnerPolygon(),
        polygonQuality,
        0.65,
        0.65,
        0.7,
        0.82,
        0.58,
        0.36,
      );
    }

    const outerTransform: Matrix4x4d = new Matrix4x4d().translation(panelWidth * 1.25, 0.0, 0.0);
    if (this.model.isShowOuterPolygon()) {
      this.drawResultPolygon(
        gl,
        projection.multiply(outerTransform),
        this.model.getOuterPolygon(),
        polygonQuality,
        0.68,
        0.78,
        0.68,
        0.18,
        0.72,
        0.24,
      );
    }
  }

  /**
   * Draws a clipping-result polygon (inner/outer). The filled surface is
   * produced either by the polygon renderer's own tessellator or by the
   * monotone-decomposition triangulator depending on the active tessellation
   * mode, while wires and points are always rendered through
   * `WebGLPolygon2DRenderer` so both modes look identical except for the
   * surface triangulation method.
   */
  private drawResultPolygon(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    polygon: Polygon2D | null,
    polygonQuality: RendererConfiguration,
    fillR: number,
    fillG: number,
    fillB: number,
    lineR: number,
    lineG: number,
    lineB: number,
  ): void {
    const monotoneMode: boolean =
      this.model.getPolygonSurfaceTessellationMode() === 'MONOTONE_DECOMPOSITION';

    if (!monotoneMode) {
      WebGLPolygon2DRenderer.draw(
        gl,
        mvp,
        polygon,
        polygonQuality,
        fillR,
        fillG,
        fillB,
        lineR,
        lineG,
        lineB,
        this.resources!,
      );
      return;
    }

    this.triangularRenderer.fillPolygonSurface(
      gl,
      mvp,
      polygon,
      polygonQuality,
      fillR,
      fillG,
      fillB,
      lineR,
      lineG,
      lineB,
    );
  }

  private drawReferenceFrame(gl: WebGL2RenderingContext, mvp: Matrix4x4d): void {
    const positions: number[] = [];
    const colors: number[] = [];
    WebGLPolygonClippingRenderer.addSegment(positions, colors, 0, 0, 0, 2, 0, 0, 1, 0, 0);
    WebGLPolygonClippingRenderer.addSegment(positions, colors, 0, 0, 0, 0, 2, 0, 0, 1, 0);
    WebGLPolygonClippingRenderer.addSegment(positions, colors, 0, 0, 0, 0, 0, 2, 0, 0, 1);
    this.drawLines(gl, mvp, positions, colors, gl.LINES, 3.0);
  }

  private drawPolygonWA(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    polygon: _Polygon2DWA | null,
    lineR: number,
    lineG: number,
    lineB: number,
    pointR: number,
    pointG: number,
    tx: number,
    tz: number,
  ): void {
    if (polygon === null) {
      return;
    }

    for (const contour of polygon.loops) {
      const head: _DoubleLinkedListNode<_VertexNode2D> | null = contour.vertices.getHead();
      if (head === null) {
        continue;
      }

      const linePositions: number[] = [];
      const lineColors: number[] = [];
      const pointPositions: number[] = [];
      const pointColors: number[] = [];

      let cursor: _DoubleLinkedListNode<_VertexNode2D> = head;
      do {
        const next: _DoubleLinkedListNode<_VertexNode2D> = cursor.next;
        WebGLPolygonClippingRenderer.addSegment(
          linePositions,
          lineColors,
          cursor.data.x + tx,
          0.0,
          cursor.data.y + tz,
          next.data.x + tx,
          0.0,
          next.data.y + tz,
          lineR,
          lineG,
          lineB,
        );

        const r: number = cursor.data.pairNode === null ? pointR : 0.15;
        const g: number = cursor.data.pairNode === null ? pointG : 0.85;
        const b: number = cursor.data.pairNode === null ? 0.45 : 0.25;
        WebGLPolygonClippingRenderer.addPoint(
          pointPositions,
          pointColors,
          cursor.data.x + tx,
          0.0,
          cursor.data.y + tz,
          r,
          g,
          b,
        );
        cursor = cursor.next;
      } while (cursor !== head);

      this.drawLines(gl, mvp, linePositions, lineColors, gl.LINES, 2.0);
      if (this.model.isShowIntersections()) {
        this.drawPoints(gl, mvp, pointPositions, pointColors, 8.0);
      }
    }
  }

  private drawLines(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    positions: number[],
    colors: number[],
    primitive: number,
    lineWidth: number,
  ): void {
    if (positions.length === 0) {
      return;
    }
    const resources: WebGLPolygon2DRendererResources = this.resources!;

    gl.useProgram(resources.lineProgram);
    WebGLPolygonClippingRenderer.setMvpUniform(gl, resources.lineProgram, mvp);
    WebGLPolygonClippingRenderer.setFloat(gl, resources.lineProgram, 'depthBiasNdc', 0.0);

    this.bindLineAttributes(gl, positions, colors);
    gl.lineWidth(lineWidth);
    gl.drawArrays(primitive, 0, positions.length / 3);
    this.unbind(gl);
  }

  private drawPoints(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    positions: number[],
    colors: number[],
    pointSize: number,
  ): void {
    if (positions.length === 0) {
      return;
    }
    const resources: WebGLPolygon2DRendererResources = this.resources!;

    gl.useProgram(resources.lineProgram);
    WebGLPolygonClippingRenderer.setMvpUniform(gl, resources.lineProgram, mvp);
    WebGLPolygonClippingRenderer.setFloat(gl, resources.lineProgram, 'depthBiasNdc', 0.0);

    this.bindLineAttributes(gl, positions, colors);
    WebGLPolygonClippingRenderer.setFloat(gl, resources.lineProgram, 'pointSizeLocal', pointSize);
    gl.drawArrays(gl.POINTS, 0, positions.length / 3);
    this.unbind(gl);
  }

  private bindLineAttributes(
    gl: WebGL2RenderingContext,
    positions: number[],
    colors: number[],
  ): void {
    const resources: WebGLPolygon2DRendererResources = this.resources!;
    gl.bindVertexArray(resources.vertexArray);

    gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(positions), gl.STREAM_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(colors), gl.STREAM_DRAW);
    gl.enableVertexAttribArray(1);
    gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);
  }

  private unbind(gl: WebGL2RenderingContext): void {
    gl.disableVertexAttribArray(0);
    gl.disableVertexAttribArray(1);
    gl.disableVertexAttribArray(2);
    gl.bindBuffer(gl.ARRAY_BUFFER, null);
    gl.bindVertexArray(null);
    gl.useProgram(null);
  }

  private static setMvpUniform(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    mvp: Matrix4x4d,
  ): void {
    const location = gl.getUniformLocation(program, 'modelViewProjectionLocal');
    if (location !== null) {
      gl.uniformMatrix4fv(location, false, mvp.exportToFloatArrayColumnOrder());
    }
  }

  private static setFloat(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: number,
  ): void {
    const location = gl.getUniformLocation(program, name);
    if (location !== null) {
      gl.uniform1f(location, value);
    }
  }

  private static addSegment(
    positions: number[],
    colors: number[],
    x1: number,
    y1: number,
    z1: number,
    x2: number,
    y2: number,
    z2: number,
    r: number,
    g: number,
    b: number,
  ): void {
    WebGLPolygonClippingRenderer.addPoint(positions, colors, x1, y1, z1, r, g, b);
    WebGLPolygonClippingRenderer.addPoint(positions, colors, x2, y2, z2, r, g, b);
  }

  private static addPoint(
    positions: number[],
    colors: number[],
    x: number,
    y: number,
    z: number,
    r: number,
    g: number,
    b: number,
  ): void {
    positions.push(x, y, z);
    colors.push(r, g, b);
  }

  private static setCapability(
    gl: WebGL2RenderingContext,
    capability: number,
    enabled: boolean,
  ): void {
    if (enabled) {
      gl.enable(capability);
    } else {
      gl.disable(capability);
    }
  }

  private static captureRgbImage(
    gl: WebGL2RenderingContext,
    width: number,
    height: number,
  ): RGBImageUncompressed {
    const bb = new Uint8Array(3 * width * height);
    gl.pixelStorei(gl.PACK_ALIGNMENT, 1);
    gl.readPixels(0, 0, width, height, gl.RGB, gl.UNSIGNED_BYTE, bb);

    const image = new RGBImageUncompressed();
    image.init(width, height);

    let pos = 0;
    for (let y = image.getYSize() - 1; y >= 0; y--) {
      for (let x = 0; x < image.getXSize(); x++) {
        image.putPixel(x, y, bb[pos]!, bb[pos + 1]!, bb[pos + 2]!);
        pos += 3;
      }
    }
    return image;
  }

  /**
   * Browser stand-in for `ImagePersistence.exportPNG(new File(name), image)`:
   * the samples are copied into a 2D canvas, which encodes the PNG, and the
   * blob is offered to the user under the name Java writes.
   */
  private static exportPng(name: string, image: RGBImageUncompressed): void {
    const width: number = image.getXSize();
    const height: number = image.getYSize();
    const canvas: HTMLCanvasElement = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context: CanvasRenderingContext2D | null = canvas.getContext('2d');
    if (context === null) {
      console.error('[PolygonClippingExample] Can not encode ' + name);
      return;
    }

    const imageData: ImageData = context.createImageData(width, height);
    const raw: Uint8Array = image.getRawImageDirectBuffer();
    for (let i = 0, j = 0; i < raw.length; i += 3, j += 4) {
      imageData.data[j] = raw[i]!;
      imageData.data[j + 1] = raw[i + 1]!;
      imageData.data[j + 2] = raw[i + 2]!;
      imageData.data[j + 3] = 255;
    }
    context.putImageData(imageData, 0, 0);

    canvas.toBlob((blob: Blob | null) => {
      if (blob === null) {
        console.error('[PolygonClippingExample] Can not encode ' + name);
        return;
      }
      const url: string = URL.createObjectURL(blob);
      const anchor: HTMLAnchorElement = document.createElement('a');
      anchor.href = url;
      anchor.download = name;
      anchor.click();
      URL.revokeObjectURL(url);
    }, 'image/png');
  }

  private calculateBounds(): Bounds2D {
    const bounds = new Bounds2D();

    WebGLPolygonClippingRenderer.includeWaPolygonBounds(bounds, this.model.getClipPolygonWA());
    WebGLPolygonClippingRenderer.includeWaPolygonBounds(bounds, this.model.getSubjectPolygonWA());
    WebGLPolygonClippingRenderer.includePolygonBounds(bounds, this.model.getInnerPolygon());
    WebGLPolygonClippingRenderer.includePolygonBounds(bounds, this.model.getOuterPolygon());

    if (!bounds.initialized()) {
      bounds.include(0.0, 0.0);
      bounds.include(4.0, 4.0);
    }

    return bounds;
  }

  private static includeWaPolygonBounds(bounds: Bounds2D, polygon: _Polygon2DWA | null): void {
    if (polygon === null) {
      return;
    }

    for (const contour of polygon.loops) {
      const head: _DoubleLinkedListNode<_VertexNode2D> | null = contour.vertices.getHead();
      if (head === null) {
        continue;
      }
      let cursor: _DoubleLinkedListNode<_VertexNode2D> = head;
      do {
        bounds.include(cursor.data.x, cursor.data.y);
        cursor = cursor.next;
      } while (cursor !== head);
    }
  }

  private static includePolygonBounds(bounds: Bounds2D, polygon: Polygon2D | null): void {
    if (polygon === null) {
      return;
    }

    for (const contour of polygon.loops) {
      for (const vertex of contour.vertices) {
        bounds.include(vertex.x, vertex.y);
      }
    }
  }
}
