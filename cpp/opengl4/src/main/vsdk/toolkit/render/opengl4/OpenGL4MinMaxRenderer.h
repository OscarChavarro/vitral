#ifndef __OPEN_GL_4_MIN_MAX_RENDERER__
#define __OPEN_GL_4_MIN_MAX_RENDERER__
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class Camera; class Geometry;
class OpenGL4MinMaxRenderer {
public:
    static void draw(Geometry* geometry, Camera* camera, const Matrix4x4d& local = Matrix4x4d::identityMatrix());
    static void draw(const double* minmax, Camera* camera, const Matrix4x4d& local = Matrix4x4d::identityMatrix());
    static void dispose();
};
#endif
