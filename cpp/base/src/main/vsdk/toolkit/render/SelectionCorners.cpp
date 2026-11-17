#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/SelectionCorners.h"

ColorRgb SelectionCorners::getDefaultColor()
{
    return ColorRgb(1, 1, 1);
}

java::ArrayList<Vector3Dd> SelectionCorners::buildSegments(const double* minmax)
{
    return buildSegments(minmax, DEFAULT_BORDER_FRACTION,
                         DEFAULT_LINE_FRACTION);
}

java::ArrayList<Vector3Dd> SelectionCorners::buildSegments(
    const double* minmax, double borderFraction, double lineFraction)
{
    java::ArrayList<Vector3Dd> segments;
    if ( minmax == nullptr ) {
        return segments;
    }

    Vector3Dd min(minmax[0], minmax[1], minmax[2]);
    Vector3Dd max(minmax[3], minmax[4], minmax[5]);
    Vector3Dd size = max.subtract(min);

    min = min.subtract(size.multiply(borderFraction));
    max = max.add(size.multiply(borderFraction));

    Vector3Dd line = size.multiply(lineFraction);

    for ( int corner = 0; corner < NUMBER_OF_CORNERS; corner++ ) {
        bool atMaxX = (corner & 1) != 0;
        bool atMaxY = (corner & 2) != 0;
        bool atMaxZ = (corner & 4) != 0;
        Vector3Dd origin(
            atMaxX ? max.x() : min.x(),
            atMaxY ? max.y() : min.y(),
            atMaxZ ? max.z() : min.z());
        double sx = atMaxX ? -line.x() : line.x();
        double sy = atMaxY ? -line.y() : line.y();
        double sz = atMaxZ ? -line.z() : line.z();

        segments.add(origin);
        segments.add(origin.add(Vector3Dd(sx, 0, 0)));
        segments.add(origin);
        segments.add(origin.add(Vector3Dd(0, sy, 0)));
        segments.add(origin);
        segments.add(origin.add(Vector3Dd(0, 0, sz)));
    }
    return segments;
}
