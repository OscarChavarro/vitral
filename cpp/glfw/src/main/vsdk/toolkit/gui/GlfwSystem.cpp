#include "GlfwSystem.h"
#include <GLFW/glfw3.h>
#ifdef __APPLE__
#define GLFW_INCLUDE_GLCOREARB
#endif
KeyEvent GlfwSystem::glfw2vsdkKeyEvent(int glfwKey, int glfwMods) {
    KeyEvent event;

    if (glfwMods & GLFW_MOD_SHIFT) {
        event.modifierMask |= KeyEvent::MASK_SHIFT;
    }
    if (glfwMods & GLFW_MOD_CONTROL) {
        event.modifierMask |= KeyEvent::MASK_CTRL;
    }
    if (glfwMods & GLFW_MOD_ALT) {
        event.modifierMask |= KeyEvent::MASK_ALT;
    }

    switch (glfwKey) {
        case GLFW_KEY_ESCAPE:
            event.keycode = KeyEvent::KEY_ESC;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_BACKSPACE:
            event.keycode = KeyEvent::KEY_BACKSPACE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_TAB:
            event.keycode = KeyEvent::KEY_TAB;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_ENTER:
            event.keycode = KeyEvent::KEY_ENTER;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_SPACE:
            event.keycode = KeyEvent::KEY_SPACE;
            event.unicodeId = ' ';
            break;

        case GLFW_KEY_A:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_A : KeyEvent::KEY_a;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'A' : 'a';
            break;
        case GLFW_KEY_B:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_B : KeyEvent::KEY_b;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'B' : 'b';
            break;
        case GLFW_KEY_C:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_C : KeyEvent::KEY_c;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'C' : 'c';
            break;
        case GLFW_KEY_D:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_D : KeyEvent::KEY_d;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'D' : 'd';
            break;
        case GLFW_KEY_E:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_E : KeyEvent::KEY_e;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'E' : 'e';
            break;
        case GLFW_KEY_F:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_F : KeyEvent::KEY_f;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'F' : 'f';
            break;
        case GLFW_KEY_G:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_G : KeyEvent::KEY_g;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'G' : 'g';
            break;
        case GLFW_KEY_H:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_H : KeyEvent::KEY_h;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'H' : 'h';
            break;
        case GLFW_KEY_I:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_I : KeyEvent::KEY_i;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'I' : 'i';
            break;
        case GLFW_KEY_J:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_J : KeyEvent::KEY_j;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'J' : 'j';
            break;
        case GLFW_KEY_K:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_K : KeyEvent::KEY_k;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'K' : 'k';
            break;
        case GLFW_KEY_L:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_L : KeyEvent::KEY_l;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'L' : 'l';
            break;
        case GLFW_KEY_M:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_M : KeyEvent::KEY_m;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'M' : 'm';
            break;
        case GLFW_KEY_N:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_N : KeyEvent::KEY_n;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'N' : 'n';
            break;
        case GLFW_KEY_O:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_O : KeyEvent::KEY_o;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'O' : 'o';
            break;
        case GLFW_KEY_P:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_P : KeyEvent::KEY_p;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'P' : 'p';
            break;
        case GLFW_KEY_Q:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_Q : KeyEvent::KEY_q;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'Q' : 'q';
            break;
        case GLFW_KEY_R:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_R : KeyEvent::KEY_r;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'R' : 'r';
            break;
        case GLFW_KEY_S:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_S : KeyEvent::KEY_s;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'S' : 's';
            break;
        case GLFW_KEY_T:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_T : KeyEvent::KEY_t;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'T' : 't';
            break;
        case GLFW_KEY_U:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_U : KeyEvent::KEY_u;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'U' : 'u';
            break;
        case GLFW_KEY_V:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_V : KeyEvent::KEY_v;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'V' : 'v';
            break;
        case GLFW_KEY_W:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_W : KeyEvent::KEY_w;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'W' : 'w';
            break;
        case GLFW_KEY_X:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_X : KeyEvent::KEY_x;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'X' : 'x';
            break;
        case GLFW_KEY_Y:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_Y : KeyEvent::KEY_y;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'Y' : 'y';
            break;
        case GLFW_KEY_Z:
            event.keycode = (glfwMods & GLFW_MOD_SHIFT) ? KeyEvent::KEY_Z : KeyEvent::KEY_z;
            event.unicodeId = (glfwMods & GLFW_MOD_SHIFT) ? 'Z' : 'z';
            break;

        case GLFW_KEY_0:
            event.keycode = KeyEvent::KEY_0;
            event.unicodeId = '0';
            break;
        case GLFW_KEY_1:
            event.keycode = KeyEvent::KEY_1;
            event.unicodeId = '1';
            break;
        case GLFW_KEY_2:
            event.keycode = KeyEvent::KEY_2;
            event.unicodeId = '2';
            break;
        case GLFW_KEY_3:
            event.keycode = KeyEvent::KEY_3;
            event.unicodeId = '3';
            break;
        case GLFW_KEY_4:
            event.keycode = KeyEvent::KEY_4;
            event.unicodeId = '4';
            break;
        case GLFW_KEY_5:
            event.keycode = KeyEvent::KEY_5;
            event.unicodeId = '5';
            break;
        case GLFW_KEY_6:
            event.keycode = KeyEvent::KEY_6;
            event.unicodeId = '6';
            break;
        case GLFW_KEY_7:
            event.keycode = KeyEvent::KEY_7;
            event.unicodeId = '7';
            break;
        case GLFW_KEY_8:
            event.keycode = KeyEvent::KEY_8;
            event.unicodeId = '8';
            break;
        case GLFW_KEY_9:
            event.keycode = KeyEvent::KEY_9;
            event.unicodeId = '9';
            break;
        case GLFW_KEY_PERIOD:
            event.keycode = KeyEvent::KEY_PERIOD;
            event.unicodeId = '.';
            break;

        case GLFW_KEY_F1:
            event.keycode = KeyEvent::KEY_F1;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F2:
            event.keycode = KeyEvent::KEY_F2;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F3:
            event.keycode = KeyEvent::KEY_F3;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F4:
            event.keycode = KeyEvent::KEY_F4;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F5:
            event.keycode = KeyEvent::KEY_F5;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F6:
            event.keycode = KeyEvent::KEY_F6;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F7:
            event.keycode = KeyEvent::KEY_F7;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F8:
            event.keycode = KeyEvent::KEY_F8;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F9:
            event.keycode = KeyEvent::KEY_F9;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F10:
            event.keycode = KeyEvent::KEY_F10;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F11:
            event.keycode = KeyEvent::KEY_F11;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_F12:
            event.keycode = KeyEvent::KEY_F12;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;

        case GLFW_KEY_UP:
            event.keycode = KeyEvent::KEY_UP;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_DOWN:
            event.keycode = KeyEvent::KEY_DOWN;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_LEFT:
            event.keycode = KeyEvent::KEY_LEFT;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_RIGHT:
            event.keycode = KeyEvent::KEY_RIGHT;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;

        case GLFW_KEY_HOME:
            event.keycode = KeyEvent::KEY_HOME;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_END:
            event.keycode = KeyEvent::KEY_END;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_PAGE_UP:
            event.keycode = KeyEvent::KEY_PAGEUP;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_PAGE_DOWN:
            event.keycode = KeyEvent::KEY_PAGEDOWN;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_INSERT:
            event.keycode = KeyEvent::KEY_INSERT;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
        case GLFW_KEY_DELETE:
            event.keycode = KeyEvent::KEY_DELETE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;

        default:
            event.keycode = KeyEvent::KEY_NONE;
            event.unicodeId = KeyEvent::KEY_NONE;
            break;
    }

    return event;
}

KeyEvent GlfwSystem::glfw2vsdkSpecialKeyEvent(int glfwKey, int glfwMods) {
    return glfw2vsdkKeyEvent(glfwKey, glfwMods);
}

MouseEvent GlfwSystem::glfw2vsdkMouseEvent(int glfwButton, int glfwAction, double x, double y) {
    MouseEvent event;
    event.setX((int)x);
    event.setY((int)y);

    switch (glfwButton) {
        case GLFW_MOUSE_BUTTON_LEFT:
            event.setButton(MouseEvent::BUTTON1);
            break;
        case GLFW_MOUSE_BUTTON_MIDDLE:
            event.setButton(MouseEvent::BUTTON2);
            break;
        case GLFW_MOUSE_BUTTON_RIGHT:
            event.setButton(MouseEvent::BUTTON3);
            break;
        default:
            event.setButton(0);
            break;
    }

    int modifiers = 0;
    if (glfwAction == GLFW_PRESS) {
        switch (glfwButton) {
            case GLFW_MOUSE_BUTTON_LEFT:
                modifiers |= MouseEvent::BUTTON1_DOWN_MASK;
                break;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                modifiers |= MouseEvent::BUTTON2_DOWN_MASK;
                break;
            case GLFW_MOUSE_BUTTON_RIGHT:
                modifiers |= MouseEvent::BUTTON3_DOWN_MASK;
                break;
        }
    }
    event.setModifiers(modifiers);

    return event;
}

MouseEvent GlfwSystem::glfw2vsdkMotionEvent(double x, double y) {
    MouseEvent event;
    event.setX((int)x);
    event.setY((int)y);
    return event;
}

MouseEvent GlfwSystem::glfw2vsdkWheelEvent(double xoffset, double yoffset) {
    MouseEvent event;
    event.setClicks((int)yoffset);
    return event;
}
