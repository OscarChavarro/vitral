#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/QuadMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/geometricProcessing/GeometryTriangulator.h"
bool GeometryTriangulator::exportToTriangleMeshGroup(Geometry* geometry, TriangleMeshGroup& outGroup)
{
    if ( geometry == 0 ) {
        return false;
    }

    if ( TriangleMeshGroup* group = (geometry != NULL && geometry->instanceOf(TriangleMeshGroup::CLASS) ? (TriangleMeshGroup*)geometry : NULL) ) {
        outGroup = *group;
        return true;
    }

    if ( TriangleMesh* mesh = (geometry != NULL && geometry->instanceOf(TriangleMesh::CLASS) ? (TriangleMesh*)geometry : NULL) ) {
        outGroup.getMeshes().clear();
        outGroup.addMesh(*mesh);
        return true;
    }

    if ( QuadMesh* quadMesh = (geometry != NULL && geometry->instanceOf(QuadMesh::CLASS) ? (QuadMesh*)geometry : NULL) ) {
        TriangleMeshGroup* tmp = quadMesh->exportToTriangleMeshGroup();
        if ( tmp == 0 ) return false;
        outGroup = *tmp;
        delete tmp;
        return true;
    }

    if ( FunctionalExplicitSurface* surface = (geometry != NULL && geometry->instanceOf(FunctionalExplicitSurface::CLASS) ? (FunctionalExplicitSurface*)geometry : NULL) ) {
        TriangleMesh* mesh = surface->getInternalTriangleMesh();
        if ( mesh == 0 ) return false;
        outGroup.getMeshes().clear();
        outGroup.addMesh(*mesh);
        return true;
    }

    return false;
}
