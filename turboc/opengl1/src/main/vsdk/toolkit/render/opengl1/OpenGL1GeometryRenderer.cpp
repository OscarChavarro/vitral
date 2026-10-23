#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include <math.h>
#include <stdarg.h>
#include <stdio.h>
#include "java/util/ArrayDeque.h"
#include "java/util/TreeMap.h"
#include "java/util/TreeSet.h"
#include "java/lang/String.h"
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
#include "vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MinMaxRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SelectionCornersRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.h"
#include "vsdk/toolkit/common/Cpp98Support.h"
#include "java/util/Collections.h"

/*
Geometries other than spheres and curves are presented as triangle meshes
drawn by `OpenGL1MeshRenderer` with the fixed function state of the shading
type, as the OpenGL 4 port does (see `OpenGL4GeometryRenderer`). Meshes are
cached by a key made of the parameters of the geometry (or its identity, for
the ones that are not edited while shown), and built again when it changes.
*/

static const int SLICES = 32;
static const int TORUS_MAJOR_SEGMENTS = 48;
static const int TORUS_MINOR_SEGMENTS = 24;
static const long MAX_CACHED_MESHES = 256;
/// Side of the square shown for an infinite plane
static const double INFINITE_PLANE_SIZE = 20.0;
/// Cells along each side of the square shown for an infinite plane
static const int INFINITE_PLANE_TILES = 10;

static TreeMap<String, OpenGL1MeshRenderer::Mesh*> meshes;
static TreeSet<String> reportedUnsupported;

static void releaseMeshes()
{
    for ( long i = 0; i < meshes.size(); i++ ) {
        OpenGL1MeshRenderer::release(meshes.getValue(i));
        delete meshes.getValue(i);
    }
    meshes.clear();
}

static String format(const char* pattern, ...)
    __attribute__((format(printf, 1, 2)));

static String format(const char* pattern, ...)
{
    char buffer[512];
    va_list args;
    va_start(args, pattern);
    vsnprintf(buffer, sizeof(buffer), pattern, args);
    va_end(args);
    return buffer;
}

static double maxAbsExtent(double* minmax)
{
    double size = 1.0;
    if ( minmax != NULL ) {
        size = fabs(minmax[3] - minmax[0]);
        if ( fabs(minmax[4] - minmax[1]) > size ) size = fabs(minmax[4] - minmax[1]);
        if ( fabs(minmax[5] - minmax[2]) > size ) size = fabs(minmax[5] - minmax[2]);
        delete[] minmax;
    }
    return size;
}

//= Primitives ============================================================

static OpenGL1MeshRenderer::Mesh* buildCone(const Cone& cone)
{
    double r1 = cone.getBottomRadius();
    double r2 = cone.getTopRadius();
    double h = cone.getHeight();
    double size = r1 > r2 ? r1 : r2;
    OpenGL1MeshBuilder builder(size > h ? size : h);

    builder.addFrustum(0, r1, h, r2, SLICES);
    builder.addDisk(0, 0, r1, FALSE, SLICES);
    if ( r2 > 0.0 ) {
        builder.addDisk(h, 0, r2, TRUE, SLICES);
    }
    return builder.build();
}

static OpenGL1MeshRenderer::Mesh* buildArrow(const Arrow& arrow)
{
    double h1 = arrow.getBaseLength();
    double h2 = arrow.getHeadLength();
    double r1 = arrow.getBaseRadius();
    double r2 = arrow.getHeadRadius();
    OpenGL1MeshBuilder builder(h1 + h2);

    builder.addFrustum(0, r1, h1, r1, SLICES);
    builder.addDisk(0, 0, r1, FALSE, SLICES);
    builder.addFrustum(h1, r2, h1 + h2, 0, SLICES);
    builder.addDisk(h1, r1, r2, FALSE, SLICES);
    return builder.build();
}

/**
Adds a face whose vertices are given in the order upper left, lower left,
lower right, upper right as seen from outside, wound so its front side looks
outside.
*/
static void addBoxFace(OpenGL1MeshBuilder& builder, const Vector3Dd& n,
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

static OpenGL1MeshRenderer::Mesh* buildBox(const Box& box)
{
    Vector3Dd size = box.getSize();
    double hx = size.x() / 2, hy = size.y() / 2, hz = size.z() / 2;
    double half = hx > hy ? hx : hy;
    OpenGL1MeshBuilder builder((half > hz ? half : hz) * 2);

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

static Vector3Dd torusPoint(double bigR, double smallR, double w, double v)
{
    return Vector3Dd((bigR + smallR * cos(v)) * cos(w),
                     (bigR + smallR * cos(v)) * sin(w),
                     smallR * sin(v));
}

static Vector3Dd torusNormal(double w, double v)
{
    return Vector3Dd(cos(v) * cos(w), cos(v) * sin(w),
                     sin(v));
}

static OpenGL1MeshRenderer::Mesh* buildTorus(const Torus& torus)
{
    double bigR = torus.getMajorRadius();
    double smallR = torus.getMinorRadius();
    OpenGL1MeshBuilder builder(bigR + smallR);
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

static void addTriangleMesh(OpenGL1MeshBuilder& builder, TriangleMesh& mesh)
{
    ArrayList<double>& p = mesh.getVertexPositions();
    ArrayList<double>& n = mesh.getVertexNormals();
    ArrayList<double>& uv = mesh.getVertexUvs();
    ArrayList<int>& idx = mesh.getTriangleIndexes();

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

static OpenGL1MeshRenderer::Mesh* buildTriangleMesh(TriangleMesh& mesh)
{
    if ( mesh.getVertexPositions().size() == 0 ||
         mesh.getTriangleIndexes().size() < 3 ) {
        return NULL;
    }
    OpenGL1MeshBuilder builder(maxAbsExtent(mesh.getMinMax()));
    addTriangleMesh(builder, mesh);
    return builder.build();
}

/**
Groups of meshes and quad meshes (i.e. imported objects), through the
triangle meshes `GeometryTriangulator` exports for them.
*/
static OpenGL1MeshRenderer::Mesh* buildTriangulatedGeometry(Geometry* geometry)
{
    TriangleMeshGroup group;
    if ( !GeometryTriangulator::exportToTriangleMeshGroup(geometry, group) ||
         group.getMeshes().size() == 0 ) {
        return NULL;
    }
    OpenGL1MeshBuilder builder(maxAbsExtent(geometry->getMinMax()));
    for ( long i = 0; i < group.getMeshes().size(); i++ ) {
        addTriangleMesh(builder, group.getMeshes()[i]);
    }
    return builder.build();
}

/**
The height field is drawn from its internal triangle mesh, visible from both
sides, with texture coordinates spanning its (x, y) bounds.
*/
static OpenGL1MeshRenderer::Mesh* buildFunctionalExplicitSurface(
    FunctionalExplicitSurface& surface)
{
    TriangleMesh* mesh = surface.getInternalTriangleMesh();
    if ( mesh == NULL ) {
        return NULL;
    }
    ArrayList<double>& p = mesh->getVertexPositions();
    ArrayList<double>& n = mesh->getVertexNormals();
    ArrayList<int>& idx = mesh->getTriangleIndexes();
    if ( p.size() == 0 || idx.size() < 3 ) {
        return NULL;
    }

    double sizeX = surface.getMaxXBound() - surface.getMinXBound();
    double sizeY = surface.getMaxYBound() - surface.getMinYBound();
    double sizeZ = surface.getMaxZBound() - surface.getMinZBound();
    double size = fabs(sizeX);
    if ( fabs(sizeY) > size ) size = fabs(sizeY);
    if ( fabs(sizeZ) > size ) size = fabs(sizeZ);
    OpenGL1MeshBuilder builder(size);
    builder.setDoubleSided(TRUE);

    for ( long t = 0; t + 2 < idx.size(); t += 3 ) {
        Vector3Dd pos[3];
        Vector3Dd nor[3];
        BOOLEAN hasNormal[3];
        double u[3];
        double v[3];
        int k;
        for ( k = 0; k < 3; k++ ) {
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
        for ( k = 0; k < 3; k++ ) {
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

static Vector3Dd validNormal(const Vector3Dd& normal, const Vector3Dd& fallback)
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
static OpenGL1MeshRenderer::Mesh* buildBiCubicPatch(ParametricBiCubicPatch& patch)
{
    int steps = patch.getApproximationSteps() > 1 ? patch.getApproximationSteps() : 1;
    int n = steps + 1;
    ArrayList<Vector3Dd> points(Collections<Vector3Dd>::nCopies(n * n, Vector3Dd()));
    ArrayList<Vector3Dd> normals(Collections<Vector3Dd>::nCopies(n * n, Vector3Dd()));

    int i;
    for ( i = 0; i < n; i++ ) {
        double s = (double)i / steps;
        for ( int j = 0; j < n; j++ ) {
            double t = (double)j / steps;
            patch.evaluate(points[(long)i * n + j], s, t);
            normals[(long)i * n + j] = patch.evaluateNormal(s, t);
        }
    }

    OpenGL1MeshBuilder builder(maxAbsExtent(patch.getMinMax()));
    builder.setDoubleSided(TRUE);
    for ( i = 0; i < steps; i++ ) {
        for ( int j = 0; j < steps; j++ ) {
            const Vector3Dd& p00 = points[(long)i * n + j];
            const Vector3Dd& p10 = points[(long)(i + 1) * n + j];
            const Vector3Dd& p11 = points[(long)(i + 1) * n + j + 1];
            const Vector3Dd& p01 = points[(long)i * n + j + 1];
            Vector3Dd face = p11.subtract(p00).crossProduct(p01.subtract(p10));
            if ( !(face.length() > 1e-15) ) {
                continue;
            }
            Vector3Dd n00 = validNormal(normals[(long)i * n + j], face);
            Vector3Dd n10 = validNormal(normals[(long)(i + 1) * n + j], face);
            Vector3Dd n11 = validNormal(normals[(long)(i + 1) * n + j + 1], face);
            Vector3Dd n01 = validNormal(normals[(long)i * n + j + 1], face);
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
static void calculatePlaneFrame(const InfinitePlane& plane, Vector3Dd outFrame[4])
{
    Vector3Dd n = plane.getNormal();
    double length = sqrt(plane.getA() * plane.getA() +
        plane.getB() * plane.getB() + plane.getC() * plane.getC());
    // The plane is a x + b y + c z + d = 0; (a, b, c) may not be unitary
    Vector3Dd center = n.multiply(length > 0 ? -plane.getD() / length : 0);
    Vector3Dd reference = fabs(n.z()) < 0.9 ?
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
static OpenGL1MeshRenderer::Mesh* buildInfinitePlane(const InfinitePlane& plane)
{
    Vector3Dd frame[4];
    calculatePlaneFrame(plane, frame);
    double h = INFINITE_PLANE_SIZE / 2;
    double d = INFINITE_PLANE_SIZE / INFINITE_PLANE_TILES;
    OpenGL1MeshBuilder builder(INFINITE_PLANE_SIZE);
    builder.setDoubleSided(TRUE);

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
static void calculateInfinitePlaneShownMinMax(const InfinitePlane& plane,
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
representation, as `Jogl2PolyhedralBoundedSolidRenderer` does in Java: its
faces are planar polygons, maybe with holes (several loops).
*/
struct SolidTessellation {
    ArrayList<Vector3Dd> triangles;
    /// Vertices given to GLU, and the ones it creates at intersections.
    /// GLU keeps pointers to them, so each one is allocated on its own (a
    ArrayList<Vector3Dd*> vertices;

    ~SolidTessellation()
    {
        for ( long i = 0; i < vertices.size(); i++ ) {
            delete vertices[i];
        }
    }

    Vector3Dd* addVertex(const Vector3Dd& position)
    {
        Vector3Dd* vertex = new Vector3Dd(position);
        vertices.add(vertex);
        return vertex;
    }
};

static void GLAPIENTRY tessVertex(void* vertex, void* data)
{
    ((SolidTessellation*)data)->triangles.add(*(const Vector3Dd*)vertex);
}

// With an edge flag callback, GLU gives only separate triangles
static void GLAPIENTRY tessEdgeFlag(GLboolean, void*)
{
}

static void GLAPIENTRY tessCombine(GLdouble coords[3], void*[4], GLfloat[4],
                            void** out, void* data)
{
    SolidTessellation* t = (SolidTessellation*)data;
    *out = t->addVertex(Vector3Dd(coords[0], coords[1], coords[2]));
}

/**
@return the key of a position, quantized so vertices shared by faces match
*/
static String vertexKey(const Vector3Dd& p)
{
    const double q = 1.0e6;
    return format("%lld/%lld/%lld", (long long)Cpp98Support::llround(p.x() * q),
                  (long long)Cpp98Support::llround(p.y() * q), (long long)Cpp98Support::llround(p.z() * q));
}

/**
Normal of a vertex of a face: the normal of the face, or (smooth shading)
the average of the normals of the faces sharing the vertex that are within
the smoothing threshold of it.
*/
static Vector3Dd resolveVertexNormal(
    const TreeMap<String, ArrayList<Vector3Dd> >* incident,
    const Vector3Dd& position, const Vector3Dd& faceNormal,
    double thresholdDegrees)
{
    if ( incident == NULL ) {
        return faceNormal;
    }
    const ArrayList<Vector3Dd>* normals = incident->get(vertexKey(position));
    if ( normals == NULL ) {
        return faceNormal;
    }
    double minimumCosine = cos(thresholdDegrees * M_PI / 180.0);
    Vector3Dd sum;
    for ( long i = 0; i < normals->size(); i++ ) {
        if ( (*normals)[i].dotProduct(faceNormal) >= minimumCosine - 1e-9 ) {
            sum = sum.add((*normals)[i]);
        }
    }
    return sum.length() > 1e-12 ? sum.normalized() : faceNormal;
}

static OpenGL1MeshRenderer::Mesh* buildPolyhedralBoundedSolid(
    PolyhedralBoundedSolid& solid, const RendererConfiguration* quality,
    BOOLEAN doubleSided)
{
    ArrayList<_PolyhedralBoundedSolidFace*>& faces = solid.getPolygonsList();
    BOOLEAN smoothNormals =
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_FLAT &&
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_NOLIGHT;

    //- Normals of the faces incident to each vertex ----------------------
    TreeMap<String, ArrayList<Vector3Dd> > incident;
    ArrayList<Vector3Dd> faceNormals(Collections<Vector3Dd>::nCopies(faces.size(), Vector3Dd()));
    long f;
    for ( f = 0; f < faces.size(); f++ ) {
        _PolyhedralBoundedSolidFace* face = faces[f];
        InfinitePlane* plane = face != NULL ? face->getContainingPlane() : NULL;
        if ( plane == NULL || !(plane->getNormal().length() > 1e-12) ) {
            continue;
        }
        faceNormals[(long)f] = plane->getNormal().normalized();
        if ( !smoothNormals ) {
            continue;
        }
        for ( long l = 0; l < face->boundariesList.size(); l++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList[l];
            if ( loop == NULL || loop->boundaryStartHalfEdge == NULL ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                if ( he->startingVertex != NULL ) {
                    incident.putIfAbsent(
                        vertexKey(he->startingVertex->position),
                        ArrayList<Vector3Dd>()).add(faceNormals[f]);
                }
                he = he->next();
            } while ( he != NULL && he != start );
        }
    }

    //- Tessellation of each face -----------------------------------------
    GLUtesselator* tess = gluNewTess();
    if ( tess == NULL ) {
        return NULL;
    }
    gluTessCallback(tess, GLU_TESS_VERTEX_DATA, (OpenGL1GluCallback)tessVertex);
    gluTessCallback(tess, GLU_TESS_EDGE_FLAG_DATA, (OpenGL1GluCallback)tessEdgeFlag);
    gluTessCallback(tess, GLU_TESS_COMBINE_DATA, (OpenGL1GluCallback)tessCombine);

    OpenGL1MeshBuilder builder(maxAbsExtent(solid.getMinMax()));
    builder.setDoubleSided(doubleSided);
    for ( f = 0; f < faces.size(); f++ ) {
        _PolyhedralBoundedSolidFace* face = faces[f];
        const Vector3Dd& normal = faceNormals[f];
        if ( face == NULL || !(normal.length() > 1e-12) ) {
            continue;
        }
        SolidTessellation t;
        // Triangles counterclockwise seen from the outside of the face
        gluTessNormal(tess, normal.x(), normal.y(), normal.z());
        gluTessBeginPolygon(tess, &t);
        for ( long l = 0; l < face->boundariesList.size(); l++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList[l];
            if ( loop == NULL || loop->boundaryStartHalfEdge == NULL ) {
                continue;
            }
            gluTessBeginContour(tess);
            _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                he = he->next();
                if ( he == NULL || he->startingVertex == NULL ) {
                    break;
                }
                Vector3Dd* vertex = t.addVertex(he->startingVertex->position);
                GLdouble c[3] = { vertex->x(), vertex->y(), vertex->z() };
                gluTessVertex(tess, c, vertex);
            } while ( he != start );
            gluTessEndContour(tess);
        }
        gluTessEndPolygon(tess);

        for ( long i = 0; i + 2 < t.triangles.size(); i += 3 ) {
            Vector3Dd n[3];
            for ( int k = 0; k < 3; k++ ) {
                n[k] = resolveVertexNormal(smoothNormals ? &incident : NULL,
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
static String keyOf(Geometry* geometry)
{
    if ( geometry != NULL && geometry->instanceOf(Arrow::CLASS) ) {
        Arrow* a = (Arrow*)geometry;
        return format("arrow/%.17g/%.17g/%.17g/%.17g", a->getBaseLength(),
            a->getHeadLength(), a->getBaseRadius(), a->getHeadRadius());
    }
    if ( geometry != NULL && geometry->instanceOf(Cone::CLASS) ) {
        Cone* c = (Cone*)geometry;
        return format("cone/%.17g/%.17g/%.17g", c->getBottomRadius(),
            c->getTopRadius(), c->getHeight());
    }
    if ( geometry != NULL && geometry->instanceOf(Box::CLASS) ) {
        Box* b = (Box*)geometry;
        Vector3Dd s = b->getSize();
        return format("box/%.17g/%.17g/%.17g", s.x(), s.y(), s.z());
    }
    if ( geometry != NULL && geometry->instanceOf(Torus::CLASS) ) {
        Torus* t = (Torus*)geometry;
        return format("torus/%.17g/%.17g", t->getMajorRadius(), t->getMinorRadius());
    }
    if ( geometry != NULL && geometry->instanceOf(TriangleMesh::CLASS) ) {
        TriangleMesh* m = (TriangleMesh*)geometry;
        return format("trianglemesh/%p/%d/%d", (void*)m, m->getNumVertices(),
            m->getNumTriangles());
    }
    if ( geometry != NULL && geometry->instanceOf(FunctionalExplicitSurface::CLASS) ) {
        FunctionalExplicitSurface* f = (FunctionalExplicitSurface*)geometry;
        // Keyed by content, not by address: editors replace the surface of
        // a body with a new one, which the allocator may place at the
        // address of the deleted one (and its mesh at the old mesh one)
        return format("functionalexplicitsurface/%.17g/%.17g/%.17g/%.17g/"
            "%.17g/%.17g/%d/%d/", f->getMinXBound(), f->getMinYBound(),
            f->getMinZBound(), f->getMaxXBound(), f->getMaxYBound(),
            f->getMaxZBound(), f->getTesselationHintX(),
            f->getTesselationHintY()) + f->getFunctionExpression().c_str();
    }
    if ( geometry != NULL && geometry->instanceOf(ParametricBiCubicPatch::CLASS) ) {
        ParametricBiCubicPatch* p = (ParametricBiCubicPatch*)geometry;
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
    if ( geometry != NULL && geometry->instanceOf(InfinitePlane::CLASS) ) {
        InfinitePlane* p = (InfinitePlane*)geometry;
        return format("infiniteplane/%.17g/%.17g/%.17g/%.17g", p->getA(),
            p->getB(), p->getC(), p->getD());
    }
    if ( geometry != NULL && geometry->instanceOf(TriangleMeshGroup::CLASS) ) {
        TriangleMeshGroup* g = (TriangleMeshGroup*)geometry;
        return format("trianglemeshgroup/%p/%ld", (void*)g,
            (long)g->getMeshes().size());
    }
    if ( geometry != NULL && geometry->instanceOf(QuadMesh::CLASS) ) {
        QuadMesh* q = (QuadMesh*)geometry;
        return format("quadmesh/%p", (void*)q);
    }
    return String();
}

static OpenGL1MeshRenderer::Mesh* buildMesh(Geometry* geometry)
{
    if ( geometry != NULL && geometry->instanceOf(Arrow::CLASS) ) return buildArrow(*(Arrow*)geometry);
    if ( geometry != NULL && geometry->instanceOf(Cone::CLASS) ) return buildCone(*(Cone*)geometry);
    if ( geometry != NULL && geometry->instanceOf(Box::CLASS) ) return buildBox(*(Box*)geometry);
    if ( geometry != NULL && geometry->instanceOf(Torus::CLASS) ) return buildTorus(*(Torus*)geometry);
    if ( geometry != NULL && geometry->instanceOf(TriangleMesh::CLASS) ) {
        TriangleMesh* m = (TriangleMesh*)geometry;
        return buildTriangleMesh(*m);
    }
    if ( geometry != NULL && geometry->instanceOf(FunctionalExplicitSurface::CLASS) ) {
        FunctionalExplicitSurface* f = (FunctionalExplicitSurface*)geometry;
        return buildFunctionalExplicitSurface(*f);
    }
    if ( geometry != NULL && geometry->instanceOf(ParametricBiCubicPatch::CLASS) ) {
        ParametricBiCubicPatch* p = (ParametricBiCubicPatch*)geometry;
        return buildBiCubicPatch(*p);
    }
    if ( geometry != NULL && geometry->instanceOf(InfinitePlane::CLASS) ) {
        InfinitePlane* p = (InfinitePlane*)geometry;
        return buildInfinitePlane(*p);
    }
    return buildTriangulatedGeometry(geometry);
}

static OpenGL1MeshRenderer::Mesh* obtainMesh(Geometry* geometry)
{
    String key = keyOf(geometry);
    if ( key.isEmpty() ) {
        return NULL;
    }
    OpenGL1MeshRenderer::Mesh** known = meshes.get(key);
    if ( known != NULL ) {
        return (*known);
    }
    OpenGL1MeshRenderer::Mesh* mesh = buildMesh(geometry);
    if ( mesh == NULL ) {
        return NULL;
    }
    if ( meshes.size() >= MAX_CACHED_MESHES ) {
        // Stale meshes (i.e. from geometries edited many times) are
        // recreated on demand
        releaseMeshes();
    }
    meshes.put(key, mesh);
    return mesh;
}

static void reportUnsupported(Geometry* geometry)
{
    String name = geometry->getClassSimpleName();
    if ( reportedUnsupported.add(name) ) {
        fprintf(stderr, "OpenGL1GeometryRenderer: geometry not supported by "
                "the OpenGL 1.2 pipeline yet: %s\n", name.c_str());
    }
}


void OpenGL1GeometryRenderer::draw(Geometry* geometry, Camera* camera,
    const ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
    RGBImageUncompressed* normalMap,
    const Matrix4x4d& local)
{
    if(!geometry||!camera||!quality)return;
    ParametricCurve* curve = (geometry != NULL && geometry->instanceOf(ParametricCurve::CLASS) ? (ParametricCurve*)geometry : NULL);
    if ( curve != 0 ) {
        ArrayList<float> positions, colors;
        ColorRgb wire = quality->getWireColor();
        for ( int segment = 1; segment < curve->getPointSize(); ++segment ) {
            if ( curve->getPointType(segment) == ParametricCurve::BREAK ) {
                ++segment; continue;
            }
            ArrayList<Vector3Dd> points = curve->calculatePoints(segment, FALSE);
            for ( long i = 0; i + 1 < points.size(); ++i ) {
                const Vector3Dd ends[2] = {points[i], points[i + 1]};
                for ( int j = 0; j < 2; ++j ) {
                    positions.add((float)ends[j].x()); positions.add((float)ends[j].y());
                    positions.add((float)ends[j].z()); colors.add((float)wire.r());
                    colors.add((float)wire.g()); colors.add((float)wire.b());
                }
            }
        }
        OpenGL1LineRenderer::drawLines(camera->calculateProjectionMatrix().multiply(local),
            positions, colors, 1.0f);
        if(quality->isBoundingVolumeSet())OpenGL1MinMaxRenderer::draw(geometry,camera,local);
        if(quality->isSelectionCornersSet())OpenGL1SelectionCornersRenderer::draw(geometry,camera,local);
        return;
    }
    Sphere* sphere = (geometry != NULL && geometry->instanceOf(Sphere::CLASS) ? (Sphere*)geometry : NULL);
    if ( sphere != 0 ) {
        // As in Java, without lights the scene is lit from the camera
        PointLight cameraLight(camera->getPosition(), ColorRgb(1, 1, 1));
        const Light* light = lights && lights->size() ? (*lights)[0] : &cameraLight;
        const SimpleMaterial defaultMaterial;
        OpenGL1SphereRenderer::draw(sphere, camera, light,
            material ? material : &defaultMaterial, quality,
            textureMap, normalMap, local, 32, 16);
        if(quality->isBoundingVolumeSet())OpenGL1MinMaxRenderer::draw(geometry,camera,local);
        if(quality->isSelectionCornersSet())OpenGL1SelectionCornersRenderer::draw(geometry,camera,local);
        return;
    }

    // Boundary representations are tessellated at each frame, as in Java
    PolyhedralBoundedSolid* solid = (geometry != NULL && geometry->instanceOf(PolyhedralBoundedSolid::CLASS) ? (PolyhedralBoundedSolid*)geometry : NULL);
    if ( solid != 0 ) {
        OpenGL1MeshRenderer::Mesh* mesh = buildPolyhedralBoundedSolid(*solid,
            quality, material != 0 && material->isDoubleSided());
        if ( mesh != 0 ) {
            OpenGL1MeshRenderer::draw(mesh, geometry, camera, lights, material,
                quality, textureMap, normalMap, local);
            OpenGL1MeshRenderer::release(mesh);
            delete mesh;
        }
        return;
    }

    OpenGL1MeshRenderer::Mesh* mesh = obtainMesh(geometry);
    if ( mesh == 0 ) {
        reportUnsupported(geometry);
        return;
    }
    InfinitePlane* plane = (geometry != NULL && geometry->instanceOf(InfinitePlane::CLASS) ? (InfinitePlane*)geometry : NULL);
    if ( plane == 0 ) {
        OpenGL1MeshRenderer::draw(mesh, geometry, camera, lights, material,
            quality, textureMap, normalMap, local);
        return;
    }

    // The bounding volume of an infinite plane is infinite: the bounding
    // volume and the selection corners are drawn around the square shown
    RendererConfiguration meshQuality = quality->clone();
    meshQuality.setBoundingVolume(FALSE);
    meshQuality.setSelectionCorners(FALSE);
    OpenGL1MeshRenderer::draw(mesh, geometry, camera, lights, material,
        &meshQuality, textureMap, normalMap, local);
    double shown[6];
    calculateInfinitePlaneShownMinMax(*plane, shown);
    if(quality->isBoundingVolumeSet())OpenGL1MinMaxRenderer::draw(shown,camera,local);
    if(quality->isSelectionCornersSet())OpenGL1SelectionCornersRenderer::draw(shown,camera,local);
}

void OpenGL1GeometryRenderer::dispose()
{
    releaseMeshes();
    OpenGL1MeshRenderer::dispose();
    OpenGL1ColoredPrimitiveRenderer::release();
    OpenGL1LineRenderer::release();
}
