package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.viewport.Viewport;

/**
Interaction technique to work with a `ScaleGizmo` using the keyboard and the
mouse, processing only vitral events, following the structure of
`RotateGizmoInteractionTechnique`.

Mouse events must have coordinates in pixels of the viewport the gizmo is seen
in, with origin at its upper left corner.

- Hover: the handle under the cursor (an axis, a two-axis band, or the
  uniform, all-axis triangle fan) is the volatile selection of the gizmo (see
  `ScaleGizmo.pickElement`).
- Click: the handle under the cursor becomes the persistent selection; a
  click over no handle keeps the selection it has.
- Drag: pressing over a handle and dragging scales every axis the handle's
  group includes (see `ScaleGizmo.groupIncludesAxis`) by the same factor: the
  ratio between the distance from the cursor to the projected origin of the
  gizmo while dragging, and that distance when the gesture started (so
  dragging away from the gizmo grows the selected axes, and towards it
  shrinks them). The gesture belongs to the viewport where it started, if it
  was given (see `getDragViewport`), so the caller must keep on feeding it the
  events of the gesture, whatever viewport the cursor is over.
- Numeric input: the technique also feeds the keyboard to the `InputGizmo` of
  the gizmo, that shows (and lets the user type) its scale factors.
*/
public class ScaleGizmoInteractionTechnique {
    /// Below this distance (in pixels) from the projected origin, the drag
    /// ratio is not measured (it would be too sensitive, or undefined at 0)
    private static final double MIN_DRAG_DISTANCE = 4.0;

    private final ScaleGizmo gizmo;
    private boolean active;

    // State of the gesture in course (drag of a handle)
    private Viewport dragViewport;
    private int dragGroup;
    private Vector3Dd dragStartScale;
    private double dragStartDistance;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public ScaleGizmoInteractionTechnique(ScaleGizmo gizmo)
    {
        this.gizmo = gizmo;
        active = false;
        endGesture();
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public ScaleGizmo getGizmo()
    {
        return gizmo;
    }

    /**
    @return true if the last cursor position was over a handle of the gizmo
    */
    public boolean isActive()
    {
        return active;
    }

    /**
    @return the viewport where the gesture in course started, or null if there
    is no gesture in course, or it was started without a viewport
    */
    public Viewport getDragViewport()
    {
        return dragViewport;
    }

    /**
    @param keyEvent key press
    @return true if the input gizmo uses the key (digits, `-`, decimal point,
    TAB, BACKSPACE, arrows, and ENTER and ESC while editing), so the caller
    must not process it as any other command
    */
    public boolean isInputGizmoKey(KeyEvent keyEvent)
    {
        return gizmo.getInputGizmo().consumesKey(keyEvent);
    }

    /**
    Feeds the key press to the gizmo (see `ScaleGizmo.processKeyPressedEvent`).
    @param keyEvent key press
    @return true if the scale factors changed
    */
    public boolean processKeyPressedEvent(KeyEvent keyEvent)
    {
        return gizmo.processKeyPressedEvent(keyEvent);
    }

    public boolean processKeyReleasedEvent(KeyEvent keyEvent)
    {
        return false;
    }

    /**
    Processes the press of a mouse button, starting a gesture confined to the
    given viewport if a handle of the gizmo is selected (that is, if dragging
    is going to scale the gizmo).
    @param e event with coordinates relative to the viewport
    @param viewport viewport where the button was pressed
    @return false (a press never changes the gizmo)
    */
    public boolean processMousePressedEvent(MouseEvent e, Viewport viewport)
    {
        boolean changed = processMousePressedEvent(e);

        dragViewport = isDragging() ? viewport : null;
        return changed;
    }

    /**
    Processes the press of a mouse button, without confining the gesture to
    any viewport. The gesture only begins if a handle is under the cursor
    (which is found again here, as the press can come without a previous
    movement): a press anywhere else must not scale anything, whatever the
    gizmo has selected.
    @param e event with coordinates relative to the viewport
    @return false (a press never changes the gizmo)
    */
    public boolean processMousePressedEvent(MouseEvent e)
    {
        endGesture();

        int selection = calculateSelection(e.getX(), e.getY());

        if ( selection != ScaleGizmo.NULL_GROUP ) {
            gizmo.setVolatileSelection(selection);
            dragGroup = selection;
            dragStartScale = gizmo.getScale();
            dragStartDistance = distanceToOrigin(e.getX(), e.getY());
        }
        return false;
    }

    /**
    @return true if a handle of the gizmo is being dragged
    */
    public boolean isDragging()
    {
        return dragGroup != ScaleGizmo.NULL_GROUP;
    }

    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        endGesture();
        return false;
    }

    /**
    A click over a handle chooses it as the persistent selection; a click
    over no handle keeps the selection it has.
    @param e event with coordinates relative to the viewport
    @return true if the persistent selection changed
    */
    public boolean processMouseClickedEvent(MouseEvent e)
    {
        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        if ( selection == ScaleGizmo.NULL_GROUP ) {
            selection = previousSelection;
        }
        gizmo.setPersistentSelection(selection);

        return selection != previousSelection;
    }

    /**
    While no gesture is in course, the handle under the cursor becomes the
    volatile selection.
    @param e event with coordinates relative to the viewport
    @return true if the volatile selection changed
    */
    public boolean processMouseMovedEvent(MouseEvent e)
    {
        if ( isDragging() ) {
            // The selection of the gizmo is fixed while dragging
            return false;
        }

        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        gizmo.setVolatileSelection(selection);

        return selection != previousSelection;
    }

    /**
    Scales every axis of the group being dragged by the ratio between the
    current and the starting distance from the cursor to the projected origin
    of the gizmo.
    @param e event with coordinates relative to the viewport
    @return true if the scale factors changed
    */
    public boolean processMouseDraggedEvent(MouseEvent e)
    {
        if ( !isDragging() || dragStartScale == null ) {
            return false;
        }

        double distance = distanceToOrigin(e.getX(), e.getY());
        double ratio = dragStartDistance >= MIN_DRAG_DISTANCE ? distance/dragStartDistance : 1.0;
        Vector3Dd newScale = new Vector3Dd(
            scaledIfIncluded(0, ratio),
            scaledIfIncluded(1, ratio),
            scaledIfIncluded(2, ratio));

        gizmo.getInputGizmo().cancelEditing();
        gizmo.setScale(newScale);
        return true;
    }

    public boolean processMouseWheelEvent(MouseEvent e)
    {
        return false;
    }

    private double scaledIfIncluded(int axis, double ratio)
    {
        double value = switch ( axis ) {
            case 0 -> dragStartScale.x();
            case 1 -> dragStartScale.y();
            default -> dragStartScale.z();
        };

        return ScaleGizmo.groupIncludesAxis(dragGroup, axis) ? value*ratio : value;
    }

    private void endGesture()
    {
        dragViewport = null;
        dragGroup = ScaleGizmo.NULL_GROUP;
        dragStartScale = null;
        dragStartDistance = 0.0;
    }

    /**
    Given a pixel coordinate, traces a ray from the camera of the gizmo to its
    geometry and determines the group of the handle hit. Updates the active
    state.
    @return one of the `*_GROUP` constants of the gizmo
    */
    private int calculateSelection(int x, int y)
    {
        Camera camera = gizmo.getCamera();

        if ( camera == null ) {
            active = false;
            return ScaleGizmo.NULL_GROUP;
        }
        camera.updateVectors();

        Ray ray = camera.generateRay(x, y);
        int selection = gizmo.pickElement(ray);

        active = selection != ScaleGizmo.NULL_GROUP;

        return selection;
    }

    /**
    @return the pixel distance between (x, y) and the projected origin of the
    gizmo, or 0 if the camera or the origin can not be projected
    */
    private double distanceToOrigin(int x, int y)
    {
        Camera camera = gizmo.getCamera();

        if ( camera == null ) {
            return 0.0;
        }
        camera.updateVectors();

        Vector3Dd projected = camera.projectPointUsingRayMethod(gizmo.getPosition());

        if ( projected == null ) {
            return 0.0;
        }

        double dx = x - projected.x();
        double dy = y - projected.y();

        return Math.sqrt(dx*dx + dy*dy);
    }
}
