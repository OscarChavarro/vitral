#ifndef VITRAL_OPEN_GL_1_CAMERA_RENDERER_H
#define VITRAL_OPEN_GL_1_CAMERA_RENDERER_H

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

class Camera;

class OpenGL1CameraRenderer {
public:
    static Matrix4x4d activate(const Camera* cam);
    static Matrix4x4d activateCenter(const Camera* cam);
    static void draw(const Camera* cam);
    static void draw(const Camera* cam, const Matrix4x4d& projection);
    static void drawVolume(const Camera* cam, const Matrix4x4d& mvp);
    static void dispose();

private:
    static void addLoopRectangle(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        double x,
        double w,
        double h,
        const float* rgb);

    static void addLine(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        double x1,
        double y1,
        double z1,
        double x2,
        double y2,
        double z2,
        const float* rgb);

    static void addVertex(
        ArrayList<float>& positions,
        ArrayList<float>& colors,
        double x,
        double y,
        double z,
        const float* rgb);
};

#endif
