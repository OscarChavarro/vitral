#ifndef __VIRTUAL_CONSOLE_TTY__
#define __VIRTUAL_CONSOLE_TTY__
#include <stddef.h>
#include <stdint.h>
#include "drivers/tty/Tty.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"

// The terminal of a virtual console, /dev/tty1 to /dev/tty12 for consoles 0 to 11 (Alt-F1 to Alt-F12), of type
// "linux": its input is the console's keyboard queue, with Enter as CR, Backspace as DEL and the arrow and function
// keys as the Linux console sends them; its output goes to VirtualConsole::putc(). Unlike Linux, the console's
// graphics stay available to the programs that run on it, through svgalib (VirtualConsole::setMode() and the rest).
class VirtualConsoleTty : public Tty {
public:
    static void init();
    // The terminal of a console; null before init().
    static VirtualConsoleTty *of(unsigned console);

    const char *type() const override { return "linux"; }
    void windowSize(WindowSize &out) override;
    int console() const override { return (int)index; }

protected:
    size_t fetch(uint8_t *buffer, size_t size) override;
    void transmit(const char *bytes, size_t count) override;

private:
    static constexpr unsigned SEQUENCE_LENGTH = 12;

    static VirtualConsoleTty *terminals[VirtualConsole::COUNT];

    unsigned index;
    // What is left of the escape sequence of a key that did not fit in the last fetch().
    char pending[SEQUENCE_LENGTH];
    unsigned pendingLength;
    unsigned pendingNext;

    VirtualConsoleTty(unsigned console, const char *deviceName);
    static const char *sequenceOf(int key, char (&buffer)[SEQUENCE_LENGTH]);
};

#endif
