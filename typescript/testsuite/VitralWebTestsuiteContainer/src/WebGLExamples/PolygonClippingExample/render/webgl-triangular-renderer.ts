import {
  MonotoneDecompositionTriangulator,
  Polygon2D,
  RendererConfiguration,
  type Matrix4x4d,
} from '@vitral/base';
import { WebGLShaderProgramUtil } from '@vitral/webgl';

interface TriangularRendererResources {
  constantProgram: WebGLProgram;
  lineProgram: WebGLProgram;
  vertexArray: WebGLVertexArrayObject;
  positionBuffer: WebGLBuffer;
  colorBuffer: WebGLBuffer;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/PolygonClippingExample/src/render/JoglTriangularRenderer.java`.
 *
 * Fills polygon surfaces by decomposing them with the
 * `MonotoneDecompositionTriangulator`, as the alternative to the tessellation
 * `WebGLPolygon2DRenderer` carries.
 *
 * Multi-pass rendering with depth bias guarantees correct visibility ordering:
 * surfaces are drawn with `POLYGON_OFFSET_FILL` so they are pushed slightly
 * behind co-planar wires and points; wires (all triangle edges, which expose
 * the full triangulation structure) are drawn with a small negative NDC depth
 * bias so they always appear in front of the fill; and points are drawn with
 * an even larger negative NDC depth bias so they remain visible over both.
 * The two bias constants, the polygon-offset factors, the pass order and the
 * `catch` that silently skips a degenerate polygon are Java's.
 *
 * Three runtime boundaries are crossed:
 *
 *   - `init` compiles its two programs from shader files, which a page fetches,
 *     so it is asynchronous. Java's `init(GLAutoDrawable)` becomes
 *     `init(gl)`, since a browser example owns its context directly.
 *   - `glPointSize` does not exist in WebGL; the value travels to the vertex
 *     shader as the `pointSizeLocal` uniform `WebGLShaderPreprocessor`
 *     injects, set where Java calls `glPointSize`.
 *   - `glGetUniformLocation` answers a negative int for an absent uniform and
 *     WebGL answers `null`, so each `loc >= 0` guard is a `!== null` guard.
 */
export class WebGLTriangularRenderer {
  private static readonly WIRE_DEPTH_BIAS_NDC = -0.001;
  private static readonly POINT_DEPTH_BIAS_NDC = -0.002;

  private resources: TriangularRendererResources | null = null;

  async init(gl: WebGL2RenderingContext): Promise<void> {
    const constantProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
      gl,
      'constantVertexShader.glsl',
      'constantPixelShader.glsl',
    );
    const lineProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
      gl,
      'lineVertexShader.glsl',
      'linePixelShader.glsl',
    );

    const vertexArray = gl.createVertexArray();
    const positionBuffer = gl.createBuffer();
    const colorBuffer = gl.createBuffer();
    if (vertexArray === null || positionBuffer === null || colorBuffer === null) {
      throw new Error('PolygonClippingExample failed to create its triangular renderer buffers.');
    }

    this.resources = { constantProgram, lineProgram, vertexArray, positionBuffer, colorBuffer };
  }

  dispose(gl: WebGL2RenderingContext): void {
    if (this.resources === null) {
      return;
    }
    gl.deleteBuffer(this.resources.colorBuffer);
    gl.deleteBuffer(this.resources.positionBuffer);
    gl.deleteVertexArray(this.resources.vertexArray);
    gl.deleteProgram(this.resources.lineProgram);
    gl.deleteProgram(this.resources.constantProgram);
    this.resources = null;
  }

  /**
   * Triangulates the given polygon with the monotone decomposition
   * triangulator and draws the resulting geometry according to `config`. The
   * render order is: surfaces first (with polygon offset), then wires
   * (triangle edges, revealing the triangulation), then points. Each
   * subsequent pass uses a larger negative NDC depth bias so it always draws
   * in front of the previous one. Degenerate or invalid polygons are silently
   * skipped.
   */
  fillPolygonSurface(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    polygon: Polygon2D | null,
    config: RendererConfiguration | null,
    fillR: number,
    fillG: number,
    fillB: number,
    lineR: number,
    lineG: number,
    lineB: number,
  ): void {
    const resources: TriangularRendererResources | null = this.resources;
    if (resources === null) {
      return;
    }
    if (polygon === null || polygon.loops.length === 0) {
      return;
    }
    if (config === null) {
      return;
    }
    if (!config.isSurfacesSet() && !config.isWiresSet() && !config.isPointsSet()) {
      return;
    }

    try {
      const pipeline = new MonotoneDecompositionTriangulator();
      const triangles: MonotoneDecompositionTriangulator.Triangle[] = [];
      const triangleCount: number = pipeline.triangulate(polygon, triangles);
      if (triangleCount <= 0) {
        return;
      }

      const vertices: number[][] = WebGLTriangularRenderer.flattenVertices(polygon);

      if (config.isSurfacesSet()) {
        const fillPositions: number[] = WebGLTriangularRenderer.buildFillPositions(
          triangleCount,
          triangles,
          vertices,
        );
        if (fillPositions.length > 0) {
          this.drawTriangleSurfaces(gl, mvp, fillPositions, fillR, fillG, fillB);
        }
      }

      if (config.isWiresSet()) {
        const wirePositions: number[] = [];
        const wireColors: number[] = [];
        WebGLTriangularRenderer.buildWirePositions(
          triangleCount,
          triangles,
          vertices,
          wirePositions,
          wireColors,
          lineR,
          lineG,
          lineB,
        );
        if (wirePositions.length > 0) {
          this.drawTriangleWires(gl, mvp, wirePositions, wireColors);
        }
      }

      if (config.isPointsSet()) {
        const pointPositions: number[] = [];
        const pointColors: number[] = [];
        WebGLTriangularRenderer.buildPointPositions(
          triangleCount,
          triangles,
          vertices,
          pointPositions,
          pointColors,
          lineR,
          lineG,
          lineB,
        );
        if (pointPositions.length > 0) {
          this.drawTrianglePoints(gl, mvp, pointPositions, pointColors);
        }
      }
    } catch {
      // Invalid or degenerate polygons are skipped by the visualizer.
    }
  }

  private static buildFillPositions(
    triangleCount: number,
    triangles: readonly MonotoneDecompositionTriangulator.Triangle[],
    vertices: readonly number[][],
  ): number[] {
    const out: number[] = [];
    for (let i = 0; i < triangleCount; i++) {
      const t = triangles[i]!;
      WebGLTriangularRenderer.addFillTriangle(
        out,
        vertices[t.point0]!,
        vertices[t.point1]!,
        vertices[t.point2]!,
      );
    }
    return out;
  }

  private static buildWirePositions(
    triangleCount: number,
    triangles: readonly MonotoneDecompositionTriangulator.Triangle[],
    vertices: readonly number[][],
    positions: number[],
    colors: number[],
    r: number,
    g: number,
    b: number,
  ): void {
    for (let i = 0; i < triangleCount; i++) {
      const t = triangles[i]!;
      const a: number[] = vertices[t.point0]!;
      const b2: number[] = vertices[t.point1]!;
      const c: number[] = vertices[t.point2]!;
      WebGLTriangularRenderer.addLineEdge(positions, colors, a, b2, r, g, b);
      WebGLTriangularRenderer.addLineEdge(positions, colors, b2, c, r, g, b);
      WebGLTriangularRenderer.addLineEdge(positions, colors, c, a, r, g, b);
    }
  }

  private static buildPointPositions(
    triangleCount: number,
    triangles: readonly MonotoneDecompositionTriangulator.Triangle[],
    vertices: readonly number[][],
    positions: number[],
    colors: number[],
    r: number,
    g: number,
    b: number,
  ): void {
    for (let i = 0; i < triangleCount; i++) {
      const t = triangles[i]!;
      WebGLTriangularRenderer.addLinePoint(positions, colors, vertices[t.point0]!, r, g, b);
      WebGLTriangularRenderer.addLinePoint(positions, colors, vertices[t.point1]!, r, g, b);
      WebGLTriangularRenderer.addLinePoint(positions, colors, vertices[t.point2]!, r, g, b);
    }
  }

  private drawTriangleSurfaces(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    positions: number[],
    r: number,
    g: number,
    b: number,
  ): void {
    if (positions.length === 0) {
      return;
    }
    const resources: TriangularRendererResources = this.resources!;

    gl.enable(gl.POLYGON_OFFSET_FILL);
    gl.polygonOffset(1.0, 1.0);

    gl.useProgram(resources.constantProgram);
    WebGLTriangularRenderer.setMvpUniform(gl, resources.constantProgram, mvp);
    WebGLTriangularRenderer.setInteger(gl, resources.constantProgram, 'withTexture', 0);
    WebGLTriangularRenderer.setInteger(gl, resources.constantProgram, 'withVertexColors', 0);
    const diffuseLocation = gl.getUniformLocation(resources.constantProgram, 'diffuseColor');
    if (diffuseLocation !== null) {
      gl.uniform3f(diffuseLocation, r, g, b);
    }

    this.bindConstantAttributes(gl, positions);
    gl.drawArrays(gl.TRIANGLES, 0, positions.length / 3);
    this.unbind(gl);

    gl.polygonOffset(0.0, 0.0);
    gl.disable(gl.POLYGON_OFFSET_FILL);
  }

  private drawTriangleWires(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    positions: number[],
    colors: number[],
  ): void {
    if (positions.length === 0) {
      return;
    }
    const resources: TriangularRendererResources = this.resources!;

    gl.useProgram(resources.lineProgram);
    WebGLTriangularRenderer.setMvpUniform(gl, resources.lineProgram, mvp);
    WebGLTriangularRenderer.setFloat(
      gl,
      resources.lineProgram,
      'depthBiasNdc',
      WebGLTriangularRenderer.WIRE_DEPTH_BIAS_NDC,
    );

    this.bindLineAttributes(gl, positions, colors);
    gl.lineWidth(1.0);
    gl.drawArrays(gl.LINES, 0, positions.length / 3);
    this.unbind(gl);
  }

  private drawTrianglePoints(
    gl: WebGL2RenderingContext,
    mvp: Matrix4x4d,
    positions: number[],
    colors: number[],
  ): void {
    if (positions.length === 0) {
      return;
    }
    const resources: TriangularRendererResources = this.resources!;

    gl.useProgram(resources.lineProgram);
    WebGLTriangularRenderer.setMvpUniform(gl, resources.lineProgram, mvp);
    WebGLTriangularRenderer.setFloat(
      gl,
      resources.lineProgram,
      'depthBiasNdc',
      WebGLTriangularRenderer.POINT_DEPTH_BIAS_NDC,
    );

    this.bindLineAttributes(gl, positions, colors);
    WebGLTriangularRenderer.setFloat(gl, resources.lineProgram, 'pointSizeLocal', 8.0);
    gl.drawArrays(gl.POINTS, 0, positions.length / 3);
    this.unbind(gl);
  }

  private bindConstantAttributes(gl: WebGL2RenderingContext, positions: number[]): void {
    const resources: TriangularRendererResources = this.resources!;
    gl.bindVertexArray(resources.vertexArray);

    gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, WebGLTriangularRenderer.toVec4Array(positions), gl.STREAM_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 4, gl.FLOAT, false, 0, 0);

    gl.disableVertexAttribArray(1);
    gl.vertexAttrib3f(1, 0.0, 0.0, 0.0);
    gl.disableVertexAttribArray(2);
    gl.vertexAttrib2f(2, 0.0, 0.0);
    gl.bindBuffer(gl.ARRAY_BUFFER, null);
  }

  private bindLineAttributes(
    gl: WebGL2RenderingContext,
    positions: number[],
    colors: number[],
  ): void {
    const resources: TriangularRendererResources = this.resources!;
    gl.bindVertexArray(resources.vertexArray);

    gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(positions), gl.STREAM_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(colors), gl.STREAM_DRAW);
    gl.enableVertexAttribArray(1);
    gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, null);
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

  private static setInteger(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: number,
  ): void {
    const location = gl.getUniformLocation(program, name);
    if (location !== null) {
      gl.uniform1i(location, value);
    }
  }

  private static addFillTriangle(positions: number[], a: number[], b: number[], c: number[]): void {
    WebGLTriangularRenderer.addFillPoint(positions, a[0]!, 0.0, a[1]!);
    WebGLTriangularRenderer.addFillPoint(positions, b[0]!, 0.0, b[1]!);
    WebGLTriangularRenderer.addFillPoint(positions, c[0]!, 0.0, c[1]!);
  }

  private static addFillPoint(positions: number[], x: number, y: number, z: number): void {
    positions.push(x, y, z);
  }

  private static addLineEdge(
    positions: number[],
    colors: number[],
    a: number[],
    b: number[],
    r: number,
    g: number,
    bColor: number,
  ): void {
    WebGLTriangularRenderer.addLinePoint(positions, colors, a, r, g, bColor);
    WebGLTriangularRenderer.addLinePoint(positions, colors, b, r, g, bColor);
  }

  private static addLinePoint(
    positions: number[],
    colors: number[],
    p: number[],
    r: number,
    g: number,
    b: number,
  ): void {
    positions.push(p[0]!, 0.0, p[1]!);
    colors.push(r, g, b);
  }

  private static toVec4Array(positions: number[]): Float32Array {
    const out = new Float32Array((positions.length / 3) * 4);
    for (let i = 0, j = 0; i < positions.length; i += 3, j += 4) {
      out[j] = positions[i]!;
      out[j + 1] = positions[i + 1]!;
      out[j + 2] = positions[i + 2]!;
      out[j + 3] = 1.0;
    }
    return out;
  }

  private static flattenVertices(polygon: Polygon2D | null): number[][] {
    const vertices: number[][] = [];
    if (polygon === null) {
      return vertices;
    }

    for (const contour of polygon.loops) {
      for (const vertex of contour.vertices) {
        vertices.push([vertex.x, vertex.y]);
      }
    }
    return vertices;
  }
}
