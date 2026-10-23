#include <math.h>
#include <stdio.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

#include "java/lang/Math.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ArrowRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"

const int OpenGL1ArrowRenderer::SLICES = 16;
unsigned int OpenGL1ArrowRenderer::displayListId = 0;
int OpenGL1ArrowRenderer::vertexCount = 0;
BOOLEAN OpenGL1ArrowRenderer::initialized = FALSE;

void OpenGL1ArrowRenderer::draw(
    const Arrow* arrow,
    const Matrix4x4d& modelMatrix,
    const Matrix4x4d& projection,
    const Camera* camera,
    const ArrayList<Light*>& lights,
    const SimpleMaterial* material,
    const RendererConfiguration* quality)
{
    if ( arrow == 0 || camera == 0 || material == 0 || quality == 0 || lights.size() == 0 ) {
        return;
    }
    if ( !ensureMesh(arrow) ) {
        return;
    }

    // Always Gouraud shaded, as the gouraudTexture GLSL program used by
    // the OpenGL 4 port
    RendererConfiguration gouraud;
    gouraud.setShadingType(RendererConfiguration::SHADING_TYPE_GOURAUD);
    gouraud.setTexture(FALSE);
    gouraud.setBumpMap(FALSE);
    OpenGL1RendererConfigurationStateSelector::activateState(
        camera, projection, modelMatrix, lights, *material, &gouraud, 0);

    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
    glEnable(GL_CULL_FACE);
    glCullFace(GL_BACK);
    glEnable(GL_POLYGON_OFFSET_FILL);
    glPolygonOffset(1.0f, 1.0f);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    glCallList(displayListId);

    glDisable(GL_POLYGON_OFFSET_FILL);
    glDisable(GL_CULL_FACE);
    OpenGL1RendererConfigurationStateSelector::deactivateState();
    (void)quality;
}

void OpenGL1ArrowRenderer::dispose()
{
    if ( displayListId != 0 ) { glDeleteLists(displayListId, 1); displayListId = 0; }
    vertexCount = 0;
    initialized = FALSE;
}

BOOLEAN OpenGL1ArrowRenderer::ensureMesh(const Arrow* arrow)
{
    if ( initialized ) {
        return TRUE;
    }
    ArrowMesh mesh = buildArrowMesh(
        arrow->getBaseRadius(),
        arrow->getHeadRadius(),
        arrow->getBaseLength(),
        arrow->getHeadLength(),
        SLICES);
    uploadMesh(mesh);
    initialized = TRUE;
    return TRUE;
}

OpenGL1ArrowRenderer::ArrowMesh OpenGL1ArrowRenderer::buildArrowMesh(
    double baseRadius,
    double headRadius,
    double baseLength,
    double headLength,
    int slices)
{
    ArrowMesh mesh;

    int i;
    for ( i = 0; i < slices; i++ ) {
        double a0 = 2.0 * Math::PI * i / slices;
        double a1 = 2.0 * Math::PI * (i + 1) / slices;
        float x0 = (float)(cos(a0) * baseRadius);
        float y0 = (float)(sin(a0) * baseRadius);
        float x1 = (float)(cos(a1) * baseRadius);
        float y1 = (float)(sin(a1) * baseRadius);
        float nx0 = (float)cos(a0);
        float ny0 = (float)sin(a0);
        float nx1 = (float)cos(a1);
        float ny1 = (float)sin(a1);

        addPos(mesh.positions, x0, y0, 0); addNorm(mesh.normals, nx0, ny0, 0); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, x1, y1, 0); addNorm(mesh.normals, nx1, ny1, 0); addUv(mesh.uvs, 1, 0);
        addPos(mesh.positions, x1, y1, (float)baseLength); addNorm(mesh.normals, nx1, ny1, 0); addUv(mesh.uvs, 1, 1);

        addPos(mesh.positions, x0, y0, 0); addNorm(mesh.normals, nx0, ny0, 0); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, x1, y1, (float)baseLength); addNorm(mesh.normals, nx1, ny1, 0); addUv(mesh.uvs, 1, 1);
        addPos(mesh.positions, x0, y0, (float)baseLength); addNorm(mesh.normals, nx0, ny0, 0); addUv(mesh.uvs, 0, 1);
    }

    for ( i = 0; i < slices; i++ ) {
        double a0 = 2.0 * Math::PI * i / slices;
        double a1 = 2.0 * Math::PI * (i + 1) / slices;
        float x0 = (float)(cos(a0) * baseRadius);
        float y0 = (float)(sin(a0) * baseRadius);
        float x1 = (float)(cos(a1) * baseRadius);
        float y1 = (float)(sin(a1) * baseRadius);

        addPos(mesh.positions, 0, 0, 0); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0.5f, 0.5f);
        addPos(mesh.positions, x1, y1, 0); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, x0, y0, 0); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 0);
    }

    float coneBase = (float)baseLength;
    for ( i = 0; i < slices; i++ ) {
        double a0 = 2.0 * Math::PI * i / slices;
        double a1 = 2.0 * Math::PI * (i + 1) / slices;
        float ox0 = (float)(cos(a0) * baseRadius);
        float oy0 = (float)(sin(a0) * baseRadius);
        float ox1 = (float)(cos(a1) * baseRadius);
        float oy1 = (float)(sin(a1) * baseRadius);
        float ix0 = (float)(cos(a0) * headRadius);
        float iy0 = (float)(sin(a0) * headRadius);
        float ix1 = (float)(cos(a1) * headRadius);
        float iy1 = (float)(sin(a1) * headRadius);

        addPos(mesh.positions, ox0, oy0, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, ox1, oy1, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 0);
        addPos(mesh.positions, ix1, iy1, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 1);

        addPos(mesh.positions, ox0, oy0, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 0);
        addPos(mesh.positions, ix1, iy1, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 1, 1);
        addPos(mesh.positions, ix0, iy0, coneBase); addNorm(mesh.normals, 0, 0, -1); addUv(mesh.uvs, 0, 1);
    }

    float apex = (float)(baseLength + headLength);
    float slantLength = (float)sqrt(headRadius * headRadius + headLength * headLength);
    float cosAlpha = slantLength > 1e-12f ? (float)(headLength / slantLength) : 1.0f;
    float sinAlpha = slantLength > 1e-12f ? (float)(headRadius / slantLength) : 0.0f;

    for ( i = 0; i < slices; i++ ) {
        double a0 = 2.0 * Math::PI * i / slices;
        double a1 = 2.0 * Math::PI * (i + 1) / slices;
        double aMid = (a0 + a1) * 0.5;
        float x0 = (float)(cos(a0) * headRadius);
        float y0 = (float)(sin(a0) * headRadius);
        float x1 = (float)(cos(a1) * headRadius);
        float y1 = (float)(sin(a1) * headRadius);
        float nx0 = (float)(cos(a0) * sinAlpha);
        float ny0 = (float)(sin(a0) * sinAlpha);
        float nx1 = (float)(cos(a1) * sinAlpha);
        float ny1 = (float)(sin(a1) * sinAlpha);
        float nxMid = (float)(cos(aMid) * sinAlpha);
        float nyMid = (float)(sin(aMid) * sinAlpha);

        addPos(mesh.positions, x0, y0, coneBase); addNorm(mesh.normals, nx0, ny0, cosAlpha); addUv(mesh.uvs, 0, 1);
        addPos(mesh.positions, x1, y1, coneBase); addNorm(mesh.normals, nx1, ny1, cosAlpha); addUv(mesh.uvs, 1, 1);
        addPos(mesh.positions, 0, 0, apex); addNorm(mesh.normals, nxMid, nyMid, cosAlpha); addUv(mesh.uvs, 0.5f, 0);
    }

    mesh.vertexCount = (int)(mesh.positions.size() / 3);
    return mesh;
}

/**
Compiles the arrow in a display list, from client side vertex arrays.
*/
void OpenGL1ArrowRenderer::uploadMesh(const ArrowMesh& mesh)
{
    displayListId = glGenLists(1);
    glNewList(displayListId, GL_COMPILE);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_NORMAL_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, mesh.positions.data());
    glNormalPointer(GL_FLOAT, 0, mesh.normals.data());
    glTexCoordPointer(2, GL_FLOAT, 0, mesh.uvs.data());
    glDrawArrays(GL_TRIANGLES, 0, mesh.vertexCount);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_NORMAL_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glEndList();

    vertexCount = mesh.vertexCount;
}

void OpenGL1ArrowRenderer::addPos(ArrayList<float>& buf, float x, float y, float z)
{
    buf.add(x); buf.add(y); buf.add(z);
}

void OpenGL1ArrowRenderer::addNorm(ArrayList<float>& buf, float x, float y, float z)
{
    float len = sqrt(x * x + y * y + z * z);
    if ( len > 1e-12f ) {
        buf.add(x / len); buf.add(y / len); buf.add(z / len);
    }
    else {
        buf.add(0); buf.add(0); buf.add(1);
    }
}

void OpenGL1ArrowRenderer::addUv(ArrayList<float>& buf, float u, float v)
{
    buf.add(u); buf.add(v);
}
