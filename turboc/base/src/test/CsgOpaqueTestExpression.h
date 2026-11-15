#ifndef __CSG_OPAQUE_TEST_EXPRESSION__
#define __CSG_OPAQUE_TEST_EXPRESSION__
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
class CsgOpaqueTestExpression : public CsgExpression {
private:
    const CsgOperandView& operand;
public:
    explicit CsgOpaqueTestExpression(const CsgOperandView& value);
    virtual CsgOperation::Value getOperation() const;
    virtual long int getOperandCount() const;
    virtual const CsgOperandView* getOperand(long int index) const;
};
#endif
