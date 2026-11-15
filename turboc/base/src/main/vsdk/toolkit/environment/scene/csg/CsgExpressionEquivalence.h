#ifndef __CSG_EXPRESSION_EQUIVALENCE__
#define __CSG_EXPRESSION_EQUIVALENCE__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperation.h"
class CsgExpression;
class CsgOperandView;
// Sufficient structural equality; unknown or cyclic expressions are rejected.
class CsgExpressionEquivalence {
private:
    static BOOLEAN sameView(const CsgOperandView& a, const CsgOperandView& b,
        ArrayList<const CsgExpression*>& pathA, ArrayList<const CsgExpression*>& pathB);
    static BOOLEAN collectOperand(const CsgOperandView& view, CsgOperation::Value operation,
        ArrayList<const CsgExpression*>& path, ArrayList<const CsgOperandView*>& output);
    static BOOLEAN sameSets(const ArrayList<const CsgOperandView*>& a,
        const ArrayList<const CsgOperandView*>& b,
        ArrayList<const CsgExpression*>& pathA, ArrayList<const CsgExpression*>& pathB);
    static BOOLEAN sameExpression(const CsgExpression& a, const CsgExpression& b,
        ArrayList<const CsgExpression*>& pathA, ArrayList<const CsgExpression*>& pathB);
public:
    static BOOLEAN same(const CsgOperandView& a, const CsgOperandView& b);
};
#endif
