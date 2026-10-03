package render.awt;

// AWT classes
import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;

/**
Heavyweight AWT component presenting a viewport set drawn by an
`AwtDrawingAreaRenderer`: the AWT counterpart of the JOGL `GLCanvas`. Each
`Viewport` of the set is an area of this canvas (see
`AwtViewportSetRenderer`), so the interaction over the set works exactly as
over the OpenGL canvas.

Frames are composed off-screen by the renderer and copied at once, so the
canvas is never cleared before painting (no flicker).
*/
public class AwtViewportSetCanvas extends Canvas
{
    private AwtDrawingAreaRenderer renderer;

    public AwtViewportSetCanvas()
    {
        this.renderer = null;
    }

    /**
    @param renderer draws the frames; until it is given, nothing is drawn
    */
    public void setRenderer(AwtDrawingAreaRenderer renderer)
    {
        this.renderer = renderer;
    }

    /**
    The frame covers the whole canvas: it is not cleared first.
    @param g
    */
    @Override
    public void update(Graphics g)
    {
        paint(g);
    }

    @Override
    public void paint(Graphics g)
    {
        if ( renderer == null ) {
            return;
        }
        renderer.display((Graphics2D)g, getWidth(), getHeight(), getPixelScale());
    }

    /**
    Draws a frame now, in the calling thread (as `GLCanvas.display` does), so
    the requests of the frame (i.e. exports to files) are done on return.
    Must be called from the event dispatch thread.
    */
    public void display()
    {
        Graphics g = getGraphics();

        if ( g == null ) {
            return;
        }
        try {
            paint(g);
        }
        finally {
            g.dispose();
        }
    }

    /**
    @return physical pixels for each logical pixel of the screen showing the
    canvas (i.e. 2 on high density displays)
    */
    private double getPixelScale()
    {
        GraphicsConfiguration configuration = getGraphicsConfiguration();

        if ( configuration == null ) {
            return 1.0;
        }
        return configuration.getDefaultTransform().getScaleX();
    }
}
