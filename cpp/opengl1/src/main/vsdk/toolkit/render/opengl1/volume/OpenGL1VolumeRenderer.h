#ifndef __OPEN_GL_1_VOLUME_RENDERER__
#define __OPEN_GL_1_VOLUME_RENDERER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class Light;
class PolyhedralBoundedSolid;
class RendererConfiguration;
class SimpleMaterial;
class Volume;

/**
Renders, as the last resort for the volumes that have no direct tessellation
in the OpenGL 1.2 pipeline, the boundary representation of a `Volume` (see
`Volume::exportToPolyhedralBoundedSolid`) with
`OpenGL1PolyhedralBoundedSolidRenderer` (without texture nor normal map). The
representation is exported once per volume, and kept while it is shown: as
triangle meshes, such volumes are not expected to be edited while shown.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4VolumeRenderer`, mirroring
`OpenGL1VolumeRenderer`. C++ port note: the exported
solids are owned by this renderer (deleted by `dispose` or when the cache
overflows), and the volumes are identified by their address, so a volume
deleted while its solid is cached must not be replaced by another one at the
same address before `dispose` is called (Java's identity map keeps the
volume alive instead).
*/
class OpenGL1VolumeRenderer {
public:
    /**
    Draws the boundary representation of the volume, if it has one.

    @param volume volume to draw
    @param camera camera that views the volume
    @param lights lights of the scene, or null for none
    @param material material of the volume
    @param quality bits of rendering configuration
    @param localTransform transformation from volume space to world space
    @return true if the volume has a boundary representation (and it was
    drawn)
    */
    static bool draw(Volume* volume, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, const Matrix4x4d& localTransform);

    /**
    @param volume a volume
    @return the boundary representation of the volume (exported once, and
    kept while the volume is shown), or null if it has none
    */
    static PolyhedralBoundedSolid* exportedSolid(Volume* volume);

    /**
    Forgets the exported boundary representations.
    */
    static void dispose();

private:
    OpenGL1VolumeRenderer();
};

#endif
