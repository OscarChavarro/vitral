#include "programs/dmesg/DmesgCommand.h"
#include "drivers/keyboard/Keyboard.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "kernel/KernelLog.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/Printf.h"
#include "syscalls/Syscalls.h"
#include "lib/vitralKernel/String.h"
#include "kernel/ArgList.h"

unsigned
DmesgCommand::rowsOf(size_t length) {
    return length <= VirtualConsole::COLS ? 1 : (unsigned)((length + VirtualConsole::COLS - 1) / VirtualConsole::COLS);
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

    unsigned console = Syscalls::console();
    unsigned rows = 0;
    const unsigned page = VirtualConsole::ROWS - 1;
    bool paging = lastLines == 0 || lastLines >= page;
    bool quit = false;
    unsigned step = page;
    for (size_t i = start; i < length && !quit;) {
        size_t end = i;
        while (end < length && text[end] != '\n') {
            end++;
        }
        unsigned lineRows = rowsOf(end - i);
        if (paging && rows + lineRows > step) {
            VirtualConsole::writeAt(console, 0, VirtualConsole::ROWS - 1, "-- More (space: page, Enter: line, q: quit) --", PROMPT);
            int key;
            while ((key = Keyboard::read()) < 0) {
                Syscalls::sleep(20);
            }
            VirtualConsole::writeAt(console, 0, VirtualConsole::ROWS - 1, "                                              ", NORMAL);
            if (key == 'q' || key == 'Q' || key == Keyboard::KEY_ESC) {
                quit = true;
                break;
            }
            step = key == Keyboard::KEY_ENTER ? 1 : page;
            rows = 0;
        }
        for (size_t j = i; j < end; j++) {
            VirtualConsole::putc(console, text[j]);
        }
        if ((end - i) % VirtualConsole::COLS != 0 || end == i) {
            VirtualConsole::putc(console, '\n');
        }
        rows += lineRows;
        i = end + 1;
    }
    delete[] text;
    return 0;
}
