import { RGBAImageUncompressed, RGBImageUncompressed, type Image } from "@vitral/base";

/**
Counterpart of `vsdk.toolkit.render.awt.AwtRGBAImageUncompressedRenderer` and
`AwtRGBImageUncompressedRenderer` for the DOM: where AWT exports a VSDK image
to a `BufferedImage` to present it in a component, a page draws it into a
`<canvas>` element.

Rows are exported top first, as `Image.getPixel(x, y)` numbers them (AWT's
`BufferedImage` rows are top first too).
*/
export class HtmlImageRenderer {
    private constructor() {}

    /**
    @param img image to export
    @param canvas canvas to draw into (resized to the image); a new one if not
    given
    @return the canvas with the image
    */
    public static exportToCanvas(img: Image, canvas: HTMLCanvasElement = document.createElement("canvas")): HTMLCanvasElement {
        const width: number = img.getXSize();
        const height: number = img.getYSize();

        canvas.width = Math.max(1, width);
        canvas.height = Math.max(1, height);
        const context: CanvasRenderingContext2D | null = canvas.getContext("2d");
        if (context === null || width <= 0 || height <= 0) {
            return canvas;
        }
        context.putImageData(HtmlImageRenderer.exportToImageData(img), 0, 0);
        return canvas;
    }

    /**
    @param img image to export (RGBA images keep their transparency, any other
    image is opaque)
    @return its pixels, top row first
    */
    public static exportToImageData(img: Image): ImageData {
        const width: number = img.getXSize();
        const height: number = img.getYSize();
        const data: ImageData = new ImageData(Math.max(1, width), Math.max(1, height));
        const out: Uint8ClampedArray = data.data;

        if (img instanceof RGBAImageUncompressed) {
            const raw: Uint8Array = img.getRawImageDirectBuffer();
            for (let y: number = 0; y < height; y++) {
                // Rows of the raw buffer are stored bottom first
                const source: number = (height - 1 - y) * width * 4;
                out.set(raw.subarray(source, source + width * 4), y * width * 4);
            }
        }
        else if (img instanceof RGBImageUncompressed) {
            for (let y: number = 0; y < height; y++) {
                for (let x: number = 0; x < width; x++) {
                    const p = img.getPixel(x, y);
                    const i: number = (y * width + x) * 4;
                    out[i] = p.r & 0xff;
                    out[i + 1] = p.g & 0xff;
                    out[i + 2] = p.b & 0xff;
                    out[i + 3] = 255;
                }
            }
        }
        else {
            for (let y: number = 0; y < height; y++) {
                for (let x: number = 0; x < width; x++) {
                    const p = img.getPixelRgb(x, y);
                    const i: number = (y * width + x) * 4;
                    out[i] = p.r & 0xff;
                    out[i + 1] = p.g & 0xff;
                    out[i + 2] = p.b & 0xff;
                    out[i + 3] = 255;
                }
            }
        }
        return data;
    }

    /**
    @param img image to export
    @return a `data:` URL with the image as a PNG, to use as the source of an
    `<img>` element or a CSS cursor
    */
    public static exportToDataUrl(img: Image): string {
        return HtmlImageRenderer.exportToCanvas(img).toDataURL("image/png");
    }
}
