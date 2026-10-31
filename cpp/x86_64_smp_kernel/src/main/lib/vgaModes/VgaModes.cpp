#include "lib/vgaModes/VgaModes.h"
#include <stdio.h>
#include <signal.h>
#include <stdlib.h>
#include <termios.h>
#include <unistd.h>
#include <vga.h>
#include <readline/readline.h>
#include <readline/history.h>

VgaModes::VgaModes(bool withText)
    : total(0), withText(withText), shown{-1, 0, 0, 0, 0, false}, original{}, haveOriginal(false), changed(false) {
    haveOriginal = tcgetattr(STDIN_FILENO, &original) == 0;
    catchFatalSignals();
    int lastMode = vga_lastmodenumber();
    for (int mode = withText ? TEXT : TEXT + 1; mode <= lastMode && total < MAX_MODES; mode++) {
        if (!vga_hasmode(mode) || !vga_getmodeinfo(mode)) {
            continue;
        }
        int bits = bitsPerPixel(mode);
        unsigned size = area(mode);
        int at = total++;
        while (at > 0) {
            int before = modes[at - 1];
            int beforeBits = bitsPerPixel(before);
            unsigned beforeSize = area(before);
            bool sameBits = beforeBits == bits;
            bool after = beforeBits < bits ||
                         (sameBits && (beforeSize < size || (beforeSize == size && width(before) <= width(mode))));
            if (after) {
                break;
            }
            modes[at] = before;
            at--;
        }
        modes[at] = mode;
    }
}

VgaModes::~VgaModes() {
    if (changed) {
        leave();
    }
}

// The signals svgalib 1.9.25 catches. Its handler (installed at its first vga_setmode(), so after this one) puts back
// the text mode and the terminal mode it found, then calls the handler that was there before: this one, which shows
// the cursor again and lets the signal end the program as it would have.
void
VgaModes::catchFatalSignals() {
    static const int FATAL[] = {SIGHUP, SIGINT, SIGQUIT, SIGILL, SIGTRAP, SIGABRT, SIGBUS, SIGFPE, SIGSEGV, SIGPIPE,
                                SIGALRM, SIGTERM};
    for (int number : FATAL) {
        signal(number, onFatalSignal);
    }
}

void
VgaModes::onFatalSignal(int number) {
    static const char RESTORE[] = "\033[0m\033[?25h\n";
    ssize_t written = write(STDOUT_FILENO, RESTORE, sizeof(RESTORE) - 1);
    (void)written;
    signal(number, SIG_DFL);
    raise(number);
}

int
VgaModes::indexOf(int mode) const {
    for (int i = 0; i < total; i++) {
        if (modes[i] == mode) {
            return i;
        }
    }
    return -1;
}

void
VgaModes::keepDepths(const int *depths, int count) {
    int kept = 0;
    for (int i = 0; i < total; i++) {
        int bits = bitsPerPixel(modes[i]);
        bool wanted = false;
        for (int k = 0; k < count && !wanted; k++) {
            wanted = depths[k] == bits;
        }
        if (wanted) {
            modes[kept++] = modes[i];
        }
    }
    total = kept;
}

void
VgaModes::header(char *out, int size) {
    snprintf(out, size, "%3s  %-18s %3s %8s  %-7s %s", "#", "MODE", "BPP", "COLORS", "ORIGIN", "NOTES");
}

void
VgaModes::row(int mode, char *out, int size) {
    vga_modeinfo *info = vga_getmodeinfo(mode);
    char note[32];
    describe(mode, note, sizeof(note));
    snprintf(out, size, "%3d  %-18s %3d %8d  %-7s %s", mode, name(mode), bitsPerPixel(mode), info ? info->colors : 0,
             origin(mode), note);
}

void
VgaModes::print(const char *title) const {
    char line[ROW_WIDTH + 1];
    printf("%s\n\n", title);
    header(line, sizeof(line));
    printf("%s\n", line);
    for (int i = 0; i < total; i++) {
        row(modes[i], line, sizeof(line));
        printf("%s\n", line);
    }
}

// svgalib names every graphics mode and gives "" for TEXT and for numbers out of range.
const char *
VgaModes::name(int mode) {
    if (mode == TEXT) {
        return "TEXT";
    }
    const char *text = vga_getmodename(mode);
    return *text ? text : "?";
}

int
VgaModes::bitsPerPixel(int mode) {
    vga_modeinfo *info = vga_getmodeinfo(mode);
    if (!info || mode == TEXT) {
        return 0;
    }
    switch (info->colors) {
    case 2: return 1;
    case 16: return 4;
    case 256: return 8;
    case 32768: return 15;
    case 65536: return 16;
    default: return info->bytesperpixel * 8;
    }
}

const char *
VgaModes::origin(int mode) {
    switch (mode) {
    case TEXT: return "03h";
    case G320x200x16: return "0Dh";
    case G640x200x16: return "0Eh";
    case G640x350x16: return "10h";
    case G640x480x2: return "11h";
    case G640x480x16: return "12h";
    case G320x200x256: return "13h";
    case G320x240x256: return "Mode X";
    case G320x400x256: return "Mode Y";
    }
    vga_modeinfo *info = vga_getmodeinfo(mode);
    if (info && (info->flags & IS_MODEX)) {
        return "Mode X";
    }
    if (info && (info->flags & IS_LINEAR)) {
        return "linear";
    }
    return "-";
}

void
VgaModes::describe(int mode, char *out, int size) {
    vga_modeinfo *info = vga_getmodeinfo(mode);
    int bits = bitsPerPixel(mode);
    if (!info) {
        snprintf(out, size, "?");
    } else if (mode == TEXT) {
        snprintf(out, size, "text, %dx%d characters", info->width, info->height);
    } else if (info->flags & IS_MODEX) {
        snprintf(out, size, "unchained, 4 planes");
    } else if (mode == G320x200x256) {
        snprintf(out, size, "chain-4, 1 byte per pixel");
    } else if (info->bytesperpixel == 0) {
        snprintf(out, size, "%s, %d plane%s", bits == 1 ? "monochrome" : "EGA",
                 bits, bits == 1 ? "" : "s");
    } else if (!(info->flags & IS_LINEAR)) {
        snprintf(out, size, "%d bits, 64 KB window", bits);
    } else {
        snprintf(out, size, "%d bit%s%s", bits, bits == 1 ? "" : "s",
                 bits == 16 ? ", RGB565" : bits == 15 ? ", RGB555"
                                       : bits >= 24   ? ", RGB"
                                       : bits <= 8    ? ", palette"
                                                      : "");
    }
}

static char
lower(char c) { return c >= 'A' && c <= 'Z' ? (char)(c - 'A' + 'a') : c; }

static bool
sameName(const char *a, const char *b) {
    while (*a && lower(*a) == lower(*b)) {
        a++, b++;
    }
    return *a == 0 && *b == 0;
}

static bool
startsWithWord(const char *text, const char *word, const char **rest) {
    while (*word && lower(*text) == lower(*word)) {
        text++, word++;
    }
    if (*word || (*text && *text != ' ')) {
        return false;
    }
    while (*text == ' ') {
        text++;
    }
    *rest = text;
    return true;
}

int
VgaModes::lookup(const char *text) {
    const char *rest;
    if (startsWithWord(text, "text", &rest)) {
        return *rest == 0 || startsWithWord(rest, "80x25", &rest) ? TEXT : -1;
    }
    if (startsWithWord(text, "graphics", &rest)) {
        text = rest;
    }
    int lastMode = vga_lastmodenumber();
    const char *c = text;
    int number = 0;
    while (*c >= '0' && *c <= '9' && number <= lastMode) {
        number = number * 10 + (*c++ - '0');
    }
    if (c != text && *c == 0) {
        return number <= lastMode ? number : -1;
    }
    // Matched here instead of with vga_getmodenumber(), which prints its own error for an unknown name and stops at the
    // first mode with that name: svgalib gives some modes the same name (G400x300x256 is 55 and 145), so the one this
    // machine has wins.
    bool sizeFirst = c != text;
    int found = -1;
    for (int mode = TEXT + 1; mode <= lastMode; mode++) {
        const char *modeName = vga_getmodename(mode);
        if (sameName(text, sizeFirst ? modeName + 1 : modeName)) {
            if (vga_hasmode(mode)) {
                return mode;
            }
            if (found < 0) {
                found = mode;
            }
        }
    }
    return found;
}

int
VgaModes::prompt(char *error, int size) const {
    error[0] = 0;
    char *line = readline("mode> ");
    if (!line || !*line) {
        free(line);
        return -1;
    }
    add_history(line);
    int mode = lookup(line);
    if (mode < 0 || (mode == TEXT && !withText)) {
        snprintf(error, size, "unknown mode: %s", line);
    } else if (indexOf(mode) < 0) {
        snprintf(error, size, "mode not available: %s", name(mode));
    }
    free(line);
    return error[0] ? -1 : mode;
}

int
VgaModes::ask() const {
    for (;;) {
        char error[MAX_TEXT + 24];
        int mode = prompt(error, sizeof(error));
        if (!error[0]) {
            return mode;
        }
        printf("%s\n", error);
    }
}

VgaModes::Selection
VgaModes::select(const char *program, int count, char *const words[], int fallback,
                 int *mode) const {
    *mode = fallback;
    if (count <= 0) {
        return Selection::Chosen;
    }
    char text[MAX_TEXT + 1];
    int length = 0;
    for (int i = 0; i < count; i++) {
        const char *word = words[i];
        if (i > 0 && length < MAX_TEXT) {
            text[length++] = ' ';
        }
        while (*word && length < MAX_TEXT) {
            text[length++] = *word++;
        }
    }
    text[length] = 0;
    const char *rest;
    if (startsWithWord(text, "list", &rest) && !*rest) {
        char title[96];
        snprintf(title, sizeof(title), "%s: %s modes (default: %d, %s)", program, withText ? "video" : "graphics",
                 fallback, name(fallback));
        print(title);
        printf("\nType a number or a name and press Enter (Up/Down: history, Ctrl-R: search).\n");
        printf("An empty line quits.\n\n");
        *mode = ask();
        return *mode < 0 ? Selection::Quit : Selection::Chosen;
    }
    *mode = lookup(text);
    if (*mode < 0 || (*mode == TEXT && !withText)) {
        printf("%s: unknown mode: %s\nRun %s list to see the modes.\n", program, text, program);
        return Selection::Unknown;
    }
    return Selection::Chosen;
}

bool
VgaModes::enter(int mode) {
    vga_modeinfo *info = vga_hasmode(mode) ? vga_getmodeinfo(mode) : 0;
    if (!info) {
        printf("mode not available: %s\n", name(mode));
        return false;
    }
    Screen next = {mode, info->width, info->height, info->colors, bitsPerPixel(mode), mode == TEXT};
    if (vga_setmode(mode) < 0) {
        printf("cannot set mode %s\n", name(mode));
        return false;
    }
    shown = next;
    changed = true;
    if (shown.text) {
        rawKeys();
        fputs("\033[0m\033[?25l\033[2J\033[H", stdout);
        fflush(stdout);
    }
    return true;
}

void
VgaModes::rawKeys() {
    if (!haveOriginal) {
        return;
    }
    termios raw = original;
    raw.c_lflag &= ~(ICANON | ECHO);
    raw.c_cc[VMIN] = 1;
    raw.c_cc[VTIME] = 0;
    tcsetattr(STDIN_FILENO, TCSANOW, &raw);
    changed = true;
}

VgaModes::Change
VgaModes::change(int next) {
    if (next < 0 || next == shown.mode) {
        return Change::None;
    }
    return enter(next) ? Change::Done : Change::Failed;
}

VgaModes::Change
VgaModes::step(int step) {
    if (total == 0) {
        return Change::None;
    }
    int at = indexOf(shown.mode);
    if (at < 0) {
        at = total - 1;
    }
    return change(modes[((at + step) % total + total) % total]);
}

VgaModes::Change
VgaModes::nextDepth() {
    int at = indexOf(shown.mode);
    if (at < 0) {
        return Change::None;
    }
    for (int k = 1; k < total; k++) {
        int candidate = modes[(at + k) % total];
        if (sameSize(candidate, shown.mode)) {
            return change(candidate);
        }
    }
    return Change::None;
}

void
VgaModes::leave() {
    vga_setmode(TEXT);
    if (haveOriginal) {
        tcsetattr(STDIN_FILENO, TCSANOW, &original);
    }
    fputs("\033[0m\033[?25h\033[2J\033[H", stdout);
    fflush(stdout);
    changed = false;
}

// vga_getmodeinfo() returns the same buffer on every call, so a mode's fields are read before asking for another.
bool
VgaModes::sameSize(int a, int b) {
    if (a == TEXT || b == TEXT || !vga_getmodeinfo(a) || !vga_getmodeinfo(b)) {
        return false;
    }
    return width(a) == width(b) && area(a) == area(b);
}

int
VgaModes::width(int mode) {
    vga_modeinfo *info = vga_getmodeinfo(mode);
    return info ? info->width : 0;
}

unsigned
VgaModes::area(int mode) {
    vga_modeinfo *info = vga_getmodeinfo(mode);
    return info ? (unsigned)info->width * (unsigned)info->height : 0;
}
