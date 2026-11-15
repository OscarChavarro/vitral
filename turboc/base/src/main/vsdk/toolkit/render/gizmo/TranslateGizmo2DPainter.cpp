#include "vsdk/toolkit/common/VSDK.h"
#include <math.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Arrow.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/GizmoSolidTessellator.h"
#include "vsdk/toolkit/gui/gizmo/ReferenceFrameGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmoLineSegment.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DProjector.h"
#include "vsdk/toolkit/render/gizmo/TranslateGizmo2DPainter.h"

static const double CONE_BASE_SHADE = 0.5;
// Sizes, in pixels, designed for legacy resolutions
enum {
    BASE_LABEL_FONT_SIZE = 14,
    BASE_LABEL_OFFSET_X = -3,
    BASE_LABEL_OFFSET_Y = 12
};

/// Axis colors and labels, created on first use
static ReferenceFrameGizmo* translateAxes = NULL;

static const ReferenceFrameGizmo& axes()
{
    if ( translateAxes == NULL ) {
        translateAxes = new ReferenceFrameGizmo();
    }
    return *translateAxes;
}

static ArrayList<Vector3Dd> translateAsList(const Vector3Dd* vertices,
                                            int count)
{
    ArrayList<Vector3Dd> list;
    int i;

    for ( i = 0; i < count; i++ ) {
        list.add(vertices[i]);
    }
    return list;
}

TranslateGizmo2DPainter::TranslateGizmo2DPainter()
{
}

void TranslateGizmo2DPainter::paint(Gizmo2DDrawList* out,
    TranslateGizmo* gizmo, const Camera* camera,
    const ViewportElementScaler* scaler)
{
    if ( out == NULL || gizmo == NULL || camera == NULL ) {
        return;
    }
    Gizmo2DProjector projector(camera, out);
    ArrayList<SimpleBody*>& elements = gizmo->getElements3dsmax();
    long i;

    //- Opaque elements ---------------------------------------------------
    ArrayList<TranslateGizmoLineSegment> segments =
        gizmo->getLineSegments();
    for ( i = 0; i < segments.size(); i++ ) {
        Vector3Dd strip[4];

        if ( gizmo->buildLineStrip(segments[i], strip) ) {
            projector.addStrip(translateAsList(strip, 4),
                               segments[i].color(), 1.0);
        }
    }
    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements[i];
        Geometry* g = element->getGeometry();

        if ( g != NULL && g->instanceOf(Cone::CLASS) ) {
            addCone(projector, element, (Cone*)g);
        }
    }

    //- Translucent elements ----------------------------------------------
    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements[i];
        Geometry* g = element->getGeometry();

        if ( g != NULL && g->instanceOf(Box::CLASS) ) {
            Box* box = (Box*)g;
            ArrayList<Vector3Dd> quad =
                GizmoSolidTessellator::buildPlaneQuad(
                    GizmoSolidTessellator::localTransform(element),
                    box->getSize().x(), box->getSize().y());

            projector.addStrip(quad, element->getMaterial()->getDiffuse(),
                element->getMaterial()->getOpacity());
        }
    }

    addLabels(projector, gizmo, scaler);
}

void TranslateGizmo2DPainter::addCone(Gizmo2DProjector& projector,
    const SimpleBody* element, const Cone* cone)
{
    double radius = cone->getBottomRadius();
    ColorRgb c = element->getMaterial()->getDiffuse();
    Matrix4x4d local = GizmoSolidTessellator::localTransform(element);
    ColorRgb dark(c.r()*CONE_BASE_SHADE, c.g()*CONE_BASE_SHADE,
                  c.b()*CONE_BASE_SHADE);
    ArrayList<Vector3Dd> baseFan =
        GizmoSolidTessellator::buildConeBaseFan(local, radius);
    ArrayList<Vector3Dd> perimeter;
    long i;

    for ( i = 1; i < baseFan.size(); i++ ) {
        perimeter.add(baseFan[i]);
    }
    projector.addPolygon(perimeter, dark, 1.0);
    projector.addFanTriangles(GizmoSolidTessellator::buildConeSideFan(
        local, radius, cone->getHeight()), c, 1.0);
}

/**
Labels of the axes, a little beyond the tip of each one.
*/
void TranslateGizmo2DPainter::addLabels(Gizmo2DProjector& projector,
    TranslateGizmo* gizmo, const ViewportElementScaler* scaler)
{
    ColorRgb highlightColor(1, 1, 0);
    int fontSize = scaler->scaleSize(BASE_LABEL_FONT_SIZE);
    double offsetX = floor(scaler->scaleLength(BASE_LABEL_OFFSET_X) + 0.5);
    double offsetY = floor(scaler->scaleLength(BASE_LABEL_OFFSET_Y) + 0.5);
    ArrayList<SimpleBody*>& things = gizmo->getElements();
    int i;

    for ( i = 0; i < things.size() &&
              i < ReferenceFrameGizmo::NUMBER_OF_AXES; i++ ) {
        SimpleBody* r = things[i];
        Geometry* g = r->getGeometry();

        if ( g == NULL ) {
            continue;
        }
        double tip = 1;
        if ( g->instanceOf(Arrow::CLASS) ) {
            Arrow* a = (Arrow*)g;
            tip = (a->getHeadLength() + a->getBaseLength()) * 1.1;
        }
        Matrix4x4d R = Matrix4x4d().translation(r->getPosition())
            .multiply(r->getRotation());
        double p[2];

        if ( projector.projectPoint(R.multiply(Vector3Dd(0, 0, tip)), p) ) {
            BOOLEAN highlighted = highlightColor.distance(
                r->getMaterial()->getDiffuse()) < VSDK::EPSILON;
            ColorRgb color = axes().getAxisColor(i);
            if ( highlighted ) {
                color = highlightColor;
            }

            projector.getDrawList()->addOverlayLabel(
                (int)p[0] + offsetX, (int)p[1] + offsetY,
                axes().getAxisLabel(i), color, fontSize,
                Gizmo2DDrawList::Alignment::LOWER_LEFT);
        }
    }
}
