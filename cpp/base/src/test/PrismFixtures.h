#ifndef __PRISM_FIXTURES__
#define __PRISM_FIXTURES__

#include "java/util/ArrayList.h"

class PolyhedralBoundedSolid;

/** One point of a plane polygon: the Java `double[2]`. */
class PrismPoint
{
public:
    double v[2];

    PrismPoint();
    PrismPoint(double x, double y);
    double& operator[](int i);
    double operator[](int i) const;
    bool operator==(const PrismPoint& other) const;
};

/** Plane polygon: the Java `double[][]`. */
typedef java::ArrayList<PrismPoint> PrismPolygon;

/**
Prisms built with the Euler operators and the translational sweep, for the
tests that need operands other than boxes: regular prisms of N sides (the
polyhedral stand-in for a cylinder) and star prisms (non-convex, with N
reflex edges).
C++ counterpart of Java's `PrismFixtures` (test code).
*/
class PrismFixtures
{
public:
    /**
    Prism over the polygon `xy` (counterclockwise), from z=0 to z=height.
    New solid, owned by the caller.
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
