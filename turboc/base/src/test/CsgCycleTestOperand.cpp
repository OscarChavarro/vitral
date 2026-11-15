#include "vsdk/toolkit/common/VSDK.h"
#include "CsgCycleTestOperand.h"
CsgOperation::Value CsgCycleTestOperand::getOperation() const { return CsgOperation::UNION; }
long int CsgCycleTestOperand::getOperandCount() const { return 1; }
// C++ port note: gcc 2.7.2 does not convert `this` to its second base class
// through `index==0 ? this : NULL` (the call through the result crashes)
const CsgOperandView* CsgCycleTestOperand::getOperand(long int index) const
{
    if (index==0) {
        const CsgOperandView* view=this;
        return view;
    }
    return NULL;
}
const Geometry* CsgCycleTestOperand::getGeometry() const { return NULL; }
Matrix4x4d CsgCycleTestOperand::getAffinePlacement() const { return Matrix4x4d::identityMatrix(); }
AffinePlacementStatus::Value CsgCycleTestOperand::queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const
{ forward=Matrix4x4d::identityMatrix(); inverse=forward; return AffinePlacementStatus::Valid; }
const CsgExpression* CsgCycleTestOperand::getChildExpression() const { return this; }

BOOLEAN CsgCycleTestOperand::isStructuralTraversalSafe() const { return TRUE; }
