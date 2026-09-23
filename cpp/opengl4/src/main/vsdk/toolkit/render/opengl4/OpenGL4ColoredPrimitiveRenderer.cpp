#include <cstdio>
#include <GL/glew.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"

unsigned int OpenGL4ColoredPrimitiveRenderer::vao = 0;
unsigned int OpenGL4ColoredPrimitiveRenderer::positionVbo = 0;
unsigned int OpenGL4ColoredPrimitiveRenderer::colorVbo = 0;
unsigned int OpenGL4ColoredPrimitiveRenderer::program = 0;

namespace {
GLuint compile(GLenum type, const char* source)
{
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, 0);
    glCompileShader(shader);
    GLint ok = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) { glDeleteShader(shader); return 0; }
    return shader;
}
}

bool OpenGL4ColoredPrimitiveRenderer::initializeIfNeeded()
{
    if ( program != 0 ) return true;
    const char* vs =
        "#version 410 core\n"
        "layout(location=0) in vec3 position;\n"
        "layout(location=1) in vec4 color;\n"
        "uniform mat4 mvp; out vec4 vertexColor;\n"
        "void main(){ gl_Position=mvp*vec4(position,1); vertexColor=color; }\n";
    const char* fs =
        "#version 410 core\n"
        "in vec4 vertexColor; out vec4 fragmentColor;\n"
        "void main(){ fragmentColor=vertexColor; }\n";
    GLuint v = compile(GL_VERTEX_SHADER, vs), f = compile(GL_FRAGMENT_SHADER, fs);
    if ( v == 0 || f == 0 ) return false;
    program = glCreateProgram();
    glAttachShader(program, v); glAttachShader(program, f); glLinkProgram(program);
    glDeleteShader(v); glDeleteShader(f);
    GLint ok = GL_FALSE; glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if ( !ok ) { glDeleteProgram(program); program = 0; return false; }
    glGenVertexArrays(1, &vao); glGenBuffers(1, &positionVbo); glGenBuffers(1, &colorVbo);
    return true;
}

void OpenGL4ColoredPrimitiveRenderer::draw(
    const Matrix4x4d& mvp, unsigned int primitiveType,
    const java::ArrayList<float>& positions, const java::ArrayList<float>& colors)
{
    if ( positions.size() == 0 || positions.size() / 3 != colors.size() / 4 || !initializeIfNeeded() ) return;
    float* matrix = mvp.exportToFloatArrayColumnOrder();
    glUseProgram(program);
    glUniformMatrix4fv(glGetUniformLocation(program, "mvp"), 1, GL_FALSE, matrix);
    delete[] matrix;
    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, positionVbo);
    glBufferData(GL_ARRAY_BUFFER, positions.size()*sizeof(float), positions.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(0); glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, colorVbo);
    glBufferData(GL_ARRAY_BUFFER, colors.size()*sizeof(float), colors.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(1); glVertexAttribPointer(1, 4, GL_FLOAT, GL_FALSE, 0, 0);
    glDrawArrays(primitiveType, 0, (GLsizei)(positions.size()/3));
    glDisableVertexAttribArray(0); glDisableVertexAttribArray(1);
    glBindVertexArray(0); glUseProgram(0);
}

void OpenGL4ColoredPrimitiveRenderer::release()
{
    if ( positionVbo ) glDeleteBuffers(1, &positionVbo);
    if ( colorVbo ) glDeleteBuffers(1, &colorVbo);
    if ( vao ) glDeleteVertexArrays(1, &vao);
    if ( program ) glDeleteProgram(program);
    vao = positionVbo = colorVbo = program = 0;
}
