#include <stddef.h>

#include "vsdk/toolkit/common/VSDK.h"
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

const double GeometryCalligraphicExporter::INFINITE_PLANE_SIZE = 20.0;

GeometryCalligraphicExporter::GeometryCalligraphicExporter()
{
}

/**
C++ port note: the meshes exported on the way are deleted here.
*/
Calligraphic3DBuffer* GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
    Geometry* geometry)
{
    Calligraphic3DBuffer* lines;

    if ( geometry == NULL ) {
        return NULL;
    }
    if ( geometry->instanceOf(Curve::CLASS) ) {
        return ((Curve*)geometry)->exportToCalligraphic3DBuffer();
    }
    if ( geometry->instanceOf(ParametricBiCubicPatch::CLASS) ) {
        QuadMesh* mesh = ((ParametricBiCubicPatch*)geometry)->exportToQuadMesh();
        lines = exportEdges(*mesh);
        delete mesh;
        return lines;
    }
    if ( geometry->instanceOf(FunctionalExplicitSurface::CLASS) ) {
        QuadMesh* mesh = ((FunctionalExplicitSurface*)geometry)->exportToQuadMesh();
        if ( mesh == NULL ) {
            return NULL;
        }
        lines = exportEdges(*mesh);
        delete mesh;
        return lines;
    }
    if ( geometry->instanceOf(SubdividedBezierPatch::CLASS) ) {
        QuadMesh* mesh = BezierPatchTessellator::tessellateQuads(
            *((SubdividedBezierPatch*)geometry), BEZIER_PATCH_STEPS,
            BEZIER_PATCH_STEPS);
        if ( mesh == NULL ) {
            return NULL;
        }
        lines = exportEdges(*mesh);
        delete mesh;
        return lines;
    }
    if ( geometry->instanceOf(InfinitePlane::CLASS) ) {
        return exportToCalligraphic3DBuffer(*((InfinitePlane*)geometry),
            Vector3Dd(0, 0, 0));
    }
    if ( geometry->instanceOf(QuadMesh::CLASS) ) {
        return exportEdges(*((QuadMesh*)geometry));
    }
    if ( geometry->instanceOf(TriangleStripMesh::CLASS) ) {
        TriangleMesh* mesh = ((TriangleStripMesh*)geometry)->exportToTriangleMesh();
        if ( mesh == NULL ) {
            return NULL;
        }
        lines = exportEdges(*mesh);
        delete mesh;
        return lines;
    }

    TriangleMeshGroup meshes;
    if ( !GeometryTriangulator::exportToTriangleMeshGroup(geometry, meshes) ) {
        return NULL;
    }
    lines = new Calligraphic3DBuffer();
    long i;
    for ( i = 0; i < meshes.getMeshes().size(); i++ ) {
        TriangleMesh& mesh = meshes.getMeshes()[i];
        addEdges(lines, mesh.getVertexPositions(), mesh.getTriangleIndexes(),
            mesh.getNumTriangles(), 3);
    }
    return lines;
}

Calligraphic3DBuffer* GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
    const InfinitePlane& plane, const Vector3Dd& nearPoint)
{
    QuadMesh* mesh = plane.exportToQuadMesh(nearPoint, INFINITE_PLANE_SIZE,
        INFINITE_PLANE_TILES);
    if ( mesh == NULL ) {
        return NULL;
    }
    Calligraphic3DBuffer* lines = exportEdges(*mesh);
    delete mesh;
    return lines;
}

Calligraphic3DBuffer* GeometryCalligraphicExporter::exportToCalligraphic3DBuffer(
    const Md2Mesh& model, int frame)
{
    TriangleMesh* mesh = model.exportToTriangleMesh(frame);
    if ( mesh == NULL ) {
        return NULL;
    }
    Calligraphic3DBuffer* lines = exportEdges(*mesh);
    delete mesh;
    return lines;
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

/**
C++ port note: the arrays are never null (an empty one has no faces), and
the set of added edges is a map to a dummy value.
*/
void GeometryCalligraphicExporter::addEdges(Calligraphic3DBuffer* lines,
    const ArrayList<double>& positions, const ArrayList<int>& faces,
    int numFaces, int sides)
{
    long numVertices = positions.size() / 3;
    HashMap<long, int> added;
    long f;
    int k;

    for ( f = 0; f < numFaces && sides*f + sides <= faces.size(); f++ ) {
        for ( k = 0; k < sides; k++ ) {
            int a = faces[sides*f + k];
            int b = faces[sides*f + (k + 1) % sides];
            if ( a == b || a < 0 || b < 0 || a >= numVertices ||
                 b >= numVertices ) {
                continue;
            }
            long key = Math::min((long)a, (long)b) * numVertices +
                Math::max((long)a, (long)b);
            if ( added.containsKey(key) ) {
                continue;
            }
            added.put(key, 1);
            lines->add3DLine(positions[3L*a], positions[3L*a + 1],
                positions[3L*a + 2], positions[3L*b], positions[3L*b + 1],
                positions[3L*b + 2]);
        }
    }
}
