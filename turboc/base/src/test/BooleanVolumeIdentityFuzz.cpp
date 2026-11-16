#include <math.h>
#include <stdio.h>

#include "BooleanVolumeIdentityFuzz.h"

#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PolyhedralTestFailures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidModeler Modeler;

const double BooleanVolumeIdentityFuzz::TOLERANCE = 1.0e-9;

/** Java `"" + double` (C++ port note: %.17g, not the shortest digits). */
static String
numberText(double value)
{
    char text[40];

    sprintf(text, "%.17g", value);
    return text;
}

const char* BooleanVolumeIdentityFuzz::familyName(Family family)
{
    static const char* const NAMES[] = {
        "CONVEX_PRISMS", "STAR_AND_CONVEX", "STAR_PRISMS" };
    return NAMES[(int)family];
}

BooleanVolumeIdentityFuzz::Summary::Summary() : total(0), correct(0)
{
}

void BooleanVolumeIdentityFuzz::Summary::merge(const String& key)
{
    long i;

    for ( i = 0; i < outcomeKeys.size(); i++ ) {
        if ( outcomeKeys[i] == key ) {
            outcomeCounts[i]++;
            return;
        }
    }
    for ( i = 0; i < outcomeKeys.size() && outcomeKeys[i] < key; i++ ) {
    }
    outcomeKeys.add(i, key);
    outcomeCounts.add(i, 1);
}

BOOLEAN BooleanVolumeIdentityFuzz::Summary::containsKey(
    const char* key) const
{
    long i;

    for ( i = 0; i < outcomeKeys.size(); i++ ) {
        if ( outcomeKeys[i] == key ) {
            return TRUE;
        }
    }
    return FALSE;
}

String BooleanVolumeIdentityFuzz::Summary::byOutcomeText() const
{
    String text = "{";
    char number[32];
    long i;

    for ( i = 0; i < outcomeKeys.size(); i++ ) {
        if ( i > 0 ) {
            text = text + ", ";
        }
        sprintf(number, "%d", outcomeCounts[i]);
        text = text + outcomeKeys[i] + "=" + number;
    }
    return text + "}";
}

BooleanVolumeIdentityFuzz::Summary
BooleanVolumeIdentityFuzz::runFamily(Family family)
{
    Summary summary;
    Random random(4201 + (int)family);
    int i;

    for ( i = 0; i < CASES_PER_FAMILY; i++ ) {
        PrismPolygon baseA = base(family, 0, random);
        PrismPolygon baseB = base(family, 1, random);
        double heightA = 0.8 + 0.6 * random.nextDouble();
        double heightB = 0.8 + 0.6 * random.nextDouble();
        Matrix4x4d place = placement(random);
        String outcome = check(baseA, heightA, baseB, heightB, place);
        int blank = outcome.indexOf(' ');
        summary.total++;
        summary.merge(blank < 0 ? outcome : outcome.substring(0, blank));
        if ( outcome == "OK" ) {
            summary.correct++;
        }
        else {
            char number[32];
            sprintf(number, "%d", i);
            summary.failures.add(String(familyName(family)) + " #" +
                number + " " + outcome);
        }
    }
    return summary;
}

PrismPolygon BooleanVolumeIdentityFuzz::base(Family family, int which,
    Random& random)
{
    BOOLEAN star = family == STAR_PRISMS ||
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

/** B is rotated about a random axis and moved less than its size. */
Matrix4x4d BooleanVolumeIdentityFuzz::placement(Random& random)
{
    double x = random.nextGaussian();
    double y = random.nextGaussian();
    double z = random.nextGaussian();
    double angle = 0.2 + 1.2 * random.nextDouble();
    double tx = 0.6 * (random.nextDouble() - 0.5);
    double ty = 0.6 * (random.nextDouble() - 0.5);
    double tz = 0.5 * (random.nextDouble() - 0.5) + 0.3;

    return Oracle::translation(tx, ty, tz).multiply(
        Oracle::rotation(angle, x, y, z));
}

PolyhedralBoundedSolid* BooleanVolumeIdentityFuzz::a(
    const PrismPolygon& base, double height)
{
    return PrismFixtures::prism(base, height);
}

PolyhedralBoundedSolid* BooleanVolumeIdentityFuzz::b(
    const PrismPolygon& base, double height, const Matrix4x4d& placement)
{
    PolyhedralBoundedSolid* solid = PrismFixtures::prism(base, height);

    Modeler::applyTransformation(solid, placement);
    return solid;
}

/**
C++ port note: the operands and the result are deleted here (the Java
code leaves them to the garbage collector), also when setOp throws.
*/
double BooleanVolumeIdentityFuzz::operationVolume(
    PolyhedralBoundedSolid* first, PolyhedralBoundedSolid* second, int op)
{
    PolyhedralBoundedSolid* result = NULL;
    double volume;

    try {
        result = Modeler::setOp(first, second, op);
    }
#ifndef VSDK_WITHOUT_EXCEPTIONS
    catch ( ... ) {
        delete first;
        delete second;
        throw;
    }
#endif
    volume = Oracle::solidVolume(result);
    delete result;
    delete first;
    delete second;
    return volume;
}

String BooleanVolumeIdentityFuzz::check(const PrismPolygon& baseA,
    double heightA, const PrismPolygon& baseB, double heightB,
    const Matrix4x4d& placement)
{
    PolyhedralBoundedSolid* solidA = a(baseA, heightA);
    PolyhedralBoundedSolid* solidB = b(baseB, heightB, placement);
    double volumeA = Oracle::solidVolume(solidA);
    double volumeB = Oracle::solidVolume(solidB);
    double scale = volumeA + volumeB > 1.0 ? volumeA + volumeB : 1.0;
    double unionVolume = 0.0;
    double intersection = 0.0;
    double aMinusB = 0.0;
    double bMinusA = 0.0;
    String failureName;
    String failureMessage;

    delete solidA;
    delete solidB;
    if ( fabs(volumeA - PrismFixtures::polygonArea(baseA) * heightA) >
             TOLERANCE * scale ||
         fabs(volumeB - PrismFixtures::polygonArea(baseB) * heightB) >
             TOLERANCE * scale ) {
        return "OPERAND volume differs from base area x height";
    }
    try {
        unionVolume = operationVolume(a(baseA, heightA),
            b(baseB, heightB, placement), Modeler::UNION);
        intersection = operationVolume(a(baseA, heightA),
            b(baseB, heightB, placement), Modeler::INTERSECTION);
        aMinusB = operationVolume(a(baseA, heightA),
            b(baseB, heightB, placement), Modeler::SUBTRACT);
        bMinusA = operationVolume(b(baseB, heightB, placement),
            a(baseA, heightA), Modeler::SUBTRACT);
    }
    PBS_CATCH_OPERATION(failureName, failureMessage)
    if ( failureName.length() > 0 ) {
        return String("EXCEPTION ") + failureName + ": " +
            firstLine(failureMessage);
    }
    double smaller = volumeA < volumeB ? volumeA : volumeB;
    if ( intersection < -TOLERANCE * scale ||
         intersection > smaller + TOLERANCE * scale ) {
        return String("RANGE |AnB|=") + numberText(intersection) +
            " |A|=" + numberText(volumeA) + " |B|=" + numberText(volumeB);
    }
    double errorUnion = unionVolume - (volumeA + volumeB - intersection);
    double errorAB = aMinusB - (volumeA - intersection);
    double errorBA = bMinusA - (volumeB - intersection);
    if ( fabs(errorUnion) > TOLERANCE * scale ||
         fabs(errorAB) > TOLERANCE * scale ||
         fabs(errorBA) > TOLERANCE * scale ) {
        return String("IDENTITY union ") + numberText(errorUnion) +
            ", A-B " + numberText(errorAB) + ", B-A " +
            numberText(errorBA) + " (|A|=" + numberText(volumeA) +
            " |B|=" + numberText(volumeB) + " |AnB|=" +
            numberText(intersection) + ")";
    }
    return "OK";
}

String BooleanVolumeIdentityFuzz::firstLine(const String& message)
{
    int cut = message.indexOf('\n');
    String line = cut < 0 ? message : message.substring(0, cut);

    return line.length() > 160 ? line.substring(0, 160) : line;
}
