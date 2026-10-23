#include <stddef.h>
#include <stdio.h>
#include <string.h>

#include "vsdk/toolkit/common/VSDK.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"
#include "GlutSystem.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
#endif
#ifdef VITRAL_GLUT_WITH_X11
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#endif

KeyEvent GlutSystem::glut2vsdkKeyEvent(unsigned char glutKey, int modifiers)
{
    KeyEvent event;

    if (modifiers & GLUT_ACTIVE_SHIFT) {
        event.modifierMask |= KeyEvent::MASK_SHIFT;
    }
    if (modifiers & GLUT_ACTIVE_CTRL) {
        event.modifierMask |= KeyEvent::MASK_CTRL;
    }
    if (modifiers & GLUT_ACTIVE_ALT) {
        event.modifierMask |= KeyEvent::MASK_ALT;
    }

    event.unicodeId = (char)glutKey;

    switch (glutKey) {
        case 27:  // ESC
            event.keycode = KeyEvent::KEY_ESC;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case 8:   // Backspace
            event.keycode = KeyEvent::KEY_BACKSPACE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case 9:   // Tab
            event.keycode = KeyEvent::KEY_TAB;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case 10:  // Enter/newline
        case 13:  // Carriage return
            event.keycode = KeyEvent::KEY_ENTER;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case 32:  // Space
            event.keycode = KeyEvent::KEY_SPACE;
            break;
        case 'A': case 'B': case 'C': case 'D': case 'E':
        case 'F': case 'G': case 'H': case 'I': case 'J':
        case 'K': case 'L': case 'M': case 'N': case 'O':
        case 'P': case 'Q': case 'R': case 'S': case 'T':
        case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
            event.keycode = KeyEvent::KEY_A + (glutKey - 'A');
            break;
        case 'a': case 'b': case 'c': case 'd': case 'e':
        case 'f': case 'g': case 'h': case 'i': case 'j':
        case 'k': case 'l': case 'm': case 'n': case 'o':
        case 'p': case 'q': case 'r': case 's': case 't':
        case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
            event.keycode = KeyEvent::KEY_a + (glutKey - 'a');
            break;
        case '0': case '1': case '2': case '3': case '4':
        case '5': case '6': case '7': case '8': case '9':
            event.keycode = KeyEvent::KEY_0 + (glutKey - '0');
            break;
        default:
            event.keycode = KeyEvent::KEY_NONE;
            break;
    }

    return event;
}

KeyEvent GlutSystem::glut2vsdkSpecialKeyEvent(int glutSpecialKey, int modifiers)
{
    KeyEvent event;

    if (modifiers & GLUT_ACTIVE_SHIFT) {
        event.modifierMask |= KeyEvent::MASK_SHIFT;
    }
    if (modifiers & GLUT_ACTIVE_CTRL) {
        event.modifierMask |= KeyEvent::MASK_CTRL;
    }
    if (modifiers & GLUT_ACTIVE_ALT) {
        event.modifierMask |= KeyEvent::MASK_ALT;
    }

    event.unicodeId = KeyEvent::KEY_NONE;

    switch (glutSpecialKey) {
        case GLUT_KEY_F1:
            event.keycode = KeyEvent::KEY_F1;
            break;
        case GLUT_KEY_F2:
            event.keycode = KeyEvent::KEY_F2;
            break;
        case GLUT_KEY_F3:
            event.keycode = KeyEvent::KEY_F3;
            break;
        case GLUT_KEY_F4:
            event.keycode = KeyEvent::KEY_F4;
            break;
        case GLUT_KEY_F5:
            event.keycode = KeyEvent::KEY_F5;
            break;
        case GLUT_KEY_F6:
            event.keycode = KeyEvent::KEY_F6;
            break;
        case GLUT_KEY_F7:
            event.keycode = KeyEvent::KEY_F7;
            break;
        case GLUT_KEY_F8:
            event.keycode = KeyEvent::KEY_F8;
            break;
        case GLUT_KEY_F9:
            event.keycode = KeyEvent::KEY_F9;
            break;
        case GLUT_KEY_F10:
            event.keycode = KeyEvent::KEY_F10;
            break;
        case GLUT_KEY_F11:
            event.keycode = KeyEvent::KEY_F11;
            break;
        case GLUT_KEY_F12:
            event.keycode = KeyEvent::KEY_F12;
            break;
        case GLUT_KEY_UP:
            event.keycode = KeyEvent::KEY_UP;
            break;
        case GLUT_KEY_DOWN:
            event.keycode = KeyEvent::KEY_DOWN;
            break;
        case GLUT_KEY_LEFT:
            event.keycode = KeyEvent::KEY_LEFT;
            break;
        case GLUT_KEY_RIGHT:
            event.keycode = KeyEvent::KEY_RIGHT;
            break;
        case GLUT_KEY_HOME:
            event.keycode = KeyEvent::KEY_HOME;
            break;
        case GLUT_KEY_END:
            event.keycode = KeyEvent::KEY_END;
            break;
        case GLUT_KEY_PAGE_UP:
            event.keycode = KeyEvent::KEY_PAGEUP;
            break;
        case GLUT_KEY_PAGE_DOWN:
            event.keycode = KeyEvent::KEY_PAGEDOWN;
            break;
        case GLUT_KEY_INSERT:
            event.keycode = KeyEvent::KEY_INSERT;
            break;
        default:
            event.keycode = KeyEvent::KEY_NONE;
            break;
    }

    return event;
}

MouseEvent GlutSystem::glut2vsdkMouseEvent(int button, int state, int x, int y)
{
    MouseEvent event;
    event.setX(x);
    event.setY(y);

    switch (button) {
        case GLUT_LEFT_BUTTON:
            event.setButton(MouseEvent::BUTTON1);
            break;
        case GLUT_MIDDLE_BUTTON:
            event.setButton(MouseEvent::BUTTON2);
            break;
        case GLUT_RIGHT_BUTTON:
            event.setButton(MouseEvent::BUTTON3);
            break;
        default:
            event.setButton(0);
            break;
    }

    int modifiers = 0;
    if (state == GLUT_DOWN) {
        switch (button) {
            case GLUT_LEFT_BUTTON:
                modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
                break;
            case GLUT_MIDDLE_BUTTON:
                modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
                break;
            case GLUT_RIGHT_BUTTON:
                modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
                break;
        }
    }
    event.setModifiers(modifiers);

    return event;
}

MouseEvent GlutSystem::glut2vsdkMotionEvent(int x, int y)
{
    MouseEvent event;
    event.setX(x);
    event.setY(y);
    return event;
}

MouseEvent GlutSystem::glut2vsdkWheelEvent(int wheel, int direction, int x, int y)
{
    MouseEvent event;
    event.setX(x);
    event.setY(y);
    event.setClicks(direction);
    return event;
}

//= Mouse buttons state ===================================================

int GlutSystem::mouseButtonsDownMask = 0;

void GlutSystem::updateMouseButtonsState(int button, int state)
{
    int mask = 0;
    switch ( button ) {
        case GLUT_LEFT_BUTTON:
            mask = MouseEvent::BUTTON1_DOWN_MASK;
            break;
        case GLUT_MIDDLE_BUTTON:
            mask = MouseEvent::BUTTON2_DOWN_MASK;
            break;
        case GLUT_RIGHT_BUTTON:
            mask = MouseEvent::BUTTON3_DOWN_MASK;
            break;
        default:
            return;
    }
    if ( state == GLUT_DOWN ) {
        mouseButtonsDownMask |= mask;
    }
    else {
        mouseButtonsDownMask &= ~mask;
    }
}

int GlutSystem::getMouseButtonsDownMask()
{
    return mouseButtonsDownMask;
}

BOOLEAN GlutSystem::isWheelButton(int button, int* outDirection)
{
    if ( button != 3 && button != 4 ) {
        return FALSE;
    }
    if ( outDirection != NULL ) {
        *outDirection = button == 3 ? 1 : -1;
    }
    return TRUE;
}

//= Labels ================================================================

#ifdef VITRAL_GLUT_WITH_X11

static Display* labelDisplay = NULL;
static ArrayList<int> labelFontSizes;
static ArrayList<XFontStruct*> labelFonts;

static Display* openLabelDisplay()
{
    if ( labelDisplay == NULL ) {
        labelDisplay = XOpenDisplay(NULL);
    }
    return labelDisplay;
}

static XFontStruct* getLabelFont(Display* display, int pixelSize)
{
    for ( long i = 0; i < labelFontSizes.size(); i++ ) {
        if ( labelFontSizes.get(i) == pixelSize ) {
            return labelFonts.get(i);
        }
    }

    char pattern[256];
    sprintf(pattern, "-*-helvetica-medium-r-normal--%d-*-*-*-*-*-iso8859-1",
        pixelSize);
    XFontStruct* font = XLoadQueryFont(display, pattern);
    if ( font == NULL ) {
        font = XLoadQueryFont(display, "fixed");
    }
    labelFontSizes.add(pixelSize);
    labelFonts.add(font);
    return font;
}

RGBAImageUncompressed* GlutSystem::calculateLabelImage(
    const char* text, const ColorRgb& color, int fontSize)
{
    RGBAImageUncompressed* image = new RGBAImageUncompressed();
    Display* display = openLabelDisplay();
    int textLength = text != NULL ? (int)strlen(text) : 0;
    XFontStruct* font = display != NULL && textLength > 0 ?
        getLabelFont(display, fontSize > 0 ? fontSize : 1) : NULL;

    // Renderers use the image without checking it: an empty text (or a
    // missing font) gives a transparent pixel
    if ( font == NULL ) {
        image->init(1, 1);
        return image;
    }

    int direction;
    int ascent;
    int descent;
    XCharStruct overall;
    XTextExtents(font, text, textLength, &direction, &ascent, &descent,
        &overall);
    int width = overall.width > 0 ? overall.width : 1;
    int height = ascent + descent > 0 ? ascent + descent : 1;

    // White text over black: the text coverage
    int screen = DefaultScreen(display);
    Pixmap pixmap = XCreatePixmap(display, RootWindow(display, screen),
        width, height, DefaultDepth(display, screen));
    GC gc = XCreateGC(display, pixmap, 0, NULL);
    XSetFont(display, gc, font->fid);
    XSetForeground(display, gc, BlackPixel(display, screen));
    XFillRectangle(display, pixmap, gc, 0, 0, width, height);
    XSetForeground(display, gc, WhitePixel(display, screen));
    XDrawString(display, pixmap, gc, 0, ascent, text, textLength);
    XImage* coverage = XGetImage(display, pixmap, 0, 0, width, height,
        AllPlanes, ZPixmap);
    XFreeGC(display, gc);
    XFreePixmap(display, pixmap);
    if ( coverage == NULL ) {
        image->init(1, 1);
        return image;
    }

    image->init(width, height);
    char r = (char)(int)(color.r() * 255.0);
    char g = (char)(int)(color.g() * 255.0);
    char b = (char)(int)(color.b() * 255.0);
    unsigned long white = WhitePixel(display, screen);
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            char a = XGetPixel(coverage, x, y) == white ? (char)255 : 0;
            // putPixel takes y from the top, as the X image does
            image->putPixel(x, y, r, g, b, a);
        }
    }
    XDestroyImage(coverage);
    return image;
}

void GlutSystem::releaseResources()
{
    if ( labelDisplay == NULL ) {
        return;
    }
    for ( long i = 0; i < labelFonts.size(); i++ ) {
        if ( labelFonts.get(i) != NULL ) {
            XFreeFont(labelDisplay, labelFonts.get(i));
        }
    }
    labelFonts.clear();
    labelFontSizes.clear();
    XCloseDisplay(labelDisplay);
    labelDisplay = NULL;
}

#else

RGBAImageUncompressed* GlutSystem::calculateLabelImage(
    const char* /*text*/, const ColorRgb& /*color*/, int /*fontSize*/)
{
    RGBAImageUncompressed* image = new RGBAImageUncompressed();
    image->init(1, 1);
    return image;
}

void GlutSystem::releaseResources()
{
}

#endif
