//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 8 2005 - Oscar Chavarro: Original base version                 =
//===========================================================================

package vitral.toolkits.environment;

import vitral.toolkits.common.Vector3D;
import vitral.toolkits.common.Matrix4x4;
import net.java.games.jogl.GL;

public class Camera {
    // Modelo basico de la camara
    private Vector3D eyePosition;
    private Vector3D focusedPosition;
    private Vector3D up;
    private int projectionMode;
    private double fov;
    private double orthogonalZoom;
    private double nearPlaneDistance;
    private double farPlaneDistance;

    // Constantes globales
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
    private Vector3D dx, dy, _dir;

    public Camera() {
        eyePosition = new Vector3D(0,-5,1);
        focusedPosition = new Vector3D(0,0,1);
        up = new Vector3D(0,0,1);
        fov = 60;
        viewport_xsize = 320;
        viewport_ysize = 320;

        projectionMode = PROJECTION_MODE_PERSPECTIVE;
        orthogonalZoom = 1;
        nearPlaneDistance = 0.05;
        farPlaneDistance = 100;

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
        return focusedPosition;
    }

    public void setFocusedPosition(Vector3D focusedPosition)
    {
        this.focusedPosition = focusedPosition;
    }

    public Vector3D getUp()
    {
        return up;
    }

    public void setUp(Vector3D up)
    {
        this.up = up;
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
    }

    /**
    PRE:
      - focusedPosition y eyePosition tienen que ser diferentes!
    POST:
      - dx queda normalizado
      - dy queda normalizado
      - _dir ... no se ha entendido bien!
    */
    public void updateVectors()
    {
        // Compute mapping from screen coordinate to a ray direction
        Vector3D _dir_no_normalizado = new Vector3D(focusedPosition.x - eyePosition.x, 
                                                focusedPosition.y - eyePosition.y, 
                                                focusedPosition.z - eyePosition.z);
        double fl = (double)(viewport_xsize / (2*Math.tan((0.5*fov)*Math.PI/180)));

        dx = _dir_no_normalizado.crossProduct(up);
        dx.normalize();

        dy = _dir_no_normalizado.crossProduct(dx);
        dy.normalize();

        _dir = _dir_no_normalizado;
        _dir.normalize();
        _dir.x = _dir.x * fl - 0.5f * (viewport_xsize*dx.x + 
                                       viewport_ysize*dy.x);
        _dir.y = _dir.y * fl - 0.5f * (viewport_xsize*dx.y + 
                                       viewport_ysize*dy.y);
        _dir.z = _dir.z * fl - 0.5f * (viewport_xsize*dx.z + 
                                       viewport_ysize*dy.z);
    }

    public final Ray generateRay(int x, int y)
    {
        // OJO: Es posible que esto este siendo lento por asignar a memoria
        //      dinamica estas dos variables. Notese que podrian ser estaticos
        //      y reutilizarse...
        // Notese como se utiliza un vector preprocesado `dy` para calcular
        // las coordenadas en la pantalla que aumentan hacia abajo
        Vector3D dir = new Vector3D(
            x*dx.x + y*dy.x + _dir.x,
            x*dx.y + y*dy.y + _dir.y,
            x*dx.z + y*dy.z + _dir.z);

        // La direccion de este rayo es un vector unitario, dado que en la
        // version actual de Ray::Ray se normaliza...
        Ray ray = new Ray(eyePosition, dir);

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
        Vector3D front;
        double focalDistance;

        front = focusedPosition.substract(eyePosition);
        focalDistance = front.length();

        up.x = R.M[0][2];
        up.y = R.M[1][2];
        up.z = R.M[2][2];

        front.x = R.M[0][0];
        front.y = R.M[1][0];
        front.z = R.M[2][0];

    front.normalize();
    front = front.multiply(focalDistance);
    focusedPosition = eyePosition.add(front);
    }

    public Matrix4x4 getRotation()
    {
        //------------------------------------------------------------
        Vector3D front;
        Vector3D left;

        up.normalize();

        front = focusedPosition.substract(eyePosition);
        front.normalize();

        left = up.crossProduct(front);
        left.normalize();

        //------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        R.identity();
        R.M[0][0] = front.x; R.M[0][1] = left.x; R.M[0][2] = up.x;
        R.M[1][0] = front.y; R.M[1][1] = left.y; R.M[1][2] = up.y;
        R.M[2][0] = front.z; R.M[2][1] = left.z; R.M[2][2] = up.z;

        return R;
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

//===========================================================================
//= Services for OpenGL (JOGL)                                              =
//===========================================================================

    /**
    stereoMode must have one of the STEREO_MODE values
    */
    public void activateGL(GL gl, int stereoMode)
    {
        Matrix4x4 R;

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        R = calculateProjectionMatrix(stereoMode);
        R.activateGL(gl);
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }

    public void activateGL(GL gl)
    {
        activateGL(gl, STEREO_MODE_CENTER);
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
        msg = msg + "  - puntoFocal(x, y, z) = " + focusedPosition + "\n";

        //------------------------------------------------------------
        Matrix4x4 R;
        double yaw, pitch, roll;

        R = calculateProjectionMatrix(STEREO_MODE_CENTER);
        yaw = R.obtainEulerYawAngle();
        pitch = R.obtainEulerPitchAngle();
        roll = R.obtainEulerRollAngle();

        //------------------------------------------------------------
        updateVectors();
        msg = msg + "    . Vector UP = " + up + "\n";
        msg = msg + "  - fov = " + fov + "\n";
        msg = msg + "  - nearPlaneDistance = " + nearPlaneDistance + "\n";
        msg = msg + "  - farPlaneDistance = " + farPlaneDistance + "\n";

        return msg;
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
