#include <cmath>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/VSDK.h"
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

namespace {
const double CONE_BASE_SHADE = 0.5;
const ColorRgb HIGHLIGHT_COLOR(1, 1, 0);
// Sizes, in pixels, designed for legacy resolutions
const int BASE_LABEL_FONT_SIZE = 14;
const int BASE_LABEL_OFFSET_X = -3;
const int BASE_LABEL_OFFSET_Y = 12;

const ReferenceFrameGizmo& axes()
{
    static const ReferenceFrameGizmo AXES;
    return AXES;
}

java::ArrayList<Vector3Dd> asList(const Vector3Dd* vertices, int count)
{
    java::ArrayList<Vector3Dd> list;

    for ( int i = 0; i < count; i++ ) {
        list.add(vertices[i]);
    }
    return list;
}
}

TranslateGizmo2DPainter::TranslateGizmo2DPainter()
{
}

void TranslateGizmo2DPainter::paint(Gizmo2DDrawList* out,
    TranslateGizmo* gizmo, const Camera* camera,
    const ViewportElementScaler* scaler)
{
    if ( out == nullptr || gizmo == nullptr || camera == nullptr ) {
        return;
    }
    Gizmo2DProjector projector(camera, out);
    java::ArrayList<SimpleBody*>& elements = gizmo->getElements3dsmax();
    long i;

    //- Opaque elements ---------------------------------------------------
    java::ArrayList<TranslateGizmoLineSegment> segments =
        gizmo->getLineSegments();
    for ( i = 0; i < segments.size(); i++ ) {
        Vector3Dd strip[4];

        if ( gizmo->buildLineStrip(segments[i], strip) ) {
            projector.addStrip(asList(strip, 4), segments[i].color(), 1.0);
        }
    }
    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements[i];
        Cone* cone = dynamic_cast<Cone*>(element->getGeometry());

        if ( cone != nullptr ) {
            addCone(projector, element, cone);
        }
    }

    //- Translucent elements ----------------------------------------------
    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements[i];
        Box* box = dynamic_cast<Box*>(element->getGeometry());

        if ( box != nullptr ) {
            java::ArrayList<Vector3Dd> quad =
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
    java::ArrayList<Vector3Dd> baseFan =
        GizmoSolidTessellator::buildConeBaseFan(local, radius);
    java::ArrayList<Vector3Dd> perimeter;

    for ( long i = 1; i < baseFan.size(); i++ ) {
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
    int fontSize = scaler->scaleSize(BASE_LABEL_FONT_SIZE);
    double offsetX = std::floor(scaler->scaleLength(BASE_LABEL_OFFSET_X) + 0.5);
    double offsetY = std::floor(scaler->scaleLength(BASE_LABEL_OFFSET_Y) + 0.5);
    java::ArrayList<SimpleBody*>& things = gizmo->getElements();

    for ( int i = 0; i < things.size() &&
              i < ReferenceFrameGizmo::NUMBER_OF_AXES; i++ ) {
        SimpleBody* r = things[i];
        Geometry* g = r->getGeometry();

        if ( g == nullptr ) {
            continue;
        }
        double tip = 1;
        Arrow* a = dynamic_cast<Arrow*>(g);
        if ( a != nullptr ) {
            tip = (a->getHeadLength() + a->getBaseLength()) * 1.1;
        }
        Matrix4x4d R = Matrix4x4d().translation(r->getPosition())
            .multiply(r->getRotation());
        double p[2];

        if ( projector.projectPoint(R.multiply(Vector3Dd(0, 0, tip)), p) ) {
            bool highlighted = HIGHLIGHT_COLOR.distance(
                r->getMaterial()->getDiffuse()) < VSDK::EPSILON;
            ColorRgb color = axes().getAxisColor(i);
            if ( highlighted ) {
                color = HIGHLIGHT_COLOR;
            }

            projector.getDrawList()->addOverlayLabel(
                (int)p[0] + offsetX, (int)p[1] + offsetY,
                axes().getAxisLabel(i), color, fontSize,
                Gizmo2DDrawList::Alignment::LOWER_LEFT);
        }
    }
}
