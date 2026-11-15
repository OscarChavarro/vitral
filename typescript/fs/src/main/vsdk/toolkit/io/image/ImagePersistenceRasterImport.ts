/**
Deep module specifiers rather than the `@vitral/base` barrel, as in
`ReaderMitScene`: this module is loaded inside every worker thread of a
parallel render.
*/
import { RGBAImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBAImageUncompressed";
import { RGBImageUncompressed } from "@vitral/base/vsdk/toolkit/media/RGBImageUncompressed";
import { FileInputStream } from "../../../../java/io/FileInputStream.js";
import { ImagePersistenceJpegReader } from "./ImagePersistenceJpegReader.js";
import { ImagePersistencePngReader } from "./ImagePersistencePngReader.js";

/**
Raster image import of the cpp `ImagePersistence::importRGB` and
`ImagePersistence::importRGBA`, from local files: the file name extension
picks the format (JPEG, PNG or binary PPM). As in cpp, `importRGB` answers an
empty image for files it cannot read, and `importRGBA` answers null for
unreadable PNG files and the opaque version of `importRGB` otherwise.
*/
export class ImagePersistenceRasterImport {
    // cpp `ImagePersistence::extractExtensionFromFile`, lowercased
    private static extension(path: string): string {
        const slash = Math.max(path.lastIndexOf("/"), path.lastIndexOf("\\"));
        const name = path.substring(slash + 1);
        const dot = name.lastIndexOf(".");
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private static readFile(path: string): Uint8Array | null {
        try {
            const stream = new FileInputStream(path);
            try {
                return stream.readAllBytes();
            } finally {
                stream.close();
            }
        } catch {
            return null;
        }
    }

    public static importRGB(path: string): RGBImageUncompressed {
        const type = ImagePersistenceRasterImport.extension(path);
        if (type === "jpg" || type === "jpeg") {
            const data = ImagePersistenceRasterImport.readFile(path);
            if (data === null) {
                console.error("Cannot open JPEG file: " + path);
                return new RGBImageUncompressed();
            }
            const decoded = ImagePersistenceJpegReader.decode(data);
            if (decoded === null) return new RGBImageUncompressed();
            const image = new RGBImageUncompressed();
            if (!image.initNoFill(decoded.width, decoded.height)) {
                console.error("Failed to allocate image memory for JPEG");
                return new RGBImageUncompressed();
            }
            let pos = 0;
            for (let y = 0; y < decoded.height; y++) {
                for (let x = 0; x < decoded.width; x++) {
                    image.putPixel(x, y, decoded.pixels[pos]!, decoded.pixels[pos + 1]!, decoded.pixels[pos + 2]!);
                    pos += 3;
                }
            }
            return image;
        }
        if (type === "png") {
            const data = ImagePersistenceRasterImport.readFile(path);
            const decoded = data === null ? null : ImagePersistencePngReader.decode(data);
            if (decoded === null) return new RGBImageUncompressed();
            const image = new RGBImageUncompressed();
            if (!image.initNoFill(decoded.width, decoded.height)) return new RGBImageUncompressed();
            // png_set_strip_alpha: the color of the samples, without compositing
            let pos = 0;
            for (let y = 0; y < decoded.height; y++) {
                for (let x = 0; x < decoded.width; x++) {
                    image.putPixel(x, y, decoded.pixels[pos]!, decoded.pixels[pos + 1]!, decoded.pixels[pos + 2]!);
                    pos += 4;
                }
            }
            return image;
        }
        if (type === "ppm") {
            return ImagePersistenceRasterImport.importPPM(path);
        }
        return new RGBImageUncompressed();
    }

    public static importRGBA(path: string): RGBAImageUncompressed | null {
        if (ImagePersistenceRasterImport.extension(path) === "png") {
            const data = ImagePersistenceRasterImport.readFile(path);
            if (data === null) {
                console.error("Cannot open PNG file: " + path);
                return null;
            }
            const decoded = ImagePersistencePngReader.decode(data);
            if (decoded === null) return null;
            const image = new RGBAImageUncompressed();
            if (!image.init(decoded.width, decoded.height)) return null;
            let pos = 0;
            for (let y = 0; y < decoded.height; y++) {
                for (let x = 0; x < decoded.width; x++) {
                    const p = decoded.pixels;
                    image.putPixel(x, y, p[pos]!, p[pos + 1]!, p[pos + 2]!, p[pos + 3]!);
                    pos += 4;
                }
            }
            return image;
        }

        // Formats without transparency: an opaque image
        const rgb = ImagePersistenceRasterImport.importRGB(path);
        const image = new RGBAImageUncompressed();
        image.init(rgb.getXSize(), rgb.getYSize());
        for (let y = 0; y < rgb.getYSize(); y++) {
            for (let x = 0; x < rgb.getXSize(); x++) {
                const pixel = rgb.getPixelRgb(x, y);
                image.putPixel(x, y, pixel.r, pixel.g, pixel.b, 255);
            }
        }
        return image;
    }

    // cpp `ImagePersistence::isTextComment`
    private static isTextComment(line: string): boolean {
        const trimmed = line.replace(/^[ \t]+/, "");
        return trimmed.length > 0 && trimmed.charAt(0) === "#";
    }

    // The binary PPM (P6) branch of the cpp `ImagePersistence::importRGB`
    private static importPPM(path: string): RGBImageUncompressed {
        const data = ImagePersistenceRasterImport.readFile(path);
        if (data === null) return new RGBImageUncompressed();
        let position = 0;
        const readLine = (): string | null => {
            if (position >= data.length) return null;
            let end = position;
            while (end < data.length && data[end] !== 0x0a) end++;
            let line = "";
            for (let i = position; i < end; i++) {
                if (data[i] !== 0x0d) line += String.fromCharCode(data[i]!);
            }
            position = end + 1;
            return line;
        };

        let exit = false;
        let stage = 1;
        let xSize = 0;
        let ySize = 0;
        do {
            const line = readLine();
            if (line === null) break;
            if (line === "255") exit = true;
            if (ImagePersistenceRasterImport.isTextComment(line)) continue;
            if (stage === 1) {
                if (!line.startsWith("P6")) {
                    console.error("Error reading internal PPM file subformat");
                    return new RGBImageUncompressed();
                }
                stage++;
            } else if (stage === 2) {
                if (line.charAt(0) !== "#") {
                    const tokens = line.split(/[ \t]+/).filter((token) => token.length > 0);
                    if (tokens.length > 0) xSize = parseInt(tokens[0]!, 10) || 0;
                    if (tokens.length > 1) ySize = parseInt(tokens[1]!, 10) || 0;
                    stage++;
                }
            }
        } while (!exit);

        if (xSize <= 0 || ySize <= 0) {
            console.error("Invalid image dimensions");
            return new RGBImageUncompressed();
        }
        const image = new RGBImageUncompressed();
        if (!image.initNoFill(xSize, ySize)) {
            console.error("Failed to allocate image memory");
            return new RGBImageUncompressed();
        }
        for (let i = 0; i < ySize; i++) {
            for (let x = 0; x < xSize; x++) {
                const p = position + x * 3;
                image.putPixel(x, i, data[p] ?? 0, data[p + 1] ?? 0, data[p + 2] ?? 0);
            }
            position += xSize * 3;
        }

        // Invert image (flip vertically), as cpp does
        for (let y = 0; y < Math.floor(ySize / 2); y++) {
            for (let x = 0; x < xSize; x++) {
                const a = image.getPixelRgb(x, y);
                const b = image.getPixelRgb(x, ySize - y - 1);
                image.putPixelRgb(x, y, b);
                image.putPixelRgb(x, ySize - y - 1, a);
            }
        }
        return image;
    }
}
