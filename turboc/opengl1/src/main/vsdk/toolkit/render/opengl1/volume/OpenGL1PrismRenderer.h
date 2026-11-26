#ifndef __OGL1_PRISM_RNDR__
#define __OGL1_PRISM_RNDR__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MeshRenderer.h"

class Camera;
class Light;
class Prism;
class RGBImageUncompressed;
class RendererConfiguration;
class SimpleMaterial;

/**
Renders a `Prism` with the OpenGL 1.2 pipeline: it is tessellated by `buildMesh` into
its walls (one flat quadrilateral per edge of the base) and its two caps (the
base triangulated by `Prism::triangulateBase`, so concave bases work), and
the mesh is cached by `OpenGL1GeometryRenderer`, identified by the
parameters of the prism.
*/
class OpenGL1PrismRenderer {
public:
    /**
    Draws the prism, with the passes selected by the configuration (see
    `OpenGL1MeshRenderer`).

    @param prism prism to draw
    @param camera camera that views the prism
    @param lights lights of the scene, or null or empty to use a light at the camera
    @param material material of the prism
    @param quality bits of rendering configuration
    @param textureMap texture, or null
    @param normalMap normal (bump) map, or null
    @param localTransform transformation from prism space to world space
    */
    static void draw(Prism* prism, Camera* camera,
        const ArrayList<Light*>* lights, const SimpleMaterial* material,
        const RendererConfiguration* quality, RGBImageUncompressed* textureMap,
        RGBImageUncompressed* normalMap, const Matrix4x4d& localTransform);

    /**
    @param prism a prism
    @return a string that identifies the mesh of the prism
    */
    static String meshKey(const Prism& prism);

    /**
    @param prism prism to tessellate
    @return a new mesh (owned by the caller) of the walls and the caps of the
    prism, or null if the prism has no valid parameters
    */
    static OpenGL1MeshRenderer::Mesh* buildMesh(const Prism& prism);

private:
    OpenGL1PrismRenderer();
};

#endif
