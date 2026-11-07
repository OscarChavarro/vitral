#include "programs/chvt/ChvtCommand.h"
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/String.h"
#include "syscalls/Syscalls.h"

void
ChvtCommand::print(const char *text) { Syscalls::write(STDOUT, text, String::length(text)); }

int
ChvtCommand::main(const ArgList &args) {
    char text[64];
    if (args.count() == 0) {
        Format::print(text, sizeof(text), "console %u is on the screen\n", VirtualConsole::active());
        print(text);
        return 0;
    }
    unsigned number = 0;
    const char *word = args.at(0);
    const char *p = word;
    for (; *p >= '0' && *p <= '9'; p++) {
        number = number * 10 + (unsigned)(*p - '0');
    }
    if (args.count() > 1 || *p || p == word || number >= VirtualConsole::COUNT) {
        Format::print(text, sizeof(text), "usage: chvt [0-%u]\n", VirtualConsole::COUNT - 1);
        print(text);
        return 1;
    }
    Syscalls::switchConsole(number);
    return 0;
}
