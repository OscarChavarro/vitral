package gui;

import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.gui.KeyEvent;

import model.ApplicationModel;

/**
Keyboard control of the visual debug ray of the application model. It
processes only vitral events.

Keys (numeric keypad): `5` shows / hides the ray, `4` / `6` move its origin
along X, `2` / `8` along Y, `1` / `7` along Z, `9` / `3` change the number of
reflection levels, `*` and `/` rotate its direction around the vertical axis
and `+` and `-` around the horizontal one.
*/
public class VisualRayDebugController
{
    private static final double ORIGIN_STEP = 0.1;
    private static final double ANGLE_STEP = Math.toRadians(5);

    private final ApplicationModel model;

    public VisualRayDebugController(ApplicationModel model)
    {
        this.model = model;
    }

    /**
    @param event key pressed event
    @return true if the key is one of the visual debug ray commands
    */
    public boolean processKeyPressedEvent(KeyEvent event)
    {
        if ( event.unicodeId == KeyEvent.KEY_NONE ) {
            return false;
        }

        switch ( event.unicodeId ) {
          case '4':
            moveOrigin(-ORIGIN_STEP, 0, 0);
            return true;
          case '6':
            moveOrigin(ORIGIN_STEP, 0, 0);
            return true;
          case '8':
            moveOrigin(0, ORIGIN_STEP, 0);
            return true;
          case '2':
            moveOrigin(0, -ORIGIN_STEP, 0);
            return true;
          case '1':
            moveOrigin(0, 0, -ORIGIN_STEP);
            return true;
          case '7':
            moveOrigin(0, 0, ORIGIN_STEP);
            return true;
          case '9':
            if ( model.isWithVisualDebugRay() ) {
                model.setVisualDebugRayLevels(model.getVisualDebugRayLevels() + 1);
            }
            return true;
          case '3':
            if ( model.isWithVisualDebugRay() ) {
                model.setVisualDebugRayLevels(model.getVisualDebugRayLevels() - 1);
                if ( model.getVisualDebugRayLevels() < 0 ) {
                    model.setVisualDebugRayLevels(0);
                }
            }
            return true;
          case '5':
            model.setWithVisualDebugRay(!model.isWithVisualDebugRay());
            return true;
          case '*':
            rotateDirection(-ANGLE_STEP, 0);
            return true;
          case '/':
            rotateDirection(ANGLE_STEP, 0);
            return true;
          case '+':
            rotateDirection(0, ANGLE_STEP);
            return true;
          case '-':
            rotateDirection(0, -ANGLE_STEP);
            return true;
          default:
            return false;
        }
    }

    private void moveOrigin(double dx, double dy, double dz)
    {
        if ( !model.isWithVisualDebugRay() ) {
            return;
        }
        Ray ray = model.getVisualDebugRay();
        Vector3Dd origin = ray.getOrigin();

        model.setVisualDebugRay(ray.withOrigin(new Vector3Dd(
            origin.x() + dx, origin.y() + dy, origin.z() + dz)));
    }

    private void rotateDirection(double deltaTheta, double deltaPhi)
    {
        if ( !model.isWithVisualDebugRay() ) {
            return;
        }
        Ray ray = model.getVisualDebugRay();
        double theta = ray.getDirection().obtainSphericalThetaAngle();
        double phi = ray.getDirection().obtainSphericalPhiAngle();

        theta += deltaTheta;
        phi += deltaPhi;
        if ( phi > Math.PI ) phi = Math.PI;
        if ( phi < 0 ) phi = 0;
        model.setVisualDebugRay(ray.withDirection(
            Vector3Dd.fromSpherical(1, theta, phi)));
    }
}
