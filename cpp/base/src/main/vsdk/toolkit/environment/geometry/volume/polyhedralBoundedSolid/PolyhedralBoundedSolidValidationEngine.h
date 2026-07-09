#ifndef __POLYHEDRAL_BOUNDED_SOLID_VALIDATION_ENGINE__
#define __POLYHEDRAL_BOUNDED_SOLID_VALIDATION_ENGINE__

class PolyhedralBoundedSolid;

class PolyhedralBoundedSolidValidationEngine {
public:
    static bool validateIntermediate(PolyhedralBoundedSolid* solid);
    static bool validateBooleanInputs(PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB);
    static bool validateStrict(PolyhedralBoundedSolid* solid);
    static bool areGeometricallyIdentical(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b);
};

#endif
