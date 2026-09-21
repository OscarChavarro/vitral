package vsdk.toolkit.render;

import vsdk.toolkit.common.color.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;

/**
Geometry of the "selection corners" mark drawn around a selected object: a
small three-line corner at each of the 8 corners of its axis aligned bounding
box, as done by 3D modeling programs.

This class only computes the segments, in the space of the bounding box, so
every rendering technology (JOGL2, JOGL4, software) draws exactly the same
mark; it does not depend on any of them. See
`RendererConfiguration.isSelectionCornersSet()`.
*/
public final class SelectionCorners
{
    /**
    Fraction of the size of the bounding box the mark is enlarged on each side,
    so it does not touch the surface of the object.
    */
    public static final double DEFAULT_BORDER_FRACTION = 0.01;

    /**
    Fraction of the size of the bounding box covered by each line of a corner.
    */
    public static final double DEFAULT_LINE_FRACTION = 0.25;

    /**
    Number of corners of the bounding box.
    */
    public static final int NUMBER_OF_CORNERS = 8;

    /**
    Number of lines drawn on each corner.
    */
    public static final int LINES_PER_CORNER = 3;

    private static final ColorRgb DEFAULT_COLOR = new ColorRgb(1, 1, 1);

    private SelectionCorners()
    {
    }

    /**
    @return the color the mark is drawn with by default
    */
    public static ColorRgb getDefaultColor()
    {
        return new ColorRgb(DEFAULT_COLOR);
    }

    /**
    Builds the segments of the mark with the default proportions.

    @param minmax bounding box as given by `Geometry.getMinMax()`:
    minimum x, y, z followed by maximum x, y, z
    @return see `buildSegments(double[], double, double)`
    */
    public static Vector3Dd[] buildSegments(double[] minmax)
    {
        return buildSegments(minmax, DEFAULT_BORDER_FRACTION, DEFAULT_LINE_FRACTION);
    }

    /**
    Builds the segments of the mark.

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
    public static Vector3Dd[] buildSegments(double[] minmax, double borderFraction, double lineFraction)
    {
        if ( minmax == null || minmax.length < 6 ) {
            return new Vector3Dd[0];
        }

        Vector3Dd min = new Vector3Dd(minmax[0], minmax[1], minmax[2]);
        Vector3Dd max = new Vector3Dd(minmax[3], minmax[4], minmax[5]);
        Vector3Dd size = max.subtract(min);

        min = min.subtract(size.multiply(borderFraction));
        max = max.add(size.multiply(borderFraction));

        Vector3Dd line = size.multiply(lineFraction);
        Vector3Dd[] segments = new Vector3Dd[NUMBER_OF_CORNERS * LINES_PER_CORNER * 2];
        int out = 0;

        for ( int corner = 0; corner < NUMBER_OF_CORNERS; corner++ ) {
            boolean atMaxX = (corner & 1) != 0;
            boolean atMaxY = (corner & 2) != 0;
            boolean atMaxZ = (corner & 4) != 0;
            Vector3Dd origin = new Vector3Dd(
                atMaxX ? max.x() : min.x(),
                atMaxY ? max.y() : min.y(),
                atMaxZ ? max.z() : min.z());
            double sx = atMaxX ? -line.x() : line.x();
            double sy = atMaxY ? -line.y() : line.y();
            double sz = atMaxZ ? -line.z() : line.z();

            segments[out++] = origin;
            segments[out++] = origin.add(new Vector3Dd(sx, 0, 0));
            segments[out++] = origin;
            segments[out++] = origin.add(new Vector3Dd(0, sy, 0));
            segments[out++] = origin;
            segments[out++] = origin.add(new Vector3Dd(0, 0, sz));
        }
        return segments;
    }
}
