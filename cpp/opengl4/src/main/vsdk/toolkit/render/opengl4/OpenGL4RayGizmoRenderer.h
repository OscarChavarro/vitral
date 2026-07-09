#ifndef __OPEN_GL_4_RAY_GIZMO_RENDERER__
#define __OPEN_GL_4_RAY_GIZMO_RENDERER__

class Camera;
class Light;
class RayGizmo;

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"

class OpenGL4RayGizmoRenderer {
public:
    static void draw(RayGizmo* gizmo, Camera* camera, const java::ArrayList<Light*>& lights);
    static void dispose();

private:
    static void addLine(java::ArrayList<float>& positions, java::ArrayList<float>& colors,
        const Vector3Dd& a, const Vector3Dd& b, const ColorRgb& color);
    static void addDot(java::ArrayList<float>& positions, java::ArrayList<float>& colors,
        const Vector3Dd& center, const ColorRgb& color, double radius);
};

#endif
