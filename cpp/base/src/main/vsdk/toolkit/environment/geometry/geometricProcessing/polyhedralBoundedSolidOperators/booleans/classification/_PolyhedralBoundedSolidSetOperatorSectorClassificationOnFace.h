//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#ifndef ___POLYHEDRAL_BOUNDED_SOLID_SET_OPERATOR_SECTOR_CLASSIFICATION_ON_FACE__
#define ___POLYHEDRAL_BOUNDED_SOLID_SET_OPERATOR_SECTOR_CLASSIFICATION_ON_FACE__

#include "java/lang/String.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/_PolyhedralBoundedSolidSectorClassification.h"

class InfinitePlane;
class _PolyhedralBoundedSolidHalfEdge;

/**
This class is used to store vertex / halfedge neigborhood information for the
vertex/face classifier, in a similar fashion to as presented in section
[MANT1988].14.5, and program [MANT1988].14.3., but biased for the set
operation algorithm as proposed on section [MANT1988].15..1. and problem
[MANT1988].15.4.

C++ counterpart of Java's
`_PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace`. The Java
copy constructor is the implicit C++ one.
*/
class _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace
    : public _PolyhedralBoundedSolidSectorClassification {
public:

    static const int AinB = 11;
    static const int AoutB = 12;
    static const int BinA = 13;
    static const int BoutA = 14;
    static const int AonBplus = 15;
    static const int AonBminus = 16;
    static const int BonAplus = 17;
    static const int BonAminus = 18;

    static const int COPLANAR_UNKNOWN = 0;
    static const int COPLANAR_DISJOINT = 1;
    static const int COPLANAR_TOUCHING = 2;
    static const int COPLANAR_OVERLAP = 3;

    /** Referenced, not owned */
    InfinitePlane* referencePlane;

    // Following attributes are not taken from [MANT1988], and all operations
    // on them are fine tunning options aditional to original algorithm.
    bool reverse;
    int coplanarRelation;

    _PolyhedralBoundedSolidSetOperatorSectorClassificationOnFace();

    /**
    Current method implements the set of changes from table [MANT1988].15.3.
    for the edge reclassification rules for the third stage of a vertex/face
    classifier.
    */
    void applyRules(int op);

    void updateLabel(int BvsA);

    java::String toString() const;
};

#endif
