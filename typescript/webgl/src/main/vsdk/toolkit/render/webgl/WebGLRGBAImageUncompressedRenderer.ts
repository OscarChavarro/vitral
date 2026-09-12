import { RGBAImageUncompressed } from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";

/**
Port of `vsdk.toolkit.render.jogl.Jogl4RGBAImageUncompressedRenderer`. The
identity map and the texture-object sentinel follow the same two runtime
boundaries documented in {@link WebGLRGBImageUncompressedRenderer}.
*/
export class WebGLRGBAImageUncompressedRenderer {
    private static readonly compiledImages = new WeakMap<
        WebGL2RenderingContext,
        WeakMap<RGBAImageUncompressed, WebGLTexture>
    >();

    public static activate(gl: WebGL2RenderingContext, img: RGBAImageUncompressed | null): WebGLTexture | null {
        if (img === null) {
            return null;
        }

        const compiledImages = WebGLRGBAImageUncompressedRenderer.getCompiledImages(gl);
        let texture = compiledImages.get(img);
        if (texture === undefined) {
            texture = WebGLRGBAImageUncompressedRenderer.upload(gl, img);
            compiledImages.set(img, texture);
        }

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);
        return texture;
    }

    public static deactivate(gl: WebGL2RenderingContext, img: RGBAImageUncompressed | null): void {
        if (img !== null && WebGLRGBAImageUncompressedRenderer.getCompiledImages(gl).has(img)) {
            gl.bindTexture(gl.TEXTURE_2D, null);
        }
    }

    public static unload(gl: WebGL2RenderingContext, img: RGBAImageUncompressed | null): void {
        if (img === null) {
            return;
        }
        const compiledImages = WebGLRGBAImageUncompressedRenderer.getCompiledImages(gl);
        const texture = compiledImages.get(img);
        if (texture === undefined) {
            return;
        }
        compiledImages.delete(img);

        gl.deleteTexture(texture);
    }

    public static async draw(gl: WebGL2RenderingContext, img: RGBAImageUncompressed): Promise<void> {
        const texture = WebGLRGBAImageUncompressedRenderer.activate(gl, img);
        if (texture === null) {
            return;
        }

        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);
        gl.enable(gl.BLEND);
        gl.blendFunc(gl.SRC_ALPHA, gl.ONE_MINUS_SRC_ALPHA);

        await WebGLImageRenderer.drawLowerLeftOverlay(gl, texture, img.getXSize(), img.getYSize());

        gl.disable(gl.BLEND);
        gl.enable(gl.DEPTH_TEST);
    }

    private static upload(gl: WebGL2RenderingContext, img: RGBAImageUncompressed): WebGLTexture {
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
            gl.RGBA8,
            img.getXSize(),
            img.getYSize(),
            0,
            gl.RGBA,
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

    private static getCompiledImages(gl: WebGL2RenderingContext): WeakMap<RGBAImageUncompressed, WebGLTexture> {
        let compiledImages = WebGLRGBAImageUncompressedRenderer.compiledImages.get(gl);
        if (compiledImages === undefined) {
            compiledImages = new WeakMap<RGBAImageUncompressed, WebGLTexture>();
            WebGLRGBAImageUncompressedRenderer.compiledImages.set(gl, compiledImages);
        }
        return compiledImages;
    }
}
