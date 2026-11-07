#ifndef __INIT_SCRIPTS__
#define __INIT_SCRIPTS__
#include <stddef.h>
#include <stdint.h>
#include "kernel/ArgList.h"
#include "kernel/Program.h"

class InitScripts {
public:
    static unsigned run();

private:
    // Run waits for the command, Background does not, and Respawn starts it again a moment after it ends, as getty
    // does for a terminal line. WaitLink waits for the interface named in the line to see a link.
    enum class Mode { Run, Background, Respawn, WaitLink };
    // What must exist for a step to run: nothing, the interface named in the step, a network card (any interface
    // but the loopback, that is, ifconfig shows something besides lo), or COM1.
    enum class Need { Nothing, Interface, Nic, Serial };
    class Step {
    public:
        Need need;
        const char *interfaceName;
        Mode mode;
        const char *line;
        // The terminal it runs on (its standard input, output and error), or null for a console of its own.
        const char *terminal;
    };
    static constexpr unsigned LINK_TIMEOUT_MS = 15000;
    static constexpr unsigned RESPAWN_PAUSE_MS = 1000;
    static constexpr unsigned RESPAWN_CHECK_MS = 500;
    static constexpr size_t RESPAWN_STACK_SIZE = 32 * 1024;

    static const Step script[];
    static const unsigned scriptLength;

    static bool satisfied(const Step &step);
    static bool hasNic();
    static bool command(const Step &step);
    static bool parse(const char *line, char (&buffer)[ArgList::BUFFER_SIZE], const char *&name, ArgList &args);
    static int64_t start(const Step &step, bool background);
    static void respawn(void *step);
    static bool waitForLink(const char *interfaceName);
};

#endif
