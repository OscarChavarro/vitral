#include "programs/dmesg/DmesgCommand.h"
#include "drivers/tty/Tty.h"
#include "fs/PollEvent.h"
#include "fs/PollRecord.h"
#include "kernel/KernelLog.h"
#include "lib/terminfo/Terminfo.h"
#include "lib/vitralKernel/Printf.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/String.h"
#include "kernel/ArgList.h"

static constexpr int DMESG_STDIN = 0;
static constexpr int DMESG_STDOUT = 1;

unsigned
DmesgCommand::rowsOf(size_t length, unsigned columns) {
    return length <= columns ? 1 : (unsigned)((length + columns - 1) / columns);
}

void
DmesgCommand::print(const char *text) { Syscalls::write(DMESG_STDOUT, text, String::length(text)); }

// One key, read in raw mode: Enter, Escape or a byte. What else an escape sequence sends is discarded.
int
DmesgCommand::waitKey() {
    Tty::Mode saved;
    Syscalls::ioctl(DMESG_STDIN, Tty::GET_MODE, &saved);
    Tty::Mode raw = saved;
    raw.localFlags &= ~(Tty::CANONICAL | Tty::ECHO_INPUT);
    raw.inputFlags &= ~Tty::CR_TO_NL;
    raw.controlChars[Tty::CC_MIN] = 1;
    raw.controlChars[Tty::CC_TIME] = 0;
    Syscalls::ioctl(DMESG_STDIN, Tty::SET_MODE, &raw);
    uint8_t c = 0;
    for (;;) {
        PollRecord record = {DMESG_STDIN, PollEvent::IN, 0};
        Syscalls::poll(&record, 1, -1);
        if (Syscalls::read(DMESG_STDIN, &c, 1) == 1) {
            break;
        }
    }
    if (c == 27) {
        Syscalls::sleep(ESCAPE_WAIT_MS);
    }
    Syscalls::ioctl(DMESG_STDIN, Tty::SET_MODE_FLUSH, &saved);
    return c == '\r' || c == '\n' ? KEY_ENTER : c;
}

int
DmesgCommand::main(const ArgList &args) {
    bool clearAfter = false;
    unsigned lastLines = 0;
    for (unsigned i = 0; i < args.count(); i++) {
        const char *word = args.at(i);
        if (String::equals(word, "-c")) {
            clearAfter = true;
        } else if (String::equals(word, "-n") && i + 1 < args.count()) {
            lastLines = 0;
            for (const char *c = args.at(++i); *c >= '0' && *c <= '9'; c++) {
                lastLines = lastLines * 10 + (unsigned)(*c - '0');
            }
            if (lastLines == 0) {
                lastLines = 1;
            }
        } else {
            printf64("usage: dmesg [-n LINES] [-c]\n");
            return 1;
        }
    }

    char *text = new char[KernelLog::SIZE + 1];
    size_t length = KernelLog::copy(text, KernelLog::SIZE + 1);
    if (clearAfter) {
        KernelLog::clear();
    }

    unsigned lines = 0;
    for (size_t i = 0; i < length; i++) {
        lines += text[i] == '\n';
    }
    unsigned skip = lastLines && lines > lastLines ? lines - lastLines : 0;
    size_t start = 0;
    for (; skip && start < length; start++) {
        if (text[start] == '\n') {
            skip--;
        }
    }

    Tty::WindowSize size;
    Syscalls::ioctl(DMESG_STDOUT, Tty::GET_WINDOW_SIZE, &size);
    unsigned columns = size.columns ? size.columns : 80;
    unsigned screenRows = size.rows > 2 ? size.rows : 24;
    const Terminfo &terminal = Terminfo::find(Syscalls::terminal()->type());
    char standout[32];
    char normal[32];
    char clearLine[32];
    terminal.format(Terminfo::Cap::StandoutOn, standout, sizeof(standout));
    terminal.format(Terminfo::Cap::Normal, normal, sizeof(normal));
    terminal.format(Terminfo::Cap::ClearToEnd, clearLine, sizeof(clearLine));
    unsigned rows = 0;
    const unsigned page = screenRows - 1;
    bool paging = lastLines == 0 || lastLines >= page;
    bool quit = false;
    unsigned step = page;
    for (size_t i = start; i < length && !quit;) {
        size_t end = i;
        while (end < length && text[end] != '\n') {
            end++;
        }
        unsigned lineRows = rowsOf(end - i, columns);
        if (paging && rows + lineRows > step) {
            print(standout);
            print("-- More (space: page, Enter: line, q: quit) --");
            print(normal);
            int key = waitKey();
            print("\r");
            print(clearLine);
            if (key == 'q' || key == 'Q' || key == KEY_ESC) {
                quit = true;
                break;
            }
            step = key == KEY_ENTER ? 1 : page;
            rows = 0;
        }
        Syscalls::write(DMESG_STDOUT, text + i, end - i);
        if ((end - i) % columns != 0 || end == i) {
            print("\n");
        }
        rows += lineRows;
        i = end + 1;
    }
    delete[] text;
    return 0;
}
