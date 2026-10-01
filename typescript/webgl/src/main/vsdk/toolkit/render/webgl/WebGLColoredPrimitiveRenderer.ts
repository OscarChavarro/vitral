import type { Matrix4x4d } from "@vitral/base";
import { WebGLShaderProgramUtil } from "./WebGLShaderProgramUtil.js";

interface ColoredPrimitiveResources {
    program: WebGLProgram;
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    colorBuffer: WebGLBuffer;
    mvpLocation: WebGLUniformLocation | null;
    depthBiasLocation: WebGLUniformLocation | null;
    pointSizeLocation: WebGLUniformLocation | null;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer`.

Draws batches of primitives (triangles, triangle strips and fans, lines,
points) with a color (and transparency) per vertex and no lighting: the GL4
replacement of immediate mode (`glBegin`, `glColor`, `glVertex`, `glEnd`)
used to draw gizmos, grids, borders and overlays.

The caller sets the depth test, blending, culling and polygon mode; this
class only binds its program and vertex buffers. The batch is uploaded on each
call, so it is meant for small amounts of geometry.

The runtime boundaries are this package's recurring ones: the per-process GL
state of Java is kept per `WebGL2RenderingContext` in a `WeakMap`, the GLSL
sources arrive over `fetch` (so drawing is asynchronous), and the point size
Java leaves to `glPointSize` travels as the `pointSizeLocal` uniform the
`WebGLShaderPreprocessor` installs, set to OpenGL's default of one pixel.
*/
export class WebGLColoredPrimitiveRenderer {
    private static readonly VERTEX_SHADER_FILE = "coloredPrimitiveVertexShader.glsl";
    private static readonly FRAGMENT_SHADER_FILE = "coloredPrimitivePixelShader.glsl";

    private static readonly resources = new WeakMap<WebGL2RenderingContext, Promise<ColoredPrimitiveResources>>();

    private constructor() {}

    /**
    Compiles the program ahead of the first frame (Java does it lazily inside
    `ensureInitialized`).
    */
    public static async prepare(gl: WebGL2RenderingContext): Promise<void> {
        await WebGLColoredPrimitiveRenderer.ensureInitialized(gl);
    }

    /**
    Draws a batch of primitives, moving it towards (negative) or away from
    (positive) the viewer by a bias in normalized device coordinates (Java's
    two overloads: without a bias, it is zero).

    @param gl OpenGL context
    @param modelViewProjection matrix that takes the positions to clip space
    @param primitiveType `gl.TRIANGLES`, `gl.TRIANGLE_STRIP`,
    `gl.TRIANGLE_FAN`, `gl.LINES`, `gl.LINE_LOOP`, `gl.POINTS`...
    @param positions x, y, z of each vertex
    @param colors r, g, b, a of each vertex
    @param depthBiasNdc depth bias, in normalized device coordinates
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        modelViewProjection: Matrix4x4d,
        primitiveType: number,
        positions: Float32Array | null,
        colors: Float32Array | null,
        depthBiasNdc: number = 0.0,
    ): Promise<void> {
        if (positions === null || colors === null || positions.length === 0) {
            return;
        }
        const vertexCount: number = positions.length / 3;
        if (colors.length !== vertexCount * 4) {
            throw new Error("positions/colors length mismatch");
        }

        const resources: ColoredPrimitiveResources = await WebGLColoredPrimitiveRenderer.ensureInitialized(gl);

        gl.useProgram(resources.program);
        gl.uniformMatrix4fv(resources.mvpLocation, false, modelViewProjection.exportToFloatArrayColumnOrder());
        gl.uniform1f(resources.depthBiasLocation, depthBiasNdc);
        if (resources.pointSizeLocation !== null) {
            gl.uniform1f(resources.pointSizeLocation, 1.0);
        }

        gl.bindVertexArray(resources.vertexArray);
        WebGLColoredPrimitiveRenderer.upload(gl, resources.positionBuffer, 0, 3, positions);
        WebGLColoredPrimitiveRenderer.upload(gl, resources.colorBuffer, 1, 4, colors);

        gl.drawArrays(primitiveType, 0, vertexCount);

        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(1);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
        gl.useProgram(null);
    }

    /**
    Releases the OpenGL objects of this renderer. PRE: the context that
    created them is current.
    */
    public static release(gl: WebGL2RenderingContext): void {
        const pending: Promise<ColoredPrimitiveResources> | undefined = WebGLColoredPrimitiveRenderer.resources.get(gl);

        if (pending === undefined) {
            return;
        }
        WebGLColoredPrimitiveRenderer.resources.delete(gl);
        void pending.then((resources: ColoredPrimitiveResources) => {
            gl.deleteBuffer(resources.positionBuffer);
            gl.deleteBuffer(resources.colorBuffer);
            gl.deleteVertexArray(resources.vertexArray);
            gl.deleteProgram(resources.program);
        });
    }

    private static upload(gl: WebGL2RenderingContext, vbo: WebGLBuffer, attribute: number, size: number, data: Float32Array): void {
        gl.bindBuffer(gl.ARRAY_BUFFER, vbo);
        gl.bufferData(gl.ARRAY_BUFFER, data, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(attribute);
        gl.vertexAttribPointer(attribute, size, gl.FLOAT, false, 0, 0);
    }

    private static ensureInitialized(gl: WebGL2RenderingContext): Promise<ColoredPrimitiveResources> {
        let pending: Promise<ColoredPrimitiveResources> | undefined = WebGLColoredPrimitiveRenderer.resources.get(gl);

        if (pending === undefined) {
            pending = WebGLColoredPrimitiveRenderer.initialize(gl);
            WebGLColoredPrimitiveRenderer.resources.set(gl, pending);
            pending.catch(() => WebGLColoredPrimitiveRenderer.resources.delete(gl));
        }
        return pending;
    }

    private static async initialize(gl: WebGL2RenderingContext): Promise<ColoredPrimitiveResources> {
        const program: WebGLProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
            gl,
            WebGLColoredPrimitiveRenderer.VERTEX_SHADER_FILE,
            WebGLColoredPrimitiveRenderer.FRAGMENT_SHADER_FILE,
        );
        const vertexArray: WebGLVertexArrayObject | null = gl.createVertexArray();
        const positionBuffer: WebGLBuffer | null = gl.createBuffer();
        const colorBuffer: WebGLBuffer | null = gl.createBuffer();

        if (vertexArray === null || positionBuffer === null || colorBuffer === null) {
            throw new Error("WebGLColoredPrimitiveRenderer: cannot create its GL objects");
        }
        return {
            program,
            vertexArray,
            positionBuffer,
            colorBuffer,
            mvpLocation: gl.getUniformLocation(program, "modelViewProjectionLocal"),
            depthBiasLocation: gl.getUniformLocation(program, "depthBiasNdc"),
            pointSizeLocation: gl.getUniformLocation(program, "pointSizeLocal"),
        };
    }
}
