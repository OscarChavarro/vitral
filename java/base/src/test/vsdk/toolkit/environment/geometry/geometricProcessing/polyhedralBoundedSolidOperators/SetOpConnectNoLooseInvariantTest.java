package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

/**
Acceptance test for the {@code setopconnect} contract from
[MANT1988] §15.7, Program 15.14.

<p>Program 15.14 (sgetnextnulledge / scanjoin / join / cuta / cutb)
guarantees by construction that, when the loop terminates, every
null-edge endpoint has been joined into the active topology and no
"loose" half-edges remain pending: <em>looseA = looseB = 0</em>. This
invariant is the hard contract that any compliant implementation of
the Connect phase must satisfy — it is what allows {@code setopfinish}
to consume {@code sonfa}/{@code sonfb} without ad-hoc recoveries.</p>

<p>This test does not rewrite the connector — it audits the existing
{@link _PolyhedralBoundedSolidSetNullEdgesConnector} against the book's
invariant on a curated set of canonical operand pairs and reads the
loose counters via the {@code getLastLoose*Count} accessors.</p>

<p>The matrix is split into two groups:
<ul>
<li><b>baseline</b>: cases the current connector already closes
correctly. They lock in the parts of Program 15.14 that are conformant
today — any future regression is caught by these tests.</li>
<li><b>pending §6.1</b>: cases that still leave loose endpoints; they
are {@link Disabled} with a precise count so progress on §6.1 of
plan-csg-boolean-fix-stage2 (and the §5.2 deferred sectoroverlap
fix) can be measured incrementally — when the connector becomes
compliant for one of these, removing the {@code @Disabled} should
make the test green without further changes.</li>
</ul>
</p>
 */
class SetOpConnectNoLooseInvariantTest
{
    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("baselineCases")
    void given_baselinePair_when_setopRuns_then_connectLeavesNoLooseEndpoints(
        String pairName,
        String opName,
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB,
        int op)
    {
        PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false);

        int looseA = _PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseACount();
        int looseB = _PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseBCount();

        assertThat(looseA)
            .as("[%s + %s] looseA after Connect must be 0 per [MANT1988] Program 15.14",
                pairName, opName)
            .isZero();
        assertThat(looseB)
            .as("[%s + %s] looseB after Connect must be 0 per [MANT1988] Program 15.14",
                pairName, opName)
            .isZero();
    }

    @Disabled("Pending §6.1-C/§5.2 unified: scanjoin's main loop misses "
        + "latent loose-pair closures for MANT1988_15_1 INTERSECTION/SUBTRACT "
        + "(looseA=4). Diagnosis: the loose halves satisfy neighbor() between "
        + "themselves but scanjoin only compares new-vs-loose. A post-pass "
        + "closure was attempted in §6.1-C-attempt-1 but fused legitimately-"
        + "separate shells (HOLLOW_BRICK case) — needs upstream §5.2 fix.")
    @ParameterizedTest(name = "{0} + {1}")
    @MethodSource("pendingCases")
    void given_pendingPair_when_setopRuns_then_connectShouldLeaveNoLooseEndpoints(
        String pairName,
        String opName,
        PolyhedralBoundedSolid solidA,
        PolyhedralBoundedSolid solidB,
        int op)
    {
        PolyhedralBoundedSolidModeler.setOp(solidA, solidB, op, false);

        int looseA = _PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseACount();
        int looseB = _PolyhedralBoundedSolidSetNullEdgesConnector
            .getLastLooseBCount();

        assertThat(looseA)
            .as("[%s + %s] looseA after Connect must be 0 per [MANT1988] Program 15.14",
                pairName, opName)
            .isZero();
        assertThat(looseB)
            .as("[%s + %s] looseB after Connect must be 0 per [MANT1988] Program 15.14",
                pairName, opName)
            .isZero();
    }

    static Stream<Arguments> baselineCases()
    {
        // Cases where the current connector already satisfies Program 15.14:
        //   looseA == looseB == 0 after Connect. Locked in as regression
        //   guard; do not move to "pending" without an investigation.
        return Stream.of(
            pairCase("MANT1988_15_1", "UNION",
                PolyhedralBoundedSolidModeler.UNION),
            pairCase("STACKED_BLOCKS", "UNION",
                PolyhedralBoundedSolidModeler.UNION),
            pairCase("STACKED_BLOCKS", "INTERSECTION",
                PolyhedralBoundedSolidModeler.INTERSECTION),
            pairCase("STACKED_BLOCKS", "SUBTRACT",
                PolyhedralBoundedSolidModeler.SUBTRACT));
    }

    static Stream<Arguments> pendingCases()
    {
        // Cases where the current connector leaves loose endpoints
        // (looseA > 0). Documented baseline at the time of writing:
        //   - MANT1988_15_1 + INTERSECTION → looseA = 4
        //   - MANT1988_15_1 + SUBTRACT     → looseA = 4
        // Both share the §5.2 sectoroverlap deferral as suspected root cause.
        return Stream.of(
            pairCase("MANT1988_15_1", "INTERSECTION",
                PolyhedralBoundedSolidModeler.INTERSECTION),
            pairCase("MANT1988_15_1", "SUBTRACT",
                PolyhedralBoundedSolidModeler.SUBTRACT));
    }

    private static Arguments pairCase(String pairName, String opName, int op)
    {
        PolyhedralBoundedSolid[] pair = createPair(pairName);
        return Arguments.of(pairName, opName, pair[0], pair[1], op);
    }

    private static PolyhedralBoundedSolid[] createPair(String name)
    {
        if ( name.equals("STACKED_BLOCKS") ) {
            return CsgSampleCorpusFixtures.createPair(
                CsgSampleCorpus.STACKED_BLOCKS);
        }
        return SimpleTestGeometryLibrary
            .createTestObjectPairMANT1988_15_1();
    }
}
