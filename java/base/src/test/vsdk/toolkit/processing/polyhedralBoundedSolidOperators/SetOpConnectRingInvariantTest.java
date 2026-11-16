package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.rotation;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
Post-connect invariant (Fable5.2Analysis.md Stage C step 1): when connect
leaves no loose ends, every null face handed to finish has exactly two loops,
and no other face carries two coincident loops. The second rule is what
defect B-01 broke: a section ring strictly inside a face must stay in that
face as a hole while the disk becomes a separate face; before the fix the
hole was moved into the disk, leaving the carrier without hole and an
annular zero-area face.

<p>Traceability: [MANT1988] Sec. 14.7 (join, laringmv), Sec. 15.6.1 (null
edges inserted as rings of the pierced face, Fig. 15.6), Sec. 15.7
(Program 15.14) and Sec. 15.8 (Program 15.15 expects two loops per null
face).</p>
 */
class SetOpConnectRingInvariantTest
{
    @ParameterizedTest(name = "{0} op={1}")
    @MethodSource("corpusCases")
    void given_referencePair_when_setOpReachesConnect_then_ringInvariantHolds(
        CsgSampleCorpus sample, int op)
    {
        PolyhedralBoundedSolid[] p = CsgSampleCorpusFixtures.createPair(sample);
        boolean bMinusA = op == BooleanOracleFuzz.B_MINUS_A;

        PolyhedralBoundedSolidModeler.setOp(bMinusA ? p[1] : p[0],
            bMinusA ? p[0] : p[1],
            bMinusA ? PolyhedralBoundedSolidModeler.SUBTRACT : op,
            false, true, false);

        assertViolationsEmpty(sample + " " + BooleanOracleFuzz.opName(op));
    }

    @ParameterizedTest(name = "{0} op={2}")
    @MethodSource("sectionRingCases")
    void given_sectionRingInsideAFace_when_setOp_then_ringInvariantHolds(
        String name, OrientedBox[] pair, int op)
    {
        BooleanOracleFuzz.CaseResult result = BooleanOracleFuzz.run(
            BooleanOracleFuzz.Family.B_ROTATED_ABOUT_RANDOM_AXIS, 0, pair, op);

        assertThat(_PolyhedralBoundedSolidSetOperator
            .getLastConnectInvariantViolations())
            .as("%s reached connect", name).isNotNull();
        assertViolationsEmpty(name + " " + BooleanOracleFuzz.opName(op));
        assertThat(result.outcome).as("%s", result)
            .isEqualTo(BooleanOracleFuzz.Outcome.OK);
    }

    @Test
    void given_nonCoplanarFuzzFamilies_when_setOp_then_ringInvariantHolds()
    {
        List<String> report = new ArrayList<>();

        for ( BooleanOracleFuzz.Family family :
                  BooleanOracleFuzz.Family.values() ) {
            if ( family == BooleanOracleFuzz.Family.INTEGER_GRID ||
                 family ==
                     BooleanOracleFuzz.Family.COPLANAR_TOP_BOTTOM_ROTATED_45 ) {
                // Coplanar contacts: Stage D (defects B-02, B-05).
                continue;
            }
            List<OrientedBox[]> pairs = BooleanOracleFuzz.pairs(family);
            int i;
            for ( i = 0; i < pairs.size(); i++ ) {
                for ( int op : BooleanOracleFuzz.OPERATIONS ) {
                    BooleanOracleFuzz.run(family, i, pairs.get(i), op);
                    ArrayList<String> v = _PolyhedralBoundedSolidSetOperator
                        .getLastConnectInvariantViolations();
                    if ( v != null && !v.isEmpty() ) {
                        report.add(family + "#" + i + " " +
                            BooleanOracleFuzz.opName(op) + " " + v);
                    }
                }
            }
        }
        assertThat(report).isEmpty();
    }

    static Stream<Arguments> corpusCases()
    {
        Stream.Builder<Arguments> rows = Stream.builder();
        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            for ( int op : BooleanOracleFuzz.OPERATIONS ) {
                rows.add(Arguments.of(sample, op));
            }
        }
        return rows.build();
    }

    static Stream<Arguments> sectionRingCases()
    {
        OrientedBox[] peg = {
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 1.5, translation(0.1, 0.2, 1.0)) };
        OrientedBox[] pegThrough = {
            new OrientedBox(2, 2, 2, translation(0, 0, 0)),
            new OrientedBox(0.5, 0.5, 3.0, translation(0.1, 0.2, 0.05)
                .multiply(rotation(0.3, 0, 0, 1))) };
        OrientedBox[] corner = {
            new OrientedBox(4, 4, 1, translation(0.05, 0.03, 1.2)),
            new OrientedBox(1, 1, 1, rotation(0.37, 0, 0, 1).multiply(
                rotation(Math.acos(1.0 / Math.sqrt(3.0)), 1, -1, 0))) };
        Stream.Builder<Arguments> rows = Stream.builder();
        for ( int op : BooleanOracleFuzz.OPERATIONS ) {
            rows.add(Arguments.of("peg-ending-inside-block", peg, op));
            rows.add(Arguments.of("peg-through-block", pegThrough, op));
            rows.add(Arguments.of("cube-corner-through-slab-face", corner, op));
        }
        return rows.build();
    }

    private static void assertViolationsEmpty(String label)
    {
        ArrayList<String> violations = _PolyhedralBoundedSolidSetOperator
            .getLastConnectInvariantViolations();
        if ( violations != null ) {
            assertThat(violations).as(label).isEmpty();
        }
    }
}
