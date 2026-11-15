#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgPlacedExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpressionEquivalence.h"
CsgPlacedExpression::CsgPlacedExpression(const CsgExpression& expression) : child(expression), modificationVersion(0) {}
AffinePlacementStatus::Value CsgPlacedExpression::setAffinePlacement(const Matrix4x4d& forward, const Matrix4x4d& inverse)
{
    AffinePlacement candidate;
    const AffinePlacementStatus::Value status=candidate.assign(forward,inverse);
    if (status==AffinePlacementStatus::Valid) { placement=candidate; ++modificationVersion; }
    return status;
}
const Geometry* CsgPlacedExpression::getGeometry() const { return NULL; }
Matrix4x4d CsgPlacedExpression::getAffinePlacement() const { return placement.getForward(); }
AffinePlacementStatus::Value CsgPlacedExpression::queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const
{ forward=placement.getForward(); inverse=placement.getInverse(); return placement.getStatus(); }
const CsgExpression* CsgPlacedExpression::getChildExpression() const { return &child; }

BOOLEAN CsgPlacedExpression::isSameSolid(const CsgOperandView& other) const
{ return CsgExpressionEquivalence::same(*this,other); }

BOOLEAN CsgPlacedExpression::queryModificationVersion(long long& version) const
{ version=modificationVersion; return TRUE; }

BOOLEAN CsgPlacedExpression::isStructuralTraversalSafe() const { return getClass()==&CsgPlacedExpression::CLASS; }

const Class CsgPlacedExpression::CLASS = { "CsgPlacedExpression", &CsgOperandView::CLASS };

const Class *
CsgPlacedExpression::getClass() const
{
    return &CLASS;
}
