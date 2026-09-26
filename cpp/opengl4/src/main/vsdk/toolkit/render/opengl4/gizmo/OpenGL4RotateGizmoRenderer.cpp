#include <glad/gl.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/GizmoVertexArrayBuilder.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmo.h"
#include "vsdk/toolkit/render/opengl4/OpenGL4ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl4/gizmo/OpenGL4RotateGizmoRenderer.h"

const float OpenGL4RotateGizmoRenderer::ARC_OPACITY = 0.35f;

void OpenGL4RotateGizmoRenderer::draw(RotateGizmo* gizmo, Camera* camera)
{
    if ( gizmo == nullptr || camera == nullptr ) {
        return;
    }
    Matrix4x4d mvp = camera->calculateProjectionMatrix();

    glDisable(GL_CULL_FACE);
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    for ( int ring = 0; ring < RotateGizmo::RING_COUNT; ring++ ) {
        ColorRgb color = gizmo->getRingColor(ring);
        java::ArrayList<java::ArrayList<Vector3Dd> > strips =
            gizmo->buildRingStrips(ring);

        for ( long i = 0; i < strips.size(); i++ ) {
            drawRing(mvp, strips.get(i), color);
        }
    }
    drawRing(mvp, gizmo->buildCameraRingStrip(), gizmo->getCameraRingColor());

    //- Translucent arc, over the rings -------------------------------
    java::ArrayList<Vector3Dd> fan = gizmo->buildArcFan();

    if ( fan.size() >= 3 ) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(GL_FALSE);
        drawArc(mvp, fan, gizmo->getArcColor());
        glDepthMask(GL_TRUE);
        glDisable(GL_BLEND);
    }
}

void OpenGL4RotateGizmoRenderer::drawArc(const Matrix4x4d& mvp,
    const java::ArrayList<Vector3Dd>& fan, const ColorRgb& c)
{
    java::ArrayList<float> positions =
        GizmoVertexArrayBuilder::buildPositions(fan);
    java::ArrayList<float> colors =
        GizmoVertexArrayBuilder::buildRgbaColors(fan.size(), c, ARC_OPACITY);

    OpenGL4ColoredPrimitiveRenderer::draw(mvp, GL_TRIANGLE_FAN, positions, colors);
}

void OpenGL4RotateGizmoRenderer::drawRing(const Matrix4x4d& mvp,
    const java::ArrayList<Vector3Dd>& strip, const ColorRgb& c)
{
    java::ArrayList<float> positions =
        GizmoVertexArrayBuilder::buildPositions(strip);
    java::ArrayList<float> colors =
        GizmoVertexArrayBuilder::buildRgbaColors(strip.size(), c, 1.0f);

    OpenGL4ColoredPrimitiveRenderer::draw(mvp, GL_TRIANGLE_STRIP, positions, colors);
}
