#include <cctype>

#include "vsdk/toolkit/gui/Gtk4System.h"

MouseEvent Gtk4System::mouseEvent(double x, double y, guint button,
                                  GdkModifierType state)
{
    MouseEvent event;
    event.setX(static_cast<int>(x));
    event.setY(static_cast<int>(y));
    event.setButton(static_cast<int>(button));
    int modifiers = 0;
    if ( state & GDK_CONTROL_MASK ) modifiers |= MouseEvent::CTRL_DOWN_MASK;
    if ( state & GDK_BUTTON1_MASK ) modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
    if ( state & GDK_BUTTON2_MASK ) modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
    if ( state & GDK_BUTTON3_MASK ) modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
    event.setModifiers(modifiers);
    return event;
}

KeyEvent Gtk4System::keyEvent(guint keyval, GdkModifierType state)
{
    KeyEvent event;
    int code = KeyEvent::KEY_NONE;
    gunichar unicode = gdk_keyval_to_unicode(keyval);
    if ( unicode >= 'a' && unicode <= 'z' ) {
        code = KeyEvent::KEY_a + static_cast<int>(unicode - 'a');
    }
    else if ( unicode >= 'A' && unicode <= 'Z' ) {
        code = KeyEvent::KEY_A + static_cast<int>(unicode - 'A');
    }
    else if ( unicode >= '0' && unicode <= '9' ) {
        code = KeyEvent::KEY_0 + static_cast<int>(unicode - '0');
    }
    else {
        switch ( keyval ) {
        case GDK_KEY_Escape: code = KeyEvent::KEY_ESC; break;
        case GDK_KEY_Delete: code = KeyEvent::KEY_DELETE; break;
        case GDK_KEY_BackSpace: code = KeyEvent::KEY_BACKSPACE; break;
        case GDK_KEY_Return: code = KeyEvent::KEY_ENTER; break;
        case GDK_KEY_Tab: code = KeyEvent::KEY_TAB; break;
        case GDK_KEY_Left: code = KeyEvent::KEY_LEFT; break;
        case GDK_KEY_Right: code = KeyEvent::KEY_RIGHT; break;
        case GDK_KEY_Up: code = KeyEvent::KEY_UP; break;
        case GDK_KEY_Down: code = KeyEvent::KEY_DOWN; break;
        case GDK_KEY_F10: code = KeyEvent::KEY_F10; break;
        case GDK_KEY_minus: code = KeyEvent::KEY_MINUS; break;
        case GDK_KEY_equal: code = KeyEvent::KEY_EQUALS; break;
        case GDK_KEY_period: code = KeyEvent::KEY_PERIOD; break;
        case GDK_KEY_comma: code = KeyEvent::KEY_COMMA; break;
        case GDK_KEY_space: code = KeyEvent::KEY_SPACE; break;
        default: break;
        }
    }

    event.keycode = code;
    event.unicodeId = unicode <= 0x7f ? static_cast<char>(unicode) : 0;
    event.modifierMask = 0;
    if ( state & GDK_CONTROL_MASK ) event.modifierMask |= KeyEvent::MASK_CTRL;
    if ( state & GDK_ALT_MASK ) event.modifierMask |= KeyEvent::MASK_ALT;
    if ( state & GDK_SHIFT_MASK ) event.modifierMask |= KeyEvent::MASK_SHIFT;
    return event;
}
