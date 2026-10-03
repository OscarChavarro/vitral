#ifndef __OPEN_GL_4_VOXEL_VOLUME_RENDERER__
#define __OPEN_GL_4_VOXEL_VOLUME_RENDERER__

#include <string>

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class Light;
class OpenGL4MeshBuilder;
class RendererConfiguration;
class RGBImageUncompressed;
class SimpleMaterial;
class VoxelVolume;

/**
Renders a `VoxelVolume` with the GL4 pipeline, as the binary cubes of
`Jogl2VoxelVolumeRenderer`: every filled voxel (whose value reaches the
threshold of the volume, see `VoxelVolume::isFilled`) is shown as a cube,
inside the volume space (the cube from <-1, -1, -1> to <1, 1, 1>). Instead
of drawing one cube per voxel, the volume is tessellated once into a mesh
with only the faces of the voxels that border an empty voxel (or the limits
of the volume), so the result looks the same with a fraction of the
triangles. The mesh is cached by `OpenGL4GeometryRenderer`, identified by
the volume and its threshold: as triangle meshes, volumes are not expected
to be edited while shown.

C++ counterpart of Java's `Jogl4VoxelVolumeRenderer`. C++ port note:
`meshKey` and `buildMesh` are package private in Java, and public here.
*/
class OpenGL4VoxelVolumeRenderer {
public:
    /**
    Draws the volume, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

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
    static std::string meshKey(const VoxelVolume& volume);

    /**
    @param volume volume to tessellate
    @return a new mesh (owned by the caller) of the boundary faces of the
    filled voxels, or null if the volume has no filled voxels
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(const VoxelVolume& volume);

private:
    OpenGL4VoxelVolumeRenderer();

    static void addVoxelFace(OpenGL4MeshBuilder& builder, const int cell[3],
        const double step[3], int axis, bool positive);
};

#endif
