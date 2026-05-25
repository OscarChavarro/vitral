#include "vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.h"
#include "vsdk/toolkit/environment/geometry/elements/Vertex.h"
#include "vsdk/toolkit/environment/geometry/elements/Ray.h"
#include "vsdk/toolkit/environment/geometry/elements/RayHit.h"
#include "vsdk/toolkit/common/VSDK.h"
#include <cmath>

TriangleStripMesh::TriangleStripMesh() : name("default") {}

double* TriangleStripMesh::calculateMinMaxPositions()
{
    double* minMax = new double[6];
    double minX = 1e308, minY = 1e308, minZ = 1e308;
    double maxX = -1e308, maxY = -1e308, maxZ = -1e308;

    for (size_t i = 0; i < vertexes.size(); i++) {
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

const std::vector<Vertex>& TriangleStripMesh::getVertexes() const { return vertexes; }
const Vertex& TriangleStripMesh::getVertexAt(int index) const { return vertexes[index]; }
void TriangleStripMesh::setVertexes(const std::vector<Vertex>& v) { vertexes = v; }
void TriangleStripMesh::setStrips(const std::vector< std::vector<int> >& indexes) { strips = indexes; }
const std::vector< std::vector<int> >& TriangleStripMesh::getStrips() const { return strips; }

static bool intersectTriangle(const Ray& ray, const Vector3Dd& v0, const Vector3Dd& v1, const Vector3Dd& v2, double& t)
{
    Vector3Dd e1 = v1.subtract(v0);
    Vector3Dd e2 = v2.subtract(v0);
    Vector3Dd h = ray.direction().crossProduct(e2);
    double a = e1.dotProduct(h);
    if (std::abs(a) < VSDK::EPSILON) return false;
    double f = 1.0 / a;
    Vector3Dd s = ray.origin().subtract(v0);
    double u = f * s.dotProduct(h);
    if (u < 0.0 || u > 1.0) return false;
    Vector3Dd q = s.crossProduct(e1);
    double v = f * ray.direction().dotProduct(q);
    if (v < 0.0 || u + v > 1.0) return false;
    double tt = f * e2.dotProduct(q);
    if (tt > VSDK::EPSILON) { t = tt; return true; }
    return false;
}

Ray* TriangleStripMesh::doIntersection(const Ray& inOut_Ray)
{
    RayHit hit;
    if (doIntersection(inOut_Ray, &hit) && hit.ray() != nullptr) {
        return new Ray(*hit.ray());
    }
    return nullptr;
}

bool TriangleStripMesh::doIntersection(const Ray& inRay, RayHit* outHit)
{
    bool found = false;
    double bestT = 1e308;
    Vector3Dd bestN;

    for (size_t s = 0; s < strips.size(); s++) {
        const std::vector<int>& strip = strips[s];
        if (strip.size() < 3) continue;
        for (size_t i = 2; i < strip.size(); i++) {
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
                found = true;
            }
        }
    }

    if (!found) return false;
    if (outHit != nullptr) {
        if (outHit->shouldStoreRay() || outHit->needsAnySurfaceData()) outHit->setRay(inRay.withT(bestT));
        else outHit->setHitDistance(bestT);
        if (outHit->needsPoint()) outHit->p = inRay.origin().add(inRay.direction().multiply(bestT));
        if (outHit->needsNormal()) outHit->n = bestN;
    }
    return true;
}

void TriangleStripMesh::doExtraInformation(const Ray& inRay, double inT, RayHit* outData)
{
    if (outData == nullptr) return;
    outData->p = inRay.origin().add(inRay.direction().multiply(inT));
}
