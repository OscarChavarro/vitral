import { ColorRgb } from "../common/color/ColorRgb.js";
import { Vector3Dd } from "../common/linealAlgebra/Vector3Dd.js";

/**
Geometry of the "selection corners" mark drawn around a selected object: a
small three-line corner at each of the 8 corners of its axis aligned bounding
box, as done by 3D modeling programs.

This class only computes the segments, in the space of the bounding box, so
every rendering technology (JOGL2, JOGL4, software) draws exactly the same
mark; it does not depend on any of them. See
`RendererConfiguration.isSelectionCornersSet()`.
*/
export class SelectionCorners {
    /**
    Fraction of the size of the bounding box the mark is enlarged on each side,
    so it does not touch the surface of the object.
    */
    public static readonly DEFAULT_BORDER_FRACTION = 0.01;

    /**
    Fraction of the size of the bounding box covered by each line of a corner.
    */
    public static readonly DEFAULT_LINE_FRACTION = 0.25;

    /**
    Number of corners of the bounding box.
    */
    public static readonly NUMBER_OF_CORNERS = 8;

    /**
    Number of lines drawn on each corner.
    */
    public static readonly LINES_PER_CORNER = 3;

    private static readonly DEFAULT_COLOR = new ColorRgb(1, 1, 1);

    private constructor() {}

    /**
    @return the color the mark is drawn with by default
    */
    public static getDefaultColor(): ColorRgb {
        return new ColorRgb(SelectionCorners.DEFAULT_COLOR);
    }

    /**
    Builds the segments of the mark (Java's two overloads: without the
    fractions, the default proportions are used).

    @param minmax bounding box as given by `Geometry.getMinMax()`:
    minimum x, y, z followed by maximum x, y, z
    @param borderFraction fraction of the size of the box the mark is
    enlarged on each side
    @param lineFraction fraction of the size of the box each line covers
    @return the end points of the segments, two consecutive points per
    segment (`NUMBER_OF_CORNERS * LINES_PER_CORNER` segments); the three lines
    of a corner start at the corner and go inside the box, along the X, Y and
    Z axes. It is empty if the bounding box is missing or incomplete.
    */
    public static buildSegments(
        minmax: ArrayLike<number> | null,
        borderFraction: number = SelectionCorners.DEFAULT_BORDER_FRACTION,
        lineFraction: number = SelectionCorners.DEFAULT_LINE_FRACTION,
    ): Vector3Dd[] {
        if (minmax === null || minmax.length < 6) {
            return [];
        }

        let min = new Vector3Dd(minmax[0]!, minmax[1]!, minmax[2]!);
        let max = new Vector3Dd(minmax[3]!, minmax[4]!, minmax[5]!);
        const size: Vector3Dd = max.subtract(min);

        min = min.subtract(size.multiply(borderFraction));
        max = max.add(size.multiply(borderFraction));

        const line: Vector3Dd = size.multiply(lineFraction);
        const segments: Vector3Dd[] = [];

        for (let corner = 0; corner < SelectionCorners.NUMBER_OF_CORNERS; corner++) {
            const atMaxX: boolean = (corner & 1) !== 0;
            const atMaxY: boolean = (corner & 2) !== 0;
            const atMaxZ: boolean = (corner & 4) !== 0;
            const origin = new Vector3Dd(atMaxX ? max.x() : min.x(), atMaxY ? max.y() : min.y(), atMaxZ ? max.z() : min.z());
            const sx: number = atMaxX ? -line.x() : line.x();
            const sy: number = atMaxY ? -line.y() : line.y();
            const sz: number = atMaxZ ? -line.z() : line.z();

            segments.push(origin);
            segments.push(origin.add(new Vector3Dd(sx, 0, 0)));
            segments.push(origin);
            segments.push(origin.add(new Vector3Dd(0, sy, 0)));
            segments.push(origin);
            segments.push(origin.add(new Vector3Dd(0, 0, sz)));
        }
        return segments;
    }
}
