package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.OrientedBox;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.rotation;
import static vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.PolyhedralBoundedSolidVolumeOracle.translation;

/**
Seeded box-pair fuzz over the six families of Fable5.2Analysis.md Section 1,
judged by {@link PolyhedralBoundedSolidVolumeOracle}: a result counts as
correct only when setOp returns without exception, the result passes
validateStrict and its divergence-theorem volume matches the exact
inclusion-exclusion volume. Each pair runs A u B, A n B, A \ B and B \ A.

<p>The same generator is used by the slow gate test and can be run as a
program to print the per-family table:
{@code java ... BooleanOracleFuzz}.</p>
 */
final class BooleanOracleFuzz
{
    static final int B_MINUS_A = 4;
    static final int[] OPERATIONS = {
        PolyhedralBoundedSolidModeler.UNION,
        PolyhedralBoundedSolidModeler.INTERSECTION,
        PolyhedralBoundedSolidModeler.SUBTRACT,
        B_MINUS_A };

    enum Family
    {
        AXIS_ALIGNED_GENERAL_POSITION(12),
        B_ROTATED_ABOUT_Z(12),
        B_ROTATED_ABOUT_RANDOM_AXIS(12),
        RANDOM_AXIS_TRANSLATED_TO_1000(12),
        INTEGER_GRID(80),
        COPLANAR_TOP_BOTTOM_ROTATED_45(12);

        final int pairs;

        Family(int pairs)
        {
            this.pairs = pairs;
        }
    }

    enum Outcome
    {
        OK,
        /** Union of solids touching only along edges or at vertices,
        rejected by design as non-manifold (B-04): a correct answer. */
        REJECTED_NON_MANIFOLD,
        WRONG_VOLUME,
        STRICT_REJECTED,
        EXCEPTION
    }

    static final class CaseResult
    {
        final Family family;
        final int pairIndex;
        final int operation;
        final Outcome outcome;
        final String path;
        final String detail;

        CaseResult(Family family, int pairIndex, int op, Outcome outcome,
            String path, String detail)
        {
            this.family = family;
            this.pairIndex = pairIndex;
            this.operation = op;
            this.outcome = outcome;
            this.path = path;
            this.detail = detail;
        }

        @Override
        public String toString()
        {
            return family + "#" + pairIndex + " " + opName(operation) + " " +
                outcome + " path=" + path + " " + detail;
        }
    }

    static final class FamilySummary
    {
        int total;
        int correct;
        /** Included in {@link #correct}. */
        int okByRejection;
        final Map<Outcome, Integer> byOutcome = new LinkedHashMap<>();
        final List<CaseResult> failures = new ArrayList<>();
    }

    private BooleanOracleFuzz()
    {
    }

    static String opName(int op)
    {
        if ( op == PolyhedralBoundedSolidModeler.UNION ) {
            return "UNION";
        }
        if ( op == PolyhedralBoundedSolidModeler.INTERSECTION ) {
            return "INTERSECTION";
        }
        if ( op == PolyhedralBoundedSolidModeler.SUBTRACT ) {
            return "A-B";
        }
        return "B-A";
    }

    /** Fixed-seed pairs of one family; the seed depends only on the family. */
    static List<OrientedBox[]> pairs(Family family)
    {
        // The translated family replays the random-axis family's seed, so
        // its cases are exactly those pairs moved to (1000, 1000, 1000).
        Family seedFamily = family == Family.RANDOM_AXIS_TRANSLATED_TO_1000 ?
            Family.B_ROTATED_ABOUT_RANDOM_AXIS : family;
        Random random = new Random(20261114L + seedFamily.ordinal() * 7919L);
        ArrayList<OrientedBox[]> result = new ArrayList<>();
        int i;

        for ( i = 0; i < family.pairs; i++ ) {
            result.add(pair(family, random));
        }
        return result;
    }

    private static double uniform(Random random, double lo, double hi)
    {
        return lo + (hi - lo) * random.nextDouble();
    }

    private static Matrix4x4d randomAxisRotation(Random random)
    {
        double x;
        double y;
        double z;
        double length;

        do {
            x = uniform(random, -1, 1);
            y = uniform(random, -1, 1);
            z = uniform(random, -1, 1);
            length = Math.sqrt(x * x + y * y + z * z);
        } while ( length < 0.2 || length > 1.0 );
        return rotation(uniform(random, 0.2, 2.9),
            x / length, y / length, z / length);
    }

    private static OrientedBox[] pair(Family family, Random random)
    {
        OrientedBox a;
        OrientedBox b;
        Matrix4x4d place;

        switch ( family ) {
          case AXIS_ALIGNED_GENERAL_POSITION:
            a = new OrientedBox(uniform(random, 0.8, 2.0),
                uniform(random, 0.8, 2.0), uniform(random, 0.8, 2.0),
                translation(0, 0, 0));
            b = new OrientedBox(uniform(random, 0.5, 1.8),
                uniform(random, 0.5, 1.8), uniform(random, 0.5, 1.8),
                translation(uniform(random, -0.7, 0.7),
                    uniform(random, -0.7, 0.7), uniform(random, -0.7, 0.7)));
            return new OrientedBox[] { a, b };
          case B_ROTATED_ABOUT_Z:
            a = new OrientedBox(uniform(random, 0.8, 2.0),
                uniform(random, 0.8, 2.0), uniform(random, 0.8, 2.0),
                translation(0, 0, 0));
            b = new OrientedBox(uniform(random, 0.5, 1.8),
                uniform(random, 0.5, 1.8), uniform(random, 0.5, 1.8),
                translation(uniform(random, -0.7, 0.7),
                    uniform(random, -0.7, 0.7), uniform(random, -0.7, 0.7))
                .multiply(rotation(uniform(random, 0.2, 1.4), 0, 0, 1)));
            return new OrientedBox[] { a, b };
          case B_ROTATED_ABOUT_RANDOM_AXIS:
          case RANDOM_AXIS_TRANSLATED_TO_1000:
            double offset =
                family == Family.RANDOM_AXIS_TRANSLATED_TO_1000 ? 1000 : 0;
            a = new OrientedBox(uniform(random, 0.8, 2.0),
                uniform(random, 0.8, 2.0), uniform(random, 0.8, 2.0),
                translation(offset, offset, offset));
            b = new OrientedBox(uniform(random, 0.5, 1.8),
                uniform(random, 0.5, 1.8), uniform(random, 0.5, 1.8),
                translation(offset + uniform(random, -0.7, 0.7),
                    offset + uniform(random, -0.7, 0.7),
                    offset + uniform(random, -0.7, 0.7))
                .multiply(randomAxisRotation(random)));
            return new OrientedBox[] { a, b };
          case INTEGER_GRID:
            a = gridBox(random);
            b = gridBox(random);
            return new OrientedBox[] { a, b };
          case COPLANAR_TOP_BOTTOM_ROTATED_45:
          default:
            a = new OrientedBox(2, 2, 1, translation(0, 0, 0));
            double side = uniform(random, 0.4, 1.2);
            place = translation(uniform(random, -1.0, 1.0),
                uniform(random, -1.0, 1.0), 0)
                .multiply(rotation(Math.PI / 4.0, 0, 0, 1));
            b = new OrientedBox(side, side, 1, place);
            return new OrientedBox[] { a, b };
        }
    }

    /** Box with integer corners in [0, 3]^3: coplanar faces, shared edges. */
    private static OrientedBox gridBox(Random random)
    {
        int[] lo = new int[3];
        int[] size = new int[3];
        int k;

        for ( k = 0; k < 3; k++ ) {
            size[k] = 1 + random.nextInt(2);
            lo[k] = random.nextInt(4 - size[k]);
        }
        return new OrientedBox(size[0], size[1], size[2],
            translation(lo[0] + size[0] / 2.0, lo[1] + size[1] / 2.0,
                lo[2] + size[2] / 2.0));
    }

    static CaseResult run(Family family, int pairIndex, OrientedBox[] pair,
        int op)
    {
        OrientedBox first = op == B_MINUS_A ? pair[1] : pair[0];
        OrientedBox second = op == B_MINUS_A ? pair[0] : pair[1];
        int kernelOp = op == B_MINUS_A ?
            PolyhedralBoundedSolidModeler.SUBTRACT : op;
        double expected = PolyhedralBoundedSolidVolumeOracle.expectedVolume(
            first, second, kernelOp);
        double tolerance = 1.0e-6 *
            Math.max(1.0, pair[0].volume() + pair[1].volume());
        PolyhedralBoundedSolid result;
        String path;

        try {
            result = PolyhedralBoundedSolidModeler.setOp(first.toSolid(),
                second.toSolid(), kernelOp, false, true, false);
        }
        catch ( NonManifoldBooleanResultException t ) {
            double overlap = PolyhedralBoundedSolidVolumeOracle.expectedVolume(
                first, second, PolyhedralBoundedSolidModeler.INTERSECTION);
            Outcome outcome = kernelOp == PolyhedralBoundedSolidModeler.UNION &&
                overlap <= tolerance ?
                Outcome.REJECTED_NON_MANIFOLD : Outcome.EXCEPTION;
            return new CaseResult(family, pairIndex, op, outcome,
                PolyhedralBoundedSolidModeler.getLastSetOpResultPath(),
                "NonManifoldBooleanResultException, overlap=" + overlap);
        }
        catch ( Throwable t ) {
            return new CaseResult(family, pairIndex, op, Outcome.EXCEPTION,
                PolyhedralBoundedSolidModeler.getLastSetOpResultPath(),
                t.getClass().getSimpleName() + ": " + firstLine(t.getMessage()));
        }
        path = PolyhedralBoundedSolidModeler.getLastSetOpResultPath();

        StringBuilder strict = new StringBuilder();
        boolean strictValid;
        try {
            strictValid = PolyhedralBoundedSolidValidationEngine.validateStrict(
                result, strict);
        }
        catch ( Throwable t ) {
            return new CaseResult(family, pairIndex, op, Outcome.EXCEPTION,
                path, "validateStrict: " + t.getClass().getSimpleName());
        }
        double volume = PolyhedralBoundedSolidVolumeOracle.solidVolume(result);
        String detail = String.format("volume=%.6f expected=%.6f", volume,
            expected);
        if ( Math.abs(volume - expected) > tolerance ) {
            return new CaseResult(family, pairIndex, op, Outcome.WRONG_VOLUME,
                path, detail + (strictValid ? " (silent)" : " (strict fail)"));
        }
        if ( !strictValid ) {
            return new CaseResult(family, pairIndex, op,
                Outcome.STRICT_REJECTED, path,
                detail + " " + firstLine(strict.toString()));
        }
        return new CaseResult(family, pairIndex, op, Outcome.OK, path, detail);
    }

    private static String firstLine(String s)
    {
        if ( s == null ) {
            return "";
        }
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }

    static FamilySummary runFamily(Family family)
    {
        FamilySummary summary = new FamilySummary();
        List<OrientedBox[]> pairs = pairs(family);
        int i;

        for ( Outcome outcome : Outcome.values() ) {
            summary.byOutcome.put(outcome, 0);
        }
        for ( i = 0; i < pairs.size(); i++ ) {
            for ( int op : OPERATIONS ) {
                CaseResult r = run(family, i, pairs.get(i), op);
                summary.total++;
                summary.byOutcome.merge(r.outcome, 1, Integer::sum);
                if ( r.outcome == Outcome.REJECTED_NON_MANIFOLD ) {
                    summary.correct++;
                    summary.okByRejection++;
                }
                else if ( r.outcome == Outcome.OK ) {
                    summary.correct++;
                }
                else {
                    summary.failures.add(r);
                }
            }
        }
        return summary;
    }

    public static void main(String[] args)
    {
        vsdk.toolkit.common.VSDK.setWithSystemExit(false);
        System.out.println("| Family | Correct (strict-valid or rejected) | " +
            "rejected non-manifold | wrong volume | strict rejected | " +
            "exception |");
        System.out.println("|---|---:|---:|---:|---:|---:|");
        ArrayList<CaseResult> failures = new ArrayList<>();
        for ( Family family : Family.values() ) {
            FamilySummary s = runFamily(family);
            System.out.println("| " + family + " | " + s.correct + " / " +
                s.total + " | " +
                s.okByRejection + " | " +
                s.byOutcome.get(Outcome.WRONG_VOLUME) + " | " +
                s.byOutcome.get(Outcome.STRICT_REJECTED) + " | " +
                s.byOutcome.get(Outcome.EXCEPTION) + " |");
            failures.addAll(s.failures);
        }
        if ( args.length > 0 && args[0].equals("-v") ) {
            for ( CaseResult r : failures ) {
                System.out.println(r);
            }
        }
    }
}
