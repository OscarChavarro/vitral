#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixState.h"

void OpenGL1MatrixState::load(unsigned int matrixMode, const Matrix4x4d& matrix)
{
    double* m = matrix.exportToDoubleArrayColumnOrder();
    glMatrixMode(matrixMode);
    glLoadMatrixd(m);
    delete[] m;
}

void OpenGL1MatrixState::push(const Matrix4x4d& modelViewProjection)
{
    push(modelViewProjection, Matrix4x4d::identityMatrix());
}

void OpenGL1MatrixState::push(const Matrix4x4d& projection, const Matrix4x4d& modelView)
{
    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    load(GL_PROJECTION, projection);
    load(GL_MODELVIEW, modelView);
}

void OpenGL1MatrixState::pop()
{
    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

Matrix4x4d OpenGL1MatrixState::depthBias(double depthBiasNdc)
{
    return Matrix4x4d::identityMatrix().withVal(2, 3, depthBiasNdc);
}
