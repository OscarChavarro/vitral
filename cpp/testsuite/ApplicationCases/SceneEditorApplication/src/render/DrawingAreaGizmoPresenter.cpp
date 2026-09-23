#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/RotateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "model/DrawingArea.h"
#include "model/InteractionMode.h"
#include "model/selection/SceneSelectionEditor.h"
#include "render/DrawingAreaGizmoPresenter.h"

DrawingAreaGizmoPresenter::DrawingAreaGizmoPresenter(
    DrawingArea* drawingArea, SceneSelectionEditor* selectionEditor,
    TranslateGizmo* translationGizmo, RotateGizmo* rotateGizmo,
    ScaleGizmo* scaleGizmo)
    : drawingArea(drawingArea), viewportSet(drawingArea->getViewportSet()),
      selectionEditor(selectionEditor), translationGizmo(translationGizmo),
      rotateGizmo(rotateGizmo), scaleGizmo(scaleGizmo), inputGizmo(nullptr)
{
}

TranslateGizmo* DrawingAreaGizmoPresenter::getTranslationGizmo() const
{
    return translationGizmo;
}

RotateGizmo* DrawingAreaGizmoPresenter::getRotateGizmo() const
{
    return rotateGizmo;
}

ScaleGizmo* DrawingAreaGizmoPresenter::getScaleGizmo() const
{
    return scaleGizmo;
}

InputGizmo* DrawingAreaGizmoPresenter::getInputGizmo() const
{
    return inputGizmo;
}

DrawingAreaGizmoPresenter::GizmoKind
DrawingAreaGizmoPresenter::prepareForView(Camera* camera)
{
    inputGizmo = nullptr;

    translationGizmo->setCamera(camera);
    rotateGizmo->setCamera(camera);
    scaleGizmo->setCamera(camera);
    // Size and line width follow the resolution of the screen
    translationGizmo->applyScale(viewportSet->getElementScaler());
    rotateGizmo->applyScale(viewportSet->getElementScaler());
    scaleGizmo->applyScale(viewportSet->getElementScaler());

    SimpleBody* selectedBody = selectionEditor->getFirstSelectedBody();
    InteractionMode mode = drawingArea->getInteractionMode();

    if ( drawingArea->shouldDrawTranslationGizmo() ) {
        Vector3Dd centroid;

        if ( selectionEditor->computeSelectionCentroid(&centroid) ) {
            translationGizmo->setTransformationMatrix(
                SceneSelectionEditor::createTranslationGizmoMatrix(centroid));
            inputGizmo = translationGizmo->getInputGizmo();
            return GizmoKind::TRANSLATE;
        }
    }
    else if ( mode == InteractionMode::ROTATE ) {
        if ( selectedBody != nullptr ) {
            rotateGizmo->setTransformationMatrix(
                SceneSelectionEditor::createRotationGizmoMatrix(selectedBody));
            inputGizmo = rotateGizmo->getInputGizmo();
            return GizmoKind::ROTATE;
        }
    }
    else if ( mode == InteractionMode::SCALE && selectedBody != nullptr ) {
        scaleGizmo->setTransformationMatrix(
            SceneSelectionEditor::createRotationGizmoMatrix(selectedBody));
        scaleGizmo->setScale(selectedBody->getScale());
        inputGizmo = scaleGizmo->getInputGizmo();
        return GizmoKind::SCALE;
    }
    return GizmoKind::NONE;
}
