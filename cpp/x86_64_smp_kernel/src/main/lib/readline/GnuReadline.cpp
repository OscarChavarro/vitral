#include <readline/readline.h>
#include <readline/history.h>
#include <stdlib.h>
#include <string.h>
#include "drivers/tty/Tty.h"
#include "lib/readline/Readline.h"

// One editor, with its history, per terminal.
static constexpr unsigned GNU_READLINE_TERMINALS = 24;
static Tty *gnuReadlineTerminals[GNU_READLINE_TERMINALS];
static Readline *gnuReadlineEditors[GNU_READLINE_TERMINALS];

static Readline *
gnuReadlineCurrent() {
    Tty *terminal = Tty::current();
    for (unsigned i = 0; i < GNU_READLINE_TERMINALS; i++) {
        if (gnuReadlineTerminals[i] == terminal) {
            return gnuReadlineEditors[i];
        }
    }
    for (unsigned i = 0; i < GNU_READLINE_TERMINALS; i++) {
        if (!gnuReadlineTerminals[i]) {
            Readline *editor = new Readline(*terminal);
            editor->setEscapeEnds(true);
            gnuReadlineEditors[i] = editor;
            gnuReadlineTerminals[i] = terminal;
            return editor;
        }
    }
    return nullptr;
}

char *
readline(const char *prompt) {
    Readline *editor = gnuReadlineCurrent();
    char line[Readline::MAX_LINE + 1];
    if (!editor || editor->read(prompt, line, sizeof(line)) != Readline::Result::Line) {
        return nullptr;
    }
    char *copy = (char *)malloc(strlen(line) + 1);
    if (copy) {
        strcpy(copy, line);
    }
    return copy;
}

void
add_history(const char *line) {
    Readline *editor = gnuReadlineCurrent();
    if (editor) {
        editor->add(line);
    }
}
