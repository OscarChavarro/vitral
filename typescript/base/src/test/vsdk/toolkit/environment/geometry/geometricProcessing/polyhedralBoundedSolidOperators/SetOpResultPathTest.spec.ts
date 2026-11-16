import { describe, expect, it } from "vitest";

import { PolyhedralBoundedSolidModeler } from "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.js";
import { CsgSampleCorpus } from "./CsgSampleCorpus.js";
import { CsgSampleCorpusFixtures } from "./CsgSampleCorpusFixtures.js";
import { OrientedBox, PolyhedralBoundedSolidVolumeOracle } from "./PolyhedralBoundedSolidVolumeOracle.js";

const translation = PolyhedralBoundedSolidVolumeOracle.translation;

/**
The result path names which code produced a boolean result: the
[MANT1988] Ch. 15 pipeline ("normal-pipeline") or one of the preflights in
front of it (contact, identity, containment). Tests can then tell a
pipeline answer from a replacement (Fable5.2Analysis.md Stage A step 4).
 */
describe("SetOpResultPathTest", () => {
    it("given_boxesInGeneralPosition_when_setOp_then_pathIsNormalPipeline", () => {
        const a = new OrientedBox(1.4, 1.2, 1.6, translation(0, 0, 0));
        const b = new OrientedBox(1.1, 1.5, 1.3, translation(0.413, -0.287, 0.351));

        PolyhedralBoundedSolidModeler.setOp(a.toSolid(), b.toSolid(), PolyhedralBoundedSolidModeler.UNION);

        expect(PolyhedralBoundedSolidModeler.getLastSetOpResultPath()).toBe("normal-pipeline");
    });

    it("given_disjointBoxes_when_setOp_then_pathNamesTheNonPipelineAnswer", () => {
        const a = new OrientedBox(1, 1, 1, translation(0, 0, 0));
        const b = new OrientedBox(1, 1, 1, translation(5, 0, 0));

        PolyhedralBoundedSolidModeler.setOp(
            a.toSolid(),
            b.toSolid(),
            PolyhedralBoundedSolidModeler.INTERSECTION,
            false,
            true,
            false,
        );

        expect(PolyhedralBoundedSolidModeler.getLastSetOpResultPath()).not.toBeNull();
        expect(PolyhedralBoundedSolidModeler.getLastSetOpResultPath()).not.toBe("normal-pipeline");
    });

    /**
    Two blocks that share part of a face (plan-stage11 Phase E). Until then
    a preflight written for this case answered the intersection and the two
    differences. Now generate and classify run, find no null edge, and the
    "no intersections found" case of [MANT1988] 15.4 answers from the
    operands as they were before generate: the differences are the
    operands, with their own 8 vertices, and the intersection is the
    shared part of the face as a lamina.
    */
    it("given_stackedBlocks_when_setOp_then_generateAndClassifyRunAndFindNoIntersection", () => {
        const ops = [PolyhedralBoundedSolidModeler.INTERSECTION, PolyhedralBoundedSolidModeler.SUBTRACT];
        for (const op of ops) {
            const pair = CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.STACKED_BLOCKS);
            const result = PolyhedralBoundedSolidModeler.setOp(pair[0]!, pair[1]!, op);
            expect(PolyhedralBoundedSolidModeler.getLastSetOpResultPath()).toBe("no-intersection-after-classify");
            if (op === PolyhedralBoundedSolidModeler.SUBTRACT) {
                expect(result.getVerticesList().size()).toBe(8);
                expect(PolyhedralBoundedSolidVolumeOracle.solidVolume(result)).toBe(
                    PolyhedralBoundedSolidVolumeOracle.solidVolume(pair[0]!),
                );
            } else {
                expect(result.getPolygonsList().size()).toBe(2);
                expect(PolyhedralBoundedSolidVolumeOracle.solidVolume(result)).toBe(0);
            }
        }
    });
});
