//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidInteriorOverlap.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/booleans/classification/_PolyhedralBoundedSolidSetNonIntersectingClassifier.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidFaceGeometry.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"

typedef _PolyhedralBoundedSolidInteriorOverlap Overlap;

/** Distance of the probe points from the cutting segments, in tolerances. */
#define CLEARANCE_IN_TOLERANCES 4.0

Overlap::Cut::Cut()
    : startX(0.0), startY(0.0), endX(0.0), endY(0.0)
{
}

Overlap::Cut::Cut(double inStartX, double inStartY, double inEndX,
    double inEndY)
    : startX(inStartX), startY(inStartY), endX(inEndX), endY(inEndY)
{
}

_PolyhedralBoundedSolidInteriorOverlap::_PolyhedralBoundedSolidInteriorOverlap()
{
}

BOOLEAN Overlap::interiorsOverlap(PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    if ( solidA == 0 || solidB == 0 ||
         solidA->getPolygonsList().size() < 1 ||
         solidB->getPolygonsList().size() < 1 ) {
        return FALSE;
    }
    double eps = numericContext.bigEpsilon();
    double* a = solidA->getMinMax();
    double* b = solidB->getMinMax();
    int axis;
    BOOLEAN separated = FALSE;

    for ( axis = 0; axis < 3; axis++ ) {
        if ( Math::min(a[axis + 3], b[axis + 3]) -
             Math::max(a[axis], b[axis]) <= eps ) {
            separated = TRUE;
            break;
        }
    }
    delete[] a;
    delete[] b;
    if ( separated ) {
        return FALSE;
    }

    FaceGeometryList facesA;
    FaceGeometryList facesB;
    BOOLEAN result;

    PolyhedralBoundedSolidFaceGeometry::of(solidA, facesA);
    PolyhedralBoundedSolidFaceGeometry::of(solidB, facesB);

    result = someFaceCellIsInside(solidA, facesA, solidB, facesB) ||
        someFaceCellIsInside(solidB, facesB, solidA, facesA);
    PolyhedralBoundedSolidFaceGeometry::dispose(facesA);
    PolyhedralBoundedSolidFaceGeometry::dispose(facesB);
    return result;
}

BOOLEAN Overlap::someFaceCellIsInside(PolyhedralBoundedSolid* solid,
    const FaceGeometryList& faces,
    PolyhedralBoundedSolid* other,
    const FaceGeometryList& otherFaces)
{
    double eps = numericContext.bigEpsilon();
    long i;
    long j;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        InfinitePlane* plane = faces[i]->getPlane();
        if ( plane == 0 ) {
            continue;
        }
        Vector3Dd normal;
        Vector3Dd origin;
        if ( !unit(plane->getNormal(), normal) ||
             !firstVertex(face, origin) ) {
            continue;
        }
        Vector3Dd u;
        // C++ port note: Java takes a null axis as a later failure; a unit
        // normal always has one.
        inPlaneAxis(normal, u);
        Vector3Dd v = normal.crossProduct(u);
        ArrayList<Cut> cuts;

        addEdges(face, origin, u, v, cuts);
        for ( j = 0; j < other->getPolygonsList().size(); j++ ) {
            if ( otherFaces[j]->getPlane() == 0 ||
                 faces[i]->boxSeparatedFrom(otherFaces[j], eps) ) {
                continue;
            }
            addCutsOfFace(plane, normal, origin, u, v,
                other->getPolygonsList().get(j), otherFaces[j]->getPlane(),
                cuts);
        }
        if ( someCellIsInside(face, plane, normal, origin, u, v, cuts,
                 solid, faces, other, otherFaces) ) {
            return TRUE;
        }
    }
    return FALSE;
}

/** The edges of every loop of `face`, as cuts. */
void Overlap::addEdges(_PolyhedralBoundedSolidFace* face,
    const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
    ArrayList<Cut>& cuts)
{
    long i;

    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        if ( he == 0 ) {
            continue;
        }
        do {
            _PolyhedralBoundedSolidHalfEdge* next = he->next();
            if ( next == 0 ) {
                break;
            }
            addCut(he->startingVertex->position,
                next->startingVertex->position, origin, u, v, cuts);
            he = next;
        } while ( he != start );
    }
}

/**
What `otherFace` leaves on the plane of the face being cut: its edges
that lie in the plane, and the stretches of the line where the two
planes meet that are inside `otherFace`.
*/
void Overlap::addCutsOfFace(InfinitePlane* plane, const Vector3Dd& normal,
    const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
    _PolyhedralBoundedSolidFace* otherFace, InfinitePlane* otherPlane,
    ArrayList<Cut>& cuts)
{
    double eps = numericContext.bigEpsilon();
    ArrayList<Vector3Dd> crossings;
    long i;
    long j;

    for ( i = 0; i < otherFace->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop =
            otherFace->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        if ( he == 0 ) {
            continue;
        }
        do {
            _PolyhedralBoundedSolidHalfEdge* next = he->next();
            if ( next == 0 ) {
                break;
            }
            Vector3Dd p = he->startingVertex->position;
            Vector3Dd q = next->startingVertex->position;
            double dp = plane->pointDistance(p);
            double dq = plane->pointDistance(q);
            BOOLEAN pOn = fabs(dp) <= eps;
            BOOLEAN qOn = fabs(dq) <= eps;
            if ( pOn && qOn ) {
                addCut(p, q, origin, u, v, cuts);
            }
            if ( pOn ) {
                crossings.add(p);
            }
            else if ( !qOn && (dp < 0.0) != (dq < 0.0) ) {
                crossings.add(p.add(q.subtract(p).multiply(
                    dp / (dp - dq))));
            }
            he = next;
        } while ( he != start );
    }
    if ( crossings.size() < 2 ) {
        return;
    }
    Vector3Dd direction = normal.crossProduct(otherPlane->getNormal());
    if ( direction.length() <= numericContext.unitVectorTolerance() *
             otherPlane->getNormal().length() ) {
        // Coplanar faces: the edges in the plane are all there is.
        return;
    }
    // Sort the crossings along the line where the planes meet.
    for ( i = 1; i < crossings.size(); i++ ) {
        Vector3Dd moving = crossings[i];
        double key = moving.dotProduct(direction);
        for ( j = i - 1; j >= 0 &&
              crossings[j].dotProduct(direction) > key; j-- ) {
            crossings[j + 1] = crossings[j];
        }
        crossings[j + 1] = moving;
    }
    for ( i = 0; i + 1 < crossings.size(); i++ ) {
        Vector3Dd p = crossings[i];
        Vector3Dd q = crossings[i + 1];
        if ( q.subtract(p).length() <= eps ) {
            continue;
        }
        Vector3Dd middle = p.add(q).multiply(0.5);
        if ( otherFace->testPointInside(middle, eps, otherPlane) !=
             Geometry::OUTSIDE ) {
            addCut(p, q, origin, u, v, cuts);
        }
    }
}

void Overlap::addCut(const Vector3Dd& p, const Vector3Dd& q,
    const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
    ArrayList<Cut>& cuts)
{
    Vector3Dd a = p.subtract(origin);
    Vector3Dd b = q.subtract(origin);

    cuts.add(Cut(a.dotProduct(u), a.dotProduct(v), b.dotProduct(u),
        b.dotProduct(v)));
}

/**
Visits every cell the cuts leave on the face, through a point on each
side of the middle of every piece of every cut.
*/
BOOLEAN Overlap::someCellIsInside(_PolyhedralBoundedSolidFace* face,
    InfinitePlane* plane, const Vector3Dd& normal, const Vector3Dd& origin,
    const Vector3Dd& u, const Vector3Dd& v, const ArrayList<Cut>& cuts,
    PolyhedralBoundedSolid* solid, const FaceGeometryList& faces,
    PolyhedralBoundedSolid* other, const FaceGeometryList& otherFaces)
{
    double eps = numericContext.bigEpsilon();
    double clearance = CLEARANCE_IN_TOLERANCES * eps;
    long i;
    long k;
    int side;

    for ( i = 0; i < cuts.size(); i++ ) {
        const Cut& cut = cuts[i];
        double rx = cut.endX - cut.startX;
        double ry = cut.endY - cut.startY;
        double length = sqrt(rx * rx + ry * ry);
        if ( length <= eps ) {
            continue;
        }
        ArrayList<double> stops;
        stopsAlong(cut, i, cuts, eps, stops);
        for ( k = 0; k + 1 < stops.size(); k++ ) {
            if ( (stops[k + 1] - stops[k]) * length <= eps ) {
                continue;
            }
            double t = (stops[k] + stops[k + 1]) / 2.0;
            for ( side = -1; side <= 1; side += 2 ) {
                double x = cut.startX + t * rx - side * clearance * ry / length;
                double y = cut.startY + t * ry + side * clearance * rx / length;
                Vector3Dd point = origin.add(u.multiply(x))
                    .add(v.multiply(y));
                if ( face->testPointInside(point, eps, plane) !=
                     Geometry::INSIDE ) {
                    continue;
                }
                if ( isWitness(point, normal, clearance, solid, faces,
                         other, otherFaces) ) {
                    return TRUE;
                }
            }
        }
    }
    return FALSE;
}

/**
`point` is in the interior of a face of `solid`, and inside `other` or
on its boundary. It proves the overlap when a point just off the face,
at the clearance distance, is inside both solids. Asking for that
point, and not only for `point` to be inside `other`, keeps the answer
out of the tolerance band: a face that enters the other solid by one
tolerance is a contact, not an overlap.
*/
BOOLEAN Overlap::isWitness(const Vector3Dd& point, const Vector3Dd& normal,
    double clearance, PolyhedralBoundedSolid* solid,
    const FaceGeometryList& faces,
    PolyhedralBoundedSolid* other,
    const FaceGeometryList& otherFaces)
{
    int status = _PolyhedralBoundedSolidSetNonIntersectingClassifier
        ::classifyPointAgainstSolid(other, otherFaces, point);
    int side;

    if ( status == Geometry::OUTSIDE ) {
        return FALSE;
    }
    for ( side = -1; side <= 1; side += 2 ) {
        Vector3Dd under = point.add(normal.multiply(side * clearance));
        if ( _PolyhedralBoundedSolidSetNonIntersectingClassifier
                 ::classifyPointAgainstSolid(solid, faces, under) ==
                 Geometry::INSIDE &&
             _PolyhedralBoundedSolidSetNonIntersectingClassifier
                 ::classifyPointAgainstSolid(other, otherFaces, under) ==
                 Geometry::INSIDE ) {
            return TRUE;
        }
    }
    return FALSE;
}

/**
Parameters in [0, 1] where `cut` is met by the other cuts, sorted, with
0 and 1 at the ends: crossings, T-junctions and the ends of the cuts
that run along it.
*/
void Overlap::stopsAlong(const Cut& cut, long index,
    const ArrayList<Cut>& cuts, double eps, ArrayList<double>& sorted)
{
    double rx = cut.endX - cut.startX;
    double ry = cut.endY - cut.startY;
    double length2 = rx * rx + ry * ry;
    double length = sqrt(length2);
    long i;
    long j;

    sorted.clear();
    sorted.add(0.0);
    sorted.add(1.0);
    for ( i = 0; i < cuts.size(); i++ ) {
        if ( i == index ) {
            continue;
        }
        const Cut& o = cuts[i];
        double wx = o.endX - o.startX;
        double wy = o.endY - o.startY;
        double otherLength = sqrt(wx * wx + wy * wy);
        double cross = rx * wy - ry * wx;
        // Ends of the other cut that lie on this one.
        addStopAt(o.startX, o.startY, cut, rx, ry, length, length2, eps, sorted);
        addStopAt(o.endX, o.endY, cut, rx, ry, length, length2, eps, sorted);
        if ( otherLength <= eps || fabs(cross) <=
             numericContext.unitVectorTolerance() * length *
                 otherLength ) {
            continue;
        }
        double qx = o.startX - cut.startX;
        double qy = o.startY - cut.startY;
        double t = (qx * wy - qy * wx) / cross;
        double s = (qx * ry - qy * rx) / cross;
        if ( t > 0.0 && t < 1.0 && s * otherLength >= -eps &&
             (s - 1.0) * otherLength <= eps ) {
            sorted.add(t);
        }
    }
    for ( i = 1; i < sorted.size(); i++ ) {
        double moving = sorted[i];
        for ( j = i - 1; j >= 0 && sorted[j] > moving; j-- ) {
            sorted[j + 1] = sorted[j];
        }
        sorted[j + 1] = moving;
    }
}

void Overlap::addStopAt(double x, double y, const Cut& cut, double rx,
    double ry, double length, double length2, double eps,
    ArrayList<double>& stops)
{
    double qx = x - cut.startX;
    double qy = y - cut.startY;
    double t = (qx * rx + qy * ry) / length2;

    if ( t > 0.0 && t < 1.0 &&
         fabs(qx * ry - qy * rx) / length <= eps ) {
        stops.add(t);
    }
}

/** C++ port note: Java returns null instead of FALSE. */
BOOLEAN Overlap::firstVertex(_PolyhedralBoundedSolidFace* face,
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

/** C++ port note: Java returns null instead of FALSE. */
BOOLEAN Overlap::unit(const Vector3Dd& vector, Vector3Dd& out)
{
    double length = vector.length();

    if ( length > 0.0 ) {
        out = vector.multiply(1.0 / length);
        return TRUE;
    }
    return FALSE;
}

/** A unit vector perpendicular to the unit vector `normal`. */
BOOLEAN Overlap::inPlaneAxis(const Vector3Dd& normal, Vector3Dd& out)
{
    double ax = fabs(normal.x());
    double ay = fabs(normal.y());
    double az = fabs(normal.z());
    Vector3Dd helper;

    if ( ax <= ay && ax <= az ) {
        helper = Vector3Dd(1, 0, 0);
    }
    else if ( ay <= az ) {
        helper = Vector3Dd(0, 1, 0);
    }
    else {
        helper = Vector3Dd(0, 0, 1);
    }
    return unit(normal.crossProduct(helper), out);
}
