#include <float.h>
#include <math.h>

#include "SplitterOracleFuzz.h"

#include "PolyhedralBoundedSolidTestFixtures.h"
#include "PolyhedralBoundedSolidVolumeOracle.h"
#include "PolyhedralTestFailures.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/PolyhedralBoundedSolidModeler.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidStructuralCopy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidValidationEngine.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

static const double TOLERANCE = 1.0e-6;

String SplitterOracleFuzz::check(PolyhedralBoundedSolid* base,
    const InfinitePlane& plane)
{
    PolyhedralBoundedSolid* solid =
        PolyhedralBoundedSolidStructuralCopy::copy(base);
    ArrayList<PolyhedralBoundedSolid*> above;
    ArrayList<PolyhedralBoundedSolid*> below;
    double volume = PolyhedralBoundedSolidVolumeOracle::solidVolume(base);
    double min = DBL_MAX;
    double max = -DBL_MAX;
    double total = 0.0;
    String failureName;
    String failureMessage;
    long i;

    for ( i = 0; i < base->getVerticesList().size(); i++ ) {
        double d = plane.pointDistance(
            base->getVerticesList().get(i)->position);
        if ( d < min ) {
            min = d;
        }
        if ( d > max ) {
            max = d;
        }
    }
    BOOLEAN cuts = min < -TOLERANCE && max > TOLERANCE;

    try {
        PolyhedralBoundedSolidModeler::split(solid, plane, above, below);
    }
    PBS_CATCH_THROWABLE(failureName, failureMessage)
    if ( failureName.length() > 0 ) {
        delete solid;
        PolyhedralBoundedSolidTestFixtures::dispose(above);
        PolyhedralBoundedSolidTestFixtures::dispose(below);
        return String("exception ") + failureName;
    }

    BOOLEAN strict = TRUE;
    BOOLEAN empty = FALSE;
    BOOLEAN sides = TRUE;
    int side;
    for ( side = 1; side >= -1; side -= 2 ) {
        ArrayList<PolyhedralBoundedSolid*>& pieces = side > 0 ? above : below;
        long p;
        for ( p = 0; p < pieces.size(); p++ ) {
            PolyhedralBoundedSolid* piece = pieces[p];
            String message;
            if ( !PolyhedralBoundedSolidValidationEngine::validateStrict(
                     piece, &message) ) {
                strict = FALSE;
            }
            total += PolyhedralBoundedSolidVolumeOracle::solidVolume(piece);
            if ( cuts && piece->getPolygonsList().size() == 0 ) {
                empty = TRUE;
            }
            for ( i = 0; i < piece->getVerticesList().size(); i++ ) {
                if ( side * plane.pointDistance(piece->getVerticesList()
                         .get(i)->position) < -TOLERANCE ) {
                    sides = FALSE;
                }
            }
        }
    }
    delete solid;
    PolyhedralBoundedSolidTestFixtures::dispose(above);
    PolyhedralBoundedSolidTestFixtures::dispose(below);
    if ( fabs(total - volume) > TOLERANCE * (volume > 1.0 ? volume : 1.0) ) {
        return "volume";
    }
    if ( empty ) {
        return "empty piece";
    }
    if ( !sides ) {
        return "side";
    }
    if ( !strict ) {
        return "strict";
    }
    return "";
}
