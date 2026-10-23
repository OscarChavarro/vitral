#include <stddef.h>
#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#include "vsdk/toolkit/environment/geometry/element/Vertex.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
TriangleStripMesh::TriangleStripMesh() : name("default") {}

double* TriangleStripMesh::calculateMinMaxPositions()
{
    double* minMax = new double[6];
    double minX = 1e308, minY = 1e308, minZ = 1e308;
    double maxX = -1e308, maxY = -1e308, maxZ = -1e308;

    for (long int i = 0; i < vertexes.size(); i++) {
        double x = vertexes[i].getPosition().x();
        double y = vertexes[i].getPosition().y();
        double z = vertexes[i].getPosition().z();
        if (x < minX) minX = x; if (y < minY) minY = y; if (z < minZ) minZ = z;
        if (x > maxX) maxX = x; if (y > maxY) maxY = y; if (z > maxZ) maxZ = z;
    }
    minMax[0]=minX; minMax[1]=minY; minMax[2]=minZ;
    minMax[3]=maxX; minMax[4]=maxY; minMax[5]=maxZ;
    return minMax;
}

double* TriangleStripMesh::getMinMax() { return calculateMinMaxPositions(); }

const ArrayList<Vertex>& TriangleStripMesh::getVertexes() const { return vertexes; }
Vertex TriangleStripMesh::getVertexAt(int index) const { return vertexes.get(index); }
void TriangleStripMesh::setVertexes(const ArrayList<Vertex>& v) { vertexes = v; }
void TriangleStripMesh::setStrips(const ArrayList< ArrayList<int> >& indexes) { strips = indexes; }
ArrayList< ArrayList<int> >& TriangleStripMesh::getStrips() { return strips; }

static BOOLEAN intersectTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2, double& t)
{
    Vector3Dd e1 = v1.subtract(v0);
    Vector3Dd e2 = v2.subtract(v0);
    Vector3Dd h = ray.getDirection().crossProduct(e2);
    double a = e1.dotProduct(h);
    if (Cpp98Support::abs(a) < VSDK::EPSILON) return FALSE;
    double f = 1.0 / a;
    Vector3Dd s = ray.getOrigin().subtract(v0);
    double u = f * s.dotProduct(h);
    if (u < 0.0 || u > 1.0) return FALSE;
    Vector3Dd q = s.crossProduct(e1);
    double v = f * ray.getDirection().dotProduct(q);
    if (v < 0.0 || u + v > 1.0) return FALSE;
    double tt = f * e2.dotProduct(q);
    if (tt > VSDK::EPSILON) { t = tt; return TRUE; }
    return FALSE;
}

Ray* TriangleStripMesh::doIntersectionFirstHit(const Ray& inOut_Ray)
{
    RayHit hit;
    if (doIntersectionFirstHit(inOut_Ray, &hit) && hit.getRay() != NULL) {
        return new Ray(*hit.getRay());
    }
    return NULL;
}

BOOLEAN TriangleStripMesh::doIntersectionFirstHit(const Ray& inRay, RayHit* outHit)
{
    BOOLEAN found = FALSE;
    double bestT = 1e308;
    Vector3Dd bestN;

    for (long int s = 0; s < strips.size(); s++) {
        ArrayList<int>& strip = strips[s];
        if (strip.size() < 3) continue;
        for (long int i = 2; i < strip.size(); i++) {
            int i0 = strip[i-2], i1 = strip[i-1], i2 = strip[i];
            if (i % 2 == 0) { int tmp = i1; i1 = i2; i2 = tmp; }
            if (i0 < 0 || i1 < 0 || i2 < 0 ||
                i0 >= (int)vertexes.size() || i1 >= (int)vertexes.size() || i2 >= (int)vertexes.size()) continue;

            const Vector3Dd& v0 = vertexes[i0].getPosition();
            const Vector3Dd& v1 = vertexes[i1].getPosition();
            const Vector3Dd& v2 = vertexes[i2].getPosition();

            double t;
            if (intersectTriangle(inRay, v0, v1, v2, t) && t < bestT) {
                bestT = t;
                bestN = v1.subtract(v0).crossProduct(v2.subtract(v0)).normalized();
                found = TRUE;
            }
        }
    }

    if (!found) return FALSE;
    if (outHit != NULL) {
        if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) outHit->setRay(inRay.withT(bestT));
        else outHit->setHitDistance(bestT);
        if (outHit->needsPoint()) outHit->point = inRay.getOrigin().add(inRay.getDirection().multiply(bestT));
        if (outHit->needsNormal()) outHit->normal = bestN;
    }
    return TRUE;
}

void TriangleStripMesh::doExtraInformation(const Ray& inRay, double inT, RayHit* outData)
{
    if (outData == NULL) return;
    outData->point = inRay.getOrigin().add(inRay.getDirection().multiply(inT));
}

const Class TriangleStripMesh::CLASS = { "TriangleStripMesh", &Surface::CLASS };

const Class *
TriangleStripMesh::getClass() const
{
    return &CLASS;
}
