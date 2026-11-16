#ifndef __COMPOSITE_SHAPE_ORACLE__
#define __COMPOSITE_SHAPE_ORACLE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

class PolyhedralBoundedSolid;

/** Axis-aligned box {x0, y0, z0, x1, y1, z1}: the Java `double[]`. */
class CompositeBox
{
public:
    double v[6];

    CompositeBox();
    CompositeBox(double x0, double y0, double z0, double x1, double y1,
        double z1);
    BOOLEAN operator==(const CompositeBox& other) const;
};

inline BOOLEAN
vsdkEquals(const CompositeBox &a, const CompositeBox &b)
{
    return a == b;
}

/**
Java `Predicate<boolean[]>` over the membership vector (one flag per
shape).
*/
typedef BOOLEAN (*CompositeMembership)(const BOOLEAN* member);

/**
Exact volumes of unions and differences of axis-aligned boxes under a rigid
motion, and of boolean expressions over several such shapes, by splitting
every shape into disjoint convex cells and adding the volumes of the
intersections of the cells (with the convex oracle of
`PolyhedralBoundedSolidVolumeOracle`).
C++ counterpart of Java's `CompositeShapeOracle` (test code).
*/
class CompositeShapeOracle
{
public:
    /** Axis-aligned boxes in the local frame. */
    class Shape
    {
    public:
        String name;
        ArrayList<CompositeBox> positive;
        ArrayList<CompositeBox> negative;
        Matrix4x4d frame;

        Shape();
        Shape(const String& name, const ArrayList<CompositeBox>& positive,
            const ArrayList<CompositeBox>& negative, const Matrix4x4d& frame);

        Shape moved(const Matrix4x4d& motion) const;

        /** Disjoint convex cells of the shape, in world coordinates. */
        ArrayList<PolyhedralBoundedSolidVolumeOracle::OrientedBox> cells()
            const;

        /**
        Builds the B-Rep with the kernel in the axis-aligned local frame (a
        union of the positive boxes minus the negative ones), then applies
        the rigid motion. Tests compare its volume with `volume()` before
        using it as an operand.
        @return a new solid, owned by the caller
        */
        PolyhedralBoundedSolid* toSolid() const;

        double volume() const;

        String toString() const;

        BOOLEAN operator==(const Shape& other) const;

    private:
        BOOLEAN insideLocal(const double* p) const;
        static BOOLEAN insideBox(const CompositeBox& b, const double* p);
        ArrayList<double> coordinates(int axis) const;
        static PolyhedralBoundedSolid* box(const CompositeBox& b);
    };

    enum { MAXIMUM_SHAPES = 8 };

    /**
    Exact volume of the points whose membership vector (one flag per shape)
    satisfies `expression`. The expression must be false when the point is
    in no shape. C++ port note: at most MAXIMUM_SHAPES shapes.
    */
    static double volume(const ArrayList<Shape>& shapes,
        CompositeMembership expression);

private:
    CompositeShapeOracle();
    static double intersectionVolume(
        const ArrayList<PolyhedralBoundedSolidVolumeOracle::OrientedBox>*
            cells, int count, int set);
    static double sumOverTuples(
        const ArrayList<PolyhedralBoundedSolidVolumeOracle::OrientedBox>**
            chosen, int chosenCount, int depth,
        const ArrayList<OracleHalfSpace>& planes);
};

inline BOOLEAN
vsdkEquals(const CompositeShapeOracle::Shape &a,
    const CompositeShapeOracle::Shape &b)
{
    return a == b;
}

#endif
