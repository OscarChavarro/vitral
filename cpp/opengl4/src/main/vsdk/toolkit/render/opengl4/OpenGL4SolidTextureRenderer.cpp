#include <cstdio>
#include <vector>

#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMesh.h"
#include "vsdk/toolkit/environment/geometry/surface/TriangleMeshGroup.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/environment/scene/SimpleScene.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4CameraRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4SolidTextureRenderer.h"

OpenGL4SolidTextureRenderer::OpenGL4SolidTextureRenderer(
    const java::String& shaderDirectory)
    : vaoId(0), positionVboId(0), normalVboId(0), vertexCount(0),
      programId(0), solidTextureId(0), uploadedTextureRevision(-9223372036854775807L),
      uploadedTextureSize(0), shaderDirectory(shaderDirectory)
{
}

OpenGL4SolidTextureRenderer::~OpenGL4SolidTextureRenderer()
{
}

void OpenGL4SolidTextureRenderer::draw(
    SimpleScene* scene, Camera* camera, const java::ArrayList<Light*>& lights,
    const std::vector<unsigned char>& solidTextureVolumeRgb8,
    int solidTextureSize, long solidTextureRevision,
    InfinitePlane* clippingPlane)
{
    if ( scene == 0 || camera == 0 ) {
        return;
    }
    ensureBuffers();
    ensureProgram();
    ensureSolidTexture(solidTextureVolumeRgb8, solidTextureSize,
                       solidTextureRevision);
    if ( programId == 0 || solidTextureId == 0 ) {
        return;
    }

    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glDisable(GL_CULL_FACE);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    glUseProgram(programId);
    configureClippingPlane(clippingPlane);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_3D, solidTextureId);
    setInt(programId, "sSolidTexture", 0);
    setInt(programId, "numberOfLights", (int)(lights.size() < 8 ? lights.size() : 8));
    setFloat(programId, "phongExponent", 24.0f);
    for ( long i = 0; i < lights.size() && i < 8; i++ ) {
        Light* light = lights.get(i);
        if ( light != 0 ) {
            char name[96];
            std::snprintf(name, sizeof(name), "lightPositionsGlobal[%ld]", i);
            setVector3(programId, name, light->getPosition());
            std::snprintf(name, sizeof(name), "lightColorsGlobal[%ld]", i);
            setColor(programId, name, light->getEmission());
        }
    }
    setVector3(programId, "cameraPositionGlobal", camera->getPosition());

    java::ArrayList<SimpleBody*>& bodies = scene->getSimpleBodies();
    for ( long i = 0; i < bodies.size(); i++ ) {
        drawBody(bodies.get(i), camera);
    }

    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_3D, 0);
    glDisable(GL_CLIP_DISTANCE0);
    glUseProgram(0);
}

void OpenGL4SolidTextureRenderer::dispose()
{
    if ( solidTextureId != 0 ) glDeleteTextures(1, &solidTextureId);
    if ( positionVboId != 0 ) glDeleteBuffers(1, &positionVboId);
    if ( normalVboId != 0 ) glDeleteBuffers(1, &normalVboId);
    if ( vaoId != 0 ) glDeleteVertexArrays(1, &vaoId);
    if ( programId != 0 ) glDeleteProgram(programId);
    solidTextureId = 0;
    positionVboId = 0;
    normalVboId = 0;
    vaoId = 0;
    programId = 0;
}

void OpenGL4SolidTextureRenderer::drawBody(SimpleBody* body, Camera* camera)
{
    if ( body == 0 || body->getGeometry() == 0 ) {
        return;
    }
    Geometry* geometry = body->getGeometry();
    double* bounds = geometry->getMinMax();
    if ( bounds == 0 ) {
        return;
    }
    setVector3(programId, "boundingBoxMinObject",
               Vector3Dd(bounds[0], bounds[1], bounds[2]));
    setVector3(programId, "boundingBoxMaxObject",
               Vector3Dd(bounds[3], bounds[4], bounds[5]));
    delete[] bounds;

    Matrix4x4d modelMatrix = body->getTransformationMatrix();
    Matrix4x4d projection = OpenGL4CameraRenderer::activate(camera);
    Matrix4x4d mvp = projection.multiply(modelMatrix);
    Matrix4x4d modelIt = modelMatrix.invert().transpose();
    setMatrix(programId, "modelViewProjectionLocal", mvp);
    setMatrix(programId, "modelViewLocal", modelMatrix);
    setMatrix(programId, "modelViewITLocal", modelIt);

    TriangleMesh* mesh = dynamic_cast<TriangleMesh*>(geometry);
    TriangleMeshGroup* group = dynamic_cast<TriangleMeshGroup*>(geometry);
    if ( mesh != 0 ) {
        std::vector<float> p, n;
        if ( buildFrame(mesh, p, n) ) {
            uploadFrame(p, n);
            glBindVertexArray(vaoId);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
    }
    else if ( group != 0 ) {
        java::ArrayList<TriangleMesh>& meshes = group->getMeshes();
        for ( long i = 0; i < meshes.size(); i++ ) {
            std::vector<float> p, n;
            if ( buildFrame(&meshes[i], p, n) ) {
                uploadFrame(p, n);
                glBindVertexArray(vaoId);
                glDrawArrays(GL_TRIANGLES, 0, vertexCount);
            }
        }
    }
}

bool OpenGL4SolidTextureRenderer::buildFrame(
    TriangleMesh* mesh, std::vector<float>& positions,
    std::vector<float>& normals)
{
    if ( mesh == 0 ) return false;
    java::ArrayList<int>& indices = mesh->getTriangleIndexes();
    java::ArrayList<double>& vertices = mesh->getVertexPositions();
    if ( indices.size() == 0 || vertices.size() == 0 ) return false;
    java::ArrayList<double>& vertexNormals = mesh->getVertexNormals();
    bool hasNormals = vertexNormals.size() >= vertices.size();
    positions.reserve(indices.size() * 3);
    normals.reserve(indices.size() * 3);
    for ( long i = 0; i < indices.size(); i++ ) {
        int idx = indices.get(i);
        int vp = idx * 3;
        positions.push_back((float)vertices.get(vp));
        positions.push_back((float)vertices.get(vp + 1));
        positions.push_back((float)vertices.get(vp + 2));
        if ( hasNormals ) {
            normals.push_back((float)vertexNormals.get(vp));
            normals.push_back((float)vertexNormals.get(vp + 1));
            normals.push_back((float)vertexNormals.get(vp + 2));
        }
        else {
            normals.push_back(0.0f);
            normals.push_back(0.0f);
            normals.push_back(1.0f);
        }
    }
    return true;
}

void OpenGL4SolidTextureRenderer::uploadFrame(
    const std::vector<float>& positions, const std::vector<float>& normals)
{
    vertexCount = (int)positions.size() / 3;
    glBindVertexArray(vaoId);
    glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
    glBufferData(GL_ARRAY_BUFFER, positions.size() * sizeof(float),
                 positions.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
    glBufferData(GL_ARRAY_BUFFER, normals.size() * sizeof(float),
                 normals.data(), GL_STREAM_DRAW);
    glEnableVertexAttribArray(1);
    glVertexAttribPointer(1, 3, GL_FLOAT, GL_FALSE, 0, 0);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);
}

void OpenGL4SolidTextureRenderer::ensureBuffers()
{
    if ( vaoId != 0 ) return;
    glGenVertexArrays(1, &vaoId);
    glGenBuffers(1, &positionVboId);
    glGenBuffers(1, &normalVboId);
}

void OpenGL4SolidTextureRenderer::ensureProgram()
{
    if ( programId != 0 ) return;
    programId = buildProgram(shaderDirectory, "solidTextureVertexShader.glsl",
                             "solidTexturePixelShader.glsl");
}

void OpenGL4SolidTextureRenderer::ensureSolidTexture(
    const std::vector<unsigned char>& volume, int size, long revision)
{
    if ( volume.empty() || size <= 0 ) return;
    if ( solidTextureId != 0 && uploadedTextureRevision == revision &&
         uploadedTextureSize == size ) {
        return;
    }
    if ( solidTextureId == 0 ) {
        glGenTextures(1, &solidTextureId);
    }
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_3D, solidTextureId);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
    glTexImage3D(GL_TEXTURE_3D, 0, GL_RGB8, size, size, size, 0,
                 GL_RGB, GL_UNSIGNED_BYTE, volume.data());
    glBindTexture(GL_TEXTURE_3D, 0);
    uploadedTextureRevision = revision;
    uploadedTextureSize = size;
}

void OpenGL4SolidTextureRenderer::configureClippingPlane(
    InfinitePlane* clippingPlane)
{
    if ( clippingPlane == 0 ) {
        glDisable(GL_CLIP_DISTANCE0);
        setInt(programId, "clippingPlaneEnabled", 0);
        return;
    }
    glEnable(GL_CLIP_DISTANCE0);
    setInt(programId, "clippingPlaneEnabled", 1);
    setVector4(programId, "clippingPlaneGlobal",
               Vector4Dd(clippingPlane->getA(), clippingPlane->getB(),
                         clippingPlane->getC(), clippingPlane->getD()));
}

java::String OpenGL4SolidTextureRenderer::readTextFile(const java::String& path)
{
    FILE* f = std::fopen(path.c_str(), "rb");
    if ( f == 0 ) return java::String();
    std::fseek(f, 0, SEEK_END);
    long fileSize = std::ftell(f);
    if ( fileSize <= 0 ) { std::fclose(f); return java::String(); }
    std::fseek(f, 0, SEEK_SET);
    char* buffer = new char[(size_t)fileSize + 1]();
    size_t readSize = std::fread(buffer, 1, fileSize, f);
    std::fclose(f);
    buffer[readSize] = '\0';
    java::String result(buffer);
    delete[] buffer;
    return result;
}

unsigned int OpenGL4SolidTextureRenderer::compileShader(
    unsigned int type, const char* source)
{
    unsigned int shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, 0);
    glCompileShader(shader);
    int ok = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if ( !ok ) {
        char log[4096];
        glGetShaderInfoLog(shader, sizeof(log), 0, log);
        std::fprintf(stderr, "OpenGL4SolidTextureRenderer shader compile error: %s\n", log);
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

unsigned int OpenGL4SolidTextureRenderer::buildProgram(
    const java::String& shaderDirectory, const char* vsFile, const char* fsFile)
{
    java::String prefix = shaderDirectory;
    if ( !prefix.empty() && prefix[prefix.length() - 1] != '/' ) {
        prefix += "/";
    }
    java::String vsSource = readTextFile(prefix + vsFile);
    java::String fsSource = readTextFile(prefix + fsFile);
    if ( vsSource.empty() || fsSource.empty() ) {
        const char* candidates[] = {
            "../../../../etc/glslShaders/",
            "../../../etc/glslShaders/",
            "../../etc/glslShaders/",
            "../etc/glslShaders/",
            "etc/glslShaders/"
        };
        for ( size_t i = 0; i < sizeof(candidates) / sizeof(candidates[0]); i++ ) {
            java::String candidate(candidates[i]);
            vsSource = readTextFile(candidate + vsFile);
            fsSource = readTextFile(candidate + fsFile);
            if ( !vsSource.empty() && !fsSource.empty() ) break;
        }
    }
    if ( vsSource.empty() || fsSource.empty() ) {
        std::fprintf(stderr, "OpenGL4SolidTextureRenderer shader not found: %s / %s\n", vsFile, fsFile);
        return 0;
    }
    unsigned int vs = compileShader(GL_VERTEX_SHADER, vsSource.c_str());
    unsigned int fs = compileShader(GL_FRAGMENT_SHADER, fsSource.c_str());
    if ( vs == 0 || fs == 0 ) return 0;
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
        std::fprintf(stderr, "OpenGL4SolidTextureRenderer link error: %s\n", log);
        glDeleteProgram(program);
        return 0;
    }
    return program;
}

void OpenGL4SolidTextureRenderer::setMatrix(
    unsigned int programId, const char* name, const Matrix4x4d& matrix)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        float* m = matrix.exportToFloatArrayColumnOrder();
        glUniformMatrix4fv(loc, 1, GL_FALSE, m);
        delete[] m;
    }
}

void OpenGL4SolidTextureRenderer::setVector3(
    unsigned int programId, const char* name, const Vector3Dd& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
}

void OpenGL4SolidTextureRenderer::setColor(
    unsigned int programId, const char* name, const ColorRgb& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.r(), (float)value.g(), (float)value.b());
}

void OpenGL4SolidTextureRenderer::setVector4(
    unsigned int programId, const char* name, const Vector4Dd& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        glUniform4f(loc, (float)value.x(), (float)value.y(),
                    (float)value.z(), (float)value.w());
    }
}

void OpenGL4SolidTextureRenderer::setInt(
    unsigned int programId, const char* name, int value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1i(loc, value);
}

void OpenGL4SolidTextureRenderer::setFloat(
    unsigned int programId, const char* name, float value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1f(loc, value);
}
