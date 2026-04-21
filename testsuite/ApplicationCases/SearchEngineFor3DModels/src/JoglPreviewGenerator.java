//= Oscar Chavarro, June 16 2007.                                           =
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//= [MIN2003] Min, Patrick. Halderman, John A. Kazhdan, Michael.            =
//=     Funkhouser, Thoimas A. "Early Experiences with a 3D Model Search    =
//=     Engine".                                                            =

// Java basic classes
import java.io.File;

// JOGL classes
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.awt.GLCanvas;

// VSDK Classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.Light;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;
import vsdk.toolkit.render.jogl.JoglLightRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyGroupRenderer;
import vsdk.toolkit.io.image.ImagePersistence;
import vsdk.toolkit.io.PersistenceElement;
import vsdk.toolkit.processing.ImageProcessing;

public class JoglPreviewGenerator
{
    private static void drawGridRectangle(GL2 gl, double z)
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

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);
        gl.glBegin(gl.GL_LINES);
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

    private void renderView(GL2 gl, SimpleBodyGroup bodies, Camera camera, RendererConfiguration quality)
    {
        double minmax[];

        gl.glClearColor(0.62f, 0.72f, 0.83f, 1);
        gl.glClear(gl.GL_COLOR_BUFFER_BIT);
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT);

        JoglCameraRenderer.activate(gl, camera);
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glColor3d(0, 0, 0);
        minmax = bodies.getMinMax();
        drawGridRectangle(gl, minmax[2]);

        if ( bodies != null ) {
            JoglSimpleBodyGroupRenderer.draw(gl, bodies, camera, quality);

            // Debug code to check correct posing to unit sphere
/*
            vsdk.toolkit.environment.geometry.volume.Sphere sphere = new vsdk.toolkit.environment.geometry.volume.Sphere(1);
            RendererConfiguration quality2;
            quality2 = new RendererConfiguration();
            quality2.setWires(true);
            quality2.setSurfaces(false);
            vsdk.toolkit.render.jogl.JoglSphereRenderer.draw(gl, sphere, camera, quality2);
*/
        }

        gl.glFlush();
    }

    /**
    Based on configurations suggested in [MIN2003]
    */
    private void configureView(int i, Camera cam, RendererConfiguration quality)
    {
        Vector3D position = new Vector3D();
        Matrix4x4 R = new Matrix4x4();
        double yaw = 0, pitch = 0;
        double fov = 60.0;

        quality.setShadingType(quality.SHADING_TYPE_GOURAUD);
        switch ( i ) {
          case 0:
            yaw = 160;
            pitch = -10;
            quality.setWires(false);
            quality.setSurfaces(true);
            break;
          case 1:
            yaw = -20;
            pitch = -10;
            quality.setWires(false);
            quality.setSurfaces(true);
            break;
          case 2:
            yaw = 160;
            pitch = -70;
            quality.setWires(false);
            quality.setSurfaces(true);
            break;
          case 3:
            yaw = 160;
            pitch = -10;
            quality.setWires(true);
            quality.setSurfaces(false);
            break;
          case 4:
            yaw = 160;
            pitch = -10;
            quality.setWires(true);
            quality.setSurfaces(true);
            break;
          case 5:
            yaw = 160;
            pitch = -10;
            fov = 30;
            quality.setWires(true);
            quality.setSurfaces(true);
            break;
          case 6:
            yaw = 160;
            pitch = -10;
            fov = 30;
            quality.setWires(false);
            quality.setSurfaces(true);
            quality.setShadingType(quality.SHADING_TYPE_FLAT);
            break;
          case 7:
          default:
            yaw = -20;
            pitch = -10;
            fov = 30;
            quality.setWires(true);
            quality.setSurfaces(true);
            break;
        }

        R = R.eulerAnglesRotation(Math.toRadians(yaw), Math.toRadians(pitch), 0);
        position = new Vector3D(-1, 0, 0);
        position = R.multiply(position);
        position = position.normalized();
        position = position.multiply(2);
        cam.setPosition(position);
        cam.setRotation(R);
        cam.setFov(fov);
        cam.setNearPlaneDistance(0.2);
        cam.setFarPlaneDistance(20);
    }

    public void calculatePreviews(
        GL2 gl, SimpleBodyGroup referenceBodies, long modelId, int viewportXSize, int viewportYSize, GLCanvas canvas)
    {
        //- Create directory for current model previews set ---------------
        String dirName = "./output/previews/" + VSDK.formatNumberWithinZeroes(modelId, 7);
        if ( !PersistenceElement.checkDirectory("./output") ||
             !PersistenceElement.checkDirectory("./output/previews") ||
             !PersistenceElement.checkDirectory(dirName) ) {
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
        Light light1;
        Light light2;
        Vector3D p;

        light1 = new Light(vsdk.toolkit.environment.LightType.POINT, new Vector3D(-10, -9, 8), new ColorRgb(0.7, 0.7, 0.7));
        light2 = new Light(vsdk.toolkit.environment.LightType.POINT, new Vector3D(10, 9, -8), new ColorRgb(0.5, 0.5, 0.5));
        light1.setId(0);
        light2.setId(1);
        JoglLightRenderer.activate(gl, light1);
        JoglLightRenderer.activate(gl, light2);

        for ( i = 0; i < 8; i++ ) {
            configureView(i, cam, quality);
            p = cam.getPosition();
            light1.setPosition(p);
            p = p.multiply(-1);
            light2.setPosition(p);
            renderView(gl, referenceBodies, cam, quality);

            //-----------------------------------------------------------------
            String filename = dirName + "/" + VSDK.formatNumberWithinZeroes(i, 2) + ".jpg";
            if ( canvas != null ) {
                canvas.swapBuffers();
            }
            RGBImage img = JoglRGBImageRenderer.getImageJOGL(gl);
            ImagePersistence.exportJPG(new File(filename), img);
            RGBImage thumbnail;
            thumbnail = new RGBImage();
            thumbnail.init(160, 120);
            ImageProcessing.resize(img, thumbnail);
            filename = dirName + "/" + VSDK.formatNumberWithinZeroes(i, 2) + "small.jpg";
            ImagePersistence.exportJPG(new File(filename), thumbnail);
        }
    }
}
