import { describe, expect, it } from "vitest";

import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidFace } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.js";
import { PolyhedralBoundedSolidTestFixtures } from "./PolyhedralBoundedSolidTestFixtures.js";

/**
Edge cases of the low-level Euler operators that the main operator suite
(PolyhedralBoundedSolidEulerOperatorsTest) does not reach: empty-loop
special cases, inverse round trips that are continued afterwards, and
back-pointer integrity (vertex -> emanating half-edge).

<p>Traceability: [MANT1988] Ch. 11.2.2 (addhe/delhe special cases for the
edge-less half-edge of an empty loop), Programs 11.6-11.8, Problems 11.3
and 11.4, and the lkemr text of Section 11.3.4 ("the special cases that one
or both of the resulting loops are empty"). Cases marked "hidden defect"
were found by the Fable 5.2 kernel analysis
(doc/polyhedralBoundedSolidDevelopment/Fable5.2Analysis.md, defects E-01 to
E-06) and stay disabled until the production fix lands.</p>
 */
describe("PolyhedralBoundedSolidEulerOperatorsEdgeCasesTest", () => {
    function createSkeletalSolid(): PolyhedralBoundedSolid {
        const solid = new PolyhedralBoundedSolid();
        PolyhedralBoundedSolidEulerOperators.mvfs(solid, new Vector3Dd(0.0, 0.0, 0.0), 1, 1);
        return solid;
    }

    /**
    Unit square lamina (faces 1 and 2, vertices 1..4) with a strut from
    vertex 1 to an interior vertex 5 inside face 1.
    */
    function createSquareLaminaWithStrut(): PolyhedralBoundedSolid {
        const solid = createSkeletalSolid();
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(1.0, 0.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 2, 3, new Vector3Dd(1.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 3, 4, new Vector3Dd(0.0, 1.0, 0.0));
        PolyhedralBoundedSolidEulerOperators.lmef(
            solid,
            solid.findFace(1)!.findHalfEdge(4),
            solid.findFace(1)!.findHalfEdge(1),
            2,
        );
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            solid.findFace(1)!.findHalfEdge(1),
            solid.findFace(1)!.findHalfEdge(1),
            5,
            new Vector3Dd(0.25, 0.25, 0.0),
        );
        return solid;
    }

    function loopSizes(face: _PolyhedralBoundedSolidFace): number[] {
        const sizes: number[] = [];
        let i: number;
        for (i = 0; i < face.boundariesList.size(); i++) {
            sizes.push(face.boundariesList.get(i)!.halfEdgesList.size());
        }
        return sizes;
    }

    /**
    Lists every vertex whose emanating half-edge is null, does not start at
    the vertex, or is no longer a member of its parent loop.
    */
    function describeBrokenEmanatingReferences(solid: PolyhedralBoundedSolid): string {
        let report = "";
        let i: number;
        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const vertex = solid.getVerticesList().get(i)!;
            const he = vertex.emanatingHalfEdge;
            if (he === null) {
                report += " v" + vertex.id + ":null";
                continue;
            }
            if (he.startingVertex !== vertex) {
                report += " v" + vertex.id + ":wrongStart";
            }
            const parentLoop = he.parentLoop as typeof he.parentLoop | null;
            if (parentLoop === null || !parentLoop.halfEdgesList.locateWindowAtElem(he)) {
                report += " v" + vertex.id + ":notInLoop";
            }
        }
        return report;
    }

    /**
    Defect (fixed) E-01: mvfs -> lmev (strut) -> lkev returns to the skeletal
    count, but the loop's half-edge list is left with a stale head node
    (list defect L-01). The next lmev on the same vertex then yields a
    two-half-edge loop whose next() chain does not visit both half-edges.
    */
    it("given_strutKilledBackToSkeleton_when_newStrutIsAdded_then_loopTraversalVisitsBothHalfEdges", () => {
        const solid = createSkeletalSolid();
        const loop = solid.findFace(1)!.boundariesList.get(0)!;
        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 2, new Vector3Dd(1.0, 0.0, 0.0));
        const back = loop.halfEdgeVertices(2, 1)!;
        PolyhedralBoundedSolidEulerOperators.lkev(solid, back, back.mirrorHalfEdge());

        PolyhedralBoundedSolidEulerOperators.smev(solid, 1, 1, 3, new Vector3Dd(0.0, 1.0, 0.0));

        expect(loop.halfEdgesList.size()).toBe(2);
        const first = loop.halfEdgesList.get(0)!;
        const second = loop.halfEdgesList.get(1)!;
        expect(first.next()).toBe(second);
        expect(second.next()).toBe(first);
        expect(first.previous()).toBe(second);
        expect(second.previous()).toBe(first);
    });

    /**
    Defect (fixed) E-02: the lkemr documentation (after [MANT1988] Sec.
    11.3.4) says the arguments can be swapped to choose which component
    stays as the outer loop. With the arguments swapped so that the outer
    component collapses to a single vertex, the operator raises the fatal
    "Case B Should not happen!" instead of leaving an empty (edge-less) loop.
    */
    it("given_strutInsideFace_when_lkemrArgumentsAreSwapped_then_outerLoopBecomesLoneVertex", () => {
        const solid = createSquareLaminaWithStrut();
        const face = solid.findFace(1)!;
        const edgeCount = solid.getEdgesList().size();

        expect(() =>
            PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(5, 1), face.findHalfEdge(1, 5)),
        ).not.toThrow();

        expect(solid.getEdgesList().size()).toBe(edgeCount - 1);
        expect(face.boundariesList.size()).toBe(2);
        expect(
            loopSizes(face)
                .slice()
                .sort((a, b) => a - b),
        ).toEqual([1, 4]);
    });

    /**
    Defect (fixed) E-03: lkemr leaves the vertex of a single-vertex ring with
    a null emanating half-edge, and the inverse lmekr does not restore it,
    so lkemr -> lmekr is not an identity on the vertex back-pointers. Any
    later pass that treats "null emanating half-edge" as "orphan vertex"
    (e.g. remakeEmanatingHalfedgesReferences) will delete a live vertex.
    */
    it("given_loneVertexRing_when_lmekrReconnectsIt_then_everyVertexHasALiveEmanatingHalfEdge", () => {
        const solid = createSquareLaminaWithStrut();
        const face = solid.findFace(1)!;
        PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));

        PolyhedralBoundedSolidEulerOperators.lmekr(
            solid,
            face.boundariesList.get(0)!.halfEdgeVertices(1, 2)!,
            face.boundariesList.get(1)!.boundaryStartHalfEdge!,
        );

        expect(face.boundariesList.size()).toBe(1);
        expect(describeBrokenEmanatingReferences(solid)).toBe("");
    });

    /**
    Defect (fixed) E-04: lmef(he, he) is documented (after Program 11.7) as
    creating a "circular" face with one edge. The container-based port
    inserts both new half-edges in the old loop and returns a face whose
    loop is empty.
    */
    it("given_sameHalfEdgeTwice_when_lmef_then_newFaceOwnsASingleSelfLoopEdge", () => {
        const solid = createSquareLaminaWithStrut();
        const face = solid.findFace(1)!;
        const he = face.findHalfEdge(2);
        const oldLoopSize = face.boundariesList.get(0)!.halfEdgesList.size();

        const newFace = PolyhedralBoundedSolidEulerOperators.lmef(solid, he, he, 9);

        expect(newFace).not.toBeNull();
        const newLoop = newFace!.boundariesList.get(0)!;
        expect(newLoop.halfEdgesList.size()).toBe(1);
        expect(newLoop.boundaryStartHalfEdge!.parentLoop).toBe(newLoop);
        expect(face.boundariesList.get(0)!.halfEdgesList.size()).toBe(oldLoopSize + 1);
    });

    /**
    Defect (fixed) E-05: after lkev removes a strut from a box face, the
    surviving vertex keeps an emanating half-edge that was unlisted from
    its loop (dangling back-pointer). validateIntermediate still reports
    the solid as valid, so the validator has a matching gap.
    */
    it("given_boxWithStrut_when_lkevRemovesStrut_then_everyVertexHasALiveEmanatingHalfEdge", () => {
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const face = solid.getPolygonsList().get(0)!;
        const he = face.boundariesList.get(0)!.boundaryStartHalfEdge!;
        const baseVertexId = he.startingVertex.id;
        PolyhedralBoundedSolidEulerOperators.lmev(solid, he, he, 99, new Vector3Dd(0.0, 0.0, -0.5));
        expect(describeBrokenEmanatingReferences(solid)).toBe("");

        PolyhedralBoundedSolidEulerOperators.lkev(
            solid,
            face.findHalfEdge(99, baseVertexId),
            face.findHalfEdge(baseVertexId, 99),
        );

        expect(solid.getVerticesList().size()).toBe(8);
        expect(solid.getEdgesList().size()).toBe(12);
        expect(describeBrokenEmanatingReferences(solid)).toBe("");
    });

    /**
    Defect (fixed) E-06: lmef does not check its precondition
    he1.parentLoop == he2.parentLoop ([MANT1988] Sec. 11.3.3). With
    half-edges from two different faces it silently migrates a whole loop,
    returns a non-null face and leaves the solid invalid. A rejected call
    must leave the solid untouched.
    */
    it("given_halfEdgesFromDifferentLoops_when_lmef_then_operationIsRejectedWithoutSideEffects", () => {
        const solid = PolyhedralBoundedSolidTestFixtures.createBoxSolid(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const he1 = solid.getPolygonsList().get(0)!.boundariesList.get(0)!.boundaryStartHalfEdge;
        const he2 = solid.getPolygonsList().get(1)!.boundariesList.get(0)!.boundaryStartHalfEdge;

        const newFace = PolyhedralBoundedSolidEulerOperators.lmef(solid, he1, he2, 50);

        expect(newFace).toBeNull();
        expect(solid.getPolygonsList().size()).toBe(6);
        expect(solid.getEdgesList().size()).toBe(12);
        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(true);
    });

    /**
    Control case (passes today): the documented default orientation of
    lkemr, where the ring collapses to a single vertex.
    */
    it("given_strutInsideFace_when_lkemr_then_ringIsALoneVertexWithoutEdge", () => {
        const solid = createSquareLaminaWithStrut();
        const face = solid.findFace(1)!;

        PolyhedralBoundedSolidEulerOperators.lkemr(solid, face.findHalfEdge(1, 5), face.findHalfEdge(5, 1));

        expect(face.boundariesList.size()).toBe(2);
        const ring = face.boundariesList.get(1)!;
        expect(ring.halfEdgesList.size()).toBe(1);
        expect(ring.boundaryStartHalfEdge!.parentEdge).toBeNull();
        expect(ring.boundaryStartHalfEdge!.startingVertex.id).toBe(5);
    });
});
