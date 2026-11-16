#include <math.h>
#include <stdio.h>

#include "BooleanCompositeOracleFuzz.h"

#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralTestFailures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef CompositeShapeOracle::Shape Shape;
typedef BooleanCompositeOracleFuzz Fuzz;

static const char* const EXPRESSIONS[3] = {
    "(A u B) - C", "(A n B) u C", "A - (B u C)" };

static String
numberText(double value)
{
    char text[40];

    sprintf(text, "%.17g", value);
    return text;
}

int BooleanCompositeOracleFuzz::casesOf(Family /*family*/)
{
    return 12;
}

const char* BooleanCompositeOracleFuzz::familyName(Family family)
{
    static const char* const NAMES[] = {
        "NON_CONVEX_AXIS_ALIGNED", "NON_CONVEX_ROTATED",
        "NON_CONVEX_ROTATED_SCALE_1E_MINUS_3",
        "NON_CONVEX_ROTATED_SCALE_1E_PLUS_3", "CHAINS",
        "NON_CONVEX_COPLANAR_BOTTOMS" };
    return NAMES[(int)family];
}

const char* BooleanCompositeOracleFuzz::outcomeName(Outcome outcome)
{
    static const char* const NAMES[] = {
        "OK", "REJECTED_NON_MANIFOLD", "WRONG_VOLUME", "STRICT_REJECTED",
        "NOT_COMMUTATIVE", "EXCEPTION" };
    return NAMES[(int)outcome];
}

BooleanCompositeOracleFuzz::CaseResult::CaseResult() :
    family(NON_CONVEX_AXIS_ALIGNED), index(0), outcome(OK)
{
}

String BooleanCompositeOracleFuzz::CaseResult::toString() const
{
    char number[32];
    String text;

    sprintf(number, "%d", index);
    text = String(familyName(family)) + " #" + number + " " + label + ": " +
        outcomeName(outcome);
    if ( detail.length() > 0 ) {
        text = text + " " + detail;
    }
    return text;
}

BOOLEAN BooleanCompositeOracleFuzz::CaseResult::operator==(
    const CaseResult& other) const
{
    return this == &other;
}

BooleanCompositeOracleFuzz::FamilySummary::FamilySummary() :
    total(0), correct(0)
{
    int i;

    for ( i = 0; i < OUTCOME_COUNT; i++ ) {
        byOutcome[i] = 0;
    }
}

String BooleanCompositeOracleFuzz::FamilySummary::byOutcomeText() const
{
    String text = "{";
    char number[32];
    int i;

    for ( i = 0; i < OUTCOME_COUNT; i++ ) {
        if ( i > 0 ) {
            text = text + ", ";
        }
        sprintf(number, "%d", byOutcome[i]);
        text = text + outcomeName((Outcome)i) + "=" + number;
    }
    return text + "}";
}

BooleanCompositeOracleFuzz::FamilySummary
BooleanCompositeOracleFuzz::runFamily(Family family)
{
    FamilySummary summary;
    Random random(seedOf(family));
    int i;

    for ( i = 0; i < casesOf(family); i++ ) {
        ArrayList<CaseResult> results;
        long k;
        if ( family == CHAINS ) {
            runChains(family, i, random, results);
        }
        else {
            runPair(family, i, random, results);
        }
        for ( k = 0; k < results.size(); k++ ) {
            const CaseResult& r = results[k];
            summary.total++;
            summary.byOutcome[(int)r.outcome]++;
            // Random rigid motions put the operands in general
            // position, where touching contacts have probability zero:
            // a non-manifold rejection there hides a broken result.
            // Only the axis-aligned family can really touch.
            BOOLEAN touchingPossible = family == NON_CONVEX_AXIS_ALIGNED;
            if ( r.outcome == OK ||
                 (r.outcome == REJECTED_NON_MANIFOLD && touchingPossible) ) {
                summary.correct++;
                if ( r.outcome == REJECTED_NON_MANIFOLD ) {
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

//= Shapes ==================================================================

Shape BooleanCompositeOracleFuzz::randomShape(Random& random, double scale)
{
    double w = 0.3 + 0.2 * random.nextDouble();
    double h = 0.3 + 0.3 * random.nextDouble();
    int kind = random.nextInt(5);
    ArrayList<CompositeBox> positive;
    ArrayList<CompositeBox> negative;
    String name;

    switch ( kind ) {
        case 0:
            name = "L";
            positive.add(CompositeBox(0, 0, 0, 1, w, h));
            positive.add(CompositeBox(0, 0, 0, w, 1, h));
            break;
        case 1:
            name = "U";
            w = w / 1.5;
            positive.add(CompositeBox(0, 0, 0, w, 1, h));
            positive.add(CompositeBox(1 - w, 0, 0, 1, 1, h));
            positive.add(CompositeBox(0, 0, 0, 1, w, h));
            break;
        case 2:
            name = "STEP";
            positive.add(CompositeBox(0, 0, 0, 1, 1, h));
            positive.add(CompositeBox(0, 0, 0, w, 1, 2 * h));
            break;
        case 3:
            name = "FRAME";
            w = w / 1.5;
            positive.add(CompositeBox(0, 0, 0, 1, 1, h));
            negative.add(CompositeBox(w, w, -0.25, 1 - w, 1 - w, h + 0.25));
            break;
        default:
            name = "BOX";
            positive.add(CompositeBox(0, 0, 0, 1, 0.5 + w, h));
            break;
    }
    scaleBoxes(positive, scale);
    scaleBoxes(negative, scale);
    return Shape(name, positive, negative,
        Oracle::translation(-0.5 * scale, -0.5 * scale, -0.5 * h * scale));
}

void BooleanCompositeOracleFuzz::scaleBoxes(ArrayList<CompositeBox>& boxes,
    double scale)
{
    long i;
    int k;

    for ( i = 0; i < boxes.size(); i++ ) {
        for ( k = 0; k < 6; k++ ) {
            boxes[i].v[k] *= scale;
        }
    }
}

/**
C++ port note: here and below, every random draw is made into a variable
first, in the left-to-right order in which Java evaluates the arguments.
*/
Matrix4x4d BooleanCompositeOracleFuzz::randomRotation(Random& random)
{
    double angle = 0.2 + 2.5 * random.nextDouble();
    double x = random.nextGaussian();
    double y = random.nextGaussian();
    double z = random.nextGaussian();

    return Oracle::rotation(angle, x, y, z);
}

Matrix4x4d BooleanCompositeOracleFuzz::randomOffset(Random& random,
    double scale)
{
    double x = scale * (0.8 * random.nextDouble() - 0.4);
    double y = scale * (0.8 * random.nextDouble() - 0.4);
    double z = scale * (0.6 * random.nextDouble() - 0.3);

    return Oracle::translation(x, y, z);
}

/**
The scaled families replay the cases of NON_CONVEX_ROTATED: the same
configurations at another size must give the same answers.
*/
int BooleanCompositeOracleFuzz::seedOf(Family family)
{
    if ( family == NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 ||
         family == NON_CONVEX_ROTATED_SCALE_1E_PLUS_3 ) {
        return 1000 + (int)NON_CONVEX_ROTATED;
    }
    return 1000 + (int)family;
}

double BooleanCompositeOracleFuzz::scaleOf(Family family)
{
    if ( family == NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 ) {
        return 1.0e-3;
    }
    if ( family == NON_CONVEX_ROTATED_SCALE_1E_PLUS_3 ) {
        return 1.0e3;
    }
    return 1.0;
}

//= Pairs ===================================================================

ArrayList<Shape> BooleanCompositeOracleFuzz::shapesOf(Family family,
    int index)
{
    Random random(seedOf(family));
    ArrayList<Shape> shapes;
    int i;

    for ( i = 0; i <= index; i++ ) {
        if ( family == CHAINS ) {
            shapes = chainShapes(random);
        }
        else {
            shapes = pairShapes(family, random);
        }
    }
    return shapes;
}

ArrayList<Shape> BooleanCompositeOracleFuzz::pairShapes(Family family,
    Random& random)
{
    double scale = scaleOf(family);
    BOOLEAN rotated = family != NON_CONVEX_AXIS_ALIGNED;
    Shape a = randomShape(random, scale);
    Shape b = randomShape(random, scale);
    if ( family == NON_CONVEX_COPLANAR_BOTTOMS ) {
        return coplanarPair(a, b, random);
    }
    Matrix4x4d motionA;
    if ( rotated ) {
        motionA = randomRotation(random);
    }
    Matrix4x4d offset = randomOffset(random, scale);
    Matrix4x4d turn;
    if ( rotated ) {
        turn = randomRotation(random);
    }
    Matrix4x4d motionB = offset.multiply(turn);
    ArrayList<Shape> shapes;

    shapes.add(a.moved(motionA));
    shapes.add(b.moved(motionA.multiply(motionB)));
    return shapes;
}

/**
Both bottoms on one plane, B turned about the plane normal and moved
along the plane, then a common random rotation: coplanar non-convex
faces whose edges cross at general angles ([MANT1988] 15.1, the case
the book leaves to a two-dimensional set operation).
*/
ArrayList<Shape> BooleanCompositeOracleFuzz::coplanarPair(const Shape& a,
    const Shape& b, Random& random)
{
    Matrix4x4d common = randomRotation(random);
    double align = a.frame.get(2, 3) - b.frame.get(2, 3);
    double x = 0.8 * random.nextDouble() - 0.4;
    double y = 0.8 * random.nextDouble() - 0.4;
    double angle = 0.2 + 2.5 * random.nextDouble();
    Matrix4x4d inPlane = Oracle::translation(x, y, align).multiply(
        Oracle::rotation(angle, 0, 0, 1));
    ArrayList<Shape> shapes;

    shapes.add(a.moved(common));
    shapes.add(b.moved(common.multiply(inPlane)));
    return shapes;
}

ArrayList<Shape> BooleanCompositeOracleFuzz::chainShapes(Random& random)
{
    Matrix4x4d common = randomRotation(random);
    Shape a = randomShape(random, 1.0).moved(common);
    Shape b0 = randomShape(random, 1.0);
    Matrix4x4d offsetB = randomOffset(random, 1.0);
    Matrix4x4d turnB = randomRotation(random);
    Shape b = b0.moved(common.multiply(offsetB.multiply(turnB)));
    Shape c0 = randomShape(random, 1.0);
    Matrix4x4d offsetC = randomOffset(random, 1.0);
    Matrix4x4d turnC = randomRotation(random);
    Shape c = c0.moved(common.multiply(offsetC.multiply(turnC)));
    ArrayList<Shape> shapes;

    shapes.add(a);
    shapes.add(b);
    shapes.add(c);
    return shapes;
}

static BOOLEAN membershipUnion(const BOOLEAN* m) { return m[0] || m[1]; }
static BOOLEAN membershipIntersection(const BOOLEAN* m) { return m[0] && m[1]; }
static BOOLEAN membershipAMinusB(const BOOLEAN* m) { return m[0] && !m[1]; }
static BOOLEAN membershipBMinusA(const BOOLEAN* m) { return m[1] && !m[0]; }
static BOOLEAN membershipUnionMinus(const BOOLEAN* m)
{
    return (m[0] || m[1]) && !m[2];
}
static BOOLEAN membershipIntersectionUnion(const BOOLEAN* m)
{
    return (m[0] && m[1]) || m[2];
}
static BOOLEAN membershipMinusUnion(const BOOLEAN* m)
{
    return m[0] && !(m[1] || m[2]);
}

void BooleanCompositeOracleFuzz::runPair(Family family, int index,
    Random& random, ArrayList<CaseResult>& results)
{
    double scale = scaleOf(family);
    ArrayList<Shape> shapes = pairShapes(family, random);
    const Shape& a = shapes[0];
    const Shape& b = shapes[1];

    String pairLabel = a.toString() + "/" + b.toString();
    String operandProblem = checkOperands(shapes);
    double tolerance = 1.0e-6 * scale * scale * scale;

    results.add(evaluate(family, index, pairLabel + " A u B",
        operandProblem, shapes, membershipUnion, tolerance, PAIR_UNION));
    results.add(evaluate(family, index, pairLabel + " A n B",
        operandProblem, shapes, membershipIntersection, tolerance,
        PAIR_INTERSECTION));
    results.add(evaluate(family, index, pairLabel + " A - B",
        operandProblem, shapes, membershipAMinusB, tolerance,
        PAIR_A_MINUS_B));
    results.add(evaluate(family, index, pairLabel + " B - A",
        operandProblem, shapes, membershipBMinusA, tolerance,
        PAIR_B_MINUS_A));
    results.add(commutativity(family, index, pairLabel + " u comm",
        operandProblem, shapes, Modeler::UNION));
    results.add(commutativity(family, index, pairLabel + " n comm",
        operandProblem, shapes, Modeler::INTERSECTION));
}

//= Chains ==================================================================

void BooleanCompositeOracleFuzz::runChains(Family family, int index,
    Random& random, ArrayList<CaseResult>& results)
{
    ArrayList<Shape> shapes = chainShapes(random);
    String label = shapes[0].toString() + "/" + shapes[1].toString() + "/" +
        shapes[2].toString() + " ";
    String operandProblem = checkOperands(shapes);

    results.add(evaluate(family, index, label + EXPRESSIONS[0],
        operandProblem, shapes, membershipUnionMinus, 1.0e-6,
        CHAIN_UNION_MINUS));
    results.add(evaluate(family, index, label + EXPRESSIONS[1],
        operandProblem, shapes, membershipIntersectionUnion, 1.0e-6,
        CHAIN_INTERSECTION_UNION));
    results.add(evaluate(family, index, label + EXPRESSIONS[2],
        operandProblem, shapes, membershipMinusUnion, 1.0e-6,
        CHAIN_MINUS_UNION));
}

//= Evaluation ==============================================================

PolyhedralBoundedSolid* BooleanCompositeOracleFuzz::setOp(
    PolyhedralBoundedSolid* a, PolyhedralBoundedSolid* b, int op)
{
    if ( Modeler::isEmptyResult(a) ) {
        if ( op == Modeler::UNION ) {
            return copy(b);
        }
        return new PolyhedralBoundedSolid();
    }
    if ( Modeler::isEmptyResult(b) ) {
        if ( op == Modeler::INTERSECTION ) {
            return new PolyhedralBoundedSolid();
        }
        return copy(a);
    }
    return Modeler::setOp(a, b, op);
}

PolyhedralBoundedSolid* BooleanCompositeOracleFuzz::copy(
    PolyhedralBoundedSolid* s)
{
    return PolyhedralBoundedSolidStructuralCopy::copy(s);
}

String BooleanCompositeOracleFuzz::checkOperands(
    const ArrayList<Shape>& shapes)
{
    long i;

    for ( i = 0; i < shapes.size(); i++ ) {
        const Shape& s = shapes[i];
        PolyhedralBoundedSolid* solid = NULL;
        String failureName;
        String failureMessage;
        String problem;
        try {
            solid = s.toSolid();
            double expected = s.volume();
            double actual = Oracle::solidVolume(solid);
            double floor = expected > 1.0e-9 ? expected : 1.0e-9;
            if ( fabs(actual - expected) > 1.0e-6 * floor ) {
                problem = String("operand ") + s.toString() + " volume " +
                    numberText(actual) + " expected " + numberText(expected);
            }
        }
        PBS_CATCH_THROWABLE(failureName, failureMessage)
        delete solid;
        if ( failureName.length() > 0 ) {
            return String("operand ") + s.toString() + " " + failureName;
        }
        if ( problem.length() > 0 ) {
            return problem;
        }
    }
    return "";
}

void BooleanCompositeOracleFuzz::build(const ArrayList<Shape>& shapes,
    ArrayList<PolyhedralBoundedSolid*>& solids)
{
    long i;

    for ( i = 0; i < shapes.size(); i++ ) {
        solids.add(shapes[i].toSolid());
    }
}

/**
The Java `Expression` lambdas. The intermediate results go to `garbage`,
deleted by the caller also when a step throws.
*/
PolyhedralBoundedSolid* BooleanCompositeOracleFuzz::apply(
    ExpressionCode code, ArrayList<PolyhedralBoundedSolid*>& ops,
    ArrayList<PolyhedralBoundedSolid*>& garbage)
{
    PolyhedralBoundedSolid* step;

    switch ( code ) {
      case PAIR_UNION:
        return setOp(ops[0], ops[1], Modeler::UNION);
      case PAIR_INTERSECTION:
        return setOp(ops[0], ops[1], Modeler::INTERSECTION);
      case PAIR_A_MINUS_B:
        return setOp(ops[0], ops[1], Modeler::SUBTRACT);
      case PAIR_B_MINUS_A:
        return setOp(ops[1], ops[0], Modeler::SUBTRACT);
      case CHAIN_UNION_MINUS:
        step = setOp(ops[0], ops[1], Modeler::UNION);
        garbage.add(step);
        return setOp(step, ops[2], Modeler::SUBTRACT);
      case CHAIN_INTERSECTION_UNION:
        step = setOp(ops[0], ops[1], Modeler::INTERSECTION);
        garbage.add(step);
        return setOp(step, ops[2], Modeler::UNION);
      case CHAIN_MINUS_UNION:
      default:
        break;
    }
    step = setOp(ops[1], ops[2], Modeler::UNION);
    garbage.add(step);
    return setOp(ops[0], step, Modeler::SUBTRACT);
}

BooleanCompositeOracleFuzz::CaseResult BooleanCompositeOracleFuzz::evaluate(
    Family family, int index, const String& label,
    const String& operandProblem, const ArrayList<Shape>& shapes,
    CompositeMembership membership, double tolerance,
    ExpressionCode expression)
{
    CaseResult r = result(family, index, label);
    double expected;

    if ( operandProblem.length() > 0 ) {
        r.outcome = EXCEPTION;
        r.detail = operandProblem;
        return r;
    }
    expected = CompositeShapeOracle::volume(shapes, membership);
    ArrayList<PolyhedralBoundedSolid*> garbage;
    PolyhedralBoundedSolid* out = NULL;
    String failureName;
    String failureMessage;
    BOOLEAN nonManifold = FALSE;
    try {
        ArrayList<PolyhedralBoundedSolid*> operands;
        build(shapes, operands);
        garbage.addAll(operands);
        out = apply(expression, operands, garbage);
        double actual = Oracle::solidVolume(out);
        String report;
        if ( out->getPolygonsList().size() > 0 &&
             !PolyhedralBoundedSolidValidationEngine::validateStrict(out,
                 &report) ) {
            r.outcome = STRICT_REJECTED;
            r.detail = firstLine(report);
        }
        else if ( fabs(actual - expected) >
                  (tolerance > 1.0e-6 * expected ? tolerance :
                   1.0e-6 * expected) ) {
            r.outcome = WRONG_VOLUME;
            r.detail = String("volume ") + numberText(actual) +
                " expected " + numberText(expected);
        }
        else {
            r.outcome = OK;
        }
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const NonManifoldBooleanResultException& e ) {
        nonManifold = TRUE;
        failureMessage = e.getMessage();
    }
#endif
    PBS_CATCH_THROWABLE_REST(failureName, failureMessage)
    if ( nonManifold ) {
        r.outcome = REJECTED_NON_MANIFOLD;
        r.detail = firstLine(failureMessage);
    }
    else if ( failureName.length() > 0 ) {
        r.outcome = EXCEPTION;
        r.detail = failureName + ": " + firstLine(failureMessage);
    }
    delete out;
    PolyhedralBoundedSolidTestFixtures::dispose(garbage);
    return r;
}

BooleanCompositeOracleFuzz::CaseResult
BooleanCompositeOracleFuzz::commutativity(Family family, int index,
    const String& label, const String& operandProblem,
    const ArrayList<Shape>& shapes, int op)
{
    CaseResult r = result(family, index, label);

    if ( operandProblem.length() > 0 ) {
        r.outcome = EXCEPTION;
        r.detail = operandProblem;
        return r;
    }
    ArrayList<PolyhedralBoundedSolid*> garbage;
    String failureName;
    String failureMessage;
    BOOLEAN nonManifold = FALSE;
    try {
        ArrayList<PolyhedralBoundedSolid*> ab;
        ArrayList<PolyhedralBoundedSolid*> ba;
        build(shapes, ab);
        garbage.addAll(ab);
        build(shapes, ba);
        garbage.addAll(ba);
        PolyhedralBoundedSolid* firstResult = setOp(ab[0], ab[1], op);
        garbage.add(firstResult);
        String first = describe(firstResult);
        PolyhedralBoundedSolid* secondResult = setOp(ba[1], ba[0], op);
        garbage.add(secondResult);
        String second = describe(secondResult);
        r.outcome = first == second ? OK : NOT_COMMUTATIVE;
        if ( r.outcome != OK ) {
            r.detail = first + " vs " + second;
        }
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const NonManifoldBooleanResultException& ) {
        nonManifold = TRUE;
    }
#endif
    PBS_CATCH_THROWABLE_REST(failureName, failureMessage)
    if ( nonManifold ) {
        r.outcome = REJECTED_NON_MANIFOLD;
    }
    else if ( failureName.length() > 0 ) {
        r.outcome = EXCEPTION;
        r.detail = failureName + ": " + firstLine(failureMessage);
    }
    PolyhedralBoundedSolidTestFixtures::dispose(garbage);
    return r;
}

String BooleanCompositeOracleFuzz::describe(PolyhedralBoundedSolid* solid)
{
    PolyhedralBoundedSolidTopologySummary t =
        PolyhedralBoundedSolidTopologySummary::from(solid);
    char text[160];

    sprintf(text, "F=%d E=%d V=%d S=%d vol=%.9g", t.getFaceCount(),
        t.getEdgeCount(), t.getVertexCount(), t.getShellCount(),
        Oracle::solidVolume(solid));
    return text;
}

BooleanCompositeOracleFuzz::CaseResult BooleanCompositeOracleFuzz::result(
    Family family, int index, const String& label)
{
    CaseResult r;

    r.family = family;
    r.index = index;
    r.label = label;
    return r;
}

String BooleanCompositeOracleFuzz::firstLine(const String& text)
{
    int cut = text.indexOf('\n');
    String line = cut < 0 ? text : text.substring(0, cut);

    return line.length() > 200 ? line.substring(0, 200) : line;
}
