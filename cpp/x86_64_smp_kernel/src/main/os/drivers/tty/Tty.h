#ifndef __TTY__
#define __TTY__
#include <stddef.h>
#include <stdint.h>

// Line discipline of the virtual consoles: turns the keyboard queue of the calling process's console into the byte
// stream a Linux console gives to its stdin (Enter as CR, Backspace as DEL, the arrow and function keys as escape
// sequences), in canonical mode (line editing and echo) or not. Flags and control character indices have Linux's
// termios values, so that vitralLibC's <termios.h> maps onto Mode one to one. There are no signals: ISIG is kept but
// Ctrl-C reaches the reader as a byte.
class Tty {
public:
    static constexpr unsigned CONTROL_CHARS = 32;

    // c_iflag
    static constexpr uint32_t IGNORE_BREAK = 0x1;
    static constexpr uint32_t BREAK_INTERRUPT = 0x2;
    static constexpr uint32_t IGNORE_PARITY = 0x4;
    static constexpr uint32_t MARK_PARITY = 0x8;
    static constexpr uint32_t PARITY_CHECK = 0x10;
    static constexpr uint32_t NL_TO_CR = 0x40;
    static constexpr uint32_t IGNORE_CR = 0x80;
    static constexpr uint32_t CR_TO_NL = 0x100;
    static constexpr uint32_t UPPER_TO_LOWER = 0x200;
    static constexpr uint32_t OUTPUT_FLOW_CONTROL = 0x400;
    static constexpr uint32_t INPUT_FLOW_CONTROL = 0x1000;

    // c_oflag
    static constexpr uint32_t POST_PROCESS = 0x1;
    static constexpr uint32_t NL_TO_CRNL = 0x4;
    static constexpr uint32_t NO_CR_AT_COLUMN_0 = 0x10;

    // c_cflag
    static constexpr uint32_t DEFAULT_CONTROL = 0xBF;

    // c_lflag
    static constexpr uint32_t SIGNALS = 0x1;
    static constexpr uint32_t CANONICAL = 0x2;
    static constexpr uint32_t ECHO_INPUT = 0x8;
    static constexpr uint32_t ECHO_ERASE = 0x10;
    static constexpr uint32_t ECHO_KILL = 0x20;
    static constexpr uint32_t ECHO_NL = 0x40;
    static constexpr uint32_t NO_FLUSH = 0x80;
    static constexpr uint32_t ECHO_CONTROL = 0x200;
    static constexpr uint32_t ECHO_KILL_ERASE = 0x800;
    static constexpr uint32_t EXTENDED_INPUT = 0x8000;

    // c_cc indices
    static constexpr unsigned CC_INTERRUPT = 0;
    static constexpr unsigned CC_QUIT = 1;
    static constexpr unsigned CC_ERASE = 2;
    static constexpr unsigned CC_KILL = 3;
    static constexpr unsigned CC_EOF = 4;
    static constexpr unsigned CC_TIME = 5;
    static constexpr unsigned CC_MIN = 6;
    static constexpr unsigned CC_SWITCH = 7;
    static constexpr unsigned CC_START = 8;
    static constexpr unsigned CC_STOP = 9;
    static constexpr unsigned CC_SUSPEND = 10;
    static constexpr unsigned CC_EOL = 11;
    static constexpr unsigned CC_REPRINT = 12;
    static constexpr unsigned CC_DISCARD = 13;
    static constexpr unsigned CC_WORD_ERASE = 14;
    static constexpr unsigned CC_LITERAL_NEXT = 15;
    static constexpr unsigned CC_EOL2 = 16;

    class Mode {
    public:
        uint32_t inputFlags;
        uint32_t outputFlags;
        uint32_t controlFlags;
        uint32_t localFlags;
        uint8_t controlChars[CONTROL_CHARS];
    };

    // The mode of the calling process's console (a Linux console's defaults until someone changes it).
    static void getMode(Mode &out);
    // Sets it; flushInput discards what was typed and not read yet (TCSAFLUSH).
    static void setMode(const Mode &mode, bool flushInput);
    // Discards what was typed and not read yet.
    static void flush();
    // True when read() would return at once: a whole line (or end of file) in canonical mode, otherwise enough bytes.
    static bool readable();
    // Reads like read(2) on a Linux console: in canonical mode up to the end of a line, waiting for it; otherwise as
    // VMIN / VTIME say. Returns the number of bytes, 0 at end of file (VEOF on an empty line) or on a timeout.
    static size_t read(uint8_t *buffer, size_t count);

private:
    static constexpr unsigned INPUT_SIZE = 256;
    static constexpr uint32_t POLL_MS = 10;

    class Terminal {
    public:
        Mode mode;
        bool configured;
        uint8_t input[INPUT_SIZE];
        unsigned length;
        unsigned ready;
        bool endOfFile;
    };

    static Terminal terminals[];

    static Terminal &mine(unsigned &console);
    static void defaults(Mode &mode);
    static void pull(Terminal &t, unsigned console);
    static void receive(Terminal &t, unsigned console, uint8_t c);
    static void store(Terminal &t, uint8_t c);
    static void echo(const Terminal &t, unsigned console, uint8_t c);
    static void eraseLast(Terminal &t, unsigned console);
    static bool control(uint8_t c) { return (c < 32 && c != '\n' && c != '\t') || c == 0x7F; }
    static size_t take(Terminal &t, uint8_t *buffer, size_t count);
};

#endif
