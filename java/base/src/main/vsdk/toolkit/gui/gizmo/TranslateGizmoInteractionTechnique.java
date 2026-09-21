package vsdk.toolkit.gui.gizmo;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.viewport.Viewport;

/**
Interaction technique to move a `TranslateGizmo` with the keyboard and the
mouse, processing only vitral events.

Mouse events must have coordinates in pixels of the viewport they are
processed for, with origin at its upper left corner.

Infinite drag: if the gesture is started with
`processMousePressedEvent(MouseEvent, Viewport)` over a handle of the gizmo,
the gesture belongs to that viewport until the button is released (see
`getDragViewport`), so the caller must keep on feeding it the events of the
gesture, whatever viewport the cursor is over. When the cursor leaves the
viewport, the technique keeps on working with "virtual" cursor coordinates
(the real ones plus the size of the viewport for each time the cursor wrapped
around) so the movement is continuous, and, if cursor wrapping is enabled,
requests to place the cursor at the opposite side of the viewport (see
`consumeCursorWarp`). Cursors are placed by the caller, as this technique knows
nothing about the GUI technology in use.

Numeric input: the technique also feeds the keyboard to the `InputGizmo` of the
gizmo, that shows (and lets the user type) its coordinates. Typing a
number and pressing ENTER moves the gizmo (so the caller, that applies its
movement to the things it manipulates, moves them precisely there); moving the
gizmo any other way discards what was typed.
*/
public class TranslateGizmoInteractionTechnique {
    private static final double KEY_MOVEMENT_STEP = 0.1;

    /// Events received after a cursor warp request and before the cursor
    /// arrives to its new position still have the old position: they are
    /// ignored, up to this number of them (if the warp never happens, cursor
    /// wrapping is given up for the rest of the gesture)
    private static final int MAX_STALE_EVENTS_AFTER_WARP = 20;
    private static final int MIN_WARP_ARRIVAL_TOLERANCE = 8;

    /// sin^2 of the minimum angle (10 degrees) between an axis and the view ray
    private static final double AXIS_PARALLEL_TO_RAY_LIMIT =
        Math.pow(Math.sin(Math.toRadians(10.0)), 2);

    /// Interaction techniques used to move the gizmo depending on the group
    private static final int MOVE_ALONG_AXIS = 1;
    private static final int MOVE_OVER_PLANE = 2;

    private final TranslateGizmo gizmo;

    private Vector3Dd lastDeltaPosition;
    private boolean active;

    // State of the gesture in course (drag confined to a viewport)
    private boolean cursorWrapEnabled;
    private Viewport dragViewport;
    private int dragSelection;
    private boolean wrappingSuspended;
    private int wrapOffsetX;
    private int wrapOffsetY;
    private int lastVirtualX;
    private int lastVirtualY;
    private boolean awaitingWarp;
    private int warpTargetX;
    private int warpTargetY;
    private int staleEvents;
    private CursorWarp pendingWarp;

    /**
    Request to place the cursor at a position of the viewport where the
    gesture started, in pixels of that viewport (origin at its upper left
    corner).
    @param x
    @param y
    */
    public record CursorWarp(int x, int y) {
    }

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public TranslateGizmoInteractionTechnique(TranslateGizmo gizmo)
    {
        this.gizmo = gizmo;
        lastDeltaPosition = new Vector3Dd();
        active = false;
        cursorWrapEnabled = false;
        endGesture();
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public TranslateGizmo getGizmo()
    {
        return gizmo;
    }

    /**
    @param keyEvent key press
    @return true if the input gizmo uses the key (digits, `-`, decimal point,
    TAB, BACKSPACE, and ENTER and ESC while editing), so the caller must
    not process it as any other command
    */
    public boolean isInputGizmoKey(KeyEvent keyEvent)
    {
        return gizmo.getInputGizmo().consumesKey(keyEvent);
    }

    /**
    @return true if the last cursor position was over an axis or plane handle
    of the gizmo
    */
    public boolean isActive()
    {
        return active;
    }

    /**
    @return the viewport where the gesture in course started, or null if there
    is no gesture in course, that is, if the button was not pressed over a
    handle of the gizmo or it has been released
    */
    public Viewport getDragViewport()
    {
        return dragViewport;
    }

    /**
    Enables the wrapping of the cursor around the viewport while dragging. It
    should be enabled only if the caller is able to place the cursor when a
    warp is requested. It is disabled by default: the gesture is still
    confined to its viewport, but the cursor can leave it.
    @param cursorWrapEnabled
    */
    public void setCursorWrapEnabled(boolean cursorWrapEnabled)
    {
        this.cursorWrapEnabled = cursorWrapEnabled;
    }

    public boolean isCursorWrapEnabled()
    {
        return cursorWrapEnabled;
    }

    /**
    The caller should place the cursor as requested after processing each
    dragged event, and events already in course will be ignored by the
    technique until the cursor arrives.
    @return the pending request to place the cursor, or null if there is
    none. The request is discarded after being returned.
    */
    public CursorWarp consumeCursorWarp()
    {
        CursorWarp warp = pendingWarp;

        pendingWarp = null;
        return warp;
    }

    public boolean processMouseEvent(MouseEvent mouseEvent)
    {
        return false;
    }

    public boolean processKeyPressedEvent(KeyEvent keyEvent)
    {
        boolean updateNeeded = false;

        gizmo.setSelectedResizing(true);
        gizmo.updateGeometryState();

        InputGizmo inputGizmo = gizmo.getInputGizmo();

        if ( inputGizmo.processKeyPressedEvent(keyEvent) ) {
            if ( inputGizmo.consumeCommit() ) {
                double[] target = inputGizmo.getValuesWithEdits();

                inputGizmo.cancelEditing();
                gizmo.setPosition(new Vector3Dd(target[0], target[1], target[2]));
                return true;
            }
            return false;
        }

        if ( keyEvent.unicode_id != KeyEvent.KEY_NONE ) {
            Vector3Dd p = gizmo.getPosition();
            boolean isMovementKey = "xXyYzZ".indexOf(keyEvent.unicode_id) >= 0;

            if ( isMovementKey ) {
                gizmo.getInputGizmo().cancelEditing();
            }

            switch ( keyEvent.unicode_id ) {
              case 'x':
                gizmo.setPosition(new Vector3Dd(p.x() - KEY_MOVEMENT_STEP, p.y(), p.z()));
                updateNeeded = true;
                break;
              case 'X':
                gizmo.setPosition(new Vector3Dd(p.x() + KEY_MOVEMENT_STEP, p.y(), p.z()));
                updateNeeded = true;
                break;
              case 'y':
                gizmo.setPosition(new Vector3Dd(p.x(), p.y() - KEY_MOVEMENT_STEP, p.z()));
                updateNeeded = true;
                break;
              case 'Y':
                gizmo.setPosition(new Vector3Dd(p.x(), p.y() + KEY_MOVEMENT_STEP, p.z()));
                updateNeeded = true;
                break;
              case 'z':
                gizmo.setPosition(new Vector3Dd(p.x(), p.y(), p.z() - KEY_MOVEMENT_STEP));
                updateNeeded = true;
                break;
              case 'Z':
                gizmo.setPosition(new Vector3Dd(p.x(), p.y(), p.z() + KEY_MOVEMENT_STEP));
                updateNeeded = true;
                break;
              default:
                break;
            }
        }

        return updateNeeded;
    }

    public boolean processKeyReleasedEvent(KeyEvent keyEvent)
    {
        return false;
    }

    /**
    Processes the press of a mouse button, starting a gesture confined to the
    given viewport if a handle of the gizmo is selected (that is, if dragging
    is going to move the gizmo).
    @param e event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed
    @return false (a press never changes the gizmo)
    */
    public boolean processMousePressedEvent(MouseEvent e, Viewport viewport)
    {
        boolean changed = processMousePressedEvent(e);

        if ( viewport != null &&
             gizmo.getCurrentSelection() != TranslateGizmo.NULL_GROUP ) {
            dragViewport = viewport;
            dragSelection = gizmo.getCurrentSelection();
            lastVirtualX = e.getX();
            lastVirtualY = e.getY();
        }
        return changed;
    }

    /**
    Processes the press of a mouse button, without confining the gesture to
    any viewport.
    @param e event with coordinates relative to the viewport
    @return false (a press never changes the gizmo)
    */
    public boolean processMousePressedEvent(MouseEvent e)
    {
        endGesture();
        Vector3Dd p = calculateInteractionPoint(e);

        if ( p == null ) {
            lastDeltaPosition = new Vector3Dd();
        }
        else {
            lastDeltaPosition = p.subtract(gizmo.getPosition());
        }
        return false;
    }

    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        endGesture();
        gizmo.setSelectedResizing(true);
        gizmo.updateGeometryState();
        return true;
    }

    public boolean processMouseClickedEvent(MouseEvent e)
    {
        gizmo.setSelectedResizing(true);
        gizmo.updateGeometryState();

        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        if ( selection == TranslateGizmo.NULL_GROUP ) {
            selection = previousSelection;
        }
        gizmo.setPersistentSelection(selection);

        return selection != previousSelection;
    }

    public boolean processMouseMovedEvent(MouseEvent e)
    {
        if ( dragViewport != null ) {
            // The selection of the gizmo is fixed while dragging. Moved events
            // still can come (i.e. when the cursor is placed by the caller)
            // and they inform the cursor arrived to its new position
            checkWarpArrival(e.getX(), e.getY());
            return false;
        }

        gizmo.setSelectedResizing(true);
        gizmo.updateGeometryState();

        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        gizmo.setVolatileSelection(selection);

        return selection != previousSelection;
    }

    public boolean processMouseDraggedEvent(MouseEvent e)
    {
        MouseEvent event = e;

        if ( dragViewport != null ) {
            event = toVirtualEvent(e);
            if ( event == null ) {
                // Old event, received before the cursor arrives to its new place
                return false;
            }
        }
        Vector3Dd p = calculateInteractionPoint(event);

        if ( p == null ) {
            return false;
        }

        gizmo.getInputGizmo().cancelEditing();
        gizmo.setPosition(p.subtract(lastDeltaPosition));
        gizmo.setSelectedResizing(false);
        return true;
    }

    public boolean processMouseWheelEvent(MouseEvent e)
    {
        return false;
    }

    private void endGesture()
    {
        dragViewport = null;
        dragSelection = TranslateGizmo.NULL_GROUP;
        wrappingSuspended = false;
        wrapOffsetX = 0;
        wrapOffsetY = 0;
        lastVirtualX = 0;
        lastVirtualY = 0;
        awaitingWarp = false;
        warpTargetX = 0;
        warpTargetY = 0;
        staleEvents = 0;
        pendingWarp = null;
    }

    /**
    Checks if the cursor arrived to the place it was requested to be placed
    at, ending the wait for it.
    @return true if the technique was not waiting for the cursor or it arrived
    */
    private boolean checkWarpArrival(int x, int y)
    {
        if ( !awaitingWarp ) {
            return true;
        }
        int tolerance = Math.max(MIN_WARP_ARRIVAL_TOLERANCE,
            Math.min(dragViewport.getPixelSizeX(), dragViewport.getPixelSizeY()) / 4);

        if ( Math.abs(x - warpTargetX) <= tolerance &&
             Math.abs(y - warpTargetY) <= tolerance ) {
            awaitingWarp = false;
            return true;
        }
        return false;
    }

    /**
    Converts a dragged event to "virtual" coordinates, which are continuous
    even if the cursor wraps around the viewport (and can be out of it), and
    requests to place the cursor if it left the viewport.
    @return the event with virtual coordinates, or null if the event must be
    ignored
    */
    private MouseEvent toVirtualEvent(MouseEvent e)
    {
        int x = e.getX();
        int y = e.getY();
        int width = dragViewport.getPixelSizeX();
        int height = dragViewport.getPixelSizeY();

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
                return null;
            }
        }

        int virtualX = x + wrapOffsetX;
        int virtualY = y + wrapOffsetY;

        if ( cursorWrapEnabled && !wrappingSuspended && width > 0 && height > 0 &&
             (x < 0 || x >= width || y < 0 || y >= height) ) {
            int wrappedX = Math.floorMod(x, width);
            int wrappedY = Math.floorMod(y, height);

            // The virtual position does not change: only the real one does
            wrapOffsetX += x - wrappedX;
            wrapOffsetY += y - wrappedY;
            warpTargetX = wrappedX;
            warpTargetY = wrappedY;
            awaitingWarp = true;
            staleEvents = 0;
            pendingWarp = new CursorWarp(wrappedX, wrappedY);
        }
        lastVirtualX = virtualX;
        lastVirtualY = virtualY;

        MouseEvent virtual = new MouseEvent();

        virtual.setX(virtualX);
        virtual.setY(virtualY);
        virtual.setButton(e.getButton());
        virtual.setModifiers(e.getModifiers());
        virtual.setClicks(e.getClicks());
        return virtual;
    }

    /**
    Given a pixel coordinate, traces a ray from the camera of the gizmo to its
    geometry and determines the group of the nearest element hit. Updates the
    active state.

    @return one of the `*_GROUP` constants of the gizmo
    */
    private int calculateSelection(int x, int y)
    {
        Camera camera = gizmo.getCamera();
        ArrayList<SimpleBody> elements = gizmo.getElements();

        camera.updateVectors();
        Ray r = camera.generateRay(x, y);
        double nearestDistance = Double.MAX_VALUE;
        int nearestElement = -1;
        int index = 1;

        // Box elements are only for display, they do not affect selections
        for ( int i = 0; index <= TranslateGizmo.XZX_SEGMENT_ELEMENT && i < elements.size();
              index++, i++ ) {
            r = r.withT(Double.MAX_VALUE);
            SimpleBody element = elements.get(i);

            Ray hit = element.getGeometry() != null ? element.doIntersectionFirstHit(r) : null;
            if ( hit != null && hit.getT() < nearestDistance ) {
                nearestDistance = hit.getT();
                nearestElement = index;
            }
        }

        int selection = switch (nearestElement) {
            case TranslateGizmo.X_AXIS_ELEMENT -> TranslateGizmo.X_AXIS_GROUP;
            case TranslateGizmo.Y_AXIS_ELEMENT -> TranslateGizmo.Y_AXIS_GROUP;
            case TranslateGizmo.Z_AXIS_ELEMENT -> TranslateGizmo.Z_AXIS_GROUP;
            case TranslateGizmo.XYY_SEGMENT_ELEMENT, TranslateGizmo.XYX_SEGMENT_ELEMENT ->
                    TranslateGizmo.XY_PLANE_GROUP;
            case TranslateGizmo.YZZ_SEGMENT_ELEMENT, TranslateGizmo.YZY_SEGMENT_ELEMENT ->
                    TranslateGizmo.YZ_PLANE_GROUP;
            case TranslateGizmo.XZZ_SEGMENT_ELEMENT, TranslateGizmo.XZX_SEGMENT_ELEMENT ->
                    TranslateGizmo.XZ_PLANE_GROUP;
            default -> TranslateGizmo.NULL_GROUP;
        };

        active = selection != TranslateGizmo.NULL_GROUP;

        return selection;
    }

    /**
    Calculates the point of the space the cursor points to, constrained by the
    group of the gizmo currently selected: over the line of the axis for axis
    groups, or over the plane for plane groups.

    @return the point, or null if there is no group selected or the cursor
    does not define a point for it
    */
    private Vector3Dd calculateInteractionPoint(MouseEvent e)
    {
        Camera camera = gizmo.getCamera();
        Vector3Dd v = null;
        int group = dragViewport != null ? dragSelection : gizmo.getCurrentSelection();
        int technique = switch (group) {
            case TranslateGizmo.X_AXIS_GROUP -> {
                v = new Vector3Dd(1, 0, 0);
                yield MOVE_ALONG_AXIS;
            }
            case TranslateGizmo.Y_AXIS_GROUP -> {
                v = new Vector3Dd(0, 1, 0);
                yield MOVE_ALONG_AXIS;
            }
            case TranslateGizmo.Z_AXIS_GROUP -> {
                v = new Vector3Dd(0, 0, 1);
                yield MOVE_ALONG_AXIS;
            }
            case TranslateGizmo.XY_PLANE_GROUP -> {
                v = new Vector3Dd(0, 0, 1);
                yield MOVE_OVER_PLANE;
            }
            case TranslateGizmo.YZ_PLANE_GROUP -> {
                v = new Vector3Dd(1, 0, 0);
                yield MOVE_OVER_PLANE;
            }
            case TranslateGizmo.XZ_PLANE_GROUP -> {
                v = new Vector3Dd(0, 1, 0);
                yield MOVE_OVER_PLANE;
            }
            default -> 0;
        };

        if ( v == null ) {
            return null;
        }

        Vector3Dd o = gizmo.getPosition();
        int mouseX = e.getX();
        int mouseY = e.getY();

        camera.updateVectors();

        if ( technique == MOVE_OVER_PLANE ) {
            Ray r = camera.generateRay(mouseX, mouseY);

            if ( r.getDirection().dotProduct(v) > 0 ) {
                v = v.multiply(-1);
            }
            InfinitePlane plane = new InfinitePlane(v, o);
            Ray hit = plane.doIntersectionFirstHit(r);

            if ( hit == null ) {
                return o;
            }
            return hit.getDirection().multiply(hit.getT()).add(hit.getOrigin());
        }

        // Move along an axis: closest point of the axis line to the cursor's
        // projector ray. It depends only on the cursor position (never on its
        // last movement), so it is continuous for perspective and orthogonal
        // cameras alike.
        Ray r = camera.generateRay(mouseX, mouseY);
        Vector3Dd axis = v.normalized();
        Vector3Dd rayDirection = r.getDirection().normalized();
        Vector3Dd w0 = o.subtract(r.getOrigin());
        double b = axis.dotProduct(rayDirection);
        double denominator = 1.0 - b * b;

        if ( denominator < AXIS_PARALLEL_TO_RAY_LIMIT ) {
            // Looking (almost) along the axis: cursor does not define a point
            return null;
        }

        double s = (b * rayDirection.dotProduct(w0) - axis.dotProduct(w0)) / denominator;

        return o.add(axis.multiply(s));
    }
}
