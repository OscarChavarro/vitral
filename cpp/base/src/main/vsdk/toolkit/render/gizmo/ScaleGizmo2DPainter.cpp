#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/linealAlgebra/Matrix4x4d.h"
#include "vsdk/toolkit/common/linealAlgebra/Vector3Dd.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/Geometry.h"
#include "vsdk/toolkit/environment/geometry/volume/Box.h"
#include "vsdk/toolkit/environment/geometry/volume/Cone.h"
#include "vsdk/toolkit/environment/material/SimpleMaterial.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/GizmoSolidTessellator.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmo.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DDrawList.h"
#include "vsdk/toolkit/render/gizmo/Gizmo2DProjector.h"
#include "vsdk/toolkit/render/gizmo/ScaleGizmo2DPainter.h"

namespace {
/// Opacity of the interior of the flat handle currently selected
const double HANDLE_OPACITY = 0.55;
/// Depth bias (towards the viewer) of the contour of the flat handles
const double CONTOUR_DEPTH_BIAS = -0.0002;

java::ArrayList<Vector3Dd> asList(const Vector3Dd* vertices, int count)
{
    java::ArrayList<Vector3Dd> list;

    for ( int i = 0; i < count; i++ ) {
        list.add(vertices[i]);
    }
    return list;
}
}

ScaleGizmo2DPainter::ScaleGizmo2DPainter()
{
}

void ScaleGizmo2DPainter::paint(Gizmo2DDrawList* out, ScaleGizmo* gizmo,
                                const Camera* camera)
{
    if ( out == nullptr || gizmo == nullptr || camera == nullptr ) {
        return;
    }
    Gizmo2DProjector projector(camera, out);
    java::ArrayList<SimpleBody*>& elements = gizmo->getElements();

    for ( long i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements[i];
        Geometry* g = element->getGeometry();
        Matrix4x4d local = GizmoSolidTessellator::localTransform(element);
        Cone* cone = dynamic_cast<Cone*>(g);
        Box* box = dynamic_cast<Box*>(g);

        if ( cone != nullptr ) {
            projector.addStrip(GizmoSolidTessellator::buildShaftStrip(local,
                cone->getBottomRadius(), cone->getTopRadius(),
                cone->getHeight()),
                element->getMaterial()->getDiffuse(), 1.0);
        }
        else if ( box != nullptr ) {
            Vector3Dd faces[GizmoSolidTessellator::BOX_NUMBER_OF_FACES][4];
            GizmoSolidTessellator::buildBoxFaceStrips(local, box->getSize(),
                                                      faces);
            for ( int f = 0; f < GizmoSolidTessellator::BOX_NUMBER_OF_FACES;
                  f++ ) {
                projector.addStrip(asList(faces[f], 4),
                    element->getMaterial()->getDiffuse(), 1.0);
            }
        }
    }

    java::ArrayList<ScaleGizmo::ContourSegment> segments =
        gizmo->buildContourSegments();
    for ( long i = 0; i < segments.size(); i++ ) {
        const ScaleGizmo::ContourSegment& segment = segments[i];
        projector.addSegment(segment.start(), segment.end(), segment.color(),
            gizmo->getLineWidth(), CONTOUR_DEPTH_BIAS);
    }

    for ( int i = 0; i < ScaleGizmo::BAND_GROUPS_COUNT; i++ ) {
        int group = ScaleGizmo::BAND_GROUPS[i];
        if ( gizmo->isBandHighlighted(group) ) {
            Vector3Dd quad[4];
            gizmo->buildBandQuad(group, quad);
            projector.addStrip(asList(quad, 4),
                ScaleGizmo::HANDLE_FILL_COLOR, HANDLE_OPACITY);
        }
    }
    if ( gizmo->isUniformHighlighted() ) {
        projector.addTriangles(gizmo->buildUniformTriangles(),
            ScaleGizmo::HANDLE_FILL_COLOR, HANDLE_OPACITY);
    }
}
