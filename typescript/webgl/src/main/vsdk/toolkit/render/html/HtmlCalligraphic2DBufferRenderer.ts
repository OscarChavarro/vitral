import { VSDK, type Calligraphic2DBuffer, type Vector3Dd } from "@vitral/base";

/**
Counterpart of `vsdk.toolkit.render.awt.AwtCalligraphic2DBufferRenderer`
for the DOM: draws the lines of a `Calligraphic2DBuffer` (normalized device
coordinates, from <-1, -1> at the lower left corner to <1, 1> at the upper
right corner) in black, with the 2D operations of a canvas
(`CanvasRenderingContext2D`), inside the area of `dx` x `dy` pixels whose
upper left corner is <`xx0`, `yy0`>. The mapping to pixels is the one of the
Java class, truncated to whole pixels.

The cpp and turboc ports do the same with `XDrawSegments` in
`XlibCalligraphic2DBufferRenderer`.
*/
export class HtmlCalligraphic2DBufferRenderer {
    private constructor() {}

    /**
    @param g 2D context of the canvas where to draw
    @param lineSet lines to draw, in normalized device coordinates
    @param xx0 left pixel of the area
    @param yy0 top pixel of the area
    @param dx width of the area, in pixels
    @param dy height of the area, in pixels
    */
    public static draw(
        g: CanvasRenderingContext2D,
        lineSet: Calligraphic2DBuffer,
        xx0: number,
        yy0: number,
        dx: number,
        dy: number,
    ): void {
        g.strokeStyle =
            "rgb(" +
            VSDK.signedByte2unsignedInteger(0) +
            "," +
            VSDK.signedByte2unsignedInteger(0) +
            "," +
            VSDK.signedByte2unsignedInteger(0) +
            ")";

        let p0: Vector3Dd;
        let p1: Vector3Dd;
        let x0: number, y0: number, x1: number, y1: number;
        let i: number;
        const xt: number = dx;
        const yt: number = dy;

        // Java's `Graphics.drawLine` covers whole pixels: half a pixel
        // offset centers the 1 pixel wide canvas line on them
        g.lineWidth = 1;
        g.beginPath();
        for (i = 0; i < lineSet.getNumLines(); i++) {
            const segment: [Vector3Dd, Vector3Dd] = lineSet.get2DLine(i);
            p0 = segment[0];
            p1 = segment[1];
            x0 = Math.trunc((xt - 1) * ((p0.x() + 1) / 2));
            y0 = Math.trunc((yt - 1) * (1 - (p0.y() + 1) / 2));
            x1 = Math.trunc((xt - 1) * ((p1.x() + 1) / 2));
            y1 = Math.trunc((yt - 1) * (1 - (p1.y() + 1) / 2));
            g.moveTo(xx0 + x0 + 0.5, yy0 + y0 + 0.5);
            g.lineTo(xx0 + x1 + 0.5, yy0 + y1 + 0.5);
        }
        g.stroke();
    }
}
