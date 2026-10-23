//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___PBS_SET_OP_SECTOR_ON_SECTOR__
#define ___PBS_SET_OP_SECTOR_ON_SECTOR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

class _PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store sector / sector neigborhood information for the
vertex/vertex classifier as proposed on section [MANT1988].15.5. and program
[MANT1988].15.6.

C++ counterpart of Java's
`_PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector`.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector {
public:
    int secta;
    int sectb;
    int s1a;
    int s2a;
    int s1b;
    int s2b;
    BOOLEAN intersect;
    _PolyhedralBoundedSolidHalfEdge* hea;
    _PolyhedralBoundedSolidHalfEdge* heb;
    BOOLEAN wa;
    BOOLEAN wb;
    enum {
        ON = 0,
        OUT = 1,
        IN = -1
    };

    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnSector();

    void fillCases();

    String toString() const;

private:
    static const char* label(int i);
};

#endif
