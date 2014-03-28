//===========================================================================
//=-------------------------------------------------------------------------=
//= Module history:                                                         =
//= - August 22 2005 - David Diaz: Original base version                    =
//= - November 15 2005 - Oscar Chavarro: Migrated to JOGL Beta Version      =
//= - November 28 2005 - Oscar Chavarro: Added activateCenter method        =
//= - February 27 2006 - Oscar Chavarro: Added drawGL method                =
//===========================================================================

package vsdk.toolkit.render.jogl;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;

// VSDK classes
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.environment.Camera;

public class JoglCameraRenderer extends JoglRenderer
{
    public static void activate(GL2 gl, Camera cam)
    {
        Matrix4x4 R;

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        R = cam.calculateProjectionMatrix();
        JoglMatrixRenderer.activate(gl, R);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    public static void activateCenter(GL2 gl, Camera cam)
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
        camera2.setNearPlaneDistance(0.1);
        camera2.setFarPlaneDistance(10.0);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        R = camera2.calculateProjectionMatrix();
        JoglMatrixRenderer.activate(gl, R);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private static void drawBase(GL2 gl)
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

    public static void drawVolume(GL2 gl, Camera cam)
    {
        // This line causes problems on some drivers!
        //gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glShadeModel(GL2.GL_FLAT);

        //- Calcule los extremos del volumen de visualizacion ---------------
        double xn;
        double yn;
        double xf;
        double yf;
        double npd; // Near plane distance
        double fpd; // Far plane distance

        npd = cam.getNearPlaneDistance();
        fpd = cam.getFarPlaneDistance();

        if ( cam.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            yn = 2 * npd * Math.tan(Math.toRadians(cam.getFov()) / 2);
            xn = yn * (cam.getViewportXSize() / cam.getViewportYSize());
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
        if ( cam.getProjectionMode() != Camera.PROJECTION_MODE_ORTHOGONAL ) {
            yf = 2 * fpd * Math.tan(Math.toRadians(cam.getFov()) / 2);
            xf = yf * (cam.getViewportXSize()/cam.getViewportYSize());
        }

        //- Genere las primitivas que muestran el volumen de visualizacion --
        double delta = 0.1;
        gl.glColor3d(1, 0.5, 0.5);   // Origen de la camara
        gl.glLineWidth((float)1.0);
        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(delta, 0, 0);
            gl.glVertex3d(0, -delta, 0);
            gl.glVertex3d(0, delta, 0);
            gl.glVertex3d(0, 0, -delta);
            gl.glVertex3d(0, 0, delta);
        gl.glEnd();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0, 1, 1);       // Plano de proyeccion
        gl.glLineWidth((float)2.0);
        gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex3d(npd, -xn/2, -yn/2);
            gl.glVertex3d(npd, xn/2, -yn/2);
            gl.glVertex3d(npd, xn/2, yn/2);
            gl.glVertex3d(npd, -xn/2, yn/2);
        gl.glEnd();
        gl.glBegin(GL.GL_LINES);
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
        gl.glBegin(GL.GL_LINE_LOOP);
            gl.glVertex3d(fpd, -xf/2, -yf/2);
            gl.glVertex3d(fpd, xf/2, -yf/2);
            gl.glVertex3d(fpd, xf/2, yf/2);
            gl.glVertex3d(fpd, -xf/2, yf/2);
        gl.glEnd();
        gl.glBegin(GL.GL_LINES);
            gl.glVertex3d(fpd, -xf/10, -yf/10);
            gl.glVertex3d(fpd, xf/10, yf/10);
            gl.glVertex3d(fpd, xf/10, -yf/10);
            gl.glVertex3d(fpd, -xf/10, yf/10);
            gl.glVertex3d(fpd, -xf/10, yf/2);
            gl.glVertex3d(fpd, 0, yf/2+yf/10);
            gl.glVertex3d(fpd, 0, yf/2+yf/10);
            gl.glVertex3d(fpd, xf/10, yf/2);
        gl.glEnd();

        gl.glBegin(GL.GL_LINES);  // Lineas de los bordes
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

    public static void draw(GL2 gl, Camera cam)
    {
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();

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
