#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/light/Light.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4LineRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4CameraControlGizmoRenderer.h"

const float OpenGL4CameraControlGizmoRenderer::EDGE_DEPTH_BIAS = -0.0005f;
const double OpenGL4CameraControlGizmoRenderer::MIN_LABEL_VISIBILITY = 0.3;

void OpenGL4CameraControlGizmoRenderer::draw(CameraControlGizmo* gizmo,
    int viewportStartX, int viewportStartY, int viewportSizeY,
    float lineWidth, LabelPainter* labelPainter)
{
    if ( gizmo == nullptr || !gizmo->isVisible() ) {
        return;
    }

    //-----------------------------------------------------------------
    int size = gizmo->getSizeInPixels();
    int x = viewportStartX + gizmo->getHudX();
    int y = viewportStartY + viewportSizeY - gizmo->getHudY() - size;
    GLint previousViewport[4];
    bool scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST) == GL_TRUE;
    bool blendWasEnabled = glIsEnabled(GL_BLEND) == GL_TRUE;
    GLint previousScissor[4];

    glGetIntegerv(GL_VIEWPORT, previousViewport);
    glGetIntegerv(GL_SCISSOR_BOX, previousScissor);

    // Own depth for the area of the gizmo
    glEnable(GL_SCISSOR_TEST);
    glScissor(x, y, size, size);
    glClear(GL_DEPTH_BUFFER_BIT);
    glViewport(x, y, size, size);
    glEnable(GL_DEPTH_TEST);
    glDisable(GL_CULL_FACE);

    double opacity = gizmo->getOpacity();
    if ( opacity < 1.0 ) {
        glEnable(GL_BLEND);
        glBlendColor(0.0f, 0.0f, 0.0f, (float)opacity);
        glBlendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA);
    }

    //-----------------------------------------------------------------
    Camera* hud = gizmo->getHudCamera();
    RendererConfiguration quality;
    quality.setSurfaces(true);
    quality.setWires(false);
    quality.setTexture(false);
    quality.setBumpMap(false);
    quality.setNormals(false);
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_GOURAUD);

    Matrix4x4d projection = hud->calculateProjectionMatrix();
    const ColorRgb& lineColor = gizmo->getLineColor();

    // All the surfaces first, so their depth hides the edges behind them
    for ( int p = 0; p < CameraControlGizmo::NUMBER_OF_PARTS; p++ ) {
        SimpleBody* part = gizmo->getParts().get(p);
        Matrix4x4d local = Matrix4x4d().translation(part->getPosition());

        // No lights: the pipeline uses a light at the camera
        OpenGL4GeometryRenderer::draw(part->getGeometry(), hud,
            (const java::ArrayList<Light*>*)nullptr, part->getMaterial(),
            &quality, nullptr, nullptr, local);
    }
    for ( int p = 0; p < CameraControlGizmo::NUMBER_OF_PARTS; p++ ) {
        java::ArrayList<float> positions = gizmo->getEdgeSegments(p);
        java::ArrayList<float> colors;
        for ( long i = 0; i < positions.size(); i += 3 ) {
            colors.add((float)lineColor.r());
            colors.add((float)lineColor.g());
            colors.add((float)lineColor.b());
        }
        OpenGL4LineRenderer::drawLines(projection, positions, colors,
            lineWidth, EDGE_DEPTH_BIAS);
    }

    //-----------------------------------------------------------------
    glViewport(previousViewport[0], previousViewport[1],
        previousViewport[2], previousViewport[3]);
    glScissor(previousScissor[0], previousScissor[1],
        previousScissor[2], previousScissor[3]);
    if ( !scissorWasEnabled ) {
        glDisable(GL_SCISSOR_TEST);
    }
    if ( opacity < 1.0 ) {
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        if ( !blendWasEnabled ) {
            glDisable(GL_BLEND);
        }
    }

    if ( labelPainter != nullptr ) {
        int faces[6];
        gizmo->getFaceRegions(faces);
        for ( int i = 0; i < 6; i++ ) {
            int face = faces[i];
            if ( gizmo->faceVisibility(face) < MIN_LABEL_VISIBILITY ) {
                continue;
            }
            double c[2];
            gizmo->projectToViewport(gizmo->getFaceLabelPosition(face), c);
            labelPainter->drawLabel(gizmo->getFaceLabel(face), c[0], c[1]);
        }
    }
    glEnable(GL_DEPTH_TEST);
}
