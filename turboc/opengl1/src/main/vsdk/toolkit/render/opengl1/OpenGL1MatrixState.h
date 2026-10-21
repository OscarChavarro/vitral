#ifndef __OPEN_GL_1_MATRIX_STATE__
#define __OPEN_GL_1_MATRIX_STATE__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

/**
Loads the matrices of the fixed function pipeline, the counterpart of the
`modelViewProjectionLocal` (and related) uniforms of the GLSL programs of the
OpenGL 4 port. Every `push` saves the projection and modelview matrices of
the application, which are restored by the matching `pop`:
<pre>
    OpenGL1MatrixState::push(mvp);
    ... draw vertices ...
    OpenGL1MatrixState::pop();
</pre>
*/
class OpenGL1MatrixState {
public:
    /**
    For non lit primitives: the whole model-view-projection goes to the
    projection matrix, and the modelview matrix is the identity.
    */
    static void push(const Matrix4x4d& modelViewProjection);

    /**
    For lit primitives: lighting is computed in the space the modelview
    matrix takes vertices to (the eye space of the camera).
    */
    static void push(const Matrix4x4d& projection, const Matrix4x4d& modelView);

    static void pop();

    /**
    Replaces the current matrix of the given mode (`GL_PROJECTION` or
    `GL_MODELVIEW`).
    */
    static void load(unsigned int matrixMode, const Matrix4x4d& matrix);

    /**
    @return the matrix that moves clip space z by `depthBiasNdc` normalized
    device units (the `depthBiasNdc` uniform of the OpenGL 4 line shader):
    z' = z + depthBiasNdc * w
    */
    static Matrix4x4d depthBias(double depthBiasNdc);

private:
    OpenGL1MatrixState();
};

#endif
