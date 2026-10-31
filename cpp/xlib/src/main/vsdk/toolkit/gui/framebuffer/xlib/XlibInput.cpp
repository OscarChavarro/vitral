#include <X11/Xutil.h>
#include <X11/keysym.h>
#include "vsdk/toolkit/gui/framebuffer/xlib/XlibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/xlib/XlibInput.h"

// The masks of AWT's InputEvent that MouseEvent leaves out.
static constexpr int SHIFT_DOWN_MASK = 64;
static constexpr int ALT_DOWN_MASK = 512;
// The longest time between the presses of a double click, and how far apart they may be.
static constexpr Time MULTIPLE_CLICK_TIME = 400;
static constexpr int MULTIPLE_CLICK_DISTANCE = 4;

XlibInput::XlibInput(XlibFramebuffer &target)
    : surface(target), deleteWindow(None), protocols(None), closed(false), warped(false), warpX(0), warpY(0),
      clickButton(0), clickTime(0), clickX(0), clickY(0), clicks(0) {
    if (surface.getDisplay()) {
        deleteWindow = XInternAtom(surface.getDisplay(), "WM_DELETE_WINDOW", False);
        protocols = XInternAtom(surface.getDisplay(), "WM_PROTOCOLS", False);
    }
}

int
XlibInput::buttonMask(unsigned int button) {
    switch (button) {
    case Button1:
        return MouseEvent::BUTTON1_DOWN_MASK;
    case Button2:
        return MouseEvent::BUTTON2_DOWN_MASK;
    case Button3:
        return MouseEvent::BUTTON3_DOWN_MASK;
    default:
        return 0;
    }
}

int
XlibInput::modifiers(unsigned int state) {
    int result = 0;
    if (state & ShiftMask) {
        result |= SHIFT_DOWN_MASK;
    }
    if (state & ControlMask) {
        result |= MouseEvent::CTRL_DOWN_MASK;
    }
    if (state & Mod1Mask) {
        result |= ALT_DOWN_MASK;
    }
    if (state & Button1Mask) {
        result |= MouseEvent::BUTTON1_DOWN_MASK;
    }
    if (state & Button2Mask) {
        result |= MouseEvent::BUTTON2_DOWN_MASK;
    }
    if (state & Button3Mask) {
        result |= MouseEvent::BUTTON3_DOWN_MASK;
    }
    return result;
}

// The same translation as XtSystem::xt2vsdkKeyEvent().
KeyEvent
XlibInput::keyEvent(XKeyEvent &native) {
    char buffer[8] = {0};
    KeySym keysym = NoSymbol;
    int count = XLookupString(&native, buffer, sizeof(buffer) - 1, &keysym, nullptr);
    KeySym base = XLookupKeysym(&native, 0);
    bool shift = (native.state & ShiftMask) != 0;
    bool control = (native.state & ControlMask) != 0;
    KeyEvent result;
    if (shift) {
        result.modifierMask |= KeyEvent::MASK_SHIFT;
    }
    if (control) {
        result.modifierMask |= KeyEvent::MASK_CTRL;
    }
    if (native.state & Mod1Mask) {
        result.modifierMask |= KeyEvent::MASK_ALT;
    }
    static const struct {
        KeySym keysym;
        int keycode;
        char unicode;
    } SPECIAL_KEYS[] = {
        {XK_Escape, KeyEvent::KEY_ESC, 27},          {XK_Return, KeyEvent::KEY_ENTER, '\n'},
        {XK_KP_Enter, KeyEvent::KEY_NUMENTER, '\n'}, {XK_BackSpace, KeyEvent::KEY_BACKSPACE, '\b'},
        {XK_Tab, KeyEvent::KEY_TAB, '\t'},           {XK_ISO_Left_Tab, KeyEvent::KEY_TAB, '\t'},
        {XK_Delete, KeyEvent::KEY_DELETE, 127},      {XK_KP_Delete, KeyEvent::KEY_DELETE, 127},
        {XK_Insert, KeyEvent::KEY_INSERT, 0},        {XK_Up, KeyEvent::KEY_UP, 0},
        {XK_KP_Up, KeyEvent::KEY_UP, 0},             {XK_Down, KeyEvent::KEY_DOWN, 0},
        {XK_KP_Down, KeyEvent::KEY_DOWN, 0},         {XK_Left, KeyEvent::KEY_LEFT, 0},
        {XK_KP_Left, KeyEvent::KEY_LEFT, 0},         {XK_Right, KeyEvent::KEY_RIGHT, 0},
        {XK_KP_Right, KeyEvent::KEY_RIGHT, 0},       {XK_Page_Up, KeyEvent::KEY_PAGEUP, 0},
        {XK_KP_Page_Up, KeyEvent::KEY_PAGEUP, 0},    {XK_Page_Down, KeyEvent::KEY_PAGEDOWN, 0},
        {XK_KP_Page_Down, KeyEvent::KEY_PAGEDOWN, 0}, {XK_Home, KeyEvent::KEY_HOME, 0},
        {XK_KP_Home, KeyEvent::KEY_HOME, 0},         {XK_End, KeyEvent::KEY_END, 0},
        {XK_KP_End, KeyEvent::KEY_END, 0},           {XK_Shift_L, KeyEvent::KEY_LSHIFT, 0},
        {XK_Shift_R, KeyEvent::KEY_RSHIFT, 0},       {XK_Control_L, KeyEvent::KEY_LCTRL, 0},
        {XK_Control_R, KeyEvent::KEY_RCTRL, 0},      {XK_Alt_L, KeyEvent::KEY_LALT, 0},
        {XK_Alt_R, KeyEvent::KEY_RALT, 0},           {XK_ISO_Level3_Shift, KeyEvent::KEY_ALTGR, 0},
        {XK_Caps_Lock, KeyEvent::KEY_CAPSLOCK, 0},   {XK_Num_Lock, KeyEvent::KEY_NUMLOCK, 0},
        {XK_Print, KeyEvent::KEY_PRINTSCREEN, 0},
    };
    for (const auto &special : SPECIAL_KEYS) {
        if (special.keysym == keysym) {
            result.keycode = special.keycode;
            result.unicodeId = special.unicode;
            return result;
        }
    }
    if (keysym >= XK_F1 && keysym <= XK_F12) {
        result.keycode = KeyEvent::KEY_F1 + (int)(keysym - XK_F1);
        return result;
    }
    // Control and a letter: the letter of the unshifted symbol.
    if (control && base >= XK_a && base <= XK_z) {
        result.keycode = (shift ? KeyEvent::KEY_A : KeyEvent::KEY_a) + (int)(base - XK_a);
        result.unicodeId = count > 0 ? buffer[0] : (char)KeyEvent::KEY_NONE;
        return result;
    }
    if (count <= 0) {
        return result;
    }
    char c = buffer[0];
    result.unicodeId = c;
    if (c >= 'A' && c <= 'Z') {
        result.keycode = KeyEvent::KEY_A + (c - 'A');
    } else if (c >= 'a' && c <= 'z') {
        result.keycode = KeyEvent::KEY_a + (c - 'a');
    } else if (c >= '0' && c <= '9') {
        result.keycode = keysym >= XK_KP_0 && keysym <= XK_KP_9 ? KeyEvent::KEY_NUM0 + (c - '0')
                                                                : KeyEvent::KEY_0 + (c - '0');
    } else if (c == ',') {
        result.keycode = KeyEvent::KEY_COMMA;
    } else if (c == '.') {
        result.keycode = keysym == XK_KP_Decimal ? KeyEvent::KEY_NUMPERIOD : KeyEvent::KEY_PERIOD;
    } else if (c == ' ') {
        result.keycode = KeyEvent::KEY_SPACE;
    } else if (c == '=') {
        result.keycode = KeyEvent::KEY_EQUALS;
    } else if (c == '-') {
        result.keycode = keysym == XK_KP_Subtract ? KeyEvent::KEY_NUMMINUS : KeyEvent::KEY_MINUS;
    } else if (c == '+' && keysym == XK_KP_Add) {
        result.keycode = KeyEvent::KEY_NUMPLUS;
    } else if (c == '*' && keysym == XK_KP_Multiply) {
        result.keycode = KeyEvent::KEY_NUMASTERISK;
    } else if (c == '/' && keysym == XK_KP_Divide) {
        result.keycode = KeyEvent::KEY_NUMSLASH;
    }
    return result;
}

// Auto repeat sends a release and a press with the same time; the release is dropped.
bool
XlibInput::isAutoRepeat(const XEvent &release) {
    Display *display = surface.getDisplay();
    if (!XEventsQueued(display, QueuedAfterReading)) {
        return false;
    }
    XEvent next;
    XPeekEvent(display, &next);
    return next.type == KeyPress && next.xkey.time == release.xkey.time &&
           next.xkey.keycode == release.xkey.keycode;
}

bool
XlibInput::translate(XEvent &native, FramebufferEvent &event) {
    Display *display = surface.getDisplay();
    event = FramebufferEvent();
    switch (native.type) {
    case Expose:
        surface.expose(native.xexpose.x, native.xexpose.y, native.xexpose.width, native.xexpose.height);
        return false;
    case ConfigureNotify: {
        // Only the last of a burst of new sizes matters.
        XEvent later;
        while (XCheckTypedWindowEvent(display, surface.getWindow(), ConfigureNotify, &later)) {
            native = later;
        }
        if (!surface.resize(native.xconfigure.width, native.xconfigure.height)) {
            return false;
        }
        event.type = FramebufferEvent::Type::Resized;
        return true;
    }
    case ClientMessage:
        if (native.xclient.message_type == protocols && (Atom)native.xclient.data.l[0] == deleteWindow) {
            closed = true;
        }
        return false;
    case KeyPress:
        event.type = FramebufferEvent::Type::KeyPressed;
        event.key = keyEvent(native.xkey);
        return true;
    case KeyRelease:
        if (isAutoRepeat(native)) {
            return false;
        }
        event.type = FramebufferEvent::Type::KeyReleased;
        event.key = keyEvent(native.xkey);
        return true;
    case ButtonPress:
    case ButtonRelease: {
        unsigned int button = native.xbutton.button;
        event.mouse.setX(native.xbutton.x);
        event.mouse.setY(native.xbutton.y);
        if (button >= 4 && button <= 7) {
            // The vertical wheel: up is a negative rotation in AWT. Releases and the horizontal wheel are ignored.
            if (native.type != ButtonPress || button > 5) {
                return false;
            }
            event.type = FramebufferEvent::Type::MouseWheel;
            event.mouse.setModifiers(modifiers(native.xbutton.state));
            event.mouse.setClicks(button == 4 ? -1 : 1);
            return true;
        }
        bool press = native.type == ButtonPress;
        int state = modifiers(native.xbutton.state);
        if (press) {
            int dx = native.xbutton.x - clickX;
            int dy = native.xbutton.y - clickY;
            bool again = button == clickButton && native.xbutton.time - clickTime <= MULTIPLE_CLICK_TIME &&
                         dx * dx + dy * dy <= MULTIPLE_CLICK_DISTANCE * MULTIPLE_CLICK_DISTANCE;
            clicks = again ? clicks + 1 : 1;
            clickButton = button;
            clickTime = native.xbutton.time;
            clickX = native.xbutton.x;
            clickY = native.xbutton.y;
        }
        event.type = press ? FramebufferEvent::Type::MousePressed : FramebufferEvent::Type::MouseReleased;
        event.mouse.setButton((int)button);
        event.mouse.setModifiers(press ? state | buttonMask(button) : state & ~buttonMask(button));
        event.mouse.setClicks(button == clickButton && clicks > 0 ? clicks : 1);
        return true;
    }
    case MotionNotify: {
        XEvent later;
        while (XCheckTypedWindowEvent(display, surface.getWindow(), MotionNotify, &later)) {
            native = later;
        }
        if (warped) {
            warped = false;
            if (native.xmotion.x == warpX && native.xmotion.y == warpY) {
                return false;
            }
        }
        event.type = FramebufferEvent::Type::MouseMoved;
        event.mouse.setX(native.xmotion.x);
        event.mouse.setY(native.xmotion.y);
        event.mouse.setModifiers(modifiers(native.xmotion.state));
        return true;
    }
    case EnterNotify:
        event.type = FramebufferEvent::Type::MouseMoved;
        event.mouse.setX(native.xcrossing.x);
        event.mouse.setY(native.xcrossing.y);
        event.mouse.setModifiers(modifiers(native.xcrossing.state));
        return true;
    default:
        return false;
    }
}

bool
XlibInput::waitEvent(FramebufferEvent &event) {
    Display *display = surface.getDisplay();
    while (display && !closed) {
        if (!XPending(display)) {
            surface.flush();
        }
        XEvent native;
        XNextEvent(display, &native);
        if (translate(native, event)) {
            return true;
        }
    }
    return false;
}

bool
XlibInput::warpPointer(int x, int y) {
    Display *display = surface.getDisplay();
    if (!display) {
        return false;
    }
    XWarpPointer(display, None, surface.getWindow(), 0, 0, 0, 0, x, y);
    warped = true;
    warpX = x;
    warpY = y;
    return true;
}
