import { Matrix4x4d, Vector4Dd } from "@vitral/base";
import { WebGLShaderProgramUtil } from "./WebGLShaderProgramUtil.js";

interface LineRendererResources {
    program: WebGLProgram;
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    colorBuffer: WebGLBuffer;
    mvpLocation: WebGLUniformLocation;
    depthBiasLocation: WebGLUniformLocation;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4LineRenderer`.

The two drawing paths are the Java ones: a line one pixel wide or thinner goes
straight to `GL_LINES`, and a thicker one is expanded on the host into screen
space quads, because a wide `glLineWidth` is not portable. The clip-volume
clipping, the pixel-space perpendicular offset, and the six-plane test are the
Java code unchanged.

Three runtime boundaries are crossed:

  - Java keeps the program and the two buffers in static fields, because a JOGL
    application owns a single `GL4` context for its whole life. A browser page
    can hold several independent `WebGL2RenderingContext` objects, so the
    resources are held per context in a `WeakMap`, as the rest of
    `@vitral/webgl` already does.
  - Java compiles the program inside `ensureInitialized`, which it can do
    because reading a shader file is synchronous. Here a shader source arrives
    over `fetch`, so both `drawLines` overloads are asynchronous.
  - `glGetUniformLocation` answers a negative int for an absent uniform, while
    WebGL answers `null`; each `loc < 0` check of the original is a `=== null`
    check here, raising the same error.

WebGL2 keeps `gl.lineWidth`, but the specification permits an implementation to
support only the value 1.0, and every browser engine in practice does exactly
that. The call is issued where Java issues it, and the thick path above is what
actually delivers a wide line.
*/
export class WebGLLineRenderer {
    private static readonly VERTEX_SHADER_FILE = "lineVertexShader.glsl";
    private static readonly FRAGMENT_SHADER_FILE = "linePixelShader.glsl";
    private static readonly CLIP_PLANES: readonly (readonly number[])[] = [
        [1.0, 0.0, 0.0, 1.0],
        [-1.0, 0.0, 0.0, 1.0],
        [0.0, 1.0, 0.0, 1.0],
        [0.0, -1.0, 0.0, 1.0],
        [0.0, 0.0, 1.0, 1.0],
        [0.0, 0.0, -1.0, 1.0],
    ];

    private static readonly resources = new WeakMap<WebGL2RenderingContext, LineRendererResources>();

    private constructor() {}

    public static async drawLines(
        gl: WebGL2RenderingContext,
        modelViewProjection: Matrix4x4d,
        positions: Float32Array,
        colors: Float32Array,
        lineWidth: number,
        depthBiasNdc = 0.0,
    ): Promise<void> {
        if (positions.length === 0) {
            return;
        }
        if (positions.length !== colors.length) {
            throw new Error("positions/colors length mismatch");
        }
        if (lineWidth <= 1.0) {
            await WebGLLineRenderer.drawPrimitives(
                gl,
                modelViewProjection,
                positions,
                colors,
                gl.LINES,
                depthBiasNdc,
                lineWidth,
            );
            return;
        }

        const thickLines = WebGLLineRenderer.buildThickLineMesh(gl, modelViewProjection, positions, colors, lineWidth);
        if (thickLines.positions.length === 0) {
            return;
        }

        await WebGLLineRenderer.drawPrimitives(
            gl,
            Matrix4x4d.identityMatrix(),
            thickLines.positions,
            thickLines.colors,
            gl.TRIANGLES,
            depthBiasNdc,
            1.0,
        );
    }

    private static async drawPrimitives(
        gl: WebGL2RenderingContext,
        modelViewProjection: Matrix4x4d,
        positions: Float32Array,
        colors: Float32Array,
        primitiveType: number,
        depthBiasNdc: number,
        lineWidth: number,
    ): Promise<void> {
        if (positions.length === 0) {
            return;
        }

        const resources = await WebGLLineRenderer.ensureInitialized(gl);
        WebGLLineRenderer.disableTextureBindings(gl);

        gl.useProgram(resources.program);
        gl.uniformMatrix4fv(resources.mvpLocation, false, modelViewProjection.exportToFloatArrayColumnOrder());
        gl.uniform1f(resources.depthBiasLocation, depthBiasNdc);

        gl.bindVertexArray(resources.vertexArray);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.colorBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, colors, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(1);
        gl.vertexAttribPointer(1, 3, gl.FLOAT, false, 0, 0);

        if (primitiveType === gl.LINES) {
            gl.lineWidth(lineWidth);
        }
        gl.drawArrays(primitiveType, 0, positions.length / 3);

        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(1);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
        gl.useProgram(null);
    }

    public static release(gl: WebGL2RenderingContext): void {
        const resources = WebGLLineRenderer.resources.get(gl);
        if (resources === undefined) {
            return;
        }

        gl.deleteBuffer(resources.positionBuffer);
        gl.deleteBuffer(resources.colorBuffer);
        gl.deleteVertexArray(resources.vertexArray);
        gl.deleteProgram(resources.program);

        WebGLLineRenderer.resources.delete(gl);
    }

    private static async ensureInitialized(gl: WebGL2RenderingContext): Promise<LineRendererResources> {
        const existing = WebGLLineRenderer.resources.get(gl);
        if (existing !== undefined) {
            return existing;
        }

        const program = await WebGLShaderProgramUtil.createProgramFromFiles(
            gl,
            WebGLLineRenderer.VERTEX_SHADER_FILE,
            WebGLLineRenderer.FRAGMENT_SHADER_FILE,
        );

        const mvpLocation = gl.getUniformLocation(program, "modelViewProjectionLocal");
        if (mvpLocation === null) {
            throw new Error("Missing uniform modelViewProjectionLocal");
        }
        const depthBiasLocation = gl.getUniformLocation(program, "depthBiasNdc");
        if (depthBiasLocation === null) {
            throw new Error("Missing uniform depthBiasNdc");
        }

        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const colorBuffer = gl.createBuffer();
        if (vertexArray === null || positionBuffer === null || colorBuffer === null) {
            throw new Error("Failed to create line renderer buffers");
        }

        const resources: LineRendererResources = {
            program,
            vertexArray,
            positionBuffer,
            colorBuffer,
            mvpLocation,
            depthBiasLocation,
        };
        WebGLLineRenderer.resources.set(gl, resources);
        return resources;
    }

    private static disableTextureBindings(gl: WebGL2RenderingContext): void {
        gl.activeTexture(gl.TEXTURE1);
        gl.bindTexture(gl.TEXTURE_2D, null);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, null);
    }

    private static buildThickLineMesh(
        gl: WebGL2RenderingContext,
        modelViewProjection: Matrix4x4d,
        positions: Float32Array,
        colors: Float32Array,
        lineWidth: number,
    ): { positions: Float32Array; colors: Float32Array } {
        const trianglePositions: number[] = [];
        const triangleColors: number[] = [];
        const viewport = gl.getParameter(gl.VIEWPORT) as Int32Array;
        const viewportWidth: number = Math.max(1.0, viewport[2] ?? 1);
        const viewportHeight: number = Math.max(1.0, viewport[3] ?? 1);
        const halfWidth: number = lineWidth / 2.0;

        for (let i = 0; i + 5 < positions.length; i += 6) {
            const clip0: Vector4Dd = modelViewProjection.multiply(
                new Vector4Dd(positions[i]!, positions[i + 1]!, positions[i + 2]!, 1.0),
            );
            const clip1: Vector4Dd = modelViewProjection.multiply(
                new Vector4Dd(positions[i + 3]!, positions[i + 4]!, positions[i + 5]!, 1.0),
            );
            const clipped: Vector4Dd[] | null = WebGLLineRenderer.clipLineToClipVolume(clip0, clip1);
            if (clipped === null) {
                continue;
            }

            const ndc0: Vector4Dd = clipped[0]!.dividedByW();
            const ndc1: Vector4Dd = clipped[1]!.dividedByW();
            const dxPixels: number = ((ndc1.x() - ndc0.x()) * viewportWidth) / 2.0;
            const dyPixels: number = ((ndc1.y() - ndc0.y()) * viewportHeight) / 2.0;
            const lengthPixels: number = Math.hypot(dxPixels, dyPixels);
            if (lengthPixels <= 1.0e-9) {
                continue;
            }

            const perpX: number = -dyPixels / lengthPixels;
            const perpY: number = dxPixels / lengthPixels;
            const offsetNdcX: number = (perpX * halfWidth * 2.0) / viewportWidth;
            const offsetNdcY: number = (perpY * halfWidth * 2.0) / viewportHeight;

            const p0Plus: number[] = [ndc0.x() + offsetNdcX, ndc0.y() + offsetNdcY, ndc0.z()];
            const p0Minus: number[] = [ndc0.x() - offsetNdcX, ndc0.y() - offsetNdcY, ndc0.z()];
            const p1Plus: number[] = [ndc1.x() + offsetNdcX, ndc1.y() + offsetNdcY, ndc1.z()];
            const p1Minus: number[] = [ndc1.x() - offsetNdcX, ndc1.y() - offsetNdcY, ndc1.z()];

            const c0: number[] = [colors[i]!, colors[i + 1]!, colors[i + 2]!];
            const c1: number[] = [colors[i + 3]!, colors[i + 4]!, colors[i + 5]!];

            WebGLLineRenderer.addVertex(trianglePositions, triangleColors, p0Plus, c0);
            WebGLLineRenderer.addVertex(trianglePositions, triangleColors, p0Minus, c0);
            WebGLLineRenderer.addVertex(trianglePositions, triangleColors, p1Plus, c1);

            WebGLLineRenderer.addVertex(trianglePositions, triangleColors, p1Plus, c1);
            WebGLLineRenderer.addVertex(trianglePositions, triangleColors, p0Minus, c0);
            WebGLLineRenderer.addVertex(trianglePositions, triangleColors, p1Minus, c1);
        }

        return {
            positions: new Float32Array(trianglePositions),
            colors: new Float32Array(triangleColors),
        };
    }

    private static addVertex(positions: number[], colors: number[], point: number[], color: number[]): void {
        positions.push(point[0]!, point[1]!, point[2]!);
        colors.push(color[0]!, color[1]!, color[2]!);
    }

    private static evaluateClipPlane(plane: readonly number[], point: Vector4Dd): number {
        return plane[0]! * point.x() + plane[1]! * point.y() + plane[2]! * point.z() + plane[3]! * point.w();
    }

    private static interpolate(start: Vector4Dd, end: Vector4Dd, t: number): Vector4Dd {
        return start.multiply(1.0 - t).add(end.multiply(t));
    }

    private static clipLineToClipVolume(start: Vector4Dd, end: Vector4Dd): Vector4Dd[] | null {
        let clippedStart: Vector4Dd = start;
        let clippedEnd: Vector4Dd = end;

        for (const plane of WebGLLineRenderer.CLIP_PLANES) {
            const d0: number = WebGLLineRenderer.evaluateClipPlane(plane, clippedStart);
            const d1: number = WebGLLineRenderer.evaluateClipPlane(plane, clippedEnd);

            if (d0 < 0.0 && d1 < 0.0) {
                return null;
            }
            if (d0 < 0.0 || d1 < 0.0) {
                const denominator: number = d0 - d1;
                if (Math.abs(denominator) < 1.0e-12) {
                    return null;
                }
                const t: number = d0 / denominator;
                const intersection: Vector4Dd = WebGLLineRenderer.interpolate(clippedStart, clippedEnd, t);
                if (d0 < 0.0) {
                    clippedStart = intersection;
                } else {
                    clippedEnd = intersection;
                }
            }
        }
        return [clippedStart, clippedEnd];
    }
}
