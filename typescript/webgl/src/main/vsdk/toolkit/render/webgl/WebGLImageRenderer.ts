import {
    Image,
    Matrix4x4d,
    RendererConfiguration,
    RGBAImageCompressed,
    RGBAImageUncompressed,
    RGBImageUncompressed,
} from "@vitral/base";
import { WebGLRendererConfigurationShaderSelector } from "./WebGLRendererConfigurationShaderSelector.js";
import { WebGLRGBAImageCompressedRenderer } from "./WebGLRGBAImageCompressedRenderer.js";
import { WebGLRGBAImageUncompressedRenderer } from "./WebGLRGBAImageUncompressedRenderer.js";
import { WebGLRGBImageUncompressedRenderer } from "./WebGLRGBImageUncompressedRenderer.js";

export enum TextureFilterMode {
    LINEAR = "LINEAR",
    NEAREST = "NEAREST",
}

interface QuadResources {
    vertexArray: WebGLVertexArrayObject;
    positionBuffer: WebGLBuffer;
    uvBuffer: WebGLBuffer;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4ImageRenderer`.

The dispatch order over the three image kinds, the textured-quad vertex
submission, the attribute locations 0 and 2, and the lower-left overlay
geometry are the Java ones.

Runtime boundaries:

  - Java identifies a texture by an `int` and reports failure with a
    non-positive id. A WebGL texture is an object, so the failure sentinel is
    `null` and every `textureId <= 0` guard of the original is a `=== null`
    guard here.
  - Java's `TextureFilterMode` is a nested enum and `minFilterParam` /
    `magFilterParam` read `GL4` constants off the class. WebGL constants live
    on the context object, so those two helpers receive the context. A third
    helper, `minFilterParamWithoutMipmaps`, exists because WebGL cannot
    generate mipmaps for a compressed texture; see
    {@link WebGLRGBAImageCompressedRenderer}.
  - `drawTexturedQuad` awaits its shader program, because a browser reaches a
    GLSL source over `fetch`. Nothing else about the call changes.
  - Java keeps the quad VAO and its two VBOs in static fields, one set per
    process, because a JOGL application owns a single `GL4` context. They are
    held per context here, as in the rest of `@vitral/webgl`.
*/
export class WebGLImageRenderer {
    private static readonly TEXTURE_QUALITY = WebGLImageRenderer.createTextureQuality();
    private static textureFilterMode: TextureFilterMode = TextureFilterMode.LINEAR;

    private static readonly quadResources = new WeakMap<WebGL2RenderingContext, QuadResources>();

    private static createTextureQuality(): RendererConfiguration {
        const textureQuality = new RendererConfiguration();
        textureQuality.setTexture(true);
        textureQuality.setUseVertexColors(false);
        return textureQuality;
    }

    /**
    Compiles the shader program `drawTexturedQuad` selects, without drawing.

    Java has no counterpart, for the reason documented on
    `WebGLSimpleCorridorSample.prepare`: a frame that compiled a shader on
    demand would span a browser task boundary, at which the WebGL drawing
    buffer is presented and cleared, losing everything drawn before it. A
    caller prepares once — the moment a JOGL program uses for
    `init(GLAutoDrawable)` — and its frames then never await a network read.
    */
    public static async prepare(gl: WebGL2RenderingContext): Promise<void> {
        await WebGLRendererConfigurationShaderSelector.selectShaderProgram(gl, WebGLImageRenderer.TEXTURE_QUALITY);
    }

    public static activate(gl: WebGL2RenderingContext, img: Image | null): WebGLTexture | null {
        if (img instanceof RGBAImageCompressed) {
            return WebGLRGBAImageCompressedRenderer.activate(gl, img);
        }
        if (img instanceof RGBAImageUncompressed) {
            return WebGLRGBAImageUncompressedRenderer.activate(gl, img);
        }
        if (img instanceof RGBImageUncompressed) {
            return WebGLRGBImageUncompressedRenderer.activate(gl, img);
        }
        return null;
    }

    public static setTextureFilterMode(mode: TextureFilterMode | null): void {
        if (mode === null) {
            WebGLImageRenderer.textureFilterMode = TextureFilterMode.LINEAR;
            return;
        }
        WebGLImageRenderer.textureFilterMode = mode;
    }

    public static getTextureFilterMode(): TextureFilterMode {
        return WebGLImageRenderer.textureFilterMode;
    }

    public static minFilterParam(gl: WebGL2RenderingContext): number {
        if (WebGLImageRenderer.textureFilterMode === TextureFilterMode.NEAREST) {
            return gl.NEAREST;
        }
        return gl.LINEAR_MIPMAP_LINEAR;
    }

    public static minFilterParamWithoutMipmaps(gl: WebGL2RenderingContext): number {
        if (WebGLImageRenderer.textureFilterMode === TextureFilterMode.NEAREST) {
            return gl.NEAREST;
        }
        return gl.LINEAR;
    }

    public static magFilterParam(gl: WebGL2RenderingContext): number {
        if (WebGLImageRenderer.textureFilterMode === TextureFilterMode.NEAREST) {
            return gl.NEAREST;
        }
        return gl.LINEAR;
    }

    public static deactivate(gl: WebGL2RenderingContext, img: Image | null): void {
        if (img instanceof RGBAImageCompressed) {
            WebGLRGBAImageCompressedRenderer.deactivate(gl, img);
        } else if (img instanceof RGBAImageUncompressed) {
            WebGLRGBAImageUncompressedRenderer.deactivate(gl, img);
        } else if (img instanceof RGBImageUncompressed) {
            WebGLRGBImageUncompressedRenderer.deactivate(gl, img);
        }
    }

    public static unload(gl: WebGL2RenderingContext, img: Image | null): void {
        if (img instanceof RGBAImageCompressed) {
            WebGLRGBAImageCompressedRenderer.unload(gl, img);
        } else if (img instanceof RGBAImageUncompressed) {
            WebGLRGBAImageUncompressedRenderer.unload(gl, img);
        } else if (img instanceof RGBImageUncompressed) {
            WebGLRGBImageUncompressedRenderer.unload(gl, img);
        }
    }

    public static async draw(gl: WebGL2RenderingContext, img: Image | null): Promise<void> {
        if (img instanceof RGBAImageCompressed) {
            await WebGLRGBAImageCompressedRenderer.draw(gl, img);
        } else if (img instanceof RGBAImageUncompressed) {
            await WebGLRGBAImageUncompressedRenderer.draw(gl, img);
        } else if (img instanceof RGBImageUncompressed) {
            await WebGLRGBImageUncompressedRenderer.draw(gl, img);
        }
    }

    public static async drawTexturedQuad(
        gl: WebGL2RenderingContext,
        texture: WebGLTexture | null,
        modelViewProjection: Matrix4x4d,
        positions: Float32Array | null,
        uvCoordinates: Float32Array | null,
        diffuseR: number,
        diffuseG: number,
        diffuseB: number,
    ): Promise<void> {
        if (texture === null || positions === null || uvCoordinates === null) {
            return;
        }
        if (positions.length % 3 !== 0 || uvCoordinates.length % 2 !== 0) {
            throw new Error("Invalid quad data");
        }
        if (positions.length / 3 !== uvCoordinates.length / 2) {
            throw new Error("Position/UV vertex count mismatch");
        }

        const resources = WebGLImageRenderer.ensureBuffers(gl);

        // Java sets glPolygonMode(GL_FRONT_AND_BACK, GL_FILL) here. WebGL has
        // no polygon mode: filled rasterization is the only one it offers, so
        // the call has no counterpart and nothing to replace it.

        const program = await WebGLRendererConfigurationShaderSelector.selectShaderProgram(
            gl,
            WebGLImageRenderer.TEXTURE_QUALITY,
        );
        WebGLRendererConfigurationShaderSelector.activateShader(
            gl,
            program,
            modelViewProjection,
            WebGLImageRenderer.TEXTURE_QUALITY,
            diffuseR,
            diffuseG,
            diffuseB,
        );

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);

        gl.bindVertexArray(resources.vertexArray);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.positionBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, positions, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(0);
        gl.vertexAttribPointer(0, 3, gl.FLOAT, false, 0, 0);

        gl.bindBuffer(gl.ARRAY_BUFFER, resources.uvBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, uvCoordinates, gl.STREAM_DRAW);
        gl.enableVertexAttribArray(2);
        gl.vertexAttribPointer(2, 2, gl.FLOAT, false, 0, 0);

        gl.drawArrays(gl.TRIANGLES, 0, positions.length / 3);

        gl.disableVertexAttribArray(0);
        gl.disableVertexAttribArray(2);
        gl.bindBuffer(gl.ARRAY_BUFFER, null);
        gl.bindVertexArray(null);
        gl.bindTexture(gl.TEXTURE_2D, null);

        WebGLRendererConfigurationShaderSelector.deactivateShader(gl);
    }

    public static async drawLowerLeftOverlay(
        gl: WebGL2RenderingContext,
        texture: WebGLTexture,
        width: number,
        height: number,
    ): Promise<void> {
        const viewport = gl.getParameter(gl.VIEWPORT) as Int32Array;
        const viewportWidth = Math.max(viewport[2] ?? 1, 1);
        const viewportHeight = Math.max(viewport[3] ?? 1, 1);

        const w = 2.0 * (width / viewportWidth);
        const h = 2.0 * (height / viewportHeight);

        const x0 = -1.0;
        const y0 = -1.0;
        const x1 = x0 + w;
        const y1 = y0 + h;

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

        await WebGLImageRenderer.drawTexturedQuad(
            gl,
            texture,
            Matrix4x4d.identityMatrix(),
            positions,
            uvCoordinates,
            1.0,
            1.0,
            1.0,
        );
    }

    public static dispose(gl: WebGL2RenderingContext): void {
        WebGLRendererConfigurationShaderSelector.dispose(gl);

        const resources = WebGLImageRenderer.quadResources.get(gl);
        if (resources === undefined) {
            return;
        }

        gl.deleteBuffer(resources.positionBuffer);
        gl.deleteBuffer(resources.uvBuffer);
        gl.deleteVertexArray(resources.vertexArray);
        WebGLImageRenderer.quadResources.delete(gl);
    }

    private static ensureBuffers(gl: WebGL2RenderingContext): QuadResources {
        const existing = WebGLImageRenderer.quadResources.get(gl);
        if (existing !== undefined) {
            return existing;
        }

        const vertexArray = gl.createVertexArray();
        const positionBuffer = gl.createBuffer();
        const uvBuffer = gl.createBuffer();
        if (vertexArray === null || positionBuffer === null || uvBuffer === null) {
            throw new Error("Failed to create WebGL image renderer quad resources.");
        }

        const resources: QuadResources = { vertexArray, positionBuffer, uvBuffer };
        WebGLImageRenderer.quadResources.set(gl, resources);
        return resources;
    }
}
