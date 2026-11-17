#include <math.h>
#include <stdio.h>

#include "NearDegenerateBooleanFuzz.h"

#include "BooleanOracleFuzz.h"
#include "PolyhedralTestFailures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef PolyhedralBoundedSolidVolumeOracle::OrientedBox OrientedBox;
typedef PolyhedralBoundedSolidModeler Modeler;

const double NearDegenerateBooleanFuzz::STEPS[STEP_COUNT] = {
    -100, -10, -1, -0.1, 0, 0.1, 1, 10, 100 };

/** Java `"" + double` (C++ port note: %.17g, not the shortest digits). */
static String
numberText(double value)
{
    char text[40];

    sprintf(text, "%.17g", value);
    return text;
}

static String
firstLineOf(const String& text)
{
    int cut = text.indexOf('\n');

    return cut < 0 ? text : text.substring(0, cut);
}

const char* NearDegenerateBooleanFuzz::contactName(Contact contact)
{
    static const char* const NAMES[] = {
        "STACKED_PARTIAL", "STACKED_SAME_FOOTPRINT", "SIDE_BY_SIDE",
        "FLUSH_FACE_CROSSING", "INNER_TANGENT", "EDGE_CONTACT" };
    return NAMES[(int)contact];
}

NearDegenerateBooleanFuzz::Summary::Summary() :
    total(0), answered(0), rejected(0), wrong(0)
{
}

void NearDegenerateBooleanFuzz::Summary::merge(const String& key)
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

void NearDegenerateBooleanFuzz::pair(Contact contact, double d,
    BOOLEAN rotated, OrientedBox& a, OrientedBox& b)
{
    switch ( contact ) {
        case STACKED_PARTIAL:
            a = box(0, 0, 0, 1, 1, 1);
            b = box(0.4, 0.3, 1 + d, 1.4, 1.3, 1.6 + d);
            break;
        case STACKED_SAME_FOOTPRINT:
            a = box(0, 0, 0, 1, 1, 1);
            b = box(0, 0, 1 + d, 1, 1, 1.7 + d);
            break;
        case SIDE_BY_SIDE:
            a = box(0, 0, 0, 1, 1, 1);
            b = box(1 + d, 0.2, 0, 1.8 + d, 0.9, 1);
            break;
        case FLUSH_FACE_CROSSING:
            a = box(0, 0, 0, 1, 1, 1);
            b = box(0.3, -0.4, 0.5 + d, 0.8, 1.5, 1 + d);
            break;
        case INNER_TANGENT:
            a = box(0, 0, 0, 1, 1, 1);
            b = box(0.2, 0.3, 0.6 + d, 0.7, 0.8, 1 + d);
            break;
        default:
            a = box(0, 0, 0, 1, 1, 1);
            b = box(1 + d, 0.1, 1 + d, 1.9 + d, 0.9, 1.8 + d);
            break;
    }
    if ( rotated ) {
        Matrix4x4d motion = Oracle::translation(0.3, -0.2, 0.5).multiply(
            Oracle::rotation(0.83, 1, 2, -1.5));
        a = OrientedBox(a.size[0], a.size[1], a.size[2],
            motion.multiply(a.transform));
        b = OrientedBox(b.size[0], b.size[1], b.size[2],
            motion.multiply(b.transform));
    }
}

OrientedBox NearDegenerateBooleanFuzz::box(double x0, double y0, double z0,
    double x1, double y1, double z1)
{
    return OrientedBox(x1 - x0, y1 - y0, z1 - z0,
        Oracle::translation((x0 + x1) / 2.0, (y0 + y1) / 2.0,
            (z0 + z1) / 2.0));
}

NearDegenerateBooleanFuzz::Summary NearDegenerateBooleanFuzz::run(
    Contact contact, BOOLEAN rotated)
{
    Summary summary;
    OrientedBox a;
    OrientedBox b;
    int s;
    int k;

    pair(contact, 0, rotated, a, b);
    PolyhedralBoundedSolid* solid = a.toSolid();
    double bigEpsilon =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid).bigEpsilon();
    delete solid;

    for ( s = 0; s < STEP_COUNT; s++ ) {
        double step = STEPS[s];
        for ( k = 0; k < BooleanOracleFuzz::OPERATION_COUNT; k++ ) {
            int op = BooleanOracleFuzz::OPERATIONS[k];
            String outcome = runOne(contact, step * bigEpsilon, rotated, op,
                bigEpsilon);
            char stepText[40];
            // Java `"" + step` of these steps: one decimal
            sprintf(stepText, "%.1f", step);
            // Port note: no conditional operators over String temporaries
            // (internal compiler error of gcc 2.7.2)
            String label = String(contactName(contact));
            if ( rotated ) {
                label = label + " rotated";
            }
            label = label + " k=" + stepText + " " +
                BooleanOracleFuzz::opName(op);
            int blank = outcome.indexOf(' ');
            summary.total++;
            if ( blank < 0 ) {
                summary.merge(outcome);
            }
            else {
                summary.merge(outcome.substring(0, blank));
            }
            if ( outcome.startsWith("OK") ) {
                summary.answered++;
            }
            else if ( outcome.startsWith("REJECTED") ) {
                summary.rejected++;
                summary.details.add(label + " " + outcome);
            }
            else {
                summary.wrong++;
                summary.details.add(label + " " + outcome);
            }
        }
    }
    return summary;
}

String NearDegenerateBooleanFuzz::runOne(Contact contact, double d,
    BOOLEAN rotated, int op, double bigEpsilon)
{
    OrientedBox pairA;
    OrientedBox pairB;
    OrientedBox alignedA;
    OrientedBox alignedB;
    BOOLEAN reversed = op == BooleanOracleFuzz::B_MINUS_A;

    pair(contact, d, rotated, pairA, pairB);
    const OrientedBox& first = reversed ? pairB : pairA;
    const OrientedBox& second = reversed ? pairA : pairB;
    int kernelOp = reversed ? (int)Modeler::SUBTRACT : op;
    // The expected volume comes from the pair before the rotation: a
    // rigid motion keeps volumes, and the half-space oracle is exact
    // for axis-aligned boxes even when they only touch.
    pair(contact, d, FALSE, alignedA, alignedB);
    double expected = Oracle::expectedVolume(
        reversed ? alignedB : alignedA, reversed ? alignedA : alignedB,
        kernelOp);
    // Faces closer than the tolerance are one face for the kernel: the
    // volume may differ by the contact area (under 2 here) times the
    // displacement, when the displacement is inside the band.
    double band = fabs(d) < 2.0 * bigEpsilon ? fabs(d) : 2.0 * bigEpsilon;
    double tolerance = 1.0e-9 + 2.0 * band;
    PolyhedralBoundedSolid* firstSolid = first.toSolid();
    PolyhedralBoundedSolid* secondSolid = second.toSolid();
    PolyhedralBoundedSolid* result = NULL;
    String failureName;
    String failureMessage;

    try {
        result = Modeler::setOp(firstSolid, secondSolid, kernelOp);
    }
    PBS_CATCH_OPERATION(failureName, failureMessage)
    delete firstSolid;
    delete secondSolid;
    if ( failureName.length() > 0 ) {
        String message = firstLineOf(failureMessage);
        return String("REJECTED ") + failureName + ": " +
            (message.length() > 150 ? message.substring(0, 150) : message);
    }
    double volume = Oracle::solidVolume(result);
    if ( fabs(volume - expected) > tolerance ) {
        delete result;
        return String("WRONG volume=") + numberText(volume) + " expected=" +
            numberText(expected);
    }
    String message;
    if ( result->getPolygonsList().size() > 0 &&
         !PolyhedralBoundedSolidValidationEngine::validateStrict(result,
             &message) ) {
        delete result;
        return String("WRONG not strict-valid: ") + firstLineOf(message);
    }
    delete result;
    return "OK";
}
