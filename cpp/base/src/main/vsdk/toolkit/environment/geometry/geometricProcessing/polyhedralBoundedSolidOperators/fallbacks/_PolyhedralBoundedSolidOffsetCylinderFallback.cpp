#include <algorithm>
#include <cmath>
#include <exception>
#include <string>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperator.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_SetOperationTrace.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidFallbackGeometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fallbacks/_PolyhedralBoundedSolidOffsetCylinderFallback.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidOffsetCylinderFallback CylinderFallback;
typedef _PolyhedralBoundedSolidFallbackGeometry FallbackGeometry;

namespace {

void addUniqueXy(std::vector<Vector3Dd>& values, const Vector3Dd& point)
{
    for ( size_t i = 0; i < values.size(); i++ ) {
        if ( FallbackGeometry::sameCoordinate(values[i].x(), point.x()) &&
             FallbackGeometry::sameCoordinate(values[i].y(), point.y()) ) {
            return;
        }
    }
    values.push_back(point);
}

}

bool CylinderFallback::describeVerticalCylinder(PolyhedralBoundedSolid* solid,
    VerticalCylinderOperandSpec& outSpec)
{
    double centerX;
    double centerY;
    double radius;
    std::vector<double> zs;
    std::vector<Vector3Dd> xy;
    long int i;

    if ( solid == 0 || solid->getVerticesList().size() < 6 ) {
        return false;
    }

    double* bounds = solid->getMinMax();
    double b[6];
    for ( i = 0; i < 6; i++ ) {
        b[i] = bounds[i];
    }
    delete[] bounds;
    if ( b[5] <= b[2] + numericContext.bigEpsilon() ) {
        return false;
    }

    centerX = (b[0] + b[3]) * 0.5;
    centerY = (b[1] + b[4]) * 0.5;
    radius = 0.0;
    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        Vector3Dd p = solid->getVerticesList().get(i)->position;
        double radialDistance;

        FallbackGeometry::addUniqueCoordinate(zs, p.z());
        addUniqueXy(xy, p);
        radialDistance = std::sqrt(
            (p.x() - centerX) * (p.x() - centerX) +
            (p.y() - centerY) * (p.y() - centerY));
        if ( radialDistance > radius ) {
            radius = radialDistance;
        }
    }

    if ( xy.size() < 3 || zs.size() < 2 ||
         radius <= numericContext.bigEpsilon() ) {
        return false;
    }

    for ( i = 0; i < solid->getVerticesList().size(); i++ ) {
        Vector3Dd p = solid->getVerticesList().get(i)->position;
        double radialDistance = std::sqrt(
            (p.x() - centerX) * (p.x() - centerX) +
            (p.y() - centerY) * (p.y() - centerY));

        if ( std::fabs(radialDistance - radius) >
             std::max(numericContext.bigEpsilon(), radius * 1.0e-6) ) {
            return false;
        }
    }

    outSpec.centerX = centerX;
    outSpec.centerY = centerY;
    outSpec.zMin = b[2];
    outSpec.radius = radius;
    outSpec.height = b[5] - b[2];
    outSpec.radialDivisions = (int)xy.size();
    outSpec.heightDivisions = std::max(1, (int)zs.size() - 1);
    return true;
}

CylinderFallback::OffsetCylinderDifferenceFallbackSpec*
CylinderFallback::prepareOffsetCylinderDifferenceFallbackSpec(
    PolyhedralBoundedSolid* inSolidA,
    PolyhedralBoundedSolid* inSolidB,
    int op)
{
    VerticalCylinderOperandSpec operandA;
    VerticalCylinderOperandSpec operandB;
    double centerDistance;

    if ( op != SUBTRACT ) {
        return 0;
    }

    if ( !describeVerticalCylinder(inSolidA, operandA) ||
         !describeVerticalCylinder(inSolidB, operandB) ) {
        return 0;
    }

    if ( operandA.radialDivisions == operandB.radialDivisions ) {
        return 0;
    }
    if ( !FallbackGeometry::sameCoordinate(operandA.radius, operandB.radius) ||
         !FallbackGeometry::sameCoordinate(operandA.height, operandB.height) ) {
        return 0;
    }

    centerDistance = std::sqrt(
        (operandA.centerX - operandB.centerX) *
        (operandA.centerX - operandB.centerX) +
        (operandA.centerY - operandB.centerY) *
        (operandA.centerY - operandB.centerY));
    if ( centerDistance <= numericContext.bigEpsilon() ||
         centerDistance >= operandA.radius + operandB.radius -
             numericContext.bigEpsilon() ) {
        return 0;
    }
    if ( operandA.zMin >= operandB.zMin + operandB.height -
             numericContext.bigEpsilon() ||
         operandB.zMin >= operandA.zMin + operandA.height -
             numericContext.bigEpsilon() ) {
        return 0;
    }

    OffsetCylinderDifferenceFallbackSpec* spec =
        new OffsetCylinderDifferenceFallbackSpec();
    spec->operandA = operandA;
    spec->operandB = operandB;
    return spec;
}

PolyhedralBoundedSolid* CylinderFallback::createFallbackCylinder(
    const VerticalCylinderOperandSpec& spec,
    int radialDivisions,
    int heightDivisions)
{
    Cone cone(spec.radius, spec.radius, spec.height);
    PolyhedralBoundedSolid* cylinder =
        cone.exportToPolyhedralBoundedSolid(radialDivisions, heightDivisions);
    Matrix4x4d translation;
    translation = translation.translation(spec.centerX, spec.centerY, spec.zMin);
    PolyhedralBoundedSolidModeler::applyTransformation(cylinder, translation);
    return cylinder;
}

PolyhedralBoundedSolid* CylinderFallback::buildOffsetCylinderDifferenceFallback(
    OffsetCylinderDifferenceFallbackSpec* spec)
{
    PolyhedralBoundedSolid* result = 0;

    if ( spec == 0 ) {
        return 0;
    }

    int radialDivisions = std::max(spec->operandA.radialDivisions,
        spec->operandB.radialDivisions);
    int heightDivisions = std::max(spec->operandA.heightDivisions,
        spec->operandB.heightDivisions);
    PolyhedralBoundedSolid* fallbackA = createFallbackCylinder(spec->operandA,
        radialDivisions, heightDivisions);
    PolyhedralBoundedSolid* fallbackB = createFallbackCylinder(spec->operandB,
        radialDivisions, heightDivisions);

    try {
        result = _PolyhedralBoundedSolidSetOperator::setOp(fallbackA, fallbackB,
            SUBTRACT, false, true);
    }
    catch ( const std::exception& e ) {
        _SetOperationTrace::tracePipelineSummary(
            (std::string("offset cylinder fallback failed: ") + e.what()).c_str());
        result = 0;
    }
    delete fallbackA;
    delete fallbackB;
    if ( result == 0 ) {
        return 0;
    }
    if ( !_PolyhedralBoundedSolidSetOperator::isStructurallyUsableSetOpResult(result) ) {
        _SetOperationTrace::tracePipelineSummary("offset cylinder fallback rejected");
        delete result;
        return 0;
    }
    _SetOperationTrace::tracePipelineSummary(
        ("offset cylinder fallback accepted faces=" +
         std::to_string(result->getPolygonsList().size()) +
         " edges=" + std::to_string(result->getEdgesList().size()) +
         " vertices=" + std::to_string(result->getVerticesList().size())).c_str());
    return result;
}
