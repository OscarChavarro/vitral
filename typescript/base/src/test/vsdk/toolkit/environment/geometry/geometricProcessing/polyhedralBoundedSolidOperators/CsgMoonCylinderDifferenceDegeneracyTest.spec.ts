import { describe, expect, it } from "vitest";

import { Boolean as JavaBoolean } from "java/lang/Boolean.js";
import { ArrayList } from "java/util/ArrayList.js";
import { HashSet } from "java/util/HashSet.js";
import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Cone } from "vsdk/toolkit/environment/geometry/volume/Cone.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import type { _PolyhedralBoundedSolidLoop } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.js";
import type { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";

/**
Harness accommodation: JUnit has no per-test time budget; each variant runs a
boolean subtraction between tessellated cylinders and then audits every
vertex/edge/face pair (O(n^2) scans).
*/
const MOON_TIMEOUT_MS = 900_000;

describe("CsgMoonCylinderDifferenceDegeneracyTest", () => {
    const RADIUS = 0.15;
    const HEIGHT = 0.5;
    const TOLERANCE = 1.0e-9;
    const MOON_OFFSET = new Vector3Dd(0.11, 0.0, 0.06);

    function samePosition(first: Vector3Dd | null, second: Vector3Dd | null): boolean {
        if (first === null || second === null) {
            return false;
        }
        return first.subtract(second).length() <= TOLERANCE;
    }

    function safeNext(halfEdge: _PolyhedralBoundedSolidHalfEdge | null): _PolyhedralBoundedSolidHalfEdge | null {
        try {
            return halfEdge!.next();
        } catch {
            return null;
        }
    }

    function safePrevious(halfEdge: _PolyhedralBoundedSolidHalfEdge | null): _PolyhedralBoundedSolidHalfEdge | null {
        try {
            return halfEdge!.previous();
        } catch {
            return null;
        }
    }

    function safeMirror(halfEdge: _PolyhedralBoundedSolidHalfEdge | null): _PolyhedralBoundedSolidHalfEdge | null {
        try {
            return halfEdge!.mirrorHalfEdge();
        } catch {
            return null;
        }
    }

    function loopAreaMagnitude(loop: _PolyhedralBoundedSolidLoop | null): number {
        let normalAccumulator = new Vector3Dd();
        let i: number;

        if (loop === null || loop.halfEdgesList === null || loop.halfEdgesList.size() < 3) {
            return 0.0;
        }

        for (i = 0; i < loop.halfEdgesList.size(); i++) {
            const halfEdge = loop.halfEdgesList.get(i);
            const next = safeNext(halfEdge);
            if (
                halfEdge === null ||
                next === null ||
                halfEdge.startingVertex === null ||
                next.startingVertex === null ||
                halfEdge.startingVertex.position === null ||
                next.startingVertex.position === null
            ) {
                return 0.0;
            }

            const p = halfEdge.startingVertex.position;
            const q = next.startingVertex.position;

            normalAccumulator = normalAccumulator.add(
                new Vector3Dd(
                    (p.y() - q.y()) * (p.z() + q.z()),
                    (p.z() - q.z()) * (p.x() + q.x()),
                    (p.x() - q.x()) * (p.y() + q.y()),
                ),
            );
        }
        return normalAccumulator.length();
    }

    function auditHalfEdge(
        face: _PolyhedralBoundedSolidFace,
        loopIndex: number,
        halfEdgeIndex: number,
        loop: _PolyhedralBoundedSolidLoop,
        halfEdge: _PolyhedralBoundedSolidHalfEdge | null,
        failures: string[],
    ): void {
        if (halfEdge === null) {
            failures.push("face " + face.id + " loop " + loopIndex + " has null half-edge at index " + halfEdgeIndex);
            return;
        }
        if (halfEdge.parentLoop !== loop) {
            failures.push(
                "half-edge " + halfEdge.id + " does not point back to face " + face.id + " loop " + loopIndex,
            );
        }
        if (halfEdge.parentEdge === null) {
            failures.push("half-edge " + halfEdge.id + " has null parent edge");
        }
        if (halfEdge.startingVertex === null) {
            failures.push("half-edge " + halfEdge.id + " has null starting vertex");
        }

        const next = safeNext(halfEdge);
        const previous = safePrevious(halfEdge);
        const mirror = safeMirror(halfEdge);

        if (next === null) {
            failures.push("half-edge " + halfEdge.id + " has null next");
        }
        if (previous === null) {
            failures.push("half-edge " + halfEdge.id + " has null previous");
        }
        if (mirror === null) {
            failures.push("half-edge " + halfEdge.id + " has null mirror");
        } else if (safeMirror(mirror) !== halfEdge) {
            failures.push("half-edge " + halfEdge.id + " mirror does not mirror back");
        }

        if (
            next !== null &&
            next.startingVertex !== null &&
            halfEdge.startingVertex !== null &&
            samePosition(halfEdge.startingVertex.position, next.startingVertex.position)
        ) {
            failures.push(
                "half-edge " +
                    halfEdge.id +
                    " is geometrically zero-length from vertex " +
                    halfEdge.startingVertex.id +
                    " to vertex " +
                    next.startingVertex.id,
            );
        }
    }

    function auditLoop(
        face: _PolyhedralBoundedSolidFace,
        loop: _PolyhedralBoundedSolidLoop | null,
        loopIndex: number,
        failures: string[],
    ): void {
        const halfEdges: (_PolyhedralBoundedSolidHalfEdge | null)[] = [];
        const vertexIds = new HashSet<number>();
        let i: number;
        let j: number;

        if (loop === null) {
            failures.push("face " + face.id + " has null loop " + loopIndex);
            return;
        }
        if (loop.parentFace !== face) {
            failures.push("face " + face.id + " loop " + loopIndex + " does not point back to the face");
        }
        if (loop.boundaryStartHalfEdge === null) {
            failures.push("face " + face.id + " loop " + loopIndex + " has null boundary start");
            return;
        }
        if (loop.halfEdgesList === null) {
            failures.push("face " + face.id + " loop " + loopIndex + " has null half-edge list");
            return;
        }
        if (loop.halfEdgesList.size() < 3) {
            failures.push("face " + face.id + " loop " + loopIndex + " has fewer than three half-edges");
        }

        for (i = 0; i < loop.halfEdgesList.size(); i++) {
            const halfEdge = loop.halfEdgesList.get(i);
            halfEdges.push(halfEdge);
            auditHalfEdge(face, loopIndex, i, loop, halfEdge, failures);
            if (halfEdge === null || halfEdge.startingVertex === null) {
                continue;
            }
            if (!vertexIds.add(halfEdge.startingVertex.id)) {
                failures.push(
                    "face " + face.id + " loop " + loopIndex + " repeats vertex " + halfEdge.startingVertex.id,
                );
            }
        }

        for (i = 0; i < halfEdges.length; i++) {
            const first = halfEdges[i]!;
            if (first === null || first.startingVertex === null || first.startingVertex.position === null) {
                continue;
            }
            for (j = i + 1; j < halfEdges.length; j++) {
                const second = halfEdges[j]!;
                if (second === null || second.startingVertex === null || second.startingVertex.position === null) {
                    continue;
                }
                if (samePosition(first.startingVertex.position, second.startingVertex.position)) {
                    failures.push(
                        "face " +
                            face.id +
                            " loop " +
                            loopIndex +
                            " repeats position " +
                            first.startingVertex.position +
                            " at vertices " +
                            first.startingVertex.id +
                            " and " +
                            second.startingVertex.id,
                    );
                }
            }
        }

        if (loopAreaMagnitude(loop) <= TOLERANCE) {
            failures.push("face " + face.id + " loop " + loopIndex + " has zero area");
        }
    }

    function auditRepeatedVerticesAcrossFace(face: _PolyhedralBoundedSolidFace, failures: string[]): void {
        const vertices: (_PolyhedralBoundedSolidVertex | null)[] = [];
        let i: number;
        let j: number;

        for (i = 0; i < face.boundariesList.size(); i++) {
            const loop = face.boundariesList.get(i);
            if (loop === null || loop.halfEdgesList === null) {
                continue;
            }
            for (j = 0; j < loop.halfEdgesList.size(); j++) {
                const halfEdge = loop.halfEdgesList.get(j);
                if (halfEdge !== null) {
                    vertices.push(halfEdge.startingVertex);
                }
            }
        }

        for (i = 0; i < vertices.length; i++) {
            const first = vertices[i]!;
            if (first === null || first.position === null) {
                continue;
            }
            for (j = i + 1; j < vertices.length; j++) {
                const second = vertices[j]!;
                if (second === null || second.position === null) {
                    continue;
                }
                if (first === second) {
                    failures.push("face " + face.id + " references vertex " + first.id + " more than once");
                } else if (samePosition(first.position, second.position)) {
                    failures.push(
                        "face " + face.id + " references coincident vertices " + first.id + " and " + second.id,
                    );
                }
            }
        }
    }

    function auditFacesAndLoops(solid: PolyhedralBoundedSolid, failures: string[]): void {
        const faceIds = new HashSet<number>();
        let i: number;
        let j: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i);

            if (face === null) {
                failures.push("face list contains a null face at index " + i);
                continue;
            }
            if (!faceIds.add(face.id)) {
                failures.push("face id " + face.id + " is repeated");
            }
            if (face.boundariesList === null || face.boundariesList.size() <= 0) {
                failures.push("face " + face.id + " has no boundary loops");
                continue;
            }

            for (j = 0; j < face.boundariesList.size(); j++) {
                auditLoop(face, face.boundariesList.get(j), j, failures);
            }
            auditRepeatedVerticesAcrossFace(face, failures);
        }
    }

    function countEdgeEndpoint(
        edgeValence: Map<_PolyhedralBoundedSolidVertex, number>,
        halfEdge: _PolyhedralBoundedSolidHalfEdge | null,
    ): void {
        if (halfEdge === null || halfEdge.startingVertex === null || !edgeValence.has(halfEdge.startingVertex)) {
            return;
        }
        edgeValence.set(halfEdge.startingVertex, edgeValence.get(halfEdge.startingVertex)! + 1);
    }

    function countLoopOutgoingHalfEdges(
        outgoingHalfEdges: Map<_PolyhedralBoundedSolidVertex, number>,
        loop: _PolyhedralBoundedSolidLoop | null,
    ): void {
        let i: number;

        if (loop === null || loop.halfEdgesList === null) {
            return;
        }
        for (i = 0; i < loop.halfEdgesList.size(); i++) {
            const halfEdge = loop.halfEdgesList.get(i);
            if (
                halfEdge === null ||
                halfEdge.startingVertex === null ||
                !outgoingHalfEdges.has(halfEdge.startingVertex)
            ) {
                continue;
            }
            outgoingHalfEdges.set(halfEdge.startingVertex, outgoingHalfEdges.get(halfEdge.startingVertex)! + 1);
        }
    }

    function auditVertexValence(solid: PolyhedralBoundedSolid, failures: string[]): void {
        const edgeValence = new Map<_PolyhedralBoundedSolidVertex, number>();
        const outgoingHalfEdges = new Map<_PolyhedralBoundedSolidVertex, number>();
        let i: number;
        let j: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex = solid.getVerticesList().get(i);
            if (vertex !== null) {
                edgeValence.set(vertex, 0);
                outgoingHalfEdges.set(vertex, 0);
            }
        }

        for (i = 0; i < solid.getEdgesList().size(); i++) {
            const edge = solid.getEdgesList().get(i);
            if (edge === null) {
                continue;
            }
            countEdgeEndpoint(edgeValence, edge.rightHalf);
            countEdgeEndpoint(edgeValence, edge.leftHalf);
        }

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i);
            if (face === null || face.boundariesList === null) {
                continue;
            }
            for (j = 0; j < face.boundariesList.size(); j++) {
                countLoopOutgoingHalfEdges(outgoingHalfEdges, face.boundariesList.get(j));
            }
        }

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex = solid.getVerticesList().get(i);
            if (vertex === null) {
                continue;
            }
            if (edgeValence.get(vertex)! < 3) {
                failures.push("vertex " + vertex.id + " has edge valence " + edgeValence.get(vertex));
            }
            if (outgoingHalfEdges.get(vertex)! < 3) {
                failures.push(
                    "vertex " + vertex.id + " has only " + outgoingHalfEdges.get(vertex) + " outgoing half-edges",
                );
            }
            if (edgeValence.get(vertex) !== outgoingHalfEdges.get(vertex)) {
                failures.push(
                    "vertex " +
                        vertex.id +
                        " has edge valence " +
                        edgeValence.get(vertex) +
                        " but " +
                        outgoingHalfEdges.get(vertex) +
                        " outgoing half-edges",
                );
            }
        }
    }

    function auditEdges(solid: PolyhedralBoundedSolid, failures: string[]): void {
        const edgeIds = new HashSet<number>();
        let i: number;

        for (i = 0; i < solid.getEdgesList().size(); i++) {
            const edge = solid.getEdgesList().get(i);

            if (edge === null) {
                failures.push("edge list contains a null edge at index " + i);
                continue;
            }
            if (!edgeIds.add(edge.id)) {
                failures.push("edge id " + edge.id + " is repeated");
            }
            if (edge.rightHalf === null) {
                failures.push("edge " + edge.id + " has null right half");
            }
            if (edge.leftHalf === null) {
                failures.push("edge " + edge.id + " has null left half");
            }
            if (edge.rightHalf === null || edge.leftHalf === null) {
                continue;
            }
            if (edge.rightHalf.parentEdge !== edge) {
                failures.push("edge " + edge.id + " right half does not point back to the edge");
            }
            if (edge.leftHalf.parentEdge !== edge) {
                failures.push("edge " + edge.id + " left half does not point back to the edge");
            }
            if (edge.rightHalf.startingVertex === null || edge.leftHalf.startingVertex === null) {
                failures.push("edge " + edge.id + " has a half-edge without starting vertex");
                continue;
            }
            if (samePosition(edge.rightHalf.startingVertex.position, edge.leftHalf.startingVertex.position)) {
                failures.push(
                    "edge " +
                        edge.id +
                        " is geometrically zero-length between vertex " +
                        edge.rightHalf.startingVertex.id +
                        " and vertex " +
                        edge.leftHalf.startingVertex.id,
                );
            }
        }
    }

    function auditVertices(solid: PolyhedralBoundedSolid, failures: string[]): void {
        const vertexIds = new HashSet<number>();
        let i: number;
        let j: number;

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex = solid.getVerticesList().get(i);

            if (vertex === null) {
                failures.push("vertex list contains a null vertex at index " + i);
                continue;
            }
            if (!vertexIds.add(vertex.id)) {
                failures.push("vertex id " + vertex.id + " is repeated");
            }
            if (vertex.position === null) {
                failures.push("vertex " + vertex.id + " has null position");
            }
            if (vertex.emanatingHalfEdge === null) {
                failures.push("vertex " + vertex.id + " has no emanating half-edge");
            } else if (vertex.emanatingHalfEdge.startingVertex === null) {
                failures.push("vertex " + vertex.id + " points to an emanating half-edge with no start vertex");
            } else if (vertex.emanatingHalfEdge.startingVertex !== vertex) {
                failures.push(
                    "vertex " +
                        vertex.id +
                        " points to an emanating half-edge that starts at vertex " +
                        vertex.emanatingHalfEdge.startingVertex.id,
                );
            }
        }

        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const first = solid.getVerticesList().get(i);
            if (first === null || first.position === null) {
                continue;
            }
            for (j = i + 1; j < solid.getVerticesList().size(); j++) {
                const second = solid.getVerticesList().get(j);
                if (second === null || second.position === null) {
                    continue;
                }
                if (samePosition(first.position, second.position)) {
                    failures.push(
                        "vertices " + first.id + " and " + second.id + " share the same position " + first.position,
                    );
                }
            }
        }
    }

    function auditDegenerateTopology(solid: PolyhedralBoundedSolid | null): string[] {
        const failures: string[] = [];

        if (solid === null) {
            failures.push("moon result is null");
            return failures;
        }

        if (solid.getVerticesList().size() <= 0) {
            failures.push("moon result has no vertices");
        }
        if (solid.getEdgesList().size() <= 0) {
            failures.push("moon result has no edges");
        }
        if (solid.getPolygonsList().size() <= 0) {
            failures.push("moon result has no faces");
        }
        if (!PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)) {
            failures.push("validateIntermediate rejected the moon result");
        }
        if (!PolyhedralBoundedSolidValidationEngine.validateStrict(solid)) {
            failures.push("validateStrict rejected the moon result");
        }

        auditVertices(solid, failures);
        auditEdges(solid, failures);
        auditVertexValence(solid, failures);
        auditFacesAndLoops(solid, failures);
        return failures;
    }

    function createCylinder(radialDivisions: number, heightDivisions: number): PolyhedralBoundedSolid {
        const cylinder = new Cone(RADIUS, RADIUS, HEIGHT).exportToPolyhedralBoundedSolid(
            radialDivisions,
            heightDivisions,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(cylinder)).toBe(true);
        return cylinder;
    }

    function createMoon(
        aRadialDivisions: number,
        aHeightDivisions: number,
        bRadialDivisions: number,
        bHeightDivisions: number,
    ): PolyhedralBoundedSolid {
        const a = createCylinder(aRadialDivisions, aHeightDivisions);
        const b = createCylinder(bRadialDivisions, bHeightDivisions);
        let translation = new Matrix4x4d();

        translation = translation.translation(MOON_OFFSET);
        PolyhedralBoundedSolidModeler.applyTransformation(b, translation);
        return PolyhedralBoundedSolidModeler.setOp(
            a,
            b,
            PolyhedralBoundedSolidModeler.SUBTRACT,
            false,
            true,
            JavaBoolean.getBoolean("vsdk.strictValidationBenchmark"),
        );
    }

    function moonCylinderResolutionVariants(): [string, number, number, number, number][] {
        return [
            ["same radial and height: A(16,1) - B(16,1)", 16, 1, 16, 1],
            ["kurlander baseline: A(30,1) - B(30,1)", 30, 1, 30, 1],
            ["same radial and height: A(24,8) - B(24,8)", 24, 8, 24, 8],
            ["same radial and height: A(30,8) - B(30,8)", 30, 8, 30, 8],
            ["same radial, B taller discretization: A(16,1) - B(16,8)", 16, 1, 16, 8],
            ["same radial, A taller discretization: A(16,8) - B(16,1)", 16, 8, 16, 1],
            ["same radial, B taller discretization: A(30,1) - B(30,8)", 30, 1, 30, 8],
            ["same radial, A taller discretization: A(30,8) - B(30,1)", 30, 8, 30, 1],
            ["different radial, same height: A(24,1) - B(16,1)", 24, 1, 16, 1],
            ["different radial, same height: A(16,1) - B(24,1)", 16, 1, 24, 1],
            ["different radial, same height: A(30,1) - B(24,1)", 30, 1, 24, 1],
            ["different radial, same height: A(24,1) - B(30,1)", 24, 1, 30, 1],
            ["different radial, same height: A(36,1) - B(30,1)", 36, 1, 30, 1],
            ["different radial, same height: A(30,1) - B(36,1)", 30, 1, 36, 1],
            ["different radial and height: A(12,4) - B(16,8)", 12, 4, 16, 8],
            ["different radial and height: A(16,8) - B(12,4)", 16, 8, 12, 4],
            ["different radial and height: A(30,8) - B(24,4)", 30, 8, 24, 4],
            ["different radial and height: A(24,4) - B(30,8)", 24, 4, 30, 8],
            ["different radial and height: A(8,2) - B(12,3)", 8, 2, 12, 3],
            ["different radial and height: A(12,3) - B(8,2)", 12, 3, 8, 2],
        ];
    }

    // Java's `ArrayList` import is used by the audit helpers there; the TS
    // audit keeps plain arrays for the local lists, so this reference only
    // documents the parity of the container choice.
    void ArrayList;

    it.each(moonCylinderResolutionVariants())(
        "%s",
        (
            label: string,
            aRadialDivisions: number,
            aHeightDivisions: number,
            bRadialDivisions: number,
            bHeightDivisions: number,
        ) => {
            let moon: PolyhedralBoundedSolid;

            try {
                moon = createMoon(aRadialDivisions, aHeightDivisions, bRadialDivisions, bHeightDivisions);
            } catch (error) {
                // Java distinguishes StackOverflowError from RuntimeException
                // here; this runtime reports both as thrown values, so the
                // single branch fails with the same message shape.
                expect.fail(label + " should build the moon without runtime failures: " + String(error));
                return;
            }

            expect(auditDegenerateTopology(moon), label).toEqual([]);
        },
        MOON_TIMEOUT_MS,
    );
});
