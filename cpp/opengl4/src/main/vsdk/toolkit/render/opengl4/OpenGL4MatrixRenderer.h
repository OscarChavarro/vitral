#ifndef __OPENGL4MATRIXRENDERER__
#define __OPENGL4MATRIXRENDERER__

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
class OpenGL4MatrixRenderer {
public:
    static void draw(const float* mvpColumnMajor16, const Matrix4x4d& A);
    static void release();

private:
    static unsigned int VAO;
    static unsigned int VBO_positions;
    static unsigned int VBO_colors;
    static unsigned int shaderProgram;
    static bool initialized;

    static void initializeIfNeeded();
    static unsigned int compileShaders();
    static java::String readShaderFile(const java::String& filename);
    static unsigned int compileShader(const java::String& source, int type);
    static void linkProgram(unsigned int vertexShader, unsigned int fragmentShader);

    OpenGL4MatrixRenderer();
    ~OpenGL4MatrixRenderer();
};

#endif
