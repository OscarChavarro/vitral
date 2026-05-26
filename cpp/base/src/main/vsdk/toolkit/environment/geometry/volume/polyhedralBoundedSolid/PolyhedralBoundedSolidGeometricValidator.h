#ifndef __VSDK_PBS_GEOMETRIC_VALIDATOR_H__
#define __VSDK_PBS_GEOMETRIC_VALIDATOR_H__

#include "java/lang/String.h"
#include "java/lang/String.h"

#include "java/util/ArrayList.h"
#include "java/lang/String.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "java/lang/String.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class Vector3Dd;

class PolyhedralBoundedSolidGeometricValidator {
public:
    static bool validateFacePointsAreCoplanar(java::ArrayList<Vector3Dd>& points);
    static bool validateFacePointsAreCoplanar(java::ArrayList<Vector3Dd>& points, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static void extractPointsFromFace(_PolyhedralBoundedSolidFace* face, java::ArrayList<Vector3Dd>& outPoints);
    static bool validateFaceIsPlanar(_PolyhedralBoundedSolidFace* face);
    static bool validateFaceIsPlanar(_PolyhedralBoundedSolidFace* face, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static bool validateAllFacesPlanarityAndPlanes(PolyhedralBoundedSolid* solid, java::String* msg);
    static bool validateConsistentFaceOrientations(PolyhedralBoundedSolid* solid, java::String* msg);
    static bool validateLoopsStrict(PolyhedralBoundedSolid* solid, java::String* msg);
    static bool validateFaceIntersectionsStrict(PolyhedralBoundedSolid* solid, java::String* msg);
    static bool validateNoCoincidentVertices(PolyhedralBoundedSolid* solid, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& context, java::String* msg);
    static bool validateUniqueFaceAndVertexIds(PolyhedralBoundedSolid* solid, java::String* msg);
};

#endif
