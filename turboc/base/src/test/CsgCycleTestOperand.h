#ifndef __CSG_CYCLE_TEST_OPERAND__
#define __CSG_CYCLE_TEST_OPERAND__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
class CsgCycleTestOperand : public CsgExpression, public CsgOperandView {
public:
    virtual CsgOperation::Value getOperation() const;
    virtual long int getOperandCount() const;
    virtual const CsgOperandView* getOperand(long int index) const;
    virtual const Geometry* getGeometry() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
    virtual const CsgExpression* getChildExpression() const;
    virtual BOOLEAN isStructuralTraversalSafe() const;
};
#endif
