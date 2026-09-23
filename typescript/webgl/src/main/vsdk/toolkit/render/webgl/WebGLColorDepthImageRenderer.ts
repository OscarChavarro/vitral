import { RGBImageUncompressed, ZBuffer } from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";
import { WebGLShaderProgramUtil } from "./WebGLShaderProgramUtil.js";

interface ColorDepthResources {
    program: WebGLProgram;
    colorTextureLocation: WebGLUniformLocation | null;
    depthTextureLocation: WebGLUniformLocation | null;
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    uvBuffer: WebGLBuffer;
    colorTexture: WebGLTexture;
    depthTexture: WebGLTexture;
    textureXSize: number;
    textureYSize: number;
    depthUploadBuffer: Float32Array;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ColorDepthImageRenderer`.

Draws an image computed outside WebGL (i.e. by `ParallelRaytracer`) together
with its depth buffer: the image goes to the color buffer and the depth values
to the depth buffer, over the lower left corner of the current viewport, one
texel per pixel. Geometry rasterized afterwards is then depth tested against
the image, which allows mixing raytraced and rasterized objects in one view.

The depth buffer must hold window space depth values for the camera used to
rasterize the rest of the view (see `DepthBufferMode.OPENGL_DEPTH`). Its row 0
is the top row of the image.

Runtime boundaries, as in the rest of `@vitral/webgl`:

  - The shader source is fetched, so `prepare` compiles it ahead of the first
    frame and `draw` awaits it (a no-op once prepared).
  - The program, buffers and textures are held per context instead of in
    static fields. `R32F` textures are sampled with `NEAREST` filtering, which
    WebGL2 supports without extensions.
  - WebGL has no polygon mode: Java's `glPolygonMode(GL_FILL)` has no
    counterpart.
*/
export class WebGLColorDepthImageRenderer {
    private static readonly VERTEX_SHADER_FILE = "colorDepthImageVertexShader.glsl";
    private static readonly FRAGMENT_SHADER_FILE = "colorDepthImagePixelShader.glsl";

    private static readonly resources = new WeakMap<WebGL2RenderingContext, Promise<ColorDepthResources>>();

    /**
    Compiles the shader program and creates the resources of this renderer
    for a context, without drawing.
    @param gl WebGL context
    */
    public static async prepare(gl: WebGL2RenderingContext): Promise<void> {
        await WebGLColorDepthImageRenderer.ensureResources(gl);
    }

    /**
    Draws the image in the color buffer and its depth in the depth buffer.
    Depth test, depth function and face culling are left as `LESS` with depth
    test enabled and culling disabled.
    @param gl WebGL context
    @param image color of each pixel
    @param depth window space depth of each pixel, of the size of `image`, or
    null to draw only the color (without touching the depth buffer)
    */
    public static async draw(
        gl: WebGL2RenderingContext,
        image: RGBImageUncompressed | null,
        depth: ZBuffer | null,
    ): Promise<void> {
        if (image === null || image.getXSize() <= 0 || image.getYSize() <= 0) {
            return;
        }
        if (depth !== null && (depth.getXSize() !== image.getXSize() || depth.getYSize() !== image.getYSize())) {
            console.warn(
                "WebGLColorDepthImageRenderer.draw: Depth buffer size does not match the image size, " +
                    "drawing color only",
            );
            depth = null;
        }
        if (depth === null) {
            WebGLImageRenderer.unload(gl, image);
            await WebGLImageRenderer.draw(gl, image);
            return;
        }

        const resources: ColorDepthResources = await WebGLColorDepthImageRenderer.ensureResources(gl);
        WebGLColorDepthImageRenderer.uploadTextures(gl, resources, image, depth);

        gl.disable(gl.CULL_FACE);
        // Depth writes need the depth test enabled; ALWAYS replaces what was there
        gl.enable(gl.DEPTH_TEST);
        gl.depthFunc(gl.ALWAYS);
        gl.depthMask(true);

        gl.useProgram(resources.program);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, resources.colorTexture);
        gl.uniform1i(resources.colorTextureLocation, 0);
        gl.activeTexture(gl.TEXTURE1);
        gl.bindTexture(gl.TEXTURE_2D, resources.depthTexture);
        gl.uniform1i(resources.depthTextureLocation, 1);

        WebGLColorDepthImageRenderer.drawLowerLeftQuad(gl, resources, image.getXSize(), image.getYSize());

        gl.activeTexture(gl.TEXTURE1);
        gl.bindTexture(gl.TEXTURE_2D, null);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, null);
        gl.useProgram(null);
        gl.depthFunc(gl.LESS);
    }

    /**
    Releases the shader program, buffers and textures of this renderer for a
    context.
    @param gl WebGL context
    */
    public static async dispose(gl: WebGL2RenderingContext): Promise<void> {
        const pending = WebGLColorDepthImageRenderer.resources.get(gl);
        if (pending === undefined) {
            return;
        }
        WebGLColorDepthImageRenderer.resources.delete(gl);
        const resources: ColorDepthResources = await pending;
        gl.deleteProgram(resources.program);
        gl.deleteBuffer(resources.positionBuffer);
        gl.deleteBuffer(resources.uvBuffer);
        gl.deleteVertexArray(resources.vertexArray);
        gl.deleteTexture(resources.colorTexture);
        gl.deleteTexture(resources.depthTexture);
    }

    private static drawLowerLeftQuad(
        gl: WebGL2RenderingContext,
        resources: ColorDepthResources,
        width: number,
        height: number,
    ): void {
        const viewport = gl.getParameter(gl.VIEWPORT) as Int32Array;
        const w: number = 2.0 * (width / Math.max(viewport[2] ?? 1, 1));
        const h: number = 2.0 * (height / Math.max(viewport[3] ?? 1, 1));
        const x0 = -1.0;
        const y0 = -1.0;
        const x1: number = x0 + w;
        const y1: number = y0 + h;
        const positions = new Float32Array([
            x0,
            y0,
            0.0,
            x1,
            y0,
            0.0,
            x1,
            y1,
            0.0,
            x0,
            y0,
            0.0,
            x1,
            y1,
            0.0,
            x0,
            y1,
            0.0,
        ]);
        const uvCoordinates = new Float32Array([0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 1.0]);

        gl.bindVertexArray(resources.vertexArray);
        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);
        gl.bindBuffer(gl.ARRAY_BUFFER, resources.uvBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, uvCoordinates, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(2);
        gl.vertexAttribPointer(2, 2, gl.FLOAT, false, 0, 0);

        gl.drawArrays(gl.TRIANGLES, 0, 6);

        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(2);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
    }

    private static uploadTextures(
        gl: WebGL2RenderingContext,
        resources: ColorDepthResources,
        image: RGBImageUncompressed,
        depth: ZBuffer,
    ): void {
        const xSize: number = image.getXSize();
        const ySize: number = image.getYSize();
        const resized: boolean = xSize !== resources.textureXSize || ySize !== resources.textureYSize;

        // The raw image stores its bottom row first, as WebGL textures, while
        // the depth buffer stores its top row first: depth rows are flipped
        const count: number = xSize * ySize;
        if (resources.depthUploadBuffer.length !== count) {
            resources.depthUploadBuffer = new Float32Array(count);
        }
        const values: Float32Array = depth.getZBuffer();
        let write = 0;
        for (let row = ySize - 1; row >= 0; row--) {
            resources.depthUploadBuffer.set(values.subarray(row * xSize, (row + 1) * xSize), write);
            write += xSize;
        }

        gl.pixelStorei(gl.UNPACK_ALIGNMENT, 1);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, resources.colorTexture);
        if (resized) {
            gl.texImage2D(
                gl.TEXTURE_2D,
                0,
                gl.RGB8,
                xSize,
                ySize,
                0,
                gl.RGB,
                gl.UNSIGNED_BYTE,
                image.getRawImageDirectBuffer(),
            );
        } else {
            gl.texSubImage2D(
                gl.TEXTURE_2D,
                0,
                0,
                0,
                xSize,
                ySize,
                gl.RGB,
                gl.UNSIGNED_BYTE,
                image.getRawImageDirectBuffer(),
            );
        }

        gl.bindTexture(gl.TEXTURE_2D, resources.depthTexture);
        if (resized) {
            gl.texImage2D(gl.TEXTURE_2D, 0, gl.R32F, xSize, ySize, 0, gl.RED, gl.FLOAT, resources.depthUploadBuffer);
        } else {
            gl.texSubImage2D(gl.TEXTURE_2D, 0, 0, 0, xSize, ySize, gl.RED, gl.FLOAT, resources.depthUploadBuffer);
        }
        gl.bindTexture(gl.TEXTURE_2D, null);

        resources.textureXSize = xSize;
        resources.textureYSize = ySize;
    }

    private static createTexture(gl: WebGL2RenderingContext): WebGLTexture {
        const texture = gl.createTexture();
        if (texture === null) {
            throw new Error("Failed to create WebGL color/depth image texture.");
        }
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.bindTexture(gl.TEXTURE_2D, null);
        return texture;
    }

    private static ensureResources(gl: WebGL2RenderingContext): Promise<ColorDepthResources> {
        const existing = WebGLColorDepthImageRenderer.resources.get(gl);
        if (existing !== undefined) {
            return existing;
        }
        const pending: Promise<ColorDepthResources> = WebGLColorDepthImageRenderer.createResources(gl);
        WebGLColorDepthImageRenderer.resources.set(gl, pending);
        pending.catch(() => WebGLColorDepthImageRenderer.resources.delete(gl));
        return pending;
    }

    private static async createResources(gl: WebGL2RenderingContext): Promise<ColorDepthResources> {
        const program: WebGLProgram = await WebGLShaderProgramUtil.createProgramFromFiles(
            gl,
            WebGLColorDepthImageRenderer.VERTEX_SHADER_FILE,
            WebGLColorDepthImageRenderer.FRAGMENT_SHADER_FILE,
        );
        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const uvBuffer = gl.createBuffer();
        if (vertexArray === null || positionBuffer === null || uvBuffer === null) {
            throw new Error("Failed to create WebGL color/depth image quad resources.");
        }
        return {
            program,
            colorTextureLocation: gl.getUniformLocation(program, "colorTexture"),
            depthTextureLocation: gl.getUniformLocation(program, "depthTexture"),
            vertexArray,
            positionBuffer,
            uvBuffer,
            colorTexture: WebGLColorDepthImageRenderer.createTexture(gl),
            depthTexture: WebGLColorDepthImageRenderer.createTexture(gl),
            textureXSize: 0,
            textureYSize: 0,
            depthUploadBuffer: new Float32Array(0),
        };
    }
}
