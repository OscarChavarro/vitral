#ifndef __POLYHEDRALBOUNDEDSOLIDVALIDATIONENGINE__
#define __POLYHEDRALBOUNDEDSOLIDVALIDATIONENGINE__

class PolyhedralBoundedSolid;

class PolyhedralBoundedSolidValidationEngine {
public:
    static bool validateIntermediate(PolyhedralBoundedSolid* solid);
    static bool validateBooleanInputs(PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB);
    static bool validateStrict(PolyhedralBoundedSolid* solid);
    static bool areGeometricallyIdentical(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b);
};

#endif
