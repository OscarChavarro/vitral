#include <stddef.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "java/util/HashMap.h"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/volume/OpenGL1VolumeRenderer.h"

/// Boundary representations of the volumes drawn, by identity
static HashMap<Volume*, PolyhedralBoundedSolid*> exportedSolids;
/// C++ port note: the exported solids, to delete them (`HashMap` has no
/// iteration)
static ArrayList<PolyhedralBoundedSolid*> ownedSolids;
static long exportedCount = 0;

/**
C++ port note: as there are no textures for boundary representations, none
is given.
*/
BOOLEAN OpenGL1VolumeRenderer::draw(Volume* volume, Camera* camera,
    const ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, const Matrix4x4d& localTransform)
{
    PolyhedralBoundedSolid* solid = exportedSolid(volume);
    if ( solid == NULL ) {
        return FALSE;
    }
    OpenGL1GeometryRenderer::draw(solid, camera, lights, material, quality,
        NULL, NULL, localTransform);
    return TRUE;
}

PolyhedralBoundedSolid* OpenGL1VolumeRenderer::exportedSolid(Volume* volume)
{
    if ( volume->instanceOf(PolyhedralBoundedSolid::CLASS) ) {
        return (PolyhedralBoundedSolid*)volume;
    }
    PolyhedralBoundedSolid** known = exportedSolids.get(volume);
    if ( known != NULL ) {
        return *known;
    }
    PolyhedralBoundedSolid* solid = volume->exportToPolyhedralBoundedSolid();
    if ( exportedCount >= MAX_CACHED_SOLIDS ) {
        clearExportedSolids();
    }
    // A null export is kept too, so it is not tried again on each frame
    exportedSolids.put(volume, solid);
    exportedCount++;
    if ( solid != NULL ) {
        ownedSolids.add(solid);
    }
    return solid;
}

void OpenGL1VolumeRenderer::clearExportedSolids()
{
    long i;

    for ( i = 0; i < ownedSolids.size(); i++ ) {
        delete ownedSolids[i];
    }
    ownedSolids.clear();
    exportedSolids.clear();
    exportedCount = 0;
}

void OpenGL1VolumeRenderer::dispose()
{
    clearExportedSolids();
}
