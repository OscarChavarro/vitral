#include "programs/shell/Shell.h"
#include "drivers/tty/Tty.h"
#include "kernel/Process.h"
#include "lib/terminfo/Terminfo.h"
#include "lib/vitralKernel/Format.h"
#include "lib/vitralKernel/Memory.h"
#include "lib/vitralKernel/String.h"
#include "kernel/ArgList.h"
#include "kernel/ProgramRegistry.h"

const char *
Shell::stateName(uint8_t state) {
    switch ((Process::State)state) {
    case Process::State::Ready: return "READY";
    case Process::State::Running: return "RUN";
    case Process::State::Sleeping: return "SLEEP";
    case Process::State::Blocked: return "WAIT";
    default: return "ZOMBIE";
    }
}

Shell::Shell()
    : terminal(*Syscalls::terminal()), pid(Syscalls::getpid()), list(new Syscalls::ProcessInfo[MAX_ROWS]),
      input(new Readline(terminal)) {
    line[0] = 0;
    input->setCompletions(&commands);
}

Shell::~Shell() { delete input; delete[] list; }

unsigned
Shell::Commands::count() const { return ProgramRegistry::count(); }

const char *
Shell::Commands::at(unsigned i) const { return ProgramRegistry::at(i)->name; }

int
Shell::main(const ArgList &) {
    Shell *shell = new Shell();
    int code = shell->run();
    delete shell;
    return code;
}

void
Shell::print(const char *s) { Syscalls::write(STDOUT, s, String::length(s)); }

void
Shell::printWith(const char *format, const char *arg) {
    char text[LINE_SIZE];
    Format::print(text, sizeof(text), format, arg);
    print(text);
}

void
Shell::ps() {
    unsigned count = Syscalls::listProcesses(list, MAX_ROWS);
    print("  PID  PPID NAME             STATE   CON TTY\n");
    for (unsigned i = 0; i < count; i++) {
        const Syscalls::ProcessInfo &p = list[i];
        char text[LINE_SIZE];
        Format::print(text, sizeof(text), "%5u %5u %-16s %-6s %3u %s\n", p.pid, p.ppid, p.name,
                      stateName(p.state), (unsigned)p.console, p.terminal);
        print(text);
    }
}

void
Shell::ls() {
    for (unsigned i = 0; i < ProgramRegistry::count(); i++) {
        print(ProgramRegistry::at(i)->name);
        print("\n");
    }
}

// Afterwards the terminal is as the shell left it: its mode (a program that ends in raw mode would leave the shell
// without echo), the cursor shown, the attributes off and the prompt at the start of a line.
void
Shell::runProgram(const char *name, const ArgList &args) {
    Tty::Mode mode;
    terminal.getMode(mode);
    int64_t child = Syscalls::run(name, args);
    if (child == -Syscalls::ERR_NOENT) { printWith("%s: command not found\n", name); return; }
    if (child < 0) { print("cannot start the process\n"); return; }
    int status = 0;
    Syscalls::waitpid(child, &status);
    terminal.setMode(mode, false);
    const Terminfo &info = Terminfo::find(terminal.type());
    print(info.get(Terminfo::Cap::CursorNormal));
    print(info.get(Terminfo::Cap::Normal));
    if (terminal.column() != 0) {
        print("\n");
    }
}

bool
Shell::execute() {
    char *words[ArgList::MAX_ARGS + 1];
    unsigned n = 0;
    for (char *p = line; *p;) {
        while (*p == ' ') {
            *p++ = 0;
        }
        if (!*p) {
            break;
        }
        if (n == ArgList::MAX_ARGS + 1) {
            print("too many arguments\n");
            return true;
        }
        if (*p == '"') {
            words[n++] = ++p;
            while (*p && *p != '"') {
                p++;
            }
            if (*p) {
                *p++ = 0;
            }
        } else {
            words[n++] = p;
            while (*p && *p != ' ') {
                p++;
            }
        }
    }
    if (n == 0) {
        return true;
    }

    const char *command = words[0];
    ArgList args;
    for (unsigned i = 1; i < n; i++) {
        args.add(words[i]);
    }

    if (String::equals(command, "ps")) {
        ps();
    } else if (String::equals(command, "ls")) {
        ls();
    } else if (String::equals(command, "exit")) {
        if (pid == 1) {
            print("exit: PID 1 (init) cannot exit\n");
        } else {
            return false;
        }
    } else if (String::equals(command, "exec")) {
        if (n < 2) { print("usage: exec PROGRAM [ARGS]\n"); return true; }
        ArgList rest;
        for (unsigned i = 2; i < n; i++) {
            rest.add(words[i]);
        }
        Syscalls::exec(words[1], rest);
        printWith("exec: %s: no such program\n", words[1]);
    } else {
        runProgram(command, args);
    }
    return true;
}

int
Shell::run() {
    for (;;) {
        Readline::Result result = input->read(PROMPT, line, sizeof(line));
        if (result == Readline::Result::Eof) {
            if (pid == 1) {
                continue;
            }
            print("exit\n");
            return 0;
        }
        input->add(line);
        if (!execute()) {
            return 0;
        }
    }
}
