#ifndef __WRITER_COMMON__
#define __WRITER_COMMON__

#include <stddef.h>
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"

/**
C++ port helper shared by the scene writers: the triangle mesh to export
for a geometry, or null if it has none.
*/
inline TriangleMesh* writerMeshOf(Geometry* g)
{
    FunctionalExplicitSurface* surface =
        (g != NULL && g->instanceOf(FunctionalExplicitSurface::CLASS) ? (FunctionalExplicitSurface*)g : NULL);
    if ( surface != NULL ) {
        return surface->getInternalTriangleMesh();
    }
    return (g != NULL && g->instanceOf(TriangleMesh::CLASS) ? (TriangleMesh*)g : NULL);
}

#endif
