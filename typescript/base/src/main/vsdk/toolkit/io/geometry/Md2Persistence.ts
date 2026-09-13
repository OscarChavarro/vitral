import { PersistenceElement } from "../PersistenceElement.js";
import type { Md2Mesh } from "../../environment/geometry/surface/Md2Mesh.js";
import type { Image } from "../../media/Image.js";
import { RGBImageUncompressed } from "../../media/RGBImageUncompressed.js";
import { ImageProcessing } from "../../processing/ImageProcessing.js";

/**
Port of `vsdk.toolkit.io.geometry.Md2Persistence`.

The Quake II MD2 reader: a header of sixteen little-endian 32-bit words, then
the skin names, the texture coordinates, the triangle list, one vertex block
per animation frame and finally the OpenGL command list, each at the byte
offset the header names.

Two boundaries, both of them the same one the `.obj` reader crosses. Java opens
a `RandomAccessFile` and walks it with `seek` and `read`; a browser has no file
system, and the resource it names is fetched whole before anything is parsed,
so what this class takes is the bytes rather than a file name and the cursor
below plays the part of the file pointer — on a local file the two are the same
thing, since `RandomAccessFile` is random access over exactly those bytes. And
Java's `loadImagefile` reads the skin through `ImagePersistence.importRGB(File)`,
which is platform-bound in both editions: the caller decodes the skin and hands
it over, which is what `WebMd2Persistence` in `@vitral/webgl` and a Node caller
each do with their own importer. Both halves of Java's `loadImagefile` go with
it — the null answer for an unnamed skin and the 64x64 test pattern for one that
could not be read — since only the caller can tell those two apart.

One detail of the arithmetic is spelled out rather than inherited. Java
evaluates `coord * scale[k] + translate[k]` and `coord / skinWidth` in `float`,
rounding after every operation; TypeScript has only `number`, which is a
`double`, and a `Float32Array` store rounds once at the end. `Math.fround` puts
the intermediate rounding back, without which the frame vertices differ from
Java's in their last bits — which the reference-driver diff for this reader
measures.

Everything between those two boundaries — the unsigned-short masking, the
`triangles[i * 2]` / `triangles[i * 2 + 1]` interleaving of vertex and texture
indices, the per-frame scale and translate, the sign of the OpenGL command
count telling a strip from a fan, and the gamma correction of the second `read`
overload — is the Java one.
*/
export class Md2Persistence extends PersistenceElement {
    private static readonly MD2_IDENT = 844121161;
    private static readonly MD2_VERSION = 8;

    /**
    Returns true if loaded successfully.

    @param inFileContent the bytes of the MD2 resource, which Java reads
    through a `RandomAccessFile`
    @param inTexture the already decoded skin, which Java reads itself through
    `ImagePersistence.importRGB`; null asks for Java's test-pattern fallback
    @param outMd2Mesh the mesh to fill
    */
    public read(inFileContent: Uint8Array, inTexture: Image | null, outMd2Mesh: Md2Mesh): boolean;
    public read(
        inFileContent: Uint8Array,
        inTexture: Image | null,
        outMd2Mesh: Md2Mesh,
        gammaCorrection: number,
    ): boolean;
    public read(
        inFileContent: Uint8Array,
        inTexture: Image | null,
        outMd2Mesh: Md2Mesh,
        gammaCorrection?: number,
    ): boolean {
        if (gammaCorrection !== undefined) {
            const ok: boolean = this.read(inFileContent, inTexture, outMd2Mesh);
            if (!ok) {
                return false;
            }
            if (
                outMd2Mesh.skins !== null &&
                outMd2Mesh.skins.length > 0 &&
                outMd2Mesh.skins[0] instanceof RGBImageUncompressed
            ) {
                ImageProcessing.gammaCorrection(outMd2Mesh.skins[0] as RGBImageUncompressed, gammaCorrection);
            }
            return true;
        }

        let i: number;
        let j: number;
        let k: number;
        let nVertPrimitive: number;
        let name: string;
        let triStrip: boolean;

        const file: _Md2ByteCursor = new _Md2ByteCursor(inFileContent);

        // Read the header.
        if (!this.readHeader(file, outMd2Mesh)) return false; // The file is not a md2 file version 8
        // Read the full paths of the skins.
        file.seek(outMd2Mesh.offsetSkins);
        for (i = 0; i < outMd2Mesh.numSkins; ++i) {
            name = file.readAsciiString(64);
            name = Md2Persistence.trim(name);
            outMd2Mesh.skinNames.push(name);
        }
        // Read texture coordinates.
        outMd2Mesh.texCoords = new Float32Array(outMd2Mesh.numTexCoords * 2);
        file.seek(outMd2Mesh.offsetTexCoords);
        for (i = 0; i < outMd2Mesh.numTexCoords; ++i) {
            let coord: number;

            coord = file.readSignedShortLE();
            outMd2Mesh.texCoords[i * 2] = Math.fround(coord / outMd2Mesh.skinWidth);
            coord = file.readSignedShortLE();
            outMd2Mesh.texCoords[i * 2 + 1] = Math.fround(coord / outMd2Mesh.skinHeight);
        }
        // Read triangles
        // Each triangle has three vertex indices and three tex. coord. indices.
        outMd2Mesh.triangles = [];
        for (i = 0; i < outMd2Mesh.numTriangles * 2; ++i) {
            outMd2Mesh.triangles.push([0, 0, 0]);
        }
        file.seek(outMd2Mesh.offsetTriangles);
        for (i = 0; i < outMd2Mesh.numTriangles; ++i) {
            let coord: number;

            for (j = 0; j < 3; ++j) {
                // This is because the number in the file is an unsigned short.
                coord = file.readSignedShortLE() & 0xffff;
                outMd2Mesh.triangles[i * 2]![j] = coord;
            }
            for (j = 0; j < 3; ++j) {
                coord = file.readSignedShortLE() & 0xffff;
                outMd2Mesh.triangles[i * 2 + 1]![j] = coord;
            }
        }
        // Read frames.
        const scale: Float32Array = new Float32Array(3);
        const translate: Float32Array = new Float32Array(3);

        file.seek(outMd2Mesh.offsetFrames);
        for (i = 0; i < outMd2Mesh.numFrames; ++i) {
            let frameVertices: Float32Array;
            let frameNormalIndices: Int16Array;
            let coord: number;
            let normalIndex: number;

            for (j = 0; j < 3; ++j) {
                scale[j] = file.readFloatLE();
            }
            for (j = 0; j < 3; ++j) {
                translate[j] = file.readFloatLE();
            }
            name = file.readAsciiString(16);
            name = Md2Persistence.trim(name);
            outMd2Mesh.frameNames.push(name);
            outMd2Mesh.frameVertices.push(new Float32Array(outMd2Mesh.numVertices * 3));
            outMd2Mesh.frameNormalIndices.push(new Int16Array(outMd2Mesh.numVertices));
            frameVertices = outMd2Mesh.frameVertices[i]!;
            frameNormalIndices = outMd2Mesh.frameNormalIndices[i]!;
            for (j = 0; j < outMd2Mesh.numVertices; ++j) {
                for (k = 0; k < 3; ++k) {
                    coord = file.readUnsignedByte(); //The number in the data is unsigned.
                    frameVertices[j * 3 + k] = Math.fround(Math.fround(coord * scale[k]!) + translate[k]!);
                }
                normalIndex = file.readUnsignedByte(); //The number in the data is unsigned.
                frameNormalIndices[j] = normalIndex;
            }
        }
        // Read OpenGL commands.
        file.seek(outMd2Mesh.offsetGlCommands);
        nVertPrimitive = file.readSignedIntLE();
        triStrip = true;
        if (nVertPrimitive < 0) {
            triStrip = false;
            nVertPrimitive = -nVertPrimitive;
        }
        while (nVertPrimitive !== 0) {
            let texCoords: Float32Array;
            let vertIndices: Int32Array;

            if (triStrip) {
                texCoords = new Float32Array(nVertPrimitive * 2);
                outMd2Mesh.glCmdTexCoordsStrip.push(texCoords);
                vertIndices = new Int32Array(nVertPrimitive);
                outMd2Mesh.glCmdVertIndexStrip.push(vertIndices);
            } else {
                texCoords = new Float32Array(nVertPrimitive * 2);
                outMd2Mesh.glCmdTexCoordsFan.push(texCoords);
                vertIndices = new Int32Array(nVertPrimitive);
                outMd2Mesh.glCmdVertIndexFan.push(vertIndices);
            }
            for (i = 0; i < nVertPrimitive; ++i) {
                texCoords[i * 2] = file.readFloatLE();
                texCoords[i * 2 + 1] = file.readFloatLE();
                vertIndices[i] = file.readSignedIntLE();
            }
            nVertPrimitive = file.readSignedIntLE();
            triStrip = true;
            if (nVertPrimitive < 0) {
                triStrip = false;
                nVertPrimitive = -nVertPrimitive;
            }
        }

        if (outMd2Mesh.numSkins === 0) outMd2Mesh.skins = new Array<Image>(1);
        else outMd2Mesh.skins = new Array<Image>(outMd2Mesh.numSkins);
        // For now, only one image.
        // Java's `loadImagefile` answers null for an unnamed skin, and the
        // array keeps that hole; here the slot is simply left unwritten, which
        // is the same hole in an `Image[]` that cannot hold null.
        if (inTexture !== null) {
            outMd2Mesh.skins[0] = inTexture;
        }
        return true;
    }

    /**
    Returns false if the file is not a md2 file version 8.
    */
    public readHeader(file: _Md2ByteCursor, outMd2Mesh: Md2Mesh): boolean {
        outMd2Mesh.ident = file.readSignedIntLE();
        outMd2Mesh.version = file.readSignedIntLE();
        if (outMd2Mesh.ident !== Md2Persistence.MD2_IDENT || outMd2Mesh.version !== Md2Persistence.MD2_VERSION) {
            return false;
        }
        outMd2Mesh.skinWidth = file.readSignedIntLE();
        outMd2Mesh.skinHeight = file.readSignedIntLE();
        outMd2Mesh.frameSize = file.readSignedIntLE();
        outMd2Mesh.numSkins = file.readSignedIntLE();
        outMd2Mesh.numVertices = file.readSignedIntLE();
        outMd2Mesh.numTexCoords = file.readSignedIntLE();
        outMd2Mesh.numTriangles = file.readSignedIntLE();
        outMd2Mesh.numGlCommands = file.readSignedIntLE();
        outMd2Mesh.numFrames = file.readSignedIntLE();
        outMd2Mesh.offsetSkins = file.readSignedIntLE();
        outMd2Mesh.offsetTexCoords = file.readSignedIntLE();
        outMd2Mesh.offsetTriangles = file.readSignedIntLE();
        outMd2Mesh.offsetFrames = file.readSignedIntLE();
        outMd2Mesh.offsetGlCommands = file.readSignedIntLE();
        outMd2Mesh.offsetEnd = file.readSignedIntLE();
        return true;
    }

    /**
    Java's `String.trim()`, which strips every character at or below the space
    (U+0020) from both ends. The MD2 name fields are NUL padded, and that is
    the padding Java's two `trim()` calls remove; JavaScript's own `trim`
    strips Unicode whitespace, which does not include NUL, so it is spelled out
    here rather than borrowed.
    */
    private static trim(value: string): string {
        let start = 0;
        let end: number = value.length;
        while (start < end && value.charCodeAt(start) <= 0x20) {
            start++;
        }
        while (end > start && value.charCodeAt(end - 1) <= 0x20) {
            end--;
        }
        return value.substring(start, end);
    }
}

/**
The byte cursor that stands in for Java's `RandomAccessFile`: the same `seek`
and the same forward reads, over a buffer that has already been fetched.
*/
export class _Md2ByteCursor {
    private position = 0;

    public constructor(private readonly bytes: Uint8Array) {}

    public seek(position: number): void {
        this.position = position;
    }

    public readUnsignedByte(): number {
        return this.bytes[this.position++]! & 0xff;
    }

    public readSignedShortLE(): number {
        const value: number = PersistenceElement.byteArray2signedShortLE(this.bytes, this.position);
        this.position += 2;
        return value;
    }

    /**
    Java reads four bytes into `byteArray2longLE` and narrows the result with
    an `(int)` cast, which is what makes a negative OpenGL command count
    negative. `| 0` is that cast.
    */
    public readSignedIntLE(): number {
        const value: number = PersistenceElement.byteArray2longLE(this.bytes, this.position) | 0;
        this.position += 4;
        return value;
    }

    public readFloatLE(): number {
        const value: number = PersistenceElement.byteArray2floatLE(this.bytes, this.position);
        this.position += 4;
        return value;
    }

    /**
    Java's `readFully(byte[]) ` plus `new String(bytes, "US-ASCII")`. A name
    field is zero padded, and Java's `String` keeps those NUL characters, which
    is why both call sites `trim()` afterwards; `String.fromCharCode` keeps
    them too, so the two agree.
    */
    public readAsciiString(length: number): string {
        let out = "";
        for (let i = 0; i < length; i++) {
            out += String.fromCharCode(this.bytes[this.position + i]! & 0xff);
        }
        this.position += length;
        return out;
    }
}
