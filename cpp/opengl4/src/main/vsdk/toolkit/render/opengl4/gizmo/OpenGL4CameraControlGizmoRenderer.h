#ifndef __OPEN_GL_4_CAMERA_CONTROL_GIZMO_RENDERER__
#define __OPEN_GL_4_CAMERA_CONTROL_GIZMO_RENDERER__

#include "java/lang/String.h"

class CameraControlGizmo;

/**
Renders a `CameraControlGizmo` (a ViewCube) as a HUD over its area of a
viewport, with the GL4 core pipeline.

The parts of the gizmo (26 boxes and the compass tube) are drawn with
`OpenGL4GeometryRenderer`, lit from the HUD camera and seen through it, in a
GL viewport restricted to the area of the gizmo, with its own depth buffer
(the depth of that area is cleared). Their edges are drawn as lines from
their boundary representations (see `CameraControlGizmo::getEdgeSegments`).
An inactive gizmo is translucent: the fragments are blended with a constant
alpha (the opacity of the gizmo), as the mesh shaders of the pipeline do not
handle the opacity of materials. Labels depend on the way images are created
and cached by the application, so they are delegated to a `LabelPainter`.

All the OpenGL state changed is restored.

C++ counterpart of Java's `Jogl4CameraControlGizmoRenderer`.
*/
class OpenGL4CameraControlGizmoRenderer {
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
        */
        virtual void drawLabel(const java::String& label, double x,
                               double y) = 0;
    };

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
    null
    */
    static void draw(CameraControlGizmo* gizmo, int viewportStartX,
                     int viewportStartY, int viewportSizeY, float lineWidth,
                     LabelPainter* labelPainter);

private:
    static const float EDGE_DEPTH_BIAS;
    static const double MIN_LABEL_VISIBILITY;

    OpenGL4CameraControlGizmoRenderer();
};

#endif
