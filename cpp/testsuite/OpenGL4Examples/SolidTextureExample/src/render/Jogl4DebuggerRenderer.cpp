#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/gui/gizmo/LightGizmoStyle.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4CameraRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ImageRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LightRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4RendererConfigurationShaderSelector.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4RayGizmoRenderer.h"
#include "model/SolidTextureModel.h"
#include "render/Jogl4DebuggerRenderer.h"

Jogl4DebuggerRenderer::Jogl4DebuggerRenderer(SolidTextureModel* model)
    : model(model), solidTextureRenderer("../../../../etc/glslShaders")
{
}

bool Jogl4DebuggerRenderer::init()
{
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

    acquireGizmoSnapshots();
    InfinitePlane clippingPlane = model->getInfinitePlaneGizmo()->getPlane();
    InfinitePlane* clippingPlanePtr =
        model->getInfinitePlaneGizmo()->isVisible() ? &clippingPlane : 0;
    if ( model->getOperationMode() == OperationMode::MESH_MODEL ) {
        drawMeshModel(clippingPlanePtr);
    }
    else {
        planesRenderer.draw(model->getTexture2DStack(), model->getCamera(),
                            clippingPlanePtr);
    }
    drawGizmos();
}

void Jogl4DebuggerRenderer::reshape(int width, int height)
{
    glViewport(0, 0, width, height);
    model->getCamera()->updateViewportResize(width, height);
}

void Jogl4DebuggerRenderer::dispose()
{
    OpenGL4RendererConfigurationShaderSelector::dispose();
    OpenGL4CameraRenderer::dispose();
    OpenGL4RayGizmoRenderer::dispose();
    planesRenderer.dispose();
    solidTextureRenderer.dispose();
    OpenGL4LightRenderer::dispose();
    OpenGL4ImageRenderer::dispose();
}

void Jogl4DebuggerRenderer::drawMeshModel(InfinitePlane* clippingPlane)
{
    java::ArrayList<Light*>& activeLights = model->getLights();
    if ( activeLights.size() == 0 ) return;
    solidTextureRenderer.draw(
        model->getScene(), model->getCamera(), activeLights,
        model->getSolidTextureVolumeRgb8(), model->getSolidTextureSize(),
        model->getSolidTextureRevision(), clippingPlane);
    for ( long i = 0; i < activeLights.size(); i++ ) {
        if ( activeLights.get(i) != 0 ) {
            OpenGL4LightRenderer::draw(
                activeLights.get(i), model->getCamera(),
                LightGizmoStyle::OMNI_BILLBOARD);
        }
    }
}

void Jogl4DebuggerRenderer::drawGizmos()
{
    java::ArrayList<Light*>& activeLights = model->getLights();
    OpenGL4RayGizmoRenderer::draw(model->getRayGizmo(), model->getCamera(), activeLights);
}

void Jogl4DebuggerRenderer::acquireGizmoSnapshots()
{
    model->getRayGizmo()->acquireSnapshot();
    model->getInfinitePlaneGizmo()->acquireSnapshot();
}
