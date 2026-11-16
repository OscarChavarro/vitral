#ifndef __PRISM_FIXTURES__
#define __PRISM_FIXTURES__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector2Dd.h"

class PolyhedralBoundedSolid;

/**
Polygon in the plane z = 0: the Java `double[][]` of points `{x, y}`
(`xy[i][0]` is `xy[i].x`).
*/
typedef ArrayList<Vector2Dd> PrismPolygon;

/**
Prisms over planar polygons, built with the Euler operators of [MANT1988]
Ch. 9 (mvfs, smev, smef) and a translational sweep (Sec. 12.2).
C++ counterpart of Java's `PrismFixtures` (test code).
*/
class PrismFixtures
{
public:
    /**
    Prism over the polygon `xy` (counterclockwise), from z=0 to z=height.
    @return a new solid, owned by the caller
    */
    static PolyhedralBoundedSolid* prism(const PrismPolygon& xy,
        double height);

    /** Regular polygon of `sides` sides inscribed in a circle. */
    static PrismPolygon regularPolygon(int sides, double radius);

    /** Star of `points` points: vertices alternate between two radii. */
    static PrismPolygon starPolygon(int points, double outerRadius,
        double innerRadius);

    /** Area of a simple polygon (positive when counterclockwise). */
    static double polygonArea(const PrismPolygon& xy);

private:
    PrismFixtures();
};

#endif
