#ifndef __OPEN_GL_1_VOXEL_VOLUME_RENDERER__
#define __OPEN_GL_1_VOXEL_VOLUME_RENDERER__

#include "java/lang/String.h"

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class OpenGL1MeshBuilder;
class RendererConfiguration;
class RGBImageUncompressed;
class SimpleMaterial;
class VoxelVolume;

/**
Renders a `VoxelVolume` with the OpenGL 1.2 pipeline, as the binary cubes of
`Jogl2VoxelVolumeRenderer`: every filled voxel (whose value reaches the
threshold of the volume, see `VoxelVolume::isFilled`) is shown as a cube,
inside the volume space (the cube from <-1, -1, -1> to <1, 1, 1>). Instead
of drawing one cube per voxel, the volume is tessellated once into a mesh
with only the faces of the voxels that border an empty voxel (or the limits
of the volume), so the result looks the same with a fraction of the
triangles. The mesh is cached by `OpenGL1GeometryRenderer`, identified by
the volume and its threshold: as triangle meshes, volumes are not expected
to be edited while shown.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4VoxelVolumeRenderer`, mirroring
`OpenGL1VoxelVolumeRenderer`. C++ port note:
`meshKey` and `buildMesh` are package private in Java, and public here.
*/
class OpenGL1VoxelVolumeRenderer {
public:
    /**
    Draws the volume, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param volume volume to draw
    @param camera camera that views the volume
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the volume
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from volume space to world space
    */
    static void draw(VoxelVolume* volume, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param volume a volume
    @return a string that identifies the mesh of the volume with its current
    threshold
    */
    static java::String meshKey(const VoxelVolume& volume);

    /**
    @param volume volume to tessellate
    @return a new mesh (owned by the caller) of the boundary faces of the
    filled voxels, or null if the volume has no filled voxels
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const VoxelVolume& volume);

private:
    OpenGL1VoxelVolumeRenderer();

    static void addVoxelFace(OpenGL1MeshBuilder& builder, const int cell[3],
        const double step[3], int axis, bool positive);
};

#endif
