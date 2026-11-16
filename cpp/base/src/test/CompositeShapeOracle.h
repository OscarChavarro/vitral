#ifndef __COMPOSITE_SHAPE_ORACLE__
#define __COMPOSITE_SHAPE_ORACLE__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"

class PolyhedralBoundedSolid;

/** Axis-aligned box {x0, y0, z0, x1, y1, z1}: the Java `double[6]`. */
class CompositeBox
{
public:
    double v[6];

    CompositeBox();
    CompositeBox(double x0, double y0, double z0, double x1, double y1,
        double z1);
    double& operator[](int i);
    double operator[](int i) const;
    bool operator==(const CompositeBox& other) const;
};

/**
Exact volume oracle for non-convex operands (Fable5.2Analysis.md Section 8,
"further tests worth writing"). A shape is a union of boxes minus a union
of boxes, all axis-aligned in the shape's own frame, followed by a rigid
motion: L, U and stepped blocks, and a frame with a through hole (genus 1).

The oracle never asks the kernel. Each shape is cut into disjoint
convex cells along the coordinates of its boxes; a cell belongs to the
shape when its centre is in some positive box and in no negative box. The
volume of the points that are in exactly the shapes of a set P is
obtained by inclusion-exclusion from the volumes of the intersections of
shapes, and each intersection is a sum over tuples of cells of the volume
of a convex polytope (`PolyhedralBoundedSolidVolumeOracle::convexVolume`).
So the oracle is exact for any boolean expression of up to a few shapes,
chains included.

Traceability: [MANT1988] Sec. 13.3 (volume as integral property),
Appendix B (regularised set operations).
C++ counterpart of Java's `CompositeShapeOracle` (test code). The Java
`Predicate<boolean[]>` is a function pointer over the membership flags.
*/
class CompositeShapeOracle
{
public:
    typedef bool (*Membership)(const bool* member);

    /** Axis-aligned boxes {x0, y0, z0, x1, y1, z1} in the local frame. */
    class Shape
    {
    public:
        java::String name;
        java::ArrayList<CompositeBox> positive;
        java::ArrayList<CompositeBox> negative;
        Matrix4x4d frame;

        Shape();
        Shape(const java::String& name,
            const java::ArrayList<CompositeBox>& positive,
            const java::ArrayList<CompositeBox>& negative,
            const Matrix4x4d& frame);
        bool operator==(const Shape& other) const;

        Shape moved(const Matrix4x4d& motion) const;

        /** Disjoint convex cells of the shape, in world coordinates. */
        java::ArrayList<PolyhedralBoundedSolidVolumeOracle::OrientedBox>
            cells() const;

        /**
        Builds the B-Rep with the kernel in the axis-aligned local frame (a
        union of the positive boxes minus the negative ones), then applies
        the rigid motion. Tests compare its volume with `volume()` before
        using it as an operand. New solid, owned by the caller.
        */
        PolyhedralBoundedSolid* toSolid() const;

        double volume() const;

        java::String toString() const;

    private:
        bool insideLocal(const double* p) const;
        static bool insideBox(const CompositeBox& b, const double* p);
        java::ArrayList<double> coordinates(int axis) const;
        static PolyhedralBoundedSolid* box(const CompositeBox& b);
    };

    /**
    Exact volume of the points whose membership vector (one flag per shape)
    satisfies `expression`. The expression must be false when the point is
    in no shape.
    */
    static double volume(const java::ArrayList<Shape>& shapes,
        Membership expression);

private:
    CompositeShapeOracle();
    static double intersectionVolume(
        const java::ArrayList<java::ArrayList<
            PolyhedralBoundedSolidVolumeOracle::OrientedBox> >& cells,
        int set);
    static double sumOverTuples(
        const java::ArrayList<java::ArrayList<
            PolyhedralBoundedSolidVolumeOracle::OrientedBox> >& chosen,
        int depth, const java::ArrayList<OracleHalfSpace>& planes);
};

#endif
