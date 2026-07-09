#include <cmath>
#include <cstdio>

#include <GL/glew.h>
#include <GL/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/gizmo/RayGizmo.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ArrowRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RayGizmoRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SphereRenderer.h"

namespace {
const float IND_OUTER_R = 0.65f;
const float IND_INNER_R = 0.17f;
const float IND_HALF_W = 0.12f;
const float IND_TIP_Z = 0.30f;
}

unsigned int OpenGL4RayGizmoRenderer::indicatorVao_ = 0;
unsigned int OpenGL4RayGizmoRenderer::indicatorPositionVbo_ = 0;
unsigned int OpenGL4RayGizmoRenderer::indicatorNormalVbo_ = 0;
unsigned int OpenGL4RayGizmoRenderer::indicatorUvVbo_ = 0;
unsigned int OpenGL4RayGizmoRenderer::indicatorProgram_ = 0;
bool OpenGL4RayGizmoRenderer::initialized_ = false;

void OpenGL4RayGizmoRenderer::draw(RayGizmo* gizmo, Camera* camera, const java::ArrayList<Light*>& lights)
{
    if ( gizmo == 0 || camera == 0 || lights.size() == 0 || !gizmo->isVisible() ) {
        return;
    }
    if ( !ensureIndicatorProgram() || !ensureIndicatorMesh() ) {
        return;
    }

    RendererConfiguration quality = buildSurfaceQuality();
    Matrix4x4d primaryModelMatrix = gizmo->getBody()->getTransformationMatrix();
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    SimpleScene* scene = gizmo->buildScene();

    java::ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        SimpleBody* body = bodies.get(i);
        if ( body == 0 || body->getGeometry() == 0 || body->getMaterial() == 0 ) {
            continue;
        }

        Geometry* geom = body->getGeometry();
        Matrix4x4d modelMatrix = body->getTransformationMatrix();
        SimpleMaterial* material = body->getMaterial();

        Arrow* arrow = dynamic_cast<Arrow*>(geom);
        if ( arrow != 0 ) {
            OpenGL4ArrowRenderer::draw(arrow, modelMatrix, projection, camera, lights, material, &quality);
            continue;
        }

        Sphere* sphere = dynamic_cast<Sphere*>(geom);
        if ( sphere != 0 ) {
            OpenGL4SphereRenderer::draw(
                sphere, camera, lights.get(0), material, &quality,
                0, 0, modelMatrix, 16, 12);
        }
    }

    drawIndicator(gizmo->getRotationAngleInRadians(), primaryModelMatrix, projection, camera, lights, &quality);
    delete scene;

    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
}

void OpenGL4RayGizmoRenderer::dispose()
{
    OpenGL4ArrowRenderer::dispose();
    OpenGL4SphereRenderer::dispose();

    if ( indicatorPositionVbo_ != 0 ) { glDeleteBuffers(1, &indicatorPositionVbo_); indicatorPositionVbo_ = 0; }
    if ( indicatorNormalVbo_ != 0 ) { glDeleteBuffers(1, &indicatorNormalVbo_); indicatorNormalVbo_ = 0; }
    if ( indicatorUvVbo_ != 0 ) { glDeleteBuffers(1, &indicatorUvVbo_); indicatorUvVbo_ = 0; }
    if ( indicatorVao_ != 0 ) { glDeleteVertexArrays(1, &indicatorVao_); indicatorVao_ = 0; }
    if ( indicatorProgram_ != 0 ) { glDeleteProgram(indicatorProgram_); indicatorProgram_ = 0; }
    initialized_ = false;
}

bool OpenGL4RayGizmoRenderer::ensureIndicatorMesh()
{
    if ( initialized_ ) {
        return true;
    }
    uploadIndicatorMesh();
    initialized_ = true;
    return true;
}

bool OpenGL4RayGizmoRenderer::ensureIndicatorProgram()
{
    if ( indicatorProgram_ != 0 ) {
        return true;
    }
    indicatorProgram_ = buildProgram("gouraudTextureVertexShader.glsl", "gouraudTexturePixelShader.glsl");
    return indicatorProgram_ != 0;
}

void OpenGL4RayGizmoRenderer::uploadIndicatorMesh()
{
    float positions[] = {
        IND_OUTER_R, 0.0f, IND_TIP_Z,
        IND_INNER_R, -IND_HALF_W, 0.0f,
        IND_INNER_R, IND_HALF_W, 0.0f
    };
    float normals[9];
    float uvs[] = { 0.5f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f };
    computeNormals(normals);

    glGenVertexArrays(1, &indicatorVao_);
    glGenBuffers(1, &indicatorPositionVbo_);
    glGenBuffers(1, &indicatorNormalVbo_);
    glGenBuffers(1, &indicatorUvVbo_);

    glBindVertexArray(indicatorVao_);
    uploadBuffer(indicatorPositionVbo_, 0, 3, positions, 9);
    uploadBuffer(indicatorNormalVbo_, 1, 3, normals, 9);
    uploadBuffer(indicatorUvVbo_, 2, 2, uvs, 6);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
}

void OpenGL4RayGizmoRenderer::drawIndicator(
    double rollAngleRadians,
    const Matrix4x4d& arrowModelMatrix,
    const Matrix4x4d& projection,
    const Camera* camera,
    const java::ArrayList<Light*>& lights,
    const RendererConfiguration* quality)
{
    Matrix4x4d rollRotation = Matrix4x4d().axisRotation(rollAngleRadians, 0, 0, 1);
    Matrix4x4d indicatorModelMatrix = arrowModelMatrix.multiply(rollRotation);
    Matrix4x4d indicatorMvp = projection.multiply(indicatorModelMatrix);
    Matrix4x4d indicatorModelIt = indicatorModelMatrix.invert().transpose();
    SimpleMaterial material = indicatorMaterial();

    glUseProgram(indicatorProgram_);
    setMatrix(indicatorProgram_, "modelViewProjectionLocal", indicatorMvp);
    setMatrix(indicatorProgram_, "modelViewLocal", indicatorModelMatrix);
    setMatrix(indicatorProgram_, "modelViewITLocal", indicatorModelIt);
    setVector3(indicatorProgram_, "cameraPositionGlobal", camera->getPosition());

    int lightCount = 0;
    for ( long i = 0; i < lights.size(); i++ ) {
        Light* light = lights.get(i);
        if ( light == 0 ) {
            continue;
        }
        char name[64];
        std::snprintf(name, sizeof(name), "lightPositionsGlobal[%d]", lightCount);
        setVector3(indicatorProgram_, name, light->getPosition());
        std::snprintf(name, sizeof(name), "lightColorsGlobal[%d]", lightCount);
        setVector3(indicatorProgram_, name, light->getEmission());
        lightCount++;
    }
    setInt(indicatorProgram_, "numberOfLights", lightCount);
    setVector3(indicatorProgram_, "ambientColor", material.getAmbient());
    setVector3(indicatorProgram_, "diffuseColor", material.getDiffuse());
    setVector3(indicatorProgram_, "specularColor", material.getSpecular());
    setFloat(indicatorProgram_, "phongExponent", (float)material.getPhongExponent());
    setInt(indicatorProgram_, "withTexture", 0);
    setInt(indicatorProgram_, "withBumpMap", 0);
    setInt(indicatorProgram_, "withVertexColors", 0);

    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);

    glBindVertexArray(indicatorVao_);
    glDrawArrays(GL_TRIANGLES, 0, 3);
    glBindVertexArray(0);
    glUseProgram(0);

    (void)quality;
}

RendererConfiguration OpenGL4RayGizmoRenderer::buildSurfaceQuality()
{
    RendererConfiguration quality;
    quality.setSurfaces(true);
    quality.setWires(false);
    quality.setPoints(false);
    quality.setTexture(false);
    quality.setBumpMap(false);
    return quality;
}

SimpleMaterial OpenGL4RayGizmoRenderer::indicatorMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.3, 0.3, 0.0));
    m = m.withDiffuse(ColorRgb(1.0, 0.9, 0.0));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 0.8));
    m = m.withPhongExponent(64.0);
    return m;
}

void OpenGL4RayGizmoRenderer::uploadBuffer(unsigned int bufferId, int attrib, int size, const float* data, int count)
{
    glBindBuffer(GL_ARRAY_BUFFER, bufferId);
    glBufferData(GL_ARRAY_BUFFER, (GLsizeiptr)(count * sizeof(float)), data, GL_STATIC_DRAW);
    glEnableVertexAttribArray((GLuint)attrib);
    glVertexAttribPointer((GLuint)attrib, size, GL_FLOAT, GL_FALSE, 0, 0);
}

void OpenGL4RayGizmoRenderer::computeNormals(float normals[9])
{
    float ax = IND_INNER_R - IND_OUTER_R;
    float ay = -IND_HALF_W;
    float az = -IND_TIP_Z;
    float bx = IND_INNER_R - IND_OUTER_R;
    float by = IND_HALF_W;
    float bz = -IND_TIP_Z;
    float nx = ay * bz - az * by;
    float ny = az * bx - ax * bz;
    float nz = ax * by - ay * bx;
    float len = std::sqrt(nx * nx + ny * ny + nz * nz);
    nx /= len;
    ny /= len;
    nz /= len;

    for ( int i = 0; i < 3; i++ ) {
        normals[i * 3] = nx;
        normals[i * 3 + 1] = ny;
        normals[i * 3 + 2] = nz;
    }
}

unsigned int OpenGL4RayGizmoRenderer::buildProgram(const char* vsFile, const char* fsFile)
{
    java::String vsSource = findShaderSource(vsFile);
    java::String fsSource = findShaderSource(fsFile);
    if ( vsSource.empty() || fsSource.empty() ) {
        std::fprintf(stderr, "OpenGL4RayGizmoRenderer shader not found: %s / %s\n", vsFile, fsFile);
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
        std::fprintf(stderr, "OpenGL4RayGizmoRenderer link error: %s\n", log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

unsigned int OpenGL4RayGizmoRenderer::compileShader(unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, 0);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), 0, log);
        std::fprintf(stderr, "OpenGL4RayGizmoRenderer shader compile error: %s\n", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

java::String OpenGL4RayGizmoRenderer::readTextFile(const java::String& path)
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

java::String OpenGL4RayGizmoRenderer::findShaderSource(const java::String& shaderFileName)
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

void OpenGL4RayGizmoRenderer::setMatrix(unsigned int programId, const char* name, const Matrix4x4d& matrix)
{
    float* values = matrix.exportToFloatArrayColumnOrder();
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniformMatrix4fv(loc, 1, GL_FALSE, values);
    delete[] values;
}

void OpenGL4RayGizmoRenderer::setVector3(unsigned int programId, const char* name, const Vector3Dd& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
}

void OpenGL4RayGizmoRenderer::setVector3(unsigned int programId, const char* name, const ColorRgb& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.r(), (float)value.g(), (float)value.b());
}

void OpenGL4RayGizmoRenderer::setInt(unsigned int programId, const char* name, int value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1i(loc, value);
}

void OpenGL4RayGizmoRenderer::setFloat(unsigned int programId, const char* name, float value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1f(loc, value);
}
