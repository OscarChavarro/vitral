#ifndef __OPEN_GL_1_LIGHT_RENDERER__
#define __OPEN_GL_1_LIGHT_RENDERER__

class Light;
class Camera;

#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/util/ArrayList.h"

class OpenGL1LightRenderer {
public:
    static void draw(const Light* light);
    static void draw(const Light* light, Camera* camera);
    static void draw(const Light* light, Camera* camera, LightGizmoStyle lightGizmoStyle);
    static void draw(const Light* light, Camera* camera,
                     LightGizmoStyle lightGizmoStyle, bool selected);

    static double getScale();
    static void setScale(double newScale);

    static void dispose();

private:
    static double scale;

    static double calculateHalfAxisLength(const Light* light, Camera* camera, int viewportWidth, int viewportHeight);
    static Vector3Dd mapPatternPointToWorld(
        const Vector3Dd& point,
        const Vector3Dd& center,
        const Vector3Dd& right,
        const Vector3Dd& up,
        double worldSize);
    static void drawLines(const Matrix4x4d& mvp,
                          const java::ArrayList<float>& positions,
                          const java::ArrayList<float>& colors,
                          float lineWidth);
    static void drawCross(const Light* light, Camera* camera, bool selected);
    static void drawOmniBillboard(const Light* light, Camera* camera,
                                  bool selected);
};

#endif
