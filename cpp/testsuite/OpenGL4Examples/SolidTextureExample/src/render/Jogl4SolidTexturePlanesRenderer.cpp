#include <vector>

#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector4Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/media/Image.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4CameraRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "render/Jogl4SolidTexturePlanesRenderer.h"

static void setMatrix(unsigned int programId, const char* name, const Matrix4x4d& matrix)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) {
        float* m = matrix.exportToFloatArrayColumnOrder();
        glUniformMatrix4fv(loc, 1, GL_FALSE, m);
        delete[] m;
    }
}

static void setVector3(unsigned int programId, const char* name, const Vector3Dd& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform3f(loc, (float)value.x(), (float)value.y(), (float)value.z());
}

static void setVector4(unsigned int programId, const char* name, const Vector4Dd& value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform4f(loc, (float)value.x(), (float)value.y(), (float)value.z(), (float)value.w());
}

static void setInt(unsigned int programId, const char* name, int value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1i(loc, value);
}

static void setFloat(unsigned int programId, const char* name, float value)
{
    int loc = glGetUniformLocation(programId, name);
    if ( loc >= 0 ) glUniform1f(loc, value);
}

Jogl4SolidTexturePlanesRenderer::Jogl4SolidTexturePlanesRenderer()
    : vaoId(0), positionVboId(0), normalVboId(0), uvVboId(0)
{
}

void Jogl4SolidTexturePlanesRenderer::draw(
    java::ArrayList<Image*>& images, Camera* camera, InfinitePlane* clippingPlane)
{
    if ( images.size() == 0 || camera == 0 ) return;
    ensureBuffers();
    std::vector<float> positions, normals, uvs;
    buildPlaneFrame((int)images.size(), positions, normals, uvs);
    uploadFrame(positions, normals, uvs);

    RendererConfiguration quality;
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_FLAT);
    quality.setTexture(true);
    quality.setBumpMap(false);
    unsigned int program =
        OpenGL4RendererConfigurationShaderSelector::selectSurfaceShaderProgram(
            &quality, true, false);
    Matrix4x4d identity = Matrix4x4d::identityMatrix();
    Matrix4x4d mvp = OpenGL4CameraRenderer::activate(camera);
    OpenGL4RendererConfigurationShaderSelector::activateShader(
        program, mvp, &quality, 1.0f, 1.0f, 1.0f);
    setMatrix(program, "modelViewLocal", identity);
    setMatrix(program, "modelViewITLocal", identity);
    setVector3(program, "cameraPositionGlobal", camera->getPosition());
    setVector3(program, "lightPositionsGlobal[0]", Vector3Dd(0, 0, 5));
    setVector3(program, "lightColorsGlobal[0]", Vector3Dd(1, 1, 1));
    setVector3(program, "ambientColor", Vector3Dd(1, 1, 1));
    setVector3(program, "diffuseColor", Vector3Dd(1, 1, 1));
    setVector3(program, "specularColor", Vector3Dd(0, 0, 0));
    setInt(program, "numberOfLights", 1);
    setInt(program, "withTexture", 1);
    setInt(program, "withBumpMap", 0);
    setFloat(program, "phongExponent", 1.0f);
    configureClippingPlane(program, clippingPlane);

    glDisable(GL_CULL_FACE);
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glBindVertexArray(vaoId);
    for ( long i = 0; i < images.size(); i++ ) {
        int textureId = images.get(i) != 0 ? OpenGL4ImageRenderer::activate(images.get(i)) : 0;
        setInt(program, "withTexture", textureId > 0 ? 1 : 0);
        if ( textureId > 0 ) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, textureId);
        }
        glDrawArrays(GL_TRIANGLE_FAN, (int)i * 4, 4);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
    glBindVertexArray(0);
    glDisable(GL_CLIP_DISTANCE0);
    OpenGL4RendererConfigurationShaderSelector::deactivateShader();
}

void Jogl4SolidTexturePlanesRenderer::dispose()
{
    if ( positionVboId != 0 ) glDeleteBuffers(1, &positionVboId);
    if ( normalVboId != 0 ) glDeleteBuffers(1, &normalVboId);
    if ( uvVboId != 0 ) glDeleteBuffers(1, &uvVboId);
    if ( vaoId != 0 ) glDeleteVertexArrays(1, &vaoId);
    positionVboId = normalVboId = uvVboId = vaoId = 0;
}

void Jogl4SolidTexturePlanesRenderer::ensureBuffers()
{
    if ( vaoId != 0 ) return;
    glGenVertexArrays(1, &vaoId);
    glGenBuffers(1, &positionVboId);
    glGenBuffers(1, &normalVboId);
    glGenBuffers(1, &uvVboId);
}

void Jogl4SolidTexturePlanesRenderer::buildPlaneFrame(
    int planeCount, std::vector<float>& positions, std::vector<float>& normals,
    std::vector<float>& uvs)
{
    positions.reserve(planeCount * 12);
    normals.reserve(planeCount * 12);
    uvs.reserve(planeCount * 8);
    for ( int i = 0; i < planeCount; i++ ) {
        float z = planeCount == 1 ? -1.0f :
            -1.0f + (2.0f * i) / (float)(planeCount - 1);
        float p[12] = {-1,-1,z, 1,-1,z, 1,1,z, -1,1,z};
        float t[8] = {0,0, 1,0, 1,1, 0,1};
        positions.insert(positions.end(), p, p + 12);
        uvs.insert(uvs.end(), t, t + 8);
        for ( int j = 0; j < 4; j++ ) {
            normals.push_back(0); normals.push_back(0); normals.push_back(1);
        }
    }
}

void Jogl4SolidTexturePlanesRenderer::uploadFrame(
    const std::vector<float>& positions, const std::vector<float>& normals,
    const std::vector<float>& uvs)
{
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

void Jogl4SolidTexturePlanesRenderer::configureClippingPlane(
    unsigned int programId, InfinitePlane* clippingPlane)
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
