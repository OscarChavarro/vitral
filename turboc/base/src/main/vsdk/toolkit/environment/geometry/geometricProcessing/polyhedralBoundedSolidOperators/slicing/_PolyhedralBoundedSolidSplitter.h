//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PLYHDRL_BNDD_SLD_SPLTTR__
#define ___PLYHDRL_BNDD_SLD_SPLTTR__


#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSectorClassification.h"

class InfinitePlane;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidLoop;
class _PolyhedralBoundedSolidVertex;

/**
This class is used to store vertex / halfedge neigborhood information, as presented
in section [MANT1988].14.5, and program [MANT1988].14.3.
*/
class _PBSSplitContext;

class _PolyhedralBoundedSolidSplitterSectorClassification
    : public _PolyhedralBoundedSolidSectorClassification {
public:
    _PolyhedralBoundedSolidSplitterSectorClassification();

    /**
    C++ port note: converts an entry made by
    `_PolyhedralBoundedSolidVertexNeighborhood::classify` (Java builds the
    entries of this type there through a factory).
    */
    explicit _PolyhedralBoundedSolidSplitterSectorClassification(
        const _PolyhedralBoundedSolidSectorClassification& other);

    String toString() const;
};

/**
Class `_PolyhedralBoundedSolidSplitterNullEdge` plays a role of a decorator
design patern for class `_PolyhedralBoundedSolidEdge`, and adds sort-ability.
*/
class _PolyhedralBoundedSolidSplitterNullEdge : public _PolyhedralBoundedSolidOperator {
private:
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext;

public:
    _PolyhedralBoundedSolidEdge* e;

    /// The default (null edge) is needed by the storage of `ArrayList`
    _PolyhedralBoundedSolidSplitterNullEdge();

    /**
    @param context tolerances of the split, or null for the default ones
    */
    _PolyhedralBoundedSolidSplitterNullEdge(_PolyhedralBoundedSolidEdge* e,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext* context);

    /**
    As Java `compareTo`: orders by the position of the starting vertex of
    the right half edge. It never answers 0.
    */
    int compareTo(const _PolyhedralBoundedSolidSplitterNullEdge& other) const;
};

/**
This is a utility class containing operations for implementing the boundary
representation split methods over winged-edge data structures, as presented
at chapter [MANT1988].14.

This class offers just one public method, which is supposed to be called
from GeometricModeler class.

C++ counterpart of Java's `_PolyhedralBoundedSolidSplitter`.
*/
class _PolyhedralBoundedSolidSplitter : public _PolyhedralBoundedSolidOperator {
public:
    /**
    Given the input `inSolid` and the cutting plane `inSplittingPlane`,
    this method appends to the `outSolidsAbove` list the solids resulting
    from cutting the solid with the plane and resulting above the plane
    (positive signed distance, on the side the plane normal points to),
    similarly, `outSolidsBelow` will be appended with solid pieces
    resulting below the plane.

    At least one solid is appended to each list. Disjoint pieces on one
    side stay together as shells of one solid; pieces that touch at a
    vertex or along an edge come out as separate solids. When the plane
    does not cut the solid (it misses it, touches it, or contains one of
    its faces), a copy of the solid goes to the list of its side and an
    empty solid to the other one.

    `inSolid` is not modified: the algorithm runs on a structural copy
    (Fable5.2Analysis.md Stage E), and the output lists never receive the
    input object.

    Current macro-algorithm follows the strategy outlined on sections
    [MANT1988].14.1, [MANT1988].14.2 and [MANT1988].14.3 and program
    [MANT1988].14.1.

    C++ port note: the caller owns the solids appended to the lists.
    */
    static void split(PolyhedralBoundedSolid* inSolid,
                      const InfinitePlane& inSplittingPlane,
                      ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
                      ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow);

private:
    typedef _PolyhedralBoundedSolidSplitterSectorClassification SectorClassification;

    /**
    Working state of one split call. The global variables of program
    [MANT1988].14.1 live here instead of in static fields, so that split is
    reentrant and leaves nothing behind (Fable5.2Analysis.md Stage E).
    */
    typedef _PBSSplitContext _SplitContext;

    static int compareToZero(double value);
    static void addsoov(_SplitContext& ctx, _PolyhedralBoundedSolidVertex* v);
    static void splitGenerate(_SplitContext& ctx,
                              PolyhedralBoundedSolid* inSolid,
                              const InfinitePlane& inSplittingPlane);
    static ArrayList<SectorClassification> getNeighborhood(
        _PolyhedralBoundedSolidVertex* vtx,
        const InfinitePlane& inSplittingPlane);
    static void reclassifyOnSectors(ArrayList<SectorClassification>& nbr,
                                    const InfinitePlane& inSplittingPlane);
    static void reclassifyOnEdges(ArrayList<SectorClassification>& nbr);
    static BOOLEAN isConvexEdge(_PolyhedralBoundedSolidHalfEdge* he);
    static void insertNullEdges(_SplitContext& ctx,
                                ArrayList<SectorClassification>& nbr,
                                PolyhedralBoundedSolid* inSolid,
                                const InfinitePlane& inSplittingPlane);
    static void splitClassify(_SplitContext& ctx,
                              PolyhedralBoundedSolid* inSolid,
                              const InfinitePlane& inSplittingPlane);
    static _PolyhedralBoundedSolidHalfEdge* canJoin(_SplitContext& ctx,
        _PolyhedralBoundedSolidHalfEdge* he);
    static void printNbr(const ArrayList<SectorClassification>& neighborSectorsInfo);
    static void printEnds(_SplitContext& ctx);
    static void cut(_SplitContext& ctx, _PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN isLoose(_SplitContext& ctx,
                           _PolyhedralBoundedSolidHalfEdge* he);
    static void splitConnect(_SplitContext& ctx);
    static void orientStrut(_SplitContext& ctx,
                            _PolyhedralBoundedSolidEdge* strut);
    static double ccwAngle(const Vector3Dd& a, const Vector3Dd& b,
                           const Vector3Dd& n);
    static void classify(_SplitContext& ctx,
                         PolyhedralBoundedSolid* above,
                         PolyhedralBoundedSolid* below);
    static BOOLEAN faceInsideFace(_PolyhedralBoundedSolidFace* a,
                               _PolyhedralBoundedSolidFace* b);
    static void fixNullFaces(ArrayList<_PolyhedralBoundedSolidFace*>& l);
    static void splitFinish(_SplitContext& ctx,
                            PolyhedralBoundedSolid* inSolid,
                            const InfinitePlane& inSplittingPlane,
                            ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
                            ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow);
    static void resolvePinchedLoops(PolyhedralBoundedSolid* piece);
    static BOOLEAN resolveOnePinch(PolyhedralBoundedSolid* piece,
                                   _PolyhedralBoundedSolidLoop* loop);
    static BOOLEAN rotatesInto(_PolyhedralBoundedSolidHalfEdge* from,
                               _PolyhedralBoundedSolidHalfEdge* to);
    static void separateShells(PolyhedralBoundedSolid* piece,
                               ArrayList<PolyhedralBoundedSolid*>& shells);
    static BOOLEAN hasTouchingShells(PolyhedralBoundedSolid* solid);
    static void updateMaximumIds(PolyhedralBoundedSolid* solid);
    static BOOLEAN straddles(PolyhedralBoundedSolid* solid,
                             const InfinitePlane& plane);
    static int sideOf(PolyhedralBoundedSolid* piece,
                      const InfinitePlane& plane);
    static void discard(PolyhedralBoundedSolid* emptied);
};

#endif
