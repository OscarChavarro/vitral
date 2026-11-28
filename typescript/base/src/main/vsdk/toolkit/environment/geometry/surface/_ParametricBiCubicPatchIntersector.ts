//= References:                                                             =
//= [WAYN1990] Knapp Wayne. "Ray with Bicubic Patch Intersection Problem",  =
//=            Ray Tracing News, volume 3, number 3, july 13 1990.          =
//= [FOLE1992] Foley, vanDam, Feiner, Hughes. "Computer Graphics, princi-   =
//=            ples and practice" - second edition, Addison Wesley, 1992.   =
//= [POVR1993] POV-Ray 1.0/2.x "bezier.c" bicubic_patch implementation, as  =
//=            ported to C++ in the povCpp project (ParametricPatch.cpp,    =
//=            ParametricBiCubicIntersection.cpp).                          =

import { VSDK } from "../../../common/VSDK.js";
import type { Matrix4x4d } from "../../../common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { Ray } from "../element/Ray.js";
import { Double } from "../../../../../java/lang/Double.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { ArrayIndexOutOfBoundsException } from "../../../../../java/lang/ArrayIndexOutOfBoundsException.js";

/**
Node in the bounding sphere hierarchy. Interior nodes have `children`,
leaves have the four corner points of the sub-patch and its parametric
range. (Java's private static nested class `PatchNode`.)
*/
class _PatchNode {
    public readonly centerX: number;
    public readonly centerY: number;
    public readonly centerZ: number;
    public readonly radiusSquared: number;
    public readonly children: _PatchNode[] | null;
    public readonly corners: Float64Array | null;
    public readonly uMin: number;
    public readonly uMax: number;
    public readonly vMin: number;
    public readonly vMax: number;

    public constructor(
        net: Float64Array,
        children: _PatchNode[] | null,
        u0: number,
        u1: number,
        v0: number,
        v1: number,
    ) {
        const sphere = _ParametricBiCubicPatchIntersector.boundingSphere(net);
        this.centerX = sphere[0]!;
        this.centerY = sphere[1]!;
        this.centerZ = sphere[2]!;
        this.radiusSquared = sphere[3]!;
        this.children = children;
        this.uMin = u0;
        this.uMax = u1;
        this.vMin = v0;
        this.vMax = v1;
        if (children === null) {
            // Corners (u0, v0), (u0, v1), (u1, v1) and (u1, v0)
            this.corners = new Float64Array(12);
            _ParametricBiCubicPatchIntersector.copyPoint(net, 0, 0, this.corners, 0);
            _ParametricBiCubicPatchIntersector.copyPoint(net, 0, 3, this.corners, 1);
            _ParametricBiCubicPatchIntersector.copyPoint(net, 3, 3, this.corners, 2);
            _ParametricBiCubicPatchIntersector.copyPoint(net, 3, 0, this.corners, 3);
        } else {
            this.corners = null;
        }
    }
}

/**
Result of a ray/patch query, in patch parameter space. (Java's package
private static nested class `_ParametricBiCubicPatchIntersector.PatchHit`.)
*/
export class _PatchHit {
    public t: number;
    public u = 0;
    public v = 0;
    public pointX = 0;
    public pointY = 0;
    public pointZ = 0;
    public triangleNx = 0;
    public triangleNy = 0;
    public triangleNz = 0;

    public constructor() {
        this.t = Double.MAX_VALUE;
    }
}

/**
Ray / bicubic patch intersection support for `ParametricBiCubicPatch`.

This follows the "subdivision tree" strategy (POV-Ray bicubic_patch type 1/3,
as replicated on povCpp) discussed in [WAYN1990]: the patch is converted to
its Bezier control net, the net is recursively split by de Casteljau
subdivision until every sub-patch is flat enough, and a tree of bounding
spheres around the sub-patch control nets is built. Each ray walks the tree,
discarding sub-trees whose bounding sphere is missed, and leaf sub-patches are
tested as two triangles built from their corners.

Differences with respect to the povCpp version, aimed at rendering quality:
<UL>
  <LI> All leaves are at the same subdivision depth, so neighboring leaves
  share their corner points and the triangle approximation has no cracks
  (T-junctions) that would let rays leak through the surface.
  <LI> The flatness criterion measures the distance from the control net to
  the bilinear interpolation of the sub-patch corners. That bounds both the
  out of plane bending (as in povCpp) and the in plane curvature of the
  sub-patch borders.
  <LI> The (u, v) parameters of the triangle hit are refined with a few Newton
  iterations on the exact surface, so the reported point, normal, tangent and
  texture coordinates are the ones of the true bicubic surface.
</UL>

Instances are immutable after construction, so they can be shared by
multiple raytracing threads.
*/
export class _ParametricBiCubicPatchIntersector {
    /// Inverse of the Bezier basis matrix [FOLE1992].11.28, used to convert
    /// a power basis coefficient matrix into a Bezier control net.
    private static readonly INVERSE_BEZIER_MATRIX: number[][] = [
        [0.0, 0.0, 0.0, 1.0],
        [0.0, 0.0, 1.0 / 3.0, 1.0],
        [0.0, 1.0 / 3.0, 2.0 / 3.0, 1.0],
        [1.0, 1.0, 1.0, 1.0],
    ];

    private static readonly DEFAULT_RELATIVE_FLATNESS = 1.0e-3;
    private static readonly MIN_SUBDIVISION_DEPTH = 2;
    private static readonly MAX_SUBDIVISION_DEPTH = 7;
    private static readonly MAX_NEWTON_ITERATIONS = 6;
    private static readonly NEWTON_TOLERANCE = 1.0e-12;
    private static readonly BARYCENTRIC_TOLERANCE = 1.0e-9;

    /// Bezier control net, stored as 16 points of 3 coordinates, with point
    /// (i, j) at index 3*(4*i + j). Index i follows the s (u) parameter and
    /// index j the t (v) parameter of `ParametricBiCubicPatch.evaluate`.
    private readonly bezierNet: Float64Array;
    private readonly root: _PatchNode;
    private readonly subdivisionDepth: number;

    /**
    Builds the intersection structures for the patch with the given power
    basis coefficient matrices (`M * G * Mt` for each coordinate).
    @param coefficientMatrixX x coordinate coefficients
    @param coefficientMatrixY y coordinate coefficients
    @param coefficientMatrixZ z coordinate coefficients
    */
    public constructor(coefficientMatrixX: Matrix4x4d, coefficientMatrixY: Matrix4x4d, coefficientMatrixZ: Matrix4x4d) {
        this.bezierNet = new Float64Array(48);
        this.storeBezierCoordinate(coefficientMatrixX, 0);
        this.storeBezierCoordinate(coefficientMatrixY, 1);
        this.storeBezierCoordinate(coefficientMatrixZ, 2);

        const rootSphere = _ParametricBiCubicPatchIntersector.boundingSphere(this.bezierNet);
        let tolerance = _ParametricBiCubicPatchIntersector.DEFAULT_RELATIVE_FLATNESS * JavaMath.sqrt(rootSphere[3]!);
        if (tolerance < VSDK.EPSILON * VSDK.EPSILON) {
            tolerance = VSDK.EPSILON * VSDK.EPSILON;
        }
        let depth = _ParametricBiCubicPatchIntersector.requiredSubdivisionDepth(this.bezierNet, 0, tolerance);
        if (depth < _ParametricBiCubicPatchIntersector.MIN_SUBDIVISION_DEPTH) {
            depth = _ParametricBiCubicPatchIntersector.MIN_SUBDIVISION_DEPTH;
        }
        this.subdivisionDepth = depth;
        this.root = this.buildTree(this.bezierNet, 0, 0.0, 1.0, 0.0, 1.0);
    }

    /**
    @return the number of de Casteljau subdivision levels used for the
    leaves of the bounding sphere tree
    */
    public getSubdivisionDepth(): number {
        return this.subdivisionDepth;
    }

    /**
    @param i control point index on the s (u) direction, in [0, 3]
    @param j control point index on the t (v) direction, in [0, 3]
    @return the Bezier control point (i, j) equivalent to current patch
    */
    public getBezierControlPoint(i: number, j: number): Vector3Dd {
        const index = 3 * (4 * i + j);
        return new Vector3Dd(
            _ParametricBiCubicPatchIntersector.at(this.bezierNet, index),
            _ParametricBiCubicPatchIntersector.at(this.bezierNet, index + 1),
            _ParametricBiCubicPatchIntersector.at(this.bezierNet, index + 2),
        );
    }

    /**
    Bounding box of the Bezier control net. By the convex hull property it
    contains the whole patch.
    @return min-max array as specified by `Geometry.getMinMax`
    */
    public getMinMax(): number[] {
        const minMax = [
            Double.MAX_VALUE,
            Double.MAX_VALUE,
            Double.MAX_VALUE,
            -Double.MAX_VALUE,
            -Double.MAX_VALUE,
            -Double.MAX_VALUE,
        ];
        for (let k = 0; k < 16; k++) {
            for (let c = 0; c < 3; c++) {
                const value = this.bezierNet[3 * k + c]!;
                if (value < minMax[c]!) {
                    minMax[c] = value;
                }
                if (value > minMax[c + 3]!) {
                    minMax[c + 3] = value;
                }
            }
        }
        return minMax;
    }

    /**
    Finds the nearest intersection of the ray with the patch.
    @param ray ray to test, its direction does not need to be normalized
    @return nearest hit with positive ray parameter, or null if the ray
    misses the patch
    */
    public intersect(ray: Ray): _PatchHit | null {
        const origin = ray.getOrigin();
        const direction = ray.getDirection();
        const o = [origin.x(), origin.y(), origin.z()];
        const d = [direction.x(), direction.y(), direction.z()];
        const directionLengthSquared = d[0]! * d[0]! + d[1]! * d[1]! + d[2]! * d[2]!;

        if (directionLengthSquared <= 0.0) {
            return null;
        }

        const best = new _PatchHit();
        this.walkTree(this.root, o, d, directionLengthSquared, best);
        if (best.t === Double.MAX_VALUE) {
            return null;
        }
        return best;
    }

    /**
    Evaluates the patch and its first partial derivatives.
    @param u parameter in the s direction
    @param v parameter in the t direction
    @param out receives point (0..2), dS/du (3..5) and dS/dv (6..8)
    */
    public evaluate(u: number, v: number, out: number[] | Float64Array): void {
        const bu = [0, 0, 0, 0];
        const dbu = [0, 0, 0, 0];
        const bv = [0, 0, 0, 0];
        const dbv = [0, 0, 0, 0];

        _ParametricBiCubicPatchIntersector.bernstein(u, bu, dbu);
        _ParametricBiCubicPatchIntersector.bernstein(v, bv, dbv);
        for (let k = 0; k < 9; k++) {
            out[k] = 0.0;
        }
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                const index = 3 * (4 * i + j);
                const w = bu[i]! * bv[j]!;
                const wu = dbu[i]! * bv[j]!;
                const wv = bu[i]! * dbv[j]!;
                for (let c = 0; c < 3; c++) {
                    const p = this.bezierNet[index + c]!;
                    out[c] = out[c]! + w * p;
                    out[3 + c] = out[3 + c]! + wu * p;
                    out[6 + c] = out[6 + c]! + wv * p;
                }
            }
        }
    }

    //= Construction ======================================================

    /**
    Computes B = Minv * C * Minv^t, where C is a power basis coefficient
    matrix and Minv the inverse Bezier basis matrix, and stores B into the
    given coordinate of the Bezier net.
    */
    private storeBezierCoordinate(coefficients: Matrix4x4d, coordinate: number): void {
        const inverse = _ParametricBiCubicPatchIntersector.INVERSE_BEZIER_MATRIX;
        const temp: number[][] = [
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
            [0, 0, 0, 0],
        ];
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                let accum = 0.0;
                for (let k = 0; k < 4; k++) {
                    accum += inverse[i]![k]! * coefficients.get(k, j);
                }
                temp[i]![j] = accum;
            }
        }
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                let accum = 0.0;
                for (let k = 0; k < 4; k++) {
                    accum += temp[i]![k]! * inverse[j]![k]!;
                }
                this.bezierNet[3 * (4 * i + j) + coordinate] = accum;
            }
        }
    }

    private static requiredSubdivisionDepth(net: Float64Array, depth: number, tolerance: number): number {
        if (
            depth >= _ParametricBiCubicPatchIntersector.MAX_SUBDIVISION_DEPTH ||
            _ParametricBiCubicPatchIntersector.subPatchFlatness(net) < tolerance
        ) {
            return depth;
        }
        const quarters = _ParametricBiCubicPatchIntersector.splitInFour(net);
        let maxDepth = depth;
        for (const quarter of quarters) {
            const childDepth = _ParametricBiCubicPatchIntersector.requiredSubdivisionDepth(
                quarter,
                depth + 1,
                tolerance,
            );
            if (childDepth > maxDepth) {
                maxDepth = childDepth;
                if (maxDepth >= _ParametricBiCubicPatchIntersector.MAX_SUBDIVISION_DEPTH) {
                    break;
                }
            }
        }
        return maxDepth;
    }

    /**
    Port of povCpp `parametricTreeBuilder`, with a uniform leaf depth.
    */
    private buildTree(net: Float64Array, depth: number, u0: number, u1: number, v0: number, v1: number): _PatchNode {
        if (depth >= this.subdivisionDepth) {
            return new _PatchNode(net, null, u0, u1, v0, v1);
        }
        const um = (u0 + u1) / 2.0;
        const vm = (v0 + v1) / 2.0;
        const quarters = _ParametricBiCubicPatchIntersector.splitInFour(net);
        const children: _PatchNode[] = new Array<_PatchNode>(4);
        children[0] = this.buildTree(quarters[0]!, depth + 1, u0, um, v0, vm);
        children[1] = this.buildTree(quarters[1]!, depth + 1, u0, um, vm, v1);
        children[2] = this.buildTree(quarters[2]!, depth + 1, um, u1, v0, vm);
        children[3] = this.buildTree(quarters[3]!, depth + 1, um, u1, vm, v1);
        return new _PatchNode(net, children, u0, u1, v0, v1);
    }

    /**
    @return the four quarters of the net, in order (low u, low v),
    (low u, high v), (high u, low v) and (high u, high v)
    */
    private static splitInFour(net: Float64Array): Float64Array[] {
        const lowU = new Float64Array(48);
        const highU = new Float64Array(48);
        _ParametricBiCubicPatchIntersector.splitU(net, lowU, highU);

        const quarters = [new Float64Array(48), new Float64Array(48), new Float64Array(48), new Float64Array(48)];
        _ParametricBiCubicPatchIntersector.splitV(lowU, quarters[0]!, quarters[1]!);
        _ParametricBiCubicPatchIntersector.splitV(highU, quarters[2]!, quarters[3]!);
        return quarters;
    }

    /**
    Splits the net at u = 1/2 (povCpp `parametricSplitUpDown`).
    */
    private static splitU(net: Float64Array, low: Float64Array, high: Float64Array): void {
        for (let j = 0; j < 4; j++) {
            for (let c = 0; c < 3; c++) {
                _ParametricBiCubicPatchIntersector.deCasteljau(
                    net[3 * (0 + j) + c]!,
                    net[3 * (4 + j) + c]!,
                    net[3 * (8 + j) + c]!,
                    net[3 * (12 + j) + c]!,
                    low,
                    high,
                    3 * j + c,
                    12,
                );
            }
        }
    }

    /**
    Splits the net at v = 1/2 (povCpp `parametricSplitLeftRight`).
    */
    private static splitV(net: Float64Array, low: Float64Array, high: Float64Array): void {
        for (let i = 0; i < 4; i++) {
            for (let c = 0; c < 3; c++) {
                const base = 12 * i + c;
                _ParametricBiCubicPatchIntersector.deCasteljau(
                    net[base]!,
                    net[base + 3]!,
                    net[base + 6]!,
                    net[base + 9]!,
                    low,
                    high,
                    base,
                    3,
                );
            }
        }
    }

    private static deCasteljau(
        p0: number,
        p1: number,
        p2: number,
        p3: number,
        low: Float64Array,
        high: Float64Array,
        start: number,
        stride: number,
    ): void {
        const p01 = (p0 + p1) / 2.0;
        const p12 = (p1 + p2) / 2.0;
        const p23 = (p2 + p3) / 2.0;
        const p012 = (p01 + p12) / 2.0;
        const p123 = (p12 + p23) / 2.0;
        const middle = (p012 + p123) / 2.0;

        low[start] = p0;
        low[start + stride] = p01;
        low[start + 2 * stride] = p012;
        low[start + 3 * stride] = middle;
        high[start] = middle;
        high[start + stride] = p123;
        high[start + 2 * stride] = p23;
        high[start + 3 * stride] = p3;
    }

    /**
    Maximum distance between the control net and the bilinear patch spanned
    by its four corners, plus the deviation of that bilinear patch with
    respect to its two triangles (half the corner twist). When this value is
    small, the two corner triangles approximate the sub-patch.
    */
    private static subPatchFlatness(net: Float64Array): number {
        let maxDistanceSquared = 0.0;
        for (let i = 0; i < 4; i++) {
            const a = i / 3.0;
            for (let j = 0; j < 4; j++) {
                const b = j / 3.0;
                let distanceSquared = 0.0;
                for (let c = 0; c < 3; c++) {
                    const bilinear =
                        (1 - a) * (1 - b) * net[c]! +
                        (1 - a) * b * net[9 + c]! +
                        a * (1 - b) * net[36 + c]! +
                        a * b * net[45 + c]!;
                    const delta = net[3 * (4 * i + j) + c]! - bilinear;
                    distanceSquared += delta * delta;
                }
                if (distanceSquared > maxDistanceSquared) {
                    maxDistanceSquared = distanceSquared;
                }
            }
        }
        let twistSquared = 0.0;
        for (let c = 0; c < 3; c++) {
            const twist = (net[c]! - net[9 + c]! - net[36 + c]! + net[45 + c]!) / 4.0;
            twistSquared += twist * twist;
        }
        return JavaMath.sqrt(maxDistanceSquared) + JavaMath.sqrt(twistSquared);
    }

    /**
    Port of povCpp `parametricBoundingSphere`.
    @return center (0..2) and squared radius (3) of a sphere containing the
    16 control points of the net
    */
    public static boundingSphere(net: Float64Array): number[] {
        let xc = 0.0;
        let yc = 0.0;
        let zc = 0.0;
        for (let k = 0; k < 16; k++) {
            xc += net[3 * k]!;
            yc += net[3 * k + 1]!;
            zc += net[3 * k + 2]!;
        }
        xc /= 16.0;
        yc /= 16.0;
        zc /= 16.0;
        let r0 = 0.0;
        for (let k = 0; k < 16; k++) {
            const x0 = net[3 * k]! - xc;
            const y0 = net[3 * k + 1]! - yc;
            const z0 = net[3 * k + 2]! - zc;
            const r1 = x0 * x0 + y0 * y0 + z0 * z0;
            if (r1 > r0) {
                r0 = r1;
            }
        }
        // Small inflation so that numerically flat leaves keep a usable
        // bounding volume
        const inflation = 1.0e-9 * (1.0 + JavaMath.abs(xc) + JavaMath.abs(yc) + JavaMath.abs(zc));
        const radius = JavaMath.sqrt(r0) + inflation;
        return [xc, yc, zc, radius * radius];
    }

    public static copyPoint(net: Float64Array, i: number, j: number, target: Float64Array, targetIndex: number): void {
        const source = 3 * (4 * i + j);
        target[3 * targetIndex] = net[source]!;
        target[3 * targetIndex + 1] = net[source + 1]!;
        target[3 * targetIndex + 2] = net[source + 2]!;
    }

    //= Ray queries =======================================================

    /**
    Port of povCpp `parametricTreeWalker`, keeping only the nearest hit and
    pruning sub-trees farther than the current nearest hit.
    */
    private walkTree(
        node: _PatchNode,
        o: number[],
        d: number[],
        directionLengthSquared: number,
        best: _PatchHit,
    ): void {
        if (!_ParametricBiCubicPatchIntersector.sphericalBoundsCheck(node, o, d, directionLengthSquared, best.t)) {
            return;
        }
        if (node.children !== null) {
            for (const child of node.children) {
                this.walkTree(child, o, d, directionLengthSquared, best);
            }
            return;
        }

        // Triangulate this sub-patch, then check for intersections in the
        // triangles (0, 1, 2) and (0, 2, 3)
        const c = node.corners!;
        const result = [0, 0, 0];
        if (_ParametricBiCubicPatchIntersector.intersectTriangle(o, d, c, 0, 1, 2, result) && result[0]! < best.t) {
            const u = node.uMin + result[2]! * (node.uMax - node.uMin);
            const v = node.vMin + (result[1]! + result[2]!) * (node.vMax - node.vMin);
            this.acceptHit(node, o, d, c, 0, 1, 2, result[0]!, u, v, best);
        }
        if (_ParametricBiCubicPatchIntersector.intersectTriangle(o, d, c, 0, 2, 3, result) && result[0]! < best.t) {
            const u = node.uMin + (result[1]! + result[2]!) * (node.uMax - node.uMin);
            const v = node.vMin + result[1]! * (node.vMax - node.vMin);
            this.acceptHit(node, o, d, c, 0, 2, 3, result[0]!, u, v, best);
        }
    }

    /**
    Port of povCpp `sphericalBoundsCheck`, extended to reject spheres whose
    entry point lies beyond the nearest hit already found.
    */
    private static sphericalBoundsCheck(
        node: _PatchNode,
        o: number[],
        d: number[],
        directionLengthSquared: number,
        maxT: number,
    ): boolean {
        const x = node.centerX - o[0]!;
        const y = node.centerY - o[1]!;
        const z = node.centerZ - o[2]!;
        const distanceSquared = x * x + y * y + z * z;
        if (distanceSquared < node.radiusSquared) {
            // Ray starts inside sphere - assume it intersects
            return true;
        }
        const projection = x * d[0]! + y * d[1]! + z * d[2]!;
        if (projection <= 0.0) {
            return false;
        }
        const closestDistanceSquared = distanceSquared - (projection * projection) / directionLengthSquared;
        if (closestDistanceSquared >= node.radiusSquared) {
            return false;
        }
        const halfChord = JavaMath.sqrt((node.radiusSquared - closestDistanceSquared) / directionLengthSquared);
        const entryT = projection / directionLengthSquared - halfChord;
        return entryT < maxT;
    }

    /**
    Moller-Trumbore ray/triangle test over the leaf corners a, b and c.
    @param result receives t (0) and the barycentric coordinates of the hit
    with respect to b (1) and c (2)
    @return true if the ray hits the triangle at a positive distance
    */
    private static intersectTriangle(
        o: number[],
        d: number[],
        corners: Float64Array,
        a: number,
        b: number,
        c: number,
        result: number[],
    ): boolean {
        const ax = corners[3 * a]!;
        const ay = corners[3 * a + 1]!;
        const az = corners[3 * a + 2]!;
        const e1x = corners[3 * b]! - ax;
        const e1y = corners[3 * b + 1]! - ay;
        const e1z = corners[3 * b + 2]! - az;
        const e2x = corners[3 * c]! - ax;
        const e2y = corners[3 * c + 1]! - ay;
        const e2z = corners[3 * c + 2]! - az;

        const px = d[1]! * e2z - d[2]! * e2y;
        const py = d[2]! * e2x - d[0]! * e2z;
        const pz = d[0]! * e2y - d[1]! * e2x;
        const determinant = e1x * px + e1y * py + e1z * pz;
        const scale =
            JavaMath.abs(e1x) +
            JavaMath.abs(e1y) +
            JavaMath.abs(e1z) +
            JavaMath.abs(e2x) +
            JavaMath.abs(e2y) +
            JavaMath.abs(e2z);
        if (JavaMath.abs(determinant) <= 1.0e-14 * scale * scale) {
            // Degenerate triangle or ray parallel to its plane
            return false;
        }
        const inverse = 1.0 / determinant;

        const sx = o[0]! - ax;
        const sy = o[1]! - ay;
        const sz = o[2]! - az;
        const b1 = (sx * px + sy * py + sz * pz) * inverse;
        const tol = _ParametricBiCubicPatchIntersector.BARYCENTRIC_TOLERANCE;
        if (b1 < -tol || b1 > 1.0 + tol) {
            return false;
        }
        const qx = sy * e1z - sz * e1y;
        const qy = sz * e1x - sx * e1z;
        const qz = sx * e1y - sy * e1x;
        const b2 = (d[0]! * qx + d[1]! * qy + d[2]! * qz) * inverse;
        if (b2 < -tol || b1 + b2 > 1.0 + tol) {
            return false;
        }
        const t = (e2x * qx + e2y * qy + e2z * qz) * inverse;
        if (t <= VSDK.EPSILON) {
            return false;
        }
        result[0] = t;
        result[1] = JavaMath.max(0.0, b1);
        result[2] = JavaMath.max(0.0, b2);
        return true;
    }

    /**
    Stores a leaf triangle hit, refining its parameters on the exact
    surface when the Newton iteration converges near the leaf.
    */
    private acceptHit(
        node: _PatchNode,
        o: number[],
        d: number[],
        corners: Float64Array,
        a: number,
        b: number,
        c: number,
        t: number,
        u: number,
        v: number,
        best: _PatchHit,
    ): void {
        const refined = [u, v, t];
        const du = node.uMax - node.uMin;
        const dv = node.vMax - node.vMin;
        if (
            this.refineOnSurface(o, d, refined) &&
            refined[0]! >= node.uMin - du &&
            refined[0]! <= node.uMax + du &&
            refined[1]! >= node.vMin - dv &&
            refined[1]! <= node.vMax + dv
        ) {
            if (refined[2]! <= VSDK.EPSILON) {
                // The exact surface root is the ray origin itself (i.e. a
                // shadow or reflected ray leaving the patch): the triangle
                // hit is only an approximation artifact and must be ignored
                return;
            }
            u = refined[0]!;
            v = refined[1]!;
            t = refined[2]!;
        }
        if (t >= best.t) {
            return;
        }

        best.t = t;
        best.u = _ParametricBiCubicPatchIntersector.clamp01(u);
        best.v = _ParametricBiCubicPatchIntersector.clamp01(v);
        best.pointX = o[0]! + t * d[0]!;
        best.pointY = o[1]! + t * d[1]!;
        best.pointZ = o[2]! + t * d[2]!;

        const e1x = corners[3 * b]! - corners[3 * a]!;
        const e1y = corners[3 * b + 1]! - corners[3 * a + 1]!;
        const e1z = corners[3 * b + 2]! - corners[3 * a + 2]!;
        const e2x = corners[3 * c]! - corners[3 * a]!;
        const e2y = corners[3 * c + 1]! - corners[3 * a + 1]!;
        const e2z = corners[3 * c + 2]! - corners[3 * a + 2]!;
        best.triangleNx = e1y * e2z - e1z * e2y;
        best.triangleNy = e1z * e2x - e1x * e2z;
        best.triangleNz = e1x * e2y - e1y * e2x;
    }

    /**
    Newton iteration over F(u, v, t) = S(u, v) - (o + t * d) = 0.
    @param uvt initial guess as input, solution as output
    @return true if the iteration converged
    */
    private refineOnSurface(o: number[], d: number[], uvt: number[]): boolean {
        const s = new Float64Array(9);
        const det = _ParametricBiCubicPatchIntersector.determinant3x3;
        for (let iteration = 0; iteration < _ParametricBiCubicPatchIntersector.MAX_NEWTON_ITERATIONS; iteration++) {
            this.evaluate(uvt[0]!, uvt[1]!, s);
            const fx = s[0]! - (o[0]! + uvt[2]! * d[0]!);
            const fy = s[1]! - (o[1]! + uvt[2]! * d[1]!);
            const fz = s[2]! - (o[2]! + uvt[2]! * d[2]!);

            // Jacobian columns: dS/du, dS/dv, -d
            const determinant = det(s[3]!, s[6]!, -d[0]!, s[4]!, s[7]!, -d[1]!, s[5]!, s[8]!, -d[2]!);
            if (JavaMath.abs(determinant) < 1.0e-30) {
                return false;
            }
            const deltaU = det(fx, s[6]!, -d[0]!, fy, s[7]!, -d[1]!, fz, s[8]!, -d[2]!) / determinant;
            const deltaV = det(s[3]!, fx, -d[0]!, s[4]!, fy, -d[1]!, s[5]!, fz, -d[2]!) / determinant;
            const deltaT = det(s[3]!, s[6]!, fx, s[4]!, s[7]!, fy, s[5]!, s[8]!, fz) / determinant;
            uvt[0] = uvt[0]! - deltaU;
            uvt[1] = uvt[1]! - deltaV;
            uvt[2] = uvt[2]! - deltaT;
            if (Double.isNaN(uvt[0]) || Double.isNaN(uvt[1]) || Double.isNaN(uvt[2])) {
                return false;
            }
            if (JavaMath.abs(deltaU) + JavaMath.abs(deltaV) < _ParametricBiCubicPatchIntersector.NEWTON_TOLERANCE) {
                return true;
            }
        }
        return false;
    }

    private static determinant3x3(
        a: number,
        b: number,
        c: number,
        d: number,
        e: number,
        f: number,
        g: number,
        h: number,
        i: number,
    ): number {
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g);
    }

    private static bernstein(t: number, b: number[], db: number[]): void {
        const s = 1.0 - t;
        b[0] = s * s * s;
        b[1] = 3.0 * t * s * s;
        b[2] = 3.0 * t * t * s;
        b[3] = t * t * t;
        db[0] = -3.0 * s * s;
        db[1] = 3.0 * s * s - 6.0 * t * s;
        db[2] = 6.0 * t * s - 3.0 * t * t;
        db[3] = 3.0 * t * t;
    }

    private static clamp01(value: number): number {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    /** Java `double[]` read: an index outside the net throws. */
    private static at(array: Float64Array, index: number): number {
        if (index < 0 || index >= array.length) {
            throw new ArrayIndexOutOfBoundsException("Index " + index + " out of bounds for length " + array.length);
        }
        return array[index]!;
    }
}
