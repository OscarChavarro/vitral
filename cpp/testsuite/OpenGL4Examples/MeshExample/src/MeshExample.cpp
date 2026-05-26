#include <cstdio>
#include <java/lang/Math.h>
#include <cstdlib>
#include <cmath>
#include "java/util/ArrayList.txx"

#include <GL/glew.h>
#define GLFW_INCLUDE_NONE
#include <GLFW/glfw3.h>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/light/LightType.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleBodyGroup.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/gui/CameraControllerOrbiter.h"
#include "vsdk/toolkit/gui/GlfwSystem.h"
#include "vsdk/toolkit/gui/LightGizmoStyle.h"
#include "vsdk/toolkit/gui/RendererConfigurationController.h"
#include "vsdk/toolkit/io/geometry/EnvironmentPersistence.h"
#include "java/io/File.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.h"

static const int WINDOW_WIDTH = 1024;
static const int WINDOW_HEIGHT = 768;
static const float SURFACE_POLYGON_OFFSET_FACTOR = 1.0f;
static const float SURFACE_POLYGON_OFFSET_UNITS = 1.0f;
static const float LINE_POLYGON_OFFSET_FACTOR = -1.0f;
static const float LINE_POLYGON_OFFSET_UNITS = -1.0f;

static java::String readTextFile(const java::String& path)
{
    FILE* f = fopen(path.c_str(), "rb");
    if (!f) return java::String();
    fseek(f, 0, SEEK_END);
    long fileSize = ftell(f);
    if (fileSize <= 0) { fclose(f); return java::String(); }
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
    for (size_t i = 0; i < sizeof(candidates)/sizeof(candidates[0]); i++) {
        java::String path = java::String(candidates[i]) + shaderFileName;
        java::String src = readTextFile(path);
        if (!src.empty()) return src;
    }
    return java::String();
}

static unsigned int compileShader(unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), nullptr, log);
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
    if (vsSource.empty() || fsSource.empty()) {
        std::fprintf(stderr, "MeshExample shader not found: %s / %s\n", vsFile, fsFile);
        return 0;
    }

    unsigned int vs = compileShader(GL_VERTEX_SHADER, vsSource.c_str());
    unsigned int fs = compileShader(GL_FRAGMENT_SHADER, fsSource.c_str());
    if (vs == 0 || fs == 0) {
        if (vs != 0) glDeleteShader(vs);
        if (fs != 0) glDeleteShader(fs);
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
    if (!ok) {
        char log[4096];
        glGetProgramInfoLog(program, sizeof(log), nullptr, log);
        std::fprintf(stderr, "MeshExample shader link error: %s\n", log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

static void setUniform3f(unsigned int program, const char* name, const Vector3Dd& v)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform3f(loc, (float)v.x(), (float)v.y(), (float)v.z());
}

static void setUniform3f(unsigned int program, const char* name, const ColorRgb& c)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform3f(loc, (float)c.r(), (float)c.g(), (float)c.b());
}

static void setUniform1i(unsigned int program, const char* name, int v)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform1i(loc, v);
}

static void setUniform1f(unsigned int program, const char* name, float v)
{
    GLint loc = glGetUniformLocation(program, name);
    if (loc >= 0) glUniform1f(loc, v);
}

static SimpleMaterial defaultMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.2, 0.2, 0.2));
    m = m.withDiffuse(ColorRgb(0.8, 0.8, 0.8));
    m = m.withSpecular(ColorRgb(1.0, 1.0, 1.0));
    m = m.withPhongExponent(32.0);
    return m;
}

static SimpleMaterial whiteWireMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.0, 0.0, 0.0));
    m = m.withDiffuse(ColorRgb(1.0, 1.0, 1.0));
    m = m.withSpecular(ColorRgb(0.0, 0.0, 0.0));
    m = m.withPhongExponent(1.0);
    return m;
}

static SimpleMaterial redPointMaterial()
{
    SimpleMaterial m;
    m = m.withAmbient(ColorRgb(0.0, 0.0, 0.0));
    m = m.withDiffuse(ColorRgb(1.0, 0.0, 0.0));
    m = m.withSpecular(ColorRgb(0.0, 0.0, 0.0));
    m = m.withPhongExponent(1.0);
    return m;
}

class MeshRenderer {
public:
    MeshRenderer() :
        vaoId(0), positionVboId(0), normalVboId(0), uvVboId(0), vertexCount(0),
        constantProgram(0), constantTexturedProgram(0), flatProgram(0), flatTexturedProgram(0),
        gouraudProgram(0), phongProgram(0), phongBumpProgram(0), cookProgram(0), cookBumpProgram(0) {}

    bool init()
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

        if (!constantProgram || !constantTexturedProgram || !flatProgram || !flatTexturedProgram ||
            !gouraudProgram || !phongProgram || !phongBumpProgram || !cookProgram || !cookBumpProgram) {
            return false;
        }

        glGenVertexArrays(1, &vaoId);
        glGenBuffers(1, &positionVboId);
        glGenBuffers(1, &normalVboId);
        glGenBuffers(1, &uvVboId);
        return true;
    }

    void dispose()
    {
        if (positionVboId != 0) glDeleteBuffers(1, &positionVboId);
        if (normalVboId != 0) glDeleteBuffers(1, &normalVboId);
        if (uvVboId != 0) glDeleteBuffers(1, &uvVboId);
        if (vaoId != 0) glDeleteVertexArrays(1, &vaoId);

        unsigned int programs[] = {
            constantProgram, constantTexturedProgram, flatProgram, flatTexturedProgram,
            gouraudProgram, phongProgram, phongBumpProgram, cookProgram, cookBumpProgram
        };
        for (size_t i = 0; i < sizeof(programs)/sizeof(programs[0]); i++) {
            if (programs[i] != 0) glDeleteProgram(programs[i]);
        }
    }

    void drawBody(SimpleBody* body, Camera* camera, const java::ArrayList<Light*>& lights, RendererConfiguration* quality)
    {
        if (body == nullptr || camera == nullptr || quality == nullptr) return;

        Geometry* geometry = body->getGeometry();
        if (geometry == nullptr) return;

        java::ArrayList<TriangleMesh*> meshes;
        TriangleMesh* tm = dynamic_cast<TriangleMesh*>(geometry);
        if (tm != nullptr) {
            meshes.add(tm);
        }
        else {
            TriangleMeshGroup* group = dynamic_cast<TriangleMeshGroup*>(geometry);
            if (group == nullptr) return;
            java::ArrayList<TriangleMesh>& groupMeshes = group->getMeshes();
            for (long int i = 0; i < groupMeshes.size(); i++) {
                meshes.add(&groupMeshes[i]);
            }
        }

        Matrix4x4d modelMatrix = body->getTransformationMatrix();
        Matrix4x4d projection = camera->calculateProjectionMatrix();
        Matrix4x4d modelViewProjection = projection.multiply(modelMatrix);
        Matrix4x4d modelIt = modelMatrix.invert().transpose();

        SimpleMaterial material = body->getMaterial() ? *(body->getMaterial()) : defaultMaterial();

        for (long int mi = 0; mi < meshes.size(); mi++) {
            TriangleMesh* mesh = meshes[mi];
            java::ArrayList<float> positions;
            java::ArrayList<float> normals;
            java::ArrayList<float> uvs;
            if (!buildFrame(mesh, positions, normals, uvs)) continue;

            uploadFrame(positions, normals, uvs);

            Image* texture = body->getTexture();
            if (texture == nullptr) {
                texture = mesh->getTextureAt(0);
            }

            int textureId = 0;
            bool withTexture = false;
            if (texture != nullptr && quality->isTextureSet()) {
                textureId = OpenGL4ImageRenderer::activate(texture);
                withTexture = textureId > 0;
            }

            if (quality->isSurfacesSet()) {
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

            if (quality->isWiresSet()) {
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

            if (quality->isPointsSet()) {
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

private:
    unsigned int vaoId;
    unsigned int positionVboId;
    unsigned int normalVboId;
    unsigned int uvVboId;
    int vertexCount;

    unsigned int constantProgram;
    unsigned int constantTexturedProgram;
    unsigned int flatProgram;
    unsigned int flatTexturedProgram;
    unsigned int gouraudProgram;
    unsigned int phongProgram;
    unsigned int phongBumpProgram;
    unsigned int cookProgram;
    unsigned int cookBumpProgram;

    unsigned int selectProgram(const RendererConfiguration* quality, bool hasTexture, bool hasNormalMap)
    {
        if (quality == nullptr) {
            return hasTexture ? constantTexturedProgram : constantProgram;
        }

        int shadingType = quality->getShadingType();
        if (shadingType == RendererConfiguration::SHADING_TYPE_NOLIGHT) {
            return (quality->isTextureSet() && hasTexture) ? constantTexturedProgram : constantProgram;
        }
        if (shadingType == RendererConfiguration::SHADING_TYPE_FLAT) {
            return (quality->isTextureSet() && hasTexture) ? flatTexturedProgram : flatProgram;
        }
        if (shadingType == RendererConfiguration::SHADING_TYPE_PHONG) {
            if (quality->isBumpMapSet() && hasNormalMap) return phongBumpProgram;
            return phongProgram;
        }
        if (shadingType == RendererConfiguration::SHADING_TYPE_COOK_TERRANCE) {
            if (quality->isBumpMapSet() && hasNormalMap) return cookBumpProgram;
            return cookProgram;
        }
        return gouraudProgram;
    }

    bool buildFrame(TriangleMesh* mesh, java::ArrayList<float>& outPositions, java::ArrayList<float>& outNormals, java::ArrayList<float>& outUvs)
    {
        if (mesh == nullptr) return false;

        java::ArrayList<int>& indices = mesh->getTriangleIndexes();
        java::ArrayList<double>& vertices = mesh->getVertexPositions();
        if (indices.size() == 0 || vertices.size() == 0) return false;

        java::ArrayList<double>& normals = mesh->getVertexNormals();
        java::ArrayList<double>& uvs = mesh->getVertexUvs();
        bool hasNormals = normals.size() >= vertices.size();
        bool hasUvs = (uvs.size() / 2) >= (vertices.size() / 3);

        outPositions.clear();
        outNormals.clear();
        outUvs.clear();
        outPositions.reserve((long int)indices.size() * 3);
        outNormals.reserve((long int)indices.size() * 3);
        outUvs.reserve((long int)indices.size() * 2);

        for (long int i = 0; i < indices.size(); i++) {
            int idx = indices[i];
            int vp = idx * 3;
            outPositions.add((float)vertices[(long int)vp]);
            outPositions.add((float)vertices[(long int)vp + 1]);
            outPositions.add((float)vertices[(long int)vp + 2]);

            if (hasNormals) {
                outNormals.add((float)normals[(long int)vp]);
                outNormals.add((float)normals[(long int)vp + 1]);
                outNormals.add((float)normals[(long int)vp + 2]);
            }
            else {
                outNormals.add(0.0f);
                outNormals.add(0.0f);
                outNormals.add(1.0f);
            }

            if (hasUvs) {
                int uv = idx * 2;
                outUvs.add((float)uvs[(long int)uv]);
                outUvs.add((float)uvs[(long int)uv + 1]);
            }
            else {
                outUvs.add(0.0f);
                outUvs.add(0.0f);
            }
        }

        return true;
    }

    void uploadFrame(java::ArrayList<float>& positions, java::ArrayList<float>& normals, java::ArrayList<float>& uvs)
    {
        vertexCount = (int)(positions.size() / 3);

        glBindVertexArray(vaoId);

        glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
        glBufferData(GL_ARRAY_BUFFER, positions.size() * sizeof(float), positions.data(), GL_STREAM_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

        glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
        glBufferData(GL_ARRAY_BUFFER, normals.size() * sizeof(float), normals.data(), GL_STREAM_DRAW);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, nullptr);

        glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
        glBufferData(GL_ARRAY_BUFFER, uvs.size() * sizeof(float), uvs.data(), GL_STREAM_DRAW);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, GL_FALSE, 0, nullptr);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    void configureProgram(
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
        if (mvpLoc >= 0) glUniformMatrix4fv(mvpLoc, 1, GL_FALSE, mvp);

        GLint modelLoc = glGetUniformLocation(programId, "modelViewLocal");
        if (modelLoc >= 0) glUniformMatrix4fv(modelLoc, 1, GL_FALSE, modelM);

        GLint modelItLoc = glGetUniformLocation(programId, "modelViewITLocal");
        if (modelItLoc >= 0) glUniformMatrix4fv(modelItLoc, 1, GL_FALSE, modelItM);

        setUniform3f(programId, "cameraPositionGlobal", camera->getPosition());

        int lightCount = 0;
        for (long int i = 0; i < lights.size(); i++) {
            Light* light = lights.get(i);
            if (light == nullptr) continue;
            char pName[64];
            char cName[64];
            snprintf(pName, sizeof(pName), "lightPositionsGlobal[%d]", lightCount);
            snprintf(cName, sizeof(cName), "lightColorsGlobal[%d]", lightCount);
            setUniform3f(programId, pName, light->getPosition());
            setUniform3f(programId, cName, light->getSpecular());
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

        if (withTexture) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, (unsigned int)textureId);
            setUniform1i(programId, "sTexture", 0);
        }

        delete[] mvp;
        delete[] modelM;
        delete[] modelItM;
    }
};

class MeshModel {
public:
    MeshModel() : scene(), camera(), qualitySelection()
    {
        Light* light0 = new Light(LightType::POINT, Vector3Dd(10, -20, 50), ColorRgb(1, 1, 1));
        light0->setId(0);
        Light* light1 = new Light(LightType::POINT, Vector3Dd(-10, 20, 50), ColorRgb(1, 1, 1));
        light1->setId(1);
        lights.add(light0);
        lights.add(light1);
    }

    ~MeshModel()
    {
        for (long int i = 0; i < lights.size(); i++) {
            delete lights[i];
        }
        lights.clear();
    }

    void configureInitialViewAndLightToScene()
    {
        java::ArrayList<SimpleBody*>& bodies = scene.getSimpleBodies();
        if (bodies.size() == 0) return;

        SimpleBodyGroup group;
        java::ArrayList<SimpleBody*>& groupBodies = group.getBodies();
        for (long int i = 0; i < bodies.size(); i++) {
            groupBodies.add(bodies[i]);
        }

        double* minmax = group.getMinMax();
        if (minmax == nullptr) return;

        Vector3Dd min(minmax[0], minmax[1], minmax[2]);
        Vector3Dd max(minmax[3], minmax[4], minmax[5]);
        delete[] minmax;

        Vector3Dd center = min.add(max).multiply(0.5);
        double radius = max.subtract(min).length() * 0.5;
        if (radius < 0.001) radius = 1.0;

        double fovRad = camera.getFov() * M_PI / 180.0;
        double viewDistance = (radius / std::tan(fovRad * 0.5)) * 1.35;
        if (viewDistance < radius * 1.5) viewDistance = radius * 1.5;

        Vector3Dd eyeDirection = Vector3Dd(0, -1, 0.35).normalized();
        Vector3Dd eye = center.add(eyeDirection.multiply(viewDistance));
        camera.setPosition(eye);
        camera.setUpMaintainingOrthogonality(Vector3Dd(0, 0, 1));
        camera.setFocusedPositionMaintainingOrthogonality(center);

        double nearPlane = java::Math::max(0.01, viewDistance - (radius * 2.2));
        double farPlane = java::Math::max(nearPlane + 1.0, viewDistance + (radius * 4.0));
        camera.setNearPlaneDistance(nearPlane);
        camera.setFarPlaneDistance(farPlane);
        camera.updateVectors();

        Vector3Dd lightDirection = Vector3Dd(1, -1, 1).normalized();
        Vector3Dd lightPos0 = center.add(lightDirection.multiply(radius * 3.0));
        Vector3Dd lightPos1 = center.add(Vector3Dd(-lightDirection.x(), -lightDirection.y(), lightDirection.z()).normalized().multiply(radius * 3.0));

        if (lights.size() > 0 && lights[0] != nullptr) lights[0]->setPosition(lightPos0);
        if (lights.size() > 1 && lights[1] != nullptr) lights[1]->setPosition(lightPos1);
    }

    SimpleScene scene;
    Camera camera;
    RendererConfiguration qualitySelection;
    java::ArrayList<Light*> lights;
};

class MeshExampleApp {
public:
    MeshExampleApp() : window(nullptr), cameraController(nullptr), qualityController(nullptr), shouldClose(false) {}

    ~MeshExampleApp()
    {
        cleanup();
    }

    bool init(const char* fileName)
    {
        if (!glfwInit()) {
            std::fprintf(stderr, "Failed to initialize GLFW\n");
            return false;
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "VITRAL mesh test - OpenGL4", nullptr, nullptr);
        if (window == nullptr) {
            std::fprintf(stderr, "Failed to create GLFW window\n");
            glfwTerminate();
            return false;
        }

        glfwMakeContextCurrent(window);
        glfwSetWindowUserPointer(window, this);
        glfwSwapInterval(1);

        glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);
        glfwSetKeyCallback(window, keyCallback);
        glfwSetMouseButtonCallback(window, mouseButtonCallback);
        glfwSetCursorPosCallback(window, cursorPosCallback);
        glfwSetScrollCallback(window, scrollCallback);

        glewExperimental = GL_TRUE;
        GLenum err = glewInit();
        if (err != GLEW_OK) {
            std::fprintf(stderr, "Failed to initialize GLEW: %s\n", glewGetErrorString(err));
            return false;
        }

        if (!loadScene(fileName)) {
            return false;
        }

        model.camera.updateViewportResize(WINDOW_WIDTH, WINDOW_HEIGHT);
        cameraController = new CameraControllerOrbiter(&model.camera);
        qualityController = new RendererConfigurationController(&model.qualitySelection);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE);
        glClearColor(0.5f, 0.5f, 0.9f, 1.0f);

        if (!renderer.init()) {
            std::fprintf(stderr, "Failed to initialize mesh renderer\n");
            return false;
        }

        return true;
    }

    void run()
    {
        while (!glfwWindowShouldClose(window) && !shouldClose) {
            glfwPollEvents();
            draw();
            glfwSwapBuffers(window);
        }
    }

private:
    GLFWwindow* window;
    MeshModel model;
    CameraControllerOrbiter* cameraController;
    RendererConfigurationController* qualityController;
    MeshRenderer renderer;
    bool shouldClose;

    bool loadScene(const char* fileName)
    {
        if (fileName == nullptr) {
            std::fprintf(stderr, "File not specified\n");
            return false;
        }

        java::File file(fileName);
        if (!file.exists() || !file.canRead()) {
            std::fprintf(stderr, "Failed to read file: %s\n", fileName);
            return false;
        }

        try {
            EnvironmentPersistence::importEnvironment(file, &model.scene);
            model.configureInitialViewAndLightToScene();
        }
        catch (...) {
            std::fprintf(stderr, "Failed to import scene\n");
            return false;
        }

        return true;
    }

    void draw()
    {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        for (long int i = 0; i < model.scene.getSimpleBodies().size(); i++) {
            renderer.drawBody(model.scene.getSimpleBodies()[i], &model.camera, model.lights, &model.qualitySelection);
        }

        for (size_t i = 0; i < model.lights.size(); i++) {
            if (model.lights[i] == nullptr) continue;
            vsdk::toolkit::render::opengl4::OpenGL4LightRenderer::draw(
                model.lights[i],
                &model.camera,
                LightGizmoStyle::OMNI_BILLBOARD);
        }
    }

    void cleanup()
    {
        renderer.dispose();
        vsdk::toolkit::render::opengl4::OpenGL4LightRenderer::dispose();
        OpenGL4ImageRenderer::dispose();

        if (qualityController != nullptr) {
            delete qualityController;
            qualityController = nullptr;
        }
        if (cameraController != nullptr) {
            delete cameraController;
            cameraController = nullptr;
        }

        if (window != nullptr) {
            glfwDestroyWindow(window);
            window = nullptr;
        }
        glfwTerminate();
    }

    static void framebufferSizeCallback(GLFWwindow* win, int width, int height)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if (app == nullptr) return;
        glViewport(0, 0, width, height);
        app->model.camera.updateViewportResize(width, height);
    }

    static void keyCallback(GLFWwindow* win, int key, int, int action, int mods)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if (app == nullptr) return;

        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            if (key == GLFW_KEY_ESCAPE) {
                app->shouldClose = true;
                return;
            }

            KeyEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkKeyEvent(key, mods);
            if (app->cameraController != nullptr) {
                app->cameraController->processKeyPressedEvent(event);
            }
            if (app->qualityController != nullptr) {
                if (app->qualityController->processKeyPressedEvent(event)) {
                    std::fprintf(stdout, "%s\n", app->model.qualitySelection.toString().c_str());
                }
            }

            if (key == GLFW_KEY_I) {
                std::fprintf(stdout, "%s\n", app->model.qualitySelection.toString().c_str());
            }
        }
        else if (action == GLFW_RELEASE) {
            KeyEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkKeyEvent(key, mods);
            if (app->cameraController != nullptr) {
                app->cameraController->processKeyReleasedEvent(event);
            }
            if (app->qualityController != nullptr) {
                app->qualityController->processKeyReleasedEvent(event);
            }
        }
    }

    static void mouseButtonCallback(GLFWwindow* win, int button, int action, int)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if (app == nullptr || app->cameraController == nullptr) return;

        double xpos = 0.0;
        double ypos = 0.0;
        glfwGetCursorPos(win, &xpos, &ypos);

        MouseEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkMouseEvent(button, action, xpos, ypos);
        if (action == GLFW_PRESS) {
            app->cameraController->processMousePressedEvent(event);
        }
        else if (action == GLFW_RELEASE) {
            app->cameraController->processMouseReleasedEvent(event);
        }
    }

    static void cursorPosCallback(GLFWwindow* win, double xpos, double ypos)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if (app == nullptr || app->cameraController == nullptr) return;

        MouseEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkMotionEvent(xpos, ypos);

        int leftButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_LEFT);
        int middleButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_MIDDLE);
        int rightButton = glfwGetMouseButton(win, GLFW_MOUSE_BUTTON_RIGHT);

        int modifiers = 0;
        if (leftButton == GLFW_PRESS) modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
        if (middleButton == GLFW_PRESS) modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
        if (rightButton == GLFW_PRESS) modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
        event.setModifiers(modifiers);

        if (leftButton == GLFW_PRESS || middleButton == GLFW_PRESS || rightButton == GLFW_PRESS) {
            app->cameraController->processMouseDraggedEvent(event);
        }
        else {
            app->cameraController->processMouseMovedEvent(event);
        }
    }

    static void scrollCallback(GLFWwindow* win, double xoffset, double yoffset)
    {
        MeshExampleApp* app = static_cast<MeshExampleApp*>(glfwGetWindowUserPointer(win));
        if (app == nullptr || app->cameraController == nullptr) return;

        MouseEvent event = vsdk::toolkit::gui::GlfwSystem::glfw2vsdkWheelEvent(xoffset, yoffset);
        app->cameraController->processMouseWheelEvent(event);
    }
};

int main(int argc, char** argv)
{
    const char* fileName = (argc >= 2) ? argv[1] : nullptr;

    MeshExampleApp app;
    if (!app.init(fileName)) {
        return 1;
    }

    app.run();
    return 0;
}
