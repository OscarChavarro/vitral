import { Vector3Dd } from "../../../../common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "./PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidNumericPolicy, ToleranceContext } from "./PolyhedralBoundedSolidNumericPolicy.js";
import { _GeometricStrictFaceIntersectionsStrategy } from "./_GeometricStrictFaceIntersectionsStrategy.js";
import { _GeometricStrictLoopsStrategy } from "./_GeometricStrictLoopsStrategy.js";
import { _PolyhedralBoundedSolidFace } from "./nodes/_PolyhedralBoundedSolidFace.js";
/** Geometric validation helpers centered on the planar-face model of [MANT1988].10.2.1 and chapter 13 primitives. */
export class PolyhedralBoundedSolidGeometricValidator {
    public static validateFacePointsAreCoplanar(
        points: readonly Vector3Dd[],
        context = PolyhedralBoundedSolidNumericPolicy.forPoints(points),
    ): boolean {
        if (points.length < 3) return false;
        let plane: null | { p: Vector3Dd; n: Vector3Dd } = null;
        for (let i = 0; i < points.length && !plane; i++)
            for (let j = i + 1; j < points.length && !plane; j++)
                for (let k = j + 1; k < points.length; k++) {
                    const n = points[j]!.subtract(points[i]!).crossProduct(points[k]!.subtract(points[i]!));
                    if (n.length() > context.bigEpsilon()) plane = { p: points[i]!, n: n.normalized() };
                }
        return (
            plane !== null &&
            points.every((p) => Math.abs(plane!.n.dotProduct(p.subtract(plane!.p))) <= context.epsilon())
        );
    }
    public static extractPointsFromFace(face: _PolyhedralBoundedSolidFace): Vector3Dd[] | null {
        const points: Vector3Dd[] = [];
        for (const loop of face.boundariesList) {
            if (loop.boundaryStartHalfEdge === null) return null;
            for (let i = 0; i < loop.halfEdgesList.size(); i++)
                points.push(loop.halfEdgesList.get(i)!.startingVertex.position);
        }
        return points;
    }
    public static validateFaceIsPlanar(
        face: _PolyhedralBoundedSolidFace,
        context = PolyhedralBoundedSolidNumericPolicy.defaultContext(),
    ): boolean {
        const points = this.extractPointsFromFace(face);
        return points !== null && this.validateFacePointsAreCoplanar(points, context);
    }
    public static validateAllFacesPlanarityAndPlanes(
        solid: PolyhedralBoundedSolid,
        context: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.defaultContext(),
        message: string[] = [],
    ): boolean {
        let valid = true;
        for (const face of solid.getPolygonsList())
            if (!this.validateFaceIsPlanar(face, context) || face.getContainingPlane() === null) {
                message.push(`Face [${face.id}] is not coplanar or has no containing plane.`);
                valid = false;
            }
        return valid;
    }
    public static validateConsistentFaceOrientations(
        solid: PolyhedralBoundedSolid,
        _context: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.defaultContext(),
        message: string[] = [],
    ): boolean {
        for (const face of solid.getPolygonsList()) {
            const normal = face.getContainingPlane()?.getNormal();
            if (normal === undefined) continue;
            let neighbours = 0,
                opposed = 0;
            for (const loop of face.boundariesList)
                for (let i = 0; i < loop.halfEdgesList.size(); i++) {
                    const mate = loop.halfEdgesList.get(i)!.mirrorHalfEdge(),
                        other = mate?.parentLoop.parentFace;
                    if (other === undefined || other === face) continue;
                    const n = other.getContainingPlane()?.getNormal();
                    if (n === undefined) continue;
                    neighbours++;
                    if (normal.dotProduct(n) < -0.5) opposed++;
                }
            if (neighbours >= 2 && opposed === neighbours) {
                message.push(`Face [${face.id}] is opposed to all neighbours.`);
                return false;
            }
        }
        return true;
    }
    /** Java-compatible strict-loop validation entry point. */
    public static validateLoopsStrict(
        solid: PolyhedralBoundedSolid,
        context: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid),
        message: string[] = [],
    ): boolean {
        return new _GeometricStrictLoopsStrategy().validate(solid, context, message);
    }
    /** Java-compatible strict face-intersection validation entry point. */
    public static validateFaceIntersectionsStrict(
        solid: PolyhedralBoundedSolid,
        context: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid),
        message: string[] = [],
    ): boolean {
        return new _GeometricStrictFaceIntersectionsStrategy().validate(solid, context, message);
    }
    public static validateNoCoincidentVertices(
        solid: PolyhedralBoundedSolid,
        context: ToleranceContext,
        message: string[] = [],
    ): boolean {
        const vertices = solid.getVerticesList();
        for (let i = 0; i < vertices.length; i++)
            for (let j = 0; j < i; j++)
                if (
                    PolyhedralBoundedSolidNumericPolicy.pointsCoincident(
                        vertices[i]!.position,
                        vertices[j]!.position,
                        context,
                    )
                ) {
                    message.push(`Vertices [${vertices[i]!.id}] and [${vertices[j]!.id}] coincide.`);
                    return false;
                }
        return true;
    }
    public static validateUniqueFaceAndVertexIds(solid: PolyhedralBoundedSolid, message: string[] = []): boolean {
        const ids = (items: { id: number }[], kind: string): boolean => {
            const seen = new Set<number>();
            for (const item of items) {
                if (seen.has(item.id)) {
                    message.push(`Duplicate ${kind} id [${item.id}].`);
                    return false;
                }
                seen.add(item.id);
            }
            return true;
        };
        return ids(solid.getVerticesList(), "vertex") && ids(solid.getPolygonsList(), "face");
    }
}
