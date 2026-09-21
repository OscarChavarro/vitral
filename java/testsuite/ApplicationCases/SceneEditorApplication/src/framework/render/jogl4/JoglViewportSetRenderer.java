package framework.render.jogl4;

import java.util.IdentityHashMap;
import java.util.Map;

import com.jogamp.opengl.GL4;

import vsdk.toolkit.common.linealAlgebra.Matrix4x4d;
import vsdk.toolkit.render.jogl.Jogl4ColoredPrimitiveRenderer;
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
        void drawView(GL4 gl, Jogl4ViewportWindow view);
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
    PRE: glViewport is set to the full set area.
    @param gl
    @param fullScreenGuiMode true if the GUI is in full screen mode
    */
    public void draw(GL4 gl, boolean fullScreenGuiMode)
    {
        forgetRemovedViewports();

        if ( viewportSet.countActiveViewports() == 1 && fullScreenGuiMode ) {
            drawSelectedViewFullScreen(gl);
        }
        else {
            drawMultipleViews(gl);
        }
    }

    /**
    Releases the OpenGL resources of all the windows.
    PRE: the OpenGL context is current and about to be destroyed (i.e. from
    `GLEventListener.dispose`).
    @param gl
    */
    public void disposeGlResources(GL4 gl)
    {
        for ( Jogl4ViewportWindow window : windows.values() ) {
            window.disposeGlResources(gl);
        }
    }

    /**
    Forgets the OpenGL resources of all the windows, because the context that
    owned them no longer exists (i.e. from `GLEventListener.init` of a new
    context, as happens when the GUI is rebuilt). They are created again when
    needed.
    */
    public void invalidateGlResources()
    {
        for ( Jogl4ViewportWindow window : windows.values() ) {
            window.invalidateGlResources();
        }
    }

    private void forgetRemovedViewports()
    {
        if ( windows.size() > viewportSet.getViewportCount() ) {
            windows.keySet().retainAll(viewportSet.getViewports());
        }
    }

    private void drawMultipleViews(GL4 gl)
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

    private void drawSelectedViewFullScreen(GL4 gl)
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

    private void activateViewport(GL4 gl, Viewport viewport)
    {
        viewport.updatePixelArea(viewportSet.getSizeXInPixels(), viewportSet.getSizeYInPixels());
        gl.glViewport(viewport.getPixelStartX(), viewport.getPixelStartY(),
            viewport.getPixelSizeX(), viewport.getPixelSizeY());
    }

    /**
    Draws the line bordering a viewport, in a highlight color if the viewport
    is the selected one.
    PRE: glViewport is set to the full set area.
    */
    private void drawBorder(GL4 gl, Viewport viewport)
    {
        if ( !viewport.isActive() || viewport.getBorder() <= 0 ) {
            return;
        }
        double epsilonx = 2.0 / ((double)viewportSet.getSizeXInPixels());
        double epsilony = 2.0 / ((double)viewportSet.getSizeYInPixels());
        double dx = viewport.getBorder() * epsilonx;
        double dy = viewport.getBorder() * epsilony;
        double x1 = viewport.getStartXPercent()*2 - 1;
        double y1 = viewport.getStartYPercent()*2 - 1;
        double x2 = x1 + viewport.getSizeXPercent()*2;
        double y2 = y1 + viewport.getSizeYPercent()*2;
        float[] outer = viewportSet.isSelected(viewport)
            ? new float[] {1.0f, 0.96f, 0.0f}
            : new float[] {0.21f, 0.25f, 0.29f};
        float[] positions = new float[] {
            (float)x1, (float)y1, 0,
            (float)x2, (float)y1, 0,
            (float)x2, (float)y2, 0,
            (float)x1, (float)y1, 0,
            (float)x2, (float)y2, 0,
            (float)x1, (float)y2, 0,
            (float)(x1+2*dx), (float)(y1+2*dy), 0,
            (float)(x2-2*dx), (float)(y1+2*dy), 0,
            (float)(x2-2*dx), (float)(y2-2*dy), 0,
            (float)(x1+2*dx), (float)(y1+2*dy), 0,
            (float)(x2-2*dx), (float)(y2-2*dy), 0,
            (float)(x1+2*dx), (float)(y2-2*dy), 0
        };
        float[] colors = new float[12 * 4];

        for ( int i = 0; i < 12; i++ ) {
            float[] c = i < 6 ? outer : new float[] {0, 0, 0};
            colors[4*i] = c[0];
            colors[4*i + 1] = c[1];
            colors[4*i + 2] = c[2];
            colors[4*i + 3] = 1.0f;
        }
        gl.glDisable(GL4.GL_DEPTH_TEST);
        gl.glDisable(GL4.GL_CULL_FACE);
        gl.glPolygonMode(GL4.GL_FRONT_AND_BACK, GL4.GL_FILL);
        Jogl4ColoredPrimitiveRenderer.draw(gl, Matrix4x4d.identityMatrix(),
            GL4.GL_TRIANGLES, positions, colors);
        gl.glEnable(GL4.GL_DEPTH_TEST);
    }
}
