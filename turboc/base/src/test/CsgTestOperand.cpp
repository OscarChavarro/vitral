#include "vsdk/toolkit/common/VSDK.h"
#include "CsgTestOperand.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
CsgTestOperand::CsgTestOperand(GeometryContainment::Value value) : result(value), smallerResult(value), switchLimit(-1), malformedRay(FALSE) {}
CsgTestOperand::CsgTestOperand(GeometryContainment::Value value, GeometryContainment::Value smaller, double limit)
    : result(value), smallerResult(smaller), switchLimit(limit), malformedRay(FALSE) {}
const Geometry* CsgTestOperand::getGeometry() const { return NULL; }
Matrix4x4d CsgTestOperand::getAffinePlacement() const { return Matrix4x4d::identityMatrix(); }
GeometryContainment::Value CsgTestOperand::classifyPointCertified(const Vector3Dd&, double tolerance) const
{ return tolerance<=switchLimit ? smallerResult : result; }

void CsgTestOperand::setMalformedRay(BOOLEAN value) { malformedRay=value; }
RaySolidQueryStatus::Value CsgTestOperand::classifyRay(const Ray&, const RayCrossingQuery&, RaySolidClassification& result) const
{ result.clear(); return malformedRay ? RaySolidQueryStatus::Supported : RaySolidQueryStatus::Unsupported; }
