#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/scene/csg/CsgTree.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpressionEquivalence.h"

CsgTree::CsgTree(CsgOperation::Value value) : operation(value), operands(4), structureVersion(0) {}
CsgTree::~CsgTree() {}
CsgOperation::Value CsgTree::getOperation() const { return operation; }
long int CsgTree::getOperandCount() const { return operands.size(); }
const CsgOperandView* CsgTree::getOperand(long int index) const
{ return index < 0 || index >= operands.size() ? (const CsgOperandView*)NULL : operands[index]; }
BOOLEAN CsgTree::canAttach(const CsgOperandView& operand) const
{
    const CsgExpression* child = operand.getChildExpression();
    if (!child) return TRUE;
    ArrayList<const CsgExpression*> pending;
    ArrayList<const CsgExpression*> visited;
    if (!pending.add(child)) return FALSE;
    for (long int i = 0; i < pending.size(); ++i) { long int j;
        const CsgExpression* current = pending[i];
        if (current == ((const CsgExpression*)(this))) return FALSE;
        BOOLEAN known = FALSE;
        for (j = 0; j < visited.size(); ++j) {
            if (visited[j] == current) { known = TRUE; break; }
        }
        if (known) continue;
        if (!visited.add(current)) return FALSE;
        for (j = 0; j < current->getOperandCount(); ++j) {
            const CsgOperandView* nested = current->getOperand(j);
            if (nested && nested->getChildExpression() && !pending.add(nested->getChildExpression()))
                return FALSE;
        }
    }
    return TRUE;
}
BOOLEAN CsgTree::addOperand(const CsgOperandView& operand)
{
    if (!canAttach(operand) || !operands.add(&operand)) return FALSE;
    ++structureVersion;
    return TRUE;
}
void CsgTree::setOperation(CsgOperation::Value value)
{
    operation = value;
    ++structureVersion;
}
BOOLEAN CsgTree::queryStructureVersion(long long& version) const { version=structureVersion; return TRUE; }
BOOLEAN CsgTree::queryModificationVersion(long long& version) const { version=structureVersion; return TRUE; }
const Geometry* CsgTree::getGeometry() const { return NULL; }
Matrix4x4d CsgTree::getAffinePlacement() const { return Matrix4x4d::identityMatrix(); }
AffinePlacementStatus::Value CsgTree::queryAffinePlacement(Matrix4x4d& forward, Matrix4x4d& inverse) const
{ forward = Matrix4x4d::identityMatrix(); inverse = forward; return AffinePlacementStatus::Valid; }
const CsgExpression* CsgTree::getChildExpression() const { return this; }

BOOLEAN CsgTree::isSameSolid(const CsgOperandView& other) const
{ return CsgExpressionEquivalence::same(*this,other); }

BOOLEAN CsgTree::isStructuralTraversalSafe() const { return getClass()==&CsgTree::CLASS; }

const Class CsgTree::CLASS = { "CsgTree", &CsgExpression::CLASS };

const Class *
CsgTree::getClass() const
{
    return &CLASS;
}
