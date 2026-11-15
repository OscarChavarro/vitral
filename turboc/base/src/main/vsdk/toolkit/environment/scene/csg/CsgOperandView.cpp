#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/RaySolidClassification.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"

CsgOperandView::~CsgOperandView() {}
AffinePlacementStatus::Value CsgOperandView::queryAffinePlacement(Matrix4x4d&, Matrix4x4d&) const
{ return AffinePlacementStatus::Indeterminate; }
// C++ port note: the cpp version cross-casts its geometry to an expression
// (dynamic_cast), that legacy compilers lack (no RTTI). No geometry of the
// toolkit is an expression, so this gives NULL as the cpp version does for
// all of them; a view whose geometry is an expression overrides this.
const CsgExpression* CsgOperandView::getChildExpression() const
{
    return NULL;
}

const CsgBodyOperand* CsgOperandView::getBodyOperand() const
{
    return NULL;
}

GeometryContainment::Value CsgOperandView::classifyPointCertified(const Vector3Dd&, double) const
{ return GeometryContainment::Unsupported; }

BOOLEAN CsgOperandView::isSameSolid(const CsgOperandView& other) const
{ return this==&other; }

RaySolidQueryStatus::Value CsgOperandView::classifyRay(const Ray&, const RayCrossingQuery&, RaySolidClassification& result) const
{ result.clear(); return RaySolidQueryStatus::Unsupported; }

BOOLEAN CsgOperandView::isStructuralTraversalSafe() const { return FALSE; }

BOOLEAN CsgOperandView::queryModificationVersion(long long&) const { return FALSE; }

void CsgOperandView::getSolidBounds(GeometryBounds& result) const { result.setUnsupported(); }

const Class CsgOperandView::CLASS = { "CsgOperandView", NULL };

const Class *
CsgOperandView::getClass() const
{
    return &CLASS;
}
