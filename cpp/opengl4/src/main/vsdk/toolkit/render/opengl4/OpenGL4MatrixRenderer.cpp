#include <cstdio>
#include <cstring>

#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#ifdef __APPLE__
#include <OpenGL/gl3.h>
#else
#include <GL/glew.h>
#include <GL/gl.h>
#endif
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4MatrixRenderer.h"
unsigned int OpenGL4MatrixRenderer::VAO = 0;
unsigned int OpenGL4MatrixRenderer::VBO_positions = 0;
unsigned int OpenGL4MatrixRenderer::VBO_colors = 0;
unsigned int OpenGL4MatrixRenderer::shaderProgram = 0;
bool OpenGL4MatrixRenderer::initialized = false;

java::String OpenGL4MatrixRenderer::readShaderFile(const java::String& filename) {
    FILE* file = fopen(filename.c_str(), "r");
    if (file) {
        fseek(file, 0, SEEK_END);
        long fileSize = ftell(file);
        if (fileSize > 0) {
            fseek(file, 0, SEEK_SET);
            char* buffer = new char[fileSize + 1];
            size_t readSize = fread(buffer, 1, fileSize, file);
            buffer[readSize] = '\0';
            fclose(file);
            java::String result(buffer);
            delete[] buffer;
            return result;
        }
        fclose(file);
    }

    const char* prefixes[] = { "../", "../../", "../../../", "../../../../", nullptr };
    for (int i = 0; prefixes[i] != nullptr; i++) {
        java::String altPath = java::String(prefixes[i]).concat(filename);
        file = fopen(altPath.c_str(), "r");
        if (file) {
            fseek(file, 0, SEEK_END);
            long fileSize = ftell(file);
            if (fileSize > 0) {
                fseek(file, 0, SEEK_SET);
                char* buffer = new char[fileSize + 1];
                size_t readSize = fread(buffer, 1, fileSize, file);
                buffer[readSize] = '\0';
                fclose(file);
                java::String result(buffer);
                delete[] buffer;
                return result;
            }
            fclose(file);
        }
    }

    fprintf(stderr, "Error: Could not open shader file: %s\n", filename.c_str());
    return "";
}

unsigned int OpenGL4MatrixRenderer::compileShader(const java::String& source, int type) {
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
    java::String vertexSource = readShaderFile("../etc/glslShaders/lineVertexShader.glsl");
    java::String fragmentSource = readShaderFile("../etc/glslShaders/linePixelShader.glsl");

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
    Vector3Dd x(A.get(0, 0), A.get(1, 0), A.get(2, 0));
    Vector3Dd y(A.get(0, 1), A.get(1, 1), A.get(2, 1));
    Vector3Dd z(A.get(0, 2), A.get(1, 2), A.get(2, 2));
    Vector3Dd translation(A.get(0, 3), A.get(1, 3), A.get(2, 3));
    java::ArrayList<float> positions;
    positions.reserve(18);
    positions.add((float)translation.x());
    positions.add((float)translation.y());
    positions.add((float)translation.z());
    positions.add((float)(translation.x() + x.x()));
    positions.add((float)(translation.y() + x.y()));
    positions.add((float)(translation.z() + x.z()));

    positions.add((float)translation.x());
    positions.add((float)translation.y());
    positions.add((float)translation.z());
    positions.add((float)(translation.x() + y.x()));
    positions.add((float)(translation.y() + y.y()));
    positions.add((float)(translation.z() + y.z()));

    positions.add((float)translation.x());
    positions.add((float)translation.y());
    positions.add((float)translation.z());
    positions.add((float)(translation.x() + z.x()));
    positions.add((float)(translation.y() + z.y()));
    positions.add((float)(translation.z() + z.z()));

    java::ArrayList<float> colors;
    colors.reserve(18);
    colors.add(1.0f); colors.add(0.0f); colors.add(0.0f);
    colors.add(1.0f); colors.add(0.0f); colors.add(0.0f);
    colors.add(0.0f); colors.add(1.0f); colors.add(0.0f);
    colors.add(0.0f); colors.add(1.0f); colors.add(0.0f);
    colors.add(0.0f); colors.add(0.0f); colors.add(1.0f);
    colors.add(0.0f); colors.add(0.0f); colors.add(1.0f);

    double mvpValues[4][4];
    int pos = 0;
    for ( int column = 0; column < 4; column++ ) {
        for ( int row = 0; row < 4; row++, pos++ ) {
            mvpValues[row][column] = mvpColumnMajor16[pos];
        }
    }
    Matrix4x4d mvp(mvpValues);
    OpenGL4LineRenderer::drawLines(mvp, positions, colors, 1.0f);
}

void OpenGL4MatrixRenderer::release() {
    OpenGL4LineRenderer::release();
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
