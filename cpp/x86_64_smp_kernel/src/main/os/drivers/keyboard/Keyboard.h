#ifndef __KEYBOARD__
#define __KEYBOARD__
#include <stdint.h>
#include "kernel/TrapFrame.h"

class Keyboard {
public:
    static constexpr int KEY_ESC = 27;
    static constexpr int KEY_ENTER = '\n';
    static constexpr int KEY_UP = 0x100;
    static constexpr int KEY_DOWN = 0x101;
    static constexpr int KEY_LEFT = 0x102;
    static constexpr int KEY_RIGHT = 0x103;
    static constexpr int KEY_PAGE_UP = 0x104;
    static constexpr int KEY_PAGE_DOWN = 0x105;
    static constexpr int KEY_HOME = 0x106;
    static constexpr int KEY_END = 0x107;
    static constexpr int KEY_INSERT = 0x108;
    static constexpr int KEY_DELETE = 0x109;
    static constexpr int KEY_F1 = 0x110;
    // Control and Shift with a letter, from A to Z: a terminal sends them as xterm's modifyOtherKeys sequence, since
    // their control byte would be the same one of Control alone.
    static constexpr int KEY_CTRL_SHIFT_A = 0x120;
    static constexpr int LETTERS = 26;

    static void init();
    // Feeds one byte of a PS/2 set 1 scancode (0xE0 prefixes included), as the PS/2 keyboard sends them; for keyboards
    // on other buses. Interrupt context, as the PS/2 interrupt handler.
    static void injectScancode(uint8_t scancode);
    static bool ps2Present() { return ps2Found; }
    // The next key of the calling process's console, or -1.
    static int read();
    // The next key of a console, or -1: for its terminal.
    static int read(unsigned console);
    static bool pending();
    static constexpr uint8_t MOD_SHIFT = 1;
    static constexpr uint8_t MOD_CTRL = 4;
    static constexpr uint8_t MOD_ALT = 8;
    static uint8_t modifiers();

private:
    static constexpr unsigned SIZE = 32;
    static volatile uint16_t queue[12][SIZE];
    static volatile unsigned head[12];
    static volatile unsigned tail[12];
    static bool shift;
    static bool alt;
    static bool ctrl;
    static bool extended;
    static bool ps2Found;

    static void push(unsigned console, uint16_t key);
    static bool probePs2();
    static TrapFrame *onIrq(TrapFrame *frame);
};

#endif
