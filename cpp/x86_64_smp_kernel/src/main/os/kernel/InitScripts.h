#ifndef __INIT_SCRIPTS__
#define __INIT_SCRIPTS__
#include "kernel/Program.h"

class InitScripts {
public:
    static unsigned run();

private:
    enum class Mode { Run, Background, WaitLink };
    // What must exist for a step to run: nothing, the interface named in the step, or a network card (any interface
    // but the loopback, that is, ifconfig shows something besides lo).
    enum class Need { Nothing, Interface, Nic };
    class Step {
    public:
        Need need;
        const char *interfaceName;
        Mode mode;
        const char *line;
    };
    static constexpr unsigned LINK_TIMEOUT_MS = 15000;

    static const Step script[];
    static const unsigned scriptLength;

    static bool satisfied(const Step &step);
    static bool hasNic();
    static bool command(const Step &step);
    static bool waitForLink(const char *interfaceName);
};

#endif
