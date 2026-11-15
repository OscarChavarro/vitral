import { Double } from "../../../../../java/lang/Double.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { CsgExactArithmetic } from "./CsgExactArithmetic.js";
import { CsgExactTransform } from "./CsgExactTransform.js";

// Coefficient order: xx, yy, zz, xy, xz, yz, x, y, z, constant.
export class CsgExactQuadric {
    public static world(local: readonly number[] | null, forward: Matrix4x4d, result: number[] | null): boolean {
        if (local === null || result === null) return false;
        for (let i = 0; i < 10; ++i) if (!Double.isFinite(local[i]!)) return false;
        const inverse: number[][] = CsgExactQuadric.zero4x4();
        if (!CsgExactTransform.inverseNumerator(forward, inverse)) return false;
        const q: number[][] = [
            [local[0]!, 0, 0, 0],
            [0, local[1]!, 0, 0],
            [0, 0, local[2]!, 0],
            [0, 0, 0, local[9]!],
        ];
        const row: number[] = [0, 0, 1, 0, 1, 2];
        const col: number[] = [1, 2, 2, 3, 3, 3];
        for (let k = 0; k < 6; ++k) {
            const half = local[3 + k]! * 0.5;
            if (!CsgExactArithmetic.equalProducts(local[3 + k]!, 0.5, half, 1)) return false;
            q[row[k]!]![col[k]!] = q[col[k]!]![row[k]!] = half;
        }
        const intermediate: number[][] = CsgExactQuadric.zero4x4();
        const world: number[][] = CsgExactQuadric.zero4x4();
        const holder: number[] = [0];
        for (let i = 0; i < 4; ++i)
            for (let j = 0; j < 4; ++j) {
                const column: number[] = [inverse[0]![j]!, inverse[1]![j]!, inverse[2]![j]!, inverse[3]![j]!];
                if (!CsgExactTransform.exactSum(q[i]!, column, 4, holder)) return false;
                intermediate[i]![j] = holder[0]!;
            }
        for (let i = 0; i < 4; ++i)
            for (let j = 0; j < 4; ++j) {
                const column: number[] = [inverse[0]![i]!, inverse[1]![i]!, inverse[2]![i]!, inverse[3]![i]!];
                const values: number[] = [
                    intermediate[0]![j]!,
                    intermediate[1]![j]!,
                    intermediate[2]![j]!,
                    intermediate[3]![j]!,
                ];
                if (!CsgExactTransform.exactSum(column, values, 4, holder)) return false;
                world[i]![j] = holder[0]!;
            }
        const candidate: number[] = [world[0]![0]!, world[1]![1]!, world[2]![2]!, 0, 0, 0, 0, 0, 0, world[3]![3]!];
        for (let k = 0; k < 6; ++k) {
            candidate[3 + k] = world[row[k]!]![col[k]!]! * 2;
            if (!CsgExactArithmetic.equalProducts(world[row[k]!]![col[k]!]!, 2, candidate[3 + k]!, 1)) return false;
        }
        for (let i = 0; i < 10; ++i) result[i] = candidate[i]!;
        return true;
    }

    private static zero4x4(): number[][] {
        return [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
        ];
    }
}
