package render.awt;

// AWT classes
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;

// VSDK classes
import vsdk.toolkit.gui.viewport.Viewport;
import vsdk.toolkit.gui.viewport.ViewportElementScaler;
import vsdk.toolkit.gui.viewport.ViewportSet;

/**
AWT presentation of a `ViewportSet`, the 2D counterpart of
`Jogl4ViewportSetRenderer`: it draws the line bordering each viewport
(highlighting the selected one), restricts the drawing to the area of each
viewport (with a clip region, as `glViewport` + scissor do in OpenGL), draws
the title of each viewport and delegates the drawing of the scene inside each
viewport to a `ViewRenderer`.

Viewports measure their pixel areas with the origin at the lower left corner
of the set (as OpenGL does), while AWT has it at the upper left corner: this
class converts between both, so view renderers receive AWT rectangles.
*/
public class AwtViewportSetRenderer
{
    /**
    Draws the scene inside one viewport.
    */
    public interface ViewRenderer
    {
        /**
        Called before drawing the selected viewport (or the only one shown).
        @param viewport
        */
        void configureView(Viewport viewport);

        /**
        Draws a viewport. The graphics are clipped to its area.
        @param g graphics of the whole set
        @param viewport viewport to draw
        @param x left pixel of the viewport area, in AWT coordinates
        @param y top pixel of the viewport area, in AWT coordinates
        @param width width of the viewport area, in pixels
        @param height height of the viewport area, in pixels
        */
        void drawView(Graphics2D g, Viewport viewport, int x, int y, int width,
                      int height);
    }

    private static final int BASE_TITLE_FONT_SIZE = 14;
    private static final int BASE_TITLE_BORDER_X = 4;
    private static final int BASE_TITLE_BORDER_Y = 1;
    private static final Color SELECTED_BORDER_COLOR = new Color(1.0f, 0.96f, 0.0f);
    private static final Color BORDER_COLOR = new Color(0.21f, 0.25f, 0.29f);

    private final ViewportSet viewportSet;
    private final ViewRenderer viewRenderer;

    /**
    @param viewportSet set to present
    @param viewRenderer draws the scene inside each viewport
    */
    public AwtViewportSetRenderer(ViewportSet viewportSet, ViewRenderer viewRenderer)
    {
        this.viewportSet = viewportSet;
        this.viewRenderer = viewRenderer;
    }

    /**
    Draws the whole set. PRE: the set has the size of the area of `g`.
    @param g graphics of the area of the set
    @param fullScreenGuiMode true if the GUI is in full screen mode
    */
    public void draw(Graphics2D g, boolean fullScreenGuiMode)
    {
        if ( viewportSet.countActiveViewports() == 1 && fullScreenGuiMode ) {
            drawSelectedViewFullScreen(g);
        }
        else {
            drawMultipleViews(g);
        }
    }

    private void drawMultipleViews(Graphics2D g)
    {
        for ( Viewport viewport : viewportSet.getViewports() ) {
            drawBorder(g, viewport);
        }

        for ( Viewport viewport : viewportSet.getViewports() ) {
            if ( !viewport.isActive() ) {
                continue;
            }
            viewport.updatePixelArea(viewportSet.getSizeXInPixels(),
                viewportSet.getSizeYInPixels());
            int x = viewport.getPixelStartX();
            int y = toAwtY(viewport.getPixelStartY() + viewport.getPixelSizeY());
            if ( viewportSet.isSelected(viewport) ) {
                viewRenderer.configureView(viewport);
            }
            drawClippedView(g, viewport, x, y, viewport.getPixelSizeX(),
                viewport.getPixelSizeY());
        }
    }

    private void drawSelectedViewFullScreen(Graphics2D g)
    {
        for ( Viewport viewport : viewportSet.getViewports() ) {
            if ( !viewport.isActive() || !viewportSet.isSelected(viewport) ) {
                continue;
            }
            viewport.updatePixelArea(viewportSet.getSizeXInPixels(),
                viewportSet.getSizeYInPixels());
            viewRenderer.configureView(viewport);
            drawClippedView(g, viewport, 0, 0, viewportSet.getSizeXInPixels(),
                viewportSet.getSizeYInPixels());
        }
    }

    private void drawClippedView(Graphics2D g, Viewport viewport, int x, int y,
                                 int width, int height)
    {
        if ( width <= 0 || height <= 0 ) {
            return;
        }
        Shape previousClip = g.getClip();
        g.clipRect(x, y, width, height);
        viewRenderer.drawView(g, viewport, x, y, width, height);
        drawTitle(g, viewport, x, y);
        g.setClip(previousClip);
    }

    /**
    Draws the area of a viewport (border included) in the border color: the
    viewport drawing covers it except for a frame of the border width.
    */
    private void drawBorder(Graphics2D g, Viewport viewport)
    {
        if ( !viewport.isActive() || viewport.getBorder() <= 0 ) {
            return;
        }
        double setWidth = viewportSet.getSizeXInPixels();
        double setHeight = viewportSet.getSizeYInPixels();
        int x1 = (int)Math.round(viewport.getStartXPercent() * setWidth);
        int x2 = (int)Math.round((viewport.getStartXPercent() +
            viewport.getSizeXPercent()) * setWidth);
        int y1 = toAwtY((int)Math.round((viewport.getStartYPercent() +
            viewport.getSizeYPercent()) * setHeight));
        int y2 = toAwtY((int)Math.round(viewport.getStartYPercent() * setHeight));

        g.setColor(viewportSet.isSelected(viewport) ? SELECTED_BORDER_COLOR : BORDER_COLOR);
        g.fillRect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
    Draws the title of a viewport at its upper left corner, informing the
    viewport of the area it occupies (for interaction).
    */
    private void drawTitle(Graphics2D g, Viewport viewport, int x, int y)
    {
        ViewportElementScaler elementScaler = viewportSet.getElementScaler();
        int borderX = elementScaler.scaleSize(BASE_TITLE_BORDER_X);
        int borderY = elementScaler.scaleSize(BASE_TITLE_BORDER_Y);
        int fontSize = elementScaler.scaleSize(BASE_TITLE_FONT_SIZE);
        String title = viewportSet.getTitleFor(viewport);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        FontMetrics metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth(title);
        int textHeight = metrics.getAscent() + metrics.getDescent();

        viewport.setTitleArea(0, 0, textWidth + 2*borderX, textHeight + 2*borderY);
        g.setColor(AwtProjectedLineLayerRenderer.toAwtColor(
            viewportSet.getTitleColorFor(viewport)));
        g.drawString(title, x + borderX, y + borderY + metrics.getAscent());
    }

    /**
    @param y vertical pixel coordinate with origin at the bottom of the set
    @return the same coordinate with origin at the top of the set
    */
    private int toAwtY(int y)
    {
        return viewportSet.getSizeYInPixels() - y;
    }
}
