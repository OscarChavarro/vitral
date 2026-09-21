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

    /// Interaction techniques used to move the gizmo depending on the group
    private static final int MOVE_ALONG_AXIS = 1;
    private static final int MOVE_OVER_PLANE = 2;

    private final TranslateGizmo gizmo;

    private int oldMouseX;
    private int oldMouseY;
    private Vector3Dd lastDeltaPosition;
    private boolean active;

    /**
    @param gizmo gizmo manipulated by this technique
    */
    public TranslateGizmoInteractionTechnique(TranslateGizmo gizmo)
    {
        this.gizmo = gizmo;
        oldMouseX = 0;
        oldMouseY = 0;
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
        oldMouseX = mouseEvent.getX();
        oldMouseY = mouseEvent.getY();
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

        oldMouseX = e.getX();
        oldMouseY = e.getY();
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
        oldMouseX = e.getX();
        oldMouseY = e.getY();

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

        oldMouseX = e.getX();
        oldMouseY = e.getY();
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

        // Move along an axis: intersect the axis with a plane facing the camera
        boolean accountForU = false;
        boolean accountForV = false;
        InfinitePlane plane;
        Vector3Dd left = camera.getLeft().normalized();
        Vector3Dd up = camera.getUp().normalized();
        double limit = Math.cos(Math.toRadians(80.0));

        v = v.normalized();
        if ( Math.abs(v.dotProduct(left)) > limit ) {
            accountForU = true;
        }
        if ( Math.abs(v.dotProduct(up)) > limit ) {
            accountForV = true;
        }

        if ( accountForU && !accountForV ) {
            plane = camera.calculateUPlaneAtPixel(mouseX, mouseY);
        }
        else if ( accountForV && !accountForU ) {
            plane = camera.calculateVPlaneAtPixel(mouseX, mouseY);
        }
        else if ( accountForU && accountForV ) {
            if ( (mouseX - oldMouseX) > (mouseY - oldMouseY) ) {
                plane = camera.calculateUPlaneAtPixel(mouseX, mouseY);
            }
            else {
                plane = camera.calculateVPlaneAtPixel(mouseX, mouseY);
            }
        }
        else {
            return null;
        }

        Ray hit = plane.doIntersectionWithNegative(new Ray(o, v));

        if ( hit == null ) {
            return null;
        }
        return hit.getOrigin().add(hit.getDirection().multiply(hit.getT()));
    }
}
