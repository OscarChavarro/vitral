package vsdk.toolkit.gui.gizmo;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.gui.KeyEvent;

/**
Keyboard interaction for a {@link RayGizmo}. The ray can be toggled with one
of the configured activation keys and then controlled from the numeric keypad:
NUM4/NUM6 move X, NUM2/NUM8 move Y, NUM1/NUM7 move Z, NUM* and NUM/ change
yaw, and NUM+/NUM- change pitch. Regular numeric editing is delegated to the
gizmo's five-field {@link InputGizmo}: x, y, z, yaw and pitch.
*/
public class RayGizmoInteractionTechniques {
    private static final double ORIGIN_STEP = 0.1;
    private static final double ANGLE_STEP_DEGREES = 5.0;

    private final RayGizmo gizmo;
    private final int[] activationKeys;
    private boolean active;
    private Vector3Dd origin;
    private double yawInDegrees;
    private double pitchInDegrees;

    public RayGizmoInteractionTechniques(RayGizmo gizmo, int... activationKeys) {
        this.gizmo = gizmo;
        this.activationKeys = activationKeys != null ? activationKeys.clone() : new int[0];
        Ray ray = currentRay();
        origin = ray.getOrigin();
        updateAnglesFromDirection(ray.getDirection());
        active = gizmo.isVisible();
        updateInputGizmo();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        gizmo.setVisible(active);
        gizmo.setDisableAfterElapsedSeconds(active ?
            Double.POSITIVE_INFINITY : RayGizmo.DEFAULT_DISABLE_TIME);
        if ( active ) {
            applyRay();
        }
    }

    public InputGizmo getInputGizmo() {
        return gizmo.getInputGizmo();
    }

    public boolean processKeyPressedEvent(KeyEvent event) {
        if ( event == null ) {
            return false;
        }
        active = gizmo.isVisible();
        if ( isActivationKey(event) || event.keycode == KeyEvent.KEY_NUM5 ) {
            setActive(!active);
            return true;
        }
        if ( !active ) {
            return false;
        }
        if ( processNumericPadCommand(event) ) {
            getInputGizmo().cancelEditing();
            applyRay();
            return true;
        }
        if ( getInputGizmo().consumesKey(event) ) {
            getInputGizmo().processKeyPressedEvent(event);
            if ( getInputGizmo().consumeCommit() ) {
                applyInputGizmoValues();
                getInputGizmo().cancelEditing();
                applyRay();
            }
            return true;
        }
        return false;
    }

    public void setRay(Ray ray) {
        if ( ray == null ) {
            return;
        }
        origin = ray.getOrigin();
        updateAnglesFromDirection(ray.getDirection());
        updateInputGizmo();
        gizmo.setRay(ray, 0.0);
    }

    private boolean processNumericPadCommand(KeyEvent event) {
        switch ( event.keycode ) {
          case KeyEvent.KEY_NUM4:
            origin = origin.add(new Vector3Dd(-ORIGIN_STEP, 0, 0));
            return true;
          case KeyEvent.KEY_NUM6:
            origin = origin.add(new Vector3Dd(ORIGIN_STEP, 0, 0));
            return true;
          case KeyEvent.KEY_NUM2:
            origin = origin.add(new Vector3Dd(0, -ORIGIN_STEP, 0));
            return true;
          case KeyEvent.KEY_NUM8:
            origin = origin.add(new Vector3Dd(0, ORIGIN_STEP, 0));
            return true;
          case KeyEvent.KEY_NUM1:
            origin = origin.add(new Vector3Dd(0, 0, -ORIGIN_STEP));
            return true;
          case KeyEvent.KEY_NUM7:
            origin = origin.add(new Vector3Dd(0, 0, ORIGIN_STEP));
            return true;
          case KeyEvent.KEY_NUMASTERISK:
            yawInDegrees -= ANGLE_STEP_DEGREES;
            return true;
          case KeyEvent.KEY_NUMSLASH:
            yawInDegrees += ANGLE_STEP_DEGREES;
            return true;
          case KeyEvent.KEY_NUMPLUS:
            pitchInDegrees += ANGLE_STEP_DEGREES;
            clampPitch();
            return true;
          case KeyEvent.KEY_NUMMINUS:
            pitchInDegrees -= ANGLE_STEP_DEGREES;
            clampPitch();
            return true;
          case KeyEvent.KEY_NUM9:
            gizmo.setMaxNumOfReflections(gizmo.getMaxNumOfReflections() + 1);
            return true;
          case KeyEvent.KEY_NUM3:
            gizmo.setMaxNumOfReflections(gizmo.getMaxNumOfReflections() - 1);
            return true;
          default:
            return false;
        }
    }

    private boolean isActivationKey(KeyEvent event) {
        for ( int key : activationKeys ) {
            if ( event.keycode == key ) {
                return true;
            }
        }
        return false;
    }

    private Ray currentRay() {
        return new Ray(gizmo.getPosition(), gizmo.getDirection());
    }

    private void applyRay() {
        updateInputGizmo();
        gizmo.setRay(new Ray(origin, direction()), 0.0);
    }

    private void applyInputGizmoValues() {
        double[] values = getInputGizmo().getValuesWithEdits();

        origin = new Vector3Dd(values[0], values[1], values[2]);
        yawInDegrees = values[3];
        pitchInDegrees = values[4];
        clampPitch();
    }

    private void updateInputGizmo() {
        getInputGizmo().setValue(0, origin.x());
        getInputGizmo().setValue(1, origin.y());
        getInputGizmo().setValue(2, origin.z());
        getInputGizmo().setValue(3, yawInDegrees);
        getInputGizmo().setValue(4, pitchInDegrees);
    }

    private void updateAnglesFromDirection(Vector3Dd direction) {
        Vector3Dd d = direction.length() > 1e-9 ?
            direction.normalized() : new Vector3Dd(0, 0, 1);

        yawInDegrees = Math.toDegrees(Math.atan2(d.y(), d.x()));
        pitchInDegrees = Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, d.z()))));
        clampPitch();
    }

    private Vector3Dd direction() {
        double yaw = Math.toRadians(yawInDegrees);
        double pitch = Math.toRadians(pitchInDegrees);
        double cp = Math.cos(pitch);

        return new Vector3Dd(
            cp * Math.cos(yaw),
            cp * Math.sin(yaw),
            Math.sin(pitch));
    }

    private void clampPitch() {
        if ( pitchInDegrees > 89.0 ) {
            pitchInDegrees = 89.0;
        }
        if ( pitchInDegrees < -89.0 ) {
            pitchInDegrees = -89.0;
        }
    }
}
