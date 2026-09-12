import { VSDK } from "../../../../../common/VSDK.js";
import { Vector3Dd } from "../../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolidGeometricValidator } from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import type { _PolyhedralBoundedSolidFace } from "../../../volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";

/** Degeneracy checks used by the CSG finishing pipeline. */
export class _PolyhedralBoundedSolidFaceValidator {
    private constructor() {}
    public static isSurfaceDegenerate(face: _PolyhedralBoundedSolidFace): boolean {
        const context = PolyhedralBoundedSolidNumericPolicy.forFace(face),
            points = PolyhedralBoundedSolidGeometricValidator.extractPointsFromFace(face);
        if (points === null || !PolyhedralBoundedSolidGeometricValidator.validateFacePointsAreCoplanar(points, context))
            return true;
        return (
            this.faceArea(face) <= context.bigEpsilon() * context.bigEpsilon() ||
            this.hasCloseNonAdjacentEdges(face, context)
        );
    }
    public static faceArea(face: _PolyhedralBoundedSolidFace): number {
        let area = 0;
        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i)!;
            const start = loop.boundaryStartHalfEdge;
            if (start === null) continue;
            let edge = start,
                vectorArea = new Vector3Dd();
            do {
                const next = edge.next();
                if (next === null) break;
                vectorArea = vectorArea.add(edge.startingVertex.position.crossProduct(next.startingVertex.position));
                edge = next;
            } while (edge !== start);
            area += 0.5 * vectorArea.length();
        }
        return area;
    }
    public static hasCloseNonAdjacentEdges(face: _PolyhedralBoundedSolidFace, context: ToleranceContext): boolean {
        const segments = this.collectFaceSegments(face),
            tolerance = Math.max(context.bigEpsilon() * 10, context.modelScale() * 1e-5);
        for (let i = 0; i < segments.length; i++)
            for (let j = i + 1; j < segments.length; j++) {
                if (segments[i]!.sharesEndpointWith(segments[j]!, context.bigEpsilon())) continue;
                if (
                    this.segmentDistance(segments[i]!.start, segments[i]!.end, segments[j]!.start, segments[j]!.end) <=
                    tolerance
                )
                    return true;
            }
        return false;
    }
    private static collectFaceSegments(face: _PolyhedralBoundedSolidFace): FaceSegment[] {
        const segments: FaceSegment[] = [];
        for (let i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i)!;
            const start = loop.boundaryStartHalfEdge;
            if (start === null) continue;
            let edge = start;
            do {
                const next = edge.next();
                if (next === null) break;
                segments.push(new FaceSegment(edge.startingVertex.position, next.startingVertex.position));
                edge = next;
            } while (edge !== start);
        }
        return segments;
    }
    private static segmentDistance(p1: Vector3Dd, q1: Vector3Dd, p2: Vector3Dd, q2: Vector3Dd): number {
        const d1 = q1.subtract(p1),
            d2 = q2.subtract(p2),
            r = p1.subtract(p2),
            a = d1.dotProduct(d1),
            e = d2.dotProduct(d2),
            f = d2.dotProduct(r);
        let s: number, t: number;
        if (a <= VSDK.EPSILON && e <= VSDK.EPSILON) return p1.subtract(p2).length();
        if (a <= VSDK.EPSILON) {
            s = 0;
            t = this.clamp(f / e, 0, 1);
        } else {
            const c = d1.dotProduct(r);
            if (e <= VSDK.EPSILON) {
                t = 0;
                s = this.clamp(-c / a, 0, 1);
            } else {
                const b = d1.dotProduct(d2),
                    denominator = a * e - b * b;
                s = denominator !== 0 ? this.clamp((b * f - c * e) / denominator, 0, 1) : 0;
                t = (b * s + f) / e;
                if (t < 0) {
                    t = 0;
                    s = this.clamp(-c / a, 0, 1);
                } else if (t > 1) {
                    t = 1;
                    s = this.clamp((b - c) / a, 0, 1);
                }
            }
        }
        return p1
            .add(d1.multiply(s))
            .subtract(p2.add(d2.multiply(t)))
            .length();
    }
    private static clamp(value: number, min: number, max: number): number {
        return value < min ? min : value > max ? max : value;
    }
}
class FaceSegment {
    public constructor(
        public readonly start: Vector3Dd,
        public readonly end: Vector3Dd,
    ) {}
    public sharesEndpointWith(other: FaceSegment, tolerance: number): boolean {
        return (
            this.start.subtract(other.start).length() <= tolerance ||
            this.start.subtract(other.end).length() <= tolerance ||
            this.end.subtract(other.start).length() <= tolerance ||
            this.end.subtract(other.end).length() <= tolerance
        );
    }
}
