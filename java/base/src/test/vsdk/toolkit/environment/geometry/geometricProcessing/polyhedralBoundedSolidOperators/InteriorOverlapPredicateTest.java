package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

import static org.assertj.core.api.Assertions.assertThat;

/**
"Do the interiors of A and B overlap?" (plan-stage11 Phase D). The
preflights of the set operator and the "result cannot be empty" guard of
`setOp` rest on this decision. It used to be taken by sampling: 27 probes
in the common bounding box and then a grid built from the vertex
coordinates. A thin overlap that falls between the samples was missed, and
more easily when the pair is not aligned with the axes.

<p>Every pair here is built from boxes, so the truth is known: the
interiors overlap exactly when the boxes overlap with positive volume
before the rigid motion applied to both. The pairs are the hard ones for a
sampling test: overlaps as thin as 10^-3 of the size, with coplanar faces,
so that no edge of one crosses a face of the other in its interior.</p>

<p>The decision has a band: an overlap thinner than about 5 tolerances is
a contact for the kernel (the tolerance is 10^-5 of the size of the pair).
The last test pins that down.</p>
 */
class InteriorOverlapPredicateTest
{
    /** One pair: two boxes given by their corners, and the truth. */
    private static final class Case {
        final String name;
        final double[] boxA;
        final double[] boxB;
        final boolean overlap;

        Case(String name, double[] a, double[] b, boolean overlap)
        {
            this.name = name;
            this.boxA = a;
            this.boxB = b;
            this.overlap = overlap;
        }
    }

    private static List<Case> cases()
    {
        List<Case> cases = new ArrayList<>();
        double[] unit = {0, 0, 0, 1, 1, 1};

        for ( double t : new double[] {0.5, 0.1, 1.0e-2, 1.0e-3} ) {
            // Same cross-section, shifted along x: only the faces across x
            // differ, every other face is coplanar with one of the other.
            cases.add(new Case("shifted, overlap " + t, unit,
                new double[] {1 - t, 0, 0, 2 - t, 1, 1}, true));
            // Coplanar top and bottom, overlapping in a thin corner column.
            cases.add(new Case("corner column " + t, unit,
                new double[] {1 - t, 1 - t, 0, 2, 2, 1}, true));
            // A thin plate inside A against its top face.
            cases.add(new Case("inner plate " + t, unit,
                new double[] {0.2, 0.2, 1 - t, 0.8, 0.8, 1}, true));
            // A thin plate through A, flush with two of its faces.
            cases.add(new Case("through plate " + t, unit,
                new double[] {0, 0.5, 0, 1, 0.5 + t, 1}, true));
            // B contains A and shares five of its faces.
            cases.add(new Case("cover, margin " + t, unit,
                new double[] {0, 0, 0, 1 + t, 1, 1}, true));
        }
        cases.add(new Case("identical", unit, unit, true));
        cases.add(new Case("face contact", unit,
            new double[] {1, 0, 0, 2, 1, 1}, false));
        cases.add(new Case("partial face contact", unit,
            new double[] {1, 0.3, 0.2, 2, 1.4, 0.7}, false));
        cases.add(new Case("stacked, partial", unit,
            new double[] {0.4, 0.3, 1, 1.4, 1.3, 1.6}, false));
        cases.add(new Case("edge contact", unit,
            new double[] {1, 0.1, 1, 1.9, 0.9, 1.8}, false));
        cases.add(new Case("vertex contact", unit,
            new double[] {1, 1, 1, 2, 2, 2}, false));
        cases.add(new Case("apart", unit,
            new double[] {1.5, 0, 0, 2.5, 1, 1}, false));
        cases.add(new Case("thin plate on top, outside", unit,
            new double[] {0.2, 0.2, 1, 0.8, 0.8, 1.001}, false));
        return cases;
    }

    @Test
    void given_axisAlignedPairs_when_interiorsOverlapIsAsked_then_answerIsExact()
    {
        assertThat(wrongAnswers(null)).isEmpty();
    }

    @Test
    void given_rotatedPairs_when_interiorsOverlapIsAsked_then_answerIsExact()
    {
        Random random = new Random(31);
        List<String> wrong = new ArrayList<>();

        for ( int i = 0; i < 12; i++ ) {
            Matrix4x4d motion = PolyhedralBoundedSolidVolumeOracle
                .translation(random.nextDouble(), random.nextDouble(),
                    random.nextDouble())
                .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(
                    3.0 * random.nextDouble(), random.nextGaussian(),
                    random.nextGaussian(), random.nextGaussian()));
            for ( String w : wrongAnswers(motion) ) {
                wrong.add("motion " + i + ": " + w);
            }
        }
        assertThat(wrong).isEmpty();
    }

    /**
    Prisms over convex polygons with the same bottom and top planes. No
    edge of one crosses a face of the other in its interior (the side
    edges end on the shared planes), so nothing but the overlap test can
    tell a thin overlap from a contact. The truth is the area of the
    intersection of the two base polygons.
    */
    @Test
    void given_prismsWithCoplanarEnds_when_interiorsOverlapIsAsked_then_answerIsExact()
    {
        Random random = new Random(57);
        List<String> wrong = new ArrayList<>();

        for ( int i = 0; i < 60; i++ ) {
            int sidesA = 3 + random.nextInt(8);
            int sidesB = 3 + random.nextInt(8);
            double[][] baseA = turned(PrismFixtures.regularPolygon(sidesA,
                1.0), 6.3 * random.nextDouble(), 0, 0);
            double direction = 6.3 * random.nextDouble();
            // Distance at which the polygons would overlap by `depth` if
            // they were discs: thin overlaps, exact contact never.
            double depth = new double[] {0.5, 0.05, 5.0e-3, 5.0e-4,
                -5.0e-4, -5.0e-3, -0.05}[i % 7];
            double[][] baseB0 = turned(PrismFixtures.regularPolygon(sidesB,
                1.0), 6.3 * random.nextDouble(), 0, 0);
            double reach = support(baseA, direction) +
                support(baseB0, direction + Math.PI) - depth;
            double[][] baseB = turned(baseB0, 0,
                reach * Math.cos(direction), reach * Math.sin(direction));
            boolean truth = intersectionArea(baseA, baseB) > 1.0e-12;
            Matrix4x4d motion = i % 2 == 0 ? null :
                PolyhedralBoundedSolidVolumeOracle.translation(
                        random.nextDouble(), random.nextDouble(),
                        random.nextDouble())
                    .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(
                        3.0 * random.nextDouble(), random.nextGaussian(),
                        random.nextGaussian(), random.nextGaussian()));
            for ( int order = 0; order < 2; order++ ) {
                PolyhedralBoundedSolid a = PrismFixtures.prism(baseA, 1.0);
                PolyhedralBoundedSolid b = PrismFixtures.prism(baseB, 1.0);
                if ( motion != null ) {
                    PolyhedralBoundedSolidModeler.applyTransformation(a,
                        motion);
                    PolyhedralBoundedSolidModeler.applyTransformation(b,
                        motion);
                }
                boolean answer = order == 0 ?
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier
                        .interiorsOverlap(a, b) :
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier
                        .interiorsOverlap(b, a);
                if ( answer != truth ) {
                    wrong.add("#" + i + " " + sidesA + "/" + sidesB +
                        " sides, depth " + depth +
                        (motion == null ? "" : " rotated") +
                        (order == 0 ? " (A,B)" : " (B,A)") + " answered " +
                        answer + ", area " + intersectionArea(baseA, baseB));
                }
            }
        }
        assertThat(wrong).isEmpty();
    }

    private static double[][] turned(double[][] xy, double angle, double dx,
        double dy)
    {
        double[][] out = new double[xy.length][2];
        double c = Math.cos(angle);
        double s = Math.sin(angle);

        for ( int i = 0; i < xy.length; i++ ) {
            out[i][0] = c * xy[i][0] - s * xy[i][1] + dx;
            out[i][1] = s * xy[i][0] + c * xy[i][1] + dy;
        }
        return out;
    }

    /** Farthest reach of a polygon along a direction. */
    private static double support(double[][] xy, double direction)
    {
        double best = -Double.MAX_VALUE;

        for ( double[] p : xy ) {
            best = Math.max(best, p[0] * Math.cos(direction) +
                p[1] * Math.sin(direction));
        }
        return best;
    }

    /** Area of the intersection of two convex polygons (Sutherland-Hodgman). */
    private static double intersectionArea(double[][] subject,
        double[][] clip)
    {
        List<double[]> polygon = new ArrayList<>();
        for ( double[] p : subject ) {
            polygon.add(p);
        }
        for ( int i = 0; i < clip.length && !polygon.isEmpty(); i++ ) {
            double[] c0 = clip[i];
            double[] c1 = clip[(i + 1) % clip.length];
            List<double[]> next = new ArrayList<>();
            for ( int j = 0; j < polygon.size(); j++ ) {
                double[] p = polygon.get(j);
                double[] q = polygon.get((j + 1) % polygon.size());
                double sp = side(c0, c1, p);
                double sq = side(c0, c1, q);
                if ( sp >= 0 ) {
                    next.add(p);
                }
                if ( (sp > 0 && sq < 0) || (sp < 0 && sq > 0) ) {
                    double t = sp / (sp - sq);
                    next.add(new double[] { p[0] + t * (q[0] - p[0]),
                        p[1] + t * (q[1] - p[1]) });
                }
            }
            polygon = next;
        }
        if ( polygon.size() < 3 ) {
            return 0.0;
        }
        return Math.abs(PrismFixtures.polygonArea(
            polygon.toArray(new double[0][])));
    }

    private static double side(double[] from, double[] to, double[] p)
    {
        return (to[0] - from[0]) * (p[1] - from[1]) -
            (to[1] - from[1]) * (p[0] - from[0]);
    }

    /**
    Inside the tolerance band the pair is a contact: a box that enters
    another by half a tolerance does not overlap it, and one that enters by
    twenty tolerances does.
    */
    @Test
    void given_overlapAroundTheTolerance_when_interiorsOverlapIsAsked_then_bandIsAContact()
    {
        double tolerance = vsdk.toolkit.environment.geometry.volume
            .polyhedralBoundedSolid.PolyhedralBoundedSolidNumericPolicy
            .forSolids(solid(new double[] {0, 0, 0, 1, 1, 1}, null),
                solid(new double[] {1, 0, 0, 2, 1, 1}, null)).bigEpsilon();

        assertThat(_PolyhedralBoundedSolidSetNonIntersectingClassifier
            .interiorsOverlap(solid(new double[] {0, 0, 0, 1, 1, 1}, null),
                solid(new double[] {1 - 0.5 * tolerance, 0, 0, 2, 1, 1},
                    null))).isFalse();
        assertThat(_PolyhedralBoundedSolidSetNonIntersectingClassifier
            .interiorsOverlap(solid(new double[] {0, 0, 0, 1, 1, 1}, null),
                solid(new double[] {1 - 20 * tolerance, 0, 0, 2, 1, 1},
                    null))).isTrue();
    }

    private static List<String> wrongAnswers(Matrix4x4d motion)
    {
        List<String> wrong = new ArrayList<>();

        for ( Case c : cases() ) {
            for ( int order = 0; order < 2; order++ ) {
                PolyhedralBoundedSolid a = solid(c.boxA, motion);
                PolyhedralBoundedSolid b = solid(c.boxB, motion);
                boolean answer = order == 0 ?
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier
                        .interiorsOverlap(a, b) :
                    _PolyhedralBoundedSolidSetNonIntersectingClassifier
                        .interiorsOverlap(b, a);
                if ( answer != c.overlap ) {
                    wrong.add(c.name + (order == 0 ? " (A,B)" : " (B,A)") +
                        " answered " + answer);
                }
            }
        }
        return wrong;
    }

    private static PolyhedralBoundedSolid solid(double[] box,
        Matrix4x4d motion)
    {
        Matrix4x4d place = PolyhedralBoundedSolidVolumeOracle.translation(
            (box[0] + box[3]) / 2.0, (box[1] + box[4]) / 2.0,
            (box[2] + box[5]) / 2.0);
        return new OrientedBox(box[3] - box[0], box[4] - box[1],
            box[5] - box[2], motion == null ? place : motion.multiply(place))
            .toSolid();
    }
}
