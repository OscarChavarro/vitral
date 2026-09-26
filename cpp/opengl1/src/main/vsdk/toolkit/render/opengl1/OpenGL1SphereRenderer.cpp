#include <cmath>
#include <cstdio>

#include <java/lang/Math.h>
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/media/RGBImageUncompressed.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/geometry/volume/Sphere.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ImageRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1RendererConfigurationStateSelector.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.h"
java::ArrayList<float> OpenGL1SphereRenderer::positions;
java::ArrayList<float> OpenGL1SphereRenderer::normals;
java::ArrayList<float> OpenGL1SphereRenderer::uvs;
java::ArrayList<unsigned int> OpenGL1SphereRenderer::indices;

int OpenGL1SphereRenderer::cachedMeridians = -1;
int OpenGL1SphereRenderer::cachedParallels = -1;
unsigned int OpenGL1SphereRenderer::indexCount = 0;

bool OpenGL1SphereRenderer::buildSphereMeshIfNeeded(int meridians, int parallels)
{
    meridians = java::Math::max(12, meridians);
    parallels = java::Math::max(8, parallels);
    if (cachedMeridians == meridians && cachedParallels == parallels && indexCount > 0) {
        return true;
    }

    Sphere unitSphere(1.0);
    long int vertexCount = (long int)(parallels + 1) * (long int)(meridians + 1);
    positions.clear();
    normals.clear();
    uvs.clear();
    indices.clear();
    positions.reserve(vertexCount * 3);
    normals.reserve(vertexCount * 3);
    uvs.reserve(vertexCount * 2);
    indices.reserve((long int)parallels * (long int)meridians * 6L);

    for (int p = 0; p <= parallels; ++p) {
        double t = static_cast<double>(p) / static_cast<double>(parallels);
        double phi = M_PI * t - M_PI / 2.0;
        for (int m = 0; m <= meridians; ++m) {
            double s = static_cast<double>(m) / static_cast<double>(meridians);
            double theta = 2.0 * M_PI * s;

            Vector3Dd pos = unitSphere.spherePosition(theta, phi);
            Vector3Dd nrm = unitSphere.sphereNormal(theta, phi);

            positions.add((float)pos.x());
            positions.add((float)pos.y());
            positions.add((float)pos.z());
            normals.add((float)nrm.x());
            normals.add((float)nrm.y());
            normals.add((float)nrm.z());
            uvs.add((float)(1.0 - s)); // Java parity
            uvs.add((float)t);
        }
    }

    int row = meridians + 1;
    for (int p = 0; p < parallels; ++p) {
        for (int m = 0; m < meridians; ++m) {
            unsigned int i0 = static_cast<unsigned int>(p * row + m);
            unsigned int i1 = i0 + 1;
            unsigned int i2 = i0 + row;
            unsigned int i3 = i2 + 1;
            indices.add(i0); indices.add(i2); indices.add(i1);
            indices.add(i1); indices.add(i2); indices.add(i3);
        }
    }

    cachedMeridians = meridians;
    cachedParallels = parallels;
    indexCount = (unsigned int)indices.size();
    return true;
}

void OpenGL1SphereRenderer::drawElements()
{
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_NORMAL_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, positions.data());
    glNormalPointer(GL_FLOAT, 0, normals.data());
    glTexCoordPointer(2, GL_FLOAT, 0, uvs.data());
    glDrawElements(GL_TRIANGLES, (GLsizei)indexCount, GL_UNSIGNED_INT, indices.data());
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_NORMAL_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
}

void OpenGL1SphereRenderer::draw(
    const Sphere* sphere,
    const Camera* camera,
    const Light* light,
    const SimpleMaterial* material,
    const RendererConfiguration* quality,
    RGBImageUncompressed* textureMap,
    RGBImageUncompressed* bumpMapHeightRgb,
    const Matrix4x4d& modelRotation,
    int meridians,
    int parallels)
{
    if (sphere == nullptr || camera == nullptr || light == nullptr || material == nullptr || quality == nullptr) {
        return;
    }
    if (!buildSphereMeshIfNeeded(meridians, parallels)) {
        return;
    }

    bool hasTexture = (textureMap != nullptr);
    int textureId = hasTexture ? OpenGL1ImageRenderer::activate(textureMap) : 0;
    // Bump mapping is not available in OpenGL 1.2 (reported by the selector)
    (void)bumpMapHeightRgb;

    Matrix4x4d localTransform = modelRotation.multiply(
        Matrix4x4d().scale(sphere->getRadius(), sphere->getRadius(), sphere->getRadius()));
    Matrix4x4d viewProjection = camera->calculateProjectionMatrix();
    Matrix4x4d modelViewProjection = viewProjection.multiply(localTransform);
    java::ArrayList<Light*> lights;
    lights.add(const_cast<Light*>(light));

    if (quality->isSurfacesSet()) {
        OpenGL1RendererConfigurationStateSelector::activateState(
            camera, viewProjection, localTransform, lights, *material,
            quality, textureId);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_TRUE);
        glDepthFunc(GL_LESS);
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(1.0f, 1.0f);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        drawElements();
        glDisable(GL_POLYGON_OFFSET_FILL);
        OpenGL1RendererConfigurationStateSelector::deactivateState();
    }

    if (quality->isWiresSet()) {
        OpenGL1RendererConfigurationStateSelector::activateConstantState(
            modelViewProjection, 1.0f, 1.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(1.0f);
        drawElements();
        glDisable(GL_POLYGON_OFFSET_LINE);
        OpenGL1RendererConfigurationStateSelector::deactivateState();
    }

    if (quality->isPointsSet()) {
        OpenGL1RendererConfigurationStateSelector::activateConstantState(
            modelViewProjection, 1.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(GL_FALSE);
        glDepthFunc(GL_LEQUAL);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_POINT);
        glPointSize(4.0f);
        drawElements();
        OpenGL1RendererConfigurationStateSelector::deactivateState();
    }

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glBindTexture(GL_TEXTURE_2D, 0);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
}

void OpenGL1SphereRenderer::dispose()
{
    positions.clear();
    normals.clear();
    uvs.clear();
    indices.clear();
    cachedMeridians = -1;
    cachedParallels = -1;
    indexCount = 0;
}
