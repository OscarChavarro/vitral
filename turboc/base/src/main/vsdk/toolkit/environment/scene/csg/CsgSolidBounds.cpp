#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/scene/csg/CsgExpression.h"
#include "vsdk/toolkit/environment/scene/csg/CsgOperandView.h"
#include "vsdk/toolkit/environment/scene/csg/CsgSolidBounds.h"

void CsgSolidBounds::query(const CsgExpression& expression, GeometryBounds& result)
{
    ArrayList<const CsgExpression*> path;
    queryExpression(expression,path,result);
}

void CsgSolidBounds::queryExpression(const CsgExpression& expression,
    ArrayList<const CsgExpression*>& path, GeometryBounds& result)
{
    result.setIndeterminate();
    const long int count=expression.getOperandCount();
    const CsgOperation::Value operation=expression.getOperation();
    if (path.contains(&expression) || count<=0 || (operation!=CsgOperation::UNION &&
        operation!=CsgOperation::INTERSECTION && operation!=CsgOperation::DIFFERENCE)) return;
    if (!path.add(&expression)) return;
    // Only the minuend bounds a difference.
    const long int used=operation==CsgOperation::DIFFERENCE ? 1 : count;
    GeometryBounds combined;
    for (long int i=0;i<used;++i) {
        const CsgOperandView* operand=expression.getOperand(i);
        GeometryBounds bounds;
        // Turbo C++ 3.0 has no declarations in conditions
        const CsgExpression* child=operand ? operand->getChildExpression() : 0;
        if (!operand) bounds.setIndeterminate();
        else if (child) {
            Matrix4x4d forward,inverse;
            if (!operand->isStructuralTraversalSafe() || !child->isStructuralTraversalSafe()) bounds.setUnsupported();
            else if (operand->queryAffinePlacement(forward,inverse)!=AffinePlacementStatus::Valid) bounds.setIndeterminate();
            else {
                GeometryBounds local;
                queryExpression(*child,path,local);
                bounds=local.transformed(forward);
            }
        }
        else operand->getSolidBounds(bounds);
        if (i==0) combined=bounds;
        // Turbo C++ 3.0 cannot destroy class temporaries of a conditional expression
        else if (operation==CsgOperation::UNION) combined=combined.enclosing(bounds);
        else combined=combined.intersecting(bounds);
    }
    path.remove(path.size()-1);
    result=combined;
}
