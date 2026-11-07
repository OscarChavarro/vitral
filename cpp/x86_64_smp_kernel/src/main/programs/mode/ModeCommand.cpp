#include "programs/mode/ModeCommand.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/Consoles.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/String.h"
#include "syscalls/Syscalls.h"

int
ModeCommand::main(const ArgList &args) {
    if (args.count() == 0) {
        listModes();
        return 0;
    }
    if (args.count() == 1) {
        return setMode(args.at(0));
    }
    print("usage: mode [NAME]\n");
    return 1;
}

void
ModeCommand::print(const char *text) { Syscalls::write(STDOUT, text, String::length(text)); }

static const char *
modeCommandColors(uint32_t colors) {
    switch (colors) {
    case 65536: return "64K";
    case 16777216: return "16M";
    default: return nullptr;
    }
}

void
ModeCommand::modeName(VgaMode::Id id, char *out, size_t size) {
    const VgaMode::Info &info = VgaMode::info(id);
    if (info.layout == VgaMode::Layout::Text) {
        Format::print(out, size, "TEXT%ux%u", (uint32_t)info.width, (uint32_t)info.height);
    } else if (info.layout == VgaMode::Layout::FramebufferText) {
        Format::print(out, size, "FBTEXT");
    } else if (modeCommandColors(info.colors)) {
        Format::print(out, size, "G%ux%ux%s", (uint32_t)info.width, (uint32_t)info.height,
                      modeCommandColors(info.colors));
    } else {
        Format::print(out, size, "G%ux%ux%u", (uint32_t)info.width, (uint32_t)info.height, info.colors);
    }
}

bool
ModeCommand::sameName(const char *a, const char *b) {
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
ModeCommand::listModes() {
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

int
ModeCommand::setMode(const char *name) {
    char text[112];
    for (unsigned i = 0; i < VgaMode::COUNT; i++) {
        VgaMode::Id id = (VgaMode::Id)i;
        char candidate[24];
        modeName(id, candidate, sizeof(candidate));
        if (!sameName(candidate, name)) {
            continue;
        }
        if (!VgaMode::available(id)) {
            Format::print(text, sizeof(text), "mode: %s is not available on this machine\n", candidate);
            print(text);
            return 1;
        }
        unsigned shown = VirtualConsole::active();
        if (VgaMode::isConsoleText(id)) {
            if (!VirtualConsole::setTextMode(id)) {
                Format::print(text, sizeof(text), "mode: cannot switch to %s\n", candidate);
                print(text);
                return 1;
            }
            unsigned back = Consoles::previous();
            if (shown == PATTERN_CONSOLE && back != PATTERN_CONSOLE) {
                Syscalls::switchConsole(back);
                shown = back;
            }
            Format::print(text, sizeof(text), "mode: %s on every console, console %u on the screen\n", candidate,
                          shown);
            print(text);
            return 0;
        }
        // First on the screen: showing an empty console writes a notice on it in text mode.
        unsigned back = shown == PATTERN_CONSOLE ? Consoles::previous() : shown;
        if (shown != PATTERN_CONSOLE) {
            Syscalls::switchConsole(PATTERN_CONSOLE);
        }
        if (!VirtualConsole::setMode(PATTERN_CONSOLE, id)) {
            Format::print(text, sizeof(text), "mode: cannot switch to %s (not enough memory?)\n", candidate);
            print(text);
            return 1;
        }
        drawTestPattern(PATTERN_CONSOLE, id);
        Format::print(text, sizeof(text), "mode: %s with a test pattern on console %u (a text mode returns to %u)\n",
                      candidate, PATTERN_CONSOLE, back);
        print(text);
        return 0;
    }
    Format::print(text, sizeof(text), "mode: unknown mode %s (mode lists them)\n", name);
    print(text);
    return 1;
}

// Eight vertical bars (black, blue, green, cyan, red, magenta, yellow, white) inside a white frame: shows that the
// mode is displayed, its size (the frame touches the edges) and its colours.
void
ModeCommand::drawTestPattern(unsigned console, VgaMode::Id id) {
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
