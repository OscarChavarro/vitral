#include <algorithm>
#include <cmath>
#include <set>
#include <string>
#include <vector>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolidNumericPolicy.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidFace.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidHalfEdge.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidLoop.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/nodes/_PolyhedralBoundedSolidVertex.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LabelCanvas.h"
#include "vsdk/toolkit/render/opengl4/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidDebugHUDRenderer.h"

namespace {

const double VERTEX_LABEL_GROUPING_PIXELS = 18.0;
const double SCREEN_DISTANCE_DELTA = 1.0;

double distanceSquared3D(const Vector3Dd& a, const Vector3Dd& b)
{
    double dx = a.x() - b.x();
    double dy = a.y() - b.y();
    double dz = a.z() - b.z();

    return dx * dx + dy * dy + dz * dz;
}

double distanceSquared2D(const Vector3Dd& a, const Vector3Dd& b)
{
    double dx = a.x() - b.x();
    double dy = a.y() - b.y();

    return dx * dx + dy * dy;
}

/**
Vertices labeled together, with the position of the first one.
*/
struct VertexLabelGroup {
    std::vector<_PolyhedralBoundedSolidVertex*> vertices;
    std::vector<Vector3Dd> projectedPositions;
    Vector3Dd projectedPosition;

    VertexLabelGroup(_PolyhedralBoundedSolidVertex* vertex,
                     const Vector3Dd& projectedPosition)
        : projectedPosition(projectedPosition)
    {
        add(vertex, projectedPosition);
    }

    void add(_PolyhedralBoundedSolidVertex* vertex,
             const Vector3Dd& projectedPosition)
    {
        vertices.push_back(vertex);
        projectedPositions.push_back(projectedPosition);
    }

    bool containsCloseVertex(_PolyhedralBoundedSolidVertex* vertex,
                             const Vector3Dd& projectedPosition,
                             double spatialTolerance) const
    {
        double spatialToleranceSquared = spatialTolerance * spatialTolerance;
        double viewportToleranceSquared = VERTEX_LABEL_GROUPING_PIXELS *
            VERTEX_LABEL_GROUPING_PIXELS;

        for ( size_t i = 0; i < vertices.size(); i++ ) {
            if ( distanceSquared3D(vertices[i]->position,
                 vertex->position) <= spatialToleranceSquared ) {
                return true;
            }
            if ( distanceSquared2D(projectedPositions[i],
                 projectedPosition) <= viewportToleranceSquared ) {
                return true;
            }
        }
        return false;
    }
};

/**
@param out receives the viewport pixel (x, y from the top) and the depth
in [0, 1] of the point
@return false if the point is outside the view volume (Java returns null)
*/
bool projectVertexToViewport(const Vector3Dd& worldPosition, Camera* camera,
                             int viewportWidth, int viewportHeight,
                             Vector3Dd& out)
{
    if ( camera == nullptr ) {
        return false;
    }

    Matrix4x4d projection = camera->calculateProjectionMatrix();
    Vector4Dd clip = projection.multiply(Vector4Dd(worldPosition.x(),
        worldPosition.y(), worldPosition.z(), 1.0));
    if ( std::abs(clip.w()) <= VSDK::EPSILON ) {
        return false;
    }

    double ndcX = clip.x() / clip.w();
    double ndcY = clip.y() / clip.w();
    double ndcZ = clip.z() / clip.w();
    if ( ndcX < -1.0 || ndcX > 1.0 || ndcY < -1.0 || ndcY > 1.0 ||
         ndcZ < -1.0 || ndcZ > 1.0 ) {
        return false;
    }

    double width = std::max(1, viewportWidth);
    double height = std::max(1, viewportHeight);
    double x = ((ndcX + 1.0) * 0.5) * width;
    double y = height - (((ndcY + 1.0) * 0.5) * height);
    double z = (ndcZ + 1.0) * 0.5;
    out = Vector3Dd(x, y, z);
    return true;
}

std::vector<VertexLabelGroup> buildVertexGroups(
    PolyhedralBoundedSolid* solid, Camera* camera,
    int viewportWidth, int viewportHeight)
{
    std::vector<VertexLabelGroup> vertexGroups;
    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    double spatialTolerance = numericContext.bigEpsilon() * SCREEN_DISTANCE_DELTA;

    for ( long i = 0; i < solid->getVerticesList().size(); i++ ) {
        _PolyhedralBoundedSolidVertex* vertex = solid->getVerticesList().get(i);
        Vector3Dd projectedPosition;

        if ( !projectVertexToViewport(vertex->position, camera,
                 viewportWidth, viewportHeight, projectedPosition) ) {
            continue;
        }
        VertexLabelGroup* group = nullptr;
        for ( size_t j = 0; j < vertexGroups.size(); j++ ) {
            if ( vertexGroups[j].containsCloseVertex(vertex,
                     projectedPosition, spatialTolerance) ) {
                group = &vertexGroups[j];
                break;
            }
        }
        if ( group == nullptr ) {
            vertexGroups.push_back(VertexLabelGroup(vertex, projectedPosition));
        }
        else {
            group->add(vertex, projectedPosition);
        }
    }
    return vertexGroups;
}

std::vector<Vector3Dd> collectProjectedFaceVertices(
    _PolyhedralBoundedSolidFace* face, Camera* camera,
    int viewportWidth, int viewportHeight)
{
    std::vector<Vector3Dd> projected;
    std::set<int> visitedVertexIds;

    for ( long i = 0; i < face->boundariesList.size(); i++ ) {
        _PolyhedralBoundedSolidLoop* loop = face->boundariesList.get(i);
        if ( loop == nullptr || loop->boundaryStartHalfEdge == nullptr ) {
            continue;
        }

        _PolyhedralBoundedSolidHalfEdge* start = loop->boundaryStartHalfEdge;
        _PolyhedralBoundedSolidHalfEdge* he = start;
        do {
            _PolyhedralBoundedSolidVertex* vertex = he->startingVertex;
            if ( vertex != nullptr &&
                 visitedVertexIds.insert(vertex->id).second ) {
                Vector3Dd projectedVertex;
                if ( projectVertexToViewport(vertex->position, camera,
                         viewportWidth, viewportHeight, projectedVertex) ) {
                    projected.push_back(projectedVertex);
                }
            }
            he = he->next();
        } while ( he != start );
    }
    return projected;
}

bool isVertexLabelVisible(_PolyhedralBoundedSolidVertex* vertex,
                          PolyhedralBoundedSolid* solid, Camera* camera)
{
    if ( vertex == nullptr || camera == nullptr ) {
        return true;
    }

    Ray visibilityRay(camera->getPosition(),
        vertex->position.subtract(camera->getPosition()));
    double vertexRayT = vertex->position.subtract(visibilityRay.getOrigin())
        .dotProduct(visibilityRay.getDirection());
    if ( vertexRayT <= VSDK::EPSILON ) {
        return true;
    }

    PolyhedralBoundedSolidNumericPolicy::ToleranceContext numericContext =
        PolyhedralBoundedSolidNumericPolicy::forSolid(solid);
    Vector3Dd closestPointOnRay = visibilityRay.getOrigin().add(
        visibilityRay.getDirection().multiply(vertexRayT));
    if ( closestPointOnRay.subtract(vertex->position).length() >=
         numericContext.bigEpsilon() ) {
        return true;
    }

    Ray* hit = solid->doIntersectionFirstHit(visibilityRay);
    if ( hit == nullptr ) {
        return true;
    }
    double hitT = hit->getT();
    delete hit;

    return !(vertexRayT - hitT >= numericContext.bigEpsilon());
}

std::vector<_PolyhedralBoundedSolidVertex*> filterVisibleVertices(
    const std::vector<_PolyhedralBoundedSolidVertex*>& vertices,
    PolyhedralBoundedSolid* solid, Camera* camera)
{
    std::vector<_PolyhedralBoundedSolidVertex*> visibleVertices;

    for ( size_t i = 0; i < vertices.size(); i++ ) {
        if ( isVertexLabelVisible(vertices[i], solid, camera) ) {
            visibleVertices.push_back(vertices[i]);
        }
    }
    return visibleVertices;
}

Vector3Dd averageProjectedPosition(const std::vector<Vector3Dd>& projectedVertices)
{
    double sx = 0.0;
    double sy = 0.0;
    double sz = 0.0;

    for ( size_t i = 0; i < projectedVertices.size(); i++ ) {
        const Vector3Dd& p = projectedVertices[i];
        sx += p.x();
        sy += p.y();
        sz += p.z();
    }

    double n = (double)projectedVertices.size();
    return Vector3Dd(sx / n, sy / n, sz / n);
}

java::String buildVertexIdsLabel(
    const std::vector<_PolyhedralBoundedSolidVertex*>& vertices)
{
    std::string label;

    for ( size_t i = 0; i < vertices.size(); i++ ) {
        if ( i > 0 ) {
            label += ", ";
        }
        label += std::to_string(vertices[i]->id);
    }
    return java::String(label.c_str());
}

/**
As Java `Math.round`.
*/
int roundToInt(double value)
{
    return (int)std::floor(value + 0.5);
}

}

void OpenGL4PolyhedralBoundedSolidDebugHUDRenderer::drawSelectedFaceLabel(
    OpenGL4LabelCanvas* g,
    PolyhedralBoundedSolid* solid,
    int faceIndex,
    Camera* camera,
    int viewportWidth,
    int viewportHeight)
{
    if ( g == nullptr || solid == nullptr || faceIndex < 0 ||
         faceIndex >= solid->getPolygonsList().size() ) {
        return;
    }

    _PolyhedralBoundedSolidFace* face = solid->getPolygonsList().get(faceIndex);
    std::vector<Vector3Dd> projectedVertices = collectProjectedFaceVertices(
        face, camera, viewportWidth, viewportHeight);
    if ( projectedVertices.empty() ) {
        return;
    }

    Vector3Dd projectedMidpoint = averageProjectedPosition(projectedVertices);
    g->setColor(ColorRgb(0, 1, 1));
    g->drawString(java::String(std::to_string(face->id).c_str()),
        roundToInt(projectedMidpoint.x()), roundToInt(projectedMidpoint.y()));
}

void OpenGL4PolyhedralBoundedSolidDebugHUDRenderer::drawDebugVertexLabels(
    OpenGL4LabelCanvas* g,
    PolyhedralBoundedSolid* solid,
    Camera* camera,
    int viewportWidth,
    int viewportHeight)
{
    if ( g == nullptr || solid == nullptr ) {
        return;
    }

    std::vector<VertexLabelGroup> vertexGroups = buildVertexGroups(solid,
        camera, viewportWidth, viewportHeight);

    for ( size_t i = 0; i < vertexGroups.size(); i++ ) {
        const VertexLabelGroup& group = vertexGroups[i];
        std::vector<_PolyhedralBoundedSolidVertex*> visibleVertices =
            filterVisibleVertices(group.vertices, solid, camera);

        if ( !visibleVertices.empty() ) {
            g->setColor(ColorRgb(1, 1, 1));
            g->drawString(buildVertexIdsLabel(visibleVertices),
                roundToInt(group.projectedPosition.x()) + 4,
                roundToInt(group.projectedPosition.y()) + 4);
        }
    }
}
