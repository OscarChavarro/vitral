#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/GeometryBounds.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/volume/ImplicitSolid.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/processing/SolidBoundaryTessellator.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshBuilder.h"
#include "vsdk/toolkit/render/opengl1/surface/OpenGL1TriangleMeshRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/_OpenGL1SolidBoundaryMeshBuilder.h"

OpenGL1MeshRenderer::Mesh* _OpenGL1SolidBoundaryMeshBuilder::buildMesh(
    Geometry* geometry)
{
    GeometryBounds bounds;
    geometry->getBounds(bounds);
    if ( bounds.getKind() == GeometryBounds::Kind::Unbounded ) {
        double c = ImplicitSolid::DEFAULT_CLIPPING_HALF_SIZE;
        bounds.setFinite(Vector3Dd(-c, -c, -c), Vector3Dd(c, c, c));
    }
    if ( bounds.getKind() != GeometryBounds::Kind::Finite ) {
        return nullptr;
    }
    SimpleBody body;
    body.setGeometryReference(geometry);
    SolidBoundaryTessellationReport report;
    double minmax[6] = {
        bounds.getMinimum().x(), bounds.getMinimum().y(),
        bounds.getMinimum().z(), bounds.getMaximum().x(),
        bounds.getMaximum().y(), bounds.getMaximum().z()};
    double size = OpenGL1MeshBuilder::characteristicSize(minmax);
    // The margin keeps the extreme points of the solid inside the region
    TriangleMesh* mesh = SolidBoundaryTessellator::tessellate(body,
        bounds.inflated(1e-3 * size), RESOLUTION, report);
    if ( mesh == nullptr ) {
        return nullptr;
    }
    OpenGL1MeshRenderer::Mesh* result =
        OpenGL1TriangleMeshRenderer::buildMesh(*mesh);
    delete mesh;
    return result;
}
