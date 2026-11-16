package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
The result path names which code produced a boolean result: the
[MANT1988] Ch. 15 pipeline ("normal-pipeline") or one of the preflights in
front of it (contact, identity, containment). Tests can then tell a
pipeline answer from a replacement (Fable5.2Analysis.md Stage A step 4).
 */
class SetOpResultPathTest
{
    @Test
    void given_boxesInGeneralPosition_when_setOp_then_pathIsNormalPipeline()
    {
        OrientedBox a = new OrientedBox(1.4, 1.2, 1.6, translation(0, 0, 0));
        OrientedBox b = new OrientedBox(1.1, 1.5, 1.3,
            translation(0.413, -0.287, 0.351));

        PolyhedralBoundedSolidModeler.setOp(a.toSolid(), b.toSolid(),
            PolyhedralBoundedSolidModeler.UNION);

        assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
            .isEqualTo("normal-pipeline");
    }

    @Test
    void given_disjointBoxes_when_setOp_then_pathNamesTheNonPipelineAnswer()
    {
        OrientedBox a = new OrientedBox(1, 1, 1, translation(0, 0, 0));
        OrientedBox b = new OrientedBox(1, 1, 1, translation(5, 0, 0));

        PolyhedralBoundedSolidModeler.setOp(a.toSolid(), b.toSolid(),
            PolyhedralBoundedSolidModeler.INTERSECTION, false, true, false);

        assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
            .isNotNull()
            .isNotEqualTo("normal-pipeline");
    }

    /**
    Two blocks that share part of a face (plan-stage11 Phase E). Until then
    a preflight written for this case answered the intersection and the two
    differences. Now generate and classify run, find no null edge, and the
    "no intersections found" case of [MANT1988] 15.4 answers from the
    operands as they were before generate: the differences are the
    operands, with their own 8 vertices, and the intersection is the
    shared part of the face as a lamina.
    */
    @Test
    void given_stackedBlocks_when_setOp_then_generateAndClassifyRunAndFindNoIntersection()
    {
        int[] ops = { PolyhedralBoundedSolidModeler.INTERSECTION,
            PolyhedralBoundedSolidModeler.SUBTRACT };

        for ( int op : ops ) {
            vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                .PolyhedralBoundedSolid[] pair = CsgSampleCorpusFixtures
                    .createPair(CsgSampleCorpus.STACKED_BLOCKS);
            vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
                .PolyhedralBoundedSolid result =
                    PolyhedralBoundedSolidModeler.setOp(pair[0], pair[1], op);

            assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
                .isEqualTo("no-intersection-after-classify");
            if ( op == PolyhedralBoundedSolidModeler.SUBTRACT ) {
                assertThat(result.getVerticesList().size()).isEqualTo(8);
                assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(
                    result)).isEqualTo(PolyhedralBoundedSolidVolumeOracle
                        .solidVolume(pair[0]));
            }
            else {
                assertThat(result.getPolygonsList().size()).isEqualTo(2);
                assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(
                    result)).isZero();
            }
        }
    }
}
