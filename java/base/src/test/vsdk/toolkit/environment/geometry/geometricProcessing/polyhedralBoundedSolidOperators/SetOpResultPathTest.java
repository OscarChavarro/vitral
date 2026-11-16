package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
The result path names which code produced a boolean result: the
[MANT1988] Ch. 15 pipeline ("normal-pipeline") or one of the preflights in
front of it (no-intersection, identity, partial coplanar area). Tests can then tell a pipeline answer from a
replacement (Fable5.2Analysis.md Stage A step 4).
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
}
