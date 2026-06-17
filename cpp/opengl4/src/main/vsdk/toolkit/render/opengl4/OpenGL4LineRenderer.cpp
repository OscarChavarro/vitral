#include <cmath>
#include <cstdio>

#include "java/util/ArrayList.txx"
#ifdef __APPLE__
#include <OpenGL/gl3.h>
#else
#include <GL/glew.h>
#include <GL/gl.h>
#endif
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
const double CLIP_PLANES[][4] = {
    { 1.0, 0.0, 0.0, 1.0 },
    { -1.0, 0.0, 0.0, 1.0 },
    { 0.0, 1.0, 0.0, 1.0 },
    { 0.0, -1.0, 0.0, 1.0 },
    { 0.0, 0.0, 1.0, 1.0 },
    { 0.0, 0.0, -1.0, 1.0 }
};

double OpenGL4LineRenderer::evaluateClipPlane(const double plane[4], const Vector4Dd& point)
{
    return plane[0] * point.x() + plane[1] * point.y() +
        plane[2] * point.z() + plane[3] * point.w();
}

Vector4Dd OpenGL4LineRenderer::interpolate(const Vector4Dd& start, const Vector4Dd& end, double t)
{
    return start.multiply(1.0 - t).add(end.multiply(t));
}

bool OpenGL4LineRenderer::clipLineToClipVolume(
    const Vector4Dd& start,
    const Vector4Dd& end,
    Vector4Dd& outStart,
    Vector4Dd& outEnd)
{
    outStart = start;
    outEnd = end;

    for ( int i = 0; i < 6; i++ ) {
        double d0 = evaluateClipPlane(CLIP_PLANES[i], outStart);
        double d1 = evaluateClipPlane(CLIP_PLANES[i], outEnd);
        if ( d0 < 0.0 && d1 < 0.0 ) {
            return false;
        }
        if ( d0 < 0.0 || d1 < 0.0 ) {
            double denominator = d0 - d1;
            if ( std::abs(denominator) < 1.0e-12 ) {
                return false;
            }
            double t = d0 / denominator;
            Vector4Dd intersection = interpolate(outStart, outEnd, t);
            if ( d0 < 0.0 ) {
                outStart = intersection;
            }
            else {
                outEnd = intersection;
            }
        }
    }
    return true;
}

void OpenGL4LineRenderer::addVertex(
    java::ArrayList<float>& positions,
    java::ArrayList<float>& colors,
    const float point[3],
    const float color[3])
{
    positions.add(point[0]);
    positions.add(point[1]);
    positions.add(point[2]);
    colors.add(color[0]);
    colors.add(color[1]);
    colors.add(color[2]);
}

unsigned int OpenGL4LineRenderer::vao_ = 0;
unsigned int OpenGL4LineRenderer::positionVbo_ = 0;
unsigned int OpenGL4LineRenderer::colorVbo_ = 0;
unsigned int OpenGL4LineRenderer::program_ = 0;
bool OpenGL4LineRenderer::initialized_ = false;

java::String OpenGL4LineRenderer::readShaderFile(const java::String& filename)
{
    FILE* file = fopen(filename.c_str(), "r");
    if ( file != 0 ) {
        fseek(file, 0, SEEK_END);
        long fileSize = ftell(file);
        if ( fileSize > 0 ) {
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

    const char* prefixes[] = { "../", "../../", "../../../", "../../../../", 0 };
    for ( int i = 0; prefixes[i] != 0; i++ ) {
        java::String altPath = java::String(prefixes[i]).concat(filename);
        file = fopen(altPath.c_str(), "r");
        if ( file != 0 ) {
            fseek(file, 0, SEEK_END);
            long fileSize = ftell(file);
            if ( fileSize > 0 ) {
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

unsigned int OpenGL4LineRenderer::compileShader(
    const java::String& source, unsigned int type)
{
    unsigned int shader = glCreateShader(type);
    const char* src = source.c_str();
    glShaderSource(shader, 1, &src, 0);
    glCompileShader(shader);

    int success = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
    if ( !success ) {
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

bool OpenGL4LineRenderer::initializeIfNeeded()
{
    if ( initialized_ ) {
        return true;
    }

    java::String vertexSource =
        readShaderFile("../etc/glslShaders/lineVertexShader.glsl");
    java::String fragmentSource =
        readShaderFile("../etc/glslShaders/linePixelShader.glsl");
    if ( vertexSource.empty() || fragmentSource.empty() ) {
        return false;
    }

    unsigned int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
    unsigned int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
    if ( vertexShader == 0 || fragmentShader == 0 ) {
        if ( vertexShader != 0 ) glDeleteShader(vertexShader);
        if ( fragmentShader != 0 ) glDeleteShader(fragmentShader);
        return false;
    }

    program_ = glCreateProgram();
    glAttachShader(program_, vertexShader);
    glAttachShader(program_, fragmentShader);
    glLinkProgram(program_);
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    int success = 0;
    glGetProgramiv(program_, GL_LINK_STATUS, &success);
    if ( !success ) {
        glDeleteProgram(program_);
        program_ = 0;
        return false;
    }

    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &positionVbo_);
    glGenBuffers(1, &colorVbo_);
    initialized_ = true;
    return true;
}

void OpenGL4LineRenderer::drawLines(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth)
{
    drawLines(modelViewProjection, positions, colors, lineWidth, 0.0f);
}

void OpenGL4LineRenderer::drawLines(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth,
    float depthBiasNdc)
{
    if ( positions.size() == 0 || colors.size() == 0 ) {
        return;
    }
    if ( positions.size() != colors.size() ) {
        fprintf(stderr, "OpenGL4LineRenderer positions/colors length mismatch\n");
        return;
    }

    if ( lineWidth <= 1.0f ) {
        drawThinLines(modelViewProjection, positions, colors, lineWidth,
            depthBiasNdc);
        return;
    }

    java::ArrayList<float> trianglePositions;
    java::ArrayList<float> triangleColors;
    buildThickLineMesh(modelViewProjection, positions, colors, lineWidth,
        trianglePositions, triangleColors);
    if ( trianglePositions.size() == 0 ) {
        return;
    }

    drawPrimitives(Matrix4x4d::identityMatrix(), trianglePositions,
        triangleColors, GL_TRIANGLES, depthBiasNdc, 1.0f);
}

void OpenGL4LineRenderer::drawThinLines(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth,
    float depthBiasNdc)
{
    drawPrimitives(modelViewProjection, positions, colors, GL_LINES,
        depthBiasNdc, lineWidth);
}

void OpenGL4LineRenderer::drawPrimitives(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    unsigned int primitiveType,
    float depthBiasNdc,
    float lineWidth)
{
    if ( positions.size() == 0 || colors.size() == 0 ) {
        return;
    }
    if ( !initializeIfNeeded() ) {
        return;
    }

    float* mvpFloat = modelViewProjection.exportToFloatArrayColumnOrder();
    glUseProgram(program_);

    GLint mvpLoc = glGetUniformLocation(program_, "modelViewProjectionLocal");
    if ( mvpLoc >= 0 ) {
        glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, mvpFloat);
    }
    GLint depthBiasLoc = glGetUniformLocation(program_, "depthBiasNdc");
    if ( depthBiasLoc >= 0 ) {
        glUniform1f(depthBiasLoc, depthBiasNdc);
    }

    glBindVertexArray(vao_);

    glBindBuffer(GL_ARRAY_BUFFER, positionVbo_);
    glBufferData(GL_ARRAY_BUFFER, positions.size() * sizeof(float),
        const_cast<java::ArrayList<float>&>(positions).data(), GL_STREAM_DRAW);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, 0);
    glEnableVertexAttribArray(0);

    glBindBuffer(GL_ARRAY_BUFFER, colorVbo_);
    glBufferData(GL_ARRAY_BUFFER, colors.size() * sizeof(float),
        const_cast<java::ArrayList<float>&>(colors).data(), GL_STREAM_DRAW);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, 0);
    glEnableVertexAttribArray(1);

    if ( primitiveType == GL_LINES ) {
        glLineWidth(lineWidth);
    }
    glDrawArrays(primitiveType, 0, (GLsizei)(positions.size() / 3));

    glDisableVertexAttribArray(0);
    glDisableVertexAttribArray(1);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
    glUseProgram(0);
    delete[] mvpFloat;
}

void OpenGL4LineRenderer::buildThickLineMesh(
    const Matrix4x4d& modelViewProjection,
    const java::ArrayList<float>& positions,
    const java::ArrayList<float>& colors,
    float lineWidth,
    java::ArrayList<float>& trianglePositions,
    java::ArrayList<float>& triangleColors)
{
    int viewport[4] = { 0, 0, 1, 1 };
    glGetIntegerv(GL_VIEWPORT, viewport);
    double viewportWidth = viewport[2] > 0 ? viewport[2] : 1.0;
    double viewportHeight = viewport[3] > 0 ? viewport[3] : 1.0;
    double halfWidth = lineWidth / 2.0;

    for ( long int i = 0; i + 5 < positions.size(); i += 6 ) {
        Vector4Dd clip0 = modelViewProjection.multiply(Vector4Dd(
            positions.get(i), positions.get(i + 1), positions.get(i + 2), 1.0));
        Vector4Dd clip1 = modelViewProjection.multiply(Vector4Dd(
            positions.get(i + 3), positions.get(i + 4), positions.get(i + 5), 1.0));
        Vector4Dd clipped0(0.0, 0.0, 0.0, 1.0);
        Vector4Dd clipped1(0.0, 0.0, 0.0, 1.0);
        if ( !clipLineToClipVolume(clip0, clip1, clipped0, clipped1) ) {
            continue;
        }

        Vector4Dd ndc0 = clipped0.dividedByW();
        Vector4Dd ndc1 = clipped1.dividedByW();
        double dxPixels = (ndc1.x() - ndc0.x()) * viewportWidth / 2.0;
        double dyPixels = (ndc1.y() - ndc0.y()) * viewportHeight / 2.0;
        double lengthPixels = std::sqrt(dxPixels * dxPixels + dyPixels * dyPixels);
        if ( lengthPixels <= 1.0e-9 ) {
            continue;
        }

        double perpX = -dyPixels / lengthPixels;
        double perpY = dxPixels / lengthPixels;
        double offsetNdcX = perpX * halfWidth * 2.0 / viewportWidth;
        double offsetNdcY = perpY * halfWidth * 2.0 / viewportHeight;

        float p0Plus[3] = {
            (float)(ndc0.x() + offsetNdcX),
            (float)(ndc0.y() + offsetNdcY),
            (float)ndc0.z()
        };
        float p0Minus[3] = {
            (float)(ndc0.x() - offsetNdcX),
            (float)(ndc0.y() - offsetNdcY),
            (float)ndc0.z()
        };
        float p1Plus[3] = {
            (float)(ndc1.x() + offsetNdcX),
            (float)(ndc1.y() + offsetNdcY),
            (float)ndc1.z()
        };
        float p1Minus[3] = {
            (float)(ndc1.x() - offsetNdcX),
            (float)(ndc1.y() - offsetNdcY),
            (float)ndc1.z()
        };

        float c0[3] = {
            colors.get(i), colors.get(i + 1), colors.get(i + 2)
        };
        float c1[3] = {
            colors.get(i + 3), colors.get(i + 4), colors.get(i + 5)
        };

        addVertex(trianglePositions, triangleColors, p0Plus, c0);
        addVertex(trianglePositions, triangleColors, p0Minus, c0);
        addVertex(trianglePositions, triangleColors, p1Plus, c1);

        addVertex(trianglePositions, triangleColors, p1Plus, c1);
        addVertex(trianglePositions, triangleColors, p0Minus, c0);
        addVertex(trianglePositions, triangleColors, p1Minus, c1);
    }
}

void OpenGL4LineRenderer::release()
{
    if ( !initialized_ ) {
        return;
    }

    if ( positionVbo_ != 0 ) {
        glDeleteBuffers(1, &positionVbo_);
        positionVbo_ = 0;
    }
    if ( colorVbo_ != 0 ) {
        glDeleteBuffers(1, &colorVbo_);
        colorVbo_ = 0;
    }
    if ( vao_ != 0 ) {
        glDeleteVertexArrays(1, &vao_);
        vao_ = 0;
    }
    if ( program_ != 0 ) {
        glDeleteProgram(program_);
        program_ = 0;
    }
    initialized_ = false;
}
