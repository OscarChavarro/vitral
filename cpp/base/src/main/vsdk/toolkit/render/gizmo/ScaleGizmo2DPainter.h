#ifndef __SCALE_GIZMO_2D_PAINTER__
#define __SCALE_GIZMO_2D_PAINTER__

class Camera;
class Gizmo2DDrawList;
class ScaleGizmo;

/**
Presents a `ScaleGizmo` as 2D primitives (see `Gizmo2DDrawList`), with the
same elements the OpenGL scale gizmo renderers draw: the shafts of the axes
and the cubes at their tips (solid), the contour of the flat handles (lines)
and the translucent interior of the handle selected.
*/
class ScaleGizmo2DPainter {
private:
    ScaleGizmo2DPainter();

public:
    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera, transformation matrix and scale set
    @param camera camera of the viewport, with its viewport size updated
    */
    static void paint(Gizmo2DDrawList* out, ScaleGizmo* gizmo,
                      const Camera* camera);
};

#endif
