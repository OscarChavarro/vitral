import {
    Arrow,
    Camera,
    ColorRgb,
    Light,
    Matrix4x4d,
    RendererConfiguration,
    SimpleMaterial,
    Vector3Dd,
} from "@vitral/base";
import { WebGLRendererConfigurationShaderSelector } from "./WebGLRendererConfigurationShaderSelector.js";

interface ArrowMesh {
    positions: Float32Array;
    normals: Float32Array;
    uvs: Float32Array;
    vertexCount: number;
}

interface ArrowResources {
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    normalBuffer: WebGLBuffer;
    uvBuffer: WebGLBuffer;
    vertexCount: number;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ArrowRenderer`.

The procedurally tessellated arrow — shaft side, shaft bottom cap, annular head
cap and cone side, sixteen slices each, with the cone's slant normals — is the
Java mesh, vertex for vertex, and so is the uniform block and the draw state
around it.

The runtime boundaries are this package's recurring ones: the mesh and its
buffers are held per `WebGL2RenderingContext` in a `WeakMap` instead of in
static fields, since a page can own several contexts; a GLSL source arrives
over `fetch`, so `draw` is asynchronous; and `glGetUniformLocation`'s negative
sentinel becomes `null`. One is specific here: WebGL has no `glPolygonMode`,
so Java's `GL_FILL` call before the draw has no counterpart — WebGL rasterizes
filled polygons and nothing else.

Java's `ensureMesh` builds the arrow once and then answers every later call
without looking at the `Arrow` it was given, so an arrow of different
proportions reuses the first one's mesh. That is kept: the gizmo that drives
this renderer builds all of its arrows from one `Arrow` instance.
*/
export class WebGLArrowRenderer {
    private static readonly SLICES = 16;
    private static readonly SURFACE_POLYGON_OFFSET_FACTOR = 1.0;
    private static readonly SURFACE_POLYGON_OFFSET_UNITS = 1.0;

    private static readonly resources = new WeakMap<WebGL2RenderingContext, ArrowResources>();

    private constructor() {}

    /**
    Draws a single arrow instance.

    @param gl          WebGL2 context
    @param arrow       source geometry (defines radii and lengths for mesh generation)
    @param modelMatrix object-to-world matrix (T x R x S)
    @param projection  pre-computed view-projection matrix
    @param camera      active camera
    @param lights      scene lights
    @param material    surface material
    @param quality     renderer configuration flags
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        arrow: Arrow | null,
        modelMatrix: Matrix4x4d | null,
        projection: Matrix4x4d | null,
        camera: Camera | null,
        lights: readonly (Light | null)[] | null,
        material: SimpleMaterial | null,
        quality: RendererConfiguration,
    ): Promise<void> {
        if (arrow === null || modelMatrix === null || projection === null) {
            return;
        }
        if (camera === null || lights === null || lights.length === 0 || material === null) {
            return;
        }

        const resources: ArrowResources = WebGLArrowRenderer.ensureMesh(gl, arrow);

        const mvp: Matrix4x4d = projection.multiply(modelMatrix);
        const modelIt: Matrix4x4d = modelMatrix.invert().transpose();

        const program: WebGLProgram = await WebGLRendererConfigurationShaderSelector.selectSurfaceShaderProgram(
            gl,
            quality,
            false,
            false,
        );

        WebGLArrowRenderer.configureProgram(gl, program, mvp, modelMatrix, modelIt, camera, lights, material, quality);

        gl.enable(gl.DEPTH_TEST);
        gl.depthMask(true);
        gl.depthFunc(gl.LESS);
        gl.enable(gl.CULL_FACE);
        gl.cullFace(gl.BACK);
        gl.enable(gl.POLYGON_OFFSET_FILL);
        gl.polygonOffset(
            WebGLArrowRenderer.SURFACE_POLYGON_OFFSET_FACTOR,
            WebGLArrowRenderer.SURFACE_POLYGON_OFFSET_UNITS,
        );

        gl.bindVertexArray(resources.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, resources.vertexCount);
        gl.bindVertexArray(null);

        gl.disable(gl.POLYGON_OFFSET_FILL);
        gl.disable(gl.CULL_FACE);

        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        const resources = WebGLArrowRenderer.resources.get(gl);
        if (resources === undefined) {
            return;
        }

        gl.deleteBuffer(resources.positionBuffer);
        gl.deleteBuffer(resources.normalBuffer);
        gl.deleteBuffer(resources.uvBuffer);
        gl.deleteVertexArray(resources.vertexArray);

        WebGLArrowRenderer.resources.delete(gl);
    }

    public static ensureMesh(gl: WebGL2RenderingContext, arrow: Arrow): ArrowResources {
        const existing = WebGLArrowRenderer.resources.get(gl);
        if (existing !== undefined) {
            return existing;
        }

        const mesh: ArrowMesh = WebGLArrowRenderer.buildArrowMesh(
            arrow.getBaseRadius(),
            arrow.getHeadRadius(),
            arrow.getBaseLength(),
            arrow.getHeadLength(),
            WebGLArrowRenderer.SLICES,
        );

        return WebGLArrowRenderer.uploadMesh(gl, mesh);
    }

    private static configureProgram(
        gl: WebGL2RenderingContext,
        program: WebGLProgram,
        modelViewProjection: Matrix4x4d,
        modelViewLocal: Matrix4x4d,
        modelViewITLocal: Matrix4x4d,
        camera: Camera,
        lights: readonly (Light | null)[],
        material: SimpleMaterial,
        quality: RendererConfiguration,
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

        WebGLArrowRenderer.setMatrix(gl, program, "modelViewLocal", modelViewLocal);
        WebGLArrowRenderer.setMatrix(gl, program, "modelViewITLocal", modelViewITLocal);
        WebGLArrowRenderer.setVector3(gl, program, "cameraPositionGlobal", camera.getPosition());

        let lightCount = 0;
        for (const light of lights) {
            if (light === null) {
                continue;
            }
            WebGLArrowRenderer.setVector3(gl, program, "lightPositionsGlobal[" + lightCount + "]", light.getPosition());
            WebGLArrowRenderer.setColor(gl, program, "lightColorsGlobal[" + lightCount + "]", light.getEmission());
            lightCount++;
        }
        WebGLArrowRenderer.setInt(gl, program, "numberOfLights", lightCount);
        WebGLArrowRenderer.setColor(gl, program, "ambientColor", material.getAmbient());
        WebGLArrowRenderer.setColor(gl, program, "diffuseColor", material.getDiffuse());
        WebGLArrowRenderer.setColor(gl, program, "specularColor", material.getSpecular());
        WebGLArrowRenderer.setFloat(gl, program, "phongExponent", material.getPhongExponent());
        WebGLArrowRenderer.setInt(gl, program, "withTexture", 0);
        WebGLArrowRenderer.setInt(gl, program, "withBumpMap", 0);
    }

    private static buildArrowMesh(
        baseRadius: number,
        headRadius: number,
        baseLength: number,
        headLength: number,
        slices: number,
    ): ArrowMesh {
        // Faces: shaft side + shaft bottom cap + cone side (each as 2 tris per slice)
        // shaft side: slices * 2 triangles
        // shaft bottom: slices * 1 triangle (fan)
        // shaft top cap (ring from baseRadius to headRadius): slices * 2 triangles
        // cone side: slices * 1 triangle (fan to apex)
        const triangleCount: number = slices * 6;
        const verticesNeeded: number = triangleCount * 3;

        const positions = new Float32Array(verticesNeeded * 3);
        const normals = new Float32Array(verticesNeeded * 3);
        const uvs = new Float32Array(verticesNeeded * 2);

        let pi = 0;
        let ni = 0;
        let ti = 0;

        // ---- Shaft side ----
        for (let i = 0; i < slices; i++) {
            const a0: number = (2.0 * Math.PI * i) / slices;
            const a1: number = (2.0 * Math.PI * (i + 1)) / slices;

            const x0: number = Math.cos(a0) * baseRadius;
            const y0: number = Math.sin(a0) * baseRadius;
            const x1: number = Math.cos(a1) * baseRadius;
            const y1: number = Math.sin(a1) * baseRadius;

            const nx0: number = Math.cos(a0);
            const ny0: number = Math.sin(a0);
            const nx1: number = Math.cos(a1);
            const ny1: number = Math.sin(a1);

            // Triangle 1: bottom-left, bottom-right, top-right
            pi = WebGLArrowRenderer.addPos(positions, pi, x0, y0, 0);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx0, ny0, 0);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, x1, y1, 0);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx1, ny1, 0);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, x1, y1, baseLength);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx1, ny1, 0);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 1);

            // Triangle 2: bottom-left, top-right, top-left
            pi = WebGLArrowRenderer.addPos(positions, pi, x0, y0, 0);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx0, ny0, 0);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, x1, y1, baseLength);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx1, ny1, 0);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 1);

            pi = WebGLArrowRenderer.addPos(positions, pi, x0, y0, baseLength);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx0, ny0, 0);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 1);
        }

        // ---- Shaft bottom cap (z=0, facing -Z) ----
        for (let i = 0; i < slices; i++) {
            const a0: number = (2.0 * Math.PI * i) / slices;
            const a1: number = (2.0 * Math.PI * (i + 1)) / slices;

            const x0: number = Math.cos(a0) * baseRadius;
            const y0: number = Math.sin(a0) * baseRadius;
            const x1: number = Math.cos(a1) * baseRadius;
            const y1: number = Math.sin(a1) * baseRadius;

            pi = WebGLArrowRenderer.addPos(positions, pi, 0, 0, 0);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0.5, 0.5);

            pi = WebGLArrowRenderer.addPos(positions, pi, x1, y1, 0);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, x0, y0, 0);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 0);
        }

        // ---- Head annular cap (flat ring from baseRadius to headRadius at z=baseLength, facing -Z) ----
        const coneBase: number = baseLength;
        for (let i = 0; i < slices; i++) {
            const a0: number = (2.0 * Math.PI * i) / slices;
            const a1: number = (2.0 * Math.PI * (i + 1)) / slices;

            const ox0: number = Math.cos(a0) * baseRadius;
            const oy0: number = Math.sin(a0) * baseRadius;
            const ox1: number = Math.cos(a1) * baseRadius;
            const oy1: number = Math.sin(a1) * baseRadius;

            const ix0: number = Math.cos(a0) * headRadius;
            const iy0: number = Math.sin(a0) * headRadius;
            const ix1: number = Math.cos(a1) * headRadius;
            const iy1: number = Math.sin(a1) * headRadius;

            // Triangle 1
            pi = WebGLArrowRenderer.addPos(positions, pi, ox0, oy0, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, ox1, oy1, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, ix1, iy1, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 1);

            // Triangle 2
            pi = WebGLArrowRenderer.addPos(positions, pi, ox0, oy0, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 0);

            pi = WebGLArrowRenderer.addPos(positions, pi, ix1, iy1, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 1);

            pi = WebGLArrowRenderer.addPos(positions, pi, ix0, iy0, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, 0, 0, -1);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 1);
        }

        // ---- Cone side ----
        const apex: number = baseLength + headLength;
        // Cone slant normal: outward normal has components
        // (cos(a)*sinAlpha, sin(a)*sinAlpha, cosAlpha) where
        // sinAlpha = headRadius/slantLength, cosAlpha = headLength/slantLength.
        const slantLength: number = Math.sqrt(headRadius * headRadius + headLength * headLength);
        const cosAlpha: number = slantLength > 1e-12 ? headLength / slantLength : 1.0;
        const sinAlpha: number = slantLength > 1e-12 ? headRadius / slantLength : 0.0;

        for (let i = 0; i < slices; i++) {
            const a0: number = (2.0 * Math.PI * i) / slices;
            const a1: number = (2.0 * Math.PI * (i + 1)) / slices;
            const aMid: number = (a0 + a1) * 0.5;

            const x0: number = Math.cos(a0) * headRadius;
            const y0: number = Math.sin(a0) * headRadius;
            const x1: number = Math.cos(a1) * headRadius;
            const y1: number = Math.sin(a1) * headRadius;

            const nx0: number = Math.cos(a0) * sinAlpha;
            const ny0: number = Math.sin(a0) * sinAlpha;
            const nx1: number = Math.cos(a1) * sinAlpha;
            const ny1: number = Math.sin(a1) * sinAlpha;
            const nxMid: number = Math.cos(aMid) * sinAlpha;
            const nyMid: number = Math.sin(aMid) * sinAlpha;

            pi = WebGLArrowRenderer.addPos(positions, pi, x0, y0, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx0, ny0, cosAlpha);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0, 1);

            pi = WebGLArrowRenderer.addPos(positions, pi, x1, y1, coneBase);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nx1, ny1, cosAlpha);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 1, 1);

            pi = WebGLArrowRenderer.addPos(positions, pi, 0, 0, apex);
            ni = WebGLArrowRenderer.addNorm(normals, ni, nxMid, nyMid, cosAlpha);
            ti = WebGLArrowRenderer.addUv(uvs, ti, 0.5, 0);
        }

        return { positions, normals, uvs, vertexCount: verticesNeeded };
    }

    private static addPos(buf: Float32Array, idx: number, x: number, y: number, z: number): number {
        buf[idx++] = x;
        buf[idx++] = y;
        buf[idx++] = z;
        return idx;
    }

    private static addNorm(buf: Float32Array, idx: number, x: number, y: number, z: number): number {
        const len: number = Math.sqrt(x * x + y * y + z * z);
        if (len > 1e-12) {
            buf[idx++] = x / len;
            buf[idx++] = y / len;
            buf[idx++] = z / len;
        } else {
            buf[idx++] = 0;
            buf[idx++] = 0;
            buf[idx++] = 1;
        }
        return idx;
    }

    private static addUv(buf: Float32Array, idx: number, u: number, v: number): number {
        buf[idx++] = u;
        buf[idx++] = v;
        return idx;
    }

    private static uploadMesh(gl: WebGL2RenderingContext, mesh: ArrowMesh): ArrowResources {
        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const normalBuffer = gl.createBuffer();
        const uvBuffer = gl.createBuffer();
        if (vertexArray === null || positionBuffer === null || normalBuffer === null || uvBuffer === null) {
            throw new Error("Failed to create arrow renderer buffers");
        }

        gl.bindVertexArray(vertexArray);

        WebGLArrowRenderer.uploadBuffer(gl, positionBuffer, 0, 3, mesh.positions);
        WebGLArrowRenderer.uploadBuffer(gl, normalBuffer, 1, 3, mesh.normals);
        WebGLArrowRenderer.uploadBuffer(gl, uvBuffer, 2, 2, mesh.uvs);

        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);

        const resources: ArrowResources = {
            vertexArray,
            positionBuffer,
            normalBuffer,
            uvBuffer,
            vertexCount: mesh.vertexCount,
        };
        WebGLArrowRenderer.resources.set(gl, resources);
        return resources;
    }

    private static uploadBuffer(
        gl: WebGL2RenderingContext,
        buffer: WebGLBuffer,
        attrib: number,
        size: number,
        data: Float32Array,
    ): void {
        gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
        gl.bufferData(gl.ARRAY_BUFFER, data, gl.STATIC_DRAW);
        gl.enableVertexAttribArray(attrib);
        gl.vertexAttribPointer(attrib, size, gl.FLOAT, false, 0, 0);
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
