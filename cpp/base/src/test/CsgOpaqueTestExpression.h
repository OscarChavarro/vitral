#ifndef __CSG_OPAQUE_TEST_EXPRESSION__
#define __CSG_OPAQUE_TEST_EXPRESSION__
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
class CsgOpaqueTestExpression : public CsgExpression {
private:
    const CsgOperandView& operand;
public:
    explicit CsgOpaqueTestExpression(const CsgOperandView& value);
    CsgOperation getOperation() const override;
    long int getOperandCount() const override;
    const CsgOperandView* getOperand(long int index) const override;
};
#endif
