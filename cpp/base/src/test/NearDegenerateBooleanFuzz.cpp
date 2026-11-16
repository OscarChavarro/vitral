#include <cmath>

#include "NearDegenerateBooleanFuzz.h"
#include "ThrowableSimpleName.h"

#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidOperationException.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"

typedef NearDegenerateBooleanFuzz Fuzz;
typedef PolyhedralBoundedSolidModeler Modeler;
typedef PolyhedralBoundedSolidVolumeOracle Oracle;
typedef Oracle::OrientedBox OrientedBox;

const double Fuzz::STEPS[STEP_COUNT] = {
    -100, -10, -1, -0.1, 0, 0.1, 1, 10, 100 };

const char* Fuzz::contactName(Contact contact)
{
    static const char* NAMES[6] = {
        "STACKED_PARTIAL", "STACKED_SAME_FOOTPRINT", "SIDE_BY_SIDE",
        "FLUSH_FACE_CROSSING", "INNER_TANGENT", "EDGE_CONTACT" };
    return NAMES[(int)contact];
}

Fuzz::Summary::Summary() : total(0), answered(0), rejected(0), wrong(0)
{
}

java::String Fuzz::Summary::detailsText() const
{
    java::String text("[");
    long i;
    for ( i = 0; i < details.size(); i++ ) {
        if ( i > 0 ) {
            text += ", ";
        }
        text += details[i];
    }
    return text + "]";
}

Fuzz::BoxPair Fuzz::pair(Contact contact, double d, bool rotated)
{
    OrientedBox a;
    OrientedBox b;

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
    return BoxPair(a, b);
}

OrientedBox Fuzz::box(double x0, double y0, double z0, double x1, double y1,
    double z1)
{
    return OrientedBox(x1 - x0, y1 - y0, z1 - z0,
        Oracle::translation((x0 + x1) / 2.0, (y0 + y1) / 2.0,
            (z0 + z1) / 2.0));
}

/** Java `text.split(" ")[0]`. */
static java::String firstWord(const java::String& text)
{
    int i = text.indexOf(' ');
    return i < 0 ? text : text.substring(0, i);
}

Fuzz::Summary Fuzz::run(Contact contact, bool rotated)
{
    Summary summary;
    PolyhedralBoundedSolid* probe = pair(contact, 0, rotated)[0].toSolid();
    double bigEpsilon = PolyhedralBoundedSolidNumericPolicy::forSolid(
        probe).bigEpsilon();
    delete probe;
    int s;
    int k;

    for ( s = 0; s < STEP_COUNT; s++ ) {
        double step = STEPS[s];
        for ( k = 0; k < 4; k++ ) {
            int op = BooleanOracleFuzz::OPERATIONS[k];
            java::String outcome = runOne(contact, step * bigEpsilon,
                rotated, op, bigEpsilon);
            java::String label = java::String(contactName(contact)) +
                (rotated ? " rotated" : "") + " k=" +
                java::String::valueOf(step) + " " +
                BooleanOracleFuzz::opName(op);
            summary.total++;
            java::String key = firstWord(outcome);
            summary.byOutcome.put(key,
                summary.byOutcome.getOrDefault(key, 0) + 1);
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

java::String Fuzz::runOne(Contact contact, double d, bool rotated, int op,
    double bigEpsilon)
{
    BoxPair boxes = pair(contact, d, rotated);
    const OrientedBox& first = op == BooleanOracleFuzz::B_MINUS_A ?
        boxes[1] : boxes[0];
    const OrientedBox& second = op == BooleanOracleFuzz::B_MINUS_A ?
        boxes[0] : boxes[1];
    int kernelOp = op == BooleanOracleFuzz::B_MINUS_A ?
        (int)Modeler::SUBTRACT : op;
    // The expected volume comes from the pair before the rotation: a
    // rigid motion keeps volumes, and the half-space oracle is exact
    // for axis-aligned boxes even when they only touch.
    BoxPair aligned = pair(contact, d, false);
    double expected = Oracle::expectedVolume(
        op == BooleanOracleFuzz::B_MINUS_A ? aligned[1] : aligned[0],
        op == BooleanOracleFuzz::B_MINUS_A ? aligned[0] : aligned[1],
        kernelOp);
    // Faces closer than the tolerance are one face for the kernel: the
    // volume may differ by the contact area (under 2 here) times the
    // displacement, when the displacement is inside the band.
    double tolerance = 1.0e-9 + 2.0 * std::fmin(std::fabs(d),
        2.0 * bigEpsilon);
    PolyhedralBoundedSolid* result;
    PolyhedralBoundedSolid* firstSolid = first.toSolid();
    PolyhedralBoundedSolid* secondSolid = second.toSolid();

    try {
        result = Modeler::setOp(firstSolid, secondSolid, kernelOp);
    }
    catch ( const PolyhedralBoundedSolidOperationException& e ) {
        delete firstSolid;
        delete secondSolid;
        java::String message = e.getMessage();
        int cut = message.indexOf('\n');
        if ( cut >= 0 ) {
            message = message.substring(0, cut);
        }
        return java::String("REJECTED ") + throwableSimpleName(e) + ": " +
            (message.length() > 150 ? message.substring(0, 150) : message);
    }
    // C++ ownership: the operands are released (Java: garbage collector)
    delete firstSolid;
    delete secondSolid;
    double volume = Oracle::solidVolume(result);
    if ( std::fabs(volume - expected) > tolerance ) {
        delete result;
        return java::String("WRONG volume=") + java::String::valueOf(volume) +
            " expected=" + java::String::valueOf(expected);
    }
    java::String message;
    if ( result->getPolygonsList().size() > 0 &&
         !PolyhedralBoundedSolidValidationEngine::validateStrict(result,
             &message) ) {
        delete result;
        int cut = message.indexOf('\n');
        return java::String("WRONG not strict-valid: ") +
            (cut < 0 ? message : message.substring(0, cut));
    }
    delete result;
    return "OK";
}
