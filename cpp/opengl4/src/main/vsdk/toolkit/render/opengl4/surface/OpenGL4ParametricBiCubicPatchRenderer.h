#ifndef __OPEN_GL_4_PARAMETRIC_BI_CUBIC_PATCH_RENDERER__
#define __OPEN_GL_4_PARAMETRIC_BI_CUBIC_PATCH_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MeshRenderer.h"

class Camera;
class Light;
class OpenGL4MeshBuilder;
class ParametricBiCubicPatch;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class Vector3Dd;

/**
Renders a `ParametricBiCubicPatch` with the GL4 pipeline: the patch is
sampled in a grid of its approximation steps, visible from both sides, and
the mesh is cached by `OpenGL4GeometryRenderer`.

C++ counterpart of Java's `Jogl4ParametricBiCubicPatchRenderer`.
*/
class OpenGL4ParametricBiCubicPatchRenderer {
public:
    /**
    Draws the patch, with the passes selected by the configuration (see
    `OpenGL4MeshRenderer`).

    @param patch patch to draw
    @param camera camera that views the patch
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the patch
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from patch space to world space
    */
    static void draw(ParametricBiCubicPatch* patch, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param patch a patch
    @return a string that identifies the mesh of the patch
    */
    static java::String meshKey(ParametricBiCubicPatch& patch);

    /**
    @param patch patch to tessellate
    @return a new mesh (owned by the caller), or null if the patch can not
    be tessellated
    */
    static OpenGL4MeshRenderer::Mesh* buildMesh(ParametricBiCubicPatch& patch);

private:
    OpenGL4ParametricBiCubicPatchRenderer();

    /**
    Adds the cell between the grid points (i, j) and (i + 1, j + 1). Normals
    that the patch can not give (degenerate points, i.e. a corner with null
    tangents) are replaced by the normal of the cell, and the cell is wound
    so its front side is the one its normals point to.
    (C++ port note: the grids are stored by rows, with `steps + 1` columns.)
    */
    static void addCell(OpenGL4MeshBuilder& builder,
        const java::ArrayList<Vector3Dd>& points,
        const java::ArrayList<Vector3Dd>& normals, int i, int j, int steps);
    static Vector3Dd validNormal(const Vector3Dd& normal,
                                 const Vector3Dd& fallback);
};

#endif
