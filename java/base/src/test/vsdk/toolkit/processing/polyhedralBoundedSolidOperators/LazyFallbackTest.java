package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
The axis-aligned cell and orthogonal profile fallbacks are built only when
the pipeline fails (Fable5.2Analysis.md Stage D step 4). Before, the cell
grid was classified for every axis-aligned operand pair, before the
preflights, whether or not it was used.
 */
class LazyFallbackTest
{
    @Test
    void given_axisAlignedBoxesThatThePipelineSolves_when_setOp_then_noFallbackIsBuilt()
    {
        OrientedBox a = new OrientedBox(1.4, 1.2, 1.6, translation(0, 0, 0));
        OrientedBox b = new OrientedBox(1.1, 1.5, 1.3,
            translation(0.413, -0.287, 0.351));
        int before = _PolyhedralBoundedSolidLazyFallback.getBuildCount();

        PolyhedralBoundedSolid result = PolyhedralBoundedSolidModeler.setOp(
            a.toSolid(), b.toSolid(), PolyhedralBoundedSolidModeler.UNION);

        assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
            .isEqualTo("normal-pipeline");
        assertThat(_PolyhedralBoundedSolidLazyFallback.getBuildCount())
            .isEqualTo(before);
        assertThat(PolyhedralBoundedSolidVolumeOracle.solidVolume(result))
            .isGreaterThan(0.0);
    }

    @Test
    void given_crossPairThatNeedsTheCellFallback_when_setOp_then_itIsBuiltOnDemand()
    {
        PolyhedralBoundedSolid[] p =
            CsgSampleCorpusFixtures.createPair(CsgSampleCorpus.CROSS_PAIR);
        int before = _PolyhedralBoundedSolidLazyFallback.getBuildCount();

        PolyhedralBoundedSolidModeler.setOp(p[0], p[1],
            PolyhedralBoundedSolidModeler.INTERSECTION, false, true, false);

        assertThat(PolyhedralBoundedSolidModeler.getLastSetOpResultPath())
            .startsWith("axis-aligned-cell-fallback");
        assertThat(_PolyhedralBoundedSolidLazyFallback.getBuildCount())
            .isGreaterThan(before);
    }
}
