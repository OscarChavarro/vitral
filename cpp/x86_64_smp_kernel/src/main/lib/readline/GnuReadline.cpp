#include <readline/readline.h>
#include <readline/history.h>
#include <stdlib.h>
#include <string.h>
#include "drivers/video/virtualConsole/VirtualConsole.h"
#include "lib/readline/Readline.h"
#include "syscalls/Syscalls.h"

static Readline *instances[VirtualConsole::COUNT];

static Readline &
current() {
    unsigned console = Syscalls::console() % VirtualConsole::COUNT;
    if (!instances[console]) {
        instances[console] = new Readline(console);
        instances[console]->setEscapeEnds(true);
    }
    return *instances[console];
}

char *
readline(const char *prompt) {
    char line[Readline::MAX_LINE + 1];
    if (current().read(prompt, line, sizeof(line)) != Readline::Result::Line) {
        return nullptr;
    }
    char *copy = (char *)malloc(strlen(line) + 1);
    if (copy) {
        strcpy(copy, line);
    }
    return copy;
}

void
add_history(const char *line) { current().add(line); }
