//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_INTRR_VRLP__
#define ___PBS_INTRR_VRLP__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class InfinitePlane;
class PolyhedralBoundedSolid;
class PolyhedralBoundedSolidFaceGeometry;
class _PolyhedralBoundedSolidFace;

/**
Decides whether the interiors of two solids overlap: whether some point is
strictly inside both. The "no proper intersection" preflights of the set
operator ([MANT1988] 15.4, problem 15.1) and the "this result cannot be
empty" guard of `setOp` rest on this decision.

The decision follows from the boundaries, with no sampling of space
(plan-stage11 Phase D). If the interiors overlap in a region R, the
boundary of R is made of pieces of the two boundaries, so one of these
holds, and each of them is enough:
  - part of a face of one solid is strictly inside the other;
  - a face of one lies on a face of the other with the material of both on
    the same side (then the points just under the shared part are inside
    both).

Each face F of one solid is therefore cut by everything of the other
solid Y that meets its plane: the segments where the faces of Y cross that
plane and the edges of Y that lie in it. Together with the edges of F
these segments divide F into cells, and inside one cell the answer to "is
this point inside Y" does not change. Every cell has one of those segments
on its border, so a point beside the middle of every piece of every
segment, on both sides, visits every cell. When such a point is inside Y
or on its boundary, the point just under it, inside the solid of F, is
tried: if it is inside both solids it is the witness. The answer is true
only with a witness in hand.

The points stand at 4 tolerances from the segments and from the face,
so an overlap thinner than about 5 tolerances is not seen: at that size
the two boundaries are the same surface for the kernel, and the pair is
a contact.

It replaced a test by sampling: 27 probes in the common bounding box,
then a grid built from the vertex coordinates of both solids. That test
missed overlaps that fell between the samples (two prisms with coplanar
ends overlapping in a corner, InteriorOverlapPredicateTest) and its cost
grew with the cube of the number of vertices.

C++ counterpart of Java's `_PolyhedralBoundedSolidInteriorOverlap`.
*/
class _PolyhedralBoundedSolidInteriorOverlap
    : public _PolyhedralBoundedSolidOperator {
public:
    /**
    True when some point is strictly inside both solids. The numeric
    context must be the one of the pair (the caller sets it).
    */
    static BOOLEAN interiorsOverlap(PolyhedralBoundedSolid* solidA,
        PolyhedralBoundedSolid* solidB);

#ifdef __TURBOC__
public:
#else
private:
#endif
    typedef ArrayList<PolyhedralBoundedSolidFaceGeometry*> FaceGeometryList;

    /** A segment in the plane of the face being cut, in plane coordinates. */
    class Cut {
    public:
        double startX;
        double startY;
        double endX;
        double endY;

        Cut();
        Cut(double startX, double startY, double endX, double endY);
    };

    _PolyhedralBoundedSolidInteriorOverlap();

    static BOOLEAN someFaceCellIsInside(PolyhedralBoundedSolid* solid,
        const FaceGeometryList& faces,
        PolyhedralBoundedSolid* other,
        const FaceGeometryList& otherFaces);
    static void addEdges(_PolyhedralBoundedSolidFace* face,
        const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
        ArrayList<Cut>& cuts);
    static void addCutsOfFace(InfinitePlane* plane, const Vector3Dd& normal,
        const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
        _PolyhedralBoundedSolidFace* otherFace, InfinitePlane* otherPlane,
        ArrayList<Cut>& cuts);
    static void addCut(const Vector3Dd& p, const Vector3Dd& q,
        const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
        ArrayList<Cut>& cuts);
    static BOOLEAN someCellIsInside(_PolyhedralBoundedSolidFace* face,
        InfinitePlane* plane, const Vector3Dd& normal,
        const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
        const ArrayList<Cut>& cuts, PolyhedralBoundedSolid* solid,
        const FaceGeometryList& faces,
        PolyhedralBoundedSolid* other,
        const FaceGeometryList& otherFaces);
    static BOOLEAN isWitness(const Vector3Dd& point, const Vector3Dd& normal,
        double clearance, PolyhedralBoundedSolid* solid,
        const FaceGeometryList& faces,
        PolyhedralBoundedSolid* other,
        const FaceGeometryList& otherFaces);
    static void stopsAlong(const Cut& cut, long index,
        const ArrayList<Cut>& cuts, double eps, ArrayList<double>& sorted);
    static void addStopAt(double x, double y, const Cut& cut, double rx,
        double ry, double length, double length2, double eps,
        ArrayList<double>& stops);
    static BOOLEAN firstVertex(_PolyhedralBoundedSolidFace* face,
        Vector3Dd& out);
    static BOOLEAN unit(const Vector3Dd& vector, Vector3Dd& out);
    static BOOLEAN inPlaneAxis(const Vector3Dd& normal, Vector3Dd& out);
};

#endif
