import { JavaArray } from "../../../../../java/lang/JavaArray.js";
import { Double } from "../../../../../java/lang/Double.js";
import { IllegalArgumentException } from "../../../../../java/lang/IllegalArgumentException.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { Geometry } from "../Geometry.js";
import { GeometryBounds } from "../element/GeometryBounds.js";
import { Ray } from "../element/Ray.js";
import { RayBoundary } from "../element/RayBoundary.js";
import { RayCrossing } from "../element/RayCrossing.js";
import { RayCrossingKind } from "../element/RayCrossingKind.js";
import { RayCrossingQuery } from "../element/RayCrossingQuery.js";
import { RayCrossingSink } from "../element/RayCrossingSink.js";
import { RayCrossingSupport } from "../element/RayCrossingSupport.js";
import { RayFirstHitStatus } from "../element/RayFirstHitStatus.js";
import { RayHit } from "../element/RayHit.js";
import { BezierPatchSubdivision } from "./BezierPatchSubdivision.js";
import { SubdividedBezierPatchNode } from "./SubdividedBezierPatchNode.js";
import { Surface } from "./Surface.js";

/**
Open bicubic Bezier patch rendered as the flat triangles of a subdivision of
its 4 x 4 control net: the PovRay bicubic_patch. Control point (i, j) has
parameters u = i/3 and v = j/3. The geometry is the triangulation chosen by
the subdivision, uSteps, vSteps and flatness, not the exact Bezier surface;
for that see ParametricBiCubicPatch. Being open, the patch has no inside:
ray classification and containment are Unsupported. Ray crossings are
Unclassified and their surface index is the hit's position in intersect
order; typed normals face the u x v side of their triangle. Bounds are the
control net's box, which holds the patch.
*/
export class SubdividedBezierPatch extends Surface {
    // Hits a single ray query can report.
    public static readonly MAXIMUM_HITS: number = 32;

    // Degenerate triangle, derivative and direction threshold.
    public static readonly DEGENERACY_EPSILON: number = 1.0e-10;

    // Depth below which PovRay ignores a hit.
    public static readonly POVRAY_MINIMUM_DEPTH: number = 0.001;

    private subdivision: BezierPatchSubdivision;

    private uSteps: number;

    private vSteps: number;

    private flatness: number;

    private controlPoints: Vector3Dd[][] = Array.from({ length: 4 }, () =>
        Array.from({ length: 4 }, () => new Vector3Dd()),
    );

    private boundingCenter: Vector3Dd;

    private boundingRadiusSquared: number;

    // Precomputed grids, (uSteps + 1) rows: grid points with vSteps + 1
    // columns; triangle planes and offsets with 2*(vSteps + 1) columns.
    private readonly gridPoints: ArrayList<Vector3Dd> = new ArrayList<Vector3Dd>();

    private readonly gridPlaneNormals: ArrayList<Vector3Dd> = new ArrayList<Vector3Dd>();

    private readonly gridPlaneOffsets: ArrayList<number> = new ArrayList<number>();

    private readonly gridSmoothNormals: ArrayList<Vector3Dd> = new ArrayList<Vector3Dd>();

    private readonly nodes: ArrayList<SubdividedBezierPatchNode> = new ArrayList<SubdividedBezierPatchNode>();

    private static sphereAround(
        count: number,
        points: Vector3Dd[],
        center: Vector3Dd[],
        radiusSquared: number[],
    ): void {
        let xc: number = 0;
        let yc: number = 0;
        let zc: number = 0;
        for (let i = 0; i < count; i++) {
            xc += points[i]!.x();
            yc += points[i]!.y();
            zc += points[i]!.z();
        }
        xc /= count;
        yc /= count;
        zc /= count;
        let r0: number = 0.0;
        for (let i = 0; i < count; i++) {
            const x0: number = points[i]!.x() - xc;
            const y0: number = points[i]!.y() - yc;
            const z0: number = points[i]!.z() - zc;
            const r1: number = x0 * x0 + y0 * y0 + z0 * z0;
            if (r1 > r0) {
                r0 = r1;
            }
        }
        center[0] = new Vector3Dd(xc, yc, zc);
        radiusSquared[0] = r0;
    }

    private static netSphere(net: Vector3Dd[][], center: Vector3Dd[], radiusSquared: number[]): void {
        let xc: number = 0;
        let yc: number = 0;
        let zc: number = 0;
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                xc += net[i]![j]!.x();
                yc += net[i]![j]!.y();
                zc += net[i]![j]!.z();
            }
        }
        xc /= 16.0;
        yc /= 16.0;
        zc /= 16.0;
        let r0: number = 0.0;
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                const x0: number = net[i]![j]!.x() - xc;
                const y0: number = net[i]![j]!.y() - yc;
                const z0: number = net[i]![j]!.z() - zc;
                const r1: number = x0 * x0 + y0 * y0 + z0 * z0;
                if (r1 > r0) {
                    r0 = r1;
                }
            }
        }
        center[0] = new Vector3Dd(xc, yc, zc);
        radiusSquared[0] = r0;
    }

    private static trianglePlane(
        v1: Vector3Dd,
        v2: Vector3Dd,
        v3: Vector3Dd,
        normal: Vector3Dd[],
        offset: number[],
    ): boolean {
        const edge1: Vector3Dd = v1.subtract(v2);
        const edge2: Vector3Dd = v3.subtract(v2);
        normal[0] = edge1.crossProduct(edge2);
        const length: number = normal[0].length();
        if (length < SubdividedBezierPatch.DEGENERACY_EPSILON) {
            normal[0] = new Vector3Dd(1.0, 0.0, 0.0);
            offset[0] = -1.0 * v1.x();
            return false;
        }
        normal[0] = new Vector3Dd(normal[0].x() / length, normal[0].y() / length, normal[0].z() / length);
        offset[0] = normal[0].dotProduct(v1);
        offset[0] = 0.0 - offset[0];
        return true;
    }

    private static planeDistance(p: Vector3Dd, n: Vector3Dd, d: number): number {
        let temp1: number = p.dotProduct(n);
        temp1 += d;
        const temp2: number = n.length();
        if (JavaMath.abs(temp2) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
            return 0;
        }
        return temp1 / temp2;
    }

    private static rayMeetsSphere(
        origin: Vector3Dd,
        direction: Vector3Dd,
        center: Vector3Dd,
        radiusSquared: number,
    ): boolean {
        const x: number = center.x() - origin.x();
        const y: number = center.y() - origin.y();
        const z: number = center.z() - origin.z();
        const dist1: number = x * x + y * y + z * z;
        if (dist1 < radiusSquared) {
            return true;
        }
        const projection: number = x * direction.x() + y * direction.y() + z * direction.z();
        if (projection <= 0.0) {
            return false;
        }
        const directionLengthSquared: number = direction.dotProduct(direction);
        if (directionLengthSquared <= SubdividedBezierPatch.DEGENERACY_EPSILON) {
            return false;
        }
        return dist1 - (projection * projection) / directionLengthSquared < radiusSquared;
    }

    private static splitLeftRight(net: Vector3Dd[][], left: Vector3Dd[][], right: Vector3Dd[][]): void {
        let temp1: Vector3Dd[] = Array.from({ length: 4 }, () => new Vector3Dd());
        let temp2: Vector3Dd[] = Array.from({ length: 4 }, () => new Vector3Dd());
        for (let i = 0; i < 4; i++) {
            temp1[0] = net[i]![0]!;
            temp1[1] = net[i]![0]!.midpoint(net[i]![1]!);
            const half: Vector3Dd = net[i]![1]!.midpoint(net[i]![2]!);
            temp1[2] = temp1[1].midpoint(half);
            temp2[2] = net[i]![2]!.midpoint(net[i]![3]!);
            temp2[1] = half.midpoint(temp2[2]);
            temp1[3] = temp1[2].midpoint(temp2[1]);
            temp2[0] = temp1[3];
            temp2[3] = net[i]![3]!;
            for (let j = 0; j < 4; j++) {
                left[i]![j] = temp1[j]!;
                right[i]![j] = temp2[j]!;
            }
        }
    }

    private static splitUpDown(net: Vector3Dd[][], top: Vector3Dd[][], bottom: Vector3Dd[][]): void {
        let temp1: Vector3Dd[] = Array.from({ length: 4 }, () => new Vector3Dd());
        let temp2: Vector3Dd[] = Array.from({ length: 4 }, () => new Vector3Dd());
        for (let i = 0; i < 4; i++) {
            temp1[0] = net[0]![i]!;
            temp1[1] = net[0]![i]!.midpoint(net[1]![i]!);
            const half: Vector3Dd = net[1]![i]!.midpoint(net[2]![i]!);
            temp1[2] = temp1[1].midpoint(half);
            temp2[2] = net[2]![i]!.midpoint(net[3]![i]!);
            temp2[1] = half.midpoint(temp2[2]);
            temp1[3] = temp1[2].midpoint(temp2[1]);
            temp2[0] = temp1[3];
            temp2[3] = net[3]![i]!;
            for (let j = 0; j < 4; j++) {
                bottom[j]![i] = temp1[j]!;
                top[j]![i] = temp2[j]!;
            }
        }
    }

    private static netFlatness(net: Vector3Dd[][]): number {
        let vertices: Vector3Dd[] = Array.from({ length: 3 }, () => new Vector3Dd());
        vertices[0] = net[0]![0]!;
        vertices[1] = net[0]![3]!;
        let temp1: number = vertices[0]!.subtract(vertices[1]!).length();
        if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
            vertices[1] = net[3]![3]!;
            temp1 = vertices[0]!.subtract(vertices[1]!).length();
            if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                return -1.0;
            }
            vertices[2] = net[3]![0]!;
            temp1 = vertices[0]!.subtract(vertices[1]!).length();
            if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                return -1.0;
            }
            temp1 = vertices[1]!.subtract(vertices[2]!).length();
            if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                return -1.0;
            }
        } else {
            vertices[2] = net[3]![0]!;
            temp1 = vertices[0]!.subtract(vertices[1]!).length();
            if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                vertices[2] = net[3]![3]!;
                temp1 = vertices[0]!.subtract(vertices[2]!).length();
                if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                    return -1.0;
                }
                temp1 = vertices[1]!.subtract(vertices[2]!).length();
                if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                    return -1.0;
                }
            } else {
                temp1 = vertices[1]!.subtract(vertices[2]!).length();
                if (JavaMath.abs(temp1) < SubdividedBezierPatch.DEGENERACY_EPSILON) {
                    return -1.0;
                }
            }
        }
        let n: Vector3Dd[] = [new Vector3Dd()];
        let d: number[] = [0];
        if (SubdividedBezierPatch.trianglePlane(vertices[0]!, vertices[1]!, vertices[2]!, n, d)) {
            let dist: number = 0.0;
            for (let i = 0; i < 4; i++) {
                for (let j = 0; j < 4; j++) {
                    temp1 = JavaMath.abs(SubdividedBezierPatch.planeDistance(net[i]![j]!, n[0]!, d[0]!));
                    if (temp1 > dist) {
                        dist = temp1;
                    }
                }
            }
            return dist;
        }
        return -1.0;
    }

    private isFlatEnough(net: Vector3Dd[][]): boolean {
        const dist: number = SubdividedBezierPatch.netFlatness(net);
        if (dist < 0.0) {
            return false;
        }
        return dist < this.flatness;
    }

    private intersectTriangle(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        orientNormal: boolean,
        v1: Vector3Dd,
        v2: Vector3Dd,
        v3: Vector3Dd,
        n: Vector3Dd,
        d: number,
        n1: Vector3Dd | null,
        n2: Vector3Dd | null,
        n3: Vector3Dd | null,
        depth: number[],
        point: Vector3Dd[],
        normal: Vector3Dd[],
    ): boolean {
        let s: number = direction.dotProduct(n);
        if (s === 0.0) {
            return false;
        }
        let t: number = origin.dotProduct(n);
        depth[0] = 0.0 - (d + t) / s;
        if (depth[0] < minimumDepth) {
            return false;
        }
        const ip: Vector3Dd = direction.multiply(depth[0]).add(origin);
        let x: number = JavaMath.abs(n.x());
        let y: number = JavaMath.abs(n.y());
        let z: number = JavaMath.abs(n.z());
        let x1: number, y1: number, x2: number, y2: number, x3: number, y3: number;
        if (x > y) {
            if (x > z) {
                x1 = v1.y() - ip.y();
                y1 = v1.z() - ip.z();
                x2 = v2.y() - ip.y();
                y2 = v2.z() - ip.z();
                x3 = v3.y() - ip.y();
                y3 = v3.z() - ip.z();
            } else {
                x1 = v1.x() - ip.x();
                y1 = v1.y() - ip.y();
                x2 = v2.x() - ip.x();
                y2 = v2.y() - ip.y();
                x3 = v3.x() - ip.x();
                y3 = v3.y() - ip.y();
            }
        } else if (y > z) {
            x1 = v1.x() - ip.x();
            y1 = v1.z() - ip.z();
            x2 = v2.x() - ip.x();
            y2 = v2.z() - ip.z();
            x3 = v3.x() - ip.x();
            y3 = v3.z() - ip.z();
        } else {
            x1 = v1.x() - ip.x();
            y1 = v1.y() - ip.y();
            x2 = v2.x() - ip.x();
            y2 = v2.y() - ip.y();
            x3 = v3.x() - ip.x();
            y3 = v3.y() - ip.y();
        }
        let crossings: number = 0;
        let signHolder: number = y1 < 0 ? -1 : 1;
        let nextSign: number = y2 < 0 ? -1 : 1;
        if (signHolder !== nextSign) {
            if (x1 > 0.0) {
                if (x2 > 0.0) {
                    crossings++;
                } else if (x1 - (y1 * (x2 - x1)) / (y2 - y1) > 0.0) {
                    crossings++;
                }
            } else if (x2 > 0.0) {
                if (x1 - (y1 * (x2 - x1)) / (y2 - y1) > 0.0) {
                    crossings++;
                }
            }
            signHolder = nextSign;
        }
        nextSign = y3 < 0 ? -1 : 1;
        if (signHolder !== nextSign) {
            if (x2 > 0.0) {
                if (x3 > 0.0) {
                    crossings++;
                } else if (x2 - (y2 * (x3 - x2)) / (y3 - y2) > 0.0) {
                    crossings++;
                }
            } else if (x3 > 0.0) {
                if (x2 - (y2 * (x3 - x2)) / (y3 - y2) > 0.0) {
                    crossings++;
                }
            }
            signHolder = nextSign;
        }
        nextSign = y1 < 0 ? -1 : 1;
        if (signHolder !== nextSign) {
            if (x3 > 0.0) {
                if (x1 > 0.0) {
                    crossings++;
                } else if (x3 - (y3 * (x1 - x3)) / (y1 - y3) > 0.0) {
                    crossings++;
                }
            } else if (x1 > 0.0) {
                if (x3 - (y3 * (x1 - x3)) / (y1 - y3) > 0.0) {
                    crossings++;
                }
            }
        }
        if (crossings !== 1) {
            return false;
        }
        point[0] = ip;
        if (this.subdivision !== BezierPatchSubdivision.SmoothGrid) {
            normal[0] = n;
            return true;
        }
        // Normal interpolated from the corner normals n1, n2, n3.
        let tempV1: Vector3Dd = v2.subtract(v3).normalizedFast();
        let tempV2: Vector3Dd = v1.subtract(v3);
        const proj: number = tempV2.dotProduct(tempV1);
        tempV1 = tempV1.multiply(proj);
        let perp: Vector3Dd = tempV1.subtract(tempV2).normalizedFast();
        let mu: number = tempV2.dotProduct(perp);
        mu = -1.0 / mu;
        perp = perp.multiply(mu);
        tempV1 = ip.subtract(v1);
        s = tempV1.dotProduct(perp);
        if (s < SubdividedBezierPatch.DEGENERACY_EPSILON) {
            normal[0] = n1!;
            if (orientNormal && normal[0].dotProduct(n) < 0) normal[0] = normal[0].multiply(-1.0);
            return true;
        }
        tempV1 = v3.subtract(v2);
        x = JavaMath.abs(tempV1.x());
        y = JavaMath.abs(tempV1.y());
        z = JavaMath.abs(tempV1.z());
        if (x > y) {
            if (x > z) {
                t = (tempV1.x() / s + v1.x() - v2.x()) / tempV1.x();
            } else {
                t = (tempV1.z() / s + v1.z() - v2.z()) / tempV1.z();
            }
        } else if (y > z) {
            t = (tempV1.y() / s + v1.y() - v2.y()) / tempV1.y();
        } else {
            t = (tempV1.z() / s + v1.z() - v2.z()) / tempV1.z();
        }
        tempV1 = n2!.subtract(n1!).multiply(s).add(n1!);
        tempV2 = n3!.subtract(n1!).multiply(s).add(n1!);
        normal[0] = tempV2.subtract(tempV1).multiply(t).add(tempV1).normalizedFast();
        if (orientNormal) {
            // The interpolation can divide by zero; then use the triangle normal.
            if (!Double.isFinite(normal[0].x()) || !Double.isFinite(normal[0].y()) || !Double.isFinite(normal[0].z()))
                normal[0] = n;
            if (normal[0].dotProduct(n) < 0) normal[0] = normal[0].multiply(-1.0);
        }
        return true;
    }

    private buildTree(net: Vector3Dd[][], depth: number): number {
        const index: number = this.nodes.size();
        this.nodes.add(new SubdividedBezierPatchNode());
        let center: Vector3Dd[] = [new Vector3Dd()];
        let radiusSquared: number[] = [0];
        SubdividedBezierPatch.netSphere(net, center, radiusSquared);
        this.nodes.get(index).center = center[0]!;
        this.nodes.get(index).radiusSquared = radiusSquared[0]!;
        let lowerLeft: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let lowerRight: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let upperLeft: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let upperRight: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let children: number[] = [-1, -1, -1, -1];
        let count: number = 0;
        if (this.isFlatEnough(net) || (depth >= this.uSteps && depth >= this.vSteps)) {
            this.nodes.get(index).leaf = true;
            this.nodes.get(index).corners[0] = net[0]![0]!;
            this.nodes.get(index).corners[1] = net[0]![3]!;
            this.nodes.get(index).corners[2] = net[3]![3]!;
            this.nodes.get(index).corners[3] = net[3]![0]!;
            return index;
        }
        if (depth >= this.uSteps) {
            SubdividedBezierPatch.splitUpDown(net, lowerLeft, upperLeft);
            children[0] = this.buildTree(lowerLeft, depth + 1);
            children[1] = this.buildTree(upperLeft, depth + 1);
            count = 2;
        } else if (depth >= this.vSteps) {
            SubdividedBezierPatch.splitLeftRight(net, lowerLeft, lowerRight);
            children[0] = this.buildTree(lowerLeft, depth + 1);
            children[1] = this.buildTree(lowerRight, depth + 1);
            count = 2;
        } else {
            SubdividedBezierPatch.splitLeftRight(net, lowerLeft, lowerRight);
            SubdividedBezierPatch.splitUpDown(lowerLeft, lowerLeft, upperLeft);
            SubdividedBezierPatch.splitUpDown(lowerRight, lowerRight, upperRight);
            children[0] = this.buildTree(lowerLeft, depth + 1);
            children[1] = this.buildTree(upperLeft, depth + 1);
            children[2] = this.buildTree(lowerRight, depth + 1);
            children[3] = this.buildTree(upperRight, depth + 1);
            count = 4;
        }
        this.nodes.get(index).leaf = false;
        this.nodes.get(index).childCount = count;
        for (let i = 0; i < 4; i++) this.nodes.get(index).children[i] = children[i]!;
        return index;
    }

    private precompute(): void {
        let netPoints: Vector3Dd[] = Array.from({ length: 16 }, () => new Vector3Dd());
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                netPoints[4 * i + j] = this.controlPoints[i]![j]!;
            }
        }
        let boundingCenterH: Vector3Dd[] = [this.boundingCenter];
        let boundingRadiusSquaredH: number[] = [this.boundingRadiusSquared];
        SubdividedBezierPatch.sphereAround(16, netPoints, boundingCenterH, boundingRadiusSquaredH);
        this.boundingCenter = boundingCenterH[0]!;
        this.boundingRadiusSquared = boundingRadiusSquaredH[0]!;
        if (
            this.subdivision === BezierPatchSubdivision.UniformGrid ||
            this.subdivision === BezierPatchSubdivision.Adaptive
        ) {
            return;
        }
        if (this.subdivision === BezierPatchSubdivision.AdaptiveTree) {
            this.buildTree(this.controlPoints, 0);
            return;
        }
        const deltaU: number = 1.0 / this.uSteps;
        const deltaV: number = 1.0 / this.vSteps;
        for (let i = 0; i < Math.trunc(this.uSteps + 1) * (this.vSteps + 1); i++) this.gridPoints.add(new Vector3Dd());
        for (let i = 0; i < Math.trunc(this.uSteps + 1) * 2 * (this.vSteps + 1); i++) {
            this.gridPlaneNormals.add(new Vector3Dd());
            this.gridPlaneOffsets.add(0.0);
        }
        // The PovRay fill order, kept because later writes overwrite earlier
        // ones with differently rounded parameters.
        for (let i = 0; i <= this.uSteps; i++) {
            const u: number = i / this.uSteps;
            for (let j = 0; j < this.vSteps; j++) {
                this.gridPoints.set(Math.trunc(this.gridIndex(i, j)), this.evaluate(u, j / this.vSteps));
            }
        }
        for (let i = 0; i < this.uSteps; i++) {
            const u: number = i / this.uSteps;
            for (let j = 0; j < this.vSteps; j++) {
                const v: number = j / this.vSteps;
                const v0: Vector3Dd = this.evaluate(u, v);
                const v1: Vector3Dd = this.evaluate(u + deltaU, v);
                const v2: Vector3Dd = this.evaluate(u, v + deltaV);
                const v3: Vector3Dd = this.evaluate(u + deltaU, v + deltaV);
                this.gridPoints.set(Math.trunc(this.gridIndex(i, j)), v0);
                this.gridPoints.set(Math.trunc(this.gridIndex(i + 1, j)), v1);
                this.gridPoints.set(Math.trunc(this.gridIndex(i, j + 1)), v2);
                this.gridPoints.set(Math.trunc(this.gridIndex(i + 1, j + 1)), v3);
                let n: Vector3Dd[] = [new Vector3Dd()];
                let d: number[] = [0];
                if (SubdividedBezierPatch.trianglePlane(v0, v2, v1, n, d)) {
                    this.gridPlaneNormals.set(Math.trunc(this.planeIndex(i, 2 * j)), n[0]!);
                    this.gridPlaneOffsets.set(Math.trunc(this.planeIndex(i, 2 * j)), d[0]!);
                } else {
                    this.gridPlaneNormals.set(Math.trunc(this.planeIndex(i, 2 * j)), new Vector3Dd(0.0, 0.0, 0.0));
                    this.gridPlaneOffsets.set(Math.trunc(this.planeIndex(i, 2 * j)), 0.0);
                }
                if (SubdividedBezierPatch.trianglePlane(v1, v2, v3, n, d)) {
                    this.gridPlaneNormals.set(Math.trunc(this.planeIndex(i, 2 * j + 1)), n[0]!);
                    this.gridPlaneOffsets.set(Math.trunc(this.planeIndex(i, 2 * j + 1)), d[0]!);
                } else {
                    this.gridPlaneNormals.set(Math.trunc(this.planeIndex(i, 2 * j + 1)), new Vector3Dd(0.0, 0.0, 0.0));
                    this.gridPlaneOffsets.set(Math.trunc(this.planeIndex(i, 2 * j + 1)), 0.0);
                }
            }
        }
        if (this.subdivision === BezierPatchSubdivision.SmoothGrid) {
            for (let i = 0; i <= this.uSteps; i++) {
                const u: number = i / this.uSteps;
                for (let j = 0; j <= this.vSteps; j++) {
                    this.gridSmoothNormals.add(this.evaluateNormal(u, j / this.vSteps));
                }
            }
        }
    }

    private gridIndex(i: number, j: number): number {
        return Math.trunc(i) * (this.vSteps + 1) + j;
    }

    private planeIndex(i: number, j: number): number {
        return Math.trunc(i) * 2 * (this.vSteps + 1) + j;
    }

    private intersectWith(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        orient: boolean,
        alreadyUsed: number,
        maximumHits: number,
        depths: number[],
        points: Vector3Dd[],
        normals: Vector3Dd[],
    ): number {
        let hits: HitList = new HitList();
        hits.used = alreadyUsed;
        hits.maximum = maximumHits;
        hits.count = 0;
        hits.depths = depths;
        hits.points = points;
        hits.normals = normals;
        hits.orientNormals = orient;
        switch (this.subdivision) {
            case BezierPatchSubdivision.UniformGrid:
                return this.intersectUniform(origin, direction, minimumDepth, hits);
            case BezierPatchSubdivision.PrecomputedGrid:
                return this.intersectGrid(origin, direction, minimumDepth, hits);
            case BezierPatchSubdivision.Adaptive:
                this.subdivide(origin, direction, minimumDepth, this.controlPoints, 0, hits);
                return hits.count;
            case BezierPatchSubdivision.AdaptiveTree:
                if (!this.nodes.isEmpty()) this.walkTree(origin, direction, minimumDepth, 0, hits);
                return hits.count;
            case BezierPatchSubdivision.SmoothGrid:
                return this.intersectSmooth(origin, direction, minimumDepth, hits);
        }
        return 0;
    }

    private intersectUniform(origin: Vector3Dd, direction: Vector3Dd, minimumDepth: number, hits: HitList): number {
        if (!SubdividedBezierPatch.rayMeetsSphere(origin, direction, this.boundingCenter, this.boundingRadiusSquared)) {
            return 0;
        }
        const deltaU: number = 1.0 / this.uSteps;
        const deltaV: number = 1.0 / this.vSteps;
        for (let i = 0; i < this.uSteps; i++) {
            const u: number = i / this.uSteps;
            for (let j = 0; j < this.vSteps; j++) {
                const v: number = j / this.vSteps;
                const v0: Vector3Dd = this.evaluate(u, v);
                const v1: Vector3Dd = this.evaluate(u + deltaU, v);
                const v2: Vector3Dd = this.evaluate(u, v + deltaV);
                const v3: Vector3Dd = this.evaluate(u + deltaU, v + deltaV);
                let n: Vector3Dd[] = [new Vector3Dd()],
                    ip: Vector3Dd[] = [new Vector3Dd()],
                    normal: Vector3Dd[] = [new Vector3Dd()];
                let d: number[] = [0],
                    depth: number[] = [0];
                if (
                    SubdividedBezierPatch.trianglePlane(v0, v2, v1, n, d) &&
                    this.intersectTriangle(
                        origin,
                        direction,
                        minimumDepth,
                        hits.orientNormals,
                        v0,
                        v2,
                        v1,
                        n[0]!,
                        d[0]!,
                        null,
                        null,
                        null,
                        depth,
                        ip,
                        normal,
                    )
                ) {
                    hits.add(depth[0]!, ip[0]!, normal[0]!);
                    if (hits.isFull()) return hits.count;
                }
                if (
                    SubdividedBezierPatch.trianglePlane(v1, v2, v3, n, d) &&
                    this.intersectTriangle(
                        origin,
                        direction,
                        minimumDepth,
                        hits.orientNormals,
                        v1,
                        v2,
                        v3,
                        n[0]!,
                        d[0]!,
                        null,
                        null,
                        null,
                        depth,
                        ip,
                        normal,
                    )
                ) {
                    hits.add(depth[0]!, ip[0]!, normal[0]!);
                    if (hits.isFull()) return hits.count;
                }
            }
        }
        return hits.count;
    }

    private intersectGrid(origin: Vector3Dd, direction: Vector3Dd, minimumDepth: number, hits: HitList): number {
        if (!SubdividedBezierPatch.rayMeetsSphere(origin, direction, this.boundingCenter, this.boundingRadiusSquared)) {
            return 0;
        }
        for (let i = 0; i < this.uSteps; i++) {
            for (let j = 0; j < this.vSteps; j++) {
                let v: Vector3Dd[] = Array.from({ length: 4 }, () => new Vector3Dd());
                v[0] = this.gridPoints.get(Math.trunc(this.gridIndex(i, j)));
                v[1] = this.gridPoints.get(Math.trunc(this.gridIndex(i + 1, j)));
                v[2] = this.gridPoints.get(Math.trunc(this.gridIndex(i, j + 1)));
                v[3] = this.gridPoints.get(Math.trunc(this.gridIndex(i + 1, j + 1)));
                let center: Vector3Dd[] = [new Vector3Dd()];
                let radiusSquared: number[] = [0];
                SubdividedBezierPatch.sphereAround(4, v, center, radiusSquared);
                if (!SubdividedBezierPatch.rayMeetsSphere(origin, direction, center[0]!, radiusSquared[0]!)) {
                    continue;
                }
                let ip: Vector3Dd[] = [new Vector3Dd()],
                    normal: Vector3Dd[] = [new Vector3Dd()];
                let depth: number[] = [0];
                let n: Vector3Dd = this.gridPlaneNormals.get(Math.trunc(this.planeIndex(i, 2 * j)));
                if (
                    !(n.x() === 0.0 && n.y() === 0.0 && n.z() === 0.0) &&
                    this.intersectTriangle(
                        origin,
                        direction,
                        minimumDepth,
                        hits.orientNormals,
                        v[0],
                        v[2],
                        v[1],
                        n,
                        this.gridPlaneOffsets.get(Math.trunc(this.planeIndex(i, 2 * j))),
                        null,
                        null,
                        null,
                        depth,
                        ip,
                        normal,
                    )
                ) {
                    hits.add(depth[0]!, ip[0]!, normal[0]!);
                    if (hits.isFull()) return hits.count;
                }
                n = this.gridPlaneNormals.get(Math.trunc(this.planeIndex(i, 2 * j + 1)));
                if (n.x() === 0.0 && n.y() === 0.0 && n.z() === 0.0) {
                    continue;
                }
                if (
                    this.intersectTriangle(
                        origin,
                        direction,
                        minimumDepth,
                        hits.orientNormals,
                        v[1],
                        v[2],
                        v[3],
                        n,
                        this.gridPlaneOffsets.get(Math.trunc(this.planeIndex(i, 2 * j + 1))),
                        null,
                        null,
                        null,
                        depth,
                        ip,
                        normal,
                    )
                ) {
                    hits.add(depth[0]!, ip[0]!, normal[0]!);
                    if (hits.isFull()) return hits.count;
                }
            }
        }
        return hits.count;
    }

    private intersectSmooth(origin: Vector3Dd, direction: Vector3Dd, minimumDepth: number, hits: HitList): number {
        if (!SubdividedBezierPatch.rayMeetsSphere(origin, direction, this.boundingCenter, this.boundingRadiusSquared)) {
            return 0;
        }
        for (let i = 0; i < this.uSteps; i++) {
            for (let j = 0; j < this.vSteps; j++) {
                const v0: Vector3Dd = this.gridPoints.get(Math.trunc(this.gridIndex(i, j)));
                const v1: Vector3Dd = this.gridPoints.get(Math.trunc(this.gridIndex(i + 1, j)));
                const v2: Vector3Dd = this.gridPoints.get(Math.trunc(this.gridIndex(i, j + 1)));
                const v3: Vector3Dd = this.gridPoints.get(Math.trunc(this.gridIndex(i + 1, j + 1)));
                let n0: Vector3Dd = this.gridSmoothNormals.get(Math.trunc(this.gridIndex(i, j)));
                let n1: Vector3Dd = this.gridSmoothNormals.get(Math.trunc(this.gridIndex(i + 1, j)));
                let n2: Vector3Dd = this.gridSmoothNormals.get(Math.trunc(this.gridIndex(i, j + 1)));
                let n3: Vector3Dd = this.gridSmoothNormals.get(Math.trunc(this.gridIndex(i + 1, j + 1)));
                let ip: Vector3Dd[] = [new Vector3Dd()],
                    normal: Vector3Dd[] = [new Vector3Dd()];
                let depth: number[] = [0];
                let n: Vector3Dd = this.gridPlaneNormals.get(Math.trunc(this.planeIndex(i, 2 * j)));
                if (!(n.x() === 0.0 && n.y() === 0.0 && n.z() === 0.0)) {
                    // Corner normals turned to the side of the triangle normal.
                    if (n0.dotProduct(n) < 0) n0 = n0.multiply(-1.0);
                    if (n1.dotProduct(n) < 0) n1 = n1.multiply(-1.0);
                    if (n2.dotProduct(n) < 0) n2 = n2.multiply(-1.0);
                    if (
                        this.intersectTriangle(
                            origin,
                            direction,
                            minimumDepth,
                            hits.orientNormals,
                            v0,
                            v2,
                            v1,
                            n,
                            this.gridPlaneOffsets.get(Math.trunc(this.planeIndex(i, 2 * j))),
                            n0,
                            n2,
                            n1,
                            depth,
                            ip,
                            normal,
                        )
                    ) {
                        hits.add(depth[0]!, ip[0]!, normal[0]!);
                        if (hits.isFull()) return hits.count;
                    }
                }
                n = this.gridPlaneNormals.get(Math.trunc(this.planeIndex(i, 2 * j + 1)));
                if (n.x() === 0.0 && n.y() === 0.0 && n.z() === 0.0) {
                    continue;
                }
                // PovRay replaces, rather than turns, these corner normals.
                if (n1.dotProduct(n) > 0) n1 = n0.multiply(-1.0);
                if (n2.dotProduct(n) > 0) n2 = n1.multiply(-1.0);
                if (n3.dotProduct(n) > 0) n3 = n2.multiply(-1.0);
                if (
                    this.intersectTriangle(
                        origin,
                        direction,
                        minimumDepth,
                        hits.orientNormals,
                        v1,
                        v2,
                        v3,
                        n,
                        this.gridPlaneOffsets.get(Math.trunc(this.planeIndex(i, 2 * j + 1))),
                        n1,
                        n2,
                        n3,
                        depth,
                        ip,
                        normal,
                    )
                ) {
                    hits.add(depth[0]!, ip[0]!, normal[0]!);
                    if (hits.isFull()) return hits.count;
                }
            }
        }
        return hits.count;
    }

    private subdivide(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        net: Vector3Dd[][],
        depth: number,
        hits: HitList,
    ): void {
        if (hits.used >= hits.maximum) {
            return;
        }
        let center: Vector3Dd[] = [new Vector3Dd()];
        let radiusSquared: number[] = [0];
        SubdividedBezierPatch.netSphere(net, center, radiusSquared);
        if (!SubdividedBezierPatch.rayMeetsSphere(origin, direction, center[0]!, radiusSquared[0]!)) {
            return;
        }
        // PovRay tests a flat piece and still subdivides it.
        if (this.isFlatEnough(net) && !hits.isFull()) {
            this.intersectPiece(
                origin,
                direction,
                minimumDepth,
                net[0]![0]!,
                net[0]![3]!,
                net[3]![3]!,
                net[3]![0]!,
                hits,
            );
        }
        let lowerLeft: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let lowerRight: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let upperLeft: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        let upperRight: Vector3Dd[][] = Array.from({ length: 4 }, () =>
            Array.from({ length: 4 }, () => new Vector3Dd()),
        );
        if (depth >= this.uSteps) {
            if (depth >= this.vSteps) {
                if (!hits.isFull()) {
                    this.intersectPiece(
                        origin,
                        direction,
                        minimumDepth,
                        net[0]![0]!,
                        net[0]![3]!,
                        net[3]![3]!,
                        net[3]![0]!,
                        hits,
                    );
                }
            } else {
                SubdividedBezierPatch.splitUpDown(net, lowerLeft, upperLeft);
                this.subdivide(origin, direction, minimumDepth, lowerLeft, depth + 1, hits);
                this.subdivide(origin, direction, minimumDepth, upperLeft, depth + 1, hits);
            }
        } else if (depth >= this.vSteps) {
            SubdividedBezierPatch.splitLeftRight(net, lowerLeft, lowerRight);
            this.subdivide(origin, direction, minimumDepth, lowerLeft, depth + 1, hits);
            this.subdivide(origin, direction, minimumDepth, lowerRight, depth + 1, hits);
        } else {
            SubdividedBezierPatch.splitLeftRight(net, lowerLeft, lowerRight);
            SubdividedBezierPatch.splitUpDown(lowerLeft, lowerLeft, upperLeft);
            SubdividedBezierPatch.splitUpDown(lowerRight, lowerRight, upperRight);
            this.subdivide(origin, direction, minimumDepth, lowerLeft, depth + 1, hits);
            this.subdivide(origin, direction, minimumDepth, upperLeft, depth + 1, hits);
            this.subdivide(origin, direction, minimumDepth, lowerRight, depth + 1, hits);
            this.subdivide(origin, direction, minimumDepth, upperRight, depth + 1, hits);
        }
    }

    private intersectPiece(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        c0: Vector3Dd,
        c1: Vector3Dd,
        c2: Vector3Dd,
        c3: Vector3Dd,
        hits: HitList,
    ): void {
        let n: Vector3Dd[] = [new Vector3Dd()],
            ip: Vector3Dd[] = [new Vector3Dd()],
            normal: Vector3Dd[] = [new Vector3Dd()];
        let d: number[] = [0],
            depth: number[] = [0];
        if (
            SubdividedBezierPatch.trianglePlane(c0, c1, c2, n, d) &&
            this.intersectTriangle(
                origin,
                direction,
                minimumDepth,
                hits.orientNormals,
                c0,
                c1,
                c2,
                n[0]!,
                d[0]!,
                null,
                null,
                null,
                depth,
                ip,
                normal,
            )
        ) {
            hits.add(depth[0]!, ip[0]!, normal[0]!);
        }
        if (hits.isFull()) {
            return;
        }
        if (
            SubdividedBezierPatch.trianglePlane(c0, c2, c3, n, d) &&
            this.intersectTriangle(
                origin,
                direction,
                minimumDepth,
                hits.orientNormals,
                c0,
                c2,
                c3,
                n[0]!,
                d[0]!,
                null,
                null,
                null,
                depth,
                ip,
                normal,
            )
        ) {
            hits.add(depth[0]!, ip[0]!, normal[0]!);
        }
    }

    private walkTree(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        index: number,
        hits: HitList,
    ): void {
        if (hits.used >= hits.maximum) {
            return;
        }
        const node: SubdividedBezierPatchNode = this.nodes.get(index);
        if (!SubdividedBezierPatch.rayMeetsSphere(origin, direction, node.center, node.radiusSquared)) {
            return;
        }
        if (!node.leaf) {
            for (let i = 0; i < node.childCount; i++) {
                this.walkTree(origin, direction, minimumDepth, node.children[i]!, hits);
            }
            return;
        }
        this.intersectPiece(
            origin,
            direction,
            minimumDepth,
            node.corners[0]!,
            node.corners[1]!,
            node.corners[2]!,
            node.corners[3]!,
            hits,
        );
    }

    private collectHits(
        ray: Ray,
        query: RayCrossingQuery,
        count: number[],
        depths: number[],
        normals: Vector3Dd[],
    ): RayFirstHitStatus {
        count[0] = 0;
        const o: Vector3Dd = ray.getOrigin();
        const d: Vector3Dd = ray.getDirection();
        if (
            !Double.isFinite(o.x()) ||
            !Double.isFinite(o.y()) ||
            !Double.isFinite(o.z()) ||
            !Double.isFinite(d.x()) ||
            !Double.isFinite(d.y()) ||
            !Double.isFinite(d.z()) ||
            (d.x() === 0 && d.y() === 0 && d.z() === 0) ||
            Double.isNaN(query.minT) ||
            Double.isNaN(query.maxT) ||
            query.minT > query.maxT
        )
            return RayFirstHitStatus.InvalidInput;
        // Hits behind the origin are not all reported, so the query must start
        // at or after it.
        if (
            !(query.minT >= 0) ||
            query.minimumChordSquared !== 0 ||
            query.minimumExteriorApproach !== Double.NEGATIVE_INFINITY
        )
            return RayFirstHitStatus.Unsupported;
        let points: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
        for (let i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++) points[i] = new Vector3Dd();
        const found: number = this.intersectOriented(
            o,
            d,
            query.minT,
            0,
            SubdividedBezierPatch.MAXIMUM_HITS,
            depths,
            points,
            normals,
        );
        // A full budget may have hidden further hits.
        if (found >= SubdividedBezierPatch.MAXIMUM_HITS) return RayFirstHitStatus.Indeterminate;
        for (let i = 0; i < found; i++) {
            if (!Double.isFinite(depths[i]!)) return RayFirstHitStatus.Indeterminate;
        }
        count[0] = found;
        return RayFirstHitStatus.Hit;
    }

    // Throws java::IllegalArgumentException for steps below 1 (or, for the
    // adaptive subdivisions, below 0) or a non-finite control point.
    public constructor(
        subdivision: BezierPatchSubdivision,
        uSteps: number,
        vSteps: number,
        flatness: number,
        controlPoints: Vector3Dd[][],
    );
    public constructor(other: SubdividedBezierPatch);
    public constructor(
        subdivisionOrOther: BezierPatchSubdivision | SubdividedBezierPatch,
        uStepsValue?: number,
        vStepsValue?: number,
        flatnessValue?: number,
        netValue?: Vector3Dd[][],
    ) {
        super();
        if (subdivisionOrOther instanceof SubdividedBezierPatch) {
            this.subdivision = subdivisionOrOther.subdivision;
            this.uSteps = 0;
            this.vSteps = 0;
            this.flatness = 0;
            this.boundingCenter = new Vector3Dd();
            this.boundingRadiusSquared = 0;
            this.assign(subdivisionOrOther);
            return;
        }
        const subdivisionValue = subdivisionOrOther;
        const uStepsValue2 = uStepsValue as number;
        const vStepsValue2 = vStepsValue as number;
        const flatnessValue2 = flatnessValue as number;
        const net = netValue as Vector3Dd[][];
        this.subdivision = subdivisionValue;
        this.uSteps = uStepsValue2;
        this.vSteps = vStepsValue2;
        this.flatness = flatnessValue2;
        this.boundingCenter = new Vector3Dd();
        this.boundingRadiusSquared = 0;
        const adaptive: boolean =
            this.subdivision === BezierPatchSubdivision.Adaptive ||
            this.subdivision === BezierPatchSubdivision.AdaptiveTree;
        if (this.uSteps < (adaptive ? 0 : 1) || this.vSteps < (adaptive ? 0 : 1)) {
            throw new IllegalArgumentException("SubdividedBezierPatch: invalid subdivision steps");
        }
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                const p: Vector3Dd = JavaArray.get(JavaArray.get(net, i), j);
                if (!Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())) {
                    throw new IllegalArgumentException("SubdividedBezierPatch: control points must be finite");
                }
                this.controlPoints[i]![j] = p;
            }
        }
        this.precompute();
    }

    /** cpp copy assignment. */
    public assign(other: SubdividedBezierPatch): SubdividedBezierPatch {
        if (this !== other) {
            this.subdivision = other.subdivision;
            this.uSteps = other.uSteps;
            this.vSteps = other.vSteps;
            this.flatness = other.flatness;
            for (let i = 0; i < 4; i++) {
                for (let j = 0; j < 4; j++) this.controlPoints[i]![j] = other.controlPoints[i]![j]!;
            }
            this.boundingCenter = other.boundingCenter;
            this.boundingRadiusSquared = other.boundingRadiusSquared;
            this.gridPoints.clear();
            this.gridPoints.addAll(other.gridPoints);
            this.gridPlaneNormals.clear();
            this.gridPlaneNormals.addAll(other.gridPlaneNormals);
            this.gridPlaneOffsets.clear();
            this.gridPlaneOffsets.addAll(other.gridPlaneOffsets);
            this.gridSmoothNormals.clear();
            this.gridSmoothNormals.addAll(other.gridSmoothNormals);
            this.nodes.clear();
            for (let i = 0; i < other.nodes.size(); i++)
                this.nodes.add(new SubdividedBezierPatchNode(other.nodes.get(i)));
        }
        return this;
    }

    public getSubdivision(): BezierPatchSubdivision {
        return this.subdivision;
    }

    public getUSteps(): number {
        return this.uSteps;
    }

    public getVSteps(): number {
        return this.vSteps;
    }

    public getFlatness(): number {
        return this.flatness;
    }

    public getControlPoint(i: number, j: number): Vector3Dd {
        return JavaArray.get(JavaArray.get(this.controlPoints, i), j);
    }

    // Point of the Bezier surface at (u, v) in [0, 1]^2.
    public evaluate(u: number, v: number): Vector3Dd {
        const u2: number = u * u;
        const u3: number = u * u2;
        const v2: number = v * v;
        const v3: number = v * v2;
        const uu1: number = 1.0 - u;
        const uu2: number = uu1 * uu1;
        const uu3: number = uu1 * uu2;
        const vv1: number = 1.0 - v;
        const vv2: number = vv1 * vv1;
        const vv3: number = vv1 * vv2;
        let t: number[][] = Array.from({ length: 4 }, () => new Array<number>(4).fill(0));
        t[0]![0] = uu3 * vv3;
        t[0]![1] = 3.0 * uu3 * v * vv2;
        t[0]![2] = 3.0 * uu3 * v2 * vv1;
        t[0]![3] = uu3 * v3;
        t[1]![0] = 3.0 * u * uu2 * vv3;
        t[1]![1] = 9.0 * u * uu2 * v * vv2;
        t[1]![2] = 9.0 * u * uu2 * v2 * vv1;
        t[1]![3] = 3.0 * u * uu2 * v3;
        t[2]![0] = 3.0 * u2 * uu1 * vv3;
        t[2]![1] = 9.0 * u2 * uu1 * v * vv2;
        t[2]![2] = 9.0 * u2 * uu1 * v2 * vv1;
        t[2]![3] = 3.0 * u2 * uu1 * v3;
        t[3]![0] = u3 * vv3;
        t[3]![1] = 3.0 * u3 * v * vv2;
        t[3]![2] = 3.0 * u3 * v2 * vv1;
        t[3]![3] = u3 * v3;
        let rx: number = 0.0;
        let ry: number = 0.0;
        let rz: number = 0.0;
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                rx += t[i]![j]! * this.controlPoints[i]![j]!.x();
                ry += t[i]![j]! * this.controlPoints[i]![j]!.y();
                rz += t[i]![j]! * this.controlPoints[i]![j]!.z();
            }
        }
        return new Vector3Dd(rx, ry, rz);
    }

    // Cross product of the unit u and v derivatives, or (1, 0, 0) where
    // either derivative vanishes.
    public evaluateNormal(u: number, v: number): Vector3Dd {
        const u2: number = u * u;
        const u3: number = u * u2;
        const v2: number = v * v;
        const v3: number = v * v2;
        let t: number[][] = Array.from({ length: 4 }, () => new Array<number>(4).fill(0));
        t[0]![0] = 3.0 * (v3 - 3.0 * v2 + 3.0 * v - 1.0) * (u2 - 2.0 * u + 1.0);
        t[0]![1] = 9.0 * v * (v2 - 2.0 * v + 1.0) * (u2 - 2.0 * u + 1.0);
        t[0]![2] = 9.0 * v2 * (v - 1.0) * (u2 - 2.0 * u + 1.0);
        t[0]![3] = 3.0 * v3 * (u2 - 2.0 * u + 1.0);
        t[1]![0] = 3.0 * (v3 - 3.0 * v2 + 3.0 * v - 1) * (3.0 * u2 - 4.0 * u + 1);
        t[1]![1] = 9.0 * v * (v2 - 2.0 * v + 1.0) * (3.0 * u2 - 4.0 * u + 1.0);
        t[1]![2] = 9.0 * v2 * (v - 1.0) * (3.0 * u2 - 4.0 * u + 1.0);
        t[1]![3] = 3.0 * v3 * (3.0 * u2 - 4.0 * u + 1.0);
        t[2]![0] = 3.0 * u * (v3 - 3.0 * v2 + 3.0 * v - 1.0) * (3.0 * u - 2.0);
        t[2]![1] = 9.0 * u * v * (v2 - 2.0 * v + 1.0) * (3.0 * u - 2.0);
        t[2]![2] = 9.0 * u * v2 * (v - 1.0) * (3.0 * u - 2.0);
        t[2]![3] = 3.0 * u * v3 * (3.0 * u - 2.0);
        t[3]![0] = 3.0 * u2 * (v3 - 3.0 * v2 + 3.0 * v - 1.0);
        t[3]![1] = 9.0 * u2 * v * (v2 - 2.0 * v + 1.0);
        t[3]![2] = 9.0 * u2 * v2 * (v - 1.0);
        t[3]![3] = 3.0 * u2 * v3;
        let ux: number = 0.0;
        let uy: number = 0.0;
        let uz: number = 0.0;
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                ux += t[i]![j]! * this.controlPoints[i]![j]!.x();
                uy += t[i]![j]! * this.controlPoints[i]![j]!.y();
                uz += t[i]![j]! * this.controlPoints[i]![j]!.z();
            }
        }
        let temp: number = ux * ux + uy * uy + uz * uz;
        if (temp < SubdividedBezierPatch.DEGENERACY_EPSILON) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        temp = JavaMath.sqrt(temp);
        const uVec: Vector3Dd = new Vector3Dd(ux / temp, uy / temp, uz / temp);

        t[0]![0] = 3.0 * (v2 - 2.0 * v + 1.0) * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
        t[0]![1] = 3.0 * (3.0 * v2 - 4.0 * v + 1.0) * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
        t[0]![2] = 3.0 * v * (3.0 * v - 2.0) * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
        t[0]![3] = 3.0 * v2 * (u3 - 3.0 * u2 + 3.0 * u - 1.0);
        t[1]![0] = 9.0 * u * (v2 - 2.0 * v + 1.0) * (u2 - 2.0 * u + 1.0);
        t[1]![1] = 9.0 * u * (3.0 * v2 - 4.0 * v + 1.0) * (u2 - 2.0 * u + 1.0);
        t[1]![2] = 9.0 * u * v * (3.0 * v - 2.0) * (u2 - 2.0 * u + 1.0);
        t[1]![3] = 9.0 * u * v2 * (u2 - 2.0 * u + 1.0);
        t[2]![0] = 9.0 * u2 * (v2 - 2.0 * v + 1.0) * (u - 1.0);
        t[2]![1] = 9.0 * u2 * (3.0 * v2 - 4.0 * v + 1.0) * (u - 1.0);
        t[2]![2] = 9.0 * u2 * v * (3.0 * v - 2.0) * (u - 1.0);
        t[2]![3] = 9.0 * u2 * v2 * (u - 1.0);
        t[3]![0] = 3.0 * u3 * (v2 - 2.0 * v + 1.0);
        t[3]![1] = 3.0 * u3 * (3.0 * v2 - 4.0 * v + 1.0);
        t[3]![2] = 3.0 * u3 * v * (3.0 * v - 2.0);
        t[3]![3] = 3.0 * u3 * v2;
        let vx: number = 0.0;
        let vy: number = 0.0;
        let vz: number = 0.0;
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                vx += t[i]![j]! * this.controlPoints[i]![j]!.x();
                vy += t[i]![j]! * this.controlPoints[i]![j]!.y();
                vz += t[i]![j]! * this.controlPoints[i]![j]!.z();
            }
        }
        temp = vx * vx + vy * vy + vz * vz;
        if (temp < SubdividedBezierPatch.DEGENERACY_EPSILON) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        temp = JavaMath.sqrt(temp);
        const vVec: Vector3Dd = new Vector3Dd(vx / temp, vy / temp, vz / temp);
        return uVec.crossProduct(vVec);
    }

    // PovRay hits along origin + depth*direction with depth >= minimumDepth,
    // in discovery order, each with its point and unit triangle (or, for
    // SmoothGrid, interpolated) normal. alreadyUsed hits of a shared budget of
    // maximumHits are already spent; returns how many were added, at most
    // maximumHits - alreadyUsed, written from index 0 of the arrays.
    public intersect(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        alreadyUsed: number,
        maximumHits: number,
        depths: number[],
        points: Vector3Dd[],
        normals: Vector3Dd[],
    ): number {
        return this.intersectWith(
            origin,
            direction,
            minimumDepth,
            false,
            alreadyUsed,
            maximumHits,
            depths,
            points,
            normals,
        );
    }

    // As intersect, with every normal turned to the u x v side of its
    // triangle; the PovRay SmoothGrid interpolation can leave it reversed or,
    // dividing by zero, not finite, and then the triangle normal is used.
    public intersectOriented(
        origin: Vector3Dd,
        direction: Vector3Dd,
        minimumDepth: number,
        alreadyUsed: number,
        maximumHits: number,
        depths: number[],
        points: Vector3Dd[],
        normals: Vector3Dd[],
    ): number {
        return this.intersectWith(
            origin,
            direction,
            minimumDepth,
            true,
            alreadyUsed,
            maximumHits,
            depths,
            points,
            normals,
        );
    }

    public override copyGeometry(): Geometry {
        return new SubdividedBezierPatch(this);
    }

    public override getBounds(result: GeometryBounds): void {
        let low: Vector3Dd = this.controlPoints[0]![0]!;
        let high: Vector3Dd = this.controlPoints[0]![0]!;
        for (let i = 0; i < 4; i++) {
            for (let j = 0; j < 4; j++) {
                const p: Vector3Dd = this.controlPoints[i]![j]!;
                low = new Vector3Dd(
                    JavaMath.min(low.x(), p.x()),
                    JavaMath.min(low.y(), p.y()),
                    JavaMath.min(low.z(), p.z()),
                );
                high = new Vector3Dd(
                    JavaMath.max(high.x(), p.x()),
                    JavaMath.max(high.y(), p.y()),
                    JavaMath.max(high.z(), p.z()),
                );
            }
        }
        if (!result.setFinite(low, high)) result.setIndeterminate();
    }

    public override queryFirstHit(
        ray: Ray,
        query: RayCrossingQuery,
        hit: RayHit | null,
        boundary: RayBoundary | null = null,
    ): RayFirstHitStatus {
        if (hit !== null) hit.clear();
        if (boundary !== null) boundary.assign(new RayBoundary());
        let depths: number[] = new Array<number>(SubdividedBezierPatch.MAXIMUM_HITS).fill(0);
        let normals: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
        for (let i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
        let countH: number[] = [0];
        const status: RayFirstHitStatus = this.collectHits(ray, query, countH, depths, normals);
        const count: number = countH[0]!;
        if (status !== RayFirstHitStatus.Hit) return status;
        let best: number = -1;
        for (let i = 0; i < count; i++) {
            if (depths[i]! <= query.maxT && (best < 0 || depths[i]! < depths[best]!)) best = i;
        }
        if (best < 0) return RayFirstHitStatus.Miss;
        if (boundary !== null) boundary.assign(new RayBoundary(depths[best]!, this, false, best));
        if (hit !== null) {
            hit.setHitDistance(depths[best]!);
            if (hit.shouldStoreRay()) hit.setRay(ray.withT(depths[best]!));
            if (hit.needsAnySurfaceData()) {
                hit.beginDetailEvaluation();
                if (hit.needsPoint()) {
                    hit.point = ray.getOrigin().add(ray.getDirection().multiply(depths[best]!));
                    hit.markDetailAvailable(RayHit.DETAIL_POINT);
                }
                if (hit.needsNormal()) {
                    hit.normal = normals[best]!;
                    hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
                }
            }
        }
        return RayFirstHitStatus.Hit;
    }

    public override querySurfaceDetail(ray: Ray, boundary: RayBoundary, hit: RayHit | null): RayFirstHitStatus {
        if (hit !== null) hit.clear();
        if (
            hit === null ||
            boundary.geometry !== this ||
            !Double.isFinite(boundary.t) ||
            boundary.surfaceIndex < 0 ||
            boundary.surfaceIndex >= SubdividedBezierPatch.MAXIMUM_HITS
        )
            return RayFirstHitStatus.InvalidInput;
        if (boundary.getInstanceCount() !== 0) return RayFirstHitStatus.Unsupported;
        let depths: number[] = new Array<number>(SubdividedBezierPatch.MAXIMUM_HITS).fill(0);
        let normals: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
        for (let i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
        let countH: number[] = [0];
        const status: RayFirstHitStatus = this.collectHits(
            ray,
            new RayCrossingQuery(0, Double.POSITIVE_INFINITY),
            countH,
            depths,
            normals,
        );
        const count: number = countH[0]!;
        if (status !== RayFirstHitStatus.Hit) return status;
        const index: number = Math.trunc(boundary.surfaceIndex);
        if (index >= count || depths[index] !== boundary.t) return RayFirstHitStatus.Indeterminate;
        hit.setHitDistance(boundary.t);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
        hit.beginDetailEvaluation();
        if (hit.needsPoint()) {
            hit.point = ray.getOrigin().add(ray.getDirection().multiply(boundary.t));
            hit.markDetailAvailable(RayHit.DETAIL_POINT);
        }
        if (hit.needsNormal()) {
            hit.normal = boundary.normalReversed ? normals[index]!.multiply(-1.0) : normals[index]!;
            hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
        return RayFirstHitStatus.Hit;
    }

    public override emitRayCrossings(ray: Ray, query: RayCrossingQuery, sink: RayCrossingSink): RayCrossingSupport {
        let depths: number[] = new Array<number>(SubdividedBezierPatch.MAXIMUM_HITS).fill(0);
        let normals: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
        for (let i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
        let countH: number[] = [0];
        const status: RayFirstHitStatus = this.collectHits(ray, query, countH, depths, normals);
        const count: number = countH[0]!;
        if (status === RayFirstHitStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
        if (status === RayFirstHitStatus.Unsupported) return RayCrossingSupport.Unsupported;
        if (status !== RayFirstHitStatus.Hit) return RayCrossingSupport.Indeterminate;
        let order: number[] = new Array<number>(SubdividedBezierPatch.MAXIMUM_HITS).fill(0);
        let sorted: number = 0;
        for (let i = 0; i < count; i++) {
            if (depths[i]! > query.maxT) continue;
            let at: number = sorted;
            while (at > 0 && depths[order[at - 1]!]! > depths[i]!) {
                order[at] = order[at - 1]!;
                at--;
            }
            order[at] = i;
            sorted++;
        }
        for (let i = 0; i < sorted; i++) {
            let event: RayCrossing = new RayCrossing(depths[order[i]!]!, this, RayCrossingKind.Unclassified);
            event.surfaceIndex = order[i]!;
            sink.crossing(event);
        }
        return RayCrossingSupport.Supported;
    }

    // Like PovRay, ignores hits nearer than POVRAY_MINIMUM_DEPTH: the
    // overlapping pieces of an adaptive subdivision would shadow themselves.
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit | null): boolean;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit | null): boolean {
        return (
            this.queryFirstHit(
                ray,
                new RayCrossingQuery(SubdividedBezierPatch.POVRAY_MINIMUM_DEPTH, Double.POSITIVE_INFINITY),
                hit,
            ) === RayFirstHitStatus.Hit
        );
    }

    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void;
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit | null): void;
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit | null): void {
        if (hit === null) return;
        hit.beginDetailEvaluation();
        if (hit.needsPoint()) {
            hit.point = ray.getOrigin().add(ray.getDirection().multiply(t));
            hit.markDetailAvailable(RayHit.DETAIL_POINT);
        }
        if (!hit.needsNormal()) return;
        let depths: number[] = new Array<number>(SubdividedBezierPatch.MAXIMUM_HITS).fill(0);
        let normals: Vector3Dd[] = Array.from({ length: SubdividedBezierPatch.MAXIMUM_HITS }, () => new Vector3Dd());
        for (let i = 0; i < SubdividedBezierPatch.MAXIMUM_HITS; i++) normals[i] = new Vector3Dd();
        let countH: number[] = [0];
        if (
            this.collectHits(ray, new RayCrossingQuery(0, Double.POSITIVE_INFINITY), countH, depths, normals) !==
            RayFirstHitStatus.Hit
        )
            return;
        const count: number = countH[0]!;
        // The caller's ray may be rebuilt from the hit, so match the nearest depth.
        let best: number = -1;
        for (let i = 0; i < count; i++) {
            if (best < 0 || JavaMath.abs(depths[i]! - t) < JavaMath.abs(depths[best]! - t)) best = i;
        }
        if (best >= 0 && JavaMath.abs(depths[best]! - t) <= 1.0e-6 * (1.0 + JavaMath.abs(t))) {
            hit.normal = normals[best]!;
            hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
        }
    }

    public override getMinMax(): Float64Array {
        let bounds: GeometryBounds = new GeometryBounds();
        this.getBounds(bounds);
        if (bounds.getKind() === GeometryBounds.Kind.Finite)
            return Float64Array.of(
                bounds.getMinimum().x(),
                bounds.getMinimum().y(),
                bounds.getMinimum().z(),
                bounds.getMaximum().x(),
                bounds.getMaximum().y(),
                bounds.getMaximum().z(),
            );
        return Float64Array.of(-1e308, -1e308, -1e308, 1e308, 1e308, 1e308);
    }
}

// The hit sink of every strategy; returns false once the hit budget is spent.
// cpp private nested class SubdividedBezierPatch::HitList.
class HitList {
    public used: number = 0;
    public maximum: number = 0;
    public count: number = 0;
    public depths: number[] = [];
    public points: Vector3Dd[] = [];
    public normals: Vector3Dd[] = [];
    public orientNormals: boolean = false;
    public add(depth: number, point: Vector3Dd, normal: Vector3Dd): boolean {
        if (this.used + this.count >= this.maximum) return false;
        this.depths[this.count] = depth;
        this.points[this.count] = point;
        this.normals[this.count] = normal;
        this.count++;
        return true;
    }
    public isFull(): boolean {
        return this.used + this.count >= this.maximum;
    }
}
