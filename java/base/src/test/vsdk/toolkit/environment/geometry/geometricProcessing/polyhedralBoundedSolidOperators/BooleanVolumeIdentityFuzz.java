package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;

/**
Seeded fuzz that needs no oracle: for any two solids the four results must
satisfy
<pre>
|A u B| = |A| + |B| - |A n B|
|A - B| = |A| - |A n B|
|B - A| = |B| - |A n B|
</pre>
with 0 &lt;= |A n B| &lt;= min(|A|, |B|). The operands are prisms: regular ones
of N sides (polyhedral cylinders) and star prisms (non-convex), B rotated
about a random axis so that no face of one is parallel to a face of the
other. The volume of each operand is also checked against the area of its
base polygon, so the prisms themselves are known to be right.

<p>A case is correct when the four operations return strict-valid solids
and the three identities hold.</p>
 */
final class BooleanVolumeIdentityFuzz
{
    static final double TOLERANCE = 1.0e-9;

    enum Family {
        /** Two regular prisms; 5 to 24 sides. */
        CONVEX_PRISMS,
        /** A star prism and a regular prism. */
        STAR_AND_CONVEX,
        /** Two star prisms. */
        STAR_PRISMS
    }

    static final class Summary {
        int total;
        int correct;
        final Map<String, Integer> byOutcome = new TreeMap<>();
        final List<String> failures = new ArrayList<>();
    }

    private BooleanVolumeIdentityFuzz()
    {
    }

    static final int CASES_PER_FAMILY = 16;

    static Summary runFamily(Family family)
    {
        Summary summary = new Summary();
        Random random = new Random(4201 + family.ordinal());
        int i;

        for ( i = 0; i < CASES_PER_FAMILY; i++ ) {
            double[][] baseA = base(family, 0, random);
            double[][] baseB = base(family, 1, random);
            double heightA = 0.8 + 0.6 * random.nextDouble();
            double heightB = 0.8 + 0.6 * random.nextDouble();
            Matrix4x4d placement = placement(random);
            String outcome = check(baseA, heightA, baseB, heightB,
                placement);
            summary.total++;
            summary.byOutcome.merge(outcome.split(" ")[0], 1, Integer::sum);
            if ( outcome.equals("OK") ) {
                summary.correct++;
            }
            else {
                summary.failures.add(family + " #" + i + " " + outcome);
            }
        }
        return summary;
    }

    private static double[][] base(Family family, int which, Random random)
    {
        boolean star = family == Family.STAR_PRISMS ||
            (family == Family.STAR_AND_CONVEX && which == 0);

        if ( star ) {
            int points = 4 + random.nextInt(4);
            return PrismFixtures.starPolygon(points,
                0.9 + 0.3 * random.nextDouble(),
                0.35 + 0.15 * random.nextDouble());
        }
        return PrismFixtures.regularPolygon(5 + random.nextInt(20),
            0.7 + 0.4 * random.nextDouble());
    }

    /** B is rotated about a random axis and moved less than its size. */
    private static Matrix4x4d placement(Random random)
    {
        double x = random.nextGaussian();
        double y = random.nextGaussian();
        double z = random.nextGaussian();
        double angle = 0.2 + 1.2 * random.nextDouble();

        return PolyhedralBoundedSolidVolumeOracle.translation(
                0.6 * (random.nextDouble() - 0.5),
                0.6 * (random.nextDouble() - 0.5),
                0.5 * (random.nextDouble() - 0.5) + 0.3)
            .multiply(PolyhedralBoundedSolidVolumeOracle.rotation(angle,
                x, y, z));
    }

    private static PolyhedralBoundedSolid a(double[][] base, double height)
    {
        return PrismFixtures.prism(base, height);
    }

    private static PolyhedralBoundedSolid b(double[][] base, double height,
        Matrix4x4d placement)
    {
        PolyhedralBoundedSolid solid = PrismFixtures.prism(base, height);

        PolyhedralBoundedSolidModeler.applyTransformation(solid, placement);
        return solid;
    }

    static String check(double[][] baseA, double heightA, double[][] baseB,
        double heightB, Matrix4x4d placement)
    {
        double volumeA = PolyhedralBoundedSolidVolumeOracle.solidVolume(
            a(baseA, heightA));
        double volumeB = PolyhedralBoundedSolidVolumeOracle.solidVolume(
            b(baseB, heightB, placement));
        double scale = Math.max(1.0, volumeA + volumeB);
        double union;
        double intersection;
        double aMinusB;
        double bMinusA;

        if ( Math.abs(volumeA - PrismFixtures.polygonArea(baseA) * heightA) >
                 TOLERANCE * scale ||
             Math.abs(volumeB - PrismFixtures.polygonArea(baseB) * heightB) >
                 TOLERANCE * scale ) {
            return "OPERAND volume differs from base area x height";
        }
        try {
            union = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(a(baseA, heightA),
                    b(baseB, heightB, placement),
                    PolyhedralBoundedSolidModeler.UNION));
            intersection = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(a(baseA, heightA),
                    b(baseB, heightB, placement),
                    PolyhedralBoundedSolidModeler.INTERSECTION));
            aMinusB = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(a(baseA, heightA),
                    b(baseB, heightB, placement),
                    PolyhedralBoundedSolidModeler.SUBTRACT));
            bMinusA = PolyhedralBoundedSolidVolumeOracle.solidVolume(
                PolyhedralBoundedSolidModeler.setOp(
                    b(baseB, heightB, placement), a(baseA, heightA),
                    PolyhedralBoundedSolidModeler.SUBTRACT));
        }
        catch ( PolyhedralBoundedSolidOperationException e ) {
            return "EXCEPTION " + e.getClass().getSimpleName() + ": " +
                firstLine(e.getMessage());
        }
        if ( intersection < -TOLERANCE * scale ||
             intersection > Math.min(volumeA, volumeB) + TOLERANCE * scale ) {
            return "RANGE |AnB|=" + intersection + " |A|=" + volumeA +
                " |B|=" + volumeB;
        }
        double errorUnion = union - (volumeA + volumeB - intersection);
        double errorAB = aMinusB - (volumeA - intersection);
        double errorBA = bMinusA - (volumeB - intersection);
        if ( Math.abs(errorUnion) > TOLERANCE * scale ||
             Math.abs(errorAB) > TOLERANCE * scale ||
             Math.abs(errorBA) > TOLERANCE * scale ) {
            return "IDENTITY union " + errorUnion + ", A-B " + errorAB +
                ", B-A " + errorBA + " (|A|=" + volumeA + " |B|=" + volumeB +
                " |AnB|=" + intersection + ")";
        }
        return "OK";
    }

    private static String firstLine(String message)
    {
        if ( message == null ) {
            return "";
        }
        String line = message.split("\n")[0];
        return line.length() > 160 ? line.substring(0, 160) : line;
    }

    /** Prints the outcome of every family. */
    public static void main(String[] args)
    {
        vsdk.toolkit.common.VSDK.setWithSystemExit(false);
        for ( Family family : Family.values() ) {
            Summary s = runFamily(family);
            System.out.println(family + ": " + s.correct + " / " + s.total +
                " " + s.byOutcome);
            s.failures.forEach(f -> System.out.println("  " + f));
        }
    }
}
