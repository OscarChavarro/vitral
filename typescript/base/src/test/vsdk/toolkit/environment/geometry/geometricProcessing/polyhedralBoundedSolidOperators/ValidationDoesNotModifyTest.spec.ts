import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import type { PolyhedralBoundedSolid } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.js";
import { PolyhedralBoundedSolidEulerOperators } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidEulerOperators.js";
import { PolyhedralBoundedSolidRepair } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidRepair.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidHalfEdge } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.js";
import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";
import { PrismFixtures } from "./PrismFixtures.js";

/**
Validation only reads (plan-stage11 Phase C). Until then every validation
rebuilt the vertex back-pointers and removed the vertices it found without
half-edges, so a validator could turn an invalid solid into a valid one and
report it as valid.

<p>The snapshot compared here is the whole structure: the order of the
face, loop, half-edge, edge and vertex lists, every link between nodes,
every id and every position.</p>
 */
describe("ValidationDoesNotModifyTest", () => {
    const bitsView = new DataView(new ArrayBuffer(8));

    /** Java `Double.doubleToLongBits` (NaN canonicalized), as a decimal string. */
    function doubleToLongBits(value: number): string {
        bitsView.setFloat64(0, Number.isNaN(value) ? Number.NaN : value);
        return bitsView.getBigInt64(0).toString();
    }

    function halfEdge(he: _PolyhedralBoundedSolidHalfEdge | null): string {
        return he === null ? "null" : "h" + he.id;
    }

    /** Every list order, link, id and position of the solid, as text. */
    function snapshot(solid: PolyhedralBoundedSolid): string {
        let text = "";
        let i: number;
        let j: number;
        let k: number;

        for (i = 0; i < solid.getPolygonsList().size(); i++) {
            const face = solid.getPolygonsList().get(i)!;
            text += "F" + face.id + (face.parentSolid === solid ? "" : " foreign");
            for (j = 0; j < face.boundariesList.size(); j++) {
                const loop = face.boundariesList.get(j)!;
                text +=
                    " L[start=" + halfEdge(loop.boundaryStartHalfEdge) + (loop.parentFace === face ? "" : " foreign");
                for (k = 0; k < loop.halfEdgesList.size(); k++) {
                    const he = loop.halfEdgesList.get(k)!;
                    const start = he.startingVertex as typeof he.startingVertex | null;
                    text +=
                        " " +
                        halfEdge(he) +
                        (he.parentLoop === loop ? "" : "!") +
                        "v" +
                        (start === null ? "null" : "" + start.id) +
                        "e" +
                        (he.parentEdge === null ? "null" : "" + he.parentEdge.id);
                }
                text += "]";
            }
            text += "\n";
        }
        for (i = 0; i < solid.getEdgesList().size(); i++) {
            const edge = solid.getEdgesList().get(i)!;
            text += "E" + edge.id + " " + halfEdge(edge.rightHalf) + " " + halfEdge(edge.leftHalf) + "\n";
        }
        for (i = 0; i < solid.getVerticesList().size(); i++) {
            const v = solid.getVerticesList().get(i)!;
            text +=
                "V" +
                v.id +
                " " +
                halfEdge(v.emanatingHalfEdge) +
                " " +
                doubleToLongBits(v.position.x()) +
                " " +
                doubleToLongBits(v.position.y()) +
                " " +
                doubleToLongBits(v.position.z()) +
                "\n";
        }
        text += "maxF=" + solid.getMaxFaceId() + " maxV=" + solid.getMaxVertexId();
        return text;
    }

    function assertUntouched(name: string, solid: PolyhedralBoundedSolid, other: PolyhedralBoundedSolid): void {
        const before = snapshot(solid);
        const otherBefore = snapshot(other);

        PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid);
        expect(snapshot(solid), name + " validateIntermediate").toBe(before);
        PolyhedralBoundedSolidValidationEngine.validateStrict(solid);
        expect(snapshot(solid), name + " validateStrict").toBe(before);
        PolyhedralBoundedSolidValidationEngine.validateStrict(solid, new StringBuilder());
        expect(snapshot(solid), name + " validateStrict with message").toBe(before);
        PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(solid, other, new StringBuilder());
        expect(snapshot(solid), name + " validateBooleanInputs").toBe(before);
        expect(snapshot(other), name + " validateBooleanInputs, other").toBe(otherBefore);
    }

    it("given_validSolids_when_everyValidatorRuns_then_nothingChanges", () => {
        for (const sample of CsgSampleCorpus.values()) {
            const pair = CsgSampleCorpusFixtures.createPair(sample);
            assertUntouched(sample + "/A", pair[0]!, pair[1]!);
            assertUntouched(sample + "/B", pair[1]!, pair[0]!);
        }
    });

    it("given_booleanResult_when_everyValidatorRuns_then_nothingChanges", () => {
        const star = PrismFixtures.prism(PrismFixtures.regularPolygon(5, 1.0), 1.0);
        const prism = PrismFixtures.prism(PrismFixtures.regularPolygon(9, 0.8), 1.2);
        PolyhedralBoundedSolidModeler.applyTransformation(
            prism,
            PolyhedralBoundedSolidVolumeOracle.translation(0.2, 0.1, 0.3).multiply(
                PolyhedralBoundedSolidVolumeOracle.rotation(0.6, 1, 2, 3),
            ),
        );
        const result = PolyhedralBoundedSolidModeler.setOp(star, prism, PolyhedralBoundedSolidModeler.SUBTRACT);
        assertUntouched("prism - prism", result, star);
    });

    /**
    A vertex whose emanating half-edge was lost is reported as invalid and
    left as it is; the explicit repair fixes it and says so.
    */
    it("given_brokenBackPointer_when_validated_then_reportedAndNotRepaired", () => {
        const solid = PrismFixtures.prism(PrismFixtures.regularPolygon(6, 1.0), 1.0);
        const vertex = solid.getVerticesList().get(3)!;
        vertex.emanatingHalfEdge = null;
        const broken = snapshot(solid);

        expect(PolyhedralBoundedSolidValidationEngine.validateIntermediate(solid)).toBe(false);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(solid, new StringBuilder())).toBe(false);
        expect(vertex.emanatingHalfEdge).toBeNull();
        expect(snapshot(solid)).toBe(broken);

        const report = PolyhedralBoundedSolidRepair.remakeEmanatingHalfEdgeReferences(solid);
        expect(report.getRelinkedVertices()).toBe(1);
        expect(report.getRemovedVertices()).toBe(0);
        expect(vertex.emanatingHalfEdge).not.toBeNull();
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(solid, new StringBuilder())).toBe(true);
        expect(PolyhedralBoundedSolidRepair.remakeEmanatingHalfEdgeReferences(solid).isClean()).toBe(true);
    });

    /**
    validateBooleanInputs used to weld the coincident vertices it found.
    Now an operand with an edge of zero length is reported and left alone.
    */
    it("given_coincidentVertices_when_booleanInputsAreValidated_then_reportedAndNotWelded", () => {
        const solid = PrismFixtures.prism(PrismFixtures.regularPolygon(6, 1.0), 1.0);
        const other = PrismFixtures.prism(PrismFixtures.regularPolygon(4, 1.0), 1.0);
        const edge = solid.getEdgesList().get(0)!;
        const he = edge.rightHalf!;
        PolyhedralBoundedSolidEulerOperators.lmev(
            solid,
            he,
            he.mirrorHalfEdge()!.next(),
            solid.getMaxVertexId() + 1,
            he.startingVertex.position,
        );
        const before = snapshot(solid);
        const message = new StringBuilder();

        expect(PolyhedralBoundedSolidValidationEngine.validateBooleanInputs(solid, other, message)).toBe(false);
        expect(message.toString()).toContain("coincident vertices");
        expect(snapshot(solid)).toBe(before);
    });
});
