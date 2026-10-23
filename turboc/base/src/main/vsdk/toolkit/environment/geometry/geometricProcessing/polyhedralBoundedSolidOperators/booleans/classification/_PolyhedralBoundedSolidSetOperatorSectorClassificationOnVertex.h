//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_SET_OP_SECTOR_ON_VERTEX__
#define ___PBS_SET_OP_SECTOR_ON_VERTEX__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"

class _PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.

C++ counterpart of Java's
`_PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex`. C++ port
note: the reference frame (`referenceLine`, `referenceU`, `referenceV`) is
null in Java until assigned; here `hasReferenceFrame` tells if it is set.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex {
public:
    _PolyhedralBoundedSolidHalfEdge* he;
    Vector3Dd ref1;
    Vector3Dd ref2;
    Vector3Dd ref12;
    BOOLEAN hasReferenceFrame;
    Vector3Dd referenceLine;
    Vector3Dd referenceU;
    Vector3Dd referenceV;
    BOOLEAN wide;

    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex();

    double getAngle() const;

    String toString() const;

    int compareTo(const _PolyhedralBoundedSolidSetOperatorSectorClassificationOnVertex& other) const;
};

#endif
