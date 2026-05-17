#include "OpenGL4MatrixRenderer.h"

#ifdef __APPLE__
#include <OpenGL/gl3.h>
#else
#include <GL/glew.h>
#include <GL/gl.h>
#endif

#include <cstdio>
#include <fstream>
#include <sstream>
#include <cstring>

namespace vsdk { namespace toolkit { namespace render { namespace opengl4 {

unsigned int OpenGL4MatrixRenderer::VAO = 0;
unsigned int OpenGL4MatrixRenderer::VBO_positions = 0;
unsigned int OpenGL4MatrixRenderer::VBO_colors = 0;
unsigned int OpenGL4MatrixRenderer::shaderProgram = 0;
bool OpenGL4MatrixRenderer::initialized = false;

std::string OpenGL4MatrixRenderer::readShaderFile(const std::string& filename) {
    std::ifstream file(filename);
    if (!file.is_open()) {
        fprintf(stderr, "Error: Could not open shader file: %s\n", filename.c_str());
        return "";
    }
    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

unsigned int OpenGL4MatrixRenderer::compileShader(const std::string& source, int type) {
    unsigned int shader = glCreateShader(type);
    const char* src = source.c_str();
    glShaderSource(shader, 1, &src, nullptr);
    glCompileShader(shader);

    int success;
    char infoLog[512];
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    if (!success) {
        glGetShaderInfoLog(shader, 512, nullptr, infoLog);
        fprintf(stderr, "Shader compilation failed: %s\n", infoLog);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

unsigned int OpenGL4MatrixRenderer::compileShaders() {
    std::string vertexSource = readShaderFile("../../../../etc/glslShaders/lineVertexShader.glsl");
    std::string fragmentSource = readShaderFile("../../../../etc/glslShaders/linePixelShader.glsl");

    if (vertexSource.empty() || fragmentSource.empty()) {
        fprintf(stderr, "Error: Shader files not found\n");
        return 0;
    }

    unsigned int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
    unsigned int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);

    if (vertexShader == 0 || fragmentShader == 0) {
        if (vertexShader != 0) glDeleteShader(vertexShader);
        if (fragmentShader != 0) glDeleteShader(fragmentShader);
        return 0;
    }

    unsigned int program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);

    int success;
    char infoLog[512];
    glGetProgramiv(program, GL_LINK_STATUS, &success);
    if (!success) {
        glGetProgramInfoLog(program, 512, nullptr, infoLog);
        fprintf(stderr, "Shader program linking failed: %s\n", infoLog);
        glDeleteProgram(program);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return 0;
    }

    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    return program;
}

void OpenGL4MatrixRenderer::initializeIfNeeded() {
    if (initialized) {
        return;
    }

    shaderProgram = compileShaders();
    if (shaderProgram == 0) {
        fprintf(stderr, "Error: Failed to compile shader program\n");
        return;
    }

    glGenVertexArrays(1, &VAO);
    glGenBuffers(1, &VBO_positions);
    glGenBuffers(1, &VBO_colors);

    glBindVertexArray(VAO);

    float positions[18] = {
        0.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 0.0f,  0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f,  0.0f, 0.0f, 1.0f
    };

    float colors[18] = {
        1.0f, 0.0f, 0.0f,  1.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 1.0f,  0.0f, 0.0f, 1.0f
    };

    glBindBuffer(GL_ARRAY_BUFFER, VBO_positions);
    glBufferData(GL_ARRAY_BUFFER, sizeof(positions), positions, GL_DYNAMIC_DRAW);

    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), (void*)0);
    glEnableVertexAttribArray(0);

    glBindBuffer(GL_ARRAY_BUFFER, VBO_colors);
    glBufferData(GL_ARRAY_BUFFER, sizeof(colors), colors, GL_DYNAMIC_DRAW);

    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 3 * sizeof(float), (void*)0);
    glEnableVertexAttribArray(1);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    initialized = true;
}

void OpenGL4MatrixRenderer::draw(const float* mvpColumnMajor16, const Matrix4x4d& A) {
    initializeIfNeeded();

    if (!initialized || shaderProgram == 0) {
        fprintf(stderr, "Error: OpenGL4MatrixRenderer not initialized\n");
        return;
    }

    Vector3Dd x(A.get(0, 0), A.get(1, 0), A.get(2, 0));
    Vector3Dd y(A.get(0, 1), A.get(1, 1), A.get(2, 1));
    Vector3Dd z(A.get(0, 2), A.get(1, 2), A.get(2, 2));
    Vector3Dd translation(A.get(0, 3), A.get(1, 3), A.get(2, 3));

    float positions[18] = {
        (float)translation.x(), (float)translation.y(), (float)translation.z(),
        (float)(translation.x() + x.x()), (float)(translation.y() + x.y()), (float)(translation.z() + x.z()),

        (float)translation.x(), (float)translation.y(), (float)translation.z(),
        (float)(translation.x() + y.x()), (float)(translation.y() + y.y()), (float)(translation.z() + y.z()),

        (float)translation.x(), (float)translation.y(), (float)translation.z(),
        (float)(translation.x() + z.x()), (float)(translation.y() + z.y()), (float)(translation.z() + z.z())
    };

    glUseProgram(shaderProgram);

    GLint mvpLoc = glGetUniformLocation(shaderProgram, "modelViewProjectionLocal");
    glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, mvpColumnMajor16);

    GLint depthBiasLoc = glGetUniformLocation(shaderProgram, "depthBiasNdc");
    glUniform1f(depthBiasLoc, 0.0f);

    glBindVertexArray(VAO);
    glBindBuffer(GL_ARRAY_BUFFER, VBO_positions);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(positions), positions);

    glDrawArrays(GL_LINES, 0, 6);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glUseProgram(0);
}

void OpenGL4MatrixRenderer::release() {
    if (!initialized) {
        return;
    }

    if (VAO != 0) {
        glDeleteVertexArrays(1, &VAO);
        VAO = 0;
    }
    if (VBO_positions != 0) {
        glDeleteBuffers(1, &VBO_positions);
        VBO_positions = 0;
    }
    if (VBO_colors != 0) {
        glDeleteBuffers(1, &VBO_colors);
        VBO_colors = 0;
    }
    if (shaderProgram != 0) {
        glDeleteProgram(shaderProgram);
        shaderProgram = 0;
    }

    initialized = false;
}

}}}}
