import { Logger, RGBAImageCompressed, VSDK } from "@vitral/base";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";

/**
The subset of `WEBGL_compressed_texture_s3tc` this renderer needs. The
extension object carries the same `GL_COMPRESSED_*_S3TC_DXT*_EXT` enum values
the Java port reads from `com.jogamp.opengl.GL`.
*/
interface S3tcExtension {
    readonly COMPRESSED_RGBA_S3TC_DXT1_EXT: number;
    readonly COMPRESSED_RGBA_S3TC_DXT3_EXT: number;
    readonly COMPRESSED_RGBA_S3TC_DXT5_EXT: number;
}

/**
Port of `vsdk.toolkit.render.jogl.Jogl4RGBAImageCompressedRenderer`.

This is the renderer that keeps a DXT-compressed image compressed all the way
to the GPU, and its CPU decompressor is the fallback for contexts where the
hardware cannot. Both paths are ported literally, including the block
arithmetic, the DXT1 `c0 > c1` colour rule, the DXT3 4-bit alpha nibbles and
the DXT5 interpolated alpha table.

Runtime boundaries:

  - The identity map and the texture-object failure sentinel follow the same
    two adaptations documented in {@link WebGLRGBImageUncompressedRenderer}.
  - Java asks `gl.isExtensionAvailable` for the S3TC names; WebGL exposes the
    same functionality, and the very same enum values, through
    `getExtension("WEBGL_compressed_texture_s3tc")`, whose returned object is
    also where the enums live. Requesting the extension is what enables it, so
    availability and the enum lookup are one step here instead of two.
  - Java calls `glGenerateMipmap` on the compressed texture. WebGL forbids
    mipmap generation for compressed internal formats, and only the top level
    is present in the file, so the compressed path uploads level 0 and uses a
    non-mipmapped minification filter. The CPU-decoded fallback path is
    uncompressed and keeps Java's `glGenerateMipmap` plus its filter choice.
  - Java's decompressor builds the DXT5 alpha index from a 48-bit `long`
    shifted by up to 45 bits. Numbers carry only 32 bits through JavaScript
    bitwise operators, so those six bytes are read and shifted as a `BigInt`,
    which reproduces the Java arithmetic exactly.
*/
export class WebGLRGBAImageCompressedRenderer {
    private static readonly compiledImages = new WeakMap<
        WebGL2RenderingContext,
        WeakMap<RGBAImageCompressed, WebGLTexture>
    >();

    public static activate(gl: WebGL2RenderingContext, img: RGBAImageCompressed | null): WebGLTexture | null {
        if (img === null) {
            return null;
        }

        const compiledImages = WebGLRGBAImageCompressedRenderer.getCompiledImages(gl);
        let texture = compiledImages.get(img);
        if (texture === undefined) {
            const uploaded = WebGLRGBAImageCompressedRenderer.upload(gl, img);
            if (uploaded === null) {
                return null;
            }
            texture = uploaded;
            compiledImages.set(img, texture);
        }

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);
        return texture;
    }

    public static deactivate(gl: WebGL2RenderingContext, img: RGBAImageCompressed | null): void {
        if (img !== null && WebGLRGBAImageCompressedRenderer.getCompiledImages(gl).has(img)) {
            gl.bindTexture(gl.TEXTURE_2D, null);
        }
    }

    public static unload(gl: WebGL2RenderingContext, img: RGBAImageCompressed | null): void {
        if (img === null) {
            return;
        }
        const compiledImages = WebGLRGBAImageCompressedRenderer.getCompiledImages(gl);
        const texture = compiledImages.get(img);
        if (texture === undefined) {
            return;
        }
        compiledImages.delete(img);

        gl.deleteTexture(texture);
    }

    public static async draw(gl: WebGL2RenderingContext, img: RGBAImageCompressed): Promise<void> {
        const texture = WebGLRGBAImageCompressedRenderer.activate(gl, img);
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

    private static upload(gl: WebGL2RenderingContext, img: RGBAImageCompressed): WebGLTexture | null {
        const imageSize = img.getCompressedDataSize();
        if (imageSize <= 0) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "WebGLRGBAImageCompressedRenderer.upload",
                "Invalid compressed texture size.",
            );
            return null;
        }

        const s3tc = gl.getExtension("WEBGL_compressed_texture_s3tc") as S3tcExtension | null;
        const s3tcAvailable = s3tc !== null;

        const internalFormat = WebGLRGBAImageCompressedRenderer.toOpenGlInternalFormat(
            s3tc,
            img.getCompressionFormat(),
        );

        const texture = gl.createTexture();
        if (texture === null) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "WebGLRGBAImageCompressedRenderer.upload",
                "Could not create texture object.",
            );
            return null;
        }

        gl.activeTexture(gl.TEXTURE0);
        gl.bindTexture(gl.TEXTURE_2D, texture);
        gl.pixelStorei(gl.UNPACK_ALIGNMENT, 1);

        if (s3tcAvailable && internalFormat !== 0) {
            const compressedData = img.getRawImageDirectBuffer();
            if (compressedData === null) {
                gl.deleteTexture(texture);
                gl.bindTexture(gl.TEXTURE_2D, null);
                return null;
            }
            gl.compressedTexImage2D(
                gl.TEXTURE_2D,
                0,
                internalFormat,
                img.getXSize(),
                img.getYSize(),
                0,
                compressedData.subarray(0, imageSize),
            );
            // Mipmap generation is unavailable for compressed formats in
            // WebGL, so only the file's top level exists.
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, WebGLImageRenderer.magFilterParam(gl));
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, WebGLImageRenderer.minFilterParamWithoutMipmaps(gl));
        } else {
            // S3TC not available (e.g. a context without the extension) —
            // decode in CPU
            Logger.reportMessage(
                null,
                VSDK.WARNING,
                "WebGLRGBAImageCompressedRenderer.upload",
                "S3TC extension not available; decoding compressed texture in CPU.",
            );
            const rgba = WebGLRGBAImageCompressedRenderer.decompressToRGBA(img);
            if (rgba === null) {
                gl.deleteTexture(texture);
                gl.bindTexture(gl.TEXTURE_2D, null);
                return null;
            }
            gl.texImage2D(
                gl.TEXTURE_2D,
                0,
                gl.RGBA8,
                img.getXSize(),
                img.getYSize(),
                0,
                gl.RGBA,
                gl.UNSIGNED_BYTE,
                rgba,
            );
            gl.generateMipmap(gl.TEXTURE_2D);

            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, WebGLImageRenderer.magFilterParam(gl));
            gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, WebGLImageRenderer.minFilterParam(gl));
        }

        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.REPEAT);

        gl.bindTexture(gl.TEXTURE_2D, null);

        return texture;
    }

    /**
    Decodes a DXT1/DXT3/DXT5 compressed image to a flat RGBA byte array
    (top-left origin, row-major). Returns null on unsupported format.
    */
    private static decompressToRGBA(img: RGBAImageCompressed): Uint8Array | null {
        const fmt = img.getCompressionFormat();
        if (
            fmt !== RGBAImageCompressed.COMPRESSION_DXT1 &&
            fmt !== RGBAImageCompressed.COMPRESSION_DXT3 &&
            fmt !== RGBAImageCompressed.COMPRESSION_DXT5
        ) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "WebGLRGBAImageCompressedRenderer.decompressToRGBA",
                "Cannot decompress format: " + fmt,
            );
            return null;
        }

        const width = img.getXSize();
        const height = img.getYSize();
        const src = img.getRawImage();
        if (src === null) {
            return null;
        }
        const rgba = new Uint8Array(width * height * 4);

        const blockSize = fmt === RGBAImageCompressed.COMPRESSION_DXT1 ? 8 : 16;
        const blockCountX = Math.max(1, Math.trunc((width + 3) / 4));
        const blockCountY = Math.max(1, Math.trunc((height + 3) / 4));
        let srcOffset = 0;

        for (let by = 0; by < blockCountY; by++) {
            for (let bx = 0; bx < blockCountX; bx++) {
                const alphaOffset = srcOffset;
                const colorOffset = fmt === RGBAImageCompressed.COMPRESSION_DXT1 ? srcOffset : srcOffset + 8;

                // Decode the 4x4 colour block (DXT1 core)
                const c0 = WebGLRGBAImageCompressedRenderer.readUShort(src, colorOffset);
                const c1 = WebGLRGBAImageCompressedRenderer.readUShort(src, colorOffset + 2);
                const lookup = WebGLRGBAImageCompressedRenderer.readInt(src, colorOffset + 4);

                const cr = new Int32Array(4);
                const cg = new Int32Array(4);
                const cb = new Int32Array(4);
                const ca = new Int32Array(4);
                WebGLRGBAImageCompressedRenderer.decodeRgb565(c0, cr, cg, cb, 0);
                WebGLRGBAImageCompressedRenderer.decodeRgb565(c1, cr, cg, cb, 1);
                ca[0] = 255;
                ca[1] = 255;
                ca[2] = 255;
                ca[3] = 255;

                if (fmt === RGBAImageCompressed.COMPRESSION_DXT1) {
                    if (c0 > c1) {
                        cr[2] = Math.trunc((2 * cr[0]! + cr[1]!) / 3);
                        cg[2] = Math.trunc((2 * cg[0]! + cg[1]!) / 3);
                        cb[2] = Math.trunc((2 * cb[0]! + cb[1]!) / 3);
                        cr[3] = Math.trunc((cr[0]! + 2 * cr[1]!) / 3);
                        cg[3] = Math.trunc((cg[0]! + 2 * cg[1]!) / 3);
                        cb[3] = Math.trunc((cb[0]! + 2 * cb[1]!) / 3);
                    } else {
                        cr[2] = Math.trunc((cr[0]! + cr[1]!) / 2);
                        cg[2] = Math.trunc((cg[0]! + cg[1]!) / 2);
                        cb[2] = Math.trunc((cb[0]! + cb[1]!) / 2);
                        cr[3] = 0;
                        cg[3] = 0;
                        cb[3] = 0;
                        ca[3] = 0;
                    }
                } else {
                    cr[2] = Math.trunc((2 * cr[0]! + cr[1]!) / 3);
                    cg[2] = Math.trunc((2 * cg[0]! + cg[1]!) / 3);
                    cb[2] = Math.trunc((2 * cb[0]! + cb[1]!) / 3);
                    cr[3] = Math.trunc((cr[0]! + 2 * cr[1]!) / 3);
                    cg[3] = Math.trunc((cg[0]! + 2 * cg[1]!) / 3);
                    cb[3] = Math.trunc((cb[0]! + 2 * cb[1]!) / 3);
                }

                for (let py = 0; py < 4; py++) {
                    for (let px = 0; px < 4; px++) {
                        const pixX = bx * 4 + px;
                        const pixY = by * 4 + py;
                        if (pixX >= width || pixY >= height) {
                            continue;
                        }
                        const idx = (lookup >> (2 * (py * 4 + px))) & 0x3;
                        const dstBase = (pixY * width + pixX) * 4;

                        rgba[dstBase + 0] = cr[idx]! & 0xff;
                        rgba[dstBase + 1] = cg[idx]! & 0xff;
                        rgba[dstBase + 2] = cb[idx]! & 0xff;

                        if (fmt === RGBAImageCompressed.COMPRESSION_DXT1) {
                            rgba[dstBase + 3] = ca[idx]! & 0xff;
                        } else if (fmt === RGBAImageCompressed.COMPRESSION_DXT3) {
                            // 4-bit alpha per pixel packed in first 8 bytes
                            const alphaShift = (py * 4 + px) * 4;
                            const alphaVal = (src[alphaOffset + Math.trunc(alphaShift / 8)]! >> (alphaShift % 8)) & 0xf;
                            rgba[dstBase + 3] = ((alphaVal << 4) | alphaVal) & 0xff;
                        } else {
                            // DXT5: interpolated alpha
                            const a0 = src[alphaOffset]! & 0xff;
                            const a1 = src[alphaOffset + 1]! & 0xff;
                            const aTable = WebGLRGBAImageCompressedRenderer.buildAlphaTable(a0, a1);
                            const alphaBits = WebGLRGBAImageCompressedRenderer.readAlphaBits(src, alphaOffset + 2);
                            const aIdx = Number((alphaBits >> BigInt(3 * (py * 4 + px))) & 0x7n);
                            rgba[dstBase + 3] = aTable[aIdx]! & 0xff;
                        }
                    }
                }

                srcOffset += blockSize;
            }
        }

        return rgba;
    }

    private static readUShort(data: Int8Array, offset: number): number {
        return (data[offset]! & 0xff) | ((data[offset + 1]! & 0xff) << 8);
    }

    private static readInt(data: Int8Array, offset: number): number {
        return (
            (data[offset]! & 0xff) |
            ((data[offset + 1]! & 0xff) << 8) |
            ((data[offset + 2]! & 0xff) << 16) |
            ((data[offset + 3]! & 0xff) << 24)
        );
    }

    private static readAlphaBits(data: Int8Array, offset: number): bigint {
        let v = 0n;
        for (let i = 0; i < 6; i++) {
            v |= BigInt(data[offset + i]! & 0xff) << BigInt(8 * i);
        }
        return v;
    }

    private static decodeRgb565(packed: number, r: Int32Array, g: Int32Array, b: Int32Array, idx: number): void {
        r[idx] = Math.trunc((((packed >> 11) & 0x1f) * 255) / 31);
        g[idx] = Math.trunc((((packed >> 5) & 0x3f) * 255) / 63);
        b[idx] = Math.trunc(((packed & 0x1f) * 255) / 31);
    }

    private static buildAlphaTable(a0: number, a1: number): Int32Array {
        const t = new Int32Array(8);
        t[0] = a0;
        t[1] = a1;
        if (a0 > a1) {
            t[2] = Math.trunc((6 * a0 + 1 * a1) / 7);
            t[3] = Math.trunc((5 * a0 + 2 * a1) / 7);
            t[4] = Math.trunc((4 * a0 + 3 * a1) / 7);
            t[5] = Math.trunc((3 * a0 + 4 * a1) / 7);
            t[6] = Math.trunc((2 * a0 + 5 * a1) / 7);
            t[7] = Math.trunc((1 * a0 + 6 * a1) / 7);
        } else {
            t[2] = Math.trunc((4 * a0 + 1 * a1) / 5);
            t[3] = Math.trunc((3 * a0 + 2 * a1) / 5);
            t[4] = Math.trunc((2 * a0 + 3 * a1) / 5);
            t[5] = Math.trunc((1 * a0 + 4 * a1) / 5);
            t[6] = 0;
            t[7] = 255;
        }
        return t;
    }

    private static toOpenGlInternalFormat(s3tc: S3tcExtension | null, compressionFormat: number): number {
        if (s3tc === null) {
            return 0;
        }
        if (compressionFormat === RGBAImageCompressed.COMPRESSION_DXT1) {
            return s3tc.COMPRESSED_RGBA_S3TC_DXT1_EXT;
        }
        if (compressionFormat === RGBAImageCompressed.COMPRESSION_DXT3) {
            return s3tc.COMPRESSED_RGBA_S3TC_DXT3_EXT;
        }
        if (compressionFormat === RGBAImageCompressed.COMPRESSION_DXT5) {
            return s3tc.COMPRESSED_RGBA_S3TC_DXT5_EXT;
        }
        return 0;
    }

    private static getCompiledImages(gl: WebGL2RenderingContext): WeakMap<RGBAImageCompressed, WebGLTexture> {
        let compiledImages = WebGLRGBAImageCompressedRenderer.compiledImages.get(gl);
        if (compiledImages === undefined) {
            compiledImages = new WeakMap<RGBAImageCompressed, WebGLTexture>();
            WebGLRGBAImageCompressedRenderer.compiledImages.set(gl, compiledImages);
        }
        return compiledImages;
    }
}
