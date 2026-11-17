#ifndef __OGL1_CAM_CTRL_GZM_RNDR__
#define __OGL1_CAM_CTRL_GZM_RNDR__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

class CameraControlGizmo;

/**
Renders a `CameraControlGizmo` (a ViewCube) as a HUD over its area of a
viewport, with the OpenGL 1.2 pipeline.

The parts of the gizmo (26 boxes and the compass tube) are drawn with
`OpenGL1GeometryRenderer`, lit from the HUD camera and seen through it, in a
GL viewport restricted to the area of the gizmo, with its own depth buffer
(the depth of that area is cleared). Their edges are drawn as lines from
their boundary representations (see `CameraControlGizmo::getEdgeSegments`).
An inactive gizmo is translucent. Labels depend on the way images are
created and cached by the application, so they are delegated to a
`LabelPainter`.

All the OpenGL state changed is restored.

C++ port note: the Java (GL4) version blends the fragments of the gizmo with
a constant alpha (`glBlendColor`), which OpenGL 1.1 does not have (it is
from the imaging subset of OpenGL 1.2). Here the area of the gizmo is read
before drawing it (`glReadPixels`), the gizmo is drawn opaque, and then the
saved pixels are drawn back over it with an alpha of `1 - opacity`
(`glDrawPixels` with blending), which gives the same result.

C++ port note: the class name is longer than 32 characters; legacy
compilers see it as `OpenGL1CameraCtrlGizmoRenderer` (see
scripts/class_renames_32.csv).
*/
class OpenGL1CameraControlGizmoRenderer {
public:
    /**
    Draws the text of the label of a face of the cube.
    */
    class LabelPainter {
    public:
        virtual ~LabelPainter() {}
        /**
        @param label text of the label
        @param x horizontal pixel of the viewport (from its left) of the
        center of the label
        @param y vertical pixel of the viewport (from its top) of the center
        of the label

        C++ port note: there is no GL context object in OpenGL 1.
        */
        virtual void drawLabel(const String& label, double x, double y) = 0;
    };

private:
    static const float EDGE_DEPTH_BIAS;
    static const double MIN_LABEL_VISIBILITY;

    OpenGL1CameraControlGizmoRenderer();

public:
    /**
    @param gizmo gizmo to draw, already placed in the viewport (see
    `CameraControlGizmo::placeInViewport`) and with its HUD camera updated
    @param viewportStartX horizontal position, in pixels of the surface, of
    the lower left corner of the viewport
    @param viewportStartY vertical position, in pixels of the surface (from
    the bottom), of the lower left corner of the viewport
    @param viewportSizeY height of the viewport, in pixels
    @param lineWidth width of the edges, in pixels
    @param labelPainter object that draws the labels of the faces; can be
    NULL
    */
    static void draw(
        CameraControlGizmo* gizmo,
        int viewportStartX,
        int viewportStartY,
        int viewportSizeY,
        float lineWidth,
        LabelPainter* labelPainter);
};

#endif
