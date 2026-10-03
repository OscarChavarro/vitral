#include <glad/gl.h>
#ifdef __APPLE__
#include <OpenGL/glu.h>
#else
#include <GL/glu.h>
#endif
#include <algorithm>
#include <array>
#include <cmath>
#include <deque>
#include <map>
#include <string>
#include <tuple>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/polyhedralBoundedSolidOperators/validation/_PolyhedralBoundedSolidFaceValidator.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ShaderProgramUtil.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SimpleMaterialRenderer.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidDebugRenderer.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"

#ifdef __APPLE__
#define VITRAL_GLU_CALLBACK(f) reinterpret_cast<GLvoid (*)()>(f)
#else
#define VITRAL_GLU_CALLBACK(f) reinterpret_cast<_GLUfuncptr>(f)
#endif

namespace {

const float SURFACE_POLYGON_OFFSET_FACTOR = 2.0f;
const float SURFACE_POLYGON_OFFSET_UNITS = 2.0f;
const double VERTEX_KEY_QUANTIZATION = 1.0e6;

/// Quantized coordinates of a vertex (Java `VertexCoordinateKey`)
typedef std::tuple<long long, long long, long long> VertexCoordinateKey;
typedef std::map<VertexCoordinateKey, std::vector<Vector3Dd> > VertexNormalsMap;

/**
As Java `Math.round`.
*/
long long quantizeCoordinate(double value)
{
    return (long long)std::floor(value * VERTEX_KEY_QUANTIZATION + 0.5);
}

VertexCoordinateKey keyFrom(const Vector3Dd& point)
{
    return VertexCoordinateKey(quantizeCoordinate(point.x()),
        quantizeCoordinate(point.y()), quantizeCoordinate(point.z()));
}

/**
Collects the triangles given by the GLU tessellator for a face (Java
`FaceTessellationCollector`): the vertices of each primitive are kept until
its end, and then emitted as separate triangles (x, y, z, 1).
*/
struct FaceTessellationCollector {
    /// Vertices given to GLU, and the ones it creates at intersections
    std::deque<std::array<double, 3> > kept;
    java::ArrayList<float>* out;
    std::vector<const double*> pending;
    GLenum mode;

    explicit FaceTessellationCollector(java::ArrayList<float>* out)
        : out(out), mode((GLenum)-1) {}

    void emitVertex(const double* v)
    {
        out->add((float)v[0]);
        out->add((float)v[1]);
        out->add((float)v[2]);
        out->add(1.0f);
    }

    void emitTriangle(const double* a, const double* b, const double* c)
    {
        emitVertex(a);
        emitVertex(b);
        emitVertex(c);
    }

    void end()
    {
        if ( mode == GL_TRIANGLES ) {
            for ( size_t i = 0; i + 2 < pending.size(); i += 3 ) {
                emitTriangle(pending[i], pending[i + 1], pending[i + 2]);
            }
        }
        else if ( mode == GL_TRIANGLE_FAN ) {
            for ( size_t i = 1; i + 1 < pending.size(); i++ ) {
                emitTriangle(pending[0], pending[i], pending[i + 1]);
            }
        }
        else if ( mode == GL_TRIANGLE_STRIP ) {
            for ( size_t i = 0; i + 2 < pending.size(); i++ ) {
                if ( (i & 1) == 0 ) {
                    emitTriangle(pending[i], pending[i + 1], pending[i + 2]);
                }
                else {
                    emitTriangle(pending[i + 1], pending[i], pending[i + 2]);
                }
            }
        }
        pending.clear();
    }
};

void GLAPIENTRY tessBegin(GLenum type, void* data)
{
    FaceTessellationCollector* collector = (FaceTessellationCollector*)data;
    collector->mode = type;
    collector->pending.clear();
}

void GLAPIENTRY tessVertex(void* vertexData, void* data)
{
    if ( vertexData == nullptr ) {
        return;
    }
    ((FaceTessellationCollector*)data)->pending.push_back(
        (const double*)vertexData);
}

void GLAPIENTRY tessEnd(void* data)
{
    ((FaceTessellationCollector*)data)->end();
}

void GLAPIENTRY tessCombine(GLdouble coords[3], void*[4], GLfloat[4],
                            void** outData, void* data)
{
    FaceTessellationCollector* collector = (FaceTessellationCollector*)data;
    std::array<double, 3> combined = {{ coords[0], coords[1], coords[2] }};
    collector->kept.push_back(combined);
    *outData = collector->kept.back().data();
}

void GLAPIENTRY tessError(GLenum, void* data)
{
    ((FaceTessellationCollector*)data)->pending.clear();
}

/**
@param out receives the triangles of the face, as (x, y, z, 1) vertices
*/
void tessellateFace(_PolyhedralBoundedSolidFace* face,
                    java::ArrayList<float>& out)
{
    if ( face == nullptr ) {
        return;
    }

    GLUtesselator* tess = gluNewTess();
    if ( tess == nullptr ) {
        return;
    }
    FaceTessellationCollector collector(&out);

    gluTessCallback(tess, GLU_TESS_BEGIN_DATA, VITRAL_GLU_CALLBACK(tessBegin));
    gluTessCallback(tess, GLU_TESS_VERTEX_DATA, VITRAL_GLU_CALLBACK(tessVertex));
    gluTessCallback(tess, GLU_TESS_END_DATA, VITRAL_GLU_CALLBACK(tessEnd));
    gluTessCallback(tess, GLU_TESS_COMBINE_DATA, VITRAL_GLU_CALLBACK(tessCombine));
    gluTessCallback(tess, GLU_TESS_ERROR_DATA, VITRAL_GLU_CALLBACK(tessError));

    gluTessBeginPolygon(tess, &collector);
    for ( long i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }
        gluTessBeginContour(tess);
        _PolyhedralBoundedSolidHalfEdge* he = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* start = he;
        do {
            he = he->next();
            if ( he == nullptr ) {
                break;
            }
            const Vector3Dd& p = he->startingVertex->position;
            std::array<double, 3> vertex = {{ p.x(), p.y(), p.z() }};
            collector.kept.push_back(vertex);
            double* kept = collector.kept.back().data();
            gluTessVertex(tess, kept, kept);
        } while ( he != start );
        gluTessEndContour(tess);
    }
    gluTessEndPolygon(tess);
    gluDeleteTess(tess);
}

void buildSmoothedVertexNormals(PolyhedralBoundedSolid* solid,
                                VertexNormalsMap& incidentNormals)
{
    if ( solid == nullptr ) {
        return;
    }

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        if ( face == nullptr || face->getContainingPlane() == nullptr ||
             _PolyhedralBoundedSolidFaceValidator::isSurfaceDegenerate(face) ) {
            continue;
        }

        Vector3Dd faceNormal = face->getContainingPlane()->getNormal();
        if ( faceNormal.length() <= VSDK::EPSILON ) {
            continue;
        }
        faceNormal = faceNormal.normalized();

        for ( long j = 0; j < face->boundariesList.size(); j++ ) {
            _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(j);
            if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
                continue;
            }
            _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
            _PolyhedralBoundedSolidHalfEdge* he = start;
            do {
                if ( he->startingVertex != nullptr ) {
                    incidentNormals[keyFrom(he->startingVertex->position)]
                        .push_back(faceNormal);
                }
                he = he->next();
            } while ( he != nullptr && he != start );
        }
    }
}

Vector3Dd resolveVertexNormal(
    const VertexNormalsMap* vertexNormals,
    float px,
    float py,
    float pz,
    const Vector3Dd& fallback,
    double smoothingThresholdDegrees)
{
    if ( vertexNormals == nullptr || vertexNormals->empty() ) {
        return fallback;
    }
    VertexNormalsMap::const_iterator found = vertexNormals->find(
        VertexCoordinateKey(quantizeCoordinate(px), quantizeCoordinate(py),
                            quantizeCoordinate(pz)));
    if ( found == vertexNormals->end() || found->second.empty() ) {
        return fallback;
    }
    const std::vector<Vector3Dd>& incidentNormals = found->second;

    double clampedThreshold = std::max(0.0,
        std::min(180.0, smoothingThresholdDegrees));
    double cosineThreshold = std::cos(clampedThreshold * M_PI / 180.0);
    Vector3Dd sum(0, 0, 0);
    int count = 0;

    for ( size_t i = 0; i < incidentNormals.size(); i++ ) {
        const Vector3Dd& candidate = incidentNormals[i];
        if ( candidate.length() <= VSDK::EPSILON ) {
            continue;
        }
        Vector3Dd normalizedCandidate = candidate.normalized();
        if ( fallback.dotProduct(normalizedCandidate) >= cosineThreshold ) {
            sum = sum.add(normalizedCandidate);
            count++;
        }
    }

    if ( count == 0 || sum.length() <= VSDK::EPSILON ) {
        return fallback;
    }
    return sum.normalized();
}

void appendFaceMesh(
    _PolyhedralBoundedSolidFace* face,
    java::ArrayList<float>& positions,
    java::ArrayList<float>& normals,
    java::ArrayList<float>& uvs,
    const VertexNormalsMap* vertexNormals,
    double smoothingThresholdDegrees)
{
    if ( face == nullptr ||
         _PolyhedralBoundedSolidFaceValidator::isSurfaceDegenerate(face) ) {
        return;
    }

    InfinitePlane* plane = face->getContainingPlane();
    if ( plane == nullptr ) {
        return;
    }
    Vector3Dd normal = plane->getNormal().normalized();
    java::ArrayList<float> faceTriangles;
    tessellateFace(face, faceTriangles);
    for ( long i = 0; i + 3 < faceTriangles.size(); i += 4 ) {
        float px = faceTriangles[i];
        float py = faceTriangles[i + 1];
        float pz = faceTriangles[i + 2];
        Vector3Dd vertexNormal = resolveVertexNormal(vertexNormals,
            px, py, pz, normal, smoothingThresholdDegrees);
        positions.add(faceTriangles[i]);
        positions.add(faceTriangles[i + 1]);
        positions.add(faceTriangles[i + 2]);
        positions.add(1.0f);
        normals.add((float)vertexNormal.x());
        normals.add((float)vertexNormal.y());
        normals.add((float)vertexNormal.z());
        uvs.add(0.0f);
        uvs.add(0.0f);
    }
}

void buildMesh(PolyhedralBoundedSolid* solid, bool smoothNormals,
               const RendererConfiguration* quality,
               OpenGL4PolyhedralBoundedSolidRenderer::MeshData& mesh)
{
    VertexNormalsMap vertexNormals;
    if ( smoothNormals ) {
        buildSmoothedVertexNormals(solid, vertexNormals);
    }

    for ( long i = 0; i < solid->getPolygonsList().size(); i++ ) {
        _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(i);
        appendFaceMesh(face, mesh.positions, mesh.normals, mesh.uvs,
            smoothNormals ? &vertexNormals : nullptr,
            quality->getVertexNormalSmoothingThresholdDegrees());
    }
    mesh.vertexCount = (int)(mesh.positions.size() / 4);
}

double clamp01(double value)
{
    if ( value < 0.0 ) {
        return 0.0;
    }
    if ( value > 1.0 ) {
        return 1.0;
    }
    return value;
}

ColorRgb evaluateFlatColor(
    const Vector3Dd& pointGlobal,
    const Vector3Dd& normalGlobal,
    const SimpleMaterial& material,
    const java::ArrayList<Light*>& activeLights,
    const Vector3Dd& cameraPosition)
{
    Vector3Dd normal = normalGlobal;
    Vector3Dd viewDir = cameraPosition.subtract(pointGlobal);
    if ( viewDir.length() > VSDK::EPSILON ) {
        viewDir = viewDir.normalized();
        if ( normal.dotProduct(viewDir) < 0.0 ) {
            normal = normal.multiply(-1.0);
        }
    }

    ColorRgb ambient = material.getAmbient();
    ColorRgb diffuse = material.getDiffuse();
    ColorRgb specular = material.getSpecular();
    double r = ambient.r();
    double g = ambient.g();
    double b = ambient.b();

    for ( long i = 0; i < activeLights.size(); i++ ) {
        Light* light = activeLights.get(i);
        Vector3Dd lightDir = light->getPosition().subtract(pointGlobal);
        if ( lightDir.length() <= VSDK::EPSILON ) {
            continue;
        }
        lightDir = lightDir.normalized();
        double ndotl = std::max(normal.dotProduct(lightDir), 0.0);
        Vector3Dd reflection = normal.multiply(2.0 * ndotl).subtract(lightDir);
        double spec = 0.0;
        if ( ndotl > 0.0 && viewDir.length() > VSDK::EPSILON &&
             reflection.length() > VSDK::EPSILON ) {
            spec = std::pow(std::max(reflection.normalized()
                .dotProduct(viewDir), 0.0), material.getPhongExponent());
        }

        ColorRgb lightColor = light->getEmission();
        r += lightColor.r() * diffuse.r() * ndotl +
            lightColor.r() * specular.r() * spec;
        g += lightColor.g() * diffuse.g() * ndotl +
            lightColor.g() * specular.g() * spec;
        b += lightColor.b() * diffuse.b() * ndotl +
            lightColor.b() * specular.b() * spec;
    }

    return ColorRgb(clamp01(r), clamp01(g), clamp01(b));
}

void appendTriangleVertex(java::ArrayList<float>& positions,
                          java::ArrayList<float>& colors,
                          const Vector3Dd& point, const ColorRgb& color)
{
    positions.add((float)point.x());
    positions.add((float)point.y());
    positions.add((float)point.z());
    colors.add((float)color.r());
    colors.add((float)color.g());
    colors.add((float)color.b());
}

/**
Java `buildFlatShadedSurfaceTriangles`: one color per triangle, evaluated at
its centroid.
*/
void buildFlatShadedSurfaceTriangles(
    const OpenGL4PolyhedralBoundedSolidRenderer::MeshData& mesh,
    const Matrix4x4d& modelMatrix,
    const Matrix4x4d& modelViewITLocal,
    const SimpleMaterial& material,
    const java::ArrayList<Light*>& activeLights,
    const Vector3Dd& cameraPosition,
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors)
{
    if ( mesh.positions.size() < 12 || mesh.normals.size() < 9 ) {
        return;
    }

    for ( long pos = 0, normal = 0;
          pos + 11 < mesh.positions.size() && normal + 8 < mesh.normals.size();
          pos += 12, normal += 9 ) {
        Vector3Dd p0(mesh.positions[pos], mesh.positions[pos + 1],
            mesh.positions[pos + 2]);
        Vector3Dd p1(mesh.positions[pos + 4], mesh.positions[pos + 5],
            mesh.positions[pos + 6]);
        Vector3Dd p2(mesh.positions[pos + 8], mesh.positions[pos + 9],
            mesh.positions[pos + 10]);

        Vector3Dd centroidLocal = p0.add(p1).add(p2).multiply(1.0 / 3.0);
        Vector3Dd centroidGlobal = modelMatrix.multiply(centroidLocal);
        Vector3Dd normalLocal = Vector3Dd(mesh.normals[normal],
            mesh.normals[normal + 1], mesh.normals[normal + 2]).normalized();
        Vector4Dd normalGlobal4 = modelViewITLocal.multiply(
            Vector4Dd(normalLocal.x(), normalLocal.y(), normalLocal.z(), 0.0));
        Vector3Dd normalGlobal = Vector3Dd(normalGlobal4.x(),
            normalGlobal4.y(), normalGlobal4.z()).normalized();

        ColorRgb color = evaluateFlatColor(centroidGlobal, normalGlobal,
            material, activeLights, cameraPosition);

        appendTriangleVertex(positions, colors, p0, color);
        appendTriangleVertex(positions, colors, p1, color);
        appendTriangleVertex(positions, colors, p2, color);
    }
}

void upload(const java::ArrayList<float>& data)
{
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr)(data.size() * sizeof(float)),
                 data.data(), GL_STREAM_DRAW);
}

void setMatrix(unsigned int programId, const char* name,
               const Matrix4x4d& matrix)
{
    GLint loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        float* values = matrix.exportToFloatArrayColumnOrder();
        glUniformMatrix4fv(loc, 1, GL_FALSE, values);
        delete[] values;
    }
}

void setVector3(unsigned int programId, const char* name,
                const Vector3Dd& value)
{
    GLint loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
    }
}

void setVector3(unsigned int programId, const char* name,
                const ColorRgb& value)
{
    GLint loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        glUniform3f(loc, (float)value.r(), (float)value.g(), (float)value.b());
    }
}

void setInt(unsigned int programId, const char* name, int value)
{
    GLint loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        glUniform1i(loc, value);
    }
}

void setFloat(unsigned int programId, const char* name, float value)
{
    GLint loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        glUniform1f(loc, value);
    }
}

void deleteLights(java::ArrayList<Light*>& lights)
{
    for ( long i = 0; i < lights.size(); i++ ) {
        delete lights.get(i);
    }
    lights.clear();
}

}

bool OpenGL4PolyhedralBoundedSolidRenderer::initialized = false;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::meshVaoId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::meshPositionVboId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::meshNormalVboId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::meshUvVboId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::colorVaoId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::colorPositionVboId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::colorDataVboId = 0;
unsigned int OpenGL4PolyhedralBoundedSolidRenderer::colorProgramId = 0;

OpenGL4PolyhedralBoundedSolidRenderer::MeshData::MeshData()
    : vertexCount(0)
{
}

void OpenGL4PolyhedralBoundedSolidRenderer::draw(
    PolyhedralBoundedSolid* solid, Camera* camera,
    const RendererConfiguration* quality)
{
    draw(solid, camera, quality, Matrix4x4d::identityMatrix());
}

void OpenGL4PolyhedralBoundedSolidRenderer::draw(
    PolyhedralBoundedSolid* solid, Camera* camera,
    const RendererConfiguration* quality, const Matrix4x4d& modelMatrix)
{
    if ( camera == nullptr ) {
        return;
    }
    Matrix4x4d mvp = camera->calculateProjectionMatrix().multiply(modelMatrix);
    draw(solid, camera, quality, modelMatrix, mvp);
}

void OpenGL4PolyhedralBoundedSolidRenderer::drawDebugFaceBoundary(
    PolyhedralBoundedSolid* solid, int faceIndex,
    const Matrix4x4d& modelViewProjection)
{
    OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugFaceBoundary(solid,
        faceIndex, modelViewProjection);
}

void OpenGL4PolyhedralBoundedSolidRenderer::drawDebugFace(
    PolyhedralBoundedSolid* solid, int faceIndex,
    const Matrix4x4d& modelMatrix, const Matrix4x4d& modelViewProjection,
    Camera* camera)
{
    OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugFace(solid,
        faceIndex, modelMatrix, modelViewProjection, camera);
}

void OpenGL4PolyhedralBoundedSolidRenderer::drawDebugEdges(
    PolyhedralBoundedSolid* solid, Camera* camera, int edgeIndex,
    const Matrix4x4d& modelViewProjection)
{
    OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugEdges(solid,
        camera, edgeIndex, modelViewProjection);
}

void OpenGL4PolyhedralBoundedSolidRenderer::draw(
    PolyhedralBoundedSolid* solid, Camera* camera,
    const RendererConfiguration* quality, const Matrix4x4d& modelMatrix,
    const Matrix4x4d& modelViewProjection)
{
    if ( solid == nullptr || quality == nullptr || camera == nullptr ) {
        return;
    }

    ensureInitialized();

    Matrix4x4d modelViewITLocal = modelMatrix.invert().transpose();
    bool smoothNormals =
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_FLAT &&
        quality->getShadingType() != RendererConfiguration::SHADING_TYPE_NOLIGHT;
    MeshData mesh;
    buildMesh(solid, smoothNormals, quality, mesh);
    SimpleMaterial material = OpenGL4SimpleMaterialRenderer::getActiveMaterial();
    java::ArrayList<Light*> activeLights;
    OpenGL4LightRenderer::getActiveLights(activeLights);
    Vector3Dd cameraPosition = camera->getPosition();

    if ( quality->isSurfacesSet() && mesh.vertexCount > 0 ) {
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_TRUE);
        glDepthFunc(GL_LESS);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR,
            SURFACE_POLYGON_OFFSET_UNITS);
        if ( material.isDoubleSided() ) {
            glDisable(GL_CULL_FACE);
        }
        else {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
        }

        if ( quality->getShadingType() ==
             RendererConfiguration::SHADING_TYPE_FLAT ) {
            java::ArrayList<float> flatPositions;
            java::ArrayList<float> flatColors;
            buildFlatShadedSurfaceTriangles(mesh, modelMatrix,
                modelViewITLocal, material, activeLights, cameraPosition,
                flatPositions, flatColors);
            if ( flatPositions.size() > 0 ) {
                drawColoredPrimitives(modelViewProjection, flatPositions,
                    flatColors, GL_TRIANGLES, 1.0f, 0.0f);
            }
        }
        else {
            unsigned int programId = OpenGL4RendererConfigurationShaderSelector
                ::selectSurfaceShaderProgram(quality, false, false);
            configureSurfaceProgram(programId, modelViewProjection,
                modelMatrix, modelViewITLocal, material, &activeLights,
                quality, cameraPosition);
            renderMesh(mesh, GL_TRIANGLES);
            OpenGL4RendererConfigurationShaderSelector::deactivateShader();
        }

        glDisable(GL_POLYGON_OFFSET_FILL);
    }
    deleteLights(activeLights);

    OpenGL4PolyhedralBoundedSolidDebugRenderer::drawDebugOverlays(solid,
        camera, quality, modelViewProjection);
}

void OpenGL4PolyhedralBoundedSolidRenderer::ensureInitialized()
{
    if ( initialized ) {
        return;
    }

    colorProgramId = OpenGL4ShaderProgramUtil::createProgramFromFiles(
        "lineVertexShader.glsl", "linePixelShader.glsl");

    glGenVertexArrays(1, &meshVaoId);
    glGenBuffers(1, &meshPositionVboId);
    glGenBuffers(1, &meshNormalVboId);
    glGenBuffers(1, &meshUvVboId);

    glGenVertexArrays(1, &colorVaoId);
    glGenBuffers(1, &colorPositionVboId);
    glGenBuffers(1, &colorDataVboId);

    initialized = true;
}

void OpenGL4PolyhedralBoundedSolidRenderer::release()
{
    if ( !initialized ) {
        return;
    }

    if ( meshPositionVboId != 0 ) {
        glDeleteBuffers(1, &meshPositionVboId);
        meshPositionVboId = 0;
    }
    if ( meshNormalVboId != 0 ) {
        glDeleteBuffers(1, &meshNormalVboId);
        meshNormalVboId = 0;
    }
    if ( meshUvVboId != 0 ) {
        glDeleteBuffers(1, &meshUvVboId);
        meshUvVboId = 0;
    }
    if ( colorPositionVboId != 0 ) {
        glDeleteBuffers(1, &colorPositionVboId);
        colorPositionVboId = 0;
    }
    if ( colorDataVboId != 0 ) {
        glDeleteBuffers(1, &colorDataVboId);
        colorDataVboId = 0;
    }
    if ( meshVaoId != 0 ) {
        glDeleteVertexArrays(1, &meshVaoId);
        meshVaoId = 0;
    }
    if ( colorVaoId != 0 ) {
        glDeleteVertexArrays(1, &colorVaoId);
        colorVaoId = 0;
    }
    if ( colorProgramId != 0 ) {
        glDeleteProgram(colorProgramId);
        colorProgramId = 0;
    }

    initialized = false;
}

void OpenGL4PolyhedralBoundedSolidRenderer::configureSurfaceProgram(
    unsigned int programId,
    const Matrix4x4d& modelViewProjection,
    const Matrix4x4d& modelViewLocal,
    const Matrix4x4d& modelViewITLocal,
    const SimpleMaterial& material,
    const java::ArrayList<Light*>* lights,
    const RendererConfiguration* quality,
    const Vector3Dd& cameraPosition)
{
    ColorRgb kd = material.getDiffuse();
    OpenGL4RendererConfigurationShaderSelector::activateShader(programId,
        modelViewProjection, quality, (float)kd.r(), (float)kd.g(),
        (float)kd.b());

    setMatrix(programId, "modelViewLocal", modelViewLocal);
    setMatrix(programId, "modelViewITLocal", modelViewITLocal);
    setVector3(programId, "cameraPositionGlobal", cameraPosition);
    setVector3(programId, "ambientColor", material.getAmbient());
    setVector3(programId, "diffuseColor", material.getDiffuse());
    setVector3(programId, "specularColor", material.getSpecular());
    setFloat(programId, "phongExponent", (float)material.getPhongExponent());
    setInt(programId, "withTexture", 0);
    setInt(programId, "withBumpMap", 0);

    int lightCount = 0;
    if ( lights != nullptr ) {
        lightCount = (int)std::min(lights->size(), 8L);
        for ( int i = 0; i < lightCount; i++ ) {
            Light* light = lights->get(i);
            std::string index = std::to_string(i);
            setVector3(programId,
                ("lightPositionsGlobal[" + index + "]").c_str(),
                light->getPosition());
            setVector3(programId,
                ("lightColorsGlobal[" + index + "]").c_str(),
                light->getEmission());
        }
    }
    setInt(programId, "numberOfLights", lightCount);
}

void OpenGL4PolyhedralBoundedSolidRenderer::renderMesh(const MeshData& mesh,
                                                       unsigned int mode)
{
    glBindVertexArray(meshVaoId);

    glBindBuffer(GL_ARRAY_BUFFER, meshPositionVboId);
    upload(mesh.positions);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 4, GL_FLOAT, GL_FALSE, 0, nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, meshNormalVboId);
    upload(mesh.normals);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, meshUvVboId);
    upload(mesh.uvs);
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, nullptr);

    glDrawArrays(mode, 0, mesh.vertexCount);

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glDisableVertexAttribArray(2);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
}

void OpenGL4PolyhedralBoundedSolidRenderer::drawColoredPrimitives(
    const Matrix4x4d& mvp,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    unsigned int mode,
    float size,
    float depthBiasNdc)
{
    if ( positions.size() == 0 || colors.size() == 0 ) {
        return;
    }

    glUseProgram(colorProgramId);
    GLint mvpLoc = glGetUniformLocation(colorProgramId,
        "modelViewProjectionLocal");
    if ( mvpLoc >= 0 ) {
        float* values = mvp.exportToFloatArrayColumnOrder();
        glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, values);
        delete[] values;
    }
    GLint depthBiasLoc = glGetUniformLocation(colorProgramId, "depthBiasNdc");
    if ( depthBiasLoc >= 0 ) {
        glUniform1f(depthBiasLoc, depthBiasNdc);
    }

    glBindVertexArray(colorVaoId);

    glBindBuffer(GL_ARRAY_BUFFER, colorPositionVboId);
    upload(positions);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, colorDataVboId);
    upload(colors);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    if ( mode == GL_POINTS ) {
        glPointSize(size);
    }
    else if ( mode == GL_LINES ) {
        glLineWidth(size);
    }
    glDrawArrays(mode, 0, (GLsizei)(positions.size() / 3));

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glUseProgram(0);
}

void OpenGL4PolyhedralBoundedSolidRenderer::buildFaceMesh(
    PolyhedralBoundedSolid* solid, int faceIndex, MeshData& outMesh)
{
    if ( faceIndex >= 0 && faceIndex < solid->getPolygonsList().size() ) {
        appendFaceMesh(solid->getPolygonsList().get(faceIndex),
            outMesh.positions, outMesh.normals, outMesh.uvs, nullptr, 0.0);
    }
    outMesh.vertexCount = (int)(outMesh.positions.size() / 4);
}
