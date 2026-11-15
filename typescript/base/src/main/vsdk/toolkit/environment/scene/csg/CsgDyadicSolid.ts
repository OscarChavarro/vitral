import { Double } from "../../../../../java/lang/Double.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { CsgDyadic } from "./CsgDyadic.js";
import { CsgDyadicTransform } from "./CsgDyadicTransform.js";

// Exact sufficient certificates; failure never establishes non-equivalence.
export class CsgDyadicSolid {
    private static proportional(a: readonly CsgDyadic[], b: readonly CsgDyadic[], count: number): boolean {
        let pivot = -1;
        for (let i = 0; i < count; ++i)
            if (a[i]!.getSign() || b[i]!.getSign()) {
                pivot = i;
                break;
            }
        if (pivot < 0 || !a[pivot]!.getSign() || a[pivot]!.getSign() !== b[pivot]!.getSign()) return false;
        for (let i = 0; i < count; ++i) {
            const x = new CsgDyadic();
            const y = new CsgDyadic();
            if (!CsgDyadic.multiply(a[i]!, b[pivot]!, x) || !CsgDyadic.multiply(b[i]!, a[pivot]!, y) || !x.equals(y))
                return false;
        }
        return true;
    }

    private static planeWorld(
        local: readonly number[] | null,
        forward: Matrix4x4d,
        result: CsgDyadic[] | null,
    ): boolean {
        if (local === null || result === null || (local[0] === 0 && local[1] === 0 && local[2] === 0)) return false;
        const input = CsgDyadicTransform.vector(4);
        const inverse = CsgDyadicTransform.square(4);
        const candidate = CsgDyadicTransform.vector(4);
        for (let i = 0; i < 4; ++i) if (!CsgDyadic.fromDouble(local[i]!, input[i]!)) return false;
        if (!CsgDyadicTransform.inverseNumerator(forward, inverse)) return false;
        for (let i = 0; i < 4; ++i) {
            const column = CsgDyadicTransform.vector(4);
            for (let j = 0; j < 4; ++j) column[j]!.assign(inverse[j]![i]!);
            if (!CsgDyadicTransform.dot(column, input, 4, candidate[i]!)) return false;
            if (inverse[3]![3]!.getSign() < 0) candidate[i]!.negate();
        }
        for (let i = 0; i < 4; ++i) result[i]!.assign(candidate[i]!);
        return true;
    }

    private static quadricLocal(local: readonly number[] | null, result: CsgDyadic[] | null): boolean {
        if (local === null || result === null) return false;
        const candidate = CsgDyadicTransform.vector(10);
        for (let i = 0; i < 10; ++i) if (!CsgDyadic.fromDouble(local[i]!, candidate[i]!)) return false;
        for (let i = 0; i < 10; ++i) result[i]!.assign(candidate[i]!);
        return true;
    }

    private static quadricWorld(
        local: readonly CsgDyadic[] | null,
        forward: Matrix4x4d,
        result: CsgDyadic[] | null,
    ): boolean {
        if (local === null || result === null) return false;
        const inverse = CsgDyadicTransform.square(4);
        if (!CsgDyadicTransform.inverseNumerator(forward, inverse)) return false;
        // Use twice the homogeneous coefficient matrix, avoiding any division.
        const q = CsgDyadicTransform.square(4);
        const intermediate = CsgDyadicTransform.square(4);
        const world = CsgDyadicTransform.square(4);
        for (let i = 0; i < 3; ++i) if (!CsgDyadic.add(local[i]!, local[i]!, q[i]![i]!)) return false;
        if (!CsgDyadic.add(local[9]!, local[9]!, q[3]![3]!)) return false;
        const row: number[] = [0, 0, 1, 0, 1, 2];
        const col: number[] = [1, 2, 2, 3, 3, 3];
        for (let i = 0; i < 6; ++i) {
            q[col[i]!]![row[i]!]!.assign(local[3 + i]!);
            q[row[i]!]![col[i]!]!.assign(q[col[i]!]![row[i]!]!);
        }
        for (let i = 0; i < 4; ++i)
            for (let j = 0; j < 4; ++j) {
                const column = CsgDyadicTransform.vector(4);
                for (let k = 0; k < 4; ++k) column[k]!.assign(inverse[k]![j]!);
                if (!CsgDyadicTransform.dot(q[i]!, column, 4, intermediate[i]![j]!)) return false;
            }
        for (let i = 0; i < 4; ++i)
            for (let j = 0; j < 4; ++j) {
                const column = CsgDyadicTransform.vector(4);
                const values = CsgDyadicTransform.vector(4);
                for (let k = 0; k < 4; ++k) {
                    column[k]!.assign(inverse[k]![i]!);
                    values[k]!.assign(intermediate[k]![j]!);
                }
                if (!CsgDyadicTransform.dot(column, values, 4, world[i]![j]!)) return false;
            }
        result[0]!.assign(world[0]![0]!);
        result[1]!.assign(world[1]![1]!);
        result[2]!.assign(world[2]![2]!);
        for (let i = 0; i < 6; ++i) result[3 + i]!.assign(world[row[i]!]![col[i]!]!);
        result[9]!.assign(world[3]![3]!);
        return true;
    }

    public static samePlanes(
        a: readonly number[] | null,
        pa: Matrix4x4d,
        b: readonly number[] | null,
        pb: Matrix4x4d,
    ): boolean {
        const x = CsgDyadicTransform.vector(4);
        const y = CsgDyadicTransform.vector(4);
        return (
            CsgDyadicSolid.planeWorld(a, pa, x) &&
            CsgDyadicSolid.planeWorld(b, pb, y) &&
            CsgDyadicSolid.proportional(x, y, 4)
        );
    }

    public static sameQuadrics(
        a: readonly number[] | null,
        pa: Matrix4x4d,
        b: readonly number[] | null,
        pb: Matrix4x4d,
    ): boolean {
        const qa = CsgDyadicTransform.vector(10);
        const qb = CsgDyadicTransform.vector(10);
        const x = CsgDyadicTransform.vector(10);
        const y = CsgDyadicTransform.vector(10);
        return (
            CsgDyadicSolid.quadricLocal(a, qa) &&
            CsgDyadicSolid.quadricLocal(b, qb) &&
            CsgDyadicSolid.quadricWorld(qa, pa, x) &&
            CsgDyadicSolid.quadricWorld(qb, pb, y) &&
            CsgDyadicSolid.proportional(x, y, 10)
        );
    }

    public static sphereQuadric(radius: number, ps: Matrix4x4d, q: readonly number[] | null, pq: Matrix4x4d): boolean {
        if (!Double.isFinite(radius) || radius <= 0) return false;
        const local = CsgDyadicTransform.vector(10);
        const quadric = CsgDyadicTransform.vector(10);
        const x = CsgDyadicTransform.vector(10);
        const y = CsgDyadicTransform.vector(10);
        const r = new CsgDyadic();
        if (!CsgDyadic.fromDouble(1, local[0]!) || !CsgDyadic.fromDouble(radius, r)) return false;
        local[2]!.assign(local[0]!);
        local[1]!.assign(local[2]!);
        if (!CsgDyadic.multiply(r, r, local[9]!)) return false;
        local[9]!.negate();
        return (
            CsgDyadicSolid.quadricLocal(q, quadric) &&
            CsgDyadicSolid.quadricWorld(local, ps, x) &&
            CsgDyadicSolid.quadricWorld(quadric, pq, y) &&
            CsgDyadicSolid.proportional(x, y, 10)
        );
    }

    public static sameEllipsoids(ra: number, pa: Matrix4x4d, rb: number, pb: Matrix4x4d): boolean {
        if (!Double.isFinite(ra) || !Double.isFinite(rb) || ra <= 0 || rb <= 0) return false;
        for (let i = 0; i < 3; ++i) if (pa.get(i, 3) !== pb.get(i, 3)) return false;
        const rA = new CsgDyadic();
        const rB = new CsgDyadic();
        const x = CsgDyadicTransform.square(3);
        const y = CsgDyadicTransform.square(3);
        if (!CsgDyadic.fromDouble(ra, rA) || !CsgDyadic.fromDouble(rb, rB)) return false;
        for (let i = 0; i < 3; ++i)
            for (let j = 0; j < 3; ++j) {
                const a = new CsgDyadic();
                const b = new CsgDyadic();
                if (
                    !CsgDyadic.fromDouble(pa.get(i, j), a) ||
                    !CsgDyadic.fromDouble(pb.get(i, j), b) ||
                    !CsgDyadic.multiply(a, rA, x[i]![j]!) ||
                    !CsgDyadic.multiply(b, rB, y[i]![j]!)
                )
                    return false;
            }
        for (let i = 0; i < 3; ++i)
            for (let j = i; j < 3; ++j) {
                const a = new CsgDyadic();
                const b = new CsgDyadic();
                if (
                    !CsgDyadicTransform.dot(x[i]!, x[j]!, 3, a) ||
                    !CsgDyadicTransform.dot(y[i]!, y[j]!, 3, b) ||
                    !a.equals(b)
                )
                    return false;
            }
        return true;
    }
}
