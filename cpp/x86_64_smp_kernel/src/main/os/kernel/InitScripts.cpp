#include "kernel/InitScripts.h"
#include "kernel/KernelLog.h"
#include "net/NetDevice.h"
#include "syscalls/Syscalls.h"
#include "net/NetDevices.h"
#include "kernel/ArgList.h"

const InitScripts::Step InitScripts::script[] = {
    {Need::Interface, "eth0", Mode::Run, "ifconfig eth0 192.168.1.199 netmask 255.255.255.0 up"},
    {Need::Interface, "eth0", Mode::WaitLink, "eth0"},
    {Need::Nic, nullptr, Mode::Background, "httpd"},
};
const unsigned InitScripts::scriptLength = sizeof(script) / sizeof(script[0]);

unsigned
InitScripts::run() {
    unsigned failures = 0;
    for (unsigned i = 0; i < scriptLength; i++) {
        const Step &step = script[i];
        if (!satisfied(step)) {
            continue;
        }
        bool ok = step.mode == Mode::WaitLink ? waitForLink(step.line) : command(step);
        if (!ok) {
            failures++;
        }
    }
    return failures;
}

bool
InitScripts::satisfied(const Step &step) {
    switch (step.need) {
    case Need::Nothing: return true;
    case Need::Interface: return NetDevices::find(step.interfaceName) != nullptr;
    case Need::Nic:
        if (hasNic()) {
            return true;
        }
        kprintf("init: %s: no network card (only the loopback), not started\n", step.line);
        return false;
    }
    return false;
}

// A network card is any interface but the loopback, so this is false when ifconfig only shows lo.
bool
InitScripts::hasNic() {
    for (unsigned i = 0; i < NetDevices::count(); i++) {
        if (!NetDevices::at(i)->isLoopback()) {
            return true;
        }
    }
    return false;
}

bool
InitScripts::command(const Step &step) {
    bool background = step.mode == Mode::Background;
    kprintf("init: %s%s\n", step.line, background ? " &" : "");

    char buffer[ArgList::BUFFER_SIZE];
    unsigned length = 0;
    for (const char *c = step.line; *c && length < sizeof(buffer) - 1; c++) {
        buffer[length++] = *c;
    }
    buffer[length] = '\0';
    const char *name = nullptr;
    ArgList args;
    bool tooMany = false;
    for (char *c = buffer; *c;) {
        while (*c == ' ') {
            *c++ = '\0';
        }
        if (!*c) {
            break;
        }
        char *word = c;
        while (*c && *c != ' ') {
            c++;
        }
        if (*c) {
            *c++ = '\0';
        }
        if (!name) {
            name = word;
        } else if (!args.add(word)) {
            tooMany = true;
        }
    }
    if (!name || tooMany) {
        kprintf("init: bad command line\n");
        return false;
    }

    if (background) {
        if (Syscalls::spawn(name, args) < 0) {
            kprintf("init: %s: cannot start\n", name);
            return false;
        }
        return true;
    }
    int64_t child = Syscalls::run(name, args);
    if (child < 0) {
        kprintf("init: %s: %s\n", name, child == -Syscalls::ERR_NOENT ? "command not found" : "cannot start");
        return false;
    }
    int status = 0;
    Syscalls::waitpid(child, &status);
    if (Syscalls::exitCode(status) != 0) {
        kprintf("init: %s failed with status %d\n", name, Syscalls::exitCode(status));
        return false;
    }
    return true;
}

bool
InitScripts::waitForLink(const char *interfaceName) {
    NetDevice *device = NetDevices::find(interfaceName);
    if (!device) {
        return false;
    }
    uint64_t start = Syscalls::uptimeMs();
    while (!device->hasLink()) {
        if (Syscalls::uptimeMs() - start >= LINK_TIMEOUT_MS) {
                kprintf("init: %s: no link after %u s, going on\n", interfaceName, LINK_TIMEOUT_MS / 1000);
            return false;
        }
        Syscalls::sleep(100);
    }
    kprintf("init: %s: link up after %u ms\n", interfaceName, (uint32_t)(Syscalls::uptimeMs() - start));
    return true;
}
