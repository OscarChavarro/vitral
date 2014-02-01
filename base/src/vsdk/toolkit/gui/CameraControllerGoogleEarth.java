/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package vsdk.toolkit.gui;


//import java.awt.event.MouseEvent;
//import java.awt.event.MouseWheelEvent;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.InfinitePlane;

/**
 *
 * @author LEIDY ALEXANDRA LOZANO JACOME
 */
public class CameraControllerGoogleEarth extends CameraController {

    private Camera camera;
    private double brinco;
    
    int Xold;
    int Yold;
    
    public CameraControllerGoogleEarth(Camera camera) {
       this.camera = camera; 
       brinco = 0.00000000000000001;
    }

    
    public boolean processMouseEvent(MouseEvent mouseEvent) {
         
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean processMousePressedEvent(MouseEvent e)
    {
        Xold=e.getX();
        Yold=e.getY();
        
     //  System.out.println("Presioné");
        //Procedimiento para selección de lineas
        camera.updateVectors();
        Ray RayA=camera.generateRay(Xold, Yold);
        
        
        
        return false;
    }

    @Override
    public boolean processMouseReleasedEvent(MouseEvent e)
    {
        
        return false;
    }

    @Override
    public boolean processMouseClickedEvent(MouseEvent e)
    {
        
        return false;
    }

    @Override
    public boolean processMouseMovedEvent(MouseEvent e)
    {
       
        return false;
    }    

    @Override
    public boolean processMouseDraggedEvent(MouseEvent e) {
       
        
        //---------------------------------------------------------------------
        // 1. Posición inicial y final
        //---------------------------------------------------------------------
        int PrevX= Xold;
        int PrevY=Yold;
         
        int x=e.getX();
        int y=e.getY();
        
        //---------------------------------------------------------------------
        // 2. Calculo rayo 1 y 2
        //---------------------------------------------------------------------
        
        Ray RayA=camera.generateRay(PrevX, PrevY);
        Ray RayB=camera.generateRay(x, y);
        
        //----------------------------------------------------------------------
        //3. Intercepción con plano infinito
        //----------------------------------------------------------------------
        
        InfinitePlane infinitePlane=new InfinitePlane(new Vector3D(0,0,1),new Vector3D(0,0,0));
        infinitePlane.doIntersection(RayA);
        infinitePlane.doIntersection(RayB);
        
        //----------------------------------------------------------------------
        //4. Distancia entre RayA y RayB
        //----------------------------------------------------------------------
        
        Vector3D pA=new Vector3D();
        Vector3D pB=new Vector3D();
        
        pA.x=RayA.origin.x+(RayA.direction.x*RayA.t);
        pA.y=RayA.origin.y+(RayA.direction.y*RayA.t);
        pA.z=RayA.origin.z+(RayA.direction.z*RayA.t);
        
        pB.x=RayB.origin.x+(RayB.direction.x*RayB.t);
        pB.y=RayB.origin.y+(RayB.direction.y*RayB.t);
        pB.z=RayB.origin.z+(RayB.direction.z*RayB.t);
        
        Vector3D d=pB.substract(pA);
        
        
        //----------------------------------------------------------------------
        //5. Mover la cámara
        //----------------------------------------------------------------------
        camera.getPosition().x=camera.getPosition().x-d.x;
        camera.getPosition().y=camera.getPosition().y-d.y;
        
        
        
        Xold=x;
        Yold=y;
             
       return true;
        
    }

  

    @Override
    public boolean processMouseWheelEvent(MouseEvent e)
    {
        /* COMENTADO PORQUE NO ESTA EL EVENTO EN LA ClASE MOUSE EVENT DE VITRAL!!!!!!
        
        // Local copy of the Camera's internal parameters
        Vector3D eyePosition;
        Vector3D focusedPosition;
        Matrix4x4 R; // Camera rotation matrix
        int projectionMode;
        double fov;
        double orthogonalZoom;
        double nearPlaneDistance;
        double farPlaneDistance;
        
        
        
         // 1. Obtain a copy of the camera's internal parameters
        eyePosition = camera.getPosition();
        focusedPosition = camera.getFocusedPosition();
        R = camera.getRotation();
        projectionMode = camera.getProjectionMode();
        fov = camera.getFov();
        orthogonalZoom = camera.getOrthogonalZoom();
        nearPlaneDistance = camera.getNearPlaneDistance();
        farPlaneDistance = camera.getFarPlaneDistance();


        
        
         int clicks = e.getWheelRotation();
        boolean updated = false;
           double altura=eyePosition.z;  
        //------------------------------------------------------------
        if ( clicks < 0 ) {

              //Cambia el delta para que el zoom vaya acorde al tamaño de la imagen
              double expo = Math.round(Math.log10(eyePosition.z))-1;
              brinco = Math.pow(10, expo );//

               
               //Limite inferiror
              if((eyePosition.z-brinco)<=12){return false;} //Fotos 0.0000000000000000000001
              
              nearPlaneDistance = altura*0.1;
              farPlaneDistance = altura*110;

      
            eyePosition.z -= brinco; focusedPosition.z -= brinco;
            updated = true;
         
         updated=true;
        }else if ( clicks > 0 ) {
             //Limite superior
          if((eyePosition.z+brinco)>=Math.pow(10, 24)){return false;}//Para las fotos Math.pow(10, 25)
     
          //Cambia el delta para que el zoom vaya acorde al tamaño de la imagen
               brinco=Math.pow(10, Math.round(Math.log10(eyePosition.z))-1);

               altura=eyePosition.z;
              
              nearPlaneDistance = altura*0.1;
              farPlaneDistance = altura*110;
               
            eyePosition.z += brinco; focusedPosition.z += brinco;
            updated = true;
        }
        
         // 4. Update camera's internal parameters from local copy
  //      R.eulerAnglesRotation(yaw, pitch, roll);
  
        camera.setPosition(eyePosition);
        camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
        camera.setRotation(R);
        camera.setOrthogonalZoom(orthogonalZoom);
        camera.setFov(fov);
        camera.setProjectionMode(projectionMode);
        camera.setNearPlaneDistance(nearPlaneDistance);
        camera.setFarPlaneDistance(farPlaneDistance);
        
        return updated;
        //------------------------------------------------------------
 */
        return false;
    }

    @Override
    public Camera getCamera()
    {
        return camera;
    }

    @Override
    public void setCamera(Camera camera)
    {
        this.camera = camera;
    }

    @Override
    public void setDeltaMovement(double factor) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean processKeyPressedEvent(vsdk.toolkit.gui.KeyEvent keyEvent) {
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
        double yaw;
        double pitch;
        double roll;
        double angleInc;
        boolean updated = false;
        double EPSILON = 0.0001;

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
        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();
  
        if ( fov > 90 ) angleInc = Math.toRadians(10);
        else if ( fov > 45 ) angleInc = Math.toRadians(5);
        else if ( fov > 15 ) angleInc = Math.toRadians(2.5);
        else if ( fov > 5 ) angleInc = Math.toRadians(1);
        else angleInc = Math.toRadians(0.1);
        
        
        
        

        // 3. Event processing: update the copy of the camera's internal parameters
        switch ( keyEvent.keycode ) {
          case vsdk.toolkit.gui.KeyEvent.KEY_UP:
            pitch -= angleInc;
            if ( pitch < Math.toRadians(-90) ) pitch = Math.toRadians(-90);
            System.out.println(pitch);
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_DOWN:
            pitch += angleInc;
            if ( pitch > Math.toRadians(90) ) pitch = Math.toRadians(90);
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_LEFT:
            yaw += angleInc;
            while ( yaw >= Math.toRadians(360) ) yaw -= Math.toRadians(360);
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_RIGHT:
            yaw -= angleInc;
            while ( yaw < 0 ) yaw += Math.toRadians(360);
            updated = true;
            break;

          // Position
          case vsdk.toolkit.gui.KeyEvent.KEY_x:
            eyePosition.x -= brinco; focusedPosition.x -= brinco;
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_X:
            eyePosition.x += brinco; focusedPosition.x += brinco;
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_y:
            eyePosition.y -= brinco; focusedPosition.y -= brinco;
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_Y:
            eyePosition.y += brinco; //focusedPosition.y += deltaMov;
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_z:
                 //Cambia el delta para que el zoom vaya acorde al tamaño de la imagen
              double expo = Math.round(Math.log10(eyePosition.z))-1;
              brinco = Math.pow(10, expo );//

                      
               double altura=eyePosition.z;  
              
               //Limite inferiror
              if((eyePosition.z-brinco)<=12){break;} //Fotos 0.0000000000000000000001
              
              nearPlaneDistance = altura*0.1;
              farPlaneDistance = altura*110;

      
            eyePosition.z -= brinco; focusedPosition.z -= brinco;
            updated = true;
            break;
              //---------------------------------------------------------------
              
              
              
          case vsdk.toolkit.gui.KeyEvent.KEY_Z:
        
          //Limite superior
          if((eyePosition.z+brinco)>=Math.pow(10, 3)){break;}//Para las fotos Math.pow(10, 25)
     
          //Cambia el delta para que el zoom vaya acorde al tamaño de la imagen
               brinco=Math.pow(10, Math.round(Math.log10(eyePosition.z))-1);

               altura=eyePosition.z;
              
              nearPlaneDistance = altura*0.1;
              farPlaneDistance = altura*110;
              
    //          System.out.println("Altura: "+eyePosition.z);
    //           System.out.println("near: "+nearPlaneDistance);
    //           System.out.println("far: "+farPlaneDistance);
              
  
               
            eyePosition.z += brinco; focusedPosition.z += brinco;
            updated = true;
            break; 
          // Rotation
          case vsdk.toolkit.gui.KeyEvent.KEY_S:
            roll -= Math.toRadians(5);
            while ( roll < 0 ) roll += Math.toRadians(360);
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_s:
            roll += Math.toRadians(5);
            while ( roll > Math.toRadians(360) ) roll -= Math.toRadians(360);
            updated = true;
            break;
  
          // View volume modification
          case vsdk.toolkit.gui.KeyEvent.KEY_A:
              System.out.println(camera);
            if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
                orthogonalZoom /= 2;
              }
              else {
                if ( fov < 0.1 - EPSILON ) fov += 0.1;
                else if ( fov < 1 - EPSILON ) fov++;
                else if ( fov < 175 - EPSILON ) fov += 5;
            }
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_a:
               System.out.println(camera);
            if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL ) {
                orthogonalZoom *= 2;
              }
              else {
                if ( fov > 5 + EPSILON ) fov -= 5;
                else if ( fov > 1 + EPSILON  ) fov--;
                else if ( fov > 0.1 + EPSILON  ) fov -= 0.1;
            }
            updated = true;
            break;
  
         case vsdk.toolkit.gui.KeyEvent.KEY_N:
             System.out.println(camera);
            nearPlaneDistance = nearPlaneDistance+0.5;// augmentLogarithmic(nearPlaneDistance, EPSILON);
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_n:
              System.out.println(camera);
            nearPlaneDistance = nearPlaneDistance-0.5;//diminishLogarithmic(nearPlaneDistance, EPSILON);
            updated = true;
            break;
  
          case vsdk.toolkit.gui.KeyEvent.KEY_F:
              System.out.println(camera);
            farPlaneDistance = farPlaneDistance+0.5;//augmentLogarithmic(farPlaneDistance, EPSILON);
            updated = true;
            break;
          case vsdk.toolkit.gui.KeyEvent.KEY_f:
              System.out.println(camera);
            farPlaneDistance = farPlaneDistance-0.5;//diminishLogarithmic(farPlaneDistance, EPSILON);
            updated = true;
            break;
  
          case vsdk.toolkit.gui.KeyEvent.KEY_p: // Rote el modo de proyeccion
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
          case vsdk.toolkit.gui.KeyEvent.KEY_i:
            System.out.println(camera);
            break;

        }

        // 4. Update camera's internal parameters from local copy
        R.eulerAnglesRotation(yaw, pitch, roll);
  
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

    @Override
    public boolean processKeyReleasedEvent(KeyEvent keyEvent) {
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
