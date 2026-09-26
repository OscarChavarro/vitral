#include <X11/Xutil.h>
#include <X11/keysym.h>

#include <cstdio>
#include <map>
#include <utility>

#include "vsdk/toolkit/common/color/ColorRgb.h"
#include "vsdk/toolkit/gui/XtSystem.h"
#include "vsdk/toolkit/media/RGBAImageUncompressed.h"

int XtSystem::buttonMaskFor(unsigned int button)
{
    switch ( button ) {
      case Button1: return MouseEvent::BUTTON1_DOWN_MASK;
      case Button2: return MouseEvent::BUTTON2_DOWN_MASK;
      case Button3: return MouseEvent::BUTTON3_DOWN_MASK;
      default: return 0;
    }
}

int XtSystem::stateToModifiers(unsigned int state)
{
    int modifiers = 0;

    if ( (state & ShiftMask) != 0 ) modifiers |= SHIFT_DOWN_MASK;
    if ( (state & ControlMask) != 0 ) modifiers |= MouseEvent::CTRL_DOWN_MASK;
    if ( (state & Mod1Mask) != 0 ) modifiers |= ALT_DOWN_MASK;
    if ( (state & Button1Mask) != 0 ) modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
    if ( (state & Button2Mask) != 0 ) modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
    if ( (state & Button3Mask) != 0 ) modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
    return modifiers;
}

MouseEvent XtSystem::xt2vsdkMouseEvent(const XEvent& event)
{
    MouseEvent result;

    switch ( event.type ) {
      case ButtonPress:
        result.setX(event.xbutton.x);
        result.setY(event.xbutton.y);
        result.setButton(static_cast<int>(event.xbutton.button));
        result.setModifiers(stateToModifiers(event.xbutton.state) |
                            buttonMaskFor(event.xbutton.button));
        result.setClicks(1);
        break;
      case ButtonRelease:
        result.setX(event.xbutton.x);
        result.setY(event.xbutton.y);
        result.setButton(static_cast<int>(event.xbutton.button));
        result.setModifiers(stateToModifiers(event.xbutton.state) &
                            ~buttonMaskFor(event.xbutton.button));
        result.setClicks(1);
        break;
      case MotionNotify:
        result.setX(event.xmotion.x);
        result.setY(event.xmotion.y);
        result.setModifiers(stateToModifiers(event.xmotion.state));
        break;
      case EnterNotify:
      case LeaveNotify:
        result.setX(event.xcrossing.x);
        result.setY(event.xcrossing.y);
        result.setModifiers(stateToModifiers(event.xcrossing.state));
        break;
      default:
        break;
    }
    return result;
}

bool XtSystem::isWheelButton(const XEvent& event)
{
    return event.xbutton.button >= 4 && event.xbutton.button <= 7;
}

MouseEvent XtSystem::xt2vsdkWheelEvent(const XEvent& event)
{
    MouseEvent result;

    result.setX(event.xbutton.x);
    result.setY(event.xbutton.y);
    result.setModifiers(stateToModifiers(event.xbutton.state));
    // Buttons 4 / 5 are the vertical wheel: up is a negative rotation in AWT
    if ( event.xbutton.button == 4 ) {
        result.setClicks(-1);
    }
    else if ( event.xbutton.button == 5 ) {
        result.setClicks(1);
    }
    return result;
}

KeyEvent XtSystem::xt2vsdkKeyEvent(XKeyEvent& event)
{
    char buffer[8] = {0};
    KeySym keysym = NoSymbol;
    int count = XLookupString(&event, buffer, sizeof(buffer) - 1, &keysym,
                              nullptr);

    return xt2vsdkKeyEvent(keysym, XLookupKeysym(&event, 0), buffer, count,
                      event.state);
}

KeyEvent XtSystem::xt2vsdkKeyEvent(KeySym keysym, KeySym base,
                                   const char* buffer, int count,
                                   unsigned int state)
{
    KeyEvent result;
    bool shift = (state & ShiftMask) != 0;
    bool control = (state & ControlMask) != 0;

    if ( shift ) result.modifierMask |= KeyEvent::MASK_SHIFT;
    if ( control ) result.modifierMask |= KeyEvent::MASK_CTRL;
    if ( (state & Mod1Mask) != 0 ) result.modifierMask |= KeyEvent::MASK_ALT;

    //- Keys identified by their symbol -----------------------------------
    switch ( keysym ) {
      case XK_Escape: result.keycode = KeyEvent::KEY_ESC; return result;
      case XK_Return: result.keycode = KeyEvent::KEY_ENTER; result.unicodeId = '\n'; return result;
      case XK_KP_Enter: result.keycode = KeyEvent::KEY_NUMENTER; result.unicodeId = '\n'; return result;
      case XK_BackSpace: result.keycode = KeyEvent::KEY_BACKSPACE; result.unicodeId = '\b'; return result;
      case XK_Tab:
      case XK_ISO_Left_Tab: result.keycode = KeyEvent::KEY_TAB; result.unicodeId = '\t'; return result;
      case XK_Delete:
      case XK_KP_Delete: result.keycode = KeyEvent::KEY_DELETE; result.unicodeId = 127; return result;
      case XK_Insert: result.keycode = KeyEvent::KEY_INSERT; return result;
      case XK_Up: case XK_KP_Up: result.keycode = KeyEvent::KEY_UP; return result;
      case XK_Down: case XK_KP_Down: result.keycode = KeyEvent::KEY_DOWN; return result;
      case XK_Left: case XK_KP_Left: result.keycode = KeyEvent::KEY_LEFT; return result;
      case XK_Right: case XK_KP_Right: result.keycode = KeyEvent::KEY_RIGHT; return result;
      case XK_Page_Up: case XK_KP_Page_Up: result.keycode = KeyEvent::KEY_PAGEUP; return result;
      case XK_Page_Down: case XK_KP_Page_Down: result.keycode = KeyEvent::KEY_PAGEDOWN; return result;
      case XK_Home: case XK_KP_Home: result.keycode = KeyEvent::KEY_HOME; return result;
      case XK_End: case XK_KP_End: result.keycode = KeyEvent::KEY_END; return result;
      case XK_Shift_L: result.keycode = KeyEvent::KEY_LSHIFT; return result;
      case XK_Shift_R: result.keycode = KeyEvent::KEY_RSHIFT; return result;
      case XK_Control_L: result.keycode = KeyEvent::KEY_LCTRL; return result;
      case XK_Control_R: result.keycode = KeyEvent::KEY_RCTRL; return result;
      case XK_Alt_L: result.keycode = KeyEvent::KEY_LALT; return result;
      case XK_Alt_R: result.keycode = KeyEvent::KEY_RALT; return result;
      case XK_ISO_Level3_Shift: result.keycode = KeyEvent::KEY_ALTGR; return result;
      case XK_Caps_Lock: result.keycode = KeyEvent::KEY_CAPSLOCK; return result;
      case XK_Num_Lock: result.keycode = KeyEvent::KEY_NUMLOCK; return result;
      case XK_Print: result.keycode = KeyEvent::KEY_PRINTSCREEN; return result;
      default: break;
    }
    if ( keysym >= XK_F1 && keysym <= XK_F12 ) {
        result.keycode = KeyEvent::KEY_F1 + static_cast<int>(keysym - XK_F1);
        return result;
    }

    //- Ctrl+letter chords: the letter from the unshifted symbol ----------
    if ( control && base >= XK_a && base <= XK_z ) {
        result.keycode = (shift ? KeyEvent::KEY_A : KeyEvent::KEY_a) +
            static_cast<int>(base - XK_a);
        result.unicodeId = count > 0 ? buffer[0] : KeyEvent::KEY_NONE;
        return result;
    }

    //- Keys identified by their character --------------------------------
    if ( count <= 0 ) {
        return result;
    }
    char c = buffer[0];
    result.unicodeId = c;
    if ( c >= 'A' && c <= 'Z' ) result.keycode = KeyEvent::KEY_A + (c - 'A');
    else if ( c >= 'a' && c <= 'z' ) result.keycode = KeyEvent::KEY_a + (c - 'a');
    else if ( c >= '0' && c <= '9' ) {
        // With NumLock the keypad gives digits, useful for the input gizmo
        if ( keysym >= XK_KP_0 && keysym <= XK_KP_9 ) {
            result.keycode = KeyEvent::KEY_NUM0 + (c - '0');
        }
        else {
            result.keycode = KeyEvent::KEY_0 + (c - '0');
        }
    }
    else if ( c == ',' ) result.keycode = KeyEvent::KEY_COMMA;
    else if ( c == '.' ) result.keycode = keysym == XK_KP_Decimal ? KeyEvent::KEY_NUMPERIOD : KeyEvent::KEY_PERIOD;
    else if ( c == ' ' ) result.keycode = KeyEvent::KEY_SPACE;
    else if ( c == '=' ) result.keycode = KeyEvent::KEY_EQUALS;
    else if ( c == '-' ) result.keycode = keysym == XK_KP_Subtract ? KeyEvent::KEY_NUMMINUS : KeyEvent::KEY_MINUS;
    else if ( c == '+' && keysym == XK_KP_Add ) result.keycode = KeyEvent::KEY_NUMPLUS;
    else if ( c == '*' && keysym == XK_KP_Multiply ) result.keycode = KeyEvent::KEY_NUMASTERISK;
    else if ( c == '/' && keysym == XK_KP_Divide ) result.keycode = KeyEvent::KEY_NUMSLASH;
    return result;
}

//= Labels ================================================================

namespace {

std::map<std::pair<Display*, int>, XFontSet>& fontSets()
{
    static std::map<std::pair<Display*, int>, XFontSet> sets;
    return sets;
}

XFontSet getFontSet(Display* display, int pixelSize)
{
    std::pair<Display*, int> key(display, pixelSize);
    std::map<std::pair<Display*, int>, XFontSet>::iterator known =
        fontSets().find(key);
    if ( known != fontSets().end() ) {
        return known->second;
    }

    // Closest size of a scalable or bitmap Helvetica, then any font
    char pattern[256];
    snprintf(pattern, sizeof(pattern),
        "-*-helvetica-medium-r-normal--%d-*-*-*-*-*-*-*,"
        "-*-*-medium-r-normal--%d-*-*-*-*-*-*-*,fixed",
        pixelSize, pixelSize);
    char** missingCharsets = nullptr;
    int missingCharsetCount = 0;
    char* defaultString = nullptr;
    XFontSet fontSet = XCreateFontSet(display, pattern, &missingCharsets,
                                      &missingCharsetCount, &defaultString);
    if ( missingCharsets != nullptr ) {
        XFreeStringList(missingCharsets);
    }
    fontSets()[key] = fontSet;
    return fontSet;
}

}

RGBAImageUncompressed* XtSystem::calculateLabelImage(
    Display* display, const java::String& label, const ColorRgb& color,
    int fontSize)
{
    RGBAImageUncompressed* image = new RGBAImageUncompressed();
    XFontSet fontSet = getFontSet(display, fontSize);
    int textLength = label.length();

    // Renderers use the image without checking it: an empty text (or a
    // missing font) gives a transparent pixel
    if ( fontSet == nullptr || textLength == 0 ) {
        image->init(1, 1);
        return image;
    }

    XRectangle ink;
    XRectangle logical;
    Xutf8TextExtents(fontSet, label.c_str(), textLength, &ink, &logical);
    int width = logical.width > 0 ? logical.width : 1;
    int height = logical.height > 0 ? logical.height : 1;

    // White text over black: the text coverage
    int screen = DefaultScreen(display);
    Window root = RootWindow(display, screen);
    Pixmap pixmap = XCreatePixmap(display, root, width, height,
                                  DefaultDepth(display, screen));
    GC gc = XCreateGC(display, pixmap, 0, nullptr);
    XSetForeground(display, gc, BlackPixel(display, screen));
    XFillRectangle(display, pixmap, gc, 0, 0, width, height);
    XSetForeground(display, gc, WhitePixel(display, screen));
    Xutf8DrawString(display, pixmap, fontSet, gc, -logical.x, -logical.y,
                    label.c_str(), textLength);
    XImage* coverage = XGetImage(display, pixmap, 0, 0, width, height,
                                 AllPlanes, ZPixmap);
    XFreeGC(display, gc);
    XFreePixmap(display, pixmap);
    if ( coverage == nullptr ) {
        image->init(1, 1);
        return image;
    }

    image->init(width, height);
    char r = static_cast<char>(static_cast<int>(color.r() * 255.0));
    char g = static_cast<char>(static_cast<int>(color.g() * 255.0));
    char b = static_cast<char>(static_cast<int>(color.b() * 255.0));
    unsigned long white = WhitePixel(display, screen);
    for ( int y = 0; y < height; y++ ) {
        for ( int x = 0; x < width; x++ ) {
            char a = XGetPixel(coverage, x, y) == white ?
                static_cast<char>(255) : 0;
            // putPixel takes y from the top, as the X image does
            image->putPixel(x, y, r, g, b, a);
        }
    }
    XDestroyImage(coverage);
    return image;
}

void XtSystem::releaseResources(Display* display)
{
    std::map<std::pair<Display*, int>, XFontSet>::iterator i =
        fontSets().begin();
    while ( i != fontSets().end() ) {
        if ( i->first.first == display ) {
            if ( i->second != nullptr ) {
                XFreeFontSet(display, i->second);
            }
            fontSets().erase(i++);
        }
        else {
            ++i;
        }
    }
}
