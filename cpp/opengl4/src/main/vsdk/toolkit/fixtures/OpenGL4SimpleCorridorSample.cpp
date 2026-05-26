#include "java/lang/String.h"
#include "vsdk/toolkit/fixtures/OpenGL4SimpleCorridorSample.h"
#include "java/util/ArrayList.txx"
#include <cmath>
#include <cstdio>

#ifdef __APPLE__
#include <OpenGL/gl3.h>
#else
#include <GL/glew.h>
#include <GL/gl.h>
#endif

namespace vsdk { namespace toolkit { namespace fixtures {

OpenGL4SimpleCorridorSample::OpenGL4SimpleCorridorSample()
    : a(6), na(6), b(20), nb(20), c(4), nc(4), interSpace(0.05),
      initialized(false), shaderProgramId(0), vertexArrayId(0),
      positionBufferId(0), colorBufferId(0),
      modelViewProjectionLocalLoc(-1), withTextureLoc(-1),
      withVertexColorsLoc(-1), diffuseColorLoc(-1), vertexCount(0) {
}

OpenGL4SimpleCorridorSample::~OpenGL4SimpleCorridorSample() {
    if (initialized) {
        dispose();
    }
}

java::String OpenGL4SimpleCorridorSample::readShaderFile(const java::String& filename) {
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

unsigned int OpenGL4SimpleCorridorSample::compileShader(const java::String& source, int type) {
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

unsigned int OpenGL4SimpleCorridorSample::compileShaders() {
    java::String vertexSource = readShaderFile("../etc/glslShaders/constantVertexShader.glsl");
    java::String fragmentSource = readShaderFile("../etc/glslShaders/constantPixelShader.glsl");

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

void OpenGL4SimpleCorridorSample::initialize() {
    java::ArrayList<float> positions;
    java::ArrayList<float> colors;

    buildGeometry(positions, colors);

    shaderProgramId = compileShaders();
    if (shaderProgramId == 0) {
        fprintf(stderr, "Error: Failed to compile shader program\n");
        return;
    }

    modelViewProjectionLocalLoc = glGetUniformLocation(shaderProgramId, "modelViewProjectionLocal");
    withTextureLoc = glGetUniformLocation(shaderProgramId, "withTexture");
    withVertexColorsLoc = glGetUniformLocation(shaderProgramId, "withVertexColors");
    diffuseColorLoc = glGetUniformLocation(shaderProgramId, "diffuseColor");

    unsigned int tmp;

    glGenVertexArrays(1, &vertexArrayId);
    glBindVertexArray(vertexArrayId);

    glGenBuffers(1, &positionBufferId);
    glBindBuffer(GL_ARRAY_BUFFER, positionBufferId);
    glBufferData(GL_ARRAY_BUFFER, positions.size() * sizeof(float), positions.data(), GL_STATIC_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    glGenBuffers(1, &colorBufferId);
    glBindBuffer(GL_ARRAY_BUFFER, colorBufferId);
    glBufferData(GL_ARRAY_BUFFER, colors.size() * sizeof(float), colors.data(), GL_STATIC_DRAW);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    vertexCount = positions.size() / 3;
    initialized = true;
}

void OpenGL4SimpleCorridorSample::drawGL(const float* mvpColumnMajor16, const Matrix4x4d& modelViewProjection) {
    if (!initialized) {
        initialize();
    }

    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);

    glUseProgram(shaderProgramId);
    glUniformMatrix4fv(modelViewProjectionLocalLoc, 1, GL_FALSE, mvpColumnMajor16);

    if (withTextureLoc >= 0) {
        glUniform1i(withTextureLoc, 0);
    }
    if (withVertexColorsLoc >= 0) {
        glUniform1i(withVertexColorsLoc, 1);
    }
    if (diffuseColorLoc >= 0) {
        glUniform3f(diffuseColorLoc, 1.0f, 1.0f, 1.0f);
    }

    glBindVertexArray(vertexArrayId);
    glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    glBindVertexArray(0);
    glUseProgram(0);
}

void OpenGL4SimpleCorridorSample::dispose() {
    if (positionBufferId != 0) {
        glDeleteBuffers(1, &positionBufferId);
        positionBufferId = 0;
    }
    if (colorBufferId != 0) {
        glDeleteBuffers(1, &colorBufferId);
        colorBufferId = 0;
    }
    if (vertexArrayId != 0) {
        glDeleteVertexArrays(1, &vertexArrayId);
        vertexArrayId = 0;
    }
    if (shaderProgramId != 0) {
        glDeleteProgram(shaderProgramId);
        shaderProgramId = 0;
    }
    initialized = false;
    vertexCount = 0;
}

void OpenGL4SimpleCorridorSample::buildGeometry(java::ArrayList<float>& positions, java::ArrayList<float>& colors) {
    appendTilesCenter(positions, colors, 0.5f, 0.5f, 0.9f, 0, false, 0);
    for (int i = 0; i < 4; i++) {
        appendTilesLong(positions, colors, 0.5f, 0.5f, 0.9f, 90 * i, false, 0);
    }

    appendTilesCenter(positions, colors, 0.0f, 0.0f, 1.0f, 0, true, c);
    for (int i = 0; i < 4; i++) {
        appendTilesLong(positions, colors, 0.0f, 0.0f, 1.0f, 90 * i, true, c);
    }

    for (int i = 0; i < 4; i++) {
        switch (i) {
            case 0:
                appendTilesWallA(positions, colors, 0.9f, 0.5f, 0.5f, 90 * i);
                break;
            case 1:
                appendTilesWallA(positions, colors, 0.5f, 0.9f, 0.5f, 90 * i);
                break;
            case 2:
                appendTilesWallA(positions, colors, 1.0f, 0.0f, 0.0f, 90 * i);
                break;
            default:
                appendTilesWallA(positions, colors, 0.0f, 1.0f, 0.0f, 90 * i);
                break;
        }
    }

    for (int i = 0; i < 4; i++) {
        appendTilesWallB(positions, colors, 0.9f, 0.5f, 0.8f, 90 * i);
        appendTilesWallC(positions, colors, 0.9f, 0.5f, 0.8f, 90 * i);
    }
}

void OpenGL4SimpleCorridorSample::appendTilesCenter(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg, bool flipYZ, double translateZ) {

    double da = a / ((double)na);
    double epsilon = 0.005;

    for (int i = 0; i < na; i++) {
        double x = -a / 2 + i * da;
        for (int j = 0; j < na; j++) {
            double y = -a / 2 + j * da;
            addQuad(positions, colors, r, g, bColor,
                x + interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + da - interSpace / 2, -epsilon,
                x + interSpace / 2, y + da - interSpace / 2, -epsilon,
                rotZDeg, flipYZ, translateZ);
        }
    }
}

void OpenGL4SimpleCorridorSample::appendTilesLong(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg, bool flipYZ, double translateZ) {

    double da = a / ((double)na);
    double db = b / ((double)nb);
    double epsilon = 0.001;

    for (int i = 0; i < nb; i++) {
        double x = -a / 2 - b + i * db;
        for (int j = 0; j < na; j++) {
            double y = -a / 2 + j * da;
            addQuad(positions, colors, r, g, bColor,
                x + interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + interSpace / 2, -epsilon,
                x + da - interSpace / 2, y + da - interSpace / 2, -epsilon,
                x + interSpace / 2, y + da - interSpace / 2, -epsilon,
                rotZDeg, flipYZ, translateZ);
        }
    }
}

void OpenGL4SimpleCorridorSample::appendTilesWallA(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg) {

    double da = a / ((double)na);
    double dc = c / ((double)nc);

    for (int i = 0; i < nc; i++) {
        double z = i * dc;
        for (int j = 0; j < na; j++) {
            double y = -a / 2 + j * da;
            addQuad(positions, colors, r, g, bColor,
                -a / 2 - b, y + interSpace / 2, z + dc - interSpace / 2,
                -a / 2 - b, y + interSpace / 2, z + interSpace / 2,
                -a / 2 - b, y + da - interSpace / 2, z + interSpace / 2,
                -a / 2 - b, y + da - interSpace / 2, z + dc - interSpace / 2,
                rotZDeg, false, 0);
        }
    }
}

void OpenGL4SimpleCorridorSample::appendTilesWallB(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg) {

    double db = b / ((double)nb);
    double dc = c / ((double)nc);

    for (int i = 0; i < nc; i++) {
        double z = i * dc;
        for (int j = 0; j < nb; j++) {
            double y = a / 2 + j * db;
            addQuad(positions, colors, r, g, bColor,
                -a / 2, y + interSpace / 2, z + dc - interSpace / 2,
                -a / 2, y + interSpace / 2, z + interSpace / 2,
                -a / 2, y + db - interSpace / 2, z + interSpace / 2,
                -a / 2, y + db - interSpace / 2, z + dc - interSpace / 2,
                rotZDeg, false, 0);
        }
    }
}

void OpenGL4SimpleCorridorSample::appendTilesWallC(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double rotZDeg) {

    double db = b / ((double)nb);
    double dc = c / ((double)nc);

    for (int i = 0; i < nb; i++) {
        double x = -a / 2 - b + i * db;
        for (int j = 0; j < nc; j++) {
            double z = j * dc;
            addQuad(positions, colors, r, g, bColor,
                x + interSpace / 2, a / 2, z + interSpace / 2,
                x + db - interSpace / 2, a / 2, z + interSpace / 2,
                x + db - interSpace / 2, a / 2, z + dc - interSpace / 2,
                x + interSpace / 2, a / 2, z + dc - interSpace / 2,
                rotZDeg, false, 0);
        }
    }
}

void OpenGL4SimpleCorridorSample::addQuad(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    float r, float g, float bColor,
    double x1, double y1, double z1,
    double x2, double y2, double z2,
    double x3, double y3, double z3,
    double x4, double y4, double z4,
    double rotZDeg, bool flipYZ, double translateZ) {

    addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x2, y2, z2, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);

    addVertex(positions, colors, x1, y1, z1, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x3, y3, z3, r, g, bColor, rotZDeg, flipYZ, translateZ);
    addVertex(positions, colors, x4, y4, z4, r, g, bColor, rotZDeg, flipYZ, translateZ);
}

void OpenGL4SimpleCorridorSample::addVertex(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    double x, double y, double z,
    float r, float g, float bColor,
    double rotZDeg, bool flipYZ, double translateZ) {

    double tx = x;
    double ty = y;
    double tz = z;

    if (flipYZ) {
        ty = -ty;
        tz = -tz;
    }

    double angle = rotZDeg * M_PI / 180.0;
    double cos_a = std::cos(angle);
    double sin_a = std::sin(angle);

    double rx = tx * cos_a - ty * sin_a;
    double ry = tx * sin_a + ty * cos_a;
    double rz = tz + translateZ;

    positions.add((float)rx);
    positions.add((float)ry);
    positions.add((float)rz);

    colors.add(r);
    colors.add(g);
    colors.add(bColor);
}

}}}
