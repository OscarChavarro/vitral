#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1MatrixState.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.h"

void OpenGL1ColoredPrimitiveRenderer::draw(
    const Matrix4x4d& mvp, unsigned int primitiveType,
    const java::ArrayList<float>& positions, const java::ArrayList<float>& colors)
{
    if ( positions.size() == 0 || positions.size() / 3 != colors.size() / 4 ) return;
    glPushAttrib(GL_ENABLE_BIT);
    glDisable(GL_LIGHTING);
    glDisable(GL_TEXTURE_2D);
    OpenGL1MatrixState::push(mvp);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions.data());
    glColorPointer(4, GL_FLOAT, 0, colors.data());
    glDrawArrays(primitiveType, 0, (GLsizei)(positions.size()/3));
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);
    OpenGL1MatrixState::pop();
    glPopAttrib();
}

void OpenGL1ColoredPrimitiveRenderer::release()
{
}
