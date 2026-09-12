import { RGBImageUncompressed } from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4RGBImageUncompressedRenderer`.

Java keys its `IdentityHashMap<RGBImageUncompressed, Integer>` on the image and
holds one entry per process, because a JOGL application owns a single `GL4`
context. A texture object belongs to the context that created it, so here the
identity map is nested inside a per-context `WeakMap`. A WebGL texture is an
object rather than an int, so Java's `textureId <= 0` failure sentinel is
`null`.
*/
export class WebGLRGBImageUncompressedRenderer {
    private static readonly compiledImages = new WeakMap<
        WebGL2RenderingContext,
        WeakMap<RGBImageUncompressed, WebGLTexture>
    >();

    public static activate(gl: WebGL2RenderingContext, img: RGBImageUncompressed | null): WebGLTexture | null {
        if (img === null) {
            return null;
        }

        const compiledImages = WebGLRGBImageUncompressedRenderer.getCompiledImages(gl);
        let texture = compiledImages.get(img);
        if (texture === undefined) {
            texture = WebGLRGBImageUncompressedRenderer.upload(gl, img);
            compiledImages.set(img, texture);
        }

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);
        return texture;
    }

    public static deactivate(gl: WebGL2RenderingContext, img: RGBImageUncompressed | null): void {
        if (img !== null && WebGLRGBImageUncompressedRenderer.getCompiledImages(gl).has(img)) {
            gl.bindTexture(gl.TEXTURE_2D, null);
        }
    }

    public static unload(gl: WebGL2RenderingContext, img: RGBImageUncompressed | null): void {
        if (img === null) {
            return;
        }
        const compiledImages = WebGLRGBImageUncompressedRenderer.getCompiledImages(gl);
        const texture = compiledImages.get(img);
        if (texture === undefined) {
            return;
        }
        compiledImages.delete(img);

        gl.deleteTexture(texture);
    }

    public static async draw(gl: WebGL2RenderingContext, img: RGBImageUncompressed): Promise<void> {
        const texture = WebGLRGBImageUncompressedRenderer.activate(gl, img);
        if (texture === null) {
            return;
        }

        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);

        await WebGLImageRenderer.drawLowerLeftOverlay(gl, texture, img.getXSize(), img.getYSize());

        gl.enable(gl.DEPTH_TEST);
    }

    private static upload(gl: WebGL2RenderingContext, img: RGBImageUncompressed): WebGLTexture {
        const texture = gl.createTexture();
        if (texture === null) {
            throw new Error("Failed to create texture");
        }

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);

        gl.pixelStorei(gl.UNPACK_ALIGNMENT, 1);
        gl.texImage2D(
            gl.TEXTURE_2D,
            0,
            gl.RGB8,
            img.getXSize(),
            img.getYSize(),
            0,
            gl.RGB,
            gl.UNSIGNED_BYTE,
            img.getRawImageDirectBuffer(),
        );
        gl.generateMipmap(gl.TEXTURE_2D);

        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, WebGLImageRenderer.magFilterParam(gl));
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, WebGLImageRenderer.minFilterParam(gl));
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.REPEAT);

        gl.bindTexture(gl.TEXTURE_2D, null);

        return texture;
    }

    private static getCompiledImages(gl: WebGL2RenderingContext): WeakMap<RGBImageUncompressed, WebGLTexture> {
        let compiledImages = WebGLRGBImageUncompressedRenderer.compiledImages.get(gl);
        if (compiledImages === undefined) {
            compiledImages = new WeakMap<RGBImageUncompressed, WebGLTexture>();
            WebGLRGBImageUncompressedRenderer.compiledImages.set(gl, compiledImages);
        }
        return compiledImages;
    }
}
