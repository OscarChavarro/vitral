#ifndef __OPEN_GL_1_RENDERER_CONFIGURATION_STATE_SELECTOR__
#define __OPEN_GL_1_RENDERER_CONFIGURATION_STATE_SELECTOR__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class Light;
class RendererConfiguration;
class SimpleMaterial;

/**
Configures the OpenGL 1.2 fixed function pipeline to present the shading type
of a `RendererConfiguration`, taking the place of the GLSL programs of
`OpenGL4RendererConfigurationShaderSelector`:
  - constant (no light): lighting disabled, the diffuse color of the material
    (or the vertex colors) modulated by the texture
  - flat: fixed function lighting with `GL_FLAT` shading
  - Gouraud: fixed function lighting with `GL_SMOOTH` shading

Per pixel shading does not exist in OpenGL 1.2: Phong and Cook-Torrance
shading types are presented as Gouraud, and bump maps are ignored (both
reported once on the standard error).

Lights are presented as the GLSL programs do: every light is a point light at
its position, with its emission as diffuse and specular colors, without
attenuation, and the ambient color of the material is added once.

Usage:
<pre>
    OpenGL1RendererConfigurationStateSelector::activateState(camera,
        camera->calculateProjectionMatrix(), local, lights, material,
        quality, textureId);
    ... draw the vertices, with normals and texture coordinates ...
    OpenGL1RendererConfigurationStateSelector::deactivateState();
</pre>
*/
class OpenGL1RendererConfigurationStateSelector {
public:
    /// Number of lights of the fixed function pipeline (GL_LIGHT0..7)
    static const int MAX_LIGHTS = 8;

    /**
    Saves the OpenGL state, and sets the matrices, lights, material and
    texture that present a surface.
    @param camera camera that views the surface, whose transformation defines
    the eye space where lighting is computed; null to compute it in world
    space
    @param viewProjection the whole projection of the camera, as given by
    `Camera::calculateProjectionMatrix`
    @param local transformation from the object to world space
    @param lights lights of the scene (at most `MAX_LIGHTS` are used)
    @param material material of the surface
    @param quality configuration to present, or null for a constant color
    @param textureId texture of the surface, or 0 if it has none
    */
    static void activateState(const Camera* camera,
                              const Matrix4x4d& viewProjection,
                              const Matrix4x4d& local,
                              const java::ArrayList<Light*>& lights,
                              const SimpleMaterial& material,
                              const RendererConfiguration* quality,
                              int textureId);

    /**
    Saves the OpenGL state, and sets a constant color without lighting
    (the counterpart of the constant GLSL program).
    */
    static void activateConstantState(const Matrix4x4d& modelViewProjection,
                                      float diffuseR, float diffuseG,
                                      float diffuseB);

    /**
    Restores the state saved by the last `activateState` or
    `activateConstantState`.
    */
    static void deactivateState();

    /**
    @return true if the configuration asks for per pixel effects not offered
    by OpenGL 1.2 (Phong or Cook-Torrance shading, or bump mapping); they are
    reported once on the standard error.
    */
    static bool reportUnsupportedFeatures(const RendererConfiguration* quality);

    static void dispose();

private:
    static bool tooManyLightsReported;
    static bool phongReported;
    static bool cookTorranceReported;
    static bool bumpMapReported;

    static void pushState();
    static void activateLights(const java::ArrayList<Light*>& lights);
    static void activateMaterial(const SimpleMaterial& material);

    OpenGL1RendererConfigurationStateSelector() {}
};

#endif
