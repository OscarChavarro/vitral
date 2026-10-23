#include <stddef.h>
#include <math.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmo.h"
#include "vsdk/toolkit/gui/gizmo/ScaleGizmoInteractionTechnique.h"

/// Below this distance (in pixels) from the projected origin, the drag
/// ratio is not measured (it would be too sensitive, or undefined at 0)
static const double MIN_DRAG_DISTANCE = 4.0;

ScaleGizmoInteractionTechnique::ScaleGizmoInteractionTechnique(
    ScaleGizmo* gizmo)
    : gizmo(gizmo), active(FALSE)
{
    endGesture();
}

ScaleGizmo* ScaleGizmoInteractionTechnique::getGizmo() const
{
    return gizmo;
}

BOOLEAN ScaleGizmoInteractionTechnique::isActive() const
{
    return active;
}

Viewport* ScaleGizmoInteractionTechnique::getDragViewport() const
{
    return dragViewport;
}

BOOLEAN ScaleGizmoInteractionTechnique::isInputGizmoKey(const KeyEvent& keyEvent)
{
    return gizmo->getInputGizmo()->consumesKey(keyEvent);
}

BOOLEAN ScaleGizmoInteractionTechnique::processKeyPressedEvent(
    const KeyEvent& keyEvent)
{
    return gizmo->processKeyPressedEvent(keyEvent);
}

BOOLEAN ScaleGizmoInteractionTechnique::processKeyReleasedEvent(const KeyEvent&)
{
    return FALSE;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMousePressedEvent(
    const MouseEvent& e, Viewport* viewport)
{
    BOOLEAN changed = processMousePressedEvent(e);

    dragViewport = isDragging() ? viewport : NULL;
    return changed;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMousePressedEvent(
    const MouseEvent& e)
{
    endGesture();

    int selection = calculateSelection(e.getX(), e.getY());

    if ( selection != ScaleGizmo::NULL_GROUP ) {
        gizmo->setVolatileSelection(selection);
        dragGroup = selection;
        dragStartScale = gizmo->getScale();
        hasDragStartScale = TRUE;
        dragStartDistance = distanceToOrigin(e.getX(), e.getY());
    }
    return FALSE;
}

BOOLEAN ScaleGizmoInteractionTechnique::isDragging() const
{
    return dragGroup != ScaleGizmo::NULL_GROUP;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMouseReleasedEvent(
    const MouseEvent&)
{
    endGesture();
    return FALSE;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMouseClickedEvent(
    const MouseEvent& e)
{
    int previousSelection = gizmo->getCurrentSelection();
    int selection = calculateSelection(e.getX(), e.getY());

    if ( selection == ScaleGizmo::NULL_GROUP ) {
        selection = previousSelection;
    }
    gizmo->setPersistentSelection(selection);

    return selection != previousSelection;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMouseMovedEvent(
    const MouseEvent& e)
{
    if ( isDragging() ) {
        // The selection of the gizmo is fixed while dragging
        return FALSE;
    }

    int previousSelection = gizmo->getCurrentSelection();
    int selection = calculateSelection(e.getX(), e.getY());

    gizmo->setVolatileSelection(selection);

    return selection != previousSelection;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMouseDraggedEvent(
    const MouseEvent& e)
{
    if ( !isDragging() || !hasDragStartScale ) {
        return FALSE;
    }

    double distance = distanceToOrigin(e.getX(), e.getY());
    double ratio = dragStartDistance >= MIN_DRAG_DISTANCE ?
        distance/dragStartDistance : 1.0;
    Vector3Dd newScale(
        scaledIfIncluded(0, ratio),
        scaledIfIncluded(1, ratio),
        scaledIfIncluded(2, ratio));

    gizmo->getInputGizmo()->cancelEditing();
    gizmo->setScale(newScale);
    return TRUE;
}

BOOLEAN ScaleGizmoInteractionTechnique::processMouseWheelEvent(const MouseEvent&)
{
    return FALSE;
}

double ScaleGizmoInteractionTechnique::scaledIfIncluded(int axis,
                                                        double ratio) const
{
    double value;

    switch ( axis ) {
      case 0:
        value = dragStartScale.x();
        break;
      case 1:
        value = dragStartScale.y();
        break;
      default:
        value = dragStartScale.z();
        break;
    }

    return ScaleGizmo::groupIncludesAxis(dragGroup, axis) ?
        value*ratio : value;
}

void ScaleGizmoInteractionTechnique::endGesture()
{
    dragViewport = NULL;
    dragGroup = ScaleGizmo::NULL_GROUP;
    hasDragStartScale = FALSE;
    dragStartScale = Vector3Dd();
    dragStartDistance = 0.0;
}

int ScaleGizmoInteractionTechnique::calculateSelection(int x, int y)
{
    Camera* camera = gizmo->getCamera();

    if ( camera == NULL ) {
        active = FALSE;
        return ScaleGizmo::NULL_GROUP;
    }
    camera->updateVectors();

    Ray ray = camera->generateRay(x, y);
    int selection = gizmo->pickElement(ray);

    active = selection != ScaleGizmo::NULL_GROUP;

    return selection;
}

double ScaleGizmoInteractionTechnique::distanceToOrigin(int x, int y)
{
    Camera* camera = gizmo->getCamera();

    if ( camera == NULL ) {
        return 0.0;
    }
    camera->updateVectors();

    Vector3Dd projected;

    if ( !camera->projectPointUsingRayMethod(gizmo->getPosition(),
                                             &projected) ) {
        return 0.0;
    }

    double dx = x - projected.x();
    double dy = y - projected.y();

    return sqrt(dx*dx + dy*dy);
}
