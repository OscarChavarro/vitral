#include "programs/serialShell/SerialShell.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "fs/OpenFlag.h"
#include "fs/PollEvent.h"
#include "fs/PollRecord.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/String.h"

static constexpr char SERIAL_SHELL_BACKSPACE = 0x08;
static constexpr char SERIAL_SHELL_DELETE = 0x7F;
static constexpr char SERIAL_SHELL_CTRL_C = 0x03;
static constexpr char SERIAL_SHELL_CTRL_U = 0x15;
static constexpr char SERIAL_SHELL_ESCAPE = 0x1B;

int
SerialShell::main(const ArgList &) {
    int64_t fd = Syscalls::open("/dev/ttyS0", OpenFlag::RDWR);
    if (fd < 0) {
        kprintf("serialShell: no /dev/ttyS0, not started\n");
        return 1;
    }
    SerialShell *shell = new SerialShell((int)fd);
    int code = shell->run();
    delete shell;
    Syscalls::close((int)fd);
    return code;
}

void
SerialShell::print(const char *text) {
    Syscalls::write(fd, text, String::length(text));
}

int
SerialShell::run() {
    print("\nVitral serial shell on COM1. Type help for the commands.\n");
    print(PROMPT);
    for (;;) {
        PollRecord record = {fd, PollEvent::IN, 0};
        if (Syscalls::poll(&record, 1, 1000) <= 0) {
            continue;
        }
        char input[64];
        int64_t got = Syscalls::read(fd, input, sizeof(input));
        for (int64_t i = 0; i < got; i++) {
            key(input[i]);
        }
    }
}

// Line editing for a VT100 style terminal: printable characters are echoed, backspace erases, Ctrl-C and Ctrl-U
// drop the line, and escape sequences (cursor keys) are ignored.
void
SerialShell::key(char c) {
    if (escape == Escape::Started) {
        escape = (c == '[' || c == 'O') ? Escape::Sequence : Escape::None;
        return;
    }
    if (escape == Escape::Sequence) {
        if (c >= 0x40 && c <= 0x7E) {
            escape = Escape::None;
        }
        return;
    }
    if (c == SERIAL_SHELL_ESCAPE) {
        escape = Escape::Started;
    } else if (c == '\r' || c == '\n') {
        print("\n");
        line[length] = 0;
        execute();
        length = 0;
        print(PROMPT);
    } else if (c == SERIAL_SHELL_BACKSPACE || c == SERIAL_SHELL_DELETE) {
        if (length > 0) {
            length--;
            print("\b \b");
        }
    } else if (c == SERIAL_SHELL_CTRL_C || c == SERIAL_SHELL_CTRL_U) {
        length = 0;
        print(c == SERIAL_SHELL_CTRL_C ? "^C\n" : "\n");
        print(PROMPT);
    } else if (c >= 0x20 && c < 0x7F && length < MAX_LINE) {
        line[length++] = c;
        char echo[2] = {c, 0};
        print(echo);
    }
}

void
SerialShell::execute() {
    char *words[MAX_WORDS];
    unsigned count = 0;
    for (char *p = line; *p && count < MAX_WORDS;) {
        while (*p == ' ') {
            *p++ = 0;
        }
        if (!*p) {
            break;
        }
        words[count++] = p;
        while (*p && *p != ' ') {
            p++;
        }
    }
    if (count == 0) {
        return;
    }
    const char *command = words[0];
    if (String::equals(command, "help")) {
        help();
    } else if (String::equals(command, "mode")) {
        mode(count, words);
    } else if (String::equals(command, "console")) {
        console(count, words);
    } else if (String::equals(command, "ps")) {
        ps();
    } else if (String::equals(command, "dmesg")) {
        dmesg();
    } else if (String::equals(command, "uptime")) {
        uptime();
    } else if (String::equals(command, "reboot")) {
        reboot();
    } else {
        char text[96];
        Format::print(text, sizeof(text), "%s: unknown command (help lists them)\n", command);
        print(text);
    }
}

void
SerialShell::help() {
    print("mode            list the video modes this machine can show\n"
          "mode NAME       switch the screen to a mode (graphics: test pattern on console 11)\n"
          "console [N]     show or switch the virtual console on the screen (0-11)\n"
          "ps              processes\n"
          "dmesg           kernel log\n"
          "uptime          time since boot\n"
          "reboot          restart the machine\n");
}

static const char *
serialShellColors(uint32_t colors) {
    switch (colors) {
    case 65536: return "64K";
    case 16777216: return "16M";
    default: return nullptr;
    }
}

void
SerialShell::modeName(VgaMode::Id id, char *out, size_t size) {
    const VgaMode::Info &info = VgaMode::info(id);
    if (info.layout == VgaMode::Layout::Text) {
        Format::print(out, size, "TEXT%ux%u", (uint32_t)info.width, (uint32_t)info.height);
    } else if (info.layout == VgaMode::Layout::FramebufferText) {
        Format::print(out, size, "FBTEXT");
    } else if (serialShellColors(info.colors)) {
        Format::print(out, size, "G%ux%ux%s", (uint32_t)info.width, (uint32_t)info.height,
                      serialShellColors(info.colors));
    } else {
        Format::print(out, size, "G%ux%ux%u", (uint32_t)info.width, (uint32_t)info.height, info.colors);
    }
}

void
SerialShell::mode(unsigned count, char *const words[]) {
    if (count == 1) {
        listModes();
    } else if (count == 2) {
        setMode(words[1]);
    } else {
        print("usage: mode [NAME]\n");
    }
}

void
SerialShell::listModes() {
    VgaMode::Id current = VirtualConsole::mode(VirtualConsole::active());
    print("  MODE              SIZE         COLORS    KIND\n");
    for (unsigned i = 0; i < VgaMode::COUNT; i++) {
        VgaMode::Id id = (VgaMode::Id)i;
        if (!VgaMode::available(id)) {
            continue;
        }
        const VgaMode::Info &info = VgaMode::info(id);
        char name[24];
        modeName(id, name, sizeof(name));
        const char *kind = info.layout == VgaMode::Layout::Text              ? "VGA text"
                           : info.layout == VgaMode::Layout::FramebufferText ? "framebuffer text"
                           : info.layout == VgaMode::Layout::Linear          ? "SVGA linear"
                           : info.layout == VgaMode::Layout::Planar          ? "VGA planar"
                                                                             : "VGA 256 colors";
        bool text = !info.graphics();
        unsigned width = text ? VgaMode::frameWidth(id) : info.width;
        unsigned height = text ? VgaMode::frameHeight(id) : info.height;
        char row[96];
        Format::print(row, sizeof(row), "%c %-17s %4ux%-7u %-9u %s\n", id == current ? '*' : ' ', name, width, height,
                      info.colors, kind);
        print(row);
    }
    print("* = current mode of the screen\n");
}

static bool
serialShellSameName(const char *a, const char *b) {
    for (; *a && *b; a++, b++) {
        char x = (*a >= 'a' && *a <= 'z') ? (char)(*a - 32) : *a;
        char y = (*b >= 'a' && *b <= 'z') ? (char)(*b - 32) : *b;
        if (x != y) {
            return false;
        }
    }
    return *a == *b;
}

void
SerialShell::setMode(const char *name) {
    char text[96];
    for (unsigned i = 0; i < VgaMode::COUNT; i++) {
        VgaMode::Id id = (VgaMode::Id)i;
        char candidate[24];
        modeName(id, candidate, sizeof(candidate));
        if (!serialShellSameName(candidate, name)) {
            continue;
        }
        if (!VgaMode::available(id)) {
            Format::print(text, sizeof(text), "mode: %s is not available on this machine\n", candidate);
            print(text);
            return;
        }
        unsigned shown = VirtualConsole::active();
        if (VgaMode::isConsoleText(id)) {
            if (!VirtualConsole::setTextMode(id)) {
                Format::print(text, sizeof(text), "mode: cannot switch to %s\n", candidate);
                print(text);
                return;
            }
            if (shown == PATTERN_CONSOLE && returnConsole != PATTERN_CONSOLE) {
                Syscalls::switchConsole(returnConsole);
                shown = returnConsole;
            }
            Format::print(text, sizeof(text), "mode: %s on every console, console %u on the screen\n", candidate,
                          shown);
            print(text);
            return;
        }
        // First on the screen: showing an empty console writes a notice on it in text mode.
        if (shown != PATTERN_CONSOLE) {
            returnConsole = shown;
            Syscalls::switchConsole(PATTERN_CONSOLE);
        }
        if (!VirtualConsole::setMode(PATTERN_CONSOLE, id)) {
            Format::print(text, sizeof(text), "mode: cannot switch to %s (not enough memory?)\n", candidate);
            print(text);
            return;
        }
        drawTestPattern(PATTERN_CONSOLE, id);
        Format::print(text, sizeof(text), "mode: %s with a test pattern on console %u (a text mode returns to %u)\n",
                      candidate, PATTERN_CONSOLE, returnConsole);
        print(text);
        return;
    }
    Format::print(text, sizeof(text), "mode: unknown mode %s (mode lists them)\n", name);
    print(text);
}

// Eight vertical bars (black, blue, green, cyan, red, magenta, yellow, white) inside a white frame: shows that the
// mode is displayed, its size (the frame touches the edges) and its colours.
void
SerialShell::drawTestPattern(unsigned console, VgaMode::Id id) {
    static const uint32_t RGB[8] = {0x000000, 0x0000FF, 0x00FF00, 0x00FFFF, 0xFF0000, 0xFF00FF, 0xFFFF00, 0xFFFFFF};
    static const uint8_t INDEX[8] = {0, 1, 2, 3, 4, 5, 14, 15};
    const VgaMode::Info &info = VgaMode::info(id);
    unsigned bytes = info.pixelBytes();
    unsigned width = info.width;
    unsigned height = info.height;
    uint32_t bar[8];
    for (unsigned b = 0; b < 8; b++) {
        uint32_t rgb = RGB[b];
        if (info.colors == 2) {
            bar[b] = b & 1;
        } else if (info.colors <= 256) {
            bar[b] = INDEX[b];
        } else if (bytes == 2) {
            bar[b] = (rgb >> 8 & 0xF800) | (rgb >> 5 & 0x07E0) | (rgb >> 3 & 0x001F);
        } else {
            bar[b] = rgb;
        }
    }
    uint32_t frame = bar[7];
    uint8_t *row = new uint8_t[width * bytes];
    for (unsigned y = 0; y < height; y++) {
        bool edge = y < 2 || y + 2 >= height;
        for (unsigned x = 0; x < width; x++) {
            uint32_t color = (edge || x < 2 || x + 2 >= width) ? frame : bar[x * 8 / width];
            for (unsigned i = 0; i < bytes; i++) {
                row[x * bytes + i] = (uint8_t)(color >> (8 * i));
            }
        }
        VirtualConsole::putSpan(console, 0, (int)y, row, (int)(width * bytes));
    }
    delete[] row;
}

void
SerialShell::console(unsigned count, char *const words[]) {
    char text[64];
    if (count == 1) {
        Format::print(text, sizeof(text), "console %u is on the screen\n", VirtualConsole::active());
        print(text);
        return;
    }
    unsigned number = 0;
    const char *p = words[1];
    for (; *p >= '0' && *p <= '9'; p++) {
        number = number * 10 + (unsigned)(*p - '0');
    }
    if (*p || p == words[1] || number >= VirtualConsole::COUNT) {
        print("usage: console [0-11]\n");
        return;
    }
    Syscalls::switchConsole(number);
}

void
SerialShell::ps() {
    Syscalls::ProcessInfo *list = new Syscalls::ProcessInfo[MAX_PROCESSES];
    unsigned count = Syscalls::listProcesses(list, MAX_PROCESSES);
    print("  PID  PPID NAME             CON\n");
    for (unsigned i = 0; i < count; i++) {
        char text[64];
        Format::print(text, sizeof(text), "%5u %5u %-16s %3u\n", list[i].pid, list[i].ppid, list[i].name,
                      (uint32_t)list[i].console);
        print(text);
    }
    delete[] list;
}

void
SerialShell::dmesg() {
    char *text = new char[KernelLog::SIZE + 1];
    size_t length = KernelLog::copy(text, KernelLog::SIZE + 1);
    Syscalls::write(fd, text, length);
    delete[] text;
}

void
SerialShell::uptime() {
    uint64_t seconds = Syscalls::uptime();
    char text[64];
    Format::print(text, sizeof(text), "up %u:%02u:%02u\n", (uint32_t)(seconds / 3600), (uint32_t)(seconds / 60 % 60),
                  (uint32_t)(seconds % 60));
    print(text);
}

// The reboot program: its own messages go to the console this shell runs on, so the terminal is told here.
void
SerialShell::reboot() {
    print("Rebooting...\n");
    ArgList args;
    int64_t child = Syscalls::run("reboot", args);
    if (child < 0) {
        print("reboot: cannot start the reboot program\n");
        return;
    }
    int status = 0;
    Syscalls::waitpid(child, &status);
}
