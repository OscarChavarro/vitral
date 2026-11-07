#ifndef __SERIAL_TTY__
#define __SERIAL_TTY__
#include <stddef.h>
#include <stdint.h>
#include "drivers/tty/Tty.h"

// /dev/ttyS0: the terminal on COM1, for a terminal (or a terminal emulator) at the other end of a null modem cable.
// Its type is "vt220", which every terminal emulator understands, and its size 80x24 until someone sets another with
// TIOCSWINSZ. The kernel log keeps going to COM1 as well, as with console=ttyS0 on Linux.
class SerialTty : public Tty {
public:
    static void init();

    const char *type() const override { return "vt220"; }

protected:
    size_t fetch(uint8_t *buffer, size_t size) override;
    void transmit(const char *bytes, size_t count) override;

private:
    static constexpr uint16_t ROWS = 24;
    static constexpr uint16_t COLUMNS = 80;

    SerialTty() : Tty("", "ttyS0", ROWS, COLUMNS) {}
};

#endif
