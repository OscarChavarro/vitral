//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_SET_OP_SECTOR_ON_FACE__
#define ___PBS_SET_OP_SECTOR_ON_FACE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class InfinitePlane;
class _PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/face classifier, in a similar fashion to as presented in section
[MANT1988].14.5, and program [MANT1988].14.3., but biased for the set
operation algorithm as proposed on section [MANT1988].15..1. and problem
[MANT1988].15.4.

C++ counterpart of Java's
`_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace`. It derives
from the operator family, as in Java, because `updateLabel` uses its
numeric context.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
    : public _PolyhedralBoundedSolidOperator {
public:
    enum {
        ABOVE = 1,
        BELOW = -1,
        ON = 0
    };

    enum {
        AinB = 11,
        AoutB = 12,
        BinA = 13,
        BoutA = 14,
        AonBplus = 15,
        AonBminus = 16,
        BonAplus = 17,
        BonAminus = 18
    };

    enum {
        COPLANAR_FACE = 10,
        INPLANE_EDGE = 20,
        CROSSING_EDGE = 30,
        UNDEFINED = 40
    };

    enum {
        COPLANAR_UNKNOWN = 0,
        COPLANAR_DISJOINT = 1,
        COPLANAR_TOUCHING = 2,
        COPLANAR_OVERLAP = 3
    };

    _PolyhedralBoundedSolidHalfEdge* sector;
    /** Referenced, not owned */
    InfinitePlane* referencePlane;
    int cl;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    BOOLEAN isWide;
    Vector3Dd position;
    int situation;
    BOOLEAN reverse;
    int coplanarRelation;

    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the edge reclassification rules for the third stage of a vertex/face
    classifier.
    */
    void applyRules(int op);

    void updateLabel(int BvsA);

    String toString() const;
};

#endif
