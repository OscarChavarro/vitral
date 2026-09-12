import { Double } from "../../../../../../../java/lang/Double.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { _PolyhedralBoundedSolidOperator } from "../_PolyhedralBoundedSolidOperator.js";

/** Shared coordinate/profile primitives for structural boolean fallbacks. */
export class _PolyhedralBoundedSolidFallbackGeometry extends _PolyhedralBoundedSolidOperator {
    public static coordinate(point: Vector3Dd, axis: number): number {
        return axis === 0 ? point.x() : axis === 1 ? point.y() : point.z();
    }
    public static sameCoordinate(a: number, b: number): boolean {
        return Math.abs(a - b) <= this.numericContext.bigEpsilon();
    }
    public static boundsMatch(a: readonly number[] | null, b: readonly number[] | null): boolean {
        return (
            a !== null &&
            b !== null &&
            a.length >= 6 &&
            b.length >= 6 &&
            a.slice(0, 6).every((value, i) => this.sameCoordinate(value, b[i]!))
        );
    }
    public static addUniqueCoordinate(values: number[], value: number): void {
        if (values.some((existing) => this.sameCoordinate(existing, value))) return;
        values.push(value);
        // Java `Collections.sort` on `ArrayList<Double>` uses `Double.compareTo`.
        values.sort((a, b) => Double.compare(a, b));
    }
    public static uniqueVertexCoordinates(solid: PolyhedralBoundedSolid, axis: number): number[] {
        const values: number[] = [];
        for (let i = 0; i < solid.getVerticesList().size(); i++)
            this.addUniqueCoordinate(values, this.coordinate(solid.getVerticesList().get(i)!.position, axis));
        return values;
    }
    public static signedAreaOnYZ(profile: readonly Vector3Dd[]): number {
        let area = 0;
        for (let i = 0; i < profile.length; i++) {
            const a = profile[i]!,
                b = profile[(i + 1) % profile.length]!;
            area += a.y() * b.z() - b.y() * a.z();
        }
        return area * 0.5;
    }
    public static extractProfileAtX(solid: PolyhedralBoundedSolid, x: number): Vector3Dd[] | null {
        let best: Vector3Dd[] | null = null,
            bestArea = 0;
        for (let faceIndex = 0; faceIndex < solid.getPolygonsList().size(); faceIndex++) {
            const face = solid.getPolygonsList().get(faceIndex)!;
            for (let loopIndex = 0; loopIndex < face.boundariesList.size(); loopIndex++) {
                const loop = face.boundariesList.get(loopIndex)!;
                if (loop.halfEdgesList.size() < 3) continue;
                const profile: Vector3Dd[] = [];
                let onPlane = true;
                for (let i = 0; i < loop.halfEdgesList.size(); i++) {
                    const point = loop.halfEdgesList.get(i)!.startingVertex.position;
                    if (!this.sameCoordinate(point.x(), x)) {
                        onPlane = false;
                        break;
                    }
                    profile.push(new Vector3Dd(point));
                }
                const area = Math.abs(this.signedAreaOnYZ(profile));
                if (onPlane && area > bestArea) {
                    bestArea = area;
                    best = profile;
                }
            }
        }
        return best;
    }
    public static sameProfilePoint(a: Vector3Dd, b: Vector3Dd): boolean {
        return (
            this.sameCoordinate(a.x(), b.x()) && this.sameCoordinate(a.y(), b.y()) && this.sameCoordinate(a.z(), b.z())
        );
    }
    public static appendProfilePoint(profile: Vector3Dd[], point: Vector3Dd): void {
        if (profile.length > 0 && this.sameProfilePoint(profile[profile.length - 1]!, point)) return;
        profile.push(point);
    }
    public static projectProfilePoint(point: Vector3Dd, x: number, zCut: number): Vector3Dd {
        return new Vector3Dd(x, point.y(), this.sameCoordinate(point.z(), zCut) ? zCut : point.z());
    }
    public static intersectProfileSegmentAtZ(a: Vector3Dd, b: Vector3Dd, x: number, zCut: number): Vector3Dd {
        if (this.sameCoordinate(a.z(), b.z())) return new Vector3Dd(x, a.y(), zCut);
        const t = (zCut - a.z()) / (b.z() - a.z());
        return new Vector3Dd(x, a.y() + (b.y() - a.y()) * t, zCut);
    }
    public static clipProfileAboveZ(profile: readonly Vector3Dd[] | null, x: number, zCut: number): Vector3Dd[] {
        if (profile === null || profile.length < 3) return [];
        const clipped: Vector3Dd[] = [];
        let previous = profile[profile.length - 1]!,
            previousInside = previous.z() + this.numericContext.bigEpsilon() >= zCut;
        for (const current of profile) {
            const currentInside = current.z() + this.numericContext.bigEpsilon() >= zCut;
            if (currentInside) {
                if (!previousInside)
                    this.appendProfilePoint(clipped, this.intersectProfileSegmentAtZ(previous, current, x, zCut));
                this.appendProfilePoint(clipped, this.projectProfilePoint(current, x, zCut));
            } else if (previousInside)
                this.appendProfilePoint(clipped, this.intersectProfileSegmentAtZ(previous, current, x, zCut));
            previous = current;
            previousInside = currentInside;
        }
        if (clipped.length > 1 && this.sameProfilePoint(clipped[0]!, clipped[clipped.length - 1]!)) clipped.pop();
        return clipped;
    }
}
