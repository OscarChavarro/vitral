//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_SPLITTER__
#define ___POLYHEDRAL_BOUNDED_SOLID_SPLITTER__


#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class InfinitePlane;
class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidEdge;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidHalfEdge;
class _PolyhedralBoundedSolidVertex;

/**
This class is used to store vertex / halfedge neigborhood information, as presented
in section [MANT1988].14.5, and program [MANT1988].14.3.
*/
class _PolyhedralBoundedSolidSplitterSectorClassification : public _PolyhedralBoundedSolidOperator {
public:
    enum {
        ABOVE = 1,
        BELOW = -1,
        ON = 0
    };

    enum {
        COPLANAR_FACE = 10,
        INPLANE_EDGE = 20,
        CROSSING_EDGE = 30,
        UNDEFINED = 40
    };

    _PolyhedralBoundedSolidHalfEdge* sector;
    int cl;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    BOOLEAN isWide;
    Vector3Dd position;
    int situation;

    _PolyhedralBoundedSolidSplitterSectorClassification();

    String toString() const;
};

/**
Class `_PolyhedralBoundedSolidSplitterNullEdge` plays a role of a decorator
design patern for class `_PolyhedralBoundedSolidEdge`, and adds sort-ability.
*/
class _PolyhedralBoundedSolidSplitterNullEdge : public _PolyhedralBoundedSolidOperator {
private:
    static PolyhedralBoundedSolidNumericPolicy::ToleranceContext nullEdgeNumericContext;

public:
    _PolyhedralBoundedSolidEdge* e;

    /// The default (null edge) is needed by the storage of `ArrayList`
    explicit _PolyhedralBoundedSolidSplitterNullEdge(
        _PolyhedralBoundedSolidEdge* e = NULL);

    static void setNumericContext(
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
    from cutting the solid with the plane and resulting above the plane,
    similarly, `outSolidsBelow` will be appended with solid pieces
    resulting below the plane.

    Current macro-algorithm follows the strategy outlined on sections
    [MANT1988].14.1, [MANT1988].14.2 and [MANT1988].14.3 and program
    [MANT1988].14.1.

    C++ port note: the caller owns the solids appended to the lists. When
    the plane does not cut the solid, `inSolid` itself is appended to
    `outSolidsAbove` (as in Java); otherwise its faces, edges and vertices
    are moved to the new solids and `inSolid` is left empty.
    */
    static void split(PolyhedralBoundedSolid* inSolid,
                      const InfinitePlane& inSplittingPlane,
                      ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
                      ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow);

private:
    typedef _PolyhedralBoundedSolidSplitterSectorClassification SectorClassification;

    /**
    Following variable `soov` ("set of ON-vertices") from program [MANT1988].14.1.
    */
    static ArrayList<_PolyhedralBoundedSolidVertex*> soov;

    /**
    Following variable `sone` ("set of null edges") from program [MANT1988].14.1.
    */
    static ArrayList<_PolyhedralBoundedSolidSplitterNullEdge> sone;

    /**
    Following variable `sonf` ("set of null faces") from program [MANT1988].14.1.
    */
    static ArrayList<_PolyhedralBoundedSolidFace*> sonf;

    static ArrayList<_PolyhedralBoundedSolidFace*> facesToFixAbove;
    static ArrayList<_PolyhedralBoundedSolidFace*> facesToFixBelow;

    /**
    Following variable `ends` from program [MANT1988].14.9.
    */
    static ArrayList<_PolyhedralBoundedSolidHalfEdge*> ends;
    static ArrayList<_PolyhedralBoundedSolidHalfEdge*> tieds;

    static int compareToZero(double value);
    static void addsoov(_PolyhedralBoundedSolidVertex* v);
    static void splitGenerate(PolyhedralBoundedSolid* inSolid,
                              const InfinitePlane& inSplittingPlane);
    static ArrayList<SectorClassification> getNeighborhood(
        _PolyhedralBoundedSolidVertex* vtx,
        const InfinitePlane& inSplittingPlane);
    static BOOLEAN checkSplitterSectorWideness(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN inplaneEdgesOn(const ArrayList<SectorClassification>& nbr);
    static void reclassifyOnSectors(ArrayList<SectorClassification>& nbr,
                                    const InfinitePlane& inSplittingPlane);
    static void reclassifyOnEdges(ArrayList<SectorClassification>& nbr);
    static void insertNullEdges(ArrayList<SectorClassification>& nbr,
                                PolyhedralBoundedSolid* inSolid,
                                const InfinitePlane& inSplittingPlane);
    static void splitClassify(PolyhedralBoundedSolid* inSolid,
                              const InfinitePlane& inSplittingPlane);
    static _PolyhedralBoundedSolidHalfEdge* canJoin(_PolyhedralBoundedSolidHalfEdge* he);
    static void printNbr(const ArrayList<SectorClassification>& neighborSectorsInfo);
    static void printEnds();
    static void cut(_PolyhedralBoundedSolidHalfEdge* he);
    static BOOLEAN isLoose(_PolyhedralBoundedSolidHalfEdge* he);
    static void splitConnect();
    static void classify(PolyhedralBoundedSolid* s,
                         PolyhedralBoundedSolid* above,
                         PolyhedralBoundedSolid* below);
    static BOOLEAN isNullFace(_PolyhedralBoundedSolidFace* f);
    static void destroy(PolyhedralBoundedSolid* inSolid);
    static BOOLEAN faceInsideFace(_PolyhedralBoundedSolidFace* a,
                               _PolyhedralBoundedSolidFace* b);
    static void fixNullFaces(ArrayList<_PolyhedralBoundedSolidFace*>& l);
    static void splitFinish(PolyhedralBoundedSolid* inSolid,
                            ArrayList<PolyhedralBoundedSolid*>& outSolidsAbove,
                            ArrayList<PolyhedralBoundedSolid*>& outSolidsBelow);
};

#endif
