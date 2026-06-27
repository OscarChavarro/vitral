#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4LIGHTRENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4LIGHTRENDERER_H__

class Light;
class Camera;

#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "java/util/ArrayList.h"

class OpenGL4LightRenderer {
public:
    static void draw(const Light* light);
    static void draw(const Light* light, Camera* camera);
    static void draw(const Light* light, Camera* camera, LightGizmoStyle lightGizmoStyle);

    static double getScale();
    static void setScale(double newScale);

    static void dispose();

private:
    static double scale_;
    static unsigned int vao_;
    static unsigned int vboPositions_;
    static unsigned int vboColors_;
    static unsigned int program_;

    static bool initIfNeeded();
    static unsigned int compileShader(unsigned int type, const char* source);
    static double calculateHalfAxisLength(const Light* light, Camera* camera, int viewportWidth, int viewportHeight);
    static Vector3Dd mapPatternPointToWorld(
        const Vector3Dd& point,
        const Vector3Dd& center,
        const Vector3Dd& right,
        const Vector3Dd& up,
        double worldSize);
    static void drawLines(const Matrix4x4d& mvp, const java::ArrayList<float>& positions, const java::ArrayList<float>& colors);
    static void drawCross(const Light* light, Camera* camera);
    static void drawOmniBillboard(const Light* light, Camera* camera);
};

#endif
