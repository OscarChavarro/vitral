import { describe, expect, it } from "vitest";

import { Matrix4x4d } from "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.js";
import { Vector3Dd } from "vsdk/toolkit/common/linealAlgebra/Vector3Dd.js";
import { Box } from "vsdk/toolkit/environment/geometry/volume/Box.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { SimpleTestGeometryLibrary } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/fixtures/SimpleTestGeometryLibrary.js";
import { _PolyhedralBoundedSolidSetVertexFaceClassifier } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetVertexFaceClassifier.js";
import { PolyhedralBoundedSolidTestFixtures } from "../../volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTestFixtures.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";

/**
Acceptance tests for the V/F classifier after removal of the "borrowed
wMANT2008" dead branch ([MANT1988] Ch. 15.6.1, problem 15.4).

<p>Each scenario forces the boolean pipeline to exercise the coplanar
vertex/face path — i.e., a vertex of one operand lies exactly on a face
of the other, or two faces overlap on a shared plane — and verifies the
resulting B-rep is valid and has the expected topology. The classifier
under test is
`_PolyhedralBoundedSolidSetVertexFaceClassifier`, whose
`vertexFaceReclassifyOnEdges`/`vertexFaceInsertNullEdges`
families collapsed to a single "no-peek" implementation in §5.1 of
plan-csg-boolean-fix-stage2.</p>

<p>Mapping to Mäntylä's figures:
<ul>
<li>Figure 15.9 — face-touching coplanar pair → covered by
`touchingBoxesUnion/Intersection`.</li>
<li>Figure 15.10 — vertex of A on edge of B's coplanar face → covered by
`halfOffsetTouchingBoxesUnion`.</li>
<li>Figure 15.11 — partial coplanar face overlap → covered by
`partiallyOverlappingFacesDifference`.</li>
<li>Figure 15.12 — coplanar vertex with sectors crossing the boundary →
covered by `lShapedExternalContactUnion`.</li>
</ul>
</p>

<p>Runtime boundary: Java resolves `vertexFaceClassify` reflectively with
`setAccessible(true)`; TypeScript erases `private` at run time, so the
method is looked up on the prototype instead.</p>
 */
describe("VertexFaceClassifierCoplanarTest", () => {
    function createBox(sx: number, sy: number, sz: number, tx: number, ty: number, tz: number): PolyhedralBoundedSolid {
        const box = new Box(new Vector3Dd(sx, sy, sz));
        const solid = box.exportToPolyhedralBoundedSolid();
        let translation = new Matrix4x4d();
        translation = translation.translation(tx, ty, tz);
        PolyhedralBoundedSolidModeler.applyTransformation(solid, translation);
        return solid;
    }

    it("given_classifier_when_inspectingApi_then_borrowedBranchIsRemoved", () => {
        // Regression guard: the §5.1 cleanup must leave no method named with
        // the legacy "Borrowed" or "NoPeekVersion" suffix in the V/F classifier.
        const declared = Object.getOwnPropertyNames(_PolyhedralBoundedSolidSetVertexFaceClassifier.prototype).concat(
            Object.getOwnPropertyNames(_PolyhedralBoundedSolidSetVertexFaceClassifier),
        );
        const survivors: string[] = [];
        for (const name of declared) {
            if (name.endsWith("Borrowed") || name.endsWith("NoPeekVersion")) {
                survivors.push(name);
            }
        }
        expect(survivors, "V/F classifier still carries legacy 'borrowed wMANT2008' methods").toEqual([]);

        // The chosen replacement method must exist with the canonical name.
        const vertexFaceClassifyMethod = (
            _PolyhedralBoundedSolidSetVertexFaceClassifier.prototype as unknown as Record<string, unknown>
        )["vertexFaceClassify"];
        expect(vertexFaceClassifyMethod).not.toBeUndefined();
    });

    it("given_touchingBoxes_when_runningUnion_then_resultIsValidTwoShellPair", () => {
        // Two unit cubes sharing only a single face produce a touching-only
        // result: the pipeline correctly preserves both shells (Euler=4)
        // because the V/F classifier classifies the shared face as boundary
        // rather than interior.
        const pair = PolyhedralBoundedSolidTestFixtures.createTouchingBoxPair();

        const result = PolyhedralBoundedSolidModeler.setOp(
            pair[0]!,
            pair[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
            true,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(
            result.getVerticesList().size() - result.getEdgesList().size() + result.getPolygonsList().size(),
            "Euler characteristic for two-shell union",
        ).toBe(4);
        const minmax = result.getMinMax();
        expect(minmax[0]).toBe(-0.5);
        expect(minmax[3]).toBe(1.5);
    });

    it("given_halfOffsetTouchingBoxes_when_runningUnion_then_brepIsValid", () => {
        // Two unit cubes sharing only part of their x=0.5 face (B is shifted
        // half a unit in z), so several vertices of B fall on the interior of
        // A's face — classic figure-15.10 vertex-on-edge case.
        const solidA = createBox(1.0, 1.0, 1.0, 0.0, 0.0, 0.0);
        const solidB = createBox(1.0, 1.0, 1.0, 1.0, 0.0, 0.5);

        const result = PolyhedralBoundedSolidModeler.setOp(solidA, solidB, PolyhedralBoundedSolidModeler.UNION, false);

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        // L-shaped extrusion: 12 vertices, 18 edges, 8 faces (V-E+F = 2).
        expect(
            result.getVerticesList().size() - result.getEdgesList().size() + result.getPolygonsList().size(),
            "Euler characteristic for L-shaped union",
        ).toBe(2);
    });

    it("given_partiallyOverlappingFaces_when_runningIntersection_then_contactSliceProduced", () => {
        // Stacked half-overlap pair: shared face is a sub-rectangle of A's
        // top face and B's bottom face → figure 15.11 partial coplanar overlap.
        const pair = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);

        const result = PolyhedralBoundedSolidModeler.setOp(
            pair[0]!,
            pair[1]!,
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        // Contact lamina: 2 coincident polygons (top and bottom of zero-thickness slab)
        expect(result.getPolygonsList().size()).toBe(2);
        expect(result.getVerticesList().size()).toBe(4);
    });

    it("given_lShapedExternalContact_when_runningUnion_then_brepIsValid", () => {
        // L-shaped pair from MANT1988 §15.1 — its UNION exercises the
        // coplanar V/F path at the inner-corner vertex (figure 15.12 analog).
        const pair = SimpleTestGeometryLibrary.createTestObjectPairMANT1988_15_1();

        const result = PolyhedralBoundedSolidModeler.setOp(
            pair[0]!,
            pair[1]!,
            PolyhedralBoundedSolidModeler.UNION,
            false,
        );

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(result)).toBe(true);
        expect(
            result.getVerticesList().size() - result.getEdgesList().size() + result.getPolygonsList().size(),
            "Euler characteristic for MANT1988 §15.1 union",
        ).toBe(2);
    });
});
