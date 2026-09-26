#ifndef __OPEN_GL_1_INPUT_GIZMO_RENDERER__
#define __OPEN_GL_1_INPUT_GIZMO_RENDERER__

#include "java/lang/String.h"
#include "java/util/ArrayList.h"
#include "vsdk/toolkit/common/color/ColorRgb.h"

class InputGizmo;
class RGBAImageUncompressed;
class Viewport;
class ViewportSet;

/**
Renders an `InputGizmo` over a viewport with the OpenGL 1.2 fixed function
pipeline, at its lower right corner: each number is a label image inside a frame with the
color of its box (see `InputGizmo::getFieldDisplayColor`), and the selected box
has a thin line below its frame. Sizes follow the screen resolution (see
`ViewportElementScaler`).

There must be one renderer for each viewport where the gizmo is drawn, as it
keeps the label images (regenerated only when the text, the color or the font
size of a box change). Text rasterization and the texture management of the
images are provided by the `Host`, so this class depends neither on the
windowing toolkit nor on the presentation of the viewport.

Usage (render thread, once per frame, with the viewport already activated):
<pre>
    renderer.draw(inputGizmo);
</pre>
*/
class OpenGL1InputGizmoRenderer {
public:
    /**
    Services this renderer needs from who presents the viewport.
    */
    class Host {
    public:
        virtual ~Host() {}

        /**
        @param text the text to draw
        @param color the color of the text
        @param fontSize the size of the font, in pixels
        @return an image with the text on transparent background
        */
        virtual RGBAImageUncompressed* createLabelImage(
            const java::String& text, const ColorRgb& color, int fontSize) = 0;

        /**
        Draws a label image with its lower left corner at a position of the
        viewport, measured in pixels from its lower left corner.
        */
        virtual void drawLabelImage(RGBAImageUncompressed* image, int x, int y) = 0;

        /**
        Marks a label image as no longer used, so its texture is released (and
        the image deleted).
        */
        virtual void discardLabelImage(RGBAImageUncompressed* image) = 0;
    };

private:
    // Sizes, in pixels, designed for legacy resolutions
    static const int BASE_FONT_SIZE = 14;
    static const int BASE_MARGIN = 10;
    static const int BASE_GAP = 4;
    static const int BASE_PADDING_X = 3;
    static const int BASE_PADDING_Y = 1;
    static const int BASE_FRAME_THICKNESS = 1;
    static const int BASE_UNDERLINE_DISTANCE = 2;
    static const int BASE_UNDERLINE_THICKNESS = 2;

    /// Rectangle {x0, y0, x1, y1, r, g, b}, in pixels of the viewport,
    /// measured from its lower left corner
    struct Rectangle {
        float values[7];
    };

    Host* host;
    ViewportSet* viewportSet;
    Viewport* viewport;

    java::ArrayList<RGBAImageUncompressed*> images;
    java::ArrayList<java::String> imageTexts;
    java::ArrayList<ColorRgb> imageColors;
    int imageFontSize;
    java::String referenceText;
    int referenceTextWidth;

    void discardImages();
    void updateImage(int field, const java::String& text, const ColorRgb& color,
                     int fontSize);

    static void addRectangle(java::ArrayList<Rectangle>& rectangles,
                             int x0, int y0, int x1, int y1, const ColorRgb& c);
    static void addFrame(java::ArrayList<Rectangle>& rectangles, int x, int y,
                         int width, int height, int thickness, const ColorRgb& c);

    /**
    Draws opaque rectangles over the whole surface of the viewport set, clipped
    to the area of this viewport with the scissor test (the same way label
    images are drawn by the viewport window). The state changed is
    restored.
    */
    void drawRectangles(const java::ArrayList<Rectangle>& rectangles);

public:
    /**
    @param host provider of label images and their drawing
    @param viewportSet set the viewport belongs to
    @param viewport viewport where the gizmo is drawn
    */
    OpenGL1InputGizmoRenderer(Host* host, ViewportSet* viewportSet,
                              Viewport* viewport);
    ~OpenGL1InputGizmoRenderer();

    /**
    Draws the gizmo over the viewport.
    @param gizmo gizmo to draw
    */
    void draw(InputGizmo* gizmo);

    /**
    Deletes the OpenGL resources (label textures) of this renderer.
    PRE: the OpenGL context that created them is current.
    */
    void disposeGlResources();
};

#endif
