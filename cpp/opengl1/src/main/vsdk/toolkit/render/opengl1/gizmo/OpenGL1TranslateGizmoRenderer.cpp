#include "vsdk/toolkit/render/opengl1/OpenGL1Api.h"

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/GizmoSolidTessellator.h"
#include "vsdk/toolkit/gui/gizmo/GizmoVertexArrayBuilder.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmoLineSegment.h"
#include "vsdk/toolkit/render/opengl1/OpenGL1ColoredPrimitiveRenderer.h"
#include "vsdk/toolkit/render/opengl1/gizmo/OpenGL1TranslateGizmoRenderer.h"

const double OpenGL1TranslateGizmoRenderer::CONE_BASE_SHADE = 0.5;

void OpenGL1TranslateGizmoRenderer::draw(TranslateGizmo* gizmo, Camera* camera)
{
    if ( gizmo == nullptr || camera == nullptr ) {
        return;
    }

    java::ArrayList<SimpleBody*>& elements = gizmo->getElements3dsmax();
    Matrix4x4d mvp = camera->calculateProjectionMatrix();
    long i;

    glDisable(GL_CULL_FACE);
    glEnable(GL_DEPTH_TEST);
    glDepthMask(GL_TRUE);
    glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

    //- Opaque elements -----------------------------------------------
    drawLines(gizmo, mvp);
    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements.get(i);
        Cone* cone = dynamic_cast<Cone*>(element->getGeometry());

        if ( cone != nullptr ) {
            drawCone(mvp, element, cone);
        }
    }

    //- Translucent elements, over the opaque ones --------------------
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glDepthMask(GL_FALSE);
    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements.get(i);
        Box* box = dynamic_cast<Box*>(element->getGeometry());

        if ( box != nullptr ) {
            drawPlaneHandle(mvp, element, box);
        }
    }

    //-----------------------------------------------------------------
    glDepthMask(GL_TRUE);
    glDisable(GL_BLEND);
}

void OpenGL1TranslateGizmoRenderer::drawLines(TranslateGizmo* gizmo,
                                              const Matrix4x4d& mvp)
{
    java::ArrayList<TranslateGizmoLineSegment> segments =
        gizmo->getLineSegments();

    for ( long i = 0; i < segments.size(); i++ ) {
        TranslateGizmoLineSegment segment = segments.get(i);
        Vector3Dd points[4];

        if ( !gizmo->buildLineStrip(segment, points) ) {
            continue;
        }
        java::ArrayList<Vector3Dd> strip;
        for ( int k = 0; k < 4; k++ ) {
            strip.add(points[k]);
        }
        drawStrip(mvp, strip, segment.color(), 1.0, GL_TRIANGLE_STRIP);
    }
}

void OpenGL1TranslateGizmoRenderer::drawCone(const Matrix4x4d& mvp,
                                             SimpleBody* element, Cone* cone)
{
    double radius = cone->getBottomRadius();
    double height = cone->getHeight();
    ColorRgb c = element->getMaterial()->getDiffuse();
    Matrix4x4d local = GizmoSolidTessellator::localTransform(element);

    // Side
    java::ArrayList<Vector3Dd> sideFan =
        GizmoSolidTessellator::buildConeSideFan(local, radius, height);
    drawStrip(mvp, sideFan, c, 1.0, GL_TRIANGLE_FAN);

    // Base, darker
    ColorRgb dark(c.r()*CONE_BASE_SHADE, c.g()*CONE_BASE_SHADE,
        c.b()*CONE_BASE_SHADE);
    java::ArrayList<Vector3Dd> baseFan =
        GizmoSolidTessellator::buildConeBaseFan(local, radius);

    drawStrip(mvp, baseFan, dark, 1.0, GL_TRIANGLE_FAN);
}

void OpenGL1TranslateGizmoRenderer::drawPlaneHandle(const Matrix4x4d& mvp,
                                                    SimpleBody* element, Box* box)
{
    ColorRgb c = element->getMaterial()->getDiffuse();
    Matrix4x4d local = GizmoSolidTessellator::localTransform(element);
    java::ArrayList<Vector3Dd> quad = GizmoSolidTessellator::buildPlaneQuad(
        local, box->getSize().x(), box->getSize().y());

    drawStrip(mvp, quad, c, element->getMaterial()->getOpacity(),
        GL_TRIANGLE_STRIP);
}

void OpenGL1TranslateGizmoRenderer::drawStrip(const Matrix4x4d& mvp,
    const java::ArrayList<Vector3Dd>& points, const ColorRgb& c, double alpha,
    unsigned int primitiveType)
{
    java::ArrayList<float> positions =
        GizmoVertexArrayBuilder::buildPositions(points);
    java::ArrayList<float> colors =
        GizmoVertexArrayBuilder::buildRgbaColors(points.size(), c, alpha);

    OpenGL1ColoredPrimitiveRenderer::draw(mvp, primitiveType, positions, colors);
}
