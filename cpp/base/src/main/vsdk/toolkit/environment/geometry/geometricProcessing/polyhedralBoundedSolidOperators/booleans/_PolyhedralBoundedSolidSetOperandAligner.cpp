//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include "java/lang/Math.h"
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

const double _PolyhedralBoundedSolidSetOperandAligner::BAND_IN_TOLERANCES = 2.0;
const double _PolyhedralBoundedSolidSetOperandAligner::ALREADY_ALIGNED = 1.0e-6;

Vector3Dd* _PolyhedralBoundedSolidSetOperandAligner::align(
    PolyhedralBoundedSolid* solidA, PolyhedralBoundedSolid* solidB)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext context =
        PolyhedralBoundedSolidNumericPolicy::forSolids(solidA, solidB);
    double tolerance = context.bigEpsilon();
    double band = BAND_IN_TOLERANCES * tolerance;
    java::ArrayList<PolyhedralBoundedSolidFaceGeometry*> facesA =
        PolyhedralBoundedSolidFaceGeometry::of(solidA);
    java::ArrayList<PolyhedralBoundedSolidFaceGeometry*> facesB =
        PolyhedralBoundedSolidFaceGeometry::of(solidB);
    java::ArrayList<Vector3Dd> basis;
    Vector3Dd displacement(0, 0, 0);
    bool found = false;
    int i;
    int j;
    int k;

    for ( i = 0; i < facesA.size(); i++ ) {
        InfinitePlane* planeA = facesA.get(i)->getPlane();
        Vector3Dd pointA;
        if ( planeA == nullptr ||
             !firstVertex(solidA->getPolygonsList().get(i), &pointA) ) {
            continue;
        }
        Vector3Dd normalA = unit(planeA->getNormal());
        for ( j = 0; j < facesB.size(); j++ ) {
            InfinitePlane* planeB = facesB.get(j)->getPlane();
            Vector3Dd pointB;
            if ( planeB == nullptr ||
                 !firstVertex(solidB->getPolygonsList().get(j), &pointB) ||
                 facesA.get(i)->boxSeparatedFrom(facesB.get(j), band) ) {
                continue;
            }
            Vector3Dd normalB = unit(planeB->getNormal());
            if ( normalA.crossProduct(normalB).length() >
                 context.unitVectorTolerance() ) {
                continue;
            }
            double distance =
                pointB.subtract(pointA).dotProduct(normalA);
            if ( java::Math::abs(distance) > band ||
                 java::Math::abs(distance) <= ALREADY_ALIGNED * tolerance ) {
                continue;
            }
            // Wanted: normalA . displacement = -distance. Each new
            // direction is taken perpendicular to the ones already
            // used, so that the earlier pairs stay aligned.
            double residual = -distance -
                normalA.dotProduct(displacement);
            Vector3Dd free = normalA;
            for ( k = 0; k < basis.size(); k++ ) {
                free = free.subtract(basis.get(k).multiply(
                    free.dotProduct(basis.get(k))));
            }
            if ( free.length() <= context.unitVectorTolerance() ) {
                if ( java::Math::abs(residual) > ALREADY_ALIGNED * tolerance ) {
                    // Two pairs along one direction at different
                    // distances: no translation serves both.
                    PolyhedralBoundedSolidFaceGeometry::release(facesA);
                    PolyhedralBoundedSolidFaceGeometry::release(facesB);
                    return nullptr;
                }
                continue;
            }
            displacement = displacement.add(free.multiply(
                residual / normalA.dotProduct(free)));
            basis.add(unit(free));
            found = true;
        }
    }
    PolyhedralBoundedSolidFaceGeometry::release(facesA);
    PolyhedralBoundedSolidFaceGeometry::release(facesB);
    if ( !found ) {
        return nullptr;
    }
    for ( i = 0; i < solidB->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* vertex =
            solidB->getVerticesList().get(i);
        vertex->position = vertex->position.add(displacement);
    }
    return new Vector3Dd(displacement);
}

/**
C++ port note: the Java null answer is a false return value.
*/
bool _PolyhedralBoundedSolidSetOperandAligner::firstVertex(
    _PolyhedralBoundedSolidFace* face, Vector3Dd* out)
{
    if ( face->boundariesList.size() < 1 ||
         face->boundariesList.get(0)->boundaryStartHalfEdge == nullptr ) {
        return false;
    }
    *out = face->boundariesList.get(0)->boundaryStartHalfEdge
        ->startingVertex->position;
    return true;
}

Vector3Dd _PolyhedralBoundedSolidSetOperandAligner::unit(const Vector3Dd& vector)
{
    double length = vector.length();

    return length > 0.0 ? vector.multiply(1.0 / length) : vector;
}
