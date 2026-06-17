#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidGeometricValidator.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
bool PolyhedralBoundedSolidGeometricValidator::validateFacePointsAreCoplanar(
    java::ArrayList<Vector3Dd>& points)
{
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::defaultContext();
    if ( points.size() >= 3 ) {
        double minX = 1e308;
        double minY = 1e308;
        double minZ = 1e308;
        double maxX = -1e308;
        double maxY = -1e308;
        double maxZ = -1e308;
        for ( long int i = 0; i < points.size(); ++i ) {
            Vector3Dd p = points.get(i);
            if ( p.x() < minX ) minX = p.x();
            if ( p.y() < minY ) minY = p.y();
            if ( p.z() < minZ ) minZ = p.z();
            if ( p.x() > maxX ) maxX = p.x();
            if ( p.y() > maxY ) maxY = p.y();
            if ( p.z() > maxZ ) maxZ = p.z();
        }
        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;
        numericContext = PolyhedralBoundedSolidNumericPolicy::fromScale(
            std::sqrt(dx*dx + dy*dy + dz*dz));
    }
    return validateFacePointsAreCoplanar(points, numericContext);
}

bool PolyhedralBoundedSolidGeometricValidator::validateFacePointsAreCoplanar(
    java::ArrayList<Vector3Dd>& points,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext)
{
    if ( points.size() < 3 ) {
        return false;
    }

    Vector3Dd p0 = points.get(0);
    bool foundSeparatedPair = false;
    for ( long int i = 1; i < points.size(); ++i ) {
        Vector3Dd p1 = points.get(i);
        if ( PolyhedralBoundedSolidNumericPolicy::pointsSeparated(
                 p0, p1, numericContext) ) {
            foundSeparatedPair = true;
            break;
        }
    }
    if ( !foundSeparatedPair ) {
        return false;
    }

    InfinitePlane* facePlane = 0;
    for ( long int i = 0; i < points.size() && facePlane == 0; ++i ) {
        for ( long int j = 0; j < points.size() && facePlane == 0; ++j ) {
            for ( long int k = 0; k < points.size(); ++k ) {
                if ( i == j || i == k || j == k ) {
                    continue;
                }

                p0 = points.get(i);
                Vector3Dd p1 = points.get(j);
                Vector3Dd p2 = points.get(k);
                if ( !PolyhedralBoundedSolidNumericPolicy::pointsSeparated(
                         p0, p2, numericContext) ||
                     !PolyhedralBoundedSolidNumericPolicy::pointsSeparated(
                         p1, p2, numericContext) ) {
                    continue;
                }

                Vector3Dd a = p2.subtract(p0).normalized();
                Vector3Dd b = p1.subtract(p0).normalized();
                double aDotB = std::abs(a.dotProduct(b));
                if ( aDotB < 1.0 - numericContext.unitVectorTolerance() ) {
                    Vector3Dd n = a.crossProduct(b).normalized();
                    facePlane = new InfinitePlane(n, p0);
                }
                break;
            }
        }
    }

    if ( facePlane == 0 ) {
        return false;
    }

    bool coplanar = true;
    for ( long int i = 1; i < points.size(); ++i ) {
        if ( facePlane->doContainmentTest(points.get(i), numericContext.epsilon()) !=
             Geometry::LIMIT ) {
            coplanar = false;
            break;
        }
    }

    delete facePlane;
    return coplanar;
}

void PolyhedralBoundedSolidGeometricValidator::extractPointsFromFace(
    _PolyhedralBoundedSolidFace* face,
    java::ArrayList<Vector3Dd>& outPoints)
{
    if ( face == 0 ) {
        return;
    }

    for ( long int j = 0; j < face->boundariesList.size(); ++j ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
        if ( loop == 0 ) {
            return;
        }

        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        if ( he == 0 ) {
            return;
        }
        _PolyhedralBoundedSolidHalfEdge* heStart = he;
        do {
            he = he->next();
            if ( he == 0 || he->startingVertex == 0 ) {
                return;
            }
            outPoints.add(he->startingVertex->position);
        } while ( he != heStart );
    }
}

bool PolyhedralBoundedSolidGeometricValidator::validateFaceIsPlanar(
    _PolyhedralBoundedSolidFace* face)
{
    return validateFaceIsPlanar(
        face, PolyhedralBoundedSolidNumericPolicy::forFace(face));
}

bool PolyhedralBoundedSolidGeometricValidator::validateFaceIsPlanar(
    _PolyhedralBoundedSolidFace* face,
    const PolyhedralBoundedSolidNumericPolicy::ToleranceContext& numericContext)
{
    java::ArrayList<Vector3Dd> points;
    extractPointsFromFace(face, points);
    return points.size() >= 3 &&
        validateFacePointsAreCoplanar(points, numericContext);
}

bool PolyhedralBoundedSolidGeometricValidator::validateAllFacesPlanarityAndPlanes(
    PolyhedralBoundedSolid* solid, java::String*)
{
    if ( solid == 0 ) {
        return false;
    }
    for ( long int i = 0; i < solid->getPolygonsList().size(); ++i ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( !validateFaceIsPlanar(face) || face->getContainingPlane() == 0 ) {
            return false;
        }
    }
    return true;
}

bool PolyhedralBoundedSolidGeometricValidator::validateConsistentFaceOrientations(
    PolyhedralBoundedSolid*, java::String*)
{
    return true;
}

bool PolyhedralBoundedSolidGeometricValidator::validateLoopsStrict(
    PolyhedralBoundedSolid*, java::String*)
{
    return true;
}

bool PolyhedralBoundedSolidGeometricValidator::validateFaceIntersectionsStrict(
    PolyhedralBoundedSolid*, java::String*)
{
    return true;
}

bool PolyhedralBoundedSolidGeometricValidator::validateNoCoincidentVertices(
    PolyhedralBoundedSolid*, const PolyhedralBoundedSolidNumericPolicy::ToleranceContext&,
    java::String*)
{
    return true;
}

bool PolyhedralBoundedSolidGeometricValidator::validateUniqueFaceAndVertexIds(
    PolyhedralBoundedSolid* solid, java::String*)
{
    if ( solid == 0 ) return false;
    java::ArrayList<int> faceIds;
    java::ArrayList<int> vertexIds;
    java::ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();
    java::ArrayList<_PolyhedralBoundedSolidVertex*>& verts = solid->getVerticesList();
    for (long int i = 0; i < faces.size(); ++i) {
        int id = faces[i]->id;
        for (long int j = 0; j < faceIds.size(); ++j) {
            if ( faceIds[j] == id ) return false;
        }
        faceIds.add(id);
    }
    for (long int i = 0; i < verts.size(); ++i) {
        int id = verts[i]->id;
        for (long int j = 0; j < vertexIds.size(); ++j) {
            if ( vertexIds[j] == id ) return false;
        }
        vertexIds.add(id);
    }
    return true;
}
