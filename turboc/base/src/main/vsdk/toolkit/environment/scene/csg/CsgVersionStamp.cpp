#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgVersionStamp.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"

CsgVersionStamp::CsgVersionStamp() : expressions(4), expressionVersions(4), operands(8),
    operandVersions(8), path(4), complete(FALSE), captured(FALSE) {}
CsgVersionStamp::CsgVersionStamp(const CsgVersionStamp& other) : expressions(4), expressionVersions(4),
    operands(8), operandVersions(8), path(4), complete(FALSE), captured(FALSE) { *this=other; }
CsgVersionStamp& CsgVersionStamp::operator=(const CsgVersionStamp& other)
{
    if (this!=&other) { long int i;
        clear();
        for (i =0;i<other.expressions.size();++i) {
            expressions.add(other.expressions[i]); expressionVersions.add(other.expressionVersions[i]);
        }
        for (i =0;i<other.operands.size();++i) {
            operands.add(other.operands[i]); operandVersions.add(other.operandVersions[i]);
        }
        complete=other.complete; captured=other.captured;
    }
    return *this;
}
CsgVersionStamp::~CsgVersionStamp() {}
void CsgVersionStamp::clear()
{
    expressions.clear(); expressionVersions.clear(); operands.clear(); operandVersions.clear();
    path.clear(); complete=FALSE; captured=FALSE;
}

BOOLEAN CsgVersionStamp::record(const CsgExpression& expression)
{
    if (path.contains(&expression) || !path.add(&expression)) return FALSE;
    long long version=0;
    if (!expression.queryStructureVersion(version)) complete=FALSE;
    if (!expressions.add(&expression) || !expressionVersions.add(version)) return FALSE;
    for (long int i=0;i<expression.getOperandCount();++i) {
        const CsgOperandView* operand=expression.getOperand(i);
        if (!operand) { complete=FALSE; continue; }
        long long operandVersion=0;
        if (!operand->queryModificationVersion(operandVersion)) complete=FALSE;
        if (!operands.add(operand) || !operandVersions.add(operandVersion)) return FALSE;
        const CsgExpression* child=operand->getChildExpression();
        if (child && !record(*child)) return FALSE;
    }
    path.remove(path.size()-1);
    return TRUE;
}

BOOLEAN CsgVersionStamp::capture(const CsgExpression& root)
{
    clear();
    complete=TRUE;
    if (!record(root)) { clear(); return FALSE; }
    path.clear();
    captured=TRUE;
    return TRUE;
}

BOOLEAN CsgVersionStamp::isCaptured() const { return captured; }
BOOLEAN CsgVersionStamp::isComplete() const { return captured && complete; }

BOOLEAN CsgVersionStamp::isCurrent() const
{ long int i;
    if (!isComplete()) return FALSE;
    for (i =0;i<expressions.size();++i) {
        long long version=0;
        if (!expressions[i]->queryStructureVersion(version) || version!=expressionVersions[i]) return FALSE;
    }
    for (i =0;i<operands.size();++i) {
        long long version=0;
        if (!operands[i]->queryModificationVersion(version) || version!=operandVersions[i]) return FALSE;
    }
    return TRUE;
}
