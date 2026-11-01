#include <bios.h>
#include <dos.h>
#include <stddef.h>
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiFramebuffer.h"
#include "vsdk/toolkit/gui/framebuffer/bgi/BgiInput.h"

enum { ESCAPE = 27 };

// The bits of the mouse driver buttons in the order of Vitral's BUTTON1 to BUTTON3: left, middle and right.
static const int MOUSE_BUTTONS[3] = {1, 4, 2};

// The letters of the scan codes 10h to 32h, as Alt gives them with no character.
static const char ALT_LETTERS[] = "qwertyuiop[]\r\0asdfghjkl;'`\0\\zxcvbnm";

static void
mouse(int function, int &bx, int &cx, int &dx)
{
    union REGS registers;
    registers.x.ax = function;
    registers.x.bx = bx;
    registers.x.cx = cx;
    registers.x.dx = dx;
    int86(0x33, &registers, &registers);
    bx = registers.x.bx;
    cx = registers.x.cx;
    dx = registers.x.dx;
}

BgiInput::BgiInput()
    : connected(FALSE), attached(FALSE), surface(NULL), buttons(0), lastX(-1), lastY(-1), queued(0)
{
}

BgiInput::~BgiInput()
{
    close();
}

void
BgiInput::open()
{
    // The interrupt vector is empty on machines without a mouse driver.
    if ( getvect(0x33) == NULL ) {
        connected = FALSE;
        return;
    }
    union REGS registers;
    registers.x.ax = 0;
    int86(0x33, &registers, &registers);
    connected = registers.x.ax == 0xFFFF;
}

void
BgiInput::close()
{
    detach();
    connected = FALSE;
}

void
BgiInput::attach(BgiFramebuffer &target)
{
    detach();
    surface = &target;
    attached = TRUE;
    queued = 0;
    if ( !connected ) {
        return;
    }
    int width = target.width();
    int height = target.height();
    cursor.use(&target, target.native(0xFFFFFFUL), target.native(0x000000UL));
    int bx = 0;
    int cx = 0;
    int dx = width - 1;
    mouse(7, bx, cx, dx);
    cx = 0;
    dx = height - 1;
    mouse(8, bx, cx, dx);
    cx = width / 2;
    dx = height / 2;
    mouse(4, bx, cx, dx);
    mouse(3, bx, cx, dx);
    buttons = bx;
    lastX = width / 2;
    lastY = height / 2;
    cursor.moveTo(lastX, lastY);
    target.setPointer(&cursor);
    cursor.show();
}

void
BgiInput::detach()
{
    if ( !attached ) {
        return;
    }
    cursor.hide();
    surface->setPointer(NULL);
    surface = NULL;
    attached = FALSE;
}

void
BgiInput::push(const FramebufferEvent &event)
{
    if ( queued < QUEUE_SIZE ) {
        queue[queued++] = event;
    }
}

int
BgiInput::buttonMask(int button)
{
    if ( button == MouseEvent::BUTTON1 ) {
        return MouseEvent::BUTTON1_DOWN_MASK;
    }
    if ( button == MouseEvent::BUTTON2 ) {
        return MouseEvent::BUTTON2_DOWN_MASK;
    }
    return MouseEvent::BUTTON3_DOWN_MASK;
}

// Queues a MouseMoved event when the pointer moved, then one press or release for every button that changed.
void
BgiInput::readMouse()
{
    int now = 0;
    int x = 0;
    int y = 0;
    mouse(3, now, x, y);
    int modifiers = 0;
    int i;
    for ( i = 0; i < 3; i++ ) {
        if ( buttons & MOUSE_BUTTONS[i] ) {
            modifiers |= buttonMask(i + 1);
        }
    }
    FramebufferEvent event;
    event.mouse.setX(x);
    event.mouse.setY(y);
    if ( x != lastX || y != lastY ) {
        lastX = x;
        lastY = y;
        cursor.moveTo(x, y);
        event.type = FramebufferEvent::MouseMoved;
        event.mouse.setModifiers(modifiers);
        push(event);
    }
    for ( i = 0; i < 3; i++ ) {
        int bit = MOUSE_BUTTONS[i];
        if ( (buttons ^ now) & bit ) {
            BOOLEAN down = (now & bit) != 0;
            modifiers = down ? modifiers | buttonMask(i + 1) : modifiers & ~buttonMask(i + 1);
            event.type = down ? FramebufferEvent::MousePressed : FramebufferEvent::MouseReleased;
            event.mouse.setButton(i + 1);
            event.mouse.setModifiers(modifiers);
            event.mouse.setClicks(down ? 1 : 0);
            push(event);
        }
    }
    buttons = now;
}

int
BgiInput::keyFor(int c)
{
    if ( c >= 'a' && c <= 'z' ) {
        return KeyEvent::KEY_a + (c - 'a');
    }
    if ( c >= 'A' && c <= 'Z' ) {
        return KeyEvent::KEY_A + (c - 'A');
    }
    if ( c >= '0' && c <= '9' ) {
        return KeyEvent::KEY_0 + (c - '0');
    }
    switch ( c ) {
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

// The keys without a character: the BIOS scan codes of the arrows, the editing keys, F1 to F12 and Alt plus a letter.
int
BgiInput::extendedKey(int scan, int &modifiers, char &letter)
{
    static const int SCAN_KEYS[][2] = {
        {0x48, KeyEvent::KEY_UP},       {0x50, KeyEvent::KEY_DOWN},     {0x4B, KeyEvent::KEY_LEFT},
        {0x4D, KeyEvent::KEY_RIGHT},    {0x47, KeyEvent::KEY_HOME},     {0x4F, KeyEvent::KEY_END},
        {0x49, KeyEvent::KEY_PAGEUP},   {0x51, KeyEvent::KEY_PAGEDOWN}, {0x52, KeyEvent::KEY_INSERT},
        {0x53, KeyEvent::KEY_DELETE},   {0x3B, KeyEvent::KEY_F1},       {0x3C, KeyEvent::KEY_F2},
        {0x3D, KeyEvent::KEY_F3},       {0x3E, KeyEvent::KEY_F4},       {0x3F, KeyEvent::KEY_F5},
        {0x40, KeyEvent::KEY_F6},       {0x41, KeyEvent::KEY_F7},       {0x42, KeyEvent::KEY_F8},
        {0x43, KeyEvent::KEY_F9},       {0x44, KeyEvent::KEY_F10},      {0x85, KeyEvent::KEY_F11},
        {0x86, KeyEvent::KEY_F12},
    };
    int i;
    for ( i = 0; i < (int)(sizeof(SCAN_KEYS) / sizeof(SCAN_KEYS[0])); i++ ) {
        if ( SCAN_KEYS[i][0] == scan ) {
            return SCAN_KEYS[i][1];
        }
    }
    if ( scan >= 0x10 && scan <= 0x32 && ALT_LETTERS[scan - 0x10] >= 'a' && ALT_LETTERS[scan - 0x10] <= 'z' ) {
        letter = ALT_LETTERS[scan - 0x10];
        modifiers = KeyEvent::MASK_ALT;
        return keyFor(letter);
    }
    return KeyEvent::KEY_NONE;
}

// One key from the BIOS: a character, Control plus a letter, or a key without a character (its scan code).
BOOLEAN
BgiInput::readKey(KeyEvent &event)
{
    if ( bioskey(0x11) == 0 ) {
        return FALSE;
    }
    unsigned key = (unsigned)bioskey(0x10);
    int c = key & 0xFF;
    int scan = key >> 8;
    event = KeyEvent();
    if ( scan == 0xE0 ) {
        // The keys of the numeric keypad that have a character.
        event.keycode = c == '\r' ? (int)KeyEvent::KEY_NUMENTER : keyFor(c);
        event.unicodeId = (char)c;
        return TRUE;
    }
    if ( c == 0 || c == 0xE0 ) {
        int modifiers = 0;
        char letter = 0;
        event.keycode = extendedKey(scan, modifiers, letter);
        event.modifierMask = modifiers;
        event.unicodeId = letter;
        return TRUE;
    }
    if ( c >= 1 && c <= 26 && c != '\t' && c != '\n' && c != '\r' && c != '\b' ) {
        event.keycode = KeyEvent::KEY_a + (c - 1);
        event.modifierMask = KeyEvent::MASK_CTRL;
        return TRUE;
    }
    event.keycode = keyFor(c);
    event.unicodeId = (char)c;
    return TRUE;
}

BOOLEAN
BgiInput::waitEvent(FramebufferEvent &event)
{
    for ( ;; ) {
        if ( queued > 0 ) {
            event = queue[0];
            int i;
            for ( i = 1; i < queued; i++ ) {
                queue[i - 1] = queue[i];
            }
            queued--;
            return TRUE;
        }
        if ( readKey(event.key) ) {
            event.type = FramebufferEvent::KeyPressed;
            return TRUE;
        }
        if ( connected && attached ) {
            readMouse();
        }
        if ( queued == 0 ) {
            // Until the next interrupt: the timer, the keyboard or the mouse. Interrupts are enabled first (sti;
            // hlt), as a BIOS or driver call may leave them disabled, and hlt would then never return.
            __emit__(0xFB, 0xF4);
        }
    }
}
