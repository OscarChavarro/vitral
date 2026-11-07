#include "drivers/tty/VirtualConsoleTty.h"
#include "drivers/char/CharDevices.h"
#include "drivers/keyboard/Keyboard.h"
#include "kernel/KernelLog.h"
#include "lib/terminfo/Terminfo.h"
#include "lib/vitralKernel/Format.h"

VirtualConsoleTty *VirtualConsoleTty::terminals[VirtualConsole::COUNT];

VirtualConsoleTty::VirtualConsoleTty(unsigned console, const char *deviceName)
    : Tty("", deviceName, VirtualConsole::ROWS, VirtualConsole::COLS), index(console), pendingLength(0),
      pendingNext(0) {}

void
VirtualConsoleTty::init() {
    for (unsigned i = 0; i < VirtualConsole::COUNT; i++) {
        char deviceName[CharDevice::NAME_LENGTH];
        Format::print(deviceName, sizeof(deviceName), "tty%u", i + 1);
        terminals[i] = new VirtualConsoleTty(i, deviceName);
        if (!CharDevices::add(terminals[i])) {
            kprintf("tty: cannot register /dev/%s\n", deviceName);
        }
    }
}

VirtualConsoleTty *
VirtualConsoleTty::of(unsigned console) { return terminals[console % VirtualConsole::COUNT]; }

void
VirtualConsoleTty::windowSize(WindowSize &out) {
    out.rows = (uint16_t)VirtualConsole::rows(index);
    out.columns = (uint16_t)VirtualConsole::columns(index);
    out.xPixels = 0;
    out.yPixels = 0;
}

// What the Linux console sends for the keys Keyboard reports as codes above 0xFF. Control and Shift with a letter,
// which the Linux console sends as the control byte of Control alone, use xterm's modifyOtherKeys form, CSI 27;6;code~,
// so programs can tell Ctrl-R from Ctrl-r. buffer holds that sequence.
const char *
VirtualConsoleTty::sequenceOf(int key, char (&buffer)[SEQUENCE_LENGTH]) {
    int letter = key - Keyboard::KEY_CTRL_SHIFT_A;
    if (letter >= 0 && letter < Keyboard::LETTERS) {
        Format::print(buffer, sizeof(buffer), "\033[27;6;%u~", (unsigned)('A' + letter));
        return buffer;
    }
    const Terminfo &entry = Terminfo::find("linux");
    switch (key) {
    case Keyboard::KEY_UP: return entry.keySequence(Terminfo::Key::Up);
    case Keyboard::KEY_DOWN: return entry.keySequence(Terminfo::Key::Down);
    case Keyboard::KEY_RIGHT: return entry.keySequence(Terminfo::Key::Right);
    case Keyboard::KEY_LEFT: return entry.keySequence(Terminfo::Key::Left);
    case Keyboard::KEY_PAGE_UP: return entry.keySequence(Terminfo::Key::PageUp);
    case Keyboard::KEY_PAGE_DOWN: return entry.keySequence(Terminfo::Key::PageDown);
    case Keyboard::KEY_HOME: return entry.keySequence(Terminfo::Key::Home);
    case Keyboard::KEY_END: return entry.keySequence(Terminfo::Key::End);
    case Keyboard::KEY_INSERT: return entry.keySequence(Terminfo::Key::Insert);
    case Keyboard::KEY_DELETE: return entry.keySequence(Terminfo::Key::Delete);
    }
    int function = key - Keyboard::KEY_F1;
    if (function >= 0 && function < 12) {
        return entry.keySequence((Terminfo::Key)((int)Terminfo::Key::F1 + function));
    }
    return "";
}

size_t
VirtualConsoleTty::fetch(uint8_t *buffer, size_t size) {
    size_t n = 0;
    while (n < size) {
        if (pendingNext < pendingLength) {
            buffer[n++] = (uint8_t)pending[pendingNext++];
            continue;
        }
        int key = Keyboard::read(index);
        if (key < 0) {
            break;
        }
        if (key == '\n') {
            buffer[n++] = '\r';
        } else if (key == '\b') {
            buffer[n++] = 0x7F;
        } else if (key < 0x100) {
            buffer[n++] = (uint8_t)key;
        } else {
            char sequence[SEQUENCE_LENGTH];
            const char *s = sequenceOf(key, sequence);
            pendingLength = pendingNext = 0;
            for (; *s && pendingLength < SEQUENCE_LENGTH; s++) {
                pending[pendingLength++] = *s;
            }
        }
    }
    return n;
}

void
VirtualConsoleTty::transmit(const char *bytes, size_t count) {
    for (size_t i = 0; i < count; i++) {
        VirtualConsole::putc(index, bytes[i]);
    }
}
