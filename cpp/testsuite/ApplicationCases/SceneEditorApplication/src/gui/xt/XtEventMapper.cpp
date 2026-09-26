#include <X11/Xutil.h>
#include <X11/keysym.h>

#include "gui/xt/XtEventMapper.h"

int XtEventMapper::buttonMaskFor(unsigned int button)
{
    switch ( button ) {
      case Button1: return MouseEvent::BUTTON1_DOWN_MASK;
      case Button2: return MouseEvent::BUTTON2_DOWN_MASK;
      case Button3: return MouseEvent::BUTTON3_DOWN_MASK;
      default: return 0;
    }
}

int XtEventMapper::stateToModifiers(unsigned int state)
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

MouseEvent XtEventMapper::toMouseEvent(const XEvent& event)
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

bool XtEventMapper::isWheelButton(const XEvent& event)
{
    return event.xbutton.button >= 4 && event.xbutton.button <= 7;
}

MouseEvent XtEventMapper::toMouseWheelEvent(const XEvent& event)
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

KeyEvent XtEventMapper::toKeyEvent(XKeyEvent& event)
{
    char buffer[8] = {0};
    KeySym keysym = NoSymbol;
    int count = XLookupString(&event, buffer, sizeof(buffer) - 1, &keysym,
                              nullptr);

    return toKeyEvent(keysym, XLookupKeysym(&event, 0), buffer, count,
                      event.state);
}

KeyEvent XtEventMapper::toKeyEvent(KeySym keysym, KeySym base,
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
