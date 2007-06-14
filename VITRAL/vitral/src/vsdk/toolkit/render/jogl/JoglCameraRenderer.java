//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 22 2005 - David Diaz: Original base version                    =
//= - November 15 2005 - Oscar Chavarro: Migrated to JOGL Beta Version      =
//= - November 28 2005 - Oscar Chavarro: Added activateCenter method        =
//= - February 27 2006 - Oscar Chavarro: Added drawGL method                =
//===========================================================================

package vsdk.toolkit.render.jogl;

import javax.media.opengl.GL;

import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.environment.Camera;

public class JoglCameraRenderer
{
    /**
    stereoMode must have one of the STEREO_MODE values
    */
    public static void activate(GL gl, int stereoMode, Camera cam)
    {
        Matrix4x4 R;

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        R = cam.calculateProjectionMatrix(stereoMode);
        JoglMatrixRenderer.activate(gl, R);
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }

    public static void activate(GL gl, Camera cam)
    {
        activate(gl, Camera.STEREO_MODE_CENTER, cam);
    }

    public static void activateCenter(GL gl, Camera cam)
    {
        Matrix4x4 R;
        Camera camera2 = new Camera(cam);
        Vector3D eye, center, neweye, newcenter;

        eye = camera2.getPosition();
        center = camera2.getFocusedPosition();
        neweye = new Vector3D(0, 0, 0);
        newcenter = center.substract(eye);
        camera2.setPosition(neweye);
        camera2.setFocusedPositionDirect(newcenter);
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        R = camera2.calculateProjectionMatrix(Camera.STEREO_MODE_CENTER);
        JoglMatrixRenderer.activate(gl, R);
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }

    private static void drawBase(GL gl)
    {
        gl.glLineWidth((float)1.0);
        gl.glBegin(GL.GL_LINES);
            gl.glColor3d(1, 0, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(1, 0, 0);

            gl.glColor3d(0, 1, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 1, 0);

            gl.glColor3d(0, 0, 1);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 1);
        gl.glEnd();
    }

    public static void drawVolume(GL gl, Camera cam)
    {
        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_LINE);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glShadeModel(gl.GL_FLAT);


        //- Calcule los extremos del volumen de visualizacion ---------------
        double xn = 1, yn = 1;
        double xf = 1, yf = 1;
        double npd; // Near plane distance
        double fpd; // Far plane distance

        npd = cam.getNearPlaneDistance();
        fpd = cam.getFarPlaneDistance();

        if ( cam.getProjectionMode() != cam.PROJECTION_MODE_ORTHOGONAL ) {
            yn = 2 * npd * Math.tan(Math.toRadians(cam.getFov()) / 2);
            xn = yn * (cam.getViewportXSize()/(double)cam.getViewportYSize());
        }
        else {
            if ( cam.getViewportXSize() > cam.getViewportYSize() ) {
                xn = 1;
                yn = cam.getViewportYSize() / cam.getViewportXSize();
              }
              else {
                xn = cam.getViewportXSize() / cam.getViewportYSize();
                yn = 1;
            }
        }

        xf = xn;
        yf = yn;
        if ( cam.getProjectionMode() != cam.PROJECTION_MODE_ORTHOGONAL ) {
            yf = 2 * fpd * Math.tan(Math.toRadians(cam.getFov()) / 2);
            xf = yf * (cam.getViewportXSize()/cam.getViewportYSize());
        }

        //- Genere las primitivas que muestran el volumen de visualizacion --
        gl.glColor3d(1, 0.5, 0.5);   // Origen de la camara
        gl.glBegin(gl.GL_POINTS);
            gl.glVertex3d(0, 0, 0);
        gl.glEnd();

        gl.glDisable(gl.GL_LIGHTING);
        gl.glColor3f(0, 1, 1);       // Plano de proyeccion
        gl.glBegin(gl.GL_LINE_LOOP);
            gl.glVertex3d(npd, -xn/2, -yn/2);
            gl.glVertex3d(npd, xn/2, -yn/2);
            gl.glVertex3d(npd, xn/2, yn/2);
            gl.glVertex3d(npd, -xn/2, yn/2);
        gl.glEnd();
        gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(npd, -xn/10, -yn/10);
            gl.glVertex3d(npd, xn/10, yn/10);
            gl.glVertex3d(npd, xn/10, -yn/10);
            gl.glVertex3d(npd, -xn/10, yn/10);
            gl.glVertex3d(npd, -xn/10, yn/2);
            gl.glVertex3d(npd, 0, yn/2+yn/10);
            gl.glVertex3d(npd, 0, yn/2+yn/10);
            gl.glVertex3d(npd, xn/10, yn/2);
        gl.glEnd();

        gl.glColor3f(0, 1, 1);       // Plano lejano
        gl.glBegin(gl.GL_LINE_LOOP);
            gl.glVertex3d(fpd, -xf/2, -yf/2);
            gl.glVertex3d(fpd, xf/2, -yf/2);
            gl.glVertex3d(fpd, xf/2, yf/2);
            gl.glVertex3d(fpd, -xf/2, yf/2);
        gl.glEnd();
        gl.glBegin(gl.GL_LINES);
            gl.glVertex3d(fpd, -xf/10, -yf/10);
            gl.glVertex3d(fpd, xf/10, yf/10);
            gl.glVertex3d(fpd, xf/10, -yf/10);
            gl.glVertex3d(fpd, -xf/10, yf/10);
            gl.glVertex3d(fpd, -xf/10, yf/2);
            gl.glVertex3d(fpd, 0, yf/2+yf/10);
            gl.glVertex3d(fpd, 0, yf/2+yf/10);
            gl.glVertex3d(fpd, xf/10, yf/2);
        gl.glEnd();

        gl.glBegin(gl.GL_LINES);  // Lineas de los bordes
            gl.glVertex3d(npd, -xn/2, -yn/2);
            gl.glVertex3d(fpd, -xf/2, -yf/2);
            gl.glVertex3d(npd, xn/2, -yn/2);
            gl.glVertex3d(fpd, xf/2, -yf/2);
            gl.glVertex3d(npd, xn/2, yn/2);
            gl.glVertex3d(fpd, xf/2, yf/2);
            gl.glVertex3d(npd, -xn/2, yn/2);
            gl.glVertex3d(fpd, -xf/2, yf/2);
        gl.glEnd();

    }

    public static void draw(GL gl, Camera cam)
    {
        gl.glPushMatrix();
        //Matrix4x4 R = cam.calculateProjectionMatrix(cam.STEREO_MODE_CENTER);

        Matrix4x4 R = cam.getRotation();
        Vector3D p = cam.getPosition();
        R.M[0][3] = p.x;
        R.M[1][3] = p.y;
        R.M[2][3] = p.z;

        JoglMatrixRenderer.activate(gl, R);
        //drawBase(gl);
        drawVolume(gl, cam);
        gl.glPopMatrix();
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
