package vsdk.toolkit.render.gizmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vsdk.toolkit.common.color.ColorRgb;

/**
Technology independent list of 2D primitives that presents gizmos over a
viewport, for drawing surfaces without a 3D API (an AWT canvas, a raster
image, a BGI/VGA frame buffer...). It is the output of the gizmo painters of
this package (`TranslateGizmo2DPainter`, `RotateGizmo2DPainter`,
`ScaleGizmo2DPainter` and `InputGizmo2DPainter`), already projected,
clipped and sorted, so a backend only has to know how to:

- fill a simple polygon with a color and an opacity,
- draw a line with a color and a width,
- draw a text with a color and a font size.

Coordinates are pixels of the viewport, with the origin at its upper left
corner and growing downwards (as AWT, BGI and most frame buffers do).

As there is no depth buffer, hidden surfaces are solved with the painter's
algorithm [FOLE1992].15.5: primitives coming from 3D elements (`isOverlay`
false) keep a depth (the mean of the normalized device depth of their
vertices) and `getPrimitivesInDrawingOrder` sorts them from far to near;
overlay primitives (input boxes, labels) are drawn after them, in the order
they were added. Primitives at the same depth keep the order they were
added, so translucent elements added after the opaque ones stay over them.
*/
public class Gizmo2DDrawList
{
    /**
    Kind of a primitive.
    */
    public enum Kind
    {
        /** Filled simple polygon (convex or not), of at least 3 vertices */
        POLYGON,
        /** Line from the vertex 0 to the vertex 1, of `getWidth()` pixels */
        LINE,
        /** Text, positioned following `getAlignment()` */
        LABEL
    }

    /**
    How the position of a label relates to its text.
    */
    public enum Alignment
    {
        /** The position is the lower left corner of the box of the text */
        LOWER_LEFT,
        /** The position is the center of the box of the text */
        CENTER
    }

    /**
    One primitive of the list. Instances are created by the `add*` methods of
    the list.
    */
    public static final class Primitive
    {
        private final Kind kind;
        private final double[] x;
        private final double[] y;
        private final ColorRgb color;
        private final double opacity;
        private final double width;
        private final double depth;
        private final boolean overlay;
        private final String text;
        private final int fontSize;
        private final Alignment alignment;

        private Primitive(Kind kind, double[] x, double[] y, ColorRgb color,
                          double opacity, double width, double depth,
                          boolean overlay, String text, int fontSize,
                          Alignment alignment)
        {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.color = new ColorRgb(color);
            this.opacity = opacity;
            this.width = width;
            this.depth = depth;
            this.overlay = overlay;
            this.text = text;
            this.fontSize = fontSize;
            this.alignment = alignment;
        }

        public Kind getKind() { return kind; }

        /** @return number of vertices (1 for labels) */
        public int getNumberOfVertices() { return x.length; }

        public double getX(int vertex) { return x[vertex]; }

        public double getY(int vertex) { return y[vertex]; }

        public ColorRgb getColor() { return color; }

        /** @return opacity, from 0 (invisible) to 1 (opaque) */
        public double getOpacity() { return opacity; }

        /** @return width of a line, in pixels */
        public double getWidth() { return width; }

        /** @return normalized device depth (bigger is farther) */
        public double getDepth() { return depth; }

        /**
        @return true for screen space primitives (input boxes, labels), drawn
        after the depth sorted ones and never sorted
        */
        public boolean isOverlay() { return overlay; }

        public String getText() { return text; }

        /** @return font size of a label, in pixels */
        public int getFontSize() { return fontSize; }

        public Alignment getAlignment() { return alignment; }
    }

    private final List<Primitive> primitives;

    public Gizmo2DDrawList()
    {
        primitives = new ArrayList<Primitive>();
    }

    public void clear()
    {
        primitives.clear();
    }

    public boolean isEmpty()
    {
        return primitives.isEmpty();
    }

    /**
    Adds a polygon coming from a 3D element, sorted by depth.
    */
    public void addPolygon(double[] x, double[] y, double depth, ColorRgb color,
                           double opacity)
    {
        primitives.add(new Primitive(Kind.POLYGON, x.clone(), y.clone(), color,
            opacity, 0, depth, false, null, 0, null));
    }

    /**
    Adds a line coming from a 3D element, sorted by depth.
    */
    public void addLine(double x0, double y0, double x1, double y1, double depth,
                        ColorRgb color, double width)
    {
        primitives.add(new Primitive(Kind.LINE, new double[] {x0, x1},
            new double[] {y0, y1}, color, 1.0, width, depth, false, null, 0, null));
    }

    /**
    Adds a screen space line (overlay), i.e. a line whose visibility was
    already solved (as the hidden line removal of a HUD).
    */
    public void addOverlayLine(double x0, double y0, double x1, double y1,
                               ColorRgb color, double width)
    {
        primitives.add(new Primitive(Kind.LINE, new double[] {x0, x1},
            new double[] {y0, y1}, color, 1.0, width, 0, true, null, 0, null));
    }

    /**
    Adds an opaque screen space rectangle (overlay).
    @param x left pixel
    @param y top pixel
    @param width width, in pixels
    @param height height, in pixels
    @param color color to fill it with
    */
    public void addOverlayRectangle(double x, double y, double width,
                                    double height, ColorRgb color)
    {
        primitives.add(new Primitive(Kind.POLYGON,
            new double[] {x, x + width, x + width, x},
            new double[] {y, y, y + height, y + height},
            color, 1.0, 0, 0, true, null, 0, null));
    }

    /**
    Adds a screen space text (overlay).
    @param x horizontal position, in pixels (see `alignment`)
    @param y vertical position, in pixels (see `alignment`)
    @param text text to draw
    @param color color of the text
    @param fontSize size of the font, in pixels
    @param alignment how (x, y) relates to the box of the text
    */
    public void addOverlayLabel(double x, double y, String text, ColorRgb color,
                                int fontSize, Alignment alignment)
    {
        primitives.add(new Primitive(Kind.LABEL, new double[] {x},
            new double[] {y}, color, 1.0, 0, 0, true, text, fontSize, alignment));
    }

    /**
    @return the primitives in the order they must be drawn: the depth sorted
    ones from far to near, then the overlays in the order they were added
    */
    public List<Primitive> getPrimitivesInDrawingOrder()
    {
        List<Primitive> sorted = new ArrayList<Primitive>();
        List<Primitive> overlays = new ArrayList<Primitive>();

        for ( Primitive p : primitives ) {
            if ( p.isOverlay() ) {
                overlays.add(p);
            }
            else {
                sorted.add(p);
            }
        }
        // Stable: equal depths keep the order they were added
        Collections.sort(sorted, Comparator.comparingDouble(
            (Primitive p) -> p.getDepth()).reversed());
        sorted.addAll(overlays);
        return sorted;
    }
}
