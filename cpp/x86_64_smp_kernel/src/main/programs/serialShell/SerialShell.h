#ifndef __SERIAL_SHELL__
#define __SERIAL_SHELL__
#include <stddef.h>
#include <stdint.h>
#include "drivers/video/vga/VgaMode.h"
#include "kernel/ArgList.h"
#include "syscalls/Syscalls.h"

// A command line on /dev/ttyS0 (COM1), for a terminal on the other end of a null modem cable: it works while the
// screen shows a graphics mode or nothing at all. Started by init when COM1 exists.
//
//   mode            lists the video modes this machine can show
//   mode NAME       switches the screen to that mode. A text mode applies to every virtual console; a graphics mode is
//                   shown with a test pattern on the last virtual console (Alt-F12), which no program draws on, and
//                   the next text mode returns the screen to the console it showed before
//   console [N]     shows or switches the virtual console on the screen
//   reboot          runs the reboot program
//   ps, dmesg, uptime, help
class SerialShell {
public:
    static int main(const ArgList &args);

private:
    static constexpr unsigned MAX_LINE = 127;
    static constexpr unsigned MAX_WORDS = 8;
    static constexpr unsigned MAX_PROCESSES = 256;
    static constexpr const char *PROMPT = "vitral$ ";
    static constexpr unsigned PATTERN_CONSOLE = 11;

    enum class Escape : uint8_t { None, Started, Sequence };

    int fd;
    char line[MAX_LINE + 1];
    unsigned length;
    Escape escape;
    unsigned returnConsole;

    explicit SerialShell(int device) : fd(device), length(0), escape(Escape::None), returnConsole(0) { line[0] = 0; }
    SerialShell(const SerialShell &) = delete;
    SerialShell &operator=(const SerialShell &) = delete;

    int run();
    void key(char c);
    void execute();
    void print(const char *text);

    void help();
    void mode(unsigned count, char *const words[]);
    void listModes();
    void setMode(const char *name);
    void console(unsigned count, char *const words[]);
    void ps();
    void dmesg();
    void uptime();
    void reboot();

    static void modeName(VgaMode::Id id, char *out, size_t size);
    static void drawTestPattern(unsigned console, VgaMode::Id id);
};

#endif
