//===========================================================================
//=-------------------------------------------------------------------------=
//= References:                                                             =
//= [FUNK2003], Funkhouser, Thomas.  Min, Patrick. Kazhdan, Michael. Chen,  =
//=     Joyce. Halderman, Alex. Dobkin, David. Jacobs, David. "A Search     =
//=     Engine for 3D Models", ACM Transactions on Graphics, Vol 22. No1.   =
//=     January 2003. Pp. 83-105                                            =
//===========================================================================

// JOGL classes
import javax.media.opengl.GL;

// VSDK Classes
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.media.Image;
import vsdk.toolkit.media.RGBImage;
import vsdk.toolkit.media.IndexedColorImage;
import vsdk.toolkit.media.NormalMap;
import vsdk.toolkit.media.ZBuffer;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.environment.scene.SimpleBodyGroup;
import vsdk.toolkit.render.jogl.JoglRGBImageRenderer;
import vsdk.toolkit.render.jogl.JoglZBufferRenderer;
import vsdk.toolkit.render.jogl.JoglSimpleBodyRenderer;
import vsdk.toolkit.render.jogl.JoglCameraRenderer;

public class JoglPerspectiveViewRenderer {
    public Image image;
    private SimpleBodyGroup bodies;
    private RendererConfiguration quality;
    private Camera camera;
    private int side = 0;
    private boolean isTransparent;

    public JoglPerspectiveViewRenderer(boolean transparent) {
        bodies = null;
        quality = new RendererConfiguration();
        quality.setWires(false);
        quality.setSurfaces(true);
        camera = new Camera();
        camera.setFov(90);
        camera.setProjectionMode(camera.PROJECTION_MODE_ORTHOGONAL);
        camera.setNearPlaneDistance(2);
        camera.setFarPlaneDistance(20);
        isTransparent = transparent;
    }

    /**
    Given a body set and a cameraConfig, current method sets bodies and camera
    to build a small virtual 3D scene to match with one neccesary to construct
    a projected image, as decribed in [FUNK2003].5, and figure [FUNK2003].8.
    Note that `cameraConfig` has the following posible values:
      - 1   Front side view (from -Y axis)
      - 2   Lateral side view (from -X axis)
      - 3   Top side view (from -Z axis)
      - 4   Corner view from -X -Y Z direction
      - 5   Corner view from  X -Y Z direction
      - 6   Corner view from  X  Y Z direction
      - 7   Corner view from -X  Y Z direction
      - 8   Tilt view edge +Y on plane -Z
      - 9   Tilt view edge -X on plane -Z
      - 10  Tilt view edge -Y on plane -Z
      - 11  Tilt view edge +X on plane -Z
      - 12  Tilt view edge +X on plane -Y
      - 13  Tilt view edge +X on plane Y
    */
    public void configureScene(SimpleBodyGroup center, int cameraConfig)
    {
        side = cameraConfig;
        bodies = center;
        Vector3D position = new Vector3D(0, 0, 0);
        Matrix4x4 R = new Matrix4x4();
        double cornerAngle;

        Vector3D down = new Vector3D(0, 0, -1);
        Vector3D cornerReference = new Vector3D(10, 10, -10);
        cornerReference.normalize();
        cornerAngle = (Math.PI/2-(Math.acos(down.dotProduct(cornerReference))));
        switch( cameraConfig ) {
          case 1:
            position = new Vector3D(0, -10, 0);
            R.eulerAnglesRotation(Math.toRadians(90), 0, 0);
            break;
          case 2:
            position = new Vector3D(-10, 0, 0);
            break;
          case 3:
            position = new Vector3D(0, 0, -10);
            R.eulerAnglesRotation(Math.toRadians(-90), Math.toRadians(90), 0);
            break;
          case 4:
            position = new Vector3D(-10, -10, 10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(45), -cornerAngle, 0);
            break;
          case 5:
            position = new Vector3D(10, -10, 10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(135), -cornerAngle, 0);
            break;
          case 6:
            position = new Vector3D(10, 10, 10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(-135), -cornerAngle, 0);
            break;
          case 7:
            position = new Vector3D(-10, 10, 10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(-45), -cornerAngle, 0);
        break;
          case 8:
            position = new Vector3D(0, 10, -10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(-90), Math.toRadians(45), 0);
        break;
          case 9:
            position = new Vector3D(-10, 0, -10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(0), Math.toRadians(45), 0);
        break;
          case 10:
            position = new Vector3D(0, -10, -10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(45), 0);
        break;
          case 11:
            position = new Vector3D(10, 0, -10);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(180), Math.toRadians(45), 0);
        break;
          case 12:
            position = new Vector3D(10, -10, 0);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(135), 0, 0);
        break;
          case 13:
            position = new Vector3D(10, 10, 0);
            position.normalize();
            position = position.multiply(10);
            R.eulerAnglesRotation(Math.toRadians(180+45), 0, 0);
        break;
          default:
            break;
        }
        camera.setPosition(position);
        camera.setRotation(R);
    }

    public void draw(GL gl) {
        //-----------------------------------------------------------------
        gl.glClearColor(0.5f, 0.5f, 0.9f, 1);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

        gl.glEnable(gl.GL_DEPTH_TEST);
        JoglCameraRenderer.activate(gl, camera);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        if ( bodies == null ) {
            gl.glColor3d(1, 1, 1); 
            gl.glBegin(GL.GL_LINES);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0.5, 0.5, 0);
            gl.glEnd();
        }
        else {
            int i;
        for ( i = 0; i < bodies.getBodies().size(); i++ ) {
                JoglSimpleBodyRenderer.draw(gl, bodies.getBodies().get(i), camera, quality);
        }
        }
        
        gl.glFlush();

        //- Obtain ZBuffer ------------------------------------------------
        IndexedColorImage zbuffer;
        NormalMap nm;
        zbuffer = JoglZBufferRenderer.importJOGLZBuffer(gl).exportIndexedColorImage();

        //- Erase internal details: keep just the depth frontier border ---
        int x, y;
        int val;
        for ( x = 0; x < zbuffer.getXSize(); x++ ) {
            for ( y = 0; y < zbuffer.getYSize(); y++ ) {
                val = zbuffer.getPixel(x, y);
                if ( val < 255 ) {
                    zbuffer.putPixel(x, y, (byte)0);
                }
                else {
                    zbuffer.putPixel(x, y, (byte)255);
                }

            }
        }

        //- Get contourns from depth buffer's gradient --------------------
        nm = new NormalMap();
        nm.importBumpMap(zbuffer, new Vector3D(1, 1, 0.1));

        if ( isTransparent ) {
            image = nm.exportToRgbaImageGradient();
          }
          else {
            image = nm.exportToRgbImageGradient();
        }

        //- Calculate borders (contourns) ---------------------------------
        if ( isTransparent ) {
            image = nm.exportToRgbaImageGradient();
          }
          else {
            image = nm.exportToRgbImageGradient();
        }

        //- Debug code (disabled) -----------------------------------------
        //image=JoglRGBImageRenderer.getImageJOGL(gl);
        //vsdk.toolkit.io.image.ImagePersistence.exportPPM(new java.io.File("./output" + side + ".ppm"), image);
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
