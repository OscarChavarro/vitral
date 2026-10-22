#include <stddef.h>
#include <math.h>
#include "java/util/TreeMap.h"
#include "java/util/concurrent/locks/ReentrantLock.h"
#include "java/lang/String.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/render/SpherePolyhedralCache.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

/// Maximum number of entries kept; when reached, the cache is emptied
static const long MAX_CACHED_ENTRIES = 256;
/// Relative distance to a pole under which a vertex is taken as a pole
static const double POLE_TOLERANCE = 1.0e-9;

static ReentrantLock& cacheLock()
{
    static ReentrantLock lock;
    return lock;
}

static TreeMap<String, SpherePolyhedralCache::Entry*>& entries()
{
    static TreeMap<String, SpherePolyhedralCache::Entry*> map;
    return map;
}

static void releaseAll(TreeMap<String, SpherePolyhedralCache::Entry*>& map)
{
    for (long i = 0; i < map.size(); i++) {
        delete map.getValue(i);
    }
    map.clear();
}

/**
Makes the angles of the corners of a triangle continuous across the seam
(a triangle whose angles span more than half a turn crosses it, so the
small ones are taken one turn further), and gives the poles the mean
angle of the other corners.
*/
static void makeContinuous(double theta[3], const bool pole[3])
{
    double min = 1.0e300;
    double max = -1.0e300;

    for (int k = 0; k < 3; k++) {
        if (!pole[k]) {
            min = Cpp98Support::fmin(min, theta[k]);
            max = Cpp98Support::fmax(max, theta[k]);
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
static ArrayList<Vector3Dd> collectTriangles(PolyhedralBoundedSolid* solid)
{
    ArrayList<Vector3Dd> triangles;
    ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid->getPolygonsList();

    for (long int f = 0; f < faces.size(); f++) {
        _PolyhedralBoundedSolidFace* face = faces.get(f);

        if (face == NULL || face->boundariesList.size() < 1) {
            continue;
        }
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(0);
        _PolyhedralBoundedSolidHalfEdge* start =
            loop != NULL ? loop->boundaryStartHalfEdge : NULL;
        ArrayList<Vector3Dd> polygon;

        if (start == NULL) {
            continue;
        }
        _PolyhedralBoundedSolidHalfEdge* he = start;
        do {
            polygon.add(he->startingVertex->position);
            he = he->next();
        } while (he != start && he != NULL);

        for (long i = 1; i + 1 < polygon.size(); i++) {
            const Vector3Dd& a = polygon[0];
            const Vector3Dd& b = polygon[i];
            const Vector3Dd& c = polygon[i + 1];
            Vector3Dd normal = b.subtract(a).crossProduct(c.subtract(a));
            Vector3Dd center = a.add(b).add(c);

            triangles.add(a);
            if (normal.dotProduct(center) >= 0) {
                triangles.add(b);
                triangles.add(c);
            }
            else {
                triangles.add(c);
                triangles.add(b);
            }
        }
    }
    return triangles;
}

/**
Sets the size of the array to `size` zeros.
*/
static void fill(ArrayList<float>& array, long size)
{
    array.clear();
    array.reserve(size);
    for (long i = 0; i < size; i++) {
        array.add(0.0f);
    }
}

static void put(ArrayList<float>& array, long index, const Vector3Dd& v)
{
    array[3 * index] = (float)v.x();
    array[3 * index + 1] = (float)v.y();
    array[3 * index + 2] = (float)v.z();
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
const ArrayList<float>& SpherePolyhedralCache::Entry::getPositions() const { return positions; }
const ArrayList<float>& SpherePolyhedralCache::Entry::getNormals() const { return normals; }
const ArrayList<float>& SpherePolyhedralCache::Entry::getUvs() const { return uvs; }
const ArrayList<float>& SpherePolyhedralCache::Entry::getTangents() const { return tangents; }
const ArrayList<float>& SpherePolyhedralCache::Entry::getBiNormals() const { return biNormals; }

const SpherePolyhedralCache::Entry* SpherePolyhedralCache::obtain(
    const Sphere* sphere, int meridians, int parallels)
{
    ReentrantLockGuard guard(cacheLock());
    TreeMap<String, Entry*>& map = entries();
    String key = Cpp98Support::toString(sphere->getRadius()) + "/" +
        Cpp98Support::toString(meridians) + "/" + Cpp98Support::toString(parallels);
    Entry** known = map.get(key);

    if (known != NULL) {
        return *known;
    }
    Entry* entry = build(sphere, meridians, parallels);
    if (map.size() >= MAX_CACHED_ENTRIES) {
        releaseAll(map);
    }
    map.put(key, entry);
    return entry;
}

void SpherePolyhedralCache::clear()
{
    ReentrantLockGuard guard(cacheLock());
    releaseAll(entries());
}

SpherePolyhedralCache::Entry* SpherePolyhedralCache::build(
    const Sphere* sphere, int meridians, int parallels)
{
    // The parametric evaluators of Sphere are not const qualified
    Sphere parametric(sphere->getRadius());
    PolyhedralBoundedSolid* solid =
        parametric.exportToPolyhedralBoundedSolid(meridians, parallels);
    ArrayList<Vector3Dd> triangles = collectTriangles(solid);
    long vertexCount = triangles.size();
    double radius = sphere->getRadius();
    Entry* entry = new Entry(radius, meridians, parallels, solid);

    fill(entry->positions, vertexCount * 3);
    fill(entry->normals, vertexCount * 3);
    fill(entry->uvs, vertexCount * 2);
    fill(entry->tangents, vertexCount * 3);
    fill(entry->biNormals, vertexCount * 3);

    for (long t = 0; t + 2 < vertexCount; t += 3) {
        double theta[3];
        double phi[3];
        bool pole[3];

        for (int k = 0; k < 3; k++) {
            const Vector3Dd& p = triangles[t + k];
            double z = radius > 0 ? p.z() / radius : 0;

            phi[k] = asin(Cpp98Support::fmax(-1.0, Cpp98Support::fmin(1.0, z)));
            pole[k] = fabs(fabs(z) - 1.0) < POLE_TOLERANCE;
            theta[k] = atan2(-p.y(), p.x());
            if (theta[k] < 0) {
                theta[k] += 2 * M_PI;
            }
        }
        makeContinuous(theta, pole);

        for (int k = 0; k < 3; k++) {
            long i = t + k;

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
