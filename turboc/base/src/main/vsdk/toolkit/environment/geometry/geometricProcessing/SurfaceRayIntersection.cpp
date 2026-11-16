#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/element/RayHit.h"
#ifndef __TURBOC__
#include "vsdk/toolkit/environment/geometry/surface/FunctionalExplicitSurface.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#endif
#include "vsdk/toolkit/environment/geometry/geometricProcessing/SurfaceRayIntersection.h"

#ifndef __TURBOC__
static BOOLEAN rayIntersectsGeometryBounds(Geometry* geometry, const Ray& inRay)
{
    // Meshes with a prepared acceleration structure keep their bounds;
    // getMinMax() walks every vertex, on every ray.
    double prepared[6];
    const TriangleMesh* mesh = geometry->instanceOf(TriangleMesh::CLASS) ?
        (const TriangleMesh*)geometry : (const TriangleMesh*)NULL;
    const TriangleMeshGroup* group = geometry->instanceOf(TriangleMeshGroup::CLASS) ?
        (const TriangleMeshGroup*)geometry : (const TriangleMeshGroup*)NULL;
    const BOOLEAN usePrepared = (mesh != 0 && mesh->getPreparedMinMax(prepared)) ||
        (group != 0 && group->getPreparedMinMax(prepared));
    double* mm = usePrepared ? prepared : geometry->getMinMax();
    Vector3Dd size(mm[3]-mm[0], mm[4]-mm[1], mm[5]-mm[2]);
    Vector3Dd center(
        (mm[3]+mm[0])/2,
        (mm[4]+mm[1])/2,
        (mm[5]+mm[2])/2
    );
    if (!usePrepared) delete [] mm;

    Box boundingVolume(size);
    Ray localRay(
        inRay.getOrigin().subtract(center),
        inRay.getDirection(),
        inRay.getT()
    );
    return boundingVolume.doIntersectionFirstHit(localRay, 0);
}
#endif

BOOLEAN SurfaceRayIntersection::doIntersectionFirstHit(Geometry* geometry, const Ray& inRay, RayHit* outHit)
{
    if ( geometry == 0 ) {
        return FALSE;
    }

#ifndef __TURBOC__
    if ( (geometry != NULL && geometry->instanceOf(TriangleMesh::CLASS)) ||
         (geometry != NULL && geometry->instanceOf(TriangleMeshGroup::CLASS)) ||
         (geometry != NULL && geometry->instanceOf(FunctionalExplicitSurface::CLASS)) ) {
        if ( !rayIntersectsGeometryBounds(geometry, inRay) ) {
            return FALSE;
        }
    }
#endif

    return geometry->doIntersectionFirstHit(inRay, outHit);
}
