#ifndef __INPUT_GIZMO_2D_PAINTER__
#define __INPUT_GIZMO_2D_PAINTER__

#include "vsdk/toolkit/common/VSDK.h"
#include "java/lang/String.h"

class ColorRgb;
class Gizmo2DDrawList;
class InputGizmo;
class ViewportElementScaler;

/**
Presents an `InputGizmo` as 2D overlay primitives (see `Gizmo2DDrawList`),
with the layout of the OpenGL input gizmo renderers: at the lower right
corner of the viewport, each number inside a frame with the color of its box
(see `InputGizmo::getFieldDisplayColor`), the frames at least as wide as the
reference text of the gizmo, and a thin line below the frame of the selected
box. Sizes follow the screen resolution (see `ViewportElementScaler`).

The width of the texts depends on the fonts of the drawing technology, so
it is measured by a `TextMeasurer` given by the backend.
*/
class InputGizmo2DPainter {
public:
    /**
    Measures texts as the backend draws `Gizmo2DDrawList` labels.
    */
    class TextMeasurer {
    public:
        virtual ~TextMeasurer() {}

        /**
        @return the width, in pixels, of the box of a text
        */
        virtual int getTextWidth(const String& text, int fontSize) const = 0;

        /**
        @return the height, in pixels, of the box of any text of a font size
        */
        virtual int getTextHeight(int fontSize) const = 0;
    };

private:
    InputGizmo2DPainter();

    static void addFrame(Gizmo2DDrawList* out, int x, int y, int width,
                         int height, int thickness, const ColorRgb& c);

public:
    /**
    @param out list receiving the primitives
    @param gizmo gizmo to present
    @param viewportWidth width of the viewport, in pixels
    @param viewportHeight height of the viewport, in pixels
    @param scaler sizes of the elements for the resolution of the screen
    @param measurer measures the texts as the backend draws them
    */
    static void paint(Gizmo2DDrawList* out, const InputGizmo* gizmo,
                      int viewportWidth, int viewportHeight,
                      const ViewportElementScaler* scaler,
                      const TextMeasurer& measurer);
};

#endif
