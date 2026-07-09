#include <cmath>
#include <cstdio>

#include <GL/glew.h>
#include <GL/gl.h>

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ArrowRenderer.h"

const int OpenGL4ArrowRenderer::SLICES = 16;
unsigned int OpenGL4ArrowRenderer::vao_ = 0;
unsigned int OpenGL4ArrowRenderer::vboPositions_ = 0;
unsigned int OpenGL4ArrowRenderer::vboNormals_ = 0;
unsigned int OpenGL4ArrowRenderer::vboUvs_ = 0;
unsigned int OpenGL4ArrowRenderer::program_ = 0;
int OpenGL4ArrowRenderer::vertexCount_ = 0;
bool OpenGL4ArrowRenderer::initialized_ = false;

void OpenGL4ArrowRenderer::draw(
    const Arrow* arrow,
    const Matrix4x4d& modelMatrix,
    const Matrix4x4d& projection,
    const Camera* camera,
    const java::ArrayList<Light*>& lights,
    const SimpleMaterial* material,
    const RendererConfiguration* quality)
{
    if ( arrow == 0 || camera == 0 || material == 0 || quality == 0 || lights.size() == 0 ) {
        return;
    }
    if ( !ensureProgram() || !ensureMesh(arrow) ) {
        return;
    }

    Matrix4x4d mvp = projection.multiply(modelMatrix);
    Matrix4x4d modelIt = modelMatrix.invert().transpose();

    glUseProgram(program_);
    setMatrix(program_, "modelViewProjectionLocal", mvp);
    setMatrix(program_, "modelViewLocal", modelMatrix);
    setMatrix(program_, "modelViewITLocal", modelIt);
    setVector3(program_, "cameraPositionGlobal", camera->getPosition());

    int lightCount = 0;
    for ( long i = 0; i < lights.size(); i++ ) {
        Light* light = lights.get(i);
        if ( light == 0 ) {
            continue;
        }
        char name[64];
        std::snprintf(name, sizeof(name), "lightPositionsGlobal[%d]", lightCount);
        setVector3(program_, name, light->getPosition());
        std::snprintf(name, sizeof(name), "lightColorsGlobal[%d]", lightCount);
        setVector3(program_, name, light->getEmission());
        lightCount++;
    }

    setInt(program_, "numberOfLights", lightCount);
    setVector3(program_, "ambientColor", material->getAmbient());
    setVector3(program_, "diffuseColor", material->getDiffuse());
    setVector3(program_, "specularColor", material->getSpecular());
    setFloat(program_, "phongExponent", (float)material->getPhongExponent());
    setInt(program_, "withTexture", 0);
    setInt(program_, "withBumpMap", 0);
    setInt(program_, "withVertexColors", 0);

    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glEnable(GL_POLYGON_OFFSET_FILL);
    glPolygonOffset(1.0f, 1.0f);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    glBindVertexArray(vao_);
    glDrawArrays(GL_TRIANGLES, 0, vertexCount_);
    glBindVertexArray(0);

    glDisable(GL_POLYGON_OFFSET_FILL);
    glDisable(GL_CULL_FACE);
    glUseProgram(0);
}

void OpenGL4ArrowRenderer::dispose()
{
    if ( vboPositions_ != 0 ) { glDeleteBuffers(1, &vboPositions_); vboPositions_ = 0; }
    if ( vboNormals_ != 0 ) { glDeleteBuffers(1, &vboNormals_); vboNormals_ = 0; }
    if ( vboUvs_ != 0 ) { glDeleteBuffers(1, &vboUvs_); vboUvs_ = 0; }
    if ( vao_ != 0 ) { glDeleteVertexArrays(1, &vao_); vao_ = 0; }
    if ( program_ != 0 ) { glDeleteProgram(program_); program_ = 0; }
    vertexCount_ = 0;
    initialized_ = false;
}

bool OpenGL4ArrowRenderer::ensureProgram()
{
    if ( program_ != 0 ) {
        return true;
    }
    program_ = buildProgram("gouraudTextureVertexShader.glsl", "gouraudTexturePixelShader.glsl");
    return program_ != 0;
}

bool OpenGL4ArrowRenderer::ensureMesh(const Arrow* arrow)
{
    if ( initialized_ ) {
        return true;
    }
    ArrowMesh mesh = buildArrowMesh(
        arrow->getBaseRadius(),
        arrow->getHeadRadius(),
        arrow->getBaseLength(),
        arrow->getHeadLength(),
        SLICES);
    uploadMesh(mesh);
    initialized_ = true;
    return true;
}

OpenGL4ArrowRenderer::ArrowMesh OpenGL4ArrowRenderer::buildArrowMesh(
    double baseRadius,
    double headRadius,
    double baseLength,
    double headLength,
    int slices)
{
    ArrowMesh mesh;

    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2.0 * java::Math::PI * i / slices;
        double a1 = 2.0 * java::Math::PI * (i + 1) / slices;
        float x0 = (float)(std::cos(a0) * baseRadius);
        float y0 = (float)(std::sin(a0) * baseRadius);
        float x1 = (float)(std::cos(a1) * baseRadius);
        float y1 = (float)(std::sin(a1) * baseRadius);
        float nx0 = (float)std::cos(a0);
        float ny0 = (float)std::sin(a0);
        float nx1 = (float)std::cos(a1);
        float ny1 = (float)std::sin(a1);

        addPos(mesh.positions, x0, y0, 0); addNorm(mesh.normals, nx0, ny0, 0); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, x1, y1, 0); addNorm(mesh.normals, nx1, ny1, 0); addUv(mesh.uvs, 1, 0);
        addPos(mesh.positions, x1, y1, (float)baseLength); addNorm(mesh.normals, nx1, ny1, 0); addUv(mesh.uvs, 1, 1);

        addPos(mesh.positions, x0, y0, 0); addNorm(mesh.normals, nx0, ny0, 0); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, x1, y1, (float)baseLength); addNorm(mesh.normals, nx1, ny1, 0); addUv(mesh.uvs, 1, 1);
        addPos(mesh.positions, x0, y0, (float)baseLength); addNorm(mesh.normals, nx0, ny0, 0); addUv(mesh.uvs, 0, 1);
    }

    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2.0 * java::Math::PI * i / slices;
        double a1 = 2.0 * java::Math::PI * (i + 1) / slices;
        float x0 = (float)(std::cos(a0) * baseRadius);
        float y0 = (float)(std::sin(a0) * baseRadius);
        float x1 = (float)(std::cos(a1) * baseRadius);
        float y1 = (float)(std::sin(a1) * baseRadius);

        addPos(mesh.positions, 0, 0, 0); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0.5f, 0.5f);
        addPos(mesh.positions, x1, y1, 0); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, x0, y0, 0); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 0);
    }

    float coneBase = (float)baseLength;
    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2.0 * java::Math::PI * i / slices;
        double a1 = 2.0 * java::Math::PI * (i + 1) / slices;
        float ox0 = (float)(std::cos(a0) * baseRadius);
        float oy0 = (float)(std::sin(a0) * baseRadius);
        float ox1 = (float)(std::cos(a1) * baseRadius);
        float oy1 = (float)(std::sin(a1) * baseRadius);
        float ix0 = (float)(std::cos(a0) * headRadius);
        float iy0 = (float)(std::sin(a0) * headRadius);
        float ix1 = (float)(std::cos(a1) * headRadius);
        float iy1 = (float)(std::sin(a1) * headRadius);

        addPos(mesh.positions, ox0, oy0, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, ox1, oy1, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 0);
        addPos(mesh.positions, ix1, iy1, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 1);

        addPos(mesh.positions, ox0, oy0, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, ix1, iy1, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 1);
        addPos(mesh.positions, ix0, iy0, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 1);
    }

    float apex = (float)(baseLength + headLength);
    float slantLength = (float)std::sqrt(headRadius * headRadius + headLength * headLength);
    float cosAlpha = slantLength > 1e-12f ? (float)(headLength / slantLength) : 1.0f;
    float sinAlpha = slantLength > 1e-12f ? (float)(headRadius / slantLength) : 0.0f;

    for ( int i = 0; i < slices; i++ ) {
        double a0 = 2.0 * java::Math::PI * i / slices;
        double a1 = 2.0 * java::Math::PI * (i + 1) / slices;
        double aMid = (a0 + a1) * 0.5;
        float x0 = (float)(std::cos(a0) * headRadius);
        float y0 = (float)(std::sin(a0) * headRadius);
        float x1 = (float)(std::cos(a1) * headRadius);
        float y1 = (float)(std::sin(a1) * headRadius);
        float nx0 = (float)(std::cos(a0) * sinAlpha);
        float ny0 = (float)(std::sin(a0) * sinAlpha);
        float nx1 = (float)(std::cos(a1) * sinAlpha);
        float ny1 = (float)(std::sin(a1) * sinAlpha);
        float nxMid = (float)(std::cos(aMid) * sinAlpha);
        float nyMid = (float)(std::sin(aMid) * sinAlpha);

        addPos(mesh.positions, x0, y0, coneBase); addNorm(mesh.normals, nx0, ny0, cosAlpha); addUv(mesh.uvs, 0, 1);
        addPos(mesh.positions, x1, y1, coneBase); addNorm(mesh.normals, nx1, ny1, cosAlpha); addUv(mesh.uvs, 1, 1);
        addPos(mesh.positions, 0, 0, apex); addNorm(mesh.normals, nxMid, nyMid, cosAlpha); addUv(mesh.uvs, 0.5f, 0);
    }

    mesh.vertexCount = (int)(mesh.positions.size() / 3);
    return mesh;
}

void OpenGL4ArrowRenderer::uploadMesh(const ArrowMesh& mesh)
{
    glGenVertexArrays(1, &vao_);
    glGenBuffers(1, &vboPositions_);
    glGenBuffers(1, &vboNormals_);
    glGenBuffers(1, &vboUvs_);

    glBindVertexArray(vao_);
    uploadBuffer(vboPositions_, 0, 3, mesh.positions);
    uploadBuffer(vboNormals_, 1, 3, mesh.normals);
    uploadBuffer(vboUvs_, 2, 2, mesh.uvs);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    vertexCount_ = mesh.vertexCount;
}

unsigned int OpenGL4ArrowRenderer::buildProgram(const char* vsFile, const char* fsFile)
{
    java::String vsSource = findShaderSource(vsFile);
    java::String fsSource = findShaderSource(fsFile);
    if ( vsSource.empty() || fsSource.empty() ) {
        std::fprintf(stderr, "OpenGL4ArrowRenderer shader not found: %s / %s\n", vsFile, fsFile);
        return 0;
    }

    unsigned int vs = compileShader(GL_VERTEX_SHADER, vsSource.c_str());
    unsigned int fs = compileShader(GL_FRAGMENT_SHADER, fsSource.c_str());
    if ( vs == 0 || fs == 0 ) {
        if ( vs != 0 ) glDeleteShader(vs);
        if ( fs != 0 ) glDeleteShader(fs);
        return 0;
    }

    unsigned int program = glCreateProgram();
    glAttachShader(program, vs);
    glAttachShader(program, fs);
    glLinkProgram(program);
    glDeleteShader(vs);
    glDeleteShader(fs);

    int ok = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetProgramInfoLog(program, sizeof(log), 0, log);
        std::fprintf(stderr, "OpenGL4ArrowRenderer link error: %s\n", log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

unsigned int OpenGL4ArrowRenderer::compileShader(unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, 0);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), 0, log);
        std::fprintf(stderr, "OpenGL4ArrowRenderer shader compile error: %s\n", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

java::String OpenGL4ArrowRenderer::readTextFile(const java::String& path)
{
    FILE* f = std::fopen(path.c_str(), "rb");
    if ( f == 0 ) return java::String();
    std::fseek(f, 0, SEEK_END);
    long size = std::ftell(f);
    if ( size <= 0 ) { std::fclose(f); return java::String(); }
    std::fseek(f, 0, SEEK_SET);
    char* buffer = new char[(size_t)size + 1u]();
    size_t readSize = std::fread(buffer, 1, (size_t)size, f);
    std::fclose(f);
    buffer[readSize] = '\0';
    java::String result(buffer);
    delete[] buffer;
    return result;
}

java::String OpenGL4ArrowRenderer::findShaderSource(const java::String& shaderFileName)
{
    const char* candidates[] = {
        "../../../../etc/glslShaders/",
        "../../../etc/glslShaders/",
        "../../etc/glslShaders/",
        "../etc/glslShaders/",
        "etc/glslShaders/"
    };
    for ( size_t i = 0; i < sizeof(candidates) / sizeof(candidates[0]); i++ ) {
        java::String src = readTextFile(java::String(candidates[i]) + shaderFileName);
        if ( !src.empty() ) return src;
    }
    return java::String();
}

void OpenGL4ArrowRenderer::addPos(java::ArrayList<float>& buf, float x, float y, float z)
{
    buf.add(x); buf.add(y); buf.add(z);
}

void OpenGL4ArrowRenderer::addNorm(java::ArrayList<float>& buf, float x, float y, float z)
{
    float len = std::sqrt(x * x + y * y + z * z);
    if ( len > 1e-12f ) {
        buf.add(x / len); buf.add(y / len); buf.add(z / len);
    }
    else {
        buf.add(0); buf.add(0); buf.add(1);
    }
}

void OpenGL4ArrowRenderer::addUv(java::ArrayList<float>& buf, float u, float v)
{
    buf.add(u); buf.add(v);
}

void OpenGL4ArrowRenderer::uploadBuffer(unsigned int bufferId, int attrib, int size, const java::ArrayList<float>& data)
{
    glBindBuffer(GL_ARRAY_BUFFER, bufferId);
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr)(data.size() * sizeof(float)), data.data(), GL_STATIC_DRAW);
    glEnableVertexAttribArray((GLuint)attrib);
    glVertexAttribPointer((GLuint)attrib, size, GL_FLOAT, GL_FALSE, 0, 0);
}

void OpenGL4ArrowRenderer::setMatrix(unsigned int programId, const char* name, const Matrix4x4d& matrix)
{
    float* values = matrix.exportToFloatArrayColumnOrder();
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniformMatrix4fv(loc, 1, GL_FALSE, values);
    delete[] values;
}

void OpenGL4ArrowRenderer::setVector3(unsigned int programId, const char* name, const Vector3Dd& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
}

void OpenGL4ArrowRenderer::setVector3(unsigned int programId, const char* name, const ColorRgb& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.r(), (float)value.g(), (float)value.b());
}

void OpenGL4ArrowRenderer::setInt(unsigned int programId, const char* name, int value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1i(loc, value);
}

void OpenGL4ArrowRenderer::setFloat(unsigned int programId, const char* name, float value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1f(loc, value);
}
