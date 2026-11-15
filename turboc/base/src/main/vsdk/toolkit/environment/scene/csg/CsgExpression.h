#ifndef __CSG_EXPRESSION__
#define __CSG_EXPRESSION__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Class.h"

#include "vsdk/toolkit/environment/scene/csg/CsgOperation.h"
class CsgOperandView;

// An ordered expression view. Difference subtracts operands 1..N from 0.
// Structure and leaf identity are independent of any evaluation strategy.
class CsgExpression {
public:
    static const Class CLASS;
    virtual const Class *getClass() const;
    virtual ~CsgExpression();
    // Opt in only if operation and operands fully describe this solid.
    virtual BOOLEAN isStructuralTraversalSafe() const;
    virtual CsgOperation::Value getOperation() const = 0;
    virtual long int getOperandCount() const = 0;
    virtual const CsgOperandView* getOperand(long int index) const = 0;
    // Counter that changes whenever operation or operand list changes; false
    // when this expression does not track its edits.
    virtual BOOLEAN queryStructureVersion(long long& version) const;
};
#endif
