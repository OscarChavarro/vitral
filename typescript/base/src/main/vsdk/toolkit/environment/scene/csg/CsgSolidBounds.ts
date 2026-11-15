import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { GeometryBounds } from "../../geometry/element/GeometryBounds.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import { CsgOperation } from "./CsgOperation.js";

// Conservative bounds of a shared expression's solid in its own coordinates.
// Union encloses every operand, intersection lies within each known operand,
// and difference lies within its minuend. Nested expressions are placed with
// outward rounding; unknown operands and cycles stay explicit.
export class CsgSolidBounds {
    public static query(expression: CsgExpression, result: GeometryBounds): void {
        const path = new ArrayList<CsgExpression>();
        CsgSolidBounds.queryExpression(expression, path, result);
    }

    private static queryExpression(
        expression: CsgExpression,
        path: ArrayList<CsgExpression>,
        result: GeometryBounds,
    ): void {
        result.setIndeterminate();
        const count = expression.getOperandCount();
        const operation = expression.getOperation();
        if (
            path.contains(expression) ||
            count <= 0 ||
            (operation !== CsgOperation.UNION &&
                operation !== CsgOperation.INTERSECTION &&
                operation !== CsgOperation.DIFFERENCE)
        )
            return;
        if (!path.add(expression)) return;
        // Only the minuend bounds a difference.
        const used = operation === CsgOperation.DIFFERENCE ? 1 : count;
        let combined = new GeometryBounds();
        for (let i = 0; i < used; ++i) {
            const operand = expression.getOperand(i);
            let bounds = new GeometryBounds();
            let child: CsgExpression | null = null;
            if (operand === null) bounds.setIndeterminate();
            else if ((child = operand.getChildExpression()) !== null) {
                const forward: Matrix4x4d[] = [new Matrix4x4d()];
                const inverse: Matrix4x4d[] = [new Matrix4x4d()];
                if (!operand.isStructuralTraversalSafe() || !child.isStructuralTraversalSafe()) bounds.setUnsupported();
                else if (operand.queryAffinePlacement(forward, inverse) !== AffinePlacementStatus.Valid)
                    bounds.setIndeterminate();
                else {
                    const local = new GeometryBounds();
                    CsgSolidBounds.queryExpression(child, path, local);
                    bounds = local.transformed(forward[0]!);
                }
            } else operand.getSolidBounds(bounds);
            if (i === 0) combined = new GeometryBounds(bounds);
            else
                combined =
                    operation === CsgOperation.UNION ? combined.enclosing(bounds) : combined.intersecting(bounds);
        }
        path.remove(path.size() - 1);
        result.assign(combined);
    }
}
