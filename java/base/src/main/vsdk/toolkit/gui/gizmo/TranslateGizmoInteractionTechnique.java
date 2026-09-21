package vsdk.toolkit.gui.gizmo;

import java.util.ArrayList;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.gui.KeyEvent;
import vsdk.toolkit.gui.MouseEvent;

public class TranslateGizmoInteractionTechnique {
    private static final double KEY_MOVEMENT_STEP = 0.1;

    /// sin^2 of the minimum angle (10 degrees) between an axis and the view ray
    private static final double AXIS_PARALLEL_TO_RAY_LIMIT =
        Math.pow(Math.sin(Math.toRadians(10.0)), 2);

    /// Interaction techniques used to move the gizmo depending on the group
    private static final int MOVE_ALONG_AXIS = 1;
    private static final int MOVE_OVER_PLANE = 2;

    private final TranslateGizmo gizmo;

    private Vector3Dd lastDeltaPosition;
    private boolean active;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public TranslateGizmoInteractionTechnique(TranslateGizmo gizmo)
    {
        this.gizmo = gizmo;
        lastDeltaPosition = new Vector3Dd();
        active = false;
    }

    /**
    @return the gizmo manipulated by this technique
    */
    public TranslateGizmo getGizmo()
    {
        return gizmo;
    }

    /**
    @return true if the last cursor position was over an axis or plane handle
    of the gizmo
    */
    public boolean isActive()
    {
        return active;
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

        if ( keyEvent.unicode_id != KeyEvent.KEY_NONE ) {
            Vector3Dd p = gizmo.getPosition();

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

    public boolean processMousePressedEvent(MouseEvent e)
    {
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

        gizmo.setSelectedResizing(true);
        gizmo.updateGeometryState();

        int previousSelection = gizmo.getCurrentSelection();
        int selection = calculateSelection(e.getX(), e.getY());

        gizmo.setVolatileSelection(selection);

        return selection != previousSelection;
    }

    public boolean processMouseDraggedEvent(MouseEvent e)
    {
        Vector3Dd p = calculateInteractionPoint(e);

        if ( p == null ) {
            return false;
        }

        gizmo.setPosition(p.subtract(lastDeltaPosition));
        gizmo.setSelectedResizing(false);
        return true;
    }

    public boolean processMouseWheelEvent(MouseEvent e)
    {
        return false;
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
        int technique = switch (gizmo.getCurrentSelection()) {
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
