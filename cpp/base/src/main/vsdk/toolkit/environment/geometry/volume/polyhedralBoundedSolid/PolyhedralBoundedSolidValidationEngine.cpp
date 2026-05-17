#include "PolyhedralBoundedSolidValidationEngine.h"

#include "PolyhedralBoundedSolid.h"
#include "PolyhedralBoundedSolidGeometricValidator.h"
#include <cmath>

bool PolyhedralBoundedSolidValidationEngine::validateIntermediate(PolyhedralBoundedSolid* solid)
{
    if ( solid == 0 ) return false;
    bool ok = PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(solid, 0);
    solid->setValidationState(ok);
    return ok;
}

bool PolyhedralBoundedSolidValidationEngine::validateBooleanInputs(PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB)
{
    return validateIntermediate(solidA) && validateIntermediate(solidB);
}

bool PolyhedralBoundedSolidValidationEngine::validateStrict(PolyhedralBoundedSolid* solid)
{
    return validateIntermediate(solid);
}

bool PolyhedralBoundedSolidValidationEngine::areGeometricallyIdentical(PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b)
{
    if ( a == 0 || b == 0 ) return false;
    double* ma = a->getMinMax();
    double* mb = b->getMinMax();
    bool same = true;
    for (int i = 0; i < 6; ++i) {
        if ( std::abs(ma[i] - mb[i]) > 1e-9 ) { same = false; break; }
    }
    delete[] ma;
    delete[] mb;
    return same;
}
