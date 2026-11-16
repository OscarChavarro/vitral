package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
§9 regression guard for the Finisher invariants:
<ul>
<li>§9.1 — {@code sanitizePairedFaces} pairs null faces by the index
    connect gave them. The old fallback to list order is gone: an unpaired
    set of null faces throws {@link SolidOperationFailedException}, so the
    baseline fixtures must finish without it.</li>
<li>§9.2 — {@code triangulateNonPlanarFaces} must not triangulate any
    face in the baseline fixtures that already pass validation. A non-zero
    count indicates the loopGlue produced a non-planar face, which is a
    signal for §9 follow-up work.</li>
</ul>
Traceability: plan-csg-boolean-fix-stage2.md §9.1 and §9.2.
 */
class SetOpFinishInvariantsTest
{
    @ParameterizedTest(name = "§9.1 null faces paired by index: {0} op={1}")
    @MethodSource("baselineFixtures")
    void given_baselineFixture_when_setopRuns_then_nullFacesArePairedByIndex(
        String label,
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB,
        int op)
    {
        assertThatCode(() -> PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, op, false, true, false))
            .as("[%s] finish must pair every null face by index", label)
            .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "§9.2 no triangulation: {0} op={1}")
    @MethodSource("baselineFixtures")
    void given_baselineFixture_when_setopRuns_then_noNonPlanarFaceTriangulated(
        String label,
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB,
        int op)
    {
        PolyhedralBoundedSolidModeler.setOp(
            solidA, solidB, op, false, true, false);

        assertThat(_PolyhedralBoundedSolidSetFinisher.getLastTriangulatedFaceCount())
            .as("[%s] triangulateNonPlanarFaces must split 0 faces in clean baseline", label)
            .isZero();
    }

    private static Stream<Arguments> baselineFixtures()
    {
        PolyhedralBoundedSolid[] mant1986 =
            PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair();
        PolyhedralBoundedSolid[] limit =
            PolyhedralBoundedSolidTestFixtures.createMant1988_15_2Pair(0);
        PolyhedralBoundedSolid[] fig6 =
            PolyhedralBoundedSolidTestFixtures.createMant1988_6_13Pair();

        return Stream.of(
            Arguments.of("MANT1986_2 UNION",
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[0],
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[1],
                PolyhedralBoundedSolidModeler.UNION),
            Arguments.of("MANT1986_2 INTERSECTION",
                mant1986[0], mant1986[1],
                PolyhedralBoundedSolidModeler.INTERSECTION),
            Arguments.of("MANT1986_2 SUBTRACT",
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[0],
                PolyhedralBoundedSolidTestFixtures.createMant1986_2Pair()[1],
                PolyhedralBoundedSolidModeler.SUBTRACT),
            Arguments.of("MANT1988_15_2 UNION",
                limit[0], limit[1],
                PolyhedralBoundedSolidModeler.UNION),
            Arguments.of("MANT1988_6_13 SUBTRACT",
                fig6[0], fig6[1],
                PolyhedralBoundedSolidModeler.SUBTRACT));
    }
}
