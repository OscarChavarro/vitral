#include <cmath>

#include "BooleanVolumeIdentityFuzz.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "ThrowableSimpleName.h"

#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef BooleanVolumeIdentityFuzz Fuzz;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;

const double Fuzz::TOLERANCE = 1.0e-9;

const Fuzz::Family Fuzz::FAMILIES[FAMILY_COUNT] = {
    CONVEX_PRISMS, STAR_AND_CONVEX, STAR_PRISMS };

const char* Fuzz::familyName(Family family)
{
    static const char* NAMES[FAMILY_COUNT] = {
        "CONVEX_PRISMS", "STAR_AND_CONVEX", "STAR_PRISMS" };
    return NAMES[(int)family];
}

Fuzz::Summary::Summary() : total(0), correct(0)
{
}

java::String Fuzz::Summary::byOutcomeText() const
{
    java::String text("{");
    long i;
    for ( i = 0; i < byOutcome.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += byOutcome.getKey(i) + "=" +
            java::String::valueOf(byOutcome.getValue(i));
    }
    return text + "}";
}

/** Java `text.split(" ")[0]`. */
static java::String firstWord(const java::String& text)
{
    int i = text.indexOf(' ');
    return i < 0 ? text : text.substring(0, i);
}

Fuzz::Summary Fuzz::runFamily(Family family)
{
    Summary summary;
    java::Random random(4201 + (long long)family);
    int i;

    for ( i = 0; i < CASES_PER_FAMILY; i++ ) {
        PrismPolygon baseA = base(family, 0, random);
        PrismPolygon baseB = base(family, 1, random);
        double heightA = 0.8 + 0.6 * random.nextDouble();
        double heightB = 0.8 + 0.6 * random.nextDouble();
        Matrix4x4d place = placement(random);
        java::String outcome = check(baseA, heightA, baseB, heightB, place);
        summary.total++;
        java::String key = firstWord(outcome);
        summary.byOutcome.put(key, summary.byOutcome.getOrDefault(key, 0) + 1);
        if ( outcome == "OK" ) {
            summary.correct++;
        }
        else {
            summary.failures.add(java::String(familyName(family)) + " #" +
                java::String::valueOf(i) + " " + outcome);
        }
    }
    return summary;
}

PrismPolygon Fuzz::base(Family family, int which, java::Random& random)
{
    bool star = family == STAR_PRISMS ||
        (family == STAR_AND_CONVEX && which == 0);
    if ( star ) {
        int points = 4 + random.nextInt(4);
        double outer = 0.9 + 0.3 * random.nextDouble();
        double inner = 0.35 + 0.15 * random.nextDouble();
        return PrismFixtures::starPolygon(points, outer, inner);
    }
    int sides = 5 + random.nextInt(20);
    double radius = 0.7 + 0.4 * random.nextDouble();
    return PrismFixtures::regularPolygon(sides, radius);
}

Matrix4x4d Fuzz::placement(java::Random& random)
{
    double x = random.nextGaussian();
    double y = random.nextGaussian();
    double z = random.nextGaussian();
    double angle = 0.2 + 1.2 * random.nextDouble();
    double tx = 0.6 * (random.nextDouble() - 0.5);
    double ty = 0.6 * (random.nextDouble() - 0.5);
    double tz = 0.5 * (random.nextDouble() - 0.5) + 0.3;
    return Oracle::translation(tx, ty, tz)
        .multiply(Oracle::rotation(angle, x, y, z));
}

PolyhedralBoundedSolid* Fuzz::a(const PrismPolygon& base, double height)
{
    return PrismFixtures::prism(base, height);
}

PolyhedralBoundedSolid* Fuzz::b(const PrismPolygon& base, double height,
    const Matrix4x4d& placement)
{
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(base, height);
    Modeler::applyTransformation(solid, placement);
    return solid;
}

double Fuzz::volumeOf(PolyhedralBoundedSolid* first,
    PolyhedralBoundedSolid* second, int op)
{
    PolyhedralBoundedSolid* result = nullptr;
    try {
        result = Modeler::setOp(first, second, op);
    }
    catch ( ... ) {
        delete first;
        delete second;
        throw;
    }
    // C++ ownership: operands and result are released (Java: garbage
    // collector)
    delete first;
    delete second;
    double volume = Oracle::solidVolume(result);
    delete result;
    return volume;
}

java::String Fuzz::check(const PrismPolygon& baseA, double heightA,
    const PrismPolygon& baseB, double heightB, const Matrix4x4d& placement)
{
    PolyhedralBoundedSolid* operandA = a(baseA, heightA);
    double volumeA = Oracle::solidVolume(operandA);
    delete operandA;
    PolyhedralBoundedSolid* operandB = b(baseB, heightB, placement);
    double volumeB = Oracle::solidVolume(operandB);
    delete operandB;
    double scale = std::fmax(1.0, volumeA + volumeB);
    double unionVolume;
    double intersection;
    double aMinusB;
    double bMinusA;

    if ( std::fabs(volumeA - PrismFixtures::polygonArea(baseA) * heightA) >
             TOLERANCE * scale ||
         std::fabs(volumeB - PrismFixtures::polygonArea(baseB) * heightB) >
             TOLERANCE * scale ) {
        return "OPERAND volume differs from base area x height";
    }
    try {
        PolyhedralBoundedSolid* first = a(baseA, heightA);
        unionVolume = volumeOf(first, b(baseB, heightB, placement),
            Modeler::UNION);
        first = a(baseA, heightA);
        intersection = volumeOf(first, b(baseB, heightB, placement),
            Modeler::INTERSECTION);
        first = a(baseA, heightA);
        aMinusB = volumeOf(first, b(baseB, heightB, placement),
            Modeler::SUBTRACT);
        first = b(baseB, heightB, placement);
        bMinusA = volumeOf(first, a(baseA, heightA), Modeler::SUBTRACT);
    }
    catch ( const PolyhedralBoundedSolidOperationException& e ) {
        return java::String("EXCEPTION ") + throwableSimpleName(e) + ": " +
            firstLine(e.getMessage());
    }
    if ( intersection < -TOLERANCE * scale ||
         intersection > std::fmin(volumeA, volumeB) + TOLERANCE * scale ) {
        return java::String("RANGE |AnB|=") +
            java::String::valueOf(intersection) + " |A|=" +
            java::String::valueOf(volumeA) + " |B|=" +
            java::String::valueOf(volumeB);
    }
    double errorUnion = unionVolume - (volumeA + volumeB - intersection);
    double errorAB = aMinusB - (volumeA - intersection);
    double errorBA = bMinusA - (volumeB - intersection);
    if ( std::fabs(errorUnion) > TOLERANCE * scale ||
         std::fabs(errorAB) > TOLERANCE * scale ||
         std::fabs(errorBA) > TOLERANCE * scale ) {
        return java::String("IDENTITY union ") +
            java::String::valueOf(errorUnion) + ", A-B " +
            java::String::valueOf(errorAB) + ", B-A " +
            java::String::valueOf(errorBA) + " (|A|=" +
            java::String::valueOf(volumeA) + " |B|=" +
            java::String::valueOf(volumeB) + " |AnB|=" +
            java::String::valueOf(intersection) + ")";
    }
    return "OK";
}

java::String Fuzz::firstLine(const java::String& message)
{
    int cut = message.indexOf('\n');
    java::String line = cut < 0 ? message : message.substring(0, cut);
    return line.length() > 160 ? line.substring(0, 160) : line;
}
