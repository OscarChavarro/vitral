package vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.environment.geometry.geometricProcessing.polyhedralBoundedSolidOperators.CompositeShapeOracle.Shape;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolid;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidTopologySummary;
import vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid.PolyhedralBoundedSolidValidationEngine;

/**
Seeded fuzz of booleans on non-convex operands and on chains of booleans,
checked against {@link CompositeShapeOracle} (Fable5.2Analysis.md Section 8,
"further tests worth writing"):

<ul>
<li>non-convex operands (L, U, stepped block, frame with a through hole),
axis-aligned in general position and rigidly rotated;</li>
<li>chains, where the result of one boolean is an operand of the next:
{@code (A ∪ B) \ C}, {@code (A ∩ B) ∪ C}, {@code A \ (B ∪ C)};</li>
<li>scale: the rotated non-convex family at 10⁻³ and at 10³;</li>
<li>commutativity of ∪ and ∩: same volume and same topology summary.</li>
</ul>

<p>A case is correct when every result is strict-valid and has the oracle
volume. A non-manifold rejection is correct only in the axis-aligned family:
after a random rigid motion the operands are in general position, where a
touching contact has probability zero, so a rejection there means the
kernel built a broken result. The scaled families replay the cases of the
rotated family. {@link BooleanCompositeOracleFuzzGateTest} is the ratchet;
{@code main} prints every family ({@code -v} also lists the rejections).</p>
 */
final class BooleanCompositeOracleFuzz
{
    enum Family
    {
        NON_CONVEX_AXIS_ALIGNED(12),
        NON_CONVEX_ROTATED(12),
        NON_CONVEX_ROTATED_SCALE_1E_MINUS_3(12),
        NON_CONVEX_ROTATED_SCALE_1E_PLUS_3(12),
        CHAINS(12),
        // Appended last: the seed of a family is its ordinal.
        NON_CONVEX_COPLANAR_BOTTOMS(12);

        final int cases;

        Family(int cases)
        {
            this.cases = cases;
        }
    }

    enum Outcome
    {
        OK,
        REJECTED_NON_MANIFOLD,
        WRONG_VOLUME,
        STRICT_REJECTED,
        NOT_COMMUTATIVE,
        EXCEPTION
    }

    static final class CaseResult
    {
        Family family;
        int index;
        String label;
        Outcome outcome;
        String detail;

        @Override
        public String toString()
        {
            return family + " #" + index + " " + label + ": " + outcome +
                (detail == null ? "" : " " + detail);
        }
    }

    static final class FamilySummary
    {
        int total;
        int correct;
        final Map<Outcome, Integer> byOutcome = new LinkedHashMap<>();
        final List<CaseResult> failures = new ArrayList<>();
        final List<CaseResult> rejected = new ArrayList<>();
    }

    private static final String[] EXPRESSIONS = {
        "(A u B) - C", "(A n B) u C", "A - (B u C)" };

    private BooleanCompositeOracleFuzz()
    {
    }

    static FamilySummary runFamily(Family family)
    {
        FamilySummary summary = new FamilySummary();
        Random random = new Random(seedOf(family));
        int i;

        for ( Outcome o : Outcome.values() ) {
            summary.byOutcome.put(o, 0);
        }
        for ( i = 0; i < family.cases; i++ ) {
            List<CaseResult> results = family == Family.CHAINS ?
                runChains(family, i, random) :
                runPair(family, i, random);
            for ( CaseResult r : results ) {
                summary.total++;
                summary.byOutcome.merge(r.outcome, 1, Integer::sum);
                // Random rigid motions put the operands in general
                // position, where touching contacts have probability zero:
                // a non-manifold rejection there hides a broken result.
                // Only the axis-aligned family can really touch.
                boolean touchingPossible =
                    family == Family.NON_CONVEX_AXIS_ALIGNED;
                if ( r.outcome == Outcome.OK ||
                     (r.outcome == Outcome.REJECTED_NON_MANIFOLD &&
                      touchingPossible) ) {
                    summary.correct++;
                    if ( r.outcome == Outcome.REJECTED_NON_MANIFOLD ) {
                        summary.rejected.add(r);
                    }
                }
                else {
                    summary.failures.add(r);
                }
            }
        }
        return summary;
    }

    //= Shapes ==============================================================

    static Shape randomShape(Random random, double scale)
    {
        double w = 0.3 + 0.2 * random.nextDouble();
        double h = 0.3 + 0.3 * random.nextDouble();
        int kind = random.nextInt(5);
        ArrayList<double[]> positive = new ArrayList<>();
        ArrayList<double[]> negative = new ArrayList<>();
        String name;

        switch ( kind ) {
            case 0:
                name = "L";
                positive.add(new double[] {0, 0, 0, 1, w, h});
                positive.add(new double[] {0, 0, 0, w, 1, h});
                break;
            case 1:
                name = "U";
                w = w / 1.5;
                positive.add(new double[] {0, 0, 0, w, 1, h});
                positive.add(new double[] {1 - w, 0, 0, 1, 1, h});
                positive.add(new double[] {0, 0, 0, 1, w, h});
                break;
            case 2:
                name = "STEP";
                positive.add(new double[] {0, 0, 0, 1, 1, h});
                positive.add(new double[] {0, 0, 0, w, 1, 2 * h});
                break;
            case 3:
                name = "FRAME";
                w = w / 1.5;
                positive.add(new double[] {0, 0, 0, 1, 1, h});
                negative.add(new double[] {w, w, -0.25, 1 - w, 1 - w,
                    h + 0.25});
                break;
            default:
                name = "BOX";
                positive.add(new double[] {0, 0, 0, 1, 0.5 + w, h});
                break;
        }
        scaleBoxes(positive, scale);
        scaleBoxes(negative, scale);
        return new Shape(name, positive, negative,
            PolyhedralBoundedSolidVolumeOracle.translation(-0.5 * scale,
                -0.5 * scale, -0.5 * h * scale));
    }

    private static void scaleBoxes(List<double[]> boxes, double scale)
    {
        for ( double[] b : boxes ) {
            int k;
            for ( k = 0; k < 6; k++ ) {
                b[k] *= scale;
            }
        }
    }

    static Matrix4x4d randomRotation(Random random)
    {
        return PolyhedralBoundedSolidVolumeOracle.rotation(
            0.2 + 2.5 * random.nextDouble(), random.nextGaussian(),
            random.nextGaussian(), random.nextGaussian());
    }

    static Matrix4x4d randomOffset(Random random, double scale)
    {
        return PolyhedralBoundedSolidVolumeOracle.translation(
            scale * (0.8 * random.nextDouble() - 0.4),
            scale * (0.8 * random.nextDouble() - 0.4),
            scale * (0.6 * random.nextDouble() - 0.3));
    }

    /**
    The scaled families replay the cases of NON_CONVEX_ROTATED: the same
    configurations at another size must give the same answers.
    */
    private static long seedOf(Family family)
    {
        if ( family == Family.NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 ||
             family == Family.NON_CONVEX_ROTATED_SCALE_1E_PLUS_3 ) {
            return 1000 + Family.NON_CONVEX_ROTATED.ordinal();
        }
        return 1000 + family.ordinal();
    }

    private static double scaleOf(Family family)
    {
        if ( family == Family.NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 ) {
            return 1.0e-3;
        }
        if ( family == Family.NON_CONVEX_ROTATED_SCALE_1E_PLUS_3 ) {
            return 1.0e3;
        }
        return 1.0;
    }

    //= Pairs ===============================================================

    /**
    The operands of case {@code index} of {@code family}, as the fuzz
    generates them (for reproducing one case in isolation).
    */
    static List<Shape> shapesOf(Family family, int index)
    {
        Random random = new Random(seedOf(family));
        List<Shape> shapes = null;
        int i;

        for ( i = 0; i <= index; i++ ) {
            shapes = family == Family.CHAINS ? chainShapes(random) :
                pairShapes(family, random);
        }
        return shapes;
    }

    private static List<Shape> pairShapes(Family family, Random random)
    {
        double scale = scaleOf(family);
        boolean rotated = family != Family.NON_CONVEX_AXIS_ALIGNED;
        Shape a = randomShape(random, scale);
        Shape b = randomShape(random, scale);
        if ( family == Family.NON_CONVEX_COPLANAR_BOTTOMS ) {
            return coplanarPair(a, b, random);
        }
        Matrix4x4d motionA = rotated ? randomRotation(random) :
            new Matrix4x4d();
        Matrix4x4d motionB = randomOffset(random, scale).multiply(
            rotated ? randomRotation(random) : new Matrix4x4d());

        return List.of(a.moved(motionA), b.moved(motionA.multiply(motionB)));
    }

    /**
    Both bottoms on one plane, B turned about the plane normal and moved
    along the plane, then a common random rotation: coplanar non-convex
    faces whose edges cross at general angles ([MANT1988] 15.1, the case
    the book leaves to a two-dimensional set operation).
    */
    private static List<Shape> coplanarPair(Shape a, Shape b, Random random)
    {
        Matrix4x4d common = randomRotation(random);
        double align = a.frame.get(2, 3) - b.frame.get(2, 3);
        Matrix4x4d inPlane = PolyhedralBoundedSolidVolumeOracle.translation(
            0.8 * random.nextDouble() - 0.4, 0.8 * random.nextDouble() - 0.4,
            align).multiply(PolyhedralBoundedSolidVolumeOracle.rotation(
            0.2 + 2.5 * random.nextDouble(), 0, 0, 1));

        return List.of(a.moved(common), b.moved(common.multiply(inPlane)));
    }

    private static List<Shape> chainShapes(Random random)
    {
        Matrix4x4d common = randomRotation(random);
        Shape a = randomShape(random, 1.0).moved(common);
        Shape b = randomShape(random, 1.0).moved(common.multiply(
            randomOffset(random, 1.0).multiply(randomRotation(random))));
        Shape c = randomShape(random, 1.0).moved(common.multiply(
            randomOffset(random, 1.0).multiply(randomRotation(random))));
        return List.of(a, b, c);
    }

    private static List<CaseResult> runPair(Family family, int index,
        Random random)
    {
        double scale = scaleOf(family);
        List<Shape> shapes = pairShapes(family, random);
        Shape a = shapes.get(0);
        Shape b = shapes.get(1);
        ArrayList<CaseResult> results = new ArrayList<>();

        String pairLabel = a + "/" + b;
        String operandProblem = checkOperands(shapes);
        double tolerance = 1.0e-6 * scale * scale * scale;

        results.add(evaluate(family, index, pairLabel + " A u B",
            operandProblem, shapes, m -> m[0] || m[1], tolerance,
            ops -> setOp(ops[0], ops[1], PolyhedralBoundedSolidModeler.UNION)));
        results.add(evaluate(family, index, pairLabel + " A n B",
            operandProblem, shapes, m -> m[0] && m[1], tolerance,
            ops -> setOp(ops[0], ops[1],
                PolyhedralBoundedSolidModeler.INTERSECTION)));
        results.add(evaluate(family, index, pairLabel + " A - B",
            operandProblem, shapes, m -> m[0] && !m[1], tolerance,
            ops -> setOp(ops[0], ops[1],
                PolyhedralBoundedSolidModeler.SUBTRACT)));
        results.add(evaluate(family, index, pairLabel + " B - A",
            operandProblem, shapes, m -> m[1] && !m[0], tolerance,
            ops -> setOp(ops[1], ops[0],
                PolyhedralBoundedSolidModeler.SUBTRACT)));
        results.add(commutativity(family, index, pairLabel + " u comm",
            operandProblem, shapes, PolyhedralBoundedSolidModeler.UNION));
        results.add(commutativity(family, index, pairLabel + " n comm",
            operandProblem, shapes,
            PolyhedralBoundedSolidModeler.INTERSECTION));
        return results;
    }

    //= Chains ==============================================================

    private static List<CaseResult> runChains(Family family, int index,
        Random random)
    {
        List<Shape> shapes = chainShapes(random);
        Shape a = shapes.get(0);
        Shape b = shapes.get(1);
        Shape c = shapes.get(2);
        String label = a + "/" + b + "/" + c + " ";
        String operandProblem = checkOperands(shapes);
        ArrayList<CaseResult> results = new ArrayList<>();
        int u = PolyhedralBoundedSolidModeler.UNION;
        int n = PolyhedralBoundedSolidModeler.INTERSECTION;
        int d = PolyhedralBoundedSolidModeler.SUBTRACT;

        results.add(evaluate(family, index, label + EXPRESSIONS[0],
            operandProblem, shapes, m -> (m[0] || m[1]) && !m[2], 1.0e-6,
            ops -> setOp(setOp(ops[0], ops[1], u), ops[2], d)));
        results.add(evaluate(family, index, label + EXPRESSIONS[1],
            operandProblem, shapes, m -> (m[0] && m[1]) || m[2], 1.0e-6,
            ops -> setOp(setOp(ops[0], ops[1], n), ops[2], u)));
        results.add(evaluate(family, index, label + EXPRESSIONS[2],
            operandProblem, shapes, m -> m[0] && !(m[1] || m[2]), 1.0e-6,
            ops -> setOp(ops[0], setOp(ops[1], ops[2], u), d)));
        return results;
    }

    //= Evaluation ==========================================================

    interface Expression
    {
        PolyhedralBoundedSolid apply(PolyhedralBoundedSolid[] operands);
    }

    /**
    Strict setOp. A chain whose first step yields the empty solid passes it
    on as an operand: the empty set is a valid operand of the next step.
    */
    static PolyhedralBoundedSolid setOp(PolyhedralBoundedSolid a,
        PolyhedralBoundedSolid b, int op)
    {
        if ( PolyhedralBoundedSolidModeler.isEmptyResult(a) ) {
            return op == PolyhedralBoundedSolidModeler.UNION ?
                copy(b) : new PolyhedralBoundedSolid();
        }
        if ( PolyhedralBoundedSolidModeler.isEmptyResult(b) ) {
            return op == PolyhedralBoundedSolidModeler.INTERSECTION ?
                new PolyhedralBoundedSolid() : copy(a);
        }
        return PolyhedralBoundedSolidModeler.setOp(a, b, op);
    }

    private static PolyhedralBoundedSolid copy(PolyhedralBoundedSolid s)
    {
        return vsdk.toolkit.environment.geometry.volume.polyhedralBoundedSolid
            .PolyhedralBoundedSolidStructuralCopy.copy(s);
    }

    private static String checkOperands(List<Shape> shapes)
    {
        for ( Shape s : shapes ) {
            try {
                PolyhedralBoundedSolid solid = s.toSolid();
                double expected = s.volume();
                double actual =
                    PolyhedralBoundedSolidVolumeOracle.solidVolume(solid);
                if ( Math.abs(actual - expected) >
                     1.0e-6 * Math.max(1.0e-9, expected) ) {
                    return "operand " + s + " volume " + actual +
                        " expected " + expected;
                }
            }
            catch ( RuntimeException e ) {
                return "operand " + s + " " + e.getClass().getSimpleName();
            }
        }
        return null;
    }

    private static PolyhedralBoundedSolid[] build(List<Shape> shapes)
    {
        PolyhedralBoundedSolid[] solids =
            new PolyhedralBoundedSolid[shapes.size()];
        int i;

        for ( i = 0; i < solids.length; i++ ) {
            solids[i] = shapes.get(i).toSolid();
        }
        return solids;
    }

    private static CaseResult evaluate(Family family, int index,
        String label, String operandProblem, List<Shape> shapes,
        Predicate<boolean[]> membership, double tolerance,
        Expression expression)
    {
        CaseResult r = result(family, index, label);
        double expected;

        if ( operandProblem != null ) {
            r.outcome = Outcome.EXCEPTION;
            r.detail = operandProblem;
            return r;
        }
        expected = CompositeShapeOracle.volume(shapes, membership);
        try {
            PolyhedralBoundedSolid out = expression.apply(build(shapes));
            double actual = PolyhedralBoundedSolidVolumeOracle.solidVolume(out);
            StringBuilder report = new StringBuilder();
            if ( out.getPolygonsList().size() > 0 &&
                 !PolyhedralBoundedSolidValidationEngine.validateStrict(out,
                     report) ) {
                r.outcome = Outcome.STRICT_REJECTED;
                r.detail = firstLine(report.toString());
            }
            else if ( Math.abs(actual - expected) >
                      Math.max(tolerance, 1.0e-6 * expected) ) {
                r.outcome = Outcome.WRONG_VOLUME;
                r.detail = "volume " + actual + " expected " + expected;
            }
            else {
                r.outcome = Outcome.OK;
            }
        }
        catch ( NonManifoldBooleanResultException e ) {
            r.outcome = Outcome.REJECTED_NON_MANIFOLD;
            r.detail = firstLine(e.getMessage());
        }
        catch ( RuntimeException e ) {
            r.outcome = Outcome.EXCEPTION;
            r.detail = e.getClass().getSimpleName() + ": " +
                firstLine(e.getMessage());
        }
        return r;
    }

    private static CaseResult commutativity(Family family, int index,
        String label, String operandProblem, List<Shape> shapes, int op)
    {
        CaseResult r = result(family, index, label);

        if ( operandProblem != null ) {
            r.outcome = Outcome.EXCEPTION;
            r.detail = operandProblem;
            return r;
        }
        try {
            PolyhedralBoundedSolid[] ab = build(shapes);
            PolyhedralBoundedSolid[] ba = build(shapes);
            String first = describe(setOp(ab[0], ab[1], op));
            String second = describe(setOp(ba[1], ba[0], op));
            r.outcome = first.equals(second) ? Outcome.OK :
                Outcome.NOT_COMMUTATIVE;
            if ( r.outcome != Outcome.OK ) {
                r.detail = first + " vs " + second;
            }
        }
        catch ( NonManifoldBooleanResultException e ) {
            r.outcome = Outcome.REJECTED_NON_MANIFOLD;
        }
        catch ( RuntimeException e ) {
            r.outcome = Outcome.EXCEPTION;
            r.detail = e.getClass().getSimpleName() + ": " +
                firstLine(e.getMessage());
        }
        return r;
    }

    private static String describe(PolyhedralBoundedSolid solid)
    {
        PolyhedralBoundedSolidTopologySummary t =
            PolyhedralBoundedSolidTopologySummary.from(solid);
        return "F=" + t.getFaceCount() + " E=" + t.getEdgeCount() + " V=" +
            t.getVertexCount() + " S=" + t.getShellCount() + " vol=" +
            String.format("%.9g",
                PolyhedralBoundedSolidVolumeOracle.solidVolume(solid));
    }

    private static CaseResult result(Family family, int index, String label)
    {
        CaseResult r = new CaseResult();
        r.family = family;
        r.index = index;
        r.label = label;
        return r;
    }

    private static String firstLine(String text)
    {
        if ( text == null ) {
            return "";
        }
        int cut = text.indexOf('\n');
        String line = cut < 0 ? text : text.substring(0, cut);
        return line.length() > 200 ? line.substring(0, 200) : line;
    }

    public static void main(String[] args)
    {
        vsdk.toolkit.common.VSDK.setWithSystemExit(false);
        for ( Family family : Family.values() ) {
            long t0 = System.nanoTime();
            FamilySummary s = runFamily(family);
            System.out.println(family + ": " + s.correct + " / " + s.total +
                " " + s.byOutcome + " " +
                (System.nanoTime() - t0) / 1000000 + " ms");
            s.failures.forEach(f -> System.out.println("  " + f));
            if ( args.length > 0 && args[0].equals("-v") ) {
                s.rejected.forEach(f -> System.out.println("  rejected " + f));
            }
        }
    }
}
