#include <math.h>
#include <stdio.h>

#include "BooleanOracleFuzz.h"

#include "PolyhedralTestFailures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;

const int BooleanOracleFuzz::OPERATIONS[BooleanOracleFuzz::OPERATION_COUNT] = {
    PolyhedralBoundedSolidModeler::UNION,
    PolyhedralBoundedSolidModeler::INTERSECTION,
    PolyhedralBoundedSolidModeler::SUBTRACT,
    BooleanOracleFuzz::B_MINUS_A };

int BooleanOracleFuzz::pairsOf(Family family)
{
    if ( family == INTEGER_GRID ) {
        return 80;
    }
    return 12;
}

const char* BooleanOracleFuzz::familyName(Family family)
{
    static const char* const NAMES[] = {
        "AXIS_ALIGNED_GENERAL_POSITION", "B_ROTATED_ABOUT_Z",
        "B_ROTATED_ABOUT_RANDOM_AXIS", "RANDOM_AXIS_TRANSLATED_TO_1000",
        "INTEGER_GRID", "COPLANAR_TOP_BOTTOM_ROTATED_45" };
    return NAMES[(int)family];
}

const char* BooleanOracleFuzz::outcomeName(Outcome outcome)
{
    static const char* const NAMES[] = {
        "OK", "REJECTED_NON_MANIFOLD", "WRONG_VOLUME", "STRICT_REJECTED",
        "EXCEPTION" };
    return NAMES[(int)outcome];
}

BooleanOracleFuzz::CaseResult::CaseResult() :
    family(AXIS_ALIGNED_GENERAL_POSITION), pairIndex(0), operation(0),
    outcome(OK), hasPath(FALSE)
{
}

BooleanOracleFuzz::CaseResult::CaseResult(Family inFamily, int inPairIndex,
    int op, Outcome inOutcome, const String* inPath, const String& inDetail) :
    family(inFamily), pairIndex(inPairIndex), operation(op),
    outcome(inOutcome), hasPath(inPath != NULL), detail(inDetail)
{
    if ( inPath != NULL ) {
        path = *inPath;
    }
}

String BooleanOracleFuzz::CaseResult::toString() const
{
    char number[32];
    String pathText = "null";

    if ( hasPath ) {
        pathText = path;
    }
    sprintf(number, "%d", pairIndex);
    return String(familyName(family)) + "#" + number + " " +
        opName(operation) + " " + outcomeName(outcome) + " path=" +
        pathText + " " + detail;
}

BOOLEAN BooleanOracleFuzz::CaseResult::operator==(
    const CaseResult& other) const
{
    return this == &other;
}

BooleanOracleFuzz::FamilySummary::FamilySummary() :
    total(0), correct(0), okByRejection(0)
{
    int i;

    for ( i = 0; i < OUTCOME_COUNT; i++ ) {
        byOutcome[i] = 0;
    }
}

String BooleanOracleFuzz::FamilySummary::byOutcomeText() const
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

BooleanOracleFuzz::BoxPair::BoxPair()
{
}

BooleanOracleFuzz::BoxPair::BoxPair(const OrientedBox& inA,
    const OrientedBox& inB) : firstBox(inA), secondBox(inB)
{
}

BOOLEAN BooleanOracleFuzz::BoxPair::operator==(const BoxPair& other) const
{
    return this == &other;
}

String BooleanOracleFuzz::opName(int op)
{
    if ( op == Modeler::UNION ) {
        return "UNION";
    }
    if ( op == Modeler::INTERSECTION ) {
        return "INTERSECTION";
    }
    if ( op == Modeler::SUBTRACT ) {
        return "A-B";
    }
    return "B-A";
}

ArrayList<BooleanOracleFuzz::BoxPair> BooleanOracleFuzz::pairs(Family family)
{
    // The translated family replays the random-axis family's seed, so
    // its cases are exactly those pairs moved to (1000, 1000, 1000).
    Family seedFamily = family;
    if ( family == RANDOM_AXIS_TRANSLATED_TO_1000 ) {
        seedFamily = B_ROTATED_ABOUT_RANDOM_AXIS;
    }
    Random random(VSDK_LL(20261114) + (long long)seedFamily * VSDK_LL(7919));
    ArrayList<BoxPair> result;
    int i;

    for ( i = 0; i < pairsOf(family); i++ ) {
        result.add(pair(family, random));
    }
    return result;
}

double BooleanOracleFuzz::uniform(Random& random, double lo, double hi)
{
    return lo + (hi - lo) * random.nextDouble();
}

Matrix4x4d BooleanOracleFuzz::randomAxisRotation(Random& random)
{
    double x;
    double y;
    double z;
    double length;

    do {
        x = uniform(random, -1, 1);
        y = uniform(random, -1, 1);
        z = uniform(random, -1, 1);
        length = sqrt(x * x + y * y + z * z);
    } while ( length < 0.2 || length > 1.0 );
    double angle = uniform(random, 0.2, 2.9);
    return Oracle::rotation(angle, x / length, y / length, z / length);
}

/**
C++ port note: Java evaluates arguments from left to right, so each random
draw is made into a variable first, in Java's order.
*/
BooleanOracleFuzz::BoxPair BooleanOracleFuzz::pair(Family family,
    Random& random)
{
    double s0;
    double s1;
    double s2;
    double t0;
    double t1;
    double t2;

    switch ( family ) {
      case AXIS_ALIGNED_GENERAL_POSITION: {
        s0 = uniform(random, 0.8, 2.0);
        s1 = uniform(random, 0.8, 2.0);
        s2 = uniform(random, 0.8, 2.0);
        OrientedBox a(s0, s1, s2, Oracle::translation(0, 0, 0));
        s0 = uniform(random, 0.5, 1.8);
        s1 = uniform(random, 0.5, 1.8);
        s2 = uniform(random, 0.5, 1.8);
        t0 = uniform(random, -0.7, 0.7);
        t1 = uniform(random, -0.7, 0.7);
        t2 = uniform(random, -0.7, 0.7);
        OrientedBox b(s0, s1, s2, Oracle::translation(t0, t1, t2));
        return BoxPair(a, b);
      }
      case B_ROTATED_ABOUT_Z: {
        s0 = uniform(random, 0.8, 2.0);
        s1 = uniform(random, 0.8, 2.0);
        s2 = uniform(random, 0.8, 2.0);
        OrientedBox a(s0, s1, s2, Oracle::translation(0, 0, 0));
        s0 = uniform(random, 0.5, 1.8);
        s1 = uniform(random, 0.5, 1.8);
        s2 = uniform(random, 0.5, 1.8);
        t0 = uniform(random, -0.7, 0.7);
        t1 = uniform(random, -0.7, 0.7);
        t2 = uniform(random, -0.7, 0.7);
        double angle = uniform(random, 0.2, 1.4);
        OrientedBox b(s0, s1, s2, Oracle::translation(t0, t1, t2)
            .multiply(Oracle::rotation(angle, 0, 0, 1)));
        return BoxPair(a, b);
      }
      case B_ROTATED_ABOUT_RANDOM_AXIS:
      case RANDOM_AXIS_TRANSLATED_TO_1000: {
        double offset = 0;
        if ( family == RANDOM_AXIS_TRANSLATED_TO_1000 ) {
            offset = 1000;
        }
        s0 = uniform(random, 0.8, 2.0);
        s1 = uniform(random, 0.8, 2.0);
        s2 = uniform(random, 0.8, 2.0);
        OrientedBox a(s0, s1, s2,
            Oracle::translation(offset, offset, offset));
        s0 = uniform(random, 0.5, 1.8);
        s1 = uniform(random, 0.5, 1.8);
        s2 = uniform(random, 0.5, 1.8);
        t0 = offset + uniform(random, -0.7, 0.7);
        t1 = offset + uniform(random, -0.7, 0.7);
        t2 = offset + uniform(random, -0.7, 0.7);
        Matrix4x4d rotation = randomAxisRotation(random);
        OrientedBox b(s0, s1, s2,
            Oracle::translation(t0, t1, t2).multiply(rotation));
        return BoxPair(a, b);
      }
      case INTEGER_GRID: {
        OrientedBox a = gridBox(random);
        OrientedBox b = gridBox(random);
        return BoxPair(a, b);
      }
      case COPLANAR_TOP_BOTTOM_ROTATED_45:
      default:
        break;
    }
    OrientedBox a(2, 2, 1, Oracle::translation(0, 0, 0));
    double side = uniform(random, 0.4, 1.2);
    t0 = uniform(random, -1.0, 1.0);
    t1 = uniform(random, -1.0, 1.0);
    Matrix4x4d place = Oracle::translation(t0, t1, 0)
        .multiply(Oracle::rotation(M_PI / 4.0, 0, 0, 1));
    OrientedBox b(side, side, 1, place);
    return BoxPair(a, b);
}

/** Box with integer corners in [0, 3]^3: coplanar faces, shared edges. */
OrientedBox BooleanOracleFuzz::gridBox(Random& random)
{
    int lo[3];
    int size[3];
    int k;

    for ( k = 0; k < 3; k++ ) {
        size[k] = 1 + random.nextInt(2);
        lo[k] = random.nextInt(4 - size[k]);
    }
    return OrientedBox(size[0], size[1], size[2],
        Oracle::translation(lo[0] + size[0] / 2.0, lo[1] + size[1] / 2.0,
            lo[2] + size[2] / 2.0));
}

BooleanOracleFuzz::CaseResult BooleanOracleFuzz::run(Family family,
    int pairIndex, const BoxPair& pair, int op)
{
    const OrientedBox& first = op == B_MINUS_A ? pair.secondBox : pair.firstBox;
    const OrientedBox& second = op == B_MINUS_A ? pair.firstBox : pair.secondBox;
    int kernelOp = op == B_MINUS_A ? (int)Modeler::SUBTRACT : op;
    double expected = Oracle::expectedVolume(first, second, kernelOp);
    double sum = pair.firstBox.volume() + pair.secondBox.volume();
    double tolerance = 1.0e-6 * (sum > 1.0 ? sum : 1.0);
    PolyhedralBoundedSolid* result = NULL;
    // C++ port note: Java leaves the operands to the garbage collector
    PolyhedralBoundedSolid* solidA = first.toSolid();
    PolyhedralBoundedSolid* solidB = second.toSolid();
    String failureName;
    String failureMessage;
    BOOLEAN nonManifold = FALSE;
    BOOLEAN failed = FALSE;
    char text[160];

    try {
        result = Modeler::setOp(solidA, solidB, kernelOp, FALSE, TRUE,
            FALSE);
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( const NonManifoldBooleanResultException& ) {
        nonManifold = TRUE;
    }
#endif
    PBS_CATCH_THROWABLE_REST(failureName, failureMessage)
    if ( result == NULL ) {
        failed = TRUE;
    }
    delete solidA;
    delete solidB;
    if ( nonManifold ) {
        double overlap = Oracle::expectedVolume(first, second,
            Modeler::INTERSECTION);
        Outcome outcome = EXCEPTION;
        if ( kernelOp == Modeler::UNION && overlap <= tolerance ) {
            outcome = REJECTED_NON_MANIFOLD;
        }
        sprintf(text, "%.17g", overlap);
        return CaseResult(family, pairIndex, op, outcome,
            Modeler::getLastSetOpResultPath(),
            String("NonManifoldBooleanResultException, overlap=") + text);
    }
    if ( failed ) {
        return CaseResult(family, pairIndex, op, EXCEPTION,
            Modeler::getLastSetOpResultPath(),
            failureName + ": " + firstLine(failureMessage));
    }
    BOOLEAN hasPath = Modeler::getLastSetOpResultPath() != NULL;
    String path;
    if ( hasPath ) {
        path = *Modeler::getLastSetOpResultPath();
    }

    String strict;
    BOOLEAN strictValid = FALSE;
    failed = TRUE;
    try {
        strictValid = PolyhedralBoundedSolidValidationEngine::validateStrict(
            result, &strict);
        failed = FALSE;
    }
    PBS_CATCH_THROWABLE(failureName, failureMessage)
    if ( failed ) {
        delete result;
        return CaseResult(family, pairIndex, op, EXCEPTION,
            hasPath ? &path : (const String*)NULL,
            String("validateStrict: ") + failureName);
    }
    double volume = Oracle::solidVolume(result);
    delete result;
    sprintf(text, "volume=%.6f expected=%.6f", volume, expected);
    String detail = text;
    if ( fabs(volume - expected) > tolerance ) {
        String suffix = " (strict fail)";
        if ( strictValid ) {
            suffix = " (silent)";
        }
        return CaseResult(family, pairIndex, op, WRONG_VOLUME,
            hasPath ? &path : (const String*)NULL, detail + suffix);
    }
    if ( !strictValid ) {
        return CaseResult(family, pairIndex, op, STRICT_REJECTED,
            hasPath ? &path : (const String*)NULL,
            detail + " " + firstLine(strict));
    }
    return CaseResult(family, pairIndex, op, OK,
        hasPath ? &path : (const String*)NULL, detail);
}

String BooleanOracleFuzz::firstLine(const String& s)
{
    int i = s.indexOf('\n');

    if ( i < 0 ) {
        return s;
    }
    return s.substring(0, i);
}

BooleanOracleFuzz::FamilySummary BooleanOracleFuzz::runFamily(Family family)
{
    FamilySummary summary;
    ArrayList<BoxPair> all = pairs(family);
    long i;
    int k;

    for ( i = 0; i < all.size(); i++ ) {
        for ( k = 0; k < OPERATION_COUNT; k++ ) {
            CaseResult r = run(family, (int)i, all[i], OPERATIONS[k]);
            summary.total++;
            summary.byOutcome[(int)r.outcome]++;
            if ( r.outcome == REJECTED_NON_MANIFOLD ) {
                summary.correct++;
                summary.okByRejection++;
            }
            else if ( r.outcome == OK ) {
                summary.correct++;
            }
            else {
                summary.failures.add(r);
            }
        }
    }
    return summary;
}
