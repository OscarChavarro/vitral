//=   previous "JoglView" class at SceneEditorApplication example).         =

package application.render.jogl;

// Java basic classes
import java.util.ArrayList;

// AWT/Swing classes
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

// JOGL classes
import com.jogamp.opengl.GL2;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.linealAlgebra.Vector3D;
import vsdk.toolkit.common.linealAlgebra.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.volume.Arrow;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.gui.AwtSystem;
import vsdk.toolkit.gui.TranslateGizmo;
import vsdk.toolkit.gui.ViewportWindow;

public class JoglAwtViewportWindow extends ViewportWindow implements KeyListener
{
    // Current configuration, available only after a call to activateViewportGL
    private int viewportStartX;
    private int viewportStartY;
    private int viewportSizeX;
    private int viewportSizeY;

    // A JoglAwtViewportWindow can request an specific size in pixels. If this size gets
    // smaller than percent-based area, the viewport is assigned to match the
    // requested size. If a requested size dimension in pixels is greater
    // than the percent-based area, the requested size is ignored and the
    // viewport will get the percent-based size. The smaller requested size
    // viewport will always be centered inside the given percent-based area.
    // A requested size of 0 means the requested size match the percent-based
    // size. This is usefull in applications willing to use just a subset of
    // the area, for example when previewing slow raytracing visualizations.
    private int viewportRequestedSizeXInPixels;
    private int viewportRequestedSizeYInPixels;

    // Every JoglAwtViewportWindow has an internal camera and renderer configuration
    private Camera camera;
    private Camera cameraPerspective;
    private Camera cameraTop;
    private Camera cameraBottom;
    private Camera cameraLeft;
    private Camera cameraFront;
    private RendererConfiguration quality;

    //
    private String title;
    private RGBAImage titleImage;
    private RGBAImage xLabelImage;
    private RGBAImage yLabelImage;
    private RGBAImage zLabelImage;
    private RGBAImage xLabelImageSelected;
    private RGBAImage yLabelImageSelected;
    private RGBAImage zLabelImageSelected;

    // Each JoglAwtViewportWindow can call a different visualization algorithm
    public static final int RENDER_MODE_ZBUFFER = 1;
    public static final int RENDER_MODE_RAYTRACING = 2;
    private int renderMode;

    public boolean showGrid;

    public JoglAwtViewportWindow()
    {
        //-----------------------------------------------------------------
        super();
        viewportRequestedSizeXInPixels = 0;
        viewportRequestedSizeYInPixels = 0;

        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        cameraPerspective = new Camera();

        cameraPerspective.setPosition(new Vector3D(-5, -5, 5));
        R = R.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);

        //cameraPerspective.setPosition(new Vector3D(0, -4, 0));
        //R = R.eulerAnglesRotation(Math.toRadians(90.0), 0.0, 0.0);

        cameraPerspective.setRotation(R);
        cameraPerspective.setName("Perspective");

        cameraTop = new Camera();
        cameraTop.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        cameraTop.setPosition(new Vector3D(0, 0, 5));
        R = R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-90), 0);
        cameraTop.setRotation(R);
        cameraTop.setOrthogonalZoom(0.25);
        cameraTop.setName("Top");

        cameraBottom = new Camera();
        cameraBottom.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        cameraBottom.setPosition(new Vector3D(0, 0, -5));
        R = R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(90), 0);
        cameraBottom.setRotation(R);
        cameraBottom.setOrthogonalZoom(0.25);
        cameraBottom.setName("Bottom");

        cameraLeft = new Camera();
        cameraLeft.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        cameraLeft.setPosition(new Vector3D(-5, 0, 0));
        R = R.identity();
        cameraLeft.setRotation(R);
        cameraLeft.setOrthogonalZoom(0.25);
        cameraLeft.setName("Left");

        cameraFront = new Camera();
        cameraFront.setProjectionMode(Camera.PROJECTION_MODE_ORTHOGONAL);
        cameraFront.setPosition(new Vector3D(0, -5, 0));
        R = R.eulerAnglesRotation(Math.toRadians(90), 0, 0);
        cameraFront.setRotation(R);
        cameraFront.setOrthogonalZoom(0.25);
        cameraFront.setName("Front");

        camera = cameraPerspective;

        //-----------------------------------------------------------------
        quality = new RendererConfiguration();

        setTitle(camera.getName());
        xLabelImage = AwtSystem.calculateLabelImage("X", new ColorRgb(0.78, 0, 0));
        yLabelImage = AwtSystem.calculateLabelImage("Y", new ColorRgb(0, 0.61, 0));
        zLabelImage = AwtSystem.calculateLabelImage("Z", new ColorRgb(0, 0, 0.76));

        xLabelImageSelected = AwtSystem.calculateLabelImage("X", new ColorRgb(1, 1, 0));
        yLabelImageSelected = AwtSystem.calculateLabelImage("Y", new ColorRgb(1, 1, 0));
        zLabelImageSelected = AwtSystem.calculateLabelImage("Z", new ColorRgb(1, 1, 0));

        this.renderMode = RENDER_MODE_ZBUFFER;

        showGrid = true;
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method.
    */
    @Override
    public void keyTyped(KeyEvent e) {
        ;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keycode;
        char unicode_id;
        boolean skipKey = false;

        keycode = e.getKeyCode();
        unicode_id = e.getKeyChar();

        if ( keycode == KeyEvent.VK_9 ) {
            // Alphanumeric 0
            skipKey = true;
            switch ( renderMode ) {
              case RENDER_MODE_ZBUFFER:
                renderMode = RENDER_MODE_RAYTRACING;
                break;
              default:
                renderMode = RENDER_MODE_ZBUFFER;
                break;
            }
        }

        if ( unicode_id != KeyEvent.CHAR_UNDEFINED && !skipKey ) {
            switch ( unicode_id ) {
              case 'g':
                if ( showGrid == true ) {
                    showGrid = false;
                }
                else {
                    showGrid = true;
                }
                break;
              case 't':
                camera = cameraTop;
                setTitle(camera.getName());
                break;
              case 'l':
                camera = cameraLeft;
                setTitle(camera.getName());
                break;
              case 'f':
                camera = cameraFront;
                setTitle(camera.getName());
                break;
              case 'b':
                camera = cameraBottom;
                setTitle(camera.getName());
                break;
              case 'p':
                camera = cameraPerspective;
                setTitle(camera.getName());
                break;
              case '0':
                switch ( getViewportRequestedSizeXInPixels() ) {
                  case 0:
                    setViewportRequestedSizeXInPixels(320);
                    setViewportRequestedSizeYInPixels(240);
                    break;
                  case 320:
                    setViewportRequestedSizeXInPixels(640);
                    setViewportRequestedSizeYInPixels(480);
                    break;
                  case 640:
                    setViewportRequestedSizeXInPixels(800);
                    setViewportRequestedSizeYInPixels(600);
                    break;
                  case 800:
                    setViewportRequestedSizeXInPixels(0);
                    setViewportRequestedSizeYInPixels(0);
                    break;
                }
                break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    public int getRenderMode()
    {
        return renderMode;
    }

    public int getViewportRequestedSizeXInPixels()
    {
        return viewportRequestedSizeXInPixels;
    }

    public int getViewportRequestedSizeYInPixels()
    {
        return viewportRequestedSizeYInPixels;
    }

    public void setViewportRequestedSizeXInPixels(int viewportRequestedSizeXInPixels)
    {
        this.viewportRequestedSizeXInPixels = viewportRequestedSizeXInPixels;
    }

    public void setViewportRequestedSizeYInPixels(int viewportRequestedSizeYInPixels)
    {
        this.viewportRequestedSizeYInPixels = viewportRequestedSizeYInPixels;
    }

    public int getViewportStartX()
    {
        return viewportStartX;
    }

    public int getViewportStartY()
    {
        return viewportStartY;
    }

    public int getViewportSizeX()
    {
        return viewportSizeX;
    }

    public int getViewportSizeY()
    {
        return viewportSizeY;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public boolean useFullContainerViewportArea()
    {
        int x, y;
        x = getViewportRequestedSizeXInPixels();
        y = getViewportRequestedSizeYInPixels();

        if ( x != 0 && y != 0 ) {
            return false;
        }
        return true;
    }    

    public Camera getCamera()
    {
        return camera;
    }

    public RendererConfiguration getRendererConfiguration()
    {
        return quality;
    }

    /**
    A given container canvas has valid pixel coordinates from (0, 0) to
    (canvasXSize-1, canvasYSize-1). Current JoglAwtViewportWindow is defined inside that
    area in terms of a start point (upper left corner) and size given in
    area percentages.
    This method calculates the (viewportStartX, viewportStartY,
    viewportSizeX, viewportSizeY) variables that defines current JoglAwtViewportWindow
    viewport in container canvas' integer pixel coordinates.
    */
    public void updateViewportConfiguration(int canvasXSize, int canvasYSize)
    {
        int w, h;
        int subCanvasXSize;
        int subCanvasYSize;

        viewportStartX = (int)(viewportStartXPercent*((double)canvasXSize))+viewportBorder+1;
        viewportStartY = (int)(viewportStartYPercent*((double)canvasYSize))+viewportBorder+1;
        subCanvasXSize = (int)(viewportSizeXPercent*((double)canvasXSize))-2*viewportBorder-2;
        subCanvasYSize = (int)(viewportSizeYPercent*((double)canvasYSize))-2*viewportBorder-2;
        if ( useFullContainerViewportArea() ) {
            viewportSizeX = subCanvasXSize;
            viewportSizeY = subCanvasYSize;
        }
        else {
            if ( viewportRequestedSizeXInPixels < subCanvasXSize ) {
                w = viewportRequestedSizeXInPixels;
            }
            else {
                w = subCanvasXSize;
            }
            if ( viewportRequestedSizeYInPixels < subCanvasYSize ) {
                h = viewportRequestedSizeYInPixels;
            }
            else {
                h = subCanvasYSize;
            }
            viewportStartX += (subCanvasXSize - w) / 2;
            viewportStartY += (subCanvasYSize - h) / 2;
            viewportSizeX = w;
            viewportSizeY = h;
        }
        cameraPerspective.updateViewportResize(viewportSizeX, viewportSizeY);
        cameraTop.updateViewportResize(viewportSizeX, viewportSizeY);
        cameraBottom.updateViewportResize(viewportSizeX, viewportSizeY);
        cameraLeft.updateViewportResize(viewportSizeX, viewportSizeY);
        cameraFront.updateViewportResize(viewportSizeX, viewportSizeY);
    }

    public void activateViewportGL(GL2 gl, int canvasXSize, int canvasYSize)
    {
        if ( !active ) {
            return;
        }
        updateViewportConfiguration(canvasXSize, canvasYSize);
        gl.glViewport(viewportStartX, viewportStartY, viewportSizeX, viewportSizeY);
    }

    /**
    PRE: glViewport is set to full canvas area, and projection and modelview
    matrices are set to identity.
    */
    public void drawBorderGL(GL2 gl, int viewportXsize, int viewportYsize)
    {
        if ( !active || viewportBorder <= 0 ) {
            return;
        }
        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glPushAttrib(GL2.GL_TEXTURE_2D);
        gl.glPushAttrib(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        double x1, y1, x2, y2;
        double epsilonx = 2.0 / ((double)viewportXsize);
        double epsilony = 2.0 / ((double)viewportYsize);
        double dx = (viewportBorder) * epsilonx;
        double dy = (viewportBorder) * epsilony;

        x1 = viewportStartXPercent*2 - 1;
        y1 = viewportStartYPercent*2 - 1;
        x2 = x1 + viewportSizeXPercent*2;
        y2 = y1 + viewportSizeYPercent*2;

        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        gl.glBegin(GL2.GL_QUADS);
            if ( selected ) {
                gl.glColor3d(1, 0.96, 0);
            }
            else {
                gl.glColor3d(0.21, 0.25, 0.29);
            }
            gl.glVertex3d(x1, y1, 0);
            gl.glVertex3d(x2, y1, 0);
            gl.glVertex3d(x2, y2, 0);
            gl.glVertex3d(x1, y2, 0);
            gl.glColor3d(0, 0, 0);
            gl.glVertex3d(x1+2*dx, y1+2*dy, 0);
            gl.glVertex3d(x2-2*dx, y1+2*dy, 0);
            gl.glVertex3d(x2-2*dx, y2-2*dy, 0);
            gl.glVertex3d(x1+2*dx, y2-2*dy, 0);
        gl.glEnd();
        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
    }

    public final void setTitle(String name)
    {
        title = name;
        titleImage = AwtSystem.calculateLabelImage(title, new ColorRgb(0.76, 0.76, 0.76));
    }

    public void drawReferenceBase(GL2 gl)
    {
        //-----------------------------------------------------------------
        int basesize = 64;
        gl.glPushAttrib(GL2.GL_VIEWPORT_BIT);
        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glPushAttrib(GL2.GL_TEXTURE_2D);
        gl.glPushAttrib(GL2.GL_LIGHTING);
        gl.glViewport(viewportStartX, viewportStartY, basesize, basesize);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        Matrix4x4 R = camera.getRotation();

        gl.glLoadIdentity();
        R = R.invert();
        gl.glRotated(90, -1, 0, 0);
        gl.glRotated(90, 0, 0, 1);
        JoglMatrixRenderer.activate(gl, R);

        gl.glPushMatrix();
        gl.glTranslated(2, 1, 0);
        drawTextureString3D(gl, xLabelImage);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(1, 2, 0);
        drawTextureString3D(gl, yLabelImage);
        gl.glPopMatrix();

        gl.glPushMatrix();
        gl.glTranslated(1, 1, 1);
        drawTextureString3D(gl, zLabelImage);
        gl.glPopMatrix();

        //gl.glLoadIdentity();
        gl.glBegin(GL2.GL_LINES);
            gl.glColor3d(0.78, 0, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(1, 0, 0);
            gl.glColor3d(0, 0.61, 0);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 1, 0);
            gl.glColor3d(0, 0, 0.76);
            gl.glVertex3d(0, 0, 0);
            gl.glVertex3d(0, 0, 1);
        gl.glEnd();

        //-----------------------------------------------------------------
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
    }

    private void drawGridRectangle(GL2 gl)
    {
        gl.glPushMatrix();

        Matrix4x4 R;
        double yaw, pitch;

        R = camera.getRotation();
        yaw = Math.toDegrees(R.obtainEulerYawAngle());
        pitch = Math.toDegrees(R.obtainEulerPitchAngle());

        if ( camera.getProjectionMode() == Camera.PROJECTION_MODE_ORTHOGONAL &&
              (pitch > -45 && pitch < 45) ) {
            if ( (yaw > 45 && yaw < 135) ||
                 (yaw < -45 && yaw > -135) ) {
                gl.glRotated(90, 1, 0, 0);
            }
            else {
                gl.glRotated(90, 0, 1, 0);
            }
        }
        
        //-----------------------------------------------------------------
        int nx = 14; // Must be an even number
        int ny = 14; // Must be an even number
        double dx = 1.0;
        double dy = 1.0;
        int x, y;
        double minx = -(((double)nx)/2) * dx;
        double maxx = (((double)nx)/2) * dx;
        double miny = -(((double)ny)/2) * dy;
        double maxy = (((double)ny)/2) * dy;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3d(0.37, 0.37, 0.37);
        for ( x = 0; x <= nx; x++ ) {
            if ( x == nx/2 ) continue;
            gl.glVertex3d(minx + ((double)x)*dx, miny, 0);
            gl.glVertex3d(minx + ((double)x)*dx, maxy, 0);
        }
        for ( y = 0; y <= ny; y++ ) {
            if ( y == ny/2 ) continue;
            gl.glVertex3d(minx, minx + ((double)y)*dy, 0);
            gl.glVertex3d(maxx, minx + ((double)y)*dy, 0);
        }
        gl.glColor3d(0, 0, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, miny, 0);
        gl.glVertex3d(minx + ((double)(nx/2))*dx, maxy, 0);
        gl.glVertex3d(minx, minx + ((double)(ny/2))*dy, 0);
        gl.glVertex3d(maxx, minx + ((double)(ny/2))*dy, 0);

        gl.glEnd();
        gl.glPopMatrix();
    }

    public void toggleGrid()
    {
        if ( showGrid ) {
            showGrid = false;
        }
        else {
            showGrid = true;
        }
    }

    public void drawGrid(GL2 gl)
    {
        //- Draw reference grid plane -------------------------------------
        if ( showGrid ) drawGridRectangle(gl);
    }

    public void drawTextureString2D(GL2 gl, int x, int y, RGBAImage i)
    {
        double dx;
        double dy;

        dx = ((double)(2*x)) / ((double)viewportSizeX);
        dy = ((double)(2*(viewportSizeY - y))) / ((double)viewportSizeY);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslated(dx, dy, 0);

        drawTextureString3D(gl, i);

        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(GL2.GL_MODELVIEW);
    }

    private void drawTextureString3D(GL2 gl, RGBAImage i)
    {
        gl.glPushAttrib(GL2.GL_ENABLE_BIT);
        gl.glRasterPos3d(-1, -1, 0);
        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        // Set texture parameters
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_GENERATE_MIPMAP,
            GL2.GL_TRUE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
            GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
            GL2.GL_NEAREST);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
            GL2.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
            GL2.GL_CLAMP_TO_EDGE);

        // Calling this configuration with GL_BLEND here generates an error on
        // some Windows Vista machines with Intel graphics, as such on
        // Dell Inspiron 1525 laptop with Mobile Intel 965 (BIOS 1566).
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

        //float c[] = {1f, 1f, 1f, 1f};
        //gl.glTexEnvfv(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_COLOR, c, 0);

        JoglImageRenderer.draw(gl, i);
        gl.glPopAttrib();
    }

    public void drawTitle(GL2 gl)
    {
        int borderx = 4;
        int bordery = 1;
        drawTextureString2D(gl, borderx, titleImage.getYSize() + bordery, titleImage);
    }

    public void updateMouseEvent(MouseEvent e, int globalViewportXSize, int globalViewportYSize)
    {
          e.translatePoint(-getViewportStartX(),
            getViewportSizeY() - (globalViewportYSize-getViewportStartY()));
    }

    public void hintConfig(int numViews, int id)
    {
        if ( numViews >= 4 ) {
            switch ( id ) {
              case 0:
                camera = cameraLeft;
                quality.setSurfaces(false);
                quality.setWires(true);
                break;
              case 1:
                camera = cameraPerspective;
                break;
              case 2:
                camera = cameraTop;
                quality.setSurfaces(false);
                quality.setWires(true);
                break;
              case 3: default:
                camera = cameraFront;
                quality.setSurfaces(false);
                quality.setWires(true);
                break;
            }
            setTitle(camera.getName());
        }
    }

    public void drawLabelsForTranslateGizmo(GL2 gl, TranslateGizmo gizmo)
    {
        ArrayList<SimpleBody> things = gizmo.getElements();
        int i;
        Vector3D lv = new Vector3D();
        Vector3D p;
        Vector3D tp = new Vector3D();
        Matrix4x4 R;
        boolean yellow;
        ColorRgb c = new ColorRgb(1, 1, 0);

        for ( i = 0; i < things.size() && i < 3; i++ ) {
            SimpleBody r = things.get(i);
            Geometry g = r.getGeometry();
            Vector3D position;

            if ( g != null ) {
                gl.glPushMatrix();
                gl.glLoadIdentity();

                lv = new Vector3D(0, 0, lv.z());
                if ( g instanceof Arrow ) {
                    Arrow a = ((Arrow)g);
                    lv = lv.withZ((a.getHeadLength() + a.getBaseLength()) * 1.1);
                }
                else {
                    lv = lv.withZ(1);
                }

                R = new Matrix4x4();
                R = R.translation(r.getPosition());
                R = R.multiply(r.getRotation());
                p = R.multiply(lv);
                camera.projectPoint(p, tp);

                //---------------------------------------------
                yellow = false;
                if ( VSDK.colorDistance(c, r.getMaterial().getDiffuse()) <
                     VSDK.EPSILON ) {
                yellow = true;
                }

                //---------------------------------------------
                switch ( i ) {
                  case 0:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            xLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            xLabelImage);
                    }
                    break;
                  case 1:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            yLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            yLabelImage);
                    }
                    break;
                  case 2:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            zLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x()-3, (int)tp.y()+12,
                            zLabelImage);
                    }
                    break;
                }
                gl.glPopMatrix();
            }
        }
    }
}
