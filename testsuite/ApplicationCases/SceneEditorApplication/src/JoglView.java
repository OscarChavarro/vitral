//===========================================================================

// AWT/Swing classes
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

// JOGL classes
import javax.media.opengl.GL;

// VSDK classes
import vsdk.toolkit.common.Vector3D;
import vsdk.toolkit.common.Matrix4x4;
import vsdk.toolkit.environment.Camera;

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
    protected double viewportStartXPercent;
    protected double viewportStartYPercent;
    protected double viewportSizeXPercent;
    protected double viewportSizeYPercent;

    // Current configuration, available only after a call to activateViewportGL
    protected int viewportStartX;
    protected int viewportStartY;
    protected int viewportSizeX;
    protected int viewportSizeY;
    protected int viewportBorder; // In pixels

    // A JoglView can request an specific size in pixels. If this size gets
    // smaller than percent-based area, the viewport is assigned to match the
    // requested size. If a requested size dimension in pixels is greater
    // than the percent-based area, the requested size is ignored and the
    // viewport will get the percent-based size. The smaller requested size
    // viewport will always be centered inside the given percent-based area.
    // A requested size of 0 means the requested size match the percent-based
    // size.
    protected int viewportRequestedSizeXInPixels;
    protected int viewportRequestedSizeYInPixels;

    // Every JoglView has an internal camera
    protected Camera camera;

    //
    protected boolean selected;
    protected boolean active;

    public JoglView()
    {
        viewportStartXPercent = 0.0;
        viewportStartYPercent = 0.0;
        viewportSizeXPercent = 1.0;
        viewportSizeYPercent = 1.0;
        viewportRequestedSizeXInPixels = 0;
        viewportRequestedSizeYInPixels = 0;
        viewportBorder = 2;

        Matrix4x4 R = new Matrix4x4();
        R.eulerAnglesRotation(Math.toRadians(45), Math.toRadians(-35), 0);
        camera = new Camera();
        camera.setPosition(new Vector3D(-5, -5, 5));
        camera.setRotation(R);

        selected = true;
        active = true;
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

        if ( unicode_id != e.CHAR_UNDEFINED && !skipKey ) {
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
        }
    }

    public void keyReleased(KeyEvent e) {
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

        viewportStartX = (int)(viewportStartXPercent*((double)canvasXSize))+viewportBorder;
        viewportStartY = (int)(viewportStartYPercent*((double)canvasYSize))+viewportBorder;
        subCanvasXSize = (int)(viewportSizeXPercent*((double)canvasXSize))-2*viewportBorder;
        subCanvasYSize = (int)(viewportSizeYPercent*((double)canvasYSize))-2*viewportBorder;
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
            gl.glVertex3d(x1+dx, y1+dy, 0);
            gl.glVertex3d(x2-dx, y1+dy, 0);
            gl.glVertex3d(x2-dx, y2-dy, 0);
            gl.glVertex3d(x1+dx, y2-dy, 0);
        gl.glEnd();
        gl.glPopAttrib();
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
}

//===========================================================================
//= EOF                                                                     =
//===========================================================================
