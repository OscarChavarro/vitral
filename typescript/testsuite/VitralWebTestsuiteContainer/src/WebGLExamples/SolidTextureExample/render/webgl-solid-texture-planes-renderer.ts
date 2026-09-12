import {
  Camera,
  InfinitePlane,
  Matrix4x4d,
  RendererConfiguration,
  Vector3Dd,
  Vector4Dd,
  type Image,
} from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLImageRenderer,
  WebGLRendererConfigurationShaderSelector,
} from '@vitral/webgl';

interface PlaneFrame {
  readonly positions: Float32Array;
  readonly normals: Float32Array;
  readonly uvs: Float32Array;
}

interface PlaneBuffers {
  readonly vertexArray: WebGLVertexArrayObject;
  readonly positionBuffer: WebGLBuffer;
  readonly normalBuffer: WebGLBuffer;
  readonly uvBuffer: WebGLBuffer;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/SolidTextureExample/src/render/Jogl4SolidTexturePlanesRenderer.java`.
 *
 * The second operation mode: the slices of the solid texture drawn as a stack
 * of textured quads from z = -1 to z = 1, one quad per slice, so the volume can
 * be inspected layer by layer instead of carved into geometry. The quad
 * construction, the texture-coordinate winding, the flat-shaded textured
 * program and its uniform sequence, and the bookkeeping that unloads an image
 * no longer in the stack are the Java ones.
 *
 * Three runtime boundaries are crossed:
 *
 *   - `glPolygonMode(GL_FRONT_AND_BACK, GL_FILL)` has no WebGL counterpart,
 *     which costs nothing because WebGL rasterizes filled polygons only.
 *   - `glEnable(GL_CLIP_DISTANCE0)` has no counterpart either, and neither does
 *     `gl_ClipDistance` in GLSL ES. The two `clippingPlane*` uniforms are set
 *     where Java sets them, and `WebGLShaderPreprocessor` turns the vertex
 *     shader's clip-distance write into a varying the fragment shader discards
 *     on.
 *   - A GLSL source arrives over `fetch`, so drawing is asynchronous.
 *
 * Java's `IdentityHashMap` of uploaded images is a `Set` here: a JavaScript
 * `Set` already compares by object identity, which is what the Java type was
 * chosen for.
 */
export class WebGLSolidTexturePlanesRenderer {
  private buffers: PlaneBuffers | null = null;
  private readonly uploadedImages: Image[] = [];

  async draw(
    gl: WebGL2RenderingContext,
    images: Image[] | null,
    camera: Camera | null,
    clippingPlane: InfinitePlane | null = null,
  ): Promise<void> {
    if (images === null || images.length === 0 || camera === null) {
      return;
    }

    this.ensureBuffers(gl);
    this.unloadImagesNotIn(gl, images);

    const planeCount: number = images.length;
    const frame: PlaneFrame = WebGLSolidTexturePlanesRenderer.buildPlaneFrame(planeCount);
    this.uploadFrame(gl, frame);

    const quality = new RendererConfiguration();
    quality.setShadingType(RendererConfiguration.SHADING_TYPE_FLAT);
    quality.setTexture(true);
    quality.setBumpMap(false);

    const program = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
      gl,
      quality,
      true,
      false,
    );
    const identity: Matrix4x4d = Matrix4x4d.identityMatrix();
    const modelViewProjection: Matrix4x4d = WebGLCameraRenderer.activate(gl, camera);
    WebGLRendererConfigurationShaderSelector.activateShader(
      gl,
      program,
      modelViewProjection,
      quality,
      1.0,
      1.0,
      1.0,
    );
    WebGLSolidTexturePlanesRenderer.configureTexturedProgram(gl, program, identity, camera);
    WebGLSolidTexturePlanesRenderer.configureClippingPlane(gl, program, clippingPlane);

    gl.disable(gl.CULL_FACE);
    gl.enable(gl.DEPTH_TEST);
    gl.depthMask(true);
    gl.depthFunc(gl.LESS);
    gl.bindVertexArray(this.buffers!.vertexArray);
    for (let i = 0; i < WebGLSolidTexturePlanesRenderer.quadCount(frame); i++) {
      const image: Image = images[i]!;
      const textureId: WebGLTexture | null = WebGLImageRenderer.activate(gl, image);
      WebGLSolidTexturePlanesRenderer.setInt(
        gl,
        program,
        'withTexture',
        textureId !== null ? 1 : 0,
      );
      if (textureId !== null) {
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, textureId);
      }
      gl.drawArrays(gl.TRIANGLE_FAN, i * 4, 4);
    }
    gl.bindTexture(gl.TEXTURE_2D, null);
    gl.bindVertexArray(null);
    WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
  }

  dispose(gl: WebGL2RenderingContext): void {
    if (this.buffers !== null) {
      gl.deleteBuffer(this.buffers.positionBuffer);
      gl.deleteBuffer(this.buffers.normalBuffer);
      gl.deleteBuffer(this.buffers.uvBuffer);
      gl.deleteVertexArray(this.buffers.vertexArray);
      this.buffers = null;
    }
    this.unloadAllImages(gl);
  }

  private unloadImagesNotIn(gl: WebGL2RenderingContext, images: readonly Image[]): void {
    const currentImages = new Set<Image>(images);

    for (let i = this.uploadedImages.length - 1; i >= 0; i--) {
      const uploaded: Image = this.uploadedImages[i]!;
      if (!currentImages.has(uploaded)) {
        WebGLImageRenderer.unload(gl, uploaded);
        this.uploadedImages.splice(i, 1);
      }
    }

    for (const image of images) {
      if (!this.uploadedImages.includes(image)) {
        this.uploadedImages.push(image);
      }
    }
  }

  private unloadAllImages(gl: WebGL2RenderingContext): void {
    for (const image of this.uploadedImages) {
      WebGLImageRenderer.unload(gl, image);
    }
    this.uploadedImages.length = 0;
  }

  private ensureBuffers(gl: WebGL2RenderingContext): void {
    if (this.buffers !== null) {
      return;
    }
    const vertexArray = gl.createVertexArray();
    const positionBuffer = gl.createBuffer();
    const normalBuffer = gl.createBuffer();
    const uvBuffer = gl.createBuffer();
    if (
      vertexArray === null ||
      positionBuffer === null ||
      normalBuffer === null ||
      uvBuffer === null
    ) {
      throw new Error('Failed to create solid texture planes renderer buffers');
    }
    this.buffers = { vertexArray, positionBuffer, normalBuffer, uvBuffer };
  }

  private static buildPlaneFrame(planeCount: number): PlaneFrame {
    const vertexCount: number = planeCount * 4;
    const positions = new Float32Array(vertexCount * 3);
    const normals = new Float32Array(vertexCount * 3);
    const uvs = new Float32Array(vertexCount * 2);

    let p = 0;
    let n = 0;
    let t = 0;
    for (let i = 0; i < planeCount; i++) {
      const z: number = planeCount === 1 ? -1.0 : -1.0 + (2.0 * i) / (planeCount - 1);

      p = WebGLSolidTexturePlanesRenderer.appendVertex(positions, p, -1.0, -1.0, z);
      p = WebGLSolidTexturePlanesRenderer.appendVertex(positions, p, 1.0, -1.0, z);
      p = WebGLSolidTexturePlanesRenderer.appendVertex(positions, p, 1.0, 1.0, z);
      p = WebGLSolidTexturePlanesRenderer.appendVertex(positions, p, -1.0, 1.0, z);

      for (let j = 0; j < 4; j++) {
        n = WebGLSolidTexturePlanesRenderer.appendVertex(normals, n, 0.0, 0.0, 1.0);
      }

      t = WebGLSolidTexturePlanesRenderer.appendUv(uvs, t, 0.0, 0.0);
      t = WebGLSolidTexturePlanesRenderer.appendUv(uvs, t, 1.0, 0.0);
      t = WebGLSolidTexturePlanesRenderer.appendUv(uvs, t, 1.0, 1.0);
      t = WebGLSolidTexturePlanesRenderer.appendUv(uvs, t, 0.0, 1.0);
    }

    return { positions, normals, uvs };
  }

  /** Java's `PlaneFrame.quadCount()`, on a record this port spells as data. */
  private static quadCount(frame: PlaneFrame): number {
    return Math.trunc(frame.positions.length / 12);
  }

  private uploadFrame(gl: WebGL2RenderingContext, frame: PlaneFrame): void {
    const buffers: PlaneBuffers = this.buffers!;
    gl.bindVertexArray(buffers.vertexArray);

    gl.bindBuffer(gl.ARRAY_BUFFER, buffers.positionBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, frame.positions, gl.STREAM_DRAW);
    gl.enableVertexAttribArray(0);
    gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, buffers.normalBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, frame.normals, gl.STREAM_DRAW);
    gl.enableVertexAttribArray(1);
    gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, buffers.uvBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, frame.uvs, gl.STREAM_DRAW);
    gl.enableVertexAttribArray(2);
    gl.vertexAttribPointer(2, 2, gl.FLOAT, false, 0, 0);

    gl.bindBuffer(gl.ARRAY_BUFFER, null);
    gl.bindVertexArray(null);
  }

  private static configureTexturedProgram(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    identity: Matrix4x4d,
    camera: Camera,
  ): void {
    WebGLSolidTexturePlanesRenderer.setMatrix(gl, program, 'modelViewLocal', identity);
    WebGLSolidTexturePlanesRenderer.setMatrix(gl, program, 'modelViewITLocal', identity);
    WebGLSolidTexturePlanesRenderer.setVector3(
      gl,
      program,
      'cameraPositionGlobal',
      camera.getPosition(),
    );
    WebGLSolidTexturePlanesRenderer.setVector3(
      gl,
      program,
      'lightPositionsGlobal[0]',
      new Vector3Dd(0.0, 0.0, 5.0),
    );
    WebGLSolidTexturePlanesRenderer.setVector3(
      gl,
      program,
      'lightColorsGlobal[0]',
      new Vector3Dd(1.0, 1.0, 1.0),
    );
    WebGLSolidTexturePlanesRenderer.setVector3(
      gl,
      program,
      'ambientColor',
      new Vector3Dd(1.0, 1.0, 1.0),
    );
    WebGLSolidTexturePlanesRenderer.setVector3(
      gl,
      program,
      'diffuseColor',
      new Vector3Dd(1.0, 1.0, 1.0),
    );
    WebGLSolidTexturePlanesRenderer.setVector3(
      gl,
      program,
      'specularColor',
      new Vector3Dd(0.0, 0.0, 0.0),
    );
    WebGLSolidTexturePlanesRenderer.setInt(gl, program, 'numberOfLights', 1);
    WebGLSolidTexturePlanesRenderer.setInt(gl, program, 'withTexture', 1);
    WebGLSolidTexturePlanesRenderer.setInt(gl, program, 'withBumpMap', 0);
    WebGLSolidTexturePlanesRenderer.setFloat(gl, program, 'phongExponent', 1.0);
  }

  private static appendVertex(
    values: Float32Array,
    offset: number,
    x: number,
    y: number,
    z: number,
  ): number {
    values[offset++] = x;
    values[offset++] = y;
    values[offset++] = z;
    return offset;
  }

  private static appendUv(values: Float32Array, offset: number, u: number, v: number): number {
    values[offset++] = u;
    values[offset++] = v;
    return offset;
  }

  private static configureClippingPlane(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    clippingPlane: InfinitePlane | null,
  ): void {
    if (clippingPlane === null) {
      WebGLSolidTexturePlanesRenderer.setInt(gl, program, 'clippingPlaneEnabled', 0);
      return;
    }

    WebGLSolidTexturePlanesRenderer.setInt(gl, program, 'clippingPlaneEnabled', 1);
    WebGLSolidTexturePlanesRenderer.setVector4(
      gl,
      program,
      'clippingPlaneGlobal',
      new Vector4Dd(
        clippingPlane.getA(),
        clippingPlane.getB(),
        clippingPlane.getC(),
        clippingPlane.getD(),
      ),
    );
  }

  private static setMatrix(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    matrix: Matrix4x4d,
  ): void {
    const loc = gl.getUniformLocation(program, name);
    if (loc !== null) {
      gl.uniformMatrix4fv(loc, false, matrix.exportToFloatArrayColumnOrder());
    }
  }

  private static setVector3(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: Vector3Dd,
  ): void {
    const loc = gl.getUniformLocation(program, name);
    if (loc !== null) {
      gl.uniform3f(loc, value.x(), value.y(), value.z());
    }
  }

  private static setVector4(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: Vector4Dd,
  ): void {
    const loc = gl.getUniformLocation(program, name);
    if (loc !== null) {
      gl.uniform4f(loc, value.x(), value.y(), value.z(), value.w());
    }
  }

  private static setInt(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: number,
  ): void {
    const loc = gl.getUniformLocation(program, name);
    if (loc !== null) {
      gl.uniform1i(loc, value);
    }
  }

  private static setFloat(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: number,
  ): void {
    const loc = gl.getUniformLocation(program, name);
    if (loc !== null) {
      gl.uniform1f(loc, value);
    }
  }
}
