#include <cstdio>

#include "java/util/ArrayList.txx"
#include <GL/glew.h>
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RayGizmoRenderer.h"
#include "model/MeshModel.h"
#include "render/Jogl4DebuggerRenderer.h"

const float Jogl4DebuggerRenderer::SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
const float Jogl4DebuggerRenderer::SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
const float Jogl4DebuggerRenderer::LINE_POLYGON_OFFSET_FACTOR = -1.0f;
const float Jogl4DebuggerRenderer::LINE_POLYGON_OFFSET_UNITS = -1.0f;

static java::String readTextFile(const java::String& path)
{
    FILE* f = fopen(path.c_str(), "rb");
    if ( !f ) return java::String();
    fseek(f, 0, SEEK_END);
    long fileSize = ftell(f);
    if ( fileSize <= 0 ) { fclose(f); return java::String(); }
    fseek(f, 0, SEEK_SET);
    char* buffer = new char[(size_t)fileSize + 1]();
    size_t readSize = fread(buffer, 1, fileSize, f);
    fclose(f);
    buffer[readSize] = '\0';
    java::String result(buffer);
    delete[] buffer;
    return result;
}

static java::String findShaderSource(const java::String& shaderFileName)
{
    const char* candidates[] = {
        "../../../../etc/glslShaders/",
        "../../../etc/glslShaders/",
        "../../etc/glslShaders/",
        "../etc/glslShaders/",
        "etc/glslShaders/"
    };
    for ( size_t i = 0; i < sizeof(candidates) / sizeof(candidates[0]); i++ ) {
        java::String path = java::String(candidates[i]) + shaderFileName;
        java::String src = readTextFile(path);
        if ( !src.empty() ) return src;
    }
    return java::String();
}

static unsigned int compileShader(unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, 0);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), 0, log);
        std::fprintf(stderr, "MeshExample shader compile error: %s\n", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

static unsigned int buildProgram(const char* vsFile, const char* fsFile)
{
    java::String vsSource = findShaderSource(vsFile);
    java::String fsSource = findShaderSource(fsFile);
    if ( vsSource.empty() || fsSource.empty() ) {
        std::fprintf(stderr, "MeshExample shader not found: %s / %s\n", vsFile, fsFile);
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
        std::fprintf(stderr, "MeshExample shader link error: %s\n", log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

static void setUniform3f(unsigned int program, const char* name, const Vector3Dd& v)
{
    GLint loc = glGetUniformLocation(program, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)v.x(), (float)v.y(), (float)v.z());
}

static void setUniform3f(unsigned int program, const char* name, const ColorRgb& c)
{
    GLint loc = glGetUniformLocation(program, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)c.r(), (float)c.g(), (float)c.b());
}

static void setUniform1i(unsigned int program, const char* name, int v)
{
    GLint loc = glGetUniformLocation(program, name);
    if ( loc >= 0 ) glUniform1i(loc, v);
}

static void setUniform1f(unsigned int program, const char* name, float v)
{
    GLint loc = glGetUniformLocation(program, name);
    if ( loc >= 0 ) glUniform1f(loc, v);
}

Jogl4DebuggerRenderer::Jogl4DebuggerRenderer(MeshModel* model)
    : model(model),
      vaoId(0), positionVboId(0), normalVboId(0), uvVboId(0), vertexCount(0),
      constantProgram(0), constantTexturedProgram(0), flatProgram(0), flatTexturedProgram(0),
      gouraudProgram(0), phongProgram(0), phongBumpProgram(0), cookProgram(0), cookBumpProgram(0)
{
}

bool Jogl4DebuggerRenderer::init()
{
    constantProgram = buildProgram("constantVertexShader.glsl", "constantPixelShader.glsl");
    constantTexturedProgram = buildProgram("constantTextureVertexShader.glsl", "constantTexturePixelShader.glsl");
    flatProgram = buildProgram("flatVertexShader.glsl", "flatPixelShader.glsl");
    flatTexturedProgram = buildProgram("flatTexturedVertexShader.glsl", "flatTexturedPixelShader.glsl");
    gouraudProgram = buildProgram("gouraudTextureVertexShader.glsl", "gouraudTexturePixelShader.glsl");
    phongProgram = buildProgram("phongTextureVertexShader.glsl", "phongTexturePixelShader.glsl");
    phongBumpProgram = buildProgram("phongTextureBumpVertexShader.glsl", "phongTextureBumpPixelShader.glsl");
    cookProgram = buildProgram("phongTextureVertexShader.glsl", "cookTexturePixelShader.glsl");
    cookBumpProgram = buildProgram("phongTextureBumpVertexShader.glsl", "cookTextureBumpPixelShader.glsl");

    if ( !constantProgram || !constantTexturedProgram || !flatProgram || !flatTexturedProgram ||
         !gouraudProgram || !phongProgram || !phongBumpProgram || !cookProgram || !cookBumpProgram ) {
        return false;
    }

    glGenVertexArrays(1, &vaoId);
    glGenBuffers(1, &positionVboId);
    glGenBuffers(1, &normalVboId);
    glGenBuffers(1, &uvVboId);
    return true;
}

void Jogl4DebuggerRenderer::display()
{
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);

    glClearColor(0.5f, 0.5f, 0.9f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    java::ArrayList<Light*>& activeLights = model->getLights();
    if ( activeLights.size() == 0 ) {
        return;
    }

    java::ArrayList<SimpleBody*>& bodies = model->getScene()->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        drawSimpleBody(bodies.get(i), model->getCamera(), activeLights, model->getQualitySelection());
    }

    for ( long i = 0; i < activeLights.size(); i++ ) {
        if ( activeLights.get(i) != 0 ) {
            OpenGL4LightRenderer::draw(activeLights.get(i), model->getCamera(), LightGizmoStyle::OMNI_BILLBOARD);
        }
    }

    model->getRayGizmo()->acquireSnapshot();
    OpenGL4RayGizmoRenderer::draw(model->getRayGizmo(), model->getCamera(), activeLights);
}

void Jogl4DebuggerRenderer::reshape(int width, int height)
{
    glViewport(0, 0, width, height);
    model->getCamera()->updateViewportResize(width, height);
}

void Jogl4DebuggerRenderer::dispose()
{
    if ( positionVboId != 0 ) glDeleteBuffers(1, &positionVboId);
    if ( normalVboId != 0 ) glDeleteBuffers(1, &normalVboId);
    if ( uvVboId != 0 ) glDeleteBuffers(1, &uvVboId);
    if ( vaoId != 0 ) glDeleteVertexArrays(1, &vaoId);

    unsigned int programs[] = {
        constantProgram, constantTexturedProgram, flatProgram, flatTexturedProgram,
        gouraudProgram, phongProgram, phongBumpProgram, cookProgram, cookBumpProgram
    };
    for ( size_t i = 0; i < sizeof(programs) / sizeof(programs[0]); i++ ) {
        if ( programs[i] != 0 ) glDeleteProgram(programs[i]);
    }
    OpenGL4RayGizmoRenderer::dispose();
    OpenGL4LightRenderer::dispose();
    OpenGL4ImageRenderer::dispose();
}

void Jogl4DebuggerRenderer::drawSimpleBody(SimpleBody* body, Camera* camera, const java::ArrayList<Light*>& lights, RendererConfiguration* quality)
{
    if ( body == 0 || camera == 0 || quality == 0 ) return;

    Geometry* geometry = body->getGeometry();
    if ( geometry == 0 ) return;

    java::ArrayList<TriangleMesh*> meshes;
    TriangleMesh* tm = dynamic_cast<TriangleMesh*>(geometry);
    if ( tm != 0 ) {
        meshes.add(tm);
    }
    else {
        TriangleMeshGroup* group = dynamic_cast<TriangleMeshGroup*>(geometry);
        if ( group == 0 ) return;
        java::ArrayList<TriangleMesh>& groupMeshes = group->getMeshes();
        for ( long i = 0; i < groupMeshes.size(); i++ ) {
            meshes.add(&groupMeshes[i]);
        }
    }

    Matrix4x4d modelMatrix = body->getTransformationMatrix();
    Matrix4x4d projection = camera->calculateProjectionMatrix();
    Matrix4x4d modelViewProjection = projection.multiply(modelMatrix);
    Matrix4x4d modelIt = modelMatrix.invert().transpose();

    SimpleMaterial material = body->getMaterial() != 0 ? *(body->getMaterial()) : defaultMaterial();

    for ( long mi = 0; mi < meshes.size(); mi++ ) {
        TriangleMesh* mesh = meshes.get(mi);
        java::ArrayList<float> positions;
        java::ArrayList<float> normals;
        java::ArrayList<float> uvs;
        if ( !buildFrame(mesh, positions, normals, uvs) ) continue;

        uploadFrame(positions, normals, uvs);

        Image* texture = body->getTexture();
        if ( texture == 0 ) {
            texture = mesh->getTextureAt(0);
        }

        int textureId = 0;
        bool withTexture = false;
        if ( texture != 0 && quality->isTextureSet() ) {
            textureId = OpenGL4ImageRenderer::activate(texture);
            withTexture = textureId > 0;
        }

        if ( quality->isSurfacesSet() ) {
            unsigned int program = selectProgram(quality, withTexture, false);
            configureProgram(program, modelViewProjection, modelMatrix, modelIt, camera, lights,
                material, quality, withTexture, textureId);

            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(SURFACE_POLYGON_OFFSET_FACTOR, SURFACE_POLYGON_OFFSET_UNITS);
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glDepthMask(GL_TRUE);
            glDepthFunc(GL_LESS);
            glBindVertexArray(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glBindVertexArray(0);
            glDisable(GL_POLYGON_OFFSET_FILL);
            glUseProgram(0);
        }

        if ( quality->isWiresSet() ) {
            RendererConfiguration wireQuality;
            wireQuality.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);
            wireQuality.setTexture(false);
            wireQuality.setBumpMap(false);
            unsigned int program = selectProgram(&wireQuality, false, false);
            configureProgram(program, modelViewProjection, modelMatrix, modelIt, camera, lights,
                whiteWireMaterial(), &wireQuality, false, 0);

            glEnable(GL_POLYGON_OFFSET_LINE);
            glPolygonOffset(LINE_POLYGON_OFFSET_FACTOR, LINE_POLYGON_OFFSET_UNITS);
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            glLineWidth(1.0f);
            glDepthMask(GL_FALSE);
            glDepthFunc(GL_LEQUAL);
            glBindVertexArray(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            glBindVertexArray(0);
            glDisable(GL_POLYGON_OFFSET_LINE);
            glUseProgram(0);
        }

        if ( quality->isPointsSet() ) {
            RendererConfiguration pointQuality;
            pointQuality.setShadingType(RendererConfiguration::SHADING_TYPE_NOLIGHT);
            pointQuality.setTexture(false);
            pointQuality.setBumpMap(false);
            unsigned int program = selectProgram(&pointQuality, false, false);
            configureProgram(program, modelViewProjection, modelMatrix, modelIt, camera, lights,
                redPointMaterial(), &pointQuality, false, 0);

            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
            glPointSize(4.0f);
            glDepthMask(GL_FALSE);
            glDepthFunc(GL_LEQUAL);
            glBindVertexArray(vaoId);
            glDrawArrays(GL_POINTS, 0, vertexCount);
            glBindVertexArray(0);
            glUseProgram(0);
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glDepthMask(GL_TRUE);
        glDepthFunc(GL_LESS);
    }
}

unsigned int Jogl4DebuggerRenderer::selectProgram(RendererConfiguration* quality, bool hasTexture, bool hasNormalMap)
{
    if ( quality == 0 ) {
        return hasTexture ? constantTexturedProgram : constantProgram;
    }

    int shadingType = quality->getShadingType();
    if ( shadingType == RendererConfiguration::SHADING_TYPE_NOLIGHT ) {
        return (quality->isTextureSet() && hasTexture) ? constantTexturedProgram : constantProgram;
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_FLAT ) {
        return (quality->isTextureSet() && hasTexture) ? flatTexturedProgram : flatProgram;
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_PHONG ) {
        if ( quality->isBumpMapSet() && hasNormalMap ) return phongBumpProgram;
        return phongProgram;
    }
    if ( shadingType == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE ) {
        if ( quality->isBumpMapSet() && hasNormalMap ) return cookBumpProgram;
        return cookProgram;
    }
    return gouraudProgram;
}

bool Jogl4DebuggerRenderer::buildFrame(TriangleMesh* mesh, java::ArrayList<float>& outPositions, java::ArrayList<float>& outNormals, java::ArrayList<float>& outUvs)
{
    if ( mesh == 0 ) return false;

    java::ArrayList<int>& indices = mesh->getTriangleIndexes();
    java::ArrayList<double>& vertices = mesh->getVertexPositions();
    if ( indices.size() == 0 || vertices.size() == 0 ) return false;

    java::ArrayList<double>& normals = mesh->getVertexNormals();
    java::ArrayList<double>& uvs = mesh->getVertexUvs();
    bool hasNormals = normals.size() >= vertices.size();
    bool hasUvs = (uvs.size() / 2) >= (vertices.size() / 3);

    outPositions.clear();
    outNormals.clear();
    outUvs.clear();
    outPositions.reserve(indices.size() * 3);
    outNormals.reserve(indices.size() * 3);
    outUvs.reserve(indices.size() * 2);

    for ( long i = 0; i < indices.size(); i++ ) {
        int idx = indices.get(i);
        int vp = idx * 3;
        outPositions.add((float)vertices.get(vp));
        outPositions.add((float)vertices.get(vp + 1));
        outPositions.add((float)vertices.get(vp + 2));

        if ( hasNormals ) {
            outNormals.add((float)normals.get(vp));
            outNormals.add((float)normals.get(vp + 1));
            outNormals.add((float)normals.get(vp + 2));
        }
        else {
            outNormals.add(0.0f);
            outNormals.add(0.0f);
            outNormals.add(1.0f);
        }

        if ( hasUvs ) {
            int uv = idx * 2;
            outUvs.add((float)uvs.get(uv));
            outUvs.add((float)uvs.get(uv + 1));
        }
        else {
            outUvs.add(0.0f);
            outUvs.add(0.0f);
        }
    }

    return true;
}

void Jogl4DebuggerRenderer::uploadFrame(java::ArrayList<float>& positions, java::ArrayList<float>& normals, java::ArrayList<float>& uvs)
{
    vertexCount = (int)(positions.size() / 3);

    glBindVertexArray(vaoId);

    glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
    glBufferData(GL_ARRAY_BUFFER, positions.size() * sizeof(float), positions.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, 0);

    glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
    glBufferData(GL_ARRAY_BUFFER, normals.size() * sizeof(float), normals.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, 0);

    glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
    glBufferData(GL_ARRAY_BUFFER, uvs.size() * sizeof(float), uvs.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(2);
    glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, 0);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
}

void Jogl4DebuggerRenderer::configureProgram(
    unsigned int programId,
    const Matrix4x4d& modelViewProjection,
    const Matrix4x4d& model,
    const Matrix4x4d& modelIt,
    Camera* camera,
    const java::ArrayList<Light*>& lights,
    const SimpleMaterial& material,
    RendererConfiguration* quality,
    bool withTexture,
    int textureId)
{
    float* mvp = modelViewProjection.exportToFloatArrayColumnOrder();
    float* modelM = model.exportToFloatArrayColumnOrder();
    float* modelItM = modelIt.exportToFloatArrayColumnOrder();

    glUseProgram(programId);

    GLint mvpLoc = glGetUniformLocation(programId, "modelViewProjectionLocal");
    if ( mvpLoc >= 0 ) glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, mvp);

    GLint modelLoc = glGetUniformLocation(programId, "modelViewLocal");
    if ( modelLoc >= 0 ) glUniformMatrix4fv(modelLoc, 1, GL_FALSE, modelM);

    GLint modelItLoc = glGetUniformLocation(programId, "modelViewITLocal");
    if ( modelItLoc >= 0 ) glUniformMatrix4fv(modelItLoc, 1, GL_FALSE, modelItM);

    setUniform3f(programId, "cameraPositionGlobal", camera->getPosition());

    int lightCount = 0;
    for ( long i = 0; i < lights.size(); i++ ) {
        Light* light = lights.get(i);
        if ( light == 0 ) continue;
        char pName[64];
        char cName[64];
        std::snprintf(pName, sizeof(pName), "lightPositionsGlobal[%d]", lightCount);
        std::snprintf(cName, sizeof(cName), "lightColorsGlobal[%d]", lightCount);
        setUniform3f(programId, pName, light->getPosition());
        setUniform3f(programId, cName, light->getEmission());
        lightCount++;
    }

    setUniform1i(programId, "numberOfLights", lightCount);
    setUniform3f(programId, "ambientColor", material.getAmbient());
    setUniform3f(programId, "diffuseColor", material.getDiffuse());
    setUniform3f(programId, "specularColor", material.getSpecular());
    setUniform1f(programId, "phongExponent", (float)material.getPhongExponent());
    setUniform1i(programId, "withTexture", withTexture ? 1 : 0);
    setUniform1i(programId, "withBumpMap", 0);
    setUniform1i(programId, "withVertexColors", quality->getUseVertexColors() ? 1 : 0);

    if ( withTexture ) {
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, (unsigned int)textureId);
        setUniform1i(programId, "sTexture", 0);
    }

    delete[] mvp;
    delete[] modelM;
    delete[] modelItM;
}

SimpleMaterial Jogl4DebuggerRenderer::defaultMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.8, 0.8, 0.8));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 1.0));
    m = m.withPhongExponent(32.0);
    return m;
}

SimpleMaterial Jogl4DebuggerRenderer::whiteWireMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.0, 0.0, 0.0));
    m = m.withDiffuse(ColorRgb(1.0, 1.0, 1.0));
    m = m.withSpecular(ColorRgb(0.0, 0.0, 0.0));
    m = m.withPhongExponent(1.0);
    return m;
}

SimpleMaterial Jogl4DebuggerRenderer::redPointMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.0, 0.0, 0.0));
    m = m.withDiffuse(ColorRgb(1.0, 0.0, 0.0));
    m = m.withSpecular(ColorRgb(0.0, 0.0, 0.0));
    m = m.withPhongExponent(1.0);
    return m;
}
