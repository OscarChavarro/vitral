package framework.render.jogl4;

import java.util.IdentityHashMap;
import java.util.Map;

import com.jogamp.opengl.GL2;

import framework.model.Viewport;
import framework.model.ViewportSet;

/**
JOGL presentation of a `ViewportSet`: it activates the GL viewport of each
`Viewport`, draws the lines bordering them (highlighting the selected one)
and delegates the drawing of the scene inside each viewport to a
`ViewRenderer`.

There is one renderer for each `ViewportSet`, so an application working with
several displays uses one renderer per display. The model is injected in the
constructor, and it is not modified by the rendering, except for the pixel
areas of the viewports, which depend on the GL surface size.
*/
public class JoglViewportSetRenderer
{
    public interface ViewRenderer
    {
        void configureView(Jogl4ViewportWindow view);
        void drawView(GL2 gl, Jogl4ViewportWindow view);
    }

    private final ViewportSet viewportSet;
    private final ViewRenderer viewRenderer;
    private final Jogl4LabelImageProvider labelImageProvider;
    private final Map<Viewport, Jogl4ViewportWindow> windows;

    public JoglViewportSetRenderer(ViewportSet viewportSet,
                                   Jogl4LabelImageProvider labelImageProvider,
                                   ViewRenderer viewRenderer)
    {
        this.viewportSet = viewportSet;
        this.labelImageProvider = labelImageProvider;
        this.viewRenderer = viewRenderer;
        this.windows = new IdentityHashMap<Viewport, Jogl4ViewportWindow>();
    }

    public ViewportSet getViewportSet()
    {
        return viewportSet;
    }

    /**
    @param viewport a viewport of the rendered set
    @return the JOGL window holding the drawing resources of the viewport
    */
    public Jogl4ViewportWindow getWindow(Viewport viewport)
    {
        Jogl4ViewportWindow window = windows.get(viewport);

        if ( window == null ) {
            window = new Jogl4ViewportWindow(viewportSet, viewport, labelImageProvider);
            windows.put(viewport, window);
        }
        return window;
    }

    /**
    @return the window of the selected viewport, or null if the set is empty
    */
    public Jogl4ViewportWindow getSelectedWindow()
    {
        Viewport viewport = viewportSet.getSelectedViewport();

        if ( viewport == null ) {
            return null;
        }
        return getWindow(viewport);
    }

    /**
    PRE: glViewport is set to the full set area, and projection and modelview
    matrices are set to identity.
    @param gl
    @param fullScreenGuiMode true if the GUI is in full screen mode
    */
    public void draw(GL2 gl, boolean fullScreenGuiMode)
    {
        forgetRemovedViewports();

        if ( viewportSet.countActiveViewports() == 1 && fullScreenGuiMode ) {
            drawSelectedViewFullScreen(gl);
        }
        else {
            drawMultipleViews(gl);
        }
    }

    private void forgetRemovedViewports()
    {
        if ( windows.size() > viewportSet.getViewportCount() ) {
            windows.keySet().retainAll(viewportSet.getViewports());
        }
    }

    private void drawMultipleViews(GL2 gl)
    {
        for ( Viewport viewport : viewportSet.getViewports() ) {
            drawBorder(gl, viewport);
        }

        for ( Viewport viewport : viewportSet.getViewports() ) {
            if ( !viewport.isActive() ) {
                continue;
            }

            Jogl4ViewportWindow view = getWindow(viewport);
            activateViewport(gl, viewport);
            if ( view.isSelected() ) {
                viewRenderer.configureView(view);
            }
            viewRenderer.drawView(gl, view);
            view.drawTitle(gl);
        }
    }

    private void drawSelectedViewFullScreen(GL2 gl)
    {
        for ( Viewport viewport : viewportSet.getViewports() ) {
            if ( !viewport.isActive() || !viewportSet.isSelected(viewport) ) {
                continue;
            }

            Jogl4ViewportWindow view = getWindow(viewport);
            activateViewport(gl, viewport);
            gl.glViewport(0, 0, viewportSet.getSizeXInPixels(), viewportSet.getSizeYInPixels());
            viewRenderer.configureView(view);
            viewRenderer.drawView(gl, view);
            view.drawTitle(gl);
        }
    }

    private void activateViewport(GL2 gl, Viewport viewport)
    {
        viewport.updatePixelArea(viewportSet.getSizeXInPixels(), viewportSet.getSizeYInPixels());
        gl.glViewport(viewport.getPixelStartX(), viewport.getPixelStartY(),
            viewport.getPixelSizeX(), viewport.getPixelSizeY());
    }

    /**
    Draws the line bordering a viewport, in a highlight color if the viewport
    is the selected one.
    PRE: glViewport is set to the full set area, and projection and modelview
    matrices are set to identity.
    */
    private void drawBorder(GL2 gl, Viewport viewport)
    {
        if ( !viewport.isActive() || viewport.getBorder() <= 0 ) {
            return;
        }
        gl.glPushAttrib(GL2.GL_DEPTH_TEST);
        gl.glPushAttrib(GL2.GL_TEXTURE_2D);
        gl.glPushAttrib(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glDisable(GL2.GL_TEXTURE_2D);
        gl.glDisable(GL2.GL_DEPTH_TEST);

        double x1;
        double y1;
        double x2;
        double y2;
        double epsilonx = 2.0 / ((double)viewportSet.getSizeXInPixels());
        double epsilony = 2.0 / ((double)viewportSet.getSizeYInPixels());
        double dx = viewport.getBorder() * epsilonx;
        double dy = viewport.getBorder() * epsilony;

        x1 = viewport.getStartXPercent()*2 - 1;
        y1 = viewport.getStartYPercent()*2 - 1;
        x2 = x1 + viewport.getSizeXPercent()*2;
        y2 = y1 + viewport.getSizeYPercent()*2;

        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        gl.glBegin(GL2.GL_QUADS);
            if ( viewportSet.isSelected(viewport) ) {
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
}
