#ifndef __PBS_VOLUME_ORACLE__
#define __PBS_VOLUME_ORACLE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class PolyhedralBoundedSolid;

/**
Plane {nx, ny, nz, d} meaning n.x <= d, n unit outward (the Java
`double[]` of four entries).
*/
class OracleHalfSpace
{
public:
    double v[4];

    OracleHalfSpace();
    OracleHalfSpace(double nx, double ny, double nz, double d);
    BOOLEAN operator==(const OracleHalfSpace& other) const;
};

inline BOOLEAN
vsdkEquals(const OracleHalfSpace &a, const OracleHalfSpace &b)
{
    return a == b;
}

/**
Independent test oracle for boolean and split results: it knows nothing
about the half-edge pipeline and only uses (a) the divergence-theorem volume
of a B-Rep and (b) the exact volume of an intersection of convex half-space
sets.

For two convex operands A and B with intersection volume I:
vol(A u B) = vol(A) + vol(B) - I, vol(A n B) = I, vol(A \ B) = vol(A) - I.
This turns every box/box (or convex/convex) boolean into a test with a
known numeric answer, independent of topology counts, which is the missing
complement of the fixture-snapshot tests (faces/edges/vertices/bbox).

Traceability: the B-Rep volume is the integral property of [MANT1988]
Sec. 13.3 (Program 13.9 avolume), generalized to faces with rings.
C++ counterpart of Java's `PolyhedralBoundedSolidVolumeOracle` (test code).
*/
class PolyhedralBoundedSolidVolumeOracle
{
public:
    /** One oriented box: half-space set plus the recipe to build its B-Rep. */
    class OrientedBox
    {
    public:
        double size[3];
        Matrix4x4d transform;

        OrientedBox();
        OrientedBox(double sx, double sy, double sz,
            const Matrix4x4d& transform);

        /** New solid, owned by the caller. */
        PolyhedralBoundedSolid* toSolid() const;

        /** Planes as {nx, ny, nz, d} meaning n.x <= d, n unit outward. */
        ArrayList<OracleHalfSpace> halfSpaces() const;

        double volume() const;
    };

    static Matrix4x4d translation(double x, double y, double z);
    static Matrix4x4d rotation(double angle, double x, double y, double z);
    static double intersectionVolume(const OrientedBox& a,
        const OrientedBox& b);

    /** Expected volume for op in {UNION, INTERSECTION, SUBTRACT} (A op B). */
    static double expectedVolume(const OrientedBox& a, const OrientedBox& b,
        int op);

    /**
    Volume of the convex polytope { x : n_k.x <= d_k }: each plane is
    clipped against all others (Sutherland-Hodgman) and the face areas are
    accumulated with the divergence theorem. Duplicate planes are merged.
    */
    static double convexVolume(const ArrayList<OracleHalfSpace>& input);

    /**
    Unsigned volume of a B-Rep by the divergence theorem. Rings contribute
    with their own (opposite) orientation, so faces with holes need no
    triangulation. Zero-area laminae contribute nothing, which is why a
    volume check alone is not sufficient and must be paired with strict
    validation.
    */
    static double solidVolume(PolyhedralBoundedSolid* solid);

private:
    PolyhedralBoundedSolidVolumeOracle();
};

inline BOOLEAN
vsdkEquals(const PolyhedralBoundedSolidVolumeOracle::OrientedBox &a,
    const PolyhedralBoundedSolidVolumeOracle::OrientedBox &b)
{
    return &a == &b;
}

#endif
