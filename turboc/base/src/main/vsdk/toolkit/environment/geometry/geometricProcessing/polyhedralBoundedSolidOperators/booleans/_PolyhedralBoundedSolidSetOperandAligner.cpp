//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/_PolyhedralBoundedSolidSetOperandAligner.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidSetOperandAligner Aligner;

/** Width of the band, in tolerances, on each side of a plane. */
#define BAND_IN_TOLERANCES 2.0

/** Distances below this fraction of the tolerance are already zero. */
#define ALREADY_ALIGNED 1.0e-6

_PolyhedralBoundedSolidSetOperandAligner::_PolyhedralBoundedSolidSetOperandAligner()
{
}

BOOLEAN Aligner::align(PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB, Vector3Dd& outDisplacement)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(solidA, solidB);
    double tolerance = context.bigEpsilon();
    double band = BAND_IN_TOLERANCES * tolerance;
    ArrayList<PolyhedralBoundedSolidFaceGeometry*> facesA;
    ArrayList<PolyhedralBoundedSolidFaceGeometry*> facesB;
    ArrayList<Vector3Dd> basis;
    Vector3Dd displacement(0, 0, 0);
    BOOLEAN found = FALSE;
    BOOLEAN conflict = FALSE;
    long i;
    long j;
    long k;

    PolyhedralBoundedSolidFaceGeometry::of(solidA, facesA);
    PolyhedralBoundedSolidFaceGeometry::of(solidB, facesB);
    for ( i = 0; i < facesA.size() && !conflict; i++ ) {
        InfinitePlane* planeA = facesA[i]->getPlane();
        Vector3Dd pointA;
        if ( planeA == 0 ||
             !firstVertex(solidA->getPolygonsList().get(i), pointA) ) {
            continue;
        }
        Vector3Dd normalA = unit(planeA->getNormal());
        for ( j = 0; j < facesB.size(); j++ ) {
            InfinitePlane* planeB = facesB[j]->getPlane();
            Vector3Dd pointB;
            if ( planeB == 0 ||
                 !firstVertex(solidB->getPolygonsList().get(j), pointB) ||
                 facesA[i]->boxSeparatedFrom(facesB[j], band) ) {
                continue;
            }
            Vector3Dd normalB = unit(planeB->getNormal());
            if ( normalA.crossProduct(normalB).length() >
                 context.unitVectorTolerance() ) {
                continue;
            }
            double distance =
                pointB.subtract(pointA).dotProduct(normalA);
            if ( fabs(distance) > band ||
                 fabs(distance) <= ALREADY_ALIGNED * tolerance ) {
                continue;
            }
            // Wanted: normalA . displacement = -distance. Each new
            // direction is taken perpendicular to the ones already
            // used, so that the earlier pairs stay aligned.
            double residual = -distance -
                normalA.dotProduct(displacement);
            Vector3Dd free = normalA;
            for ( k = 0; k < basis.size(); k++ ) {
                free = free.subtract(basis[k].multiply(
                    free.dotProduct(basis[k])));
            }
            if ( free.length() <= context.unitVectorTolerance() ) {
                if ( fabs(residual) > ALREADY_ALIGNED * tolerance ) {
                    // Two pairs along one direction at different
                    // distances: no translation serves both.
                    conflict = TRUE;
                    break;
                }
                continue;
            }
            displacement = displacement.add(free.multiply(
                residual / normalA.dotProduct(free)));
            basis.add(unit(free));
            found = TRUE;
        }
    }
    PolyhedralBoundedSolidFaceGeometry::dispose(facesA);
    PolyhedralBoundedSolidFaceGeometry::dispose(facesB);
    if ( conflict || !found ) {
        return FALSE;
    }
    for ( i = 0; i < solidB->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* vertex =
            solidB->getVerticesList().get(i);
        vertex->position = vertex->position.add(displacement);
    }
    outDisplacement = displacement;
    return TRUE;
}

/** C++ port note: Java returns null instead of FALSE. */
BOOLEAN Aligner::firstVertex(_PolyhedralBoundedSolidFace* face,
    Vector3Dd& out)
{
    if ( face->boundariesList.size() < 1 ||
         face->boundariesList.get(0)->boundaryStartHalfEdge == 0 ) {
        return FALSE;
    }
    out = face->boundariesList.get(0)->boundaryStartHalfEdge
        ->startingVertex->position;
    return TRUE;
}

Vector3Dd Aligner::unit(const Vector3Dd& vector)
{
    double length = vector.length();

    if ( length > 0.0 ) {
        return vector.multiply(1.0 / length);
    }
    return vector;
}
