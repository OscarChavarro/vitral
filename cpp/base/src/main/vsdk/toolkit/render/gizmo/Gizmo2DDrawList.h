#ifndef __GIZMO_2D_DRAW_LIST__
#define __GIZMO_2D_DRAW_LIST__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"

/**
Technology independent list of 2D primitives that presents gizmos over a
viewport, for drawing surfaces without a 3D API (an X11 drawable, a
`Framebuffer`, a raster image...). It is the output of the gizmo painters
of this package (`TranslateGizmo2DPainter`, `RotateGizmo2DPainter`,
`ScaleGizmo2DPainter` and `InputGizmo2DPainter`), already projected,
clipped and sorted, so a backend only has to know how to:

- fill a simple polygon with a color and an opacity,
- draw a line with a color and a width,
- draw a text with a color and a font size.

Coordinates are pixels of the viewport, with the origin at its upper left
corner and growing downwards (as X11, BGI and most frame buffers do).

As there is no depth buffer, hidden surfaces are solved with the painter's
algorithm [FOLE1992].15.5: primitives coming from 3D elements (`isOverlay`
false) keep a depth (the mean of the normalized device depth of their
vertices) and `getPrimitivesInDrawingOrder` sorts them from far to near;
overlay primitives (input boxes, labels) are drawn after them, in the order
they were added. Primitives at the same depth keep the order they were
added, so translucent elements added after the opaque ones stay over them.
*/
class Gizmo2DDrawList {
public:
    /**
    Kind of a primitive.
    */
    enum class Kind {
        /** Filled simple polygon (convex or not), of at least 3 vertices */
        POLYGON,
        /** Line from the vertex 0 to the vertex 1, of `getWidth()` pixels */
        LINE,
        /** Text, positioned following `getAlignment()` */
        LABEL
    };

    /**
    How the position of a label relates to its text.
    */
    enum class Alignment {
        /** The position is the lower left corner of the box of the text */
        LOWER_LEFT,
        /** The position is the center of the box of the text */
        CENTER
    };

    /**
    One primitive of the list. Instances are created by the `add*` methods
    of the list.
    */
    class Primitive {
    private:
        Kind kind;
        java::ArrayList<double> x;
        java::ArrayList<double> y;
        ColorRgb color;
        double opacity;
        double width;
        double depth;
        bool overlay;
        java::String text;
        int fontSize;
        Alignment alignment;

    public:
        Primitive();
        Primitive(Kind kind, const java::ArrayList<double>& x,
                  const java::ArrayList<double>& y, const ColorRgb& color,
                  double opacity, double width, double depth, bool overlay,
                  const java::String& text, int fontSize, Alignment alignment);

        Kind getKind() const { return kind; }

        /** @return number of vertices (1 for labels) */
        int getNumberOfVertices() const { return static_cast<int>(x.size()); }

        double getX(int vertex) const { return x[vertex]; }

        double getY(int vertex) const { return y[vertex]; }

        const ColorRgb& getColor() const { return color; }

        /** @return opacity, from 0 (invisible) to 1 (opaque) */
        double getOpacity() const { return opacity; }

        /** @return width of a line, in pixels */
        double getWidth() const { return width; }

        /** @return normalized device depth (bigger is farther) */
        double getDepth() const { return depth; }

        /**
        @return true for screen space primitives (input boxes, labels),
        drawn after the depth sorted ones and never sorted
        */
        bool isOverlay() const { return overlay; }

        const java::String& getText() const { return text; }

        /** @return font size of a label, in pixels */
        int getFontSize() const { return fontSize; }

        Alignment getAlignment() const { return alignment; }
    };

private:
    java::ArrayList<Primitive> primitives;

public:
    Gizmo2DDrawList();

    void clear();
    bool isEmpty() const;

    /**
    Adds a polygon coming from a 3D element, sorted by depth.
    */
    void addPolygon(const java::ArrayList<double>& x,
                    const java::ArrayList<double>& y, double depth,
                    const ColorRgb& color, double opacity);

    /**
    Adds a line coming from a 3D element, sorted by depth.
    */
    void addLine(double x0, double y0, double x1, double y1, double depth,
                 const ColorRgb& color, double width);

    /**
    Adds an opaque screen space rectangle (overlay).
    @param x left pixel
    @param y top pixel
    @param width width, in pixels
    @param height height, in pixels
    @param color color to fill it with
    */
    void addOverlayRectangle(double x, double y, double width, double height,
                             const ColorRgb& color);

    /**
    Adds a screen space text (overlay).
    @param x horizontal position, in pixels (see `alignment`)
    @param y vertical position, in pixels (see `alignment`)
    @param text text to draw (UTF-8)
    @param color color of the text
    @param fontSize size of the font, in pixels
    @param alignment how (x, y) relates to the box of the text
    */
    void addOverlayLabel(double x, double y, const java::String& text,
                         const ColorRgb& color, int fontSize,
                         Alignment alignment);

    /**
    @return the primitives in the order they must be drawn: the depth
    sorted ones from far to near, then the overlays in the order they were
    added
    */
    java::ArrayList<Primitive> getPrimitivesInDrawingOrder() const;
};

#endif
