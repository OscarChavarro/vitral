#ifndef __GIZMO_2D_DRAW_LIST__
#define __GIZMO_2D_DRAW_LIST__

#include "vsdk/toolkit/common/VSDK.h"
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
    /// C++98 replacement of a scoped enumeration: keeps the `Kind::VALUE`
    /// syntax
    class Kind {
      public:
        enum Value {
            /** Filled simple polygon (convex or not), of at least 3 vertices */
            POLYGON,
            /** Line from the vertex 0 to the vertex 1, of `getWidth()` pixels */
            LINE,
            /** Text, positioned following `getAlignment()` */
            LABEL
        };
        Kind() : value(POLYGON) {}
        Kind(Value v) : value(v) {}
        operator Value() const { return value; }
      private:
        Value value;
    };

    /**
    How the position of a label relates to its text.
    */
    class Alignment {
      public:
        enum Value {
            /** The position is the lower left corner of the box of the text */
            LOWER_LEFT,
            /** The position is the center of the box of the text */
            CENTER
        };
        Alignment() : value(LOWER_LEFT) {}
        Alignment(Value v) : value(v) {}
        operator Value() const { return value; }
      private:
        Value value;
    };

    /**
    One primitive of the list. Instances are created by the `add*` methods
    of the list.
    */
    class Primitive {
    private:
        Kind kind;
        ArrayList<double> x;
        ArrayList<double> y;
        ColorRgb color;
        double opacity;
        double width;
        double depth;
        BOOLEAN overlay;
        String text;
        int fontSize;
        Alignment alignment;

    public:
        Primitive();
        Primitive(Kind kind, const ArrayList<double>& x,
                  const ArrayList<double>& y, const ColorRgb& color,
                  double opacity, double width, double depth,
                  BOOLEAN overlay, const String& text, int fontSize,
                  Alignment alignment);

        Kind getKind() const { return kind; }

        /** @return number of vertices (1 for labels) */
        int getNumberOfVertices() const { return (int)x.size(); }

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
        BOOLEAN isOverlay() const { return overlay; }

        const String& getText() const { return text; }

        /** @return font size of a label, in pixels */
        int getFontSize() const { return fontSize; }

        Alignment getAlignment() const { return alignment; }
    };

private:
    ArrayList<Primitive> primitives;

public:
    Gizmo2DDrawList();

    void clear();
    BOOLEAN isEmpty() const;

    /**
    Adds a polygon coming from a 3D element, sorted by depth.
    */
    void addPolygon(const ArrayList<double>& x, const ArrayList<double>& y,
                    double depth, const ColorRgb& color, double opacity);

    /**
    Adds a line coming from a 3D element, sorted by depth.
    */
    void addLine(double x0, double y0, double x1, double y1, double depth,
                 const ColorRgb& color, double width);

    /**
    Adds a screen space line (overlay), i.e. a line whose visibility was
    already solved (as the hidden line removal of a HUD).
    */
    void addOverlayLine(double x0, double y0, double x1, double y1,
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
    void addOverlayLabel(double x, double y, const String& text,
                         const ColorRgb& color, int fontSize,
                         Alignment alignment);

    /**
    @return the primitives in the order they must be drawn: the depth
    sorted ones from far to near, then the overlays in the order they were
    added
    */
    ArrayList<Primitive> getPrimitivesInDrawingOrder() const;
};

#endif
