#include <cmath>

#include "BooleanOracleFuzz.h"
#include "ThrowableSimpleName.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/NonManifoldBooleanResultException.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

const int BooleanOracleFuzz::OPERATIONS[4] = {
    PolyhedralBoundedSolidModeler::UNION,
    PolyhedralBoundedSolidModeler::INTERSECTION,
    PolyhedralBoundedSolidModeler::SUBTRACT,
    BooleanOracleFuzz::B_MINUS_A };

const BooleanOracleFuzz::Family BooleanOracleFuzz::FAMILIES[FAMILY_COUNT] = {
    AXIS_ALIGNED_GENERAL_POSITION, B_ROTATED_ABOUT_Z,
    B_ROTATED_ABOUT_RANDOM_AXIS, RANDOM_AXIS_TRANSLATED_TO_1000,
    INTEGER_GRID, COPLANAR_TOP_BOTTOM_ROTATED_45 };

int BooleanOracleFuzz::pairsOf(Family family)
{
    return family == INTEGER_GRID ? 80 : 12;
}

const char* BooleanOracleFuzz::familyName(Family family)
{
    static const char* NAMES[FAMILY_COUNT] = {
        "AXIS_ALIGNED_GENERAL_POSITION", "B_ROTATED_ABOUT_Z",
        "B_ROTATED_ABOUT_RANDOM_AXIS", "RANDOM_AXIS_TRANSLATED_TO_1000",
        "INTEGER_GRID", "COPLANAR_TOP_BOTTOM_ROTATED_45" };
    return NAMES[(int)family];
}

const char* BooleanOracleFuzz::outcomeName(Outcome outcome)
{
    static const char* NAMES[OUTCOME_COUNT] = {
        "OK", "REJECTED_NON_MANIFOLD", "WRONG_VOLUME", "STRICT_REJECTED",
        "EXCEPTION" };
    return NAMES[(int)outcome];
}

BooleanOracleFuzz::CaseResult::CaseResult() :
    family(AXIS_ALIGNED_GENERAL_POSITION), pairIndex(0), operation(0),
    outcome(OK)
{
}

BooleanOracleFuzz::CaseResult::CaseResult(Family family, int pairIndex,
    int op, Outcome outcome, const java::String& path,
    const java::String& detail) :
    family(family), pairIndex(pairIndex), operation(op), outcome(outcome),
    path(path), detail(detail)
{
}

bool BooleanOracleFuzz::CaseResult::operator==(const CaseResult& other) const
{
    return this == &other;
}

java::String BooleanOracleFuzz::CaseResult::toString() const
{
    return java::String(familyName(family)) + "#" +
        java::String::valueOf(pairIndex) + " " + opName(operation) + " " +
        outcomeName(outcome) + " path=" + path + " " + detail;
}

BooleanOracleFuzz::FamilySummary::FamilySummary() :
    total(0), correct(0), okByRejection(0)
{
    int i;
    for ( i = 0; i < OUTCOME_COUNT; i++ ) {
        byOutcome[i] = 0;
    }
}

java::String BooleanOracleFuzz::FamilySummary::byOutcomeText() const
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

BooleanOracleFuzz::BoxPair::BoxPair()
{
}

BooleanOracleFuzz::BoxPair::BoxPair(const OrientedBox& a,
    const OrientedBox& b)
{
    v[0] = a;
    v[1] = b;
}

const BooleanOracleFuzz::OrientedBox& BooleanOracleFuzz::BoxPair::operator[](
    int i) const
{
    return v[i];
}

bool BooleanOracleFuzz::BoxPair::operator==(const BoxPair& other) const
{
    return this == &other;
}

java::String BooleanOracleFuzz::opName(int op)
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

java::ArrayList<BooleanOracleFuzz::BoxPair> BooleanOracleFuzz::pairs(
    Family family)
{
    // The translated family replays the random-axis family's seed, so
    // its cases are exactly those pairs moved to (1000, 1000, 1000).
    Family seedFamily = family == RANDOM_AXIS_TRANSLATED_TO_1000 ?
        B_ROTATED_ABOUT_RANDOM_AXIS : family;
    java::Random random(20261114LL + (long long)seedFamily * 7919LL);
    java::ArrayList<BoxPair> result;
    int i;

    for ( i = 0; i < pairsOf(family); i++ ) {
        result.add(pair(family, random));
    }
    return result;
}

double BooleanOracleFuzz::uniform(java::Random& random, double lo, double hi)
{
    return lo + (hi - lo) * random.nextDouble();
}

Matrix4x4d BooleanOracleFuzz::randomAxisRotation(java::Random& random)
{
    double x;
    double y;
    double z;
    double length;

    do {
        x = uniform(random, -1, 1);
        y = uniform(random, -1, 1);
        z = uniform(random, -1, 1);
        length = std::sqrt(x * x + y * y + z * z);
    } while ( length < 0.2 || length > 1.0 );
    // Java evaluates the arguments left to right
    double angle = uniform(random, 0.2, 2.9);
    return Oracle::rotation(angle, x / length, y / length, z / length);
}

BooleanOracleFuzz::BoxPair BooleanOracleFuzz::pair(Family family,
    java::Random& random)
{
    OrientedBox a;
    OrientedBox b;
    Matrix4x4d place;
    // C++ port note: Java evaluates arguments left to right; each random
    // draw is named in that order, since C++ leaves the order unspecified.
    switch ( family ) {
      case AXIS_ALIGNED_GENERAL_POSITION: {
        double ax = uniform(random, 0.8, 2.0);
        double ay = uniform(random, 0.8, 2.0);
        double az = uniform(random, 0.8, 2.0);
        a = OrientedBox(ax, ay, az, Oracle::translation(0, 0, 0));
        double bx = uniform(random, 0.5, 1.8);
        double by = uniform(random, 0.5, 1.8);
        double bz = uniform(random, 0.5, 1.8);
        double tx = uniform(random, -0.7, 0.7);
        double ty = uniform(random, -0.7, 0.7);
        double tz = uniform(random, -0.7, 0.7);
        b = OrientedBox(bx, by, bz, Oracle::translation(tx, ty, tz));
        return BoxPair(a, b);
      }
      case B_ROTATED_ABOUT_Z: {
        double ax = uniform(random, 0.8, 2.0);
        double ay = uniform(random, 0.8, 2.0);
        double az = uniform(random, 0.8, 2.0);
        a = OrientedBox(ax, ay, az, Oracle::translation(0, 0, 0));
        double bx = uniform(random, 0.5, 1.8);
        double by = uniform(random, 0.5, 1.8);
        double bz = uniform(random, 0.5, 1.8);
        double tx = uniform(random, -0.7, 0.7);
        double ty = uniform(random, -0.7, 0.7);
        double tz = uniform(random, -0.7, 0.7);
        double angle = uniform(random, 0.2, 1.4);
        b = OrientedBox(bx, by, bz, Oracle::translation(tx, ty, tz)
            .multiply(Oracle::rotation(angle, 0, 0, 1)));
        return BoxPair(a, b);
      }
      case B_ROTATED_ABOUT_RANDOM_AXIS:
      case RANDOM_AXIS_TRANSLATED_TO_1000: {
        double offset = family == RANDOM_AXIS_TRANSLATED_TO_1000 ? 1000 : 0;
        double ax = uniform(random, 0.8, 2.0);
        double ay = uniform(random, 0.8, 2.0);
        double az = uniform(random, 0.8, 2.0);
        a = OrientedBox(ax, ay, az,
            Oracle::translation(offset, offset, offset));
        double bx = uniform(random, 0.5, 1.8);
        double by = uniform(random, 0.5, 1.8);
        double bz = uniform(random, 0.5, 1.8);
        double tx = offset + uniform(random, -0.7, 0.7);
        double ty = offset + uniform(random, -0.7, 0.7);
        double tz = offset + uniform(random, -0.7, 0.7);
        Matrix4x4d move = Oracle::translation(tx, ty, tz);
        b = OrientedBox(bx, by, bz,
            move.multiply(randomAxisRotation(random)));
        return BoxPair(a, b);
      }
      case INTEGER_GRID:
        a = gridBox(random);
        b = gridBox(random);
        return BoxPair(a, b);
      case COPLANAR_TOP_BOTTOM_ROTATED_45:
      default: {
        a = OrientedBox(2, 2, 1, Oracle::translation(0, 0, 0));
        double side = uniform(random, 0.4, 1.2);
        double tx = uniform(random, -1.0, 1.0);
        double ty = uniform(random, -1.0, 1.0);
        place = Oracle::translation(tx, ty, 0)
            .multiply(Oracle::rotation(M_PI / 4.0, 0, 0, 1));
        b = OrientedBox(side, side, 1, place);
        return BoxPair(a, b);
      }
    }
}

BooleanOracleFuzz::OrientedBox BooleanOracleFuzz::gridBox(
    java::Random& random)
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

static java::String pathText()
{
    const java::String* path = PolyhedralBoundedSolidModeler
        ::getLastSetOpResultPath();
    return path == nullptr ? java::String("null") : *path;
}

BooleanOracleFuzz::CaseResult BooleanOracleFuzz::run(Family family,
    int pairIndex, const BoxPair& pair, int op)
{
    const OrientedBox& first = op == B_MINUS_A ? pair[1] : pair[0];
    const OrientedBox& second = op == B_MINUS_A ? pair[0] : pair[1];
    int kernelOp = op == B_MINUS_A ? (int)Modeler::SUBTRACT : op;
    double expected = Oracle::expectedVolume(first, second, kernelOp);
    double tolerance = 1.0e-6 *
        std::fmax(1.0, pair[0].volume() + pair[1].volume());
    PolyhedralBoundedSolid* result;
    java::String path;
    PolyhedralBoundedSolid* firstSolid = first.toSolid();
    PolyhedralBoundedSolid* secondSolid = second.toSolid();

    try {
        result = Modeler::setOp(firstSolid, secondSolid, kernelOp, false,
            true, false);
    }
    catch ( const NonManifoldBooleanResultException& t ) {
        delete firstSolid;
        delete secondSolid;
        double overlap = Oracle::expectedVolume(first, second,
            Modeler::INTERSECTION);
        Outcome outcome = kernelOp == Modeler::UNION &&
            overlap <= tolerance ?
            REJECTED_NON_MANIFOLD : EXCEPTION;
        return CaseResult(family, pairIndex, op, outcome, pathText(),
            java::String("NonManifoldBooleanResultException, overlap=") +
            java::String::valueOf(overlap));
    }
    catch ( const java::Throwable& t ) {
        delete firstSolid;
        delete secondSolid;
        return CaseResult(family, pairIndex, op, EXCEPTION, pathText(),
            throwableSimpleName(t) + ": " + firstLine(t.getMessage()));
    }
    // C++ ownership: the operands are released (Java: garbage collector)
    delete firstSolid;
    delete secondSolid;
    path = pathText();
    java::String strict;
    bool strictValid;
    try {
        strictValid = PolyhedralBoundedSolidValidationEngine::validateStrict(
            result, &strict);
    }
    catch ( const java::Throwable& t ) {
        delete result;
        return CaseResult(family, pairIndex, op, EXCEPTION, path,
            java::String("validateStrict: ") + throwableSimpleName(t));
    }
    double volume = Oracle::solidVolume(result);
    delete result;
    java::String detail = java::String::format("volume=%.6f expected=%.6f",
        volume, expected);
    if ( std::fabs(volume - expected) > tolerance ) {
        return CaseResult(family, pairIndex, op, WRONG_VOLUME, path,
            detail + (strictValid ? " (silent)" : " (strict fail)"));
    }
    if ( !strictValid ) {
        return CaseResult(family, pairIndex, op, STRICT_REJECTED, path,
            detail + " " + firstLine(strict));
    }
    return CaseResult(family, pairIndex, op, OK, path, detail);
}

java::String BooleanOracleFuzz::firstLine(const java::String& s)
{
    int i = s.indexOf('\n');
    return i < 0 ? s : s.substring(0, i);
}

BooleanOracleFuzz::FamilySummary BooleanOracleFuzz::runFamily(Family family)
{
    FamilySummary summary;
    java::ArrayList<BoxPair> pairList = pairs(family);
    long i;
    int k;

    for ( i = 0; i < pairList.size(); i++ ) {
        for ( k = 0; k < 4; k++ ) {
            int op = OPERATIONS[k];
            CaseResult r = run(family, (int)i, pairList[i], op);
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
