#include <unistd.h>
#include <vga.h>
#include <vgamouse.h>
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.h"

static constexpr int ESCAPE = 27;

// The svgalib buttons in the order of Vitral's BUTTON1 to BUTTON3.
static const int SVGALIB_BUTTONS[3] = {MOUSE_LEFTBUTTON, MOUSE_MIDDLEBUTTON, MOUSE_RIGHTBUTTON};

SvgalibInput::SvgalibInput()
    : connected(false), attached(false), surface(nullptr), buttons(0), lastX(-1), lastY(-1), queued(0) {}

SvgalibInput::~SvgalibInput() {
    close();
}

void
SvgalibInput::open() {
    MouseCaps caps;
    connected = mouse_getcaps(&caps) == 0;
}

void
SvgalibInput::close() {
    detach();
    if (connected) {
        mouse_close();
        connected = false;
    }
}

void
SvgalibInput::attach(SvgalibFramebuffer &target) {
    detach();
    surface = &target;
    attached = true;
    queued = 0;
    if (!connected) {
        return;
    }
    int width = target.width();
    int height = target.height();
    cursor.useGraphics(width, height, (int)target.native(0xFFFFFF), (int)target.native(0x000000));
    mouse_update();
    mouse_setscale(1);
    mouse_setxrange(0, width - 1);
    mouse_setyrange(0, height - 1);
    mouse_setposition(width / 2, height / 2);
    buttons = mouse_getbutton();
    lastX = width / 2;
    lastY = height / 2;
    cursor.moveTo(lastX, lastY);
    target.setPointer(&cursor);
    cursor.show();
}

void
SvgalibInput::detach() {
    if (!attached) {
        return;
    }
    cursor.hide();
    surface->setPointer(nullptr);
    surface = nullptr;
    attached = false;
}

void
SvgalibInput::push(const FramebufferEvent &event) {
    if (queued < QUEUE_SIZE) {
        queue[queued++] = event;
    }
}

int
SvgalibInput::buttonMask(int button) {
    if (button == MouseEvent::BUTTON1) {
        return MouseEvent::BUTTON1_DOWN_MASK;
    }
    if (button == MouseEvent::BUTTON2) {
        return MouseEvent::BUTTON2_DOWN_MASK;
    }
    return MouseEvent::BUTTON3_DOWN_MASK;
}

// Queues a MouseMoved event when the pointer moved, then one press or release for every button that changed.
void
SvgalibInput::readMouse() {
    int x = mouse_getx();
    int y = mouse_gety();
    int now = mouse_getbutton();
    int modifiers = 0;
    for (int i = 0; i < 3; i++) {
        if (buttons & SVGALIB_BUTTONS[i]) {
            modifiers |= buttonMask(i + 1);
        }
    }
    FramebufferEvent event;
    event.mouse.setX(x);
    event.mouse.setY(y);
    if (x != lastX || y != lastY) {
        lastX = x;
        lastY = y;
        cursor.moveTo(x, y);
        event.type = FramebufferEvent::Type::MouseMoved;
        event.mouse.setModifiers(modifiers);
        push(event);
    }
    for (int i = 0; i < 3; i++) {
        int bit = SVGALIB_BUTTONS[i];
        if ((buttons ^ now) & bit) {
            bool down = (now & bit) != 0;
            modifiers = down ? modifiers | buttonMask(i + 1) : modifiers & ~buttonMask(i + 1);
            event.type = down ? FramebufferEvent::Type::MousePressed : FramebufferEvent::Type::MouseReleased;
            event.mouse.setButton(i + 1);
            event.mouse.setModifiers(modifiers);
            event.mouse.setClicks(down ? 1 : 0);
            push(event);
        }
    }
    buttons = now;
}

int
SvgalibInput::keyFor(int c) {
    if (c >= 'a' && c <= 'z') {
        return KeyEvent::KEY_a + (c - 'a');
    }
    if (c >= 'A' && c <= 'Z') {
        return KeyEvent::KEY_A + (c - 'A');
    }
    if (c >= '0' && c <= '9') {
        return KeyEvent::KEY_0 + (c - '0');
    }
    switch (c) {
    case '\r':
    case '\n':
        return KeyEvent::KEY_ENTER;
    case '\t':
        return KeyEvent::KEY_TAB;
    case '\b':
    case 127:
        return KeyEvent::KEY_BACKSPACE;
    case ' ':
        return KeyEvent::KEY_SPACE;
    case ',':
        return KeyEvent::KEY_COMMA;
    case '.':
        return KeyEvent::KEY_PERIOD;
    case '=':
        return KeyEvent::KEY_EQUALS;
    case '-':
        return KeyEvent::KEY_MINUS;
    case ESCAPE:
        return KeyEvent::KEY_ESC;
    default:
        return KeyEvent::KEY_NONE;
    }
}

// One key from the terminal: a byte, Control plus a letter, Alt plus a key (Escape then the key), or an escape
// sequence: CSI A to D for the arrows, H and F for Home and End, CSI [ A to E for F1 to F5 (Linux console) and
// CSI number ~ for the editing and function keys.
bool
SvgalibInput::readKey(KeyEvent &event) {
    int c = vga_getkey();
    if (c == 0) {
        return false;
    }
    event = KeyEvent();
    c &= 0xFF;
    if (c != ESCAPE) {
        if (c >= 1 && c <= 26 && c != '\t' && c != '\n' && c != '\r' && c != '\b') {
            event.keycode = KeyEvent::KEY_a + (c - 1);
            event.modifierMask = KeyEvent::MASK_CTRL;
            return true;
        }
        event.keycode = keyFor(c);
        event.unicodeId = (char)c;
        return true;
    }
    int next = vga_getkey() & 0xFF;
    if (next == 0) {
        event.keycode = KeyEvent::KEY_ESC;
        event.unicodeId = (char)ESCAPE;
        return true;
    }
    if (next != '[' && next != 'O') {
        event.keycode = keyFor(next);
        event.unicodeId = (char)next;
        event.modifierMask = KeyEvent::MASK_ALT;
        return true;
    }
    int code = vga_getkey() & 0xFF;
    switch (code) {
    case 'A':
        event.keycode = KeyEvent::KEY_UP;
        return true;
    case 'B':
        event.keycode = KeyEvent::KEY_DOWN;
        return true;
    case 'C':
        event.keycode = KeyEvent::KEY_RIGHT;
        return true;
    case 'D':
        event.keycode = KeyEvent::KEY_LEFT;
        return true;
    case 'H':
        event.keycode = KeyEvent::KEY_HOME;
        return true;
    case 'F':
        event.keycode = KeyEvent::KEY_END;
        return true;
    case 'P':
    case 'Q':
    case 'R':
    case 'S':
        event.keycode = KeyEvent::KEY_F1 + (code - 'P');
        return true;
    case '[': {
        int function = vga_getkey() & 0xFF;
        if (function >= 'A' && function <= 'E') {
            event.keycode = KeyEvent::KEY_F1 + (function - 'A');
        }
        return true;
    }
    default:
        break;
    }
    int number = 0;
    while (code >= '0' && code <= '9') {
        number = number * 10 + (code - '0');
        code = vga_getkey() & 0xFF;
    }
    if (code != '~') {
        return true;
    }
    static const int TILDE_KEYS[][2] = {
        {1, KeyEvent::KEY_HOME},    {2, KeyEvent::KEY_INSERT},  {3, KeyEvent::KEY_DELETE}, {4, KeyEvent::KEY_END},
        {5, KeyEvent::KEY_PAGEUP},  {6, KeyEvent::KEY_PAGEDOWN}, {7, KeyEvent::KEY_HOME},  {8, KeyEvent::KEY_END},
        {11, KeyEvent::KEY_F1},     {12, KeyEvent::KEY_F2},     {13, KeyEvent::KEY_F3},    {14, KeyEvent::KEY_F4},
        {15, KeyEvent::KEY_F5},     {17, KeyEvent::KEY_F6},     {18, KeyEvent::KEY_F7},    {19, KeyEvent::KEY_F8},
        {20, KeyEvent::KEY_F9},     {21, KeyEvent::KEY_F10},    {23, KeyEvent::KEY_F11},   {24, KeyEvent::KEY_F12},
    };
    for (const int *pair : TILDE_KEYS) {
        if (pair[0] == number) {
            event.keycode = pair[1];
        }
    }
    return true;
}

bool
SvgalibInput::waitEvent(FramebufferEvent &event) {
    for (;;) {
        if (queued > 0) {
            event = queue[0];
            for (int i = 1; i < queued; i++) {
                queue[i - 1] = queue[i];
            }
            queued--;
            return true;
        }
        if (readKey(event.key)) {
            event.type = FramebufferEvent::Type::KeyPressed;
            return true;
        }
        int events = vga_waitevent(VGA_KEYEVENT | (connected ? VGA_MOUSEEVENT : 0), nullptr, nullptr, nullptr,
                                   nullptr);
        if (events < 0) {
            usleep(20000);
        } else if ((events & VGA_MOUSEEVENT) && attached) {
            readMouse();
        }
    }
}
