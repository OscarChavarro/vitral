#include "drivers/tty/Tty.h"
#include "drivers/tty/VirtualConsoleTty.h"
#include "kernel/Scheduler.h"
#include "lib/vitralKernel/LockGuard.h"
#include "syscalls/Syscalls.h"

Tty::Tty(const char *deviceDirectory, const char *deviceName, uint16_t rows, uint16_t columns)
    : CharDevice(deviceDirectory, deviceName), length(0), ready(0), endOfFile(false), outputColumn(0),
      outputEscape(OutputEscape::None) {
    defaults(mode);
    size.rows = rows;
    size.columns = columns;
    size.xPixels = 0;
    size.yPixels = 0;
}

Tty *
Tty::current() {
    Process *self = Scheduler::cpuCount() ? Scheduler::current() : nullptr;
    Tty *terminal = self ? self->terminal() : nullptr;
    return terminal ? terminal : VirtualConsoleTty::of(Scheduler::currentConsole());
}

void
Tty::defaults(Mode &m) {
    static const uint8_t CONTROL_DEFAULTS[CONTROL_CHARS] = {
        3, 0x1C, 0x7F, 0x15, 4, 0, 1, 0, 0x11, 0x13, 0x1A, 0, 0x12, 0x0F, 0x17, 0x16, 0,
    };
    m.inputFlags = CR_TO_NL | OUTPUT_FLOW_CONTROL;
    m.outputFlags = POST_PROCESS | NL_TO_CRNL;
    m.controlFlags = DEFAULT_CONTROL;
    m.localFlags = SIGNALS | CANONICAL | ECHO_INPUT | ECHO_ERASE | ECHO_KILL | ECHO_CONTROL | ECHO_KILL_ERASE |
                   EXTENDED_INPUT;
    for (unsigned i = 0; i < CONTROL_CHARS; i++) {
        m.controlChars[i] = CONTROL_DEFAULTS[i];
    }
}

void
Tty::getMode(Mode &out) {
    LockGuard guard(lock);
    out = mode;
}

void
Tty::setMode(const Mode &m, bool flushInput) {
    if (flushInput) {
        flush();
    }
    LockGuard guard(lock);
    mode = m;
    if (!(m.localFlags & CANONICAL)) {
        ready = length;
    }
}

void
Tty::flush() {
    uint8_t discarded[64];
    while (fetch(discarded, sizeof(discarded)) > 0) {
    }
    LockGuard guard(lock);
    length = 0;
    ready = 0;
    endOfFile = false;
}

void
Tty::windowSize(WindowSize &out) {
    LockGuard guard(lock);
    out = size;
}

// Keeps outputColumn: printable characters advance it, CR and NL return it to 0, and escape sequences do not count.
void
Tty::track(char c) {
    uint8_t byte = (uint8_t)c;
    if (outputEscape == OutputEscape::Started) {
        outputEscape = (c == '[' || c == '(' || c == ')') ? OutputEscape::Sequence : OutputEscape::None;
        return;
    }
    if (outputEscape == OutputEscape::Sequence) {
        if ((byte >= 0x40 && byte <= 0x7E) && c != '[') {
            outputEscape = OutputEscape::None;
        }
        return;
    }
    if (c == 27) {
        outputEscape = OutputEscape::Started;
    } else if (c == '\r' || c == '\n') {
        outputColumn = 0;
    } else if (c == '\b') {
        outputColumn = outputColumn ? outputColumn - 1 : 0;
    } else if (c == '\t') {
        outputColumn = (outputColumn + 8) & ~7u;
    } else if (byte >= 32 && byte != 0x7F) {
        outputColumn++;
    }
    unsigned columns = size.columns ? size.columns : 80;
    if (outputColumn >= columns) {
        outputColumn = 0;
    }
}

// With the lock held: output processing (OPOST) and the device.
void
Tty::emit(const char *bytes, size_t count) {
    bool crnl = (mode.outputFlags & POST_PROCESS) && (mode.outputFlags & NL_TO_CRNL);
    char out[OUTPUT_CHUNK * 2];
    size_t n = 0;
    for (size_t i = 0; i < count; i++) {
        if (n + 2 > sizeof(out)) {
            transmit(out, n);
            n = 0;
        }
        if (bytes[i] == '\n' && crnl) {
            out[n++] = '\r';
            track('\r');
        }
        out[n++] = bytes[i];
        track(bytes[i]);
    }
    if (n) {
        transmit(out, n);
    }
}

int64_t
Tty::write(const void *buffer, size_t count) {
    const char *bytes = (const char *)buffer;
    for (size_t done = 0; done < count; done += OUTPUT_CHUNK) {
        LockGuard guard(lock);
        emit(bytes + done, count - done < OUTPUT_CHUNK ? count - done : OUTPUT_CHUNK);
    }
    return (int64_t)count;
}

void
Tty::print(const char *text) {
    size_t n = 0;
    while (text[n]) {
        n++;
    }
    write(text, n);
}

unsigned
Tty::column() {
    LockGuard guard(lock);
    return outputColumn;
}

void
Tty::store(uint8_t c) {
    bool canonical = mode.localFlags & CANONICAL;
    if (length < INPUT_SIZE - (canonical && c != '\n' ? 1 : 0)) {
        input[length++] = c;
    }
}

void
Tty::echo(uint8_t c) {
    if ((mode.localFlags & ECHO_CONTROL) && control(c)) {
        char shown[2] = {'^', (char)(c ^ 0x40)};
        emit(shown, 2);
        return;
    }
    char byte = (char)c;
    emit(&byte, 1);
}

void
Tty::eraseLast() {
    if (length <= ready) {
        return;
    }
    uint8_t c = input[--length];
    if ((mode.localFlags & (ECHO_INPUT | ECHO_ERASE)) != (ECHO_INPUT | ECHO_ERASE)) {
        return;
    }
    int columns = (mode.localFlags & ECHO_CONTROL) && control(c) ? 2 : 1;
    for (int i = 0; i < columns; i++) {
        emit("\b \b", 3);
    }
}

void
Tty::receive(uint8_t c) {
    const Mode &m = mode;
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
        store(c);
        ready = length;
        if (echoOn) {
            echo(c);
        }
        return;
    }
    // A serial terminal may send Backspace as BS rather than DEL: both erase.
    if ((c == m.controlChars[CC_ERASE] && c) || c == '\b') {
        eraseLast();
        return;
    }
    if (c == m.controlChars[CC_KILL] && c) {
        while (length > ready) {
            eraseLast();
        }
        return;
    }
    if (c == m.controlChars[CC_EOF] && c) {
        if (length == ready) {
            endOfFile = true;
        }
        ready = length;
        return;
    }
    bool endsLine = c == '\n' || (c == m.controlChars[CC_EOL] && c) || (c == m.controlChars[CC_EOL2] && c);
    store(c);
    if (echoOn || (c == '\n' && (m.localFlags & ECHO_NL))) {
        echo(c);
    }
    if (endsLine) {
        ready = length;
    }
}

// With the lock held.
void
Tty::pull() {
    uint8_t bytes[32];
    size_t got;
    while ((got = fetch(bytes, sizeof(bytes))) > 0) {
        for (size_t i = 0; i < got; i++) {
            receive(bytes[i]);
        }
    }
}

bool
Tty::readyLocked() const {
    if (mode.localFlags & CANONICAL) {
        return ready > 0 || endOfFile;
    }
    return ready > 0;
}

size_t
Tty::take(uint8_t *buffer, size_t count) {
    size_t n = count < ready ? count : ready;
    for (size_t i = 0; i < n; i++) {
        buffer[i] = input[i];
    }
    for (size_t i = n; i < length; i++) {
        input[i - n] = input[i];
    }
    length -= (unsigned)n;
    ready -= (unsigned)n;
    return n;
}

bool
Tty::readable() {
    LockGuard guard(lock);
    pull();
    return readyLocked();
}

int64_t
Tty::read(void *buffer, size_t count) {
    if (count == 0) {
        return 0;
    }
    LockGuard guard(lock);
    pull();
    if (ready > 0) {
        return (int64_t)take((uint8_t *)buffer, count);
    }
    if ((mode.localFlags & CANONICAL) && endOfFile) {
        endOfFile = false;
        return 0;
    }
    if (!(mode.localFlags & CANONICAL) && mode.controlChars[CC_MIN] == 0 && mode.controlChars[CC_TIME] == 0) {
        return 0;
    }
    return -Err::AGAIN;
}

bool
Tty::waitReadable(int timeoutMs) {
    uint64_t start = Syscalls::uptimeMs();
    while (!readable()) {
        if (timeoutMs >= 0 && Syscalls::uptimeMs() - start >= (uint64_t)timeoutMs) {
            return false;
        }
        Syscalls::sleep(POLL_MS);
    }
    return true;
}

size_t
Tty::readWaiting(uint8_t *buffer, size_t count) {
    if (count == 0) {
        return 0;
    }
    uint64_t start = Syscalls::uptimeMs();
    unsigned seen = 0;
    for (;;) {
        {
            LockGuard guard(lock);
            pull();
            if (mode.localFlags & CANONICAL) {
                if (ready > 0) {
                    return take(buffer, count);
                }
                if (endOfFile) {
                    endOfFile = false;
                    return 0;
                }
            } else {
                unsigned least = mode.controlChars[CC_MIN];
                uint64_t time = mode.controlChars[CC_TIME] * 100ull;
                if (least > count) {
                    least = (unsigned)count;
                }
                if (ready != seen) {
                    seen = ready;
                    if (least > 0) {
                        start = Syscalls::uptimeMs();
                    }
                }
                if (least > 0 ? ready >= least : ready > 0) {
                    return take(buffer, count);
                }
                if (least == 0 && time == 0) {
                    return 0;
                }
                bool timing = time > 0 && (least == 0 || seen > 0);
                if (timing && Syscalls::uptimeMs() - start >= time) {
                    return take(buffer, count);
                }
            }
        }
        Syscalls::sleep(POLL_MS);
    }
}

int64_t
Tty::ioctl(uint64_t request, void *argument) {
    switch (request) {
    case GET_MODE:
        if (!argument) {
            return -Err::FAULT;
        }
        getMode(*(Mode *)argument);
        return 0;
    case SET_MODE:
    case SET_MODE_DRAIN:
    case SET_MODE_FLUSH:
        if (!argument) {
            return -Err::FAULT;
        }
        setMode(*(const Mode *)argument, request == SET_MODE_FLUSH);
        return 0;
    case FLUSH:
        // TCIFLUSH (0) and TCIOFLUSH (2) discard the input; output is never queued.
        if ((uintptr_t)argument != 1) {
            flush();
        }
        return 0;
    case GET_WINDOW_SIZE:
        if (!argument) {
            return -Err::FAULT;
        }
        windowSize(*(WindowSize *)argument);
        return 0;
    case SET_WINDOW_SIZE: {
        if (!argument) {
            return -Err::FAULT;
        }
        LockGuard guard(lock);
        size = *(const WindowSize *)argument;
        return 0;
    }
    default:
        return -Err::NOTTY;
    }
}
