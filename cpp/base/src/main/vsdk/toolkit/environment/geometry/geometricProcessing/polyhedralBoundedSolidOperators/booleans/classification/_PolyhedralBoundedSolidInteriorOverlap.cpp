//= References:                                                             =
//= [MANT1988] Mantyla Martti. "An Introduction To Solid Modeling",         =
//=     Computer Science Press, 1988.                                       =

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

typedef _PolyhedralBoundedSolidInteriorOverlap InteriorOverlap;

const double InteriorOverlap::CLEARANCE_IN_TOLERANCES = 4.0;

bool InteriorOverlap::interiorsOverlap(PolyhedralBoundedSolid* solidA,
    PolyhedralBoundedSolid* solidB)
{
    if ( solidA == nullptr || solidB == nullptr ||
         solidA->getPolygonsList().size() < 1 ||
         solidB->getPolygonsList().size() < 1 ) {
        return false;
    }
    double eps = numericContext.bigEpsilon();
    double* a = solidA->getMinMax();
    double* b = solidB->getMinMax();
    int axis;

    for ( axis = 0; axis < 3; axis++ ) {
        if ( java::Math::min(a[axis + 3], b[axis + 3]) -
             java::Math::max(a[axis], b[axis]) <= eps ) {
            delete[] a;
            delete[] b;
            return false;
        }
    }
    delete[] a;
    delete[] b;

    FaceGeometries facesA = PolyhedralBoundedSolidFaceGeometry::of(solidA);
    FaceGeometries facesB = PolyhedralBoundedSolidFaceGeometry::of(solidB);

    bool overlap = someFaceCellIsInside(solidA, facesA, solidB, facesB) ||
        someFaceCellIsInside(solidB, facesB, solidA, facesA);
    PolyhedralBoundedSolidFaceGeometry::release(facesA);
    PolyhedralBoundedSolidFaceGeometry::release(facesB);
    return overlap;
}

bool InteriorOverlap::someFaceCellIsInside(PolyhedralBoundedSolid* solid,
    const FaceGeometries& faces,
    PolyhedralBoundedSolid* other,
    const FaceGeometries& otherFaces)
{
    double eps = numericContext.bigEpsilon();
    int i;
    int j;

    for ( i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        InfinitePlane* plane = faces.get(i)->getPlane();
        if ( plane == nullptr ) {
            continue;
        }
        Vector3Dd normal;
        Vector3Dd origin;
        if ( !unit(plane->getNormal(), &normal) ||
             !firstVertex(face, &origin) ) {
            continue;
        }
        Vector3Dd u;
        inPlaneAxis(normal, &u);
        Vector3Dd v = normal.crossProduct(u);
        java::ArrayList<Cut> cuts;

        addEdges(face, origin, u, v, cuts);
        for ( j = 0; j < other->getPolygonsList().size(); j++ ) {
            if ( otherFaces.get(j)->getPlane() == nullptr ||
                 faces.get(i)->boxSeparatedFrom(otherFaces.get(j), eps) ) {
                continue;
            }
            addCutsOfFace(plane, normal, origin, u, v,
                other->getPolygonsList().get(j), otherFaces.get(j)->getPlane(),
                cuts);
        }
        if ( someCellIsInside(face, plane, normal, origin, u, v, cuts,
                 solid, faces, other, otherFaces) ) {
            return true;
        }
    }
    return false;
}

/** The edges of every loop of `face`, as cuts. */
void InteriorOverlap::addEdges(_PolyhedralBoundedSolidFace* face,
    const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
    java::ArrayList<Cut>& cuts)
{
    int i;

    for ( i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        if ( he == nullptr ) {
            continue;
        }
        do {
            _PolyhedralBoundedSolidHalfEdge* next = he->next();
            if ( next == nullptr ) {
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
void InteriorOverlap::addCutsOfFace(InfinitePlane* plane,
    const Vector3Dd& normal, const Vector3Dd& origin, const Vector3Dd& u,
    const Vector3Dd& v, _PolyhedralBoundedSolidFace* otherFace,
    InfinitePlane* otherPlane, java::ArrayList<Cut>& cuts)
{
    double eps = numericContext.bigEpsilon();
    java::ArrayList<Vector3Dd> crossings;
    int i;
    int j;

    for ( i = 0; i < otherFace->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop =
            otherFace->boundariesList.get(i);
        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        if ( he == nullptr ) {
            continue;
        }
        do {
            _PolyhedralBoundedSolidHalfEdge* next = he->next();
            if ( next == nullptr ) {
                break;
            }
            Vector3Dd p = he->startingVertex->position;
            Vector3Dd q = next->startingVertex->position;
            double dp = plane->pointDistance(p);
            double dq = plane->pointDistance(q);
            bool pOn = java::Math::abs(dp) <= eps;
            bool qOn = java::Math::abs(dq) <= eps;
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
        Vector3Dd moving = crossings.get(i);
        double key = moving.dotProduct(direction);
        for ( j = i - 1; j >= 0 &&
              crossings.get(j).dotProduct(direction) > key; j-- ) {
            crossings.set(j + 1, crossings.get(j));
        }
        crossings.set(j + 1, moving);
    }
    for ( i = 0; i + 1 < crossings.size(); i++ ) {
        Vector3Dd p = crossings.get(i);
        Vector3Dd q = crossings.get(i + 1);
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

void InteriorOverlap::addCut(const Vector3Dd& p, const Vector3Dd& q,
    const Vector3Dd& origin, const Vector3Dd& u, const Vector3Dd& v,
    java::ArrayList<Cut>& cuts)
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
bool InteriorOverlap::someCellIsInside(_PolyhedralBoundedSolidFace* face,
    InfinitePlane* plane, const Vector3Dd& normal, const Vector3Dd& origin,
    const Vector3Dd& u, const Vector3Dd& v, const java::ArrayList<Cut>& cuts,
    PolyhedralBoundedSolid* solid, const FaceGeometries& faces,
    PolyhedralBoundedSolid* other, const FaceGeometries& otherFaces)
{
    double eps = numericContext.bigEpsilon();
    double clearance = CLEARANCE_IN_TOLERANCES * eps;
    int i;
    int k;
    int side;

    for ( i = 0; i < cuts.size(); i++ ) {
        Cut cut = cuts.get(i);
        double rx = cut.endX - cut.startX;
        double ry = cut.endY - cut.startY;
        double length = java::Math::sqrt(rx * rx + ry * ry);
        if ( length <= eps ) {
            continue;
        }
        java::ArrayList<double> stops = stopsAlong(cut, i, cuts, eps);
        for ( k = 0; k + 1 < stops.size(); k++ ) {
            if ( (stops.get(k + 1) - stops.get(k)) * length <= eps ) {
                continue;
            }
            double t = (stops.get(k) + stops.get(k + 1)) / 2.0;
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
                    return true;
                }
            }
        }
    }
    return false;
}

/**
`point` is in the interior of a face of `solid`, and inside `other` or
on its boundary. It proves the overlap when a point just off the face,
at the clearance distance, is inside both solids. Asking for that
point, and not only for `point` to be inside `other`, keeps the answer
out of the tolerance band: a face that enters the other solid by one
tolerance is a contact, not an overlap.
*/
bool InteriorOverlap::isWitness(const Vector3Dd& point,
    const Vector3Dd& normal, double clearance, PolyhedralBoundedSolid* solid,
    const FaceGeometries& faces, PolyhedralBoundedSolid* other,
    const FaceGeometries& otherFaces)
{
    int status = _PolyhedralBoundedSolidSetNonIntersectingClassifier
        ::classifyPointAgainstSolid(other, otherFaces, point);
    int side;

    if ( status == Geometry::OUTSIDE ) {
        return false;
    }
    for ( side = -1; side <= 1; side += 2 ) {
        Vector3Dd under = point.add(normal.multiply(side * clearance));
        if ( _PolyhedralBoundedSolidSetNonIntersectingClassifier
                 ::classifyPointAgainstSolid(solid, faces, under) ==
                 Geometry::INSIDE &&
             _PolyhedralBoundedSolidSetNonIntersectingClassifier
                 ::classifyPointAgainstSolid(other, otherFaces, under) ==
                 Geometry::INSIDE ) {
            return true;
        }
    }
    return false;
}

/**
Parameters in [0, 1] where `cut` is met by the other cuts, sorted, with
0 and 1 at the ends: crossings, T-junctions and the ends of the cuts
that run along it.
*/
java::ArrayList<double> InteriorOverlap::stopsAlong(const Cut& cut,
    int index, const java::ArrayList<Cut>& cuts, double eps)
{
    double rx = cut.endX - cut.startX;
    double ry = cut.endY - cut.startY;
    double length2 = rx * rx + ry * ry;
    double length = java::Math::sqrt(length2);
    java::ArrayList<double> stops;
    int i;
    int j;

    stops.add(0.0);
    stops.add(1.0);
    for ( i = 0; i < cuts.size(); i++ ) {
        if ( i == index ) {
            continue;
        }
        Cut o = cuts.get(i);
        double wx = o.endX - o.startX;
        double wy = o.endY - o.startY;
        double otherLength = java::Math::sqrt(wx * wx + wy * wy);
        double cross = rx * wy - ry * wx;
        // Ends of the other cut that lie on this one.
        addStopAt(o.startX, o.startY, cut, rx, ry, length, length2, eps, stops);
        addStopAt(o.endX, o.endY, cut, rx, ry, length, length2, eps, stops);
        if ( otherLength <= eps || java::Math::abs(cross) <=
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
            stops.add(t);
        }
    }
    java::ArrayList<double> sorted;
    for ( i = 0; i < stops.size(); i++ ) {
        sorted.add(stops.get(i));
    }
    for ( i = 1; i < sorted.size(); i++ ) {
        double moving = sorted.get(i);
        for ( j = i - 1; j >= 0 && sorted.get(j) > moving; j-- ) {
            sorted.set(j + 1, sorted.get(j));
        }
        sorted.set(j + 1, moving);
    }
    return sorted;
}

void InteriorOverlap::addStopAt(double x, double y, const Cut& cut,
    double rx, double ry, double length, double length2, double eps,
    java::ArrayList<double>& stops)
{
    double qx = x - cut.startX;
    double qy = y - cut.startY;
    double t = (qx * rx + qy * ry) / length2;

    if ( t > 0.0 && t < 1.0 &&
         java::Math::abs(qx * ry - qy * rx) / length <= eps ) {
        stops.add(t);
    }
}

/**
C++ port note: the Java null answer is a false return value.
*/
bool InteriorOverlap::firstVertex(_PolyhedralBoundedSolidFace* face,
    Vector3Dd* out)
{
    if ( face->boundariesList.size() < 1 ||
         face->boundariesList.get(0)->boundaryStartHalfEdge == nullptr ) {
        return false;
    }
    *out = face->boundariesList.get(0)->boundaryStartHalfEdge
        ->startingVertex->position;
    return true;
}

/**
C++ port note: the Java null answer (zero vector) is a false return value.
*/
bool InteriorOverlap::unit(const Vector3Dd& vector, Vector3Dd* out)
{
    double length = vector.length();

    if ( length > 0.0 ) {
        *out = vector.multiply(1.0 / length);
        return true;
    }
    return false;
}

/** A unit vector perpendicular to the unit vector `normal`. */
bool InteriorOverlap::inPlaneAxis(const Vector3Dd& normal, Vector3Dd* out)
{
    double ax = java::Math::abs(normal.x());
    double ay = java::Math::abs(normal.y());
    double az = java::Math::abs(normal.z());
    Vector3Dd helper = ax <= ay && ax <= az ? Vector3Dd(1, 0, 0) :
        (ay <= az ? Vector3Dd(0, 1, 0) : Vector3Dd(0, 0, 1));

    return unit(normal.crossProduct(helper), out);
}
