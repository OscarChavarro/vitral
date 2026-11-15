import { Double } from "../../../../../java/lang/Double.js";
import { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Box } from "../../geometry/volume/Box.js";
import { PlanarHalfSpace } from "../../geometry/volume/PlanarHalfSpace.js";
import { QuadricSurface } from "../../geometry/volume/QuadricSurface.js";
import { Sphere } from "../../geometry/volume/Sphere.js";
import { AffinePlacementStatus } from "../AffinePlacementStatus.js";
import type { SceneSolid } from "../SceneSolid.js";
import { CsgDyadicSolid } from "./CsgDyadicSolid.js";
import { CsgExactArithmetic } from "./CsgExactArithmetic.js";
import { CsgExactPlane } from "./CsgExactPlane.js";
import { CsgExactQuadric } from "./CsgExactQuadric.js";

// Conservative, exact sufficient conditions for equality of toolkit solids.
export class CsgSolidEquivalence {
    private static sameEllipsoidLinear(a: Matrix4x4d, radiusA: number, b: Matrix4x4d, radiusB: number): boolean {
        const x: number[][] = [
            [0, 0, 0],
            [0, 0, 0],
            [0, 0, 0],
        ];
        const y: number[][] = [
            [0, 0, 0],
            [0, 0, 0],
            [0, 0, 0],
        ];
        for (let i = 0; i < 3; ++i)
            for (let j = 0; j < 3; ++j) {
                x[i]![j] = a.get(i, j) * radiusA;
                y[i]![j] = b.get(i, j) * radiusB;
                if (
                    !Double.isFinite(x[i]![j]!) ||
                    !Double.isFinite(y[i]![j]!) ||
                    !CsgExactArithmetic.equalProducts(a.get(i, j), radiusA, x[i]![j]!, 1) ||
                    !CsgExactArithmetic.equalProducts(b.get(i, j), radiusB, y[i]![j]!, 1)
                )
                    return CsgDyadicSolid.sameEllipsoids(radiusA, a, radiusB, b);
            }
        // Equal Gram matrices describe the same affine image of a sphere.
        for (let i = 0; i < 3; ++i)
            for (let j = i; j < 3; ++j)
                if (!CsgExactArithmetic.equalSumProducts(x[i]!, x[j]!, y[i]!, y[j]!, 3))
                    return CsgDyadicSolid.sameEllipsoids(radiusA, a, radiusB, b);
        return true;
    }

    private static sameTranslation(a: Matrix4x4d, b: Matrix4x4d): boolean {
        for (let i = 0; i < 3; ++i) if (a.get(i, 3) !== b.get(i, 3)) return false;
        return true;
    }

    private static signedPermutation(a: Matrix4x4d, b: Matrix4x4d, axes: number[], signs: number[]): boolean {
        if (!CsgSolidEquivalence.sameTranslation(a, b)) return false;
        const used: boolean[] = [false, false, false];
        for (let j = 0; j < 3; ++j) {
            let found = false;
            for (let k = 0; k < 3 && !found; ++k)
                if (!used[k]) {
                    let same = true;
                    let reverse = true;
                    for (let i = 0; i < 3; ++i) {
                        if (a.get(i, k) !== b.get(i, j)) same = false;
                        if (a.get(i, k) !== -b.get(i, j)) reverse = false;
                    }
                    if (same || reverse) {
                        axes[j] = k;
                        signs[j] = same ? 1 : -1;
                        used[k] = true;
                        found = true;
                    }
                }
            if (!found) return false;
        }
        return true;
    }

    private static proportional(a: readonly number[], b: readonly number[], count: number): boolean {
        let pivot = -1;
        for (let i = 0; i < count; ++i) {
            if (!Double.isFinite(a[i]!) || !Double.isFinite(b[i]!)) return false;
            if (pivot < 0 && (a[i] !== 0 || b[i] !== 0)) pivot = i;
        }
        if (pivot < 0 || a[pivot] === 0 || b[pivot] === 0 || a[pivot]! < 0 !== b[pivot]! < 0) return false;
        for (let i = 0; i < count; ++i)
            if (!CsgExactArithmetic.equalProducts(a[i]!, b[pivot]!, b[i]!, a[pivot]!)) return false;
        return true;
    }

    private static sameBox(a: Box, pa: Matrix4x4d, b: Box, pb: Matrix4x4d): boolean {
        const ab = a.getRawBounds();
        const bb = b.getRawBounds();
        const amin: number[] = [ab[0]!.x(), ab[0]!.y(), ab[0]!.z()];
        const amax: number[] = [ab[1]!.x(), ab[1]!.y(), ab[1]!.z()];
        const bmin: number[] = [bb[0]!.x(), bb[0]!.y(), bb[0]!.z()];
        const bmax: number[] = [bb[1]!.x(), bb[1]!.y(), bb[1]!.z()];
        for (let i = 0; i < 3; ++i)
            if (
                !Double.isFinite(amin[i]!) ||
                !Double.isFinite(amax[i]!) ||
                !Double.isFinite(bmin[i]!) ||
                !Double.isFinite(bmax[i]!) ||
                !(amin[i]! < amax[i]!) ||
                !(bmin[i]! < bmax[i]!)
            )
                return false;
        // Axis-aligned world boxes: compare exact dyadic affine endpoints, including
        // translations, scales, reflections and permutations without rounded sums.
        const ai: number[] = [0, 0, 0];
        const bi: number[] = [0, 0, 0];
        const usedA: boolean[] = [false, false, false];
        const usedB: boolean[] = [false, false, false];
        let axisAligned = true;
        for (let i = 0; i < 3; ++i) {
            ai[i] = -1;
            bi[i] = -1;
            for (let j = 0; j < 3; ++j) {
                if (pa.get(i, j) !== 0) {
                    if (ai[i]! >= 0) axisAligned = false;
                    ai[i] = j;
                }
                if (pb.get(i, j) !== 0) {
                    if (bi[i]! >= 0) axisAligned = false;
                    bi[i] = j;
                }
            }
            if (ai[i]! < 0 || bi[i]! < 0) axisAligned = false;
            else {
                if (usedA[ai[i]!] || usedB[bi[i]!]) axisAligned = false;
                usedA[ai[i]!] = true;
                usedB[bi[i]!] = true;
            }
        }
        if (axisAligned) {
            for (let i = 0; i < 3; ++i) {
                const sa = pa.get(i, ai[i]!);
                const sb = pb.get(i, bi[i]!);
                if (
                    !CsgExactArithmetic.equalAffine(
                        sa,
                        sa > 0 ? amin[ai[i]!]! : amax[ai[i]!]!,
                        pa.get(i, 3),
                        sb,
                        sb > 0 ? bmin[bi[i]!]! : bmax[bi[i]!]!,
                        pb.get(i, 3),
                    ) ||
                    !CsgExactArithmetic.equalAffine(
                        sa,
                        sa > 0 ? amax[ai[i]!]! : amin[ai[i]!]!,
                        pa.get(i, 3),
                        sb,
                        sb > 0 ? bmax[bi[i]!]! : bmin[bi[i]!]!,
                        pb.get(i, 3),
                    )
                )
                    return false;
            }
            return true;
        }
        // For general affine boxes, match parallel edge directions and compare
        // their exact edge vectors and a corresponding world-space corner.
        const used: boolean[] = [false, false, false];
        const reversed: boolean[] = [false, false, false];
        for (let j = 0; j < 3; ++j) {
            let found = false;
            for (let k = 0; k < 3 && !found; ++k)
                if (!used[k]) {
                    let pivot = -1;
                    for (let i = 0; i < 3; ++i)
                        if (pa.get(i, k) !== 0) {
                            pivot = i;
                            break;
                        }
                    if (pivot < 0 || pb.get(pivot, j) === 0) continue;
                    let same = true;
                    for (let i = 0; i < 3; ++i)
                        if (
                            !CsgExactArithmetic.equalProducts(
                                pa.get(i, k),
                                pb.get(pivot, j),
                                pb.get(i, j),
                                pa.get(pivot, k),
                            )
                        )
                            same = false;
                    const reverse = pa.get(pivot, k) < 0 !== pb.get(pivot, j) < 0;
                    for (let i = 0; i < 3 && same; ++i) {
                        const ac: number[] = [pa.get(i, k), -pa.get(i, k)];
                        const av: number[] = [amax[k]!, amin[k]!];
                        const bc: number[] = [pb.get(i, j), -pb.get(i, j)];
                        const bv: number[] = [reverse ? bmin[j]! : bmax[j]!, reverse ? bmax[j]! : bmin[j]!];
                        if (!CsgExactArithmetic.equalSumProducts(ac, av, bc, bv, 2)) same = false;
                    }
                    if (same) {
                        used[k] = true;
                        reversed[j] = reverse;
                        found = true;
                    }
                }
            if (!found) return false;
        }
        for (let i = 0; i < 3; ++i) {
            const ac: number[] = [pa.get(i, 0), pa.get(i, 1), pa.get(i, 2), pa.get(i, 3)];
            const av: number[] = [amin[0]!, amin[1]!, amin[2]!, 1];
            const bc: number[] = [pb.get(i, 0), pb.get(i, 1), pb.get(i, 2), pb.get(i, 3)];
            const bv: number[] = [
                reversed[0] ? bmax[0]! : bmin[0]!,
                reversed[1] ? bmax[1]! : bmin[1]!,
                reversed[2] ? bmax[2]! : bmin[2]!,
                1,
            ];
            if (!CsgExactArithmetic.equalSumProducts(ac, av, bc, bv, 4)) return false;
        }
        return true;
    }

    private static axisPlane(
        coefficients: readonly number[],
        placement: Matrix4x4d,
        axis: number[],
        scale: number[],
        offset: number[],
        root: number[],
        positive: boolean[],
    ): boolean {
        let local = -1;
        for (let i = 0; i < 4; ++i) if (!Double.isFinite(coefficients[i]!)) return false;
        for (let i = 0; i < 3; ++i)
            if (coefficients[i] !== 0) {
                if (local >= 0) return false;
                local = i;
            }
        if (local < 0) return false;
        axis[0] = -1;
        for (let row = 0; row < 3; ++row) {
            if (placement.get(row, local) === 0) continue;
            let single = true;
            for (let column = 0; column < 3; ++column)
                if (column !== local && placement.get(row, column) !== 0) single = false;
            if (single) {
                if (axis[0]! >= 0) return false;
                axis[0] = row;
            }
        }
        if (axis[0]! < 0) return false;
        const candidate = -coefficients[3]! / coefficients[local]!;
        if (
            !Double.isFinite(candidate) ||
            !CsgExactArithmetic.equalProducts(candidate, coefficients[local]!, -coefficients[3]!, 1)
        )
            return false;
        scale[0] = placement.get(axis[0]!, local);
        offset[0] = placement.get(axis[0]!, 3);
        root[0] = candidate;
        positive[0] = coefficients[local]! < 0 === scale[0] < 0;
        return true;
    }

    private static sameOrientedPlane(
        a: readonly number[],
        pa: Matrix4x4d,
        b: readonly number[],
        pb: Matrix4x4d,
    ): boolean {
        const axisA: number[] = [-1];
        const axisB: number[] = [-1];
        const scaleA: number[] = [0];
        const scaleB: number[] = [0];
        const offsetA: number[] = [0];
        const offsetB: number[] = [0];
        const rootA: number[] = [0];
        const rootB: number[] = [0];
        const positiveA: boolean[] = [false];
        const positiveB: boolean[] = [false];
        if (
            CsgSolidEquivalence.axisPlane(a, pa, axisA, scaleA, offsetA, rootA, positiveA) &&
            CsgSolidEquivalence.axisPlane(b, pb, axisB, scaleB, offsetB, rootB, positiveB)
        )
            return (
                axisA[0] === axisB[0] &&
                positiveA[0] === positiveB[0] &&
                CsgExactArithmetic.equalAffine(scaleA[0]!, rootA[0]!, offsetA[0]!, scaleB[0]!, rootB[0]!, offsetB[0]!)
            );
        const worldA: number[] = [0, 0, 0, 0];
        const worldB: number[] = [0, 0, 0, 0];
        if (CsgExactPlane.world(a, pa, worldA) && CsgExactPlane.world(b, pb, worldB))
            return CsgSolidEquivalence.proportional(worldA, worldB, 4);
        const axes: number[] = [0, 0, 0];
        const signs: number[] = [0, 0, 0];
        if (!CsgSolidEquivalence.signedPermutation(pa, pb, axes, signs)) return CsgDyadicSolid.samePlanes(a, pa, b, pb);
        const transformed: number[] = [
            signs[0]! * a[axes[0]!]!,
            signs[1]! * a[axes[1]!]!,
            signs[2]! * a[axes[2]!]!,
            a[3]!,
        ];
        return CsgSolidEquivalence.proportional(transformed, b, 4);
    }

    private static sameHalfSpace(a: PlanarHalfSpace, pa: Matrix4x4d, b: PlanarHalfSpace, pb: Matrix4x4d): boolean {
        const x = a.getBoundaryPlane();
        const y = b.getBoundaryPlane();
        const nx = x.getCoefficients();
        const ny = y.getCoefficients();
        const av: number[] = [nx.x(), nx.y(), nx.z(), x.getConstantCoefficient()];
        const bv: number[] = [ny.x(), ny.y(), ny.z(), y.getConstantCoefficient()];
        if ((av[0] === 0 && av[1] === 0 && av[2] === 0) || (bv[0] === 0 && bv[1] === 0 && bv[2] === 0)) return false;
        return CsgSolidEquivalence.sameOrientedPlane(av, pa, bv, pb);
    }

    private static sphereQuadric(sphere: Sphere, ps: Matrix4x4d, quadric: QuadricSurface, pq: Matrix4x4d): boolean {
        const sphereRadius = sphere.getRadius();
        if (!Double.isFinite(sphereRadius) || !(sphereRadius > 0)) return false;
        const squared = sphereRadius * sphereRadius;
        if (Double.isFinite(squared) && CsgExactArithmetic.equalProducts(sphereRadius, sphereRadius, squared, 1)) {
            const local: number[] = [1, 1, 1, 0, 0, 0, 0, 0, 0, -squared];
            const worldA: number[] = new Array<number>(10).fill(0);
            const worldB: number[] = new Array<number>(10).fill(0);
            if (
                CsgExactQuadric.world(local, ps, worldA) &&
                CsgSolidEquivalence.worldQuadric(quadric, pq, worldB) &&
                CsgSolidEquivalence.proportional(worldA, worldB, 10)
            )
                return true;
        }
        const sq = quadric.squareCoefficients();
        const mixed = quadric.mixedCoefficients();
        const lin = quadric.linearCoefficients();
        const coefficients: number[] = [
            sq.x(),
            sq.y(),
            sq.z(),
            mixed.x(),
            mixed.y(),
            mixed.z(),
            lin.x(),
            lin.y(),
            lin.z(),
            quadric.constantCoefficient(),
        ];
        if (CsgDyadicSolid.sphereQuadric(sphereRadius, ps, coefficients, pq)) return true;
        const axes: number[] = [0, 0, 0];
        const signs: number[] = [0, 0, 0];
        if (
            !CsgSolidEquivalence.sameTranslation(ps, pq) ||
            (!CsgSolidEquivalence.signedPermutation(ps, pq, axes, signs) &&
                !CsgSolidEquivalence.sameEllipsoidLinear(ps, 1, pq, 1))
        )
            return false;
        const q = sq.x();
        const radius = sphere.getRadius();
        const constant = quadric.constantCoefficient();
        if (
            !Double.isFinite(q) ||
            !Double.isFinite(radius) ||
            !Double.isFinite(constant) ||
            !(q > 0) ||
            !(radius > 0) ||
            sq.y() !== q ||
            sq.z() !== q ||
            mixed.x() !== 0 ||
            mixed.y() !== 0 ||
            mixed.z() !== 0 ||
            lin.x() !== 0 ||
            lin.y() !== 0 ||
            lin.z() !== 0
        )
            return false;
        // The intermediate must itself be exact; otherwise conservatively decline.
        const intermediate = q * radius;
        if (!Double.isFinite(intermediate) || !CsgExactArithmetic.equalProducts(q, radius, intermediate, 1))
            return false;
        return CsgExactArithmetic.equalProducts(intermediate, radius, -constant, 1);
    }

    private static halfSpaceQuadric(
        plane: PlanarHalfSpace,
        pp: Matrix4x4d,
        quadric: QuadricSurface,
        pq: Matrix4x4d,
    ): boolean {
        const sq = quadric.squareCoefficients();
        const mixed = quadric.mixedCoefficients();
        const lin = quadric.linearCoefficients();
        if (sq.x() !== 0 || sq.y() !== 0 || sq.z() !== 0 || mixed.x() !== 0 || mixed.y() !== 0 || mixed.z() !== 0)
            return false;
        const boundary = plane.getBoundaryPlane();
        const n = boundary.getCoefficients();
        if ((n.x() === 0 && n.y() === 0 && n.z() === 0) || (lin.x() === 0 && lin.y() === 0 && lin.z() === 0))
            return false;
        const a: number[] = [n.x(), n.y(), n.z(), boundary.getConstantCoefficient()];
        const b: number[] = [lin.x(), lin.y(), lin.z(), quadric.constantCoefficient()];
        return CsgSolidEquivalence.sameOrientedPlane(a, pp, b, pq);
    }

    private static worldQuadric(quadric: QuadricSurface, placement: Matrix4x4d, result: number[]): boolean {
        const sq = quadric.squareCoefficients();
        const mixed = quadric.mixedCoefficients();
        const lin = quadric.linearCoefficients();
        const local: number[] = [
            sq.x(),
            sq.y(),
            sq.z(),
            mixed.x(),
            mixed.y(),
            mixed.z(),
            lin.x(),
            lin.y(),
            lin.z(),
            quadric.constantCoefficient(),
        ];
        return CsgExactQuadric.world(local, placement, result);
    }

    private static sameQuadric(a: QuadricSurface, pa: Matrix4x4d, b: QuadricSurface, pb: Matrix4x4d): boolean {
        const worldA: number[] = new Array<number>(10).fill(0);
        const worldB: number[] = new Array<number>(10).fill(0);
        if (CsgSolidEquivalence.worldQuadric(a, pa, worldA) && CsgSolidEquivalence.worldQuadric(b, pb, worldB))
            return CsgSolidEquivalence.proportional(worldA, worldB, 10);
        const as = a.squareCoefficients();
        const am = a.mixedCoefficients();
        const al = a.linearCoefficients();
        const bs = b.squareCoefficients();
        const bm = b.mixedCoefficients();
        const bl = b.linearCoefficients();
        const localA: number[] = [
            as.x(),
            as.y(),
            as.z(),
            am.x(),
            am.y(),
            am.z(),
            al.x(),
            al.y(),
            al.z(),
            a.constantCoefficient(),
        ];
        const localB: number[] = [
            bs.x(),
            bs.y(),
            bs.z(),
            bm.x(),
            bm.y(),
            bm.z(),
            bl.x(),
            bl.y(),
            bl.z(),
            b.constantCoefficient(),
        ];
        if (CsgDyadicSolid.sameQuadrics(localA, pa, localB, pb)) return true;
        const axes: number[] = [0, 0, 0];
        const signs: number[] = [0, 0, 0];
        if (!CsgSolidEquivalence.signedPermutation(pa, pb, axes, signs)) return false;
        const sq: number[] = [as.x(), as.y(), as.z()];
        const lin: number[] = [al.x(), al.y(), al.z()];
        const mixed: number[][] = [
            [0, am.x(), am.y()],
            [am.x(), 0, am.z()],
            [am.y(), am.z(), 0],
        ];
        const av: number[] = [
            sq[axes[0]!]!,
            sq[axes[1]!]!,
            sq[axes[2]!]!,
            signs[0]! * signs[1]! * mixed[axes[0]!]![axes[1]!]!,
            signs[0]! * signs[2]! * mixed[axes[0]!]![axes[2]!]!,
            signs[1]! * signs[2]! * mixed[axes[1]!]![axes[2]!]!,
            signs[0]! * lin[axes[0]!]!,
            signs[1]! * lin[axes[1]!]!,
            signs[2]! * lin[axes[2]!]!,
            a.constantCoefficient(),
        ];
        const bv: number[] = [
            bs.x(),
            bs.y(),
            bs.z(),
            bm.x(),
            bm.y(),
            bm.z(),
            bl.x(),
            bl.y(),
            bl.z(),
            b.constantCoefficient(),
        ];
        return CsgSolidEquivalence.proportional(av, bv, 10);
    }

    public static same(a: SceneSolid, b: SceneSolid): boolean {
        if (a === b) return true;
        // Each scene specialization explicitly exposes its unmodified core solid.
        const ga = a.getSolidEquivalenceGeometry();
        const gb = b.getSolidEquivalenceGeometry();
        if (ga === null || gb === null) return false;
        const pa: Matrix4x4d[] = [new Matrix4x4d()];
        const ia: Matrix4x4d[] = [new Matrix4x4d()];
        const pb: Matrix4x4d[] = [new Matrix4x4d()];
        const ib: Matrix4x4d[] = [new Matrix4x4d()];
        if (
            a.queryAffinePlacement(pa, ia) !== AffinePlacementStatus.Valid ||
            b.queryAffinePlacement(pb, ib) !== AffinePlacementStatus.Valid
        )
            return false;
        const sa = ga instanceof Sphere ? ga : null;
        const sb = gb instanceof Sphere ? gb : null;
        if (sa !== null && sb !== null && sa.constructor === Sphere && sb.constructor === Sphere) {
            const ra = sa.getRadius();
            const rb = sb.getRadius();
            if (
                !Double.isFinite(ra) ||
                !Double.isFinite(rb) ||
                ra <= 0 ||
                rb <= 0 ||
                !CsgSolidEquivalence.sameTranslation(pa[0]!, pb[0]!)
            )
                return false;
            const used: boolean[] = [false, false, false];
            for (let j = 0; j < 3; ++j) {
                let found = false;
                for (let k = 0; k < 3 && !found; ++k)
                    if (!used[k]) {
                        let same = true;
                        let reverse = true;
                        for (let i = 0; i < 3; ++i) {
                            if (!CsgExactArithmetic.equalProducts(pa[0]!.get(i, k), ra, pb[0]!.get(i, j), rb))
                                same = false;
                            if (!CsgExactArithmetic.equalProducts(pa[0]!.get(i, k), ra, -pb[0]!.get(i, j), rb))
                                reverse = false;
                        }
                        if (same || reverse) {
                            used[k] = true;
                            found = true;
                        }
                    }
                if (!found) return CsgSolidEquivalence.sameEllipsoidLinear(pa[0]!, ra, pb[0]!, rb);
            }
            return true;
        }
        const ba = ga instanceof Box ? ga : null;
        const bb = gb instanceof Box ? gb : null;
        if (ba !== null && bb !== null && ba.constructor === Box && bb.constructor === Box)
            return CsgSolidEquivalence.sameBox(ba, pa[0]!, bb, pb[0]!);
        const ha = ga instanceof PlanarHalfSpace ? ga : null;
        const hb = gb instanceof PlanarHalfSpace ? gb : null;
        if (ha !== null && hb !== null && ha.constructor === PlanarHalfSpace && hb.constructor === PlanarHalfSpace)
            return CsgSolidEquivalence.sameHalfSpace(ha, pa[0]!, hb, pb[0]!);
        const qa = ga instanceof QuadricSurface ? ga : null;
        const qb = gb instanceof QuadricSurface ? gb : null;
        if (qa !== null && qb !== null && qa.constructor === QuadricSurface && qb.constructor === QuadricSurface)
            return CsgSolidEquivalence.sameQuadric(qa, pa[0]!, qb, pb[0]!);
        if (sa !== null && qb !== null && sa.constructor === Sphere && qb.constructor === QuadricSurface)
            return CsgSolidEquivalence.sphereQuadric(sa, pa[0]!, qb, pb[0]!);
        if (sb !== null && qa !== null && sb.constructor === Sphere && qa.constructor === QuadricSurface)
            return CsgSolidEquivalence.sphereQuadric(sb, pb[0]!, qa, pa[0]!);
        if (ha !== null && qb !== null && ha.constructor === PlanarHalfSpace && qb.constructor === QuadricSurface)
            return CsgSolidEquivalence.halfSpaceQuadric(ha, pa[0]!, qb, pb[0]!);
        if (hb !== null && qa !== null && hb.constructor === PlanarHalfSpace && qa.constructor === QuadricSurface)
            return CsgSolidEquivalence.halfSpaceQuadric(hb, pb[0]!, qa, pa[0]!);
        return false;
    }
}
