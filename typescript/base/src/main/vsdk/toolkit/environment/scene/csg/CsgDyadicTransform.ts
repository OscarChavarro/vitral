import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { CsgDyadic } from "./CsgDyadic.js";

// Exact homogeneous inverse numerator; no intermediate binary64 rounding.
export class CsgDyadicTransform {
    public static dot(
        a: readonly CsgDyadic[] | null,
        b: readonly CsgDyadic[] | null,
        count: number,
        result: CsgDyadic,
    ): boolean {
        if (a === null || b === null || count <= 0 || count > 4) return false;
        const sum = new CsgDyadic();
        for (let i = 0; i < count; ++i) {
            const product = new CsgDyadic();
            const next = new CsgDyadic();
            if (!CsgDyadic.multiply(a[i]!, b[i]!, product) || !CsgDyadic.add(sum, product, next)) return false;
            sum.assign(next);
        }
        result.assign(sum);
        return true;
    }

    public static inverseNumerator(forward: Matrix4x4d, result: CsgDyadic[][] | null): boolean {
        if (
            result === null ||
            forward.get(3, 0) !== 0 ||
            forward.get(3, 1) !== 0 ||
            forward.get(3, 2) !== 0 ||
            forward.get(3, 3) !== 1
        )
            return false;
        const matrix = CsgDyadicTransform.square(3);
        const translation = CsgDyadicTransform.vector(3);
        const cofactors = CsgDyadicTransform.square(3);
        const candidate = CsgDyadicTransform.square(4);
        for (let i = 0; i < 3; ++i) {
            if (!CsgDyadic.fromDouble(-forward.get(i, 3), translation[i]!)) return false;
            for (let j = 0; j < 3; ++j) if (!CsgDyadic.fromDouble(forward.get(i, j), matrix[i]![j]!)) return false;
        }
        for (let i = 0; i < 3; ++i)
            for (let j = 0; j < 3; ++j) {
                const r = (i + 1) % 3;
                const s = (i + 2) % 3;
                const c = (j + 1) % 3;
                const d = (j + 2) % 3;
                const first = new CsgDyadic();
                const second = new CsgDyadic();
                if (
                    !CsgDyadic.multiply(matrix[r]![c]!, matrix[s]![d]!, first) ||
                    !CsgDyadic.multiply(matrix[r]![d]!, matrix[s]![c]!, second) ||
                    !CsgDyadic.subtract(first, second, cofactors[i]![j]!)
                )
                    return false;
            }
        const determinant = new CsgDyadic();
        if (!CsgDyadicTransform.dot(matrix[0]!, cofactors[0]!, 3, determinant) || determinant.getSign() === 0)
            return false;
        for (let i = 0; i < 3; ++i) {
            for (let j = 0; j < 3; ++j) candidate[i]![j]!.assign(cofactors[j]![i]!);
            if (!CsgDyadicTransform.dot(candidate[i]!, translation, 3, candidate[i]![3]!)) return false;
        }
        candidate[3]![3]!.assign(determinant);
        for (let i = 0; i < 4; ++i) for (let j = 0; j < 4; ++j) result[i]![j]!.assign(candidate[i]![j]!);
        return true;
    }

    /** Storage for a C++ `CsgDyadic name[n]` array. */
    public static vector(n: number): CsgDyadic[] {
        return Array.from({ length: n }, () => new CsgDyadic());
    }

    /** Storage for a C++ `CsgDyadic name[n][n]` array. */
    public static square(n: number): CsgDyadic[][] {
        return Array.from({ length: n }, () => CsgDyadicTransform.vector(n));
    }
}
