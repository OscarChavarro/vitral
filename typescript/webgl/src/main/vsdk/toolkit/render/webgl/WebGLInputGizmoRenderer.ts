import {
    InputGizmo,
    Matrix4x4d,
    type ColorRgb,
    type RGBAImageUncompressed,
    type Viewport,
    type ViewportElementScaler,
    type ViewportSet,
} from "@vitral/base";
import { WebGLColoredPrimitiveRenderer } from "./WebGLColoredPrimitiveRenderer.js";
import { WebGLImageRenderer } from "./WebGLImageRenderer.js";

/**
Services a `WebGLInputGizmoRenderer` needs from who presents the viewport
(Java's nested interface `Jogl4InputGizmoRenderer.Host`).
*/
export interface WebGLInputGizmoRendererHost {
    /**
    @param text the text to draw
    @param color the color of the text
    @param fontSize the size of the font, in pixels
    @return an image with the text on transparent background
    */
    createLabelImage(text: string, color: ColorRgb, fontSize: number): RGBAImageUncompressed;

    /**
    Draws a label image with its lower left corner at a position of the
    viewport, measured in pixels from its lower left corner.
    */
    drawLabelImage(gl: WebGL2RenderingContext, image: RGBAImageUncompressed, x: number, y: number): Promise<void>;

    /**
    Marks a label image as no longer used, so its texture is released.
    */
    discardLabelImage(image: RGBAImageUncompressed): void;
}

/**
Port of `vsdk.toolkit.render.jogl.gizmo.Jogl4InputGizmoRenderer`.

Renders an {@link InputGizmo} over a viewport with the WebGL pipeline, at
its lower right corner: each number is a label image inside a frame with the
color of its box (see `InputGizmo.getFieldDisplayColor`), and the selected box
has a thin line below its frame. Sizes follow the screen resolution (see
`ViewportElementScaler`).

There must be one renderer for each viewport where the gizmo is drawn, as it
keeps the label images (regenerated only when the text, the color or the font
size of a box change). Text rasterization and the texture management of the
images are provided by the `Host`, so this class depends neither on the DOM
nor on the presentation of the viewport.

Usage (once per frame, with the viewport already activated):
<pre>
    await renderer.draw(gl, inputGizmo);
</pre>

WebGL has no polygon mode: the frames are always filled triangles, which is
what Java selects with `glPolygonMode(GL_FILL)`.
*/
export class WebGLInputGizmoRenderer {
    // Sizes, in pixels, designed for legacy resolutions
    private static readonly BASE_FONT_SIZE: number = 14;
    private static readonly BASE_MARGIN: number = 10;
    private static readonly BASE_GAP: number = 4;
    private static readonly BASE_PADDING_X: number = 3;
    private static readonly BASE_PADDING_Y: number = 1;
    private static readonly BASE_FRAME_THICKNESS: number = 1;
    private static readonly BASE_UNDERLINE_DISTANCE: number = 2;
    private static readonly BASE_UNDERLINE_THICKNESS: number = 2;

    private readonly host: WebGLInputGizmoRendererHost;
    private readonly viewportSet: ViewportSet;
    private readonly viewport: Viewport;

    private images: (RGBAImageUncompressed | null)[] | null = null;
    private imageTexts: (string | null)[] = [];
    private imageColors: (ColorRgb | null)[] = [];
    private imageFontSize: number = 0;
    private referenceText: string | null = null;
    private referenceTextWidth: number = 0;

    /**
    @param host provider of label images and their drawing
    @param viewportSet set the viewport belongs to
    @param viewport viewport where the gizmo is drawn
    */
    public constructor(host: WebGLInputGizmoRendererHost, viewportSet: ViewportSet, viewport: Viewport) {
        this.host = host;
        this.viewportSet = viewportSet;
        this.viewport = viewport;
    }

    /**
    Draws the gizmo over the viewport.
    @param gl WebGL context
    @param gizmo gizmo to draw
    */
    public async draw(gl: WebGL2RenderingContext | null, gizmo: InputGizmo | null): Promise<void> {
        if (gl === null || gizmo === null) {
            return;
        }
        const count: number = gizmo.getNumberOfFields();
        const scaler: ViewportElementScaler = this.viewportSet.getElementScaler();
        const fontSize: number = scaler.scaleSize(WebGLInputGizmoRenderer.BASE_FONT_SIZE);
        const margin: number = scaler.scaleSize(WebGLInputGizmoRenderer.BASE_MARGIN);
        const gap: number = scaler.scaleSize(WebGLInputGizmoRenderer.BASE_GAP);
        const paddingX: number = scaler.scaleSize(WebGLInputGizmoRenderer.BASE_PADDING_X);
        const paddingY: number = scaler.scaleSize(WebGLInputGizmoRenderer.BASE_PADDING_Y);
        const frameThickness: number = Math.max(1, scaler.scaleSize(WebGLInputGizmoRenderer.BASE_FRAME_THICKNESS));
        const underlineDistance: number = scaler.scaleSize(WebGLInputGizmoRenderer.BASE_UNDERLINE_DISTANCE);
        const underlineThickness: number =
            Math.max(1, scaler.scaleSize(WebGLInputGizmoRenderer.BASE_UNDERLINE_THICKNESS));

        if (this.images === null || this.images.length !== count) {
            this.discardImages();
            this.images = new Array<RGBAImageUncompressed | null>(count).fill(null);
            this.imageTexts = new Array<string | null>(count).fill(null);
            this.imageColors = new Array<ColorRgb | null>(count).fill(null);
        }
        // The frames have at least the width of the reference text of the gizmo
        if (fontSize !== this.imageFontSize || this.referenceTextWidth === 0 ||
            gizmo.getReferenceText() !== this.referenceText) {
            // The reference image is only measured, it is never drawn
            this.referenceText = gizmo.getReferenceText();
            this.referenceTextWidth = this.host.createLabelImage(
                this.referenceText, InputGizmo.HIGHLIGHT_COLOR, fontSize).getXSize();
        }

        //-----------------------------------------------------------------
        const images: (RGBAImageUncompressed | null)[] = this.images;
        const colors: ColorRgb[] = [];
        const frameWidths: number[] = [];
        let totalWidth: number = 0;

        for (let i: number = 0; i < count; i++) {
            colors.push(gizmo.getFieldDisplayColor(i));
            this.updateImage(i, gizmo.getDisplayText(i), colors[i]!, fontSize);
            frameWidths.push(Math.max(this.referenceTextWidth, images[i]!.getXSize())
                + 2 * (paddingX + frameThickness));
            totalWidth += frameWidths[i]!;
        }
        this.imageFontSize = fontSize;
        totalWidth += (count - 1) * gap;

        const frameHeight: number = images[0]!.getYSize() + 2 * (paddingY + frameThickness);
        let x: number = this.viewport.getPixelSizeX() - margin - totalWidth;
        const y: number = margin;
        const rectangles: number[][] = [];
        const frameStarts: number[] = [];

        for (let i: number = 0; i < count; i++) {
            frameStarts.push(x);
            WebGLInputGizmoRenderer.addFrame(rectangles, x, y, frameWidths[i]!, frameHeight,
                frameThickness, colors[i]!);
            if (i === gizmo.getSelectedField()) {
                const underlineY: number = y - underlineDistance - underlineThickness;

                WebGLInputGizmoRenderer.addRectangle(rectangles, x, underlineY, x + frameWidths[i]!,
                    underlineY + underlineThickness, colors[i]!);
            }
            x += frameWidths[i]! + gap;
        }
        await this.drawRectangles(gl, rectangles);

        for (let i: number = 0; i < count; i++) {
            const textX: number = frameStarts[i]! + Math.trunc((frameWidths[i]! - images[i]!.getXSize()) / 2);
            const textY: number = y + frameThickness + paddingY;

            await this.host.drawLabelImage(gl, images[i]!, textX, textY);
        }
    }

    /**
    Deletes the WebGL resources (label textures) of this renderer.
    @param gl WebGL context
    */
    public disposeGlResources(gl: WebGL2RenderingContext): void {
        if (this.images === null) {
            return;
        }
        for (let i: number = 0; i < this.images.length; i++) {
            const image: RGBAImageUncompressed | null = this.images[i]!;
            if (image !== null) {
                WebGLImageRenderer.unload(gl, image);
                this.images[i] = null;
                this.imageTexts[i] = null;
            }
        }
    }

    private discardImages(): void {
        if (this.images === null) {
            return;
        }
        for (const image of this.images) {
            if (image !== null) {
                this.host.discardLabelImage(image);
            }
        }
    }

    private updateImage(field: number, text: string, color: ColorRgb, fontSize: number): void {
        const images: (RGBAImageUncompressed | null)[] = this.images!;
        const current: RGBAImageUncompressed | null = images[field]!;
        const currentColor: ColorRgb | null = this.imageColors[field]!;

        if (current !== null && fontSize === this.imageFontSize &&
            text === this.imageTexts[field] && color.equals(currentColor)) {
            return;
        }
        if (current !== null) {
            this.host.discardLabelImage(current);
        }
        // An empty text (i.e. all its characters deleted) cannot be rasterized:
        // an empty box is drawn with a blank text
        images[field] = this.host.createLabelImage(text.length === 0 ? " " : text, color, fontSize);
        this.imageTexts[field] = text;
        this.imageColors[field] = color;
    }

    //= Frames ============================================================
    // Rectangles are {x0, y0, x1, y1, r, g, b}, in pixels of the viewport,
    // measured from its lower left corner

    private static addRectangle(rectangles: number[][],
                                x0: number, y0: number, x1: number, y1: number, c: ColorRgb): void {
        rectangles.push([x0, y0, x1, y1, c.r(), c.g(), c.b()]);
    }

    private static addFrame(rectangles: number[][], x: number, y: number,
                            width: number, height: number, thickness: number, c: ColorRgb): void {
        WebGLInputGizmoRenderer.addRectangle(rectangles, x, y, x + width, y + thickness, c);
        WebGLInputGizmoRenderer.addRectangle(rectangles, x, y + height - thickness, x + width, y + height, c);
        WebGLInputGizmoRenderer.addRectangle(rectangles, x, y + thickness, x + thickness, y + height - thickness, c);
        WebGLInputGizmoRenderer.addRectangle(rectangles, x + width - thickness, y + thickness, x + width,
            y + height - thickness, c);
    }

    /**
    Draws opaque rectangles over the whole surface of the viewport set, clipped
    to the area of this viewport with the scissor test (the same way label
    images are drawn by the viewport window). The state changed is
    restored.
    */
    private async drawRectangles(gl: WebGL2RenderingContext, rectangles: number[][]): Promise<void> {
        if (rectangles.length === 0) {
            return;
        }
        const currentViewport: Int32Array = gl.getParameter(gl.VIEWPORT) as Int32Array;

        let surfaceWidth: number = this.viewportSet.getSizeXInPixels();
        let surfaceHeight: number = this.viewportSet.getSizeYInPixels();

        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            surfaceWidth = currentViewport[0]! + currentViewport[2]!;
            surfaceHeight = currentViewport[1]! + currentViewport[3]!;
        }

        const positions: Float32Array = new Float32Array(rectangles.length * 6 * 3);
        const colors: Float32Array = new Float32Array(rectangles.length * 6 * 4);
        let vertex: number = 0;

        for (const r of rectangles) {
            const x0: number = this.viewport.getPixelStartX() + r[0]!;
            const y0: number = this.viewport.getPixelStartY() + r[1]!;
            const x1: number = this.viewport.getPixelStartX() + r[2]!;
            const y1: number = this.viewport.getPixelStartY() + r[3]!;
            const corners: number[] = [x0, y0,  x1, y0,  x1, y1,  x0, y0,  x1, y1,  x0, y1];

            for (let k: number = 0; k < 6; k++) {
                positions[3 * vertex] = 2.0 * corners[2 * k]! / surfaceWidth - 1.0;
                positions[3 * vertex + 1] = 2.0 * corners[2 * k + 1]! / surfaceHeight - 1.0;
                positions[3 * vertex + 2] = 0;
                colors[4 * vertex] = r[4]!;
                colors[4 * vertex + 1] = r[5]!;
                colors[4 * vertex + 2] = r[6]!;
                colors[4 * vertex + 3] = 1.0;
                vertex++;
            }
        }

        const scissorWasEnabled: boolean = gl.isEnabled(gl.SCISSOR_TEST);
        const previousScissor: Int32Array = gl.getParameter(gl.SCISSOR_BOX) as Int32Array;

        gl.viewport(0, 0, surfaceWidth, surfaceHeight);
        gl.enable(gl.SCISSOR_TEST);
        gl.scissor(this.viewport.getPixelStartX(), this.viewport.getPixelStartY(),
            this.viewport.getPixelSizeX(), this.viewport.getPixelSizeY());
        gl.disable(gl.DEPTH_TEST);
        gl.disable(gl.CULL_FACE);

        await WebGLColoredPrimitiveRenderer.draw(gl, Matrix4x4d.identityMatrix(),
            gl.TRIANGLES, positions, colors);

        gl.enable(gl.DEPTH_TEST);
        gl.scissor(previousScissor[0]!, previousScissor[1]!, previousScissor[2]!, previousScissor[3]!);
        if (!scissorWasEnabled) {
            gl.disable(gl.SCISSOR_TEST);
        }
        gl.viewport(currentViewport[0]!, currentViewport[1]!, currentViewport[2]!, currentViewport[3]!);
    }
}
