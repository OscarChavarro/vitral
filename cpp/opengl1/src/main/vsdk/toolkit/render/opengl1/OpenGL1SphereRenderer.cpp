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
#include "vsdk/toolkit/render/SpherePolyhedralCache.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1SphereRenderer.h"
const SpherePolyhedralCache::Entry* OpenGL1SphereRenderer::obtainTessellation(int meridians, int parallels)
{
    // The tessellation comes from a unit sphere converted into a polyhedral
    // bounded solid (shared with the other drawing algorithms), scaled by the
    // radius of each sphere drawn
    Sphere unitSphere(1.0);
    return SpherePolyhedralCache::obtain(&unitSphere,
        java::Math::max(12, meridians), java::Math::max(8, parallels));
}

void OpenGL1SphereRenderer::drawElements(const SpherePolyhedralCache::Entry* entry)
{
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_NORMAL_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, entry->getPositions().data());
    glNormalPointer(GL_FLOAT, 0, entry->getNormals().data());
    glTexCoordPointer(2, GL_FLOAT, 0, entry->getUvs().data());
    glDrawArrays(GL_TRIANGLES, 0, (GLsizei)entry->getVertexCount());
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
    const SpherePolyhedralCache::Entry* entry = obtainTessellation(meridians, parallels);
    if (entry == nullptr || entry->getVertexCount() <= 0) {
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
        drawElements(entry);
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
        drawElements(entry);
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
        drawElements(entry);
        OpenGL1RendererConfigurationStateSelector::deactivateState();
    }

    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
    glBindTexture(GL_TEXTURE_2D, 0);
    glDepthMask(GL_TRUE);
    glDepthFunc(GL_LESS);
}

void OpenGL1SphereRenderer::dispose()
{
    // The tessellations are client side arrays owned by SpherePolyhedralCache,
    // there are no OpenGL resources to release
}
