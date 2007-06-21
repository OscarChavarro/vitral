//===========================================================================
//=-------------------------------------------------------------------------=
//= Oscar Chavarro, June 16 2007.                                           =
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//= [MIN2003] Min, Patrick. Halderman, John A. Kazhdan, Michael.            =
//=     Funkhouser, Thoimas A. "Early Experiences with a 3D Model Search    =
//=     Engine".                                                            =
//===========================================================================

// Java basic classes
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.io.File;

// JOGL classes
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;

// VSDK Classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyGroupRenderer;
import vsdk.toolkit.io.image.ImagePersistence;

public class JoglPreviewGenerator
{
    private boolean
    checkDirectory(String dirName)
    {
        File dirFd = new File(dirName);

        if ( dirFd.exists() && (!dirFd.isDirectory() ) ) {
            System.err.println("Directory " + dirName + " can not be created, because a file with that name already exists (not overwriten).");
            return false;
        }

        if ( !dirFd.exists() && !dirFd.mkdir() ) {
            System.err.println("Directory " + dirName + " can not be created, check permisions and available free disk space.");
            return false;
        }

        return true;
    }

    private static void drawGridRectangle(GL gl)
    {
        int nx = 14; // Must be an even number
        int ny = 14; // Must be an even number
        double dx = 1.0;
        double dy = 1.0;
        int x, y;
        double minx = -(((double)nx)/2) * dx;
        double maxx = (((double)nx)/2) * dx;
        double miny = -(((double)ny)/2) * dy;
        double maxy = (((double)ny)/2) * dy;
        double z = -1;

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINES);
        gl.glColor3d(0.37, 0.37, 0.37);
        for ( x = 0; x <= nx; x++ ) {
            if ( x == nx/2 ) continue;
            gl.glVertex3d(minx + ((double)x)*dx, miny, z);
            gl.glVertex3d(minx + ((double)x)*dx, maxy, z);
        }
        for ( y = 0; y <= ny; y++ ) {
            if ( y == ny/2 ) continue;
            gl.glVertex3d(minx, minx + ((double)y)*dy, z);
            gl.glVertex3d(maxx, minx + ((double)y)*dy, z);
        }
        gl.glColor3d(0, 0, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, miny, z);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, maxy, z);
        gl.glVertex3d(minx, minx + ((double)(ny/2))*dy, z);
        gl.glVertex3d(maxx, minx + ((double)(ny/2))*dy, z);

        gl.glEnd();
    }

    private void renderView(GL gl, SimpleBodyGroup bodies, Camera camera, RendererConfiguration quality)
    {
        gl.glClearColor(0.7f, 0.7f, 0.7f, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        JoglCameraRenderer.activate(gl, camera);
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glColor3d(0, 0, 0);
        drawGridRectangle(gl);

        if ( bodies != null ) {
            JoglSimpleBodyGroupRenderer.draw(gl, bodies, camera, quality);
        }
        gl.glColor3d(1, 1, 0);
            gl.glBegin(GL.GL_LINE_LOOP);
                gl.glVertex3d(-1, -1, 0);
                gl.glVertex3d(1, -1, 0);
                gl.glVertex3d(1, 1, 0);
                gl.glVertex3d(-1, 1, 0);
            gl.glEnd();

        gl.glFlush();
    }

    /**
    Based on configurations suggested in [MIN2003]
    */
    private void configureView(int i, Camera cam, RendererConfiguration quality)
    {
        Vector3D p = new Vector3D();
        Matrix4x4 R = new Matrix4x4();

        quality.setWires(true);
        quality.setSurfaces(false);

        switch ( i ) {
          case 0:
            p = new Vector3D(15, -15, 0);
            R.eulerAnglesRotation(Math.toRadians(135), 0, 0);
            break;
          default:
            p = new Vector3D(0, 0, 15);
            R.eulerAnglesRotation(0, Math.toRadians(-90), 0);
            break;
        }

        cam.setPosition(p);
        cam.setRotation(R);
    }

    public void calculatePreviews(
        GL gl, SimpleBodyGroup referenceBodies, long modelId, int viewportXSize, int viewportYSize, GLCanvas canvas)
    {
        //- Create directory for current model previews set ---------------
        DecimalFormat f1 = new DecimalFormat("0000000");
        DecimalFormat f2 = new DecimalFormat("00");
        String dirName = "./output/previews/" + f1.format(modelId, new StringBuffer(""), new FieldPosition(0)).toString();
        if ( !checkDirectory("./output") ||
             !checkDirectory("./output/previews") ||
             !checkDirectory(dirName) ) {
            System.err.println("Unable to create / find directories for preview generation!");
            System.err.println("Aborting preview generation.");
            return;
        }

        //-----------------------------------------------------------------
        gl.glViewport(0, 0, viewportXSize, viewportYSize);
        int i;
        Camera cam = new Camera();
        cam.updateViewportResize(viewportXSize, viewportYSize);
        RendererConfiguration quality = new RendererConfiguration();

        for ( i = 0; i < 2; i++ ) {
            configureView(i, cam, quality);
            renderView(gl, referenceBodies, cam, quality);

            //-----------------------------------------------------------------
            String filename = dirName + "/" + f2.format(i, new StringBuffer(""), new FieldPosition(0)).toString() + ".jpg";
            if ( canvas != null ) {
                canvas.swapBuffers();
            }
            RGBImage img = JoglRGBImageRenderer.getImageJOGL(gl);
            ImagePersistence.exportJPG(new File(filename), img);
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
