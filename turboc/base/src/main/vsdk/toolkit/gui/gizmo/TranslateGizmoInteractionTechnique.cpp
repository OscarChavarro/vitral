#include <float.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/environment/camera/Camera.h"
#include "vsdk/toolkit/environment/geometry/element/Ray.h"
#include "vsdk/toolkit/environment/geometry/surface/InfinitePlane.h"
#include "vsdk/toolkit/environment/scene/SimpleBody.h"
#include "vsdk/toolkit/gui/gizmo/InputGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmo.h"
#include "vsdk/toolkit/gui/gizmo/TranslateGizmoInteractionTechnique.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/common/Cpp98Support.h"

static const double KEY_MOVEMENT_STEP = 0.1;

/// Events received after a cursor warp request and before the cursor
/// arrives to its new position still have the old position: they are
/// ignored, up to this number of them (if the warp never happens, cursor
/// wrapping is given up for the rest of the gesture)
static const int MAX_STALE_EVENTS_AFTER_WARP = 20;
static const int MIN_WARP_ARRIVAL_TOLERANCE = 8;

/// sin^2 of the minimum angle (10 degrees) between an axis and the view ray
static const double AXIS_PARALLEL_TO_RAY_LIMIT =
    pow(sin(10.0 * M_PI / 180.0), 2);

/// Interaction techniques used to move the gizmo depending on the group
static const int MOVE_ALONG_AXIS = 1;
static const int MOVE_OVER_PLANE = 2;

static int floorMod(int x, int y)
{
    int m = x % y;
    return m < 0 ? m + y : m;
}

TranslateGizmoInteractionTechnique::TranslateGizmoInteractionTechnique(
    TranslateGizmo* gizmo)
    : gizmo(gizmo), active(false), cursorWrapEnabled(false)
{
    endGesture();
}

TranslateGizmo* TranslateGizmoInteractionTechnique::getGizmo() const
{
    return gizmo;
}

bool TranslateGizmoInteractionTechnique::isInputGizmoKey(
    const KeyEvent& keyEvent)
{
    return gizmo->getInputGizmo()->consumesKey(keyEvent);
}

bool TranslateGizmoInteractionTechnique::isActive() const
{
    return active;
}

Viewport* TranslateGizmoInteractionTechnique::getDragViewport() const
{
    return dragViewport;
}

void TranslateGizmoInteractionTechnique::setCursorWrapEnabled(
    bool cursorWrapEnabled)
{
    this->cursorWrapEnabled = cursorWrapEnabled;
}

bool TranslateGizmoInteractionTechnique::isCursorWrapEnabled() const
{
    return cursorWrapEnabled;
}

bool TranslateGizmoInteractionTechnique::consumeCursorWarp(
    CursorWarp* outWarp)
{
    bool hadWarp = hasPendingWarp;

    if ( hadWarp && outWarp != NULL ) {
        *outWarp = pendingWarp;
    }
    hasPendingWarp = false;
    return hadWarp;
}

bool TranslateGizmoInteractionTechnique::processMouseEvent(const MouseEvent&)
{
    return false;
}

bool TranslateGizmoInteractionTechnique::processKeyPressedEvent(
    const KeyEvent& keyEvent)
{
    bool updateNeeded = false;

    gizmo->setSelectedResizing(true);
    gizmo->updateGeometryState();

    InputGizmo* inputGizmo = gizmo->getInputGizmo();

    if ( inputGizmo->processKeyPressedEvent(keyEvent) ) {
        if ( inputGizmo->consumeCommit() ) {
            ArrayList<double> target = inputGizmo->getValuesWithEdits();

            inputGizmo->cancelEditing();
            gizmo->setPosition(Vector3Dd(target[0], target[1], target[2]));
            return true;
        }
        return false;
    }

    if ( keyEvent.unicodeId != KeyEvent::KEY_NONE ) {
        Vector3Dd p = gizmo->getPosition();
        bool isMovementKey = strchr("xXyYzZ", keyEvent.unicodeId) != NULL;

        if ( isMovementKey ) {
            gizmo->getInputGizmo()->cancelEditing();
        }

        switch ( keyEvent.unicodeId ) {
          case 'x':
            gizmo->setPosition(Vector3Dd(p.x() - KEY_MOVEMENT_STEP, p.y(), p.z()));
            updateNeeded = true;
            break;
          case 'X':
            gizmo->setPosition(Vector3Dd(p.x() + KEY_MOVEMENT_STEP, p.y(), p.z()));
            updateNeeded = true;
            break;
          case 'y':
            gizmo->setPosition(Vector3Dd(p.x(), p.y() - KEY_MOVEMENT_STEP, p.z()));
            updateNeeded = true;
            break;
          case 'Y':
            gizmo->setPosition(Vector3Dd(p.x(), p.y() + KEY_MOVEMENT_STEP, p.z()));
            updateNeeded = true;
            break;
          case 'z':
            gizmo->setPosition(Vector3Dd(p.x(), p.y(), p.z() - KEY_MOVEMENT_STEP));
            updateNeeded = true;
            break;
          case 'Z':
            gizmo->setPosition(Vector3Dd(p.x(), p.y(), p.z() + KEY_MOVEMENT_STEP));
            updateNeeded = true;
            break;
          default:
            break;
        }
    }

    return updateNeeded;
}

bool TranslateGizmoInteractionTechnique::processKeyReleasedEvent(
    const KeyEvent&)
{
    return false;
}

bool TranslateGizmoInteractionTechnique::processMousePressedEvent(
    const MouseEvent& e, Viewport* viewport)
{
    bool changed = processMousePressedEvent(e);

    if ( viewport != NULL &&
         gizmo->getCurrentSelection() != TranslateGizmo::NULL_GROUP ) {
        dragViewport = viewport;
        dragSelection = gizmo->getCurrentSelection();
        lastVirtualX = e.getX();
        lastVirtualY = e.getY();
    }
    return changed;
}

bool TranslateGizmoInteractionTechnique::processMousePressedEvent(
    const MouseEvent& e)
{
    endGesture();
    Vector3Dd p;

    if ( !calculateInteractionPoint(e, &p) ) {
        lastDeltaPosition = Vector3Dd();
    }
    else {
        lastDeltaPosition = p.subtract(gizmo->getPosition());
    }
    return false;
}

bool TranslateGizmoInteractionTechnique::processMouseReleasedEvent(
    const MouseEvent&)
{
    endGesture();
    gizmo->setSelectedResizing(true);
    gizmo->updateGeometryState();
    return true;
}

bool TranslateGizmoInteractionTechnique::processMouseClickedEvent(
    const MouseEvent& e)
{
    gizmo->setSelectedResizing(true);
    gizmo->updateGeometryState();

    int previousSelection = gizmo->getCurrentSelection();
    int selection = calculateSelection(e.getX(), e.getY());

    if ( selection == TranslateGizmo::NULL_GROUP ) {
        selection = previousSelection;
    }
    gizmo->setPersistentSelection(selection);

    return selection != previousSelection;
}

bool TranslateGizmoInteractionTechnique::processMouseMovedEvent(
    const MouseEvent& e)
{
    if ( dragViewport != NULL ) {
        // The selection of the gizmo is fixed while dragging. Moved events
        // still can come (i.e. when the cursor is placed by the caller)
        // and they inform the cursor arrived to its new position
        checkWarpArrival(e.getX(), e.getY());
        return false;
    }

    gizmo->setSelectedResizing(true);
    gizmo->updateGeometryState();

    int previousSelection = gizmo->getCurrentSelection();
    int selection = calculateSelection(e.getX(), e.getY());

    gizmo->setVolatileSelection(selection);

    return selection != previousSelection;
}

bool TranslateGizmoInteractionTechnique::processMouseDraggedEvent(
    const MouseEvent& e)
{
    MouseEvent event = e;

    if ( dragViewport != NULL ) {
        if ( !toVirtualEvent(e, &event) ) {
            // Old event, received before the cursor arrives to its new place
            return false;
        }
    }
    Vector3Dd p;

    if ( !calculateInteractionPoint(event, &p) ) {
        return false;
    }

    gizmo->getInputGizmo()->cancelEditing();
    gizmo->setPosition(p.subtract(lastDeltaPosition));
    gizmo->setSelectedResizing(false);
    return true;
}

bool TranslateGizmoInteractionTechnique::processMouseWheelEvent(
    const MouseEvent&)
{
    return false;
}

void TranslateGizmoInteractionTechnique::endGesture()
{
    dragViewport = NULL;
    dragSelection = TranslateGizmo::NULL_GROUP;
    wrappingSuspended = false;
    wrapOffsetX = 0;
    wrapOffsetY = 0;
    lastVirtualX = 0;
    lastVirtualY = 0;
    awaitingWarp = false;
    warpTargetX = 0;
    warpTargetY = 0;
    staleEvents = 0;
    hasPendingWarp = false;
}

bool TranslateGizmoInteractionTechnique::checkWarpArrival(int x, int y)
{
    if ( !awaitingWarp ) {
        return true;
    }
    int minSize = dragViewport->getPixelSizeX() < dragViewport->getPixelSizeY() ?
        dragViewport->getPixelSizeX() : dragViewport->getPixelSizeY();
    int tolerance = MIN_WARP_ARRIVAL_TOLERANCE > minSize / 4 ?
        MIN_WARP_ARRIVAL_TOLERANCE : minSize / 4;

    if ( Cpp98Support::abs(x - warpTargetX) <= tolerance &&
         Cpp98Support::abs(y - warpTargetY) <= tolerance ) {
        awaitingWarp = false;
        return true;
    }
    return false;
}

bool TranslateGizmoInteractionTechnique::toVirtualEvent(
    const MouseEvent& e, MouseEvent* outVirtual)
{
    int x = e.getX();
    int y = e.getY();
    int width = dragViewport->getPixelSizeX();
    int height = dragViewport->getPixelSizeY();

    if ( awaitingWarp ) {
        if ( checkWarpArrival(x, y) ) {
            // Cursor already at its new place
        }
        else if ( ++staleEvents > MAX_STALE_EVENTS_AFTER_WARP ) {
            // The cursor was not placed: go on without wrapping, keeping
            // the continuity of the virtual coordinates
            wrappingSuspended = true;
            awaitingWarp = false;
            wrapOffsetX = lastVirtualX - x;
            wrapOffsetY = lastVirtualY - y;
        }
        else {
            return false;
        }
    }

    int virtualX = x + wrapOffsetX;
    int virtualY = y + wrapOffsetY;

    if ( cursorWrapEnabled && !wrappingSuspended && width > 0 && height > 0 &&
         (x < 0 || x >= width || y < 0 || y >= height) ) {
        int wrappedX = floorMod(x, width);
        int wrappedY = floorMod(y, height);

        // The virtual position does not change: only the real one does
        wrapOffsetX += x - wrappedX;
        wrapOffsetY += y - wrappedY;
        warpTargetX = wrappedX;
        warpTargetY = wrappedY;
        awaitingWarp = true;
        staleEvents = 0;
        pendingWarp = CursorWarp(wrappedX, wrappedY);
        hasPendingWarp = true;
    }
    lastVirtualX = virtualX;
    lastVirtualY = virtualY;

    MouseEvent virtualEvent;

    virtualEvent.setX(virtualX);
    virtualEvent.setY(virtualY);
    virtualEvent.setButton(e.getButton());
    virtualEvent.setModifiers(e.getModifiers());
    virtualEvent.setClicks(e.getClicks());
    *outVirtual = virtualEvent;
    return true;
}

int TranslateGizmoInteractionTechnique::calculateSelection(int x, int y)
{
    Camera* camera = gizmo->getCamera();
    ArrayList<SimpleBody*>& elements = gizmo->getElements();

    camera->updateVectors();
    Ray r = camera->generateRay(x, y);
    double nearestDistance = DBL_MAX;
    int nearestElement = -1;
    int index = 1;

    // Box elements are only for display, they do not affect selections
    for ( int i = 0;
          index <= TranslateGizmo::XZX_SEGMENT_ELEMENT && i < elements.size();
          index++, i++ ) {
        r = r.withT(DBL_MAX);
        SimpleBody* element = elements.get(i);

        Ray* hit = element->getGeometry() != NULL ?
            element->doIntersectionFirstHit(r) : NULL;
        if ( hit != NULL && hit->getT() < nearestDistance ) {
            nearestDistance = hit->getT();
            nearestElement = index;
        }
        delete hit;
    }

    int selection;
    switch ( nearestElement ) {
      case TranslateGizmo::X_AXIS_ELEMENT:
        selection = TranslateGizmo::X_AXIS_GROUP;
        break;
      case TranslateGizmo::Y_AXIS_ELEMENT:
        selection = TranslateGizmo::Y_AXIS_GROUP;
        break;
      case TranslateGizmo::Z_AXIS_ELEMENT:
        selection = TranslateGizmo::Z_AXIS_GROUP;
        break;
      case TranslateGizmo::XYY_SEGMENT_ELEMENT:
      case TranslateGizmo::XYX_SEGMENT_ELEMENT:
        selection = TranslateGizmo::XY_PLANE_GROUP;
        break;
      case TranslateGizmo::YZZ_SEGMENT_ELEMENT:
      case TranslateGizmo::YZY_SEGMENT_ELEMENT:
        selection = TranslateGizmo::YZ_PLANE_GROUP;
        break;
      case TranslateGizmo::XZZ_SEGMENT_ELEMENT:
      case TranslateGizmo::XZX_SEGMENT_ELEMENT:
        selection = TranslateGizmo::XZ_PLANE_GROUP;
        break;
      default:
        selection = TranslateGizmo::NULL_GROUP;
        break;
    }

    active = selection != TranslateGizmo::NULL_GROUP;

    return selection;
}

bool TranslateGizmoInteractionTechnique::calculateInteractionPoint(
    const MouseEvent& e, Vector3Dd* outPoint)
{
    Camera* camera = gizmo->getCamera();
    Vector3Dd v;
    bool hasDirection = true;
    int technique = 0;
    int group = dragViewport != NULL ?
        dragSelection : gizmo->getCurrentSelection();

    switch ( group ) {
      case TranslateGizmo::X_AXIS_GROUP:
        v = Vector3Dd(1, 0, 0);
        technique = MOVE_ALONG_AXIS;
        break;
      case TranslateGizmo::Y_AXIS_GROUP:
        v = Vector3Dd(0, 1, 0);
        technique = MOVE_ALONG_AXIS;
        break;
      case TranslateGizmo::Z_AXIS_GROUP:
        v = Vector3Dd(0, 0, 1);
        technique = MOVE_ALONG_AXIS;
        break;
      case TranslateGizmo::XY_PLANE_GROUP:
        v = Vector3Dd(0, 0, 1);
        technique = MOVE_OVER_PLANE;
        break;
      case TranslateGizmo::YZ_PLANE_GROUP:
        v = Vector3Dd(1, 0, 0);
        technique = MOVE_OVER_PLANE;
        break;
      case TranslateGizmo::XZ_PLANE_GROUP:
        v = Vector3Dd(0, 1, 0);
        technique = MOVE_OVER_PLANE;
        break;
      default:
        hasDirection = false;
        break;
    }

    if ( !hasDirection || camera == NULL ) {
        return false;
    }

    Vector3Dd o = gizmo->getPosition();
    int mouseX = e.getX();
    int mouseY = e.getY();

    camera->updateVectors();

    if ( technique == MOVE_OVER_PLANE ) {
        Ray r = camera->generateRay(mouseX, mouseY);

        if ( r.getDirection().dotProduct(v) > 0 ) {
            v = v.multiply(-1);
        }
        InfinitePlane plane(v, o);
        Ray* hit = plane.doIntersectionFirstHit(r);

        if ( hit == NULL ) {
            *outPoint = o;
            return true;
        }
        *outPoint = hit->getDirection().multiply(hit->getT()).add(hit->getOrigin());
        delete hit;
        return true;
    }

    // Move along an axis: closest point of the axis line to the cursor's
    // projector ray. It depends only on the cursor position (never on its
    // last movement), so it is continuous for perspective and orthogonal
    // cameras alike.
    Ray r = camera->generateRay(mouseX, mouseY);
    Vector3Dd axis = v.normalized();
    Vector3Dd rayDirection = r.getDirection().normalized();
    Vector3Dd w0 = o.subtract(r.getOrigin());
    double b = axis.dotProduct(rayDirection);
    double denominator = 1.0 - b * b;

    if ( denominator < AXIS_PARALLEL_TO_RAY_LIMIT ) {
        // Looking (almost) along the axis: cursor does not define a point
        return false;
    }

    double s = (b * rayDirection.dotProduct(w0) - axis.dotProduct(w0)) /
        denominator;

    *outPoint = o.add(axis.multiply(s));
    return true;
}
