#include <GL/glew.h>
#include <GL/glu.h>
#include <cmath>
#include <cstdarg>
#include <cstdio>
#include <deque>
#include <map>
#include <set>
#include <string>
#include <typeinfo>
#include <vector>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/ParametricCurve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/geometry/volume/Torus.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/PointLight.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshBuilder.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SphereRenderer.h"

/*
Geometries other than spheres and curves are presented as triangle meshes
drawn by `OpenGL4MeshRenderer` with the GLSL programs of the shading type,
as the Java GL4 pipeline does (see `Jogl4GeometryRenderer`). Meshes are
cached by a key made of the parameters of the geometry (or its identity, for
the ones that are not edited while shown), and built again when it changes.
*/
namespace {

const int SLICES = 32;
const int TORUS_MAJOR_SEGMENTS = 48;
const int TORUS_MINOR_SEGMENTS = 24;
const size_t MAX_CACHED_MESHES = 256;
/// Side of the square shown for an infinite plane
const double INFINITE_PLANE_SIZE = 20.0;
/// Cells along each side of the square shown for an infinite plane
const int INFINITE_PLANE_TILES = 10;

std::map<std::string, OpenGL4MeshRenderer::Mesh*> meshes;
std::set<std::string> reportedUnsupported;

void releaseMeshes()
{
    std::map<std::string, OpenGL4MeshRenderer::Mesh*>::iterator i;
    for ( i = meshes.begin(); i != meshes.end(); ++i ) {
        OpenGL4MeshRenderer::release(i->second);
        delete i->second;
    }
    meshes.clear();
}

std::string format(const char* pattern, ...)
    __attribute__((format(printf, 1, 2)));

std::string format(const char* pattern, ...)
{
    char buffer[512];
    va_list args;
    va_start(args, pattern);
    vsnprintf(buffer, sizeof(buffer), pattern, args);
    va_end(args);
    return buffer;
}

double maxAbsExtent(double* minmax)
{
    double size = 1.0;
    if ( minmax != nullptr ) {
        size = std::fabs(minmax[3] - minmax[0]);
        if ( std::fabs(minmax[4] - minmax[1]) > size ) size = std::fabs(minmax[4] - minmax[1]);
        if ( std::fabs(minmax[5] - minmax[2]) > size ) size = std::fabs(minmax[5] - minmax[2]);
        delete[] minmax;
    }
    return size;
}

//= Primitives ============================================================

OpenGL4MeshRenderer::Mesh* buildCone(const Cone& cone)
{
    double r1 = cone.getBottomRadius();
    double r2 = cone.getTopRadius();
    double h = cone.getHeight();
    double size = r1 > r2 ? r1 : r2;
    OpenGL4MeshBuilder builder(size > h ? size : h);

    builder.addFrustum(0, r1, h, r2, SLICES);
    builder.addDisk(0, 0, r1, false, SLICES);
    if ( r2 > 0.0 ) {
        builder.addDisk(h, 0, r2, true, SLICES);
    }
    return builder.build();
}

OpenGL4MeshRenderer::Mesh* buildArrow(const Arrow& arrow)
{
    double h1 = arrow.getBaseLength();
    double h2 = arrow.getHeadLength();
    double r1 = arrow.getBaseRadius();
    double r2 = arrow.getHeadRadius();
    OpenGL4MeshBuilder builder(h1 + h2);

    builder.addFrustum(0, r1, h1, r1, SLICES);
    builder.addDisk(0, 0, r1, false, SLICES);
    builder.addFrustum(h1, r2, h1 + h2, 0, SLICES);
    builder.addDisk(h1, r1, r2, false, SLICES);
    return builder.build();
}

/**
Adds a face whose vertices are given in the order upper left, lower left,
lower right, upper right as seen from outside, wound so its front side looks
outside.
*/
void addBoxFace(OpenGL4MeshBuilder& builder, const Vector3Dd& n,
                const Vector3Dd& a, const Vector3Dd& b,
                const Vector3Dd& c, const Vector3Dd& d)
{
    Vector3Dd winding = b.subtract(a).crossProduct(c.subtract(b));

    if ( winding.dotProduct(n) >= 0 ) {
        builder.addQuad(a, n, 0, 1, b, n, 0, 0, c, n, 1, 0, d, n, 1, 1);
    }
    else {
        builder.addQuad(d, n, 1, 1, c, n, 1, 0, b, n, 0, 0, a, n, 0, 1);
    }
}

OpenGL4MeshRenderer::Mesh* buildBox(const Box& box)
{
    Vector3Dd size = box.getSize();
    double hx = size.x() / 2, hy = size.y() / 2, hz = size.z() / 2;
    double half = hx > hy ? hx : hy;
    OpenGL4MeshBuilder builder((half > hz ? half : hz) * 2);

    addBoxFace(builder, Vector3Dd(0, 0, -1),
        Vector3Dd(-hx, -hy, -hz), Vector3Dd(-hx, hy, -hz),
        Vector3Dd(hx, hy, -hz), Vector3Dd(hx, -hy, -hz));
    addBoxFace(builder, Vector3Dd(0, 0, 1),
        Vector3Dd(-hx, -hy, hz), Vector3Dd(hx, -hy, hz),
        Vector3Dd(hx, hy, hz), Vector3Dd(-hx, hy, hz));
    addBoxFace(builder, Vector3Dd(0, -1, 0),
        Vector3Dd(-hx, -hy, hz), Vector3Dd(-hx, -hy, -hz),
        Vector3Dd(hx, -hy, -hz), Vector3Dd(hx, -hy, hz));
    addBoxFace(builder, Vector3Dd(-1, 0, 0),
        Vector3Dd(-hx, hy, hz), Vector3Dd(-hx, hy, -hz),
        Vector3Dd(-hx, -hy, -hz), Vector3Dd(-hx, -hy, hz));
    addBoxFace(builder, Vector3Dd(0, 1, 0),
        Vector3Dd(hx, hy, hz), Vector3Dd(hx, hy, -hz),
        Vector3Dd(-hx, hy, -hz), Vector3Dd(-hx, hy, hz));
    addBoxFace(builder, Vector3Dd(1, 0, 0),
        Vector3Dd(hx, -hy, hz), Vector3Dd(hx, -hy, -hz),
        Vector3Dd(hx, hy, -hz), Vector3Dd(hx, hy, hz));
    return builder.build();
}

Vector3Dd torusPoint(double bigR, double smallR, double w, double v)
{
    return Vector3Dd((bigR + smallR * std::cos(v)) * std::cos(w),
                     (bigR + smallR * std::cos(v)) * std::sin(w),
                     smallR * std::sin(v));
}

Vector3Dd torusNormal(double w, double v)
{
    return Vector3Dd(std::cos(v) * std::cos(w), std::cos(v) * std::sin(w),
                     std::sin(v));
}

OpenGL4MeshRenderer::Mesh* buildTorus(const Torus& torus)
{
    double bigR = torus.getMajorRadius();
    double smallR = torus.getMinorRadius();
    OpenGL4MeshBuilder builder(bigR + smallR);
    const int nw = TORUS_MAJOR_SEGMENTS;
    const int nv = TORUS_MINOR_SEGMENTS;

    for ( int i = 0; i < nw; i++ ) {
        double w0 = 2 * M_PI * i / nw;
        double w1 = 2 * M_PI * (i + 1) / nw;
        for ( int j = 0; j < nv; j++ ) {
            double v0 = 2 * M_PI * j / nv;
            double v1 = 2 * M_PI * (j + 1) / nv;
            builder.addQuad(
                torusPoint(bigR, smallR, w0, v0), torusNormal(w0, v0),
                (double)i / nw, (double)j / nv,
                torusPoint(bigR, smallR, w1, v0), torusNormal(w1, v0),
                (double)(i + 1) / nw, (double)j / nv,
                torusPoint(bigR, smallR, w1, v1), torusNormal(w1, v1),
                (double)(i + 1) / nw, (double)(j + 1) / nv,
                torusPoint(bigR, smallR, w0, v1), torusNormal(w0, v1),
                (double)i / nw, (double)(j + 1) / nv);
        }
    }
    return builder.build();
}

//= Meshes and surfaces ===================================================

void addTriangleMesh(OpenGL4MeshBuilder& builder, TriangleMesh& mesh)
{
    java::ArrayList<double>& p = mesh.getVertexPositions();
    java::ArrayList<double>& n = mesh.getVertexNormals();
    java::ArrayList<double>& uv = mesh.getVertexUvs();
    java::ArrayList<int>& idx = mesh.getTriangleIndexes();

    for ( long t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        double u[3] = { 0, 0, 0 };
        double v[3] = { 0, 0, 0 };
        for ( int k = 0; k < 3; k++ ) {
            long i = idx[t + k];
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            nor[k] = n.size() >= 3 * i + 3 ?
                Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2]) :
                Vector3Dd(0, 0, 1);
            if ( uv.size() >= 2 * i + 2 ) {
                u[k] = uv[2 * i];
                v[k] = uv[2 * i + 1];
            }
        }
        builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                            pos[2], nor[2], u[2], v[2]);
    }
}

OpenGL4MeshRenderer::Mesh* buildTriangleMesh(TriangleMesh& mesh)
{
    if ( mesh.getVertexPositions().size() == 0 ||
         mesh.getTriangleIndexes().size() < 3 ) {
        return nullptr;
    }
    OpenGL4MeshBuilder builder(maxAbsExtent(mesh.getMinMax()));
    addTriangleMesh(builder, mesh);
    return builder.build();
}

/**
Groups of meshes and quad meshes (i.e. imported objects), through the
triangle meshes `GeometryTriangulator` exports for them.
*/
OpenGL4MeshRenderer::Mesh* buildTriangulatedGeometry(Geometry* geometry)
{
    TriangleMeshGroup group;
    if ( !GeometryTriangulator::exportToTriangleMeshGroup(geometry, group) ||
         group.getMeshes().size() == 0 ) {
        return nullptr;
    }
    OpenGL4MeshBuilder builder(maxAbsExtent(geometry->getMinMax()));
    for ( long i = 0; i < group.getMeshes().size(); i++ ) {
        addTriangleMesh(builder, group.getMeshes()[i]);
    }
    return builder.build();
}

/**
The height field is drawn from its internal triangle mesh, visible from both
sides, with texture coordinates spanning its (x, y) bounds.
*/
OpenGL4MeshRenderer::Mesh* buildFunctionalExplicitSurface(
    FunctionalExplicitSurface& surface)
{
    TriangleMesh* mesh = surface.getInternalTriangleMesh();
    if ( mesh == nullptr ) {
        return nullptr;
    }
    java::ArrayList<double>& p = mesh->getVertexPositions();
    java::ArrayList<double>& n = mesh->getVertexNormals();
    java::ArrayList<int>& idx = mesh->getTriangleIndexes();
    if ( p.size() == 0 || idx.size() < 3 ) {
        return nullptr;
    }

    double sizeX = surface.getMaxXBound() - surface.getMinXBound();
    double sizeY = surface.getMaxYBound() - surface.getMinYBound();
    double sizeZ = surface.getMaxZBound() - surface.getMinZBound();
    double size = std::fabs(sizeX);
    if ( std::fabs(sizeY) > size ) size = std::fabs(sizeY);
    if ( std::fabs(sizeZ) > size ) size = std::fabs(sizeZ);
    OpenGL4MeshBuilder builder(size);
    builder.setDoubleSided(true);

    for ( long t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        bool hasNormal[3];
        double u[3];
        double v[3];
        for ( int k = 0; k < 3; k++ ) {
            long i = idx[t + k];
            pos[k] = Vector3Dd(p[3 * i], p[3 * i + 1], p[3 * i + 2]);
            hasNormal[k] = n.size() >= 3 * i + 3;
            if ( hasNormal[k] ) {
                nor[k] = Vector3Dd(n[3 * i], n[3 * i + 1], n[3 * i + 2]);
            }
            u[k] = sizeX != 0 ? (pos[k].x() - surface.getMinXBound()) / sizeX : 0;
            v[k] = sizeY != 0 ? (pos[k].y() - surface.getMinYBound()) / sizeY : 0;
        }
        Vector3Dd face = pos[1].subtract(pos[0]).crossProduct(pos[2].subtract(pos[0]));
        if ( face.length() < 1e-15 ) {
            continue;
        }
        for ( int k = 0; k < 3; k++ ) {
            if ( !hasNormal[k] || !(nor[k].length() > 1e-12) ) {
                nor[k] = face;
            }
        }
        // The front side is the one the normals point to
        if ( nor[0].add(nor[1]).add(nor[2]).dotProduct(face) >= 0 ) {
            builder.addTriangle(pos[0], nor[0], u[0], v[0], pos[1], nor[1], u[1], v[1],
                                pos[2], nor[2], u[2], v[2]);
        }
        else {
            builder.addTriangle(pos[2], nor[2], u[2], v[2], pos[1], nor[1], u[1], v[1],
                                pos[0], nor[0], u[0], v[0]);
        }
    }
    return builder.build();
}

Vector3Dd validNormal(const Vector3Dd& normal, const Vector3Dd& fallback)
{
    if ( !(normal.length() > 1e-12) ) {
        return fallback.normalized();
    }
    return normal;
}

/**
The patch is sampled in a grid of its approximation steps, visible from both
sides. Normals it can not give (degenerate points) are replaced by the one
of the cell, and each cell is wound so its front side is the one its normals
point to.
*/
OpenGL4MeshRenderer::Mesh* buildBiCubicPatch(ParametricBiCubicPatch& patch)
{
    int steps = patch.getApproximationSteps() > 1 ? patch.getApproximationSteps() : 1;
    int n = steps + 1;
    std::vector<Vector3Dd> points((size_t)n * n);
    std::vector<Vector3Dd> normals((size_t)n * n);

    for ( int i = 0; i < n; i++ ) {
        double s = (double)i / steps;
        for ( int j = 0; j < n; j++ ) {
            double t = (double)j / steps;
            patch.evaluate(points[(size_t)i * n + j], s, t);
            normals[(size_t)i * n + j] = patch.evaluateNormal(s, t);
        }
    }

    OpenGL4MeshBuilder builder(maxAbsExtent(patch.getMinMax()));
    builder.setDoubleSided(true);
    for ( int i = 0; i < steps; i++ ) {
        for ( int j = 0; j < steps; j++ ) {
            const Vector3Dd& p00 = points[(size_t)i * n + j];
            const Vector3Dd& p10 = points[(size_t)(i + 1) * n + j];
            const Vector3Dd& p11 = points[(size_t)(i + 1) * n + j + 1];
            const Vector3Dd& p01 = points[(size_t)i * n + j + 1];
            Vector3Dd face = p11.subtract(p00).crossProduct(p01.subtract(p10));
            if ( !(face.length() > 1e-15) ) {
                continue;
            }
            Vector3Dd n00 = validNormal(normals[(size_t)i * n + j], face);
            Vector3Dd n10 = validNormal(normals[(size_t)(i + 1) * n + j], face);
            Vector3Dd n11 = validNormal(normals[(size_t)(i + 1) * n + j + 1], face);
            Vector3Dd n01 = validNormal(normals[(size_t)i * n + j + 1], face);
            double u0 = (double)i / steps, u1 = (double)(i + 1) / steps;
            double v0 = (double)j / steps, v1 = (double)(j + 1) / steps;

            if ( n00.add(n10).add(n11).add(n01).dotProduct(face) >= 0 ) {
                builder.addQuad(p00, n00, u0, v0, p10, n10, u1, v0,
                                p11, n11, u1, v1, p01, n01, u0, v1);
            }
            else {
                builder.addQuad(p01, n01, u0, v1, p11, n11, u1, v1,
                                p10, n10, u1, v0, p00, n00, u0, v0);
            }
        }
    }
    return builder.build();
}

/**
@param outFrame the point of the plane closest to the origin, two
orthonormal directions (u, v) on it and its normal n, (u, v, n) right handed
*/
void calculatePlaneFrame(const InfinitePlane& plane, Vector3Dd outFrame[4])
{
    Vector3Dd n = plane.getNormal();
    double length = std::sqrt(plane.getA() * plane.getA() +
        plane.getB() * plane.getB() + plane.getC() * plane.getC());
    // The plane is a x + b y + c z + d = 0; (a, b, c) may not be unitary
    Vector3Dd center = n.multiply(length > 0 ? -plane.getD() / length : 0);
    Vector3Dd reference = std::fabs(n.z()) < 0.9 ?
        Vector3Dd(0, 0, 1) : Vector3Dd(1, 0, 0);
    Vector3Dd u = reference.crossProduct(n).normalized();

    outFrame[0] = center;
    outFrame[1] = u;
    outFrame[2] = n.crossProduct(u).normalized();
    outFrame[3] = n;
}

/**
As in Java, a square of `INFINITE_PLANE_SIZE` units centered at the point of
the plane closest to the origin is shown, visible from both sides.
*/
OpenGL4MeshRenderer::Mesh* buildInfinitePlane(const InfinitePlane& plane)
{
    Vector3Dd frame[4];
    calculatePlaneFrame(plane, frame);
    double h = INFINITE_PLANE_SIZE / 2;
    double d = INFINITE_PLANE_SIZE / INFINITE_PLANE_TILES;
    OpenGL4MeshBuilder builder(INFINITE_PLANE_SIZE);
    builder.setDoubleSided(true);

    for ( int i = 0; i < INFINITE_PLANE_TILES; i++ ) {
        double a0 = -h + i * d, a1 = a0 + d;
        for ( int j = 0; j < INFINITE_PLANE_TILES; j++ ) {
            double b0 = -h + j * d, b1 = b0 + d;
            double t = INFINITE_PLANE_TILES;
            builder.addQuad(
                frame[0].add(frame[1].multiply(a0)).add(frame[2].multiply(b0)), frame[3],
                i / t, j / t,
                frame[0].add(frame[1].multiply(a1)).add(frame[2].multiply(b0)), frame[3],
                (i + 1) / t, j / t,
                frame[0].add(frame[1].multiply(a1)).add(frame[2].multiply(b1)), frame[3],
                (i + 1) / t, (j + 1) / t,
                frame[0].add(frame[1].multiply(a0)).add(frame[2].multiply(b1)), frame[3],
                i / t, (j + 1) / t);
        }
    }
    return builder.build();
}

/**
@param outMinMax bounding box of the square shown for an infinite plane (its
own bounding volume is infinite)
*/
void calculateInfinitePlaneShownMinMax(const InfinitePlane& plane,
                                       double outMinMax[6])
{
    Vector3Dd frame[4];
    calculatePlaneFrame(plane, frame);
    double h = INFINITE_PLANE_SIZE / 2;

    for ( int k = 0; k < 3; k++ ) {
        outMinMax[k] = 1e308;
        outMinMax[k + 3] = -1e308;
    }
    for ( int i = -1; i <= 1; i += 2 ) {
        for ( int j = -1; j <= 1; j += 2 ) {
            Vector3Dd p = frame[0].add(frame[1].multiply(i * h))
                .add(frame[2].multiply(j * h));
            double c[3] = { p.x(), p.y(), p.z() };
            for ( int k = 0; k < 3; k++ ) {
                if ( c[k] < outMinMax[k] ) outMinMax[k] = c[k];
                if ( c[k] > outMinMax[k + 3] ) outMinMax[k + 3] = c[k];
            }
        }
    }
}

//= Boundary representations ==============================================

/**
Triangles produced by the GLU tessellator for the faces of a boundary
representation, as `Jogl4PolyhedralBoundedSolidRenderer` does in Java: its
faces are planar polygons, maybe with holes (several loops).
*/
struct SolidTessellation {
    std::vector<Vector3Dd> triangles;
    /// Vertices given to GLU, and the ones it creates at intersections
    std::deque<Vector3Dd> vertices;
};

void GLAPIENTRY tessVertex(void* vertex, void* data)
{
    ((SolidTessellation*)data)->triangles.push_back(*(const Vector3Dd*)vertex);
}

// With an edge flag callback, GLU gives only separate triangles
void GLAPIENTRY tessEdgeFlag(GLboolean, void*)
{
}

void GLAPIENTRY tessCombine(GLdouble coords[3], void*[4], GLfloat[4],
                            void** out, void* data)
{
    SolidTessellation* t = (SolidTessellation*)data;
    t->vertices.push_back(Vector3Dd(coords[0], coords[1], coords[2]));
    *out = &t->vertices.back();
}

/**
@return the key of a position, quantized so vertices shared by faces match
*/
std::string vertexKey(const Vector3Dd& p)
{
    const double q = 1.0e6;
    return format("%lld/%lld/%lld", (long long)std::llround(p.x() * q),
                  (long long)std::llround(p.y() * q), (long long)std::llround(p.z() * q));
}

/**
Normal of a vertex of a face: the normal of the face, or (smooth shading)
the average of the normals of the faces sharing the vertex that are within
the smoothing threshold of it.
*/
Vector3Dd resolveVertexNormal(
    const std::map<std::string, std::vector<Vector3Dd> >* incident,
    const Vector3Dd& position, const Vector3Dd& faceNormal,
    double thresholdDegrees)
{
    if ( incident == nullptr ) {
        return faceNormal;
    }
    std::map<std::string, std::vector<Vector3Dd> >::const_iterator normals =
        incident->find(vertexKey(position));
    if ( normals == incident->end() ) {
        return faceNormal;
    }
    double minimumCosine = std::cos(thresholdDegrees * M_PI / 180.0);
    Vector3Dd sum;
    for ( size_t i = 0; i < normals->second.size(); i++ ) {
        if ( normals->second[i].dotProduct(faceNormal) >= minimumCosine - 1e-9 ) {
            sum = sum.add(normals->second[i]);
        }
    }
    return sum.length() > 1e-12 ? sum.normalized() : faceNormal;
}

OpenGL4MeshRenderer::Mesh* buildPolyhedralBoundedSolid(
    PolyhedralBoundedSolid& solid, const RendererConfiguration* quality,
    bool doubleSided)
{
    java::ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid.getPolygonsList();
    bool smoothNormals =
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_FLAT &&
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_NOLIGHT;

    //- Normals of the faces incident to each vertex ----------------------
    std::map<std::string, std::vector<Vector3Dd> > incident;
    std::vector<Vector3Dd> faceNormals((size_t)faces.size());
    for ( long f = 0; f < faces.size(); f++ ) {
        _PolyhedralBoundedSolidFace* face = faces[f];
        InfinitePlane* plane = face != nullptr ? face->getContainingPlane() : nullptr;
        if ( plane == nullptr || !(plane->getNormal().length() > 1e-12) ) {
            continue;
        }
        faceNormals[(size_t)f] = plane->getNormal().normalized();
        if ( !smoothNormals ) {
            continue;
        }
        for ( long l = 0; l < face->boundariesList.size(); l++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList[l];
            if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                if ( he->startingVertex != nullptr ) {
                    incident[vertexKey(he->startingVertex->position)]
                        .push_back(faceNormals[(size_t)f]);
                }
                he = he->next();
            } while ( he != nullptr && he != start );
        }
    }

    //- Tessellation of each face -----------------------------------------
    GLUtesselator* tess = gluNewTess();
    if ( tess == nullptr ) {
        return nullptr;
    }
    gluTessCallback(tess, GLU_TESS_VERTEX_DATA, (_GLUfuncptr)tessVertex);
    gluTessCallback(tess, GLU_TESS_EDGE_FLAG_DATA, (_GLUfuncptr)tessEdgeFlag);
    gluTessCallback(tess, GLU_TESS_COMBINE_DATA, (_GLUfuncptr)tessCombine);

    OpenGL4MeshBuilder builder(maxAbsExtent(solid.getMinMax()));
    builder.setDoubleSided(doubleSided);
    for ( long f = 0; f < faces.size(); f++ ) {
        _PolyhedralBoundedSolidFace* face = faces[f];
        const Vector3Dd& normal = faceNormals[(size_t)f];
        if ( face == nullptr || !(normal.length() > 1e-12) ) {
            continue;
        }
        SolidTessellation t;
        // Triangles counterclockwise seen from the outside of the face
        gluTessNormal(tess, normal.x(), normal.y(), normal.z());
        gluTessBeginPolygon(tess, &t);
        for ( long l = 0; l < face->boundariesList.size(); l++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList[l];
            if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
                continue;
            }
            gluTessBeginContour(tess);
            _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                he = he->next();
                if ( he == nullptr || he->startingVertex == nullptr ) {
                    break;
                }
                t.vertices.push_back(he->startingVertex->position);
                GLdouble c[3] = { t.vertices.back().x(), t.vertices.back().y(),
                                  t.vertices.back().z() };
                gluTessVertex(tess, c, &t.vertices.back());
            } while ( he != start );
            gluTessEndContour(tess);
        }
        gluTessEndPolygon(tess);

        for ( size_t i = 0; i + 2 < t.triangles.size(); i += 3 ) {
            Vector3Dd n[3];
            for ( int k = 0; k < 3; k++ ) {
                n[k] = resolveVertexNormal(smoothNormals ? &incident : nullptr,
                    t.triangles[i + k], normal,
                    quality->getVertexNormalSmoothingThresholdDegrees());
            }
            builder.addTriangle(t.triangles[i], n[0], 0, 0,
                                t.triangles[i + 1], n[1], 0, 0,
                                t.triangles[i + 2], n[2], 0, 0);
        }
    }
    gluDeleteTess(tess);
    return builder.build();
}

//= Cache =================================================================

/**
@return a string that identifies the mesh of the geometry, or an empty one
if the geometry is not supported. Parametric geometries are identified by
their parameters; triangle meshes by identity, as they are not edited while
shown.
*/
std::string keyOf(Geometry* geometry)
{
    if ( Arrow* a = dynamic_cast<Arrow*>(geometry) ) {
        return format("arrow/%.17g/%.17g/%.17g/%.17g", a->getBaseLength(),
            a->getHeadLength(), a->getBaseRadius(), a->getHeadRadius());
    }
    if ( Cone* c = dynamic_cast<Cone*>(geometry) ) {
        return format("cone/%.17g/%.17g/%.17g", c->getBottomRadius(),
            c->getTopRadius(), c->getHeight());
    }
    if ( Box* b = dynamic_cast<Box*>(geometry) ) {
        Vector3Dd s = b->getSize();
        return format("box/%.17g/%.17g/%.17g", s.x(), s.y(), s.z());
    }
    if ( Torus* t = dynamic_cast<Torus*>(geometry) ) {
        return format("torus/%.17g/%.17g", t->getMajorRadius(), t->getMinorRadius());
    }
    if ( TriangleMesh* m = dynamic_cast<TriangleMesh*>(geometry) ) {
        return format("trianglemesh/%p/%d/%d", (void*)m, m->getNumVertices(),
            m->getNumTriangles());
    }
    if ( FunctionalExplicitSurface* f = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        // Keyed by content, not by address: editors replace the surface of
        // a body with a new one, which the allocator may place at the
        // address of the deleted one (and its mesh at the old mesh one)
        return format("functionalexplicitsurface/%.17g/%.17g/%.17g/%.17g/"
            "%.17g/%.17g/%d/%d/", f->getMinXBound(), f->getMinYBound(),
            f->getMinZBound(), f->getMaxXBound(), f->getMaxYBound(),
            f->getMaxZBound(), f->getTesselationHintX(),
            f->getTesselationHintY()) + f->getFunctionExpression().c_str();
    }
    if ( ParametricBiCubicPatch* p = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        double hash = 0;
        for ( int i = 0; i < 4; i++ ) {
            for ( int j = 0; j < 4; j++ ) {
                hash += (i * 4 + j + 1) * (p->geometryMatrixX.get(i, j) +
                    3 * p->geometryMatrixY.get(i, j) + 7 * p->geometryMatrixZ.get(i, j));
            }
        }
        return format("parametricbicubicpatch/%p/%d/%d/%.17g", (void*)p,
            p->getType(), p->getApproximationSteps(), hash);
    }
    if ( InfinitePlane* p = dynamic_cast<InfinitePlane*>(geometry) ) {
        return format("infiniteplane/%.17g/%.17g/%.17g/%.17g", p->getA(),
            p->getB(), p->getC(), p->getD());
    }
    if ( TriangleMeshGroup* g = dynamic_cast<TriangleMeshGroup*>(geometry) ) {
        return format("trianglemeshgroup/%p/%ld", (void*)g,
            (long)g->getMeshes().size());
    }
    if ( QuadMesh* q = dynamic_cast<QuadMesh*>(geometry) ) {
        return format("quadmesh/%p", (void*)q);
    }
    return std::string();
}

OpenGL4MeshRenderer::Mesh* buildMesh(Geometry* geometry)
{
    if ( Arrow* a = dynamic_cast<Arrow*>(geometry) ) return buildArrow(*a);
    if ( Cone* c = dynamic_cast<Cone*>(geometry) ) return buildCone(*c);
    if ( Box* b = dynamic_cast<Box*>(geometry) ) return buildBox(*b);
    if ( Torus* t = dynamic_cast<Torus*>(geometry) ) return buildTorus(*t);
    if ( TriangleMesh* m = dynamic_cast<TriangleMesh*>(geometry) ) {
        return buildTriangleMesh(*m);
    }
    if ( FunctionalExplicitSurface* f = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        return buildFunctionalExplicitSurface(*f);
    }
    if ( ParametricBiCubicPatch* p = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        return buildBiCubicPatch(*p);
    }
    if ( InfinitePlane* p = dynamic_cast<InfinitePlane*>(geometry) ) {
        return buildInfinitePlane(*p);
    }
    return buildTriangulatedGeometry(geometry);
}

OpenGL4MeshRenderer::Mesh* obtainMesh(Geometry* geometry)
{
    std::string key = keyOf(geometry);
    if ( key.empty() ) {
        return nullptr;
    }
    std::map<std::string, OpenGL4MeshRenderer::Mesh*>::iterator known =
        meshes.find(key);
    if ( known != meshes.end() ) {
        return known->second;
    }
    OpenGL4MeshRenderer::Mesh* mesh = buildMesh(geometry);
    if ( mesh == nullptr ) {
        return nullptr;
    }
    if ( meshes.size() >= MAX_CACHED_MESHES ) {
        // Stale meshes (i.e. from geometries edited many times) are
        // recreated on demand
        releaseMeshes();
    }
    meshes[key] = mesh;
    return mesh;
}

void reportUnsupported(Geometry* geometry)
{
    const char* name = typeid(*geometry).name();
    if ( reportedUnsupported.insert(name).second ) {
        fprintf(stderr, "OpenGL4GeometryRenderer: geometry not supported by "
                "the GL4 pipeline yet: %s\n", name);
    }
}

}

void OpenGL4GeometryRenderer::draw(Geometry* geometry, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap,
    const Matrix4x4d& local)
{
    if(!geometry||!camera||!quality)return;
    ParametricCurve* curve = dynamic_cast<ParametricCurve*>(geometry);
    if ( curve != 0 ) {
        java::ArrayList<float> positions, colors;
        ColorRgb wire = quality->getWireColor();
        for ( int segment = 1; segment < curve->getPointSize(); ++segment ) {
            if ( curve->getPointType(segment) == ParametricCurve::BREAK ) {
                ++segment; continue;
            }
            java::ArrayList<Vector3Dd> points = curve->calculatePoints(segment, false);
            for ( long i = 0; i + 1 < points.size(); ++i ) {
                const Vector3Dd ends[2] = {points[i], points[i + 1]};
                for ( int j = 0; j < 2; ++j ) {
                    positions.add((float)ends[j].x()); positions.add((float)ends[j].y());
                    positions.add((float)ends[j].z()); colors.add((float)wire.r());
                    colors.add((float)wire.g()); colors.add((float)wire.b());
                }
            }
        }
        OpenGL4LineRenderer::drawLines(camera->calculateProjectionMatrix().multiply(local),
            positions, colors, 1.0f);
        if(quality->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(geometry,camera,local);
        if(quality->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(geometry,camera,local);
        return;
    }
    Sphere* sphere = dynamic_cast<Sphere*>(geometry);
    if ( sphere != 0 ) {
        // As in Java, without lights the scene is lit from the camera
        PointLight cameraLight(camera->getPosition(), ColorRgb(1, 1, 1));
        const Light* light = lights && lights->size() ? (*lights)[0] : &cameraLight;
        const SimpleMaterial defaultMaterial;
        OpenGL4SphereRenderer::draw(sphere, camera, light,
            material ? material : &defaultMaterial, quality,
            textureMap, normalMap, local, 32, 16);
        if(quality->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(geometry,camera,local);
        if(quality->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(geometry,camera,local);
        return;
    }

    // Boundary representations are tessellated at each frame, as in Java
    PolyhedralBoundedSolid* solid = dynamic_cast<PolyhedralBoundedSolid*>(geometry);
    if ( solid != 0 ) {
        OpenGL4MeshRenderer::Mesh* mesh = buildPolyhedralBoundedSolid(*solid,
            quality, material != 0 && material->isDoubleSided());
        if ( mesh != 0 ) {
            OpenGL4MeshRenderer::draw(mesh, geometry, camera, lights, material,
                quality, textureMap, normalMap, local);
            OpenGL4MeshRenderer::release(mesh);
            delete mesh;
        }
        return;
    }

    OpenGL4MeshRenderer::Mesh* mesh = obtainMesh(geometry);
    if ( mesh == 0 ) {
        reportUnsupported(geometry);
        return;
    }
    InfinitePlane* plane = dynamic_cast<InfinitePlane*>(geometry);
    if ( plane == 0 ) {
        OpenGL4MeshRenderer::draw(mesh, geometry, camera, lights, material,
            quality, textureMap, normalMap, local);
        return;
    }

    // The bounding volume of an infinite plane is infinite: the bounding
    // volume and the selection corners are drawn around the square shown
    RendererConfiguration meshQuality = quality->clone();
    meshQuality.setBoundingVolume(false);
    meshQuality.setSelectionCorners(false);
    OpenGL4MeshRenderer::draw(mesh, geometry, camera, lights, material,
        &meshQuality, textureMap, normalMap, local);
    double shown[6];
    calculateInfinitePlaneShownMinMax(*plane, shown);
    if(quality->isBoundingVolumeSet())OpenGL4MinMaxRenderer::draw(shown,camera,local);
    if(quality->isSelectionCornersSet())OpenGL4SelectionCornersRenderer::draw(shown,camera,local);
}

void OpenGL4GeometryRenderer::dispose()
{
    releaseMeshes();
    OpenGL4MeshRenderer::dispose();
    OpenGL4ColoredPrimitiveRenderer::release();
    OpenGL4LineRenderer::release();
}
