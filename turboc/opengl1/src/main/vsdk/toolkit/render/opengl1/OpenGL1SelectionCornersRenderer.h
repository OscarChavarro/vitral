#ifndef __OPN_GL_1_SLCTN_CRNRS_RNDRR__
#define __OPN_GL_1_SLCTN_CRNRS_RNDRR__
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class Camera; class Geometry;
class OpenGL1SelectionCornersRenderer {
public:
    static void draw(Geometry* geometry, Camera* camera, const Matrix4x4d& local = Matrix4x4d::identityMatrix());
    static void draw(const double* minmax, Camera* camera, const Matrix4x4d& local = Matrix4x4d::identityMatrix());
};
#endif
