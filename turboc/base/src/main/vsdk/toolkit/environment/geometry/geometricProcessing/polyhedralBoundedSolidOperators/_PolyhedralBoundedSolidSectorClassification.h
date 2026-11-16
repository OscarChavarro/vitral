//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_SECTOR_CLASSIFICATION__
#define ___PBS_SECTOR_CLASSIFICATION__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidOperator.h"

class _PolyhedralBoundedSolidHalfEdge;

/**
One entry of the neighbourhood of a vertex classified against a plane, as
in section [MANT1988].14.5 and program [MANT1988].14.3: the splitter and the
vertex/face classifier of the set operations ([MANT1988].15.6.1, problem
[MANT1988].15.4) use the same entries and differ only in their
reclassification rules. Built by `_PolyhedralBoundedSolidVertexNeighborhood`.
*/
class _PolyhedralBoundedSolidSectorClassification
    : public _PolyhedralBoundedSolidOperator {
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
    int classification;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    BOOLEAN isWide;
    Vector3Dd position;
    int situation;

    _PolyhedralBoundedSolidSectorClassification() :
        sector(NULL), classification(ON), isWide(FALSE), position(),
        situation(UNDEFINED) {}
    virtual ~_PolyhedralBoundedSolidSectorClassification() {}
};

#endif
