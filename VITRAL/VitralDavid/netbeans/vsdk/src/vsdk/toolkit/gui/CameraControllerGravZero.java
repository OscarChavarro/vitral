package vsdk.toolkit.gui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyEvent;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Camera;

public class CameraControllerGravZero implements CameraController 
{

    private Camera camera;
    private int oldMouseX;
    private int oldMouseY; 
    
    public CameraControllerGravZero(Camera camera) 
    {
        this.camera = camera;
        oldMouseX = 0;
        oldMouseY = 0;
    }

    public boolean processMouseEventAwt(MouseEvent mouseEvent) 
    {
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
  
    public boolean processKeyPressedEventAwt(KeyEvent keyEvent) 
    {
        double movX=0;
        double movY=0;
        double movZ=0;
        
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
        double yaw=0;
        double pitch=0;
        double roll=0;
        double angleInc=7;
        boolean updated = false;
        double EPSILON = 0.0001;

        // 1. Obtain a copy of the camera's internal parameters
        projectionMode = camera.getProjectionMode();
        fov = camera.getFov();
        orthogonalZoom = camera.getOrthogonalZoom();
        nearPlaneDistance = camera.getNearPlaneDistance();
        farPlaneDistance = camera.getFarPlaneDistance();

        // 2. Calculate variables used for interaction manipulation
        unicode_id = keyEvent.getKeyChar();
        keycode = keyEvent.getKeyCode();

        // 3. Event processing: update the copy of the camera's internal parameters
        if ( unicode_id == keyEvent.CHAR_UNDEFINED ) {
            switch ( keycode ) {
              case KeyEvent.VK_UP:
                pitch = -angleInc;
                yaw=roll=0;
                updated = true;
                break;
              case KeyEvent.VK_DOWN:
                pitch = angleInc;
                yaw=roll=0;
                updated = true;
                break;
              case KeyEvent.VK_LEFT:
                yaw = angleInc;
                pitch=roll=0;
                updated = true;
                break;
              case KeyEvent.VK_RIGHT:
                yaw = -angleInc;
                pitch=roll=0;
                updated = true;
                break;
            }

        }
        else {
            switch ( unicode_id ) {
              // Position
              case 'a':
                movX = deltaMov; 
                updated = true;
                break;
              case 'd':
                movX = -deltaMov; 
                updated = true;
                break;
              case 'q':
                movY = -deltaMov; 
                updated = true;
                break;
              case 'e':
                movY = deltaMov; 
                updated = true;
                break;
              case 'w':
                movZ = deltaMov; 
                updated = true;
                break;
              case 's':
                movZ = -deltaMov; 
                updated = true;
                break; 
              // Rotation
              case 'z':
                roll = -7;
                pitch=yaw=0;
                updated = true;
                break;
              case 'x':
                roll = 7;
                pitch=yaw=0;
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
              case 'S':
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
        R=camera.getRotation(pitch, yaw, roll);
        
        camera.translate(movX, movY, movZ);
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
        oldMouseX = e.getX();
        oldMouseY = e.getX();
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
    
    boolean shift=true;
    
    public boolean processMouseMovedEventAwt(MouseEvent e)
    {   
        if(e.isShiftDown())
        {
            shift=true;
            java.awt.Component component=(java.awt.Component)e.getSource();
            component.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
            
            return false;
        }
        
        if(shift)
        {
            java.awt.image.BufferedImage cursor = new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Cursor transparentCursor = java.awt.Toolkit.getDefaultToolkit().createCustomCursor(cursor,new java.awt.Point(0,0),"TransparentCursor");
            java.awt.Component component=(java.awt.Component)e.getSource();
            component.setCursor(transparentCursor);
            shift=false;
        }
        //------------------------------------------------------------
        int deltaX;
        int deltaY;
        boolean updated = true;

        deltaX = e.getX() - oldMouseX;
        deltaY = e.getY() - oldMouseY;
        
        if ( deltaX > 5 ) deltaX = 5;
        if ( deltaX < -5 ) deltaX = -5;
        if ( deltaY > 5 ) deltaY = 5;
        if ( deltaY < -5 ) deltaY = -5;

        //------------------------------------------------------------
        Matrix4x4 R; // Camera rotation matrix

        R = camera.getRotation(deltaY, -deltaX, 0);

        camera.setRotation(R);

        //------------------------------------------------------------
        

        camera.setRotation(R);
        
        try
        {
            java.awt.Component component=(java.awt.Component)e.getSource();
            java.awt.Point posComp=component.getLocationOnScreen();
            
            java.awt.Robot r=new java.awt.Robot();
            r.mouseMove(posComp.x+((int)component.getWidth()/2),posComp.y+((int)component.getHeight()/2));
            
            java.awt.Point p=((java.awt.Component)e.getSource()).getMousePosition();
            
            oldMouseX=p.x;
            oldMouseY=p.y;
        }
        catch(java.awt.AWTException awte)
        {
        
        }
        return updated;
    }

    public boolean processMouseDraggedEventAwt(MouseEvent e)
    {
        //------------------------------------------------------------
        int deltaX;
        int deltaY;
        boolean updated = true;
        double senseFactor = 0.04;

        deltaX = e.getX() - oldMouseX;
        deltaY = e.getY() - oldMouseY;

        if ( deltaX > 5 ) deltaX = 5;
        if ( deltaX < -5 ) deltaX = -5;
        if ( deltaY > 5 ) deltaY = 5;
        if ( deltaY < -5 ) deltaY = -5;

        //------------------------------------------------------------
        Matrix4x4 R; // Camera rotation matrix

        // Obtain a copy of the camera's internal parameters

        R = camera.getRotation(0,0, deltaX);

        // Update camera's internal parameters from local copy
        //R.eulerAnglesRotation(yaw, pitch, roll);
        camera.setRotation(R);

        //------------------------------------------------------------
        oldMouseX = e.getX();
        oldMouseY = e.getY();
        return updated;
    }

    public boolean processMouseWheelEventAwt(MouseWheelEvent e)
    {
        //------------------------------------------------------------
        double fov, angleInc;
        boolean updated = false;
        double EPSILON = 0.0001;
        double orthogonalZoom;

        fov = camera.getFov();
        orthogonalZoom = camera.getOrthogonalZoom();

        if ( fov > 90 ) angleInc = Math.toRadians(10);
        else if ( fov > 45 ) angleInc = Math.toRadians(5);
        else if ( fov > 15 ) angleInc = Math.toRadians(2.5);
        else if ( fov > 5 ) angleInc = Math.toRadians(1);
        else angleInc = Math.toRadians(0.1);

        int clicks = e.getWheelRotation();

        //------------------------------------------------------------
        if ( clicks > 0 ) {
            if ( camera.getProjectionMode() == camera.PROJECTION_MODE_ORTHOGONAL ) {
                orthogonalZoom /= clicks;
              }
              else {
                if ( fov < 0.1 - EPSILON ) fov += 0.1*clicks;
                else if ( fov < 1 - EPSILON ) fov += clicks;
                else if ( fov < 175 - EPSILON ) fov += 5*clicks;
            }
            updated = true;
        }
        else if ( clicks < 0 ) {
            if ( camera.getProjectionMode() == camera.PROJECTION_MODE_ORTHOGONAL ) {
                orthogonalZoom *= 2*clicks;
              }
              else {
                if ( fov > 5 + EPSILON ) fov -= 5*clicks;
                else if ( fov > 1 + EPSILON  ) fov -= clicks;
                else if ( fov > 0.1 + EPSILON  ) fov -= 0.1*clicks;
            }
            updated = true;
        }

        //------------------------------------------------------------
        camera.setFov(fov);
        camera.setOrthogonalZoom(orthogonalZoom);

        return updated;
    }
}
