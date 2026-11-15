package vsdk.toolkit.environment.scene.csg;

import java.util.ArrayList;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.element.GeometryBounds;
import vsdk.toolkit.environment.scene.AffinePlacementStatus;

/**
Conservative bounds of a shared expression's solid in its own coordinates.
Union encloses every operand, intersection lies within each known operand,
and difference lies within its minuend. Nested expressions are placed with
outward rounding; unknown operands and cycles stay explicit.
*/
public class CsgSolidBounds {
    private static void queryExpression(CsgExpression expression, ArrayList<CsgExpression> path, GeometryBounds result) {
    result.setIndeterminate();
    final long count=expression.getOperandCount();
    final CsgOperation operation=expression.getOperation();
    if (path.contains(expression) || count<=0 || (operation!=CsgOperation.UNION &&
        operation!=CsgOperation.INTERSECTION && operation!=CsgOperation.DIFFERENCE)) return;
    if (!path.add(expression)) return;
    // Only the minuend bounds a difference.
    final long used=operation==CsgOperation.DIFFERENCE ? 1 : count;
    GeometryBounds combined = new GeometryBounds();
    for (long i=0;i<used;++i) {
        final CsgOperandView operand =expression.getOperand(i);
        GeometryBounds bounds = new GeometryBounds();
        final CsgExpression child = operand == null ? null : operand.getChildExpression();
        if (operand == null) bounds.setIndeterminate();
        else if (child != null) {
            Matrix4x4d[] forward = {new Matrix4x4d()};
            Matrix4x4d[] inverse = {new Matrix4x4d()};
            if (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe()) bounds.setUnsupported();
            else if (operand.queryAffinePlacement(forward,inverse)!=AffinePlacementStatus.Valid) bounds.setIndeterminate();
            else {
                GeometryBounds local = new GeometryBounds();
                queryExpression(child,path,local);
                bounds=local.transformed(forward[0]);
            }
        }
        else operand.getSolidBounds(bounds);
        if (i==0) combined=bounds;
        else combined=operation==CsgOperation.UNION ? combined.enclosing(bounds) : combined.intersecting(bounds);
    }
    path.remove(path.size()-1);
    result.assign(combined);
}

    public static void query(CsgExpression expression, GeometryBounds result) {
    ArrayList<CsgExpression> path = new ArrayList<CsgExpression>();
    queryExpression(expression,path,result);
}
}

