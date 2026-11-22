import { Gizmo2DDrawList, type ColorRgb, type InputGizmo2DPainter } from "@vitral/base";

/**
Counterpart of `vsdk.toolkit.render.awt.AwtGizmo2DDrawListRenderer` for the
DOM: draws a `Gizmo2DDrawList` (the 2D presentation of the gizmos, computed
by the painters of `vsdk.toolkit.render.gizmo`) with the 2D operations of a
canvas (`CanvasRenderingContext2D`), so gizmos can be shown over a viewport
without any 3D API.

Opaque polygons coming from 3D elements are also outlined with a thin stroke
of their own color, so the seams antialiasing leaves between neighbor
polygons are not seen. Labels use the same font as the label images of
`WebSystem.calculateLabelImage`, so the layout of the input gizmo matches
the one of the WebGL presentation; `TEXT_MEASURER` measures them.
*/
export class HtmlGizmo2DDrawListRenderer {
    private static readonly FONT_NAME = "Arial, Helvetica, sans-serif";
    private static readonly SEAM_STROKE_WIDTH = 1.0;
    /// Java's `MEASURE_CONTEXT`: a context only used to measure texts
    private static measureContext: OffscreenCanvasRenderingContext2D | CanvasRenderingContext2D | null = null;

    /**
    Measures texts as `draw` presents the labels of a draw list.
    */
    public static readonly TEXT_MEASURER: InputGizmo2DPainter.TextMeasurer = {
        getTextWidth(text: string, fontSize: number): number {
            const context = HtmlGizmo2DDrawListRenderer.measurer();
            if (context === null) {
                return Math.ceil(text.length * fontSize * 0.6);
            }
            context.font = HtmlGizmo2DDrawListRenderer.font(fontSize);
            return Math.ceil(context.measureText(text).width);
        },

        getTextHeight(fontSize: number): number {
            const context = HtmlGizmo2DDrawListRenderer.measurer();
            if (context === null) {
                return Math.ceil(fontSize * 1.15);
            }
            context.font = HtmlGizmo2DDrawListRenderer.font(fontSize);
            const metrics: TextMetrics = context.measureText("0");
            return Math.ceil(metrics.fontBoundingBoxAscent + metrics.fontBoundingBoxDescent);
        },
    };

    private constructor() {}

    /**
    @param g 2D context of the canvas where to draw
    @param list primitives to draw, in pixels of the viewport
    @param x0 left pixel of the viewport in `g`
    @param y0 top pixel of the viewport in `g`
    */
    public static draw(g: CanvasRenderingContext2D, list: Gizmo2DDrawList | null, x0: number, y0: number): void {
        if (list === null || list.isEmpty()) {
            return;
        }
        g.save();
        for (const p of list.getPrimitivesInDrawingOrder()) {
            const color: string = HtmlGizmo2DDrawListRenderer.toCssColor(p.getColor(), p.getOpacity());
            g.fillStyle = color;
            g.strokeStyle = color;
            switch (p.getKind()) {
                case Gizmo2DDrawList.Kind.POLYGON:
                    HtmlGizmo2DDrawListRenderer.polygon(g, p, x0, y0);
                    g.fill();
                    if (!p.isOverlay() && p.getOpacity() >= 1.0) {
                        g.lineWidth = HtmlGizmo2DDrawListRenderer.SEAM_STROKE_WIDTH;
                        g.lineCap = "round";
                        g.lineJoin = "round";
                        g.stroke();
                    }
                    break;
                case Gizmo2DDrawList.Kind.LINE:
                    g.lineWidth = Math.max(1.0, p.getWidth());
                    g.lineCap = "round";
                    g.lineJoin = "round";
                    g.beginPath();
                    g.moveTo(x0 + p.getX(0), y0 + p.getY(0));
                    g.lineTo(x0 + p.getX(1), y0 + p.getY(1));
                    g.stroke();
                    break;
                case Gizmo2DDrawList.Kind.LABEL:
                    HtmlGizmo2DDrawListRenderer.drawLabel(g, p, x0, y0);
                    break;
                default:
                    break;
            }
        }
        g.restore();
    }

    private static polygon(g: CanvasRenderingContext2D, p: Gizmo2DDrawList.Primitive, x0: number, y0: number): void {
        g.beginPath();
        g.moveTo(x0 + p.getX(0), y0 + p.getY(0));
        for (let i: number = 1; i < p.getNumberOfVertices(); i++) {
            g.lineTo(x0 + p.getX(i), y0 + p.getY(i));
        }
        g.closePath();
    }

    /**
    Draws a label whose box has the height given by `TEXT_MEASURER` and the
    baseline at its ascent from the top, as in the label images of
    `WebSystem`.
    */
    private static drawLabel(g: CanvasRenderingContext2D, p: Gizmo2DDrawList.Primitive, x0: number, y0: number): void {
        const text: string = p.getText() ?? "";
        g.font = HtmlGizmo2DDrawListRenderer.font(p.getFontSize());
        const ascent: number = g.measureText(text).fontBoundingBoxAscent;
        const height: number = HtmlGizmo2DDrawListRenderer.TEXT_MEASURER.getTextHeight(p.getFontSize());
        let left: number = x0 + p.getX(0);
        let bottom: number = y0 + p.getY(0);

        if (p.getAlignment() === Gizmo2DDrawList.Alignment.CENTER) {
            left -= HtmlGizmo2DDrawListRenderer.TEXT_MEASURER.getTextWidth(text, p.getFontSize()) / 2.0;
            bottom += height / 2.0;
        }
        g.textBaseline = "alphabetic";
        g.fillText(text, left, bottom - height + ascent);
    }

    private static font(fontSize: number): string {
        return fontSize + "px " + HtmlGizmo2DDrawListRenderer.FONT_NAME;
    }

    private static measurer(): OffscreenCanvasRenderingContext2D | CanvasRenderingContext2D | null {
        if (HtmlGizmo2DDrawListRenderer.measureContext === null) {
            if (typeof OffscreenCanvas !== "undefined") {
                HtmlGizmo2DDrawListRenderer.measureContext = new OffscreenCanvas(1, 1).getContext("2d");
            } else if (typeof document !== "undefined") {
                HtmlGizmo2DDrawListRenderer.measureContext = document.createElement("canvas").getContext("2d");
            }
        }
        return HtmlGizmo2DDrawListRenderer.measureContext;
    }

    /**
    @param c a vitral color, with components between 0 and 1
    @param opacity between 0 (transparent) and 1 (opaque)
    @return the same color for the canvas
    */
    private static toCssColor(c: ColorRgb, opacity: number): string {
        const f = HtmlGizmo2DDrawListRenderer.clamp;
        return (
            "rgba(" +
            Math.round(f(c.r()) * 255) +
            "," +
            Math.round(f(c.g()) * 255) +
            "," +
            Math.round(f(c.b()) * 255) +
            "," +
            f(opacity) +
            ")"
        );
    }

    private static clamp(value: number): number {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
