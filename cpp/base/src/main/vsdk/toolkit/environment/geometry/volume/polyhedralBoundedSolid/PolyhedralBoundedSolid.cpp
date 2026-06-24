#include <cfloat>
#include <cmath>

#include "java/util/ArrayList.txx"
#include <algorithm>
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidPredicates.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
PolyhedralBoundedSolid::PolyhedralBoundedSolid()
    : maxVertexId(-1),
      maxFaceId(-1),
      modelIsValid(false),
      queryPlaneCache(0),
      queryFaceAabb(0),
      queryNumericContext(0),
      queryFaceCount(0)
{
}

PolyhedralBoundedSolid::~PolyhedralBoundedSolid()
{
    clearVisibilityQueryCache();
    for (long int i = 0; i < polygonsList.size(); ++i) delete polygonsList[i];
    for (long int i = 0; i < edgesList.size(); ++i) delete edgesList[i];
    for (long int i = 0; i < verticesList.size(); ++i) delete verticesList[i];
}

_PolyhedralBoundedSolidFace* PolyhedralBoundedSolid::findFace(int id)
{
    for (long int i = 0; i < polygonsList.size(); ++i) {
        if (polygonsList[i] != 0 && polygonsList[i]->id == id) {
            return polygonsList[i];
        }
    }
    return 0;
}

_PolyhedralBoundedSolidVertex* PolyhedralBoundedSolid::findVertex(int id)
{
    for (long int i = 0; i < verticesList.size(); ++i) {
        if (verticesList[i] != 0 && verticesList[i]->id == id) {
            return verticesList[i];
        }
    }
    return 0;
}

java::ArrayList<_PolyhedralBoundedSolidFace*>&
PolyhedralBoundedSolid::getPolygonsList()
{
    return polygonsList;
}

void PolyhedralBoundedSolid::setPolygonsList(
    java::ArrayList<_PolyhedralBoundedSolidFace*>& v)
{
    polygonsList.clear();
    for (long int i = 0; i < v.size(); ++i) {
        polygonsList.add(v.get(i));
    }
}

java::ArrayList<_PolyhedralBoundedSolidEdge*>&
PolyhedralBoundedSolid::getEdgesList()
{
    return edgesList;
}

void PolyhedralBoundedSolid::setEdgesList(
    java::ArrayList<_PolyhedralBoundedSolidEdge*>& v)
{
    edgesList.clear();
    for (long int i = 0; i < v.size(); ++i) {
        edgesList.add(v.get(i));
    }
}

java::ArrayList<_PolyhedralBoundedSolidVertex*>&
PolyhedralBoundedSolid::getVerticesList()
{
    return verticesList;
}

void PolyhedralBoundedSolid::setVerticesList(
    java::ArrayList<_PolyhedralBoundedSolidVertex*>& v)
{
    verticesList.clear();
    for (long int i = 0; i < v.size(); ++i) {
        verticesList.add(v.get(i));
    }
}

int PolyhedralBoundedSolid::getMaxVertexId() const { return maxVertexId; }
void PolyhedralBoundedSolid::setMaxVertexId(int v) { maxVertexId = v; }
int PolyhedralBoundedSolid::getMaxFaceId() const { return maxFaceId; }
void PolyhedralBoundedSolid::setMaxFaceId(int v) { maxFaceId = v; }

Ray* PolyhedralBoundedSolid::doIntersectionFirstHit(const Ray& inOutRay)
{
    RayHit hit(RayHit::DETAIL_NONE, true);
    if (doIntersectionFirstHit(inOutRay, &hit) && hit.ray() != 0) {
        return new Ray(*hit.ray());
    }
    return 0;
}

bool PolyhedralBoundedSolid::doIntersectionFirstHit(const Ray& inRay, RayHit* outHit)
{
    double minT = DBL_MAX;
    RayHit bestInfo;
    bool found = false;
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(this);

    for (long int i = 0; i < polygonsList.size(); ++i) {
        _PolyhedralBoundedSolidFace* face = polygonsList.get(i);
        if (face == 0) {
            continue;
        }

        InfinitePlane* containingPlane = face->getContainingPlane();
        if (containingPlane == 0) {
            continue;
        }

        RayHit planeHit;
        bool hitsPlane = containingPlane->doIntersectionFirstHit(inRay, &planeHit);
        if (hitsPlane && planeHit.ray() != 0 && planeHit.ray()->getT() < minT) {
            Ray hit = *(planeHit.ray());
            hit = hit.withDirection(hit.getDirection().normalized());
            Vector3Dd p = hit.getOrigin().add(hit.getDirection().multiply(hit.getT()));
            int pos = face->testPointInside(
                p,
                numericContext.bigEpsilon(),
                containingPlane);
            if (pos == Geometry::INSIDE || pos == Geometry::LIMIT) {
                minT = hit.getT();
                bestInfo = planeHit;
                found = true;
            }
        }

        delete containingPlane;
    }

    if (!found) {
        return false;
    }

    if (outHit != 0) {
        outHit->clone(bestInfo);
        outHit->setRay(inRay.withT(minT));
    }
    return true;
}

void PolyhedralBoundedSolid::doExtraInformation(
    const Ray& inRay,
    double inT,
    RayHit* outData)
{
    if (outData == 0) {
        return;
    }
    doIntersectionFirstHit(inRay.withT(inT), outData);
}

double* PolyhedralBoundedSolid::getMinMax()
{
    double* minMax = new double[6];
    double minX = DBL_MAX;
    double minY = DBL_MAX;
    double minZ = DBL_MAX;
    double maxX = -DBL_MAX;
    double maxY = -DBL_MAX;
    double maxZ = -DBL_MAX;

    for (long int i = 0; i < verticesList.size(); ++i) {
        _PolyhedralBoundedSolidVertex* v = verticesList.get(i);
        if (v == 0) {
            continue;
        }
        const Vector3Dd& p = v->position;
        minX = std::min(minX, p.x());
        minY = std::min(minY, p.y());
        minZ = std::min(minZ, p.z());
        maxX = std::max(maxX, p.x());
        maxY = std::max(maxY, p.y());
        maxZ = std::max(maxZ, p.z());
    }

    if (verticesList.size() == 0) {
        minX = minY = minZ = maxX = maxY = maxZ = 0.0;
    }

    minMax[0] = minX;
    minMax[1] = minY;
    minMax[2] = minZ;
    minMax[3] = maxX;
    minMax[4] = maxY;
    minMax[5] = maxZ;
    return minMax;
}

bool PolyhedralBoundedSolid::isValid() const { return modelIsValid; }
void PolyhedralBoundedSolid::setValidationState(bool flag) { modelIsValid = flag; }

void PolyhedralBoundedSolid::merge(PolyhedralBoundedSolid* other)
{
    if (other == 0) return;
    int offsetFacesId = getMaxFaceId();
    int offsetVertexId = getMaxVertexId();

    while (other->getPolygonsList().size() != 0) {
        _PolyhedralBoundedSolidFace* f = other->getPolygonsList()[0];
        f->id += offsetFacesId;
        if (f->id > maxFaceId) maxFaceId = f->id;
        polygonsList.add(f);
        other->getPolygonsList().remove(0L);
    }
    while (other->getEdgesList().size() != 0) {
        edgesList.add(other->getEdgesList()[0]);
        other->getEdgesList().remove(0L);
    }
    while (other->getVerticesList().size() != 0) {
        _PolyhedralBoundedSolidVertex* v = other->getVerticesList()[0];
        v->id += offsetVertexId;
        if (v->id > maxVertexId) maxVertexId = v->id;
        verticesList.add(v);
        other->getVerticesList().remove(0L);
    }
}

void PolyhedralBoundedSolid::computeFaceAabb(
    _PolyhedralBoundedSolidFace* face,
    int index)
{
    double minX = DBL_MAX;
    double minY = DBL_MAX;
    double minZ = DBL_MAX;
    double maxX = -DBL_MAX;
    double maxY = -DBL_MAX;
    double maxZ = -DBL_MAX;

    if (face != 0) {
        for (long int b = 0; b < face->boundariesList.size(); ++b) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(b);
            if (loop == 0 || loop->boundaryStartHalfEdge == 0) {
                continue;
            }

            _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* start = he;
            do {
                if (he->startingVertex != 0) {
                    Vector3Dd q = he->startingVertex->position;
                    minX = std::min(minX, q.x());
                    minY = std::min(minY, q.y());
                    minZ = std::min(minZ, q.z());
                    maxX = std::max(maxX, q.x());
                    maxY = std::max(maxY, q.y());
                    maxZ = std::max(maxZ, q.z());
                }
                he = he->next();
            } while (he != 0 && he != start);
        }
    }

    if (minX == DBL_MAX) {
        minX = minY = minZ = maxX = maxY = maxZ = 0.0;
    }

    int o = index * 6;
    queryFaceAabb[o] = minX;
    queryFaceAabb[o + 1] = minY;
    queryFaceAabb[o + 2] = minZ;
    queryFaceAabb[o + 3] = maxX;
    queryFaceAabb[o + 4] = maxY;
    queryFaceAabb[o + 5] = maxZ;
}

bool PolyhedralBoundedSolid::rayReachesFaceAabb(
    const Vector3Dd& origin,
    double dirX,
    double dirY,
    double dirZ,
    double maxT,
    int faceIndex,
    double pad)
{
    int o = faceIndex * 6;
    double tMin = 0.0;
    double tMax = maxT;

    if (std::abs(dirX) < 1.0e-12) {
        if (origin.x() < queryFaceAabb[o] - pad ||
            origin.x() > queryFaceAabb[o + 3] + pad) {
            return false;
        }
    }
    else {
        double t1 = (queryFaceAabb[o] - pad - origin.x()) / dirX;
        double t2 = (queryFaceAabb[o + 3] + pad - origin.x()) / dirX;
        if (t1 > t2) std::swap(t1, t2);
        if (t1 > tMin) tMin = t1;
        if (t2 < tMax) tMax = t2;
        if (tMin > tMax) return false;
    }

    if (std::abs(dirY) < 1.0e-12) {
        if (origin.y() < queryFaceAabb[o + 1] - pad ||
            origin.y() > queryFaceAabb[o + 4] + pad) {
            return false;
        }
    }
    else {
        double t1 = (queryFaceAabb[o + 1] - pad - origin.y()) / dirY;
        double t2 = (queryFaceAabb[o + 4] + pad - origin.y()) / dirY;
        if (t1 > t2) std::swap(t1, t2);
        if (t1 > tMin) tMin = t1;
        if (t2 < tMax) tMax = t2;
        if (tMin > tMax) return false;
    }

    if (std::abs(dirZ) < 1.0e-12) {
        if (origin.z() < queryFaceAabb[o + 2] - pad ||
            origin.z() > queryFaceAabb[o + 5] + pad) {
            return false;
        }
    }
    else {
        double t1 = (queryFaceAabb[o + 2] - pad - origin.z()) / dirZ;
        double t2 = (queryFaceAabb[o + 5] + pad - origin.z()) / dirZ;
        if (t1 > t2) std::swap(t1, t2);
        if (t1 > tMin) tMin = t1;
        if (t2 < tMax) tMax = t2;
        if (tMin > tMax) return false;
    }

    return true;
}

void PolyhedralBoundedSolid::clearVisibilityQueryCache()
{
    if (queryPlaneCache != 0) {
        for (int i = 0; i < queryFaceCount; ++i) {
            delete queryPlaneCache[i];
        }
        delete[] queryPlaneCache;
        queryPlaneCache = 0;
    }
    delete[] queryFaceAabb;
    queryFaceAabb = 0;
    delete queryNumericContext;
    queryNumericContext = 0;
    queryFaceCount = 0;
}

void PolyhedralBoundedSolid::beginVisibilityQueries()
{
    clearVisibilityQueryCache();

    queryNumericContext = new PolyhedralBoundedSolidNumericPolicy::ToleranceContext(
        PolyhedralBoundedSolidNumericPolicy::forSolid(this));
    queryFaceCount = static_cast<int>(polygonsList.size());
    queryPlaneCache = new InfinitePlane*[queryFaceCount];
    queryFaceAabb = new double[queryFaceCount * 6];

    for (int i = 0; i < queryFaceCount; ++i) {
        queryPlaneCache[i] = 0;
        _PolyhedralBoundedSolidFace* face = polygonsList.get(i);
        if (face != 0) {
            queryPlaneCache[i] = face->getContainingPlane();
        }
        computeFaceAabb(face, i);
    }
}

void PolyhedralBoundedSolid::endVisibilityQueries()
{
    clearVisibilityQueryCache();
}

bool PolyhedralBoundedSolid::visibilityQueriesActive() const
{
    return queryPlaneCache != 0;
}

InfinitePlane* PolyhedralBoundedSolid::cachedFacePlane(int faceIndex)
{
    if (queryPlaneCache != 0 &&
        faceIndex >= 0 &&
        faceIndex < queryFaceCount) {
        return queryPlaneCache[faceIndex];
    }
    if (faceIndex < 0 || faceIndex >= polygonsList.size()) {
        return 0;
    }
    return polygonsList.get(faceIndex)->getContainingPlane();
}

PolyhedralBoundedSolidNumericPolicy::ToleranceContext
PolyhedralBoundedSolid::queryToleranceContext()
{
    if (queryNumericContext != 0) {
        return *queryNumericContext;
    }
    return PolyhedralBoundedSolidNumericPolicy::forSolid(this);
}

bool PolyhedralBoundedSolid::queryRayReachesFace(
    const Vector3Dd& origin,
    double dirX,
    double dirY,
    double dirZ,
    double maxT,
    int faceIndex,
    double pad)
{
    if (queryFaceAabb == 0) {
        return true;
    }
    return rayReachesFaceAabb(
        origin, dirX, dirY, dirZ, maxT, faceIndex, pad);
}

bool PolyhedralBoundedSolid::queryPointNearFace(
    const Vector3Dd& point,
    int faceIndex,
    double pad)
{
    if (queryFaceAabb == 0) {
        return true;
    }
    int o = faceIndex * 6;
    return point.x() >= queryFaceAabb[o] - pad &&
        point.x() <= queryFaceAabb[o + 3] + pad &&
        point.y() >= queryFaceAabb[o + 1] - pad &&
        point.y() <= queryFaceAabb[o + 4] + pad &&
        point.z() >= queryFaceAabb[o + 2] - pad &&
        point.z() <= queryFaceAabb[o + 5] + pad;
}

int PolyhedralBoundedSolid::computeQuantitativeInvisibility(
    const Vector3Dd& origin,
    const Vector3Dd& p)
{
    return PolyhedralBoundedSolidPredicates::quantitativeInvisibility(
        this, origin, p);
}

int PolyhedralBoundedSolid::compareValue(double a, double b, double tolerance)
{
    double delta = std::abs(a - b);
    if (delta < tolerance) return 0;
    return (a > b) ? 1 : -1;
}

void PolyhedralBoundedSolid::revert()
{
    for (long int i = 0; i < polygonsList.size(); ++i) {
        if (polygonsList[i] != 0) {
            polygonsList[i]->revert();
        }
    }
}

PolyhedralBoundedSolid* PolyhedralBoundedSolid::exportToPolyhedralBoundedSolid()
{
    return this;
}
