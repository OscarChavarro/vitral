import {
    Camera,
    ColorRgb,
    InfinitePlane,
    Light,
    Matrix4x4d,
    RendererConfiguration,
    SimpleBody,
    SimpleScene,
    TriangleMesh,
    TriangleMeshGroup,
    Vector3Dd,
    Vector4Dd,
    type Geometry,
} from "@vitral/base";
import { WebGLCameraRenderer } from "./WebGLCameraRenderer.js";
import { WebGLShaderProgramUtil } from "./WebGLShaderProgramUtil.js";

interface SolidTextureMeshFrame {
    readonly positions: Float32Array;
    readonly normals: Float32Array;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4SolidTextureRenderer`.

Draws a scene shaded by a 3D texture: every body's object-space position is
normalized against its own bounding box and used as the texture coordinate, so
the solid texture appears carved out of the body rather than wrapped around it.
The volume is uploaded once per revision, the per-body uniform sequence is the
Java one, and the shaders are the very same `solidTextureVertexShader.glsl` and
`solidTexturePixelShader.glsl`.

Four runtime boundaries are crossed:

  - Java's constructor takes the `Path` of the shader directory, because it
    reads the two GLSL files from disk. A browser has no file system: the
    shader tree is served, and {@link WebGLShaderLoader} owns its base URL, so
    this renderer takes no constructor argument at all.
  - A GLSL source arrives over `fetch`, so {@link draw} is asynchronous and
    {@link prepare} exists to compile the program where a JOGL program does its
    `init(GLAutoDrawable)` work; see the drawing-buffer rule recorded for this
    package.
  - `glEnable(GL_CLIP_DISTANCE0)` has no WebGL counterpart, and neither does
    `gl_ClipDistance` in GLSL ES. The two `clippingPlane*` uniforms are set
    exactly where Java sets them, and `WebGLShaderPreprocessor` turns the
    vertex shader's clip-distance write into a varying the fragment shader
    discards on; the enable and disable calls have nothing to map onto and are
    gone.
  - `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)`, `GL_POLYGON_OFFSET_LINE`,
    `glLineWidth` and `glPointSize` have no WebGL counterparts. The wire pass
    draws the three edges of every triangle as `LINES` (with the depth state
    Java also sets for that pass), and the point size travels to the vertex
    shader as the `pointSizeLocal` uniform `WebGLShaderPreprocessor` installs.
  - A WebGL texture is an object rather than an `int`, so Java's
    `solidTextureId <= 0` guard is a `null` check, and the upload needs no
    intermediate direct `ByteBuffer` — a `Uint8Array` is what `texImage3D`
    takes.
*/
export class WebGLSolidTextureRenderer {
    private static readonly VERTEX_SHADER_FILE = "solidTextureVertexShader.glsl";
    private static readonly FRAGMENT_SHADER_FILE = "solidTexturePixelShader.glsl";
    private static readonly MAX_LIGHTS = 8;
    private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 1.0;
    private static readonly SURFACE_POLYGON_OFFSET_UNITS = 1.0;
    private static readonly POINT_SIZE = 4.0;
    private static readonly POINT_COLOR = new ColorRgb(1, 0, 0);

    private vertexArray: WebGLVertexArrayObject | null = null;
    private positionBuffer: WebGLBuffer | null = null;
    private normalBuffer: WebGLBuffer | null = null;
    private vertexCount = 0;
    private wireVertexArray: WebGLVertexArrayObject | null = null;
    private wirePositionBuffer: WebGLBuffer | null = null;
    private wireNormalBuffer: WebGLBuffer | null = null;
    private wireVertexCount = 0;
    private program: WebGLProgram | null = null;
    private solidTexture: WebGLTexture | null = null;
    private uploadedTextureRevision = Number.NEGATIVE_INFINITY;
    private uploadedTextureSize = 0;

    /**
    Compiles the program without drawing; see the class documentation.
    */
    public async prepare(gl: WebGL2RenderingContext): Promise<void> {
        await this.ensureProgram(gl);
    }

    /**
    Java's three `draw` overloads are one method with optional trailing
    arguments: without `quality`, the scene is drawn as Java's overloads
    without a `RendererConfiguration` do, as textured surfaces with Phong
    shading.
    */
    public async draw(
        gl: WebGL2RenderingContext,
        scene: SimpleScene | null,
        camera: Camera | null,
        lights: Light[] | null,
        solidTextureVolumeRgb8: Uint8Array | null,
        solidTextureSize: number,
        solidTextureRevision: number,
        clippingPlane: InfinitePlane | null = null,
        quality: RendererConfiguration | null = null,
    ): Promise<void> {
        if (quality === null) {
            quality = new RendererConfiguration();
            quality.setShadingType(RendererConfiguration.SHADING_TYPE_PHONG);
        }
        if (scene === null || camera === null || lights === null) {
            return;
        }
        this.ensureBuffers(gl);
        const program: WebGLProgram = await this.ensureProgram(gl);
        this.ensureSolidTexture(gl, solidTextureVolumeRgb8, solidTextureSize, solidTextureRevision);

        gl.enable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);

        gl.useProgram(program);
        WebGLSolidTextureRenderer.configureClippingPlane(gl, program, clippingPlane);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_3D, this.solidTexture);
        WebGLSolidTextureRenderer.setInt(gl, program, "sSolidTexture", 0);
        WebGLSolidTextureRenderer.setInt(
            gl,
            program,
            "solidTextureDisabled",
            quality.isTextureSet() && this.solidTexture !== null ? 0 : 1,
        );
        WebGLSolidTextureRenderer.setInt(gl, program, "shadingTypeSelected", 1);
        WebGLSolidTextureRenderer.setInt(gl, program, "shadingType", quality.getShadingType());
        WebGLSolidTextureRenderer.setInt(
            gl,
            program,
            "numberOfLights",
            Math.min(lights.length, WebGLSolidTextureRenderer.MAX_LIGHTS),
        );
        WebGLSolidTextureRenderer.setFloat(gl, program, "phongExponent", 24.0);

        for (let i = 0; i < lights.length && i < WebGLSolidTextureRenderer.MAX_LIGHTS; i++) {
            const light: Light = lights[i]!;
            WebGLSolidTextureRenderer.setVector3(gl, program, "lightPositionsGlobal[" + i + "]", light.getPosition());
            WebGLSolidTextureRenderer.setColor(gl, program, "lightColorsGlobal[" + i + "]", light.getEmission());
        }

        WebGLSolidTextureRenderer.setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());

        const bodies = scene.getSimpleBodies();
        for (let i = 0; i < bodies.size(); i++) {
            this.drawBody(gl, program, bodies.get(i), camera, quality);
        }

        gl.bindVertexArray(null);
        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.bindTexture(gl.TEXTURE_3D, null);
        gl.useProgram(null);
    }

    public dispose(gl: WebGL2RenderingContext): void {
        if (this.solidTexture !== null) {
            gl.deleteTexture(this.solidTexture);
            this.solidTexture = null;
        }
        if (this.positionBuffer !== null) {
            gl.deleteBuffer(this.positionBuffer);
            this.positionBuffer = null;
        }
        if (this.normalBuffer !== null) {
            gl.deleteBuffer(this.normalBuffer);
            this.normalBuffer = null;
        }
        if (this.vertexArray !== null) {
            gl.deleteVertexArray(this.vertexArray);
            this.vertexArray = null;
        }
        if (this.wirePositionBuffer !== null) {
            gl.deleteBuffer(this.wirePositionBuffer);
            this.wirePositionBuffer = null;
        }
        if (this.wireNormalBuffer !== null) {
            gl.deleteBuffer(this.wireNormalBuffer);
            this.wireNormalBuffer = null;
        }
        if (this.wireVertexArray !== null) {
            gl.deleteVertexArray(this.wireVertexArray);
            this.wireVertexArray = null;
        }
        if (this.program !== null) {
            gl.deleteProgram(this.program);
            this.program = null;
        }
        this.uploadedTextureRevision = Number.NEGATIVE_INFINITY;
        this.uploadedTextureSize = 0;
    }

    private drawBody(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        body: SimpleBody,
        camera: Camera,
        quality: RendererConfiguration,
    ): void {
        const geometry: Geometry | null = body.getGeometry();
        if (geometry === null) {
            return;
        }

        const bounds: Float64Array | number[] | null = geometry.getMinMax();
        if (bounds === null || bounds.length < 6) {
            return;
        }
        const min = new Vector3Dd(bounds[0]!, bounds[1]!, bounds[2]!);
        const max = new Vector3Dd(bounds[3]!, bounds[4]!, bounds[5]!);
        WebGLSolidTextureRenderer.setVector3(gl, program, "boundingBoxMinObject", min);
        WebGLSolidTextureRenderer.setVector3(gl, program, "boundingBoxMaxObject", max);

        const modelMatrix: Matrix4x4d = body.getTransformationMatrix();
        const projection: Matrix4x4d = WebGLCameraRenderer.activate(gl, camera);
        const modelViewProjection: Matrix4x4d = projection.multiply(modelMatrix);
        const modelIt: Matrix4x4d = modelMatrix.invert().transpose();
        WebGLSolidTextureRenderer.setMatrix(gl, program, "modelViewProjectionLocal", modelViewProjection);
        WebGLSolidTextureRenderer.setMatrix(gl, program, "modelViewLocal", modelMatrix);
        WebGLSolidTextureRenderer.setMatrix(gl, program, "modelViewITLocal", modelIt);

        for (const mesh of WebGLSolidTextureRenderer.meshesOf(geometry)) {
            const frame: SolidTextureMeshFrame | null = WebGLSolidTextureRenderer.buildFrame(mesh);
            if (frame === null || frame.positions.length === 0) {
                continue;
            }
            this.uploadFrame(gl, frame, quality.isWiresSet());
            this.drawMesh(gl, program, quality);
        }
    }

    /**
    Draws the currently uploaded mesh once per primitive kind enabled in the
    given configuration: shaded surfaces, wires and points.
    */
    private drawMesh(gl: WebGL2RenderingContext, program: WebGLProgram, quality: RendererConfiguration): void {
        if (quality.isSurfacesSet()) {
            WebGLSolidTextureRenderer.setInt(gl, program, "constantColorEnabled", 0);
            gl.enable(gl.POLYGON_OFFSET_FILL);
            gl.polygonOffset(
                WebGLSolidTextureRenderer.SURFACE_POLYGON_OFFSET_FACTOR,
                WebGLSolidTextureRenderer.SURFACE_POLYGON_OFFSET_UNITS,
            );
            gl.depthMask(true);
            gl.depthFunc(gl.LESS);
            gl.bindVertexArray(this.vertexArray);
            gl.drawArrays(gl.TRIANGLES, 0, this.vertexCount);
            gl.disable(gl.POLYGON_OFFSET_FILL);
        }

        if (quality.isWiresSet()) {
            WebGLSolidTextureRenderer.setInt(gl, program, "constantColorEnabled", 1);
            WebGLSolidTextureRenderer.setColor(gl, program, "constantColor", quality.getWireColor());
            // Java outlines the filled triangles with glPolygonMode(GL_LINE),
            // biased by POLYGON_OFFSET_LINE; see the class documentation.
            gl.depthMask(false);
            gl.depthFunc(gl.LEQUAL);
            gl.bindVertexArray(this.wireVertexArray);
            gl.drawArrays(gl.LINES, 0, this.wireVertexCount);
        }

        if (quality.isPointsSet()) {
            WebGLSolidTextureRenderer.setInt(gl, program, "constantColorEnabled", 1);
            WebGLSolidTextureRenderer.setColor(gl, program, "constantColor", WebGLSolidTextureRenderer.POINT_COLOR);
            WebGLSolidTextureRenderer.setFloat(gl, program, "pointSizeLocal", WebGLSolidTextureRenderer.POINT_SIZE);
            gl.depthMask(false);
            gl.depthFunc(gl.LEQUAL);
            gl.bindVertexArray(this.vertexArray);
            gl.drawArrays(gl.POINTS, 0, this.vertexCount);
            WebGLSolidTextureRenderer.setFloat(gl, program, "pointSizeLocal", 0.0);
        }

        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
    }

    /**
    The edge list that stands in for `glPolygonMode(..., GL_LINE)`: every
    triangle of the frame becomes its three edges, so the wire pass can draw
    `LINES` over exactly the geometry the surface pass filled.
    */
    private static buildWireFrame(frame: SolidTextureMeshFrame): SolidTextureMeshFrame {
        const triangleCount: number = Math.trunc(frame.positions.length / 9);
        const positions = new Float32Array(triangleCount * 6 * 3);
        const normals = new Float32Array(triangleCount * 6 * 3);
        const edges: readonly (readonly number[])[] = [
            [0, 1],
            [1, 2],
            [2, 0],
        ];

        let p = 0;
        for (let triangle = 0; triangle < triangleCount; triangle++) {
            for (const edge of edges) {
                for (const corner of edge) {
                    const vertex: number = (triangle * 3 + corner) * 3;
                    positions[p] = frame.positions[vertex]!;
                    positions[p + 1] = frame.positions[vertex + 1]!;
                    positions[p + 2] = frame.positions[vertex + 2]!;
                    normals[p] = frame.normals[vertex]!;
                    normals[p + 1] = frame.normals[vertex + 1]!;
                    normals[p + 2] = frame.normals[vertex + 2]!;
                    p += 3;
                }
            }
        }
        return { positions, normals };
    }

    private static meshesOf(geometry: Geometry): TriangleMesh[] {
        const meshes: TriangleMesh[] = [];
        if (geometry instanceof TriangleMesh) {
            meshes.push(geometry);
        } else if (geometry instanceof TriangleMeshGroup) {
            for (const groupMesh of geometry.getMeshes()) {
                if (groupMesh instanceof TriangleMesh) {
                    meshes.push(groupMesh);
                }
            }
        }
        return meshes;
    }

    private static buildFrame(mesh: TriangleMesh): SolidTextureMeshFrame | null {
        const indices: Int32Array | null = mesh.getTriangleIndexes();
        const vertices: Float64Array | null = mesh.getVertexPositions();
        if (indices === null || vertices === null || indices.length === 0 || vertices.length === 0) {
            return null;
        }

        const normals: Float64Array | null = mesh.getVertexNormals();
        const hasNormals: boolean = normals !== null && normals.length >= vertices.length;
        const outPositions = new Float32Array(indices.length * 3);
        const outNormals = new Float32Array(indices.length * 3);

        let p = 0;
        let n = 0;
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
        }
        return { positions: outPositions, normals: outNormals };
    }

    /**
    @param withWires whether to also upload the edge list of the wire pass,
    which Java does not need (see the class documentation)
    */
    private uploadFrame(gl: WebGL2RenderingContext, frame: SolidTextureMeshFrame, withWires: boolean): void {
        this.vertexCount = Math.trunc(frame.positions.length / 3);
        WebGLSolidTextureRenderer.uploadInto(gl, this.vertexArray, this.positionBuffer, this.normalBuffer, frame);
        if (withWires) {
            const wireFrame: SolidTextureMeshFrame = WebGLSolidTextureRenderer.buildWireFrame(frame);
            this.wireVertexCount = Math.trunc(wireFrame.positions.length / 3);
            WebGLSolidTextureRenderer.uploadInto(
                gl,
                this.wireVertexArray,
                this.wirePositionBuffer,
                this.wireNormalBuffer,
                wireFrame,
            );
        }
    }

    private static uploadInto(
        gl: WebGL2RenderingContext,
        vertexArray: WebGLVertexArrayObject | null,
        positionBuffer: WebGLBuffer | null,
        normalBuffer: WebGLBuffer | null,
        frame: SolidTextureMeshFrame,
    ): void {
        gl.bindVertexArray(vertexArray);

        gl.bindBuffer(gl.ARRAY_BUFFER, positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, frame.positions, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, normalBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, frame.normals, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
    }

    private ensureBuffers(gl: WebGL2RenderingContext): void {
        if (this.vertexArray !== null) {
            return;
        }
        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const normalBuffer = gl.createBuffer();
        const wireVertexArray = gl.createVertexArray();
        const wirePositionBuffer = gl.createBuffer();
        const wireNormalBuffer = gl.createBuffer();
        if (
            vertexArray === null ||
            positionBuffer === null ||
            normalBuffer === null ||
            wireVertexArray === null ||
            wirePositionBuffer === null ||
            wireNormalBuffer === null
        ) {
            throw new Error("Failed to create solid texture renderer buffers");
        }
        this.vertexArray = vertexArray;
        this.positionBuffer = positionBuffer;
        this.normalBuffer = normalBuffer;
        this.wireVertexArray = wireVertexArray;
        this.wirePositionBuffer = wirePositionBuffer;
        this.wireNormalBuffer = wireNormalBuffer;
    }

    private async ensureProgram(gl: WebGL2RenderingContext): Promise<WebGLProgram> {
        if (this.program !== null) {
            return this.program;
        }
        this.program = await WebGLShaderProgramUtil.createProgramFromFiles(
            gl,
            WebGLSolidTextureRenderer.VERTEX_SHADER_FILE,
            WebGLSolidTextureRenderer.FRAGMENT_SHADER_FILE,
        );
        return this.program;
    }

    private ensureSolidTexture(
        gl: WebGL2RenderingContext,
        volume: Uint8Array | null,
        size: number,
        revision: number,
    ): void {
        if (volume === null || volume.length === 0 || size <= 0) {
            return;
        }
        if (
            this.solidTexture !== null &&
            this.uploadedTextureRevision === revision &&
            this.uploadedTextureSize === size
        ) {
            return;
        }

        if (this.solidTexture === null) {
            this.solidTexture = gl.createTexture();
        }

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_3D, this.solidTexture);
        gl.pixelStorei(gl.UNPACK_ALIGNMENT, 1);
        gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
        gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_3D, gl.TEXTURE_WRAP_R, gl.CLAMP_TO_EDGE);
        gl.texImage3D(gl.TEXTURE_3D, 0, gl.RGB8, size, size, size, 0, gl.RGB, gl.UNSIGNED_BYTE, volume);
        gl.bindTexture(gl.TEXTURE_3D, null);

        this.uploadedTextureRevision = revision;
        this.uploadedTextureSize = size;
    }

    private static configureClippingPlane(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        clippingPlane: InfinitePlane | null,
    ): void {
        if (clippingPlane === null) {
            WebGLSolidTextureRenderer.setInt(gl, program, "clippingPlaneEnabled", 0);
            return;
        }

        WebGLSolidTextureRenderer.setInt(gl, program, "clippingPlaneEnabled", 1);
        WebGLSolidTextureRenderer.setVector4(
            gl,
            program,
            "clippingPlaneGlobal",
            new Vector4Dd(clippingPlane.getA(), clippingPlane.getB(), clippingPlane.getC(), clippingPlane.getD()),
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

    private static setVector3(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: Vector3Dd): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.x(), value.y(), value.z());
        }
    }

    private static setVector4(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: Vector4Dd): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform4f(loc, value.x(), value.y(), value.z(), value.w());
        }
    }

    private static setColor(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: ColorRgb): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.r(), value.g(), value.b());
        }
    }

    private static setInt(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: number): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform1i(loc, value);
        }
    }

    private static setFloat(gl: WebGL2RenderingContext, program: WebGLProgram, name: string, value: number): void {
        const loc = gl.getUniformLocation(program, name);
        if (loc !== null) {
            gl.uniform1f(loc, value);
        }
    }
}
