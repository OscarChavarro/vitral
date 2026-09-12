import {
    ImageNotRecognizedException,
    ImagePersistence,
    Logger,
    RGBAImageCompressed,
    RGBAImageUncompressed,
    RGBImageUncompressed,
    RGBPixel,
    VSDK,
    type Image,
} from "@vitral/base";

/**
Browser-bound half of `vsdk.toolkit.io.image.ImagePersistence`, the counterpart
of the `java.io.File` importers.

A browser has no local file system, so the resource a browser program names is
a URL fetched over HTTP, and this class is to `@vitral/webgl` what
`ImagePersistence` in `@vitral/fs` is to Node. What it ports is Java's
`importRGB(File)` / `importRGBA(File)` dispatch: the file-name extension picks
the path, `dds` goes to the DXT reader that keeps the blocks compressed, and
anything else goes to the platform's own decoder.

The platform decoder is the boundary. Java reaches JPEG and PNG through
`javax.imageio`, wrapped by the AWT `ImagePersistenceHelper`
(`ImagePersistenceAwt`), which walks the decoded `BufferedImage` pixel by
pixel into a Vitral image; the browser's own image decoders are the exact
counterpart of that helper, reached through `createImageBitmap`, and the
pixel-by-pixel transfer below is the port of
`AwtRGBImageUncompressedRenderer.importFromAwtBufferedImage` and its RGBA
twin, including their top-left-origin traversal and their byte masking. No
third-party codec is introduced, in either package.

Since a fetch is asynchronous, both importers are asynchronous here.
*/
export class WebImagePersistence {
    /**
    Given the URL of an image resource, this method tries to recognize the
    format from the resource name and load its contents into an image.

    Java answers `RGBAImageCompressed` for a DXT-compressed DDS resource and
    `RGBImageUncompressed` for every other format, which is why the Java
    signature is generic over the returned image type.

    @param imageUrl The URL of the resource containing the image
    @return An Image entity that contains the image loaded in memory
    */
    public static async importRGB(imageUrl: string): Promise<Image> {
        const type = WebImagePersistence.extractExtensionFromUrl(imageUrl);

        if (type === "dds") {
            return WebImagePersistence.importDDSCompressed(imageUrl);
        }

        const bitmap = await WebImagePersistence.decodeWithPlatformDecoder(imageUrl);
        try {
            const retImage = new RGBImageUncompressed();
            WebImagePersistence.transferToRGB(bitmap, retImage, imageUrl);
            return retImage;
        } finally {
            bitmap.close();
        }
    }

    /**
    The alpha-preserving flavor of {@link importRGB}. As in Java, a DDS
    resource is read by the DXT reader and every other format goes to the
    platform decoder.

    @param imageUrl The URL of the resource containing the image
    @return An Image entity that contains the image loaded in memory
    */
    public static async importRGBA(imageUrl: string): Promise<Image> {
        const type = WebImagePersistence.extractExtensionFromUrl(imageUrl);

        if (type === "dds") {
            return WebImagePersistence.importDDSCompressed(imageUrl);
        }

        const bitmap = await WebImagePersistence.decodeWithPlatformDecoder(imageUrl);
        try {
            const retImage = new RGBAImageUncompressed();
            WebImagePersistence.transferToRGBA(bitmap, retImage, imageUrl);
            return retImage;
        } finally {
            bitmap.close();
        }
    }

    private static async importDDSCompressed(imageUrl: string): Promise<RGBAImageCompressed> {
        const fileData = await WebImagePersistence.readAllBytes(imageUrl);
        return ImagePersistence.importDDSCompressed(fileData, imageUrl);
    }

    private static async readAllBytes(imageUrl: string): Promise<Uint8Array> {
        const response = await fetch(imageUrl);
        if (!response.ok) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "WebImagePersistence.readAllBytes",
                'Cannot import image resource "' + imageUrl + '"',
            );
            throw new ImageNotRecognizedException("Error reading image resource: HTTP " + response.status, imageUrl);
        }
        return new Uint8Array(await response.arrayBuffer());
    }

    private static async decodeWithPlatformDecoder(imageUrl: string): Promise<ImageBitmap> {
        const response = await fetch(imageUrl);
        if (!response.ok) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "WebImagePersistence.importRGB",
                'Cannot import image resource "' + imageUrl + '"',
            );
            throw new ImageNotRecognizedException("Error reading image resource: HTTP " + response.status, imageUrl);
        }
        try {
            return await createImageBitmap(await response.blob());
        } catch (error) {
            Logger.reportMessage(
                null,
                VSDK.ERROR,
                "WebImagePersistence.importRGB",
                'Cannot import image resource "' + imageUrl + '"',
            );
            throw new ImageNotRecognizedException("Error reading image resource:\n" + String(error), imageUrl);
        }
    }

    /**
    The browser's decoders answer an opaque `ImageBitmap`, the counterpart of
    AWT's `BufferedImage`. Its samples are read the way
    `BufferedImage.getRGB` reads a raster: by drawing the bitmap once into a
    canvas and taking back the RGBA bytes of that canvas.
    */
    private static rasterOf(bitmap: ImageBitmap, imageUrl: string): Uint8ClampedArray {
        const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
        const context = canvas.getContext("2d", { willReadFrequently: true });
        if (context === null) {
            throw new ImageNotRecognizedException("No 2D context available to read image samples", imageUrl);
        }
        context.drawImage(bitmap, 0, 0);
        return context.getImageData(0, 0, bitmap.width, bitmap.height).data;
    }

    private static transferToRGB(bitmap: ImageBitmap, output: RGBImageUncompressed, imageUrl: string): void {
        const w = bitmap.width;
        const h = bitmap.height;
        const w2 = output.getXSize();
        const h2 = output.getYSize();

        if (w !== w2 || h !== h2) {
            if (!output.initNoFill(w, h)) {
                throw new ImageNotRecognizedException("Could not allocate image of " + w + "x" + h, imageUrl);
            }
        }

        const raster = WebImagePersistence.rasterOf(bitmap, imageUrl);
        let x: number;
        let y: number;
        let pixel: number;
        const p = new RGBPixel();

        for (y = 0; y < h; y++) {
            for (x = 0; x < w; x++) {
                pixel = (y * w + x) * 4;
                p.r = VSDK.unsigned8BitInteger2signedByte(raster[pixel]! & 0xff);
                p.g = VSDK.unsigned8BitInteger2signedByte(raster[pixel + 1]! & 0xff);
                p.b = VSDK.unsigned8BitInteger2signedByte(raster[pixel + 2]! & 0xff);
                output.putPixelRgb(x, y, p);
            }
        }
    }

    private static transferToRGBA(bitmap: ImageBitmap, output: RGBAImageUncompressed, imageUrl: string): void {
        const w = bitmap.width;
        const h = bitmap.height;
        const w2 = output.getXSize();
        const h2 = output.getYSize();

        if (w !== w2 || h !== h2) {
            if (!output.initNoFill(w, h)) {
                throw new ImageNotRecognizedException("Could not allocate image of " + w + "x" + h, imageUrl);
            }
        }

        const raster = WebImagePersistence.rasterOf(bitmap, imageUrl);
        let x: number;
        let y: number;
        let pixel: number;

        for (y = 0; y < h; y++) {
            for (x = 0; x < w; x++) {
                pixel = (y * w + x) * 4;
                output.putPixel(
                    x,
                    y,
                    VSDK.unsigned8BitInteger2signedByte(raster[pixel]! & 0xff),
                    VSDK.unsigned8BitInteger2signedByte(raster[pixel + 1]! & 0xff),
                    VSDK.unsigned8BitInteger2signedByte(raster[pixel + 2]! & 0xff),
                    VSDK.unsigned8BitInteger2signedByte(raster[pixel + 3]! & 0xff),
                );
            }
        }
    }

    /**
    Port of `PersistenceElement.extractExtensionFromFile`, which tokenizes the
    file name on `.` and answers the last token. The resource name is the last
    path segment of the URL, without its query or fragment, which is what
    `File.getName()` answers for a path.
    */
    private static extractExtensionFromUrl(imageUrl: string): string {
        const withoutQuery = imageUrl.split("?")[0]!.split("#")[0]!;
        const segments = withoutQuery.split("/");
        const filename = segments[segments.length - 1]!;
        const tokens = filename.split(".").filter((token) => token.length > 0);
        return tokens.length === 0 ? "" : tokens[tokens.length - 1]!;
    }
}
