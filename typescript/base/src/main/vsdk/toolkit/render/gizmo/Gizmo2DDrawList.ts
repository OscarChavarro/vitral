import { Collections } from "../../../../java/util/Collections.js";
import type { Comparator } from "../../../../java/util/Comparator.js";
import { ColorRgb } from "../../common/color/ColorRgb.js";

/**
Kind of a primitive (Java `Gizmo2DDrawList.Kind`).
*/
enum Gizmo2DDrawListKind {
    /** Filled simple polygon (convex or not), of at least 3 vertices */
    POLYGON,
    /** Line from the vertex 0 to the vertex 1, of `getWidth()` pixels */
    LINE,
    /** Text, positioned following `getAlignment()` */
    LABEL,
}

/**
How the position of a label relates to its text (Java
`Gizmo2DDrawList.Alignment`).
*/
enum Gizmo2DDrawListAlignment {
    /** The position is the lower left corner of the box of the text */
    LOWER_LEFT,
    /** The position is the center of the box of the text */
    CENTER,
}

/**
One primitive of the list (Java `Gizmo2DDrawList.Primitive`). Instances are
created by the `add*` methods of the list.
*/
class Gizmo2DDrawListPrimitive {
    private readonly kind: Gizmo2DDrawListKind;
    private readonly x: number[];
    private readonly y: number[];
    private readonly color: ColorRgb;
    private readonly opacity: number;
    private readonly width: number;
    private readonly depth: number;
    private readonly overlay: boolean;
    private readonly text: string | null;
    private readonly fontSize: number;
    private readonly alignment: Gizmo2DDrawListAlignment | null;

    /** @internal */
    public constructor(
        kind: Gizmo2DDrawListKind,
        x: number[],
        y: number[],
        color: ColorRgb,
        opacity: number,
        width: number,
        depth: number,
        overlay: boolean,
        text: string | null,
        fontSize: number,
        alignment: Gizmo2DDrawListAlignment | null,
    ) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.color = new ColorRgb(color);
        this.opacity = opacity;
        this.width = width;
        this.depth = depth;
        this.overlay = overlay;
        this.text = text;
        this.fontSize = fontSize;
        this.alignment = alignment;
    }

    public getKind(): Gizmo2DDrawListKind {
        return this.kind;
    }

    /** @return number of vertices (1 for labels) */
    public getNumberOfVertices(): number {
        return this.x.length;
    }

    public getX(vertex: number): number {
        return this.x[vertex]!;
    }

    public getY(vertex: number): number {
        return this.y[vertex]!;
    }

    public getColor(): ColorRgb {
        return this.color;
    }

    /** @return opacity, from 0 (invisible) to 1 (opaque) */
    public getOpacity(): number {
        return this.opacity;
    }

    /** @return width of a line, in pixels */
    public getWidth(): number {
        return this.width;
    }

    /** @return normalized device depth (bigger is farther) */
    public getDepth(): number {
        return this.depth;
    }

    /**
    @return true for screen space primitives (input boxes, labels), drawn
    after the depth sorted ones and never sorted
    */
    public isOverlay(): boolean {
        return this.overlay;
    }

    public getText(): string | null {
        return this.text;
    }

    /** @return font size of a label, in pixels */
    public getFontSize(): number {
        return this.fontSize;
    }

    public getAlignment(): Gizmo2DDrawListAlignment | null {
        return this.alignment;
    }
}

/**
Port of `vsdk.toolkit.render.gizmo.Gizmo2DDrawList`.

Technology independent list of 2D primitives that presents gizmos over a
viewport, for drawing surfaces without a 3D API (a canvas, a raster image, a
BGI/VGA frame buffer...). It is the output of the gizmo painters of this
package (`TranslateGizmo2DPainter`, `RotateGizmo2DPainter`,
`ScaleGizmo2DPainter` and `InputGizmo2DPainter`), already projected, clipped
and sorted, so a backend only has to know how to:

- fill a simple polygon with a color and an opacity,
- draw a line with a color and a width,
- draw a text with a color and a font size.

Coordinates are pixels of the viewport, with the origin at its upper left
corner and growing downwards (as AWT, BGI and most frame buffers do).

As there is no depth buffer, hidden surfaces are solved with the painter's
algorithm [FOLE1992].15.5: primitives coming from 3D elements (`isOverlay`
false) keep a depth (the mean of the normalized device depth of their
vertices) and `getPrimitivesInDrawingOrder` sorts them from far to near;
overlay primitives (input boxes, labels) are drawn after them, in the order
they were added. Primitives at the same depth keep the order they were
added, so translucent elements added after the opaque ones stay over them.
*/
export class Gizmo2DDrawList {
    private readonly primitives: Gizmo2DDrawListPrimitive[];

    public constructor() {
        this.primitives = [];
    }

    public clear(): void {
        this.primitives.length = 0;
    }

    public isEmpty(): boolean {
        return this.primitives.length === 0;
    }

    /**
    Adds a polygon coming from a 3D element, sorted by depth.
    */
    public addPolygon(x: number[], y: number[], depth: number, color: ColorRgb, opacity: number): void {
        this.primitives.push(
            new Gizmo2DDrawListPrimitive(
                Gizmo2DDrawListKind.POLYGON,
                x.slice(),
                y.slice(),
                color,
                opacity,
                0,
                depth,
                false,
                null,
                0,
                null,
            ),
        );
    }

    /**
    Adds a line coming from a 3D element, sorted by depth.
    */
    public addLine(
        x0: number,
        y0: number,
        x1: number,
        y1: number,
        depth: number,
        color: ColorRgb,
        width: number,
    ): void {
        this.primitives.push(
            new Gizmo2DDrawListPrimitive(
                Gizmo2DDrawListKind.LINE,
                [x0, x1],
                [y0, y1],
                color,
                1.0,
                width,
                depth,
                false,
                null,
                0,
                null,
            ),
        );
    }

    /**
    Adds a screen space line (overlay), i.e. a line whose visibility was
    already solved (as the hidden line removal of a HUD).
    */
    public addOverlayLine(x0: number, y0: number, x1: number, y1: number, color: ColorRgb, width: number): void {
        this.primitives.push(
            new Gizmo2DDrawListPrimitive(
                Gizmo2DDrawListKind.LINE,
                [x0, x1],
                [y0, y1],
                color,
                1.0,
                width,
                0,
                true,
                null,
                0,
                null,
            ),
        );
    }

    /**
    Adds an opaque screen space rectangle (overlay).
    @param x left pixel
    @param y top pixel
    @param width width, in pixels
    @param height height, in pixels
    @param color color to fill it with
    */
    public addOverlayRectangle(x: number, y: number, width: number, height: number, color: ColorRgb): void {
        this.primitives.push(
            new Gizmo2DDrawListPrimitive(
                Gizmo2DDrawListKind.POLYGON,
                [x, x + width, x + width, x],
                [y, y, y + height, y + height],
                color,
                1.0,
                0,
                0,
                true,
                null,
                0,
                null,
            ),
        );
    }

    /**
    Adds a screen space text (overlay).
    @param x horizontal position, in pixels (see `alignment`)
    @param y vertical position, in pixels (see `alignment`)
    @param text text to draw
    @param color color of the text
    @param fontSize size of the font, in pixels
    @param alignment how (x, y) relates to the box of the text
    */
    public addOverlayLabel(
        x: number,
        y: number,
        text: string,
        color: ColorRgb,
        fontSize: number,
        alignment: Gizmo2DDrawListAlignment,
    ): void {
        this.primitives.push(
            new Gizmo2DDrawListPrimitive(
                Gizmo2DDrawListKind.LABEL,
                [x],
                [y],
                color,
                1.0,
                0,
                0,
                true,
                text,
                fontSize,
                alignment,
            ),
        );
    }

    /**
    @return the primitives in the order they must be drawn: the depth sorted
    ones from far to near, then the overlays in the order they were added
    */
    public getPrimitivesInDrawingOrder(): Gizmo2DDrawListPrimitive[] {
        const sorted: Gizmo2DDrawListPrimitive[] = [];
        const overlays: Gizmo2DDrawListPrimitive[] = [];

        for (const p of this.primitives) {
            if (p.isOverlay()) {
                overlays.push(p);
            } else {
                sorted.push(p);
            }
        }
        // Stable: equal depths keep the order they were added
        const farToNear: Comparator<Gizmo2DDrawListPrimitive> = {
            compare: (a, b) => (b.getDepth() < a.getDepth() ? -1 : b.getDepth() > a.getDepth() ? 1 : 0),
        };
        Collections.sort(sorted, farToNear);
        sorted.push(...overlays);
        return sorted;
    }
}

// Java nested types: `Gizmo2DDrawList.Kind`, `Gizmo2DDrawList.Alignment` and
// `Gizmo2DDrawList.Primitive`
export namespace Gizmo2DDrawList {
    export const Kind = Gizmo2DDrawListKind;
    export type Kind = Gizmo2DDrawListKind;
    export const Alignment = Gizmo2DDrawListAlignment;
    export type Alignment = Gizmo2DDrawListAlignment;
    export const Primitive = Gizmo2DDrawListPrimitive;
    export type Primitive = Gizmo2DDrawListPrimitive;
}
