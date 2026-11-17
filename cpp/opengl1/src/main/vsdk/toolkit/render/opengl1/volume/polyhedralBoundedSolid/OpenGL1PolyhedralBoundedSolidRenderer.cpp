#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "java/util/Collections.h"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include <cmath>
#include <cstdarg>
#include <cstdio>

#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/polyhedralBoundedSolid/OpenGL1PolyhedralBoundedSolidRenderer.h"

namespace {

java::String format(const char* pattern, ...)
    __attribute__((format(printf, 1, 2)));

java::String format(const char* pattern, ...)
{
    char buffer[512];
    va_list args;
    va_start(args, pattern);
    vsnprintf(buffer, sizeof(buffer), pattern, args);
    va_end(args);
    return buffer;
}

/**
Triangles produced by the GLU tessellator for the faces of a boundary
representation, as `Jogl2PolyhedralBoundedSolidRenderer` does in Java: its
faces are planar polygons, maybe with holes (several loops).
*/
struct SolidTessellation {
    java::ArrayList<Vector3Dd> triangles;
    /// Vertices given to GLU, and the ones it creates at intersections
    /// (owned; GLU keeps their addresses, so they do not move)
    java::ArrayList<Vector3Dd*> vertices;

    ~SolidTessellation()
    {
        for ( long i = 0; i < vertices.size(); i++ ) {
            delete vertices[i];
        }
    }
};

void GLAPIENTRY tessVertex(void* vertex, void* data)
{
    ((SolidTessellation*)data)->triangles.add(*(const Vector3Dd*)vertex);
}

// With an edge flag callback, GLU gives only separate triangles
void GLAPIENTRY tessEdgeFlag(GLboolean, void*)
{
}

void GLAPIENTRY tessCombine(GLdouble coords[3], void*[4], GLfloat[4],
                            void** out, void* data)
{
    SolidTessellation* t = (SolidTessellation*)data;
    t->vertices.add(new Vector3Dd(coords[0], coords[1], coords[2]));
    *out = t->vertices[t->vertices.size() - 1];
}

/**
@return the key of a position, quantized so vertices shared by faces match
*/
java::String vertexKey(const Vector3Dd& p)
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
    const java::TreeMap<java::String, java::ArrayList<Vector3Dd> >* incident,
    const Vector3Dd& position, const Vector3Dd& faceNormal,
    double thresholdDegrees)
{
    if ( incident == nullptr ) {
        return faceNormal;
    }
    java::ArrayList<Vector3Dd> const* normals = incident->get(vertexKey(position));
    if ( normals == nullptr ) {
        return faceNormal;
    }
    double minimumCosine = std::cos(thresholdDegrees * M_PI / 180.0);
    Vector3Dd sum;
    for ( size_t i = 0; i < (*normals).size(); i++ ) {
        if ( (*normals)[i].dotProduct(faceNormal) >= minimumCosine - 1e-9 ) {
            sum = sum.add((*normals)[i]);
        }
    }
    return sum.length() > 1e-12 ? sum.normalized() : faceNormal;
}

OpenGL1MeshRenderer::Mesh* buildPolyhedralBoundedSolid(
    PolyhedralBoundedSolid& solid, const RendererConfiguration* quality,
    bool doubleSided)
{
    CircularArrayList<_PolyhedralBoundedSolidFace*>& faces = solid.getPolygonsList();
    bool smoothNormals =
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_FLAT &&
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_NOLIGHT;

    //- Normals of the faces incident to each vertex ----------------------
    java::TreeMap<java::String, java::ArrayList<Vector3Dd> > incident;
    java::ArrayList<Vector3Dd> faceNormals = java::Collections::nCopies<Vector3Dd>((size_t)faces.size(), Vector3Dd());
    for ( long f = 0; f < faces.size(); f++ ) {
        _PolyhedralBoundedSolidFace* face = faces.get((int)f);
        InfinitePlane* plane = face != nullptr ? face->getContainingPlane() : nullptr;
        if ( plane == nullptr || !(plane->getNormal().length() > 1e-12) ) {
            continue;
        }
        faceNormals[(size_t)f] = plane->getNormal().normalized();
        if ( !smoothNormals ) {
            continue;
        }
        for ( long l = 0; l < face->boundariesList.size(); l++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(l);
            if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                if ( he->startingVertex != nullptr ) {
                    incident.putIfAbsent(vertexKey(he->startingVertex->position), java::ArrayList<Vector3Dd>())
                        .add(faceNormals[(size_t)f]);
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
    gluTessCallback(tess, GLU_TESS_VERTEX_DATA, (OpenGL1GluCallback)tessVertex);
    gluTessCallback(tess, GLU_TESS_EDGE_FLAG_DATA, (OpenGL1GluCallback)tessEdgeFlag);
    gluTessCallback(tess, GLU_TESS_COMBINE_DATA, (OpenGL1GluCallback)tessCombine);

    double* minmax = solid.getMinMax();
    OpenGL1MeshBuilder builder(OpenGL1MeshBuilder::characteristicSize(minmax));
    delete[] minmax;
    builder.setDoubleSided(doubleSided);
    for ( long f = 0; f < faces.size(); f++ ) {
        _PolyhedralBoundedSolidFace* face = faces.get((int)f);
        const Vector3Dd& normal = faceNormals[(size_t)f];
        if ( face == nullptr || !(normal.length() > 1e-12) ) {
            continue;
        }
        SolidTessellation t;
        // Triangles counterclockwise seen from the outside of the face
        gluTessNormal(tess, normal.x(), normal.y(), normal.z());
        gluTessBeginPolygon(tess, &t);
        for ( long l = 0; l < face->boundariesList.size(); l++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(l);
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
                Vector3Dd* vertex = new Vector3Dd(he->startingVertex->position);
                t.vertices.add(vertex);
                GLdouble c[3] = { vertex->x(), vertex->y(), vertex->z() };
                gluTessVertex(tess, c, vertex);
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

}

void OpenGL1PolyhedralBoundedSolidRenderer::draw(
    PolyhedralBoundedSolid* solid, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, const Matrix4x4d& modelMatrix)
{
    if ( solid == nullptr || quality == nullptr ) {
        return;
    }
    // Boundary representations are tessellated at each frame, as in Java
    OpenGL1MeshRenderer::Mesh* mesh = buildPolyhedralBoundedSolid(*solid,
        quality, material != nullptr && material->isDoubleSided());
    if ( mesh != nullptr ) {
        OpenGL1MeshRenderer::draw(mesh, solid, camera, lights, material,
            quality, nullptr, nullptr, modelMatrix);
        OpenGL1MeshRenderer::release(mesh);
        delete mesh;
    }
}
