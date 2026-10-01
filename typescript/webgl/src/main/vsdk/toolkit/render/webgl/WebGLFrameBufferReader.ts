import { RGBImageUncompressed, ZBuffer } from "@vitral/base";
import { WebGLShaderProgramUtil } from "./WebGLShaderProgramUtil.js";

interface DepthPackResources {
    program: WebGLProgram;
    vertexArray: WebGLVertexArrayObject;
    depthTextureLocation: WebGLUniformLocation | null;
    viewportOriginLocation: WebGLUniformLocation | null;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4FrameBufferReader`.

Reads back the contents of the current frame buffer (the area of the current
WebGL viewport) into VSDK images.

`readColor` is Java's. `readDepth` crosses a runtime boundary: WebGL can not
read a depth buffer with `readPixels`, so the depth must have been drawn into
a depth texture (see `WebGLOffscreenFrameBuffer`), which is passed here. The
texels of the current viewport are packed into an 8 bit color target by the
`depthPack*.glsl` program (24 bits per depth, without loss) and unpacked
after `readPixels`, giving the same window depths in [0, 1] that
`glReadPixels(GL_DEPTH_COMPONENT)` gives. Since the GLSL sources arrive over
`fetch`, `readDepth` is asynchronous.
*/
export class WebGLFrameBufferReader {
    private static readonly VERTEX_SHADER_FILE: string = "depthPackVertexShader.glsl";
    private static readonly FRAGMENT_SHADER_FILE: string = "depthPackPixelShader.glsl";
    private static readonly MAX_DEPTH_CODE: number = 16777215;

    private static readonly resources: WeakMap<WebGL2RenderingContext, Promise<DepthPackResources>> =
        new WeakMap<WebGL2RenderingContext, Promise<DepthPackResources>>();

    private constructor() {}

    /**
    @param gl WebGL context
    @return the colors of the area of the current viewport of the frame
    buffer bound for reading, with the first row at the top
    */
    public static readColor(gl: WebGL2RenderingContext): RGBImageUncompressed {
        const view: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
        const width: number = view[2]!;
        const height: number = view[3]!;
        const bb: Uint8Array = new Uint8Array(4 * width * height);

        gl.pixelStorei(gl.PACK_ALIGNMENT, 1);
        gl.readPixels(view[0]!, view[1]!, width, height, gl.RGBA, gl.UNSIGNED_BYTE, bb);

        const image: RGBImageUncompressed = new RGBImageUncompressed();
        image.init(width, height);

        let pos: number = 0;
        for (let y: number = height - 1; y >= 0; y--) {
            for (let x: number = 0; x < width; x++) {
                image.putPixel(x, y, bb[pos]!, bb[pos + 1]!, bb[pos + 2]!);
                pos += 4;
            }
        }
        return image;
    }

    /**
    @param gl WebGL context
    @param depthTexture depth texture (`DEPTH_COMPONENT24`) the frame was
    drawn into
    @return the depths of the area of the current viewport, with the first
    row at the top
    */
    public static async readDepth(gl: WebGL2RenderingContext, depthTexture: WebGLTexture): Promise<ZBuffer> {
        const view: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;
        const x0: number = view[0]!;
        const y0: number = view[1]!;
        const width: number = view[2]!;
        const height: number = view[3]!;
        const r: DepthPackResources = await WebGLFrameBufferReader.ensureInitialized(gl);
        const previousFrameBuffer: WebGLFramebuffer | null =
            gl.getParameter(gl.FRAMEBUFFER_BINDING) as WebGLFramebuffer | null;
        const depthTestEnabled: boolean = gl.isEnabled(gl.DEPTH_TEST);
        const blendEnabled: boolean = gl.isEnabled(gl.BLEND);

        // Target for the packed depths
        const target: WebGLFramebuffer | null = gl.createFramebuffer();
        const color: WebGLRenderbuffer | null = gl.createRenderbuffer();
        gl.bindRenderbuffer(gl.RENDERBUFFER, color);
        gl.renderbufferStorage(gl.RENDERBUFFER, gl.RGBA8, Math.max(1, width), Math.max(1, height));
        gl.bindRenderbuffer(gl.RENDERBUFFER, null);
        gl.bindFramebuffer(gl.FRAMEBUFFER, target);
        gl.framebufferRenderbuffer(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.RENDERBUFFER, color);

        // The depth texture can not be sampled while it is attached to the
        // frame buffer being drawn: the target above is a different one
        gl.viewport(0, 0, width, height);
        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.BLEND);
        gl.useProgram(r.program);
        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, depthTexture);
        gl.uniform1i(r.depthTextureLocation, 0);
        gl.uniform2i(r.viewportOriginLocation, x0, y0);
        gl.bindVertexArray(r.vertexArray);
        gl.drawArrays(gl.TRIANGLES, 0, 3);
        gl.bindVertexArray(null);
        gl.bindTexture(gl.TEXTURE_2D, null);
        gl.useProgram(null);

        const bb: Uint8Array = new Uint8Array(4 * width * height);
        gl.pixelStorei(gl.PACK_ALIGNMENT, 1);
        gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, bb);

        gl.bindFramebuffer(gl.FRAMEBUFFER, previousFrameBuffer);
        gl.deleteFramebuffer(target);
        gl.deleteRenderbuffer(color);
        gl.viewport(x0, y0, width, height);
        if (depthTestEnabled) {
            gl.enable(gl.DEPTH_TEST);
        }
        if (blendEnabled) {
            gl.enable(gl.BLEND);
        }

        const result: ZBuffer = new ZBuffer(width, height);

        let pos: number = 0;
        for (let y: number = height - 1; y >= 0; y--) {
            for (let x: number = 0; x < width; x++) {
                const code: number = (bb[pos]! << 16) | (bb[pos + 1]! << 8) | bb[pos + 2]!;
                result.setZ(x, y, code / WebGLFrameBufferReader.MAX_DEPTH_CODE);
                pos += 4;
            }
        }
        return result;
    }

    /**
    Releases the program of the depth reads of a context.
    @param gl WebGL context
    */
    public static release(gl: WebGL2RenderingContext): void {
        const pending: Promise<DepthPackResources> | undefined = WebGLFrameBufferReader.resources.get(gl);

        if (pending === undefined) {
            return;
        }
        WebGLFrameBufferReader.resources.delete(gl);
        void pending.then((r: DepthPackResources): void => {
            gl.deleteProgram(r.program);
            gl.deleteVertexArray(r.vertexArray);
        });
    }

    private static ensureInitialized(gl: WebGL2RenderingContext): Promise<DepthPackResources> {
        let pending: Promise<DepthPackResources> | undefined = WebGLFrameBufferReader.resources.get(gl);

        if (pending === undefined) {
            pending = WebGLShaderProgramUtil.createProgramFromFiles(gl,
                WebGLFrameBufferReader.VERTEX_SHADER_FILE,
                WebGLFrameBufferReader.FRAGMENT_SHADER_FILE,
            ).then((program: WebGLProgram): DepthPackResources => {
                const vertexArray: WebGLVertexArrayObject | null = gl.createVertexArray();
                if (vertexArray === null) {
                    throw new Error("Failed to create vertex array");
                }
                return {
                    program,
                    vertexArray,
                    depthTextureLocation: gl.getUniformLocation(program, "depthTexture"),
                    viewportOriginLocation: gl.getUniformLocation(program, "viewportOrigin"),
                };
            });
            WebGLFrameBufferReader.resources.set(gl, pending);
        }
        return pending;
    }
}
