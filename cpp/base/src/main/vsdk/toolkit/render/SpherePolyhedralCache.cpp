#include <cmath>
#include <map>
#include <mutex>
#include <string>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/render/SpherePolyhedralCache.h"

namespace {
    /// Maximum number of entries kept; when reached, the cache is emptied
    const std::size_t MAX_CACHED_ENTRIES = 256;
    /// Relative distance to a pole under which a vertex is taken as a pole
    const double POLE_TOLERANCE = 1.0e-9;

    std::mutex& cacheMutex()
    {
        static std::mutex mutex;
        return mutex;
    }

    std::map<std::string, SpherePolyhedralCache::Entry*>& entries()
    {
        static std::map<std::string, SpherePolyhedralCache::Entry*> map;
        return map;
    }

    void releaseAll(std::map<std::string, SpherePolyhedralCache::Entry*>& map)
    {
        for (std::map<std::string, SpherePolyhedralCache::Entry*>::iterator it = map.begin();
             it != map.end(); ++it) {
            delete it->second;
        }
        map.clear();
    }

    /**
    Makes the angles of the corners of a triangle continuous across the seam
    (a triangle whose angles span more than half a turn crosses it, so the
    small ones are taken one turn further), and gives the poles the mean
    angle of the other corners.
    */
    void makeContinuous(double theta[3], const bool pole[3])
    {
        double min = 1.0e300;
        double max = -1.0e300;

        for (int k = 0; k < 3; k++) {
            if (!pole[k]) {
                min = std::fmin(min, theta[k]);
                max = std::fmax(max, theta[k]);
            }
        }
        if (max - min > M_PI) {
            for (int k = 0; k < 3; k++) {
                if (!pole[k] && theta[k] < M_PI) {
                    theta[k] += 2 * M_PI;
                }
            }
        }

        double sum = 0;
        int count = 0;

        for (int k = 0; k < 3; k++) {
            if (!pole[k]) {
                sum += theta[k];
                count++;
            }
        }
        for (int k = 0; k < 3; k++) {
            if (pole[k]) {
                theta[k] = count > 0 ? sum / count : 0;
            }
        }
    }

    /**
    @return the vertices of the faces of the solid as triangles (a fan per
    face, from its outer loop), three consecutive per triangle, each one
    counterclockwise seen from outside the sphere
    */
    std::vector<Vector3Dd> collectTriangles(PolyhedralBoundedSolid* solid)
    {
        std::vector<Vector3Dd> triangles;
        java::ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();

        for (long int f = 0; f < faces.size(); f++) {
            _PolyhedralBoundedSolidFace* face = faces.get(f);

            if (face == nullptr || face->boundariesList.size() < 1) {
                continue;
            }
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(0);
            _PolyhedralBoundedSolidHalfEdge* start =
                loop != nullptr ? loop->boundaryStartHalfEdge : nullptr;
            std::vector<Vector3Dd> polygon;

            if (start == nullptr) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                polygon.push_back(he->startingVertex->position);
                he = he->next();
            } while (he != start && he != nullptr);

            for (std::size_t i = 1; i + 1 < polygon.size(); i++) {
                const Vector3Dd& a = polygon[0];
                const Vector3Dd& b = polygon[i];
                const Vector3Dd& c = polygon[i + 1];
                Vector3Dd normal = b.subtract(a).crossProduct(c.subtract(a));
                Vector3Dd center = a.add(b).add(c);

                triangles.push_back(a);
                if (normal.dotProduct(center) >= 0) {
                    triangles.push_back(b);
                    triangles.push_back(c);
                }
                else {
                    triangles.push_back(c);
                    triangles.push_back(b);
                }
            }
        }
        return triangles;
    }

    void put(std::vector<float>& array, std::size_t index, const Vector3Dd& v)
    {
        array[3 * index] = (float)v.x();
        array[3 * index + 1] = (float)v.y();
        array[3 * index + 2] = (float)v.z();
    }
}

SpherePolyhedralCache::Entry::Entry(double radius, int meridians, int parallels,
                                    PolyhedralBoundedSolid* solid)
    : radius(radius), meridians(meridians), parallels(parallels), solid(solid)
{
}

SpherePolyhedralCache::Entry::~Entry()
{
    delete solid;
}

double SpherePolyhedralCache::Entry::getRadius() const { return radius; }
int SpherePolyhedralCache::Entry::getMeridians() const { return meridians; }
int SpherePolyhedralCache::Entry::getParallels() const { return parallels; }
PolyhedralBoundedSolid* SpherePolyhedralCache::Entry::getSolid() const { return solid; }
int SpherePolyhedralCache::Entry::getVertexCount() const { return (int)(positions.size() / 3); }
const std::vector<float>& SpherePolyhedralCache::Entry::getPositions() const { return positions; }
const std::vector<float>& SpherePolyhedralCache::Entry::getNormals() const { return normals; }
const std::vector<float>& SpherePolyhedralCache::Entry::getUvs() const { return uvs; }
const std::vector<float>& SpherePolyhedralCache::Entry::getTangents() const { return tangents; }
const std::vector<float>& SpherePolyhedralCache::Entry::getBiNormals() const { return biNormals; }

const SpherePolyhedralCache::Entry* SpherePolyhedralCache::obtain(
    const Sphere* sphere, int meridians, int parallels)
{
    std::lock_guard<std::mutex> lock(cacheMutex());
    std::map<std::string, Entry*>& map = entries();
    std::string key = std::to_string(sphere->getRadius()) + "/" +
        std::to_string(meridians) + "/" + std::to_string(parallels);
    std::map<std::string, Entry*>::iterator it = map.find(key);

    if (it != map.end()) {
        return it->second;
    }
    Entry* entry = build(sphere, meridians, parallels);
    if (map.size() >= MAX_CACHED_ENTRIES) {
        releaseAll(map);
    }
    map[key] = entry;
    return entry;
}

void SpherePolyhedralCache::clear()
{
    std::lock_guard<std::mutex> lock(cacheMutex());
    releaseAll(entries());
}

SpherePolyhedralCache::Entry* SpherePolyhedralCache::build(
    const Sphere* sphere, int meridians, int parallels)
{
    // The parametric evaluators of Sphere are not const qualified
    Sphere parametric(sphere->getRadius());
    PolyhedralBoundedSolid* solid =
        parametric.exportToPolyhedralBoundedSolid(meridians, parallels);
    std::vector<Vector3Dd> triangles = collectTriangles(solid);
    std::size_t vertexCount = triangles.size();
    double radius = sphere->getRadius();
    Entry* entry = new Entry(radius, meridians, parallels, solid);

    entry->positions.resize(vertexCount * 3);
    entry->normals.resize(vertexCount * 3);
    entry->uvs.resize(vertexCount * 2);
    entry->tangents.resize(vertexCount * 3);
    entry->biNormals.resize(vertexCount * 3);

    for (std::size_t t = 0; t + 2 < vertexCount; t += 3) {
        double theta[3];
        double phi[3];
        bool pole[3];

        for (int k = 0; k < 3; k++) {
            const Vector3Dd& p = triangles[t + k];
            double z = radius > 0 ? p.z() / radius : 0;

            phi[k] = std::asin(std::fmax(-1.0, std::fmin(1.0, z)));
            pole[k] = std::fabs(std::fabs(z) - 1.0) < POLE_TOLERANCE;
            theta[k] = std::atan2(-p.y(), p.x());
            if (theta[k] < 0) {
                theta[k] += 2 * M_PI;
            }
        }
        makeContinuous(theta, pole);

        for (int k = 0; k < 3; k++) {
            std::size_t i = t + k;

            put(entry->positions, i, triangles[i]);
            put(entry->normals, i, parametric.sphereNormal(theta[k], phi[k]));
            put(entry->tangents, i, parametric.sphereTangent(theta[k], phi[k]));
            put(entry->biNormals, i, parametric.sphereBinormal(theta[k], phi[k]));
            entry->uvs[2 * i] = (float)(1.0 - theta[k] / (2 * M_PI));
            entry->uvs[2 * i + 1] = (float)((phi[k] + M_PI / 2) / M_PI);
        }
    }
    return entry;
}
