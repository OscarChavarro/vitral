package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;
import vsdk.toolkit.gui.viewport.Viewport;

/**
Interaction technique to work with a `RotateGizmo` using the keyboard and the
mouse, processing only vitral events.

Mouse events must have coordinates in pixels of the viewport the gizmo is seen
in, with origin at its upper left corner.

- Hover: the ring under the cursor is the volatile selection of the gizmo.
- Click: the ring under the cursor becomes the persistent selection of the
  gizmo; a click over no ring keeps the selection it has.
- Drag: pressing over a ring and dragging rotates the gizmo around the axis of
  the ring, so the point of the ring grabbed keeps under the cursor: the angle
  of the cursor around the axis is measured in the plane of the ring, and the
  gizmo takes the orientation it had when pressed plus that angle (so there
  is no drift, and turning several times is possible). While dragging the gizmo
  shows the arc swept (see `RotateGizmo.setArc`), which disappears on release.
  The gesture belongs to the viewport where it started, if it was given (see
  `getDragViewport`), so the caller must keep on feeding it the events of the
  gesture, whatever viewport the cursor is over. The angle is not updated
  while the plane of the ring is seen almost edge on, as the cursor does not
  define a point of it.
- Numeric input: the keyboard is fed to the `InputGizmo` of the gizmo, that
  shows (and lets the user type) the angles, in degrees, of its orientation.
  Typing numbers and pressing ENTER (or stepping them with the arrow keys)
  rotates the gizmo to exactly that orientation (so the caller, that applies
  its orientation to the things it manipulates, orients them precisely so);
  rotating the gizmo any other way discards what was typed.
- The keys `x`, `y`, `z` (`X`, `Y`, `Z`) rotate the gizmo one degree
  clockwise (counterclockwise) around its own axes.
*/
public class RotateGizmoInteractionTechnique {
    private static final double KEY_ROTATION_STEP = Math.toRadians(1.0);

    /// Minimum cosine of the angle between the view ray and the axis of the
    /// ring (that is, sine of the angle with its plane) to measure angles
    private static final double MIN_RAY_TO_AXIS_COSINE = 0.05;

    private final RotateGizmo gizmo;
    private boolean active;

    // State of the gesture in course (drag of a ring)
    private int dragRing;
    private Viewport dragViewport;
    private Matrix4x4d dragStartTransformation;
    private Vector3Dd dragU;
    private Vector3Dd dragV;
    private Vector3Dd dragAxis;
    private double dragStartAngle;
    private double dragLastAngle;
    private double dragSweep;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public RotateGizmoInteractionTechnique(RotateGizmo gizmo)
    {
        this.gizmo = gizmo;
        active = false;
        endGesture();
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public RotateGizmo getGizmo()
    {
        return gizmo;
    }

    /**
    @return true if the last cursor position was over a ring of the gizmo
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
    @return true if a ring is being dragged
    */
    public boolean isDragging()
    {
        return dragRing >= 0;
    }

    /**
    @param keyEvent key press
    @return true if the input gizmo uses the key (digits, `-`, decimal point,
    TAB, BACKSPACE, arrows, and ENTER and ESC while editing), so the caller must
    not process it as any other command
    */
    public boolean isInputGizmoKey(KeyEvent keyEvent)
    {
        return gizmo.getInputGizmo().consumesKey(keyEvent);
    }

    /**
    Processes a key press.
    @param keyEvent key press
    @return true if the orientation of the gizmo changed
    */
    public boolean processKeyPressedEvent(KeyEvent keyEvent)
    {
        InputGizmo inputGizmo = gizmo.getInputGizmo();

        if ( inputGizmo.processKeyPressedEvent(keyEvent) ) {
            if ( inputGizmo.consumeCommit() ) {
                double[] angles = inputGizmo.getValuesWithEdits();

                inputGizmo.cancelEditing();
                Matrix4x4d rotation = RotateGizmo.createRotationFromAnglesInDegrees(
                    angles[0], angles[1], angles[2]);

                gizmo.setTransformationMatrix(
                    rotation.withTranslation(gizmo.getPosition()));
                return true;
            }
            return false;
        }

        Vector3Dd axis;

        switch ( keyEvent.unicode_id ) {
          case 'x', 'X' -> axis = new Vector3Dd(1, 0, 0);
          case 'y', 'Y' -> axis = new Vector3Dd(0, 1, 0);
          case 'z', 'Z' -> axis = new Vector3Dd(0, 0, 1);
          default -> {
            return false;
          }
        }

        double angle = Character.isUpperCase(keyEvent.unicode_id) ?
            KEY_ROTATION_STEP : -KEY_ROTATION_STEP;
        Matrix4x4d delta = new Matrix4x4d().axisRotation(angle, axis);

        inputGizmo.cancelEditing();
        gizmo.setTransformationMatrix(gizmo.getTransformationMatrix().multiply(delta));
        return true;
    }

    public boolean processKeyReleasedEvent(KeyEvent keyEvent)
    {
        return false;
    }

    /**
    Processes the press of a mouse button over the viewport where the gizmo is
    seen. If the cursor is over a ring, the ring becomes the chosen one and a
    gesture to rotate around it starts, confined to the given viewport.
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
    Processes the press of a mouse button. If the cursor is over a ring, the
    ring becomes the chosen one and a gesture to rotate around it starts. The
    caller also needs to know if the cursor is over a ring (see `isActive`),
    to let a press over the gizmo grab it, instead of selecting what is behind.
    @param e event with coordinates relative to the viewport
    @return false (a press never changes the gizmo)
    */
    public boolean processMousePressedEvent(MouseEvent e)
    {
        endGesture();
        int selection = calculateSelection(e.getX(), e.getY());

        if ( selection != RotateGizmo.NULL_GROUP ) {
            beginGesture(selection - 1, e.getX(), e.getY());
        }
        return false;
    }

    /**
    Ends the gesture in course, if there is one, so the arc disappears.
    @param e event
    @return true if there was a gesture, so the gizmo must be drawn again
    */
    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        boolean wasDragging = isDragging();

        endGesture();
        return wasDragging;
    }

    /**
    Makes the ring under the cursor the persistent selection, if there is one.
    @param e event with coordinates relative to the viewport
    @return true if the current selection changed
    */
    public boolean processMouseClickedEvent(MouseEvent e)
    {
        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        if ( selection == RotateGizmo.NULL_GROUP ) {
            selection = previousSelection;
        }
        gizmo.setPersistentSelection(selection);
        return selection != previousSelection;
    }

    /**
    Makes the ring under the cursor the volatile selection.
    @param e event with coordinates relative to the viewport
    @return true if the current selection changed, so the gizmo must be drawn
    again
    */
    public boolean processMouseMovedEvent(MouseEvent e)
    {
        if ( isDragging() ) {
            // The ring is fixed while dragging
            return false;
        }
        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        gizmo.setVolatileSelection(selection);
        return gizmo.getCurrentSelection() != previousSelection;
    }

    /**
    Rotates the gizmo around the axis of the ring being dragged, so the point
    of the ring under the cursor when the gesture started follows it.
    @param e event with coordinates relative to the viewport of the gesture
    (they can be out of it)
    @return true if the gizmo changed, so it must be drawn again and what it
    manipulates must be updated
    */
    public boolean processMouseDraggedEvent(MouseEvent e)
    {
        if ( !isDragging() ) {
            return false;
        }

        double angle = calculateAngle(e.getX(), e.getY());

        if ( Double.isNaN(angle) ) {
            return false;
        }
        if ( Double.isNaN(dragLastAngle) ) {
            // First point of the plane the cursor defines: the arc starts here
            dragStartAngle = angle;
        }
        else {
            double delta = angle - dragLastAngle;

            // The shortest way from the previous angle: the sweep is
            // continuous when the cursor goes around the axis
            delta -= 2 * Math.PI * Math.round(delta / (2 * Math.PI));
            dragSweep += delta;
        }
        dragLastAngle = angle;

        Vector3Dd unitAxis = new Vector3Dd(dragRing == 0 ? 1 : 0, dragRing == 1 ? 1 : 0,
            dragRing == 2 ? 1 : 0);

        gizmo.getInputGizmo().cancelEditing();
        gizmo.setTransformationMatrix(dragStartTransformation.multiply(
            new Matrix4x4d().axisRotation(dragSweep, unitAxis)));
        gizmo.setArc(dragRing, dragU, dragV, dragStartAngle, dragSweep);
        return true;
    }

    public boolean processMouseWheelEvent(MouseEvent e)
    {
        return false;
    }

    /**
    Starts the gesture to rotate around a ring, keeping the plane of the ring
    (and the directions angles are measured from) as they are now. The angle
    of the point grabbed is measured at once, so the gizmo follows the cursor
    from the place it was pressed at.
    */
    private void beginGesture(int ring, int x, int y)
    {
        dragRing = ring;
        dragStartTransformation = new Matrix4x4d(gizmo.getTransformationMatrix());
        dragAxis = gizmo.getAxisDirection(ring);
        dragU = gizmo.getAxisDirection((ring + 1) % RotateGizmo.RING_COUNT);
        dragV = gizmo.getAxisDirection((ring + 2) % RotateGizmo.RING_COUNT);
        dragStartAngle = 0;
        dragLastAngle = Double.NaN;
        dragSweep = 0;
        gizmo.setPersistentSelection(RotateGizmo.groupOfRing(ring));
        gizmo.clearArc();

        // NaN if the ring is seen edge on: the first angle is taken when dragging
        double angle = calculateAngle(x, y);

        if ( !Double.isNaN(angle) ) {
            dragStartAngle = angle;
            dragLastAngle = angle;
        }
    }

    private void endGesture()
    {
        dragRing = -1;
        dragViewport = null;
        dragStartTransformation = null;
        dragU = null;
        dragV = null;
        dragAxis = null;
        dragStartAngle = 0;
        dragLastAngle = Double.NaN;
        dragSweep = 0;
        gizmo.clearArc();
    }

    /**
    Calculates the angle, around the axis of the ring being dragged, of the
    point of its plane the cursor points to.
    @return the angle in radians, measured from `dragU` towards `dragV`, or NaN
    if the cursor does not define a point of the plane (it is seen edge on, or
    the point is behind the camera)
    */
    private double calculateAngle(int x, int y)
    {
        Camera camera = gizmo.getCamera();

        camera.updateVectors();
        Ray ray = camera.generateRay(x, y);
        Vector3Dd direction = ray.getDirection().normalized();
        double cosine = direction.dotProduct(dragAxis);

        if ( Math.abs(cosine) < MIN_RAY_TO_AXIS_COSINE ) {
            return Double.NaN;
        }

        Vector3Dd center = dragStartTransformation.extractTranslation();
        double distance = center.subtract(ray.getOrigin()).dotProduct(dragAxis) / cosine;

        // The rays of an orthogonal camera start at a plane, and the gizmo can
        // be behind it
        if ( distance <= 0 && camera.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            return Double.NaN;
        }

        Vector3Dd fromCenter = ray.getOrigin().add(direction.multiply(distance)).subtract(center);

        return Math.atan2(fromCenter.dotProduct(dragV), fromCenter.dotProduct(dragU));
    }

    /**
    Given a pixel coordinate, traces a ray from the camera of the gizmo to its
    rings and determines the nearest one hit. Updates the active state.

    @return one of the `*_RING_GROUP` constants of the gizmo, or `NULL_GROUP`
    */
    private int calculateSelection(int x, int y)
    {
        Camera camera = gizmo.getCamera();

        gizmo.updateGeometryState();
        camera.updateVectors();
        Ray ray = camera.generateRay(x, y);
        int selection = gizmo.pickRing(ray);

        active = selection != RotateGizmo.NULL_GROUP;
        return selection;
    }
}
