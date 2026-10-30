#include "drivers/tty/Tty.h"
#include "drivers/keyboard/Keyboard.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "syscalls/Syscalls.h"

Tty::Terminal Tty::terminals[VirtualConsole::COUNT];

// What a Linux console sends for the keys Keyboard reports as codes above 0xFF.
static const char *
sequenceOf(int key) {
    switch (key) {
    case Keyboard::KEY_UP: return "\033[A";
    case Keyboard::KEY_DOWN: return "\033[B";
    case Keyboard::KEY_RIGHT: return "\033[C";
    case Keyboard::KEY_LEFT: return "\033[D";
    case Keyboard::KEY_PAGE_UP: return "\033[5~";
    case Keyboard::KEY_PAGE_DOWN: return "\033[6~";
    }
    static const char *const FUNCTION_KEYS[] = {
        "\033[[A", "\033[[B", "\033[[C", "\033[[D", "\033[[E", "\033[17~",
        "\033[18~", "\033[19~", "\033[20~", "\033[21~", "\033[23~", "\033[24~",
    };
    int function = key - Keyboard::KEY_F1;
    if (function >= 0 && function < (int)(sizeof(FUNCTION_KEYS) / sizeof(FUNCTION_KEYS[0]))) {
        return FUNCTION_KEYS[function];
    }
    return "";
}

void
Tty::defaults(Mode &mode) {
    static const uint8_t CONTROL_DEFAULTS[CONTROL_CHARS] = {
        3, 0x1C, 0x7F, 0x15, 4, 0, 1, 0, 0x11, 0x13, 0x1A, 0, 0x12, 0x0F, 0x17, 0x16, 0,
    };
    mode.inputFlags = CR_TO_NL | OUTPUT_FLOW_CONTROL;
    mode.outputFlags = POST_PROCESS | NL_TO_CRNL;
    mode.controlFlags = DEFAULT_CONTROL;
    mode.localFlags = SIGNALS | CANONICAL | ECHO_INPUT | ECHO_ERASE | ECHO_KILL | ECHO_CONTROL | ECHO_KILL_ERASE |
                      EXTENDED_INPUT;
    for (unsigned i = 0; i < CONTROL_CHARS; i++) {
        mode.controlChars[i] = CONTROL_DEFAULTS[i];
    }
}

Tty::Terminal &
Tty::mine(unsigned &console) {
    console = Syscalls::console() % VirtualConsole::COUNT;
    Terminal &t = terminals[console];
    if (!t.configured) {
        defaults(t.mode);
        t.length = 0;
        t.ready = 0;
        t.endOfFile = false;
        t.configured = true;
    }
    return t;
}

void
Tty::getMode(Mode &out) {
    unsigned console;
    out = mine(console).mode;
}

void
Tty::setMode(const Mode &mode, bool flushInput) {
    unsigned console;
    Terminal &t = mine(console);
    t.mode = mode;
    if (flushInput) {
        flush();
    } else if (!(mode.localFlags & CANONICAL)) {
        t.ready = t.length;
    }
}

void
Tty::flush() {
    unsigned console;
    Terminal &t = mine(console);
    while (Keyboard::read() >= 0) {
    }
    t.length = 0;
    t.ready = 0;
    t.endOfFile = false;
}

void
Tty::store(Terminal &t, uint8_t c) {
    bool canonical = t.mode.localFlags & CANONICAL;
    if (t.length < INPUT_SIZE - (canonical && c != '\n' ? 1 : 0)) {
        t.input[t.length++] = c;
    }
}

void
Tty::echo(const Terminal &t, unsigned console, uint8_t c) {
    if ((t.mode.localFlags & ECHO_CONTROL) && control(c)) {
        VirtualConsole::putc(console, '^');
        VirtualConsole::putc(console, (char)(c ^ 0x40));
        return;
    }
    VirtualConsole::putc(console, (char)c);
}

void
Tty::eraseLast(Terminal &t, unsigned console) {
    if (t.length <= t.ready) {
        return;
    }
    uint8_t c = t.input[--t.length];
    if ((t.mode.localFlags & (ECHO_INPUT | ECHO_ERASE)) != (ECHO_INPUT | ECHO_ERASE)) {
        return;
    }
    int columns = (t.mode.localFlags & ECHO_CONTROL) && control(c) ? 2 : 1;
    for (int i = 0; i < columns; i++) {
        VirtualConsole::putc(console, '\b');
        VirtualConsole::putc(console, ' ');
        VirtualConsole::putc(console, '\b');
    }
}

void
Tty::receive(Terminal &t, unsigned console, uint8_t c) {
    const Mode &m = t.mode;
    if (c == '\r') {
        if (m.inputFlags & IGNORE_CR) {
            return;
        }
        if (m.inputFlags & CR_TO_NL) {
            c = '\n';
        }
    } else if (c == '\n' && (m.inputFlags & NL_TO_CR)) {
        c = '\r';
    }
    if ((m.inputFlags & UPPER_TO_LOWER) && c >= 'A' && c <= 'Z') {
        c = (uint8_t)(c - 'A' + 'a');
    }
    bool echoOn = m.localFlags & ECHO_INPUT;
    if (!(m.localFlags & CANONICAL)) {
        store(t, c);
        t.ready = t.length;
        if (echoOn) {
            echo(t, console, c);
        }
        return;
    }
    if (c == m.controlChars[CC_ERASE] && c) {
        eraseLast(t, console);
        return;
    }
    if (c == m.controlChars[CC_KILL] && c) {
        while (t.length > t.ready) {
            eraseLast(t, console);
        }
        return;
    }
    if (c == m.controlChars[CC_EOF] && c) {
        if (t.length == t.ready) {
            t.endOfFile = true;
        }
        t.ready = t.length;
        return;
    }
    bool endsLine = c == '\n' || (c == m.controlChars[CC_EOL] && c) || (c == m.controlChars[CC_EOL2] && c);
    store(t, c);
    if (echoOn || (c == '\n' && (m.localFlags & ECHO_NL))) {
        echo(t, console, c);
    }
    if (endsLine) {
        t.ready = t.length;
    }
}

void
Tty::pull(Terminal &t, unsigned console) {
    int key;
    while ((key = Keyboard::read()) >= 0) {
        if (key == '\n') {
            receive(t, console, '\r');
        } else if (key == '\b') {
            receive(t, console, 0x7F);
        } else if (key < 0x100) {
            receive(t, console, (uint8_t)key);
        } else {
            for (const char *s = sequenceOf(key); *s; s++) {
                receive(t, console, (uint8_t)*s);
            }
        }
    }
}

size_t
Tty::take(Terminal &t, uint8_t *buffer, size_t count) {
    size_t n = count < t.ready ? count : t.ready;
    for (size_t i = 0; i < n; i++) {
        buffer[i] = t.input[i];
    }
    for (size_t i = n; i < t.length; i++) {
        t.input[i - n] = t.input[i];
    }
    t.length -= (unsigned)n;
    t.ready -= (unsigned)n;
    return n;
}

bool
Tty::readable() {
    unsigned console;
    Terminal &t = mine(console);
    pull(t, console);
    if (t.mode.localFlags & CANONICAL) {
        return t.ready > 0 || t.endOfFile;
    }
    unsigned least = t.mode.controlChars[CC_TIME] ? 1 : t.mode.controlChars[CC_MIN];
    return t.ready >= (least ? least : 1);
}

size_t
Tty::read(uint8_t *buffer, size_t count) {
    unsigned console;
    Terminal &t = mine(console);
    if (count == 0) {
        return 0;
    }
    uint64_t start = Syscalls::uptimeMs();
    unsigned seen = 0;
    for (;;) {
        pull(t, console);
        if (t.mode.localFlags & CANONICAL) {
            if (t.ready > 0) {
                return take(t, buffer, count);
            }
            if (t.endOfFile) {
                t.endOfFile = false;
                return 0;
            }
        } else {
            unsigned least = t.mode.controlChars[CC_MIN];
            uint64_t time = t.mode.controlChars[CC_TIME] * 100ull;
            if (least > count) {
                least = (unsigned)count;
            }
            if (t.ready != seen) {
                seen = t.ready;
                if (least > 0) {
                    start = Syscalls::uptimeMs();
                }
            }
            if (least > 0 ? t.ready >= least : t.ready > 0) {
                return take(t, buffer, count);
            }
            if (least == 0 && time == 0) {
                return 0;
            }
            bool timing = time > 0 && (least == 0 || seen > 0);
            if (timing && Syscalls::uptimeMs() - start >= time) {
                return take(t, buffer, count);
            }
        }
        Syscalls::sleep(POLL_MS);
    }
}
