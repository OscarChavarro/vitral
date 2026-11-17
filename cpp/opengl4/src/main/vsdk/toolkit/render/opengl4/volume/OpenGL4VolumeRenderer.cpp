#include "java/util/ArrayList.txx"
#include "java/util/TreeMap.txx"
#include "vsdk/toolkit/environment/geometry/volume/Volume.h"
#include "vsdk/toolkit/environment/geometry/volume/polyhedralBoundedSolid/PolyhedralBoundedSolid.h"
#include "vsdk/toolkit/render/opengl4/volume/OpenGL4VolumeRenderer.h"
#include "vsdk/toolkit/render/opengl4/volume/polyhedralBoundedSolid/OpenGL4PolyhedralBoundedSolidRenderer.h"

namespace {

const long MAX_CACHED_SOLIDS = 256;

/// Boundary representations of the volumes drawn, by identity
java::TreeMap<Volume*, PolyhedralBoundedSolid*> exportedSolids;

void clearExportedSolids()
{
    for ( long i = 0; i < exportedSolids.size(); i++ ) {
        delete exportedSolids.getValue(i);
    }
    exportedSolids.clear();
}

}

bool OpenGL4VolumeRenderer::draw(Volume* volume, Camera* camera,
    const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
    const RendererConfiguration* quality, const Matrix4x4d& localTransform)
{
    PolyhedralBoundedSolid* solid = exportedSolid(volume);
    if ( solid == nullptr ) {
        return false;
    }
    OpenGL4PolyhedralBoundedSolidRenderer::draw(solid, camera, lights,
        material, quality, localTransform);
    return true;
}

PolyhedralBoundedSolid* OpenGL4VolumeRenderer::exportedSolid(Volume* volume)
{
    PolyhedralBoundedSolid* brep = dynamic_cast<PolyhedralBoundedSolid*>(volume);
    if ( brep != nullptr ) {
        return brep;
    }
    if ( exportedSolids.containsKey(volume) ) {
        return *exportedSolids.get(volume);
    }
    PolyhedralBoundedSolid* solid = volume->exportToPolyhedralBoundedSolid();
    if ( exportedSolids.size() >= MAX_CACHED_SOLIDS ) {
        clearExportedSolids();
    }
    // A null export is kept too, so it is not tried again on each frame
    exportedSolids.put(volume, solid);
    return solid;
}

void OpenGL4VolumeRenderer::dispose()
{
    clearExportedSolids();
}
