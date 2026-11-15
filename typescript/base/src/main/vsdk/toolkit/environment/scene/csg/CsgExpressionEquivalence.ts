import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { CsgExpression } from "./CsgExpression.js";
import type { CsgOperandView } from "./CsgOperandView.js";
import { CsgOperation } from "./CsgOperation.js";
import { CsgPlacedExpression } from "./CsgPlacedExpression.js";
import { CsgTree } from "./CsgTree.js";

// Sufficient structural equality; unknown or cyclic expressions are rejected.
export class CsgExpressionEquivalence {
    // C++ `typeid(value) == typeid(type)`.
    private static isType(value: object, type: abstract new (...args: never[]) => object): boolean {
        return value.constructor === type;
    }

    public static same(a: CsgOperandView, b: CsgOperandView): boolean {
        const pathA = new ArrayList<CsgExpression>();
        const pathB = new ArrayList<CsgExpression>();
        return CsgExpressionEquivalence.sameView(a, b, pathA, pathB);
    }

    private static sameView(
        a: CsgOperandView,
        b: CsgOperandView,
        pathA: ArrayList<CsgExpression>,
        pathB: ArrayList<CsgExpression>,
    ): boolean {
        const childA = a.getChildExpression();
        const childB = b.getChildExpression();
        if (childA === null || childB === null)
            return childA === null && childB === null && a.isSameSolid(b) && b.isSameSolid(a);
        // Arbitrary application views may add inversion, clipping or other
        // semantics not represented by their child and placement. Only the known
        // shared structural views can be compared by unpacking their children.
        const structuralA =
            CsgExpressionEquivalence.isType(a, CsgTree) || CsgExpressionEquivalence.isType(a, CsgPlacedExpression);
        const structuralB =
            CsgExpressionEquivalence.isType(b, CsgTree) || CsgExpressionEquivalence.isType(b, CsgPlacedExpression);
        if (!structuralA || !structuralB) return false;
        const forwardA: Matrix4x4d[] = [new Matrix4x4d()];
        const inverseA: Matrix4x4d[] = [new Matrix4x4d()];
        const forwardB: Matrix4x4d[] = [new Matrix4x4d()];
        const inverseB: Matrix4x4d[] = [new Matrix4x4d()];
        if (
            a.queryAffinePlacement(forwardA, inverseA) !== AffinePlacementStatus.Valid ||
            b.queryAffinePlacement(forwardB, inverseB) !== AffinePlacementStatus.Valid
        )
            return false;
        for (let i = 0; i < 4; ++i)
            for (let j = 0; j < 4; ++j) if (forwardA[0]!.get(i, j) !== forwardB[0]!.get(i, j)) return false;
        return CsgExpressionEquivalence.sameExpression(childA, childB, pathA, pathB);
    }

    private static collectOperand(
        view: CsgOperandView,
        operation: CsgOperation,
        path: ArrayList<CsgExpression>,
        output: ArrayList<CsgOperandView>,
    ): boolean {
        const child = view.getChildExpression();
        const knownView =
            CsgExpressionEquivalence.isType(view, CsgTree) ||
            CsgExpressionEquivalence.isType(view, CsgPlacedExpression);
        if (
            child === null ||
            !knownView ||
            !CsgExpressionEquivalence.isType(child, CsgTree) ||
            child.getOperation() !== operation
        )
            return output.add(view);
        const forward: Matrix4x4d[] = [new Matrix4x4d()];
        const inverse: Matrix4x4d[] = [new Matrix4x4d()];
        if (view.queryAffinePlacement(forward, inverse) !== AffinePlacementStatus.Valid) return false;
        let identity = true;
        for (let i = 0; i < 4; ++i)
            for (let j = 0; j < 4; ++j) if (forward[0]!.get(i, j) !== (i === j ? 1.0 : 0.0)) identity = false;
        if (!identity) return output.add(view);
        if (path.contains(child) || child.getOperandCount() <= 0 || !path.add(child)) return false;
        let valid = true;
        for (let i = 0; i < child.getOperandCount() && valid; ++i) {
            const item = child.getOperand(i);
            valid = item !== null && CsgExpressionEquivalence.collectOperand(item, operation, path, output);
        }
        path.remove(path.size() - 1);
        return valid;
    }

    private static sameSets(
        a: ArrayList<CsgOperandView>,
        b: ArrayList<CsgOperandView>,
        pathA: ArrayList<CsgExpression>,
        pathB: ArrayList<CsgExpression>,
    ): boolean {
        // Union and intersection are idempotent as solid-set operations. Check
        // both directions without consuming matches so duplicates are harmless.
        for (let direction = 0; direction < 2; ++direction) {
            const from = direction === 0 ? a : b;
            const to = direction === 0 ? b : a;
            for (let i = 0; i < from.size(); ++i) {
                let found = false;
                for (let j = 0; j < to.size() && !found; ++j)
                    found =
                        direction === 0
                            ? CsgExpressionEquivalence.sameView(from.get(i), to.get(j), pathA, pathB)
                            : CsgExpressionEquivalence.sameView(to.get(j), from.get(i), pathA, pathB);
                if (!found) return false;
            }
        }
        return true;
    }

    private static sameExpression(
        a: CsgExpression,
        b: CsgExpression,
        pathA: ArrayList<CsgExpression>,
        pathB: ArrayList<CsgExpression>,
    ): boolean {
        if (pathA.contains(a) || pathB.contains(b)) return false;
        if (!CsgExpressionEquivalence.isType(a, CsgTree) || !CsgExpressionEquivalence.isType(b, CsgTree)) return false;
        const op = a.getOperation();
        if (
            a.getOperandCount() <= 0 ||
            b.getOperandCount() <= 0 ||
            op !== b.getOperation() ||
            (op !== CsgOperation.UNION && op !== CsgOperation.INTERSECTION && op !== CsgOperation.DIFFERENCE)
        )
            return false;
        if (!pathA.add(a)) return false;
        if (!pathB.add(b)) {
            pathA.remove(pathA.size() - 1);
            return false;
        }
        let equal = true;
        let start = 0;
        if (op === CsgOperation.DIFFERENCE) {
            const firstA = a.getOperand(0);
            const firstB = b.getOperand(0);
            equal =
                firstA !== null && firstB !== null && CsgExpressionEquivalence.sameView(firstA, firstB, pathA, pathB);
            start = 1;
        }
        const itemsA = new ArrayList<CsgOperandView>();
        const itemsB = new ArrayList<CsgOperandView>();
        const associative = op === CsgOperation.DIFFERENCE ? CsgOperation.UNION : op;
        for (let i = start; equal && i < a.getOperandCount(); ++i) {
            const item = a.getOperand(i);
            equal = item !== null && CsgExpressionEquivalence.collectOperand(item, associative, pathA, itemsA);
        }
        for (let i = start; equal && i < b.getOperandCount(); ++i) {
            const item = b.getOperand(i);
            equal = item !== null && CsgExpressionEquivalence.collectOperand(item, associative, pathB, itemsB);
        }
        if (equal) equal = CsgExpressionEquivalence.sameSets(itemsA, itemsB, pathA, pathB);
        pathA.remove(pathA.size() - 1);
        pathB.remove(pathB.size() - 1);
        return equal;
    }
}
