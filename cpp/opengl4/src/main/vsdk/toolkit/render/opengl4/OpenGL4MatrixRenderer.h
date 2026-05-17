#ifndef OPENGL4MATRIXRENDERER_H
#define OPENGL4MATRIXRENDERER_H

#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

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
    static std::string readShaderFile(const std::string& filename);
    static unsigned int compileShader(const std::string& source, int type);
    static void linkProgram(unsigned int vertexShader, unsigned int fragmentShader);

    OpenGL4MatrixRenderer();
    ~OpenGL4MatrixRenderer();
};

}}}}

#endif // OPENGL4MATRIXRENDERER_H
