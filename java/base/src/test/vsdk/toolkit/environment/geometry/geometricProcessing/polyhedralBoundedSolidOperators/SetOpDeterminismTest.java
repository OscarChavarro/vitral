package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;

import static org.assertj.core.api.Assertions.assertThat;

/**
Determinism (plan-stage11 Phase B): the same operation on the same operands
gives the same solid, whatever ran before in the JVM. "Same" means the same
topology summary and the same vertex positions in the same order, bit for
bit; a typed failure must repeat as the same failure.

<p>The kernel keeps global counters (ids) and per-operation state; a result
that depends on them shows up here as a difference between two runs.</p>
 */
class SetOpDeterminismTest
{
    @Test
    void given_referencePairs_when_eachOperationRunsTwice_then_resultsAreIdentical()
    {
        List<String> differences = new ArrayList<>();

        for ( CsgSampleCorpus sample : CsgSampleCorpus.values() ) {
            for ( int op : BooleanOracleFuzz.OPERATIONS ) {
                String first = fingerprint(sample, op);
                // Unrelated work in between: other operations move the
                // global id counters and leave their own state behind.
                fingerprint(CsgSampleCorpus.values()[
                    (sample.ordinal() + 1) % CsgSampleCorpus.values().length],
                    PolyhedralBoundedSolidModeler.UNION);
                String second = fingerprint(sample, op);
                if ( !first.equals(second) ) {
                    differences.add(sample + " " +
                        BooleanOracleFuzz.opName(op));
                }
            }
        }

        assertThat(differences)
            .as("operations whose result changed between two runs")
            .isEmpty();
    }

    @Test
    void given_starPrisms_when_eachOperationRunsTwice_then_resultsAreIdentical()
    {
        List<String> differences = new ArrayList<>();

        for ( int op : BooleanOracleFuzz.OPERATIONS ) {
            String first = prismFingerprint(op);
            String second = prismFingerprint(op);
            if ( !first.equals(second) ) {
                differences.add(BooleanOracleFuzz.opName(op));
            }
        }

        assertThat(differences).isEmpty();
    }

    private static String prismFingerprint(int op)
    {
        PolyhedralBoundedSolid a = PrismFixtures.prism(
            PrismFixtures.starPolygon(5, 1.0, 0.45), 1.0);
        PolyhedralBoundedSolid b = PrismFixtures.prism(
            PrismFixtures.regularPolygon(9, 0.8), 1.2);
        PolyhedralBoundedSolidModeler.applyTransformation(b,
            PolyhedralBoundedSolidVolumeOracle.translation(0.2, 0.1, 0.3)
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(0.6,
                    1, 2, 3)));
        return run(a, b, op);
    }

    private static String fingerprint(CsgSampleCorpus sample, int op)
    {
        PolyhedralBoundedSolid[] pair =
            CsgSampleCorpusFixtures.createPair(sample);
        return run(pair[0], pair[1], op);
    }

    private static String run(PolyhedralBoundedSolid a,
        PolyhedralBoundedSolid b, int op)
    {
        PolyhedralBoundedSolid result;

        try {
            result = op == BooleanOracleFuzz.B_MINUS_A ?
                PolyhedralBoundedSolidModeler.setOp(b, a,
                    PolyhedralBoundedSolidModeler.SUBTRACT) :
                PolyhedralBoundedSolidModeler.setOp(a, b, op);
        }
        catch ( PolyhedralBoundedSolidOperationException e ) {
            return "THROWS " + e.getClass().getSimpleName();
        }
        StringBuilder text = new StringBuilder();
        text.append(PolyhedralBoundedSolidTopologySummary.from(result));
        for ( int i = 0; i < result.getVerticesList().size(); i++ ) {
            vsdk.toolkit.common.linealAlgebra.Vector3Dd p =
                result.getVerticesList().get(i).position;
            text.append('\n')
                .append(Double.doubleToLongBits(p.x())).append(' ')
                .append(Double.doubleToLongBits(p.y())).append(' ')
                .append(Double.doubleToLongBits(p.z()));
        }
        for ( int i = 0; i < result.getPolygonsList().size(); i++ ) {
            text.append('\n').append(
                result.getPolygonsList().get(i).boundariesList.size());
        }
        return text.toString();
    }
}
