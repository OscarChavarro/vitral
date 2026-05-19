#ifndef __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4LIGHTRENDERER_H__
#define __VSDK_TOOLKIT_RENDER_OPENGL4_OPENGL4LIGHTRENDERER_H__

class Light;
class Camera;

#include "vsdk/toolkit/gui/LightGizmoStyle.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include <vector>

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

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
    static void drawLines(const Matrix4x4d& mvp, const std::vector<float>& positions, const std::vector<float>& colors);
    static void drawCross(const Light* light, Camera* camera);
    static void drawOmniBillboard(const Light* light, Camera* camera);
};

}}}}

#endif
