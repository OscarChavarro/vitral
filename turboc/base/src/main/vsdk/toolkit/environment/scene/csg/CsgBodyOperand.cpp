#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/SceneSolid.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/scene/csg/CsgSolidEquivalence.h"
#include "vsdk/toolkit/environment/scene/csg/CsgBodyOperand.h"

CsgBodyOperand::CsgBodyOperand(const SceneSolid& value) : body(value) {}
const Geometry* CsgBodyOperand::getGeometry() const { return body.getGeometry(); }
Matrix4x4d CsgBodyOperand::getAffinePlacement() const { return body.getAffinePlacement(); }

AffinePlacementStatus::Value CsgBodyOperand::queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const
{ return body.queryAffinePlacement(forward, inverse); }

GeometryContainment::Value CsgBodyOperand::classifyPointCertified(const Vector3Dd& point, double tolerance) const
{ return body.classifyPointCertified(point, tolerance); }

void CsgBodyOperand::getSolidBounds(GeometryBounds& result) const { body.getSolidWorldBounds(result); }

BOOLEAN CsgBodyOperand::isSameSolid(const CsgOperandView& other) const
{
    const CsgBodyOperand* bodyOperand=other.getBodyOperand();
    return bodyOperand && CsgSolidEquivalence::same(body,bodyOperand->body);
}

RaySolidQueryStatus::Value CsgBodyOperand::classifyRay(const Ray& ray, const RayCrossingQuery& query,
    RaySolidClassification& result) const
{ return body.classifyRay(ray,query,result); }

BOOLEAN CsgBodyOperand::isStructuralTraversalSafe() const { return body.isStructuralTraversalSafe(); }

BOOLEAN CsgBodyOperand::queryModificationVersion(long long& version) const
{ version=body.getModificationVersion(); return TRUE; }

const CsgBodyOperand* CsgBodyOperand::getBodyOperand() const { return this; }

const Class CsgBodyOperand::CLASS = { "CsgBodyOperand", &CsgOperandView::CLASS };

const Class *
CsgBodyOperand::getClass() const
{
    return &CLASS;
}
