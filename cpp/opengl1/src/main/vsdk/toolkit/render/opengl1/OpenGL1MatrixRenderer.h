#ifndef __OPEN_GL_1_MATRIX_RENDERER__
#define __OPEN_GL_1_MATRIX_RENDERER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class OpenGL1MatrixRenderer {
public:
    static void draw(const float* mvpColumnMajor16, const Matrix4x4d& A);
    static void release();

private:
    OpenGL1MatrixRenderer();
    ~OpenGL1MatrixRenderer();
};

#endif
