package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

/**
Seeded oracle fuzz for the splitter with planes in general position
(plan-stage11 Phase B). {@link SplitterOracleFuzzTest} cuts the reference
corpus with planes through its vertices; this one cuts solids the corpus
does not have, with planes through random interior points:
<ul>
<li>star prisms (N reflex edges crossed at once);</li>
<li>regular prisms of many sides;</li>
<li>L, U, stepped and frame shapes (the frame has a through hole, genus
    1), built with unions and differences of boxes and rotated.</li>
</ul>
The check is the one of {@link SplitterOracleFuzzTest}: the pieces conserve
the volume, each lies on its side, each is strict-valid and none is empty.
 */
class SplitterGeneralPlaneOracleFuzzTest
{
    /** First measurement (2026-11-16): 240 of 240. */
    private static final int MINIMUM_CORRECT = 240;

    @Test
    void given_nonConvexSolidsAndGeneralPlanes_when_split_then_correctCountDoesNotRegress()
    {
        Random random = new Random(77);
        List<String> failures = new ArrayList<>();
        Map<String, Integer> byReason = new TreeMap<>();
        int total = 0;
        int correct = 0;

        for ( int s = 0; s < 30; s++ ) {
            String name;
            PolyhedralBoundedSolid base;
            Matrix4x4d motion = PolyhedralBoundedSolidVolumeOracle
                .translation(random.nextDouble(), random.nextDouble(),
                    random.nextDouble())
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(
                    3.0 * random.nextDouble(), random.nextGaussian(),
                    random.nextGaussian(), random.nextGaussian()));
            if ( s % 3 == 0 ) {
                int points = 4 + random.nextInt(4);
                name = "STAR" + points;
                base = PrismFixtures.prism(PrismFixtures.starPolygon(points,
                    1.0, 0.4 + 0.1 * random.nextDouble()), 1.0);
                PolyhedralBoundedSolidModeler.applyTransformation(base,
                    motion);
            }
            else if ( s % 3 == 1 ) {
                int sides = 5 + random.nextInt(28);
                name = "PRISM" + sides;
                base = PrismFixtures.prism(
                    PrismFixtures.regularPolygon(sides, 1.0), 1.0);
                PolyhedralBoundedSolidModeler.applyTransformation(base,
                    motion);
            }
            else {
                CompositeShapeOracle.Shape shape = BooleanCompositeOracleFuzz
                    .randomShape(random, 1.0).moved(motion);
                name = shape.name;
                base = shape.toSolid();
            }
            double[] minMax = base.getMinMax();
            for ( int k = 0; k < 8; k++ ) {
                Vector3Dd n = new Vector3Dd(random.nextGaussian(),
                    random.nextGaussian(), random.nextGaussian());
                Vector3Dd p = new Vector3Dd(
                    between(random, minMax[0], minMax[3]),
                    between(random, minMax[1], minMax[4]),
                    between(random, minMax[2], minMax[5]));
                if ( n.length() < 1.0e-3 ) {
                    continue;
                }
                total++;
                String reason = SplitterOracleFuzzTest.check(base,
                    new InfinitePlane(n.multiply(1.0 / n.length()), p));
                if ( reason == null ) {
                    correct++;
                }
                else {
                    byReason.merge(reason, 1, Integer::sum);
                    failures.add(name + " s=" + s + " k=" + k + " " + reason);
                }
            }
        }

        System.out.println("general-plane split fuzz: " + correct + " / " +
            total + " " + byReason);
        failures.forEach(f -> System.out.println("  " + f));
        assertThat(correct).as("correct split results must not regress")
            .isGreaterThanOrEqualTo(MINIMUM_CORRECT);
    }

    /** A value in the middle 80 % of [lo, hi]. */
    private static double between(Random random, double lo, double hi)
    {
        return lo + (0.1 + 0.8 * random.nextDouble()) * (hi - lo);
    }
}
