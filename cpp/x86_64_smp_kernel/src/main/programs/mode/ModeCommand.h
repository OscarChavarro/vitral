#ifndef __MODE_COMMAND__
#define __MODE_COMMAND__
#include <stddef.h>
#include <stdint.h>
#include "drivers/video/vga/VgaMode.h"
#include "kernel/ArgList.h"

// mode [NAME]: lists the video modes this machine can show, or switches the screen to one. A text mode applies to
// every virtual console; a graphics mode is shown with a test pattern on the last virtual console (Alt-F12), which no
// program draws on, and the next text mode returns the screen to the console it showed before. Useful from the serial
// terminal, where it works whatever the screen shows.
class ModeCommand {
public:
    static int main(const ArgList &args);

private:
    static constexpr unsigned PATTERN_CONSOLE = 11;
    static constexpr int STDOUT = 1;

    static void print(const char *text);
    static void modeName(VgaMode::Id id, char *out, size_t size);
    static bool sameName(const char *a, const char *b);
    static void listModes();
    static int setMode(const char *name);
    static void drawTestPattern(unsigned console, VgaMode::Id id);
};

#endif
