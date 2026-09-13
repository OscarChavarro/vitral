import {
    Camera,
    ColorRgb,
    Light,
    Matrix4x4d,
    Md2Mesh,
    RendererConfiguration,
    SimpleMaterial,
    Vector3Dd,
    type Image,
} from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";
import { WebGLRendererConfigurationShaderSelector } from "./WebGLRendererConfigurationShaderSelector.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4Md2MeshRenderer`.

An MD2 model is a stack of vertex frames plus a list of OpenGL commands
(triangle strips and fans) that index into them. Every draw call interpolates
between the current frame and the next by the fraction of a frame period the
mesh's elapsed time has reached, expands the strips and fans into an
independent triangle list, and streams that list into three buffers. All of
that is Java's, `buildFrame` and `appendVertex` included, down to the
`1.0 - v` flip of the texture coordinate and the even/odd winding correction
of the strip expansion.

Four boundaries are crossed, three of them the ones every ported JOGL4
renderer crosses:

  - Java holds its VAO, its three VBOs and its texture in static fields, one
    set for the process. A WebGL object belongs to the context that made it,
    so the same state lives in a `WeakMap` keyed by context, and `initGL`,
    `draw` and `dispose` each look it up.
  - `glPolygonMode(GL_FRONT_AND_BACK, GL_LINE)` and `POLYGON_OFFSET_LINE` have
    no WebGL counterpart, so the wireframe pass draws the three edges of every
    triangle as `LINES` over a second buffer set, with the depth state Java
    also sets for that pass standing in for the polygon-offset bias. This is
    the same substitution `MeshExample`'s renderer makes.
  - `glPointSize` has no WebGL counterpart either; the size travels to the
    vertex shader as the `pointSizeLocal` uniform `WebGLShaderPreprocessor`
    installs.
  - Java compiles its shaders at `init`; a browser reaches a GLSL source over
    `fetch`, so `draw` is asynchronous, and {@link prepare} exists so that a
    program can be compiled outside a frame — the drawing buffer is presented
    and cleared at task boundaries, so a frame must not await a network read.

`glGetUniformLocation` answers `null` rather than a negative int in WebGL, so
every `loc >= 0` guard of the Java original is a `!== null` guard here.
*/
export class WebGLMd2MeshRenderer {
    private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 1.0;
    private static readonly SURFACE_POLYGON_OFFSET_UNITS = 1.0;

    private static readonly state = new WeakMap<WebGL2RenderingContext, _Md2RendererState>();

    public static initGL(gl: WebGL2RenderingContext, _md2Mesh: Md2Mesh | null): void {
        if (WebGLMd2MeshRenderer.state.has(gl)) {
            return;
        }
        WebGLMd2MeshRenderer.state.set(gl, {
            surfaceBuffers: WebGLMd2MeshRenderer.createBuffers(gl),
            wireBuffers: WebGLMd2MeshRenderer.createBuffers(gl),
            vertexCount: 0,
            wireVertexCount: 0,
            texture: null,
            textureOwner: null,
        });
    }

    /**
    Compiles the shader programs the three passes can reach, without drawing.
    No Java counterpart: Java's `initGL` allocates buffers only, because its
    shader sources are read from disk inside the draw call.
    */
    public static async prepare(gl: WebGL2RenderingContext): Promise<void> {
        WebGLMd2MeshRenderer.initGL(gl, null);
        const phongQuality = new RendererConfiguration();
        await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(gl, phongQuality, true, false);
        await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(gl, phongQuality, false, false);
        await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
            gl,
            WebGLMd2MeshRenderer.noLightQuality(),
            false,
            false,
        );
    }

    public static async draw(
        gl: WebGL2RenderingContext,
        md2Mesh: Md2Mesh | null,
        camera: Camera | null,
        light: Light | null,
        quality: RendererConfiguration | null,
        xTranslation: number,
    ): Promise<void> {
        if (md2Mesh === null || camera === null || light === null || quality === null) {
            return;
        }
        WebGLMd2MeshRenderer.initGL(gl, md2Mesh);
        const state: _Md2RendererState = WebGLMd2MeshRenderer.state.get(gl)!;

        const frame: _InterpolatedFrame | null = WebGLMd2MeshRenderer.buildFrame(md2Mesh);
        if (frame === null || frame.positions.length === 0) {
            return;
        }
        WebGLMd2MeshRenderer.uploadFrame(gl, state, frame);

        const textureId: WebGLTexture | null = WebGLMd2MeshRenderer.updateTexture(gl, state, md2Mesh);
        const hasTexture: boolean = textureId !== null;
        const useTexture: boolean = quality.isTextureSet() && hasTexture;

        let model = new Matrix4x4d();
        model = model.translation(xTranslation, 0.0, 0.0);
        const projection: Matrix4x4d = camera.calculateProjectionMatrix();
        const mvp: Matrix4x4d = projection.multiply(model);
        const modelIT: Matrix4x4d = model.invert().transpose();

        let material = new SimpleMaterial();
        material = material.withAmbient(new ColorRgb(0.2, 0.2, 0.2));
        material = material.withDiffuse(new ColorRgb(0.8, 0.8, 0.8));
        material = material.withSpecular(new ColorRgb(1.0, 1.0, 1.0));
        material = material.withPhongExponent(32.0);

        if (quality.isSurfacesSet()) {
            const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                quality,
                useTexture,
                false,
            );
            WebGLMd2MeshRenderer.configureProgram(
                gl,
                program,
                mvp,
                model,
                modelIT,
                camera,
                light,
                material,
                quality,
                useTexture,
                textureId,
            );
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(true);
            gl.depthFunc(gl.LESS);
            gl.enable(gl.POLYGON_OFFSET_FILL);
            gl.polygonOffset(
                WebGLMd2MeshRenderer.SURFACE_POLYGON_OFFSET_FACTOR,
                WebGLMd2MeshRenderer.SURFACE_POLYGON_OFFSET_UNITS,
            );
            gl.disable(gl.CULL_FACE);
            // Java sets glPolygonMode(GL_FRONT_AND_BACK, GL_FILL); WebGL
            // rasterizes filled polygons only and has no polygon-mode entry
            // point.
            WebGLMd2MeshRenderer.render(gl, state);
            gl.disable(gl.POLYGON_OFFSET_FILL);
            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isWiresSet()) {
            const wireQuality: RendererConfiguration = WebGLMd2MeshRenderer.noLightQuality();
            const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                wireQuality,
                false,
                false,
            );
            let wireMaterial = new SimpleMaterial(material);
            wireMaterial = wireMaterial.withAmbient(new ColorRgb(0, 0, 0));
            wireMaterial = wireMaterial.withDiffuse(new ColorRgb(1, 1, 1));
            wireMaterial = wireMaterial.withSpecular(new ColorRgb(0, 0, 0));
            WebGLMd2MeshRenderer.configureProgram(
                gl,
                program,
                mvp,
                model,
                modelIT,
                camera,
                light,
                wireMaterial,
                wireQuality,
                false,
                null,
            );
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(false);
            gl.depthFunc(gl.LEQUAL);
            gl.disable(gl.CULL_FACE);
            // Java biases the outlined polygon with POLYGON_OFFSET_LINE and one
            // pixel of glLineWidth; WebGL has neither, and the edge list below
            // is drawn as LINES with the depth state Java also sets here.
            gl.bindVertexArray(state.wireBuffers.vertexArray);
            gl.drawArrays(gl.LINES, 0, state.wireVertexCount);
            gl.bindVertexArray(null);
            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        if (quality.isPointsSet()) {
            const pointQuality: RendererConfiguration = WebGLMd2MeshRenderer.noLightQuality();
            const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
                gl,
                pointQuality,
                false,
                false,
            );
            let pointMaterial = new SimpleMaterial(material);
            pointMaterial = pointMaterial.withAmbient(new ColorRgb(0, 0, 0));
            pointMaterial = pointMaterial.withDiffuse(new ColorRgb(1, 0, 0));
            pointMaterial = pointMaterial.withSpecular(new ColorRgb(0, 0, 0));
            WebGLMd2MeshRenderer.configureProgram(
                gl,
                program,
                mvp,
                model,
                modelIT,
                camera,
                light,
                pointMaterial,
                pointQuality,
                false,
                null,
            );
            gl.enable(gl.DEPTH_TEST);
            gl.depthMask(false);
            gl.depthFunc(gl.LEQUAL);
            gl.disable(gl.CULL_FACE);
            // Java's glPointSize(4.0f), as the preprocessor-installed uniform.
            WebGLMd2MeshRenderer.setFloat(gl, program, "pointSizeLocal", 4.0);
            gl.bindVertexArray(state.surfaceBuffers.vertexArray);
            gl.drawArrays(gl.POINTS, 0, state.vertexCount);
            gl.bindVertexArray(null);
            WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
        }

        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.bindTexture(gl.TEXTURE_2D, null);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        const state: _Md2RendererState | undefined = WebGLMd2MeshRenderer.state.get(gl);
        if (state === undefined) {
            return;
        }
        if (state.textureOwner !== null) {
            WebGLImageRenderer.unload(gl, state.textureOwner);
        }
        WebGLMd2MeshRenderer.deleteBuffers(gl, state.surfaceBuffers);
        WebGLMd2MeshRenderer.deleteBuffers(gl, state.wireBuffers);
        WebGLMd2MeshRenderer.state.delete(gl);
    }

    /**
    Java writes the three fields of the wire and point `RendererConfiguration`
    inline in each branch; both branches write the same two, so they are made
    once here.
    */
    private static noLightQuality(): RendererConfiguration {
        const quality = new RendererConfiguration();
        quality.setTexture(false);
        quality.setShadingType(RendererConfiguration.SHADING_TYPE_NOLIGHT);
        return quality;
    }

    private static configureProgram(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        mvp: Matrix4x4d,
        model: Matrix4x4d,
        modelIT: Matrix4x4d,
        camera: Camera,
        light: Light,
        material: SimpleMaterial,
        quality: RendererConfiguration,
        withTexture: boolean,
        textureId: WebGLTexture | null,
    ): void {
        const kd: ColorRgb = material.getDiffuse();
        WebGLRendererConfigurationShaderSelector.activateShader(gl, program, mvp, quality, kd.r(), kd.g(), kd.b());

        WebGLMd2MeshRenderer.setMatrix(gl, program, "modelViewLocal", model);
        WebGLMd2MeshRenderer.setMatrix(gl, program, "modelViewITLocal", modelIT);
        WebGLMd2MeshRenderer.setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());
        WebGLMd2MeshRenderer.setVector3(gl, program, "lightPositionsGlobal[0]", light.getPosition());
        WebGLMd2MeshRenderer.setColor(gl, program, "lightColorsGlobal[0]", light.getEmission());
        WebGLMd2MeshRenderer.setInt(gl, program, "numberOfLights", 1);
        WebGLMd2MeshRenderer.setColor(gl, program, "ambientColor", material.getAmbient());
        WebGLMd2MeshRenderer.setColor(gl, program, "diffuseColor", material.getDiffuse());
        WebGLMd2MeshRenderer.setColor(gl, program, "specularColor", material.getSpecular());
        WebGLMd2MeshRenderer.setFloat(gl, program, "phongExponent", material.getPhongExponent());
        WebGLMd2MeshRenderer.setInt(gl, program, "withTexture", withTexture ? 1 : 0);
        WebGLMd2MeshRenderer.setInt(gl, program, "withBumpMap", 0);

        if (withTexture) {
            gl.activeTexture(gl.TEXTURE0);
            gl.bindTexture(gl.TEXTURE_2D, textureId);
        }
    }

    private static setMatrix(
        gl: WebGL2RenderingContext,
        programId: WebGLProgram,
        name: string,
        matrix: Matrix4x4d,
    ): void {
        const loc: WebGLUniformLocation | null = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniformMatrix4fv(loc, false, matrix.exportToFloatArrayColumnOrder());
        }
    }

    private static setVector3(
        gl: WebGL2RenderingContext,
        programId: WebGLProgram,
        name: string,
        value: Vector3Dd,
    ): void {
        const loc: WebGLUniformLocation | null = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.x(), value.y(), value.z());
        }
    }

    private static setColor(gl: WebGL2RenderingContext, programId: WebGLProgram, name: string, value: ColorRgb): void {
        const loc: WebGLUniformLocation | null = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform3f(loc, value.r(), value.g(), value.b());
        }
    }

    private static setInt(gl: WebGL2RenderingContext, programId: WebGLProgram, name: string, value: number): void {
        const loc: WebGLUniformLocation | null = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform1i(loc, value);
        }
    }

    private static setFloat(gl: WebGL2RenderingContext, programId: WebGLProgram, name: string, value: number): void {
        const loc: WebGLUniformLocation | null = gl.getUniformLocation(programId, name);
        if (loc !== null) {
            gl.uniform1f(loc, value);
        }
    }

    private static updateTexture(
        gl: WebGL2RenderingContext,
        state: _Md2RendererState,
        md2Mesh: Md2Mesh,
    ): WebGLTexture | null {
        if (md2Mesh.skins === null || md2Mesh.skins.length === 0 || md2Mesh.skins[0] === undefined) {
            state.textureOwner = null;
            state.texture = null;
            return null;
        }
        const current: Image = md2Mesh.skins[0];
        if (current !== state.textureOwner) {
            state.textureOwner = current;
            state.texture = WebGLImageRenderer.activate(gl, current);
        }
        return state.texture;
    }

    private static uploadFrame(gl: WebGL2RenderingContext, state: _Md2RendererState, frame: _InterpolatedFrame): void {
        state.vertexCount = Math.trunc(frame.positions.length / 3);
        WebGLMd2MeshRenderer.uploadInto(gl, state.surfaceBuffers, frame);

        const wireFrame: _InterpolatedFrame = WebGLMd2MeshRenderer.buildWireFrame(frame);
        state.wireVertexCount = Math.trunc(wireFrame.positions.length / 3);
        WebGLMd2MeshRenderer.uploadInto(gl, state.wireBuffers, wireFrame);
    }

    private static uploadInto(gl: WebGL2RenderingContext, buffers: _Md2Buffers, frame: _InterpolatedFrame): void {
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

    private static render(gl: WebGL2RenderingContext, state: _Md2RendererState): void {
        gl.bindVertexArray(state.surfaceBuffers.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, state.vertexCount);
        gl.bindVertexArray(null);
    }

    private static buildFrame(md2Mesh: Md2Mesh): _InterpolatedFrame | null {
        if (md2Mesh.frameVertices.length === 0 || md2Mesh.frameNormalIndices.length === 0) {
            return null;
        }

        const normalsTable: readonly (readonly number[])[] = Md2Mesh.anorms;
        const animStartEnd: Int16Array = new Int16Array(2);
        md2Mesh.returnStartEndAnim(md2Mesh.getCurrentAnimationInd(), animStartEnd);
        const frameTimeSeg: number = md2Mesh.getFrameTimeSeg();
        const elapsedTimeSeg: number = md2Mesh.getElapsedTimeSeg();
        let t: number = elapsedTimeSeg / frameTimeSeg;
        let length: number = animStartEnd[1]! - animStartEnd[0]! + 1;
        if (length <= 0) {
            length = 1;
        }
        const frame: number = (Math.trunc(t) % length) + animStartEnd[0]!;
        t = t - Math.trunc(t);
        const nextFrame: number = frame === animStartEnd[1]! ? animStartEnd[0]! : frame + 1;

        const verts: Float32Array = md2Mesh.frameVertices[frame]!;
        const nextVerts: Float32Array = md2Mesh.frameVertices[nextFrame]!;
        const normalIdx: Int16Array = md2Mesh.frameNormalIndices[frame]!;
        const nextNormalIdx: Int16Array = md2Mesh.frameNormalIndices[nextFrame]!;

        const positionsList: number[] = [];
        const normalsList: number[] = [];
        const uvsList: number[] = [];

        if (md2Mesh.glCmdVertIndexStrip.length !== 0 || md2Mesh.glCmdVertIndexFan.length !== 0) {
            for (let i = 0; i < md2Mesh.glCmdVertIndexStrip.length; i++) {
                const strip: Int32Array = md2Mesh.glCmdVertIndexStrip[i]!;
                const stripUv: Float32Array = md2Mesh.glCmdTexCoordsStrip[i]!;
                for (let j = 2; j < strip.length; j++) {
                    let ia: number;
                    let ib: number;
                    const ic: number = j;
                    if ((j & 1) === 0) {
                        ia = j - 2;
                        ib = j - 1;
                    } else {
                        ia = j - 1;
                        ib = j - 2;
                    }
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        strip[ia]!,
                        stripUv[ia * 2]!,
                        stripUv[ia * 2 + 1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        strip[ib]!,
                        stripUv[ib * 2]!,
                        stripUv[ib * 2 + 1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        strip[ic]!,
                        stripUv[ic * 2]!,
                        stripUv[ic * 2 + 1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                }
            }
            for (let i = 0; i < md2Mesh.glCmdVertIndexFan.length; i++) {
                const fan: Int32Array = md2Mesh.glCmdVertIndexFan[i]!;
                const fanUv: Float32Array = md2Mesh.glCmdTexCoordsFan[i]!;
                for (let j = 2; j < fan.length; j++) {
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        fan[0]!,
                        fanUv[0]!,
                        fanUv[1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        fan[j - 1]!,
                        fanUv[(j - 1) * 2]!,
                        fanUv[(j - 1) * 2 + 1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        fan[j]!,
                        fanUv[j * 2]!,
                        fanUv[j * 2 + 1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                }
            }
        } else {
            const triCount: number = md2Mesh.numTriangles;
            for (let i = 0; i < triCount; i++) {
                for (let j = 0; j < 3; j++) {
                    const vIndex: number = md2Mesh.triangles![i * 2]![j]!;
                    const tIndex: number = md2Mesh.triangles![i * 2 + 1]![j]!;
                    const ti2: number = tIndex * 2;
                    WebGLMd2MeshRenderer.appendVertex(
                        positionsList,
                        normalsList,
                        uvsList,
                        vIndex,
                        md2Mesh.texCoords![ti2]!,
                        md2Mesh.texCoords![ti2 + 1]!,
                        verts,
                        nextVerts,
                        normalIdx,
                        nextNormalIdx,
                        normalsTable,
                        t,
                    );
                }
            }
        }

        return {
            positions: new Float32Array(positionsList),
            normals: new Float32Array(normalsList),
            uvs: new Float32Array(uvsList),
        };
    }

    private static appendVertex(
        positionsList: number[],
        normalsList: number[],
        uvsList: number[],
        vIndex: number,
        u: number,
        v: number,
        verts: Float32Array,
        nextVerts: Float32Array,
        normalIdx: Int16Array,
        nextNormalIdx: Int16Array,
        normalsTable: readonly (readonly number[])[],
        t: number,
    ): void {
        const vi3: number = vIndex * 3;
        positionsList.push(verts[vi3]! + t * (nextVerts[vi3]! - verts[vi3]!));
        positionsList.push(verts[vi3 + 1]! + t * (nextVerts[vi3 + 1]! - verts[vi3 + 1]!));
        positionsList.push(verts[vi3 + 2]! + t * (nextVerts[vi3 + 2]! - verts[vi3 + 2]!));

        const normal: readonly number[] = normalsTable[normalIdx[vIndex]!]!;
        const normalN: readonly number[] = normalsTable[nextNormalIdx[vIndex]!]!;
        normalsList.push(normal[0]! + t * (normalN[0]! - normal[0]!));
        normalsList.push(normal[1]! + t * (normalN[1]! - normal[1]!));
        normalsList.push(normal[2]! + t * (normalN[2]! - normal[2]!));

        uvsList.push(u);
        uvsList.push(1.0 - v);
    }

    /**
    The edge list that stands in for `glPolygonMode(..., GL_LINE)`: every
    triangle of the interpolated frame becomes its three edges, so the
    wireframe pass can draw `LINES` over exactly the geometry the surface pass
    filled. No Java counterpart; see the class documentation.
    */
    private static buildWireFrame(frame: _InterpolatedFrame): _InterpolatedFrame {
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

    private static createBuffers(gl: WebGL2RenderingContext): _Md2Buffers {
        const vertexArray: WebGLVertexArrayObject | null = gl.createVertexArray();
        const positionBuffer: WebGLBuffer | null = gl.createBuffer();
        const normalBuffer: WebGLBuffer | null = gl.createBuffer();
        const uvBuffer: WebGLBuffer | null = gl.createBuffer();
        if (vertexArray === null || positionBuffer === null || normalBuffer === null || uvBuffer === null) {
            throw new Error("WebGLMd2MeshRenderer could not allocate its vertex buffers.");
        }
        return { vertexArray, positionBuffer, normalBuffer, uvBuffer };
    }

    private static deleteBuffers(gl: WebGL2RenderingContext, buffers: _Md2Buffers): void {
        gl.deleteBuffer(buffers.positionBuffer);
        gl.deleteBuffer(buffers.normalBuffer);
        gl.deleteBuffer(buffers.uvBuffer);
        gl.deleteVertexArray(buffers.vertexArray);
    }
}

interface _Md2Buffers {
    readonly vertexArray: WebGLVertexArrayObject;
    readonly positionBuffer: WebGLBuffer;
    readonly normalBuffer: WebGLBuffer;
    readonly uvBuffer: WebGLBuffer;
}

/**
What Java keeps in the static fields of `Jogl4Md2MeshRenderer`, one set per
WebGL context instead of one set per process.
*/
interface _Md2RendererState {
    readonly surfaceBuffers: _Md2Buffers;
    readonly wireBuffers: _Md2Buffers;
    vertexCount: number;
    wireVertexCount: number;
    texture: WebGLTexture | null;
    textureOwner: Image | null;
}

/**
Java's private `InterpolatedFrame`.
*/
interface _InterpolatedFrame {
    readonly positions: Float32Array;
    readonly normals: Float32Array;
    readonly uvs: Float32Array;
}
