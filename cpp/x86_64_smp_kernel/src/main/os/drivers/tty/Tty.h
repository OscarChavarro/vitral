#ifndef __TTY__
#define __TTY__
#include <stddef.h>
#include <stdint.h>
#include "drivers/char/CharDevice.h"
#include "lib/vitralKernel/SpinLock.h"

// A terminal: the line discipline between a device that moves bytes (a virtual console with its keyboard, a serial
// port, later a network connection) and the processes that have it as their standard input, output and error. The
// device only supplies the bytes it received (fetch()) and sends the bytes it is given (transmit()); this class turns
// them into what a Linux terminal gives to read(2) and takes from write(2): canonical mode with line editing and echo
// or not, VMIN / VTIME, CR to NL on input and NL to CR NL on output. Flags and control character indices have Linux's
// termios values, so that vitralLibC's <termios.h> maps onto Mode one to one. There are no signals: ISIG is kept but
// Ctrl-C reaches the reader as a byte.
//
// As every character device, read() does not wait (-Err::AGAIN). readWaiting() waits as read(2) does, in the caller's
// context: it must not run inside a system call. The device's input is pulled when someone reads or polls, so
// nothing here runs in an interrupt handler.
class Tty : public CharDevice {
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

    // ioctl() requests, with Linux's numbers. The mode requests take a Mode, the window size ones a WindowSize.
    static constexpr uint64_t GET_MODE = 0x5401;        // TCGETS
    static constexpr uint64_t SET_MODE = 0x5402;        // TCSETS
    static constexpr uint64_t SET_MODE_DRAIN = 0x5403;  // TCSETSW
    static constexpr uint64_t SET_MODE_FLUSH = 0x5404;  // TCSETSF
    static constexpr uint64_t FLUSH = 0x540B;           // TCFLSH, with the queue selector as the argument value
    static constexpr uint64_t GET_WINDOW_SIZE = 0x5413; // TIOCGWINSZ
    static constexpr uint64_t SET_WINDOW_SIZE = 0x5414; // TIOCSWINSZ

    class Mode {
    public:
        uint32_t inputFlags;
        uint32_t outputFlags;
        uint32_t controlFlags;
        uint32_t localFlags;
        uint8_t controlChars[CONTROL_CHARS];
    };

    // struct winsize.
    class WindowSize {
    public:
        uint16_t rows;
        uint16_t columns;
        uint16_t xPixels;
        uint16_t yPixels;
    };

    // The terminal of the calling process: its standard input, output and error. Null only during the boot, before
    // VirtualConsoleTty::init().
    static Tty *current();

    int64_t read(void *buffer, size_t count) override;
    int64_t write(const void *buffer, size_t count) override;
    bool readable() override;
    int64_t ioctl(uint64_t request, void *argument) override;
    Tty *asTty() override { return this; }

    void getMode(Mode &out);
    // flushInput discards what was typed and not read yet (TCSAFLUSH).
    void setMode(const Mode &mode, bool flushInput);
    // Discards what was typed and not read yet.
    void flush();
    // Reads like read(2) on a Linux terminal: in canonical mode up to the end of a line, waiting for it; otherwise as
    // VMIN / VTIME say. Returns the number of bytes, 0 at end of file (VEOF on an empty line) or on a timeout.
    size_t readWaiting(uint8_t *buffer, size_t count);
    // Waits until read() has something to return; timeoutMs < 0 waits for ever. False on a timeout.
    bool waitReadable(int timeoutMs);
    // Writes text, a C string.
    void print(const char *text);
    // The column the output reached, from 0: to know whether a program left the cursor in the middle of a line.
    unsigned column();
    // Its terminfo name, the TERM of what runs on it.
    virtual const char *type() const = 0;
    virtual void windowSize(WindowSize &out);
    // The virtual console this terminal is, or -1.
    virtual int console() const { return -1; }

protected:
    Tty(const char *deviceDirectory, const char *deviceName, uint16_t rows, uint16_t columns);

    // The bytes the device received since the last call, without waiting.
    virtual size_t fetch(uint8_t *buffer, size_t size) = 0;
    // Sends bytes as they are. Called with the terminal's lock held and at most OUTPUT_CHUNK bytes at a time (twice
    // that with NL to CR NL), so a slow line keeps interrupts off only for a short while.
    virtual void transmit(const char *bytes, size_t count) = 0;

private:
    static constexpr unsigned INPUT_SIZE = 256;
    static constexpr uint32_t POLL_MS = 10;
    static constexpr size_t OUTPUT_CHUNK = 32;

    enum class OutputEscape : uint8_t { None, Started, Sequence };

    SpinLock lock;
    Mode mode;
    WindowSize size;
    uint8_t input[INPUT_SIZE];
    unsigned length;
    unsigned ready;
    bool endOfFile;
    unsigned outputColumn;
    OutputEscape outputEscape;

    static void defaults(Mode &mode);
    static bool control(uint8_t c) { return (c < 32 && c != '\n' && c != '\t') || c == 0x7F; }
    void pull();
    void receive(uint8_t c);
    void store(uint8_t c);
    void echo(uint8_t c);
    void eraseLast();
    void emit(const char *bytes, size_t count);
    void track(char c);
    bool readyLocked() const;
    size_t take(uint8_t *buffer, size_t count);
};

#endif
