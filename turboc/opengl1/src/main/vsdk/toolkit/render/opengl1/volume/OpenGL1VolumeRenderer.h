#ifndef __OPEN_GL_1_VOLUME_RENDERER__
#define __OPEN_GL_1_VOLUME_RENDERER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class Camera;
class Light;
class PolyhedralBoundedSolid;
class RendererConfiguration;
class SimpleMaterial;
class Volume;

/**
Draws with OpenGL 1.2 the volumes without a renderer of their own, through
their boundary representation (`Volume::exportToPolyhedralBoundedSolid`),
drawn as a polyhedral bounded solid (as `Jogl4VolumeRenderer` of the Java
OpenGL 4 port). The exported solids are kept, by identity of the volume, so
the export is made once.

C++ port note: the volumes are identified by their address, so a volume
deleted and replaced by a new one at the same address would be drawn with
the solid exported from the old one until `dispose` is called.
*/
class OpenGL1VolumeRenderer {
private:
    enum { MAX_CACHED_SOLIDS = 256 };

    OpenGL1VolumeRenderer();

    static void clearExportedSolids();

public:
    /**
    @return true if the volume was drawn (it has a boundary representation)
    */
    static BOOLEAN draw(Volume* volume, Camera* camera,
        const ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, const Matrix4x4d& localTransform);

    /**
    @return the boundary representation of the volume (the volume itself for
    a polyhedral bounded solid), owned by this renderer, or null if it has
    none
    */
    static PolyhedralBoundedSolid* exportedSolid(Volume* volume);

    /**
    Deletes the exported solids.
    */
    static void dispose();
};

#endif
