#include "vsdk/toolkit/common/VSDK.h"
#include "CsgPolicyTestOperand.h"
CsgPolicyTestOperand::CsgPolicyTestOperand(const CsgExpression& value, BOOLEAN invert)
    : expression(value), inverted(invert) {}
BOOLEAN CsgPolicyTestOperand::isInverted() const { return inverted; }
const Geometry* CsgPolicyTestOperand::getGeometry() const { return NULL; }
Matrix4x4d CsgPolicyTestOperand::getAffinePlacement() const { return Matrix4x4d::identityMatrix(); }
AffinePlacementStatus::Value CsgPolicyTestOperand::queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const
{ forward=Matrix4x4d::identityMatrix(); inverse=forward; return AffinePlacementStatus::Valid; }
const CsgExpression* CsgPolicyTestOperand::getChildExpression() const { return &expression; }
