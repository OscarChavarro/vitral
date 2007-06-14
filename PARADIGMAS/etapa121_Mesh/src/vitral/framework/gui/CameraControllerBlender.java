package vitral.framework.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.Matrix4x4;
import vitral.toolkits.environment.Camera;

public class CameraControllerBlender
    implements CameraController {

    private Camera camera;

    public CameraControllerBlender(Camera camera) {
        this.camera = camera;
    }

    public boolean processMouseEventAwt(MouseEvent mouseEvent) {
      return true;
    }

    private double augmentLogarithmic(double val, double EPSILON)
    {
        if ( val < 0.001 ) val += 0.0001;
        else if ( val < 0.01 ) val += 0.001;
        else if ( val < 0.1 - EPSILON ) val += 0.01;
        else if ( val < 1 - EPSILON ) val += 0.1;
        else if ( val < 10 - EPSILON ) val += 1;
        else if ( val < 100 - EPSILON ) val += 10;
        else if ( val < 1000 - EPSILON ) val += 100;
        else if ( val < 10000 - EPSILON ) val += 1000;
        else if ( val < 100000 - EPSILON ) val += 10000;
        else if ( val < 1000000 - EPSILON ) val += 100000;
        else if ( val < 10000000 - EPSILON ) val *= 2;
        else val = 10000000;

        return val;
    }

    private double diminishLogarithmic(double val, double EPSILON)
    {
        if ( val > 10000000 + EPSILON ) val /= 2;
        else if ( val > 1000000 + EPSILON ) val -= 1000000;
        else if ( val > 100000 + EPSILON ) val -= 100000;
        else if ( val > 10000 + EPSILON ) val -= 10000;
        else if ( val > 1000 + EPSILON ) val -= 1000;
        else if ( val > 100 + EPSILON ) val -= 100;
        else if ( val > 10 + EPSILON ) val -= 10;
        else if ( val > 1 + EPSILON ) val -= 1;
        else if ( val > 0.1 + EPSILON ) val -= 0.1;
        else if ( val > 0.01 + EPSILON ) val -= 0.01;
        else if ( val > 0.001 + EPSILON ) val -= 0.001;
        else if ( val > 0.0001 + EPSILON ) val -= 0.0001;
        else val = 0.0001;
        return val;
    }

    public boolean processKeyBoardEventAwt(KeyEvent keyEvent) {
        // Local copy of the Camera's internal parameters
        Vector3D eyePosition;
        Vector3D focusedPosition;
        Matrix4x4 R; // Camera rotation matrix
        int projectionMode;
        double fov;
        double orthogonalZoom;
        double nearPlaneDistance;
        double farPlaneDistance;

        // Internal variables to control the interaction
        char unicode_id;
        int keycode;
        double deltaMov = 0.25;
        double yaw;
        double pitch;
        double roll;
        double angleInc;
        boolean updated = false;
        double EPSILON = 0.0001;
        int SHIFT_MASK = 0x0001;
        int mask;

        // 1. Obtain a copy of the camera's internal parameters
        eyePosition = camera.getPosition();
        focusedPosition = camera.getFocusedPosition();
        R = camera.getRotation();
        projectionMode = camera.getProjectionMode();
        fov = camera.getFov();
        orthogonalZoom = camera.getOrthogonalZoom();
        nearPlaneDistance = camera.getNearPlaneDistance();
        farPlaneDistance = camera.getFarPlaneDistance();

        // 2. Calculate variables used for interaction manipulation
        unicode_id = keyEvent.getKeyChar();
        keycode = keyEvent.getKeyCode();
        mask = keyEvent.getModifiers();

    System.out.println("Modificadores: " + keyEvent.getKeyModifiersText(keyEvent.getModifiers()) + "int(" + keyEvent.getModifiers() + ")");
        if ( unicode_id == keyEvent.CHAR_UNDEFINED ) {
        System.out.println("Unicode: <UNDEFINED>");
     }
     else {
          System.out.println("Unicode: " + unicode_id);
    }
        System.out.println("Keycode: " +  keycode);

        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();

        if ( fov > 90 ) angleInc = Math.toRadians(10);
        else if ( fov > 45 ) angleInc = Math.toRadians(5);
        else if ( fov > 15 ) angleInc = Math.toRadians(2.5);
        else if ( fov > 5 ) angleInc = Math.toRadians(1);
        else angleInc = Math.toRadians(0.1);

        // 3. Event processing: update the copy of the camera's internal parameters
        if ( unicode_id == keyEvent.CHAR_UNDEFINED ) {
            switch ( keycode ) {
              case KeyEvent.VK_UP:
                pitch -= angleInc;
                if ( pitch < Math.toRadians(-90) ) pitch = Math.toRadians(-90);
                updated = true;
                break;
              case KeyEvent.VK_DOWN:
                pitch += angleInc;
                if ( pitch > Math.toRadians(90) ) pitch = Math.toRadians(90);
                updated = true;
                break;
              case KeyEvent.VK_LEFT:
                yaw += angleInc;
                while ( yaw >= Math.toRadians(360) ) yaw -= Math.toRadians(360);
                updated = true;
                break;
              case KeyEvent.VK_RIGHT:
                yaw -= angleInc;
                while ( yaw < 0 ) yaw += Math.toRadians(360);
                updated = true;
                break;
              case KeyEvent.VK_X:
        if ( (mask & SHIFT_MASK) == 0 ) {
                    // Minuscula
                    eyePosition.x -= deltaMov; focusedPosition.x -= deltaMov;
                    updated = true;
                }
        else if ( (mask & SHIFT_MASK) == SHIFT_MASK ) {
                    // Mayuscula
                    eyePosition.x += deltaMov; focusedPosition.x += deltaMov;
                    updated = true;
        }
            }

        }
        else {
            switch ( unicode_id ) {
              // Position
              case 'y':
                eyePosition.y -= deltaMov; focusedPosition.y -= deltaMov;
                updated = true;
                break;
              case 'Y':
                eyePosition.y += deltaMov; focusedPosition.y += deltaMov;
                updated = true;
                break;
              case 'z':
                eyePosition.z -= deltaMov; focusedPosition.z -= deltaMov;
                updated = true;
                break;
              case 'Z':
                eyePosition.z += deltaMov; focusedPosition.z += deltaMov;
                updated = true;
                break;
              // Rotation
              case 'S':
                roll -= Math.toRadians(5);
                while ( roll < 0 ) roll += Math.toRadians(360);
                updated = true;
                break;
              case 's':
                roll += Math.toRadians(5);
                while ( roll > Math.toRadians(360) ) roll -= Math.toRadians(360);
                updated = true;
                break;

              // View volume modification
              case 'A':
                if ( camera.getProjectionMode() == camera.PROJECTION_MODE_ORTHOGONAL ) {
                    orthogonalZoom /= 2;
                  }
                  else {
                    if ( fov < 0.1 - EPSILON ) fov += 0.1;
                    else if ( fov < 1 - EPSILON ) fov++;
                    else if ( fov < 175 - EPSILON ) fov += 5;
                }
                updated = true;
                break;
              case 'a':
                if ( camera.getProjectionMode() == camera.PROJECTION_MODE_ORTHOGONAL ) {
                    orthogonalZoom *= 2;
                  }
                  else {
                    if ( fov > 5 + EPSILON ) fov -= 5;
                    else if ( fov > 1 + EPSILON  ) fov--;
                    else if ( fov > 0.1 + EPSILON  ) fov -= 0.1;
                }
                updated = true;
                break;

          case 'N':
                nearPlaneDistance = augmentLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
            break;
          case 'n':
                nearPlaneDistance = diminishLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
            break;

          case 'F':
                farPlaneDistance = augmentLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
            break;
          case 'f':
                farPlaneDistance = diminishLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
            break;

              case 'p': // Rote el modo de proyeccion
                switch ( projectionMode ) {
                  case Camera.PROJECTION_MODE_PERSPECTIVE:
                    projectionMode = Camera.PROJECTION_MODE_ORTHOGONAL;
                    break;
                  default:
                    projectionMode = Camera.PROJECTION_MODE_PERSPECTIVE;
                    break;
                }
                updated = true;
                break;

              // Queries
              case 'i':
                System.out.println(camera);
                break;
            }
        }

        // 4. Update camera's internal parameters from local copy
        R.eulerAnglesRotation(yaw, pitch, roll);

        camera.setPosition(eyePosition);
        camera.setFocusedPosition(focusedPosition);
        camera.setRotation(R);
        camera.setOrthogonalZoom(orthogonalZoom);
        camera.setFov(fov);
        camera.setProjectionMode(projectionMode);
        camera.setNearPlaneDistance(nearPlaneDistance);
        camera.setFarPlaneDistance(farPlaneDistance);

        return updated;
    }

    public boolean processKeyReleasedEventAwt(KeyEvent keyEvent) {
    return false;
    }

    public boolean processMousePressedEventAwt(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseReleasedEventAwt(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseClickedEventAwt(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseMovedEventAwt(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseDraggedEventAwt(MouseEvent e)
    {
        return false;
    }

    public boolean processMouseWheelEventAwt(MouseWheelEvent e)
    {
        return false;
    }

    public boolean processKeyPressedEventAwt(KeyEvent e)
    {
        return false;
    }

}
