#ifndef __OPEN_GL_1_SUBDIVIDED_BEZIER_PATCH_RENDERER__
#define __OPEN_GL_1_SUBDIVIDED_BEZIER_PATCH_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;
class SubdividedBezierPatch;

/**
Renders a `SubdividedBezierPatch` with the OpenGL 1.2 pipeline: its surface is
tessellated by `BezierPatchTessellator` into a grid of `TESSELLATION_STEPS`
x `TESSELLATION_STEPS` steps of its parameters, visible from both sides, and
the mesh is cached by `OpenGL1GeometryRenderer`, identified by the patch.

OpenGL 1.2 (fixed function) counterpart of Java's `Jogl4SubdividedBezierPatchRenderer`, mirroring
`OpenGL1SubdividedBezierPatchRenderer`.
*/
class OpenGL1SubdividedBezierPatchRenderer {
public:
    /**
    Draws the patch, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param patch patch to draw
    @param camera camera that views the patch
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the patch
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from patch space to world space
    */
    static void draw(SubdividedBezierPatch* patch, Camera* camera,
        const java::ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param patch a patch
    @return a string that identifies the mesh of the patch
    */
    static java::String meshKey(const SubdividedBezierPatch& patch);

    /**
    @param patch patch to tessellate
    @return a new mesh (owned by the caller), or null if the patch can not
    be tessellated
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const SubdividedBezierPatch& patch);

    /// Parameter steps of the tessellated patch along u and v
    static const int TESSELLATION_STEPS = 24;

private:
    OpenGL1SubdividedBezierPatchRenderer();
};

#endif
