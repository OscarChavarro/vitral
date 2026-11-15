#ifndef __CSG_POLICY_TEST_OPERAND__
#define __CSG_POLICY_TEST_OPERAND__

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
class CsgPolicyTestOperand : public CsgOperandView {
private:
    const CsgExpression& expression;
    BOOLEAN inverted;
public:
    CsgPolicyTestOperand(const CsgExpression& expression, BOOLEAN inverted);
    BOOLEAN isInverted() const;
    virtual const Geometry* getGeometry() const;
    virtual Matrix4x4d getAffinePlacement() const;
    virtual AffinePlacementStatus::Value queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const;
    virtual const CsgExpression* getChildExpression() const;
};
#endif
