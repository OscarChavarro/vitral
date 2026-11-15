import { Double } from "../../../../../java/lang/Double.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { CsgExactArithmetic } from "./CsgExactArithmetic.js";

// Homogeneous inverse numerator det(A) * [A^-1, -A^-1 t; 0, 1].
export class CsgExactTransform {
    public static exactSum(
        a: readonly number[] | null,
        b: readonly number[] | null,
        count: number,
        result: number[],
    ): boolean {
        if (a === null || b === null || count <= 0 || count > 4) return false;
        let candidate = 0;
        for (let i = 0; i < count; ++i) candidate += a[i]! * b[i]!;
        if (!Double.isFinite(candidate)) return false;
        const c: number[] = [candidate, 0, 0, 0];
        const d: number[] = [1, 0, 0, 0];
        if (!CsgExactArithmetic.equalSumProducts(a, b, c, d, count)) return false;
        result[0] = candidate;
        return true;
    }

    public static inverseNumerator(forward: Matrix4x4d, result: number[][] | null): boolean {
        if (
            result === null ||
            forward.get(3, 0) !== 0 ||
            forward.get(3, 1) !== 0 ||
            forward.get(3, 2) !== 0 ||
            forward.get(3, 3) !== 1
        )
            return false;
        const cofactor: number[][] = [
            [0, 0, 0],
            [0, 0, 0],
            [0, 0, 0],
        ];
        const holder: number[] = [0];
        for (let i = 0; i < 3; ++i)
            for (let j = 0; j < 3; ++j) {
                const r = (i + 1) % 3;
                const s = (i + 2) % 3;
                const c = (j + 1) % 3;
                const d = (j + 2) % 3;
                const a: number[] = [forward.get(r, c), -forward.get(r, d)];
                const b: number[] = [forward.get(s, d), forward.get(s, c)];
                if (!CsgExactTransform.exactSum(a, b, 2, holder)) return false;
                cofactor[i]![j] = holder[0]!;
            }
        const detTerms: number[] = [forward.get(0, 0), forward.get(0, 1), forward.get(0, 2)];
        const determinant: number[] = [0];
        if (!CsgExactTransform.exactSum(detTerms, cofactor[0]!, 3, determinant) || determinant[0] === 0) return false;
        const candidate: number[][] = [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
        ];
        for (let i = 0; i < 3; ++i) {
            for (let j = 0; j < 3; ++j) candidate[i]![j] = cofactor[j]![i]!;
            const translation: number[] = [-forward.get(0, 3), -forward.get(1, 3), -forward.get(2, 3)];
            if (!CsgExactTransform.exactSum(candidate[i]!, translation, 3, holder)) return false;
            candidate[i]![3] = holder[0]!;
        }
        candidate[3]![3] = determinant[0]!;
        for (let i = 0; i < 4; ++i) for (let j = 0; j < 4; ++j) result[i]![j] = candidate[i]![j]!;
        return true;
    }
}
