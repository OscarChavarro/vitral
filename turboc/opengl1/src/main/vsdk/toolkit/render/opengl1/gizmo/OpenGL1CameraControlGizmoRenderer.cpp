#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/material/RendererConfiguration.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/CameraControlGizmo.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1GeometryRenderer.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1LineRenderer.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1CameraControlGizmoRenderer.h"

const float OpenGL1CameraControlGizmoRenderer::EDGE_DEPTH_BIAS = -0.0005f;
const double OpenGL1CameraControlGizmoRenderer::MIN_LABEL_VISIBILITY = 0.3;

void OpenGL1CameraControlGizmoRenderer::draw(
    CameraControlGizmo* gizmo,
    int viewportStartX,
    int viewportStartY,
    int viewportSizeY,
    float lineWidth,
    LabelPainter* labelPainter)
{
    if ( gizmo == NULL || !gizmo->isVisible() ) {
        return;
    }

    //-----------------------------------------------------------------
    int size = gizmo->getSizeInPixels();
    int x = viewportStartX + gizmo->getHudX();
    int y = viewportStartY + viewportSizeY - gizmo->getHudY() - size;
    GLint previousViewport[4];
    GLboolean scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST);
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

    // C++ port note: what is behind the gizmo, to be blended over it
    double opacity = gizmo->getOpacity();
    unsigned char* background = NULL;
    if ( opacity < 1.0 ) {
        background = new unsigned char[(long)size * size * 4];
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glReadPixels(x, y, size, size, GL_RGBA, GL_UNSIGNED_BYTE, background);
    }

    //-----------------------------------------------------------------
    Camera* hud = gizmo->getHudCamera();
    RendererConfiguration quality;
    quality.setSurfaces(TRUE);
    quality.setWires(FALSE);
    quality.setTexture(FALSE);
    quality.setBumpMap(FALSE);
    quality.setNormals(FALSE);
    quality.setShadingType(RendererConfiguration::SHADING_TYPE_GOURAUD);

    Matrix4x4d projection = hud->calculateProjectionMatrix();
    ColorRgb lineColor = gizmo->getLineColor();
    int p;

    // All the surfaces first, so their depth hides the edges behind them
    for ( p = 0; p < CameraControlGizmo::NUMBER_OF_PARTS; p++ ) {
        SimpleBody* part = gizmo->getParts()[p];
        Matrix4x4d local = Matrix4x4d().translation(part->getPosition());

        // No lights: the pipeline uses a light at the camera
        OpenGL1GeometryRenderer::draw(part->getGeometry(), hud,
            NULL, part->getMaterial(), &quality, NULL, NULL, local);
    }
    for ( p = 0; p < CameraControlGizmo::NUMBER_OF_PARTS; p++ ) {
        ArrayList<float> positions = gizmo->getEdgeSegments(p);
        ArrayList<float> colors;
        for ( long i = 0; i < positions.size(); i += 3 ) {
            colors.add((float)lineColor.r());
            colors.add((float)lineColor.g());
            colors.add((float)lineColor.b());
        }
        OpenGL1LineRenderer::drawLines(projection, positions, colors,
            lineWidth, EDGE_DEPTH_BIAS);
    }

    //- C++ port note: translucency, without glBlendColor ----------------
    if ( background != NULL ) {
        long n = (long)size * size;
        unsigned char alpha = (unsigned char)(255.0 * (1.0 - opacity) + 0.5);
        for ( long i = 0; i < n; i++ ) {
            background[4 * i + 3] = alpha;
        }
        glPushAttrib(GL_ALL_ATTRIB_BITS);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glDisable(GL_FOG);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glRasterPos2f(-1.0f, -1.0f);
        glDrawPixels(size, size, GL_RGBA, GL_UNSIGNED_BYTE, background);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopAttrib();
        delete[] background;
    }

    //-----------------------------------------------------------------
    glViewport(previousViewport[0], previousViewport[1],
        previousViewport[2], previousViewport[3]);
    glScissor(previousScissor[0], previousScissor[1],
        previousScissor[2], previousScissor[3]);
    if ( !scissorWasEnabled ) {
        glDisable(GL_SCISSOR_TEST);
    }

    if ( labelPainter != NULL ) {
        ArrayList<int> faces = gizmo->getFaceRegions();
        for ( long i = 0; i < faces.size(); i++ ) {
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
