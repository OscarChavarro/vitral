import {
  Camera,
  ColorRgb,
  Light,
  LightGizmoStyle,
  Matrix4x4d,
  RendererConfiguration,
  SimpleBody,
  SimpleMaterial,
  TriangleMesh,
  TriangleMeshGroup,
  Vector3Dd,
  type Geometry,
  type Image,
} from '@vitral/base';
import {
  WebGLCameraRenderer,
  WebGLImageRenderer,
  WebGLLightRenderer,
  WebGLRayGizmoRenderer,
  WebGLRendererConfigurationShaderSelector,
} from '@vitral/webgl';
import { MeshModel } from '../model/mesh-model';

interface MeshFrame {
  readonly positions: Float32Array;
  readonly normals: Float32Array;
  readonly uvs: Float32Array;
}

interface MeshBuffers {
  readonly vertexArray: WebGLVertexArrayObject;
  readonly positionBuffer: WebGLBuffer;
  readonly normalBuffer: WebGLBuffer;
  readonly uvBuffer: WebGLBuffer;
}

/**
 * Port of
 * `java/testsuite/Jogl4Examples/MeshExample/src/render/Jogl4DebuggerRenderer.java`.
 *
 * The `GLEventListener` callbacks keep their Java names and their Java
 * contents; what changes is who calls them, which in the browser is the
 * example component's lifecycle rather than JOGL's animator.
 *
 * Three places cross a runtime boundary that WebGL draws differently from
 * OpenGL 4, and each is marked where it happens:
 *
 *   - `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)` has no WebGL counterpart, so
 *     the wireframe pass draws the three edges of every triangle as `LINES`
 *     instead of asking the rasterizer to outline the filled primitive. Java's
 *     polygon-offset bias for that pass becomes the depth-func and depth-mask
 *     state it already sets, since `POLYGON_OFFSET_LINE` is equally absent.
 *   - `glPointSize` has no WebGL counterpart either; the size travels to the
 *     vertex shader as the `pointSizeLocal` uniform that
 *     `WebGLShaderPreprocessor` installs.
 *   - Java compiles its shaders once at `init(GLAutoDrawable)`; a browser
 *     reaches a GLSL source over `fetch`, so every method that can reach a
 *     shader program is asynchronous here.
 */
export class WebGLDebuggerRenderer {
  private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 1.0;
  private static readonly SURFACE_POLYGON_OFFSET_UNITS = 1.0;

  private surfaceBuffers: MeshBuffers | null = null;
  private wireBuffers: MeshBuffers | null = null;
  private vertexCount = 0;
  private wireVertexCount = 0;

  constructor(private readonly model: MeshModel) {}

  init(gl: WebGL2RenderingContext): void {
    this.surfaceBuffers = WebGLDebuggerRenderer.createBuffers(gl);
    this.wireBuffers = WebGLDebuggerRenderer.createBuffers(gl);
  }

  async display(gl: WebGL2RenderingContext): Promise<void> {
    gl.enable(gl.DEPTH_TEST);
    gl.depthMask(true);
    gl.depthFunc(gl.LESS);
    gl.disable(gl.CULL_FACE);

    gl.clearColor(0.5, 0.5, 0.9, 1.0);
    gl.clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT);

    const activeLights: Light[] = this.model.getLights();
    if (activeLights.length === 0) {
      return;
    }

    const bodies = this.model.getScene().getSimpleBodies();
    for (let i = 0; i < bodies.size(); i++) {
      await this.drawSimpleBody(
        gl,
        bodies.get(i),
        this.model.getCamera(),
        activeLights,
        this.model.getQualitySelection(),
      );
    }

    for (const light of activeLights) {
      await WebGLLightRenderer.draw(
        gl,
        light,
        this.model.getCamera(),
        LightGizmoStyle.OMNI_BILLBOARD,
      );
    }

    // Apply any pending network update before drawing the ray gizmo so the
    // body state is stable for the entire frame.
    this.model.getRayGizmo().acquireSnapshot();
    await WebGLRayGizmoRenderer.draw(
      gl,
      this.model.getRayGizmo(),
      this.model.getCamera(),
      activeLights,
    );
  }

  reshape(gl: WebGL2RenderingContext, x: number, y: number, width: number, height: number): void {
    gl.viewport(x, y, width, height);
    this.model.getCamera().updateViewportResize(width, height);
  }

  dispose(gl: WebGL2RenderingContext): void {
    WebGLRendererConfigurationShaderSelector.dispose(gl);
    WebGLCameraRenderer.dispose(gl);
    WebGLRayGizmoRenderer.dispose(gl);

    WebGLDebuggerRenderer.deleteBuffers(gl, this.surfaceBuffers);
    WebGLDebuggerRenderer.deleteBuffers(gl, this.wireBuffers);
    this.surfaceBuffers = null;
    this.wireBuffers = null;
  }

  private async drawSimpleBody(
    gl: WebGL2RenderingContext,
    body: SimpleBody,
    camera: Camera,
    lights: Light[],
    quality: RendererConfiguration,
  ): Promise<void> {
    const geometry: Geometry | null = body.getGeometry();
    if (geometry === null) {
      return;
    }

    const meshes: TriangleMesh[] = [];
    if (geometry instanceof TriangleMesh) {
      meshes.push(geometry);
    } else if (geometry instanceof TriangleMeshGroup) {
      for (const groupMesh of geometry.getMeshes()) {
        if (groupMesh instanceof TriangleMesh) {
          meshes.push(groupMesh);
        }
      }
    } else {
      return;
    }

    const modelMatrix: Matrix4x4d = body.getTransformationMatrix();
    const projection: Matrix4x4d = WebGLCameraRenderer.activate(gl, camera);
    const modelViewProjection: Matrix4x4d = projection.multiply(modelMatrix);
    const modelIt: Matrix4x4d = modelMatrix.invert().transpose();

    let material: SimpleMaterial | null = body.getMaterial();
    if (material === null) {
      material = WebGLDebuggerRenderer.defaultMaterial();
    }

    for (const mesh of meshes) {
      const frame: MeshFrame | null = WebGLDebuggerRenderer.buildFrame(mesh);
      if (frame === null || frame.positions.length === 0) {
        continue;
      }

      this.uploadFrame(gl, frame);

      let texture: Image | null = body.getTexture();
      if (texture === null) {
        const textures: Image[] | null = mesh.getTextures();
        if (textures !== null && textures.length > 0) {
          texture = textures[0]!;
        }
      }

      let textureId: WebGLTexture | null = null;
      let withTexture = false;
      if (texture !== null && quality.isTextureSet()) {
        textureId = WebGLImageRenderer.activate(gl, texture);
        withTexture = textureId !== null;
      }

      if (quality.isSurfacesSet()) {
        const program = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
          gl,
          quality,
          withTexture,
          false,
        );
        WebGLDebuggerRenderer.configureProgram(
          gl,
          program,
          modelViewProjection,
          modelMatrix,
          modelIt,
          camera,
          lights,
          material,
          quality,
          withTexture,
          textureId,
        );

        gl.enable(gl.POLYGON_OFFSET_FILL);
        gl.polygonOffset(
          WebGLDebuggerRenderer.SURFACE_POLYGON_OFFSET_FACTOR,
          WebGLDebuggerRenderer.SURFACE_POLYGON_OFFSET_UNITS,
        );
        // Java sets glPolygonMode(GL_FRONT_AND_BACK, GL_FILL); WebGL
        // rasterizes filled polygons only and has no polygon-mode entry point.
        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.bindVertexArray(this.surfaceBuffers!.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, this.vertexCount);
        gl.bindVertexArray(null);
        gl.disable(gl.POLYGON_OFFSET_FILL);
        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
      }

      if (quality.isWiresSet()) {
        const wireQuality = new RendererConfiguration();
        wireQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        wireQuality.setTexture(false);
        wireQuality.setBumpMap(false);
        const program = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
          gl,
          wireQuality,
          false,
          false,
        );
        WebGLDebuggerRenderer.configureProgram(
          gl,
          program,
          modelViewProjection,
          modelMatrix,
          modelIt,
          camera,
          lights,
          WebGLDebuggerRenderer.whiteWireMaterial(),
          wireQuality,
          false,
          null,
        );

        // Java biases the outlined polygon with POLYGON_OFFSET_LINE and one
        // pixel of glLineWidth; WebGL has neither, and the edge list below is
        // drawn as LINES with the depth state Java also sets for this pass.
        gl.depthMask(false);
        gl.depthFunc(gl.LEQUAL);
        gl.bindVertexArray(this.wireBuffers!.vertexArray);
        gl.drawArrays(gl.LINES, 0, this.wireVertexCount);
        gl.bindVertexArray(null);
        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
      }

      if (quality.isPointsSet()) {
        const pointQuality = new RendererConfiguration();
        pointQuality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        pointQuality.setTexture(false);
        pointQuality.setBumpMap(false);
        const program = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
          gl,
          pointQuality,
          false,
          false,
        );
        WebGLDebuggerRenderer.configureProgram(
          gl,
          program,
          modelViewProjection,
          modelMatrix,
          modelIt,
          camera,
          lights,
          WebGLDebuggerRenderer.redPointMaterial(),
          pointQuality,
          false,
          null,
        );

        WebGLDebuggerRenderer.setFloat(gl, program, 'pointSizeLocal', 4.0);
        gl.depthMask(false);
        gl.depthFunc(gl.LEQUAL);
        gl.bindVertexArray(this.surfaceBuffers!.vertexArray);
        gl.drawArrays(gl.POINTS, 0, this.vertexCount);
        gl.bindVertexArray(null);
        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
      }

      gl.bindTexture(gl.TEXTURE_2D, null);
      gl.depthMask(true);
      gl.depthFunc(gl.LESS);
    }
  }

  private static configureProgram(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    modelViewProjection: Matrix4x4d,
    model: Matrix4x4d,
    modelIt: Matrix4x4d,
    camera: Camera,
    lights: Light[],
    material: SimpleMaterial,
    quality: RendererConfiguration,
    withTexture: boolean,
    textureId: WebGLTexture | null,
  ): void {
    const kd: ColorRgb = material.getDiffuse();
    WebGLRendererConfigurationShaderSelector.activateShader(
      gl,
      program,
      modelViewProjection,
      quality,
      kd.r(),
      kd.g(),
      kd.b(),
    );

    WebGLDebuggerRenderer.setMatrix(gl, program, 'modelViewLocal', model);
    WebGLDebuggerRenderer.setMatrix(gl, program, 'modelViewITLocal', modelIt);
    WebGLDebuggerRenderer.setVector3(gl, program, 'cameraPositionGlobal', camera.getPosition());
    let lightCount = 0;
    for (const light of lights) {
      WebGLDebuggerRenderer.setVector3(
        gl,
        program,
        'lightPositionsGlobal[' + lightCount + ']',
        light.getPosition(),
      );
      WebGLDebuggerRenderer.setColor(
        gl,
        program,
        'lightColorsGlobal[' + lightCount + ']',
        light.getEmission(),
      );
      lightCount++;
    }
    WebGLDebuggerRenderer.setInt(gl, program, 'numberOfLights', lightCount);
    WebGLDebuggerRenderer.setColor(gl, program, 'ambientColor', material.getAmbient());
    WebGLDebuggerRenderer.setColor(gl, program, 'diffuseColor', material.getDiffuse());
    WebGLDebuggerRenderer.setColor(gl, program, 'specularColor', material.getSpecular());
    WebGLDebuggerRenderer.setFloat(gl, program, 'phongExponent', material.getPhongExponent());
    WebGLDebuggerRenderer.setInt(gl, program, 'withTexture', withTexture ? 1 : 0);
    WebGLDebuggerRenderer.setInt(gl, program, 'withBumpMap', 0);

    if (withTexture) {
      gl.activeTexture(gl.TEXTURE0);
      gl.bindTexture(gl.TEXTURE_2D, textureId);
    }
  }

  private static buildFrame(mesh: TriangleMesh): MeshFrame | null {
    const indices: Int32Array | null = mesh.getTriangleIndexes();
    const vertices: Float64Array | null = mesh.getVertexPositions();
    if (indices === null || vertices === null || indices.length === 0 || vertices.length === 0) {
      return null;
    }

    const normals: Float64Array | null = mesh.getVertexNormals();
    const uvs: Float64Array | null = mesh.getVertexUvs();
    const hasNormals: boolean = normals !== null && normals.length >= vertices.length;
    const hasUvs: boolean =
      uvs !== null && Math.trunc(uvs.length / 2) >= Math.trunc(vertices.length / 3);

    const outPositions = new Float32Array(indices.length * 3);
    const outNormals = new Float32Array(indices.length * 3);
    const outUvs = new Float32Array(indices.length * 2);

    let p = 0;
    let n = 0;
    let t = 0;
    for (const idx of indices) {
      const vp: number = idx * 3;
      outPositions[p++] = vertices[vp]!;
      outPositions[p++] = vertices[vp + 1]!;
      outPositions[p++] = vertices[vp + 2]!;

      if (hasNormals) {
        outNormals[n++] = normals![vp]!;
        outNormals[n++] = normals![vp + 1]!;
        outNormals[n++] = normals![vp + 2]!;
      } else {
        outNormals[n++] = 0.0;
        outNormals[n++] = 0.0;
        outNormals[n++] = 1.0;
      }

      if (hasUvs) {
        const uv: number = idx * 2;
        outUvs[t++] = uvs![uv]!;
        outUvs[t++] = uvs![uv + 1]!;
      } else {
        outUvs[t++] = 0.0;
        outUvs[t++] = 0.0;
      }
    }

    return { positions: outPositions, normals: outNormals, uvs: outUvs };
  }

  /**
   * The edge list that stands in for `glPolygonMode(..., GL_LINE)`: every
   * triangle of the frame becomes its three edges, so the wireframe pass can
   * draw `LINES` over exactly the geometry the surface pass filled.
   */
  private static buildWireFrame(frame: MeshFrame): MeshFrame {
    const triangleCount: number = Math.trunc(frame.positions.length / 9);
    const positions = new Float32Array(triangleCount * 6 * 3);
    const normals = new Float32Array(triangleCount * 6 * 3);
    const uvs = new Float32Array(triangleCount * 6 * 2);

    let p = 0;
    let n = 0;
    let t = 0;
    const edges: readonly (readonly number[])[] = [
      [0, 1],
      [1, 2],
      [2, 0],
    ];

    for (let triangle = 0; triangle < triangleCount; triangle++) {
      for (const edge of edges) {
        for (const corner of edge) {
          const vertex: number = triangle * 3 + corner;
          positions[p++] = frame.positions[vertex * 3]!;
          positions[p++] = frame.positions[vertex * 3 + 1]!;
          positions[p++] = frame.positions[vertex * 3 + 2]!;
          normals[n++] = frame.normals[vertex * 3]!;
          normals[n++] = frame.normals[vertex * 3 + 1]!;
          normals[n++] = frame.normals[vertex * 3 + 2]!;
          uvs[t++] = frame.uvs[vertex * 2]!;
          uvs[t++] = frame.uvs[vertex * 2 + 1]!;
        }
      }
    }

    return { positions, normals, uvs };
  }

  private static defaultMaterial(): SimpleMaterial {
    let m = new SimpleMaterial();
    m = m.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(new ColorRgb(0.8, 0.8, 0.8));
    m = m.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
    m = m.withPhongExponent(32.0);
    return m;
  }

  private static whiteWireMaterial(): SimpleMaterial {
    let m = new SimpleMaterial();
    m = m.withAmbient(new ColorRgb(0.0, 0.0, 0.0));
    m = m.withDiffuse(new ColorRgb(1.0, 1.0, 1.0));
    m = m.withSpecular(new ColorRgb(0.0, 0.0, 0.0));
    m = m.withPhongExponent(1.0);
    return m;
  }

  private static redPointMaterial(): SimpleMaterial {
    let m = new SimpleMaterial();
    m = m.withAmbient(new ColorRgb(0.0, 0.0, 0.0));
    m = m.withDiffuse(new ColorRgb(1.0, 0.0, 0.0));
    m = m.withSpecular(new ColorRgb(0.0, 0.0, 0.0));
    m = m.withPhongExponent(1.0);
    return m;
  }

  private uploadFrame(gl: WebGL2RenderingContext, frame: MeshFrame): void {
    this.vertexCount = Math.trunc(frame.positions.length / 3);
    WebGLDebuggerRenderer.uploadInto(gl, this.surfaceBuffers!, frame);

    const wireFrame: MeshFrame = WebGLDebuggerRenderer.buildWireFrame(frame);
    this.wireVertexCount = Math.trunc(wireFrame.positions.length / 3);
    WebGLDebuggerRenderer.uploadInto(gl, this.wireBuffers!, wireFrame);
  }

  private static uploadInto(
    gl: WebGL2RenderingContext,
    buffers: MeshBuffers,
    frame: MeshFrame,
  ): void {
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

  private static createBuffers(gl: WebGL2RenderingContext): MeshBuffers {
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
      throw new Error('Failed to create mesh renderer buffers');
    }
    return { vertexArray, positionBuffer, normalBuffer, uvBuffer };
  }

  private static deleteBuffers(gl: WebGL2RenderingContext, buffers: MeshBuffers | null): void {
    if (buffers === null) {
      return;
    }
    gl.deleteBuffer(buffers.positionBuffer);
    gl.deleteBuffer(buffers.normalBuffer);
    gl.deleteBuffer(buffers.uvBuffer);
    gl.deleteVertexArray(buffers.vertexArray);
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

  private static setColor(
    gl: WebGL2RenderingContext,
    program: WebGLProgram,
    name: string,
    value: ColorRgb,
  ): void {
    const loc = gl.getUniformLocation(program, name);
    if (loc !== null) {
      gl.uniform3f(loc, value.r(), value.g(), value.b());
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
