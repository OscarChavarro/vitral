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
//===========================================================================

package vitral.toolkits.environment;

import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.Matrix4x4;

public class Camera {
    // Basic Camera Model
    private Vector3D eyePosition;
    private Vector3D up;
    private Vector3D front;
    private Vector3D left;
    private double focalDistance;
    private int projectionMode;
    private double fov;
    private double orthogonalZoom;
    private double nearPlaneDistance;
    private double farPlaneDistance;

    // Global constants
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
    private Vector3D dx, dy, _dir, upWithScale, leftWithScale;
    
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

        focalDistance=10;
        // OJO: dx, dy y _dir no estan inicializados!
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

        /*
        // MIT raytracer code version
        double fl = (double)(viewport_xsize / (2*Math.tan((0.5*fov)*Math.PI/180)));
        _dir = new Vector3D();
        _dir.x = front.x * fl + 0.5f * (viewport_xsize*left.x + viewport_ysize*up.x);
        _dir.y = front.y * fl + 0.5f * (viewport_xsize*left.y + viewport_ysize*up.y);
        _dir.z = front.z * fl + 0.5f * (viewport_xsize*left.z + viewport_ysize*up.z);
        // End of MIT raytracer code version
        */

        // AQUYNZA code version
        front.normalize();
        double fovFactor = viewport_xsize/viewport_ysize;
        _dir = front.multiply(0.5);
        upWithScale = up.multiply(Math.tan(Math.toRadians(fov/2)));
        leftWithScale = left.multiply(-fovFactor*Math.tan(Math.toRadians(fov/2)));
        // End of AQUYNZA code version
    }

    /**
    Given a 2D integer coordinate in viewport space, this method calculates a proyector ray
    that emanates from the eye position and passes over the (u, v) float coordinate in the
    projection plane. Note that the (u, v) coordinate correspond to the (x, y) coordinate.

    This method is of vital importance to many fundamental algorithms of visualization
    (i.e. ray casting, ray tracing, radiosity), object selection and others (simulation,
    colition detection, visual debugging). As it is important to improve the efficiency of
    this method, some precalculated values are stored in the class attributes `_dir`,
    `upWithScale` and `leftWithScale`, which values are stored in the `updateVectors`
    method, leading to the precondition:

    PRE:
      - At least a call to the updateVectors method must be done before calling this method,
        and after changing any camera parameter the updateVectors method must be called again
        to reflect the changes in this calculation.
    */
    public final Ray generateRay(int x, int y)
    {
        /*
        // MIT raytracer code version
        Vector3D dir = new Vector3D(
            -x*left.x - y*up.x + _dir.x,
            -x*left.y - y*up.y + _dir.y,
            -x*left.z - y*up.z + _dir.z
        );
        // End of MIT raytracer code version
        */

        // AQUYNZA code version
        double u, v;
        double mi_x, mi_y;

        // 1. Convert integer image coordinates into values in the range [-0.5, 0.5]
        u = ((double)x - viewport_xsize/2.0) / viewport_xsize;
        v = ((viewport_ysize - (double)y - 1) -  viewport_ysize/2.0) / viewport_ysize;

        // 2. Calculate the ray direction
        Vector3D dx = upWithScale.multiply(v);
        Vector3D dy = leftWithScale.multiply(u);
        Vector3D dir = dx.add(dy).add(_dir);
        // End of AQUYNZA code version

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
    
    /*
     *This method returns a matrix that can be applied to the camera to rotate it on their local coordinates
     *
     *@param dx double Rotation relative to the x axis on camera coordinates
     *@param dy double Rotation relative to the y axis on camera coordinates
     *@param dz double Rotation relative to the z axis on camera coordinates
     *
     *@see setRotation
     *
     *@returns A matrix that represents the rotation of the input parameters
     */
    public Matrix4x4 getRotation(double dx, double dy, double dz)
    {
       //se hallan los valores de sen y cos; se divide entre 57.29 para convertir de grados a radianes
        double sino[]={Math.sin(dx/57.29577951), Math.sin(dy/57.29577951), Math.sin(dz/57.29577951)};
        double cosi[]={Math.cos(dx/57.29577951), Math.cos(dy/57.29577951), Math.cos(dz/57.29577951)};
        double cosiComp[]={1.0-cosi[0], 1.0-cosi[1], 1.0-cosi[2]};//este es el complemento del coseno
        
//      Esta es la matriz teorica de rotacion que gira n grados en el eje left de la camara (si)
//      double[] rotStraf=new Double[]={ (cosiComp[0]*left.x*left.x+cosi[0]),        (cosiComp[0]*left.x*left.y-sino[0]*left.z), (cosiComp[0]*left.x*left.z+sino[0]*left.y), 0,
//                                       (cosiComp[0]*left.x*left.y+sino[0]*left.z), (cosiComp[0]*left.y*left.y +cosi[0])      , (cosiComp[0]*left.y*left.z-sino[0]*left.x), 0,
//                                       (cosiComp[0]*left.x*left.z-sino[0]*left.z), (cosiComp[0]*left.y*left.z+sino[0]*left.x), (cosiComp[0]*left.z*left.z+cosi[0]),        0,
//                                                  0,                                                   0,                                             0,                   1};

//esta es la representacion de esa matriz para opengl 
//      double[] rotStraf=new Double[]{ (cosiComp[0]*left.x*left.x+cosi[0]),        (cosiComp[0]*left.x*left.y-sino[0]*left.z), (cosiComp[0]*left.x*left.z+sino[0]*left.y), 0,
//                                      (cosiComp[0]*left.x*left.y+sino[0]*left.z), (cosiComp[0]*left.y*left.y +cosi[0]),       (cosiComp[0]*left.y*left.z-sino[0]*left.x), 0,
//                                      (cosiComp[0]*left.x*left.z-sino[0]*left.y), (cosiComp[0]*left.y*left.z+sino[0]*left.x), (cosiComp[0]*left.z*left.z+cosi[0]),        0,
//                                                       0,                                            0,                                      0,                           1};

        // Esta es la matriz en la representacion del toolkit
        Matrix4x4 rStraf=new Matrix4x4();
        rStraf.M[0][0]=cosiComp[0]*left.x*left.x+cosi[0];           rStraf.M[0][1]=cosiComp[0]*left.x*left.y-sino[0]*left.z;    rStraf.M[0][2]=cosiComp[0]*left.x*left.z+sino[0]*left.y;    rStraf.M[0][3]=0;
        rStraf.M[1][0]=cosiComp[0]*left.x*left.y+sino[0]*left.z;    rStraf.M[1][1]=cosiComp[0]*left.y*left.y +cosi[0];          rStraf.M[1][2]=cosiComp[0]*left.y*left.z-sino[0]*left.x;    rStraf.M[1][3]=0;
        rStraf.M[2][0]=cosiComp[0]*left.x*left.z-sino[0]*left.y;    rStraf.M[2][1]=cosiComp[0]*left.y*left.z+sino[0]*left.x;    rStraf.M[2][2]=cosiComp[0]*left.z*left.z+cosi[0];           rStraf.M[2][3]=0;
        rStraf.M[3][0]=0;                                           rStraf.M[3][1]=0;                                           rStraf.M[3][2]=0;                                           rStraf.M[3][3]=1;
        

//      Esta es la matriz teorica de rotacion que gira n grados en el eje up de la camara (no)
//      double[] rotUp = new Double[]{  (cosiComp[1]*up.x*up.x+cosi[1]),      (cosiComp[1]*up.x*up.y-sino[1]*up.z), (cosiComp[1]*up.x*up.z+sino[1]*up.y), 0,
//                                      (cosiComp[1]*up.x*up.y+sino[1]*up.z), (cosiComp[1]*up.y*up.y +cosi[1])    , (cosiComp[1]*up.y*up.z-sino[1]*up.x), 0,
//                                      (cosiComp[1]*up.x*up.z-sino[1]*up.z), (cosiComp[1]*up.y*up.z+sino[1]*up.x), (cosiComp[1]*up.z*up.z+cosi[1]),      0,
//                                                     0,                                      0,                                    0,                   1};

//esta es la representacion de esa matriz para opengl
//      double[] rotUp = new Double[]{   (cosiComp[1]*up.x*up.x+cosi[1]),      (cosiComp[1]*up.x*up.y-sino[1]*up.z), (cosiComp[1]*up.x*up.z+sino[1]*up.y), 0,
//                                       (cosiComp[1]*up.x*up.y+sino[1]*up.z), (cosiComp[1]*up.y*up.y+cosi[1]),      (cosiComp[1]*up.y*up.z-sino[1]*up.x), 0,
//                                       (cosiComp[1]*up.x*up.z-sino[1]*up.y), (cosiComp[1]*up.y*up.z+sino[1]*up.x), (cosiComp[1]*up.z*up.z+cosi[1]),      0,
//                                                     0,                                      0,                                      0,                  1};

//Esta es la matriz en la representacion del toolkit
        Matrix4x4 rUp=new Matrix4x4();
        rUp.M[0][0]=cosiComp[1]*up.x*up.x+cosi[1];         rUp.M[0][1]=cosiComp[1]*up.x*up.y-sino[1]*up.z;    rUp.M[0][2]=cosiComp[1]*up.x*up.z+sino[1]*up.y;    rUp.M[0][3]=0;
        rUp.M[1][0]=cosiComp[1]*up.x*up.y+sino[1]*up.z;    rUp.M[1][1]=cosiComp[1]*up.y*up.y+cosi[1];         rUp.M[1][2]=cosiComp[1]*up.y*up.z-sino[1]*up.x;    rUp.M[1][3]=0;
        rUp.M[2][0]=cosiComp[1]*up.x*up.z-sino[1]*up.y;    rUp.M[2][1]=cosiComp[1]*up.y*up.z+sino[1]*up.x;    rUp.M[2][2]=cosiComp[1]*up.z*up.z+cosi[1];         rUp.M[2][3]=0;
        rUp.M[3][0]=0;                                     rUp.M[3][1]=0;                                     rUp.M[3][2]=0;                                     rUp.M[3][3]=1;

//      Esta es la matriz de rotacion que gira n grados en el eje front de la camara (no se)
//      double[] rotFront = new Double[]{ (cosiComp[2]*front.x*front.x+cosi[2]),         (cosiComp[2]*front.x*front.y-sino[2]*front.z), (cosiComp[2]*front.x*front.z+sino[2]*front.y), 0,
//                                        (cosiComp[2]*front.x*front.y+sino[2]*front.z), (cosiComp[2]*front.y*front.y +cosi[2])       , (cosiComp[2]*front.y*front.z-sino[2]*front.x), 0,
//                                        (cosiComp[2]*front.x*front.z-sino[2]*front.z), (cosiComp[2]*front.y*front.z+sino[2]*front.x), (cosiComp[2]*front.z*front.z+cosi[2]),         0,
//                                                               0                                               0,                                         0,                         1};

//esta es la representacion de esa matriz para opengl
//      double[] rotFront = new Double[]{  (cosiComp[2]*front.x*front.x+cosi[2]),         (cosiComp[2]*front.x*front.y-sino[2]*front.z), (cosiComp[2]*front.x*front.z+sino[2]*front.y), 0,
//                                         (cosiComp[2]*front.x*front.y+sino[2]*front.z), (cosiComp[2]*front.y*front.y+cosi[2]),         (cosiComp[2]*front.y*front.z-sino[2]*front.x), 0,
//                                         (cosiComp[2]*front.x*front.z-sino[2]*front.y), (cosiComp[2]*front.y*front.z+sino[2]*front.x), (cosiComp[2]*front.z*front.z+cosi[2]),         0,
//                                                               0,                                               0,                                         0,                         1};

        // esta es la representacion de esa matriz en opengl
        Matrix4x4 rFr=new Matrix4x4();

        rFr.M[0][0]=cosiComp[2]*front.x*front.x+cosi[2];            rFr.M[0][1]=cosiComp[2]*front.x*front.y-sino[2]*front.z;    rFr.M[0][2]=cosiComp[2]*front.x*front.z+sino[2]*front.y;    rFr.M[0][3]=0;
        rFr.M[1][0]=cosiComp[2]*front.x*front.y+sino[2]*front.z;    rFr.M[1][1]=cosiComp[2]*front.y*front.y+cosi[2];            rFr.M[1][2]=cosiComp[2]*front.y*front.z-sino[2]*front.x;    rFr.M[1][3]=0;
        rFr.M[2][0]=cosiComp[2]*front.x*front.z-sino[2]*front.y;    rFr.M[2][1]=cosiComp[2]*front.y*front.z+sino[2]*front.x;    rFr.M[2][2]=cosiComp[2]*front.z*front.z+cosi[2];            rFr.M[2][3]=0;
        rFr.M[3][0]=0;                                              rFr.M[3][1]=0;                                              rFr.M[3][2]=0;                                              rFr.M[3][3]=1;
        
        // Aca se halla la matriz de rotacion
        Matrix4x4 rotacion=new Matrix4x4();
        rotacion.identity();
        rotacion=rotacion.multiply(rStraf);
        rotacion=rotacion.multiply(rUp);
        rotacion=rotacion.multiply(rFr);

        // Aca se actualizan los vectores        
        Vector3D frontAux=rotacion.multiply(front);
        Vector3D leftAux=rotacion.multiply(left);
        Vector3D upAux=rotacion.multiply(up);
        frontAux.normalize();
        leftAux.normalize();
        upAux.normalize();

        // Aca se arma la nueva matriz de rotacion        
        Matrix4x4 retRot=new Matrix4x4();
        retRot.M[0][0]=frontAux.x;
        retRot.M[0][1]=leftAux.x;
        retRot.M[0][2]=upAux.x;
        retRot.M[0][3]=0;

        retRot.M[1][0]=frontAux.y;
        retRot.M[1][1]=leftAux.y;
        retRot.M[1][2]=upAux.y;
        retRot.M[1][3]=0;

        retRot.M[2][0]=frontAux.z;
        retRot.M[2][1]=leftAux.z;
        retRot.M[2][2]=upAux.z;
        retRot.M[2][3]=0;

        retRot.M[3][0]=0;
        retRot.M[3][1]=0;
        retRot.M[3][2]=0;
        retRot.M[3][3]=1;
        
        return retRot;
    }
    
    /*
     *This method translate the camera on its local coordinates
     *
     *@param dx double Translation relative to the x axis on camera coordinates
     *@param dy double Translation relative to the y axis on camera coordinates
     *@param dz double Translation relative to the z axis on camera coordinates
     *
     */
    public void translate(double dx, double dy, double dz)
    {
        Vector3D frontAux=front.multiply(dz);
        Vector3D upAux=up.multiply(dy);
        Vector3D leftAux=left.multiply(dx);
        
        eyePosition=eyePosition.add(frontAux);
        eyePosition=eyePosition.add(upAux);
        eyePosition=eyePosition.add(leftAux);
    }

    public Matrix4x4 calculateProjectionMatrix(int stereoMode)
    {
        //- 1. Calculate the base projection matrix -------------------------
        double leftDistance, rightDistance, upDistance, downDistance, aspect;
        Matrix4x4 P = new Matrix4x4();

        switch ( projectionMode ) {
          case Camera.PROJECTION_MODE_ORTHOGONAL:
            P.orthogonalProjection(-1/orthogonalZoom, 1/orthogonalZoom,
                                   -1/orthogonalZoom, 1/orthogonalZoom,
                                   nearPlaneDistance, farPlaneDistance);
            break;
          case Camera.PROJECTION_MODE_PERSPECTIVE:
            aspect = viewport_xsize / viewport_ysize; 
            upDistance = nearPlaneDistance * Math.tan(fov * Math.PI / 360);
            downDistance = -upDistance;
            leftDistance = aspect * downDistance;
            rightDistance = aspect * upDistance;
            P.frustumProjection(leftDistance, rightDistance,
                                downDistance, upDistance,
                                nearPlaneDistance, farPlaneDistance);
            break;
        }

        //- 2. Take into account the camera position and orientation -------
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

        return P.multiply(R);
    }

    public String toString()
    {
        //------------------------------------------------------------
        String msg;

        msg = "<Camera>:\n";

        if ( projectionMode == PROJECTION_MODE_PERSPECTIVE ) {
            msg = msg + "  - Camara en modo de proyeccion PERSPECTIVA\n";
          }
          else if ( projectionMode == PROJECTION_MODE_ORTHOGONAL ) {
            msg = msg + "  - Camara en modo de proyeccion PARALEqqqqqLA\n";
          }
          else {
            msg = msg + "  - Camara en modo de proyeccion DESCONOCIDO\n";
          }
        ;

        msg = msg + "  - posicion(x, y, z) = " + eyePosition + "\n";
        msg = msg + "  - puntoFocal(x, y, z) = " + eyePosition.add(front.multiply(focalDistance)) + "\n";

        //------------------------------------------------------------
        Matrix4x4 R;
        double yaw, pitch, roll;

        R = calculateProjectionMatrix(STEREO_MODE_CENTER);
        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();

        //------------------------------------------------------------
        updateVectors();
        msg = msg + "  - Marco de referencia:\n";
        msg = msg + "    . Vector UP = " + up + "( longitud " + up.length() + ")\n";
        msg = msg + "    . Vector FRONT = " + front + " (longitud " + front.length() + ")\n";
        msg = msg + "    . Vector LEFT = " + left + " (longitud " + front.length() + ")\n";
        msg = msg + "  - fov = " + fov + "\n";
        msg = msg + "  - nearPlaneDistance = " + nearPlaneDistance + "\n";
        msg = msg + "  - farPlaneDistance = " + farPlaneDistance + "\n";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
