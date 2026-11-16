#include <cmath>

#include "BooleanCompositeOracleFuzz.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "ThrowableSimpleName.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidTopologySummary.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef BooleanCompositeOracleFuzz Fuzz;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

const Fuzz::Family Fuzz::FAMILIES[FAMILY_COUNT] = {
    NON_CONVEX_AXIS_ALIGNED, NON_CONVEX_ROTATED,
    NON_CONVEX_ROTATED_SCALE_1E_MINUS_3, NON_CONVEX_ROTATED_SCALE_1E_PLUS_3,
    CHAINS, NON_CONVEX_COPLANAR_BOTTOMS };

static const char* EXPRESSIONS[3] = {
    "(A u B) - C", "(A n B) u C", "A - (B u C)" };

int Fuzz::casesOf(Family family)
{
    (void)family;
    return 12;
}

const char* Fuzz::familyName(Family family)
{
    static const char* NAMES[FAMILY_COUNT] = {
        "NON_CONVEX_AXIS_ALIGNED", "NON_CONVEX_ROTATED",
        "NON_CONVEX_ROTATED_SCALE_1E_MINUS_3",
        "NON_CONVEX_ROTATED_SCALE_1E_PLUS_3", "CHAINS",
        "NON_CONVEX_COPLANAR_BOTTOMS" };
    return NAMES[(int)family];
}

const char* Fuzz::outcomeName(Outcome outcome)
{
    static const char* NAMES[OUTCOME_COUNT] = {
        "OK", "REJECTED_NON_MANIFOLD", "WRONG_VOLUME", "STRICT_REJECTED",
        "NOT_COMMUTATIVE", "EXCEPTION" };
    return NAMES[(int)outcome];
}

Fuzz::CaseResult::CaseResult() :
    family(NON_CONVEX_AXIS_ALIGNED), index(0), outcome(OK), hasDetail(false)
{
}

bool Fuzz::CaseResult::operator==(const CaseResult& other) const
{
    return this == &other;
}

java::String Fuzz::CaseResult::toString() const
{
    return java::String(familyName(family)) + " #" +
        java::String::valueOf(index) + " " + label + ": " +
        outcomeName(outcome) +
        (hasDetail ? java::String(" ") + detail : java::String(""));
}

Fuzz::FamilySummary::FamilySummary() : total(0), correct(0)
{
    int i;
    for ( i = 0; i < OUTCOME_COUNT; i++ ) {
        byOutcome[i] = 0;
    }
}

java::String Fuzz::FamilySummary::byOutcomeText() const
{
    java::String text("{");
    int i;
    for ( i = 0; i < OUTCOME_COUNT; i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += java::String(outcomeName((Outcome)i)) + "=" +
            java::String::valueOf(byOutcome[i]);
    }
    return text + "}";
}

Fuzz::FamilySummary Fuzz::runFamily(Family family)
{
    FamilySummary summary;
    java::Random random(seedOf(family));
    int i;

    for ( i = 0; i < casesOf(family); i++ ) {
        java::ArrayList<CaseResult> results = family == CHAINS ?
            runChains(family, i, random) :
            runPair(family, i, random);
        long k;
        for ( k = 0; k < results.size(); k++ ) {
            const CaseResult& r = results[k];
            summary.total++;
            summary.byOutcome[(int)r.outcome]++;
            // Random rigid motions put the operands in general
            // position, where touching contacts have probability zero:
            // a non-manifold rejection there hides a broken result.
            // Only the axis-aligned family can really touch.
            bool touchingPossible = family == NON_CONVEX_AXIS_ALIGNED;
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

Fuzz::Shape Fuzz::randomShape(java::Random& random, double scale)
{
    double w = 0.3 + 0.2 * random.nextDouble();
    double h = 0.3 + 0.3 * random.nextDouble();
    int kind = random.nextInt(5);
    java::ArrayList<CompositeBox> positive;
    java::ArrayList<CompositeBox> negative;
    java::String name;

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
            negative.add(CompositeBox(w, w, -0.25, 1 - w, 1 - w,
                h + 0.25));
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

void Fuzz::scaleBoxes(java::ArrayList<CompositeBox>& boxes, double scale)
{
    long i;
    for ( i = 0; i < boxes.size(); i++ ) {
        int k;
        for ( k = 0; k < 6; k++ ) {
            boxes[i][k] *= scale;
        }
    }
}

Matrix4x4d Fuzz::randomRotation(java::Random& random)
{
    // Java evaluates the arguments left to right
    double angle = 0.2 + 2.5 * random.nextDouble();
    double x = random.nextGaussian();
    double y = random.nextGaussian();
    double z = random.nextGaussian();
    return Oracle::rotation(angle, x, y, z);
}

Matrix4x4d Fuzz::randomOffset(java::Random& random, double scale)
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
long long Fuzz::seedOf(Family family)
{
    if ( family == NON_CONVEX_ROTATED_SCALE_1E_MINUS_3 ||
         family == NON_CONVEX_ROTATED_SCALE_1E_PLUS_3 ) {
        return 1000 + (long long)NON_CONVEX_ROTATED;
    }
    return 1000 + (long long)family;
}

double Fuzz::scaleOf(Family family)
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

Fuzz::ShapeList Fuzz::shapesOf(Family family, int index)
{
    java::Random random(seedOf(family));
    ShapeList shapes;
    int i;

    for ( i = 0; i <= index; i++ ) {
        shapes = family == CHAINS ? chainShapes(random) :
            pairShapes(family, random);
    }
    return shapes;
}

Fuzz::ShapeList Fuzz::pairShapes(Family family, java::Random& random)
{
    double scale = scaleOf(family);
    bool rotated = family != NON_CONVEX_AXIS_ALIGNED;
    Shape a = randomShape(random, scale);
    Shape b = randomShape(random, scale);
    if ( family == NON_CONVEX_COPLANAR_BOTTOMS ) {
        return coplanarPair(a, b, random);
    }
    Matrix4x4d motionA = rotated ? randomRotation(random) : Matrix4x4d();
    Matrix4x4d offset = randomOffset(random, scale);
    Matrix4x4d motionB = offset.multiply(
        rotated ? randomRotation(random) : Matrix4x4d());
    ShapeList shapes;
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
Fuzz::ShapeList Fuzz::coplanarPair(const Shape& a, const Shape& b,
    java::Random& random)
{
    Matrix4x4d common = randomRotation(random);
    double align = a.frame.get(2, 3) - b.frame.get(2, 3);
    double x = 0.8 * random.nextDouble() - 0.4;
    double y = 0.8 * random.nextDouble() - 0.4;
    Matrix4x4d move = Oracle::translation(x, y, align);
    double angle = 0.2 + 2.5 * random.nextDouble();
    Matrix4x4d inPlane = move.multiply(Oracle::rotation(angle, 0, 0, 1));
    ShapeList shapes;
    shapes.add(a.moved(common));
    shapes.add(b.moved(common.multiply(inPlane)));
    return shapes;
}

Fuzz::ShapeList Fuzz::chainShapes(java::Random& random)
{
    Matrix4x4d common = randomRotation(random);
    Shape a = randomShape(random, 1.0).moved(common);
    Shape b0 = randomShape(random, 1.0);
    Matrix4x4d offsetB = randomOffset(random, 1.0);
    Shape b = b0.moved(common.multiply(
        offsetB.multiply(randomRotation(random))));
    Shape c0 = randomShape(random, 1.0);
    Matrix4x4d offsetC = randomOffset(random, 1.0);
    Shape c = c0.moved(common.multiply(
        offsetC.multiply(randomRotation(random))));
    ShapeList shapes;
    shapes.add(a);
    shapes.add(b);
    shapes.add(c);
    return shapes;
}

static bool unionOf(const bool* m) { return m[0] || m[1]; }
static bool intersectionOf(const bool* m) { return m[0] && m[1]; }
static bool aMinusB(const bool* m) { return m[0] && !m[1]; }
static bool bMinusA(const bool* m) { return m[1] && !m[0]; }

static PolyhedralBoundedSolid* applyUnion(PolyhedralBoundedSolid** ops)
{
    return Fuzz::setOp(ops[0], ops[1], Modeler::UNION);
}

static PolyhedralBoundedSolid* applyIntersection(PolyhedralBoundedSolid** ops)
{
    return Fuzz::setOp(ops[0], ops[1], Modeler::INTERSECTION);
}

static PolyhedralBoundedSolid* applyAMinusB(PolyhedralBoundedSolid** ops)
{
    return Fuzz::setOp(ops[0], ops[1], Modeler::SUBTRACT);
}

static PolyhedralBoundedSolid* applyBMinusA(PolyhedralBoundedSolid** ops)
{
    return Fuzz::setOp(ops[1], ops[0], Modeler::SUBTRACT);
}

java::ArrayList<Fuzz::CaseResult> Fuzz::runPair(Family family, int index,
    java::Random& random)
{
    double scale = scaleOf(family);
    ShapeList shapes = pairShapes(family, random);
    const Shape& a = shapes[0];
    const Shape& b = shapes[1];
    java::ArrayList<CaseResult> results;
    java::String pairLabel = a.toString() + "/" + b.toString();
    java::String operandProblem;
    bool hasProblem = checkOperands(shapes, &operandProblem);
    double tolerance = 1.0e-6 * scale * scale * scale;

    results.add(evaluate(family, index, pairLabel + " A u B",
        hasProblem, operandProblem, shapes, unionOf, tolerance,
        applyUnion));
    results.add(evaluate(family, index, pairLabel + " A n B",
        hasProblem, operandProblem, shapes, intersectionOf, tolerance,
        applyIntersection));
    results.add(evaluate(family, index, pairLabel + " A - B",
        hasProblem, operandProblem, shapes, aMinusB, tolerance,
        applyAMinusB));
    results.add(evaluate(family, index, pairLabel + " B - A",
        hasProblem, operandProblem, shapes, bMinusA, tolerance,
        applyBMinusA));
    results.add(commutativity(family, index, pairLabel + " u comm",
        hasProblem, operandProblem, shapes, Modeler::UNION));
    results.add(commutativity(family, index, pairLabel + " n comm",
        hasProblem, operandProblem, shapes, Modeler::INTERSECTION));
    return results;
}

//= Chains ==================================================================

/** setOp(first, second, op), releasing the intermediate result `first`. */
static PolyhedralBoundedSolid* thenRelease(PolyhedralBoundedSolid* first,
    PolyhedralBoundedSolid* second, int op, bool intermediateIsFirst)
{
    PolyhedralBoundedSolid* out = nullptr;
    PolyhedralBoundedSolid* intermediate = intermediateIsFirst ?
        first : second;
    try {
        out = Fuzz::setOp(first, second, op);
    }
    catch ( ... ) {
        delete intermediate;
        throw;
    }
    delete intermediate;
    return out;
}

static bool chain0(const bool* m) { return (m[0] || m[1]) && !m[2]; }
static bool chain1(const bool* m) { return (m[0] && m[1]) || m[2]; }
static bool chain2(const bool* m) { return m[0] && !(m[1] || m[2]); }

static PolyhedralBoundedSolid* applyChain0(PolyhedralBoundedSolid** ops)
{
    return thenRelease(Fuzz::setOp(ops[0], ops[1], Modeler::UNION), ops[2],
        Modeler::SUBTRACT, true);
}

static PolyhedralBoundedSolid* applyChain1(PolyhedralBoundedSolid** ops)
{
    return thenRelease(Fuzz::setOp(ops[0], ops[1], Modeler::INTERSECTION),
        ops[2], Modeler::UNION, true);
}

static PolyhedralBoundedSolid* applyChain2(PolyhedralBoundedSolid** ops)
{
    return thenRelease(ops[0], Fuzz::setOp(ops[1], ops[2], Modeler::UNION),
        Modeler::SUBTRACT, false);
}

java::ArrayList<Fuzz::CaseResult> Fuzz::runChains(Family family, int index,
    java::Random& random)
{
    ShapeList shapes = chainShapes(random);
    const Shape& a = shapes[0];
    const Shape& b = shapes[1];
    const Shape& c = shapes[2];
    java::String label = a.toString() + "/" + b.toString() + "/" +
        c.toString() + " ";
    java::String operandProblem;
    bool hasProblem = checkOperands(shapes, &operandProblem);
    java::ArrayList<CaseResult> results;

    results.add(evaluate(family, index, label + EXPRESSIONS[0],
        hasProblem, operandProblem, shapes, chain0, 1.0e-6, applyChain0));
    results.add(evaluate(family, index, label + EXPRESSIONS[1],
        hasProblem, operandProblem, shapes, chain1, 1.0e-6, applyChain1));
    results.add(evaluate(family, index, label + EXPRESSIONS[2],
        hasProblem, operandProblem, shapes, chain2, 1.0e-6, applyChain2));
    return results;
}

//= Evaluation ==============================================================

PolyhedralBoundedSolid* Fuzz::setOp(PolyhedralBoundedSolid* a,
    PolyhedralBoundedSolid* b, int op)
{
    if ( Modeler::isEmptyResult(a) ) {
        return op == Modeler::UNION ? copy(b) : new PolyhedralBoundedSolid();
    }
    if ( Modeler::isEmptyResult(b) ) {
        return op == Modeler::INTERSECTION ?
            new PolyhedralBoundedSolid() : copy(a);
    }
    return Modeler::setOp(a, b, op);
}

PolyhedralBoundedSolid* Fuzz::copy(PolyhedralBoundedSolid* s)
{
    return PolyhedralBoundedSolidStructuralCopy::copy(s);
}

bool Fuzz::checkOperands(const ShapeList& shapes, java::String* problem)
{
    long i;
    for ( i = 0; i < shapes.size(); i++ ) {
        const Shape& s = shapes[i];
        try {
            PolyhedralBoundedSolid* solid = s.toSolid();
            double expected = s.volume();
            double actual = Oracle::solidVolume(solid);
            delete solid;
            if ( std::fabs(actual - expected) >
                 1.0e-6 * std::fmax(1.0e-9, expected) ) {
                *problem = java::String("operand ") + s.toString() +
                    " volume " + java::String::valueOf(actual) +
                    " expected " + java::String::valueOf(expected);
                return true;
            }
        }
        catch ( const java::RuntimeException& e ) {
            *problem = java::String("operand ") + s.toString() + " " +
                throwableSimpleName(e);
            return true;
        }
    }
    return false;
}

PolyhedralBoundedSolid** Fuzz::build(const ShapeList& shapes)
{
    PolyhedralBoundedSolid** solids =
        new PolyhedralBoundedSolid*[shapes.size()];
    long i;
    for ( i = 0; i < shapes.size(); i++ ) {
        solids[i] = nullptr;
    }
    try {
        for ( i = 0; i < shapes.size(); i++ ) {
            solids[i] = shapes[i].toSolid();
        }
    }
    catch ( ... ) {
        release(solids, shapes.size());
        throw;
    }
    return solids;
}

void Fuzz::release(PolyhedralBoundedSolid** solids, long count)
{
    long i;
    for ( i = 0; i < count; i++ ) {
        delete solids[i];
    }
    delete[] solids;
}

Fuzz::CaseResult Fuzz::evaluate(Family family, int index,
    const java::String& label, bool hasOperandProblem,
    const java::String& operandProblem, const ShapeList& shapes,
    CompositeShapeOracle::Membership membership, double tolerance,
    Expression expression)
{
    CaseResult r = result(family, index, label);
    double expected;

    if ( hasOperandProblem ) {
        r.outcome = EXCEPTION;
        r.hasDetail = true;
        r.detail = operandProblem;
        return r;
    }
    expected = CompositeShapeOracle::volume(shapes, membership);
    try {
        PolyhedralBoundedSolid** operands = build(shapes);
        PolyhedralBoundedSolid* out = nullptr;
        try {
            out = expression(operands);
        }
        catch ( ... ) {
            release(operands, shapes.size());
            throw;
        }
        // C++ ownership: the operands are released (Java: garbage collector)
        release(operands, shapes.size());
        double actual = Oracle::solidVolume(out);
        java::String report;
        if ( out->getPolygonsList().size() > 0 &&
             !PolyhedralBoundedSolidValidationEngine::validateStrict(out,
                 &report) ) {
            r.outcome = STRICT_REJECTED;
            r.hasDetail = true;
            r.detail = firstLine(report);
        }
        else if ( std::fabs(actual - expected) >
                  std::fmax(tolerance, 1.0e-6 * expected) ) {
            r.outcome = WRONG_VOLUME;
            r.hasDetail = true;
            r.detail = java::String("volume ") +
                java::String::valueOf(actual) + " expected " +
                java::String::valueOf(expected);
        }
        else {
            r.outcome = OK;
        }
        delete out;
    }
    catch ( const NonManifoldBooleanResultException& e ) {
        r.outcome = REJECTED_NON_MANIFOLD;
        r.hasDetail = true;
        r.detail = firstLine(e.getMessage());
    }
    catch ( const java::RuntimeException& e ) {
        r.outcome = EXCEPTION;
        r.hasDetail = true;
        r.detail = throwableSimpleName(e) + ": " + firstLine(e.getMessage());
    }
    return r;
}

Fuzz::CaseResult Fuzz::commutativity(Family family, int index,
    const java::String& label, bool hasOperandProblem,
    const java::String& operandProblem, const ShapeList& shapes, int op)
{
    CaseResult r = result(family, index, label);

    if ( hasOperandProblem ) {
        r.outcome = EXCEPTION;
        r.hasDetail = true;
        r.detail = operandProblem;
        return r;
    }
    PolyhedralBoundedSolid** ab = nullptr;
    PolyhedralBoundedSolid** ba = nullptr;
    try {
        ab = build(shapes);
        ba = build(shapes);
        PolyhedralBoundedSolid* firstResult = setOp(ab[0], ab[1], op);
        java::String first = describe(firstResult);
        delete firstResult;
        PolyhedralBoundedSolid* secondResult = setOp(ba[1], ba[0], op);
        java::String second = describe(secondResult);
        delete secondResult;
        r.outcome = first == second ? OK : NOT_COMMUTATIVE;
        if ( r.outcome != OK ) {
            r.hasDetail = true;
            r.detail = first + " vs " + second;
        }
    }
    catch ( const NonManifoldBooleanResultException& e ) {
        r.outcome = REJECTED_NON_MANIFOLD;
    }
    catch ( const java::RuntimeException& e ) {
        r.outcome = EXCEPTION;
        r.hasDetail = true;
        r.detail = throwableSimpleName(e) + ": " + firstLine(e.getMessage());
    }
    // C++ ownership: the operands are released (Java: garbage collector)
    if ( ab != nullptr ) {
        release(ab, shapes.size());
    }
    if ( ba != nullptr ) {
        release(ba, shapes.size());
    }
    return r;
}

java::String Fuzz::describe(PolyhedralBoundedSolid* solid)
{
    PolyhedralBoundedSolidTopologySummary t =
        PolyhedralBoundedSolidTopologySummary::from(solid);
    // C `%#.9g` keeps the trailing zeros, as Java `%.9g`
    return java::String("F=") + java::String::valueOf(t.getFaceCount()) +
        " E=" + java::String::valueOf(t.getEdgeCount()) + " V=" +
        java::String::valueOf(t.getVertexCount()) + " S=" +
        java::String::valueOf(t.getShellCount()) + " vol=" +
        java::String::format("%#.9g", Oracle::solidVolume(solid));
}

Fuzz::CaseResult Fuzz::result(Family family, int index,
    const java::String& label)
{
    CaseResult r;
    r.family = family;
    r.index = index;
    r.label = label;
    return r;
}

java::String Fuzz::firstLine(const java::String& text)
{
    int cut = text.indexOf('\n');
    java::String line = cut < 0 ? text : text.substring(0, cut);
    return line.length() > 200 ? line.substring(0, 200) : line;
}
