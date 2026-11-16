import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { Random } from "java/util/Random.js";
import type { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { PrismFixtures } from "./PrismFixtures.js";

/**
Random sequences of Euler operators and their inverses (plan-stage11 Phase
B). On each solid a seeded sequence of edge splits (lmev) and face splits
(lmef) is applied and then undone in reverse order (lkev, lkef).

<p>After every single operator:</p>
<ul>
<li>the solid passes the intermediate validation (planar faces, every edge
    used by exactly two half-edges, closed loops);</li>
<li>the Euler-Poincare characteristic is the one of the start: lmev adds
    one vertex and one edge, lmef one edge and one face;</li>
<li>the volume is the one of the start: the new vertices lie on edges and
    the new edges inside faces.</li>
</ul>

<p>After the whole sequence is undone the counts of faces, edges and
vertices are the initial ones and the solid is strict-valid.</p>

<p>Traceability: [MANT1988] Sec. 9.2 (Euler operators and their inverses),
Programs 11.6 to 11.10.</p>
 */
describe("EulerOperatorSequenceFuzzTest", () => {
    const STEPS = 40;

    function exercise(name: string, solid: PolyhedralBoundedSolid, random: Random): void {
        const before = PolyhedralBoundedSolidTopologySummary.from(solid);
        const volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(solid);
        // Undo stack: the edge each operator made, and whether lmev or
        // lmef made it. (Java ArrayDeque push/pop: a stack.)
        const undo: _PolyhedralBoundedSolidEdge[] = [];
        const undoVertex: number[] = [];
        let step: number;

        for (step = 0; step < STEPS; step++) {
            let label = name + " step " + step;
            if (random.nextBoolean()) {
                undoVertex.push(splitRandomEdge(solid, random));
                undo.push(newestEdge(solid));
                label += " lmev";
            } else {
                const made = splitRandomFace(solid, random);
                if (made === null) {
                    continue;
                }
                undo.push(made);
                undoVertex.push(-1);
                label += " lmef";
            }
            assertInvariants(label, solid, before, volume);
        }
        while (undo.length > 0) {
            const edge = undo.pop()!;
            const vertexId = undoVertex.pop()!;
            let label = name + " undo, " + undo.length + " left";
            if (vertexId >= 0) {
                // lkev removes the starting vertex of its first argument:
                // the vertex lmev made, along the edge lmev made.
                const he = edge.rightHalf!.startingVertex.id === vertexId ? edge.rightHalf! : edge.leftHalf!;
                PolyhedralBoundedSolidEulerOperators.lkev(solid, he, he.mirrorHalfEdge());
                label += " lkev";
            } else {
                PolyhedralBoundedSolidEulerOperators.lkef(solid, edge.rightHalf!, edge.leftHalf!);
                label += " lkef";
            }
            assertInvariants(label, solid, before, volume);
        }
        const after = PolyhedralBoundedSolidTopologySummary.from(solid);
        expect(after.getFaceCount(), name + " faces").toBe(before.getFaceCount());
        expect(after.getEdgeCount(), name + " edges").toBe(before.getEdgeCount());
        expect(after.getVertexCount(), name + " vertices").toBe(before.getVertexCount());
        const message = new StringBuilder();
        const strict = PolyhedralBoundedSolidValidationEngine.validateStrict(solid, message);
        expect(strict, name + " strict: " + message.toString()).toBe(true);
    }

    function assertInvariants(
        label: string,
        solid: PolyhedralBoundedSolid,
        before: PolyhedralBoundedSolidTopologySummary,
        volume: number,
    ): void {
        const now = PolyhedralBoundedSolidTopologySummary.from(solid);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid), label + " valid").toBe(true);
        expect(now.getAdjustedEulerCharacteristic(), label + " Euler-Poincare characteristic").toBe(
            before.getAdjustedEulerCharacteristic(),
        );
        expect(now.getShellCount(), label + " shells").toBe(before.getShellCount());
        expect(
            Math.abs(PolyhedralBoundedSolidVolumeOracle.solidVolume(solid) - volume),
            label + " volume",
        ).toBeLessThanOrEqual(1.0e-9 * Math.max(1.0, Math.abs(volume)));
    }

    /** Splits a random edge at a point inside it; returns the new vertex id. */
    function splitRandomEdge(solid: PolyhedralBoundedSolid, random: Random): number {
        const edge = solid.getEdgesList().get(random.nextInt(solid.getEdgesList().size()))!;
        const he = edge.rightHalf!;
        const a = he.startingVertex.position;
        const b = he.next()!.startingVertex.position;
        const t = 0.3 + 0.4 * random.nextDouble();
        const id = solid.getMaxVertexId() + 1;
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            he,
            he.mirrorHalfEdge()!.next(),
            id,
            a.add(b.subtract(a).multiply(t)),
        );
        return id;
    }

    /**
    Cuts a corner off a random face without rings: a new edge from the
    vertex before a convex corner to the vertex after it. Returns the new
    edge, or null when the face drawn has no corner that can be cut (a
    triangle, a face with rings, or a corner whose cut would cross the
    boundary).
    */
    function splitRandomFace(solid: PolyhedralBoundedSolid, random: Random): _PolyhedralBoundedSolidEdge | null {
        const face = solid.getPolygonsList().get(random.nextInt(solid.getPolygonsList().size()))!;
        if (face.boundariesList.size() !== 1) {
            return null;
        }
        const start = face.boundariesList.get(0)!.boundaryStartHalfEdge!;
        let count = 0;
        let he = start;
        do {
            count++;
            he = he.next()!;
        } while (he !== start);
        if (count < 4) {
            return null;
        }
        const normal = face.getContainingPlane()!.getNormal();
        let skip = random.nextInt(count);
        for (he = start; skip > 0; skip--) {
            he = he.next()!;
        }
        for (let tries = 0; tries < count; tries++, he = he.next()!) {
            if (!isEar(he, normal)) {
                continue;
            }
            const faceCount = solid.getPolygonsList().size();
            const made = PolyhedralBoundedSolidEulerOperators.lmef(
                solid,
                he,
                he.next()!.next(),
                solid.getMaxFaceId() + 1,
            )!;
            expect(solid.getPolygonsList().size()).toBe(faceCount + 1);
            return made.boundariesList.get(0)!.boundaryStartHalfEdge!.parentEdge === null ? null : newestEdge(solid);
        }
        return null;
    }

    /** lmev and lmef append the edge they make to the edge list. */
    function newestEdge(solid: PolyhedralBoundedSolid): _PolyhedralBoundedSolidEdge {
        return solid.getEdgesList().get(solid.getEdgesList().size() - 1)!;
    }

    /**
    True when the triangle (he, he.next, he.next.next) is an ear of the
    face: its corner at he.next is strictly convex and no other vertex of
    the loop lies inside it or on its border.
    */
    function isEar(he: _PolyhedralBoundedSolidHalfEdge, normal: Vector3Dd): boolean {
        const a = he.startingVertex.position;
        const b = he.next()!.startingVertex.position;
        const c = he.next()!.next()!.startingVertex.position;
        const size = b.subtract(a).length() * c.subtract(b).length();
        if (b.subtract(a).crossProduct(c.subtract(b)).dotProduct(normal) <= 1.0e-6 * size) {
            return false;
        }
        let other = he.next()!.next()!.next()!;
        while (other !== he) {
            const p = other.startingVertex.position;
            if (
                side(a, b, p, normal) >= -1.0e-9 * size &&
                side(b, c, p, normal) >= -1.0e-9 * size &&
                side(c, a, p, normal) >= -1.0e-9 * size
            ) {
                return false;
            }
            other = other.next()!;
        }
        return true;
    }

    function side(from: Vector3Dd, to: Vector3Dd, p: Vector3Dd, normal: Vector3Dd): number {
        return to.subtract(from).crossProduct(p.subtract(from)).dotProduct(normal);
    }

    it("given_corpusSolids_when_randomOperatorsAreAppliedAndUndone_then_solidIsRestored", () => {
        const random = new Random(1109);
        for (const sample of CsgSampleCorpus.values()) {
            const pair = CsgSampleCorpusFixtures.createPair(sample);
            exercise(sample + "/A", pair[0]!, random);
            exercise(sample + "/B", pair[1]!, random);
        }
    });

    it("given_starPrism_when_randomOperatorsAreAppliedAndUndone_then_solidIsRestored", () => {
        exercise("STAR7", PrismFixtures.prism(PrismFixtures.starPolygon(7, 1.0, 0.45), 1.0), new Random(5));
    });
});
