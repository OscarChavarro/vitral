package vsdk.toolkit.gui;

import vsdk.toolkit.environment.geometry.element.Ray;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.common.linealAlgebra.Vector3Dd;
import vsdk.toolkit.environment.camera.Camera;
import vsdk.toolkit.environment.geometry.surface.InfinitePlane;

public class CameraControllerGoogleEarth extends CameraController {
    private Camera camera;
    private double jumpStep;
    private int xOld;
    private int yOld;

    /**
    @param camera
    */
    public CameraControllerGoogleEarth(Camera camera) {
        this.camera = camera;
        jumpStep = 0.00000000000000001;
    }

    /**
    @param mouseEvent
    @return true if current event leads to a model update, false if not.
    */
    public boolean processMouseEvent(MouseEvent mouseEvent) {

        return false; //To change body of generated methods, choose Tools | Templates.
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processMousePressedEvent(MouseEvent e) {
        setxOld(e.getX());
        setyOld(e.getY());

        // Procedure for line selection
        camera.updateVectors();
        Ray RayA = camera.generateRay(getxOld(), getyOld());

        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processMouseReleasedEvent(MouseEvent e) {

        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processMouseClickedEvent(MouseEvent e) {

        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processMouseMovedEvent(MouseEvent e) {

        return false;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processMouseDraggedEvent(MouseEvent e) {

        //---------------------------------------------------------------------
        // 1. Initial and final position
        int PrevX = getxOld();
        int PrevY = getyOld();

        int x = e.getX();
        int y = e.getY();

        //---------------------------------------------------------------------
        // 2. Ray 1 and 2 calculation
        Ray RayA = camera.generateRay(PrevX, PrevY);
        Ray RayB = camera.generateRay(x, y);

        //----------------------------------------------------------------------
        // 3. Intersection with the infinite plane
        InfinitePlane infinitePlane = new InfinitePlane(new Vector3Dd(0, 0, 1), new Vector3Dd(0, 0, 0));
        Ray hitA = infinitePlane.doIntersection(RayA);
        Ray hitB = infinitePlane.doIntersection(RayB);
        if ( hitA == null || hitB == null ) {
            return false;
        }

        //----------------------------------------------------------------------
        // 4. Distance between RayA and RayB
        Vector3Dd pA = new Vector3Dd();
        Vector3Dd pB = new Vector3Dd();

        pA = pA.withX(hitA.getOrigin().x() + (hitA.getDirection().x() * hitA.getT()));
        pA = pA.withY(hitA.getOrigin().y() + (hitA.getDirection().y() * hitA.getT()));
        pA = pA.withZ(hitA.getOrigin().z() + (hitA.getDirection().z() * hitA.getT()));

        pB = pB.withX(hitB.getOrigin().x() + (hitB.getDirection().x() * hitB.getT()));
        pB = pB.withY(hitB.getOrigin().y() + (hitB.getDirection().y() * hitB.getT()));
        pB = pB.withZ(hitB.getOrigin().z() + (hitB.getDirection().z() * hitB.getT()));

        Vector3Dd d = pB.subtract(pA);

        //----------------------------------------------------------------------
        // 5. Move the camera
        Vector3Dd currentPosition = camera.getPosition();
        camera.setPosition(new Vector3Dd(
            currentPosition.x() - d.x(),
            currentPosition.y() - d.y(),
            currentPosition.z()));

        setxOld(x);
        setyOld(y);

        return true;
    }

    /**
    @param e
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processMouseWheelEvent(MouseEvent e) {
        // Local copy of the Camera's internal parameters
        Vector3Dd eyePosition;
        Vector3Dd focusedPosition;
        Matrix4x4d R; // Camera rotation matrix
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

        int clicks = e.getClicks();//();
        boolean updated = false;
        double altura = eyePosition.z();
        //------------------------------------------------------------
        if (clicks < 0) {

            // Adjust the delta so the zoom matches the image size
            double expo = Math.round(Math.log10(eyePosition.z())) - 1;
            jumpStep = Math.pow(10, expo);//

            // Lower limit
            if ((eyePosition.z() - jumpStep) <= 12) {
                return false;
            } // Photos 0.0000000000000000000001

            nearPlaneDistance = altura * 0.1;
            farPlaneDistance = altura * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() - jumpStep);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpStep);

            updated = true;
        } else if (clicks > 0) {
            // Upper limit
            if ((eyePosition.z() + jumpStep) >= Math.pow(10, 24)) {
                return false;
            } // For photos Math.pow(10, 25)

            // Adjust the delta so the zoom matches the image size
            jumpStep = Math.pow(10, Math.round(Math.log10(eyePosition.z())) - 1);

            altura = eyePosition.z();

            nearPlaneDistance = altura * 0.1;
            farPlaneDistance = altura * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() + jumpStep);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpStep);
            updated = true;
        }

         // 4. Update camera's internal parameters from local copy
        //      R = R.eulerAnglesRotation(yaw, pitch, roll);
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

    /**
    @return current camera being under control
    */
    @Override
    public Camera getCamera() {
        return camera;
    }

    /**
    @param camera
    */
    @Override
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    /**
    @param keyEvent
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processKeyPressedEvent(vsdk.toolkit.gui.KeyEvent keyEvent) {
        // Local copy of the Camera's internal parameters
        Vector3Dd eyePosition;
        Vector3Dd focusedPosition;
        Matrix4x4d R; // Camera rotation matrix
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

        if (fov > 90) {
            angleInc = Math.toRadians(10);
        } else if (fov > 45) {
            angleInc = Math.toRadians(5);
        } else if (fov > 15) {
            angleInc = Math.toRadians(2.5);
        } else if (fov > 5) {
            angleInc = Math.toRadians(1);
        } else {
            angleInc = Math.toRadians(0.1);
        }

        // 3. Event processing: update the copy of the camera's internal parameters
        switch (keyEvent.keycode) {
            case KeyEvent.KEY_UP:
                pitch -= angleInc;
                if (pitch < Math.toRadians(-90)) {
                    pitch = Math.toRadians(-90);
                }
                updated = true;
                break;
            case KeyEvent.KEY_DOWN:
                pitch += angleInc;
                if (pitch > Math.toRadians(90)) {
                    pitch = Math.toRadians(90);
                }
                updated = true;
                break;
            case KeyEvent.KEY_LEFT:
                yaw += angleInc;
                while (yaw >= Math.toRadians(360)) {
                    yaw -= Math.toRadians(360);
                }
                updated = true;
                break;
            case KeyEvent.KEY_RIGHT:
                yaw -= angleInc;
                while (yaw < 0) {
                    yaw += Math.toRadians(360);
                }
                updated = true;
                break;

            // Position
            case KeyEvent.KEY_x:
                eyePosition = eyePosition.withX(eyePosition.x() - jumpStep);
                focusedPosition = focusedPosition.withX(focusedPosition.x() - jumpStep);
                updated = true;
                break;
            case KeyEvent.KEY_X:
                eyePosition = eyePosition.withX(eyePosition.x() + jumpStep);
                focusedPosition = focusedPosition.withX(focusedPosition.x() + jumpStep);
                updated = true;
                break;
            case KeyEvent.KEY_y:
                eyePosition = eyePosition.withY(eyePosition.y() - jumpStep);
                focusedPosition = focusedPosition.withY(focusedPosition.y() - jumpStep);
                updated = true;
                break;
            case KeyEvent.KEY_Y:
                eyePosition = eyePosition.withY(eyePosition.y() + jumpStep); //focusedPosition.y() += deltaMov;
                updated = true;
                break;
            case KeyEvent.KEY_z:
                // Adjust the delta so the zoom matches the image size
                double expo = Math.round(Math.log10(eyePosition.z())) - 1;
                jumpStep = Math.pow(10, expo);//

                double altura = eyePosition.z();

                // Lower limit
                if ((eyePosition.z() - jumpStep) <= 12) {
                    break;
                } // Photos 0.0000000000000000000001

                nearPlaneDistance = altura * 0.1;
                farPlaneDistance = altura * 110;

                eyePosition = eyePosition.withZ(eyePosition.z() - jumpStep);
                focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpStep);
                updated = true;
                break;
              //---------------------------------------------------------------

            case KeyEvent.KEY_Z:

                // Upper limit
                if ((eyePosition.z() + jumpStep) >= Math.pow(10, 4)) {
                    break;
                } // For photos Math.pow(10, 25)

                // Adjust the delta so the zoom matches the image size
                jumpStep = Math.pow(10, Math.round(Math.log10(eyePosition.z())) - 1);

                altura = eyePosition.z();

                nearPlaneDistance = altura * 0.1;
                farPlaneDistance = altura * 110;

                eyePosition = eyePosition.withZ(eyePosition.z() + jumpStep);
                focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpStep);
                updated = true;
                break;
            // Rotation
            case KeyEvent.KEY_S:
                roll -= Math.toRadians(5);
                while (roll < 0) {
                    roll += Math.toRadians(360);
                }
                updated = true;
                break;
            case KeyEvent.KEY_s:
                roll += Math.toRadians(5);
                while (roll > Math.toRadians(360)) {
                    roll -= Math.toRadians(360);
                }
                updated = true;
                break;

            // View volume modification
            case KeyEvent.KEY_A:
                if (camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL) {
                    orthogonalZoom /= 2;
                } else {
                    if (fov < 0.1 - EPSILON) {
                        fov += 0.1;
                    } else if (fov < 1 - EPSILON) {
                        fov++;
                    } else if (fov < 175 - EPSILON) {
                        fov += 5;
                    }
                }
                updated = true;
                break;
            case KeyEvent.KEY_a:
                if (camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL) {
                    orthogonalZoom *= 2;
                } else {
                    if (fov > 5 + EPSILON) {
                        fov -= 5;
                    } else if (fov > 1 + EPSILON) {
                        fov--;
                    } else if (fov > 0.1 + EPSILON) {
                        fov -= 0.1;
                    }
                }
                updated = true;
                break;

            case KeyEvent.KEY_N:
                nearPlaneDistance = nearPlaneDistance + 0.5;// augmentLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
                break;
            case KeyEvent.KEY_n:
                nearPlaneDistance = nearPlaneDistance - 0.5;//diminishLogarithmic(nearPlaneDistance, EPSILON);
                updated = true;
                break;

            case KeyEvent.KEY_F:
                farPlaneDistance = farPlaneDistance + 0.5;//augmentLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
                break;
            case KeyEvent.KEY_f:
                farPlaneDistance = farPlaneDistance - 0.5;//diminishLogarithmic(farPlaneDistance, EPSILON);
                updated = true;
                break;

            case KeyEvent.KEY_p: // Rote el modo de proyeccion
                switch (projectionMode) {
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
            case KeyEvent.KEY_i:
                System.out.println(camera);
                break;
        }

        // 4. Update camera's internal parameters from local copy
        R = R.eulerAnglesRotation(yaw, pitch, roll);

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

    /**
    @param keyEvent
    @return true if current event leads to a model update, false if not.
    */
    @Override
    public boolean processKeyReleasedEvent(KeyEvent keyEvent) {
        return false; //To change body of generated methods, choose Tools | Templates.
    }

    /**
    @param factor
    */
    @Override
    public void setDeltaMovement(double factor) {

    }

    /**
    @return the xOld
    */
    public int getxOld() {
        return xOld;
    }

    /**
    @param xOld the xOld to set
    */
    public void setxOld(int xOld) {
        this.xOld = xOld;
    }

    /**
    @return the yOld
    */
    public int getyOld() {
        return yOld;
    }

    /**
    @param yOld the yOld to set
    */
    public void setyOld(int yOld) {
        this.yOld = yOld;
    }

    /**
    @param jumpValue
    */
    public void zoomOut(double jumpValue) {
        Vector3Dd eyePosition;
        Vector3Dd focusedPosition;

        double nearPlaneDistance;
        double farPlaneDistance;

        eyePosition = camera.getPosition();
        focusedPosition = camera.getFocusedPosition();

        double altura = eyePosition.z();

        nearPlaneDistance = altura * 0.1;
        farPlaneDistance = altura * 110;

        eyePosition = eyePosition.withZ(eyePosition.z() + jumpValue);
        focusedPosition = focusedPosition.withZ(focusedPosition.z() + jumpValue);

        camera.setPosition(eyePosition);
        camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
        camera.setNearPlaneDistance(nearPlaneDistance);
        camera.setFarPlaneDistance(farPlaneDistance);

    }

    /**
    @param jumpValue
    */
    public void zoomIn(double jumpValue) {

        Vector3Dd eyePosition;
        Vector3Dd focusedPosition;

        double nearPlaneDistance;
        double farPlaneDistance;

        eyePosition = camera.getPosition();
        focusedPosition = camera.getFocusedPosition();

        double altura = eyePosition.z();
        if ((eyePosition.z() - jumpValue) >= 12) {
            nearPlaneDistance = altura * 0.1;
            farPlaneDistance = altura * 110;

            eyePosition = eyePosition.withZ(eyePosition.z() - jumpValue);
            focusedPosition = focusedPosition.withZ(focusedPosition.z() - jumpValue);

            camera.setPosition(eyePosition);
            camera.setFocusedPositionMaintainingOrthogonality(focusedPosition);
            camera.setNearPlaneDistance(nearPlaneDistance);
            camera.setFarPlaneDistance(farPlaneDistance);
        }
    }
    
}
