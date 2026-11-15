#ifndef __ROTATE_GIZMO_2D_PAINTER__
#define __ROTATE_GIZMO_2D_PAINTER__

class Camera;
class Gizmo2DDrawList;
class RotateGizmo;
class ViewportElementScaler;

/**
Presents a `RotateGizmo` as 2D primitives (see `Gizmo2DDrawList`), with the
same elements the OpenGL rotate gizmo renderers draw: the visible part of
its three axis rings and the camera ring (as quads facing the camera, with
the colors the gizmo gives them) and, while a ring is dragged, the
translucent sector of the swept arc, plus the label with its angle that the
viewport windows draw next to the middle of the arc.
*/
class RotateGizmo2DPainter {
private:
    RotateGizmo2DPainter();

public:
    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera and transformation matrix set
    @param camera camera of the viewport, with its viewport size updated
    @param scaler sizes of the elements for the resolution of the screen
    */
    static void paint(Gizmo2DDrawList* out, const RotateGizmo* gizmo,
                      const Camera* camera,
                      const ViewportElementScaler* scaler);
};

#endif
