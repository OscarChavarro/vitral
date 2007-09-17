//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//= - August 24 2005 - David Diaz / Cesar Bustacara: Design changes to      =
//=   decouple JOGL from the Camera data model, extra utilitary methods     =
//=   added                                                                 =
//= - August 25 2005 - Oscar Chavarro: English translation of comments      =
//= - September 12 2005 - Oscar Chavarro: generateRay updated               =
//= - November 15 2005 - Oscar Chavarro: generateRay updated (Bug?)         =
//= - November 23 2005 - Oscar Chavarro: updated methods for direct access  =
//=   of coordinate base system and access maintaining ortoghonality.       =
//= - November 24 2005 - Oscar Chavarro: new generateRay algorithm, now     =
//=   consistent with JOGL/OpenGL transformation interpretation.            =
//= - April 7 2006 - Oscar Chavarro: calculateUPlaneAtPixel, proyectPoint   =
//= - November 5 2006 - Oscar Chavarro: plane calculation methods updated   =
//= - November 5 2006 - Oscar Chavarro: added Cohen-Sutherland line         =
//=   clipping functionality                                                =
//===========================================================================

package vsdk.toolkit.environment;

import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.Entity;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Ray;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.InfinitePlane;

public class Camera extends Entity
{
    /// Check the general attribute description in superclass Entity.
    public static final long serialVersionUID = 20060502L;

    // Basic Camera Model
    private Vector3D up;
    private Vector3D front;
    private Vector3D left;
    private Vector3D eyePosition;
    private double focalDistance;
    private int projectionMode;
    private double fov;
    private double orthogonalZoom;
    private double nearPlaneDistance;
    private double farPlaneDistance;

    /// This string should be used for specific application defined
    /// functionality. Can be null.
    private String name;

    // Global constants
    public static final int OPCODE_FAR = (0x01 << 1);
    public static final int OPCODE_NEAR = (0x01 << 2);
    public static final int OPCODE_RIGHT = (0x01 << 3);
    public static final int OPCODE_LEFT = (0x01 << 4);
    public static final int OPCODE_UP = (0x01 << 5);
    public static final int OPCODE_DOWN = (0x01 << 6);

    public static final int STEREO_MODE_CENTER = 1;
    public static final int STEREO_MODE_LEFT_EYE = 2;
    public static final int STEREO_MODE_RIGHT_EYE = 3;

    public static final int PROJECTION_MODE_ORTHOGONAL = 4;
    public static final int PROJECTION_MODE_PERSPECTIVE = 5;

    /**
    Una `Camera` debe saber de qu&eacute; tama&ntilde;o es el viewport para
    el cual est&aacute; generando una proyecci&oacute;n, para poder modificar
    sus par&aacute;metros internos en funci&oacute;n del &aacute;ngulo de
    vision (`fov`) y de la actual proporci&oacute;n de ancho/alto del
    viewport. Las variables internas `viewport_xsize` y `viewport_ysize`
    representan el tama&nacute;o en pixels para el viewport, y son valores
    que solo pueden ser cambiados por el m&eacute;todo 
    `Camera::updateViewportResize`. Estos dos valores son para uso interno de
    la clase c&aacute;mara y no pueden ser consultados (i.e. son una copia
    de la configuraci&oacute;n del viewport, que debe ser administrado por
    la aplicaci&oacute;n que use `Camera`s).
    */
    private double viewport_xsize;
    /// Ver la documentaci&oacute;n de `viewport_xsize`
    private double viewport_ysize;

    // Vectores privados que se preprocesan para agilizar los calculos
    private Vector3D dx, dy, _dir, upWithScale, rightWithScale;
    
    public Camera() 
    {
        eyePosition = new Vector3D(0,-5,1);
        
        up = new Vector3D(0,0,1);
        front=new Vector3D(0,1,0);
        left=new Vector3D(-1,0,0);
        
        fov = 60;
        viewport_xsize = 320;
        viewport_ysize = 320;

        projectionMode = PROJECTION_MODE_PERSPECTIVE;
        orthogonalZoom = 1;
        nearPlaneDistance = 0.05;
        farPlaneDistance = 100;

        focalDistance = 10;
        updateVectors();
    }

    public Camera(Camera b)
    {
        eyePosition = new Vector3D(b.eyePosition);
        
        up = new Vector3D(b.up);
        front=new Vector3D(b.front);
        left=new Vector3D(b.left);
        
        fov = b.fov;
        viewport_xsize = b.viewport_xsize;
        viewport_ysize = b.viewport_ysize;

        projectionMode = b.projectionMode;
        orthogonalZoom = b.orthogonalZoom;
        nearPlaneDistance = b.nearPlaneDistance;
        farPlaneDistance = b.farPlaneDistance;

        focalDistance = b.focalDistance;

        updateVectors();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String n)
    {
        name = new String(n);
    }

    public double getViewportXSize()
    {
        return viewport_xsize;
    }

    public double getViewportYSize()
    {
        return viewport_ysize;
    }

    public Vector3D getPosition()
    {
        return eyePosition;
    }

    public void setPosition(Vector3D eyePosition)
    {
        this.eyePosition = eyePosition;
    }

    
    public Vector3D getFocusedPosition()
    {
        return eyePosition.add(front.multiply(focalDistance));
    }

    /**
    This method changes the `front` unit vector and the `focalDistance` based 
    on `focusedPosition` parameter and current `eyePosition` value, WITHOUT
    changing any other vector. This method does NOT change the value of `up`
    of `left` vectors and can be used in advanced applications to directly
    access basic camera parameters.
     */
    public void setFocusedPositionDirect(Vector3D focusedPosition)
    {
        front = focusedPosition.substract(eyePosition);
        focalDistance = front.length();
        front.normalize();
    }

    /**
    This method changes the `front` unit vector, the `focalDistance`, the 
    `left` vector and the `up` vector, based on:
      - `focusedPosition` parameter
      - current `eyePosition` value
      - current `up` vector (taken as a hint)
    This method CAN change the value of `up` and `left` vectors, to
    allow the user specify a left-handed orthogonal reference frame formed by
    the vectors <up, front, left>. The initial up vector is taken as a hint
    to specify a new up vector, similar to the original but forming a 90
    degree angle with the front direction. The left vector is always changed
    to form a third orthogonal vectors to former ones. Note that the three
    resulting vectors are left normalized.
    \todo
    This method FAILS if the initial value of `up` is parallel to the
    `front` direction. Validation and exception handling are needed.
     */
    public void setFocusedPositionMaintainingOrthogonality(Vector3D focusedPosition)
    {
        front = focusedPosition.substract(eyePosition);
        focalDistance=front.length();
        front.normalize();
        
        left = up.crossProduct(front);
        left.normalize();
        
        up = front.crossProduct(left);
        up.normalize();
    }

    public Vector3D getUp()
    {
        return up;
    }

    public Vector3D getFront()
    {
        return front;
    }

    public Vector3D getLeft()
    {
        return left;
    }

    /**
     * En este metodo no se tiene en cuenta si up y front quedan mirando 
       para el mismo lado
     */
    public void setUpDirect(Vector3D up)
    {
        this.up = new Vector3D(up);
    }

    public void setLeftDirect(Vector3D left)
    {
        this.left = new Vector3D(left);
    }

    /**
     * En este metodo no se tiene en cuenta si up y front quedan mirando 
       para el mismo lado
     */
    public void setUpMaintainingOrthogonality(Vector3D up)
    {
        up.normalize();

        left = up.crossProduct(front);
        left.normalize();
 
        this.up=front.crossProduct(left);
        this.up.normalize();
    }

    public double getFov()
    {
        return fov;
    }

    public void setFov(double fov)
    {
        this.fov = fov;
    }

    public double getNearPlaneDistance()
    {
        return nearPlaneDistance;
    }

    public void setNearPlaneDistance(double nearPlaneDistance)
    {
        this.nearPlaneDistance = nearPlaneDistance;
    }

    public double getFarPlaneDistance()
    {
        return farPlaneDistance;
    }

    public void setFarPlaneDistance(double farPlaneDistance)
    {
        this.farPlaneDistance = farPlaneDistance;
    }

    public int getProjectionMode()
    {
        return projectionMode;
    }

    public void setProjectionMode(int projectionMode)
    {
        this.projectionMode = projectionMode;
    }

    public void updateViewportResize(int dx, int dy)
    {
        viewport_xsize = dx;
        viewport_ysize = dy;
        updateVectors();
    }

    /**
    PRE:
      - focusedPosition y eyePosition tienen que ser diferentes!
    POST:
      - left queda normalizado
      - up queda normalizado
    \todo 
    Document the way in which vectors are calculated, acording to
    the projection transformation.
    */
    
    public void updateVectors()
    {
        up.normalize();
        left.normalize();
        front.normalize();

        double fovFactor = viewport_xsize/viewport_ysize;
        _dir = front.multiply(0.5);
        upWithScale = up.multiply(Math.tan(Math.toRadians(fov/2)));
        rightWithScale = left.multiply(-fovFactor*Math.tan(Math.toRadians(fov/2)));
    }

    /**
    Given a 2D integer coordinate in viewport space, this method calculates a proyector ray
    that emanates from the eye position and passes over the (u, v) float coordinate in the
    projection plane. Note that the (u, v) coordinate correspond to the (x, y) coordinate.

    This method is of vital importance to many fundamental algorithms of visualization
    (i.e. ray casting, ray tracing, radiosity), object selection and others (simulation,
    colition detection, visual debugging). As it is important to improve the efficiency of
    this method, some precalculated values are stored in the class attributes `_dir`,
    `upWithScale` and `rightWithScale`, which values are stored in the `updateVectors`
    method, leading to the precondition:

    PRE:
      - At least a call to the updateVectors method must be done before calling this method,
        and after changing any camera parameter the updateVectors method must be called again
        to reflect the changes in this calculation.
    */
    public final Ray generateRay(int x, int y)
    {
        double u, v;
        double mi_x, mi_y;

        // 1. Convert integer image coordinates into values in the range [-0.5, 0.5]
        u = ((double)x - viewport_xsize/2.0) / viewport_xsize;
        v = ((viewport_ysize - (double)y - 1) -  viewport_ysize/2.0) / viewport_ysize;

        // 2. Calculate the ray direction
        Vector3D dv = upWithScale.multiply(v);
        Vector3D du = rightWithScale.multiply(u);
        Vector3D dir = dv.add(du).add(_dir);

        // 3. Build up and return a ray with origin in the eye position and with calculated direction
        Ray ray;

        ray = new Ray(eyePosition, dir);

        return ray;
    }

    public double getOrthogonalZoom()
    {
        return orthogonalZoom;
    }

    public void setOrthogonalZoom(double orthogonalZoom)
    {
        this.orthogonalZoom = orthogonalZoom;
    }

    public void setRotation(Matrix4x4 R)
    {
        up.x = R.M[0][2];
        up.y = R.M[1][2];
        up.z = R.M[2][2];
        up.normalize();

        front.x = R.M[0][0];
        front.y = R.M[1][0];
        front.z = R.M[2][0];
        front.normalize();

        left.x=R.M[0][1];
        left.y=R.M[1][1];
        left.z=R.M[2][1];
        left.normalize();
    }

    public Matrix4x4 getRotation()
    {
        //------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        R.identity();
        R.M[0][0] = front.x; R.M[0][1] = left.x; R.M[0][2] = up.x;
        R.M[1][0] = front.y; R.M[1][1] = left.y; R.M[1][2] = up.y;
        R.M[2][0] = front.z; R.M[2][1] = left.z; R.M[2][2] = up.z;

        return R;
    }
    
    /**
    Note that projectionMatrix = transformationMatrix*viewVolumeMatrix
    */
    public Matrix4x4 calculateViewVolumeMatrix()
    {
        //- Calculate the base projection matrix ----------------------------
        double leftDistance, rightDistance, upDistance, downDistance, aspect;
        Matrix4x4 P = new Matrix4x4();

        aspect = viewport_xsize / viewport_ysize; 
        switch ( projectionMode ) {
          case Camera.PROJECTION_MODE_ORTHOGONAL:
            P.orthogonalProjection(-aspect/orthogonalZoom,
                                    aspect/orthogonalZoom,
                                   -1/orthogonalZoom, 1/orthogonalZoom,
                                   nearPlaneDistance, farPlaneDistance);
            break;
          case Camera.PROJECTION_MODE_PERSPECTIVE:
            upDistance = nearPlaneDistance * Math.tan(Math.toRadians(fov/2));
            downDistance = -upDistance;
            leftDistance = aspect * downDistance;
            rightDistance = aspect * upDistance;
            P.frustumProjection(leftDistance, rightDistance,
                                downDistance, upDistance,
                                nearPlaneDistance, farPlaneDistance);
            break;
        }
        return P;
    }

    public Matrix4x4 calculateTransformationMatrix()
    {
        return calculateTransformationMatrix(STEREO_MODE_CENTER);
    }
    /**
    Note that projectionMatrix = transformationMatrix*viewVolumeMatrix
    */
    public Matrix4x4 calculateTransformationMatrix(int stereoMode)
    {
        //- Take into account the camera position and orientation ----------
        Matrix4x4 R;
        Matrix4x4 R1;
        Matrix4x4 T1 = new Matrix4x4();
        Matrix4x4 R_adic2 = new Matrix4x4();
        Matrix4x4 R_adic1 = new Matrix4x4();
        Matrix4x4 R2 = new Matrix4x4();
        Matrix4x4 Tstereo = new Matrix4x4();
        Vector3D pstereo = new Vector3D();
        double factor_distancia_entre_ojos = 0.04;

        R1 = getRotation();
        R1.invert();

        T1.translation(-eyePosition.x, -eyePosition.y, -eyePosition.z);
        R_adic2.axisRotation(Math.toRadians(90), 0, 0, 1);
        R_adic1.axisRotation(Math.toRadians(-90), 1, 0, 0);

        R = R_adic1.multiply(R_adic2.multiply(R1.multiply(T1)));

        if ( stereoMode == STEREO_MODE_LEFT_EYE ) {
            pstereo.x = -R.M[0][0] * factor_distancia_entre_ojos;
            pstereo.y = -R.M[0][1] * factor_distancia_entre_ojos;
            pstereo.z = -R.M[0][2] * factor_distancia_entre_ojos;
            Tstereo.translation(pstereo.x, pstereo.y, pstereo.z);
            R.multiply(Tstereo);
        }
        if ( stereoMode == STEREO_MODE_RIGHT_EYE ) {
            pstereo.x = R.M[0][0] * factor_distancia_entre_ojos;
            pstereo.y = R.M[0][1] * factor_distancia_entre_ojos;
            pstereo.z = R.M[0][2] * factor_distancia_entre_ojos;
            Tstereo.translation(pstereo.x, pstereo.y, pstereo.z);
            R.multiply(Tstereo);
        }
        return R;
    }

    /**
    Note that projectionMatrix = transformationMatrix*viewVolumeMatrix
    */
    public Matrix4x4 calculateProjectionMatrix(int stereoMode)
    {
        Matrix4x4 P = calculateViewVolumeMatrix();
        Matrix4x4 R = calculateTransformationMatrix(stereoMode);
        return P.multiply(R);
    }

    /**
    Provides an object to text report convertion, optimized for human
    readability and debugging. Do not use for serialization or persistence
    purposes.
    */
    public String toString()
    {
        //------------------------------------------------------------
        String msg;

        msg = "<Camera>:\n";
        msg + = "  - Name: \"" + getName() + "\"\n";
        if ( projectionMode == PROJECTION_MODE_PERSPECTIVE ) {
            msg = msg + "  - Camera in PERSPECTIVE projection mode\n";
          }
          else if ( projectionMode == PROJECTION_MODE_ORTHOGONAL ) {
            msg = msg + "  - Camera in PARALEL projection mode\n";
            msg = msg + "  - Orthogonal zoom = " + orthogonalZoom + "\n";
          }
          else {
            msg = msg + "  - UNKNOWN Camera projection mode!\n";
          }
        ;

        msg = msg + "  - eyePosition(x, y, z) = " + eyePosition + "\n";
        msg = msg + "  - focusedPointPosition(x, y, z) = " + eyePosition.add(front.multiply(focalDistance)) + "\n";

        //------------------------------------------------------------
        Matrix4x4 R, TP;
        double yaw, pitch, roll;

        TP = calculateProjectionMatrix(STEREO_MODE_CENTER);
        R = getRotation();
        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();

        msg = msg + "  - Rotation yaw/pitch/roll: <" +
            VSDK.formatDouble(yaw) + ", " +
            VSDK.formatDouble(pitch) + ", " +
            VSDK.formatDouble(roll) + "> RAD (<" +
            VSDK.formatDouble(Math.toDegrees(yaw)) + ", " +
            VSDK.formatDouble(Math.toDegrees(pitch)) + ", " +
            VSDK.formatDouble(Math.toDegrees(roll)) + "> DEG)\n";

        //------------------------------------------------------------
        updateVectors();
        msg = msg + "  - Reference frame:\n";
        msg = msg + "    . Vector UP = " + up + " (longitud " + VSDK.formatDouble(up.length()) + ")\n";
        msg = msg + "    . Vector FRONT = " + front + " (longitud " + VSDK.formatDouble(front.length()) + ")\n";
        msg = msg + "    . Vector LEFT = " + left + " (longitud " + VSDK.formatDouble(front.length()) + ")\n";
        msg = msg + "  - Reference frame with scales:\n";
        msg = msg + "    . Vector UP' = " + upWithScale + " (longitud " + VSDK.formatDouble(upWithScale.length()) + ")\n";
        msg = msg + "    . Vector FRONT' = " + _dir + " (longitud " + VSDK.formatDouble(_dir.length()) + ")\n";
        msg = msg + "    . Vector RIGHT' = " + rightWithScale + " (longitud " + VSDK.formatDouble(rightWithScale.length()) + ")\n";
        msg = msg + "  - fov = " + VSDK.formatDouble(fov) + "\n";
        msg = msg + "  - nearPlaneDistance = " + VSDK.formatDouble(nearPlaneDistance) + "\n";
        msg = msg + "  - farPlaneDistance = " + VSDK.formatDouble(farPlaneDistance) + "\n";
        msg = msg + "  - Viewport size in pixels = (" + VSDK.formatDouble(viewport_xsize) + ", " + VSDK.formatDouble(viewport_ysize) + ")\n";
        msg = msg + "  - Transformation * projection matrix:" + TP;

        //------------------------------------------------------------
        Matrix4x4 P;
        double leftDistance, rightDistance, upDistance, downDistance, aspect;

        P = new Matrix4x4();
        aspect = viewport_xsize / viewport_ysize; 
        if ( projectionMode == PROJECTION_MODE_PERSPECTIVE ) {
            upDistance = nearPlaneDistance * Math.tan(Math.toRadians(fov/2));
            downDistance = -upDistance;
            leftDistance = aspect * downDistance;
            rightDistance = aspect * upDistance;
            P.frustumProjection(leftDistance, rightDistance,
                                downDistance, upDistance,
                                nearPlaneDistance, farPlaneDistance);
            msg = msg + "  - Projection matrix:" + P;
          }
          else if ( projectionMode == PROJECTION_MODE_ORTHOGONAL ) {
            P.orthogonalProjection(-aspect/orthogonalZoom, 
                                    aspect/orthogonalZoom,
                                   -1/orthogonalZoom, 1/orthogonalZoom,
                                   nearPlaneDistance, farPlaneDistance);
            msg = msg + "  - Projection matrix:" + P;
        }

        //------------------------------------------------------------
        return msg;
    }

    /**
    Given `this` camera and the pixel (x, y) in its viewport, this method
    calculates an infinite plane that pass by the corresponding proyector
    ray origin and by the proyection plane (u, v) point, where (u, v) is
    the proyection of pixel (x, y). The plane is perpendicular to the v
    direction.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    */
    public InfinitePlane calculateUPlaneAtPixel(int x, int y)
    {
        // 1. Calculate the angle between the front vector and the plane
        updateVectors();
        double u = ((double)x - viewport_xsize/2.0) / viewport_xsize;
        return calculateUPlane(u);
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    */
    public InfinitePlane calculateUPlane(double u)
    {
        Vector3D du = rightWithScale.multiply(u);
        Vector3D f = new Vector3D(front);
        Vector3D dir = du.add(_dir);

        f.normalize();

        double alpha;

        dir.normalize();

        alpha = Math.acos(f.dotProduct(dir));
        if ( u > 0 ) alpha *= -1;

        // 2. Calculate the plane normal
        Matrix4x4 R = new Matrix4x4();
        Vector3D n;

        R.axisRotation(alpha, up);
        n = R.multiply(du);
        n.normalize();

        // 3. Build the plane and return
        InfinitePlane plane;

        plane = new InfinitePlane(n, eyePosition);

        return plane;
    }

    /**
    Given `this` camera and the pixel (x, y) in its viewport, this method
    calculates an infinite plane that pass by the corresponding proyector
    ray origin and by the proyection plane (u, v) point, where (u, v) is
    the proyection of pixel (x, y). The plane is perpendicular to the v
    direction.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    */
    public InfinitePlane calculateVPlaneAtPixel(int x, int y)
    {
        // 1. Calculate the angle between the front vector and the plane
        updateVectors();
        double v = ((viewport_ysize - (double)y - 1) -  viewport_ysize/2.0) / viewport_ysize;
        return calculateVPlane(v);
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    */
    public InfinitePlane calculateVPlane(double v)
    {
        Vector3D dv = upWithScale.multiply(v);
        Vector3D f = new Vector3D(front);
        Vector3D dir = dv.add(_dir);

        f.normalize();

        double alpha;

        dir.normalize();

        alpha = Math.acos(f.dotProduct(dir));
        if ( v > 0 ) alpha *= -1;

        // 2. Calculate the plane normal
        Matrix4x4 R = new Matrix4x4();
        Vector3D n;

        R.axisRotation(alpha, left);
        n = R.multiply(dv);
        n.normalize();

        // 3. Build the plane and return
        InfinitePlane plane;

        plane = new InfinitePlane(n, eyePosition);

        return plane;
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    */
    public InfinitePlane calculateNearPlane()
    {
        InfinitePlane plane;

        Vector3D f = new Vector3D(front);
        f.normalize();
        Vector3D back = f.multiply(-1);
        f = f.multiply(nearPlaneDistance);
        Vector3D c = eyePosition.add(f);

        plane = new InfinitePlane(back, c);

        return plane;
    }

    /**
    PRE: updateVectors() must be called before this method if camera model
    is new or recently changed.

    WARNING: This is currently considering only the perspective case!
    TODO: The paralel projection case!
    */
    public InfinitePlane calculateFarPlane()
    {
        InfinitePlane plane;

        Vector3D f = new Vector3D(front);
        f.normalize();
        f = f.multiply(farPlaneDistance);
        Vector3D c = eyePosition.add(f);
        plane = new InfinitePlane(front, c);

        return plane;
    }

    /**
    Given a point in "clipping coordinates space", this method calculates
    a six bit opcode, as explained in [FOLE1992].6.5.3, suitable for use in the
    Cohen-Suterland line clipping algorithm, taking the input point in
    homogeneous space, as noted in [FOLE1992].6.5.4.

    Note that the "clipping coodinate space" is the result of transforming
    world coordinate space with the composed transform-project matrix for
    current camera, as returned by the `calculateProjectionMatrix` method.

    Note that in VSDK, the clipping space correspond to the frustum for
    the minmax cube from <-1, -1, -1> to <1, 1, 1>.

    WARNING: This algoritm FAILS when the point to be tested is in the
    plane passing through eye position of the camera and paralel to near 
    plane! In this case, W gets 0 value, and points are not correctly
    classified.
    @todo: check this method... currently disabled due to non working cases!
    */
/*
    private int calculateOutcodeBits(Vector4D p)
    {
        int bits = 0x00;

        if ( p.w > 0 ) {
            if ( p.x >  p.w ) bits |= OPCODE_RIGHT;
            if ( p.x < -p.w ) bits |= OPCODE_LEFT;
            if ( p.y >  p.w ) bits |= OPCODE_UP;
            if ( p.y < -p.w ) bits |= OPCODE_DOWN;
            if ( p.z >  p.w ) bits |= OPCODE_FAR;
            if ( p.z < -p.w ) bits |= OPCODE_NEAR;
        }
        else {
            if ( p.x > -p.w ) bits |= OPCODE_RIGHT;
            if ( p.x <  p.w ) bits |= OPCODE_LEFT;
            if ( p.y > -p.w ) bits |= OPCODE_UP;
            if ( p.y <  p.w ) bits |= OPCODE_DOWN;
            if ( p.z > -p.w ) bits |= OPCODE_FAR;
            if ( p.z <  p.w ) bits |= OPCODE_NEAR;
        }
        return bits;
    }
*/

    /**
    Given a point in world space, this method calculates a six bit opcode,
    as explained in [FOLE1992].6.5.3, suitable for use in the Cohen-Suterland
    line clipping algorithm. The camera view volume should be represented
    by its six bounding planes.
    */
    private int calculateOutcodeBits(Vector3D p,
                                     InfinitePlane right, InfinitePlane left,
                                     InfinitePlane up, InfinitePlane down,
                                     InfinitePlane far, InfinitePlane near)
    {
        int bits = 0x00;

        if ( right.doContainmentTestHalfSpace(p, VSDK.EPSILON) ==
             Geometry.OUTSIDE ) {
            bits |= OPCODE_RIGHT;
        }
        if ( left.doContainmentTestHalfSpace(p, VSDK.EPSILON) ==
             Geometry.OUTSIDE ) {
            bits |= OPCODE_LEFT;
        }
        if ( up.doContainmentTestHalfSpace(p, VSDK.EPSILON) ==
             Geometry.OUTSIDE) {
            bits |= OPCODE_UP;
        }
        if ( down.doContainmentTestHalfSpace(p, VSDK.EPSILON) ==
             Geometry.OUTSIDE ) {
            bits |= OPCODE_DOWN;
        }
        if ( far.doContainmentTestHalfSpace(p, VSDK.EPSILON) ==
             Geometry.OUTSIDE ) {
            bits |= OPCODE_FAR;
        }
        if ( near.doContainmentTestHalfSpace(p, VSDK.EPSILON) ==
             Geometry.OUTSIDE ) {
            bits |= OPCODE_NEAR;
        }

        return bits;
    }

    /**
    This method implements the Cohen-Sutherland line clipping algorithm with
    respect to the view volume defined by current camera. Recieves the two
    line endpoints and return true if any part of this line lies inside the
    view volume.  In the case the line crosses the view volume, the new
    resulting endpoints are calculated and returned.

    This algorithm structure follows the one proposed in [FOLE1992].3.12.3,
    generalizing it to the 3D case, as noted in [FOLE1992].6.5.3.
    */
    public boolean clipLineCohenSutherland(Vector3D point0, Vector3D point1,
                             Vector3D clippedPoint0, Vector3D clippedPoint1)
    {
        //- Local variables definition ------------------------------------
        int outcode0;                // 6bit containment code for point0
        int outcode1;                // 6bit containment code for point1
        int outcodeout;              // Selected endpoint code for iteration
        Vector3D clippingMidPoint;   // Selected endpoint clipped for iteration
        Ray testRay;                 // Ray use for general line/plane clipping
        Vector3D dirFromP0ToP1;      // Temporary for testRay construction
        InfinitePlane rightPlane;    // 6 planes defining current camera
        InfinitePlane leftPlane;     //   view volume. Note that intersection
        InfinitePlane upPlane;       //   tests are done against these planes
        InfinitePlane downPlane;     //   using general case non-optimal
        InfinitePlane nearPlane;     //   intersections! This sould be
        InfinitePlane farPlane;      //   optimized
        InfinitePlane clippingPlane; // Selected plane for each iteration

        //- Algorithm initial state ---------------------------------------
        clippedPoint0.x = point0.x;
        clippedPoint0.y = point0.y;
        clippedPoint0.z = point0.z;
        clippedPoint1.x = point1.x;
        clippedPoint1.y = point1.y;
        clippedPoint1.z = point1.z;
        updateVectors();
        clippingMidPoint = new Vector3D();
        rightPlane = calculateUPlane(0.5);
        leftPlane = calculateUPlane(-0.5);
        upPlane = calculateVPlane(0.5);
        downPlane = calculateVPlane(-0.5);
        nearPlane = calculateNearPlane();
        farPlane = calculateFarPlane();
        clippingPlane = null;
        outcode0 = calculateOutcodeBits(point0, rightPlane, leftPlane, 
                                      upPlane, downPlane, nearPlane, farPlane);
        outcode1 = calculateOutcodeBits(point1, rightPlane, leftPlane,
                                      upPlane, downPlane, nearPlane, farPlane);
        dirFromP0ToP1 = point1.substract(point0);
        dirFromP0ToP1.normalize();

        //- Main Cohen-Sutherland iteration cycle (incremental clipping) --
        boolean linePasses = false; // Algorithm return value
        boolean done = false;       // Iteration exit condition
        do {
            //- Trivial cases: trivial accept and trivial reject ----------
            if ( outcode0 == 0x0 && outcode1 == 0x0 ) {
                linePasses = true;
                done = true;
            }
            else if ( (outcode0 & outcode1) != 0x0 ) {
                linePasses = false;
                done = true;
            }
            //- Iterative cases: clipping with each of the 6 planes -------
            else {
                if ( dirFromP0ToP1.length() < VSDK.EPSILON ) {
                    // continue;
                    return false;
                }
                //--------------------------------------------------
                if ( outcode0 != 0 ) {
                    outcodeout = outcode0;
                  }
                  else {
                    outcodeout = outcode1;
                }
                testRay = new Ray(point0, dirFromP0ToP1);

                //--------------------------------------------------
                clippingPlane = null;
                if ( (OPCODE_UP & outcodeout) != 0x0 ) {
                    clippingPlane = upPlane;
                }
                else if ( (OPCODE_DOWN & outcodeout) != 0x0 ) {
                    clippingPlane = downPlane;
                }
                else if ( (OPCODE_LEFT & outcodeout) != 0x0 ) {
                    clippingPlane = leftPlane;
                }
                else if ( (OPCODE_RIGHT & outcodeout) != 0x0 ) {
                    clippingPlane = rightPlane;
                }
                else if ( (OPCODE_NEAR & outcodeout) != 0x0 ) {
                    // Warning: Why test with the contrary plane?
                    clippingPlane = farPlane;
                }
                else if ( (OPCODE_FAR & outcodeout) != 0x0 ) {
                    // Warning: Why test with the contrary plane?
                    clippingPlane = nearPlane;
                }
                else {
                    // Not possible: non implemented case!
                        VSDK.reportMessage(this, VSDK.WARNING, 
                            "clipLineCohenSutherland", 
                            "Unusal ray case, check code and data");
                }

                if ( clippingPlane != null ) {
                    if ( !clippingPlane.doIntersection(testRay) ) {
                        VSDK.reportMessage(this, VSDK.WARNING, 
                            "clipLineCohenSutherland", 
                            "Unusal ray assembly, check code and data");
                    }
                    clippingMidPoint = testRay.origin.add(
                        testRay.direction.multiply(testRay.t));
                    linePasses = true;
                }

                //--------------------------------------------------
                Vector3D p = new Vector3D();
                if ( outcodeout == outcode0 ) {
                    clippedPoint0.x = clippingMidPoint.x;
                    clippedPoint0.y = clippingMidPoint.y;
                    clippedPoint0.z = clippingMidPoint.z;
                    outcode0 = calculateOutcodeBits(clippedPoint0,
                        rightPlane, leftPlane, upPlane, 
                        downPlane, nearPlane, farPlane);
                  }
                  else {
                    clippedPoint1.x = clippingMidPoint.x;
                    clippedPoint1.y = clippingMidPoint.y;
                    clippedPoint1.z = clippingMidPoint.z;
                    outcode1 = calculateOutcodeBits(clippedPoint1,
                        rightPlane, leftPlane, upPlane, 
                        downPlane, nearPlane, farPlane);

                }
            }
        } while ( !done );

        return linePasses;
    }

    /**
    Given a point in world coordinates, this method calculates a pixel
    coordinates in viewport space (in the form <u, v, 0>). Returns true
    if the pixel lies inside the viewport, false otherwise.

    TODO: Current implementation is suspicious. It should use a simple
    multiplication of point with projection matrix... but this idea is not
    working.
    */
    public boolean projectPoint(Vector3D inPoint, Vector3D outProjected)
    {
        // 1. Calculate vectors
        updateVectors();
        Vector3D upCopy = new Vector3D(up);
        Vector3D rightCopy = new Vector3D(left.multiply(-1));
        double fovFactor = viewport_xsize/viewport_ysize;
        double scaleFactor = 1.0/Math.tan(Math.toRadians(fov/2));

        upCopy.normalize();
        upCopy = upCopy.multiply(scaleFactor);
        rightCopy.normalize();
        rightCopy = rightCopy.multiply(scaleFactor).multiply(1/fovFactor);

        // 2. Calculate projector
        Vector3D p = getPosition();
        Ray r = new Ray(p, inPoint.substract(p));
        if ( r.direction.length() < VSDK.EPSILON ) return false;

        // 3. Calculate projection plane
        Vector3D center = new Vector3D(front);
        center.normalize();
        center = center.add(p);
        InfinitePlane plane = new InfinitePlane(front.multiply(-1), center);

        if ( !plane.doIntersection(r) ) {
            return false;
        }

        // 4. Calculate projected global coordinates XYZ
        Vector3D projected = r.origin.add(r.direction.multiply(r.t)).substract(center);

        // 5. Scale point to viewport
        outProjected.x = (projected.dotProduct(rightCopy)/2+0.5)*viewport_xsize;
        outProjected.y = (1-(projected.dotProduct(upCopy)/2+0.5))*viewport_ysize;
        outProjected.z = 0.0;

        return true;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
