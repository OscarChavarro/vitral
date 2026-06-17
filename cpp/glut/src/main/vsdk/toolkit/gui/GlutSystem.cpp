#include "GlutSystem.h"
#ifdef __APPLE__
#include <GLUT/glut.h>
#else
#include <GL/glut.h>
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

    event.unicode_id = (char)glutKey;

    switch (glutKey) {
        case 27:  // ESC
            event.keycode = KeyEvent::KEY_ESC;
            event.unicode_id = KeyEvent::KEY_NONE;
            break;
        case 8:   // Backspace
            event.keycode = KeyEvent::KEY_BACKSPACE;
            event.unicode_id = KeyEvent::KEY_NONE;
            break;
        case 9:   // Tab
            event.keycode = KeyEvent::KEY_TAB;
            event.unicode_id = KeyEvent::KEY_NONE;
            break;
        case 10:  // Enter/newline
        case 13:  // Carriage return
            event.keycode = KeyEvent::KEY_ENTER;
            event.unicode_id = KeyEvent::KEY_NONE;
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

    event.unicode_id = KeyEvent::KEY_NONE;

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
