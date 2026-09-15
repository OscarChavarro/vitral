import { Double } from "../../../../../java/lang/Double.js";
import { IllegalStateException } from "../../../../../java/lang/IllegalStateException.js";
import { StringBuilder } from "../../../../../java/lang/StringBuilder.js";
import { Vector3Dd } from "../../../common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidGeometricValidator } from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.js";
import {
    PolyhedralBoundedSolidNumericPolicy,
    type ToleranceContext,
} from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.js";
import { PolyhedralBoundedSolidValidationEngine } from "../../../environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidEdge } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidFace } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "../../../environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";

/**
Port of `vsdk.toolkit.io.geometry.stl._StlSolidValidator`, a package-private
class of the Java module: the preconditions `StlWriter` enforces before it
triangulates, in the Java order — no degenerate edge, planar faces, strict
loops, no coincident vertices, triangulable loops, and a solid that passes
`validateIntermediate` — each failing with the Java message. Numbers inside a
message go through the ported `Double.toString`, so the diagnostic text is the
one a JVM prints.
*/
export class _StlSolidValidator {
    private constructor() {}

    public static validate(solid: PolyhedralBoundedSolid): void {
        const numericContext: ToleranceContext = PolyhedralBoundedSolidNumericPolicy.forSolid(solid);

        _StlSolidValidator.validateNoDegenerateEdges(solid, numericContext);

        const msg = new StringBuilder();
        if (!PolyhedralBoundedSolidGeometricValidator.validateAllFacesPlanarityAndPlanes(solid, numericContext, msg)) {
            throw new IllegalStateException(
                "STL export rejected: non-planar face geometry detected:\n" + msg.toString(),
            );
        }

        msg.clear();
        if (!PolyhedralBoundedSolidGeometricValidator.validateLoopsStrict(solid, numericContext, msg)) {
            throw new IllegalStateException(
                "STL export rejected: invalid face loop geometry detected:\n" + msg.toString(),
            );
        }

        msg.clear();
        if (!PolyhedralBoundedSolidGeometricValidator.validateNoCoincidentVertices(solid, numericContext, msg)) {
            throw new IllegalStateException("STL export rejected: degenerate vertices detected:\n" + msg.toString());
        }

        _StlSolidValidator.validateFacesHaveTriangulableLoops(solid, numericContext);

        if (!PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)) {
            throw new IllegalStateException(
                "STL export rejected: solid failed validateIntermediate; " +
                    "export requires a manifold planar intermediate solid",
            );
        }
    }

    private static validateNoDegenerateEdges(solid: PolyhedralBoundedSolid, numericContext: ToleranceContext): void {
        let i: number;
        for (i = 0; i < solid.getEdgesList().size(); i++) {
            const edge: _PolyhedralBoundedSolidEdge | null = solid.getEdgesList().get(i);
            if (edge === null || edge.rightHalf === null || edge.leftHalf === null) {
                continue;
            }
            const rightHe: _PolyhedralBoundedSolidHalfEdge = edge.rightHalf;
            const leftHe: _PolyhedralBoundedSolidHalfEdge = edge.leftHalf;
            const length: number = Vector3Dd.distance(rightHe.startingVertex.position, leftHe.startingVertex.position);
            if (length <= numericContext.bigEpsilon()) {
                throw new IllegalStateException(
                    "STL export rejected: edge " +
                        edge.id +
                        " is degenerate between vertices " +
                        rightHe.startingVertex.id +
                        " and " +
                        leftHe.startingVertex.id +
                        " (length " +
                        Double.toString(length) +
                        ")",
                );
            }
        }
    }

    private static validateFacesHaveTriangulableLoops(
        solid: PolyhedralBoundedSolid,
        numericContext: ToleranceContext,
    ): void {
        let i: number;
        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face: _PolyhedralBoundedSolidFace = solid.getPolygonsList().get(i)!;
            _StlSolidValidator.validateFaceLoops(face, numericContext);
        }
    }

    private static validateFaceLoops(face: _PolyhedralBoundedSolidFace, _numericContext: ToleranceContext): void {
        if (face.getContainingPlane() === null) {
            throw new IllegalStateException("STL export rejected: face " + face.id + " has no containing plane");
        }
        let i: number;
        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop: _PolyhedralBoundedSolidLoop | null = face.boundariesList.get(i);
            if (loop === null || loop.boundaryStartHalfEdge === null) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i + " has no traversable boundary",
                );
            }
            const points: Vector3Dd[] = _StlSolidValidator.collectLoopPoints(face, loop, i);
            if (points.length < 3) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + i + " has fewer than 3 vertices",
                );
            }
            const areaTolerance: number = PolyhedralBoundedSolidNumericPolicy.areaTolerance2D(
                PolyhedralBoundedSolidNumericPolicy.forFace(face),
            );
            const projectedArea: number = _StlSolidValidator.projectedLoopAreaMagnitude(points, face);
            if (projectedArea <= areaTolerance) {
                throw new IllegalStateException(
                    "STL export rejected: face " +
                        face.id +
                        " loop " +
                        i +
                        " has near-zero area" +
                        _StlSolidValidator.buildLoopDiagnostics(face, loop, i, projectedArea, areaTolerance),
                );
            }
        }
    }

    private static collectLoopPoints(
        face: _PolyhedralBoundedSolidFace,
        loop: _PolyhedralBoundedSolidLoop,
        loopIndex: number,
    ): Vector3Dd[] {
        const points: Vector3Dd[] = [];
        const start: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
        let current: _PolyhedralBoundedSolidHalfEdge | null = start;
        do {
            if (current === null || current.startingVertex === null || current.startingVertex.position === null) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + loopIndex + " is not traversable",
                );
            }
            points.push(current.startingVertex.position);
            current = current.next();
            if (current === null) {
                throw new IllegalStateException(
                    "STL export rejected: face " + face.id + " loop " + loopIndex + " is not closed",
                );
            }
        } while (current !== start);
        return points;
    }

    private static projectedLoopAreaMagnitude(points: Vector3Dd[], face: _PolyhedralBoundedSolidFace): number {
        const anchor: Vector3Dd = points[0]!;
        const normal: Vector3Dd = face.getContainingPlane()!.getNormal().normalized();
        const axis: Vector3Dd = _StlSolidValidator.chooseReferenceAxis(normal);
        const u: Vector3Dd = axis.crossProduct(normal).normalized();
        const v: Vector3Dd = normal.crossProduct(u).normalized();

        let areaTwice = 0.0;
        let i: number;
        for (i = 0; i < points.length; i++) {
            const current: Vector3Dd = points[i]!.subtract(anchor);
            const next: Vector3Dd = points[(i + 1) % points.length]!.subtract(anchor);
            const x0: number = current.dotProduct(u);
            const y0: number = current.dotProduct(v);
            const x1: number = next.dotProduct(u);
            const y1: number = next.dotProduct(v);
            areaTwice += x0 * y1 - x1 * y0;
        }
        return Math.abs(areaTwice * 0.5);
    }

    private static chooseReferenceAxis(normal: Vector3Dd): Vector3Dd {
        const ax: number = Math.abs(normal.x());
        const ay: number = Math.abs(normal.y());
        const az: number = Math.abs(normal.z());
        if (ax <= ay && ax <= az) {
            return new Vector3Dd(1.0, 0.0, 0.0);
        }
        if (ay <= az) {
            return new Vector3Dd(0.0, 1.0, 0.0);
        }
        return new Vector3Dd(0.0, 0.0, 1.0);
    }

    private static buildLoopDiagnostics(
        face: _PolyhedralBoundedSolidFace,
        loop: _PolyhedralBoundedSolidLoop,
        loopIndex: number,
        projectedArea: number,
        areaTolerance: number,
    ): string {
        let msg = "";
        msg += "\n  projectedArea=" + Double.toString(projectedArea);
        msg += " areaTolerance=" + Double.toString(areaTolerance);
        msg += " faceScale=" + Double.toString(PolyhedralBoundedSolidNumericPolicy.forFace(face).modelScale());
        msg +=
            " solidScale=" +
            Double.toString(PolyhedralBoundedSolidNumericPolicy.forSolid(face.parentSolid).modelScale());
        if (face.getContainingPlane() !== null) {
            const normal: Vector3Dd = face.getContainingPlane()!.getNormal();
            msg +=
                " normal=(" +
                Double.toString(normal.x()) +
                ", " +
                Double.toString(normal.y()) +
                ", " +
                Double.toString(normal.z()) +
                ")";
        }
        msg += "\n  loop " + loopIndex + " vertices:";

        const start: _PolyhedralBoundedSolidHalfEdge | null = loop.boundaryStartHalfEdge;
        let current: _PolyhedralBoundedSolidHalfEdge | null = start;
        do {
            msg +=
                "\n    v" +
                current!.startingVertex.id +
                "=(" +
                Double.toString(current!.startingVertex.position.x()) +
                ", " +
                Double.toString(current!.startingVertex.position.y()) +
                ", " +
                Double.toString(current!.startingVertex.position.z()) +
                ")";
            current = current!.next();
        } while (current !== null && current !== start);
        return msg;
    }
}
