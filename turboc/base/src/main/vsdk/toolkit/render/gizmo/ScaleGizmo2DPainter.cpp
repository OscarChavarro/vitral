#include "vsdk/toolkit/common/VSDK.h"
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

/// Opacity of the interior of the flat handle currently selected
static const double HANDLE_OPACITY = 0.55;
/// Depth bias (towards the viewer) of the contour of the flat handles
static const double CONTOUR_DEPTH_BIAS = -0.0002;

static ArrayList<Vector3Dd> scaleAsList(const Vector3Dd* vertices, int count)
{
    ArrayList<Vector3Dd> list;
    int i;

    for ( i = 0; i < count; i++ ) {
        list.add(vertices[i]);
    }
    return list;
}

ScaleGizmo2DPainter::ScaleGizmo2DPainter()
{
}

void ScaleGizmo2DPainter::paint(Gizmo2DDrawList* out, ScaleGizmo* gizmo,
                                const Camera* camera)
{
    if ( out == NULL || gizmo == NULL || camera == NULL ) {
        return;
    }
    Gizmo2DProjector projector(camera, out);
    ArrayList<SimpleBody*>& elements = gizmo->getElements();
    long i;
    int f;

    for ( i = 0; i < elements.size(); i++ ) {
        SimpleBody* element = elements[i];
        Geometry* g = element->getGeometry();
        Matrix4x4d local = GizmoSolidTessellator::localTransform(element);

        if ( g != NULL && g->instanceOf(Cone::CLASS) ) {
            Cone* cone = (Cone*)g;
            projector.addStrip(GizmoSolidTessellator::buildShaftStrip(local,
                cone->getBottomRadius(), cone->getTopRadius(),
                cone->getHeight()),
                element->getMaterial()->getDiffuse(), 1.0);
        }
        else if ( g != NULL && g->instanceOf(Box::CLASS) ) {
            Box* box = (Box*)g;
            Vector3Dd faces[GizmoSolidTessellator::BOX_NUMBER_OF_FACES][4];
            GizmoSolidTessellator::buildBoxFaceStrips(local, box->getSize(),
                                                      faces);
            for ( f = 0; f < GizmoSolidTessellator::BOX_NUMBER_OF_FACES;
                  f++ ) {
                projector.addStrip(scaleAsList(faces[f], 4),
                    element->getMaterial()->getDiffuse(), 1.0);
            }
        }
    }

    ArrayList<ScaleGizmo::ContourSegment> segments =
        gizmo->buildContourSegments();
    for ( i = 0; i < segments.size(); i++ ) {
        const ScaleGizmo::ContourSegment& segment = segments[i];
        projector.addSegment(segment.start(), segment.end(), segment.color(),
            gizmo->getLineWidth(), CONTOUR_DEPTH_BIAS);
    }

    for ( f = 0; f < ScaleGizmo::BAND_GROUPS_COUNT; f++ ) {
        int group = ScaleGizmo::BAND_GROUPS[f];
        if ( gizmo->isBandHighlighted(group) ) {
            Vector3Dd quad[4];
            gizmo->buildBandQuad(group, quad);
            projector.addStrip(scaleAsList(quad, 4),
                ScaleGizmo::HANDLE_FILL_COLOR, HANDLE_OPACITY);
        }
    }
    if ( gizmo->isUniformHighlighted() ) {
        projector.addTriangles(gizmo->buildUniformTriangles(),
            ScaleGizmo::HANDLE_FILL_COLOR, HANDLE_OPACITY);
    }
}
