#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmo.h"
#include "vsdk/toolkit/gui/viewport/ViewportElementScaler.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DProjector.h"
#include "vsdk/toolkit/render/gizmo/RotateGizmo2DPainter.h"

namespace {
const double ARC_OPACITY = 0.35;
// Size, in pixels, designed for legacy resolutions
const int BASE_ARC_LABEL_FONT_SIZE = 16;
/// The degree sign, in UTF-8
const char* const DEGREE_SIGN = "\xc2\xb0";
}

RotateGizmo2DPainter::RotateGizmo2DPainter()
{
}

void RotateGizmo2DPainter::paint(Gizmo2DDrawList* out,
    const RotateGizmo* gizmo, const Camera* camera,
    const ViewportElementScaler* scaler)
{
    if ( out == nullptr || gizmo == nullptr || camera == nullptr ) {
        return;
    }
    Gizmo2DProjector projector(camera, out);

    for ( int ring = 0; ring < RotateGizmo::RING_COUNT; ring++ ) {
        ColorRgb color = gizmo->getRingColor(ring);
        java::ArrayList<java::ArrayList<Vector3Dd> > strips =
            gizmo->buildRingStrips(ring);

        for ( long i = 0; i < strips.size(); i++ ) {
            projector.addStrip(strips[i], color, 1.0);
        }
    }
    projector.addStrip(gizmo->buildCameraRingStrip(),
                       gizmo->getCameraRingColor(), 1.0);

    //- Translucent arc and its label -------------------------------------
    projector.addFanAsPolygon(gizmo->buildArcFan(), gizmo->getArcColor(),
                              ARC_OPACITY);

    Vector3Dd anchor;
    if ( gizmo->getArcLabelPosition(&anchor) ) {
        double p[2];

        if ( projector.projectPoint(anchor, p) ) {
            out->addOverlayLabel((int)p[0], (int)p[1],
                InputGizmo::format(gizmo->getArcSweepInDegrees(),
                    RotateGizmo::ANGLE_DECIMALS) + DEGREE_SIGN,
                gizmo->getArcColor(),
                scaler->scaleSize(BASE_ARC_LABEL_FONT_SIZE),
                Gizmo2DDrawList::Alignment::CENTER);
        }
    }
}
