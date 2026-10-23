#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.h"
BOOLEAN GeometryTriangulator::exportToTriangleMeshGroup(Geometry* geometry, TriangleMeshGroup& outGroup)
{
    if ( geometry == 0 ) {
        return FALSE;
    }

    if ( geometry->instanceOf(TriangleMeshGroup::CLASS) ) {
        TriangleMeshGroup* group = (TriangleMeshGroup*)geometry;
        outGroup = *group;
        return TRUE;
    }

    if ( geometry->instanceOf(TriangleMesh::CLASS) ) {
        TriangleMesh* mesh = (TriangleMesh*)geometry;
        outGroup.getMeshes().clear();
        outGroup.addMesh(*mesh);
        return TRUE;
    }

    if ( geometry->instanceOf(QuadMesh::CLASS) ) {
        QuadMesh* quadMesh = (QuadMesh*)geometry;
        TriangleMeshGroup* tmp = quadMesh->exportToTriangleMeshGroup();
        if ( tmp == 0 ) return FALSE;
        outGroup = *tmp;
        delete tmp;
        return TRUE;
    }

    if ( geometry->instanceOf(FunctionalExplicitSurface::CLASS) ) {
        FunctionalExplicitSurface* surface = (FunctionalExplicitSurface*)geometry;
        TriangleMesh* mesh = surface->getInternalTriangleMesh();
        if ( mesh == 0 ) return FALSE;
        outGroup.getMeshes().clear();
        outGroup.addMesh(*mesh);
        return TRUE;
    }

    return FALSE;
}
