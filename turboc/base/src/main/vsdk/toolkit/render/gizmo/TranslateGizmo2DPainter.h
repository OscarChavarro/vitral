#ifndef __TRANSLATE_GIZMO_2D_PAINTER__
#define __TRANSLATE_GIZMO_2D_PAINTER__

class Camera;
class Cone;
class Gizmo2DDrawList;
class Gizmo2DProjector;
class SimpleBody;
class TranslateGizmo;
class ViewportElementScaler;

/**
Presents a `TranslateGizmo` as 2D primitives (see `Gizmo2DDrawList`), with
the same elements the OpenGL translate gizmo renderers draw: the lines of
the axes and plane handles (as quads facing the camera), the cones of the
axis heads (with a darker base) and the translucent quad of the selected
plane handle; plus the labels of the axes the viewport windows draw next to
their tips (yellow when the axis is highlighted).
*/
class TranslateGizmo2DPainter {
private:
    TranslateGizmo2DPainter();

    static void addCone(Gizmo2DProjector& projector,
                        const SimpleBody* element, const Cone* cone);
    static void addLabels(Gizmo2DProjector& projector, TranslateGizmo* gizmo,
                          const ViewportElementScaler* scaler);

public:
    /**
    @param out list receiving the primitives
    @param gizmo gizmo, with its camera and transformation matrix set
    @param camera camera of the viewport, with its viewport size updated
    @param scaler sizes of the elements for the resolution of the screen
    */
    static void paint(Gizmo2DDrawList* out, TranslateGizmo* gizmo,
                      const Camera* camera,
                      const ViewportElementScaler* scaler);
};

#endif
