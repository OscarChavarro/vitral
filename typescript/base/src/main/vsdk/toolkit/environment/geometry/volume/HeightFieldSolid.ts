import { Double } from "../../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import { Math as JavaMath } from "../../../../../java/lang/Math.js";
import { ArrayList } from "../../../../../java/util/ArrayList.js";
import { Collections } from "../../../../../java/util/Collections.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import { FloatingInterval } from "../../../common/numerics/FloatingInterval.js";
import type { Geometry } from "../Geometry.js";
import { GeometryBounds } from "../element/GeometryBounds.js";
import { GeometryContainment } from "../element/GeometryContainment.js";
import type { Ray } from "../element/Ray.js";
import { RayBoundary } from "../element/RayBoundary.js";
import { RayCrossing } from "../element/RayCrossing.js";
import { RayCrossingKind } from "../element/RayCrossingKind.js";
import { RayCrossingQuery } from "../element/RayCrossingQuery.js";
import type { RayCrossingSink } from "../element/RayCrossingSink.js";
import { RayCrossingSupport } from "../element/RayCrossingSupport.js";
import { RayFirstHitStatus } from "../element/RayFirstHitStatus.js";
import { RayHit } from "../element/RayHit.js";
import { RaySolidBoundary } from "../element/RaySolidBoundary.js";
import { Box } from "./Box.js";
import { RaySolidClassification } from "../element/RaySolidClassification.js";
import { RaySolidQueryStatus } from "../element/RaySolidQueryStatus.js";
import { HeightFieldBlock } from "./HeightFieldBlock.js";
import { HeightFieldTraceResult } from "./HeightFieldTraceResult.js";
import { HeightFieldTraversal } from "./HeightFieldTraversal.js";
import { Solid } from "./Solid.js";

/**
Height field: heights h(x, z) on the integer grid 0 <= x < columns,
0 <= z < rows, stored in single precision and interpolated linearly over the
two triangles of each cell, split along the diagonal from (x+1, z) to
(x, z+1). The solid is the part of the bounding box below the surface,
{ min <= p <= max, y < h(x, z) }; the box must lie in the grid domain.
Surface index 0 is the height surface and 1..6 the box faces -x, +x, -y,
+y, -z, +z. Ray queries split the ray at cell edges and diagonals, where the
height difference is linear, so crossings are exact up to rounding.
traceFirstHit is the PovRay block traversal, which reports only the first
surface hit and never box faces. Edits invalidate the traversal blocks until
updateBlocks is called.
*/
export class HeightFieldSolid extends Solid {
    // Tolerances of the PovRay first-hit traversal.
    public static readonly INTERSECTION_EPSILON: number = 1.0e-5;

    public static readonly SMALL_TOLERANCE: number = 0.001;

    public static readonly MAX_DISTANCE: number = 1.0e7;

    private columns: number;

    private rows: number;

    private readonly heights: ArrayList<number> = new ArrayList<number>();

    private minimum: Vector3Dd;

    private maximum: Vector3Dd;

    private blockSize: number;

    private inverseBlockSize: number;

    private blockColumns: number;

    private blockRows: number;

    private readonly blocks: ArrayList<HeightFieldBlock> = new ArrayList<HeightFieldBlock>();

    private blocksValid: boolean;

    private static minValue(x: number, y: number): number {
        return x > y ? y : x;
    }

    private static maxValue(x: number, y: number): number {
        return x < y ? y : x;
    }

    private static signOf(x: number): number {
        return x > 0.0 ? 1 : x === 0.0 ? 0 : -1;
    }

    private heightAt(x: number, z: number): number {
        return this.heights.get(z * this.columns + x)!;
    }

    private blockAt(x: number, z: number): HeightFieldBlock | null {
        if (x < 0 || z < 0 || x >= this.blockColumns || z >= this.blockRows) return null;
        return this.blocks.get(x * this.blockRows + z);
    }

    private intersectPixel(
        x: number,
        z: number,
        origin: Vector3Dd,
        direction: Vector3Dd,
        state: HeightFieldTraversal,
        height1: number,
        height2: number,
    ): number {
        if (x < 0 || z < 0 || x + 1 >= this.columns || z + 1 >= this.rows) {
            return 0;
        }
        let t1V1: Vector3Dd = new Vector3Dd();
        let t1V2: Vector3Dd = new Vector3Dd();
        let t1V3: Vector3Dd = new Vector3Dd();
        let t2V1: Vector3Dd = new Vector3Dd();
        let t2V2: Vector3Dd = new Vector3Dd();
        let t2V3: Vector3Dd = new Vector3Dd();
        let localNormal: Vector3Dd = new Vector3Dd();
        let pos1: number;
        let pos2: number;
        let dot: number;
        let depth1: number = Double.POSITIVE_INFINITY;
        let depth2: number = Double.POSITIVE_INFINITY;
        let s: number;
        let t: number;
        let y1: number;
        let y2: number;
        let y3: number;
        let y4: number;
        let maxHeight: number;
        let minHeight: number;

        y1 = this.heightAt(x, z);
        y2 = this.heightAt(x + 1, z);
        y3 = this.heightAt(x, z + 1);
        y4 = this.heightAt(x + 1, z + 1);

        t1V1 = new Vector3Dd(x, y1, z);
        t1V2 = new Vector3Dd(1.0, y2 - y1, 0.0);
        t1V3 = new Vector3Dd(0.0, y3 - y1, 1.0);
        t2V1 = new Vector3Dd(x + 1, y4, z + 1);
        t2V2 = new Vector3Dd(-1.0, y3 - y4, 0.0);
        t2V3 = new Vector3Dd(0.0, y2 - y4, -1.0);

        // First, we check to see if it is even possible for the ray to
        // intersect the triangle.

        if (
            HeightFieldSolid.maxValue(y1, HeightFieldSolid.maxValue(y2, y3)) >= height1 &&
            HeightFieldSolid.minValue(y1, HeightFieldSolid.minValue(y2, y3)) <= height2
        ) {
            localNormal = t1V3.crossProduct(t1V2);
            dot = localNormal.dotProduct(direction);

            if (dot > HeightFieldSolid.INTERSECTION_EPSILON || dot < -HeightFieldSolid.INTERSECTION_EPSILON) {
                pos1 = localNormal.dotProduct(t1V1);

                pos2 = localNormal.dotProduct(origin);

                pos1 -= pos2;

                depth1 = pos1 / dot;

                if (depth1 > HeightFieldSolid.SMALL_TOLERANCE && depth1 < HeightFieldSolid.MAX_DISTANCE) {
                    s = origin.x() + depth1 * direction.x() - x;
                    t = origin.z() + depth1 * direction.z() - z;

                    if (
                        s < -HeightFieldSolid.INTERSECTION_EPSILON ||
                        t < -HeightFieldSolid.INTERSECTION_EPSILON ||
                        s + t > 1.0 + HeightFieldSolid.INTERSECTION_EPSILON
                    ) {
                        depth1 = Double.POSITIVE_INFINITY;
                    }
                } else {
                    depth1 = Double.POSITIVE_INFINITY;
                }
            }
        }

        /**
    First, we check to see if it is even possible for the ray to
    intersect the triangle.
    Rewritten to get around Code Builder FP stack problem.
    Original code:
         if((maxValue(y4,maxValue(y2,y3)) >=
    height1) && (minValue(y4,minValue(y2,y3)) <=
    height2))
    */

        maxHeight = HeightFieldSolid.maxValue(y4, HeightFieldSolid.maxValue(y2, y3));
        minHeight = HeightFieldSolid.minValue(y4, HeightFieldSolid.minValue(y2, y3));
        if (maxHeight >= height1 && minHeight <= height2) {
            localNormal = t2V3.crossProduct(t2V2);
            dot = localNormal.dotProduct(direction);

            if (dot > HeightFieldSolid.INTERSECTION_EPSILON || dot < -HeightFieldSolid.INTERSECTION_EPSILON) {
                pos1 = localNormal.dotProduct(t2V1);

                pos2 = localNormal.dotProduct(origin);
                pos1 -= pos2;

                depth2 = pos1 / dot;

                if (depth2 > HeightFieldSolid.SMALL_TOLERANCE && depth2 < HeightFieldSolid.MAX_DISTANCE) {
                    s = origin.x() + depth2 * direction.x() - x;
                    t = origin.z() + depth2 * direction.z() - z;

                    if (
                        s > 1.0 + HeightFieldSolid.INTERSECTION_EPSILON ||
                        t > 1.0 + HeightFieldSolid.INTERSECTION_EPSILON ||
                        s + t < 1.0 - HeightFieldSolid.INTERSECTION_EPSILON
                    ) {
                        depth2 = Double.POSITIVE_INFINITY;
                    }
                } else {
                    depth2 = Double.POSITIVE_INFINITY;
                }
            }
        }

        if (depth1 >= HeightFieldSolid.MAX_DISTANCE && depth2 >= HeightFieldSolid.MAX_DISTANCE) {
            return 0;
        }

        state.setDepth(depth2 < depth1 ? depth2 : depth1);
        return 1;
    }

    private intersectSubBlock(
        block: HeightFieldBlock | null,
        origin: Vector3Dd,
        direction: Vector3Dd,
        state: HeightFieldTraversal,
        start: Vector3Dd,
        end: Vector3Dd,
    ): number {
        if (block === null) {
            return 0;
        }
        let y1: number;
        let y2: number;
        let sx: number;
        let sy: number;
        let sz: number;
        let ex: number;
        let ez: number;
        let f: number;
        let ix: number;
        let iz: number;
        let length: number;
        let i: number;

        if (HeightFieldSolid.minValue(start.y(), end.y()) > block.getMaximumHeight()) {
            return 0;
        }

        if (HeightFieldSolid.maxValue(start.y(), end.y()) < block.getMinimumHeight()) {
            return 0;
        }

        sx = start.x();
        sy = start.y();
        sz = start.z();
        ex = end.x();
        ez = end.z();

        if (state.getXDom()) {
            if (state.getIsdx() >= 0) {
                f = JavaMath.floor(sx) - sx;
                sx = JavaMath.floor(sx);
                sy += state.getMyx() * f;
                sz += state.getMzx() * f;
                ex = JavaMath.floor(ex);
                ix = Math.trunc(sx);
            } else {
                f = JavaMath.ceil(sx) - sx;
                sx = JavaMath.ceil(sx);
                sy += state.getMyx() * f;
                sz += state.getMzx() * f;
                ex = JavaMath.ceil(ex);
                ix = Math.trunc(sx) - 1;
            }

            length = Math.trunc(0.5 + JavaMath.abs(ex - sx));

            if (state.getIsdz() >= 0) {
                f = sz - JavaMath.ceil(sz);
                iz = Math.trunc(JavaMath.floor(sz));
            } else {
                f = JavaMath.floor(sz) - sz;
                iz = Math.trunc(JavaMath.ceil(sz)) - 1;
            }

            if (state.getGdy() >= 0.0) {
                y1 = sy;
                y2 = sy + state.getGdy();
            } else {
                y1 = sy + state.getGdy();
                y2 = sy;
            }

            for (i = 0; i <= length; i++) {
                if (this.intersectPixel(ix, iz, origin, direction, state, y1, y2) !== 0) {
                    return 1;
                }
                f += state.getGdz();
                if (f >= 0.0) {
                    iz += state.getIsdz();
                    if (this.intersectPixel(ix, iz, origin, direction, state, y1, y2) !== 0) {
                        return 1;
                    }
                    f -= 1.0;
                }
                ix += state.getIsdx();
                y1 += state.getGdy();
                y2 += state.getGdy();
            }
        } else {
            if (state.getIsdz() >= 0) {
                f = JavaMath.floor(sz) - sz;
                sz = JavaMath.floor(sz);
                sy += state.getMyz() * f;
                sx += state.getMxz() * f;
                ez = JavaMath.floor(ez);
                iz = Math.trunc(sz);
            } else {
                f = JavaMath.ceil(sz) - sz;
                sz = JavaMath.ceil(sz);
                sy += state.getMyz() * f;
                sx += state.getMxz() * f;
                ez = JavaMath.ceil(ez);
                iz = Math.trunc(sz) - 1;
            }

            length = Math.trunc(0.5 + JavaMath.abs(ez - sz));

            if (state.getIsdx() >= 0) {
                f = sx - JavaMath.ceil(sx);
                ix = Math.trunc(JavaMath.floor(sx));
            } else {
                f = JavaMath.floor(sx) - sx;
                ix = Math.trunc(JavaMath.ceil(sx)) - 1;
            }

            if (state.getGdy() >= 0.0) {
                y1 = sy;
                y2 = sy + state.getGdy();
            } else {
                y1 = sy + state.getGdy();
                y2 = sy;
            }

            for (i = 0; i <= length; i++) {
                if (this.intersectPixel(ix, iz, origin, direction, state, y1, y2) !== 0) {
                    return 1;
                }
                f += state.getGdx();
                if (f >= 0.0) {
                    ix += state.getIsdx();
                    if (this.intersectPixel(ix, iz, origin, direction, state, y1, y2) !== 0) {
                        return 1;
                    }
                    f -= 1.0;
                }
                iz += state.getIsdz();
                y1 += state.getGdy();
                y2 += state.getGdy();
            }
        }
        return 0;
    }

    private intersectHfNode(
        origin: Vector3Dd,
        direction: Vector3Dd,
        state: HeightFieldTraversal,
        start: Vector3Dd,
        end: Vector3Dd,
    ): number {
        // cpp swaps pointers to two Vector3Dd values; the port swaps one-element holders.
        let curr: Vector3Dd[];
        let next: Vector3Dd[];
        let temp: Vector3Dd[];
        let temp1: Vector3Dd[] = [new Vector3Dd()];
        let temp2: Vector3Dd[] = [new Vector3Dd()];
        let sx: number;
        let sy: number;
        let sz: number;
        let ex: number;
        let ey: number;
        let ez: number;
        let x: number;
        let y: number;
        let z: number;
        let tnear: number;
        let tfar: number;
        let blockSize: number;
        let invBlkSize: number;
        let ix: number;
        let iz: number;
        let xSize: number;
        let zSize: number;
        let length: number;
        let i: number;

        x = sx = start.x();
        y = sy = start.y();
        z = sz = start.z();
        ex = end.x();
        ey = end.y();
        ez = end.z();

        blockSize = this.blockSize;
        invBlkSize = this.inverseBlockSize;

        xSize = JavaMath.abs(Math.trunc(ex * invBlkSize) - Math.trunc(sx * invBlkSize));
        zSize = JavaMath.abs(Math.trunc(ez * invBlkSize) - Math.trunc(sz * invBlkSize));
        length = xSize + zSize;

        curr = temp1;
        next = temp2;
        curr[0] = new Vector3Dd(x, y, z);
        let t: number;

        if (state.getXDom()) {
            if (state.getIsdx() >= 0) {
                ix = Math.trunc(JavaMath.floor(sx * invBlkSize));
                tnear = blockSize * (ix + 1) - sx;

                if (state.getIsdz() >= 0) {
                    iz = Math.trunc(JavaMath.floor(sz * invBlkSize));
                    tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
                } else {
                    iz = Math.trunc(JavaMath.ceil(sz * invBlkSize)) - 1;
                    tfar = state.getGdx() * (sz - blockSize * iz);
                }
                for (i = 0; i < length; i++) {
                    if (tnear < tfar) {
                        t = tnear;
                        x = sx + t;
                        y = sy + state.getMyx() * t;
                        z = sz + state.getMzx() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        ix++;
                        tnear = blockSize * (ix + 1) - sx;
                    } else {
                        t = tfar;
                        x = sx + t;
                        y = sy + state.getMyx() * t;
                        z = sz + state.getMzx() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        iz += state.getIsdz();
                        if (state.getIsdz() >= 0) {
                            tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
                        } else {
                            tfar = state.getGdx() * (sz - blockSize * iz);
                        }
                    }
                }
            } else {
                ix = Math.trunc(JavaMath.ceil(sx * invBlkSize)) - 1;
                tnear = sx - blockSize * ix;

                if (state.getIsdz() >= 0) {
                    iz = Math.trunc(JavaMath.floor(sz * invBlkSize));
                    tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
                } else {
                    iz = Math.trunc(JavaMath.ceil(sz * invBlkSize)) - 1;
                    tfar = state.getGdx() * (sz - blockSize * iz);
                }

                for (i = 0; i < length; i++) {
                    if (tnear < tfar) {
                        t = tnear;
                        x = sx - t;
                        y = sy - state.getMyx() * t;
                        z = sz - state.getMzx() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        ix--;
                        tnear = sx - blockSize * ix;
                    } else {
                        t = tfar;
                        x = sx - t;
                        y = sy - state.getMyx() * t;
                        z = sz - state.getMzx() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        iz += state.getIsdz();
                        if (state.getIsdz() >= 0) {
                            tfar = state.getGdx() * (blockSize * (iz + 1) - sz);
                        } else {
                            tfar = state.getGdx() * (sz - blockSize * iz);
                        }
                    }
                }
            }
        } else {
            if (state.getIsdz() >= 0) {
                iz = Math.trunc(JavaMath.floor(sz * invBlkSize));
                tnear = blockSize * (iz + 1) - sz;

                if (state.getIsdx() >= 0) {
                    ix = Math.trunc(JavaMath.floor(sx * invBlkSize));
                    tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
                } else {
                    ix = Math.trunc(JavaMath.ceil(sx * invBlkSize)) - 1;
                    tfar = state.getGdz() * (sx - blockSize * ix);
                }
                for (i = 0; i < length; i++) {
                    if (tnear < tfar) {
                        t = tnear;
                        z = sz + t;
                        y = sy + state.getMyz() * t;
                        x = sx + state.getMxz() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        iz++;
                        tnear = blockSize * (iz + 1) - sz;
                    } else {
                        t = tfar;
                        z = sz + t;
                        y = sy + state.getMyz() * t;
                        x = sx + state.getMxz() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        ix += state.getIsdx();
                        if (state.getIsdx() >= 0) {
                            tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
                        } else {
                            tfar = state.getGdz() * (sx - blockSize * ix);
                        }
                    }
                }
            } else {
                iz = Math.trunc(JavaMath.ceil(sz * invBlkSize)) - 1;
                tnear = sz - blockSize * iz;

                if (state.getIsdx() >= 0) {
                    ix = Math.trunc(JavaMath.floor(sx * invBlkSize));
                    tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
                } else {
                    ix = Math.trunc(JavaMath.ceil(sx * invBlkSize)) - 1;
                    tfar = state.getGdz() * (sx - blockSize * ix);
                }
                for (i = 0; i < length; i++) {
                    if (tnear < tfar) {
                        t = tnear;
                        z = sz - t;
                        y = sy - state.getMyz() * t;
                        x = sx - state.getMxz() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        iz--;
                        tnear = sz - blockSize * iz;
                    } else {
                        t = tfar;
                        z = sz - t;
                        y = sy - state.getMyz() * t;
                        x = sx - state.getMxz() * t;
                        next[0] = new Vector3Dd(x, y, z);
                        if (
                            this.intersectSubBlock(
                                this.blockAt(ix, iz),
                                origin,
                                direction,
                                state,
                                curr[0]!,
                                next[0]!,
                            ) !== 0
                        ) {
                            return 1;
                        }
                        temp = curr;
                        curr = next;
                        next = temp;
                        ix += state.getIsdx();
                        if (state.getIsdx() >= 0) {
                            tfar = state.getGdz() * (blockSize * (ix + 1) - sx);
                        } else {
                            tfar = state.getGdz() * (sx - blockSize * ix);
                        }
                    }
                }
            }
        }
        next[0] = new Vector3Dd(ex, ey, ez);
        if (state.getIsdx() >= 0) {
            ix = Math.trunc(JavaMath.floor(ex * invBlkSize));
        } else {
            ix = Math.trunc(JavaMath.ceil(ex * invBlkSize)) - 1;
        }
        if (state.getIsdz() >= 0) {
            iz = Math.trunc(JavaMath.floor(ez * invBlkSize));
        } else {
            iz = Math.trunc(JavaMath.ceil(ez * invBlkSize)) - 1;
        }
        if (this.intersectSubBlock(this.blockAt(ix, iz), origin, direction, state, curr[0]!, next[0]!) !== 0) {
            return 1;
        }
        return 0;
    }

    private isInsideGrid(x: number, z: number): boolean {
        return x >= 0 && z >= 0 && x <= this.columns - 1 && z <= this.rows - 1;
    }

    private heightDifference(
        origin: Vector3Dd,
        direction: Vector3Dd,
        t: number,
        cellX: number,
        cellZ: number,
        firstTriangle: boolean,
    ): number {
        const x: number = origin.x() + direction.x() * t;
        const y: number = origin.y() + direction.y() * t;
        const z: number = origin.z() + direction.z() * t;
        const fx: number = x - cellX;
        const fz: number = z - cellZ;
        if (firstTriangle) {
            const y1: number = this.getHeight(cellX, cellZ);
            return (
                y - (y1 + fx * (this.getHeight(cellX + 1, cellZ) - y1) + fz * (this.getHeight(cellX, cellZ + 1) - y1))
            );
        }
        const y4: number = this.getHeight(cellX + 1, cellZ + 1);
        return (
            y -
            (y4 +
                (1.0 - fx) * (this.getHeight(cellX, cellZ + 1) - y4) +
                (1.0 - fz) * (this.getHeight(cellX + 1, cellZ) - y4))
        );
    }

    private heightRange(
        lowX: number,
        highX: number,
        lowZ: number,
        highZ: number,
        lowest: number[],
        highest: number[],
    ): void {
        let x0: number = Math.trunc(JavaMath.floor(lowX)),
            x1: number = Math.trunc(JavaMath.ceil(highX));
        let z0: number = Math.trunc(JavaMath.floor(lowZ)),
            z1: number = Math.trunc(JavaMath.ceil(highZ));
        if (x0 < 0) x0 = 0;
        if (z0 < 0) z0 = 0;
        if (x1 > this.columns - 1) x1 = this.columns - 1;
        if (z1 > this.rows - 1) z1 = this.rows - 1;
        lowest[0] = Double.POSITIVE_INFINITY;
        highest[0] = Double.NEGATIVE_INFINITY;
        for (let z = z0; z <= z1; z++) {
            for (let x = x0; x <= x1; x++) {
                const h: number = this.getHeight(x, z);
                if (h < lowest[0]) lowest[0] = h;
                if (h > highest[0]) highest[0] = h;
            }
        }
    }

    private nearestSurface(point: Vector3Dd): number {
        let best: number = 0;
        let distance: number = Double.POSITIVE_INFINITY;
        const h: number = this.evaluateHeight(point.x(), point.z());
        if (Double.isFinite(h)) distance = JavaMath.abs(point.y() - h);
        const faces: number[] = [
            point.x() - this.minimum.x(),
            this.maximum.x() - point.x(),
            point.y() - this.minimum.y(),
            this.maximum.y() - point.y(),
            point.z() - this.minimum.z(),
            this.maximum.z() - point.z(),
        ];
        for (let i = 0; i < 6; i++) {
            if (JavaMath.abs(faces[i]!) < distance) {
                distance = JavaMath.abs(faces[i]!);
                best = i + 1;
            }
        }
        return best;
    }

    private surfaceNormal(point: Vector3Dd, surfaceIndex: number): Vector3Dd {
        switch (Math.trunc(surfaceIndex)) {
            case 1:
                return new Vector3Dd(-1, 0, 0);
            case 2:
                return new Vector3Dd(1, 0, 0);
            case 3:
                return new Vector3Dd(0, -1, 0);
            case 4:
                return new Vector3Dd(0, 1, 0);
            case 5:
                return new Vector3Dd(0, 0, -1);
            case 6:
                return new Vector3Dd(0, 0, 1);
            default:
                break;
        }
        let first: Vector3Dd[] = [new Vector3Dd()],
            second: Vector3Dd[] = [new Vector3Dd()];
        this.getSurfaceTangents(point, first, second);
        return second[0]!.crossProduct(first[0]!);
    }

    private classifyRegion(center: Vector3Dd, halfWidths: Vector3Dd, tolerance: number): GeometryContainment {
        if (!this.isValid()) return GeometryContainment.Indeterminate;
        if (!FloatingInterval.isSupportedEnvironment()) return GeometryContainment.Unsupported;
        const x: FloatingInterval = FloatingInterval.around(center.x(), halfWidths.x());
        const y: FloatingInterval = FloatingInterval.around(center.y(), halfWidths.y());
        const z: FloatingInterval = FloatingInterval.around(center.z(), halfWidths.z());
        if (!x.isValid() || !y.isValid() || !z.isValid()) return GeometryContainment.InvalidInput;
        if (
            x.getUpper() < this.minimum.x() ||
            x.getLower() > this.maximum.x() ||
            y.getUpper() < this.minimum.y() ||
            y.getLower() > this.maximum.y() ||
            z.getUpper() < this.minimum.z() ||
            z.getLower() > this.maximum.z()
        )
            return GeometryContainment.Outside;
        let lowestH: number[] = [0],
            highestH: number[] = [0];
        this.heightRange(x.getLower(), x.getUpper(), z.getLower(), z.getUpper(), lowestH, highestH);
        const lowest: number = lowestH[0]!,
            highest: number = highestH[0]!;
        // The interpolated surface lies between the heights of its cell corners.
        if (y.getLower() > highest) return GeometryContainment.Outside;
        if (
            x.getLower() > this.minimum.x() &&
            x.getUpper() < this.maximum.x() &&
            y.getLower() > this.minimum.y() &&
            y.getUpper() < this.maximum.y() &&
            z.getLower() > this.minimum.z() &&
            z.getUpper() < this.maximum.z() &&
            y.getUpper() < lowest
        )
            return GeometryContainment.Inside;
        if (
            !(tolerance > 0) ||
            !(center.x() > this.minimum.x()) ||
            !(center.x() < this.maximum.x()) ||
            !(center.z() > this.minimum.z()) ||
            !(center.z() < this.maximum.z())
        )
            return GeometryContainment.Indeterminate;
        // A surface point straight above or below the centre within the ball.
        let cx: number = Math.trunc(JavaMath.floor(center.x()));
        let cz: number = Math.trunc(JavaMath.floor(center.z()));
        if (cx > this.columns - 2) cx = this.columns - 2;
        if (cz > this.rows - 2) cz = this.rows - 2;
        const fx: FloatingInterval = FloatingInterval.exact(center.x()).subtract(FloatingInterval.exact(cx));
        const fz: FloatingInterval = FloatingInterval.exact(center.z()).subtract(FloatingInterval.exact(cz));
        let h: FloatingInterval = new FloatingInterval();
        if (fx.add(fz).getUpper() <= 1.0) {
            const y1: FloatingInterval = FloatingInterval.exact(this.getHeight(cx, cz));
            h = y1
                .add(fx.multiply(FloatingInterval.exact(this.getHeight(cx + 1, cz)).subtract(y1)))
                .add(fz.multiply(FloatingInterval.exact(this.getHeight(cx, cz + 1)).subtract(y1)));
        } else if (fx.add(fz).getLower() > 1.0) {
            const y4: FloatingInterval = FloatingInterval.exact(this.getHeight(cx + 1, cz + 1));
            const one: FloatingInterval = FloatingInterval.exact(1.0);
            h = y4
                .add(one.subtract(fx).multiply(FloatingInterval.exact(this.getHeight(cx, cz + 1)).subtract(y4)))
                .add(one.subtract(fz).multiply(FloatingInterval.exact(this.getHeight(cx + 1, cz)).subtract(y4)));
        } else {
            return GeometryContainment.Indeterminate;
        }
        if (!h.isValid() || !(h.getLower() > this.minimum.y()) || !(h.getUpper() < this.maximum.y()))
            return GeometryContainment.Indeterminate;
        const offset: FloatingInterval = h.subtract(FloatingInterval.exact(center.y()));
        if (
            offset.isValid() &&
            JavaMath.max(JavaMath.abs(offset.getLower()), JavaMath.abs(offset.getUpper())) < tolerance
        )
            return GeometryContainment.Boundary;
        return GeometryContainment.Indeterminate;
    }

    // Heights start at zero. A grid needs at least 2 x 2 points.
    public constructor(columnCount: number, rowCount: number, minimumCorner: Vector3Dd, maximumCorner: Vector3Dd);
    /** cpp copy constructor. */
    public constructor(other: HeightFieldSolid);
    public constructor(
        columnCountOrOther: number | HeightFieldSolid,
        rowCount?: number,
        minimumCorner?: Vector3Dd,
        maximumCorner?: Vector3Dd,
    ) {
        super();
        if (columnCountOrOther instanceof HeightFieldSolid) {
            const other = columnCountOrOther;
            this.columns = other.columns;
            this.rows = other.rows;
            this.heights.addAll(other.heights);
            this.minimum = other.minimum;
            this.maximum = other.maximum;
            this.blockSize = other.blockSize;
            this.inverseBlockSize = other.inverseBlockSize;
            this.blockColumns = other.blockColumns;
            this.blockRows = other.blockRows;
            for (let i = 0; i < other.blocks.size(); i++) this.blocks.add(new HeightFieldBlock(other.blocks.get(i)));
            this.blocksValid = other.blocksValid;
            return;
        }
        const columnCount = columnCountOrOther;
        this.columns = columnCount < 2 ? 2 : columnCount;
        this.rows = (rowCount as number) < 2 ? 2 : (rowCount as number);
        this.minimum = minimumCorner as Vector3Dd;
        this.maximum = maximumCorner as Vector3Dd;
        this.blockSize = 0;
        this.inverseBlockSize = 0;
        this.blockColumns = 0;
        this.blockRows = 0;
        this.blocksValid = false;
        for (let i = 0; i < this.columns * this.rows; i++) this.heights.add(0.0);
    }

    /** cpp copy assignment. */
    public assign(other: HeightFieldSolid): HeightFieldSolid {
        if (this !== other) {
            this.columns = other.columns;
            this.rows = other.rows;
            this.heights.clear();
            this.heights.addAll(other.heights);
            this.minimum = other.minimum;
            this.maximum = other.maximum;
            this.blockSize = other.blockSize;
            this.inverseBlockSize = other.inverseBlockSize;
            this.blockColumns = other.blockColumns;
            this.blockRows = other.blockRows;
            this.blocks.clear();
            for (let i = 0; i < other.blocks.size(); i++) this.blocks.add(new HeightFieldBlock(other.blocks.get(i)));
            this.blocksValid = other.blocksValid;
        }
        return this;
    }

    public getColumnCount(): number {
        return this.columns;
    }

    public getRowCount(): number {
        return this.rows;
    }

    public getHeight(x: number, z: number): number {
        return this.heights.get(Math.trunc(z * this.columns + x));
    }

    public setHeight(x: number, z: number, height: number): void {
        this.heights.set(Math.trunc(z * this.columns + x), Math.fround(height));
        this.blocksValid = false;
    }

    public getMinimum(): Vector3Dd {
        return this.minimum;
    }

    public getMaximum(): Vector3Dd {
        return this.maximum;
    }

    public setBounds(minimumCorner: Vector3Dd, maximumCorner: Vector3Dd): void {
        this.minimum = minimumCorner;
        this.maximum = maximumCorner;
        this.blocksValid = false;
    }

    // Grid, finite heights and an ordered box inside the grid domain.
    public isValid(): boolean {
        const values: number[] = [
            this.minimum.x(),
            this.minimum.y(),
            this.minimum.z(),
            this.maximum.x(),
            this.maximum.y(),
            this.maximum.z(),
        ];
        for (let i = 0; i < 6; i++) if (!Double.isFinite(values[i]!)) return false;
        return (
            this.minimum.x() < this.maximum.x() &&
            this.minimum.y() < this.maximum.y() &&
            this.minimum.z() < this.maximum.z() &&
            this.minimum.x() >= 0 &&
            this.minimum.z() >= 0 &&
            this.maximum.x() <= this.columns - 1 &&
            this.maximum.z() <= this.rows - 1
        );
    }

    // Rebuilds the traversal blocks; heights below the box are counted at its
    // floor.
    public updateBlocks(): void {
        const maxX: number = Math.trunc(this.columns - 1);
        const maxZ: number = Math.trunc(this.rows - 1);
        const size: number = HeightFieldSolid.maxValue(maxX, maxZ);
        this.blockSize = JavaMath.ceil(JavaMath.sqrt(size + 1.0));
        this.inverseBlockSize = 1.0 / this.blockSize;
        this.blockColumns = Math.trunc(JavaMath.ceil((maxX + 1.0) * this.inverseBlockSize));
        this.blockRows = Math.trunc(JavaMath.ceil((maxZ + 1.0) * this.inverseBlockSize));
        this.blocks.clear();
        for (let i = 0; i < this.blockColumns * this.blockRows; i++) this.blocks.add(new HeightFieldBlock());
        const n: number = Math.trunc(this.blockSize);
        // Adjacent blocks share their border points.
        for (let j = 0; j < this.blockRows; j++) {
            for (let j2 = 0; j2 <= n && j * n + j2 <= maxZ; j2++) {
                const z: number = j * n + j2;
                for (let i = 0; i < this.blockColumns; i++) {
                    for (let i2 = 0; i2 <= n && i * n + i2 <= maxX; i2++) {
                        const x: number = i * n + i2;
                        let height: number = this.heightAt(x, z);
                        if (height < this.minimum.y()) {
                            height = this.minimum.y();
                        }
                        this.blocks.get(Math.trunc(Math.trunc(i) * this.blockRows + j)).include(height);
                    }
                }
            }
        }
        this.blocksValid = true;
    }

    public hasCurrentBlocks(): boolean {
        return this.blocksValid;
    }

    // Interpolated height, NaN outside the grid.
    public evaluateHeight(x: number, z: number): number {
        if (!this.isInsideGrid(x, z)) return Double.NaN;
        let cx: number = Math.trunc(JavaMath.floor(x));
        let cz: number = Math.trunc(JavaMath.floor(z));
        if (cx > this.columns - 2) cx = this.columns - 2;
        if (cz > this.rows - 2) cz = this.rows - 2;
        const fx: number = x - cx;
        const fz: number = z - cz;
        if (fx + fz <= 1.0) {
            const y1: number = this.getHeight(cx, cz);
            return y1 + fx * (this.getHeight(cx + 1, cz) - y1) + fz * (this.getHeight(cx, cz + 1) - y1);
        }
        const y4: number = this.getHeight(cx + 1, cz + 1);
        return y4 + (1.0 - fx) * (this.getHeight(cx, cz + 1) - y4) + (1.0 - fz) * (this.getHeight(cx + 1, cz) - y4);
    }

    // Edge vectors of the triangle under the point, in the PovRay order; their
    // cross product second x first is the upward surface normal.
    public getSurfaceTangents(point: Vector3Dd, first: Vector3Dd[], second: Vector3Dd[]): void {
        let px: number = Math.trunc(point.x());
        let pz: number = Math.trunc(point.z());
        if (px < 0) px = 0;
        if (pz < 0) pz = 0;
        if (px > this.columns - 2) px = Math.trunc(this.columns - 2);
        if (pz > this.rows - 2) pz = Math.trunc(this.rows - 2);
        const x: number = point.x() - px;
        const z: number = point.z() - pz;
        if (x + z <= 1) {
            const y1: number = this.heightAt(px, pz);
            const y2: number = this.heightAt(px + 1, pz);
            const y3: number = this.heightAt(px, pz + 1);
            first[0] = new Vector3Dd(1.0, y2 - y1, 0.0);
            second[0] = new Vector3Dd(0.0, y3 - y1, 1.0);
        } else {
            const y1: number = this.heightAt(px + 1, pz + 1);
            const y2: number = this.heightAt(px, pz + 1);
            const y3: number = this.heightAt(px + 1, pz);
            first[0] = new Vector3Dd(-1.0, y2 - y1, 0.0);
            second[0] = new Vector3Dd(0.0, y3 - y1, -1.0);
        }
    }

    // PovRay first surface hit along origin + depth*direction, depth beyond
    // SMALL_TOLERANCE; requires current blocks. Like PovRay height fields, the
    // box must stay at least one unit inside the grid on x and z
    // (1 <= x <= columns - 2, likewise z); otherwise hits may be missed. It
    // can also report hits on the cells just outside the box. The typed ray
    // queries have neither limitation.
    public traceFirstHit(origin: Vector3Dd, direction: Vector3Dd, depth: number[]): HeightFieldTraceResult {
        depth[0] = 0.0;
        if (!this.blocksValid) {
            return HeightFieldTraceResult.MissedBounds;
        }
        let depth1H: number[] = [0.0];
        let depth2H: number[] = [Double.POSITIVE_INFINITY];
        let faceOverlap: boolean[] = [false];
        if (
            Box.intersectBounds(
                origin,
                direction,
                this.minimum,
                this.maximum,
                HeightFieldSolid.INTERSECTION_EPSILON,
                false,
                depth1H,
                depth2H,
                faceOverlap,
            ) !== RayFirstHitStatus.Hit
        ) {
            return HeightFieldTraceResult.MissedBounds;
        }
        let depth1: number = depth1H[0]!;
        let depth2: number = depth2H[0]!;
        if (depth1 < HeightFieldSolid.SMALL_TOLERANCE || depth1 > HeightFieldSolid.MAX_DISTANCE) {
            if (depth2 < HeightFieldSolid.SMALL_TOLERANCE || depth2 > HeightFieldSolid.MAX_DISTANCE) {
                return HeightFieldTraceResult.MissedBounds;
            }
            depth1 = depth2;
        } else if (depth2 < HeightFieldSolid.SMALL_TOLERANCE || depth2 > HeightFieldSolid.MAX_DISTANCE) {
            depth2 = depth1;
        }
        if (depth1 === depth2) {
            depth1 = 0.0;
        }
        const temp1: Vector3Dd = direction.multiply(depth1).add(origin);
        const temp2: Vector3Dd = direction.multiply(depth2).add(origin);
        let mzx: number;
        let myx: number;
        if (JavaMath.abs(direction.x()) > HeightFieldSolid.INTERSECTION_EPSILON) {
            mzx = direction.z() / direction.x();
            myx = direction.y() / direction.x();
        } else {
            mzx = direction.z() / HeightFieldSolid.INTERSECTION_EPSILON;
            myx = direction.y() / HeightFieldSolid.INTERSECTION_EPSILON;
        }
        let mxz: number;
        let myz: number;
        if (JavaMath.abs(direction.z()) > HeightFieldSolid.INTERSECTION_EPSILON) {
            mxz = direction.x() / direction.z();
            myz = direction.y() / direction.z();
        } else {
            mxz = direction.x() / HeightFieldSolid.INTERSECTION_EPSILON;
            myz = direction.y() / HeightFieldSolid.INTERSECTION_EPSILON;
        }
        const isdx: number = HeightFieldSolid.signOf(direction.x());
        const isdz: number = HeightFieldSolid.signOf(direction.z());
        const xDom: boolean = JavaMath.abs(direction.x()) >= JavaMath.abs(direction.z());
        const gdx: number = JavaMath.abs(mxz);
        const gdz: number = JavaMath.abs(mzx);
        const gdy: number = xDom ? myx * isdx : myz * isdz;
        let state: HeightFieldTraversal = new HeightFieldTraversal(isdx, isdz, xDom, gdx, gdy, gdz, myx, mxz, mzx, myz);
        if (this.intersectHfNode(origin, direction, state, temp1, temp2) !== 0) {
            depth[0] = state.getDepth();
            return HeightFieldTraceResult.Hit;
        }
        return HeightFieldTraceResult.MissedSurface;
    }

    public override copyGeometry(): Geometry {
        return new HeightFieldSolid(this);
    }

    public override getBounds(result: GeometryBounds): void {
        if (!this.isValid()) {
            result.setIndeterminate();
            return;
        }
        let lowestH: number[] = [0],
            highestH: number[] = [0];
        this.heightRange(this.minimum.x(), this.maximum.x(), this.minimum.z(), this.maximum.z(), lowestH, highestH);
        // cpp also reads the lowest height here, unused by this bound.
        const highest: number = highestH[0]!;
        const top: number = JavaMath.min(this.maximum.y(), highest);
        if (!Double.isFinite(top)) {
            result.setIndeterminate();
            return;
        }
        if (!(top > this.minimum.y())) {
            result.setEmpty();
            return;
        }
        if (!result.setFinite(this.minimum, new Vector3Dd(this.maximum.x(), top, this.maximum.z())))
            result.setIndeterminate();
    }

    public override classifyPoint(point: Vector3Dd, tolerance: number): GeometryContainment {
        if (!Double.isFinite(tolerance) || tolerance < 0) return GeometryContainment.InvalidInput;
        if (tolerance !== 0) return this.classifyPointCertified(point, tolerance);
        if (!this.isValid()) return GeometryContainment.Indeterminate;
        if (
            point.x() < this.minimum.x() ||
            point.x() > this.maximum.x() ||
            point.y() < this.minimum.y() ||
            point.y() > this.maximum.y() ||
            point.z() < this.minimum.z() ||
            point.z() > this.maximum.z()
        )
            return GeometryContainment.Outside;
        const h: number = this.evaluateHeight(point.x(), point.z());
        if (!Double.isFinite(h)) return GeometryContainment.Indeterminate;
        if (point.y() > h) return GeometryContainment.Outside;
        if (
            point.y() < h &&
            point.x() > this.minimum.x() &&
            point.x() < this.maximum.x() &&
            point.y() > this.minimum.y() &&
            point.y() < this.maximum.y() &&
            point.z() > this.minimum.z() &&
            point.z() < this.maximum.z()
        )
            return GeometryContainment.Inside;
        return GeometryContainment.Boundary;
    }

    public override classifyPointCertified(point: Vector3Dd, tolerance: number): GeometryContainment {
        if (
            !Double.isFinite(tolerance) ||
            tolerance < 0 ||
            !Double.isFinite(point.x()) ||
            !Double.isFinite(point.y()) ||
            !Double.isFinite(point.z())
        )
            return GeometryContainment.InvalidInput;
        return this.classifyRegion(point, new Vector3Dd(tolerance, tolerance, tolerance), tolerance);
    }

    public override classifyBoxCertified(center: Vector3Dd, halfWidths: Vector3Dd): GeometryContainment {
        const result: GeometryContainment = this.classifyRegion(center, halfWidths, -1.0);
        return result === GeometryContainment.Boundary ? GeometryContainment.Indeterminate : result;
    }

    // cpp lambda `addLines` of classifyRay, capturing cuts, entry and exit.
    private static addLines(cuts: ArrayList<number>, entry: number, exit: number, start: number, slope: number): void {
        if (slope === 0) return;
        const a: number = start + slope * entry,
            b: number = start + slope * exit;
        const low: number = JavaMath.min(a, b),
            high: number = JavaMath.max(a, b);
        for (let k = JavaMath.ceil(low); k <= high; k += 1.0) {
            const t: number = (k - start) / slope;
            if (t > entry && t < exit) cuts.add(t);
        }
    }

    public override classifyRay(
        ray: Ray,
        query: RayCrossingQuery,
        result: RaySolidClassification,
    ): RaySolidQueryStatus {
        result.clear();
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
            query.minT >= query.maxT
        )
            return RaySolidQueryStatus.InvalidInput;
        if (query.minimumChordSquared !== 0 || query.minimumExteriorApproach !== Double.NEGATIVE_INFINITY)
            return RaySolidQueryStatus.Unsupported;
        if (!this.isValid()) return RaySolidQueryStatus.Indeterminate;
        let entryH: number[] = [query.minT],
            exitH: number[] = [query.maxT];
        let overlapH: boolean[] = [false];
        const clip: RayFirstHitStatus = Box.intersectBounds(
            o,
            d,
            this.minimum,
            this.maximum,
            0.0,
            true,
            entryH,
            exitH,
            overlapH,
        );
        const entry: number = entryH[0]!,
            exit: number = exitH[0]!;
        const overlap: boolean = overlapH[0]!;
        if (clip === RayFirstHitStatus.InvalidInput) return RaySolidQueryStatus.InvalidInput;
        if (clip === RayFirstHitStatus.Miss || (clip === RayFirstHitStatus.Hit && !overlap && !(exit > entry))) {
            result.insideSegments.add(false);
            return RaySolidQueryStatus.Supported;
        }
        if (clip !== RayFirstHitStatus.Hit || overlap || !Double.isFinite(entry) || !Double.isFinite(exit))
            return RaySolidQueryStatus.Indeterminate;

        // Cuts at cell edges and diagonals, where the surface plane changes.
        let cuts: ArrayList<number> = new ArrayList<number>();
        cuts.add(entry);
        cuts.add(exit);
        HeightFieldSolid.addLines(cuts, entry, exit, o.x(), d.x());
        HeightFieldSolid.addLines(cuts, entry, exit, o.z(), d.z());
        HeightFieldSolid.addLines(cuts, entry, exit, o.x() + o.z(), d.x() + d.z());
        Collections.sort(cuts, null);

        let starts: ArrayList<number> = new ArrayList<number>();
        let inside: ArrayList<boolean> = new ArrayList<boolean>();
        if (entry > query.minT) {
            starts.add(query.minT);
            inside.add(false);
        }
        for (let i = 0; i + 1 < cuts.size(); i++) {
            const ta: number = cuts.get(i),
                tb: number = cuts.get(i + 1);
            if (!(tb > ta)) continue;
            const mid: number = 0.5 * (ta + tb);
            let cx: number = Math.trunc(JavaMath.floor(o.x() + d.x() * mid));
            let cz: number = Math.trunc(JavaMath.floor(o.z() + d.z() * mid));
            if (cx < 0) cx = 0;
            if (cz < 0) cz = 0;
            if (cx > this.columns - 2) cx = this.columns - 2;
            if (cz > this.rows - 2) cz = this.rows - 2;
            const first: boolean = o.x() + d.x() * mid - cx + (o.z() + d.z() * mid - cz) <= 1.0;
            const ga: number = this.heightDifference(o, d, ta, cx, cz, first);
            const gb: number = this.heightDifference(o, d, tb, cx, cz, first);
            if (!Double.isFinite(ga) || !Double.isFinite(gb)) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
            if ((ga < 0 && gb > 0) || (ga > 0 && gb < 0)) {
                // One crossing of the plane inside this piece.
                const root: number = ta + (tb - ta) * (ga / (ga - gb));
                starts.add(ta);
                inside.add(ga < 0);
                if (root > ta && root < tb) {
                    starts.add(root);
                    inside.add(gb < 0);
                }
                continue;
            }
            const gm: number = this.heightDifference(o, d, mid, cx, cz, first);
            if (gm === 0 || !Double.isFinite(gm)) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
            starts.add(ta);
            inside.add(gm < 0);
        }
        if (exit < query.maxT) {
            starts.add(exit);
            inside.add(false);
        }
        if (starts.isEmpty()) {
            result.insideSegments.add(false);
            return RaySolidQueryStatus.Supported;
        }
        let state: boolean = inside.get(0);
        result.insideSegments.add(state);
        for (let i = 1; i < starts.size(); i++) {
            if (inside.get(i) === state) continue;
            state = inside.get(i);
            const t: number = starts.get(i);
            let surface: number = 0;
            if (t === entry || t === exit) surface = this.nearestSurface(o.add(d.multiply(t)));
            if (
                !result.boundaries.add(new RaySolidBoundary(t, this, false, surface)) ||
                !result.insideSegments.add(state)
            ) {
                result.clear();
                return RaySolidQueryStatus.Indeterminate;
            }
        }
        return RaySolidQueryStatus.Supported;
    }

    public override emitRayCrossings(ray: Ray, query: RayCrossingQuery, sink: RayCrossingSink): RayCrossingSupport {
        let classification: RaySolidClassification = new RaySolidClassification();
        const status: RaySolidQueryStatus = this.classifyRay(ray, query, classification);
        if (status === RaySolidQueryStatus.InvalidInput) return RayCrossingSupport.InvalidInput;
        if (status === RaySolidQueryStatus.Unsupported) return RayCrossingSupport.Unsupported;
        if (status !== RaySolidQueryStatus.Supported) return RayCrossingSupport.Indeterminate;
        for (let i = 0; i < classification.boundaries.size(); i++) {
            sink.crossing(
                new RayCrossing(
                    classification.boundaries.get(i).t,
                    this,
                    classification.insideSegments.get(i + 1) ? RayCrossingKind.Entering : RayCrossingKind.Leaving,
                ),
            );
        }
        return RayCrossingSupport.Supported;
    }

    public override queryFirstHit(
        ray: Ray,
        query: RayCrossingQuery,
        hit: RayHit | null,
        boundary: RayBoundary | null = null,
    ): RayFirstHitStatus {
        if (hit !== null) hit.clear();
        if (boundary !== null) boundary.assign(new RayBoundary());
        if (!Double.isNaN(query.minT) && query.minT === query.maxT) return RayFirstHitStatus.Unsupported;
        let classification: RaySolidClassification = new RaySolidClassification();
        const status: RaySolidQueryStatus = this.classifyRay(ray, query, classification);
        if (status === RaySolidQueryStatus.InvalidInput) return RayFirstHitStatus.InvalidInput;
        if (status === RaySolidQueryStatus.Unsupported) return RayFirstHitStatus.Unsupported;
        if (status !== RaySolidQueryStatus.Supported) return RayFirstHitStatus.Indeterminate;
        if (classification.boundaries.isEmpty()) return RayFirstHitStatus.Miss;
        const first: RayBoundary = classification.boundaries.get(0);
        if (boundary !== null) boundary.assign(first);
        if (hit !== null) {
            hit.setHitDistance(first.t);
            if (hit.shouldStoreRay()) hit.setRay(ray.withT(first.t));
            if (hit.needsAnySurfaceData()) {
                hit.beginDetailEvaluation();
                const point: Vector3Dd = ray.getOrigin().add(ray.getDirection().multiply(first.t));
                if (hit.needsPoint()) {
                    hit.point = point;
                    hit.markDetailAvailable(RayHit.DETAIL_POINT);
                }
                if (hit.needsNormal()) {
                    const normal: Vector3Dd = this.surfaceNormal(point, first.surfaceIndex);
                    const length: number = normal.length();
                    if (Double.isFinite(length) && length > 0) {
                        hit.normal = normal.multiply(1.0 / length);
                        hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
                    }
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
            boundary.surfaceIndex > 6
        )
            return RayFirstHitStatus.InvalidInput;
        if (boundary.getInstanceCount() !== 0) return RayFirstHitStatus.Unsupported;
        hit.setHitDistance(boundary.t);
        if (hit.shouldStoreRay()) hit.setRay(ray.withT(boundary.t));
        hit.beginDetailEvaluation();
        const point: Vector3Dd = ray.getOrigin().add(ray.getDirection().multiply(boundary.t));
        if (hit.needsPoint()) {
            hit.point = point;
            hit.markDetailAvailable(RayHit.DETAIL_POINT);
        }
        if (hit.needsNormal()) {
            const normal: Vector3Dd = this.surfaceNormal(point, boundary.surfaceIndex);
            const length: number = normal.length();
            if (Double.isFinite(length) && length > 0) {
                hit.normal = normal.multiply((boundary.normalReversed ? -1.0 : 1.0) / length);
                hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
            }
        }
        return RayFirstHitStatus.Hit;
    }

    public override doIntersectionFirstHit(ray: Ray, hit: RayHit): boolean;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit | null): boolean;
    public override doIntersectionFirstHit(ray: Ray, hit: RayHit | null): boolean {
        return (
            this.queryFirstHit(ray, new RayCrossingQuery(1e-9, Double.POSITIVE_INFINITY), hit) === RayFirstHitStatus.Hit
        );
    }

    public override doExtraInformation(ray: Ray, t: number, hit: RayHit): void;
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit | null): void;
    public override doExtraInformation(ray: Ray, t: number, hit: RayHit | null): void {
        if (hit === null) return;
        hit.beginDetailEvaluation();
        const point: Vector3Dd = ray.getOrigin().add(ray.getDirection().multiply(t));
        if (hit.needsPoint()) {
            hit.point = point;
            hit.markDetailAvailable(RayHit.DETAIL_POINT);
        }
        if (hit.needsNormal()) {
            const normal: Vector3Dd = this.surfaceNormal(point, this.nearestSurface(point));
            const length: number = normal.length();
            if (Double.isFinite(length) && length > 0) {
                hit.normal = normal.multiply(1.0 / length);
                hit.markDetailAvailable(RayHit.DETAIL_NORMAL);
            }
        }
    }

    public override doContainmentTest(point: Vector3Dd, tolerance: number): number {
        // Legacy integer adapter over the typed contract.
        const result: GeometryContainment = this.classifyPoint(point, tolerance);
        if (result === GeometryContainment.Inside) return HeightFieldSolid.INSIDE;
        if (result === GeometryContainment.Boundary) return HeightFieldSolid.LIMIT;
        if (result === GeometryContainment.Outside) return HeightFieldSolid.OUTSIDE;
        throw new IllegalStateException("Height field containment cannot be represented by the legacy integer result");
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
