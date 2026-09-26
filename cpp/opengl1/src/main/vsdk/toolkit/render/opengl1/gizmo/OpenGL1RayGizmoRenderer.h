#ifndef VITRAL_OPEN_GL_1_RAY_GIZMO_RENDERER_H
#define VITRAL_OPEN_GL_1_RAY_GIZMO_RENDERER_H

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;
class Light;
class RayGizmo;
class RendererConfiguration;
class SimpleMaterial;

class OpenGL1RayGizmoRenderer {
public:
    static void draw(RayGizmo* gizmo, Camera* camera, const java::ArrayList<Light*>& lights);
    static void dispose();

private:
    static void drawIndicator(
        double rollAngleRadians,
        const Matrix4x4d& arrowModelMatrix,
        const Matrix4x4d& projection,
        const Camera* camera,
        const java::ArrayList<Light*>& lights,
        const RendererConfiguration* quality);
    static RendererConfiguration buildSurfaceQuality();
    static SimpleMaterial indicatorMaterial();
    static void computeNormals(float normals[9]);
};

#endif
