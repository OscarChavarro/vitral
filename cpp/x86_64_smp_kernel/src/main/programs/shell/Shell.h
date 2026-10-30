#ifndef __SHELL__
#define __SHELL__
#include "lib/readline/Readline.h"
#include "syscalls/Syscalls.h"
#include "kernel/ArgList.h"

class Shell {
public:
    static int main(const ArgList &args);

private:
    static constexpr unsigned MAX_ROWS = 256;
    static constexpr uint8_t NORMAL = 0x07;
    static constexpr const char *PROMPT = "> ";

    class Commands : public Readline::Completions {
    public:
        unsigned count() const override;
        const char *at(unsigned i) const override;
    };

    Shell();
    ~Shell();
    Shell(const Shell &) = delete;
    Shell &operator=(const Shell &) = delete;

    int run();
    bool execute();
    void print(const char *s);
    void printWith(const char *format, const char *arg);
    void ps();
    void ls();
    void runProgram(const char *name, const ArgList &args);
    static const char *stateName(uint8_t state);

    unsigned console;
    uint32_t pid;
    Syscalls::ProcessInfo *list;
    Readline *input;
    Commands commands;
    char line[Readline::MAX_LINE + 1];
};

#endif
