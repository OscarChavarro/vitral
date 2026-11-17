#ifndef __PROJECTED_LINE_LAYER__
#define __PROJECTED_LINE_LAYER__

#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/media/Calligraphic2DBuffer.h"

/**
A set of 2D lines (or points) sharing one drawing style, already projected
to the normalized device coordinates of a viewport (from <-1, -1> at its
lower left corner to <1, 1> at its upper right corner). It is the technology
independent output of `ProjectedLinesViewBuilder`: any 2D drawing technology
(an Xlib drawable, a raster image, a plotter) can present it by mapping
those coordinates to its own pixels, as `Calligraphic2DBuffer` suggests.
*/
class ProjectedLineLayer {
public:
    /**
    How the elements of a layer are drawn.
    */
    enum Style {
        /** Continuous lines */
        SOLID,
        /** Dashed lines (i.e. hidden lines) */
        DASHED,
        /** Points: each element is a degenerate line, drawn as a small mark */
        POINTS
    };

private:
    Calligraphic2DBuffer lines;
    ColorRgb color;
    double width;
    Style style;

    ProjectedLineLayer(const ProjectedLineLayer& other);
    ProjectedLineLayer& operator=(const ProjectedLineLayer& other);

public:
    /**
    @param color color of every element of the layer
    @param width line width (or point size), in pixels of a standard
    resolution screen (see `ViewportElementScaler`)
    @param style how the elements are drawn
    */
    ProjectedLineLayer(const ColorRgb& color, double width, Style style)
        : color(color), width(width), style(style) {}

    /**
    @return the lines of the layer, in normalized device coordinates
    */
    Calligraphic2DBuffer* getLines() { return &lines; }
    const Calligraphic2DBuffer* getLines() const { return &lines; }

    const ColorRgb& getColor() const { return color; }
    double getWidth() const { return width; }
    Style getStyle() const { return style; }

    /**
    @return true if the layer has nothing to draw
    */
    bool isEmpty() const { return lines.getNumLines() == 0; }

    /**
    Deletes the layers of a list (C++ port: the lists own their layers).
    */
    static void deleteAll(java::ArrayList<ProjectedLineLayer*>& layers)
    {
        for ( long i = 0; i < layers.size(); i++ ) {
            delete layers.get(i);
        }
        layers.clear();
    }
};

#endif
