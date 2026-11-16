import { describe, expect, it } from "vitest";

import { StringBuilder } from "java/lang/StringBuilder.js";
import { PolyhedralBoundedSolidStructuralCopy } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.js";
import { PolyhedralBoundedSolidTopologySummary } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.js";
import { PolyhedralBoundedSolidValidationEngine } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.js";
import type { _PolyhedralBoundedSolidVertex } from "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

/**
Structural copy ([MANT1988] Sec. 15.8, "scopy"): the copy has the same
topology, geometry and identifiers as the source, passes the same
validation, and shares no node with it, so changing one leaves the other
intact.
 */
describe("PolyhedralBoundedSolidStructuralCopyTest", () => {
    function corpusSolids(): [string, CsgSampleCorpus, number][] {
        const rows: [string, CsgSampleCorpus, number][] = [];
        for (const sample of CsgSampleCorpus.values()) {
            rows.push([sample + "/A", sample, 0]);
            rows.push([sample + "/B", sample, 1]);
        }
        return rows;
    }

    it.each(corpusSolids())(
        "given_corpusSolid_when_copied_then_copyIsEqualAndIndependent: %s",
        (_name: string, sample: CsgSampleCorpus, which: number) => {
            const source = CsgSampleCorpusFixtures.createPair(sample)[which]!;
            const sourceStrict = PolyhedralBoundedSolidValidationEngine.validateStrict(source, new StringBuilder());

            const copy = PolyhedralBoundedSolidStructuralCopy.copy(source);

            expect(PolyhedralBoundedSolidTopologySummary.from(copy).toString()).toBe(
                PolyhedralBoundedSolidTopologySummary.from(source).toString(),
            );
            expect(PolyhedralBoundedSolidVolumeOracle.solidVolume(copy)).toBe(
                PolyhedralBoundedSolidVolumeOracle.solidVolume(source),
            );
            expect(PolyhedralBoundedSolidValidationEngine.validateStrict(copy, new StringBuilder())).toBe(sourceStrict);
            expect(copy.getMaxVertexId()).toBe(source.getMaxVertexId());
            expect(copy.getMaxFaceId()).toBe(source.getMaxFaceId());
            let i: number;
            for (i = 0; i < source.getVerticesList().size(); i++) {
                expect(copy.getVerticesList().get(i)).not.toBe(source.getVerticesList().get(i));
                expect(copy.getVerticesList().get(i)!.position).not.toBe(source.getVerticesList().get(i)!.position);
                expect(copy.getVerticesList().get(i)!.id).toBe(source.getVerticesList().get(i)!.id);
            }
            for (i = 0; i < source.getPolygonsList().size(); i++) {
                expect(copy.getPolygonsList().get(i)!.parentSolid).toBe(copy);
            }
        },
    );

    it("given_copy_when_sourceIsChanged_then_copyIsUntouched", () => {
        const source = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.HOLLOW_BRICK)[0]!;
        const copy = PolyhedralBoundedSolidStructuralCopy.copy(source);
        const volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(copy);
        const faces = copy.getPolygonsList().size();
        let i: number;

        for (i = 0; i < source.getVerticesList().size(); i++) {
            source.getVerticesList().get(i)!.position = source.getVerticesList().get(i)!.position.multiply(3.0);
        }
        // Java assigns null to the (non-null typed) field; the cast keeps the same corruption.
        source.getPolygonsList().get(0)!.boundariesList.get(0)!.halfEdgesList.get(0)!.startingVertex =
            null as unknown as _PolyhedralBoundedSolidVertex;

        expect(PolyhedralBoundedSolidVolumeOracle.solidVolume(copy)).toBe(volume);
        expect(copy.getPolygonsList().size()).toBe(faces);
        expect(PolyhedralBoundedSolidValidationEngine.validateStrict(copy, new StringBuilder())).toBe(true);
    });

    it("given_null_when_copied_then_null", () => {
        expect(PolyhedralBoundedSolidStructuralCopy.copy(null)).toBeNull();
    });
});
