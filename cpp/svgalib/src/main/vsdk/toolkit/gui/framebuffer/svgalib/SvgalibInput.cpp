#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <vga.h>
#include <vgamouse.h>
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/svgalib/SvgalibInput.h"

static constexpr int ESCAPE = 27;
static constexpr int NUMBER_TEXT_SIZE = 16;
static constexpr int MODIFY_OTHER_KEYS = 27;

// The svgalib buttons in the order of Vitral's BUTTON1 to BUTTON3.
static const int SVGALIB_BUTTONS[3] = {MOUSE_LEFTBUTTON, MOUSE_MIDDLEBUTTON, MOUSE_RIGHTBUTTON};

// The Linux mousedev driver scales absolute pointers, like the ones of a virtual machine, to this screen size.
static const char *const MOUSEDEV_WIDTH_FILE = "/sys/module/mousedev/parameters/xres";
static const char *const MOUSEDEV_HEIGHT_FILE = "/sys/module/mousedev/parameters/yres";

static int
readNumber(const char *path) {
    FILE *file = fopen(path, "r");
    if (!file) {
        return -1;
    }
    char text[NUMBER_TEXT_SIZE];
    int number = fgets(text, sizeof(text), file) ? atoi(text) : -1;
    fclose(file);
    return number;
}

static void
writeNumber(const char *path, int number) {
    FILE *file = fopen(path, "w");
    if (file) {
        fprintf(file, "%d\n", number);
        fclose(file);
    }
}

static int
clampTo(int value, int last) {
    return value < 0 ? 0 : value > last ? last : value;
}

SvgalibInput::SvgalibInput()
    : connected(false), attached(false), surface(nullptr), buttons(0), lastX(-1), lastY(-1), savedScaleWidth(-1),
      savedScaleHeight(-1), queued(0) {}

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
    mouse_update();
    mouse_setscale(1);
    mouse_setxrange(0, width - 1);
    mouse_setyrange(0, height - 1);
    int startX = width / 2;
    int startY = height / 2;
    fitAbsolutePointer(width, height, startX, startY);
    mouse_setposition(startX, startY);
    buttons = mouse_getbutton();
    lastX = startX;
    lastY = startY;
    target.showPointer(lastX, lastY);
}

// Makes a whole movement of an absolute pointer cover the screen. mousedev reports the first position as a movement
// from the center of the scale it had when vga_init() opened the mouse, with its vertical axis going up: starting from
// that point instead of the center of the screen leaves the drawn pointer exactly under the real one.
void
SvgalibInput::fitAbsolutePointer(int width, int height, int &startX, int &startY) {
    int scaleWidth = readNumber(MOUSEDEV_WIDTH_FILE);
    int scaleHeight = readNumber(MOUSEDEV_HEIGHT_FILE);
    if (scaleWidth <= 0 || scaleHeight <= 0) {
        return;
    }
    savedScaleWidth = scaleWidth;
    savedScaleHeight = scaleHeight;
    writeNumber(MOUSEDEV_WIDTH_FILE, width);
    writeNumber(MOUSEDEV_HEIGHT_FILE, height);
    startX = clampTo(scaleWidth / 2, width - 1);
    startY = clampTo(height - scaleHeight / 2, height - 1);
}

void
SvgalibInput::restoreAbsolutePointer() {
    if (savedScaleWidth > 0) {
        writeNumber(MOUSEDEV_WIDTH_FILE, savedScaleWidth);
        writeNumber(MOUSEDEV_HEIGHT_FILE, savedScaleHeight);
        savedScaleWidth = -1;
        savedScaleHeight = -1;
    }
}

void
SvgalibInput::detach() {
    if (!attached) {
        return;
    }
    surface->hidePointer();
    surface = nullptr;
    attached = false;
    restoreAbsolutePointer();
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
        surface->movePointer(x, y);
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

// xterm's modifyOtherKeys form of a key with modifiers: CSI 27;modifiers;code~, where modifiers is 1 plus 1 for Shift,
// 2 for Alt and 4 for Control, and code is the character of the key (Control and Shift with R: CSI 27;6;82~).
void
SvgalibInput::modifiedKey(int modifiers, int code, KeyEvent &event) {
    int bits = modifiers - 1;
    event.keycode = keyFor(code);
    event.modifierMask = ((bits & 1) ? KeyEvent::MASK_SHIFT : 0) | ((bits & 2) ? KeyEvent::MASK_ALT : 0) |
                         ((bits & 4) ? KeyEvent::MASK_CTRL : 0);
    if ((bits & 4) == 0 && code > 0 && code < 0x80) {
        event.unicodeId = (char)code;
    }
}

// One key from the terminal: a byte, Control plus a letter, Alt plus a key (Escape then the key), or an escape
// sequence: CSI A to D for the arrows, H and F for Home and End, CSI [ A to E for F1 to F5 (Linux console) and
// CSI number ~ for the editing and function keys, and CSI 27;modifiers;code~ for keys with modifiers.
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
    int numbers[3] = {0, 0, 0};
    int count = 0;
    while (count < 3) {
        while (code >= '0' && code <= '9') {
            numbers[count] = numbers[count] * 10 + (code - '0');
            code = vga_getkey() & 0xFF;
        }
        count++;
        if (code != ';') {
            break;
        }
        code = vga_getkey() & 0xFF;
    }
    if (code != '~') {
        return true;
    }
    if (count == 3 && numbers[0] == MODIFY_OTHER_KEYS) {
        modifiedKey(numbers[1], numbers[2], event);
        return true;
    }
    int number = numbers[0];
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
        if (attached) {
            surface->flush();
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
