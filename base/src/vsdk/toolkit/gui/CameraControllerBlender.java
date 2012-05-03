package vsdk.toolkit.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
//import java.awt.event.KeyEvent;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.environment.Camera;

public class CameraControllerBlender extends CameraController {

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
  
    public boolean processKeyPressedEvent(KeyEvent keyEvent) {
        System.out.println("MY EVENT!");
        return false;
    }

    public boolean processKeyPressedEventAwt(java.awt.event.KeyEvent keyEvent) {
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
        double deltaMov = 0.1;
        double angleInc;
        boolean updated = false;
        double EPSILON = 0.0001;
        int mask;
        int SHIFT_MASK = java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
        int CTRL_MASK = java.awt.event.KeyEvent.CTRL_DOWN_MASK;
        Vector3D u, v, w;
        Matrix4x4 DR, T1, T2, COMPUESTA;

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
        mask = keyEvent.getModifiersEx();

        u = new Vector3D(R.M[0][0], R.M[1][0], R.M[2][0]);
        v = new Vector3D(R.M[0][1], R.M[1][1], R.M[2][1]);
        w = new Vector3D(R.M[0][2], R.M[1][2], R.M[2][2]);

        if ( fov > 90 ) angleInc = Math.toRadians(10);
        else if ( fov > 45 ) angleInc = Math.toRadians(5);
        else if ( fov > 15 ) angleInc = Math.toRadians(2.5);
        else if ( fov > 5 ) angleInc = Math.toRadians(1);
        else angleInc = Math.toRadians(0.1);

        // 3. Event processing: update the copy of the camera's internal parameters
//        if ( unicode_id == keyEvent.CHAR_UNDEFINED ) {

            switch ( keycode ) {
              case java.awt.event.KeyEvent.VK_X:
                if ( (mask & SHIFT_MASK) == 0 ) {
                    // Minuscula
                    eyePosition.x -= deltaMov; focusedPosition.x -= deltaMov;
                    updated = true;
                }
                else if ( (mask & SHIFT_MASK) != 0 ) {
                    // Mayuscula
                    eyePosition.x += deltaMov; focusedPosition.x += deltaMov;
                    updated = true;
                }
                break;

              case java.awt.event.KeyEvent.VK_NUMPAD5: // Rote el modo de proyeccion
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

              case java.awt.event.KeyEvent.VK_NUMPAD4:
                if ( (mask & CTRL_MASK) == 0 ) {
                    // Rotation
                    T1 = new Matrix4x4();
                    DR = new Matrix4x4();
                    T2 = new Matrix4x4();
                    T1.translation(focusedPosition.multiply(-1));
                    DR.axisRotation(-Math.toRadians(15), 0, 0, 1);
                    T2.translation(focusedPosition);
                    COMPUESTA = T2.multiply(DR.multiply(T1));
                    eyePosition = COMPUESTA.multiply(eyePosition);
                    R = COMPUESTA.multiply(R);
                    updated = true;
                }
                else if ( (mask & CTRL_MASK) != 0 ) {
                    // Translation
                    eyePosition = 
                        eyePosition.add(v.multiply(deltaMov));
                    focusedPosition = 
                        focusedPosition.add(v.multiply(deltaMov));
                    updated = true;
                }
                break;

              case java.awt.event.KeyEvent.VK_NUMPAD6:
                if ( (mask & CTRL_MASK) == 0 ) {
                    // Rotation
                    T1 = new Matrix4x4();
                    DR = new Matrix4x4();
                    T2 = new Matrix4x4();
                    T1.translation(focusedPosition.multiply(-1));
                    DR.axisRotation(Math.toRadians(15), 0, 0, 1);
                    T2.translation(focusedPosition);
                    COMPUESTA = T2.multiply(DR.multiply(T1));
                    eyePosition = COMPUESTA.multiply(eyePosition);
                    R = COMPUESTA.multiply(R);
                    updated = true;
                }
                else if ( (mask & CTRL_MASK) != 0 ) {
                    // Translation
                    eyePosition = 
                        eyePosition.substract(v.multiply(deltaMov));
                    focusedPosition = 
                        focusedPosition.substract(v.multiply(deltaMov));
                    updated = true;
                }
                break;

              case java.awt.event.KeyEvent.VK_NUMPAD2:
                if ( (mask & CTRL_MASK) == 0 ) {
                    // Rotation
                    T1 = new Matrix4x4();
                    DR = new Matrix4x4();
                    T2 = new Matrix4x4();
                    T1.translation(focusedPosition.multiply(-1));
                    DR.axisRotation(-Math.toRadians(15), v.x, v.y, v.z);
                    T2.translation(focusedPosition);
                    COMPUESTA = T2.multiply(DR.multiply(T1));
                    eyePosition = COMPUESTA.multiply(eyePosition);
                    R = COMPUESTA.multiply(R);
                    updated = true;
                }
                else if ( (mask & CTRL_MASK) != 0 ) {
                    // Translation
                    eyePosition = 
                        eyePosition.substract(w.multiply(deltaMov));
                    focusedPosition = 
                        focusedPosition.substract(w.multiply(deltaMov));
                    updated = true;
                }
                break;

              case java.awt.event.KeyEvent.VK_NUMPAD8:
                if ( (mask & CTRL_MASK) == 0 ) {
                    // Rotation
                    T1 = new Matrix4x4();
                    DR = new Matrix4x4();
                    T2 = new Matrix4x4();
                    T1.translation(focusedPosition.multiply(-1));
                    DR.axisRotation(Math.toRadians(15), v.x, v.y, v.z);
                    T2.translation(focusedPosition);
                    COMPUESTA = T2.multiply(DR.multiply(T1));
                    eyePosition = COMPUESTA.multiply(eyePosition);
                    R = COMPUESTA.multiply(R);
                    updated = true;
                }
                else if ( (mask & CTRL_MASK) != 0 ) {
                    // Translation
                    eyePosition = 
                        eyePosition.add(w.multiply(deltaMov));
                    focusedPosition = 
                        focusedPosition.add(w.multiply(deltaMov));
                    updated = true;
                }
                break;

              case java.awt.event.KeyEvent.VK_NUMPAD1:
                updated = true;
                R.axisRotation(0, 0, 0, 1);
                break;

              case 107: // Warning: How to tell java.awt.event.KeyEvent.VK_NUMPAD_PLUS: ?
                // Translation
                eyePosition = 
                    eyePosition.add(u.multiply(deltaMov));
                focusedPosition = 
                    focusedPosition.add(u.multiply(deltaMov));
                updated = true;
                break;

              case 109: // Warning: How to tell java.awt.event.KeyEvent.VK_NUMPAD_LESS: ?
                // Translation
                eyePosition = 
                    eyePosition.substract(u.multiply(deltaMov));
                focusedPosition = 
                    focusedPosition.substract(u.multiply(deltaMov));
                updated = true;
                break;

            }


/*
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
    
              // Queries
              case 'i':
                System.out.println(camera);
                break;
            }
        }
*/
        // Heuristic to simulate Blender's orthogonal behavior
        double d = eyePosition.length();
        if ( d < 0.1 ) {
            orthogonalZoom = 10;
          }
          else {
            orthogonalZoom = 1 / d;
        }

        // 4. Update camera's internal parameters from local copy
        camera.setPosition(eyePosition);
        camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
        camera.setRotation(R);
        camera.setOrthogonalZoom(orthogonalZoom);
        camera.setFov(fov);
        camera.setProjectionMode(projectionMode);
        camera.setNearPlaneDistance(nearPlaneDistance);
        camera.setFarPlaneDistance(farPlaneDistance);
  
        return updated;
    }

    public boolean processKeyReleasedEventAwt(java.awt.event.KeyEvent keyEvent) {
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

    public Camera getCamera()
    {
        return camera;
    }

    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }

    public void setDeltaMovement(double factor)
    {
        ;
    }
}
