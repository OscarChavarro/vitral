#ifndef __DRAWING_AREA_GIZMO_PRESENTER__
#define __DRAWING_AREA_GIZMO_PRESENTER__

class Camera;
class DrawingArea;
class InputGizmo;
class RotateGizmo;
class ScaleGizmo;
class SceneSelectionEditor;
class TranslateGizmo;
class ViewportSet;

/**
Decides which manipulation gizmo of a `DrawingArea` must be presented in a
view (following the interaction mode and the selection) and places it over
the selection, seen from the camera of the view. It does not draw: renderers
of each technology draw the gizmo it prepares. Everything is referenced, not
owned.
*/
class DrawingAreaGizmoPresenter {
public:
    /**
    Gizmo to present in a view.
    */
    enum class GizmoKind {
        NONE,
        TRANSLATE,
        ROTATE,
        SCALE
    };

private:
    DrawingArea* drawingArea;
    ViewportSet* viewportSet;
    SceneSelectionEditor* selectionEditor;
    TranslateGizmo* translationGizmo;
    RotateGizmo* rotateGizmo;
    ScaleGizmo* scaleGizmo;
    /// Input gizmo of the gizmo prepared, or null if it has none
    InputGizmo* inputGizmo;

public:
    /**
    @param drawingArea drawing area whose interaction mode is followed
    @param selectionEditor selection over which gizmos are placed
    @param translationGizmo gizmo to present in translation mode
    @param rotateGizmo gizmo to present in rotation mode
    @param scaleGizmo gizmo to present in scale mode
    */
    DrawingAreaGizmoPresenter(DrawingArea* drawingArea,
                              SceneSelectionEditor* selectionEditor,
                              TranslateGizmo* translationGizmo,
                              RotateGizmo* rotateGizmo,
                              ScaleGizmo* scaleGizmo);

    TranslateGizmo* getTranslationGizmo() const;
    RotateGizmo* getRotateGizmo() const;
    ScaleGizmo* getScaleGizmo() const;

    /**
    @return the input gizmo of the gizmo prepared by the last call to
    `prepareForView`, or null if it has none
    */
    InputGizmo* getInputGizmo() const;

    /**
    Selects the gizmo to present in a view and places it.
    @param camera camera of the view
    @return the gizmo to draw, already placed and scaled for the view
    */
    GizmoKind prepareForView(Camera* camera);
};

#endif
