package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.stream.Stream;

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

<p>Every case of the matrix must close: the cases that used to leave
loose endpoints (MANT1988_15_1 intersection and difference) close since
connect drops isolated struts.</p>
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
                PolyhedralBoundedSolidModeler.SUBTRACT),
            // These two left four loose ends (an isolated strut on each
            // solid at a coplanar boundary-ray contact) until connect
            // learned to drop isolated struts (2026-11-16).
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
