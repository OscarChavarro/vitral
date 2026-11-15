import type { ColorRgb } from "../../common/color/ColorRgb.js";
import type { InputGizmo } from "../../gui/gizmo/InputGizmo.js";
import type { ViewportElementScaler } from "../../gui/viewport/ViewportElementScaler.js";
import { Gizmo2DDrawList } from "./Gizmo2DDrawList.js";

/**
Measures texts as the backend draws `Gizmo2DDrawList` labels (Java
`InputGizmo2DPainter.TextMeasurer`).
*/
interface InputGizmo2DPainterTextMeasurer {
    /**
    @return the width, in pixels, of the box of a text
    */
    getTextWidth(text: string, fontSize: number): number;

    /**
    @return the height, in pixels, of the box of any text of a font size
    */
    getTextHeight(fontSize: number): number;
}

/**
Port of `vsdk.toolkit.render.gizmo.InputGizmo2DPainter`.

Presents an {@link InputGizmo} as 2D overlay primitives (see
`Gizmo2DDrawList`), with the layout of `Jogl4InputGizmoRenderer`: at the
lower right corner of the viewport, each number inside a frame with the color
of its box (see `InputGizmo.getFieldDisplayColor`), the frames at least as
wide as the reference text of the gizmo, and a thin line below the frame of
the selected box. Sizes follow the screen resolution (see
`ViewportElementScaler`).

The width of the texts depends on the fonts of the drawing technology, so
it is measured by a `TextMeasurer` given by the backend.
*/
export class InputGizmo2DPainter {
    // Sizes, in pixels, designed for legacy resolutions
    private static readonly BASE_FONT_SIZE = 14;
    private static readonly BASE_MARGIN = 10;
    private static readonly BASE_GAP = 4;
    private static readonly BASE_PADDING_X = 3;
    private static readonly BASE_PADDING_Y = 1;
    private static readonly BASE_FRAME_THICKNESS = 1;
    private static readonly BASE_UNDERLINE_DISTANCE = 2;
    private static readonly BASE_UNDERLINE_THICKNESS = 2;

    private constructor() {
    }

    /**
    @param out list receiving the primitives
    @param gizmo gizmo to present
    @param viewportWidth width of the viewport, in pixels
    @param viewportHeight height of the viewport, in pixels
    @param scaler sizes of the elements for the resolution of the screen
    @param measurer measures the texts as the backend draws them
    */
    public static paint(out: Gizmo2DDrawList | null, gizmo: InputGizmo | null,
                        viewportWidth: number, viewportHeight: number,
                        scaler: ViewportElementScaler,
                        measurer: InputGizmo2DPainterTextMeasurer): void {
        if (out === null || gizmo === null || gizmo.getNumberOfFields() <= 0) {
            return;
        }
        const count = gizmo.getNumberOfFields();
        const fontSize = scaler.scaleSize(InputGizmo2DPainter.BASE_FONT_SIZE);
        const margin = scaler.scaleSize(InputGizmo2DPainter.BASE_MARGIN);
        const gap = scaler.scaleSize(InputGizmo2DPainter.BASE_GAP);
        const paddingX = scaler.scaleSize(InputGizmo2DPainter.BASE_PADDING_X);
        const paddingY = scaler.scaleSize(InputGizmo2DPainter.BASE_PADDING_Y);
        const frameThickness = Math.max(1, scaler.scaleSize(InputGizmo2DPainter.BASE_FRAME_THICKNESS));
        const underlineDistance = scaler.scaleSize(InputGizmo2DPainter.BASE_UNDERLINE_DISTANCE);
        const underlineThickness = Math.max(1, scaler.scaleSize(InputGizmo2DPainter.BASE_UNDERLINE_THICKNESS));
        const referenceWidth = measurer.getTextWidth(gizmo.getReferenceText(), fontSize);
        const textHeight = measurer.getTextHeight(fontSize);

        //-----------------------------------------------------------------
        const colors: ColorRgb[] = [];
        const texts: string[] = [];
        const textWidths: number[] = [];
        const frameWidths: number[] = [];
        let totalWidth = (count - 1) * gap;

        for (let i = 0; i < count; i++) {
            colors[i] = gizmo.getFieldDisplayColor(i);
            // An empty text (i.e. all its characters deleted) is a blank box
            texts[i] = gizmo.getDisplayText(i).length === 0 ? " " : gizmo.getDisplayText(i);
            textWidths[i] = measurer.getTextWidth(texts[i]!, fontSize);
            frameWidths[i] = Math.max(referenceWidth, textWidths[i]!) +
                2*(paddingX + frameThickness);
            totalWidth += frameWidths[i]!;
        }

        const frameHeight = textHeight + 2*(paddingY + frameThickness);
        let x = viewportWidth - margin - totalWidth;
        const top = viewportHeight - margin - frameHeight;

        for (let i = 0; i < count; i++) {
            InputGizmo2DPainter.addFrame(out, x, top, frameWidths[i]!, frameHeight,
                frameThickness, colors[i]!);
            if (i === gizmo.getSelectedField()) {
                out.addOverlayRectangle(x, top + frameHeight + underlineDistance,
                    frameWidths[i]!, underlineThickness, colors[i]!);
            }
            out.addOverlayLabel(x + Math.trunc((frameWidths[i]! - textWidths[i]!) / 2),
                top + frameHeight - frameThickness - paddingY, texts[i]!, colors[i]!,
                fontSize, Gizmo2DDrawList.Alignment.LOWER_LEFT);
            x += frameWidths[i]! + gap;
        }
    }

    private static addFrame(out: Gizmo2DDrawList, x: number, y: number, width: number,
                            height: number, thickness: number, c: ColorRgb): void {
        out.addOverlayRectangle(x, y, width, thickness, c);
        out.addOverlayRectangle(x, y + height - thickness, width, thickness, c);
        out.addOverlayRectangle(x, y + thickness, thickness, height - 2*thickness, c);
        out.addOverlayRectangle(x + width - thickness, y + thickness, thickness,
            height - 2*thickness, c);
    }
}

// Java nested type: `InputGizmo2DPainter.TextMeasurer`
export namespace InputGizmo2DPainter {
    export type TextMeasurer = InputGizmo2DPainterTextMeasurer;
}
