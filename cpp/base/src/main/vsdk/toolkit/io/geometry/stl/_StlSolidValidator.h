#ifndef __STL_SOLID_VALIDATOR__
#define __STL_SOLID_VALIDATOR__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"

class PolyhedralBoundedSolid;
class _PolyhedralBoundedSolidFace;
class _PolyhedralBoundedSolidLoop;

/**
Checks that a solid can be exported to STL. Internal collaborator of
`StlWriter`.
@throws java::IllegalStateException when it cannot
*/
class _StlSolidValidator {
public:
    static void validate(PolyhedralBoundedSolid* solid);

private:
    _StlSolidValidator() {}

    static void validateNoDegenerateEdges(
        PolyhedralBoundedSolid* solid,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static void validateFacesHaveTriangulableLoops(
        PolyhedralBoundedSolid* solid,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static void validateFaceLoops(
        _PolyhedralBoundedSolidFace* face,
        const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext);
    static java::ArrayList<Vector3Dd> collectLoopPoints(
        _PolyhedralBoundedSolidFace* face,
        _PolyhedralBoundedSolidLoop* loop,
        int loopIndex);
    static double projectedLoopAreaMagnitude(
        const java::ArrayList<Vector3Dd>& points,
        _PolyhedralBoundedSolidFace* face);
    static Vector3Dd chooseReferenceAxis(const Vector3Dd& normal);
    static java::String buildLoopDiagnostics(
        _PolyhedralBoundedSolidFace* face,
        _PolyhedralBoundedSolidLoop* loop,
        int loopIndex,
        double projectedArea,
        double areaTolerance);
};

#endif
