#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/curve/Curve.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryCalligraphicExporter.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/Md2Mesh.h"
#include "vsdk/toolkit/environment/geometry/surface/ParametricBiCubicPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/SubdividedBezierPatch.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleStripMesh.h"
#include "vsdk/toolkit/media/Calligraphic3DBuffer.h"
#include "vsdk/toolkit/processing/BezierPatchTessellator.h"

GeometryCalligraphicExporter::GeometryCalligraphicExporter()
{
}

namespace {

/// Exports the edges of a mesh, owned and deleted here
template <class M>
Calligraphic3DBuffer* exportOwnedEdges(M* mesh)
{
    if ( mesh == nullptr ) {
        return nullptr;
    }
    Calligraphic3DBuffer* lines = GeometryCalligraphicExporter::exportEdges(*mesh);
    delete mesh;
    return lines;
}

}

Calligraphic3DBuffer*
GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(Geometry* geometry)
{
    if ( geometry == nullptr ) {
        return nullptr;
    }
    if ( Curve* curve = dynamic_cast<Curve*>(geometry) ) {
        return curve->exportToCalligraphic3DBuffer();
    }
    if ( ParametricBiCubicPatch* patch = dynamic_cast<ParametricBiCubicPatch*>(geometry) ) {
        return exportOwnedEdges(patch->exportToQuadMesh());
    }
    if ( FunctionalExplicitSurface* surface = dynamic_cast<FunctionalExplicitSurface*>(geometry) ) {
        QuadMesh* mesh = surface->exportToQuadMesh();
        return mesh != nullptr ? exportOwnedEdges(mesh) : nullptr;
    }
    if ( SubdividedBezierPatch* patch = dynamic_cast<SubdividedBezierPatch*>(geometry) ) {
        QuadMesh* mesh = BezierPatchTessellator::tessellateQuads(*patch,
            BEZIER_PATCH_STEPS, BEZIER_PATCH_STEPS);
        return mesh != nullptr ? exportOwnedEdges(mesh) : nullptr;
    }
    if ( InfinitePlane* plane = dynamic_cast<InfinitePlane*>(geometry) ) {
        return exportToCalligraphic3DBuffer(*plane, Vector3Dd(0, 0, 0));
    }
    if ( QuadMesh* mesh = dynamic_cast<QuadMesh*>(geometry) ) {
        return exportEdges(*mesh);
    }
    if ( TriangleStripMesh* strips = dynamic_cast<TriangleStripMesh*>(geometry) ) {
        TriangleMesh* mesh = strips->exportToTriangleMesh();
        return mesh != nullptr ? exportOwnedEdges(mesh) : nullptr;
    }

    TriangleMeshGroup meshes;
    if ( !GeometryTriangulator::exportToTriangleMeshGroup(geometry, meshes) ) {
        return nullptr;
    }
    Calligraphic3DBuffer* lines = new Calligraphic3DBuffer();
    for ( long i = 0; i < meshes.getMeshes().size(); i++ ) {
        TriangleMesh& mesh = meshes.getMeshes()[i];
        addEdges(lines, mesh.getVertexPositions(), mesh.getTriangleIndexes(),
            mesh.getNumTriangles(), 3);
    }
    return lines;
}

Calligraphic3DBuffer*
GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
    const InfinitePlane& plane, const Vector3Dd& near)
{
    QuadMesh* mesh = plane.exportToQuadMesh(near, INFINITE_PLANE_SIZE,
        INFINITE_PLANE_TILES);
    return mesh != nullptr ? exportOwnedEdges(mesh) : nullptr;
}

Calligraphic3DBuffer*
GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
    Md2Mesh& model, int frame)
{
    TriangleMesh* mesh = model.exportToTriangleMesh(frame);
    return mesh != nullptr ? exportOwnedEdges(mesh) : nullptr;
}

Calligraphic3DBuffer* GeometryCalligraphicExporter::exportEdges(QuadMesh& mesh)
{
    Calligraphic3DBuffer* lines = new Calligraphic3DBuffer();

    addEdges(lines, mesh.getVertexPositions(), mesh.getQuadIndices(),
        mesh.getNumQuads(), 4);
    return lines;
}

Calligraphic3DBuffer* GeometryCalligraphicExporter::exportEdges(TriangleMesh& mesh)
{
    Calligraphic3DBuffer* lines = new Calligraphic3DBuffer();

    addEdges(lines, mesh.getVertexPositions(), mesh.getTriangleIndexes(),
        mesh.getNumTriangles(), 3);
    return lines;
}

void GeometryCalligraphicExporter::addEdges(Calligraphic3DBuffer* lines,
    const java::ArrayList<double>& positions,
    const java::ArrayList<int>& faces, int numFaces, int sides)
{
    // Java null arrays are the empty lists of the C++ port: the loops below
    // do nothing for them
    long numVertices = positions.size() / 3;
    java::HashMap<long, bool> added;
    int f;
    int k;

    for ( f = 0; f < numFaces && sides*f + sides <= faces.size(); f++ ) {
        for ( k = 0; k < sides; k++ ) {
            int a = faces[sides*f + k];
            int b = faces[sides*f + (k + 1) % sides];
            if ( a == b || a < 0 || b < 0 || a >= numVertices ||
                 b >= numVertices ) {
                continue;
            }
            long key = java::Math::min(a, b) * numVertices + java::Math::max(a, b);
            if ( added.containsKey(key) ) {
                continue;
            }
            added.put(key, true);
            lines->add3DLine(positions[3*a], positions[3*a + 1],
                positions[3*a + 2], positions[3*b], positions[3*b + 1],
                positions[3*b + 2]);
        }
    }
}
