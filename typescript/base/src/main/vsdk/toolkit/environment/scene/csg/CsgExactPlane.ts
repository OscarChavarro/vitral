import { Double } from "../../../../../java/lang/Double.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { CsgExactTransform } from "./CsgExactTransform.js";

// Exact sufficient conversion of a local oriented plane to world coefficients.
// Returns false when any binary64 intermediate is not exactly representable.
export class CsgExactPlane {
    public static world(local: readonly number[] | null, forward: Matrix4x4d, result: number[] | null): boolean {
        if (local === null || result === null) return false;
        for (let i = 0; i < 4; ++i) if (!Double.isFinite(local[i]!)) return false;
        if (local[0] === 0 && local[1] === 0 && local[2] === 0) return false;
        const inverse: number[][] = [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
        ];
        if (!CsgExactTransform.inverseNumerator(forward, inverse)) return false;
        const candidate: number[] = [0, 0, 0, 0];
        const holder: number[] = [0];
        for (let i = 0; i < 4; ++i) {
            const column: number[] = [inverse[0]![i]!, inverse[1]![i]!, inverse[2]![i]!, inverse[3]![i]!];
            if (!CsgExactTransform.exactSum(column, local, 4, holder)) return false;
            candidate[i] = holder[0]!;
        }
        const sign = inverse[3]![3]! < 0 ? -1 : 1;
        for (let i = 0; i < 4; ++i) result[i] = sign * candidate[i]!;
        return true;
    }
}
