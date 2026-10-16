#ifndef __GLFW_SYSTEM__
#define __GLFW_SYSTEM__

#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/MouseEvent.h"

class ColorRgb;
class RGBAImageUncompressed;

class GlfwSystem {
public:
    /** Font size, in pixels, of the labels created without giving one. */
    static const int DEFAULT_LABEL_FONT_SIZE = 14;

    /**
    C++ counterpart of Java's `AwtSystem.calculateLabelImage`: rasterizes a
    text with FreeType 2 (GLFW has no text facilities of its own), so GLFW
    applications can feed OpenGL4 label renderers (i.e. through an
    `OpenGL4LabelImageProvider`). Like the GTK4 provider, the first row of
    the image is the bottom one, as OpenGL expects.
    Without FreeType support, or when no font is found, the image is a
    single transparent pixel.
    @param text the text to draw (UTF-8)
    @param color the color of the text
    @param fontSize the size of the font, in pixels
    @return a new image, owned by the caller, with the text on transparent
    background
    */
    static RGBAImageUncompressed* calculateLabelImage(
        const char* text, const ColorRgb& color,
        int fontSize = DEFAULT_LABEL_FONT_SIZE);

    static KeyEvent glfw2vsdkKeyEvent(int glfwKey, int glfwMods);
    static KeyEvent glfw2vsdkSpecialKeyEvent(int glfwKey, int glfwMods);
    static MouseEvent glfw2vsdkMouseEvent(int glfwButton, int glfwAction, double x, double y);
    static MouseEvent glfw2vsdkMotionEvent(double x, double y);
    static MouseEvent glfw2vsdkWheelEvent(double xoffset, double yoffset);

private:
    GlfwSystem();
    ~GlfwSystem();
};

#endif
