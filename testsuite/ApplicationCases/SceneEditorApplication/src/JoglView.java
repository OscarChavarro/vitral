//===========================================================================

// Java basic classes
import java.util.ArrayList;

// AWT/Swing classes
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

// JOGL classes
import javax.media.opengl.GL;

// VSDK classes
import vsdk.toolkit.common.VSDK;
import vsdk.toolkit.common.ColorRgb;
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.common.RendererConfiguration;
import vsdk.toolkit.environment.Camera;
import vsdk.toolkit.environment.geometry.Geometry;
import vsdk.toolkit.environment.geometry.Arrow;
import vsdk.toolkit.environment.scene.SimpleBody;
import vsdk.toolkit.media.RGBAImage;
import vsdk.toolkit.media.RGBAPixel;
import vsdk.toolkit.render.jogl.JoglImageRenderer;
import vsdk.toolkit.render.jogl.JoglMatrixRenderer;
import vsdk.toolkit.render.awt.AwtRGBAImageRenderer;
import vsdk.toolkit.gui.TranslateGizmo;

/**
A `JoglView` represents an specific Jogl viewport inside a canvas. One Jogl
canvas may contain one ore more `JoglView`s, arrange geometrically in 2D,
ussually forming a partition (or at least a set of non overlaping 2D
rectangles).

The JoglView covers an area defined in terms of percentages.  For example,
a Jogl canvas with a single JoglView covering all of its area, will have
a view located at start percent (0%, 0%) with size (100%, 100%). When
the containing canvas resizes, the internal `JoglView`s will resize
accordingly and will keep its ocupation percentages (see 
`viewportStartXPercent`, `viewportStartYPercent`, `viewportSizeXPercent` and
`viewportSizeYPercent` attributes.
).
*/
public class JoglView implements KeyListener
{
    // JoglView occupancy specification with respect to containing canvas
    private double viewportStartXPercent;
    private double viewportStartYPercent;
    private double viewportSizeXPercent;
    private double viewportSizeYPercent;

    // Current configuration, available only after a call to activateViewportGL
    private int viewportStartX;
    private int viewportStartY;
    private int viewportSizeX;
    private int viewportSizeY;
    private int viewportBorder; // In pixels

    // A JoglView can request an specific size in pixels. If this size gets
    // smaller than percent-based area, the viewport is assigned to match the
    // requested size. If a requested size dimension in pixels is greater
    // than the percent-based area, the requested size is ignored and the
    // viewport will get the percent-based size. The smaller requested size
    // viewport will always be centered inside the given percent-based area.
    // A requested size of 0 means the requested size match the percent-based
    // size.
    private int viewportRequestedSizeXInPixels;
    private int viewportRequestedSizeYInPixels;

    // Every JoglView has an internal camera and renderer configuration
    private Camera camera;
    private Camera cameraPerspective;
    private Camera cameraTop;
    private Camera cameraBottom;
    private Camera cameraLeft;
    private Camera cameraFront;
    private RendererConfiguration quality;

    //
    private boolean selected;
    private boolean active;
    private String title;
    private RGBAImage titleImage;
    private RGBAImage xLabelImage;
    private RGBAImage yLabelImage;
    private RGBAImage zLabelImage;
    private RGBAImage xLabelImageSelected;
    private RGBAImage yLabelImageSelected;
    private RGBAImage zLabelImageSelected;
    private Font font;

    // Each JoglView can call a different visualization algorithm
    public static final int RENDER_MODE_ZBUFFER = 1;
    public static final int RENDER_MODE_RAYTRACING = 2;
    private int renderMode;

    public boolean showGrid;

    public JoglView()
    {
        //-----------------------------------------------------------------
        viewportStartXPercent = 0.0;
        viewportStartYPercent = 0.0;
        viewportSizeXPercent = 1.0;
        viewportSizeYPercent = 1.0;
        viewportRequestedSizeXInPixels = 0;
        viewportRequestedSizeYInPixels = 0;
        viewportBorder = 2;

        //-----------------------------------------------------------------
        Matrix4x4 R = new Matrix4x4();

        cameraPerspective = new Camera();
        cameraPerspective.setPosition(new Vector3D(-5, -5, 5));
        R.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);
        cameraPerspective.setRotation(R);
        cameraPerspective.setName("Perspective");

        cameraTop = new Camera();
        cameraTop.setProjectionMode(cameraTop.PROJECTION_MODE_ORTHOGONAL);
        cameraTop.setPosition(new Vector3D(0, 0, 5));
        R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(-90), 0);
        cameraTop.setRotation(R);
        cameraTop.setOrthogonalZoom(0.5);
        cameraTop.setName("Top");

        cameraBottom = new Camera();
        cameraBottom.setProjectionMode(cameraBottom.PROJECTION_MODE_ORTHOGONAL);
        cameraBottom.setPosition(new Vector3D(0, 0, -5));
        R.eulerAnglesRotation(Math.toRadians(90), Math.toRadians(90), 0);
        cameraBottom.setRotation(R);
        cameraBottom.setOrthogonalZoom(0.5);
        cameraBottom.setName("Bottom");

        cameraLeft = new Camera();
        cameraLeft.setProjectionMode(cameraLeft.PROJECTION_MODE_ORTHOGONAL);
        cameraLeft.setPosition(new Vector3D(-5, 0, 0));
        R.identity();
        cameraLeft.setRotation(R);
        cameraLeft.setOrthogonalZoom(0.5);
        cameraLeft.setName("Left");

        cameraFront = new Camera();
        cameraFront.setProjectionMode(cameraFront.PROJECTION_MODE_ORTHOGONAL);
        cameraFront.setPosition(new Vector3D(0, -5, 0));
        R.eulerAnglesRotation(Math.toRadians(90), 0, 0);
        cameraFront.setRotation(R);
        cameraFront.setOrthogonalZoom(0.5);
        cameraFront.setName("Front");

        camera = cameraPerspective;

        //-----------------------------------------------------------------
        quality = new RendererConfiguration();

        selected = true;
        active = true;

        setTitle(camera.getName());
        xLabelImage = calculateLabelImage("X", new ColorRgb(0.78, 0, 0));
        yLabelImage = calculateLabelImage("Y", new ColorRgb(0, 0.61, 0));
        zLabelImage = calculateLabelImage("Z", new ColorRgb(0, 0, 0.76));

        xLabelImageSelected = calculateLabelImage("X", new ColorRgb(1, 1, 0));
        yLabelImageSelected = calculateLabelImage("Y", new ColorRgb(1, 1, 0));
        zLabelImageSelected = calculateLabelImage("Z", new ColorRgb(1, 1, 0));

        font = new Font("Arial", Font.PLAIN, 14);

        this.renderMode = RENDER_MODE_ZBUFFER;

        showGrid = true;
    }

    /**
    Do NOT call your controller from the `keyTyped` method, or the controller
    will be invoked twice for each key. Call it only from the `keyPressed` and
    `keyReleased` method.
    */
    public void keyTyped(KeyEvent e) {
        ;
    }

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

        if ( unicode_id != e.CHAR_UNDEFINED && !skipKey ) {
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

    public void keyReleased(KeyEvent e) {
    }

    public int getRenderMode()
    {
        return renderMode;
    }

    public double getViewportStartXPercent()
    {
        return viewportStartXPercent;
    }

    public double getViewportStartYPercent()
    {
        return viewportStartYPercent;
    }

    public double getViewportSizeXPercent()
    {
        return viewportSizeXPercent;
    }

    public double getViewportSizeYPercent()
    {
        return viewportSizeYPercent;
    }

    public void setViewportStartXPercent(double viewportStartXPercent)
    {
        this.viewportStartXPercent = viewportStartXPercent;
    }

    public void setViewportStartYPercent(double viewportStartYPercent)
    {
        this.viewportStartYPercent = viewportStartYPercent;
    }

    public void setViewportSizeXPercent(double viewportSizeXPercent)
    {
        this.viewportSizeXPercent = viewportSizeXPercent;
    }

    public void setViewportSizeYPercent(double viewportSizeYPercent)
    {
        this.viewportSizeYPercent = viewportSizeYPercent;
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

    public void setSelected(boolean val)
    {
        selected = val;
    }

    public boolean getSelected()
    {
        return selected;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean val)
    {
        active = val;
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
    (canvasXSize-1, canvasYSize-1). Current JoglView is defined inside that
    area in terms of a start point (upper left corner) and size given in
    area percentages.
    This method calculates the (viewportStartX, viewportStartY,
    viewportSizeX, viewportSizeY) variables that defines current JoglView
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
        camera.updateViewportResize(viewportSizeX, viewportSizeY);
    }

    public void activateViewportGL(GL gl, int canvasXSize, int canvasYSize)
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
    public void drawBorderGL(GL gl, int viewportXsize, int viewportYsize)
    {
        if ( !active ) {
            return;
        }
        gl.glPushAttrib(gl.GL_DEPTH_TEST);
        gl.glPushAttrib(gl.GL_TEXTURE_2D);
        gl.glPushAttrib(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glDisable(gl.GL_DEPTH_TEST);

        double x1, y1, x2, y2;
        double epsilonx = 2.0 / ((double)viewportXsize);
        double epsilony = 2.0 / ((double)viewportYsize);
        double dx = (viewportBorder) * epsilonx;
        double dy = (viewportBorder) * epsilony;

        x1 = viewportStartXPercent*2 - 1;
        y1 = viewportStartYPercent*2 - 1;
        x2 = x1 + viewportSizeXPercent*2;
        y2 = y1 + viewportSizeYPercent*2;

        gl.glPolygonMode(gl.GL_FRONT_AND_BACK, gl.GL_FILL);
        gl.glBegin(gl.GL_QUADS);
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

    public RGBAImage calculateLabelImage(String label, ColorRgb color)
    {
        RGBAImage labelImage;

        //-----------------------------------------------------------------
        labelImage = new RGBAImage();
        labelImage.init(200, 30);

        BufferedImage bi;
        Graphics offlineContext;

        bi = AwtRGBAImageRenderer.exportToAwtBufferedImage(labelImage);
        offlineContext = bi.getGraphics();
        FontMetrics fm = offlineContext.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(label, offlineContext);
        Rectangle ri = r.getBounds();

        //-----------------------------------------------------------------
        labelImage.init(ri.width-ri.x+10, (ri.height-ri.y)/2+5);
        bi = AwtRGBAImageRenderer.exportToAwtBufferedImage(labelImage);
        offlineContext = bi.getGraphics();

        //-----------------------------------------------------------------
        offlineContext.setColor(
            new Color((float)color.r, (float)color.g, (float)color.b));
        offlineContext.setFont(font);
        offlineContext.drawString(label, -ri.x, -ri.y);

        AwtRGBAImageRenderer.importFromAwtBufferedImage(bi, labelImage);
        return labelImage;
    }

    public void setTitle(String name)
    {
        title = new String(name);
        titleImage = calculateLabelImage(title, new ColorRgb(0.76, 0.76, 0.76));
    }

    public void drawReferenceBase(GL gl)
    {
        //-----------------------------------------------------------------
        int basesize = 64;
        gl.glPushAttrib(gl.GL_VIEWPORT_BIT);
        gl.glPushAttrib(gl.GL_DEPTH_TEST);
        gl.glPushAttrib(gl.GL_TEXTURE_2D);
        gl.glPushAttrib(gl.GL_LIGHTING);
        gl.glViewport(viewportStartX, viewportStartY, basesize, basesize);

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glPushMatrix();

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glDisable(gl.GL_DEPTH_TEST);

        //-----------------------------------------------------------------
        Matrix4x4 R = camera.getRotation();

        gl.glLoadIdentity();
        R.invert();
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
        gl.glBegin(gl.GL_LINES);
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
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(gl.GL_MODELVIEW);

        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
        gl.glPopAttrib();
    }

    private void drawGridRectangle(GL gl)
    {
        gl.glPushMatrix();

        Matrix4x4 R;
        double yaw, pitch;

        R = camera.getRotation();
        yaw = Math.toDegrees(R.obtainEulerYawAngle());
        pitch = Math.toDegrees(R.obtainEulerPitchAngle());

        if ( camera.getProjectionMode() == camera.PROJECTION_MODE_ORTHOGONAL &&
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

        gl.glDisable(gl.GL_LIGHTING);
        gl.glDisable(gl.GL_TEXTURE_2D);
        gl.glLineWidth(1.0f);
        gl.glBegin(GL.GL_LINES);
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

    public void drawGrid(GL gl)
    {
        //- Draw reference grid plane -------------------------------------
        if ( showGrid ) drawGridRectangle(gl);
    }

    public void drawTextureString2D(GL gl, int x, int y, RGBAImage i)
    {
        double dx;
        double dy;

        dx = ((double)(2*x)) / ((double)viewportSizeX);
        dy = ((double)(2*(viewportSizeY - y))) / ((double)viewportSizeY);

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glTranslated(dx, dy, 0);

        drawTextureString3D(gl, i);

        gl.glPopMatrix();
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glPopMatrix();
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }

    private void drawTextureString3D(GL gl, RGBAImage i)
    {
        gl.glRasterPos3d(-1, -1, 0);
        gl.glEnable(gl.GL_TEXTURE_2D);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // First: activate texture, Second: set texture parameters
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);
        gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
        gl.glTexEnvf(gl.GL_TEXTURE_ENV, gl.GL_TEXTURE_ENV_MODE, gl.GL_BLEND);
        JoglImageRenderer.draw(gl, i);
    }

    public void drawTitle(GL gl)
    {
        int borderx = 4;
        int bordery = 1;
        drawTextureString2D(gl, borderx, titleImage.getYSize() + bordery, titleImage);
    }

    public boolean inside(double x, double y)
    {
        if ( x >= viewportStartXPercent &&
             x <= viewportStartXPercent + viewportSizeXPercent &&
             y >= viewportStartYPercent &&
             y <= viewportStartYPercent + viewportSizeYPercent ) {
            return true;
        }
        return false;
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

    public void drawLabelsForTranslateGizmo(GL gl, TranslateGizmo gizmo)
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

                lv.x = lv.y = 0;
                if ( g instanceof Arrow ) {
                    Arrow a = ((Arrow)g);
                    lv.z = (a.getHeadLength() + a.getBaseLength()) * 1.1;
                }
                else {
                    lv.z = 1;
                }

                R = new Matrix4x4();
                R.translation(r.getPosition());
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
                        drawTextureString2D(gl, (int)tp.x-3, (int)tp.y+12,
                            xLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x-3, (int)tp.y+12,
                            xLabelImage);
                    }
                    break;
                  case 1:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x-3, (int)tp.y+12,
                            yLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x-3, (int)tp.y+12,
                            yLabelImage);
                    }
                    break;
                  case 2:
                    if ( yellow ) {
                        drawTextureString2D(gl, (int)tp.x-3, (int)tp.y+12,
                            zLabelImageSelected);
                    }
                    else {
                        drawTextureString2D(gl, (int)tp.x-3, (int)tp.y+12,
                            zLabelImage);
                    }
                    break;
                }
                gl.glPopMatrix();
            }
        }
    }
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
