#ifndef __SELECTION_CORNERS__
#define __SELECTION_CORNERS__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

/**
Geometry of the mark that presents a selected object: three short lines on
each corner of its bounding box (as in 3ds Max), slightly enlarged so it does
not touch the surface of the object. It is independent of any rendering
technology: each renderer draws the segments built here.
*/
class SelectionCorners {
public:
    /**
    Fraction of the size of the bounding box the mark is enlarged on each
    side, so it does not touch the surface of the object.
    */
    static constexpr double DEFAULT_BORDER_FRACTION = 0.01;

    /**
    Fraction of the size of the bounding box covered by each line of a
    corner.
    */
    static constexpr double DEFAULT_LINE_FRACTION = 0.25;

    /** Number of corners of the bounding box. */
    static const int NUMBER_OF_CORNERS = 8;

    /** Number of lines drawn on each corner. */
    static const int LINES_PER_CORNER = 3;

    /**
    @return the color the mark is drawn with by default
    */
    static ColorRgb getDefaultColor();

    /**
    Builds the segments of the mark with the default proportions.
    @param minmax bounding box as given by `Geometry::getMinMax()`: minimum
    x, y, z followed by maximum x, y, z (may be null)
    @return see `buildSegments(const double*, double, double)`
    */
    static java::ArrayList<Vector3Dd> buildSegments(const double* minmax);

    /**
    Builds the segments of the mark.
    @param minmax bounding box as given by `Geometry::getMinMax()`: minimum
    x, y, z followed by maximum x, y, z (may be null)
    @param borderFraction fraction of the size of the box the mark is
    enlarged on each side
    @param lineFraction fraction of the size of the box each line covers
    @return the end points of the segments, two consecutive points per
    segment (`NUMBER_OF_CORNERS * LINES_PER_CORNER` segments); the three
    lines of a corner start at the corner and go inside the box, along the
    X, Y and Z axes. It is empty if the bounding box is missing.
    */
    static java::ArrayList<Vector3Dd> buildSegments(const double* minmax,
        double borderFraction, double lineFraction);

private:
    SelectionCorners() {}
};

#endif
