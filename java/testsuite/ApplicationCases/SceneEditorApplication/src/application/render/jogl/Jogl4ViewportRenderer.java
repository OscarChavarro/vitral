package application.render.jogl;

import com.jogamp.opengl.GL2;
import vsdk.toolkit.gui.ViewportWindowSetManager;

public class Jogl4ViewportRenderer
{
    public interface ViewRenderer
    {
        void configureView(JoglAwtViewportWindow view);
        void drawView(GL2 gl, JoglAwtViewportWindow view);
    }

    private final ViewportWindowSetManager viewOrganizer;
    private final ViewRenderer viewRenderer;

    public Jogl4ViewportRenderer(ViewportWindowSetManager viewOrganizer,
                                 ViewRenderer viewRenderer)
    {
        this.viewOrganizer = viewOrganizer;
        this.viewRenderer = viewRenderer;
    }

    public void draw(GL2 gl, boolean fullScreenGuiMode)
    {
        int n = viewOrganizer.countActiveViews();

        if ( n == 1 && fullScreenGuiMode ) {
            drawSelectedViewFullScreen(gl);
        }
        else {
            drawMultipleViews(gl);
        }
    }

    private void drawMultipleViews(GL2 gl)
    {
        JoglAwtViewportWindow view;
        int i;

        for ( i = 0; i < viewOrganizer.getViews().size(); i++ ) {
            view = (JoglAwtViewportWindow)viewOrganizer.getViews().get(i);
            view.drawBorderGL(gl, viewOrganizer.getGlobalViewportXSize(), viewOrganizer.getGlobalViewportYSize());
        }

        for ( i = 0; i < viewOrganizer.getViews().size(); i++ ) {
            view = (JoglAwtViewportWindow)viewOrganizer.getViews().get(i);

            if ( !view.isActive() ) {
                continue;
            }

            view.activateViewportGL(gl, viewOrganizer.getGlobalViewportXSize(), viewOrganizer.getGlobalViewportYSize());
            if ( view.isSelected() ) {
                viewRenderer.configureView(view);
            }
            viewRenderer.drawView(gl, view);
            view.drawTitle(gl);
        }
    }

    private void drawSelectedViewFullScreen(GL2 gl)
    {
        JoglAwtViewportWindow view;
        int i;

        for ( i = 0; i < viewOrganizer.getViews().size(); i++ ) {
            view = (JoglAwtViewportWindow)viewOrganizer.getViews().get(i);

            if ( !view.isActive() || !view.isSelected() ) {
                continue;
            }

            view.activateViewportGL(gl, viewOrganizer.getGlobalViewportXSize(), viewOrganizer.getGlobalViewportYSize());
            gl.glViewport(0, 0, viewOrganizer.getGlobalViewportXSize(), viewOrganizer.getGlobalViewportYSize());
            viewRenderer.configureView(view);
            viewRenderer.drawView(gl, view);
            view.drawTitle(gl);
        }
    }
}
