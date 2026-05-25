#ifndef __VSDK_PBS_GEOMETRIC_VALIDATOR_H__
#define __VSDK_PBS_GEOMETRIC_VALIDATOR_H__

#include <string>
#include <vector>

#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class Vector3Dd;

class PolyhedralBoundedSolidGeometricValidator {
public:
    static bool validateFacePointsAreCoplanar(const std::vector<Vector3Dd>& points);
    static bool validateFacePointsAreCoplanar(const std::vector<Vector3Dd>& points, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static std::vector<Vector3Dd> extractPointsFromFace(_PolyhedralBoundedSolidFace* face);
    static bool validateFaceIsPlanar(_PolyhedralBoundedSolidFace* face);
    static bool validateFaceIsPlanar(_PolyhedralBoundedSolidFace* face, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static bool validateAllFacesPlanarityAndPlanes(PolyhedralBoundedSolid* solid, std::string* msg);
    static bool validateConsistentFaceOrientations(PolyhedralBoundedSolid* solid, std::string* msg);
    static bool validateLoopsStrict(PolyhedralBoundedSolid* solid, std::string* msg);
    static bool validateFaceIntersectionsStrict(PolyhedralBoundedSolid* solid, std::string* msg);
    static bool validateNoCoincidentVertices(PolyhedralBoundedSolid* solid, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context, std::string* msg);
    static bool validateUniqueFaceAndVertexIds(PolyhedralBoundedSolid* solid, std::string* msg);
};

#endif
